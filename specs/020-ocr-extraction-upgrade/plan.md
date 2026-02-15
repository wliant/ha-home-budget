# Implementation Plan: OCR Extraction Upgrade

**Branch**: `020-ocr-extraction-upgrade` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/020-ocr-extraction-upgrade/spec.md`

## Summary

Replace the LLaVA vision LLM used for receipt text extraction with two non-LLM extraction methods: PyMuPDF direct text extraction for structured PDFs and PaddleOCR for image-based OCR. The text LLM (llama3.1) is used to parse raw extracted text into structured line items. Remove the vision model dependency entirely. Add comprehensive node-level unit tests for all 4 pipeline nodes.

## Technical Context

**Language/Version**: Python 3.11+
**Primary Dependencies**: FastAPI, LangGraph, LangChain, PaddleOCR 3.x, PaddlePaddle 3.x (CPU), PyMuPDF 1.25+, Pillow, structlog
**Storage**: N/A (stateless processor)
**Testing**: pytest 8.0+, pytest-asyncio 0.24+, unittest.mock
**Target Platform**: Linux container (Docker), Home Assistant add-on, CPU-only
**Project Type**: Single Python service (`ocr-processor/`)
**Performance Goals**: Structured PDF processing at least 3x faster than previous vision model approach. Image OCR processing in 1-3 seconds per image on CPU.
**Constraints**: No GPU required. PaddleOCR models auto-download on first run (~200MB). Docker image size increases ~1.5-3 GB due to PaddlePaddle.
**Scale/Scope**: Single household, low throughput (< 10 receipts/day)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | Spec written first, technology-agnostic |
| II. Clarify Before Planning | PASS | Spec fully validated (16/16 checklist items pass) |
| III. Incremental, Story-Based Delivery | PASS | 4 user stories, all P1 (form inseparable change) |
| IV. Constitution Gates | PASS | This section validates gates |
| V. Task Traceability | N/A | Tasks not yet generated |
| VI. Test-Optional, Test-First | PASS | Tests explicitly requested in spec (US4) |
| VII. Artifact Consistency | N/A | Will validate after task generation |
| Technical Stack (NON-NEGOTIABLE) | PASS | This feature modifies only the `ocr-processor/` Python service — not the Next.js frontend or Spring Boot backend. The OCR processor is a standalone microservice outside the core stack constraint. |

**Post-Phase 1 Re-check**: All gates still pass. The design adds PaddleOCR as a new dependency to the OCR processor service only, does not affect the frontend/backend stack.

## Project Structure

### Documentation (this feature)

```text
specs/020-ocr-extraction-upgrade/
├── plan.md              # This file
├── research.md          # Phase 0: PaddleOCR, PyMuPDF, restructure decisions
├── data-model.md        # Phase 1: State model, config changes
├── quickstart.md        # Phase 1: Integration scenarios
├── contracts/
│   └── api.md           # Phase 1: API contract (unchanged external, internal changes)
└── tasks.md             # Phase 2 output (not yet created)
```

### Source Code (repository root)

```text
ocr-processor/
├── src/ocr_processor/
│   ├── main.py              # FastAPI app (health check update)
│   ├── agent.py             # LangGraph pipeline (no changes)
│   ├── config.py            # Settings (remove vision_model)
│   ├── models.py            # Pydantic DTOs (no changes)
│   ├── errors.py            # Error codes (no changes)
│   ├── logging.py           # Structured logging (no changes)
│   └── nodes/
│       ├── validate.py      # Validation node (no changes)
│       ├── extract.py       # Extract node (MAJOR rewrite)
│       ├── classify.py      # Classify node (no changes)
│       └── format.py        # Format node (no changes)
├── tests/
│   ├── conftest.py          # Test fixtures (update)
│   ├── test_api.py          # Integration tests (update references)
│   ├── test_validate_node.py    # NEW: validate unit tests
│   ├── test_extract_node.py     # NEW: extract unit tests
│   ├── test_classify_node.py    # NEW: classify unit tests
│   └── test_format_node.py      # NEW: format unit tests
├── pyproject.toml           # Dependencies (add paddleocr, paddlepaddle)
└── Dockerfile               # System deps for PaddleOCR (if exists)
```

**Structure Decision**: Single Python service. All changes within `ocr-processor/`. The extract node (`nodes/extract.py`) receives the major rewrite. Four new test files are created for node-level unit tests.

## Complexity Tracking

> No constitution violations to justify.
