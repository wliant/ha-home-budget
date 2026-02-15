# Data Model: OCR Extraction Upgrade

**Feature**: 020-ocr-extraction-upgrade
**Date**: 2026-02-16

## Entities

### AgentState (Modified)

The LangGraph pipeline state dictionary. No structural changes — the same fields flow between nodes.

| Field | Type | Set By | Consumed By | Notes |
|-------|------|--------|-------------|-------|
| file_bytes | bytes | API | validate, extract | Raw uploaded file |
| file_type | str | API | validate, extract | MIME type |
| categories | list[CategoryInput] | API | classify | User-provided categories |
| image_bytes | bytes | extract | (unused after LLaVA removal) | Keep for backward compat |
| extracted_text | str | extract | (logged) | Raw text from PDF/OCR |
| line_items | list[dict] | extract, classify | classify, format | Structured items |
| receipt_date | str \| None | extract | format | ISO date string |
| expenses | list[ExpenseOutput] | format | API response | Final output |
| error | ErrorResponse \| None | any node | API | Error state |

### Settings (Modified)

| Field | Type | Current | After Change |
|-------|------|---------|-------------|
| ollama_host | str | Keep | Keep |
| vision_model | str | "llava:13b" | **Removed** |
| text_model | str | "llama3.1:latest" | Keep |
| max_file_size_mb | int | 10 | Keep |
| log_level | str | "INFO" | Keep |
| request_timeout | int | 60 | Keep |

### Extract Node Output (Unchanged Interface)

The extract node continues to return the same dictionary structure:

```python
{
    "image_bytes": bytes,        # Image bytes (from PDF conversion or original)
    "extracted_text": str,       # Raw text (now from PDF/OCR instead of LLaVA)
    "line_items": list[dict],    # [{description: str, amount: float}, ...]
    "receipt_date": str,         # "YYYY-MM-DD"
}
```

### Classify Node Input/Output (Unchanged)

Input: `line_items` (list of dicts with description + amount) + `categories`
Output: `line_items` (list of dicts with description + amount + category_id + category_name)

### New Internal Types

#### PaddleOCR Engine (Module-Level Singleton)

Initialized once at import time. Not part of the state model — internal to extract node.

```
PaddleOCR(lang="en", ocr_version="PP-OCRv5", device="cpu")
```

## State Transitions

```
validate → extract → classify → format → END
```

No changes to the pipeline graph structure. Only the internal implementation of the extract node changes.

## Validation Rules

- PDF text extraction: minimum 50 characters of text to be considered "structured"
- Full-page image detection: image covering ≥95% of page area = scanned page
- PaddleOCR output: at least one non-empty text line required
- Text LLM parsing: must return valid JSON with `is_receipt`, `line_items`, and optionally `date`
