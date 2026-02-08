# Quickstart: Parent Category Budget & Expense Support

**Feature**: 012-parent-category-budgets
**Date**: 2026-02-09

## Prerequisites

- Running MySQL 8.0 database with existing schema (categories, budgets, expenses tables)
- Running Spring Boot backend (`budget-backend`)
- Running Next.js frontend (`budget-frontend`)
- At least one parent category with child categories (e.g., "Food" → "Groceries", "Dining Out")

## Integration Scenarios

### Scenario 1: Create Budget on Parent Category

**Steps**:
1. Navigate to **Budgets → Create New Budget**
2. In the category dropdown, observe **grouped select**:
   - "Food" appears as a group header AND a selectable item
   - "Groceries" and "Dining Out" appear indented under "Food"
3. Select **"Food"** (the parent category)
4. Set Year: 2026, Month: January, Amount: $500
5. Click **Create Budget**

**Expected Result**:
- Budget created successfully for "Food" category
- Budget appears in budget list with "Food" label
- No "including children" subtotal shown yet (no child budgets exist)

### Scenario 2: Create Child Budget — Parent Category Budget Does Not Exist

**Steps**:
1. Navigate to **Budgets → Create New Budget**
2. Select **"Groceries"** (child of "Food")
3. Set Year: 2026, Month: February, Amount: $300
4. Observe: a checkbox appears: **"Also create budget for parent category 'Food'"** (checked by default)
5. The parent budget amount field shows $300 (same as child amount); change it to $400
6. Click **Create Budget**

**Expected Result**:
- Two budgets created: "Groceries" ($300) and "Food" ($400) for February 2026
- Both appear in budget list

### Scenario 3: Create Child Budget — Parent Category Budget Already Exists

**Steps**:
1. Ensure "Food" has a budget of $500 for January 2026 (from Scenario 1)
2. Navigate to **Budgets → Create New Budget**
3. Select **"Groceries"** (child of "Food")
4. Set Year: 2026, Month: January, Amount: $200
5. Observe: no checkbox appears (parent budget already exists for this period)
6. Click **Create Budget**

**Expected Result**:
- "Groceries" budget created for $200
- "Food" parent category budget auto-incremented from $500 to $700
- Info message displayed: **"Budget for 'Food' has been updated from $500.00 to $700.00 for January 2026."**

### Scenario 4: Expense Aggregation — Child Expense Appears in Parent

**Steps**:
1. Ensure "Food" has a budget of $500 for January 2026
2. Ensure "Groceries" (child of "Food") has a budget for January 2026
3. Create an expense: $50, "Weekly groceries", category: "Groceries", date: Jan 15, 2026
4. Navigate to **Budgets** list

**Expected Result**:
- "Groceries" budget shows $50 spending
- "Food" budget shows $50 spending (aggregated from child "Groceries")
- "Food" budget row shows: **Budget: $500** and below it: **Including children: $700** (if child budgets exist)

### Scenario 5: Expense List Filter by Parent Category

**Steps**:
1. Ensure expenses exist on "Groceries" and "Dining Out" (both children of "Food")
2. Navigate to **Expenses** list
3. Filter by category: **"Food"**

**Expected Result**:
- Expense list shows ALL expenses: those directly on "Food" AND those on "Groceries" and "Dining Out"
- Total amount reflects sum of all three categories' expenses

### Scenario 6: Direct Expense on Parent Category

**Steps**:
1. Navigate to **Expenses → Record Expense**
2. Select category: **"Food"** (parent category)
3. Enter: Amount: $25, Description: "Misc food purchase", Date: Jan 20, 2026
4. Click **Save**

**Expected Result**:
- Expense created and linked to "Food" budget for January 2026
- "Food" budget spending includes this $25 (direct) + any child category expenses

### Scenario 7: Dashboard with Parent + Child Budgets

**Steps**:
1. Ensure multiple budgets exist for current month (parent + child categories)
2. Navigate to **Dashboard**

**Expected Result**:
- Dashboard shows aggregate: total budget = sum of ALL monthly budgets (parent + child)
- Total spending = sum of ALL expenses (each expense counted once, via its own budget)
- No double-counting of expenses (child expense appears in parent aggregation for display, but is only linked to one budget)

## Validation Checklist

- [ ] Parent categories appear in budget creation dropdown (grouped select)
- [ ] Budget can be created on a parent category
- [ ] Checkbox appears for parent category budget when creating child budget (no parent budget exists)
- [ ] Auto-increment happens when parent category budget already exists
- [ ] Info message shows after auto-increment with old/new amounts
- [ ] Budget list shows "including children" subtotal for parent category budgets
- [ ] Expense aggregation: child expenses count toward parent spending
- [ ] Expense filter by parent category includes child category expenses
- [ ] Yearly budget logic unchanged for all categories
- [ ] Dashboard totals are correct (no double-counting)
