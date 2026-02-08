# Implementation Plan: Comprehensive Backend Test Suite

**Branch**: `010-backend-test-suite` | **Date**: 2026-02-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-backend-test-suite/spec.md`

## Summary

Add a comprehensive three-tier test suite (unit, integration, end-to-end) to the Spring Boot backend. Unit tests use Mockito to verify BudgetService, CategoryService, and ExpenseService business logic without external dependencies. Integration tests use Testcontainers with MySQL 8.0 to verify repository queries, cascade operations, and constraint enforcement against a production-parity database. E2E tests exercise the full HTTP stack via TestRestTemplate with a running Spring Boot instance backed by MySQL Testcontainer.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.0, spring-boot-starter-test (JUnit 5, Mockito, AssertJ), Testcontainers (MySQL module), Maven Failsafe Plugin
**Storage**: MySQL 8.0 (via Testcontainers for integration/E2E), H2 in-memory (retained for existing unit tests)
**Testing**: JUnit 5, Mockito (unit), Testcontainers + Liquibase (integration), TestRestTemplate (E2E)
**Target Platform**: Spring Boot backend running on Java 17
**Project Type**: Backend (Spring Boot)
**Performance Goals**: Unit tests complete within 30 seconds; integration + E2E tests complete within 5 minutes
**Constraints**: Unit tests must have zero external dependencies; integration/E2E require Docker for Testcontainers
**Scale/Scope**: 3 service classes, 3 repository interfaces, 3 controllers, ~15 API endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Specification-First | PASS | Spec completed and validated with quality checklist |
| II. Clarify Before Planning | PASS | No clarifications needed — spec was unambiguous |
| III. Incremental, Story-Based Delivery | PASS | 3 user stories (P1: unit, P2: integration, P3: E2E), each independently testable |
| IV. Constitution Gates | PASS | This check |
| V. Task Traceability | N/A | Tasks not yet generated |
| VI. Test-Optional, Test-First | PASS | Tests are explicitly requested — this IS a test feature |
| VII. Artifact Consistency | N/A | Pending `/speckit.analyze` |
| Technical Stack: Next.js frontend | N/A | Feature is backend-only test infrastructure |
| Technical Stack: Spring Boot backend | PASS | Tests target the existing Spring Boot backend |
| Authentication: X-Hass-User | PASS | E2E tests verify X-Hass-User header handling |
| Multi-User Household | PASS | Tests verify user identity propagation via X-Hass-User |

**Post-Phase 1 Re-check**: All gates PASS. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/010-backend-test-suite/
├── plan.md              # This file
├── research.md          # Phase 0 output - technology decisions
├── data-model.md        # Phase 1 output - entities under test
├── quickstart.md        # Phase 1 output - test execution guide
├── contracts/           # Phase 1 output - API contracts under test
│   └── test-contracts.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
budget-backend/
├── pom.xml                                          # Updated: add Testcontainers, Failsafe plugin
├── src/main/java/com/homebudget/                    # Existing (no changes)
│   ├── service/
│   │   ├── BudgetService.java
│   │   ├── CategoryService.java
│   │   └── ExpenseService.java
│   ├── controller/
│   │   ├── BudgetController.java
│   │   ├── CategoryController.java
│   │   └── ExpenseController.java
│   └── repository/
│       ├── BudgetRepository.java
│       ├── CategoryRepository.java
│       └── ExpenseRepository.java
└── src/test/
    ├── java/com/homebudget/
    │   ├── config/                                  # NEW: Test infrastructure
    │   │   └── AbstractIntegrationTest.java         # Shared Testcontainers base class
    │   ├── service/                                 # NEW: Unit + Integration tests
    │   │   ├── BudgetServiceTest.java               # Unit tests (Mockito)
    │   │   ├── CategoryServiceTest.java             # Unit tests (Mockito)
    │   │   ├── ExpenseServiceTest.java              # Unit tests (Mockito)
    │   │   ├── BudgetServiceIntegrationTest.java    # Integration tests (Testcontainers)
    │   │   ├── CategoryServiceIntegrationTest.java  # Integration tests (Testcontainers)
    │   │   └── ExpenseServiceIntegrationTest.java   # Integration tests (Testcontainers)
    │   ├── repository/                              # NEW: Repository integration tests
    │   │   ├── BudgetRepositoryIntegrationTest.java
    │   │   ├── CategoryRepositoryIntegrationTest.java
    │   │   └── ExpenseRepositoryIntegrationTest.java
    │   ├── e2e/                                     # NEW: End-to-end tests
    │   │   ├── BudgetE2ETest.java
    │   │   ├── CategoryE2ETest.java
    │   │   └── ExpenseE2ETest.java
    │   ├── controller/                              # Existing
    │   │   └── HealthControllerTest.java
    │   └── util/                                    # Existing
    │       └── TestLogAppender.java
    └── resources/
        ├── application-test.yml                     # Existing (H2 for unit tests)
        └── application-integration-test.yml         # NEW: Testcontainers MySQL config
```

