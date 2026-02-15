# API Contract: OCR Extraction Upgrade

**Feature**: 020-ocr-extraction-upgrade
**Date**: 2026-02-16

## External API (No Changes)

The `/process` and `/health` endpoints remain unchanged. This feature only modifies internal processing logic.

### POST /process

**Request**: Unchanged
- `file`: UploadFile (JPEG, PNG, or PDF)
- `categories`: JSON string of `[{id: int, name: str}, ...]`

**Response (200)**: Unchanged
```json
{
  "expenses": [
    {
      "amount": 12.99,
      "description": "Grocery items",
      "expense_date": "2026-02-16",
      "category_id": 1,
      "category_name": "Groceries"
    }
  ]
}
```

**Error Responses**: Unchanged
- 422: Non-retryable errors (UNSUPPORTED_FORMAT, EMPTY_CATEGORIES, FILE_TOO_LARGE, NOT_A_RECEIPT, NO_EXPENSE_DATA)
- 503: Retryable errors (MODEL_SERVER_UNREACHABLE, PROCESSING_TIMEOUT)

### GET /health

**Response**: Unchanged structure
```json
{
  "status": "healthy",
  "ollama_reachable": true
}
```

Note: `ollama_reachable` still checks the Ollama server since the text model (llama3.1) is still required for text parsing and classification. The vision model check is implicitly removed since Ollama tag listing doesn't check specific models.

## Internal Contract Changes

### Extract Node

**Input** (unchanged):
```python
{
    "file_bytes": bytes,
    "file_type": str,  # "application/pdf", "image/jpeg", "image/png"
}
```

**Output** (unchanged interface, different source):
```python
{
    "image_bytes": bytes,
    "extracted_text": str,      # Was: LLaVA raw response; Now: OCR/PDF raw text
    "line_items": [{"description": str, "amount": float}],
    "receipt_date": str,        # "YYYY-MM-DD"
}
```

**Internal flow change**:
- Before: file → (PDF→image) → base64 → LLaVA vision LLM → structured JSON
- After: file → (PDF text extraction OR PaddleOCR) → raw text → text LLM parsing → structured JSON

### Config

**Removed**: `vision_model` setting
**Kept**: All other settings unchanged
