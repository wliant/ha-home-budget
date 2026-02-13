# Feature Specification: Backend Test Coverage Improvement

**Feature Branch**: `014-backend-test-coverage`
**Created**: 2026-02-11
**Status**: Draft
**Input**: User description: "need to work on the backend spring project test coverage. relook at all the test case and create the test plan which cover unit, integration and end2end test ensure all tests are passing and test coverage is more than 75%."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Audit and Fix Existing Tests (Priority: P1)

As a developer, I want all existing backend tests to pass reliably so that the test suite serves as a trustworthy safety net for future changes.

**Why this priority**: If existing tests are broken or flaky, adding new tests provides no value. A green test suite is the foundation for all other test coverage work.

**Independent Test**: Can be verified by running the full test suite (`mvn verify`) and confirming all tests pass with zero failures.

**Acceptance Scenarios**:

1. **Given** the backend project with its current test suite, **When** all unit tests are executed, **Then** every test passes without failures or errors
2. **Given** the backend project with its current test suite, **When** all integration tests are executed against a real database, **Then** every test passes without failures or errors
3. **Given** the backend project with its current test suite, **When** all end-to-end tests are executed, **Then** every test passes without failures or errors
4. **Given** a test that was previously failing, **When** the root cause is identified as a test defect (not a production bug), **Then** the test is fixed while preserving its original intent

---

### User Story 2 - Add Unit Tests for Untested Business Logic (Priority: P1)

As a developer, I want unit tests covering all service classes and business logic so that regressions are caught quickly without requiring a running database.

**Why this priority**: Unit tests are the fastest feedback loop and the biggest contributor to overall coverage. Several service methods and all controller classes currently lack unit-level testing.

**Independent Test**: Can be verified by running `mvn test` (unit tests only) and checking that coverage of service and controller classes exceeds 75%.

**Acceptance Scenarios**:

1. **Given** the ExpenseInputJobService class has no tests, **When** unit tests are added, **Then** all public methods of ExpenseInputJobService have corresponding test cases covering happy path and error scenarios
2. **Given** BudgetController, CategoryController, and ExpenseController have no unit tests, **When** controller unit tests are added, **Then** each controller endpoint has tests verifying request mapping, input validation, response structure, and error handling
3. **Given** the ExpenseInputJobController has no tests, **When** controller unit tests are added, **Then** all endpoints including bulk upload workflow are tested
4. **Given** GlobalExceptionHandler has no direct tests, **When** unit tests are added, **Then** each exception type returns the correct status code and error response structure

---

### User Story 3 - Add Integration Tests for Untested Repositories and Services (Priority: P2)

As a developer, I want integration tests covering all repository classes and service interactions with the database so that data access logic is validated against a real database schema.

**Why this priority**: Three repositories (ExpenseFileRepository, TemporaryExpenseRecordRepository, ExpenseInputJobRepository) and the ExpenseInputJobService lack integration tests. These validate that queries and schema work correctly with a real database.

**Independent Test**: Can be verified by running integration tests via `mvn verify` and checking that all repository and service integration tests pass.

**Acceptance Scenarios**:

1. **Given** ExpenseFileRepository has no integration tests, **When** tests are added, **Then** CRUD operations and custom queries are validated against a real database
2. **Given** TemporaryExpenseRecordRepository has no integration tests, **When** tests are added, **Then** CRUD operations and custom queries are validated against a real database
3. **Given** ExpenseInputJobRepository has no integration tests, **When** tests are added, **Then** CRUD operations and custom queries are validated against a real database
4. **Given** ExpenseInputJobService has no integration tests, **When** tests are added, **Then** the service correctly persists and retrieves data through the full service-repository stack

---

### User Story 4 - Add End-to-End Tests for Untested API Flows (Priority: P2)

As a developer, I want end-to-end tests for all API endpoints so that the full request-response lifecycle is validated including authentication, serialization, and error responses.

**Why this priority**: The ExpenseInputJobController (bulk upload workflow) has no E2E tests. E2E tests validate the complete stack from HTTP request through controller, service, repository, and database back to HTTP response.

**Independent Test**: Can be verified by running E2E tests and confirming all API endpoints are exercised through the full stack.

**Acceptance Scenarios**:

1. **Given** ExpenseInputJobController has no E2E tests, **When** E2E tests are added, **Then** the bulk upload workflow (create job, upload file, confirm, process) is tested end-to-end
2. **Given** the existing E2E tests for Budget, Category, and Expense, **When** they are reviewed for completeness, **Then** any missing endpoint scenarios are added

---

### User Story 5 - Add Infrastructure and Cross-Cutting Tests (Priority: P3)

As a developer, I want tests for cross-cutting concerns (filters, interceptors, aspects, exception handling) so that infrastructure code is verified and contributes to the overall coverage target.

