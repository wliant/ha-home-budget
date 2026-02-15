import json

from langchain_core.messages import HumanMessage
from langchain_ollama import ChatOllama

from ocr_processor.config import settings
from ocr_processor.errors import (
    MODEL_SERVER_UNREACHABLE,
    NO_EXPENSE_DATA,
    PROCESSING_TIMEOUT,
    NonRetryableError,
    RetryableError,
)
from ocr_processor.logging import get_logger, log_agent_step

logger = get_logger(__name__)

CLASSIFY_PROMPT_TEMPLATE = """You are a household expense classifier. Given receipt line items and a list of spending categories, assign each line item to the most appropriate category.

Categories (id and name):
{categories}

Line items from receipt:
{line_items}

Group the line items by category. For items in the same category, sum their amounts into one expense.

Return ONLY valid JSON in this format:
{{
  "expenses": [
    {{
      "description": "brief description of what was purchased",
      "amount": 0.00,
      "category_id": 1,
      "category_name": "Category Name"
    }}
  ]
}}

Rules:
- Every line item must be assigned to exactly one category
- If all items belong to the same category, return one expense with the summed total
- If items span multiple categories, return multiple expenses grouped by category
- Use the exact category id and name from the list above
- Amounts should be positive numbers
- Return ONLY valid JSON, no other text"""


async def classify_node(state: dict) -> dict:
    line_items = state["line_items"]
    categories = state["categories"]

    categories_str = "\n".join(
        f"- id: {c.id if hasattr(c, 'id') else c['id']}, name: {c.name if hasattr(c, 'name') else c['name']}"
        for c in categories
    )
    items_str = "\n".join(
        f"- {item['description']}: ${item['amount']:.2f}" for item in line_items
    )

    log_agent_step(
        logger,
        "classify",
        input_summary=f"items={len(line_items)}, categories={len(categories)}",
    )

    prompt = CLASSIFY_PROMPT_TEMPLATE.format(
        categories=categories_str,
        line_items=items_str,
    )

    try:
        llm = ChatOllama(
            model=settings.text_model,
            base_url=settings.ollama_base_url,
            timeout=settings.request_timeout,
        )

        response = await llm.ainvoke([HumanMessage(content=prompt)])
    except Exception as e:
        error_msg = str(e).lower()
        if "timeout" in error_msg or "timed out" in error_msg:
            raise RetryableError(
                PROCESSING_TIMEOUT,
                "Receipt processing timed out, please try again",
            )
        if "connection" in error_msg or "connect" in error_msg or "refused" in error_msg:
            raise RetryableError(
                MODEL_SERVER_UNREACHABLE,
                "The AI model server is temporarily unavailable",
            )
        raise RetryableError(
            MODEL_SERVER_UNREACHABLE,
            f"Failed to communicate with AI model server: {e}",
        )

    raw_text = response.content
    log_agent_step(logger, "classify", output_summary=f"raw_length={len(raw_text)}")

    try:
        json_start = raw_text.find("{")
        json_end = raw_text.rfind("}") + 1
        if json_start == -1 or json_end == 0:
            raise ValueError("No JSON found")
        parsed = json.loads(raw_text[json_start:json_end])
    except (json.JSONDecodeError, ValueError):
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not classify expense data from the receipt",
        )

    classified_expenses = parsed.get("expenses", [])
    if not classified_expenses:
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not classify expense data from the receipt",
        )

    # Validate category IDs exist in input
    valid_ids = {c.id if hasattr(c, "id") else c["id"] for c in categories}
    for exp in classified_expenses:
        if exp.get("category_id") not in valid_ids:
            # Fall back to first category if LLM hallucinated an ID
            first = categories[0]
            exp["category_id"] = first.id if hasattr(first, "id") else first["id"]
            exp["category_name"] = first.name if hasattr(first, "name") else first["name"]

    log_agent_step(
        logger,
        "classify",
        output_summary=f"classified_expenses={len(classified_expenses)}",
    )

    return {"line_items": classified_expenses}