**Structure Decision**: All test code lives within the existing `budget-backend/src/test/` directory following Maven conventions. Unit tests use `*Test.java` suffix (run by Surefire), integration tests use `*IntegrationTest.java` suffix (run by Failsafe), and E2E tests use `*E2ETest.java` suffix (also run by Failsafe).

## Architecture & Design Decisions

### Test Pyramid

```
        /  E2E  \          3 classes  — Full HTTP + DB
       /----------\
      / Integration \      6 classes  — Service/Repository + DB
     /----------------\
    /    Unit Tests     \   3 classes  — Service logic only
   /____________________\
```

### Unit Test Design

- **Framework**: JUnit 5 + Mockito
- **Annotation**: `@ExtendWith(MockitoExtension.class)` (no Spring context)
- **Mocking**: `@Mock` for repositories and dependent services, `@InjectMocks` for service under test
- **Scope**: All public methods of BudgetService, CategoryService, ExpenseService
- **Coverage**: Success paths, error paths (exceptions), edge cases (null, boundary values)
- **No database**: All repository interactions mocked via `when().thenReturn()`

### Integration Test Design

- **Framework**: JUnit 5 + Spring Boot Test + Testcontainers
- **Base class**: `AbstractIntegrationTest` with shared MySQL container
- **Container**: `MySQLContainer<>("mysql:8.0")` — static, shared across all integration tests
- **Schema**: Liquibase migrations (same as production)
- **Profiles**: `@ActiveProfiles("integration-test")`
- **Isolation**: `@Transactional` with automatic rollback per test
- **Scope**: Repository custom queries, service business logic with real DB, cascade operations, constraint enforcement

### E2E Test Design

- **Framework**: JUnit 5 + Spring Boot Test (`RANDOM_PORT`) + TestRestTemplate
- **Base class**: Extends `AbstractIntegrationTest` for shared MySQL container
- **HTTP Client**: `TestRestTemplate` (real HTTP calls through servlet filters)
- **Headers**: `X-Hass-User` header set on each request via `HttpHeaders`
- **Isolation**: Explicit cleanup via `@BeforeEach` with repository `deleteAll()`
- **Scope**: Full request-response lifecycle, HTTP status codes, response body validation, header handling

### Testcontainers Configuration

```
AbstractIntegrationTest (abstract)
├── @Testcontainers
├── static MySQLContainer mysql (shared across all subclasses)
├── @DynamicPropertySource → spring.datasource.url, username, password
├── @ActiveProfiles("integration-test")
└── Liquibase enabled → runs production migrations
```

### Maven Configuration Changes

**New dependencies** (scope: test):
- `org.testcontainers:testcontainers` (BOM version management)
- `org.testcontainers:mysql`
- `org.testcontainers:junit-jupiter`

**New plugin**:
- `maven-failsafe-plugin` 3.0.0 — runs `*IntegrationTest.java` and `*E2ETest.java` during `verify` phase

### Test Profile Configuration

**application-integration-test.yml** (new):
- `spring.datasource.*`: Overridden by `@DynamicPropertySource` from Testcontainers
- `spring.liquibase.enabled: true` — runs production Liquibase migrations
- `spring.jpa.hibernate.ddl-auto: none` — Liquibase manages schema
- `logging.level.com.homebudget: DEBUG`

## Dependencies

### New Test Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| org.testcontainers:testcontainers-bom | 1.19.3 | BOM for Testcontainers version management |
| org.testcontainers:testcontainers | (BOM) | Core Testcontainers framework |
| org.testcontainers:mysql | (BOM) | MySQL container module |
| org.testcontainers:junit-jupiter | (BOM) | JUnit 5 integration for Testcontainers |

### Existing Dependencies Used

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-test | JUnit 5, Mockito, AssertJ, MockMvc, TestRestTemplate |
| h2 | Retained for existing HealthControllerTest |
| liquibase-core | Database migrations in Testcontainers |
| mysql-connector-j | MySQL JDBC driver for Testcontainers |

## Complexity Tracking

> No constitution violations to justify.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
