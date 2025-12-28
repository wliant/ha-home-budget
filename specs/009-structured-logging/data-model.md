# Data Model: Comprehensive Structured Logging

**Feature**: 009-structured-logging
**Date**: 2025-12-28
**Purpose**: Define logical entities and their relationships for structured logging feature

## Overview

This feature introduces logging infrastructure entities that support correlation tracking, user context, and configurable log management. These are runtime/configuration entities, not database-persisted entities.

---

## Entity 1: LogEntry

**Description**: A single structured log record emitted by the application

**Attributes**:
- `timestamp`: ISO-8601 formatted timestamp with timezone (e.g., "2025-12-28T10:15:30.123Z")
- `level`: Log severity level (TRACE, DEBUG, INFO, WARN, ERROR)
- `logger_name`: Fully qualified class name of the logger (e.g., "com.homebudget.service.BudgetService")
- `thread_name`: Name of thread that generated log entry (e.g., "http-nio-8080-exec-1")
- `message`: Human-readable log message
- `correlation_id`: Unique identifier for the request (UUID format)
- `user_id`: User identifier from X-Hass-User header ("alice", "bob", or "anonymous")
- `application_name`: Name of the application ("homebudget-backend")
- `environment`: Deployment environment ("dev", "prod")
- `version`: Application version from build metadata
- `exception_class`: (Optional) Fully qualified exception class name
- `exception_message`: (Optional) Exception message
- `stack_trace`: (Optional) Full exception stack trace as array of strings
- `method_name`: (Optional) Method name for performance logs
- `duration_ms`: (Optional) Execution duration in milliseconds
- `http_method`: (Optional) HTTP method for request logs (GET, POST, PUT, DELETE)
- `http_path`: (Optional) Request path for request logs ("/api/budgets")
- `http_status`: (Optional) HTTP status code for response logs (200, 404, 500)

**Relationships**:
- Multiple LogEntry instances share same `correlation_id` (one request generates many logs)
- Multiple LogEntry instances share same `user_id` (one user generates many logs)

**Validation Rules**:
- `timestamp` MUST be in ISO-8601 format with timezone
- `level` MUST be one of: TRACE, DEBUG, INFO, WARN, ERROR
- `correlation_id` MUST be present for all request-scoped logs
- `user_id` MUST be present for all user action logs
- `stack_trace` MUST be present when `level` is ERROR and exception occurred
- `duration_ms` MUST be non-negative integer

**State Transitions**: N/A (immutable once written)

**Examples**:

```json
{
  "@timestamp": "2025-12-28T10:15:30.123Z",
  "level": "INFO",
  "logger_name": "com.homebudget.filter.CorrelationIdFilter",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Request started",
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "alice",
  "application_name": "homebudget-backend",
  "environment": "prod",
  "version": "1.0.0",
  "http_method": "POST",
  "http_path": "/api/expenses"
}
```

```json
{
  "@timestamp": "2025-12-28T10:15:30.456Z",
  "level": "ERROR",
  "logger_name": "com.homebudget.exception.GlobalExceptionHandler",
  "thread_name": "http-nio-8080-exec-1",
  "message": "Validation error: Expense amount must be positive",
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "alice",
  "application_name": "homebudget-backend",
  "environment": "prod",
  "version": "1.0.0",
  "exception_class": "javax.validation.ConstraintViolationException",
  "exception_message": "amount: must be greater than 0",
  "stack_trace": [
    "at com.homebudget.service.ExpenseService.createExpense(ExpenseService.java:42)",
    "at com.homebudget.controller.ExpenseController.create(ExpenseController.java:28)"
  ]
}
```

---

## Entity 2: CorrelationContext

**Description**: Request-scoped context object containing correlation ID and user metadata

**Attributes**:
- `correlation_id`: Unique identifier for the current request (UUID)
- `user_id`: User identifier from X-Hass-User header
- `request_start_time`: Timestamp when request was received (milliseconds since epoch)
- `http_method`: HTTP method (GET, POST, PUT, DELETE)
- `http_path`: Request path ("/api/budgets")
- `http_query`: Query string parameters (optional)
- `remote_address`: Client IP address (optional, for troubleshooting)

