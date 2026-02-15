# Tasks: OCR Extraction Upgrade

**Input**: Design documents from `/specs/020-ocr-extraction-upgrade/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Tests are explicitly requested (User Story 4, FR-007 through FR-009). TDD approach: test tasks are separate for clarity but all external deps are mocked.

**Organization**: Tasks grouped by user story. All 4 stories are P1 and form an inseparable change, so they execute sequentially. US1-US3 share `extract.py` (same file, sequential). US4 test files are independent and parallelizable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- All paths relative to `ocr-processor/`

---

## Phase 1: Setup

**Purpose**: Add new dependencies for PaddleOCR and image processing

- [x] T001 Add paddlepaddle, paddleocr, opencv-python-headless, and numpy to ocr-processor/pyproject.toml dependencies

---

## Phase 2: Foundational (Config & Health Check)

**Purpose**: Remove vision model configuration and update health check — MUST complete before extract node rewrite

- [x] T002 Remove vision_model field from Settings class in ocr-processor/src/ocr_processor/config.py
- [x] T003 Update health check in ocr-processor/src/ocr_processor/main.py to remove any vision-model-specific logic (keep ollama_reachable check for text model)

**Checkpoint**: Configuration no longer references vision model

---

## Phase 3: User Story 1 — Extract Text from Structured PDFs (Priority: P1)

**Goal**: Detect PDFs with embedded text and extract directly via PyMuPDF without image conversion or OCR

**Independent Test**: Upload a structured PDF → text extracted directly from PDF structure → no vision model called

- [x] T004 [US1] Add PDF text detection helpers to ocr-processor/src/ocr_processor/nodes/extract.py: `_has_extractable_text(page)` checks if a page has real embedded text (non-empty get_text() AND no full-page image covering ≥95% of page area), and `_extract_pdf_text(pdf_bytes)` iterates all pages, detects structured vs scanned per-page, returns concatenated text for structured pages and list of page images for scanned pages
- [x] T005 [US1] Add text LLM parsing function `_parse_text_with_llm(raw_text)` to ocr-processor/src/ocr_processor/nodes/extract.py: create PARSE_PROMPT (similar to current EXTRACT_PROMPT but text-only input), call ChatOllama with settings.text_model, parse JSON response into line_items/receipt_date/is_receipt, handle errors (timeout → RetryableError, connection → RetryableError, bad JSON → NonRetryableError)

**Checkpoint**: PDF text extraction helpers and text LLM parsing ready for integration

---

## Phase 4: User Story 2 — Extract Text from Images Using OCR Engine (Priority: P1)

**Goal**: Use PaddleOCR to extract text from JPEG/PNG images and scanned PDF pages

**Independent Test**: Upload a JPEG photo of a receipt → text extracted via PaddleOCR → no vision model called

- [x] T006 [US2] Add PaddleOCR engine initialization and `_extract_text_with_ocr(image_bytes)` helper to ocr-processor/src/ocr_processor/nodes/extract.py: initialize PaddleOCR once at module level (lang="en", ocr_version="PP-OCRv5", device="cpu", disable doc orientation/unwarping), convert image bytes to numpy array via PIL, call ocr_engine.predict(), concatenate rec_texts into raw text string, return extracted text
- [x] T007 [US2] Add `_convert_pdf_pages_to_images(pdf_bytes)` helper to ocr-processor/src/ocr_processor/nodes/extract.py: convert each scanned PDF page to PNG bytes via fitz get_pixmap(dpi=200), return list of image byte arrays for PaddleOCR processing

**Checkpoint**: OCR extraction and PDF-to-image conversion ready for integration

---

## Phase 5: User Story 3 — Remove Vision LLM Dependency (Priority: P1)

**Goal**: Rewrite extract_node() to orchestrate new extraction methods, remove all LLaVA/vision model code

**Independent Test**: Process a receipt end-to-end — no vision model called during extraction, classification still uses text LLM

- [x] T008 [US3] Rewrite `extract_node()` function in ocr-processor/src/ocr_processor/nodes/extract.py: remove LLaVA ChatOllama call and EXTRACT_PROMPT, remove base64 encoding logic, implement new flow (1. if PDF → call _extract_pdf_text to get structured text and scanned page images, 2. if image → call _extract_text_with_ocr, 3. for scanned PDF pages → convert to images then _extract_text_with_ocr, 4. if raw text empty → raise NonRetryableError NO_EXPENSE_DATA, 5. call _parse_text_with_llm to get structured line_items, 6. return same output format: image_bytes, extracted_text, line_items, receipt_date), remove unused imports (base64, ChatOllama from extract-level usage for vision)

**Checkpoint**: Extract node fully rewritten, no vision model dependency, same output interface

---

## Phase 6: User Story 4 — Comprehensive Node-Level Tests (Priority: P1)

**Goal**: Every processing node has dedicated unit tests with all external dependencies mocked

**Independent Test**: Run `pytest tests/` — all tests pass without Ollama, PaddleOCR, or any external service

- [x] T009 [US4] Update ocr-processor/tests/conftest.py with shared node-level test fixtures: sample state dicts (valid JPEG state, valid PDF state, valid line_items, sample categories), mock LLM response fixtures (valid JSON extraction response, valid classification response), sample OCR text output
- [x] T010 [P] [US4] Create ocr-processor/tests/test_validate_node.py with tests: valid JPEG passes, valid PNG passes, valid PDF passes, unsupported format raises NonRetryableError UNSUPPORTED_FORMAT, file exceeding max size raises NonRetryableError FILE_TOO_LARGE, empty categories list raises NonRetryableError EMPTY_CATEGORIES
- [x] T011 [P] [US4] Create ocr-processor/tests/test_extract_node.py with tests: structured PDF extracts text via PyMuPDF (mock fitz.open), image extracts text via PaddleOCR (mock PaddleOCR.predict), scanned PDF falls back to image conversion then OCR (mock both fitz and PaddleOCR), text LLM parsing produces correct line_items (mock ChatOllama), empty extraction text raises NonRetryableError, LLM timeout raises RetryableError, LLM connection error raises RetryableError
- [x] T012 [P] [US4] Create ocr-processor/tests/test_classify_node.py with tests: valid classification returns categorized expenses (mock ChatOllama), invalid category ID falls back to first category, empty classification result raises NonRetryableError, LLM timeout raises RetryableError, malformed JSON raises NonRetryableError
- [x] T013 [P] [US4] Create ocr-processor/tests/test_format_node.py with tests: amounts formatted to 2 decimal places (Decimal quantize), date parsed from ISO format string, missing receipt_date defaults to today, category_id and category_name preserved from input, multiple items formatted correctly
- [x] T014 [US4] Update ocr-processor/tests/test_api.py to remove references to LLaVA/vision model in test docstrings and assertions, update test_process_receipt_returns_valid_structure to not check for vision model, ensure integration tests still validate response structure

**Checkpoint**: All 4 processing nodes have ≥2 unit tests each, full suite runs without external services

---

## Phase 7: Polish & Validation

**Purpose**: Run tests, validate against quickstart scenarios

- [x] T015 Install new dependencies with `cd ocr-processor && uv sync` and run full test suite with `uv run pytest tests/ -v` to verify all tests pass without external services
- [x] T016 Verify no remaining references to vision_model or LLaVA in ocr-processor/src/ (grep for "llava", "vision_model", "vision") and run quickstart.md scenario validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001 must complete first)
- **US1 (Phase 3)**: Depends on Phase 2 — adds helpers to extract.py
- **US2 (Phase 4)**: Depends on Phase 3 — adds more helpers to extract.py (same file, sequential)
- **US3 (Phase 5)**: Depends on Phase 4 — rewrites extract_node() using all helpers
- **US4 (Phase 6)**: Depends on Phase 5 — tests the completed implementation
- **Polish (Phase 7)**: Depends on Phase 6 — validates everything

### User Story Dependencies

- **US1 (Structured PDF)**: Foundation for US2 and US3 (provides _extract_pdf_text and _parse_text_with_llm helpers)
- **US2 (Image OCR)**: Adds PaddleOCR path, depends on US1's parsing function
- **US3 (Remove Vision LLM)**: Integrates US1 + US2 helpers into extract_node(), removes LLaVA
- **US4 (Tests)**: Tests the final implementation from US1-US3

### Within-Phase Sequential Requirements

- T004 → T005 (both modify extract.py, text LLM parsing builds on PDF extraction context)
- T006 → T007 (both modify extract.py, PDF-to-image depends on OCR helper)
- T008 depends on T004-T007 (uses all helpers)
- T009 must complete before T010-T014 (shared fixtures)
- T010-T013 are [P] (independent test files)
- T014 runs after T010-T013 (update existing tests last)

### Parallel Opportunities

**Phase 6 — Test files are parallelizable:**
```
T010 [P]: test_validate_node.py   ─┐
T011 [P]: test_extract_node.py    ─┤ All can run in parallel
T012 [P]: test_classify_node.py   ─┤ (after T009 conftest.py)
T013 [P]: test_format_node.py     ─┘
```

---

## Implementation Strategy

### MVP First (US1 + US2 + US3)

1. Complete Phase 1: Add dependencies
2. Complete Phase 2: Config cleanup
3. Complete Phases 3-5: Rewrite extract node (US1 → US2 → US3)
4. **STOP and VALIDATE**: Test manually with a real PDF and image receipt
5. Complete Phase 6: Add unit tests (US4)
6. Complete Phase 7: Polish and final validation

### Incremental Delivery

Since all stories are P1 and inseparable, the delivery is:
1. Setup + Config → ready for development
2. US1 + US2 + US3 → core extraction rewrite complete
3. US4 → test coverage complete
4. Polish → production ready

---

## Notes

- All 4 user stories are P1 and form an inseparable change per the spec
- US1-US3 all modify `extract.py` → strictly sequential within those phases
- US4 test files are independent → parallelizable (except conftest.py first)
- The text LLM (llama3.1) is used in the new extract node for parsing raw text — this is NOT a vision model call (satisfies FR-004)
- PaddleOCR auto-downloads models on first run (~200MB) — first test run may be slow
