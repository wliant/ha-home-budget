# Quickstart: Category Expense Aggregates

**Feature**: 016-category-expense-aggregates
**Date**: 2026-02-15

## Integration Scenarios

### Scenario 1: Create Expense (Simplified Flow)

**Before**: Expense required budget selection or auto-resolution.
**After**: Expense only requires a category.

```
1. User navigates to /expenses/new
2. User enters: amount=$50, description="Groceries", date=2026-02-15
3. User selects category: "Fresh Produce" (child of "Groceries")
4. User clicks Submit
5. POST /api/expenses { amount: 50, description: "Groceries", expenseDate: "2026-02-15", categoryId: 5 }
6. Backend saves expense with category_id=5, created_by=<X-Hass-User>
7. Response: 201 Created with ExpenseResponse (no budgetId, no warnings)
```

### Scenario 2: View Monthly Category Aggregates

```
1. GET /api/expenses/aggregates/monthly?year=2026&month=2
2. Response:
   [
     { categoryId: 1, categoryName: "Groceries", directAmount: 0, childrenAmount: 150, totalAmount: 150, year: 2026, month: 2 },
     { categoryId: 5, categoryName: "Fresh Produce", parentCategoryId: 1, directAmount: 100, childrenAmount: 0, totalAmount: 100, year: 2026, month: 2 },
     { categoryId: 6, categoryName: "Pantry", parentCategoryId: 1, directAmount: 50, childrenAmount: 0, totalAmount: 50, year: 2026, month: 2 },
     { categoryId: 2, categoryName: "Utilities", directAmount: 80, childrenAmount: 0, totalAmount: 80, year: 2026, month: 2 }
   ]
   Note: "Groceries" totalAmount (150) = sum of children (100 + 50) + direct (0)
```

### Scenario 3: Yearly Budget View with Category Aggregates

```
1. GET /api/budgets/yearly/2026
2. Backend:
   a. Fetches all budgets for 2026
   b. For each category with a budget:
      - Gets monthly expense aggregates from ExpenseRepository (GROUP BY category, MONTH)
      - For parent categories: adds child category aggregates
      - Compares budget amount vs. aggregate spending
3. Response: YearlyBudgetViewDTO with spending from category aggregates
```

### Scenario 4: Delete Budget (No Expense Impact)

```
1. User has budget "Groceries 2026" with ID=10
2. 5 expenses exist for category "Groceries" in 2026
3. DELETE /api/budgets/10
4. Budget is deleted
5. GET /api/expenses?categoryId=1&year=2026 → returns all 5 expenses (unaffected)
```

### Scenario 5: Edit Expense Category

```
1. User edits expense ID=42, changes category from "Fresh Produce" (id=5) to "Pantry" (id=6)
2. PUT /api/expenses/42 { categoryId: 6 }
3. Backend updates expense.category_id = 6
4. Next aggregate query reflects: Fresh Produce decreased, Pantry increased, parent "Groceries" unchanged
```

## Migration Verification

After running the Liquibase migration:

```
1. Verify all expenses have category_id set:
   SELECT COUNT(*) FROM expenses WHERE category_id IS NULL; → should return 0

2. Verify budget_id column is dropped:
   DESCRIBE expenses; → no budget_id column

3. Verify expenses still exist:
   SELECT COUNT(*) FROM expenses; → same count as before migration

4. Verify new index exists:
   SHOW INDEX FROM expenses WHERE Key_name = 'idx_expenses_category_date';
```

## Key API Changes Summary

| Endpoint | Change | Impact |
|----------|--------|--------|
| POST /api/expenses | Remove budgetId from request | Frontend form simplified |
| PUT /api/expenses/{id} | Remove budgetId from request | Frontend edit simplified |
| GET /api/expenses | Remove budgetId filter | Expense list uses category/date filters |
| GET /api/budgets/{id} | No expense list; add categorySpending | Budget detail shows aggregate |
| DELETE /api/budgets/{id} | No cascade to expenses | Safe budget deletion |
| GET /api/budgets/yearly/{year} | Spending from category aggregates | Same response shape, different data source |
| GET /api/budgets/current-month | Spending from category aggregates | Dashboard updated |
| GET /api/expenses/aggregates/monthly | NEW endpoint | Monthly category aggregates |
| GET /api/expenses/aggregates/yearly | NEW endpoint | Yearly category aggregates |