**Why this priority**: While not business logic, infrastructure code like request filters, logging interceptors, and performance aspects runs on every request. Testing these classes fills coverage gaps and prevents silent infrastructure failures.

**Independent Test**: Can be verified by running the full test suite and confirming infrastructure classes have test coverage.

**Acceptance Scenarios**:

1. **Given** HassUserHeaderFilter has no dedicated tests, **When** tests are added, **Then** the filter correctly extracts and propagates the X-Hass-User header
2. **Given** CorrelationIdFilter has no dedicated tests, **When** tests are added, **Then** correlation IDs are generated and set in the MDC context
3. **Given** PerformanceLoggingAspect has no tests, **When** tests are added, **Then** slow method detection and logging behavior are verified
4. **Given** LoggingInterceptor has no tests, **When** tests are added, **Then** request/response logging behavior is verified
5. **Given** SensitiveDataMasker has no tests, **When** tests are added, **Then** passwords, tokens, and secrets are correctly masked in output

---

### User Story 6 - Achieve and Verify 75%+ Code Coverage (Priority: P1)

As a developer, I want a code coverage measurement tool configured in the build so that I can verify the project meets the 75% coverage target and identify remaining gaps.

**Why this priority**: Without measurement, there is no way to verify the 75% target is met. Coverage reporting should be added early so that progress can be tracked as tests are written.

**Independent Test**: Can be verified by running the build with coverage enabled and checking the generated report shows 75% or higher overall line coverage.

**Acceptance Scenarios**:

1. **Given** the build system has no coverage tool configured, **When** a coverage tool is added to the build, **Then** running the build produces a coverage report
2. **Given** all tests (unit, integration, E2E) have been executed, **When** the coverage report is generated, **Then** overall line coverage is 75% or higher
3. **Given** coverage falls below 75% in a specific area, **When** the coverage report is reviewed, **Then** the specific uncovered classes and methods are identifiable for targeted test writing

---

### Edge Cases

- What happens when a test depends on database state from another test? Tests must be isolated and not depend on execution order
- How are flaky tests handled? Flaky tests must be identified, investigated, and either fixed or quarantined with a documented reason
- What happens when a new service method has no clear expected behavior? The existing production code is the source of truth; tests document the current behavior
- How are file upload tests handled in the test environment? Test files must be created in temporary directories that are cleaned up after each test
- What if coverage tool reports differ between unit-only and full test suite runs? The 75% target applies to the combined coverage of all test types

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All existing tests MUST pass without modifications to production code (test defects only)
- **FR-002**: The test suite MUST include unit tests for all controller classes (BudgetController, CategoryController, ExpenseController, ExpenseInputJobController)
- **FR-003**: The test suite MUST include unit tests for ExpenseInputJobService
- **FR-004**: The test suite MUST include unit tests for GlobalExceptionHandler
- **FR-005**: The test suite MUST include integration tests for ExpenseFileRepository, TemporaryExpenseRecordRepository, and ExpenseInputJobRepository
- **FR-006**: The test suite MUST include integration tests for ExpenseInputJobService
- **FR-007**: The test suite MUST include end-to-end tests for ExpenseInputJobController API endpoints
- **FR-008**: The test suite MUST include unit tests for infrastructure classes: HassUserHeaderFilter, CorrelationIdFilter, PerformanceLoggingAspect, LoggingInterceptor, and SensitiveDataMasker
- **FR-009**: A code coverage measurement tool MUST be configured in the build to produce coverage reports
- **FR-010**: Overall backend code coverage MUST be 75% or higher as measured by line coverage
- **FR-011**: Each test MUST be isolated and not depend on execution order or shared state from other tests
- **FR-012**: Unit tests MUST run without external dependencies (no database, no network)
- **FR-013**: Integration and E2E tests MUST use a containerized database for isolation from production data

### Assumptions

- Production code is assumed to be correct; tests document and verify existing behavior rather than discovering bugs
- The 75% coverage target applies to combined line coverage across all test types (unit + integration + E2E)
- Coverage measurement excludes auto-generated code, configuration classes, and main application entry points where testing provides minimal value
- The existing test infrastructure (Testcontainers, H2, Maven Surefire/Failsafe) is retained and extended rather than replaced
- File upload tests will use temporary test fixtures created during test setup

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of existing and new tests pass when the full test suite is executed
- **SC-002**: Overall backend line coverage reaches 75% or higher as reported by the coverage tool
- **SC-003**: All 5 controller classes have dedicated unit tests covering their endpoints
- **SC-004**: All 4 service classes have both unit tests and integration tests
- **SC-005**: All 6 repository classes have integration tests
- **SC-006**: All infrastructure classes (filters, interceptors, aspects) have dedicated tests
- **SC-007**: Unit tests complete execution in under 60 seconds
- **SC-008**: The full test suite (unit + integration + E2E) completes in under 10 minutes
