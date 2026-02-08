# Data Model: Parent Category Budget & Expense Support

**Feature**: 012-parent-category-budgets
**Date**: 2026-02-09

## Entities

### Category (unchanged)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| name | String | unique, not null | |
| icon | String | nullable | Material icon name |
| parent_category_id | Long | FK → categories.id, nullable | Max 2-level hierarchy |
| is_system | Boolean | default false | System categories cannot be deleted |
| created_by | String | not null | X-Hass-User value |
| created_at | DateTime | auto-set | |
| updated_at | DateTime | auto-set | |
| version | Long | optimistic lock | |

**Relationships**:
- One Category → Many Children (self-referential, parent_category_id)
- One Category → Many Budgets
- One Category → Many Expenses

**No changes**: The category model is unaffected. Parent categories can now have budgets (restriction removed at service layer, not data model layer).

### Budget (unchanged schema, changed validation)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| year | Integer | not null, 2000-9999 | |
| month | Integer | nullable, 1-12 | null = yearly budget |
| total_amount | BigDecimal(10,2) | not null, >= 0 | |
| description | String | nullable | |
| category_id | Long | FK → categories.id, not null | **Now allows parent categories** |
| created_by | String | not null | X-Hass-User value |
| created_at | DateTime | auto-set | |
| updated_at | DateTime | auto-set | |
| version | Long | optimistic lock | |

**Relationships**:
- Many Budgets → One Category
- One Budget → Many Expenses

**Validation Changes**:
- **REMOVED**: `countByParentCategoryId(category.getId()) > 0` check that prevented parent categories
- **ADDED**: When creating a monthly budget for a child category, check if parent category has a budget for the same period. If yes, auto-increment parent category budget amount. If no, optionally create via request flag.
- **PRESERVED**: Duplicate budget check (same category + year + month), yearly parent budget logic (month=null)

**Uniqueness**: Unique constraint on (category_id, year, month) remains. This prevents duplicate budgets for any category (parent or child) for the same period.

### Expense (unchanged)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Long | PK, auto-increment | |
| amount | BigDecimal(10,2) | not null, > 0 | |
| description | String | not null | |
| expense_date | LocalDate | not null | |
| budget_id | Long | FK → budgets.id, not null | Cascade delete |
| category_id | Long | FK → categories.id, nullable | |
| created_by | String | not null | X-Hass-User value |
| created_at | DateTime | auto-set | |
| updated_at | DateTime | auto-set | |
| version | Long | optimistic lock | |

**Relationships**:
- Many Expenses → One Budget
- Many Expenses → One Category (optional)

**No schema changes**: Expenses can already reference any category. The change is in how expenses are *aggregated* — child category expenses now count toward parent category spending in views.

## DTO Changes

### BudgetDTO (request — extended)

| New Field | Type | Purpose |
|-----------|------|---------|
| createParentCategoryBudget | Boolean | Flag: create parent category's budget when creating child budget |
| parentCategoryBudgetAmount | BigDecimal | Amount for the parent category budget (if creating) |

### BudgetSummaryDTO (response — extended)

| New Field | Type | Purpose |
|-----------|------|---------|
| childrenBudgetSum | BigDecimal | Sum of all child category budgets for the same period |
| childrenSpending | BigDecimal | Sum of spending across all child category budgets |
| isParentCategory | Boolean | Whether this budget's category has children |

### BudgetValidationDTO (response — extended)

| New Field | Type | Purpose |
|-----------|------|---------|
| parentCategoryBudgetExists | Boolean | Whether the selected category's parent category has a budget for this period |
| parentCategoryBudgetId | Long | ID of parent category's budget (if exists) |
| parentCategoryBudgetAmount | BigDecimal | Amount of parent category's budget (if exists) |
| parentCategoryName | String | Name of the parent category (for display) |

### CreateBudgetRequest (frontend — extended)

| New Field | Type | Purpose |
|-----------|------|---------|
| createParentCategoryBudget | boolean | Flag: create parent category budget |
| parentCategoryBudgetAmount | number | Amount for parent category budget |

## New Repository Queries

### BudgetRepository

```
findByCategoryIdAndYearAndMonthIsNotNull(categoryId, year) → List<Budget>
  -- Find all monthly budgets for a category in a year (already exists as findMonthlyBudgetsForCategory)

findByCategoryIdAndYearAndMonth(categoryId, year, month) → Optional<Budget>
  -- Find specific monthly budget for a category (already exists)

sumBudgetsByChildCategoriesAndPeriod(parentCategoryId, year, month) → BigDecimal
  -- NEW: Sum budget amounts for all child categories of a parent, for a given period

sumExpensesByChildCategoriesAndPeriod(parentCategoryId, year, month) → BigDecimal
  -- NEW: Sum expense amounts across all child category budgets for a given period
```

### ExpenseRepository

```
findByFiltersPageableWithChildCategories(categoryIds, startDate, endDate, ...) → Page<Expense>
  -- MODIFIED: Accept List<Long> categoryIds instead of single categoryId

getFilteredTotalAmountWithChildCategories(categoryIds, startDate, endDate, ...) → BigDecimal
  -- MODIFIED: Accept List<Long> categoryIds instead of single categoryId
```

## State Transitions

### Budget Creation Flow (updated)

```
User selects category (parent or child)
  ├── If parent category selected:
  │     └── Create budget directly (no parent category check needed)
  └── If child category selected:
        ├── Check: does parent category have budget for this period?
        │     ├── YES → Auto-increment parent category budget by child amount
        │     │         Return info: "Parent 'Food' updated from $500 to $700"
        │     └── NO → Show checkbox (default checked) to create parent category budget
        │           ├── Checked → Create both child and parent category budgets
        │           └── Unchecked → Create only child budget
        └── Continue with existing yearly parent budget logic (month=null)
```

### Expense Aggregation Flow (new)

```
View requests budget summary for parent category budget
  └── Server computes:
        ├── Direct spending: expenses on this budget (budget_id match)
        ├── Child spending: expenses on all child category budgets for same period
        └── Total spending = direct + child spending
```
