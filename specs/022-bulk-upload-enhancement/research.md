# Research: Bulk Upload Enhancement

**Feature**: 022-bulk-upload-enhancement
**Date**: 2026-02-16

## Existing Implementation Analysis

### Current Status Enum

**Current**: `INIT, PROCESSING, RETRYABLE, COMPLETED, FAILED`
**Required**: `UPLOADED, PROCESSING, RETRYABLE, FAILED, PROCESSED, COMPLETED`

**Decision**: Rename enum values and add new `PROCESSED` status.
- `INIT` → `UPLOADED` (initial state after file upload)
- `COMPLETED` → `PROCESSED` (OCR extraction complete, records available for review)
- Add `COMPLETED` (user has finalized the job, temp records converted to expenses)

**Rationale**: The current `COMPLETED` conflates "OCR finished" with "user confirmed". The spec requires distinguishing between "records extracted and ready for review" (PROCESSED) and "records converted to actual expenses" (COMPLETED). This split enables the review-then-confirm workflow.

**Alternatives considered**:
- Adding only `CONFIRMED` status: Rejected because spec explicitly uses `COMPLETED`
- Keeping `COMPLETED` as-is and adding `FINALIZED`: Rejected to match spec terminology

### Database Migration Strategy

**Decision**: Use Liquibase `UPDATE` statements to rename existing status values in-place, then alter the column to support the new enum values.

**Rationale**: MySQL stores `@Enumerated(EnumType.STRING)` as VARCHAR, so status values are plain strings. An UPDATE to rename existing rows (INIT→UPLOADED, COMPLETED→PROCESSED) followed by the Java enum change is safe and reversible.

**Migration steps**:
1. Update existing rows: `INIT` → `UPLOADED`, `COMPLETED` → `PROCESSED`
2. No column type change needed (VARCHAR(20) is sufficient)
3. Java enum update happens in code simultaneously

**Alternatives considered**:
- Adding new statuses without renaming: Rejected because INIT is not user-facing and COMPLETED would be ambiguous
- Using numeric status codes: Rejected, existing pattern uses STRING enums throughout

### Frontend Architecture: Expandable Rows

**Decision**: Replace the current flat `FlatRow` table with a two-level Material-UI Table. Job rows are primary rows; expanding a job reveals a nested sub-table of temporary records below it.

**Rationale**: The current flat table interleaves job cells and record cells in the same table rows, making it hard to add per-job actions and per-record toolbars. An expandable row pattern (MUI `Collapse` inside a `TableRow`) cleanly separates job-level and record-level concerns.

**Implementation pattern**:
- Each job row has an expand/collapse icon in the action column
- Clicking expand reveals a `Collapse` component containing a nested `Table` of temp records
- The nested table has its own toolbar (Merge, Delete buttons) and column headers
- Inline editing uses controlled `TextField`/`Select` components per cell

**Alternatives considered**:
- Accordion component: Rejected, doesn't integrate well with table layout
- Modal/drawer for records: Rejected, spec explicitly calls for expandable sub-table below the job row
- Keep flat table: Rejected, doesn't match spec's expandable row requirement

### Inline Editing Pattern

**Decision**: Use controlled MUI `TextField` and `Select` components rendered directly in table cells. Each row tracks local edit state. Save (tick) persists to server; Undo reverts to last-saved values.

**Rationale**: The current implementation uses an edit dialog (modal). The spec requires inline editing where fields are editable directly in the table. MUI's `TextField` with `size="small"` and `variant="standard"` provides compact inline editing.

**State management**:
- `editedRecords: Map<number, Partial<TemporaryExpenseRecordDTO>>` — tracks unsaved changes per record ID
- On save: call PATCH API, clear edit state for that record, update record in job data
- On undo: clear edit state for that record (reverts to server-saved values)

**Alternatives considered**:
- Edit mode toggle per row: Rejected, adds complexity; all fields should be editable at all times
- Form-based editing with submit: Rejected, spec shows save/undo per row

### Merge Records Backend Logic

**Decision**: Implement merge as a single backend endpoint that creates one new record and deletes the originals atomically.

**Rationale**: Client sends record IDs; backend performs the merge within a transaction to ensure atomicity. The merged record inherits the job association and uses the merge rules from the spec.

**Merge rules** (from spec):
- `date` = earliest selected date
- `description` = all descriptions joined by newline (`\n`)
- `amount` = sum of all amounts
- `category` = category of the first selected record (by ID order)

**Decision on unsaved edits**: The spec says "merge uses currently displayed (edited) values, not last saved values." This means the frontend should save all pending edits before calling merge, OR send the current field values as part of the merge request.

**Chosen approach**: Frontend saves all edited records first (sequential PATCH calls), then calls merge with record IDs. This keeps the merge endpoint simple (operates on saved data) and ensures data consistency.

**Alternatives considered**:
- Client-side merge with single create + bulk delete: Rejected, not atomic and more network calls
- Merge endpoint accepting full record data: Rejected, overcomplicates the API; save-then-merge is simpler

### Complete Job Endpoint

**Decision**: Reuse the existing `confirmJobs` logic but rename the endpoint semantics. The endpoint will accept a single job ID, validate the job has temp records, convert them to expenses, and set status to COMPLETED.

**Rationale**: The existing `confirmJobs` already converts temp records to expenses and attaches files. The main changes are: (1) operate on a single job, (2) set job status to COMPLETED instead of leaving it at PROCESSED, (3) prevent completion of jobs with zero records.

**Endpoint**: `POST /api/expense-input-jobs/{jobId}/complete`

**Alternatives considered**:
- Keep `/confirm` endpoint and add status change: Rejected, spec uses "complete" terminology
- Batch complete: Rejected, spec implies one job at a time for explicit user action

### Delete Temporary Records

**Decision**: New endpoint to delete specific records by ID within a job.

**Rationale**: The current system only supports deleting entire jobs. The spec requires selecting individual temporary records and deleting them without affecting the job or other records.

**Endpoint**: `DELETE /api/expense-input-jobs/temporary-records`
**Request body**: `{ recordIds: [1, 2, 3] }`

### Polling Strategy

**Decision**: Keep existing 3-second polling interval. Poll only while any job has status UPLOADED or PROCESSING.

**Rationale**: Current implementation already polls every 3 seconds when jobs are in processing state. The spec says "every few seconds" which aligns. Stop polling when all jobs reach terminal states (PROCESSED, FAILED, COMPLETED) or RETRYABLE.

**Frontend polling changes**:
- Poll when any job has status `UPLOADED` or `PROCESSING`
- RETRYABLE is a terminal state (user must click retry manually)
- After retry click, status goes back to PROCESSING, polling resumes

### Retry Mechanism

**Decision**: Add retry icon in action column for RETRYABLE jobs. Clicking retry resets status to UPLOADED and clears error message, triggering the scheduled processing task to pick it up again.

**Rationale**: The existing backend `processPendingJobs()` scheduled task already picks up RETRYABLE jobs. Setting status back to UPLOADED (new name for INIT) triggers reprocessing. The retry count is preserved to enforce max retry limits.

**Frontend**: New `retryJob(jobId)` service method calling `POST /api/expense-input-jobs/{jobId}/retry`.

**Backend**: New endpoint that validates job is RETRYABLE, resets status to UPLOADED, clears errorMessage.
