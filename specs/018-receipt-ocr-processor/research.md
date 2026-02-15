# Research: Receipt OCR Processor

**Feature**: 018-receipt-ocr-processor
**Date**: 2026-02-15

## R1: Python Web Framework for OCR API

**Decision**: FastAPI
**Rationale**: FastAPI provides async support (important for long-running Ollama calls), automatic OpenAPI docs, Pydantic model validation, and multipart file upload handling out of the box. It's the standard choice for Python ML/AI service APIs.
**Alternatives considered**:
- Flask: Simpler but lacks async, Pydantic integration, and auto-generated docs
- Django REST Framework: Too heavyweight for a single-endpoint microservice

## R2: LangGraph Agent Architecture

**Decision**: LangGraph StateGraph with 4 sequential nodes (validate → extract → classify → format) and error routing
**Rationale**: LangGraph provides a declarative graph-based workflow with built-in state management, node-level logging via callbacks, and conditional routing for error handling. This maps directly to the spec's requirement for structured step-by-step processing logs.
**Alternatives considered**:
- Plain LangChain chains: Less control over error routing, no graph visualization, harder to add nodes later
- Direct Ollama HTTP calls: Would lose LangChain's callback system for agent logging and require manual prompt management

## R3: Vision Model Integration (llava:13b)

**Decision**: Use `langchain-ollama` package with `ChatOllama` (multimodal) for vision model calls
**Rationale**: `langchain-ollama` is the official LangChain integration for Ollama. `ChatOllama` supports multimodal inputs (image + text prompt) required for sending receipt images to llava:13b. It handles base64 encoding and the Ollama API protocol.
**Alternatives considered**:
- Raw Ollama REST API via httpx: Would work but loses LangChain callback integration for logging
- `ollama` Python package: Simpler but doesn't integrate with LangGraph callbacks

## R4: PDF to Image Conversion

**Decision**: PyMuPDF (fitz) for PDF first-page rendering
**Rationale**: PyMuPDF renders PDF pages to images efficiently, has no system-level dependencies (pure Python wheel), and is well-maintained. Rendering to image keeps the vision pipeline uniform for both photo and PDF inputs.
**Alternatives considered**:
- pdf2image + Poppler: Requires system-level Poppler installation in Docker, adds complexity
- Pillow: Cannot open PDFs without Ghostscript dependency

## R5: Package Management

**Decision**: uv (as specified by user)
**Rationale**: uv is a fast Python package manager that replaces pip + pip-tools. Generates lock files for reproducible builds. Supports `pyproject.toml` natively.
**Alternatives considered**: None — user explicitly requested uv.

## R6: Backend HTTP Client for OCR Service

**Decision**: Spring Boot's `RestTemplate` (or `WebClient` for async) with multipart form data
**Rationale**: The backend needs to POST the receipt file + categories JSON to the OCR service. `RestTemplate` is already available in Spring Boot 3.2.0 and handles multipart uploads. Since `processPendingJobs()` runs on a scheduled thread, blocking `RestTemplate` is acceptable.
**Alternatives considered**:
- WebClient (reactive): Adds complexity for a scheduled background job that processes sequentially
- Apache HttpClient: Already in classpath (for tests) but RestTemplate is more idiomatic for Spring

## R7: TemporaryExpenseRecord Relationship Change

**Decision**: Change OneToOne to OneToMany (ExpenseInputJob → TemporaryExpenseRecord)
**Rationale**: A single receipt can contain items spanning multiple categories, producing multiple expense objects. The current OneToOne relationship would require creating multiple jobs for one receipt, which breaks the "one receipt = one job" model.
**Changes required**:
- Database: Liquibase migration to drop unique constraint on `temporary_expense_records.job_id`
- Model: `ExpenseInputJob.temporaryRecord` → `temporaryRecords` (List)
- DTO: `ExpenseInputJobDTO.temporaryRecord` → `temporaryRecords` (List)
- Service: All methods that read/write temporary records adapted for lists
- Frontend: Bulk upload and confirmation flows adapted for multiple records per job

## R8: Structured Logging for Agent Steps

**Decision**: Python `structlog` with JSON output, LangChain callbacks for agent-level logging
**Rationale**: structlog produces JSON logs matching the existing backend's Logback JSON format. LangChain's callback system allows attaching a custom handler to each graph node, capturing input/output at each processing step without modifying node logic.
**Alternatives considered**:
- Standard Python logging with JSON formatter: Works but structlog provides better structured context binding
- LangSmith: External service, unnecessary for private home network deployment

## R9: Error Classification

**Decision**: Two error categories with HTTP status codes
**Rationale**: Aligns with spec FR-008 through FR-011.
- **Non-retryable (HTTP 422)**: Invalid file format, file too large, not a receipt, empty category list, no extractable data. Client should not retry.
- **Retryable (HTTP 503)**: Ollama server unreachable, processing timeout. Client should retry after delay.
- All errors include JSON body: `{"error_code": "string", "message": "string", "retryable": boolean}`

## R10: Docker Integration

**Decision**: Two Dockerfiles (dev with hot reload, prod optimized) + docker-compose service entries
**Rationale**: Matches existing project pattern where backend and frontend each have `Dockerfile` (dev) and `Dockerfile.prod`.
- Dev: Mount source code, use `uvicorn --reload`, expose port 8082
- Prod: Multi-stage build with uv, copy only compiled deps, run uvicorn in production mode
- Service added to both `docker-compose.yml` and `docker-compose.prod.yml` on `homebudget-network`