**Relationships**:
- One CorrelationContext per HTTP request
- CorrelationContext values populate LogEntry fields via MDC

**Validation Rules**:
- `correlation_id` MUST be UUID format or alphanumeric (if provided by client)
- `user_id` MAY be null (defaults to "anonymous")
- `request_start_time` MUST be valid timestamp

**State Transitions**:
1. Created in `CorrelationIdFilter.doFilter()` at request start
2. Stored in MDC for thread-local access
3. Cleared in finally block at request end to prevent thread pool contamination

**Lifecycle**: Request-scoped (thread-local via MDC)

**Example**:

```java
CorrelationContext context = new CorrelationContext(
    correlationId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    userId: "alice",
    requestStartTime: 1703772930123L,
    httpMethod: "POST",
    httpPath: "/api/expenses",
    httpQuery: "budgetId=123",
    remoteAddress: "192.168.1.100"
);
```

---

## Entity 3: LogConfiguration

**Description**: Application configuration for logging behavior

**Attributes**:
- `root_level`: Global default log level (INFO, DEBUG, ERROR, WARN, TRACE)
- `package_levels`: Map of package names to log levels (e.g., "com.homebudget.service" → DEBUG)
- `async_queue_size`: Maximum size of async log queue (default: 10000)
- `sensitive_field_patterns`: List of regex patterns for sensitive data masking
- `json_enabled`: Boolean flag to enable/disable JSON formatting (default: true)
- `include_stack_trace`: Boolean flag to include stack traces in error logs (default: true)
- `slow_request_threshold_ms`: Threshold for logging slow requests (default: 1000)
- `slow_query_threshold_ms`: Threshold for logging slow database queries (default: 100)

**Relationships**:
- Single global LogConfiguration per application instance
- Read from `application.properties` or environment variables

**Validation Rules**:
- `root_level` MUST be one of: TRACE, DEBUG, INFO, WARN, ERROR
- `async_queue_size` MUST be between 1000 and 50000 (bounded queue)
- `sensitive_field_patterns` MUST be valid regex patterns
- `slow_request_threshold_ms` MUST be positive integer
- `slow_query_threshold_ms` MUST be positive integer

**State Transitions**:
1. Loaded at application startup from configuration files
2. Can be updated via `/actuator/loggers` endpoint at runtime (per-package levels only)
3. Updated values effective immediately without restart

**Default Configuration**:

```properties
logging.level.root=INFO
logging.level.com.homebudget=INFO
logging.async.queue-size=10000
logging.sensitive-fields=password,token,secret,apiKey,authorization
logging.json.enabled=true
logging.stack-trace.enabled=true
logging.slow-request.threshold-ms=1000
logging.slow-query.threshold-ms=100
```

**Example (Runtime)**:

```json
{
  "root_level": "INFO",
  "package_levels": {
    "com.homebudget": "INFO",
    "com.homebudget.service": "DEBUG",
    "com.homebudget.repository": "DEBUG"
  },
  "async_queue_size": 10000,
  "sensitive_field_patterns": ["password", "token", "secret", "apiKey", "authorization"],
  "json_enabled": true,
  "include_stack_trace": true,
  "slow_request_threshold_ms": 1000,
  "slow_query_threshold_ms": 100
}
```

---

## Entity 4: SensitiveDataMask

**Description**: Represents a masked sensitive value in logs

**Attributes**:
- `field_name`: Name of the sensitive field (e.g., "password", "token")
- `masked_value`: Masked representation (e.g., "***MASKED***")
- `original_length`: Length of original value (for debugging, not the value itself)

**Relationships**:
- Applied to LogEntry `message` field before log emission
- Configured via LogConfiguration `sensitive_field_patterns`

**Validation Rules**:
- `field_name` MUST match one of the configured sensitive field patterns
- `masked_value` MUST NOT contain original sensitive data
- `original_length` MAY be included for debugging (e.g., "***MASKED(8)***" for 8-character password)

