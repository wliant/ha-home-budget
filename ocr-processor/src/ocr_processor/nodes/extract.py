import io
from datetime import date

import fitz
import pytesseract
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama
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


EXTRACT_PROMPT = ChatPromptTemplate.from_messages([
    ("system", (
        "You are a receipt parser. Analyze the receipt text and extract structured data.\n"
        "Rules:\n"
        "- Set is_receipt to false if the text is clearly NOT from a receipt or invoice\n"
        "- Extract every distinct line item with its description and amount\n"
        "- If there's only a total and no line items, create one line item with the total\n"
        "- Amounts should be positive numbers\n"
        "- Date should be in YYYY-MM-DD format if visible, null otherwise"
    )),
    ("human", "Receipt text:\n{receipt_text}"),
])

MIN_STRUCTURED_TEXT_LENGTH = 50
FULL_PAGE_IMAGE_COVERAGE_THRESHOLD = 0.95


def _has_extractable_text(page: fitz.Page) -> bool:
    """Check if a PDF page has real embedded text (not scanned image with OCR overlay)."""
    text = page.get_text().strip()
    if not text or len(text) < MIN_STRUCTURED_TEXT_LENGTH:
        return False

    page_area = abs(page.rect)
    if page_area == 0:
        return False

    for img in page.get_images(full=True):
        bbox = page.get_image_bbox(img)
        intersection = abs(bbox & page.rect)
        if intersection / page_area >= FULL_PAGE_IMAGE_COVERAGE_THRESHOLD:
            return False

    return True


def _extract_pdf_text(pdf_bytes: bytes) -> tuple[str, list[bytes]]:
    """Extract text from PDF pages. Returns (structured_text, scanned_page_images).

    For pages with embedded text, extracts text directly.
    For scanned/image-only pages, converts to PNG images for OCR.
    """
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    structured_text_parts = []
    scanned_page_images = []

    for page in doc:
        if _has_extractable_text(page):
            structured_text_parts.append(page.get_text())
        else:
            pix = page.get_pixmap(dpi=200)
            scanned_page_images.append(pix.tobytes("png"))

    doc.close()

    structured_text = "\n".join(structured_text_parts)
    return structured_text, scanned_page_images


def _extract_text_with_ocr(image_bytes: bytes) -> str:
    """Extract text from image bytes using TesseractOCR."""
    pil_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    # Use pytesseract to extract text
    # Config options: --psm 6 assumes uniform text block, --oem 3 uses default OCR engine mode
    text = pytesseract.image_to_string(pil_image, config='--psm 6 --oem 3')

    return text.strip()


async def _parse_text_with_llm(raw_text: str) -> ReceiptExtraction:
    """Parse raw receipt text into structured data using the text LLM with structured output."""
    try:
        llm = ChatOllama(
            model=settings.text_model,
            base_url=settings.ollama_base_url,
            timeout=settings.request_timeout,
        )
        structured_llm = llm.with_structured_output(ReceiptExtraction)
        chain = EXTRACT_PROMPT | structured_llm
        result = await chain.ainvoke({"receipt_text": raw_text})
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

    log_agent_step(logger, "extract", output_summary=f"structured_items={len(result.line_items)}")

    return result


async def extract_node(state: dict) -> dict:
    file_bytes = state["file_bytes"]
    file_type = state["file_type"]

    log_agent_step(logger, "extract", input_summary=f"file_type={file_type}")

    raw_text = ""
    image_bytes = file_bytes

    if file_type == "application/pdf":
        structured_text, scanned_page_images = _extract_pdf_text(file_bytes)

        if structured_text.strip():
            raw_text = structured_text
            log_agent_step(logger, "extract", output_summary="pdf_text_extraction")

        if scanned_page_images:
            ocr_texts = []
            for page_img in scanned_page_images:
                ocr_text = _extract_text_with_ocr(page_img)
                if ocr_text.strip():
                    ocr_texts.append(ocr_text)
            if ocr_texts:
                raw_text = raw_text + "\n" + "\n".join(ocr_texts) if raw_text else "\n".join(ocr_texts)
                log_agent_step(logger, "extract", output_summary=f"pdf_ocr_pages={len(scanned_page_images)}")

            if scanned_page_images:
                image_bytes = scanned_page_images[0]
    else:
        raw_text = _extract_text_with_ocr(file_bytes)
        log_agent_step(logger, "extract", output_summary="image_ocr_extraction")

    if not raw_text.strip():
        raise NonRetryableError(
            NO_EXPENSE_DATA,
            "Could not extract any text from the receipt",
        )

    log_agent_step(logger, "extract", output_summary=f"raw_text_length={len(raw_text)}")

    result = await _parse_text_with_llm(raw_text)

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
        "extract",
        output_summary=f"items={len(line_items)}, date={receipt_date}, total={total_amount}",
    )

    return {
        "image_bytes": image_bytes,
        "extracted_text": raw_text,
        "line_items": line_items,
        "receipt_date": receipt_date,
        "total_amount": total_amount,
    }
