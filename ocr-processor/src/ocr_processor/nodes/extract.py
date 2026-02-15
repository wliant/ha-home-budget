import base64
import json
from datetime import date

import fitz
from langchain_core.messages import HumanMessage
from langchain_ollama import ChatOllama

from ocr_processor.config import settings
from ocr_processor.errors import (
    MODEL_SERVER_UNREACHABLE,
    NOT_A_RECEIPT,
    NO_EXPENSE_DATA,
    PROCESSING_TIMEOUT,
    NonRetryableError,
    RetryableError,
)
from ocr_processor.logging import get_logger, log_agent_step

logger = get_logger(__name__)

EXTRACT_PROMPT = """Analyze this receipt image and extract the following information as JSON:
{
  "is_receipt": true/false,
  "date": "YYYY-MM-DD or null if not visible",
  "line_items": [
    {
      "description": "item description",
      "amount": 0.00
    }
  ],
  "total": 0.00
}

Rules:
- Set is_receipt to false if the image is clearly NOT a receipt or invoice
- Extract every distinct line item with its amount
- If there's only a total and no line items, create one line item with the total
- Amounts should be numbers (not strings)
- Date should be in YYYY-MM-DD format if visible
- Return ONLY valid JSON, no other text"""


def _convert_pdf_to_image(pdf_bytes: bytes) -> bytes:
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    page = doc.load_page(0)
    pix = page.get_pixmap(dpi=200)
    img_bytes = pix.tobytes("png")
    doc.close()
    return img_bytes


async def extract_node(state: dict) -> dict:
    file_bytes = state["file_bytes"]
    file_type = state["file_type"]

    log_agent_step(logger, "extract", input_summary=f"file_type={file_type}")

    if file_type == "application/pdf":
        image_bytes = _convert_pdf_to_image(file_bytes)
        mime_type = "image/png"
    else:
        image_bytes = file_bytes
        mime_type = file_type

    b64_image = base64.b64encode(image_bytes).decode("utf-8")

    try:
        llm = ChatOllama(
            model=settings.vision_model,
            base_url=settings.ollama_base_url,
            timeout=settings.request_timeout,
        )

        message = HumanMessage(
            content=[
                {"type": "text", "text": EXTRACT_PROMPT},
                {
                    "type": "image_url",
                    "image_url": {"url": f"data:{mime_type};base64,{b64_image}"},
                },
            ]
        )

        response = await llm.ainvoke([message])
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
    log_agent_step(logger, "extract", output_summary=f"raw_response_length={len(raw_text)}")

    try:
        json_start = raw_text.find("{")
        json_end = raw_text.rfind("}") + 1
        if json_start == -1 or json_end == 0:
            raise ValueError("No JSON found in response")
        parsed = json.loads(raw_text[json_start:json_end])
    except (json.JSONDecodeError, ValueError):
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not extract any expense data from the receipt",
        )

    if not parsed.get("is_receipt", True) is True:
        raise NonRetryableError(
            NOT_A_RECEIPT,
            "The uploaded file does not appear to be a receipt",
        )

    line_items = parsed.get("line_items", [])
    if not line_items:
        total = parsed.get("total")
        if total and float(total) > 0:
            line_items = [{"description": "Receipt total", "amount": float(total)}]
        else:
            raise NonRetryableError(
                NO_EXPENSE_DATA,
                "Could not extract any expense data from the receipt",
            )

    receipt_date = parsed.get("date")
    if not receipt_date:
        receipt_date = date.today().isoformat()

    log_agent_step(
        logger,
        "extract",
        output_summary=f"items={len(line_items)}, date={receipt_date}",
    )

    return {
        "image_bytes": image_bytes,
        "extracted_text": raw_text,
        "line_items": line_items,
        "receipt_date": receipt_date,
    }
