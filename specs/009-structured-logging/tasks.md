# Tasks: Comprehensive Structured Logging

**Input**: Design documents from `/specs/009-structured-logging/`
**Prerequisites**: plan.md (completed), spec.md (completed), research.md (completed), data-model.md (completed), contracts/ (completed), quickstart.md (completed)

**Tests**: Tests are included for logging verification using JUnit 5 and Logback TestAppender

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each logging capability.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `budget-backend/src/main/java/com/homebudget/`
- **Resources**: `budget-backend/src/main/resources/`
- **Tests**: `budget-backend/src/test/java/com/homebudget/`
- Frontend: No changes (backend-only feature)

## Phase 1: Setup (Logging Infrastructure)

**Purpose**: Add logging dependencies and create base infrastructure

- [x] T001 Add Logstash Logback Encoder dependency (7.4) to budget-backend/pom.xml
- [x] T002 [P] Add Spring Boot AOP dependency to budget-backend/pom.xml
- [x] T003 [P] Add Spring Boot Actuator dependency to budget-backend/pom.xml
- [x] T004 Create logback-spring.xml configuration in budget-backend/src/main/resources/logback-spring.xml
- [x] T005 [P] Create LoggingConfig Java class in budget-backend/src/main/java/com/homebudget/config/LoggingConfig.java
- [x] T006 [P] Create test utility TestLogAppender in budget-backend/src/test/java/com/homebudget/util/TestLogAppender.java

---

## Phase 2: Foundational (Core Logging Components)

**Purpose**: Core logging infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until correlation ID and user context extraction are complete

- [x] T007 Create CorrelationIdFilter servlet filter in budget-backend/src/main/java/com/homebudget/filter/CorrelationIdFilter.java
- [x] T008 Create LogContext MDC helper utility in budget-backend/src/main/java/com/homebudget/util/LogContext.java
- [x] T009 Register CorrelationIdFilter in LoggingConfig (ensure order before controllers)
- [x] T010 Add logging configuration properties to budget-backend/src/main/resources/application.yml

**Checkpoint**: ✅ Correlation ID and user context infrastructure ready - user story logging can now proceed

---

## Phase 3: User Story 1 - Structured Application Logging (Priority: P1) 🎯 MVP

**Goal**: Implement JSON-formatted logging with correlation IDs and user context for all backend operations

**Independent Test**: Trigger any API request and verify logs are JSON-formatted with correlation_id, user_id, timestamp, level, message fields

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US1] Create LoggingIntegrationTest in budget-backend/src/test/java/com/homebudget/logging/LoggingIntegrationTest.java
- [ ] T012 [P] [US1] Test case: Verify JSON log format with required fields (timestamp, level, logger_name, correlation_id, user_id)
- [ ] T013 [P] [US1] Test case: Verify correlation ID consistency across single request lifecycle
- [ ] T014 [P] [US1] Test case: Verify user ID extracted from X-Hass-User header and present in logs
- [ ] T015 [P] [US1] Test case: Verify multiple concurrent requests have different correlation IDs

### Implementation for User Story 1

- [x] T016 [US1] Configure Logstash JSON encoder in logback-spring.xml with custom fields (correlation_id, user_id, application_name, environment)
- [x] T017 [US1] Configure AsyncAppender in logback-spring.xml with queue size 10000 and discard strategy
- [x] T018 [US1] Add ISO-8601 timestamp pattern to logback-spring.xml
- [x] T019 [US1] Verify CorrelationIdFilter generates UUID and stores in MDC (from Phase 2, already implemented)
- [x] T020 [US1] Verify CorrelationIdFilter extracts X-Hass-User header and stores in MDC (from Phase 2, already implemented)
- [x] T021 [US1] Add correlation ID to HTTP response headers in CorrelationIdFilter
- [ ] T022 [US1] Test logging integration: Run LoggingIntegrationTest and verify all assertions pass

**Checkpoint**: Structured JSON logging with correlation tracking is fully functional

---

## Phase 4: User Story 2 - Comprehensive Error Logging (Priority: P1) 🎯 Critical

**Goal**: Implement detailed error logging with stack traces, request context, and sensitive data masking

**Independent Test**: Trigger validation error or exception and verify ERROR log contains exception_class, exception_message, stack_trace, correlation_id, user_id

### Tests for User Story 2

