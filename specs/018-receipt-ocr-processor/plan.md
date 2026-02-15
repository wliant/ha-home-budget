# Implementation Plan: Receipt OCR Processor

**Branch**: `018-receipt-ocr-processor` | **Date**: 2026-02-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/018-receipt-ocr-processor/spec.md`

## Summary

Build a Python microservice (`ocr-processor/`) that accepts a receipt image or PDF plus a list of categories (id+name), uses LangGraph/LangChain with Ollama vision and language models to extract expense data, and returns structured JSON. The Spring Boot backend calls this service during `ExpenseInputJob` processing, replacing the current random-data stub in `processPendingJobs()`. The current `TemporaryExpenseRecord` one-to-one relationship with `ExpenseInputJob` must be changed to one-to-many to support receipts that produce multiple expenses across different categories.

## Technical Context

**Language/Version**: Python 3.11+ (OCR processor), Java 17 (backend integration)
**Primary Dependencies**: FastAPI, LangGraph, LangChain, langchain-ollama, Pillow, PyMuPDF (fitz), uvicorn, httpx
**Storage**: N/A (stateless service; files passed by backend)
**Testing**: pytest with httpx TestClient
**Target Platform**: Linux container (Docker), private home network
**Project Type**: Microservice (new `ocr-processor/` directory at repo root)
**Performance Goals**: Process a clear receipt within 60 seconds
**Constraints**: Ollama server at configurable address (default 192.168.1.248:11434); models llava:13b (vision) and llama3.1:latest (text reasoning)
**Scale/Scope**: Single household use; low concurrency (1-2 concurrent requests)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Specification-First (I) | PASS | spec.md complete, clarified |
| Clarify Before Planning (II) | PASS | `/speckit.clarify` completed with 2 questions resolved |
| Incremental Delivery (III) | PASS | 5 user stories prioritized P1/P2/P3 |
| Constitution Gates (IV) | PASS | This check |
| Task Traceability (V) | N/A | Checked at task generation |
| Test-Optional (VI) | PASS | Spec requests at least 1 pytest test |
| Artifact Consistency (VII) | N/A | Checked after task generation |
| **Technical Stack** | **VIOLATION** | Python service, not Spring Boot — see Complexity Tracking |
| Authentication | PASS | OCR service is internal-only, called by backend which handles auth |
| Deployment | PASS | Containerized for private network |
| Multi-User | PASS | Backend passes user context; OCR service is stateless |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Python service instead of Spring Boot | OCR processing requires LangGraph/LangChain ecosystem which is Python-native. Vision model integration (llava:13b) via LangChain has mature Python libraries but no equivalent Java support. The user explicitly requested Python with uv. | Adding Ollama HTTP calls directly in Java would bypass LangGraph agent workflow, lose structured agent logging, and require reimplementing prompt chaining logic that LangChain provides out of the box. |

## Project Structure

### Documentation (this feature)

```text
specs/018-receipt-ocr-processor/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
ocr-processor/
├── pyproject.toml           # uv project config with dependencies
├── Dockerfile               # Production container
├── Dockerfile.dev           # Development container with hot reload
├── .python-version          # Pin Python 3.11
├── src/
│   └── ocr_processor/
│       ├── __init__.py
│       ├── main.py          # FastAPI app, health endpoint, /process endpoint
│       ├── config.py        # Settings (Ollama host, models, file size limit)
│       ├── models.py        # Pydantic request/response models
│       ├── agent.py         # LangGraph agent definition (graph, nodes, edges)
│       ├── nodes/
│       │   ├── __init__.py
│       │   ├── validate.py  # Input validation node
│       │   ├── extract.py   # Vision model receipt extraction node
│       │   ├── classify.py  # Category matching node (LLM)
│       │   └── format.py    # Output formatting node
│       ├── errors.py        # Error types (retryable vs non-retryable)
│       └── logging.py       # Structured JSON logging setup
└── tests/
    ├── conftest.py          # Fixtures, test client
    ├── test_api.py          # API endpoint tests
    └── fixtures/
        └── sample_receipt.jpg  # Test receipt image

budget-backend/  (modifications only)
├── src/main/java/com/homebudget/
│   ├── service/
│   │   ├── ExpenseInputJobService.java  # Replace stub with OCR API call
│   │   └── OcrProcessorClient.java      # New: HTTP client for OCR service
│   ├── dto/
│   │   └── OcrResponseDTO.java          # New: OCR API response mapping
│   └── config/
│       └── OcrProcessorConfig.java      # New: OCR service URL config
└── src/main/resources/
    ├── application-dev.properties        # Add OCR_PROCESSOR_URL
    └── application-prod.properties       # Add OCR_PROCESSOR_URL

docker-compose.yml          # Add ocr-processor service
docker-compose.prod.yml     # Add ocr-processor service
```

**Structure Decision**: New `ocr-processor/` directory at repo root, sibling to `budget-backend/` and `budget-frontend/`. This follows the existing project organization pattern where each service has its own top-level directory.

## Architecture Decisions

### 1. LangGraph Agent Pipeline

The OCR processing uses a LangGraph StateGraph with 4 nodes:

```
[validate] → [extract] → [classify] → [format]
                 ↓              ↓
              [error]        [error]
```

- **validate**: Check file type, size, category list non-empty. Fail fast with non-retryable error.
- **extract**: Send image to llava:13b via Ollama. Extract receipt text, amounts, date, line items. On Ollama connection failure → retryable error. On "not a receipt" detection → non-retryable error.
- **classify**: Send extracted items + category list to llama3.1:latest. Match each line item to best category. Single-item receipts → 1 expense. Multi-category receipts → multiple expenses.
- **format**: Build final JSON response with amount, description, date, category id+name.

Each node logs its input/output for agent observability.

### 2. Backend Integration Pattern

The Spring Boot backend's `processPendingJobs()` scheduled method will:
1. Find jobs in PROCESSING status
2. For each job, call OCR processor via `OcrProcessorClient` (HTTP POST with multipart file + categories JSON)
3. Parse the response into `TemporaryExpenseRecord` entities
4. Handle errors: retryable → keep PROCESSING, non-retryable → FAILED with error message

### 3. One-to-Many TemporaryExpenseRecord

Current: `ExpenseInputJob` ↔ `TemporaryExpenseRecord` is OneToOne.
Required: OneToMany — a single receipt may produce multiple expense line items across categories.

Changes:
- `TemporaryExpenseRecord.job` stays as `@ManyToOne` (change from `@OneToOne`, remove unique constraint)
- `ExpenseInputJob.temporaryRecord` becomes `List<TemporaryExpenseRecord> temporaryRecords` with `@OneToMany`
- DTOs, service methods, and frontend updated to handle lists

### 4. PDF Processing

Use PyMuPDF (fitz) to render PDF first page to an image, then process through the same vision pipeline as photos. This keeps the agent pipeline uniform.
