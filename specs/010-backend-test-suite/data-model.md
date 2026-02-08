# Data Model: Comprehensive Backend Test Suite

**Feature**: 010-backend-test-suite
**Date**: 2026-02-08

## Overview

This feature does not introduce new data entities. It tests the existing data model comprising Budget, Category, and Expense. This document describes the entities and their relationships as they will be exercised by the test suite.

## Existing Entities Under Test

### Category

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | Primary key, auto-generated |
| name | String (max 100) | Required, unique |
| icon | String (max 10) | Optional |
| createdBy | String (max 100) | Required |
| createdAt | Timestamp | Auto-generated |
| isSystem | Boolean | Default false |
| parentCategoryId | Long | Optional, self-referencing FK |

**Relationships**:
- Self-referencing parent-child hierarchy (max 2 levels)
- One-to-many with Budget
- One-to-many with Expense

**Constraints tested**:
- Unique name (case-sensitive at DB level, case-insensitive at service level)
- Max hierarchy depth: 2 levels
- System categories cannot be deleted or modified
- Cannot delete category with children, budgets, or expenses
- Circular reference prevention

### Budget

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | Primary key, auto-generated |
| year | Integer | Required, range 2000-9999 |
| month | Integer | Required, range 1-12 |
| totalAmount | BigDecimal (10,2) | Required, >= 0 |
| description | String (max 500) | Optional |
| createdBy | String (max 100) | Required |
| createdAt | Timestamp | Auto-generated |
| updatedAt | Timestamp | Auto-updated |
| version | Long | Optimistic locking |
| categoryId | Long | FK to Category |

**Relationships**:
- Many-to-one with Category (required for creation)
- One-to-many with Expense (cascade delete, orphan removal)

**Constraints tested**:
- Unique (categoryId, year, month) combination
- Parent-child budget amount validation
- Sum of child budgets must equal parent budget
- Optimistic locking on concurrent updates

### Expense

| Field | Type | Constraints |
|-------|------|-------------|
| id | Long | Primary key, auto-generated |
| amount | BigDecimal (10,2) | Required, >= 0 |
| description | String (max 500) | Required |
| expenseDate | LocalDate | Required |
| budgetId | Long | FK to Budget, required |
| categoryId | Long | FK to Category, optional |
| createdBy | String (max 100) | Required |
| createdAt | Timestamp | Auto-generated |
| updatedAt | Timestamp | Auto-updated |
| version | Long | Optimistic locking |

**Relationships**:
- Many-to-one with Budget (required, cascade delete from Budget)
- Many-to-one with Category (optional)

**Constraints tested**:
- Budget foreign key enforcement
- Optional category foreign key
- Date mismatch warning (expense date vs budget month)
- Optimistic locking on concurrent updates

## Entity Relationship Diagram

```
Category (self-referencing)
├── parentCategoryId → Category.id (max 2 levels)
├── Budget.categoryId → Category.id (one-to-many)
└── Expense.categoryId → Category.id (one-to-many, optional)

Budget
├── Budget.categoryId → Category.id (many-to-one, required)
└── Expense.budgetId → Budget.id (one-to-many, cascade delete)

Expense
├── Expense.budgetId → Budget.id (many-to-one, required)
└── Expense.categoryId → Category.id (many-to-one, optional)
```

## Test Data Patterns

### Unit Test Data
- In-memory objects constructed via constructors or builders
- No persistence — all repository calls mocked
- Test data covers: valid objects, boundary values, null/missing fields

### Integration Test Data
- Persisted via repositories in a real MySQL Testcontainer
- Schema initialized by Liquibase migrations (same as production)
- Seed data includes: system "Uncategorized" category (id=1, isSystem=true)
- Test isolation via `@Transactional` rollback

### E2E Test Data
- Created via API calls (POST endpoints)
- Cleaned up via `@BeforeEach` repository cleanup
- Tests full serialization/deserialization cycle