- [ ] T023 [P] [US2] Create ErrorLoggingTest in budget-backend/src/test/java/com/homebudget/logging/ErrorLoggingTest.java
- [ ] T024 [P] [US2] Create SensitiveDataMaskingTest in budget-backend/src/test/java/com/homebudget/logging/SensitiveDataMaskingTest.java
- [ ] T025 [P] [US2] Test case: Verify exception logging with full stack trace
- [ ] T026 [P] [US2] Test case: Verify validation errors logged at WARN level with field details
- [ ] T027 [P] [US2] Test case: Verify database errors logged at ERROR level with SQL state
- [ ] T028 [P] [US2] Test case: Verify sensitive data (password, token) is masked in log messages

### Implementation for User Story 2

- [x] T029 [P] [US2] Create SensitiveDataMasker utility in budget-backend/src/main/java/com/homebudget/util/SensitiveDataMasker.java
- [x] T030 [US2] Implement regex-based sensitive field detection in SensitiveDataMasker (patterns: password, token, secret, apiKey, authorization)
- [x] T031 [US2] Implement masking replacement logic in SensitiveDataMasker (replace with "***MASKED***")
- [x] T032 [US2] Add sensitive-fields configuration property to application.yml
- [x] T033 [US2] Modify GlobalExceptionHandler in budget-backend/src/main/java/com/homebudget/exception/GlobalExceptionHandler.java
- [x] T034 [US2] Add structured ERROR logging to GlobalExceptionHandler exception methods with correlation_id, user_id, exception details
- [x] T035 [US2] Add WARN level logging for validation exceptions in GlobalExceptionHandler
- [x] T036 [US2] Add ERROR level logging for database exceptions in GlobalExceptionHandler
- [x] T037 [US2] Apply SensitiveDataMasker to exception messages before logging
- [ ] T038 [US2] Test error logging: Run ErrorLoggingTest and SensitiveDataMaskingTest, verify all assertions pass

**Checkpoint**: Comprehensive error logging with sensitive data masking is fully functional

---

## Phase 5: User Story 3 - Debug Logging for Complex Issues (Priority: P2)

**Goal**: Implement DEBUG-level logging for complex business logic (category validation, budget calculations, expense attribution)

**Independent Test**: Enable DEBUG logging for service package, execute complex operation, verify debug logs show decision points and intermediate values

### Tests for User Story 3

- [ ] T039 [P] [US3] Create DebugLoggingTest in budget-backend/src/test/java/com/homebudget/logging/DebugLoggingTest.java
- [ ] T040 [P] [US3] Test case: Verify DEBUG logs appear when log level is DEBUG
- [ ] T041 [P] [US3] Test case: Verify DEBUG logs suppressed when log level is INFO
- [ ] T042 [P] [US3] Test case: Verify debug logs include intermediate calculation values
- [ ] T043 [P] [US3] Test case: Verify debug logs include decision point outcomes

### Implementation for User Story 3

- [x] T044 [P] [US3] Add DEBUG logging to CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [x] T045 [US3] Add debug logs for category hierarchy validation (parent lookup, circular reference check, depth validation)
- [x] T046 [US3] Add debug logs for category parent-child relationship updates
- [x] T047 [P] [US3] Add DEBUG logging to BudgetService in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [x] T048 [US3] Add debug logs for budget calculation steps (individual amounts, running totals, final calculated value)
- [x] T049 [US3] Add debug logs for parent-child budget validation
- [x] T050 [P] [US3] Add DEBUG logging to ExpenseService in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [x] T051 [US3] Add debug logs for expense budget attribution logic (date matching, budget selection criteria, selected budget)
- [x] T052 [US3] Add debug logs for expense creation workflow
- [x] T053 [US3] Use SLF4J parameterized logging (lazy evaluation) for all debug statements
- [x] T054 [US3] Add isDebugEnabled() guards for expensive debug operations
- [ ] T055 [US3] Test debug logging: Run DebugLoggingTest, verify all assertions pass

**Checkpoint**: Debug logging for complex business logic is fully functional

---

## Phase 6: User Story 4 - Request Tracing and Performance Logging (Priority: P2)

**Goal**: Implement request entry/exit logging with execution duration for all API endpoints

**Independent Test**: Make API request and verify request start log (http_method, http_path) and completion log (http_status, duration_ms) with same correlation_id

### Tests for User Story 4

