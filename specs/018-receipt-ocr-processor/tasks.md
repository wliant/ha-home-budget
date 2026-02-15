# Tasks: Receipt OCR Processor

**Input**: Design documents from `/specs/018-receipt-ocr-processor/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ocr-processor-api.yaml, quickstart.md

**Tests**: The spec requests at least 1 pytest test (FR-016). Test tasks are included for the OCR processor only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Create the OCR processor Python project structure and configure dependencies

- [x] T001 Create ocr-processor project directory with src/ocr_processor/ and tests/ structure per plan.md at ocr-processor/
- [x] T002 Create pyproject.toml with uv project config, dependencies (fastapi, uvicorn, langchain, langgraph, langchain-ollama, pillow, pymupdf, httpx, structlog, python-multipart), and dev dependencies (pytest, httpx) at ocr-processor/pyproject.toml
- [x] T003 Create .python-version pinning Python 3.11 at ocr-processor/.python-version
- [x] T004 Run uv sync to install dependencies and generate lock file at ocr-processor/
- [x] T005 [P] Create __init__.py files for ocr_processor package and nodes subpackage at ocr-processor/src/ocr_processor/__init__.py and ocr-processor/src/ocr_processor/nodes/__init__.py

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Implement Settings config class with Pydantic BaseSettings (OLLAMA_HOST default 192.168.1.248:11434, VISION_MODEL default llava:13b, TEXT_MODEL default llama3.1:latest, MAX_FILE_SIZE_MB default 10, LOG_LEVEL default INFO) at ocr-processor/src/ocr_processor/config.py
- [x] T007 [P] Implement Pydantic request/response models (CategoryInput, ExpenseOutput, ProcessResponse, ErrorResponse) per data-model.md at ocr-processor/src/ocr_processor/models.py
- [x] T008 [P] Implement error types: OcrError base, NonRetryableError (HTTP 422), RetryableError (HTTP 503), with error codes from contracts/ocr-processor-api.yaml at ocr-processor/src/ocr_processor/errors.py
- [x] T009 [P] Implement structured JSON logging setup with structlog, configure per-request context binding and agent step logging at ocr-processor/src/ocr_processor/logging.py
- [x] T010 Implement FastAPI app skeleton with health endpoint (GET /health checking Ollama reachability), error handlers for OcrError types, and CORS/middleware config at ocr-processor/src/ocr_processor/main.py
- [x] T011 Implement LangGraph AgentState TypedDict and empty graph skeleton (validate→extract→classify→format nodes with error routing) at ocr-processor/src/ocr_processor/agent.py

**Checkpoint**: Foundation ready - FastAPI app runs, health endpoint works, graph skeleton defined

---

## Phase 3: User Story 1 - Process a Receipt Photo into Expense Data (Priority: P1) 🎯 MVP

**Goal**: Accept a receipt photo + categories, extract expense data using vision AI, return structured JSON

**Independent Test**: POST /process with a JPEG receipt image and category list → returns JSON with expense objects containing amount, description, date, category_id, category_name

### Implementation for User Story 1

- [x] T012 [US1] Implement validate node: check file content type (JPEG/PNG only for US1), file size <= MAX_FILE_SIZE_MB, categories list non-empty; raise NonRetryableError on failure at ocr-processor/src/ocr_processor/nodes/validate.py
- [x] T013 [US1] Implement extract node: send image to llava:13b via ChatOllama multimodal, prompt to extract receipt text/amounts/date/line items, parse structured response; raise RetryableError on connection failure, NonRetryableError if not a receipt at ocr-processor/src/ocr_processor/nodes/extract.py
- [x] T014 [US1] Implement classify node: send extracted items + category list to llama3.1:latest via ChatOllama, prompt to match each item to best category by id+name, return single or multiple expense groupings at ocr-processor/src/ocr_processor/nodes/classify.py
- [x] T015 [US1] Implement format node: build list of ExpenseOutput from classified items, default expense_date to today if not extracted, ensure amounts are 2 decimal places at ocr-processor/src/ocr_processor/nodes/format.py
- [x] T016 [US1] Wire up complete LangGraph StateGraph with all 4 nodes, conditional error edges, and compile graph in agent.py at ocr-processor/src/ocr_processor/agent.py
- [x] T017 [US1] Implement POST /process endpoint: accept multipart file + categories JSON string, parse categories, invoke agent graph, return ProcessResponse or error at ocr-processor/src/ocr_processor/main.py
- [x] T018 [US1] Add a sample receipt image for testing at ocr-processor/tests/fixtures/sample_receipt.jpg
- [x] T019 [US1] Create pytest test that POSTs sample_receipt.jpg with categories to /process and validates response structure (expenses array with amount, description, expense_date, category_id, category_name) at ocr-processor/tests/test_api.py

**Checkpoint**: OCR processor accepts receipt photos and returns structured expense JSON

---

## Phase 4: User Story 2 - Process a PDF Receipt (Priority: P1)

**Goal**: Accept PDF receipts and process them through the same pipeline as photos

**Independent Test**: POST /process with a PDF receipt → returns same structured expense JSON as photo processing

### Implementation for User Story 2

- [x] T020 [US2] Extend validate node to accept application/pdf content type alongside JPEG/PNG at ocr-processor/src/ocr_processor/nodes/validate.py
- [x] T021 [US2] Add PDF-to-image conversion in extract node: detect PDF input, use PyMuPDF (fitz) to render first page to PNG bytes, then process through same vision pipeline at ocr-processor/src/ocr_processor/nodes/extract.py
- [x] T022 [US2] Add a sample PDF receipt for testing at ocr-processor/tests/fixtures/sample_receipt.pdf
- [x] T023 [US2] Add pytest test for PDF processing: POST sample_receipt.pdf to /process and validate response structure at ocr-processor/tests/test_api.py

**Checkpoint**: Both photo and PDF receipts produce structured expense JSON

---

## Phase 5: User Story 3 - Receive Clear Error Feedback (Priority: P2)

**Goal**: Return clear, classified errors (retryable vs non-retryable) for all failure modes

**Independent Test**: Send invalid inputs (non-receipt image, unsupported format, empty categories, large file) and verify appropriate error codes, messages, and retryable flags

### Implementation for User Story 3

- [x] T024 [US3] Add not-a-receipt detection in extract node: analyze vision model response for receipt indicators, raise NonRetryableError with NOT_A_RECEIPT code if image is clearly not a receipt at ocr-processor/src/ocr_processor/nodes/extract.py
- [x] T025 [US3] Add no-extractable-data handling in extract node: if vision model returns no amounts/items, raise NonRetryableError with NO_EXPENSE_DATA code at ocr-processor/src/ocr_processor/nodes/extract.py
- [x] T026 [US3] Add timeout handling in extract and classify nodes: wrap Ollama calls with configurable timeout (default 60s), raise RetryableError with PROCESSING_TIMEOUT code on timeout at ocr-processor/src/ocr_processor/nodes/extract.py and ocr-processor/src/ocr_processor/nodes/classify.py
- [x] T027 [US3] Add pytest tests for error scenarios: unsupported format (422), empty categories (422), and validate error response structure (error_code, message, retryable) at ocr-processor/tests/test_api.py

**Checkpoint**: All error paths return properly classified errors with correct HTTP status codes

---

## Phase 6: User Story 4 - View Processing Logs (Priority: P3)

**Goal**: Structured agent logs capture each processing step for debugging and quality improvement

**Independent Test**: Process a receipt and verify structured log output contains entries for each step (validate, extract, classify, format) with input/output data

### Implementation for User Story 4

- [x] T028 [US4] Add LangChain callback handler that logs node entry/exit with input state summary and output state summary using structlog at ocr-processor/src/ocr_processor/logging.py
- [x] T029 [US4] Attach callback handler to graph invocation in agent.py, add per-request correlation ID to log context at ocr-processor/src/ocr_processor/agent.py
- [x] T030 [US4] Add step-level logging in each node: log input received, processing decisions, output produced, and any warnings at ocr-processor/src/ocr_processor/nodes/validate.py, extract.py, classify.py, format.py

**Checkpoint**: Processing logs show complete audit trail for each receipt processed

---

## Phase 7: User Story 5 - Containerized Deployment (Priority: P2)

**Goal**: OCR processor runs as a containerized service in dev and prod environments

**Independent Test**: Build container, start via docker-compose, verify /health returns 200 within 30 seconds

### Implementation for User Story 5

- [x] T031 [US5] Create development Dockerfile using python:3.11-slim base, install uv, copy project, run uvicorn with reload on port 8082 at ocr-processor/Dockerfile.dev
- [x] T032 [US5] Create production Dockerfile with multi-stage build: builder stage installs deps with uv, runtime stage copies only installed packages, runs uvicorn on port 8082 at ocr-processor/Dockerfile
- [x] T033 [US5] Add ocr-processor service to docker-compose.yml (dev): build from ocr-processor/Dockerfile.dev, mount source for hot reload, expose port 8082, set OLLAMA_HOST env var, join homebudget-network at docker-compose.yml
- [x] T034 [US5] Add ocr-processor service to docker-compose.prod.yml: build from ocr-processor/Dockerfile, container name homebudget-ocr-processor, set OLLAMA_HOST env var, join homebudget-network at docker-compose.prod.yml
- [x] T035 [US5] Add OCR_PROCESSOR_URL and OLLAMA_HOST to .env.example and .env.prod.example at .env.example and .env.prod.example

**Checkpoint**: `docker-compose up ocr-processor` starts and /health returns healthy

---

## Phase 8: Backend Integration

**Purpose**: Connect Spring Boot backend to OCR processor, replace processing stub, change TemporaryExpenseRecord to OneToMany

### Database Migration

- [x] T036 Create Liquibase migration to drop unique constraint on temporary_expense_records.job_id column at budget-backend/src/main/resources/db/changelog/

### Model & DTO Changes

- [x] T037 Change ExpenseInputJob.temporaryRecord OneToOne to temporaryRecords OneToMany (List<TemporaryExpenseRecord>), update getter/setter at budget-backend/src/main/java/com/homebudget/model/ExpenseInputJob.java
- [x] T038 Change TemporaryExpenseRecord.job from @OneToOne to @ManyToOne, remove unique=true from @JoinColumn at budget-backend/src/main/java/com/homebudget/model/TemporaryExpenseRecord.java
- [x] T039 [P] Create OcrExpenseDTO (amount, description, expenseDate, categoryId, categoryName) and OcrResponseDTO (List<OcrExpenseDTO> expenses) at budget-backend/src/main/java/com/homebudget/dto/OcrResponseDTO.java and budget-backend/src/main/java/com/homebudget/dto/OcrExpenseDTO.java
- [x] T040 Update ExpenseInputJobDTO.temporaryRecord to temporaryRecords (List<TemporaryExpenseRecordDTO>) at budget-backend/src/main/java/com/homebudget/dto/ExpenseInputJobDTO.java

### Service Changes

- [x] T041 [P] Create OcrProcessorClient service: POST multipart file + categories JSON to OCR_PROCESSOR_URL/process, parse OcrResponseDTO, handle retryable/non-retryable errors at budget-backend/src/main/java/com/homebudget/service/OcrProcessorClient.java
- [x] T042 [P] Add OCR_PROCESSOR_URL property to application-dev.properties (default http://ocr-processor:8082) and application-prod.properties at budget-backend/src/main/resources/application-dev.properties and budget-backend/src/main/resources/application-prod.properties
- [x] T043 Replace processPendingJobs() stub in ExpenseInputJobService: call OcrProcessorClient with job file + all categories, create multiple TemporaryExpenseRecords from response, handle retryable (keep PROCESSING) vs non-retryable (FAILED) errors at budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [x] T044 Update toDTO() in ExpenseInputJobService to map List<TemporaryExpenseRecord> to List<TemporaryExpenseRecordDTO> at budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [x] T045 Update confirmJobs() in ExpenseInputJobService to handle multiple TemporaryExpenseRecords per job at budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [x] T046 Update deleteJobs() in ExpenseInputJobService to delete all TemporaryExpenseRecords for each job at budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [x] T047 Update updateTemporaryRecord() to accept record ID instead of job ID (since multiple records per job) at budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java and budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java

### Frontend Changes

- [x] T048 Update ExpenseInputJob type and bulk-upload UI components to display and handle multiple temporary records per job at budget-frontend/src/

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Final integration verification

- [x] T049 Create .gitignore for ocr-processor (Python patterns: __pycache__, .venv, *.pyc, .mypy_cache) at ocr-processor/.gitignore
- [x] T050 Create .dockerignore for ocr-processor (.venv, __pycache__, tests, .git) at ocr-processor/.dockerignore
- [x] T051 Run quickstart.md scenarios to validate end-to-end integration

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1
- **Phase 3 (US1 - Photo Processing)**: Depends on Phase 2 — **MVP target**
- **Phase 4 (US2 - PDF Processing)**: Depends on Phase 3 (extends extract node)
- **Phase 5 (US3 - Error Feedback)**: Depends on Phase 3 (enhances existing nodes)
- **Phase 6 (US4 - Logging)**: Depends on Phase 3 (adds to existing nodes)
- **Phase 7 (US5 - Deployment)**: Depends on Phase 2 (needs app to containerize)
- **Phase 8 (Backend Integration)**: Depends on Phase 3 (needs working OCR API)
- **Phase 9 (Polish)**: Depends on all previous phases

### User Story Dependencies

- **US1 (Photo Processing)**: Foundation only — no other story dependencies
- **US2 (PDF Processing)**: Extends US1 extract node
- **US3 (Error Feedback)**: Enhances US1 nodes
- **US4 (Logging)**: Adds to US1 nodes
- **US5 (Deployment)**: Independent of other stories (can start after Phase 2)

### Within Each User Story

- Models/schemas before service logic
- Service logic before endpoint integration
- Core implementation before tests

### Parallel Opportunities

Phase 2:
```
T007 (models) || T008 (errors) || T009 (logging) — all different files
```

Phase 7 + Phase 3:
```
US5 (Docker/deployment) can proceed in parallel with US1 (core processing) after Phase 2
```

Phase 8:
```
T039 (OcrDTOs) || T041 (OcrProcessorClient) || T042 (properties) — different files
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T011)
3. Complete Phase 3: User Story 1 (T012-T019)
4. **STOP and VALIDATE**: Test with real receipt photo against Ollama
5. Can be used standalone via curl/API

### Incremental Delivery

1. Setup + Foundational → App skeleton runs
2. US1 (Photo) → Core receipt processing works → **MVP!**
3. US2 (PDF) → PDF support added
4. US3 (Errors) → Robust error handling
5. US4 (Logging) → Full observability
6. US5 (Deployment) → Docker integration (can be done earlier in parallel)
7. Backend Integration → Full end-to-end with existing app
8. Polish → Production-ready

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US5 (Deployment) can proceed in parallel with US1-US4 since it only needs the app skeleton
- Backend integration (Phase 8) is a separate phase because it modifies existing code and changes the DB schema
- The test in T019 requires a running Ollama server with llava:13b loaded
