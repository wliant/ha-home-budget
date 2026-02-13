# Tasks: Backend Test Coverage Improvement

**Input**: Design documents from `/specs/014-backend-test-coverage/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: This feature IS about tests - all tasks are test-related.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `budget-backend/src/main/java/com/homebudget/` (production code - read only)
- **Tests**: `budget-backend/src/test/java/com/homebudget/` (new test files)
- **Build**: `budget-backend/pom.xml` (JaCoCo configuration)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configure code coverage tooling and establish baseline

- [x] T001 [US6] Add JaCoCo Maven plugin to `budget-backend/pom.xml` with `prepare-agent` and `report` goals. Configure executions: (1) `prepare-agent` in `initialize` phase, (2) `report` in `verify` phase. Add coverage exclusion rules for `com/homebudget/Application.class`, `com/homebudget/model/*`, `com/homebudget/dto/*`, `com/homebudget/exception/*Exception.class`. Ensure merged execution data from both Surefire and Failsafe. HTML report output at `target/site/jacoco/index.html`.
- [x] T002 [US6] Run `./mvnw verify` in `budget-backend/` to verify JaCoCo produces a coverage report. Open `target/site/jacoco/index.html` and record the baseline coverage percentage. Document the baseline in this task's completion notes. **Result: Baseline was 68.2% line coverage (1005/1474 lines). After adding targeted tests: 86.0% line coverage (1267/1474 lines).**

---

## Phase 2: Foundational (Audit Existing Tests)

**Purpose**: Ensure all existing tests pass before adding new ones

**⚠️ CRITICAL**: No new test work should begin until all existing tests are green

- [x] T003 [US1] Run `./mvnw test` in `budget-backend/` to execute all unit tests. If any tests fail, identify root cause and fix test defects only (no production code changes). Existing unit test files: `budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java`, `CategoryServiceTest.java`, `ExpenseServiceTest.java`, `budget-backend/src/test/java/com/homebudget/controller/HealthControllerTest.java`.
- [x] T004 [US1] Run `./mvnw verify` in `budget-backend/` to execute all integration and E2E tests. If any tests fail, identify root cause and fix test defects only. Existing integration tests: `budget-backend/src/test/java/com/homebudget/service/*IntegrationTest.java`, `budget-backend/src/test/java/com/homebudget/repository/*IntegrationTest.java`, `budget-backend/src/test/java/com/homebudget/config/SmokeIntegrationTest.java`. Existing E2E tests: `budget-backend/src/test/java/com/homebudget/e2e/*E2ETest.java`. Docker must be running for Testcontainers.

**Checkpoint**: All existing tests pass. Baseline coverage report generated. Ready for new test development.

---

## Phase 3: User Story 2 - Add Unit Tests for Untested Business Logic (Priority: P1)

**Goal**: Add unit tests for all untested controllers, services, and the global exception handler to maximize coverage of business logic with fast, isolated tests.

**Independent Test**: Run `./mvnw test` and verify all new unit test files pass. Controller and service classes should have >75% coverage.

### ExpenseInputJobService Unit Tests

- [x] T005 [US2] Create `budget-backend/src/test/java/com/homebudget/service/ExpenseInputJobServiceTest.java`. Use `@ExtendWith(MockitoExtension.class)` with `@Mock` for `ExpenseInputJobRepository`, `TemporaryExpenseRecordRepository`, `ExpenseFileRepository`, `ExpenseRepository`, `BudgetRepository`, `CategoryRepository`. Test all public methods: `createJobs()` (happy path with mock MultipartFile, error on empty file list), `getJobs()` (returns list, returns empty), `updateTemporaryRecord()` (happy path, job not found, record not found), `confirmJobs()` (happy path confirming multiple jobs, job not found, already completed), `deleteJobs()` (happy path, empty list), `processPendingJobs()` (processes pending job, handles failure gracefully, no pending jobs). Use `@Nested` classes with `@DisplayName` to group tests by method. Reference production code at `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`.

### Controller Unit Tests

- [x] T006 [P] [US2] Create `budget-backend/src/test/java/com/homebudget/controller/BudgetControllerTest.java`. Use `@WebMvcTest(BudgetController.class)` with `@MockBean BudgetService`. Test all 9 endpoints: `POST /api/budgets` (create with valid body, create with X-Hass-User header, validation error 400), `GET /api/budgets` (list all, empty list), `GET /api/budgets/{id}` (found, not found 404), `PUT /api/budgets/{id}` (update, not found), `DELETE /api/budgets/{id}` (success, not found), `GET /api/budgets/current` (found, not found), `GET /api/budgets/monthly-summary` (with year/month params), `GET /api/budgets/validation` (with categoryId/year/month params), `GET /api/budgets/yearly` (with year param). Verify response status codes, JSON structure, and exception handler integration. Reference production code at `budget-backend/src/main/java/com/homebudget/controller/BudgetController.java`.

- [x] T007 [P] [US2] Create `budget-backend/src/test/java/com/homebudget/controller/CategoryControllerTest.java`. Use `@WebMvcTest(CategoryController.class)` with `@MockBean CategoryService`. Test all 7 endpoints: `POST /api/categories` (create with valid body, with X-Hass-User header, validation error), `GET /api/categories` (list all, empty), `GET /api/categories/{id}` (found, not found 404), `PUT /api/categories/{id}` (update, not found, duplicate name 409), `DELETE /api/categories/{id}` (success, not found, category in use 409), `GET /api/categories/hierarchy` (returns tree), `GET /api/categories/{id}/expense-count` (returns count, not found). Reference production code at `budget-backend/src/main/java/com/homebudget/controller/CategoryController.java`.

- [x] T008 [P] [US2] Create `budget-backend/src/test/java/com/homebudget/controller/ExpenseControllerTest.java`. Use `@WebMvcTest(ExpenseController.class)` with `@MockBean ExpenseService`. Test all 10 endpoints: `POST /api/expenses` JSON (create, with X-Hass-User, validation error), `POST /api/expenses` multipart (create with files), `GET /api/expenses` (with filter params: budgetId, categoryId, startDate, endDate, createdBy), `GET /api/expenses/list` (paginated with all filter params), `GET /api/expenses/years` (returns year list), `GET /api/expenses/creators` (returns creator list), `GET /api/expenses/{id}` (found, not found), `PUT /api/expenses/{id}` JSON and multipart, `DELETE /api/expenses/{id}` (success, not found). Reference production code at `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java`.

- [x] T009 [P] [US2] Create `budget-backend/src/test/java/com/homebudget/controller/ExpenseInputJobControllerTest.java`. Use `@WebMvcTest(ExpenseInputJobController.class)` with `@MockBean ExpenseInputJobService`. Test all 5 endpoints: `POST /api/expense-input-jobs` (multipart upload with mock files, with X-Hass-User header), `GET /api/expense-input-jobs` (list all jobs, empty list), `PATCH /api/expense-input-jobs/{jobId}/temporary-record` (update record, job not found), `POST /api/expense-input-jobs/confirm` (confirm jobs with X-Hass-User, validation error), `DELETE /api/expense-input-jobs` (delete jobs, empty list). Reference production code at `budget-backend/src/main/java/com/homebudget/controller/ExpenseInputJobController.java`.

### Global Exception Handler Tests

- [x] T010 [P] [US2] Create `budget-backend/src/test/java/com/homebudget/exception/GlobalExceptionHandlerTest.java`. Test the GlobalExceptionHandler directly by instantiating it and calling each `@ExceptionHandler` method. Verify all 12 handlers: `handleValidationErrors` (returns 400 with field errors), `handleBudgetNotFound` (returns 404), `handleDuplicateBudget` (returns 409), `handleParentBudgetMismatch` (returns 400 with detail), `handleIllegalArgument` (returns 400), `handleMaxUploadSize` (returns 413), `handleExpenseNotFound` (returns 404), `handleCategoryInUse` (returns 409), `handleCategoryNotFound` (returns 404), `handleDuplicateCategory` (returns 409), `handleDatabaseException` (returns 500), `handleGenericException` (returns 500). For each handler verify: correct HTTP status, error message in response body, ErrorResponse structure (status, message, timestamp fields). Reference production code at `budget-backend/src/main/java/com/homebudget/exception/GlobalExceptionHandler.java`.

- [x] T011 [US2] Run `./mvnw test` in `budget-backend/` to verify all new and existing unit tests pass together. Confirm zero failures. **Result: 281 tests, 0 failures.**

**Checkpoint**: All controllers and services have unit tests. `./mvnw test` passes with zero failures.

---

## Phase 4: User Story 3 - Add Integration Tests for Untested Repositories and Services (Priority: P2)

**Goal**: Add integration tests for the three untested repositories (ExpenseFile, TemporaryExpenseRecord, ExpenseInputJob) and the ExpenseInputJobService using real MySQL via Testcontainers.

**Independent Test**: Run `./mvnw verify` and verify all new integration tests pass against a real MySQL database.

### Repository Integration Tests

- [x] T012 [P] [US3] Create `budget-backend/src/test/java/com/homebudget/repository/ExpenseFileRepositoryIntegrationTest.java`. Extend `AbstractIntegrationTest`. Use `@SpringBootTest` and `@Transactional`. Test: save and retrieve ExpenseFile entity, `findByExpenseIdOrderByIdAsc()` returns files in order, `findByExpenseIdOrderByIdAsc()` returns empty for nonexistent expense, `countByExpenseId()` returns correct count. Create prerequisite Budget, Category, and Expense entities in `@BeforeEach`. Reference production code at `budget-backend/src/main/java/com/homebudget/repository/ExpenseFileRepository.java` and entity at `budget-backend/src/main/java/com/homebudget/model/ExpenseFile.java`.

- [x] T013 [P] [US3] Create `budget-backend/src/test/java/com/homebudget/repository/ExpenseInputJobRepositoryIntegrationTest.java`. Extend `AbstractIntegrationTest`. Use `@SpringBootTest` and `@Transactional`. Test: save and retrieve ExpenseInputJob entity, `findAllByOrderByCreatedAtDesc()` returns jobs in descending order, `findByStatusOrderByCreatedAtAsc()` filters by PENDING/COMPLETED status correctly, status enum (PENDING, PROCESSING, COMPLETED, FAILED) persists and retrieves correctly. Reference production code at `budget-backend/src/main/java/com/homebudget/repository/ExpenseInputJobRepository.java` and entity at `budget-backend/src/main/java/com/homebudget/model/ExpenseInputJob.java`.

- [x] T014 [P] [US3] Create `budget-backend/src/test/java/com/homebudget/repository/TemporaryExpenseRecordRepositoryIntegrationTest.java`. Extend `AbstractIntegrationTest`. Use `@SpringBootTest` and `@Transactional`. Test: save and retrieve TemporaryExpenseRecord, `findByJobId()` returns record for existing job, `findByJobId()` returns empty for nonexistent job, `findByJobIdIn()` returns records for multiple job IDs, `findByJobIdIn()` returns empty for nonexistent IDs. Create prerequisite ExpenseInputJob and Category entities in `@BeforeEach`. Reference production code at `budget-backend/src/main/java/com/homebudget/repository/TemporaryExpenseRecordRepository.java` and entity at `budget-backend/src/main/java/com/homebudget/model/TemporaryExpenseRecord.java`.

### Service Integration Test

- [ ] T015 [US3] Create `budget-backend/src/test/java/com/homebudget/service/ExpenseInputJobServiceIntegrationTest.java`. Extend `AbstractIntegrationTest`. Use `@SpringBootTest` and `@Transactional`. Autowire `ExpenseInputJobService` and relevant repositories. Test: `createJobs()` creates job with temporary record persisted to real DB, `getJobs()` returns persisted jobs, `updateTemporaryRecord()` updates record in real DB, `confirmJobs()` creates real expenses from temporary records, `deleteJobs()` removes jobs from DB, `processPendingJobs()` processes a PENDING job and updates status. Use `MockMultipartFile` for file simulation and `@TempDir` for file storage. Clean up test data in `@BeforeEach`. Reference production code at `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`.

- [x] T016 [US3] Run `./mvnw verify` in `budget-backend/` to verify all new integration tests pass alongside existing integration tests. Docker must be running. Confirm zero failures. **Result: 108 integration/E2E tests pass, 0 failures. Fixed: Testcontainers singleton container pattern, parent yearly budget requirement in tests, Hibernate L1 cache clearing, MySQL DATETIME ordering, category name uniqueness.**

**Checkpoint**: All 6 repositories have integration tests. ExpenseInputJobService has integration tests. `./mvnw verify` passes.

---

## Phase 5: User Story 4 - Add End-to-End Tests for Untested API Flows (Priority: P2)

**Goal**: Add E2E tests for the ExpenseInputJobController bulk upload workflow and review existing E2E tests for completeness.

**Independent Test**: Run `./mvnw verify` and verify the new E2E test exercises the full bulk upload workflow through HTTP.

- [ ] T017 [US4] Create `budget-backend/src/test/java/com/homebudget/e2e/ExpenseInputJobE2ETest.java`. Extend `AbstractIntegrationTest`. Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`. Test the complete bulk upload workflow: (1) `POST /api/expense-input-jobs` with multipart file upload using `MultiValueMap<String, Object>` and `ByteArrayResource`, verify 200 response with job list, (2) `GET /api/expense-input-jobs` verify jobs are listed, (3) `PATCH /api/expense-input-jobs/{jobId}/temporary-record` update temporary record with category/amount, (4) `POST /api/expense-input-jobs/confirm` confirm jobs with X-Hass-User header, verify expenses are created. Also test error cases: upload with no files, confirm nonexistent jobs. Create prerequisite Category and Budget via API in `@BeforeEach`. Use same `headers()` helper pattern as existing E2E tests for X-Hass-User header. Reference existing E2E patterns at `budget-backend/src/test/java/com/homebudget/e2e/BudgetE2ETest.java`.

- [ ] T018 [US4] Review existing E2E tests for completeness: `budget-backend/src/test/java/com/homebudget/e2e/BudgetE2ETest.java` (verify yearly view endpoint tested), `budget-backend/src/test/java/com/homebudget/e2e/CategoryE2ETest.java` (verify hierarchy endpoint tested), `budget-backend/src/test/java/com/homebudget/e2e/ExpenseE2ETest.java` (verify paginated list endpoint and file upload tested). Add missing scenarios if any endpoints are not covered by existing E2E tests.

- [ ] T019 [US4] Run `./mvnw verify` in `budget-backend/` to verify all E2E tests pass. Confirm zero failures.

**Checkpoint**: All API endpoints have E2E test coverage. `./mvnw verify` passes.

---

## Phase 6: User Story 5 - Add Infrastructure and Cross-Cutting Tests (Priority: P3)

**Goal**: Add tests for filters, interceptors, aspects, and utility classes to fill coverage gaps in infrastructure code.

**Independent Test**: Run `./mvnw test` and verify all new infrastructure tests pass.

### Filter Tests

- [x] T020 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/filter/HassUserHeaderFilterTest.java`. Use plain JUnit 5 with `MockHttpServletRequest`, `MockHttpServletResponse`, and `MockFilterChain` from `org.springframework.mock.web`. Test: (1) filter extracts X-Hass-User header and sets it in MDC context, (2) filter continues chain when header is present, (3) filter continues chain when header is absent (graceful handling), (4) MDC context is cleared after filter chain completes. Reference production code at `budget-backend/src/main/java/com/homebudget/filter/HassUserHeaderFilter.java`.

- [x] T021 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/filter/CorrelationIdFilterTest.java`. Use plain JUnit 5 with `MockHttpServletRequest`, `MockHttpServletResponse`, and `MockFilterChain`. Test: (1) filter generates a UUID correlation ID and sets it in MDC, (2) correlation ID is a valid UUID format, (3) MDC is cleared after filter chain completes, (4) existing correlation ID header is reused if present. Reference production code at `budget-backend/src/main/java/com/homebudget/filter/CorrelationIdFilter.java`.

- [x] T022 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/config/AuthHeaderInterceptorTest.java`. Use plain JUnit 5 with `MockHttpServletRequest`, `MockHttpServletResponse`, and `MockFilterChain`. Test: (1) filter forwards X-Hass-User header correctly through the chain, (2) filter handles missing header gracefully. Reference production code at `budget-backend/src/main/java/com/homebudget/config/AuthHeaderInterceptor.java`.

### Aspect and Interceptor Tests

- [x] T023 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/aspect/PerformanceLoggingAspectTest.java`. Use `@ExtendWith(MockitoExtension.class)` with `@Mock ProceedingJoinPoint` and mock `MethodSignature`. Test: (1) `logMethodPerformance()` calls `joinPoint.proceed()` and returns its result, (2) execution time is logged, (3) slow methods (>100ms) are logged at WARN level (use `TestLogAppender` from `budget-backend/src/test/java/com/homebudget/util/TestLogAppender.java` to capture log output), (4) fast methods are logged at DEBUG level. Reference production code at `budget-backend/src/main/java/com/homebudget/aspect/PerformanceLoggingAspect.java`.

- [x] T024 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/interceptor/LoggingInterceptorTest.java`. Use plain JUnit 5 with `MockHttpServletRequest` and `MockHttpServletResponse`. Test: (1) `preHandle()` logs request method, URI, and returns true, (2) `afterCompletion()` logs response status and execution time, (3) exception parameter is logged when present. Reference production code at `budget-backend/src/main/java/com/homebudget/interceptor/LoggingInterceptor.java`.

### Utility Tests

- [x] T025 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/util/SensitiveDataMaskerTest.java`. Use plain JUnit 5 + AssertJ. Test: (1) `mask()` replaces password values with `***`, (2) `mask()` replaces token values with `***`, (3) `mask()` replaces secret values with `***`, (4) `mask()` leaves non-sensitive data unchanged, (5) `containsSensitiveData()` returns true for strings with sensitive patterns, (6) `containsSensitiveData()` returns false for safe strings, (7) constructor with custom patterns works correctly, (8) `getSensitivePatterns()` returns the configured patterns. Reference production code at `budget-backend/src/main/java/com/homebudget/util/SensitiveDataMasker.java`.

- [x] T026 [P] [US5] Create `budget-backend/src/test/java/com/homebudget/util/LogContextTest.java`. Use plain JUnit 5 + AssertJ. Test: (1) `setCorrelationId()` and `getCorrelationId()` round-trip correctly, (2) `setUserId()` and `getUserId()` round-trip correctly, (3) `clear()` removes both correlationId and userId from MDC, (4) `isContextInitialized()` returns true after set and false after clear, (5) `getContextInfo()` returns formatted string with both values. Clean up MDC in `@AfterEach`. Reference production code at `budget-backend/src/main/java/com/homebudget/util/LogContext.java`.

- [x] T027 [US5] Run `./mvnw test` in `budget-backend/` to verify all new infrastructure unit tests pass alongside existing unit tests. Confirm zero failures. **Result: 281 tests, 0 failures (merged with T011 run).**

**Checkpoint**: All infrastructure classes have dedicated tests. `./mvnw test` passes.

---

## Phase 7: Verification & Coverage Target (Final)

**Purpose**: Verify 75%+ coverage target and address any remaining gaps

- [x] T028 [US6] Run full test suite with `./mvnw verify` in `budget-backend/`. Open JaCoCo report at `target/site/jacoco/index.html`. Verify overall line coverage is 75% or higher. If below 75%, identify the top uncovered classes/methods and document them. **Result: 86.2% line coverage (1270/1474 lines). Well above 75% target.**
- [x] T029 [US6] If coverage is below 75% after T028, add targeted tests for the most impactful uncovered code areas. Focus on classes with the lowest coverage that are NOT in the exclusion list (not DTOs, entities, or exception constructors). Repeat `./mvnw verify` and re-check coverage until 75% is achieved. **Result: N/A - already at 86.2%, no additional tests needed.**
- [x] T030 Run final `./mvnw verify` in `budget-backend/` to confirm all tests (unit + integration + E2E) pass with zero failures. Verify unit tests complete in under 60 seconds and full suite completes in under 10 minutes. Record final test counts and coverage percentage. **Result: 324 unit tests + 108 integration/E2E tests = 432 total, 0 failures. Full suite completes in ~30 seconds. Final coverage: 86.2% line coverage.**

**Checkpoint**: All success criteria met. 75%+ coverage achieved. All tests pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Audit)**: Depends on Phase 1 (JaCoCo must be configured first for baseline)
- **Phase 3 (US2 Unit Tests)**: Depends on Phase 2 (existing tests must pass first)
- **Phase 4 (US3 Integration Tests)**: Depends on Phase 2. Can run in parallel with Phase 3.
- **Phase 5 (US4 E2E Tests)**: Depends on Phase 2. Can run in parallel with Phases 3-4.
- **Phase 6 (US5 Infrastructure Tests)**: Depends on Phase 2. Can run in parallel with Phases 3-5.
- **Phase 7 (Verification)**: Depends on ALL previous phases completing

### User Story Dependencies

- **US6 (Coverage Tool)**: No dependencies on other stories - enables measurement
- **US1 (Audit)**: Depends on US6 (need JaCoCo for baseline measurement)
- **US2 (Unit Tests)**: Depends on US1 (existing tests must pass first)
- **US3 (Integration Tests)**: Depends on US1. Independent of US2.
- **US4 (E2E Tests)**: Depends on US1. Independent of US2/US3.
- **US5 (Infrastructure Tests)**: Depends on US1. Independent of US2/US3/US4.

### Within Each User Story

- Read production code before writing tests
- Follow existing test patterns (Mockito for unit, Testcontainers for integration, TestRestTemplate for E2E)
- Run test suite after completing each story to verify no regressions

### Parallel Opportunities

**Phase 3 (US2)**: T006, T007, T008, T009, T010 can all run in parallel (different controller test files)

**Phase 4 (US3)**: T012, T013, T014 can run in parallel (different repository test files)

**Phase 6 (US5)**: T020, T021, T022, T023, T024, T025, T026 can all run in parallel (different infrastructure test files)

**Cross-phase**: Phases 3, 4, 5, 6 can run in parallel after Phase 2 completes (all user stories are independent)

---

## Parallel Example: User Story 2

```bash
# Launch all controller tests in parallel (different files, no dependencies):
Task: "Create BudgetControllerTest.java" (T006)
Task: "Create CategoryControllerTest.java" (T007)
Task: "Create ExpenseControllerTest.java" (T008)
Task: "Create ExpenseInputJobControllerTest.java" (T009)
Task: "Create GlobalExceptionHandlerTest.java" (T010)
```

## Parallel Example: User Story 5

```bash
# Launch all infrastructure tests in parallel (different files, no dependencies):
Task: "Create HassUserHeaderFilterTest.java" (T020)
Task: "Create CorrelationIdFilterTest.java" (T021)
Task: "Create AuthHeaderInterceptorTest.java" (T022)
Task: "Create PerformanceLoggingAspectTest.java" (T023)
Task: "Create LoggingInterceptorTest.java" (T024)
Task: "Create SensitiveDataMaskerTest.java" (T025)
Task: "Create LogContextTest.java" (T026)
```

---

## Implementation Strategy

### MVP First (US6 + US1 + US2)

1. Complete Phase 1: Setup JaCoCo → baseline coverage report
2. Complete Phase 2: Audit existing tests → all green
3. Complete Phase 3: Unit tests for controllers + services → biggest coverage gain
4. **STOP and VALIDATE**: Run `./mvnw test` and check coverage. Unit tests alone may push past 75%.

### Incremental Delivery

1. Phase 1 + 2 → Foundation ready (JaCoCo + all existing tests green)
2. Phase 3 (US2) → Unit tests added → Check coverage
3. Phase 4 (US3) → Integration tests added → Check coverage
4. Phase 5 (US4) → E2E tests added → Check coverage
5. Phase 6 (US5) → Infrastructure tests added → Check coverage
6. Phase 7 → Final verification and gap filling

### Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate progress
- Production code is READ ONLY - only test code and pom.xml are modified
