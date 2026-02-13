# Quickstart: Backend Test Coverage Improvement

**Feature**: 014-backend-test-coverage
**Date**: 2026-02-11

## Prerequisites

- Docker running (required for Testcontainers MySQL)
- Maven wrapper (`./mvnw`) available in `budget-backend/`
- No local Java runtime required (Maven wrapper handles this)

## Running Tests

### Unit Tests Only (Fast - ~30 seconds)

```bash
cd budget-backend
./mvnw test
```

Runs: All `*Test.java` files (excludes `*IntegrationTest.java` and `*E2ETest.java`)
Database: H2 in-memory
Profile: `test`

### Full Test Suite (Unit + Integration + E2E - ~5 minutes)

```bash
cd budget-backend
./mvnw verify
```

Runs: Unit tests (Surefire) + Integration/E2E tests (Failsafe)
Database: MySQL 8.0 via Testcontainers
Profile: `test` for unit, `integration-test` for integration/E2E

### Coverage Report

After JaCoCo configuration (US6):

```bash
cd budget-backend
./mvnw verify
# Report at: target/site/jacoco/index.html
```

## Test File Locations

```
budget-backend/src/test/java/com/homebudget/
├── config/
│   ├── AbstractIntegrationTest.java    # Base class for Testcontainers tests
│   └── SmokeIntegrationTest.java       # Infrastructure smoke test
├── controller/
│   ├── HealthControllerTest.java       # Existing
│   ├── BudgetControllerTest.java       # NEW (US2)
│   ├── CategoryControllerTest.java     # NEW (US2)
│   ├── ExpenseControllerTest.java      # NEW (US2)
│   └── ExpenseInputJobControllerTest.java # NEW (US2)
├── service/
│   ├── BudgetServiceTest.java          # Existing
│   ├── CategoryServiceTest.java        # Existing
│   ├── ExpenseServiceTest.java         # Existing
│   ├── ExpenseInputJobServiceTest.java # NEW (US2)
│   ├── BudgetServiceIntegrationTest.java    # Existing
│   ├── CategoryServiceIntegrationTest.java  # Existing
│   ├── ExpenseServiceIntegrationTest.java   # Existing
│   └── ExpenseInputJobServiceIntegrationTest.java # NEW (US3)
├── repository/
│   ├── BudgetRepositoryIntegrationTest.java           # Existing
│   ├── CategoryRepositoryIntegrationTest.java         # Existing
│   ├── ExpenseRepositoryIntegrationTest.java          # Existing
│   ├── ExpenseFileRepositoryIntegrationTest.java      # NEW (US3)
│   ├── ExpenseInputJobRepositoryIntegrationTest.java  # NEW (US3)
│   └── TemporaryExpenseRecordRepositoryIntegrationTest.java # NEW (US3)
├── e2e/
│   ├── BudgetE2ETest.java             # Existing
│   ├── CategoryE2ETest.java           # Existing
│   ├── ExpenseE2ETest.java            # Existing
│   └── ExpenseInputJobE2ETest.java    # NEW (US4)
├── exception/
│   └── GlobalExceptionHandlerTest.java # NEW (US2)
├── filter/
│   ├── HassUserHeaderFilterTest.java   # NEW (US5)
│   ├── CorrelationIdFilterTest.java    # NEW (US5)
│   └── AuthHeaderInterceptorTest.java  # NEW (US5)
├── logging/
│   ├── PerformanceLoggingAspectTest.java # NEW (US5)
│   └── LoggingInterceptorTest.java       # NEW (US5)
└── util/
    ├── TestLogAppender.java            # Existing utility
    ├── SensitiveDataMaskerTest.java    # NEW (US5)
    └── LogContextTest.java             # NEW (US5)
```

## Integration Scenarios

### Scenario 1: Run existing tests and verify all pass (US1)

```bash
cd budget-backend
./mvnw verify
# Expected: BUILD SUCCESS, 0 failures
```

### Scenario 2: Check coverage after adding all new tests (US6)

```bash
cd budget-backend
./mvnw verify
# Open target/site/jacoco/index.html
# Verify: Overall line coverage >= 75%
```

### Scenario 3: Run only unit tests for fast feedback (US2)

```bash
cd budget-backend
./mvnw test
# Expected: All unit tests pass in < 60 seconds
```

### Scenario 4: Run only integration/E2E tests

```bash
cd budget-backend
./mvnw verify -DskipTests=true
# Runs only Failsafe (integration + E2E), skips Surefire (unit)
```

## New Test Files Summary

| User Story | New Test Files | Type |
|------------|---------------|------|
| US2 | BudgetControllerTest, CategoryControllerTest, ExpenseControllerTest, ExpenseInputJobControllerTest, ExpenseInputJobServiceTest, GlobalExceptionHandlerTest | Unit |
| US3 | ExpenseFileRepositoryIntegrationTest, ExpenseInputJobRepositoryIntegrationTest, TemporaryExpenseRecordRepositoryIntegrationTest, ExpenseInputJobServiceIntegrationTest | Integration |
| US4 | ExpenseInputJobE2ETest | E2E |
| US5 | HassUserHeaderFilterTest, CorrelationIdFilterTest, AuthHeaderInterceptorTest, PerformanceLoggingAspectTest, LoggingInterceptorTest, SensitiveDataMaskerTest, LogContextTest | Unit |
| US6 | (pom.xml configuration only) | Config |

**Total new test files**: ~18
