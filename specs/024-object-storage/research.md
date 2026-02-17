# Research: Object Storage for File Management

**Feature**: 024-object-storage
**Date**: 2026-02-17

## R1: Object Storage Technology Choice

**Decision**: MinIO (self-hosted, S3-compatible object storage)

**Rationale**:
- S3-compatible API — industry standard, well-documented
- Runs as a single Docker container — simple deployment for home network
- Persistent storage via Docker volumes
- Includes web console for debugging (port 9001)
- Production-grade, widely used in self-hosted environments

**Alternatives Considered**:
- **SeaweedFS**: More complex setup, designed for distributed systems — overkill for household scale
- **Local filesystem with volumes**: Current approach; files tied to container lifecycle, no standard API
- **Cloud S3 (AWS/GCP)**: Requires internet access, ongoing costs — violates private home network constraint

## R2: Java SDK for S3-Compatible Storage

**Decision**: AWS SDK for Java v2 (`software.amazon.awssdk:s3`)

**Rationale**:
- De facto standard for S3 API interaction
- AWS SDK v1 (`com.amazonaws`) reached end-of-support December 2025 — must use v2
- Works with MinIO via `endpointOverride()` + `pathStyleAccessEnabled(true)`
- Zero code changes needed if ever migrating to real AWS S3
- Thread-safe `S3Client` — suitable as singleton Spring bean

**Alternatives Considered**:
- **MinIO Java SDK** (`io.minio:minio`): Different API surface (not standard S3), smaller ecosystem, would lock into MinIO-specific patterns
- **Spring Cloud AWS**: Heavier dependency, more auto-configuration than needed for simple use case

## R3: File Path Column Reuse

**Decision**: Reuse existing `filePath` VARCHAR(500) column to store object keys

**Rationale**:
- No database schema migration needed
- Object keys are shorter than absolute filesystem paths (e.g., `input-jobs/2026/job-42` vs `/app/data/expense-files/input-jobs/2026/job-42`)
- Bucket name is application-level config, not stored per-file
- Simplifies implementation — fewer moving parts

**Alternatives Considered**:
- **New `objectKey` column + migration**: More explicit separation but adds unnecessary migration complexity for a column that serves the same purpose

## R4: Docker Compose MinIO Configuration

**Decision**: Add MinIO service to both `docker-compose.yml` (dev) and `docker-compose.prod.yml` (prod)

**Rationale**:
- Both environments need object storage for the backend to function
- Dev uses default credentials (`minioadmin`/`minioadmin123`) for ease of setup
- Prod requires credentials via environment variables (no defaults)
- Both use named Docker volumes (`minio-data`) for persistence across restarts
- Health check via `curl http://localhost:9000/minio/health/live`

## R5: Backend Architecture Pattern

**Decision**: `StorageConfig` (configuration) + `StorageService` (service layer)

**Rationale**:
- `StorageConfig` as `@Configuration` creates `S3Client` bean and ensures bucket exists on startup
- `StorageService` as `@Service` provides clean API (`putObject`, `getObject`, `deleteObject`, `copyObject`) hiding SDK complexity
- `ExpenseService` and `ExpenseInputJobService` depend on `StorageService` — single point of change for storage operations
- `OcrProcessorClient.processReceipt()` changes from `Path` to `byte[]` parameter — the calling service reads bytes from storage and passes them

**Alternatives Considered**:
- **Direct S3Client usage in each service**: Duplicates boilerplate (request building, error handling) across services
- **Spring Content / Spring Resource abstraction**: Over-abstraction for a simple use case with 4-5 operations
