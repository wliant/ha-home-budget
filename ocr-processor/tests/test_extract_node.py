import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from ocr_processor.errors import NonRetryableError, RetryableError
from ocr_processor.nodes.extract import (
    _extract_text_with_ocr,
    _has_extractable_text,
    _parse_text_with_llm,
    extract_node,
)


# --- _has_extractable_text tests ---


def test_has_extractable_text_with_real_text():
    """Page with sufficient embedded text and no full-page image should return True."""
    mock_page = MagicMock()
    mock_page.get_text.return_value = "A" * 100  # >50 chars
    mock_page.rect = MagicMock()
    mock_page.rect.__abs__ = MagicMock(return_value=612.0 * 792.0)
    mock_page.get_images.return_value = []
    assert _has_extractable_text(mock_page) is True


def test_has_extractable_text_empty_text():
    """Page with no text should return False."""
    mock_page = MagicMock()
    mock_page.get_text.return_value = "   "
    assert _has_extractable_text(mock_page) is False


def test_has_extractable_text_short_text():
    """Page with very little text (<50 chars) should return False."""
    mock_page = MagicMock()
    mock_page.get_text.return_value = "short text"
    assert _has_extractable_text(mock_page) is False


# --- _parse_text_with_llm tests ---


@pytest.mark.asyncio
async def test_parse_text_with_llm_valid_response(valid_extract_llm_response, mock_llm_response):
    """Valid LLM response should be parsed into structured data."""
    mock_resp = mock_llm_response(valid_extract_llm_response)

    with patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        result = await _parse_text_with_llm("Milk $3.99\nBread $2.49")

    assert result["is_receipt"] is True
    assert result["date"] == "2026-02-15"
    assert len(result["line_items"]) == 2
    assert result["line_items"][0]["description"] == "Milk"
    assert result["line_items"][0]["amount"] == 3.99


@pytest.mark.asyncio
async def test_parse_text_with_llm_timeout():
    """LLM timeout should raise RetryableError."""
    with patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.side_effect = Exception("Connection timed out")
        mock_ollama.return_value = mock_instance

        with pytest.raises(RetryableError) as exc_info:
            await _parse_text_with_llm("some text")
        assert exc_info.value.error_code == "PROCESSING_TIMEOUT"


@pytest.mark.asyncio
async def test_parse_text_with_llm_connection_error():
    """LLM connection error should raise RetryableError."""
    with patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.side_effect = Exception("Connection refused")
        mock_ollama.return_value = mock_instance

        with pytest.raises(RetryableError) as exc_info:
            await _parse_text_with_llm("some text")
        assert exc_info.value.error_code == "MODEL_SERVER_UNREACHABLE"


@pytest.mark.asyncio
async def test_parse_text_with_llm_bad_json(mock_llm_response):
    """Invalid JSON response should raise NonRetryableError."""
    mock_resp = mock_llm_response("This is not JSON at all")

    with patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        with pytest.raises(NonRetryableError) as exc_info:
            await _parse_text_with_llm("some text")
        assert exc_info.value.error_code == "NO_EXPENSE_DATA"


# --- _extract_text_with_ocr tests ---


