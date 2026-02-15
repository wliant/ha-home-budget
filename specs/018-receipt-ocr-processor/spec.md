# Feature Specification: Receipt OCR Processor

**Feature Branch**: `018-receipt-ocr-processor`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "Create a new folder ocr-processor. Python project using LangGraph/LangChain that communicates with an Ollama server to process receipt images/PDFs. Exposes an API that accepts a photo or PDF and a list of categories, returns expense objects in JSON. Uses llava:13b and llama3.1:latest models. Includes agent logging, uv for dependencies, Dockerfile, dev/prod build integration, proper error codes, and pytest tests."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Process a Receipt Photo into Expense Data (Priority: P1)

A household member uploads a photo of a receipt along with their list of spending categories. The system analyzes the receipt image using vision AI, extracts expense details (amount, description, date), matches them to the most appropriate category from the provided list, and returns structured expense data as JSON.

**Why this priority**: This is the core value proposition — converting a receipt image into structured expense data. Without this, the service has no purpose.

**Independent Test**: Can be fully tested by sending a receipt image and category list to the API endpoint and verifying the returned JSON contains correctly extracted expense fields.

**Acceptance Scenarios**:

1. **Given** the OCR service is running and the AI model server is reachable, **When** a user uploads a clear receipt photo and a list of categories, **Then** the system returns a JSON array containing one or more expense objects with amount, description, date, and matched category.
2. **Given** a receipt with a single transaction (e.g., grocery store), **When** the receipt is processed with categories ["Groceries", "Dining", "Transport"], **Then** the system returns exactly one expense object matched to "Groceries".
3. **Given** a receipt with line items spanning multiple categories (e.g., a supermarket receipt with groceries and household items), **When** processed with categories ["Groceries", "Household", "Personal Care"], **Then** the system returns multiple expense objects, each matched to the appropriate category.

---

### User Story 2 - Process a PDF Receipt (Priority: P1)

A user uploads a PDF receipt (e.g., an emailed invoice or digital receipt). The system extracts image content from the PDF and processes it the same way as a photo upload.

**Why this priority**: PDF receipts are equally common as photos (email receipts, online purchases). Supporting both formats from day one is essential for practical use.

**Independent Test**: Can be tested by uploading a PDF receipt and verifying the same structured expense JSON is returned.

**Acceptance Scenarios**:

1. **Given** a valid PDF file containing a receipt, **When** uploaded to the API with a category list, **Then** the system returns structured expense data identical in format to photo-based processing.
2. **Given** a multi-page PDF where only the first page contains a receipt, **When** processed, **Then** the system extracts expense data from the first page.

---

### User Story 3 - Receive Clear Error Feedback (Priority: P2)

When something goes wrong — whether it's a temporary server issue or a fundamentally invalid input — the user receives a clear, actionable error response that tells them whether to retry or fix their input.

**Why this priority**: Proper error handling is critical for integration with the frontend. The calling system needs to know whether to retry automatically or present an error message to the user.

**Independent Test**: Can be tested by sending various invalid inputs (non-receipt images, corrupted files, unsupported formats) and verifying appropriate error codes and messages.

**Acceptance Scenarios**:

1. **Given** the AI model server is temporarily unreachable, **When** a receipt is uploaded, **Then** the system returns a retryable error with a message indicating temporary unavailability.
2. **Given** an uploaded file that is clearly not a receipt (e.g., a photo of a landscape), **When** processed, **Then** the system returns a non-retryable error indicating the file does not appear to be a receipt.
3. **Given** an uploaded file in an unsupported format (e.g., .docx), **When** submitted, **Then** the system returns a non-retryable error specifying the accepted file formats.
4. **Given** a valid receipt but the category list is empty, **When** submitted, **Then** the system returns a non-retryable error indicating at least one category must be provided.

---

### User Story 4 - View Processing Logs (Priority: P3)

A developer or system administrator can view detailed agent processing logs to understand how the AI analyzed a receipt, what decisions it made, and where failures occurred. This aids debugging and quality improvement.

**Why this priority**: Logging is essential for debugging and improving accuracy over time, but the system functions without it being user-facing.

**Independent Test**: Can be tested by submitting a receipt and checking that structured log output captures each processing step (image analysis, text extraction, category matching).

**Acceptance Scenarios**:

1. **Given** a receipt is being processed, **When** the agent executes each step, **Then** structured logs capture: input received, vision model analysis, extracted text/data, category matching decisions, and final output.
2. **Given** processing fails at any step, **When** viewing logs, **Then** the exact failure point and error details are clearly identifiable.

---

### User Story 5 - Containerized Deployment (Priority: P2)

The OCR processor runs as a containerized service alongside the existing budget application in both development and production environments.

**Why this priority**: The service must be deployable as part of the existing application stack to be useful. Docker integration enables consistent environments and easy deployment.

**Independent Test**: Can be tested by building the container image, running it, and verifying the API is accessible and functional.

**Acceptance Scenarios**:

