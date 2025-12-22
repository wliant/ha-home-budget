# Phase 1: Data Model - Homepage Dashboard Update

**Feature**: Homepage Dashboard Update (006)
**Date**: 2025-12-22
**Status**: Design Complete

## Overview

This feature reuses existing data models from previous features (budgets, expenses). No new entities or database schema changes are required. This document maps existing entities to homepage widget requirements.

## Existing Entities (Reference)

### Budget Summary (from Feature 002)

**Source**: Backend DTO `BudgetSummaryDTO.java`

**Purpose**: Represents current month budget status for household

**Attributes**:
- `id`: Long - Unique budget identifier
- `month`: Integer - Month number (1-12)
- `year`: Integer - Year (e.g., 2025)
- `amount`: BigDecimal - Total budgeted amount
- `spent`: BigDecimal - Total spent so far
- `remaining`: BigDecimal - Amount remaining (amount - spent)
- `spendingPercentage`: BigDecimal - Percentage of budget used
- `categoryId`: Long (nullable) - Associated category if applicable
- `categoryName`: String (nullable) - Category name for display
- `createdBy`: String - Username from X-Hass-User header
- `createdAt`: LocalDateTime - Creation timestamp
- `updatedAt`: LocalDateTime - Last modification timestamp

**Relationships**:
- One-to-many with Expense (budget contains multiple expenses)
- Many-to-one with Category (optional, for category-specific budgets)

**Validation Rules**:
- `amount` must be > 0
- `month` must be 1-12
- `year` must be current year ± 10 years
- `createdBy` must match X-Hass-User header

**State Transitions**:
- **On track**: spendingPercentage ≤ 80%
- **Warning**: spendingPercentage > 80% and ≤ 100%
- **Overspent**: spendingPercentage > 100%

**Homepage Usage**:
- Budget Summary Card displays: amount, spent, remaining, spendingPercentage
- Visual indicator based on state (green/yellow/red)
- Period display: "{month} {year}" (e.g., "December 2025")

---

### Expense (from Feature 002)

**Source**: Backend DTO `ExpenseDTO.java`

**Purpose**: Records individual spending transactions

**Attributes**:
- `id`: Long - Unique expense identifier
- `amount`: BigDecimal - Expense amount (must be > 0)
- `description`: String - User-provided expense description (max 255 chars)
- `expenseDate`: LocalDate - Date expense occurred
- `budgetId`: Long - Associated budget ID
- `categoryId`: Long (nullable) - Associated category ID
- `categoryName`: String (nullable) - Category name for display
- `categoryIcon`: String (nullable) - Category icon for display
- `createdBy`: String - Username from X-Hass-User header
- `createdAt`: LocalDateTime - Creation timestamp
- `updatedAt`: LocalDateTime - Last modification timestamp
- `version`: Integer - Optimistic locking version
- `warnings`: String[] (nullable) - Validation warnings (e.g., date mismatch)

**Relationships**:
- Many-to-one with Budget (expense belongs to one budget)
- Many-to-one with Category (expense belongs to one category)

**Validation Rules**:
- `amount` must be > 0
- `description` must not be empty, max 255 characters
- `expenseDate` must be within budget period or generate warning
- `budgetId` must reference existing budget
- `createdBy` must match X-Hass-User header

**Homepage Usage**:
- Recent Activity Card displays 5 most recent expenses
- Display fields: expenseDate, amount, description, categoryName, createdBy
- Sorted by: expenseDate DESC (most recent first)
- Filter: None (show all household expenses)

---

### System Status (from Feature 001)

**Source**: Backend health endpoint response

**Purpose**: Backend service health information

**Attributes**:
- `status`: String - Health status ("UP", "DOWN")
- `service`: String - Service name (e.g., "home-budget-backend")
- `version`: String - Backend version (e.g., "1.0.0-SNAPSHOT")

**Homepage Usage**:
- System Status section (existing functionality, retained)
- Display backend connection health
- Color-coded status indicator

---

