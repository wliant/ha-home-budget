# Data Model: Backend Test Coverage Improvement

**Feature**: 014-backend-test-coverage
**Date**: 2026-02-11

## Overview

This feature does not introduce any new entities, tables, or data model changes. All work is confined to the test source tree (`src/test/`).

## Existing Entities Under Test

The following entities already exist and will be exercised by new tests. No modifications to these entities are required.

### Core Entities

| Entity | Table | Test Coverage Target |
|--------|-------|---------------------|
| Budget | budgets | Unit + Integration + E2E (existing) |
| Category | categories | Unit + Integration + E2E (existing) |
| Expense | expenses | Unit + Integration + E2E (existing) |
| ExpenseFile | expense_files | Integration (new) |
| ExpenseInputJob | expense_input_jobs | Unit + Integration + E2E (new) |
| TemporaryExpenseRecord | temporary_expense_records | Integration (new) |

### Entity Relationships Relevant to Testing

```
Budget (1) ←→ (N) Expense
Category (1) ←→ (N) Expense
Category (1) ←→ (N) Category (parent-child, max 2 levels)
Budget (N) ←→ (1) Category (optional)
Expense (1) ←→ (N) ExpenseFile
ExpenseInputJob (1) ←→ (1) TemporaryExpenseRecord
```

### Key Constraints to Validate in Tests

- Budget uniqueness: (year, month, category_id) must be unique
- Category hierarchy: maximum 2 levels deep
- Category name: case-insensitive uniqueness
- ExpenseInputJob status lifecycle: PENDING → PROCESSING → COMPLETED/FAILED
- TemporaryExpenseRecord: one-to-one with ExpenseInputJob

## Test Data Patterns

### Shared Test Fixtures

Tests should create minimal, self-contained data sets:

- **Budget test data**: Year 2024, month 1-12, amounts $100-$5000
- **Category test data**: "Test Category" with optional parent "Test Parent"
- **Expense test data**: Amounts $10-$500, dates within budget month
- **ExpenseInputJob test data**: CSV file content simulating bulk upload
- **User identity**: "test-user" as X-Hass-User header value
