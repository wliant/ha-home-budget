# Feature Specification: Bulk Upload Enhancement

**Feature Branch**: `022-bulk-upload-enhancement`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "Enhance Bulk Upload to support job lifecycle tracking, expandable extracted records, inline editing + merge workflow, and client-driven processing call."

## Clarifications

### Session 2026-02-16

- Q: How does the user trigger processing for an uploaded job? → A: Processing starts automatically after upload (same as current behavior). RETRYABLE jobs show a retry icon in the action column for manual re-trigger.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Job Upload & Table Management (Priority: P1)

A household member opens the bulk upload page and sees a toolbar with "Bulk Upload" and "Refresh" buttons, plus a table of previously uploaded jobs. They click "Bulk Upload" to upload one or more receipt files. Each file appears as a new row in the job table with status "UPLOADED", the filename, a timestamp, and a message column. The user can delete a job (with confirmation), and refresh the table to reload from the server.

**Why this priority**: The job table is the foundation for the entire feature. Without it, no other workflow (processing, editing, merging, completing) can function.

**Independent Test**: Upload a file and verify it appears in the job table with correct status, filename, and timestamp. Delete a job and verify it disappears. Refresh and verify the table reloads.

**Acceptance Scenarios**:

1. **Given** a user is on the bulk upload page, **When** they click "Bulk Upload" and select a file, **Then** a new row appears in the job table with status "UPLOADED", the original filename, creation timestamp, and an empty message.
2. **Given** a job exists in the table, **When** the user clicks the delete icon and confirms, **Then** the job and all its associated temporary records are removed from the table and the system.
3. **Given** the job table is displayed, **When** the user clicks "Refresh", **Then** the table reloads with the latest data from the server.
4. **Given** the job table has multiple rows, **When** the user views the table, **Then** they see columns for checkbox, status, filename, created timestamp, message, and action icons.

---

### User Story 2 — Job Processing & Status Polling (Priority: P1)

After a file is uploaded, processing starts automatically. The system sends the file along with the current list of expense categories for extraction. The job status changes to "PROCESSING" and the system polls for updates every few seconds. When processing completes, the status updates to "PROCESSED" and the extracted records become available. If processing fails, the status changes to "FAILED" or "RETRYABLE" with an error message. For retryable jobs, a retry icon appears in the action column allowing the user to manually re-trigger processing.

**Why this priority**: Processing is the core value proposition — converting uploaded files into structured expense records. Without it, uploads have no purpose.

**Independent Test**: Upload a file and verify it automatically transitions from UPLOADED to PROCESSING to PROCESSED with extracted records available.

**Acceptance Scenarios**:

1. **Given** a user uploads a file, **When** the job is created with status "UPLOADED", **Then** processing begins automatically, the status changes to "PROCESSING", and the system begins polling for updates.
2. **Given** a job is in "PROCESSING" status, **When** processing completes successfully, **Then** the status changes to "PROCESSED", the message updates, and an expand icon appears in the action column.
3. **Given** a job is in "PROCESSING" status, **When** processing fails with a retryable error, **Then** the status changes to "RETRYABLE", the message shows the error details, and a retry icon appears in the action column.
4. **Given** a job is in "PROCESSING" status, **When** processing fails permanently, **Then** the status changes to "FAILED" and the message shows the error details.
5. **Given** a job has status "RETRYABLE", **When** the user clicks the retry icon, **Then** the system re-attempts processing following the same automatic flow.

---

### User Story 3 — Record Review & Inline Editing (Priority: P1)

When a job reaches "PROCESSED" or "COMPLETED" status, the user clicks the expand icon to reveal the extracted temporary records in a sub-table below the job row. Each record shows date, description, amount, and category — all editable inline. The user can modify any field and save changes individually using a save icon, or revert unsaved edits using an undo icon.

**Why this priority**: Reviewing and correcting extracted data is essential for accuracy before records become actual expenses.

**Independent Test**: Expand a processed job, edit a record's description and amount, save, and verify the changes persist. Use undo to revert an unsaved edit and verify it returns to the original value.

**Acceptance Scenarios**:

1. **Given** a job has status "PROCESSED", **When** the user clicks the expand icon, **Then** a sub-table appears below the job row showing all extracted temporary records with columns: checkbox, date, description, amount, category, and action.
2. **Given** a temporary record is displayed, **When** the user edits the date field, **Then** the field becomes editable and shows the new value locally without saving.
3. **Given** a temporary record has unsaved edits, **When** the user clicks the save (tick) icon, **Then** the changes are persisted to the server and the row reflects the saved state.
4. **Given** a temporary record has unsaved edits, **When** the user clicks the undo icon, **Then** all local edits revert to the last saved values.
5. **Given** the category column on a temporary record, **When** the user clicks it, **Then** a dropdown appears showing all available expense categories for selection.

---

### User Story 4 — Record Merge & Delete (Priority: P2)

When reviewing extracted records, the user can select two or more records via checkboxes and click "Merge" to combine them into a single record. The merged record takes the earliest date, concatenates descriptions (separated by newlines), sums the amounts, and uses the category from the first selected row. The user can also select one or more records and delete them.

**Why this priority**: Merge and delete are editing conveniences that improve efficiency but are not required for the core upload-to-expense flow.

**Independent Test**: Select 3 records, click Merge, and verify the resulting record has the earliest date, combined description, summed amount, and the first record's category. Select records and delete them, verifying removal.

**Acceptance Scenarios**:

1. **Given** the user has selected 2 or more temporary records, **When** they click "Merge", **Then** the selected records are replaced by a single record with: date = earliest selected date, description = all descriptions joined by newlines, amount = sum of all amounts, category = category of the first selected row.
2. **Given** the user has selected fewer than 2 records, **When** they view the toolbar, **Then** the "Merge" button is disabled.
3. **Given** the user has selected one or more temporary records, **When** they click "Delete", **Then** the selected records are removed from the sub-table and the system.

