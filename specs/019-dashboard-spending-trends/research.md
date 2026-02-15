# Research: Dashboard Spending Trends

**Feature**: 019-dashboard-spending-trends
**Date**: 2026-02-16

## Research Topics

### R1: Existing Aggregate API Endpoints

**Decision**: Reuse existing monthly and yearly aggregate endpoints; add a new daily aggregate endpoint.

**Rationale**: Feature 016 (Category Expense Aggregates) already provides:
- `GET /api/expenses/aggregates/monthly?year=X&month=M` — returns `CategoryExpenseAggregateDTO[]` with per-category spending grouped by month. When `month` is omitted, returns all months for the year.
- `GET /api/expenses/aggregates/yearly?year=X` — returns per-category yearly totals.

Both include parent category rollup (directAmount, childrenAmount, totalAmount). The frontend already has corresponding service methods: `expenseService.getMonthlyAggregates(year, month?)` and `expenseService.getYearlyAggregates(year)`.

**Gap identified**: No daily aggregate endpoint exists. A new `GET /api/expenses/aggregates/daily?year=X&month=M` endpoint is needed, returning `CategoryExpenseAggregateDTO[]` with a `day` field for daily granularity.

**Alternatives considered**:
- Fetch raw expenses and aggregate client-side: Rejected — too much data transfer and processing for potentially thousands of expenses.
- Create a single trend-specific endpoint: Rejected — the existing aggregate pattern is clean and consistent, adding a daily variant keeps the API uniform.

### R2: Charting Library

**Decision**: Use Recharts `LineChart` component (already installed).

**Rationale**: Recharts 3.7.0 is already a project dependency (`budget-frontend/package.json`). Currently used for `PieChart` in `BudgetPieChart.tsx`. Recharts provides `LineChart`, `Line`, `XAxis`, `YAxis`, `CartesianGrid`, `Tooltip`, `Legend`, `ResponsiveContainer` components that satisfy all requirements (multi-line chart, legend toggle, tooltips, responsive).

**Alternatives considered**:
- Chart.js / react-chartjs-2: Rejected — adding a second charting library creates unnecessary dependency bloat when Recharts already handles the use case.
- D3.js: Rejected — too low-level for this use case.
- Nivo: Rejected — no existing usage in the project.

### R3: Current Dashboard Architecture

**Decision**: Replace the entire dashboard page content with the trend chart.

**Rationale**: The current dashboard page (`budget-frontend/src/app/dashboard/page.tsx`) displays 3 monthly budget summary cards with donut charts (PieChart). Per the spec, the pie chart and monthly summary cards will be fully removed and replaced with the spending trend line chart.

The `BudgetPieChart` component (`budget-frontend/src/components/dashboard/BudgetPieChart.tsx`) will no longer be used by the dashboard after the change. It can remain in the codebase as it may be used elsewhere in the future.

### R4: Daily Aggregate Backend Pattern

**Decision**: Follow the same pattern as the existing monthly/yearly aggregates.

**Rationale**: The existing pattern uses:
1. Repository: JPQL query with `GROUP BY` returning `Object[]` arrays
2. Service: `ExpenseAggregateService` processes raw results, handles parent category rollup
3. Controller: `ExpenseController` exposes REST endpoint
4. DTO: `CategoryExpenseAggregateDTO` carries the result (add `day` field)

For daily aggregates, the query will be:
```
SELECT e.category.id, DAY(e.expenseDate), COALESCE(SUM(e.amount), 0)
FROM Expense e
WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month
GROUP BY e.category.id, DAY(e.expenseDate)
```

### R5: Yearly View — All Years with Data

**Decision**: Use the existing `GET /api/expenses/years` endpoint to determine the year range, then aggregate per year.

**Rationale**: The spec requires that yearly granularity shows all years with expense data. The existing `/api/expenses/years` endpoint returns a list of distinct years that have expenses. The frontend can call this first, then fetch yearly aggregates for each year.

Alternatively, a dedicated endpoint could return multi-year aggregates, but the simpler approach of fetching years + one aggregate call per year is sufficient for a small household dataset (likely <10 years).

### R6: Recharts Legend Toggle Behavior

**Decision**: Recharts Legend supports built-in click handling for toggling line visibility.

**Rationale**: Recharts `Legend` component emits `onClick` events with the `dataKey` of the clicked item. Combined with React state tracking which categories are hidden, this provides the toggle behavior without any additional library. The `Line` component's `hide` prop (or conditional rendering) can control visibility. When lines are hidden, Recharts auto-rescales the Y-axis.
