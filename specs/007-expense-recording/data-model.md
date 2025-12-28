# Data Model: Expense Recording

**Feature**: 007-expense-recording
**Date**: 2025-12-22

## Overview

This feature introduces **no new backend entities**. All data structures already exist from Feature 002 (Budget Management) and Feature 005 (Category Management). This document describes how existing entities are used in the expense recording flow.

---

## Entity Relationships

```
┌─────────────────┐
│   User          │ (Home Assistant identity via X-Hass-User header)
│                 │
│ - username      │ (from header, no DB entity)
└────────┬────────┘
         │ creates
         │ (createdBy field)
         ▼
┌─────────────────┐         ┌─────────────────┐
│   Expense       │────────>│   Category      │
│   (EXISTING)    │ belongs │   (EXISTING)    │
│                 │    to   │                 │
│ - id            │         │ - id            │
│ - amount        │         │ - name          │
│ - description   │         │ - icon          │
│ - expenseDate   │         │ - parentId      │
│ - budgetId      │         │ - createdBy     │
│ - categoryId    │         │ - isSystem      │
│ - createdBy     │         └─────────────────┘
│ - createdAt     │                 │
│ - updatedAt     │                 │ child of
│ - version       │                 ▼
└────────┬────────┘         ┌─────────────────┐
         │                  │   Category      │
         │ counted          │   (parent)      │
         │ against          └─────────────────┘
         ▼
┌─────────────────┐
│   Budget        │
│   (EXISTING)    │
│                 │
│ - id            │
│ - name          │
│ - startDate     │
│ - endDate       │
│ - totalBudget   │
│ - categoryBudget│ (map: categoryId -> amount)
│ - createdBy     │
└─────────────────┘
```

---

## Entities (All Existing)

### Expense (Feature 002)

**Backend**: `ExpenseEntity` (JPA), `ExpenseDTO` (API)
**Frontend**: `ExpenseDTO` interface (TypeScript)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | number | Auto | Primary key |
| amount | number | Yes | Expense amount (decimal, 2 places) |
| description | string | Yes | Free-text description (1-500 chars) |
| expenseDate | string | Yes | ISO date (YYYY-MM-DD) |
| budgetId | number | Yes | Foreign key to Budget |
| categoryId | number | Optional | Foreign key to Category |
| categoryName | string | Read-only | Denormalized for display |
| categoryIcon | string | Read-only | Denormalized for display |
| createdBy | string | Auto | X-Hass-User header value |
| createdAt | string | Auto | ISO timestamp |
| updatedAt | string | Auto | ISO timestamp |
| version | number | Auto | Optimistic locking |
| warnings | string[] | Optional | Business warnings (e.g., date mismatch) |

**Validation Rules** (Backend):
- `amount > 0`
- `description` length 1-500
- `budgetId` must reference existing budget
- `categoryId` must reference existing category (if provided)
- `expenseDate` must be within budget's date range (warning if not)

**Indexes** (Database):
- Primary key: `id`
- Foreign keys: `budgetId`, `categoryId`
- Query optimization: `(budgetId, expenseDate)`, `(categoryId, expenseDate)`

---

### Category (Feature 005)

**Backend**: `CategoryEntity` (JPA), `CategoryDTO` (API)
**Frontend**: `CategoryDTO` interface (TypeScript)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | number | Auto | Primary key |
| name | string | Yes | Category name (unique) |
| icon | string | Optional | Material icon name |
| parentCategoryId | number | Optional | Foreign key to parent Category |
| parentCategory | CategoryDTO | Read-only | Nested parent object |
| childCategories | CategoryDTO[] | Read-only | Nested children array |
| createdBy | string | Auto | X-Hass-User header value |
| createdAt | string | Auto | ISO timestamp |
| updatedAt | string | Auto | ISO timestamp |
| version | number | Auto | Optimistic locking |
| isSystem | boolean | Auto | System-defined category (cannot delete) |
| expenseCount | number | Read-only | Count of expenses in category |
| budgetCount | number | Read-only | Count of budgets using category |

**Hierarchy Rules**:
- Maximum depth: 2 levels (parent + child)
- Parent category cannot have a parent (no grandparents)
- Deleting parent category reassigns children to null (top-level)

**Used In Expense Recording**:
- Expense form fetches all categories via `GET /api/categories`
- User selects category from Autocomplete dropdown
- Selected `categoryId` is included in `CreateExpenseRequest`
- Parent category hierarchy is displayed in dropdown for clarity ("Parent > Child")

---

### Budget (Feature 002)

