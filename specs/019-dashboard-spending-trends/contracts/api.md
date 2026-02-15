# API Contracts: Dashboard Spending Trends

**Feature**: 019-dashboard-spending-trends
**Date**: 2026-02-16

## Existing Endpoints (No Changes)

### GET /api/expenses/aggregates/monthly

Returns monthly expense aggregates per category with parent rollup.

**Parameters**:
- `year` (required, int) — Year to aggregate
- `month` (optional, int 1-12) — Specific month; if omitted, returns all months for the year

**Response**: `CategoryExpenseAggregateDTO[]`

**Usage in this feature**: Monthly granularity view. Call with `year` only (no `month`) to get all 12 months for the trend chart.

---

### GET /api/expenses/aggregates/yearly

Returns yearly expense aggregates per category with parent rollup.

**Parameters**:
- `year` (required, int) — Year to aggregate

**Response**: `CategoryExpenseAggregateDTO[]`

**Usage in this feature**: Yearly granularity view. Called once per year that has data.

---

### GET /api/expenses/years

Returns distinct years that have expense data.

**Response**: `int[]` (sorted descending)

**Usage in this feature**: Yearly granularity — determines which years to show.

---

## New Endpoint

### GET /api/expenses/aggregates/daily

Returns daily expense aggregates per category with parent rollup for a specific month.

**Parameters**:
- `year` (required, int) — Year
- `month` (required, int 1-12) — Month

**Response**: `CategoryExpenseAggregateDTO[]`

Each element includes:
```json
{
  "categoryId": 1,
  "categoryName": "Groceries",
  "categoryIcon": "shopping_cart",
  "parentCategoryId": null,
  "directAmount": 45.50,
  "childrenAmount": 0,
  "totalAmount": 45.50,
  "year": 2026,
  "month": 2,
  "day": 15
}
```

**Validation**:
- `month` is required (unlike monthly endpoint)
- `month` must be 1-12, otherwise 400 Bad Request

**Error Responses**:
- `400 Bad Request` — Invalid month parameter

---

## DTO Changes

### CategoryExpenseAggregateDTO

**New field**:
- `day` (Integer, nullable) — Day of month (1-31). Populated only for daily aggregates; null for monthly and yearly.

### Frontend Type: CategoryExpenseAggregate

**New field**:
- `day` (number | null | undefined) — Mirrors the backend field.
