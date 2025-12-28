# Technical Research: Comprehensive Structured Logging

**Feature**: 009-structured-logging
**Date**: 2025-12-28
**Purpose**: Document technical decisions, alternatives considered, and implementation rationale for structured logging feature

## Research Summary

This document captures technical decisions made during Phase 0 research for implementing comprehensive structured logging in the Spring Boot backend. All decisions align with functional requirements from spec.md and technical constraints from the constitution.

---

## Decision 1: Logging Framework Selection

**Decision**: Use Logback with Logstash JSON Encoder

**Rationale**:
- Logback is the default logging implementation for Spring Boot (SLF4J facade)
- Already present in project dependencies (no new framework introduction)
- Logstash Logback Encoder provides production-ready JSON formatting
- Mature ecosystem with extensive Spring Boot integration
- Supports MDC (Mapped Diagnostic Context) for correlation ID tracking
- Asynchronous appenders built-in for performance (FR-027)

**Alternatives Considered**:
1. **Log4j2** - More features but introduces new dependency, migration complexity
2. **Custom JSON serialization** - Reinventing wheel, lack of testing, maintenance burden
3. **Plain text logging with regex parsing** - Fragile, error-prone, violates FR-001 (structured JSON)

**Implementation Notes**:
- Add dependency: `net.logstash.logback:logstash-logback-encoder:7.4`
- Configure in `logback-spring.xml` with JSON encoder
- Use `AsyncAppender` wrapping `ConsoleAppender` for stdout

---

## Decision 2: Correlation ID Strategy

**Decision**: Use servlet filter to generate UUID correlation IDs and store in MDC

**Rationale**:
- Servlet filters execute before controllers, ensuring correlation ID present for all request processing
- MDC (Mapped Diagnostic Context) is thread-local storage standard for logging frameworks
- UUID provides globally unique IDs with negligible collision probability
- Correlation ID automatically included in all logs via MDC reference in Logback pattern
- Complies with FR-002 (unique correlation ID for each request)

**Alternatives Considered**:
1. **Controller-level interceptor** - Too late in request lifecycle, misses filter/security logs
2. **AOP on controllers** - Misses non-controller paths (actuator, error handlers)
3. **Manual correlation ID passing** - Error-prone, doesn't scale, violates DRY principle

**Implementation Notes**:
- Create `CorrelationIdFilter` implementing `Filter` interface
- Check for existing `X-Correlation-ID` header (if provided by client)
- Generate UUID if not present: `UUID.randomUUID().toString()`
- Store in MDC: `MDC.put("correlationId", correlationId)`
- Clear MDC in finally block to prevent thread pool contamination
- Add correlation ID to response headers for client-side correlation

---

## Decision 3: User Context Extraction

**Decision**: Extract X-Hass-User header in same correlation filter and store in MDC

**Rationale**:
- X-Hass-User header is available at servlet filter level
- MDC supports multiple context variables (correlationId + userId)
- Centralizing context extraction in single filter reduces complexity
- Complies with FR-003 (include user identifier in all log entries)
- Aligns with constitution requirement to use X-Hass-User for authentication

**Alternatives Considered**:
1. **Separate user extraction filter** - Unnecessary duplication, ordering dependencies
2. **AOP on service methods** - Misses controller/filter logs, repetitive
3. **ThreadLocal in custom class** - Reinvents MDC, lacks logging framework integration

**Implementation Notes**:
- Extract header: `request.getHeader("X-Hass-User")`
- Store in MDC: `MDC.put("userId", userId != null ? userId : "anonymous")`
- Clear MDC alongside correlation ID in finally block
- Default to "anonymous" if header missing (defensive coding)

---

## Decision 4: Request/Response Logging Approach

**Decision**: Use Spring MVC HandlerInterceptor for request entry/exit logging

**Rationale**:
- HandlerInterceptor provides preHandle (before controller) and afterCompletion (after response)
- Access to HttpServletRequest, HttpServletResponse, and handler information
- Can measure execution duration using timestamps
- Integrates cleanly with Spring MVC request lifecycle
- Supports FR-018 (log request entry) and FR-019 (log request completion)