1. **Given** the project Docker configuration, **When** the development environment starts, **Then** the OCR processor service starts alongside the existing budget backend and frontend services.
2. **Given** a production deployment, **When** the full stack is deployed, **Then** the OCR processor is available and can communicate with the configured AI model server.

---

### Edge Cases

- What happens when the receipt image is blurry or partially cut off? The system should attempt processing and return whatever data it can extract, with reduced confidence indicated in the response.
- What happens when the receipt is in a non-English language? The system should attempt to process it; the AI model may extract amounts and dates regardless of language.
- What happens when the receipt contains no discernible amounts? The system returns a non-retryable error indicating it could not extract expense data from the image.
- What happens when the uploaded file exceeds a reasonable size limit? The system returns a non-retryable error specifying the maximum allowed file size.
- What happens when multiple receipts are in a single image? The system processes all visible receipts and returns expense objects for each.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept image files (JPEG, PNG) and PDF files as input via an API endpoint.
- **FR-002**: System MUST accept a list of categories (each with an ID and name) alongside the receipt file.
- **FR-003**: System MUST use a vision AI model to analyze the receipt image and extract: total amount, item description(s), and date of purchase.
- **FR-004**: System MUST match extracted expenses to the most appropriate category from the provided category list.
- **FR-005**: System MUST return a JSON array of expense objects, each containing: amount, description, expense date, and matched category (ID and name).
- **FR-006**: System MUST return a single expense object when the receipt represents one transaction category.
- **FR-007**: System MUST return multiple expense objects when receipt line items belong to different categories from the provided list.
- **FR-008**: System MUST return a non-retryable error for invalid inputs (unsupported file format, missing required fields, file too large).
- **FR-009**: System MUST return a non-retryable error for data quality issues where retrying will not help (file is not a receipt, no expense data extractable).
- **FR-010**: System MUST return a retryable error for temporary failures (AI model server unreachable, processing timeout) where the client should retry.
- **FR-011**: All error responses MUST include a structured body with an error code, a human-readable message, and a boolean `retryable` flag.
- **FR-012**: System MUST produce structured logs for each processing step: input validation, image analysis, data extraction, category matching, and response generation.
- **FR-013**: System MUST be configurable for the AI model server address (host and port).
- **FR-014**: System MUST be packaged as a container image for deployment.
- **FR-015**: System MUST integrate into the existing development and production container orchestration.
- **FR-016**: System MUST have at least one automated test that submits a real receipt image and validates the response structure.

### Key Entities

- **Receipt Input**: The uploaded file (image or PDF) plus a list of categories (each with an ID and name) provided by the caller.
- **Expense Object**: The output entity containing: amount (decimal), description (text), expense date (date), and matched category (ID and name from the input list).
- **Error Response**: Standardized error entity containing: error code (string), message (text), and retryable flag (boolean).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clear receipt photo is processed and returns valid expense JSON within 60 seconds.
- **SC-002**: PDF receipts produce the same quality of output as photo uploads.
- **SC-003**: At least 80% of clear, standard-format receipts are correctly parsed (amount, date, and category match).
- **SC-004**: Non-receipt images are correctly identified and rejected with a non-retryable error at least 90% of the time.
- **SC-005**: Temporary server failures return retryable errors with zero false positives (valid data issues are never marked as retryable).
- **SC-006**: All processing steps are visible in structured logs for any given request.
- **SC-007**: The containerized service starts and responds to health checks within 30 seconds of container startup.
- **SC-008**: At least one automated test passes, verifying end-to-end receipt processing.

## Assumptions

- The AI model server (Ollama) is pre-configured and running separately; this service does not manage the model server lifecycle.
- The vision model handles multi-language receipts to the extent of its training, with English receipts having the highest accuracy.
- File size limit is assumed to be 10MB per upload, which covers virtually all receipt photos and PDFs.
- The only caller of this service is the Spring Boot backend. The backend calls the OCR service as part of processing an `ExpenseInputJob` entity. The OCR service does not need its own authentication — it trusts the backend caller.
- The expense date defaults to "today" if the AI cannot extract a date from the receipt.
- Currency is assumed to be the same as the household budget currency; no currency conversion is performed.

## Clarifications

### Session 2026-02-15

- Q: Who calls the OCR processor API? → A: Spring Boot backend only, as part of processing an `ExpenseInputJob` entity.
- Q: Should the OCR service return category name only or ID+name? → A: Category ID + name. Input receives id+name pairs, response returns matched ID+name.

## Scope Boundaries

**In Scope**:
- Single receipt processing (one receipt per API call)
- Image (JPEG, PNG) and PDF input formats
- Category matching from a provided list
- Error classification (retryable vs non-retryable)
- Container deployment integration
- Agent processing logs

**Out of Scope**:
- Batch processing of multiple receipts in one call
- Receipt storage or archival
- Training or fine-tuning AI models
- Currency detection or conversion
- Direct user-facing UI (this is a backend API service)
- Managing or deploying the Ollama server itself
