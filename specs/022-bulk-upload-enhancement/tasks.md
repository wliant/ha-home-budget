# Tasks: Bulk Upload Enhancement

**Input**: Design documents from `/specs/022-bulk-upload-enhancement/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/expense-input-api.yaml, quickstart.md

**Tests**: Not requested. No test tasks included.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No setup needed. Existing project structure, dependencies, database schema, and Liquibase infrastructure are all in place.

*(No tasks — all infrastructure already exists)*

---

## Phase 2: Foundational (Backend Status Enum + DB Migration)

**Purpose**: Rename status enum values (INIT→UPLOADED, COMPLETED→PROCESSED) and add new COMPLETED status so all user stories use the correct job lifecycle.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 [P] Create Liquibase migration `budget-backend/src/main/resources/db/changelog/changes/013-update-job-status-enum.xml`. Two UPDATE statements: (1) `UPDATE expense_input_jobs SET status = 'UPLOADED' WHERE status = 'INIT'`, (2) `UPDATE expense_input_jobs SET status = 'PROCESSED' WHERE status = 'COMPLETED'`. Include rollback with reverse UPDATEs. Add `<include>` entry in `budget-backend/src/main/resources/db/changelog/db.changelog-master.xml` after the 012 entry with comment "Feature 022: Bulk Upload Enhancement - rename job statuses".

- [X] T002 [P] Update the `Status` enum in `budget-backend/src/main/java/com/homebudget/model/ExpenseInputJob.java`. Rename `INIT` to `UPLOADED` (comment: "Initial state when job is created/uploaded"), rename `COMPLETED` to `PROCESSED` (comment: "OCR extraction completed, records available for review"), add new `COMPLETED` value (comment: "User confirmed, temp records converted to actual expenses"). Keep PROCESSING, RETRYABLE, FAILED unchanged.

- [X] T003 Update all status references in `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`. In `createJobs()`: change `Status.INIT` to `Status.UPLOADED` (line 79). In `processPendingJobs()`: change `findByStatusOrderByCreatedAtAsc(Status.INIT)` to `Status.UPLOADED` (line 180), change `job.getStatus() == Status.INIT` to `Status.UPLOADED` (line 207), change `job.setStatus(Status.COMPLETED)` to `Status.PROCESSED` (line 253), update log message "Job {} completed" to "Job {} processed" (line 256). Verify all other Status references still match (PROCESSING, RETRYABLE, FAILED are unchanged).

**Checkpoint**: Backend uses new status values (UPLOADED, PROCESSED, COMPLETED). Database rows migrated. All existing functionality works with renamed statuses.

---

## Phase 3: User Story 1 — Job Upload & Table Management (Priority: P1) — MVP

**Goal**: Display a job table with upload, refresh, and delete actions. Each uploaded file appears as a row with status, filename, timestamp, message, and action icons.

**Independent Test**: Upload a file and verify it appears in the job table with UPLOADED status. Delete a job and verify removal. Refresh and verify reload.

### Implementation for User Story 1

- [X] T004 [P] [US1] Update `budget-frontend/src/services/expenseInputJobService.ts`. Change the `ExpenseInputJobDTO.status` type from `'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'` to `'UPLOADED' | 'PROCESSING' | 'RETRYABLE' | 'FAILED' | 'PROCESSED' | 'COMPLETED'`. No other changes to service methods.

- [X] T005 [US1] Rewrite `budget-frontend/src/app/expenses/bulk-upload/page.tsx` to implement the job-level table. **Remove**: `FlatRow` interface, `flatRows` useMemo, `selectableJobIds` useMemo, `toggleSelect`, `handleSelectAll`, `handleConfirm`, `handleDelete` (batch), `openEditDialog`, `handleEditSave`, `editingRecord`/`editForm` state, the edit `<Dialog>` component, and the old flat table rendering. **Add**: (1) Toolbar with "Bulk Upload" button (keep existing upload handler) and "Refresh" button (calls `fetchJobs()`). (2) Job table with `<Table>` containing columns: status (Chip with color per status — UPLOADED=default, PROCESSING=warning, RETRYABLE=warning, FAILED=error, PROCESSED=info, COMPLETED=success), filename, created timestamp (formatted), message (show `errorMessage` from job), and action column. (3) Action column: delete `IconButton` for all jobs (with `window.confirm()` dialog, calls `deleteJobs([job.id])` then `fetchJobs()`). Expand and retry icons are placeholders for now (added in US2). (4) Empty state message when no jobs. Keep `jobs` state, `fetchJobs`, `loading`, `error` state, and initial `useEffect` to fetch on mount. Remove polling for now (added in US2).

**Checkpoint**: Job table displays with upload, refresh, and delete per job. Status chips show correct colors. No expand/polling/inline editing yet.

---

## Phase 4: User Story 2 — Job Processing & Status Polling (Priority: P1)

**Goal**: Auto-poll for status updates while jobs are processing. Show retry icon for RETRYABLE jobs. Show expand icon for PROCESSED/COMPLETED jobs.

**Independent Test**: Upload a file and verify it transitions UPLOADED → PROCESSING → PROCESSED via polling. For a RETRYABLE job, click retry and verify it reprocesses.

### Implementation for User Story 2

- [X] T006 [P] [US2] Add `retryJob(Long jobId)` method to `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`. Find job by ID (throw `ExpenseNotFoundException` if not found). Validate status is `RETRYABLE` (throw `IllegalStateException("Job is not in RETRYABLE status")` otherwise). Set status to `UPLOADED`, clear `errorMessage` (set to null), keep `retryCount` as-is. Save and return `toDTO(job)`.

- [X] T007 [US2] Add `@PostMapping("/{jobId}/retry")` endpoint to `budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java`. Accept `@PathVariable Long jobId`. Call `jobService.retryJob(jobId)`. Return `ResponseEntity.ok(dto)`. Log the retry action. No request body needed.

- [X] T008 [P] [US2] Add `retryJob(jobId: number)` method to `budget-frontend/src/services/expenseInputJobService.ts`. Call `api.post<ExpenseInputJobDTO>(\`/api/expense-input-jobs/${jobId}/retry\`)` and return `response.data`.

- [X] T009 [US2] Update `budget-frontend/src/app/expenses/bulk-upload/page.tsx` to add polling and action icons. (1) **Polling**: Add `useEffect` that checks if any job has status `'UPLOADED'` or `'PROCESSING'`; if so, set a 3-second `setInterval` calling `fetchJobs()`. Clear interval on cleanup or when no jobs need polling. (2) **Action column icons**: For each job row, render: delete icon (all jobs, already from T005), expand icon (visible when status is `'PROCESSED'` or `'COMPLETED'` — use `KeyboardArrowDown`/`KeyboardArrowUp` icon, toggles expand state but expand content added in US3), retry icon (visible when status is `'RETRYABLE'` — use `Replay` icon, calls `retryJob(job.id)` then `fetchJobs()`). Add `expandedJobIds: Set<number>` state for tracking expanded jobs (toggled by expand icon click). Import `KeyboardArrowDown`, `KeyboardArrowUp`, `Replay` from `@mui/icons-material`.

**Checkpoint**: Polling updates the table in real time. Retry re-triggers processing. Expand icon toggles but no content yet.

---

## Phase 5: User Story 3 — Record Review & Inline Editing (Priority: P1)

**Goal**: Expand a PROCESSED/COMPLETED job to see temporary records in a sub-table with inline editable fields and save/undo per row.

**Independent Test**: Expand a processed job, edit a record's description and amount, save, and verify persistence. Undo an edit and verify revert.

### Implementation for User Story 3

- [X] T010 [US3] Update `budget-frontend/src/app/expenses/bulk-upload/page.tsx` to add expandable record sub-table with inline editing. (1) **Expandable row**: Below each job `<TableRow>`, add a second `<TableRow>` containing a `<Collapse in={expandedJobIds.has(job.id)}>` with a nested `<Table>`. The nested table has columns: date, description, amount, category, action. (2) **Inline editing**: Render each record field as a controlled input — date as `<TextField type="date" size="small" variant="standard">`, description as `<TextField size="small" variant="standard">`, amount as `<TextField type="number" size="small" variant="standard">`, category as `<CategorySelect>` component. (3) **Edit state**: Add `editedRecords: Map<number, Partial<TemporaryExpenseRecordDTO>>` state (use `useState` with `new Map()`). When user changes a field, update the map entry for that record ID. Display edited value if present, otherwise display server value. (4) **Save action**: Tick icon (`Check` from `@mui/icons-material`) per row. On click: call `updateTemporaryRecord(recordId, { amount, description, expenseDate, categoryId })` using merged edited+server values. On success: update the record in `jobs` state and clear the record from `editedRecords`. (5) **Undo action**: Undo icon (`Undo` from `@mui/icons-material`) per row. On click: remove the record from `editedRecords` map (fields revert to server values). Only show undo if the record has unsaved edits. (6) **Category dropdown**: Use existing `<CategorySelect>` component (already imported in old code). (7) For COMPLETED jobs, records should be viewable but not editable (disable inputs, hide save/undo icons).

**Checkpoint**: Full inline editing works. Save persists to server. Undo reverts locally. COMPLETED jobs show read-only records.

---

## Phase 6: User Story 4 — Record Merge & Delete (Priority: P2)

**Goal**: Select temporary records via checkboxes and merge (>=2) or delete them via toolbar buttons in the sub-table.

**Independent Test**: Select 3 records, click Merge, verify merged record has earliest date, concatenated descriptions, summed amounts, first category. Select records and delete, verify removal.

### Implementation for User Story 4

- [X] T011 [P] [US4] Create `budget-backend/src/main/java/com/homebudget/dto/RecordIdsRequest.java`. Simple DTO with `private List<Long> recordIds` field, getter, and setter. Used by both merge and delete-records endpoints.

- [X] T012 [P] [US4] Add `void deleteByIdIn(List<Long> ids)` method to `budget-backend/src/main/java/com/homebudget/repository/TemporaryExpenseRecordRepository.java`. Spring Data JPA derives the query automatically.

- [X] T013 [US4] Add `mergeTemporaryRecords(List<Long> recordIds)` and `deleteTemporaryRecords(List<Long> recordIds)` methods to `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`. **Merge**: Validate recordIds has >=2 entries. Fetch all records by IDs. Validate all belong to the same job and job status is not COMPLETED. Sort records by ID ascending. Create new `TemporaryExpenseRecord` with: `expenseDate` = earliest date across all records, `description` = descriptions joined by `"\n"`, `amount` = sum of all amounts, `category` = first record's category, `job` = the shared job. Save the new record. Delete all original records by IDs using `tempRepository.deleteByIdIn(recordIds)`. Return `toTemporaryDTO(merged)`. **Delete**: Validate recordIds is non-empty. Fetch records. Validate job status is not COMPLETED. Call `tempRepository.deleteByIdIn(recordIds)`.

- [X] T014 [US4] Add two endpoints to `budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java`. (1) `@PostMapping("/temporary-records/merge")` accepting `@RequestBody RecordIdsRequest request`. Call `jobService.mergeTemporaryRecords(request.getRecordIds())`. Return `ResponseEntity.ok(mergedDTO)`. (2) `@DeleteMapping("/temporary-records")` accepting `@RequestBody RecordIdsRequest request`. Call `jobService.deleteTemporaryRecords(request.getRecordIds())`. Return `ResponseEntity.noContent().build()`. Add import for `RecordIdsRequest`.

- [X] T015 [P] [US4] Add `mergeRecords(recordIds: number[])` and `deleteRecords(recordIds: number[])` methods to `budget-frontend/src/services/expenseInputJobService.ts`. `mergeRecords`: `api.post<TemporaryExpenseRecordDTO>('/api/expense-input-jobs/temporary-records/merge', { recordIds })`. `deleteRecords`: `api.delete('/api/expense-input-jobs/temporary-records', { data: { recordIds } })`.

- [X] T016 [US4] Update `budget-frontend/src/app/expenses/bulk-upload/page.tsx` to add record selection, merge, and delete. (1) **Selection state**: Add `selectedRecordIds: Map<number, Set<number>>` state (keyed by jobId, values are sets of record IDs). Add checkbox column to the sub-table. Select-all checkbox in sub-table header toggles all records for that job. (2) **Sub-table toolbar**: Above the nested records table (inside the Collapse), add a `<Box>` with Merge button (enabled when >=2 records selected for that job, uses `MergeType` or `CallMerge` icon) and Delete button (enabled when >=1 record selected, uses `Delete` icon). (3) **Merge handler**: For each selected record that has unsaved edits in `editedRecords`, call `updateTemporaryRecord()` first to save pending changes. Then call `mergeRecords(selectedIds)`. Then call `fetchJobs()` and clear selection for that job. (4) **Delete handler**: Call `deleteRecords(selectedIds)`. Then call `fetchJobs()` and clear selection for that job.

**Checkpoint**: Merge combines records correctly. Delete removes selected records. Selection checkboxes work per-job.

---

## Phase 7: User Story 5 — Job Completion (Priority: P2)

**Goal**: Complete a PROCESSED job to convert all temporary records into actual expenses and set status to COMPLETED.

**Independent Test**: Process a job, review records, complete it, verify expenses created and status is COMPLETED.

### Implementation for User Story 5

- [X] T017 [P] [US5] Add `completeJob(Long jobId, String username)` method to `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`. Find job by ID (throw `ExpenseNotFoundException` if not found). Validate status is `PROCESSED` (throw `IllegalStateException("Job must be in PROCESSED status to complete")` if not). Get temp records for the job. Validate at least one record exists (throw `IllegalStateException("No temporary records to finalize")` if empty). For each unconfirmed record: call existing `createExpenseFromTemporary(record, job, username)`, set `confirmed=true`, set `confirmedAt=LocalDateTime.now()`, save record. Set job status to `COMPLETED`. Save job. Return `toDTO(job)`. Log completion with username and record count.

- [X] T018 [US5] Add `@PostMapping("/{jobId}/complete")` endpoint to `budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java`. Accept `@PathVariable Long jobId` and `@RequestHeader(HASS_USER_HEADER) String username`. Call `jobService.completeJob(jobId, username)`. Return `ResponseEntity.ok(dto)`. Log the complete action.

- [X] T019 [P] [US5] Add `completeJob(jobId: number)` method to `budget-frontend/src/services/expenseInputJobService.ts`. Call `api.post<ExpenseInputJobDTO>(\`/api/expense-input-jobs/${jobId}/complete\`)` and return `response.data`.

- [X] T020 [US5] Update `budget-frontend/src/app/expenses/bulk-upload/page.tsx` to add job completion UI. Add a "Complete" button (`CheckCircle` icon) in the job action column, visible only when status is `'PROCESSED'`. On click: call `completeJob(job.id)`, then `fetchJobs()`. Show success feedback (e.g., brief alert or the status chip updating to COMPLETED). Handle 400 errors (display error message from response for "no records" or "wrong status" cases). Completed jobs keep expand icon but records become read-only (already handled in T010).

**Checkpoint**: Full workflow from upload to completed expenses. Job status transitions to COMPLETED. Actual expenses created in system.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and final validation.

- [X] T021 Remove the old `confirmJobs` endpoint and related code. In `budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java`: remove the `@PostMapping("/confirm")` method. In `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`: remove the `confirmJobs()` method (the `completeJob` method from T017 replaces it). In `budget-frontend/src/services/expenseInputJobService.ts`: remove the `confirmJobs` method. Keep `budget-backend/src/main/java/com/homebudget/dto/ConfirmExpenseInputJobsRequest.java` since it is still used by the `deleteJobs` endpoint.

- [X] T022 Validate all 12 integration scenarios from `specs/022-bulk-upload-enhancement/quickstart.md`. Walk through each scenario verifying code paths: upload+process (S1), inline edit+save (S2), undo (S3), merge (S4), delete records (S5), complete job (S6), zero-records-complete (S7), retry (S8), delete job (S9), refresh (S10), non-retryable failure (S11), merge-with-edits (S12). Fix any gaps found.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed.
- **Foundational (Phase 2)**: No dependencies — can start immediately. **BLOCKS all user story work.**
- **US1 (Phase 3)**: Depends on Phase 2. T004 is parallel (frontend service, different project). T005 depends on T004.
- **US2 (Phase 4)**: Depends on Phase 3 (T005 must be complete for frontend changes). Backend tasks (T006-T007) can start after Phase 2.
- **US3 (Phase 5)**: Depends on Phase 4 (T009 must be complete — needs expand icon state).
- **US4 (Phase 6)**: Depends on Phase 5 (T010 must be complete — needs sub-table). Backend tasks (T011-T014) can start after Phase 2.
- **US5 (Phase 7)**: Depends on Phase 5 (T010 must be complete — needs expandable view). Backend tasks (T017-T018) can start after Phase 2. Can run in parallel with Phase 6.
- **Polish (Phase 8)**: Depends on all user stories being complete.

### Within Each User Story

- Backend service methods before controller endpoints
- Frontend service methods before page UI changes
- Backend and frontend service tasks can run in parallel [P] (different projects)

### Parallel Opportunities

```
Phase 2:  T001 ─┐
          T002 ─┤→ T003
                │
Phase 3:  T004 ─┤→ T005
                │
Phase 4:  T006 → T007 ─┐  (backend sequential)
          T008 ─────────┤→ T009  (frontend service parallel with backend)
                        │
Phase 5:                └→ T010
                              │
Phase 6:  T011 ─┐             │  (backend can start early)
          T012 ─┤→ T013 → T014│
          T015 ─┤─────────────┤→ T016
                │             │
Phase 7:  T017 → T018        │  (backend can start early)
          T019 ─┤─────────────┤→ T020
                │
Phase 8:  T021 → T022
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001-T003)
2. Complete Phase 3: US1 frontend (T004-T005)
3. **STOP and VALIDATE**: Upload files, see job table, delete jobs, refresh

### Incremental Delivery

1. Foundational (T001-T003) → Status enum correct
2. US1 (T004-T005) → Job table with upload/refresh/delete (MVP)
3. US2 (T006-T009) → Polling, retry, action icons
4. US3 (T010) → Expandable rows with inline editing
5. US4 (T011-T016) → Merge and delete records
6. US5 (T017-T020) → Complete job → actual expenses
7. Polish (T021-T022) → Cleanup old code, validate scenarios

---

## Notes

- [P] tasks = different files/projects, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- The frontend page (`page.tsx`) is a **complete rewrite** — T005 replaces all existing content, subsequent tasks add to it incrementally
- Backend tasks across stories can start early (after Phase 2) since they modify different methods in the same service file — coordinate carefully
- The existing `CategorySelect` component is reused for inline category editing
- The existing `formatExpenseAmount` utility is reused for amount display
- No new database tables — only status value renames via Liquibase migration
- The old `confirmJobs` flow is replaced by `completeJob` per-job; cleanup in T021