---

### User Story 5 — Job Completion (Priority: P2)

After reviewing and editing extracted records, the user completes the job. This converts all remaining temporary records into actual expenses in the system and changes the job status to "COMPLETED". The user can still expand a completed job to view its records.

**Why this priority**: Completion is the final step that delivers the business value (actual expense records), but requires the preceding stories to function.

**Independent Test**: Process a job, review records, complete the job, and verify all temporary records appear as actual expenses and the job status changes to "COMPLETED".

**Acceptance Scenarios**:

1. **Given** a job has status "PROCESSED" with one or more temporary records, **When** the user completes the job, **Then** all temporary records are inserted as actual expenses and the job status changes to "COMPLETED".
2. **Given** a job has status "COMPLETED", **When** the user views the job table, **Then** the expand icon is still visible, allowing the user to review the records.
3. **Given** a job has status "PROCESSED" with zero temporary records (all deleted), **When** the user attempts to complete the job, **Then** the system prevents completion and shows a message indicating no records to finalize.

---

### Edge Cases

- What happens when the user uploads a file in an unsupported format? The system creates the job but processing returns an error, setting status to "FAILED" with an appropriate message.
- What happens if the user navigates away during processing? The polling stops, but the server continues processing. When the user returns and refreshes, the job reflects the current server-side status.
- What happens when a user deletes a job that is currently processing? The job and any associated records are deleted; any in-progress processing result is discarded when it completes.
- What happens when the user merges records that have unsaved edits? The merge uses the currently displayed (edited) values, not the last saved values.
- What happens if two household members upload files simultaneously? Each user sees all jobs in the table. Jobs are shared across the household.
- What happens when the user tries to complete a job that has already been completed? The system prevents double-completion and shows an informational message.
- What happens when processing produces zero extracted records? The job status is "PROCESSED" with a message indicating no records were found. The user can delete the job.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST display a job table showing all expense input jobs with columns: checkbox, status, filename, created timestamp, message, and action icons.
- **FR-002**: The system MUST provide a "Bulk Upload" button that allows users to upload one or more files, creating one job per file with status "UPLOADED".
- **FR-003**: The system MUST provide a "Refresh" button that reloads the job table from the server.
- **FR-004**: The system MUST allow users to delete a job (with confirmation dialog), removing the job and all associated temporary records.
- **FR-005**: The system MUST automatically start processing after a file is uploaded, sending the file and current category list for extraction. For jobs with status "RETRYABLE", a retry icon in the action column allows manual re-trigger.
- **FR-006**: The system MUST poll for job status updates during processing and reflect status changes (PROCESSING, PROCESSED, RETRYABLE, FAILED) in the table in real time.
- **FR-007**: The action column MUST display: a delete icon for all jobs, an expand icon for jobs with status "PROCESSED" or "COMPLETED", and a retry icon for jobs with status "RETRYABLE".
- **FR-008**: When expanded, the system MUST display a sub-table of temporary records with columns: checkbox, date (editable), description (editable), amount (editable), category (editable dropdown), and action icons (save, undo).
- **FR-009**: The system MUST allow inline editing of temporary record fields (date, description, amount, category) with save and undo actions per row.
- **FR-010**: The system MUST support merging 2 or more selected temporary records using the rules: earliest date, newline-concatenated descriptions, summed amounts, first row's category.
- **FR-011**: The system MUST support deleting selected temporary records.
- **FR-012**: The system MUST support completing a job, which converts all remaining temporary records into actual expenses and sets the job status to "COMPLETED".
- **FR-013**: The system MUST prevent completing a job that has zero temporary records.
- **FR-014**: The system MUST track and display the following job statuses: UPLOADED, PROCESSING, RETRYABLE, FAILED, PROCESSED, COMPLETED.

### Key Entities

- **ExpenseInputJob**: Represents a single uploaded file for bulk expense entry. Has a status lifecycle (UPLOADED → PROCESSING → PROCESSED/RETRYABLE/FAILED → COMPLETED), a filename, creation timestamp, and a message for status details or errors. Associated with zero or more temporary records.
- **TemporaryExpenseRecord**: An extracted expense record from a processed job. Has date, description, amount, and category. Exists temporarily until the user completes the job, at which point it becomes an actual expense. Can be edited, merged, or deleted before completion.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can upload a file and see it appear in the job table within 2 seconds.
- **SC-002**: Processing status updates are visible to the user within 5 seconds of the server completing processing.
- **SC-003**: Users can edit, save, and undo changes to extracted records without page reload.
- **SC-004**: Merging records produces correct results (earliest date, concatenated descriptions, summed amounts, first category) 100% of the time.
- **SC-005**: Completing a job creates the exact number of actual expenses matching the remaining temporary records.
- **SC-006**: The entire flow from upload to completed expenses can be accomplished in under 5 minutes for a typical receipt file.

## Assumptions

- The existing bulk upload page and file upload mechanism already exist and will be enhanced (not built from scratch).
- The processing/extraction service (OCR + LLM) already exists as a separate component; this feature integrates with it via a processing endpoint.
- All household members share visibility of all jobs (no per-user isolation of jobs).
- The category list used during processing is the same flat list of all categories available in the system.
- Files supported for upload are the same as currently supported (images and PDFs).
- Polling interval for processing status is a reasonable default (e.g., every 3-5 seconds) and stops when a terminal status is reached.
- The "Merge" and "Delete" buttons for temporary records appear in a toolbar above the expanded sub-table.
