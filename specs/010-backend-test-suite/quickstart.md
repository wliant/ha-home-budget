# Quickstart: Comprehensive Backend Test Suite

**Feature**: 010-backend-test-suite
**Date**: 2026-02-08

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for Testcontainers — runs MySQL containers)

## Running Tests

### Run Unit Tests Only
```bash
cd budget-backend
mvn test
```
Runs all `*Test.java` files. No database or Docker required.

### Run Integration Tests Only
```bash
cd budget-backend
mvn verify -DskipUnitTests=true
```
Runs all `*IntegrationTest.java` files. Requires Docker for MySQL Testcontainer.

### Run E2E Tests Only
```bash
cd budget-backend
mvn verify -DskipUnitTests=true -Dit.test="*E2ETest"
```
Runs all `*E2ETest.java` files. Requires Docker for MySQL Testcontainer.

### Run All Tests
```bash
cd budget-backend
mvn verify
```
Runs unit tests first, then integration and E2E tests.

## Integration Scenarios

### Scenario 1: Unit Test — Budget Service Create Budget

1. Mock `BudgetRepository`, `CategoryRepository`
2. Create a `BudgetDTO` with year=2026, month=2, totalAmount=1000, categoryId=1
3. Call `budgetService.createBudget(dto, "testuser")`
4. Verify repository `save()` was called with correct entity
5. Verify returned DTO has expected values

### Scenario 2: Integration Test — Category Hierarchy Persistence

1. Testcontainer MySQL starts with Liquibase migrations
2. Create parent category "Food" via `categoryRepository.save()`
3. Create child category "Groceries" with parentCategoryId=Food.id
4. Query `categoryRepository.findByParentCategoryId(Food.id)`
5. Assert child category "Groceries" is returned
6. Query `categoryRepository.findByParentCategoryIsNullOrderByNameAsc()`
7. Assert "Food" is in the list, "Groceries" is not

### Scenario 3: Integration Test — Budget Cascade Delete

1. Create category, budget, and three expenses persisted in MySQL
2. Delete the budget via `budgetRepository.deleteById()`
3. Verify all three expenses are also deleted
4. Verify category still exists

### Scenario 4: E2E Test — Complete Expense Workflow

1. Full Spring Boot app starts with MySQL Testcontainer
2. `POST /api/categories` with `X-Hass-User: testuser` — create "Food" category (expect 201)
3. `POST /api/budgets` with category, year, month — create budget (expect 201)
4. `POST /api/expenses` with budgetId, amount, description, date — create expense (expect 201)
5. `GET /api/expenses?budgetId={id}` — verify expense appears in list (expect 200)
6. `GET /api/budgets/{id}` — verify spending summary includes expense (expect 200)
7. `DELETE /api/expenses/{id}` — delete expense (expect 204)
8. `GET /api/budgets/{id}` — verify spending is now 0 (expect 200)

### Scenario 5: E2E Test — Validation Error Handling

1. `POST /api/budgets` with missing `categoryId` — expect 400 with validation errors
2. `POST /api/expenses` with negative amount — expect 400 with validation errors
3. `GET /api/budgets/999` for non-existent budget — expect 404
4. `POST /api/categories` with duplicate name — expect 409

### Scenario 6: E2E Test — X-Hass-User Header

1. `POST /api/budgets` with `X-Hass-User: user1` — create budget
2. `GET /api/budgets/{id}` — verify `createdBy` is "user1"
3. `POST /api/expenses` with `X-Hass-User: user2` — create expense on same budget
4. `GET /api/expenses/{id}` — verify `createdBy` is "user2"

## Test File Locations

```
budget-backend/src/test/java/com/homebudget/
├── service/                           # Unit tests
│   ├── BudgetServiceTest.java
│   ├── CategoryServiceTest.java
│   └── ExpenseServiceTest.java
├── repository/                        # Integration tests
│   ├── BudgetRepositoryIntegrationTest.java
│   ├── CategoryRepositoryIntegrationTest.java
│   └── ExpenseRepositoryIntegrationTest.java
├── service/                           # Integration tests (service + DB)
│   ├── BudgetServiceIntegrationTest.java
│   ├── CategoryServiceIntegrationTest.java
│   └── ExpenseServiceIntegrationTest.java
├── e2e/                               # End-to-end tests
│   ├── BudgetE2ETest.java
│   ├── CategoryE2ETest.java
│   └── ExpenseE2ETest.java
├── config/                            # Test infrastructure
│   └── AbstractIntegrationTest.java
└── controller/                        # Existing
    └── HealthControllerTest.java
```

## Test Configuration Files

```
budget-backend/src/test/resources/
├── application-test.yml               # Existing (H2 for unit tests)
└── application-integration-test.yml   # New (Testcontainers MySQL)
```