def test_extract_text_with_ocr_returns_text():
    """PaddleOCR should extract text from image bytes."""
    # Create a minimal valid 1x1 PNG image
    import io
    from PIL import Image
    img = Image.new("RGB", (10, 10), color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    png_bytes = buf.getvalue()

    mock_result = MagicMock()
    mock_result.json = {
        "rec_texts": ["GROCERY STORE", "Milk $3.99", "TOTAL $3.99"],
        "rec_scores": [0.95, 0.92, 0.98],
        "rec_boxes": [[0, 0, 100, 20], [0, 30, 100, 50], [0, 60, 100, 80]],
    }

    with patch("ocr_processor.nodes.extract._get_ocr_engine") as mock_engine:
        mock_engine.return_value.predict.return_value = [mock_result]
        result = _extract_text_with_ocr(png_bytes)

    assert "GROCERY STORE" in result
    assert "Milk $3.99" in result
    assert "TOTAL $3.99" in result


# --- extract_node integration tests (with mocks) ---


@pytest.mark.asyncio
async def test_extract_node_structured_pdf(valid_extract_llm_response, mock_llm_response):
    """Structured PDF should extract text directly via PyMuPDF and parse with LLM."""
    mock_page = MagicMock()
    mock_page.get_text.return_value = "Order\nItem: Mouse $29.99\nTotal: $29.99\nDate: 2026-02-15"
    mock_page.rect = MagicMock()
    mock_page.rect.__abs__ = MagicMock(return_value=612.0 * 792.0)
    mock_page.get_images.return_value = []

    mock_doc = MagicMock()
    mock_doc.__iter__ = MagicMock(return_value=iter([mock_page]))
    mock_doc.close = MagicMock()

    mock_resp = mock_llm_response(valid_extract_llm_response)

    with patch("ocr_processor.nodes.extract.fitz") as mock_fitz, \
         patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_fitz.open.return_value = mock_doc
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {"file_bytes": b"fake-pdf", "file_type": "application/pdf"}
        result = await extract_node(state)

    assert "line_items" in result
    assert len(result["line_items"]) == 2
    assert result["receipt_date"] == "2026-02-15"


@pytest.mark.asyncio
async def test_extract_node_image_ocr(valid_extract_llm_response, mock_llm_response):
    """Image files should use PaddleOCR and then parse with LLM."""
    mock_ocr_result = MagicMock()
    mock_ocr_result.json = {
        "rec_texts": ["Milk $3.99", "Bread $2.49"],
        "rec_scores": [0.95, 0.92],
    }

    mock_resp = mock_llm_response(valid_extract_llm_response)

    # Create a minimal valid PNG image
    import io
    from PIL import Image
    img = Image.new("RGB", (10, 10), color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    png_bytes = buf.getvalue()

    with patch("ocr_processor.nodes.extract._get_ocr_engine") as mock_engine, \
         patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_engine.return_value.predict.return_value = [mock_ocr_result]
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {"file_bytes": png_bytes, "file_type": "image/jpeg"}
        result = await extract_node(state)

    assert "line_items" in result
    assert len(result["line_items"]) == 2
    assert result["receipt_date"] == "2026-02-15"


@pytest.mark.asyncio
async def test_extract_node_scanned_pdf(valid_extract_llm_response, mock_llm_response):
    """Scanned PDF should fall back to image conversion + PaddleOCR."""
    # Page with no extractable text (short text)
    mock_page = MagicMock()
    mock_page.get_text.return_value = ""
    mock_pix = MagicMock()

    # Create a minimal valid PNG for the pixmap output
    import io
    from PIL import Image
    img = Image.new("RGB", (10, 10), color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    png_bytes = buf.getvalue()

    mock_pix.tobytes.return_value = png_bytes
    mock_page.get_pixmap.return_value = mock_pix

    mock_doc = MagicMock()
    mock_doc.__iter__ = MagicMock(return_value=iter([mock_page]))
    mock_doc.close = MagicMock()

    mock_ocr_result = MagicMock()
    mock_ocr_result.json = {
        "rec_texts": ["Eggs $4.99", "TOTAL $4.99"],
        "rec_scores": [0.95, 0.98],
    }

    mock_resp = mock_llm_response(valid_extract_llm_response)

    with patch("ocr_processor.nodes.extract.fitz") as mock_fitz, \
         patch("ocr_processor.nodes.extract._get_ocr_engine") as mock_engine, \
         patch("ocr_processor.nodes.extract.ChatOllama") as mock_ollama:
        mock_fitz.open.return_value = mock_doc
        mock_engine.return_value.predict.return_value = [mock_ocr_result]
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {"file_bytes": b"fake-scanned-pdf", "file_type": "application/pdf"}
        result = await extract_node(state)

    assert "line_items" in result
    assert result["receipt_date"] == "2026-02-15"


@pytest.mark.asyncio
async def test_extract_node_empty_text_raises_error():
    """Empty extraction text should raise NonRetryableError."""
    mock_ocr_result = MagicMock()
    mock_ocr_result.json = {"rec_texts": [], "rec_scores": []}

    # Create a minimal valid PNG
    import io
    from PIL import Image
    img = Image.new("RGB", (10, 10), color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    png_bytes = buf.getvalue()

    with patch("ocr_processor.nodes.extract._get_ocr_engine") as mock_engine:
        mock_engine.return_value.predict.return_value = [mock_ocr_result]

        state = {"file_bytes": png_bytes, "file_type": "image/jpeg"}
        with pytest.raises(NonRetryableError) as exc_info:
            await extract_node(state)
        assert exc_info.value.error_code == "NO_EXPENSE_DATA"
