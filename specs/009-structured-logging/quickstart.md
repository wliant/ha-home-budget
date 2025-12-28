# Quick Start: Comprehensive Structured Logging Testing

**Feature**: 009-structured-logging
**Date**: 2025-12-28
**Purpose**: Quick reference guide for testing logging functionality

## Prerequisites

- Backend application running on http://localhost:8080
- Docker containers running (if using Docker deployment)
- Access to application logs (docker logs, console output, or log file)
- jq installed for JSON log parsing (optional but recommended)

---

## Test Scenario 1: Verify Structured JSON Logging (US1 - P1)

**Objective**: Confirm all logs are emitted in JSON format with required fields

### Steps

1. **Start application** and trigger any API request:
   ```bash
   curl -X GET http://localhost:8080/api/budgets \
     -H "X-Hass-User: alice"
   ```

2. **View logs** (Docker):
   ```bash
   docker logs homebudget-backend --tail 20
   ```

3. **Verify JSON structure** (using jq):
   ```bash
   docker logs homebudget-backend --tail 20 | grep -v "^[[:space:]]*$" | jq '.'
   ```

### Expected Results

Each log entry should be valid JSON with these fields:
```json
{
  "@timestamp": "2025-12-28T10:15:30.123Z",
  "level": "INFO",
  "logger_name": "com.homebudget.*",
  "thread_name": "http-nio-8080-exec-*",
  "message": "...",
  "correlation_id": "<uuid>",
  "user_id": "alice",
  "application_name": "homebudget-backend",
  "environment": "dev|prod"
}
```

### Success Criteria

- ✅ All logs are valid JSON (jq parses without errors)
- ✅ @timestamp field present in ISO-8601 format
- ✅ correlation_id field present (UUID format)
- ✅ user_id field present with value "alice"
- ✅ level field is one of: TRACE, DEBUG, INFO, WARN, ERROR

---

## Test Scenario 2: Verify Correlation ID Tracking (US1 - P1)

**Objective**: Confirm all logs for a single request share the same correlation ID

### Steps

1. **Trigger API request**:
   ```bash
   curl -X POST http://localhost:8080/api/budgets \
     -H "X-Hass-User: bob" \
     -H "Content-Type: application/json" \
     -d '{
       "category": "Food",
       "year": 2025,
       "month": 1,
       "amount": 500.00
     }'
   ```

2. **Extract logs** for this request (filter by user):
   ```bash
   docker logs homebudget-backend | grep '"user_id":"bob"' | jq '{correlation_id, level, message}'
   ```

3. **Count unique correlation IDs**:
   ```bash
   docker logs homebudget-backend | grep '"user_id":"bob"' | jq -r '.correlation_id' | sort | uniq
   ```

### Expected Results

All log entries for the single request should have the same correlation ID:
```
<same-uuid>
<same-uuid>
<same-uuid>
```

### Success Criteria

