# Feature Specification: Object Storage for File Management

**Feature Branch**: `024-object-storage`
**Created**: 2026-02-17
**Status**: Draft
**Input**: User description: "Work on the file storage feature. All files should be written to an object storage. Create this object storage in the docker compose file, for both prod and dev. When reading file, it should read from the object storage."

## Clarifications

### Session 2026-02-17

- Q: Should the existing filePath column be reused to store the object key, or should a new column be added? → A: Reuse existing filePath column to store the object storage key (e.g., `input-jobs/2026/file.jpg`). No schema migration needed for file references.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Store Uploaded Files in Object Storage (Priority: P1)

When a household member uploads a receipt (via record expense or bulk upload), the file is stored in a centralized object storage service instead of the local filesystem. This ensures files persist across container restarts and are accessible from any service in the system.

**Why this priority**: This is the core migration — all file writes must go to object storage. Without this, files are lost on container restart in production and cannot be shared across services.

**Independent Test**: Upload a receipt via bulk upload or record expense. Verify the file appears in the object storage bucket. Restart the backend container and verify the file is still accessible.

**Acceptance Scenarios**:

1. **Given** a user uploads a receipt via record expense, **When** the upload completes, **Then** the file is stored in object storage (not the local filesystem)
2. **Given** a user uploads files via bulk upload, **When** the jobs are created, **Then** all uploaded files are stored in object storage
3. **Given** the backend container restarts, **When** a user accesses previously uploaded files, **Then** the files are still available from object storage
4. **Given** a file upload fails to reach object storage, **When** the error is detected, **Then** the user receives a clear error message and the upload is marked as failed

---

### User Story 2 - Read Files from Object Storage (Priority: P1)

When the system needs to read a stored file (e.g., sending a receipt to the OCR processor, or moving a file from job staging to expense storage), it reads the file from object storage rather than the local filesystem.

**Why this priority**: Reading is the counterpart to writing — the system must be able to retrieve files from object storage for OCR processing and file management operations.

**Independent Test**: Upload a receipt via bulk upload. Verify that the OCR processor receives the file content correctly from object storage. Complete the job and verify the expense file reference is updated to the new object storage location.

**Acceptance Scenarios**:

1. **Given** a bulk upload job is being processed, **When** the backend sends the file to the OCR processor, **Then** it reads the file content from object storage
2. **Given** a job is completed and temporary records are confirmed, **When** the system moves the file to expense storage, **Then** the file is copied/moved within object storage (not the local filesystem)
3. **Given** a job or expense is deleted, **When** cleanup runs, **Then** the corresponding file is removed from object storage

---

### User Story 3 - Object Storage Available in Dev and Prod Environments (Priority: P1)

The object storage service is defined in the container orchestration for both development and production environments. In development, the storage data persists via a volume so developers don't lose test files between restarts.

**Why this priority**: Both environments must have the object storage service available for the system to function. This is infrastructure that enables all other stories.

**Independent Test**: Start the development environment. Verify the object storage service is running and accessible. Repeat for the production environment configuration.

**Acceptance Scenarios**:

1. **Given** a developer starts the development environment, **When** all services are running, **Then** the object storage service is accessible and ready to accept files
2. **Given** the production environment is deployed, **When** all services start, **Then** the object storage service is running with persistent data storage
3. **Given** the development environment is restarted, **When** the object storage service comes back up, **Then** previously stored files are still available

---

### Edge Cases

- What happens when the object storage service is temporarily unreachable during file upload? The upload should fail with a retryable error.
- What happens when a file is deleted from object storage but still referenced in the database? The system should handle missing files gracefully (log warning, show "file unavailable" to user).
- What happens when the object storage volume runs out of space? The upload should fail with a clear error message.
- What happens to existing files on the local filesystem after migration? Existing files stored on local filesystem before this feature should still be accessible (migration of existing files is out of scope; only new files use object storage).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST store all newly uploaded files (expense receipts, bulk upload files) in object storage instead of the local filesystem
- **FR-002**: System MUST read files from object storage when processing bulk upload jobs (sending to OCR processor)
- **FR-003**: System MUST read files from object storage when moving files from job staging to expense permanent storage
- **FR-004**: System MUST delete files from object storage when jobs or expenses are deleted
- **FR-005**: The object storage service MUST be defined in the container orchestration configuration for both development and production environments
- **FR-006**: Object storage data MUST persist across container restarts in both environments (via persistent volumes)
- **FR-007**: System MUST store the object storage key (path within the bucket) in the existing filePath database column — no new columns or schema migration needed for file references
- **FR-008**: System MUST handle object storage connection failures gracefully with appropriate error messages and retry behavior consistent with existing patterns
- **FR-009**: System MUST organize files in object storage using a logical path structure (e.g., by year and purpose — input-jobs vs expense-files)

### Key Entities

- **Stored File Reference**: The existing filePath column is reused to store the object storage key (path within the bucket, e.g., `input-jobs/2026/filename.jpg`). The bucket name is configured at the application level, not stored per-file.
- **Object Storage Bucket**: A logical container for files in the object storage service. One bucket is used for all application files.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All newly uploaded files are stored in object storage — zero files written to the local filesystem for new uploads
- **SC-002**: Files persist across container restarts — 100% of files are accessible after restarting the backend service
- **SC-003**: Bulk upload OCR processing works end-to-end with files stored in object storage — job processing success rate is unchanged from before migration
- **SC-004**: File upload and retrieval completes within the same time constraints as the current filesystem approach (no noticeable degradation to users)
- **SC-005**: Both development and production environments have the object storage service configured and running as part of the standard deployment

## Assumptions

- The object storage service runs as a container alongside the existing services (database, backend, frontend, OCR processor) in the same Docker network
- A single bucket is sufficient for all application files
- The object storage service provides a standard API compatible with common object storage protocols
- Existing files on the local filesystem from before this feature are out of scope for migration — they can be manually migrated later if needed
- File size limits remain the same (10MB per file) and are enforced at the application level, not the storage level
- No public access to stored files is needed — all access goes through the backend service
