# Feature Specification: Comprehensive Backend Test Suite

**Feature Branch**: `010-backend-test-suite`
**Created**: 2026-02-08
**Status**: Draft
**Input**: User description: "Create a comprehensive test suite for the backend project with unit tests, integration tests, and end-to-end tests. Unit tests should not depend on external components like databases but should test based on Budget, Category, Expense. Integration tests should use Testcontainers. End-to-end tests should have entry points by calling the API."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unit Tests for Core Business Logic (Priority: P1)

As a developer, I want unit tests for the Budget, Category, and Expense service layers so that I can verify business logic correctness without needing a database or any external dependencies.

**Why this priority**: Unit tests form the foundation of the test pyramid. They run fast, catch logic errors early, and provide the highest ROI for development confidence. Without reliable unit tests, all other testing layers are built on uncertain ground.

**Independent Test**: Can be fully tested by running the unit test suite in isolation — no database, no containers, no network. Delivers immediate feedback on whether business rules (validation, calculations, constraints) work correctly.

**Acceptance Scenarios**:

1. **Given** a Budget service with mocked dependencies, **When** a developer creates a budget with valid data, **Then** the service correctly delegates to the repository and returns the expected result.
2. **Given** a Category service with mocked dependencies, **When** a developer attempts to create a duplicate category name, **Then** the service raises the appropriate error without touching a database.
3. **Given** an Expense service with mocked dependencies, **When** a developer creates an expense with a date outside the budget's month, **Then** the service produces a date mismatch warning.
4. **Given** the Budget service, **When** a developer creates a child-category budget that violates parent budget constraints, **Then** the service raises a parent budget mismatch error.
5. **Given** the Category service, **When** a developer attempts to create a circular parent-child relationship, **Then** the service raises a circular category error.
6. **Given** the Category service, **When** a developer attempts to delete a category that has associated budgets or expenses, **Then** the service raises a category-in-use error.

---

### User Story 2 - Integration Tests with Real Database (Priority: P2)

As a developer, I want integration tests that exercise the full service-to-database flow using a real MySQL database (via Testcontainers) so that I can verify that queries, transactions, and data persistence work correctly in a production-like environment.

**Why this priority**: Integration tests catch issues that unit tests cannot — ORM mapping errors, query correctness, transaction isolation, cascade behaviors, and constraint enforcement. Using Testcontainers ensures tests run against the same database engine as production.

**Independent Test**: Can be fully tested by running integration tests that spin up a MySQL Testcontainer. Delivers confidence that data layer operations (CRUD, queries, constraints) function correctly against real MySQL.

**Acceptance Scenarios**:

1. **Given** a running MySQL Testcontainer, **When** a developer runs the integration test suite, **Then** all repository and service operations execute against a real MySQL instance and pass.
2. **Given** a budget with expenses persisted in MySQL, **When** the budget is deleted, **Then** all associated expenses are also deleted (cascade verified in real database).
3. **Given** a category hierarchy persisted in MySQL, **When** querying for the hierarchy, **Then** the parent-child relationships are correctly returned.
4. **Given** two budgets for the same category/year/month, **When** the second budget creation is attempted, **Then** the duplicate budget constraint is enforced by the real database.
5. **Given** a category with a unique name constraint, **When** a duplicate name is inserted, **Then** the database-level constraint prevents the duplicate.

---

### User Story 3 - End-to-End API Tests (Priority: P3)

As a developer, I want end-to-end tests that exercise the full application stack by calling REST API endpoints so that I can verify the complete request-response lifecycle including authentication headers, validation, serialization, and error responses.

**Why this priority**: End-to-end tests validate the complete system behavior from HTTP request to response. They catch issues in controller mappings, request validation, header processing (X-Hass-User), response serialization, and error handling that neither unit nor integration tests cover.

**Independent Test**: Can be fully tested by starting the full application with a MySQL Testcontainer and making HTTP requests to API endpoints. Delivers confidence that the entire system works as expected from a client's perspective.

**Acceptance Scenarios**:

