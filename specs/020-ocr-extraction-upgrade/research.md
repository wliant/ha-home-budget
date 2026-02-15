# Research: OCR Extraction Upgrade

**Feature**: 020-ocr-extraction-upgrade
**Date**: 2026-02-16

## Research Topics

### R1: PDF Structured Text Extraction with PyMuPDF

**Decision**: Use `page.get_text()` from PyMuPDF (already a dependency) to extract embedded text from structured PDFs.

**Rationale**: PyMuPDF (`pymupdf>=1.25.0`) is already in the project dependencies, currently used only for PDF-to-image conversion. It provides `page.get_text("text")` which returns plain UTF-8 text from pages with embedded text content. This is sub-millisecond per page — orders of magnitude faster than rasterizing to an image and sending to a vision LLM.

For multi-page PDFs, iterate all pages with `for page in doc:` and concatenate text.

**Detection heuristic for structured vs scanned PDFs**:
1. Call `page.get_text().strip()` — if empty, page has no embedded text → image-only
2. Check full-page image coverage via `page.get_images()` + `page.get_image_bbox()` — if a single image covers ≥95% of the page area, it's a scanned page even if OCR text overlay exists
3. Use a minimum text length threshold (e.g., 50 chars) to filter out noise

**Alternatives considered**:
- pdfplumber: Adds a new dependency for the same functionality PyMuPDF already provides. Rejected.
- pdfminer.six: More complex API, slower for this use case. Rejected.
- Tabula: Focused on table extraction only, not general text. Rejected.

### R2: PaddleOCR for Image-Based Text Extraction

**Decision**: Use PaddleOCR (PP-OCRv5) with PaddlePaddle CPU as the replacement for LLaVA vision model for image-based OCR.

**Rationale**: PaddleOCR is a mature, dedicated OCR engine supporting 100+ languages. It runs entirely locally on CPU without requiring a GPU-heavy vision LLM. Typical processing time is 1-2 seconds per receipt image on CPU, comparable to the LLaVA approach but with lower resource requirements.

**Installation**:
- `paddlepaddle>=3.0.0,<4.0.0` (CPU-only deep learning framework)
- `paddleocr>=3.0.0,<4.0.0` (OCR library)
- `opencv-python-headless>=4.6.0` (must install before paddleocr to avoid pulling full opencv with X11 deps)

**API pattern** (PaddleOCR 3.x):
```python
from paddleocr import PaddleOCR
import numpy as np
from PIL import Image
import io

# Initialize once at module level
ocr_engine = PaddleOCR(
    use_doc_orientation_classify=False,
    use_doc_unwarping=False,
    use_textline_orientation=False,
    lang="en",
    ocr_version="PP-OCRv5",
    device="cpu",
)

# Process image bytes
pil_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
image_array = np.array(pil_image)
results = ocr_engine.predict(image_array)

# Extract text lines
for res in results:
    data = res.json
    texts = data["rec_texts"]       # list of recognized text strings
    scores = data["rec_scores"]     # confidence scores
```

**Input formats**: File paths, numpy arrays, URLs, PDF files, or lists of mixed inputs.

**Docker requirements**: Needs `libgl1`, `libglib2.0-0`, `libsm6`, `libxext6`, `libxrender-dev`, `libgomp1` system packages. Image size increases ~1.5-3 GB due to PaddlePaddle.

**Model caching**: Models auto-download to `~/.paddlex/official_models/` on first run. Can be pre-downloaded during Docker build.

**Alternatives considered**:
- Tesseract (pytesseract): Mature but lower accuracy on receipts, requires system-level Tesseract installation. Rejected.
- EasyOCR: Good accuracy but heavier than PaddleOCR, less actively maintained. Rejected.
- Google Cloud Vision / AWS Textract: External cloud services, adds cost and network dependency. Rejected — system must run locally.

### R3: Extract Node Restructure Strategy

**Decision**: Replace the vision LLM call with a two-phase approach: (1) get raw text via PyMuPDF or PaddleOCR, (2) parse raw text into structured line_items using the text LLM (llama3.1).

**Rationale**: The current extract node sends an image to LLaVA which returns structured JSON with `line_items`, `date`, and `is_receipt`. The new OCR/PDF extraction methods produce raw text only. To maintain the same output format (FR-006: extract must produce line_items consumed by classify), the raw text needs to be parsed into structured data.

Using the text LLM (llama3.1, already available) to parse raw OCR text into structured JSON is the most robust approach. It handles varied receipt formats, messy OCR output, and ambiguous line item boundaries better than regex/heuristic parsing. This satisfies FR-004 (no vision LLM called) while keeping FR-005 (classify node unchanged) and FR-006 (same output format).

**New extract node flow**:
1. **PDF with embedded text**: `fitz.open()` → `page.get_text()` → check if structured (non-empty text, no full-page image) → use text directly
2. **PDF without embedded text (scanned)**: `fitz.open()` → `page.get_pixmap()` → PaddleOCR on image
3. **Image files (JPEG/PNG)**: PaddleOCR directly on image bytes
4. **Empty text check**: If raw text is empty/whitespace after extraction → raise NonRetryableError
5. **Parse with text LLM**: Send raw text to llama3.1 with a receipt parsing prompt → get structured JSON (same format as current LLaVA output)
6. **Return**: Same `{image_bytes, extracted_text, line_items, receipt_date}` format

**Alternatives considered**:
- Regex-based parsing of OCR text: Too fragile for varied receipt formats. Rejected.
- Adding a new "parse" node between extract and classify: Adds complexity to the pipeline graph. The parsing is logically part of extraction. Rejected.
- Having classify handle raw text directly: Changes the interface contract between nodes, violates FR-006. Rejected.

### R4: Configuration Changes

**Decision**: Remove `vision_model` setting from config. Keep `text_model` for both extract (parsing) and classify nodes.

**Rationale**: With LLaVA removed, the `vision_model` setting has no consumers. The text model is now used in two places: extract node (raw text → structured JSON) and classify node (line items → categorized expenses). A single `text_model` setting serves both.

The health check endpoint should still verify Ollama reachability (text model is still required) but no longer needs to check for vision model availability.

### R5: Test Strategy

**Decision**: Create dedicated unit tests for each node (validate, extract, classify, format) using mocks for all external dependencies.

**Rationale**: The current test suite (`test_api.py`) has 5 integration tests, some requiring a running Ollama server. The spec requires node-level unit tests that run without external services (FR-007, FR-008).

**Test structure**:
- `tests/test_validate_node.py`: Test valid/invalid file types, file size limits, empty categories
- `tests/test_extract_node.py`: Test PDF text extraction path (mock fitz), image OCR path (mock PaddleOCR), text LLM parsing (mock ChatOllama), empty text error
- `tests/test_classify_node.py`: Test classification with mock LLM, category validation, JSON parsing
- `tests/test_format_node.py`: Test amount formatting (Decimal), date parsing, missing fields

All external dependencies (PaddleOCR, ChatOllama, fitz) mocked via `unittest.mock.patch`.
