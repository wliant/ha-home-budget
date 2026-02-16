from langchain_core.prompts import ChatPromptTemplate
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
from ocr_processor.models import ClassificationResult, ClassifiedLineItem

logger = get_logger(__name__)

CLASSIFY_PROMPT = ChatPromptTemplate.from_messages([
    ("system", (
        "You are a household expense classifier. Given receipt line items and a list "
        "of spending categories, assign each line item to the most appropriate category.\n\n"
        "Rules:\n"
        "- Each line item should be assigned to exactly one category from the list\n"
        "- Use the exact category id and name from the provided list\n"
        "- If you are not confident about a category for an item, set category_id and category_name to null\n"
        "- Amounts should be positive numbers\n"
        "- Keep the original description and amount from the line items"
    )),
    ("human", (
        "Categories (id and name):\n{categories}\n\n"
        "Line items from receipt:\n{line_items}"
    )),
])


async def classify_node(state: dict) -> dict:
    line_items = state["line_items"]
    categories = state["categories"]

    categories_str = "\n".join(
        f"- id: {c.id}, name: {c.name}" for c in categories
    )
    items_str = "\n".join(
        f"- {item.description}: ${item.amount:.2f}" for item in line_items
    )

    log_agent_step(
        logger,
        "classify",
        input_summary=f"items={len(line_items)}, categories={len(categories)}",
    )

    try:
        llm = ChatOllama(
            model=settings.text_model,
            base_url=settings.ollama_base_url,
            timeout=settings.request_timeout,
        )
        structured_llm = llm.with_structured_output(ClassificationResult)
        chain = CLASSIFY_PROMPT | structured_llm
        result = await chain.ainvoke({
            "categories": categories_str,
            "line_items": items_str,
        })
    except NonRetryableError:
        raise
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

    if result is None or not result.items:
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not classify expense data from the receipt",
        )

    # Validate category IDs — set to null if LLM hallucinated an ID
    valid_ids = {c.id for c in categories}
    classified_items = []
    for item in result.items:
        if item.category_id is not None and item.category_id not in valid_ids:
            item = ClassifiedLineItem(
                description=item.description,
                amount=item.amount,
                category_id=None,
                category_name=None,
            )
        classified_items.append(item)

    log_agent_step(
        logger,
        "classify",
        output_summary=f"classified_items={len(classified_items)}",
    )

    return {"classified_items": classified_items}
