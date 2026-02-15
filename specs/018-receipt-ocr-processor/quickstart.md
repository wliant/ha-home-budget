# Quickstart: Receipt OCR Processor

**Feature**: 018-receipt-ocr-processor
**Date**: 2026-02-15

## Prerequisites

- Docker and docker-compose running
- Ollama server accessible at configured address (default: 192.168.1.248:11434)
- Ollama models pulled: `llava:13b` and `llama3.1:latest`

## Integration Scenario 1: Direct OCR API Call (Development Testing)

Test the OCR processor service standalone.

### Steps

1. Start the service:
   ```bash
   docker-compose up ocr-processor
   ```

2. Verify health:
   ```bash
   curl http://localhost:8082/health
   # Expected: {"status": "healthy", "ollama_reachable": true}
   ```

3. Process a receipt:
   ```bash
   curl -X POST http://localhost:8082/process \
     -F "file=@/path/to/receipt.jpg" \
     -F 'categories=[{"id": 1, "name": "Groceries"}, {"id": 2, "name": "Dining"}, {"id": 3, "name": "Transport"}]'
   ```

4. Expected response:
   ```json
   {
     "expenses": [
       {
         "amount": 45.99,
         "description": "Grocery store purchase",
         "expense_date": "2026-02-15",
         "category_id": 1,
         "category_name": "Groceries"
       }
     ]
   }
   ```

## Integration Scenario 2: End-to-End via Backend

Test the full flow through the Spring Boot backend.

### Steps

1. Start all services:
   ```bash
   docker-compose up
   ```

2. Upload a receipt file via the existing bulk upload endpoint:
   ```bash
   curl -X POST http://localhost:8081/api/expense-input-jobs \
     -H "X-Hass-User: testuser" \
     -F "files=@/path/to/receipt.jpg"
   ```

3. Wait for processing (the backend's scheduled job polls every 2 seconds):
   ```bash
   curl http://localhost:8081/api/expense-input-jobs \
     -H "X-Hass-User: testuser"
   ```

4. Expected: Job status transitions from PROCESSING → COMPLETED with one or more `temporaryRecords`.

5. Confirm the expense(s):
   ```bash
   curl -X POST http://localhost:8081/api/expense-input-jobs/confirm \
     -H "X-Hass-User: testuser" \
     -H "Content-Type: application/json" \
     -d '{"jobIds": [1]}'
   ```

## Integration Scenario 3: Error Handling

### Non-retryable error (not a receipt):
```bash
curl -X POST http://localhost:8082/process \
  -F "file=@/path/to/landscape-photo.jpg" \
  -F 'categories=[{"id": 1, "name": "Groceries"}]'
# Expected: HTTP 422, {"error_code": "NOT_A_RECEIPT", "message": "...", "retryable": false}
```

### Retryable error (Ollama down):
```bash
# Stop/disconnect Ollama, then:
curl -X POST http://localhost:8082/process \
  -F "file=@/path/to/receipt.jpg" \
  -F 'categories=[{"id": 1, "name": "Groceries"}]'
# Expected: HTTP 503, {"error_code": "MODEL_SERVER_UNREACHABLE", "message": "...", "retryable": true}
```

## Running Tests

```bash
cd ocr-processor
uv run pytest tests/ -v
```

Note: Tests that call the real Ollama server require the server to be accessible. Tests with mocked responses run offline.
