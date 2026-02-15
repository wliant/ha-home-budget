# Quickstart: OCR Extraction Upgrade

**Feature**: 020-ocr-extraction-upgrade
**Date**: 2026-02-16

## Prerequisites

- OCR processor service running (`uv run uvicorn ocr_processor.main:app` in `ocr-processor/`)
- Ollama server running with `llama3.1:latest` model loaded (for text parsing and classification)
- PaddleOCR models downloaded (auto-downloads on first run)
- LLaVA model is NOT required (vision model dependency removed)

## Integration Scenarios

### Scenario 1: Structured PDF Receipt (Fast Path)

1. Upload a digitally-generated PDF receipt (e.g., Amazon order confirmation, email-attached invoice)
2. The system detects embedded text in the PDF via PyMuPDF
3. Text is extracted directly without image conversion or OCR
4. The text LLM parses raw text into structured line items
5. Classification proceeds as before

**Data flow**: PDF bytes → PyMuPDF `get_text()` → raw text → text LLM parsing → line_items → classify → format → response

**Verification**: No vision model is called. Processing should be noticeably faster than the previous approach since no image conversion or base64 encoding occurs.

### Scenario 2: Image Receipt (JPEG/PNG)

1. Upload a photo of a paper receipt (JPEG or PNG)
2. PaddleOCR extracts text from the image
3. The text LLM parses OCR output into structured line items
4. Classification proceeds as before

**Data flow**: Image bytes → PIL → numpy array → PaddleOCR → raw text → text LLM parsing → line_items → classify → format → response

### Scenario 3: Scanned PDF (Image-Only)

1. Upload a scanned PDF receipt (contains only images, no embedded text)
2. PyMuPDF detects no extractable text (or full-page image coverage)
3. Falls back to converting PDF pages to images
4. PaddleOCR processes the images
5. The text LLM parses OCR output, classification proceeds

**Data flow**: PDF bytes → PyMuPDF detects scanned → page to pixmap → PaddleOCR → raw text → text LLM → classify → format → response

### Scenario 4: Multi-Page PDF

1. Upload a multi-page PDF receipt
2. Text from all pages is extracted (not just the first page)
3. Text is concatenated and sent for parsing

### Scenario 5: No Text Extracted

1. Upload a very blurry image or corrupted file
2. PaddleOCR returns empty/minimal text
3. System returns a 422 error with `NO_EXPENSE_DATA` code

### Scenario 6: Run Unit Tests

1. Run `cd ocr-processor && uv run pytest tests/`
2. All node-level tests pass without requiring Ollama or PaddleOCR (all mocked)
3. Tests cover: validate, extract (both paths), classify, format

## Quick Verification Checklist

- [ ] Structured PDF: text extracted directly without vision model call
- [ ] JPEG image: text extracted via PaddleOCR, no vision model call
- [ ] PNG image: same as JPEG
- [ ] Scanned PDF: falls back to image conversion + PaddleOCR
- [ ] Multi-page PDF: text from all pages extracted
- [ ] Empty extraction: returns appropriate error
- [ ] Unit tests: all pass without external services
- [ ] Health endpoint: still returns `ollama_reachable` (for text model)
- [ ] No `vision_model` config references remain
- [ ] Classification step unchanged
