# Feature Specification: OCR Extraction Upgrade

**Feature Branch**: `020-ocr-extraction-upgrade`
**Created**: 2026-02-16
**Status**: Draft
**Input**: Replace the vision LLM (LLaVA) used for receipt text extraction with two non-LLM extraction methods: PDF structural conversion and PaddleOCR. Remove LLaVA dependency. Update tests to ensure every processing node is independently tested.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Extract Text from Structured PDFs (Priority: P1)

A household member uploads a PDF receipt (such as an e-receipt from an online store). The system detects that the PDF contains embedded text or structural elements and converts it directly to a text representation without using a vision model. The extracted text is then parsed for expense information. This is faster and more reliable than sending the PDF through an image-based OCR pipeline.

**Why this priority**: Structured PDFs (digital receipts, email-attached invoices) are a common receipt format in household use. Direct text extraction avoids unnecessary image conversion and LLM calls, improving speed and accuracy.

**Independent Test**: Upload a structured PDF receipt. The system extracts text, identifies line items and amounts, and returns expense data — all without calling a vision model.

**Acceptance Scenarios**:

1. **Given** a user uploads a PDF receipt with embedded text, **When** the system processes it, **Then** the text is extracted directly from the PDF structure and expense data is returned.
2. **Given** a user uploads a PDF that contains only scanned images (no embedded text), **When** the system processes it, **Then** the system falls back to image-based OCR extraction.
3. **Given** a user uploads a multi-page PDF, **When** the system processes it, **Then** text from all pages is extracted (not just the first page).

---

### User Story 2 — Extract Text from Images Using OCR Engine (Priority: P1)

A household member uploads a photo of a paper receipt (JPEG/PNG) or a scanned PDF. The system uses an OCR engine to read the text from the image, replacing the previous vision LLM approach. The OCR engine runs locally without requiring a large language model for text recognition.

**Why this priority**: Image receipts (camera photos, scanned documents) are the other primary input type. Replacing the vision LLM with a dedicated OCR engine reduces resource usage, eliminates dependency on a GPU-heavy model, and improves processing speed.

**Independent Test**: Upload a JPEG photo of a receipt. The system extracts text via OCR, identifies line items and amounts, and returns expense data — without calling a vision model.

**Acceptance Scenarios**:

1. **Given** a user uploads a JPEG photo of a receipt, **When** the system processes it, **Then** text is extracted via the OCR engine and expense data is returned.
2. **Given** a user uploads a PNG image of a receipt, **When** the system processes it, **Then** text is extracted via the OCR engine and expense data is returned.
3. **Given** a user uploads a scanned PDF (image-only, no embedded text), **When** the system processes it, **Then** the PDF pages are converted to images and processed through the OCR engine.
4. **Given** a receipt image with poor lighting or slight rotation, **When** the OCR engine processes it, **Then** readable text is still extracted (degraded accuracy is acceptable for poor quality inputs).

---

### User Story 3 — Remove Vision LLM Dependency (Priority: P1)

The system no longer requires a vision language model (such as LLaVA) to extract text from receipt images. The vision model configuration and all calls to it for text extraction are removed. The text-based LLM for expense classification (categorizing line items) remains unchanged.

**Why this priority**: Removing the vision LLM dependency reduces infrastructure requirements (no GPU needed for OCR), speeds up processing, and simplifies the deployment footprint.

**Independent Test**: Process a receipt end-to-end. Verify that no vision model is called during the extraction step. The classification step may still use a text LLM.

**Acceptance Scenarios**:

1. **Given** the system configuration, **When** the extraction step runs, **Then** no call is made to a vision language model.
2. **Given** the system previously required a vision model, **When** the system starts up, **Then** it no longer checks for or depends on a vision model being available.
3. **Given** a receipt is processed, **When** the extraction step completes, **Then** the extracted text is passed to the classification step in the same format as before.

---

### User Story 4 — Comprehensive Node-Level Tests (Priority: P1)

Each processing node (validate, extract, classify, format) has dedicated unit tests that verify its behavior independently. Tests use mocking to isolate each node from external dependencies. This ensures reliable test execution without needing external services.

**Why this priority**: The user explicitly requested tests for each node. Node-level tests ensure correctness of individual components and enable confident refactoring.

