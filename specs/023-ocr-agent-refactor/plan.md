# Implementation Plan: OCR Agent Refactor

**Branch**: `023-ocr-agent-refactor` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/023-ocr-agent-refactor/spec.md`

## Summary

Refactor the OCR receipt processor to: (1) accept an optional user-selected category and conditionally skip classification, (2) restructure the LangGraph agent to use Pydantic structured LLM output with ChatPromptTemplate, (3) create a dual-agent architecture with "self-hosted" (Ollama + OCR) and "paid" (multimodal vision model) agents, and (4) add smart format-step consolidation logic. Also update the Spring Boot backend caller to pass category and handle nullable category responses.

## Technical Context

**Language/Version**: Python 3.11+ (OCR processor), Java 17 (backend integration)
**Primary Dependencies**: FastAPI, LangGraph >=0.2.0, LangChain >=0.3.0, langchain-ollama >=0.2.0, langchain-anthropic (new, for paid agent), pytesseract, PyMuPDF, Pillow, structlog, pydantic-settings
**Storage**: N/A (stateless processor)
**Testing**: pytest, pytest-asyncio
**Target Platform**: Linux container (Docker), private home network
**Project Type**: Multi-service (OCR processor + backend integration)
**Performance Goals**: Process a receipt within 60s (self-hosted), 30s (paid)
**Constraints**: Single-user processing (no concurrency requirements), <10MB file size
**Scale/Scope**: Household use, ~10-50 receipts/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Next.js frontend | PASS | No frontend changes in this feature (out of scope) |
| Spring Boot backend | PASS | Backend `OcrProcessorClient` updated to pass category and handle null |
| X-Hass-User auth | PASS | No auth changes; backend already reads header before calling OCR |
| Private network deployment | PASS | OCR processor runs on local network |
| Multi-user household | PASS | `createdBy` already tracked on jobs; no change needed |
| Specification-First | PASS | spec.md completed and clarified |
| Clarify Before Planning | PASS | `/speckit.clarify` completed with 2 questions |

**Post-Phase 1 Re-check**: All gates still pass. The paid agent adds a new external dependency (cloud LLM API) but this is configured via environment variable and the default remains "self-hosted" (no external calls by default).

## Project Structure

### Documentation (this feature)

```text
specs/023-ocr-agent-refactor/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── process-api.yaml # Updated OpenAPI spec
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
ocr-processor/
├── src/ocr_processor/
│   ├── __init__.py
│   ├── main.py                    # FastAPI app - add selected_category param
│   ├── config.py                  # Settings - add agent_name, paid LLM config
│   ├── models.py                  # Pydantic models - add structured LLM models
│   ├── errors.py                  # Error classes (unchanged)
│   ├── logging.py                 # Logging (unchanged)
│   ├── callbacks.py               # Callbacks (unchanged)
│   ├── agents/                    # NEW: dual agent module
│   │   ├── __init__.py            # Agent registry and dispatcher
│   │   ├── self_hosted.py         # Self-hosted LangGraph (Ollama + OCR)
│   │   └── paid.py                # Paid LangGraph (multimodal vision)
│   └── nodes/
│       ├── __init__.py
│       ├── validate.py            # Updated: pass through selected_category
│       ├── extract.py             # Refactored: structured output, split OCR vs vision
│       ├── extract_vision.py      # NEW: multimodal vision extract for paid agent
│       ├── classify.py            # Refactored: structured output, null categories
│       └── format.py              # Refactored: smart consolidation logic
└── tests/
    ├── test_api.py                # Updated
    ├── test_classify_node.py      # Updated
    ├── test_extract_node.py       # Updated
    ├── test_format_node.py        # Updated
    └── test_validate_node.py      # Updated

budget-backend/
└── src/main/java/com/homebudget/
    ├── service/
    │   ├── OcrProcessorClient.java     # Updated: pass category, handle null
    │   └── ExpenseInputJobService.java # Updated: pass default category to OCR
    └── dto/
        ├── OcrExpenseDTO.java          # Already supports null (Long type)
        └── OcrResponseDTO.java         # Unchanged
```

**Structure Decision**: Existing multi-service layout. New `agents/` package within `ocr-processor` for dual agent graphs. Existing `nodes/` package retains shared node implementations. New `extract_vision.py` for the paid agent's multimodal extract node.
