# Data Model: Dashboard Spending Trends

**Feature**: 019-dashboard-spending-trends
**Date**: 2026-02-16

## Entities

### No New Entities Required

This feature uses existing entities and aggregation patterns from Feature 016.

### Modified Entity: CategoryExpenseAggregateDTO

The existing `CategoryExpenseAggregateDTO` will be extended with a `day` field to support daily granularity.

| Field | Type | Description |
|-------|------|-------------|
| categoryId | Long | Category identifier |
| categoryName | String | Category display name |
| categoryIcon | String | Category icon identifier |
| parentCategoryId | Long (nullable) | Parent category ID for hierarchy |
| directAmount | BigDecimal | Direct spending on this category |
| childrenAmount | BigDecimal | Sum of child category spending |
| totalAmount | BigDecimal | directAmount + childrenAmount |
| year | Integer | Year of the aggregate |
| month | Integer (nullable) | Month (1-12), null for yearly |
| **day** | **Integer (nullable)** | **Day (1-31), null for monthly/yearly** |

### Existing Entities Used

**Expense** (no changes):
- `id`, `amount`, `description`, `expenseDate`, `categoryId`, `createdBy`
- Aggregation source: GROUP BY category + time bucket

**Category** (no changes):
- `id`, `name`, `icon`, `parentCategoryId`
- Used for line labels, colors, and hierarchy

## Relationships

```
Expense ─── N:1 ──→ Category
Category ─── N:1 ──→ Category (parent, max 2-level hierarchy)
```

## Aggregation Patterns

### Daily Aggregation
- Group: `category_id × DAY(expense_date)` for a specific year+month
- Result: One row per category per day with spending data

### Monthly Aggregation (existing)
- Group: `category_id × MONTH(expense_date)` for a specific year
- Result: One row per category per month

### Yearly Aggregation (existing)
- Group: `category_id` for a specific year
- Result: One row per category per year
