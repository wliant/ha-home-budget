# Expense Input Job State Machine

## Overview

The ExpenseInputJob entity implements a state machine with automatic retry logic for processing receipt images through OCR.

## States

### INIT
- **Initial state** when a job is first created
- Job file has been saved but OCR processing hasn't started
- Transitions to: PROCESSING

### PROCESSING
- **Active processing** state when OCR service is being called
- Job is currently being processed by the OCR processor
- Transitions to: COMPLETED, RETRYABLE, FAILED

### RETRYABLE
- **Temporary failure** state when a retryable error occurs
- Job will be retried by the scheduler
- Retry count is incremented each time job enters this state
- Transitions to: PROCESSING (retry), FAILED (max retries reached)

### COMPLETED
- **Success state** when OCR processing completed successfully
- Temporary expense records have been created
- Terminal state (no further transitions)

### FAILED
- **Permanent failure** state when:
  - Non-retryable error occurs, OR
  - Maximum retry count (3) is reached
- Terminal state (no further transitions)

## State Transitions

```
INIT
  ↓
PROCESSING ──────────────────┐
  ↓                           │
  ├─→ COMPLETED (success)     │
  │                           │
  ├─→ RETRYABLE (error) ──────┤
  │      ↓                    │
  │   retry_count++           │
  │      ↓                    │
  │   (if retry_count < 3)    │
  │      ↓                    │
  └──────┘                    │
                              │
  └─→ FAILED (max retries     │
      or non-retryable error) │
```

## Retry Logic

- **Maximum Retries**: 3 attempts
- **Retry Counter**: `retry_count` column tracks attempts
- **Retry Trigger**: Scheduler picks up INIT, PROCESSING, and RETRYABLE jobs
- **Backoff**: Fixed 2-second delay between scheduler runs (no exponential backoff)

### Retry Decision Tree

1. **OCR Success** → Status = COMPLETED, Clear error message
2. **Retryable Error**:
   - Increment retry_count
   - If retry_count >= 3 → Status = FAILED, Error message includes retry count
   - If retry_count < 3 → Status = RETRYABLE, Error message shows "Attempt X/3"
3. **Non-Retryable Error** → Status = FAILED immediately (no retry)
4. **Exception** → Treated as retryable error (follows step 2)

## Error Messages

Error messages include contextual information:

- **Retryable (in progress)**: `"Attempt 2/3 failed: Connection timeout"`
- **Failed (max retries)**: `"Failed after 3 retries: Connection timeout"`
- **Non-retryable**: `"Invalid file format"`

## Database Schema

```sql
CREATE TABLE expense_input_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) NOT NULL,  -- INIT, PROCESSING, RETRYABLE, COMPLETED, FAILED
    retry_count INT NOT NULL DEFAULT 0,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_expense_input_jobs_status (status)
);
```

## Implementation Details

### Service Layer (`ExpenseInputJobService`)

**Constants**:
- `MAX_RETRIES = 3`
- `fixedDelay = 2000` (2 seconds between scheduler runs)

**Scheduler Method**: `processPendingJobs()`
- Runs every 2 seconds
- Queries for jobs in INIT, PROCESSING, or RETRYABLE status
- Transitions INIT/RETRYABLE → PROCESSING before attempting OCR
- Handles all state transitions based on OCR result

**State Transition Logic**:
```java
if (result.isSuccess()) {
    status = COMPLETED
    errorMessage = null
} else if (result.isRetryable()) {
    retryCount++
    if (retryCount >= MAX_RETRIES) {
        status = FAILED
        errorMessage = "Failed after 3 retries: ..."
    } else {
        status = RETRYABLE
        errorMessage = "Attempt X/3 failed: ..."
    }
} else {
    status = FAILED
    errorMessage = result.getErrorMessage()
}
```

## API Response

The ExpenseInputJobDTO includes:
```json
{
  "id": 123,
  "status": "RETRYABLE",
  "retryCount": 2,
  "originalFilename": "receipt.jpg",
  "errorMessage": "Attempt 2/3 failed: OCR service unavailable",
  "createdAt": "2026-02-16T01:30:00",
  "updatedAt": "2026-02-16T01:30:05"
}
```

## Testing Considerations

- Test state transitions for all paths (success, retryable, non-retryable, max retries)
- Verify retry_count increments correctly
- Verify error messages format correctly
- Test scheduler doesn't process COMPLETED or FAILED jobs
- Test concurrent processing doesn't create duplicate records

## Migration

Database migration `012-add-expense-job-retry-count.xml`:
- Adds `retry_count` column (default 0)
- Migrates existing PENDING → INIT
- Rollback support included