**Backend**: `BudgetEntity` (JPA), `BudgetDTO` (API)
**Frontend**: `BudgetDTO` interface (TypeScript)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | number | Auto | Primary key |
| name | string | Yes | Budget name (e.g., "January 2025") |
| startDate | string | Yes | ISO date (YYYY-MM-DD) |
| endDate | string | Yes | ISO date (YYYY-MM-DD) |
| totalBudget | number | Yes | Overall budget amount |
| categoryBudgets | Map<number, number> | Optional | Per-category budget allocations |
| totalSpent | number | Read-only | Sum of all expenses in budget |
| remainingBudget | number | Read-only | totalBudget - totalSpent |
| createdBy | string | Auto | X-Hass-User header value |
| createdAt | string | Auto | ISO timestamp |
| updatedAt | string | Auto | ISO timestamp |
| version | number | Auto | Optimistic locking |

**Date Range Constraint**:
- `startDate <= endDate`
- Budgets should not overlap (enforced by business logic, not DB)

**Used In Expense Recording**:
- Backend auto-selects budget where `expenseDate BETWEEN startDate AND endDate`
- If multiple budgets match (data integrity issue), returns error
- If no budget matches, expense still created but `warnings` field populated

---

### User (Home Assistant)

**No Database Entity** - User identity is determined by `X-Hass-User` HTTP header

| Field | Source | Description |
|-------|--------|-------------|
| username | X-Hass-User header | Home Assistant username (e.g., "alice", "bob") |

**Authentication Flow**:
1. User accesses frontend via Home Assistant proxy
2. Nginx adds `X-Hass-User: [username]` header to all requests
3. Backend reads header and populates `createdBy` field
4. No user table, no user authentication logic

**Multi-User Support**:
- Each expense is tagged with creator's username
- Expense list can be filtered by `createdBy`
- Budget and category visibility is shared across all household members

---

## Frontend Data Models

### ExpenseFormState

**Purpose**: Local component state for expense creation form

```typescript
interface ExpenseFormState {
  amount: string;          // String for input control, convert to number on submit
  description: string;     // Free-text input
  expenseDate: string;     // ISO date string (YYYY-MM-DD)
  categoryId: number | null; // Selected category ID
  budgetId: number | null; // Auto-selected budget ID (hidden from user)
  errors: Record<string, string>; // Validation error messages
  loading: boolean;        // Submission in progress
  successMessage: string | null; // Post-submission feedback
  errorMessage: string | null;   // Submission error feedback
}
```

**Initialization**:
```typescript
const initialState: ExpenseFormState = {
  amount: '',
  description: '',
  expenseDate: getTodayISO(), // Default to today
  categoryId: null,
  budgetId: null,
  errors: {},
  loading: false,
  successMessage: null,
  errorMessage: null,
};
```

---

### CreateExpenseRequest (Existing)

**Purpose**: API request DTO for expense creation

```typescript
interface CreateExpenseRequest {
  amount: number;          // Parsed from form string
  description: string;     // Trimmed
  expenseDate: string;     // ISO date (YYYY-MM-DD)
  budgetId: number;        // Auto-selected from date
  categoryId?: number | null; // Optional category
}
```

**Conversion from Form State**:
```typescript
const buildRequest = (formState: ExpenseFormState): CreateExpenseRequest => {
  return {
    amount: parseFloat(formState.amount),
    description: formState.description.trim(),
    expenseDate: formState.expenseDate,
    budgetId: formState.budgetId!, // Must be set by budget lookup
    categoryId: formState.categoryId,
  };
};
```

---

## Data Flow

### 1. Form Initialization

```
User navigates to /expenses/new
  ↓
Page component mounts
  ↓
useEffect triggers:
  1. Fetch categories: GET /api/categories
  2. Initialize form with default values (date = today)
  ↓
Form renders with:
  - Date picker (defaulted to today)
  - Amount input (empty)
  - Description input (empty)
  - Category Autocomplete (populated with categories)
```

### 2. User Input & Validation

```
User enters amount: "42.50"
  ↓
onChange handler updates formState.amount
  ↓
onBlur triggers validation:
  - Check amount > 0
  - Check valid number format
  ↓
Display inline error if invalid

User types description: "Groceries at Whole Foods"
  ↓
onChange handler updates formState.description
  ↓
onBlur triggers validation:
  - Check length > 0
  - Check length <= 500
  ↓
Display inline error if invalid

User selects category: "Groceries" (id: 5)
  ↓
onChange handler updates formState.categoryId = 5
  ↓
Required field validation passes

User changes date: "2025-12-15"
  ↓
onChange handler updates formState.expenseDate
  ↓
Trigger budget lookup (see step 3)
```

### 3. Budget Auto-Selection