- ✅ All logs for one request share single correlation ID
- ✅ Correlation ID is UUID format (e.g., "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
- ✅ Different requests have different correlation IDs

---

## Test Scenario 3: Verify Error Logging with Stack Traces (US2 - P1)

**Objective**: Confirm exceptions are logged with full stack traces

### Steps

1. **Trigger validation error** (negative amount):
   ```bash
   curl -X POST http://localhost:8080/api/expenses \
     -H "X-Hass-User: alice" \
     -H "Content-Type: application/json" \
     -d '{
       "budgetId": 1,
       "amount": -50.00,
       "description": "Invalid expense",
       "date": "2025-01-15"
     }'
   ```

2. **Find error log** entry:
   ```bash
   docker logs homebudget-backend | grep '"level":"ERROR"' | tail -1 | jq '.'
   ```

### Expected Results

Error log should contain:
```json
{
  "@timestamp": "...",
  "level": "ERROR",
  "logger_name": "com.homebudget.exception.GlobalExceptionHandler",
  "message": "Validation error: ...",
  "correlation_id": "<uuid>",
  "user_id": "alice",
  "exception_class": "javax.validation.ConstraintViolationException",
  "exception_message": "amount: must be greater than 0",
  "stack_trace": [
    "at com.homebudget.service.ExpenseService.createExpense(...)",
    "at com.homebudget.controller.ExpenseController.create(...)",
    "..."
  ]
}
```

### Success Criteria

- ✅ level field is "ERROR"
- ✅ exception_class field present with exception type
- ✅ exception_message field present
- ✅ stack_trace field present as array of strings
- ✅ Stack trace includes method names and line numbers

---

## Test Scenario 4: Verify Debug Logging for Business Logic (US3 - P2)

**Objective**: Confirm debug logs show decision points in complex operations

### Steps

1. **Enable DEBUG logging** for service package:
   ```bash
   curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "DEBUG"}'
   ```

2. **Trigger complex operation** (category hierarchy validation):
   ```bash
   curl -X POST http://localhost:8080/api/categories \
     -H "X-Hass-User: alice" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Groceries",
       "parentId": 1
     }'
   ```

3. **View debug logs**:
   ```bash
   docker logs homebudget-backend | grep '"level":"DEBUG"' | grep CategoryService | jq '.message'
   ```

4. **Disable DEBUG logging**:
   ```bash
   curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": null}'
   ```

### Expected Results

DEBUG logs should show validation steps:
```
"Validating category hierarchy: parentId=1, depth=1"
"Parent category lookup: id=1, name=Food, exists=true"
"Circular reference check: parent=1, child=2, result=false"
"Depth validation: current=1, max=2, result=true"
"Category hierarchy validation completed: valid=true"
```

### Success Criteria

- ✅ DEBUG logs appear when level is DEBUG
- ✅ DEBUG logs include decision points and intermediate values
- ✅ DEBUG logs disappear when level is reset to INFO
- ✅ DEBUG logs include correlation_id and user_id

---

## Test Scenario 5: Verify Request/Response Logging (US4 - P2)

**Objective**: Confirm API requests are logged with duration

### Steps

1. **Trigger API request**:
   ```bash
   curl -X GET http://localhost:8080/api/budgets \
     -H "X-Hass-User: alice"
   ```

2. **Find request start log**:
   ```bash
   docker logs homebudget-backend | grep '"message":"Request started"' | tail -1 | jq '{timestamp: ."@timestamp", http_method, http_path, correlation_id}'
   ```

3. **Find request completion log**:
   ```bash
   docker logs homebudget-backend | grep '"message":"Request completed"' | tail -1 | jq '{timestamp: ."@timestamp", http_status, duration_ms, correlation_id}'
   ```

### Expected Results

Request start log:
```json
{
  "timestamp": "2025-12-28T10:15:30.100Z",
  "http_method": "GET",
  "http_path": "/api/budgets",
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Request completion log:
```json
{
  "timestamp": "2025-12-28T10:15:30.250Z",
  "http_status": 200,
  "duration_ms": 150,
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Success Criteria

- ✅ Request start and completion logs have same correlation_id
- ✅ duration_ms field present in completion log
- ✅ http_status field present in completion log
- ✅ Timestamps show request start before completion

---

## Test Scenario 6: Verify Slow Request Detection (US4 - P2)

**Objective**: Confirm slow requests are logged with WARN level

### Steps

1. **Trigger slow operation** (create many budgets in loop):
   ```bash
   for i in {1..10}; do
     curl -X POST http://localhost:8080/api/budgets \
       -H "X-Hass-User: alice" \
       -H "Content-Type: application/json" \
       -d "{\"category\": \"Category$i\", \"year\": 2025, \"month\": $i, \"amount\": 500.00}" &
   done
   wait
   ```

2. **Check for WARN level slow request logs**:
   ```bash
   docker logs homebudget-backend | grep '"level":"WARN"' | grep '"message":"Slow request"' | jq '{duration_ms, http_path}'
   ```

### Expected Results

If any request exceeds 1000ms threshold:
```json
{
  "duration_ms": 1234,
  "http_path": "/api/budgets"
}
```

### Success Criteria

- ✅ Requests >1000ms logged with level "WARN"
- ✅ Slow request log includes duration_ms field
- ✅ Slow request log includes http_path field

---

## Test Scenario 7: Verify Sensitive Data Masking (US2 - P1)

**Objective**: Confirm passwords and tokens are masked in logs

### Steps

1. **Trigger operation with sensitive data** (simulated - use existing endpoint):
   ```bash
   # Note: Actual implementation may not have password fields,
   # this tests the masking framework if sensitive data appears in logs
   ```

2. **Search logs for masked values**:
   ```bash
   docker logs homebudget-backend | grep -i "password\|token\|secret" | grep "MASKED"
   ```

### Expected Results

If sensitive field appears in log:
```
"User authentication: username=alice, password=***MASKED***"
"API call with token=***MASKED***"
```

### Success Criteria

- ✅ No plain-text passwords in logs
- ✅ No plain-text tokens in logs
- ✅ Sensitive values replaced with "***MASKED***"

---

## Test Scenario 8: Verify Log Level Management (US5 - P3)

**Objective**: Confirm log levels can be changed without restart

### Steps

1. **View current log levels**:
   ```bash
   curl http://localhost:8080/actuator/loggers/com.homebudget | jq '.'
   ```

2. **Change to DEBUG**:
   ```bash
   curl -X POST http://localhost:8080/actuator/loggers/com.homebudget \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "DEBUG"}'
   ```

3. **Verify change** (no restart):
   ```bash
   curl http://localhost:8080/actuator/loggers/com.homebudget | jq '.'
   ```

4. **Trigger request** and verify DEBUG logs appear:
   ```bash
   curl -X GET http://localhost:8080/api/budgets -H "X-Hass-User: alice"
   docker logs homebudget-backend | grep '"level":"DEBUG"' | tail -5
   ```

5. **Reset to INFO**:
   ```bash
   curl -X POST http://localhost:8080/actuator/loggers/com.homebudget \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "INFO"}'
   ```

### Expected Results

Before change:
```json
{
  "configuredLevel": "INFO",
  "effectiveLevel": "INFO"
}
```

After change to DEBUG:
```json
{
  "configuredLevel": "DEBUG",
  "effectiveLevel": "DEBUG"
}
```

DEBUG logs appear after level change without restart.

### Success Criteria

- ✅ Log level changes immediately without application restart
- ✅ DEBUG logs appear when level is DEBUG
- ✅ DEBUG logs disappear when level reverts to INFO
- ✅ Level changes via REST API (no config file edits)

---

## Test Scenario 9: Verify User Context in Logs (US1 - P1)

**Objective**: Confirm logs include user identifier from X-Hass-User header

### Steps

1. **Trigger request as user "alice"**:
   ```bash
   curl -X POST http://localhost:8080/api/expenses \
     -H "X-Hass-User: alice" \
     -H "Content-Type: application/json" \
     -d '{
       "budgetId": 1,
       "amount": 25.50,
       "description": "Test expense",
       "date": "2025-01-15"
     }'
   ```

2. **Trigger request as user "bob"**:
   ```bash
   curl -X POST http://localhost:8080/api/expenses \
     -H "X-Hass-User: bob" \
     -H "Content-Type: application/json" \
     -d '{
       "budgetId": 1,
       "amount": 30.00,
       "description": "Bob expense",
       "date": "2025-01-15"
     }'
   ```

3. **Filter logs by user**:
   ```bash
   docker logs homebudget-backend | jq 'select(.user_id == "alice") | {timestamp: ."@timestamp", user_id, message}' | tail -5
   docker logs homebudget-backend | jq 'select(.user_id == "bob") | {timestamp: ."@timestamp", user_id, message}' | tail -5
   ```

### Expected Results

Alice's logs:
```json
{
  "timestamp": "2025-12-28T10:15:30.123Z",
  "user_id": "alice",
  "message": "Request started"
}
```

Bob's logs:
```json
{
  "timestamp": "2025-12-28T10:16:45.456Z",
  "user_id": "bob",
  "message": "Request started"
}
```

### Success Criteria

- ✅ All logs include user_id field
- ✅ user_id matches X-Hass-User header value
- ✅ Logs can be filtered by user_id
- ✅ Different users' logs are distinguishable

---

## Test Scenario 10: Verify Performance Logging (US4 - P2)

**Objective**: Confirm method execution duration is logged for service methods

### Steps

1. **Enable DEBUG logging**:
   ```bash
   curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.aspect \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "DEBUG"}'
   ```

2. **Trigger service method**:
   ```bash
   curl -X POST http://localhost:8080/api/budgets \
     -H "X-Hass-User: alice" \
     -H "Content-Type: application/json" \
     -d '{
       "category": "Food",
       "year": 2025,
       "month": 1,
       "amount": 500.00
     }'
   ```

3. **View performance logs**:
   ```bash
   docker logs homebudget-backend | grep PerformanceLoggingAspect | jq '{method_name, duration_ms}'
   ```

### Expected Results

```json
{
  "method_name": "com.homebudget.service.BudgetService.createBudget",
  "duration_ms": 45
}
```

### Success Criteria

- ✅ Performance logs include method_name field
- ✅ Performance logs include duration_ms field
- ✅ duration_ms is a positive integer
- ✅ Method name is fully qualified

---

## Quick Reference Commands

### View Logs (Docker)
```bash
# Tail latest logs
docker logs homebudget-backend --tail 50 --follow

# View all logs
docker logs homebudget-backend

# Filter by level
docker logs homebudget-backend | jq 'select(.level == "ERROR")'
```

### Log Level Management
```bash
# Get all loggers
curl http://localhost:8080/actuator/loggers | jq '.loggers | keys'

# Set DEBUG for specific package
curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Reset to inherited level
curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": null}'
```

### Log Analysis
```bash
# Count logs by level
docker logs homebudget-backend | jq -r '.level' | sort | uniq -c

# Find slow requests
docker logs homebudget-backend | jq 'select(.duration_ms > 1000)'

# Find all errors
docker logs homebudget-backend | jq 'select(.level == "ERROR") | {timestamp: ."@timestamp", message, exception_class}'

# Track single request by correlation ID
CORRELATION_ID="<uuid-from-logs>"
docker logs homebudget-backend | jq "select(.correlation_id == \"$CORRELATION_ID\")"
```

---

## Troubleshooting

### Issue: Logs not in JSON format

**Solution**: Check logback-spring.xml configuration, ensure Logstash encoder is configured

### Issue: correlation_id missing from logs

**Solution**: Verify CorrelationIdFilter is registered, check filter order in configuration

### Issue: user_id always "anonymous"

**Solution**: Verify X-Hass-User header is sent with requests, check filter extracts header

### Issue: No DEBUG logs despite setting level to DEBUG

**Solution**: Check effective log level, verify parent logger level, clear MDC contamination

### Issue: Stack traces not appearing in error logs

**Solution**: Verify exception is passed as last parameter to log.error(), check Logback configuration

---

## Summary

This quickstart guide covers 10 test scenarios validating:
- ✅ Structured JSON logging (US1)
- ✅ Correlation ID tracking (US1)
- ✅ Error logging with stack traces (US2)
- ✅ Debug logging for business logic (US3)
- ✅ Request/response logging (US4)
- ✅ Slow request detection (US4)
- ✅ Sensitive data masking (US2)
- ✅ Log level management (US5)
- ✅ User context tracking (US1)
- ✅ Performance logging (US4)

All test scenarios map to functional requirements (FR-001 through FR-028) and success criteria (SC-001 through SC-012).