**Independent Test**: Run the test suite. All node-level tests pass without requiring external services (Ollama, etc.).

**Acceptance Scenarios**:

1. **Given** the test suite, **When** tests are run, **Then** each of the four processing nodes (validate, extract, classify, format) has at least one dedicated test.
2. **Given** the extract node tests, **When** they run, **Then** they cover both PDF text extraction and image OCR extraction paths.
3. **Given** the classify node tests, **When** they run, **Then** they mock the LLM call and verify category assignment logic.
4. **Given** the format node tests, **When** they run, **Then** they verify amount formatting and date parsing.
5. **Given** the validate node tests, **When** they run, **Then** they cover valid inputs, invalid file types, oversized files, and empty categories.
6. **Given** no external services are running, **When** the full test suite executes, **Then** all unit tests pass (integration tests may be skipped).

---

### Edge Cases

- What happens when a PDF contains a mix of embedded text and scanned images? The system should extract embedded text first, then fall back to OCR for image-only pages.
- What happens when the OCR engine returns empty text? The system should raise an appropriate error indicating no text could be extracted.
- What happens when the extracted text has no recognizable receipt structure (no amounts, no items)? The existing classification step should handle this — if no expenses can be identified, the system returns an appropriate error.
- What happens with non-English receipts? OCR should support common scripts; accuracy may vary by language. No explicit multilingual requirement for this iteration.
- What happens with very large PDFs (many pages)? The existing file size limit (configurable, default 10MB) remains in effect.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST extract text from structured PDFs by reading embedded text content directly (without converting to images first).
- **FR-002**: The system MUST detect whether a PDF contains extractable text or is image-only, and choose the appropriate extraction method.
- **FR-003**: The system MUST extract text from receipt images (JPEG, PNG) and image-only PDFs using a dedicated OCR engine.
- **FR-004**: The system MUST NOT call a vision language model (e.g., LLaVA) during the text extraction step.
- **FR-005**: The text-based LLM used for expense classification MUST remain unchanged.
- **FR-006**: The extraction step MUST produce output in the same format consumed by the classification step (list of line items with descriptions and amounts).
- **FR-007**: Each processing node (validate, extract, classify, format) MUST have dedicated unit tests.
- **FR-008**: Unit tests MUST NOT require external services to run; they MUST mock all external dependencies.
- **FR-009**: The extract node tests MUST cover both the PDF text extraction path and the image OCR path.
- **FR-010**: The system MUST continue to support all currently supported file types (JPEG, PNG, PDF).
- **FR-011**: The system MUST raise an appropriate error when text extraction produces no usable content.

### Key Entities

- **Receipt File**: The uploaded file (JPEG, PNG, or PDF) containing receipt information. Now processed via direct text extraction or OCR rather than a vision model.
- **Extracted Text**: The raw text content obtained from the receipt, either from PDF structural extraction or OCR. Replaces the vision model's structured JSON output.
- **Line Item**: An individual expense entry with description and amount, parsed from the extracted text.
- **Processing Node**: A discrete step in the receipt processing pipeline (validate, extract, classify, format), each independently testable.

## Assumptions

- The OCR engine runs locally within the same container as the receipt processor — no additional external service is needed for OCR.
- PDF text extraction uses the existing PDF processing library already in the project for reading embedded text.
- The extracted raw text from OCR or PDF conversion will need to be parsed into structured data (line items). The text LLM used for classification can also handle this parsing, or a dedicated parsing step can be added.
- The vision model configuration can be fully removed from the system settings; only the text model configuration remains.
- The system health check no longer needs to verify vision model availability (but should still check text model availability if applicable).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Receipt processing completes without calling any vision language model.
- **SC-002**: Structured PDFs with embedded text are processed at least 3x faster than the previous vision model approach.
- **SC-003**: Each of the 4 processing nodes has at least 2 unit tests covering primary paths.
- **SC-004**: The full unit test suite runs in under 10 seconds without external dependencies.
- **SC-005**: Image receipts (JPEG/PNG) are processed with OCR and return expense data with accuracy comparable to the previous approach.
- **SC-006**: The system's external service dependencies are reduced by removing the vision model requirement.
