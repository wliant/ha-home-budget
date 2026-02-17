# Implementation Plan: Object Storage for File Management

**Branch**: `024-object-storage` | **Date**: 2026-02-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/024-object-storage/spec.md`

## Summary

Migrate all file storage operations (upload, read, move, delete) from the local filesystem to MinIO object storage (S3-compatible). Add MinIO as a Docker service in both dev and prod compose files. Introduce a `StorageService` that wraps the AWS SDK v2 `S3Client`, and refactor `ExpenseService` and `ExpenseInputJobService` to use it. The existing `filePath` database column is reused to store object keys — no schema migration needed.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend — no changes needed)
**Primary Dependencies**: Spring Boot 3.2.0, AWS SDK for Java v2 (`software.amazon.awssdk:s3`), MinIO (Docker container)
**Storage**: MinIO (S3-compatible object storage), MySQL 8.0 (existing — no schema changes)
**Testing**: Existing test suite (no new tests unless requested)
**Target Platform**: Docker containers on private home network
**Project Type**: Web application (backend + frontend)
**Performance Goals**: File upload/download latency comparable to local filesystem (sub-second for files up to 10MB within Docker network)
**Constraints**: Single bucket, files up to 10MB, private network only (no public access)
**Scale/Scope**: Household use — low volume (dozens of files/day at most)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Frontend uses Next.js | PASS | No frontend changes needed for this feature |
| Backend uses Spring Boot (Java) | PASS | All changes in Spring Boot backend |
| Home Assistant auth (`X-Hass-User`) | PASS | No auth changes; existing header flow unchanged |
| Private home network deployment | PASS | MinIO runs alongside other containers in Docker network |
| Multi-user household support | PASS | Storage is shared; user identity unchanged |
| Specification-first | PASS | spec.md written and clarified before planning |
| Incremental story-based delivery | PASS | 3 user stories, all P1 (infrastructure feature) |

All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/024-object-storage/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (no new API endpoints)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── config/
│   │   └── StorageConfig.java          # NEW: S3Client bean configuration
│   └── service/
│       ├── StorageService.java         # NEW: Object storage operations (put, get, delete, copy)
│       ├── ExpenseService.java         # MODIFY: Use StorageService instead of filesystem
│       └── ExpenseInputJobService.java # MODIFY: Use StorageService instead of filesystem
├── src/main/resources/
│   └── application.yml                 # MODIFY: Add storage config properties
└── pom.xml                             # MODIFY: Add AWS SDK v2 dependency

docker-compose.yml                      # MODIFY: Add minio service + volume (dev)
docker-compose.prod.yml                 # MODIFY: Add minio service + volume (prod)
```

**Structure Decision**: Existing web application structure (budget-backend / budget-frontend). Only backend changes needed — new `StorageConfig` and `StorageService` classes, modifications to existing services, and Docker Compose updates.

## Implementation Approach

### New Files

1. **`StorageConfig.java`** — Spring `@Configuration` class that creates the `S3Client` bean
   - Reads endpoint, access key, secret key, bucket name, region from application properties
   - Configures `pathStyleAccessEnabled(true)` (required for MinIO)
   - Creates bucket on startup if it doesn't exist (`@PostConstruct` or `ApplicationRunner`)

2. **`StorageService.java`** — Spring `@Service` wrapping S3 operations
   - `putObject(String key, byte[] data, String contentType)` — upload file
   - `getObject(String key)` — download file as byte array
   - `deleteObject(String key)` — delete file
   - `copyObject(String sourceKey, String destKey)` — copy file (for move = copy + delete source)
   - `objectExists(String key)` — check if file exists

### Modified Files

3. **`ExpenseService.java`** — Replace all `Files.*` and `Path` operations:
   - `saveExpenseFiles()` → use `StorageService.putObject()` with key pattern `expense-files/{year}/{category-slug}/{expenseId}_{fileId}`
   - `attachExistingFile()` → use `StorageService.copyObject()` + `StorageService.deleteObject()` instead of `Files.move()`
   - Remove `resolveExpenseDir()` and `sanitizeFolderName()` — keys are flat strings, no directory creation needed
   - Keep `sanitizeFolderName()` for key generation (consistent naming)

4. **`ExpenseInputJobService.java`** — Replace all `Files.*` and `Path` operations:
   - `createJobs()` → use `StorageService.putObject()` with key pattern `input-jobs/{year}/job-{jobId}`
   - `processPendingJobs()` → use `StorageService.getObject()` instead of `Files.readAllBytes()`
   - `deleteJobs()` → use `StorageService.deleteObject()` instead of `Files.deleteIfExists()`
   - Remove `resolveJobPath()` — generate key string directly

5. **`OcrProcessorClient.java`** — Change `processReceipt()` signature from `Path filePath` to `byte[] fileBytes` (the service will pass bytes read from storage instead of a filesystem path)

6. **`pom.xml`** — Add AWS SDK v2 BOM + `s3` module dependency

7. **`application.yml`** — Add `app.storage.*` properties (endpoint, access-key, secret-key, bucket-name, region)

8. **`docker-compose.yml`** — Add `minio` service with health check, `minio-data` volume, env vars for backend

9. **`docker-compose.prod.yml`** — Add `minio` service (no default credentials), `minio-data` volume, env vars for backend

### Object Key Structure

```
input-jobs/{year}/job-{jobId}                              # Bulk upload job files
expense-files/{year}/{category-slug}/{expenseId}_{fileId}  # Expense attachment files
```

This mirrors the existing filesystem directory structure but as flat object keys within a single bucket (`homebudget-files`).

### Backward Compatibility

- Existing files on local filesystem (if any) will NOT be migrated — per spec assumption
- The `filePath` column will now store object keys (e.g., `input-jobs/2026/job-42`) instead of absolute paths (e.g., `/app/data/expense-files/input-jobs/2026/job-42`)
- No database schema migration needed

## Complexity Tracking

No violations to justify. The implementation adds one new dependency (AWS SDK v2) and one new Docker service (MinIO), both standard choices for this use case.
