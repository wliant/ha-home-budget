import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from ocr_processor.errors import NonRetryableError, RetryableError
from ocr_processor.nodes.classify import classify_node


@pytest.mark.asyncio
async def test_valid_classification(sample_line_items, sample_categories, valid_classify_llm_response, mock_llm_response):
    """Valid classification should return categorized expenses."""
    mock_resp = mock_llm_response(valid_classify_llm_response)

    with patch("ocr_processor.nodes.classify.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {
            "line_items": sample_line_items,
            "categories": sample_categories,
        }
        result = await classify_node(state)

    assert "line_items" in result
    classified = result["line_items"]
    assert len(classified) >= 1
    assert "category_id" in classified[0]
    assert "category_name" in classified[0]
    assert classified[0]["category_id"] == 1
    assert classified[0]["category_name"] == "Groceries"


@pytest.mark.asyncio
async def test_invalid_category_id_falls_back(sample_line_items, sample_categories, mock_llm_response):
    """Invalid category ID should fall back to first category."""
    response = json.dumps({
        "expenses": [
            {"description": "Item", "amount": 5.00, "category_id": 999, "category_name": "Fake"},
        ]
    })
    mock_resp = mock_llm_response(response)

    with patch("ocr_processor.nodes.classify.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {
            "line_items": sample_line_items,
            "categories": sample_categories,
        }
        result = await classify_node(state)

    classified = result["line_items"]
    # Should fall back to first category (id=1, Groceries)
    assert classified[0]["category_id"] == 1
    assert classified[0]["category_name"] == "Groceries"


@pytest.mark.asyncio
async def test_empty_classification_raises_error(sample_line_items, sample_categories, mock_llm_response):
    """Empty classification result should raise NonRetryableError."""
    response = json.dumps({"expenses": []})
    mock_resp = mock_llm_response(response)

    with patch("ocr_processor.nodes.classify.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {
            "line_items": sample_line_items,
            "categories": sample_categories,
        }
        with pytest.raises(NonRetryableError) as exc_info:
            await classify_node(state)
        assert exc_info.value.error_code == "NO_EXPENSE_DATA"


@pytest.mark.asyncio
async def test_llm_timeout_raises_retryable(sample_line_items, sample_categories):
    """LLM timeout should raise RetryableError."""
    with patch("ocr_processor.nodes.classify.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.side_effect = Exception("Connection timed out")
        mock_ollama.return_value = mock_instance

        state = {
            "line_items": sample_line_items,
            "categories": sample_categories,
        }
        with pytest.raises(RetryableError) as exc_info:
            await classify_node(state)
        assert exc_info.value.error_code == "PROCESSING_TIMEOUT"


@pytest.mark.asyncio
async def test_malformed_json_raises_error(sample_line_items, sample_categories, mock_llm_response):
    """Malformed JSON response should raise NonRetryableError."""
    mock_resp = mock_llm_response("Not valid JSON at all")

    with patch("ocr_processor.nodes.classify.ChatOllama") as mock_ollama:
        mock_instance = AsyncMock()
        mock_instance.ainvoke.return_value = mock_resp
        mock_ollama.return_value = mock_instance

        state = {
            "line_items": sample_line_items,
            "categories": sample_categories,
        }
        with pytest.raises(NonRetryableError) as exc_info:
            await classify_node(state)
        assert exc_info.value.error_code == "NO_EXPENSE_DATA"
