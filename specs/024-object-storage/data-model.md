# Data Model: Object Storage for File Management

**Feature**: 024-object-storage
**Date**: 2026-02-17

## Overview

No database schema changes are required. The existing `file_path` column in both `expense_files` and `expense_input_jobs` tables is reused to store object storage keys instead of local filesystem paths.

## Existing Entities (No Changes)

### ExpenseFile

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGINT | NO | Primary key (auto-increment) |
| expense_id | BIGINT | NO | FK to expenses(id), CASCADE DELETE |
| file_path | VARCHAR(500) | NO | **Now stores object key** (e.g., `expense-files/2026/groceries/42_7`) |
| original_filename | VARCHAR(255) | NO | Original user-facing filename |

### ExpenseInputJob

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | BIGINT | NO | Primary key (auto-increment) |
| file_path | VARCHAR(500) | NO | **Now stores object key** (e.g., `input-jobs/2026/job-15`) |
| original_filename | VARCHAR(255) | NO | Original user-facing filename |
| status | ENUM | NO | UPLOADED, PROCESSING, RETRYABLE, FAILED, PROCESSED, COMPLETED |
| ... | ... | ... | Other columns unchanged |

## Object Key Patterns

All keys are within a single bucket: `homebudget-files`

| Purpose | Key Pattern | Example |
|---------|-------------|---------|
| Bulk upload job file | `input-jobs/{year}/job-{jobId}` | `input-jobs/2026/job-15` |
| Expense attachment | `expense-files/{year}/{category-slug}/{expenseId}_{fileId}` | `expense-files/2026/groceries/42_7` |

## New Configuration Entity (Application-Level)

| Property | Value | Description |
|----------|-------|-------------|
| `app.storage.endpoint` | `http://minio:9000` | MinIO API endpoint |
| `app.storage.access-key` | (env var) | MinIO access key |
| `app.storage.secret-key` | (env var) | MinIO secret key |
| `app.storage.bucket-name` | `homebudget-files` | Single bucket for all files |
| `app.storage.region` | `us-east-1` | Required by SDK, ignored by MinIO |

## File Lifecycle

1. **Upload (job)**: File bytes → `StorageService.putObject("input-jobs/2026/job-{id}", bytes)` → key stored in `expense_input_jobs.file_path`
2. **Upload (expense)**: File bytes → `StorageService.putObject("expense-files/2026/{cat}/{eid}_{fid}", bytes)` → key stored in `expense_files.file_path`
3. **Read (OCR)**: `StorageService.getObject(job.getFilePath())` → byte array → sent to OCR processor
4. **Move (job→expense)**: `StorageService.copyObject(jobKey, expenseKey)` + `StorageService.deleteObject(jobKey)`
5. **Delete**: `StorageService.deleteObject(key)` when job/expense is deleted
