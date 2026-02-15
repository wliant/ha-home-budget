import json
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock

import pytest
from httpx import ASGITransport, AsyncClient

from ocr_processor.main import app
from ocr_processor.models import CategoryInput

FIXTURES_DIR = Path(__file__).parent / "fixtures"


# --- API-level fixtures ---


@pytest.fixture
def sample_receipt_path():
    return FIXTURES_DIR / "sample_receipt.jpg"


@pytest.fixture
def sample_categories_json():
    return '[{"id": 1, "name": "Groceries"}, {"id": 2, "name": "Dining"}, {"id": 3, "name": "Transport"}]'


@pytest.fixture
async def client():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


# --- Shared node-level fixtures ---


@pytest.fixture
def sample_categories():
    return [
        CategoryInput(id=1, name="Groceries"),
        CategoryInput(id=2, name="Dining"),
        CategoryInput(id=3, name="Transport"),
    ]


@pytest.fixture
def sample_jpeg_state(sample_categories):
    return {
        "file_bytes": b"\xff\xd8\xff\xe0fake-jpeg-content",
        "file_type": "image/jpeg",
        "categories": sample_categories,
    }


@pytest.fixture
def sample_png_state(sample_categories):
    return {
        "file_bytes": b"\x89PNG\r\n\x1a\nfake-png-content",
        "file_type": "image/png",
        "categories": sample_categories,
    }


@pytest.fixture
def sample_pdf_state(sample_categories):
    return {
        "file_bytes": b"%PDF-1.4 fake-pdf-content",
        "file_type": "application/pdf",
        "categories": sample_categories,
    }


@pytest.fixture
def sample_line_items():
    return [
        {"description": "Milk", "amount": 3.99},
        {"description": "Bread", "amount": 2.49},
        {"description": "Eggs", "amount": 4.99},
    ]


@pytest.fixture
def sample_classified_items():
    return [
        {"description": "Milk and Bread", "amount": 6.48, "category_id": 1, "category_name": "Groceries"},
        {"description": "Eggs", "amount": 4.99, "category_id": 1, "category_name": "Groceries"},
    ]


@pytest.fixture
def valid_extract_llm_response():
    """Mock LLM response for text parsing in extract node."""
    return json.dumps({
        "is_receipt": True,
        "date": "2026-02-15",
        "line_items": [
            {"description": "Milk", "amount": 3.99},
            {"description": "Bread", "amount": 2.49},
        ],
        "total": 6.48,
    })


@pytest.fixture
def valid_classify_llm_response():
    """Mock LLM response for classification in classify node."""
    return json.dumps({
        "expenses": [
            {"description": "Milk and Bread", "amount": 6.48, "category_id": 1, "category_name": "Groceries"},
        ]
    })


@pytest.fixture
def sample_ocr_text():
    """Sample raw text that PaddleOCR might return from a receipt."""
    return "GROCERY STORE\n123 Main St\nMilk           $3.99\nBread          $2.49\nEggs           $4.99\nTOTAL          $11.47\n02/15/2026"


@pytest.fixture
def sample_pdf_structured_text():
    """Sample text extracted from a structured PDF receipt."""
    return "Order Confirmation\nItem: Wireless Mouse  $29.99\nItem: USB Cable  $9.99\nSubtotal: $39.98\nTax: $3.20\nTotal: $43.18\nDate: 2026-02-15"


@pytest.fixture
def mock_llm_response():
    """Create a mock ChatOllama response object."""
    def _make_response(content: str):
        mock_resp = MagicMock()
        mock_resp.content = content
        return mock_resp
    return _make_response
