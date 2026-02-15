# Data Model: Receipt OCR Processor

**Feature**: 018-receipt-ocr-processor
**Date**: 2026-02-15

## Entities

### OCR Processor (Python Service)

These are Pydantic models — not persisted. Used for API request/response validation.

#### CategoryInput
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | integer | yes | Category ID from backend |
| name | string | yes | Category display name |

#### ProcessRequest
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| file | binary (multipart) | yes | Receipt image (JPEG, PNG) or PDF |
| categories | JSON string (list of CategoryInput) | yes | Available categories for matching |

#### ExpenseOutput
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | decimal (2dp) | yes | Extracted expense amount |
| description | string | yes | Expense description from receipt |
| expense_date | date (YYYY-MM-DD) | yes | Date from receipt, defaults to today |
| category_id | integer | yes | Matched category ID |
| category_name | string | yes | Matched category name |

#### ProcessResponse
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| expenses | list of ExpenseOutput | yes | Extracted expenses (1 or more) |

#### ErrorResponse
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| error_code | string | yes | Machine-readable error code |
| message | string | yes | Human-readable error message |
| retryable | boolean | yes | Whether client should retry |

#### AgentState (LangGraph internal)
| Field | Type | Description |
|-------|------|-------------|
| file_bytes | bytes | Raw file content |
| file_type | string | "image/jpeg", "image/png", "application/pdf" |
| categories | list of CategoryInput | Input categories |
| image | bytes | Processed image (after PDF conversion if needed) |
| extracted_text | string | Raw text from vision model |
| line_items | list of dict | Parsed items (amount, description) |
| receipt_date | date or null | Extracted date |
| expenses | list of ExpenseOutput | Final classified expenses |
| error | ErrorResponse or null | Error if processing failed |

---

### Backend Modifications (Spring Boot / MySQL)

#### ExpenseInputJob (MODIFIED)
| Field | Change | Description |
|-------|--------|-------------|
| temporaryRecords | OneToOne → OneToMany | Support multiple expense records per job |

No new columns. Only relationship annotation change.

#### TemporaryExpenseRecord (MODIFIED)
| Field | Change | Description |
|-------|--------|-------------|
| job_id | Remove unique constraint | Allow multiple records per job |

#### Liquibase Migration
```sql
-- Drop unique constraint on temporary_expense_records.job_id
ALTER TABLE temporary_expense_records DROP INDEX UK_job_id;
-- (The exact constraint name may vary; check existing schema)
```

#### OcrResponseDTO (NEW - Java)
| Field | Type | Description |
|-------|------|-------------|
| expenses | List&lt;OcrExpenseDTO&gt; | Parsed from OCR JSON response |

#### OcrExpenseDTO (NEW - Java)
| Field | Type | Description |
|-------|------|-------------|
| amount | BigDecimal | Expense amount |
| description | String | Expense description |
| expenseDate | LocalDate | Expense date |
| categoryId | Long | Matched category ID |
| categoryName | String | Matched category name |

## Relationships

```
ProcessRequest ──contains──> CategoryInput (1:N)
ProcessResponse ──contains──> ExpenseOutput (1:N)

ExpenseInputJob ──has many──> TemporaryExpenseRecord (1:N, changed from 1:1)
TemporaryExpenseRecord ──references──> Category (N:1)
```

## Validation Rules

- File size: max 10MB (OCR service), max 5MB (backend upload — existing)
- File types: JPEG, PNG, PDF only
- Categories list: at least 1 category required
- Amount: positive decimal, 2 decimal places
- Description: non-empty string, max 500 characters
- Expense date: valid date, defaults to today if not extractable
