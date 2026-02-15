# Quickstart: Dashboard Spending Trends

**Feature**: 019-dashboard-spending-trends
**Date**: 2026-02-16

## Prerequisites

- Backend running with MySQL containing expense data across multiple categories, months, and years
- Frontend dev server running (`npm run dev` in `budget-frontend/`)
- At least 3+ categories with expenses spanning 6+ months for meaningful visualization

## Integration Scenarios

### Scenario 1: Monthly Trend View (Default)

1. Navigate to `/dashboard`
2. The chart loads showing monthly spending trends for the current year
3. Each category with expenses appears as a separate colored line
4. X-axis shows months (Jan–Dec), Y-axis shows amounts
5. Hover over a data point to see category name and formatted amount

**Data flow**: Frontend calls `GET /api/expenses/aggregates/monthly?year=2026` → transforms response into per-category time series → renders LineChart

### Scenario 2: Switch to Daily View

1. From the monthly view, click the "Daily" granularity button
2. Chart switches to show daily data for the current month
3. X-axis shows days 1–28/29/30/31
4. Use prev/next month buttons to navigate

**Data flow**: Frontend calls `GET /api/expenses/aggregates/daily?year=2026&month=2` → transforms into daily time series → renders LineChart

### Scenario 3: Switch to Yearly View

1. From any view, click the "Yearly" granularity button
2. Chart shows one data point per year for all years with data
3. X-axis shows years (e.g., 2024, 2025, 2026)

**Data flow**: Frontend calls `GET /api/expenses/years` to get year list, then `GET /api/expenses/aggregates/yearly?year=X` for each year → combines into yearly time series → renders LineChart

### Scenario 4: Toggle Category Visibility

1. With any chart view loaded showing multiple category lines
2. Click a category name in the legend
3. That category's line hides, Y-axis rescales
4. Click again to show the line

### Scenario 5: Empty State

1. Navigate to `/dashboard` with no expense data
2. Chart area shows an informative message ("No expense data available. Start adding expenses to see spending trends.")

### Scenario 6: Navigate Between Periods

1. In monthly view, click "Previous Year" to see last year's monthly trends
2. In daily view, click "Previous Month" to see last month's daily spending
3. Navigation updates the chart data accordingly

## Quick Verification Checklist

- [ ] Monthly chart renders with correct Y-axis amounts
- [ ] Daily chart shows correct per-day totals
- [ ] Yearly chart spans all years with data
- [ ] Legend click hides/shows category lines
- [ ] Tooltips show currency-formatted amounts
- [ ] Empty state displays when no data exists
- [ ] Responsive on mobile (375px width)
- [ ] Period navigation (prev/next) works in daily and monthly views
