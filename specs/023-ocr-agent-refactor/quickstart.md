# Quickstart: OCR Agent Refactor

**Feature**: 023-ocr-agent-refactor
**Date**: 2026-02-16

## Integration Scenarios

### Scenario 1: Upload with Pre-selected Category (Self-Hosted)

1. User selects "Groceries" category in the bulk upload dialog
2. User uploads a grocery receipt image
3. Backend calls `POST /process` with:
   - `file`: receipt image
   - `categories`: all available categories
   - `selected_category`: `{"id": 5, "name": "Groceries"}`
4. OCR processor (self-hosted agent):
   - Validates file
   - Extracts text via Tesseract OCR, parses with Ollama → gets line items + total
   - Skips classification (selected_category present)
   - Format step: all items have same category (Groceries) → consolidates to 1 record
5. Returns: `{"expenses": [{"amount": 45.50, "description": "Milk, Bread, Eggs", "expense_date": "2026-02-15", "category_id": 5, "category_name": "Groceries"}]}`
6. Backend creates 1 temporary expense record with category = Groceries

### Scenario 2: Upload without Category (Self-Hosted)

1. User uploads a receipt without selecting a category
2. Backend calls `POST /process` with `file` and `categories` only (no `selected_category`)
3. OCR processor (self-hosted agent):
   - Validates file
   - Extracts text via OCR, parses with Ollama → line items
   - Classifies each line item using Ollama → some get categories, some get null
   - Format step: mixed categories → returns each line item as separate record
4. Returns: `{"expenses": [{"amount": 12.50, "description": "Coffee", "expense_date": "2026-02-15", "category_id": 3, "category_name": "Dining"}, {"amount": 5.00, "description": "Magazine", "expense_date": "2026-02-15", "category_id": null, "category_name": null}]}`
5. Backend creates 2 temporary expense records; the null-category one has no category set

### Scenario 3: Paid Agent with Vision

1. System configured with `AGENT_NAME=paid` and `ANTHROPIC_API_KEY=sk-...`
2. User uploads a receipt image
3. Backend calls `POST /process` (same API, agent selection is server-side config)
4. OCR processor (paid agent):
   - Validates file
   - Sends receipt image directly to Claude vision model → structured extraction (no OCR)
   - Classification and formatting proceed the same as self-hosted
5. Returns same format `ProcessResponse`

### Scenario 4: Null Category Handling

1. OCR processor returns an expense with `category_id: null`
2. Backend `processPendingJobs()`:
   - `ocrExpense.getCategoryId()` returns null
   - Existing code: `if (ocrExpense.getCategoryId() != null)` — skips setting category
   - Falls back to job's `defaultCategory` if set
   - If neither OCR nor job has category → `TemporaryExpenseRecord.category` stays null
3. User sees the record in the UI with no category, can manually assign one

## Test Flows

### Manual Test: Pre-selected Category

```bash
# Self-hosted agent
curl -X POST http://localhost:8082/process \
  -F "file=@receipt.jpg" \
  -F 'categories=[{"id":1,"name":"Groceries"},{"id":2,"name":"Dining"}]' \
  -F 'selected_category={"id":1,"name":"Groceries"}'

# Expect: single expense with category_id=1
```

### Manual Test: No Category (Classification)

```bash
curl -X POST http://localhost:8082/process \
  -F "file=@receipt.jpg" \
  -F 'categories=[{"id":1,"name":"Groceries"},{"id":2,"name":"Dining"}]'

# Expect: expenses with category_id set or null per item
```

### Manual Test: Paid Agent

```bash
# Set env: AGENT_NAME=paid ANTHROPIC_API_KEY=sk-...
curl -X POST http://localhost:8082/process \
  -F "file=@receipt.jpg" \
  -F 'categories=[{"id":1,"name":"Groceries"},{"id":2,"name":"Dining"}]'

# Expect: same response format, processed via vision model
```
