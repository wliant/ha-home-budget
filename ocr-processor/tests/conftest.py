import os
from pathlib import Path

import pytest
from httpx import ASGITransport, AsyncClient

from ocr_processor.main import app

FIXTURES_DIR = Path(__file__).parent / "fixtures"


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