**Masking Algorithm**:
1. Scan log message for field name patterns (case-insensitive)
2. If pattern matches sensitive field, extract value portion
3. Replace value with masked string
4. Optionally include original length

**Example**:

Before masking:
```
"User login attempt with password=MySecret123 and token=abc-def-ghi-jkl"
```

After masking:
```
"User login attempt with password=***MASKED*** and token=***MASKED***"
```

---

## Entity 5: PerformanceMetric

**Description**: Execution performance data included in logs

**Attributes**:
- `method_name`: Fully qualified method name (e.g., "com.homebudget.service.BudgetService.calculateTotal")
- `duration_ms`: Execution duration in milliseconds
- `arguments`: Method arguments (sensitive data masked)
- `return_type`: Return type of method (optional)
- `exception_thrown`: Boolean flag indicating if exception occurred

**Relationships**:
- Captured by `PerformanceLoggingAspect` for service methods
- Included in LogEntry as additional fields

**Validation Rules**:
- `duration_ms` MUST be non-negative
- `arguments` MUST have sensitive data masked before logging
- Logged at DEBUG level for all methods, WARN level if duration > threshold

**Example**:

```json
{
  "@timestamp": "2025-12-28T10:15:30.789Z",
  "level": "DEBUG",
  "logger_name": "com.homebudget.aspect.PerformanceLoggingAspect",
  "message": "Method execution completed",
  "correlation_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "alice",
  "method_name": "com.homebudget.service.BudgetService.calculateCategoryTotal",
  "duration_ms": 45,
  "arguments": "{categoryId=123, month=2025-01}",
  "exception_thrown": false
}
```

---

## Cross-Entity Relationships

### Request Lifecycle Flow

```
1. CorrelationIdFilter creates CorrelationContext
   ├─ Generates/extracts correlation_id
   ├─ Extracts user_id from X-Hass-User header
   ├─ Stores in MDC (thread-local)
   └─ Records request_start_time

2. HandlerInterceptor logs request start
   └─ Creates LogEntry with correlation_id, user_id from MDC

3. Service method executes
   ├─ PerformanceLoggingAspect captures execution metrics
   ├─ Service logs debug statements
   └─ All LogEntry instances include correlation_id, user_id from MDC

4. Exception occurs (if any)
   ├─ GlobalExceptionHandler logs error
   └─ Creates LogEntry with stack_trace and exception details

5. HandlerInterceptor logs request completion
   ├─ Calculates duration from request_start_time
   └─ Creates LogEntry with http_status, duration_ms

6. CorrelationIdFilter cleans up
   └─ Clears MDC to prevent thread pool contamination
```

### Data Flow Diagram

```
HTTP Request → CorrelationIdFilter
                ├─ Generate correlation_id
                ├─ Extract user_id (X-Hass-User)
                └─ Store in MDC

MDC (Thread-Local) ─┬─→ LogEntry (request start)
                    ├─→ LogEntry (service debug logs)
                    ├─→ LogEntry (performance metrics)
                    ├─→ LogEntry (exception logs)
                    └─→ LogEntry (request completion)

All LogEntry instances → Logback → JSON Formatter → Console (stdout)
                                      └─ Apply SensitiveDataMask
```

---

## Configuration Relationships

```
LogConfiguration (application.properties)
   ├─ root_level → Controls global log filtering
   ├─ package_levels → Controls per-package log filtering
   ├─ sensitive_field_patterns → Used by SensitiveDataMasker
   ├─ async_queue_size → Configures AsyncAppender buffer
   └─ slow_request_threshold_ms → Controls WARN level for slow requests
```

---

## Summary

This data model defines 5 logical entities:

1. **LogEntry**: The core structured log record (persisted to stdout)
2. **CorrelationContext**: Request-scoped metadata (runtime only, thread-local)
3. **LogConfiguration**: Application logging configuration (loaded from config files)
4. **SensitiveDataMask**: Masked sensitive data representation (applied during logging)
5. **PerformanceMetric**: Method execution performance data (captured by AOP)

All entities work together to provide comprehensive structured logging with correlation tracking, user context, sensitive data protection, and performance monitoring.
