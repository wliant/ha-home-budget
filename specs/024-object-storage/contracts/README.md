# API Contracts: Object Storage for File Management

No new API endpoints are introduced by this feature. The existing endpoints remain unchanged:

- `POST /api/expenses` — multipart upload (internal storage changes from filesystem to object storage)
- `PUT /api/expenses/{id}` — multipart update (same)
- `POST /api/expense-input-jobs` — bulk upload (same)
- `DELETE /api/expense-input-jobs` — job deletion (same)

The change is entirely internal: file bytes are written to/read from MinIO instead of the local filesystem. Request and response formats are unchanged.