1. **Given** the full application running with a MySQL Testcontainer, **When** a developer sends a POST request to create a budget with a valid X-Hass-User header, **Then** the API returns a 201 response with the created budget.
2. **Given** a budget exists, **When** a developer sends a GET request to retrieve it, **Then** the API returns a 200 response with the correct budget data including spending summary.
3. **Given** no X-Hass-User header is provided, **When** a developer sends a POST request to create a resource, **Then** the API handles the missing header appropriately.
4. **Given** an invalid budget ID, **When** a developer sends a GET request, **Then** the API returns a 404 response with an appropriate error message.
5. **Given** the application is running, **When** a developer exercises the complete workflow (create category → create budget → create expense → query expenses → delete expense), **Then** each step returns the correct HTTP status and response body.
6. **Given** invalid request data (e.g., negative amount, missing required fields), **When** a developer sends a POST request, **Then** the API returns a 400 response with validation error details.

---

### Edge Cases

- What happens when unit tests encounter null or empty values for required fields (description, amount, dates)?
- How does the system handle concurrent budget modifications with optimistic locking in integration tests?
- What happens when an expense references a non-existent budget or category?
- How do tests verify the hierarchical category constraints (max 2 levels)?
- What happens when the Testcontainer MySQL instance is unavailable during integration tests?
- How does the system handle boundary values for budget year (2000, 9999) and month (1, 12)?
- What happens when deletion is attempted on a system category (isSystem = true)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The test suite MUST include unit tests that verify Budget service business logic without any external dependencies (no database, no containers)
- **FR-002**: The test suite MUST include unit tests that verify Category service business logic including hierarchy validation, duplicate detection, and system category protection
- **FR-003**: The test suite MUST include unit tests that verify Expense service business logic including date mismatch warnings and filtering logic
- **FR-004**: Unit tests MUST use mocked dependencies (repositories, other services) to isolate business logic under test
- **FR-005**: The test suite MUST include integration tests that use Testcontainers to run against a real MySQL database instance
- **FR-006**: Integration tests MUST verify repository operations including custom queries, cascade deletes, and constraint enforcement
- **FR-007**: Integration tests MUST verify service layer operations with real database transactions and data persistence
- **FR-008**: The test suite MUST include end-to-end tests that exercise REST API endpoints via HTTP requests
- **FR-009**: End-to-end tests MUST verify the complete request-response lifecycle including HTTP status codes, response bodies, and error handling
- **FR-010**: End-to-end tests MUST verify X-Hass-User header handling for user identity propagation
- **FR-011**: End-to-end tests MUST use a real database (via Testcontainers) for full-stack testing
- **FR-012**: All test tiers MUST cover Budget, Category, and Expense domains
- **FR-013**: Tests MUST be independently runnable — each test tier (unit, integration, e2e) can be executed separately
- **FR-014**: Each test MUST be isolated — test execution order MUST NOT affect results
- **FR-015**: Integration and e2e tests MUST clean up test data between test executions to prevent cross-test contamination

### Key Entities

- **Budget**: Monthly financial plan associated with a category. Key attributes: year, month, amount, description, category link, creator. Participates in parent-child budget validation with category hierarchies.
- **Category**: Classification for budgets and expenses. Supports hierarchical parent-child relationships (max 2 levels). Key attributes: name (unique), icon, system flag, parent link.
- **Expense**: Individual spending record linked to a budget and optionally a category. Key attributes: amount, description, date, budget link, category link, creator. Supports flexible filtering and date mismatch warnings.

## Assumptions

- Unit tests will use mocking (via the existing test framework) to isolate service logic from database dependencies
- Testcontainers for MySQL is the chosen approach for integration and e2e tests, replacing the existing H2 in-memory database for these test tiers
- The existing H2 test configuration may be retained for backward compatibility but Testcontainers provides production-parity testing
- Test data setup will use the service/repository layer directly rather than raw SQL scripts
- The X-Hass-User header value in tests will use simple string identifiers (e.g., "testuser1", "testuser2")
- Database schema initialization in Testcontainers will rely on Liquibase migrations (same as production)
- Tests will follow standard naming conventions: `*Test.java` for unit tests, `*IntegrationTest.java` for integration tests, `*E2ETest.java` for end-to-end tests

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Unit test suite covers all public methods of Budget, Category, and Expense service classes including both success and error paths
- **SC-002**: Unit tests execute without any external dependencies and complete within 30 seconds for the entire unit test suite
- **SC-003**: Integration tests verify all repository custom queries and constraint behaviors against a real MySQL instance
- **SC-004**: End-to-end tests cover all REST API endpoints (CRUD operations) for Budget, Category, and Expense
- **SC-005**: All tests pass consistently when run repeatedly (no flaky tests)
- **SC-006**: Each test tier can be run independently without requiring the other tiers to pass first
- **SC-007**: The test suite catches regressions — existing application behavior is captured such that breaking changes cause test failures