- [ ] T056 [P] [US4] Create RequestLoggingTest in budget-backend/src/test/java/com/homebudget/logging/RequestLoggingTest.java
- [ ] T057 [P] [US4] Create PerformanceLoggingTest in budget-backend/src/test/java/com/homebudget/logging/PerformanceLoggingTest.java
- [ ] T058 [P] [US4] Test case: Verify request start log with HTTP method, path, correlation_id
- [ ] T059 [P] [US4] Test case: Verify request completion log with status code, duration, correlation_id
- [ ] T060 [P] [US4] Test case: Verify slow requests (>1000ms) logged at WARN level
- [ ] T061 [P] [US4] Test case: Verify method execution time logging for service methods

### Implementation for User Story 4

- [x] T062 [P] [US4] Create HandlerInterceptor in budget-backend/src/main/java/com/homebudget/interceptor/LoggingInterceptor.java
- [x] T063 [US4] Implement preHandle method: Log request start with INFO level (http_method, http_path, correlation_id, user_id)
- [x] T064 [US4] Implement preHandle method: Store request start time in request attribute
- [x] T065 [US4] Implement afterCompletion method: Calculate request duration from start time
- [x] T066 [US4] Implement afterCompletion method: Log request completion with INFO level (http_status, duration_ms, correlation_id)
- [x] T067 [US4] Implement afterCompletion method: Log slow requests with WARN level if duration > 1000ms
- [x] T068 [US4] Register LoggingInterceptor in LoggingConfig (ensure excluded paths for actuator endpoints)
- [ ] T069 [P] [US4] Create PerformanceLoggingAspect in budget-backend/src/main/java/com/homebudget/aspect/PerformanceLoggingAspect.java
- [ ] T070 [US4] Define pointcut for all service layer methods (@Pointcut("execution(* com.homebudget.service..*.*(..))"))
- [ ] T071 [US4] Implement @Around advice: Measure method execution time
- [ ] T072 [US4] Implement @Around advice: Log at DEBUG level for all methods with duration
- [ ] T073 [US4] Implement @Around advice: Log at WARN level for slow methods (>100ms)
- [ ] T074 [US4] Include method name, masked arguments, duration in performance logs
- [ ] T075 [US4] Test request and performance logging: Run RequestLoggingTest and PerformanceLoggingTest, verify all assertions pass

**Checkpoint**: Request tracing and performance logging is fully functional

---

## Phase 7: User Story 5 - Log Level Management and Filtering (Priority: P3)

**Goal**: Enable runtime log level configuration via Spring Boot Actuator without application restart

**Independent Test**: Use REST API to change log level to DEBUG, verify DEBUG logs appear, reset to INFO, verify DEBUG logs disappear

### Tests for User Story 5

- [ ] T076 [P] [US5] Create LogLevelManagementTest in budget-backend/src/test/java/com/homebudget/logging/LogLevelManagementTest.java
- [ ] T077 [P] [US5] Test case: Verify GET /actuator/loggers returns all loggers with levels
- [ ] T078 [P] [US5] Test case: Verify GET /actuator/loggers/{name} returns specific logger level
- [ ] T079 [P] [US5] Test case: Verify POST /actuator/loggers/{name} changes log level without restart
- [ ] T080 [P] [US5] Test case: Verify POST with null resets logger to inherited level
- [ ] T081 [P] [US5] Test case: Verify invalid log level returns 400 Bad Request

### Implementation for User Story 5

- [x] T082 [US5] Enable Spring Boot Actuator loggers endpoint in application.yml (management.endpoints.web.exposure.include=health,info,loggers)
- [x] T083 [US5] Configure global log level (logging.level.root=INFO) in application.yml
- [x] T084 [US5] Configure package-specific log levels in application.yml (logging.level.com.homebudget=INFO)
- [x] T085 [US5] Add profile-specific logging configuration in application-dev.properties (logging.level.com.homebudget=DEBUG)
- [ ] T086 [US5] Add profile-specific logging configuration in application-prod.properties (logging.level.com.homebudget=INFO)
- [ ] T087 [US5] Document log level management in application.yml comments
- [ ] T088 [US5] Test log level management: Run LogLevelManagementTest, verify all assertions pass

**Checkpoint**: Runtime log level management is fully functional

---

## Phase 8: Integration & Validation

**Purpose**: Verify all user stories work together and logging meets success criteria

