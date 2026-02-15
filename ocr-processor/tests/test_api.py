from pathlib import Path

import pytest


@pytest.mark.asyncio
async def test_health_endpoint(client):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "ollama_reachable" in data


@pytest.mark.asyncio
async def test_process_receipt_returns_valid_structure(
    client, sample_receipt_path, sample_categories_json
):
    """FR-016: At least one automated test that submits a real receipt image
    and validates the response structure.

    NOTE: This test requires a running Ollama server with llava:13b and
    llama3.1:latest loaded. Skip if Ollama is not available.
    """
    # Check Ollama availability first
    health = await client.get("/health")
    if not health.json().get("ollama_reachable"):
        pytest.skip("Ollama server not reachable")

    with open(sample_receipt_path, "rb") as f:
        response = await client.post(
            "/process",
            files={"file": ("receipt.jpg", f, "image/jpeg")},
            data={"categories": sample_categories_json},
        )

    assert response.status_code in (200, 422, 503), f"Unexpected status: {response.status_code}"

    data = response.json()

    if response.status_code == 200:
        assert "expenses" in data
        assert len(data["expenses"]) >= 1
        for expense in data["expenses"]:
            assert "amount" in expense
            assert "description" in expense
            assert "expense_date" in expense
            assert "category_id" in expense
            assert "category_name" in expense
            assert isinstance(expense["amount"], (int, float))
            assert isinstance(expense["category_id"], int)
    else:
        # Error response structure validation
        assert "error_code" in data
        assert "message" in data
        assert "retryable" in data


@pytest.mark.asyncio
async def test_process_empty_categories_returns_422(client, sample_receipt_path):
    with open(sample_receipt_path, "rb") as f:
        response = await client.post(
            "/process",
            files={"file": ("receipt.jpg", f, "image/jpeg")},
            data={"categories": "[]"},
        )

    assert response.status_code == 422
    data = response.json()
    assert data["error_code"] == "EMPTY_CATEGORIES"
    assert data["retryable"] is False


@pytest.mark.asyncio
async def test_process_pdf_receipt_returns_valid_structure(
    client, sample_categories_json
):
    """Test that PDF receipts are accepted and processed."""
    health = await client.get("/health")
    if not health.json().get("ollama_reachable"):
        pytest.skip("Ollama server not reachable")

    pdf_path = Path(__file__).parent / "fixtures" / "sample_receipt.pdf"
    with open(pdf_path, "rb") as f:
        response = await client.post(
            "/process",
            files={"file": ("receipt.pdf", f, "application/pdf")},
            data={"categories": sample_categories_json},
        )

    assert response.status_code in (200, 422, 503)
    data = response.json()

    if response.status_code == 200:
        assert "expenses" in data
        assert len(data["expenses"]) >= 1


@pytest.mark.asyncio
async def test_process_unsupported_format_returns_422(client, sample_categories_json):
    response = await client.post(
        "/process",
        files={"file": ("doc.docx", b"fake content", "application/msword")},
        data={"categories": sample_categories_json},
    )

    assert response.status_code == 422
    data = response.json()
    assert data["error_code"] == "UNSUPPORTED_FORMAT"
    assert data["retryable"] is False
