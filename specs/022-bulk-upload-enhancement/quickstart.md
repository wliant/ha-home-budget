# Quickstart: Bulk Upload Enhancement

**Feature**: 022-bulk-upload-enhancement
**Date**: 2026-02-16

## Integration Scenarios

### Scenario 1: Upload and Process a Receipt

1. User navigates to bulk upload page
2. Clicks "Bulk Upload", selects a receipt image
3. `POST /api/expense-input-jobs` with multipart file
4. Response: job with status `UPLOADED`
5. Job appears in table with UPLOADED status
6. Backend scheduled task picks up job, sets status to PROCESSING
7. Frontend polling detects PROCESSING status
8. OCR processes the file, extracts records
9. Status transitions to PROCESSED
10. Frontend polling detects PROCESSED, expand icon appears
11. User clicks expand to see extracted records

**Verify**: Job transitions UPLOADED → PROCESSING → PROCESSED. Records appear in sub-table.

### Scenario 2: Inline Edit and Save a Record

1. User expands a PROCESSED job
2. Temporary records visible in sub-table with editable fields
3. User changes the amount field of record #1
4. Row shows unsaved state
5. User clicks save (tick) icon
6. `PATCH /api/expense-input-jobs/temporary-records/{recordId}` with new values
7. Response: updated record DTO
8. Row reflects saved state

**Verify**: PATCH call succeeds, field persists on page refresh.

### Scenario 3: Undo an Edit

1. User edits description of a record
2. Field shows new value locally
3. User clicks undo icon
4. Field reverts to last saved value (no API call)

**Verify**: No network request on undo. Value reverts.

### Scenario 4: Merge Two Records

1. User expands a PROCESSED job with 3 records:
   - Record A: date=2026-01-15, desc="Coffee", amount=5.00, cat=Food
   - Record B: date=2026-01-10, desc="Lunch", amount=12.50, cat=Food
   - Record C: date=2026-01-20, desc="Dinner", amount=25.00, cat=Dining
2. User selects records A and B via checkboxes
3. Clicks "Merge" button
4. Frontend saves any pending edits first
5. `POST /api/expense-input-jobs/temporary-records/merge` with `{ recordIds: [A.id, B.id] }`
6. Backend creates merged record:
   - date = 2026-01-10 (earliest)
   - description = "Coffee\nLunch" (concatenated)
   - amount = 17.50 (sum)
   - category = Food (from first selected record A)
7. Records A and B removed, merged record appears
8. Record C unchanged

**Verify**: Merged record has correct values. Original records gone.

### Scenario 5: Delete Temporary Records

1. User expands a PROCESSED job
2. Selects 2 records via checkboxes
3. Clicks "Delete" button
4. `DELETE /api/expense-input-jobs/temporary-records` with `{ recordIds: [...] }`
5. Selected records removed from sub-table

**Verify**: Records deleted. Remaining records unaffected.

### Scenario 6: Complete a Job

1. User has a PROCESSED job with 3 reviewed temporary records
2. User clicks complete action for the job
3. `POST /api/expense-input-jobs/{jobId}/complete`
4. Backend:
   - Creates 3 Expense records via ExpenseService
   - Attaches original file to each expense
   - Sets confirmed=true, confirmedAt=now() on each temp record
   - Sets job status to COMPLETED
5. Job row shows COMPLETED status
6. Expand icon still available to view records

**Verify**: 3 new expenses in system. Job status is COMPLETED. Temp records show confirmed.

### Scenario 7: Complete Job with Zero Records (Error)

1. User has a PROCESSED job
2. User deletes all temporary records
3. User attempts to complete the job
4. `POST /api/expense-input-jobs/{jobId}/complete`
5. Backend returns 400 error: "No temporary records to finalize"
6. Frontend shows error message

**Verify**: Completion blocked. Job stays PROCESSED.

### Scenario 8: Retry a Failed Job

1. A job fails with retryable error (OCR service temporarily unavailable)
2. Job shows RETRYABLE status with error message
3. Retry icon visible in action column
4. User clicks retry icon
5. `POST /api/expense-input-jobs/{jobId}/retry`
6. Backend resets status to UPLOADED, clears errorMessage
7. Scheduled task picks up job again for reprocessing
8. Polling resumes

**Verify**: Job transitions RETRYABLE → UPLOADED → PROCESSING → PROCESSED.

### Scenario 9: Delete a Job

1. User sees a FAILED job they want to remove
2. Clicks delete icon in action column
3. Confirmation dialog appears
4. User confirms deletion
5. `DELETE /api/expense-input-jobs` with `{ jobIds: [id] }`
6. Job and all its temporary records removed

**Verify**: Job gone from table. No orphaned records.

### Scenario 10: Refresh Table

1. User clicks "Refresh" button in toolbar
2. `GET /api/expense-input-jobs`
3. Table reloads with latest server data

**Verify**: Any changes from other users or background processing reflected.

### Scenario 11: Processing Failure (Non-Retryable)

1. User uploads an unsupported file
2. Job created with UPLOADED status
3. Scheduled task attempts processing
4. OCR returns 400 (bad request)
5. Job transitions to FAILED with error message
6. No retry icon (non-retryable)

**Verify**: Job shows FAILED with error. Only delete icon in actions.

### Scenario 12: Merge Uses Edited (Unsaved) Values

1. User has 2 records, edits record A's amount to 99.00 (unsaved)
2. Selects both records and clicks Merge
3. Frontend saves record A first (PATCH with amount=99.00)
4. Then calls merge endpoint
5. Merged record uses saved value (99.00 for A's amount)

**Verify**: Merge result includes the edited amount.
