import pytest

from ocr_processor.errors import NonRetryableError
from ocr_processor.nodes.validate import validate_node


@pytest.mark.asyncio
async def test_valid_jpeg_passes(sample_jpeg_state):
    result = await validate_node(sample_jpeg_state)
    assert result["file_type"] == "image/jpeg"
    assert result["file_bytes"] == sample_jpeg_state["file_bytes"]


@pytest.mark.asyncio
async def test_valid_png_passes(sample_png_state):
    result = await validate_node(sample_png_state)
    assert result["file_type"] == "image/png"


@pytest.mark.asyncio
async def test_valid_pdf_passes(sample_pdf_state):
    result = await validate_node(sample_pdf_state)
    assert result["file_type"] == "application/pdf"


@pytest.mark.asyncio
async def test_unsupported_format_raises_error(sample_categories):
    state = {
        "file_bytes": b"fake-docx-content",
        "file_type": "application/msword",
        "categories": sample_categories,
    }
    with pytest.raises(NonRetryableError) as exc_info:
        await validate_node(state)
    assert exc_info.value.error_code == "UNSUPPORTED_FORMAT"
    assert exc_info.value.retryable is False


@pytest.mark.asyncio
async def test_file_too_large_raises_error(sample_categories):
    large_bytes = b"x" * (11 * 1024 * 1024)  # 11MB, exceeds default 10MB limit
    state = {
        "file_bytes": large_bytes,
        "file_type": "image/jpeg",
        "categories": sample_categories,
    }
    with pytest.raises(NonRetryableError) as exc_info:
        await validate_node(state)
    assert exc_info.value.error_code == "FILE_TOO_LARGE"
    assert exc_info.value.retryable is False


@pytest.mark.asyncio
async def test_empty_categories_raises_error():
    state = {
        "file_bytes": b"fake-jpeg-content",
        "file_type": "image/jpeg",
        "categories": [],
    }
    with pytest.raises(NonRetryableError) as exc_info:
        await validate_node(state)
    assert exc_info.value.error_code == "EMPTY_CATEGORIES"
    assert exc_info.value.retryable is False
