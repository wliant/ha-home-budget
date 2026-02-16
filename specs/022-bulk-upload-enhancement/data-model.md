# Data Model: Bulk Upload Enhancement

**Feature**: 022-bulk-upload-enhancement
**Date**: 2026-02-16

## Entities

### ExpenseInputJob (existing — modified)

Represents a single uploaded file for bulk expense entry.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-generated | |
| status | Enum(STRING) | NOT NULL | **CHANGED**: UPLOADED, PROCESSING, RETRYABLE, FAILED, PROCESSED, COMPLETED |
| retryCount | Integer | NOT NULL, default 0 | Tracks OCR retry attempts |
| originalFilename | String(255) | NOT NULL | Original uploaded filename |
| filePath | String(500) | NOT NULL | Server-side storage path |
| createdBy | String(100) | NOT NULL | X-Hass-User value |
| errorMessage | String(500) | nullable | Error details or status message |
| createdAt | LocalDateTime | NOT NULL, immutable | Set on creation |
| updatedAt | LocalDateTime | NOT NULL | Updated on every change |
| temporaryRecords | List | OneToMany, cascade ALL, orphanRemoval | Child records |

**Status lifecycle**:
```
UPLOADED → PROCESSING → PROCESSED → COMPLETED
    ↓          ↓
    └──→ RETRYABLE ──→ UPLOADED (retry)
              ↓
           FAILED (max retries)
    ↓
 FAILED (non-retryable error)
```

**Status changes from current**:
- `INIT` renamed to `UPLOADED` — clearer user-facing name
- `COMPLETED` renamed to `PROCESSED` — means OCR extraction finished, records available
- New `COMPLETED` — means user confirmed, temp records converted to actual expenses

### TemporaryExpenseRecord (existing — no schema changes)

An extracted expense record from a processed job. Exists temporarily until the job is completed.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-generated | |
| job | ExpenseInputJob | FK(job_id), NOT NULL, CASCADE | Parent job |
| amount | BigDecimal(10,2) | NOT NULL | Extracted or user-edited amount |
| description | String(500) | NOT NULL | Extracted or user-edited description |
| expenseDate | LocalDate | NOT NULL | Extracted or user-edited date |
| category | Category | FK(category_id), nullable, SET NULL | Selected category |
| confirmed | boolean | NOT NULL, default false | Whether converted to expense |
| confirmedAt | LocalDateTime | nullable | When confirmed/completed |
| createdAt | LocalDateTime | NOT NULL, immutable | |
| updatedAt | LocalDateTime | NOT NULL | |

### Relationships

```
ExpenseInputJob 1 ──── * TemporaryExpenseRecord
                              │
                              └──── 0..1 Category (FK, nullable)
```

- One job has many temporary records (CASCADE delete)
- Each record optionally references a Category
- When a job is completed, each temp record becomes an Expense + ExpenseFile

## Database Migration

### Migration: 013-update-job-status-enum.xml

**Purpose**: Rename existing status values to match new lifecycle.

**Operations**:
1. `UPDATE expense_input_jobs SET status = 'UPLOADED' WHERE status = 'INIT'`
2. `UPDATE expense_input_jobs SET status = 'PROCESSED' WHERE status = 'COMPLETED'`

**Rollback**:
1. `UPDATE expense_input_jobs SET status = 'INIT' WHERE status = 'UPLOADED'`
2. `UPDATE expense_input_jobs SET status = 'COMPLETED' WHERE status = 'PROCESSED'`

**Note**: No column type change needed. The `status` column is `VARCHAR(20)` which accommodates all new values. The `COMPLETED` value (new meaning: user finalized) doesn't conflict because existing `COMPLETED` rows are renamed to `PROCESSED` first.

## State Transitions

### Job Status Transitions

| From | To | Trigger | Actor |
|------|----|---------|-------|
| (new) | UPLOADED | File uploaded | System (on upload) |
| UPLOADED | PROCESSING | Scheduled task picks up job | System (automatic) |
| PROCESSING | PROCESSED | OCR extraction succeeds | System (automatic) |
| PROCESSING | RETRYABLE | OCR extraction fails (retryable error) | System (automatic) |
| PROCESSING | FAILED | OCR extraction fails (non-retryable or max retries) | System (automatic) |
| RETRYABLE | UPLOADED | User clicks retry | User (manual) |
| PROCESSED | COMPLETED | User completes the job | User (manual) |

### Record Lifecycle

| Event | Effect |
|-------|--------|
| OCR extraction | Creates TemporaryExpenseRecord entries for the job |
| User edits field | Updates record via PATCH (date, description, amount, category) |
| User merges records | Creates one merged record, deletes originals |
| User deletes records | Removes selected records |
| User completes job | Sets confirmed=true, confirmedAt=now(), creates Expense + ExpenseFile |

## Validation Rules

### ExpenseInputJob
- Status transitions must follow the state machine (no skipping states)
- Cannot delete a job if any record has confirmed=true
- Cannot complete a job with zero temporary records
- Cannot complete a job that is not in PROCESSED status
- Retry is only allowed when status is RETRYABLE

### TemporaryExpenseRecord
- amount: required, positive decimal
- description: required, non-blank, max 500 chars
- expenseDate: required, valid date
- category: optional (nullable)
- Merge requires >= 2 records from the same job
- Cannot edit or delete records in a COMPLETED job
