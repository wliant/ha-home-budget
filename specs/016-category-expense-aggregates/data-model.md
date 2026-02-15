# Data Model: Category Expense Aggregates

**Feature**: 016-category-expense-aggregates
**Date**: 2026-02-15

## Entity Changes

### Expense (Modified)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| id | Long | Yes | Primary key, auto-generated |
| amount | Decimal(10,2) | Yes | Min 0 |
| description | String(500) | Yes | Non-blank |
| expense_date | Date | Yes | Date the expense was incurred |
| category_id | Long (FK → categories) | **Yes** | **Changed from optional to required** |
| created_by | String(100) | Yes | From X-Hass-User header |
| created_at | DateTime | Yes | Auto-set on insert |
| updated_at | DateTime | Yes | Auto-set on insert/update |
| version | Long | Yes | Optimistic locking |

**Removed fields**:
- ~~budget_id~~ (Long, FK → budgets) — Previously required, now completely removed

**Relationships**:
- ManyToOne → Category (required, non-null)
- ~~ManyToOne → Budget~~ — Removed

### Budget (Modified)

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| id | Long | Yes | Primary key, auto-generated |
| year | Integer | Yes | 2000-9999 |
| month | Integer | No | 1-12; null = yearly budget |
| total_amount | Decimal(10,2) | Yes | Min 0 |
| description | String(500) | No | |
| category_id | Long (FK → categories) | Yes | Unchanged |
| created_by | String(100) | Yes | From X-Hass-User header |
| created_at | DateTime | Yes | Auto-set on insert |
| updated_at | DateTime | Yes | Auto-set on insert/update |
| version | Long | Yes | Optimistic locking |

**Removed relationships**:
- ~~OneToMany → Expense (mappedBy budget, CascadeType.ALL, orphanRemoval)~~ — Removed entirely
- ~~addExpense() / removeExpense() helper methods~~ — Removed

**Preserved relationships**:
- ManyToOne → Category (unchanged)

### Category (Unchanged)

No changes to the Category entity. The 2-level hierarchy (parent_category_id) remains as-is. Categories gain importance as the sole grouping dimension for expenses.

## Derived Data (Not Persisted)

### Monthly Category Aggregate

Calculated on-the-fly via JPQL GROUP BY queries.

| Field | Source | Notes |
|-------|--------|-------|
| category_id | GROUP BY | From expense.category_id |
| year | Derived | YEAR(expense.expense_date) |
| month | Derived | MONTH(expense.expense_date) |
| total_amount | SUM | SUM(expense.amount) |

**Parent rollup**: For parent categories, the service layer sums the aggregates of all child categories plus any direct expenses on the parent category.

### Yearly Category Aggregate

| Field | Source | Notes |
|-------|--------|-------|
| category_id | GROUP BY | From expense.category_id |
| year | Derived | YEAR(expense.expense_date) |
| total_amount | SUM | SUM(expense.amount) for all months |

## Migration Plan

### Changeset 1: Populate Missing Categories

For any expenses that have a budget but no category, copy the budget's category:

```sql
UPDATE expenses e
JOIN budgets b ON e.budget_id = b.id
SET e.category_id = b.category_id
WHERE e.category_id IS NULL;
```

### Changeset 2: Make category_id NOT NULL

```sql
ALTER TABLE expenses MODIFY COLUMN category_id BIGINT NOT NULL;
```

### Changeset 3: Drop budget_id

1. Drop foreign key constraint `fk_expenses_budget`
2. Drop index `idx_expenses_budget_id`
3. Drop column `budget_id`

### New Indexes

Add composite index for aggregate query performance:

```sql
CREATE INDEX idx_expenses_category_date ON expenses (category_id, expense_date);
```

## Validation Rules

### Expense

- `amount`: Required, >= 0, precision 10, scale 2
- `description`: Required, non-blank, max 500 chars
- `expense_date`: Required
- `category_id`: **Required** (was optional). Frontend enforces selection; backend defaults to "Uncategorized" system category if null.
- `created_by`: Required, set from X-Hass-User header

### Budget

- No changes to budget validation rules
- Budget creation no longer triggers expense reassignment
- Budget deletion no longer cascades to expenses

## State Transitions

### Expense Lifecycle (Simplified)

```
Created → (category assigned) → Saved
  ↓
Updated → (category may change) → Saved
  ↓
Deleted → Removed
```

No budget-related state transitions remain. The expense lifecycle is purely: create with category, optionally update, optionally delete.

### Budget Lifecycle (Simplified)

```
Created → (category + period assigned) → Saved
  ↓
Updated → (amount/description changed) → Saved
  ↓
Deleted → Removed (NO cascade to expenses)
```

Budget creation no longer triggers:
- ~~Auto-reassignment of parent budget expenses to monthly budgets~~
- ~~Auto-creation of parent category budgets (this was budget-to-budget, stays if still needed)~~

Note: Budget-to-budget relationships (yearly parent ↔ monthly children, parent category budget) are **unchanged** by this feature.
