from ocr_processor.config import settings
from ocr_processor.errors import (
    EMPTY_CATEGORIES,
    FILE_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    NonRetryableError,
)
from ocr_processor.logging import get_logger, log_agent_step

logger = get_logger(__name__)

ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png"}
ALLOWED_PDF_TYPES = {"application/pdf"}
ALLOWED_TYPES = ALLOWED_IMAGE_TYPES | ALLOWED_PDF_TYPES


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
            f"Only JPEG, PNG, and PDF files are accepted. Got: {file_type}",
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

    log_agent_step(
        logger,
        "validate",
        output_summary="validation passed",
    )

    return {"file_bytes": file_bytes, "file_type": file_type, "categories": categories}
