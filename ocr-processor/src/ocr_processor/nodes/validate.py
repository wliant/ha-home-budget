import io

from PIL import Image
from pillow_heif import register_heif_opener

from ocr_processor.config import settings
from ocr_processor.errors import (
    EMPTY_CATEGORIES,
    FILE_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    NonRetryableError,
)
from ocr_processor.logging import get_logger, log_agent_step

logger = get_logger(__name__)

# Register HEIF/HEIC opener so Pillow can handle these formats
register_heif_opener()

ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png"}
CONVERTIBLE_IMAGE_TYPES = {"image/heic", "image/heif", "image/webp"}
ALLOWED_PDF_TYPES = {"application/pdf"}
ALLOWED_TYPES = ALLOWED_IMAGE_TYPES | CONVERTIBLE_IMAGE_TYPES | ALLOWED_PDF_TYPES


def _convert_to_jpeg(file_bytes: bytes) -> bytes:
    """Convert image bytes (HEIC, HEIF, WEBP, etc.) to JPEG."""
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    return buf.getvalue()


async def validate_node(state: dict) -> dict:
    file_bytes = state["file_bytes"]
    file_type = state["file_type"]
    categories = state["categories"]

    log_agent_step(
        logger,
        "validate",
        input_summary=f"file_type={file_type}, size={len(file_bytes)}, categories={len(categories)}",
    )

    if file_type not in ALLOWED_TYPES:
        raise NonRetryableError(
            UNSUPPORTED_FORMAT,
            f"Only JPEG, PNG, HEIC, HEIF, WEBP, and PDF files are accepted. Got: {file_type}",
        )

    if len(file_bytes) > settings.max_file_size_bytes:
        raise NonRetryableError(
            FILE_TOO_LARGE,
            f"File size exceeds {settings.max_file_size_mb}MB limit",
        )

    if not categories:
        raise NonRetryableError(
            EMPTY_CATEGORIES,
            "At least one category must be provided",
        )

    # Convert HEIC/HEIF/WEBP to JPEG so downstream nodes can process them
    if file_type in CONVERTIBLE_IMAGE_TYPES:
        log_agent_step(logger, "validate", output_summary=f"converting {file_type} to JPEG")
        file_bytes = _convert_to_jpeg(file_bytes)
        file_type = "image/jpeg"

    log_agent_step(
        logger,
        "validate",
        output_summary="validation passed",
    )

    result = {"file_bytes": file_bytes, "file_type": file_type, "categories": categories}

    selected_category = state.get("selected_category")
    if selected_category is not None:
        result["selected_category"] = selected_category

    return result