- [ ] T089 Run quickstart.md Test Scenario 1: Verify Structured JSON Logging
- [ ] T090 [P] Run quickstart.md Test Scenario 2: Verify Correlation ID Tracking
- [ ] T091 [P] Run quickstart.md Test Scenario 3: Verify Error Logging with Stack Traces
- [ ] T092 [P] Run quickstart.md Test Scenario 4: Verify Debug Logging for Business Logic
- [ ] T093 [P] Run quickstart.md Test Scenario 5: Verify Request/Response Logging
- [ ] T094 [P] Run quickstart.md Test Scenario 6: Verify Slow Request Detection
- [ ] T095 [P] Run quickstart.md Test Scenario 7: Verify Sensitive Data Masking
- [ ] T096 [P] Run quickstart.md Test Scenario 8: Verify Log Level Management
- [ ] T097 [P] Run quickstart.md Test Scenario 9: Verify User Context in Logs
- [ ] T098 [P] Run quickstart.md Test Scenario 10: Verify Performance Logging
- [ ] T099 Verify all 12 success criteria from spec.md (SC-001 through SC-012)
- [ ] T100 Measure logging performance overhead (verify <5ms per request at INFO level)
- [ ] T101 Test async logging queue behavior under load (verify bounded queue doesn't overflow)
- [ ] T102 Verify sensitive data masking compliance (100% masking of passwords, tokens)
- [ ] T103 Test log level changes without application restart (verify <60 seconds propagation)
- [ ] T104 Run all test suites (LoggingIntegrationTest, ErrorLoggingTest, DebugLoggingTest, RequestLoggingTest, PerformanceLoggingTest, LogLevelManagementTest)

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and finalization

- [ ] T105 [P] Add inline comments to CorrelationIdFilter explaining correlation ID generation and MDC usage
- [ ] T106 [P] Add inline comments to LoggingInterceptor explaining request timing logic
- [ ] T107 [P] Add inline comments to PerformanceLoggingAspect explaining AOP pointcut
- [ ] T108 [P] Add JavaDoc to SensitiveDataMasker utility methods
- [ ] T109 [P] Add JavaDoc to LogContext utility methods
- [ ] T110 Update CLAUDE.md with logging framework information (Logback, Logstash encoder)
- [ ] T111 Add logging best practices to project documentation
- [ ] T112 Review all log messages for clarity and consistency
- [ ] T113 Verify no DEBUG logs in production-critical paths that could impact performance
- [ ] T114 Final code review: Verify all classes follow SLF4J best practices (parameterized logging, no string concatenation)
- [ ] T115 Final validation: Run full integration test suite and verify all tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational - JSON logging foundation
- **User Story 2 (Phase 4)**: Depends on Foundational - Error logging (independent of US1)
- **User Story 3 (Phase 5)**: Depends on Foundational - Debug logging (independent of US1, US2)
- **User Story 4 (Phase 6)**: Depends on Foundational - Performance logging (independent of US1-3)
- **User Story 5 (Phase 7)**: Depends on Foundational - Log level management (independent of US1-4)
- **Integration (Phase 8)**: Depends on all desired user stories being complete
- **Polish (Phase 9)**: Depends on Integration completion

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational - Independent (relies on GlobalExceptionHandler which exists)
- **User Story 3 (P2)**: Can start after Foundational - Independent (adds debug logs to existing services)
- **User Story 4 (P2)**: Can start after Foundational - Independent (request interceptor orthogonal to other features)
- **User Story 5 (P3)**: Can start after Foundational - Independent (actuator configuration separate from logging logic)

All user stories are independently implementable and testable!

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Core infrastructure before usage
- Shared utilities before dependent components
- Configuration before behavior
- Verification tests after implementation

### Parallel Opportunities

- **Setup Phase**: Tasks T001, T002, T003, T005, T006 can run in parallel (different files)
- **Foundational Phase**: Tasks T007, T008 can run in parallel (different files)
- **User Story 1 Tests**: Tasks T011-T015 can run in parallel (different test methods)
- **User Story 2 Tests**: Tasks T023-T028 can run in parallel (different test methods)
- **User Story 2 Implementation**: Tasks T029 (SensitiveDataMasker) can run parallel to T033 (GlobalExceptionHandler modification)
- **User Story 3 Implementation**: Tasks T044, T047, T050 can run in parallel (different service files)
- **User Story 4 Implementation**: Tasks T062 (LoggingInterceptor) and T069 (PerformanceLoggingAspect) can run in parallel (different files)
- **User Story 4 Tests**: Tasks T056-T061 can run in parallel (different test methods)
- **User Story 5 Tests**: Tasks T076-T081 can run in parallel (different test methods)
- **Integration Phase**: Tasks T089-T098 can run in parallel (independent test scenarios)
- **Polish Phase**: Tasks T105-T109 can run in parallel (different files)
- **All User Stories (Phase 3-7)**: Can run in parallel by different developers after Foundational phase completes

---

## Parallel Example: User Story 2 (Error Logging)

```bash
# Launch all tests for User Story 2 together:
Task T023: "Create ErrorLoggingTest"
Task T024: "Create SensitiveDataMaskingTest"
Task T025: "Test case: Exception logging with stack trace"
Task T026: "Test case: Validation errors at WARN level"
Task T027: "Test case: Database errors at ERROR level"
Task T028: "Test case: Sensitive data masking"

# Launch parallel implementation tasks:
Task T029: "Create SensitiveDataMasker utility" (independent file)
Task T033: "Modify GlobalExceptionHandler" (independent file)
# Both can run concurrently, then integrated via T037

# Sequential after parallel completion:
Task T030-T032: Configure SensitiveDataMasker
Task T034-T037: Enhance GlobalExceptionHandler with logging
Task T038: Run tests to verify
```

---

## Parallel Example: User Stories after Foundational

```bash
# After Phase 2 (Foundational) completes, launch all P1 stories in parallel:

Developer A or Agent A:
- Phase 3 (US1): Structured JSON logging implementation

Developer B or Agent B:
- Phase 4 (US2): Error logging implementation

Developer C or Agent C:
- Phase 5 (US3): Debug logging implementation

# All three stories are independently implementable and testable
# No coordination required between stories
# Each completes its own tests and validation
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only - Critical Logging)

1. Complete Phase 1: Setup (T001-T006)
2. Complete Phase 2: Foundational (T007-T010) - CRITICAL BLOCKER
3. Complete Phase 3: User Story 1 (T011-T022) - Structured JSON logging
4. Complete Phase 4: User Story 2 (T023-T038) - Error logging
5. **STOP and VALIDATE**: Test structured logging and error logging independently
6. Run quickstart scenarios 1, 2, 3, 7
7. Verify SC-001, SC-002, SC-007, SC-008 from success criteria
8. Deploy/demo if ready (MVP provides essential logging for production support)

### Incremental Delivery

1. Complete Setup + Foundational → Logging infrastructure ready
2. Add User Story 1 → Test independently → Deploy (Structured logging available)
3. Add User Story 2 → Test independently → Deploy (Error tracking available)
4. Add User Story 3 → Test independently → Deploy (Debug logging available)
5. Add User Story 4 → Test independently → Deploy (Performance monitoring available)
6. Add User Story 5 → Test independently → Deploy (Log level management available)
7. Each story adds observability value without breaking previous stories

### Parallel Team Strategy

With multiple developers or AI agents:

1. All team members complete Setup + Foundational together (T001-T010)
2. Once Foundational is done (correlation ID and user context working):
   - Developer A: User Story 1 (Structured JSON logging)
   - Developer B: User Story 2 (Error logging)
   - Developer C: User Story 3 (Debug logging)
   - Developer D: User Story 4 (Performance logging)
   - Developer E: User Story 5 (Log level management)
3. Stories complete and integrate independently
4. Integration phase validates all stories work together

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests use JUnit 5 with Spring Boot Test and custom Logback TestAppender
- Verify tests fail before implementing (Red-Green-Refactor TDD)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Use SLF4J parameterized logging (not string concatenation) for performance
- Apply sensitive data masking to all log messages that may contain user input
- Configure different log levels for dev vs prod environments
- Monitor async queue behavior under high load

---

## Task Summary

- **Total Tasks**: 115
- **Setup Phase**: 6 tasks (T001-T006)
- **Foundational Phase**: 4 tasks (T007-T010)
- **User Story 1 (Structured Logging - P1)**: 12 tasks (T011-T022)
- **User Story 2 (Error Logging - P1)**: 17 tasks (T023-T039)
- **User Story 3 (Debug Logging - P2)**: 17 tasks (T039-T055)
- **User Story 4 (Performance Logging - P2)**: 20 tasks (T056-T075)
- **User Story 5 (Log Management - P3)**: 13 tasks (T076-T088)
- **Integration & Validation**: 16 tasks (T089-T104)
- **Polish & Cross-Cutting**: 11 tasks (T105-T115)

**Parallel Opportunities**: 45+ tasks marked [P] can run concurrently
**Independent Stories**: All 5 user stories can be developed independently after Foundational phase
**MVP Scope**: Phases 1-4 (Setup + Foundational + US1 + US2 = 39 tasks for critical logging coverage)
