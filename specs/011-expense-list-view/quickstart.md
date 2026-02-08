# Quickstart: Expense List View

**Feature**: 011-expense-list-view
**Date**: 2026-02-09

## Prerequisites

- MySQL 8.0 running (via `docker-compose up`)
- Spring Boot backend running on port 8080
- Next.js frontend running on port 3000
- At least one budget and some expenses already recorded

## Integration Scenarios

### Scenario 1: View Current Year Expenses (US1 + US2)

**Steps**:
1. Navigate to `/expenses` in the browser
2. The page loads with the year filter pre-set to the current year (2026)
3. A table displays expenses with columns: Date, Description, Category, Amount, Created By
4. Summary bar shows total count and sum (e.g., "156 expenses totaling $4,523.75")
5. Results are paginated at 50 per page, sorted by date descending

**Expected API Call**:
```
GET /api/expenses/list?year=2026&page=0&size=50&sortBy=expenseDate&sortDirection=DESC
```

**Expected Response**: 200 OK with `ExpenseListResponse` containing first page of 2026 expenses.

### Scenario 2: Change Year Filter (US2)

**Steps**:
1. From the expense list page, click the year dropdown
2. Available years are populated from `GET /api/expenses/years`
3. Select "2025"
4. The table auto-updates to show 2025 expenses
5. Summary updates with 2025 totals

**Expected API Calls**:
```
GET /api/expenses/years
GET /api/expenses/list?year=2025&page=0&size=50&sortBy=expenseDate&sortDirection=DESC
```

### Scenario 3: Apply Multiple Filters (US3)

**Steps**:
1. From the expense list page (year=2026)
2. Select month: "March"
3. Table auto-updates to March 2026 expenses
4. Select category: "Groceries"
5. Table auto-updates to March 2026 Groceries expenses
6. Set min amount: 20, max amount: 100
7. Table auto-updates to filtered results
8. Summary shows count and total for filtered results

**Expected API Call** (after all filters applied):
```
GET /api/expenses/list?year=2026&month=3&categoryId=5&minAmount=20&maxAmount=100&page=0&size=50&sortBy=expenseDate&sortDirection=DESC
```

### Scenario 4: Sort by Column (US4)

**Steps**:
1. From the expense list page with data showing
2. Click the "Amount" column header
3. Table re-sorts by amount ascending, sort indicator appears on Amount column
4. Click "Amount" header again
5. Table re-sorts by amount descending, indicator flips

**Expected API Calls**:
```
GET /api/expenses/list?year=2026&page=0&size=50&sortBy=amount&sortDirection=ASC
GET /api/expenses/list?year=2026&page=0&size=50&sortBy=amount&sortDirection=DESC
```

### Scenario 5: Navigate Pages

**Steps**:
1. From the expense list page showing 156 total expenses
2. Click "Next Page" or page 2
3. Table shows expenses 51-100
4. Click "Previous Page"
5. Table shows expenses 1-50

**Expected API Calls**:
```
GET /api/expenses/list?year=2026&page=1&size=50&sortBy=expenseDate&sortDirection=DESC
GET /api/expenses/list?year=2026&page=0&size=50&sortBy=expenseDate&sortDirection=DESC
```

### Scenario 6: Clear All Filters

**Steps**:
1. From the expense list page with multiple filters active
2. Click "Clear Filters" button
3. Year resets to current year, all optional filters removed
4. Table shows all expenses for current year

### Scenario 7: Empty State

**Steps**:
1. Select a year with no expenses (or apply filters that match nothing)
2. Table area shows empty state message: "No expenses found for the selected filters"
3. Summary shows "0 expenses totaling $0.00"

## Backend API Quick Test

```bash
# Get available years
curl -H "X-Hass-User: dev-user" http://localhost:8080/api/expenses/years

# Get expense creators
curl -H "X-Hass-User: dev-user" http://localhost:8080/api/expenses/creators

# Get paginated expense list (current year, defaults)
curl -H "X-Hass-User: dev-user" "http://localhost:8080/api/expenses/list?year=2026"

# Get filtered list (March 2026, Groceries, sorted by amount)
curl -H "X-Hass-User: dev-user" "http://localhost:8080/api/expenses/list?year=2026&month=3&categoryId=5&sortBy=amount&sortDirection=ASC"

# Get filtered list with amount range
curl -H "X-Hass-User: dev-user" "http://localhost:8080/api/expenses/list?year=2026&minAmount=10&maxAmount=100&page=0&size=50"
```

## Navigation Integration

The expense list is accessible via:
- Main navigation sidebar: "Expenses" menu item at `/expenses`
- Breadcrumb trail: Home > Expenses