**Alternatives Considered**:
1. **Servlet Filter** - Too low-level, includes static resources, harder to exclude paths
2. **AOP @Around on controllers** - Misses framework-level request handling, repetitive
3. **Logbook library** - External dependency, opinionated formatting, learning curve

**Implementation Notes**:
- Implement `HandlerInterceptor` interface
- In preHandle: Log HTTP method, path, correlation ID, user ID, timestamp
- Store request start time in request attribute: `request.setAttribute("requestStartTime", System.currentTimeMillis())`
- In afterCompletion: Calculate duration, log status code, duration, correlation ID
- Log at INFO level for normal requests, WARN for slow requests (>1000ms per FR-020)
- Exclude actuator endpoints from logging to reduce noise

---

## Decision 5: Sensitive Data Masking Strategy

**Decision**: Implement field name pattern matching with configurable masking rules

**Rationale**:
- Field name patterns (password, token, secret, apiKey) are reliable indicators
- Regex-based matching handles variations (Password, PASSWORD, user_password)
- Configurable in application.properties for flexibility
- Can be applied to log message strings and structured data
- Meets FR-011 (mask sensitive data using field name pattern matching)

**Alternatives Considered**:
1. **Manual masking at each log site** - Error-prone, easy to forget, doesn't scale
2. **Custom Logback encoder** - Complex, hard to maintain, overkill
3. **No masking** - Security risk, violates SC-008 (100% masking compliance)

**Implementation Notes**:
- Create `SensitiveDataMasker` utility class
- Default patterns: `(password|passwd|pwd|token|secret|apikey|api_key|authorization|auth)\\s*[:=]\\s*[^\\s,}]+`
- Case-insensitive matching
- Replace matched values with "***MASKED***"
- Apply masking in exception logging and debug logging where request bodies are logged
- Configuration: `logging.sensitive-fields=password,token,secret,apiKey,authorization`

---

## Decision 6: Asynchronous Logging Configuration

**Decision**: Use Logback AsyncAppender with bounded queue size of 10,000 entries

**Rationale**:
- Asynchronous logging prevents I/O from blocking application threads
- Bounded queue prevents memory exhaustion under extreme load
- Logback AsyncAppender is mature and well-tested
- Discarding strategy for queue overflow (discard TRACE/DEBUG, keep INFO/WARN/ERROR)
- Meets FR-027 (async logging) and FR-028 (bounded queue)

**Alternatives Considered**:
1. **Synchronous logging only** - Violates FR-027, unacceptable performance impact
2. **Unbounded queue** - Violates FR-028, risk of OutOfMemoryError under load
3. **Disruptor-based logging (LMAX)** - Complex setup, marginal benefit for current scale

**Implementation Notes**:
- Configure AsyncAppender in logback-spring.xml
- Queue size: 10,000 entries (per FR-028)
- Discard threshold: 20% (discard DEBUG/TRACE when queue 80% full)
- Never block: `neverBlock=false` (block instead of dropping ERROR logs)
- Include caller data: `includeCallerData=false` (performance optimization)

---

## Decision 7: JSON Log Format Schema

**Decision**: Use Logstash JSON encoder with custom field names matching observability standards

**Rationale**:
- Logstash encoder produces ECS-compatible (Elastic Common Schema) JSON
- Standard field names enable log aggregation tool compatibility
- Custom fields for correlation ID, user ID via MDC
- Stack traces serialized as structured data (not plain text)
- Meets FR-001 (structured JSON format with consistent schema)

**Alternatives Considered**:
1. **Custom Jackson-based serialization** - Reinventing wheel, testing burden
2. **Plain key-value pairs** - Not JSON, harder to parse
3. **XML logging** - Verbose, outdated, poor tool support

**Implementation Notes**:
- Required fields: `@timestamp`, `level`, `logger_name`, `thread_name`, `message`
- MDC fields: `correlation_id`, `user_id`
- Exception fields: `stack_trace`, `exception_class`, `exception_message`
- Custom fields: `application_name`, `environment`, `version` (from Spring Boot properties)
- Timestamp format: ISO-8601 with timezone (FR-004)

---

## Decision 8: Log Level Configuration

