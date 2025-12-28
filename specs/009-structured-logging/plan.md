# Implementation Plan: Comprehensive Structured Logging

**Branch**: `009-structured-logging` | **Date**: 2025-12-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-structured-logging/spec.md`

## Summary

This feature implements comprehensive structured logging for the Spring Boot backend to improve observability and debugging capabilities. The implementation will add JSON-formatted logging with correlation IDs, detailed error context, debug logging for complex business logic, request/response tracing, and configurable log levels. The primary technical approach uses Logback with Logstash JSON encoder for structured output, Spring Boot's MDC (Mapped Diagnostic Context) for correlation tracking, and AOP (Aspect-Oriented Programming) for request interceptors.

## Technical Context

**Language/Version**: Java 17 (existing Spring Boot backend)
**Primary Dependencies**: Logback (SLF4J implementation), Logstash Logback Encoder for JSON formatting, Spring Boot AOP for request interception
**Storage**: N/A (logs written to stdout, consumed by external aggregation)
**Testing**: JUnit 5, Spring Boot Test, Logback test appenders for log verification
**Target Platform**: Docker containers (Spring Boot backend container)
**Project Type**: Web application (Spring Boot backend)
**Performance Goals**: <5ms logging overhead per API request at INFO level
**Constraints**: Async logging queue <10,000 entries, log entries <10KB, 100% sensitive data masking compliance
**Scale/Scope**: All existing backend endpoints (budgets, categories, expenses, dashboard) + global exception handling

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ I. Specification-First

**Status**: PASS

The specification (spec.md) describes logging requirements from user/operator perspective without prescribing implementation details. Specific technologies (Logback, Logstash encoder) are mentioned only in Assumptions section as context, not as requirements.

### ✅ II. Clarify Before Planning

**Status**: PASS

The specification has no [NEEDS CLARIFICATION] markers. All requirements are concrete and testable (e.g., "MUST emit all log entries in structured JSON format", "MUST generate unique correlation ID for each incoming request").

### ✅ III. Incremental, Story-Based Delivery

**Status**: PASS

Feature is organized into 5 prioritized user stories (2 P1, 2 P2, 1 P3). Each story is independently testable:
- US1 (P1): Structured logging infrastructure - verifiable by checking log format
- US2 (P1): Error logging - verifiable by triggering errors and checking stack traces
- US3 (P2): Debug logging - verifiable by enabling debug level and checking business logic traces
- US4 (P2): Performance logging - verifiable by making requests and checking duration logs
- US5 (P3): Log level management - verifiable by changing config and checking log output

MVP consists of US1+US2 (structured logging foundation + comprehensive error logging).

### ✅ IV. Constitution Gates

**Status**: PASS

This planning phase validates against constitution requirements. Phase 1 design will be re-validated after artifacts are generated.

### ✅ V. Task Traceability

**Status**: DEFERRED

Will be validated during `/speckit.tasks` execution. Tasks will follow format `- [ ] [T###] [P] [US#] Description with file/path`.

### ✅ VI. Test-Optional, Test-First When Included

**Status**: PASS

Tests are not explicitly requested in specification. Logging behavior verification will use Spring Boot integration tests with test appenders to capture and validate log output. Tests will be written before implementation (TDD approach for log verification).

### ✅ VII. Artifact Consistency

**Status**: DEFERRED

Will be validated via `/speckit.analyze` after `/speckit.tasks` generates tasks.md.

### ✅ Project-Specific Constraints

**Technical Stack Compliance**: PASS
- Uses existing Spring Boot (Java) backend ✓
- No frontend changes (backend-only feature) ✓

**Authentication Integration**: PASS
- Logging will capture X-Hass-User header value in all log entries (FR-003) ✓
- No changes to authentication flow ✓

**Multi-User Household Support**: PASS
- User identifier from X-Hass-User will be included in all log entries (FR-003) ✓
- Enables filtering logs by household member ✓

**Overall Gate Status**: ✅ **PASS** - Proceed to Phase 0 Research

---

## Post-Design Constitution Re-Check

*Re-validation after Phase 1 design artifacts generated*

### ✅ Technical Stack Compliance

**Status**: PASS

- Uses existing Spring Boot backend (Java 17) ✓
- Adds Logback with Logstash encoder (industry-standard logging stack) ✓
- No frontend changes required ✓
- Maintains X-Hass-User header integration ✓

### ✅ Architecture Consistency

**Status**: PASS

- No new microservices or projects introduced ✓
- Logging infrastructure added to existing backend codebase ✓
- Uses Spring Boot native features (Actuator, AOP, Filters) ✓
- No architectural complexity added ✓

### ✅ Design Artifact Quality

**Status**: PASS

- research.md: 13 technical decisions documented with rationale ✓
- data-model.md: 5 entities with validation rules and relationships ✓
- contracts/: REST API specification for log management ✓
- quickstart.md: 10 test scenarios covering all user stories ✓

**Overall Post-Design Status**: ✅ **PASS** - Ready for task generation (`/speckit.tasks`)

## Project Structure

### Documentation (this feature)

```text
specs/009-structured-logging/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── config/
│   │   └── LoggingConfig.java              # NEW: Logback configuration, MDC setup
│   ├── filter/
│   │   └── CorrelationIdFilter.java        # NEW: Generates/extracts correlation ID
│   ├── interceptor/
│   │   └── LoggingInterceptor.java         # NEW: Request/response logging with AOP
│   ├── aspect/
│   │   └── PerformanceLoggingAspect.java   # NEW: Method execution time logging
│   ├── exception/
│   │   └── GlobalExceptionHandler.java     # MODIFY: Add structured error logging
│   ├── util/
│   │   ├── LogContext.java                 # NEW: MDC helper for correlation ID, user context
│   │   └── SensitiveDataMasker.java        # NEW: Mask passwords, tokens in logs
│   ├── service/
│   │   ├── BudgetService.java              # MODIFY: Add debug logging for calculations
│   │   ├── CategoryService.java            # MODIFY: Add debug logging for hierarchy validation
│   │   └── ExpenseService.java             # MODIFY: Add debug logging for budget attribution
│   └── controller/
│       ├── BudgetController.java           # MODIFY: Add request logging (via interceptor)
│       ├── CategoryController.java         # MODIFY: Add request logging (via interceptor)
│       └── ExpenseController.java          # MODIFY: Add request logging (via interceptor)
├── src/main/resources/
│   ├── logback-spring.xml                  # NEW: Logback configuration with JSON encoder
│   └── application.properties              # MODIFY: Add logging level configuration
└── src/test/java/com/homebudget/
    ├── logging/
    │   ├── LoggingIntegrationTest.java     # NEW: Verify log format and correlation IDs
    │   ├── ErrorLoggingTest.java           # NEW: Verify error logging with stack traces
    │   └── SensitiveDataMaskingTest.java   # NEW: Verify password/token masking
    └── util/
        └── TestLogAppender.java            # NEW: Custom appender for capturing logs in tests

budget-frontend/
└── (no changes - backend-only feature)
```

**Structure Decision**: This is a web application with existing backend/frontend separation. All logging implementation is in the Spring Boot backend (`budget-backend/`). Frontend is unaffected as logging is a backend infrastructure concern.

## Complexity Tracking

> **No violations** - All constitution checks pass. This feature adds logging infrastructure without introducing architectural complexity or violating project principles.
