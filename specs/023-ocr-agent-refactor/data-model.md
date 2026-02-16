# Data Model: OCR Agent Refactor

**Feature**: 023-ocr-agent-refactor
**Date**: 2026-02-16

## Agent State (LangGraph)

The `AgentState` TypedDict carries data through the graph nodes.

### Fields

| Field | Type | Set By | Description |
|-------|------|--------|-------------|
| `file_bytes` | `bytes` | caller | Raw file content |
| `file_type` | `str` | caller | MIME type (image/jpeg, image/png, application/pdf) |
| `categories` | `list[CategoryInput]` | caller | Available expense categories |
| `selected_category` | `CategoryInput \| None` | caller | User-selected category (optional, new) |
| `image_bytes` | `bytes` | extract | Image bytes for display/debug |
| `extracted_text` | `str` | extract | OCR-extracted text (self-hosted only) |
| `receipt_date` | `str \| None` | extract | Receipt date in YYYY-MM-DD format |
| `total_amount` | `Decimal` | extract | Receipt total amount (new) |
| `line_items` | `list[LineItem]` | extract | Extracted line items (new typed model) |
| `classified_items` | `list[ClassifiedLineItem]` | classify | Line items with optional categories (new) |
| `expenses` | `list[ExpenseOutput]` | format | Final expense records |
| `error` | `ErrorResponse \| None` | any node | Error state |

## Pydantic Models

### Input Models

**CategoryInput** (existing, unchanged):
- `id: int`
- `name: str`

### LLM Structured Output Models (new)

**ReceiptExtraction** — output schema for extract LLM call:
- `is_receipt: bool`
- `date: str | None` — YYYY-MM-DD format or null
- `total: Decimal`
- `line_items: list[ExtractedLineItem]`

**ExtractedLineItem** — single line item from extraction:
- `description: str`
- `amount: Decimal`

**ClassificationResult** — output schema for classify LLM call:
- `items: list[ClassifiedLineItem]`

**ClassifiedLineItem** — line item with optional category:
- `description: str`
- `amount: Decimal`
- `category_id: int | None` — null when LLM lacks confidence
- `category_name: str | None` — null when category_id is null

### Output Models

**ExpenseOutput** (updated — category now optional):
- `amount: Decimal` (2 decimal places)
- `description: str`
- `expense_date: date`
- `category_id: int | None` (was: `int`)
- `category_name: str | None` (was: `str`)

**ProcessResponse** (updated — allow empty expenses list when no data):
- `expenses: list[ExpenseOutput]` (min_length=1 constraint retained)

## State Transitions

### Self-Hosted Agent Flow

```
START → validate → extract (OCR + text LLM) → [conditional]
  ├─ selected_category present → format → END
  └─ selected_category absent  → classify (text LLM) → format → END
```

### Paid Agent Flow

```
START → validate → extract_vision (multimodal LLM) → [conditional]
  ├─ selected_category present → format → END
  └─ selected_category absent  → classify (text LLM) → format → END
```

Note: The paid agent's classify step can use the same cloud LLM (text mode) or fall back to Ollama. Decision: use the same paid LLM for consistency.

## Configuration Model

**Settings** (updated):
- `agent_name: str = "self-hosted"` — which agent to use ("self-hosted" or "paid")
- `ollama_host: str` — Ollama server address (self-hosted agent)
- `text_model: str` — Ollama model name (self-hosted agent)
- `anthropic_api_key: str = ""` — API key for paid agent
- `paid_model: str = "claude-sonnet-4-5-20250929"` — Cloud model name
- `max_file_size_mb: int = 10`
- `request_timeout: int = 60`
- `log_level: str = "INFO"`