**Decision**: Use Spring Boot profile-based configuration with per-package log levels

**Rationale**:
- Spring Boot natively supports `logging.level.*` properties
- Different profiles for dev (DEBUG) vs prod (INFO)
- Per-package granularity allows targeted debugging
- Environment variables can override for production troubleshooting
- Meets FR-023 (configurable log levels) and FR-024 (per-component levels)

**Alternatives Considered**:
1. **Hard-coded log levels** - Inflexible, violates FR-023
2. **External configuration service** - Overkill, adds complexity
3. **Runtime log level API** - Security risk (unauthorized level changes)

**Implementation Notes**:
- Default: `logging.level.root=INFO`
- Dev profile: `logging.level.com.homebudget=DEBUG`
- Prod profile: `logging.level.com.homebudget=INFO`
- Per-component: `logging.level.com.homebudget.service=DEBUG`
- Environment variable override: `LOGGING_LEVEL_COM_HOMEBUDGET=DEBUG`

---

## Decision 9: Debug Logging for Business Logic

**Decision**: Add explicit debug logging statements in service layer methods with structured context

**Rationale**:
- Business logic lives in service layer (CategoryService, BudgetService, ExpenseService)
- Debug logs should include decision points, intermediate values, validation results
- SLF4J parameterized logging for performance (lazy evaluation when DEBUG disabled)
- Meets FR-013, FR-014, FR-015 (debug logging for complex operations)

**Alternatives Considered**:
1. **AOP-based debug logging** - Generic, loses business context, hard to customize
2. **No debug logging** - Violates FR-013-015, makes debugging production issues impossible
3. **Always-on verbose logging** - Violates FR-017, performance impact

**Implementation Notes**:
- Example: `log.debug("Validating category hierarchy: parentId={}, depth={}, result={}", parentId, depth, validationResult)`
- Log at decision points: before/after validation, before/after calculations
- Include IDs for traceability (categoryId, budgetId, expenseId)
- Use structured arguments (not string concatenation) for performance
- Guard expensive operations: `if (log.isDebugEnabled()) { ... }`

---

## Decision 10: Log Configuration Reload Mechanism

**Decision**: Use Spring Boot Actuator's `/actuator/loggers` endpoint for runtime log level changes

**Rationale**:
- Spring Boot Actuator provides built-in logger management endpoint
- Allows GET to view current levels, POST to update levels
- Changes take effect immediately without application restart
- RESTful API, no custom code needed
- Meets FR-026 (reload configuration without restart)

**Alternatives Considered**:
1. **File watcher on logback.xml** - Complex, potential race conditions
2. **JMX-based management** - Requires JMX setup, harder to automate
3. **Custom API endpoint** - Reinventing wheel, security considerations

**Implementation Notes**:
- Enable actuator: `management.endpoints.web.exposure.include=loggers`
- Secure endpoint: `management.endpoint.loggers.enabled=true`
- View levels: `GET /actuator/loggers/com.homebudget`
- Change level: `POST /actuator/loggers/com.homebudget {"configuredLevel": "DEBUG"}`
- Changes persist for application lifetime (not written to config file)

---

## Decision 11: Performance Logging with AOP

**Decision**: Use Spring AOP @Around advice on service methods to log execution duration

**Rationale**:
- AOP allows cross-cutting performance logging without modifying business logic
- @Around advice can measure method execution time
- Pointcut expressions target service layer methods selectively
- Meets FR-022 (execution duration in operation logs)

**Alternatives Considered**:
1. **Manual timing in each method** - Repetitive, error-prone, clutters business logic
2. **Metrics library (Micrometer)** - Different concern (metrics vs logs), violates requirement for logging
3. **No performance logging** - Violates FR-022, can't identify slow operations

**Implementation Notes**:
- Create `PerformanceLoggingAspect` with @Aspect annotation
- Pointcut: `@Pointcut("execution(* com.homebudget.service..*.*(..))")`
- @Around advice: Record start time, proceed, calculate duration, log if >threshold
- Log at DEBUG level for all methods, WARN level for slow methods (>100ms)
- Include method name, arguments (masked), duration, correlation ID

---

