import base64
import io
from datetime import date

import fitz
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage
from PIL import Image

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
from ocr_processor.models import ExtractedLineItem, ReceiptExtraction

logger = get_logger(__name__)

VISION_SYSTEM_PROMPT = (
    "You are a receipt parser. Analyze the receipt image and extract structured data.\n"
    "Rules:\n"
    "- Set is_receipt to false if the image is clearly NOT a receipt or invoice\n"
    "- Extract every distinct line item with its description and amount\n"
    "- If there's only a total and no line items, create one line item with the total\n"
    "- Amounts should be positive numbers\n"
    "- Date should be in YYYY-MM-DD format if visible, null otherwise"
)


def _get_image_bytes_and_media_type(file_bytes: bytes, file_type: str) -> tuple[bytes, str]:
    """Get image bytes suitable for vision API from file bytes."""
    if file_type == "application/pdf":
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        page = doc[0]
        pix = page.get_pixmap(dpi=200)
        img_bytes = pix.tobytes("png")
        doc.close()
        return img_bytes, "image/png"

    # For JPEG/PNG, ensure it's valid
    if file_type == "image/jpeg":
        return file_bytes, "image/jpeg"
    if file_type == "image/png":
        return file_bytes, "image/png"

    # Fallback: convert to PNG
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue(), "image/png"


async def extract_vision_node(state: dict) -> dict:
    file_bytes = state["file_bytes"]
    file_type = state["file_type"]

    log_agent_step(logger, "extract_vision", input_summary=f"file_type={file_type}")

    image_bytes, media_type = _get_image_bytes_and_media_type(file_bytes, file_type)
    b64_image = base64.standard_b64encode(image_bytes).decode("utf-8")

    try:
        llm = ChatAnthropic(
            model=settings.paid_model,
            api_key=settings.anthropic_api_key,
            timeout=settings.request_timeout,
        )
        structured_llm = llm.with_structured_output(ReceiptExtraction)

        message = HumanMessage(content=[
            {"type": "text", "text": VISION_SYSTEM_PROMPT},
            {
                "type": "image_url",
                "image_url": {"url": f"data:{media_type};base64,{b64_image}"},
            },
            {"type": "text", "text": "Please extract the receipt data from this image."},
        ])

        result = await structured_llm.ainvoke([message])
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

    if result is None:
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not extract any expense data from the receipt",
        )

    log_agent_step(logger, "extract_vision", output_summary=f"structured_items={len(result.line_items)}")

    if not result.is_receipt:
        raise NonRetryableError(
            NOT_A_RECEIPT,
            "The uploaded file does not appear to be a receipt",
        )

    line_items = result.line_items
    if not line_items:
        if result.total and float(result.total) > 0:
            line_items = [ExtractedLineItem(description="Receipt total", amount=result.total)]
        else:
            raise NonRetryableError(
                NO_EXPENSE_DATA,
                "Could not extract any expense data from the receipt",
            )

    receipt_date = result.date
    if not receipt_date:
        receipt_date = date.today().isoformat()

    total_amount = str(result.total)

    log_agent_step(
        logger,
        "extract_vision",
        output_summary=f"items={len(line_items)}, date={receipt_date}, total={total_amount}",
    )

    return {
        "image_bytes": image_bytes,
        "line_items": line_items,
        "receipt_date": receipt_date,
        "total_amount": total_amount,
    }
