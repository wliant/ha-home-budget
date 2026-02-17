# Tasks: Object Storage for File Management

**Input**: Design documents from `/specs/024-object-storage/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks grouped by user story. US3 (Docker Compose / infrastructure) is executed first as setup since it enables all other stories.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add MinIO dependency and create the storage abstraction layer

- [X] T001 Add AWS SDK v2 BOM and s3 dependency to budget-backend/pom.xml
- [X] T002 Add storage configuration properties to budget-backend/src/main/resources/application.yml
- [X] T003 Create StorageConfig with S3Client bean and bucket auto-creation in budget-backend/src/main/java/com/homebudget/config/StorageConfig.java
- [X] T004 Create StorageService with putObject, getObject, deleteObject, copyObject, objectExists methods in budget-backend/src/main/java/com/homebudget/service/StorageService.java

---

## Phase 2: User Story 3 - Object Storage Available in Dev and Prod Environments (Priority: P1)

**Goal**: Add MinIO as a Docker service in both dev and prod compose files with persistent volumes

**Independent Test**: Start dev environment, verify MinIO is running and accessible. Check MinIO console at http://localhost:9001.

- [X] T005 [US3] Add minio service, minio-data volume, and backend storage env vars to docker-compose.yml
- [X] T006 [US3] Add minio service, minio-data volume, and backend storage env vars to docker-compose.prod.yml

**Checkpoint**: MinIO is running in both environments. Backend can connect and auto-create the bucket on startup.

---

## Phase 3: User Story 1 - Store Uploaded Files in Object Storage (Priority: P1) 🎯 MVP

**Goal**: All file writes go to MinIO instead of local filesystem

**Independent Test**: Upload a receipt via record expense or bulk upload. Verify the file appears in MinIO bucket. Restart backend and verify file is still accessible.

- [X] T007 [US1] Refactor ExpenseInputJobService.createJobs() to use StorageService.putObject() instead of Files.write, remove resolveJobPath(), update filePath to store object key in budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [X] T008 [US1] Refactor ExpenseService.saveExpenseFiles() to use StorageService.putObject() instead of Files.write, update filePath to store object key in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java

**Checkpoint**: New file uploads write to MinIO. Object keys stored in filePath column. No files written to local filesystem.

---

## Phase 4: User Story 2 - Read Files from Object Storage (Priority: P1)

**Goal**: All file reads, moves, and deletes operate on MinIO instead of local filesystem

**Independent Test**: Upload via bulk upload, verify OCR processes correctly from MinIO. Complete job, verify file moves within MinIO. Delete job/expense, verify file removed from MinIO.

- [X] T009 [US2] Change OcrProcessorClient.processReceipt() signature from Path to byte[] parameter in budget-backend/src/main/java/com/homebudget/service/OcrProcessorClient.java
- [X] T010 [US2] Refactor ExpenseInputJobService.processPendingJobs() to read file bytes from StorageService.getObject() and pass byte[] to OcrProcessorClient in budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [X] T011 [US2] Refactor ExpenseService.attachExistingFile() to use StorageService.copyObject() + deleteObject() instead of Files.move() in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [X] T012 [US2] Refactor ExpenseInputJobService.deleteJobs() to use StorageService.deleteObject() instead of Files.deleteIfExists() in budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java
- [X] T013 [US2] Add file deletion from MinIO when expense is deleted (currently only DB cascade, files orphaned) in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java

**Checkpoint**: Full file lifecycle (upload → read → move → delete) works end-to-end with MinIO. No local filesystem operations remain.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and validation

- [X] T014 Remove EXPENSE_FILE_DIR configuration and any remaining java.nio.file imports for file storage from ExpenseService and ExpenseInputJobService
- [X] T015 Verify backend compiles and existing tests pass with ./mvnw compile and ./mvnw test
- [ ] T016 Run quickstart.md integration scenarios manually to validate end-to-end flow

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001 → T002 → T003 → T004 (sequential, each builds on previous)
- **US3 (Phase 2)**: Depends on Setup — Docker Compose changes can run in parallel (T005 ∥ T006)
- **US1 (Phase 3)**: Depends on Setup (StorageService must exist) — T007 ∥ T008
- **US2 (Phase 4)**: Depends on US1 (files must be in MinIO to read/move/delete) — T009 first, then T010-T013
- **Polish (Phase 5)**: Depends on all user stories complete

### Within Each Phase

- **Phase 1**: Sequential (T001 → T002 → T003 → T004) — each depends on previous
- **Phase 2**: T005 and T006 can run in parallel (different files)
- **Phase 3**: T007 and T008 can run in parallel (different files)
- **Phase 4**: T009 first (signature change), then T010-T013 (T010-T012 can be parallel, T013 independent)
- **Phase 5**: Sequential (T014 → T015 → T016)

### Parallel Opportunities

```bash
# Phase 2 parallel:
T005 (docker-compose.yml) ∥ T006 (docker-compose.prod.yml)

# Phase 3 parallel:
T007 (ExpenseInputJobService) ∥ T008 (ExpenseService)

# Phase 4 partial parallel (after T009):
T010 (job processing) ∥ T011 (file move) ∥ T012 (job delete) ∥ T013 (expense delete)
```

---

## Implementation Strategy

### MVP First (User Story 1 — File Writes)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: US3 Docker Compose (T005-T006)
3. Complete Phase 3: US1 File Writes (T007-T008)
4. **STOP and VALIDATE**: Upload files, verify they appear in MinIO

### Incremental Delivery

1. Setup + US3 → MinIO running, StorageService ready
2. Add US1 → File writes go to MinIO (MVP!)
3. Add US2 → File reads, moves, deletes use MinIO (complete migration)
4. Polish → Cleanup, verify build, run integration scenarios

---

## Notes

- No database schema migration needed — filePath column reused for object keys
- Object keys are shorter than absolute paths (e.g., `input-jobs/2026/job-42`)
- Bucket `homebudget-files` is auto-created on backend startup by StorageConfig
- Existing files on local filesystem are NOT migrated (per spec assumptions)
- MinIO web console available at http://localhost:9001 for debugging