## Decision 12: Exception Logging Strategy

**Decision**: Enhance GlobalExceptionHandler with structured error logging including stack traces

**Rationale**:
- GlobalExceptionHandler already handles all uncaught exceptions
- Centralized location for error logging ensures consistency
- Can add exception class, message, stack trace to logs
- Meets FR-006 (log all exceptions with stack traces)

**Alternatives Considered**:
1. **Try-catch in every method** - Repetitive, violates DRY, easy to miss
2. **AOP @AfterThrowing** - Misses exceptions handled locally, less comprehensive
3. **No exception logging** - Violates FR-006, debugging impossible

**Implementation Notes**:
- In GlobalExceptionHandler methods, add: `log.error("Exception occurred", exception)`
- SLF4J automatically includes stack trace when exception passed as last parameter
- Include request context: correlation ID (from MDC), user ID (from MDC), HTTP method, path
- Different log messages for different exception types (validation, database, business logic)
- Log validation errors at WARN level (FR-007), system errors at ERROR level (FR-006)

---

## Decision 13: Testing Strategy for Logging

**Decision**: Use JUnit 5 with custom Logback TestAppender to capture and verify logs

**Rationale**:
- Logback supports programmatic appender attachment for testing
- TestAppender can capture log events in memory for assertion
- Allows verifying log level, message content, MDC fields, exception presence
- Integration tests can verify end-to-end logging behavior

**Alternatives Considered**:
1. **Manual log file inspection** - Not automated, doesn't scale, fragile
2. **No logging tests** - Can't verify compliance with FR-001 to FR-028
3. **Mocking SLF4J** - Doesn't test Logback configuration, misses JSON formatting

**Implementation Notes**:
- Create `TestLogAppender extends AppenderBase<ILoggingEvent>`
- Store captured events in `List<ILoggingEvent>`
- Attach/detach appender in @BeforeEach/@AfterEach
- Assertions: event.getLevel(), event.getMessage(), event.getMDCPropertyMap(), event.getThrowableProxy()
- Integration tests: Make HTTP request, verify correlation ID in logs, verify user ID in logs

---

## Open Questions & Future Considerations

### Resolved
- ✅ **Correlation ID format**: UUID provides uniqueness without coordination
- ✅ **Async queue size**: 10,000 entries balances memory vs buffering capacity
- ✅ **Sensitive data patterns**: Configurable regex patterns cover common cases
- ✅ **Performance overhead**: Async logging + lazy evaluation keeps impact <5ms

### Future Enhancements (Out of Scope)
- **Distributed tracing integration**: OpenTelemetry/Zipkin integration for microservices (if architecture evolves)
- **Structured exception codes**: Error code taxonomy for automated alerting (FR-012 partially implemented)
- **Log sampling**: Reduce log volume under extreme load (>10,000 req/s)
- **Query performance logging**: Per-query logging for database optimization (FR-021 implemented for slow queries only)

---

## Dependencies & Versioning

### New Maven Dependencies

```xml
<!-- Logstash JSON Encoder for structured logging -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Spring Boot AOP for performance logging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Spring Boot Actuator for log level management -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Existing Dependencies (No Changes)
- SLF4J API: Provided by Spring Boot Starter
- Logback: Default Spring Boot logging implementation
- JUnit 5: Existing test framework

---

## Implementation Checklist

Phase 0 (This Document):
- ✅ Research logging frameworks
- ✅ Define correlation ID strategy
- ✅ Define user context extraction
- ✅ Design request/response logging
- ✅ Design sensitive data masking
- ✅ Design async logging configuration
- ✅ Define JSON schema
- ✅ Define log level configuration
- ✅ Design debug logging approach
- ✅ Define log reload mechanism
- ✅ Design performance logging
- ✅ Design exception logging
- ✅ Define testing strategy

Phase 1 (Next Steps):
- Create data-model.md (logging entities: LogEntry, CorrelationContext, LogConfiguration)
- Create contracts/ (API for log level management endpoint)
- Create quickstart.md (test scenarios for log verification)
- Update agent context with logging technologies

Phase 2 (Future):
- Generate tasks.md via `/speckit.tasks`