```
User selects/changes expense date
  ↓
Call budgetService.getBudgets()
  ↓
Filter budgets where date BETWEEN startDate AND endDate
  ↓
If exactly 1 budget matches:
  - Set formState.budgetId = budget.id
  - Enable submit button
If 0 budgets match:
  - Set formState.errorMessage = "No budget found for [date]"
  - Disable submit button
If >1 budgets match:
  - Set formState.errorMessage = "Multiple budgets found (data integrity issue)"
  - Disable submit button
```

### 4. Form Submission

```
User clicks "Create Expense" button
  ↓
Validate all fields (amount, description, category, date, budgetId)
  ↓
If validation fails:
  - Display inline errors
  - Return without submission
If validation passes:
  ↓
Build CreateExpenseRequest from formState
  ↓
Call expenseService.createExpense(request)
  ↓
Backend processes:
  1. Read X-Hass-User header → createdBy
  2. Validate budgetId and categoryId exist
  3. Check expenseDate within budget range (warn if not)
  4. Insert ExpenseEntity into database
  5. Return ExpenseDTO
  ↓
Frontend receives response:
  - Success (200): Display success message, navigate to /
  - Error (400/500): Display error message in Snackbar
```

### 5. Backend Budget Allocation

```
Expense created with categoryId = 5, budgetId = 10
  ↓
Backend updates budget:
  - Increment totalSpent by expense.amount
  - If categoryBudgets[5] exists, increment by expense.amount
  - Recalculate remainingBudget = totalBudget - totalSpent
  ↓
If expense has parent category (e.g., category 5 has parent 3):
  - Also increment categoryBudgets[3] by expense.amount
  ↓
Return updated budget in response (optional, not used in UI)
```

---

## Data Validation Matrix

| Field | Frontend Validation | Backend Validation | Error Handling |
|-------|-------------------|-------------------|----------------|
| amount | Required, > 0, numeric, max 2 decimals | Required, > 0, numeric | Inline error + Snackbar |
| description | Required, 1-500 chars, trimmed | Required, 1-500 chars | Inline error |
| expenseDate | Required, valid ISO date | Required, valid date, within budget range (warn) | Inline error + Snackbar |
| categoryId | Required, exists in fetched categories | Required, exists in DB | Inline error + Snackbar |
| budgetId | Auto-selected, must exist for date | Required, exists in DB | Snackbar error if not found |
| createdBy | N/A (read from header) | Required, auto-populated from header | Backend error if header missing |

---

## Database Queries (Backend)

### Query 1: Find Budget by Date

```sql
SELECT * FROM budgets
WHERE ? BETWEEN start_date AND end_date
LIMIT 2; -- Check for multiple matches
```

**Purpose**: Auto-select budget for expense based on expenseDate
**Frequency**: Once per expense creation
**Index**: `(start_date, end_date)` composite index

---

### Query 2: Fetch All Categories

```sql
SELECT * FROM categories
ORDER BY parent_category_id NULLS FIRST, name ASC;
```

**Purpose**: Populate category dropdown in expense form
**Frequency**: Once per page load
**Index**: `(parent_category_id, name)` composite index

---

### Query 3: Insert Expense

```sql
INSERT INTO expenses (amount, description, expense_date, budget_id, category_id, created_by, created_at, updated_at, version)
VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)
RETURNING *;
```

**Purpose**: Create new expense record
**Frequency**: Once per form submission
**Triggers**: May trigger budget totalSpent recalculation

---

### Query 4: Update Budget Spent

```sql
UPDATE budgets
SET total_spent = (SELECT SUM(amount) FROM expenses WHERE budget_id = ?),
    updated_at = NOW(),
    version = version + 1
WHERE id = ?;
```

**Purpose**: Recalculate budget totals after expense insertion
**Frequency**: Once per expense creation
**Index**: `(budget_id)` on expenses table

---

## Performance Considerations

### Frontend

1. **Category Caching**: Fetch categories once per page load, not per render
2. **Debounced Budget Lookup**: Wait 300ms after date change before budget lookup
3. **Optimistic UI Updates**: Show loading state during submission, not blocking
4. **Form Reset**: Clear form state after successful submission to prevent duplicate submissions

### Backend

1. **Budget Lookup Index**: Composite index on `(start_date, end_date)` for fast range queries
2. **Category Eager Loading**: Fetch parent category in single query (JOIN) to avoid N+1
3. **Transaction Isolation**: Use READ_COMMITTED to avoid dirty reads during concurrent expense creation
4. **Batch Updates**: If future features support bulk expense import, use batch inserts

---

## Next Steps

Data model complete. Proceed to:
1. Create contracts/ (API specifications)
2. Create quickstart.md (integration scenarios)
3. Update agent context
