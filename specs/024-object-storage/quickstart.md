# Quickstart: Object Storage for File Management

**Feature**: 024-object-storage
**Date**: 2026-02-17

## Prerequisites

- Docker and Docker Compose installed
- Existing homebudget stack running (mysql, backend, frontend, ocr-processor)

## Setup

1. Start the stack (MinIO will start alongside existing services):
   ```bash
   docker compose up -d
   ```

2. Verify MinIO is running:
   ```bash
   docker compose ps minio
   # Should show "healthy" status
   ```

3. Access MinIO console (dev only):
   - URL: http://localhost:9001
   - Username: `minioadmin`
   - Password: `minioadmin123`

## Integration Test Scenarios

### Scenario 1: Upload via Record Expense

1. Open the app and navigate to record expense
2. Fill in expense details and attach a file (receipt image or PDF)
3. Submit the expense
4. **Verify**: Check MinIO console → `homebudget-files` bucket → `expense-files/{year}/{category}/` → file exists
5. **Verify**: Check database → `expense_files.file_path` contains object key (not filesystem path)

### Scenario 2: Upload via Bulk Upload

1. Open bulk upload dialog
2. Upload one or more receipt files
3. **Verify**: Check MinIO console → `homebudget-files` bucket → `input-jobs/{year}/` → files exist
4. Wait for OCR processing to complete
5. **Verify**: OCR results appear correctly (file was read from object storage)

### Scenario 3: Job Completion (File Move)

1. After OCR processing, review temporary records
2. Confirm/complete the job
3. **Verify**: File moved from `input-jobs/{year}/job-{id}` to `expense-files/{year}/{category}/{eid}_{fid}`
4. **Verify**: Original job file key no longer exists in MinIO

### Scenario 4: Deletion

1. Delete a job (before completion)
2. **Verify**: File removed from `input-jobs/` in MinIO
3. Delete an expense with attachment
4. **Verify**: File removed from `expense-files/` in MinIO

### Scenario 5: Persistence Across Restarts

1. Upload a file (via either method)
2. Restart the backend: `docker compose restart backend`
3. **Verify**: Previously uploaded files are still accessible
4. Restart MinIO: `docker compose restart minio`
5. **Verify**: All files in the bucket are still present (volume persistence)

### Scenario 6: Error Handling

1. Stop MinIO: `docker compose stop minio`
2. Try uploading a file
3. **Verify**: User receives a clear error message (not a stack trace)
4. Start MinIO: `docker compose start minio`
5. Retry upload — should succeed