## Frontend-Only Types (No Backend Persistence)

### Widget State

**Purpose**: UI state for homepage widgets (not persisted)

**Attributes**:
- `isLoading`: boolean - Data fetch in progress
- `error`: string | null - Error message if fetch failed
- `data`: BudgetSummaryDTO | ExpenseDTO[] | null - Fetched data

**Usage**: Each widget (BudgetSummaryCard, RecentActivityCard) maintains own loading/error state

---

## Data Flow

### Budget Summary Widget

```
User opens homepage
  → BudgetSummaryCard.useEffect()
  → budgetService.getCurrentMonthBudget()
  → GET /api/budgets/current
  → Backend queries current month budget
  → Returns BudgetSummaryDTO
  → Widget displays budget summary
```

**Query Logic** (Backend):
```sql
SELECT * FROM budgets
WHERE month = MONTH(CURRENT_DATE)
  AND year = YEAR(CURRENT_DATE)
LIMIT 1
```

### Recent Activity Widget

```
User opens homepage
  → RecentActivityCard.useEffect()
  → expenseService.getAllExpenses({ limit: 5, sort: 'date,desc' })
  → GET /api/expenses?limit=5&sort=date,desc
  → Backend queries recent expenses
  → Returns ExpenseDTO[]
  → Widget displays expense list
```

**Query Logic** (Backend):
```sql
SELECT * FROM expenses
ORDER BY expense_date DESC
LIMIT 5
```

---

## No Schema Changes

**Database**: MySQL 8.0 (existing)

**Tables Used**:
- `budgets` - Existing table (Feature 002)
- `expenses` - Existing table (Feature 002)
- `categories` - Existing table (Feature 004)

**No Migrations Required**: All necessary tables and columns already exist

---

## Data Validation

### Frontend Validation

**Budget Summary**:
- Verify `spendingPercentage` calculation: (spent / amount) * 100
- Handle null budget (no budget for current month → show empty state)
- Format currency using existing `formatCurrency()` utility

**Recent Expenses**:
- Verify date formatting using existing `formatExpenseDate()` utility
- Handle empty array (no expenses → show empty state)
- Truncate long descriptions (max 50 chars with ellipsis)

### Backend Validation

**No new validation rules**: Uses existing validation from Features 002 and 004

---

## Assumptions

1. **Current month budget exists**: Most users will have created a budget for the current month (empty state handles exception)
2. **Recent expenses exist**: Household has recorded at least some expenses (empty state handles exception)
3. **Backend APIs available**: Homepage assumes backend is running (system status widget shows health)
4. **Household-wide data**: All users see same budget and expenses (no user-level filtering per constitution)

---

## Performance Considerations

### Query Performance

**Budget Summary**:
- Query: Index on (month, year) → Fast lookup (typically 1 row)
- Expected: <50ms query time

**Recent Expenses**:
- Query: Index on expense_date DESC → Fast sort
- Limit: 5 rows only
- Expected: <100ms query time

### Frontend Rendering

**Budget Summary Card**:
- Data size: ~200 bytes JSON
- Render time: <10ms (simple card with 4 text fields + progress bar)

**Recent Activity Card**:
- Data size: ~1KB JSON (5 expenses @ ~200 bytes each)
- Render time: <20ms (list of 5 items)

**Total Homepage Load**:
- API calls: 2 parallel requests → ~100ms (max of both)
- Rendering: ~30ms
- **Total**: <150ms (well under 2s target)

---

## Future Extensions (Out of Scope)

- **Budget comparison**: Show previous month budget for comparison (Future Feature 007)
- **Spending trends**: Chart showing spending over time (Future Feature 008)
- **Category breakdown**: Pie chart of spending by category (Future Feature 008)
- **Budget alerts**: Notifications when approaching budget limit (Future Feature 009)

---

## Data Model Approval

**Status**: ✅ Approved
**Schema Changes**: None required
**Breaking Changes**: None
**Migration Required**: No

**Ready for**: Contract generation (Phase 1 continued)
