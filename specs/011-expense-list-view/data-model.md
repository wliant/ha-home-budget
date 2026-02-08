# Data Model: Expense List View

**Feature**: 011-expense-list-view
**Date**: 2026-02-09

## Overview

This feature does not introduce new database entities or schema changes. It extends the query layer over existing entities (Expense, Category, Budget) to support paginated, filtered, and sorted list retrieval with aggregate summaries.

## Existing Entities (No Changes)

### Expense

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-generated | |
| amount | BigDecimal(10,2) | NOT NULL, >= 0 | Filterable by range (minAmount, maxAmount) |
| description | String(500) | NOT NULL | Sortable |
| expenseDate | LocalDate | NOT NULL | Filterable by year/month range, sortable |
| budget | Budget (FK) | NOT NULL | ManyToOne relationship |
| category | Category (FK) | NULLABLE | ManyToOne, filterable |
| createdBy | String(100) | NOT NULL | Filterable, sortable |
| createdAt | LocalDateTime | NOT NULL, auto-set | Audit field |
| updatedAt | LocalDateTime | NOT NULL, auto-set | Audit field |
| version | Long | Optimistic locking | |

### Category

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-generated | |
| name | String(100) | NOT NULL, UNIQUE | Display in filter and table |
| icon | String(10) | NULLABLE | Display in table category column |
| parentCategory | Category (FK) | NULLABLE | Hierarchical support |
| isSystem | Boolean | DEFAULT false | e.g., "Uncategorized" |

### Budget

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-generated | |
| year | Integer | NOT NULL, 2000-9999 | Used for year filter mapping |
| month | Integer | NOT NULL, 1-12 | Used for month filter mapping |

## New DTOs (No Schema Changes)

### ExpenseListResponse

A response wrapper for the paginated expense list endpoint. Does not map to a database table.

| Field | Type | Description |
|-------|------|-------------|
| content | List<ExpenseDTO> | Current page of expenses |
| totalElements | Long | Total matching expenses across all pages |
| totalPages | Integer | Total number of pages |
| currentPage | Integer | Current page number (0-based) |
| pageSize | Integer | Items per page |
| totalAmount | BigDecimal | Sum of amounts for ALL matching expenses (not just current page) |
| sortBy | String | Current sort field |
| sortDirection | String | "ASC" or "DESC" |

### ExpenseListFilters (Query Parameters)

Parameters accepted by the enhanced list endpoint. Not persisted.

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| year | Integer | YES | Current year | Filter by expense year |
| month | Integer | No | null | Filter by expense month (1-12) |
| categoryId | Long | No | null | Filter by category |
| minAmount | BigDecimal | No | null | Minimum expense amount (inclusive) |
| maxAmount | BigDecimal | No | null | Maximum expense amount (inclusive) |
| createdBy | String | No | null | Filter by creator username |
| page | Integer | No | 0 | Page number (0-based) |
| size | Integer | No | 50 | Page size |
| sortBy | String | No | "expenseDate" | Sort field |
| sortDirection | String | No | "DESC" | Sort direction |

## Query Patterns

### Paginated Filter Query

Extends existing `findByFilters` with:
- `Pageable` parameter for pagination + sorting
- `minAmount` / `maxAmount` for amount range filtering
- Returns `Page<Expense>` instead of `List<Expense>`

### Aggregate Summary Query

New query for count + sum across all matching expenses:
```
SELECT COUNT(e), COALESCE(SUM(e.amount), 0)
FROM Expense e
WHERE [same filter conditions as paginated query]
```

### Distinct Years Query

New query for year filter population:
```
SELECT DISTINCT YEAR(e.expenseDate) FROM Expense e ORDER BY 1 DESC
```

### Distinct Creators Query

New query for created-by filter population:
```
SELECT DISTINCT e.createdBy FROM Expense e ORDER BY e.createdBy ASC
```

## Validation Rules

| Rule | Description |
|------|-------------|
| minAmount <= maxAmount | When both provided, min must not exceed max |
| month in 1-12 | When provided, must be valid month |
| year in 2000-9999 | Must be a reasonable year range |
| page >= 0 | Page number must be non-negative |
| size in 1-100 | Page size bounded for performance |
| sortBy in allowed fields | Must be one of: expenseDate, description, categoryName, amount, createdBy |
| sortDirection in ASC/DESC | Must be valid direction |
