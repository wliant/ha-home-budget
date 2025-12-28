# API Contract: Expense Recording

**Feature**: 007-expense-recording
**Date**: 2025-12-22
**Note**: All endpoints below already exist from Feature 002. No new backend endpoints required.

---

## Endpoints Used

### 1. Create Expense

**Endpoint**: `POST /api/expenses`

**Description**: Create a new expense record. Automatically associates expense with appropriate budget based on date.

**Authentication**: X-Hass-User header (required)

**Request Headers**:
```http
Content-Type: application/json
X-Hass-User: alice
```

**Request Body**:
```json
{
  "amount": 42.50,
  "description": "Groceries at Whole Foods",
  "expenseDate": "2025-12-22",
  "budgetId": 10,
  "categoryId": 5
}
```

**Request Schema**:
```typescript
interface CreateExpenseRequest {
  amount: number;          // Required, > 0, max 2 decimal places
  description: string;     // Required, 1-500 characters
  expenseDate: string;     // Required, ISO date format (YYYY-MM-DD)
  budgetId: number;        // Required, must reference existing budget
  categoryId?: number | null; // Optional, must reference existing category if provided
}
```

**Success Response (201 Created)**:
```json
{
  "id": 123,
  "amount": 42.50,
  "description": "Groceries at Whole Foods",
  "expenseDate": "2025-12-22",
  "budgetId": 10,
  "categoryId": 5,
  "categoryName": "Groceries",
  "categoryIcon": "shopping_cart",
  "createdBy": "alice",
  "createdAt": "2025-12-22T14:30:00Z",
  "updatedAt": "2025-12-22T14:30:00Z",
  "version": 0,
  "warnings": []
}
```

**Error Response (400 Bad Request)**:
```json
{
  "timestamp": "2025-12-22T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be greater than 0"
    },
    {
      "field": "categoryId",
      "message": "Category with ID 999 does not exist"
    }
  ]
}
```

**Error Response (404 Not Found)**:
```json
{
  "timestamp": "2025-12-22T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Budget with ID 999 does not exist"
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "timestamp": "2025-12-22T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "X-Hass-User header is required"
}
```

**Response Schema**:
```typescript
interface ExpenseDTO {
  id: number;              // Auto-generated primary key
  amount: number;          // Expense amount
  description: string;     // User-provided description
  expenseDate: string;     // ISO date (YYYY-MM-DD)
  budgetId: number;        // Associated budget ID
  categoryId?: number | null; // Associated category ID (optional)
  categoryName?: string;   // Denormalized category name
  categoryIcon?: string;   // Denormalized category icon
  createdBy: string;       // Username from X-Hass-User header
  createdAt: string;       // ISO timestamp
  updatedAt: string;       // ISO timestamp
  version: number;         // Optimistic locking version
  warnings?: string[];     // Business warnings (e.g., date mismatch)
}
```

**Business Rules**:
1. `createdBy` is automatically populated from `X-Hass-User` header
2. If `expenseDate` is outside budget's date range, a warning is added to `warnings` array
3. Budget's `totalSpent` is automatically incremented by `amount`
4. If category has a parent, parent category budget is also incremented

**Validation Rules**:
- `amount`: Required, must be > 0, max 2 decimal places
- `description`: Required, 1-500 characters, trimmed
- `expenseDate`: Required, valid ISO date format
- `budgetId`: Required, must reference existing budget
- `categoryId`: Optional, must reference existing category if provided
- `X-Hass-User` header: Required, must be present

**Example curl**:
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "amount": 42.50,
    "description": "Groceries at Whole Foods",
    "expenseDate": "2025-12-22",
    "budgetId": 10,
    "categoryId": 5
  }'
```

---

### 2. Get All Categories

**Endpoint**: `GET /api/categories`

**Description**: Fetch all categories for populating expense form dropdown. Returns hierarchical category structure.

**Authentication**: X-Hass-User header (required)

**Request Headers**:
```http
X-Hass-User: alice
```

**Request Parameters**: None

**Success Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "Food",
    "icon": "restaurant",
    "parentCategoryId": null,
    "parentCategory": null,
    "childCategories": [
      {
        "id": 5,
        "name": "Groceries",
        "icon": "shopping_cart",
        "parentCategoryId": 1,
        "expenseCount": 12,
        "budgetCount": 3
      },
      {
        "id": 6,
        "name": "Dining Out",
        "icon": "local_dining",
        "parentCategoryId": 1,
        "expenseCount": 8,
        "budgetCount": 2
      }
    ],
    "createdBy": "alice",
    "createdAt": "2025-01-01T00:00:00Z",
    "updatedAt": "2025-01-01T00:00:00Z",
    "version": 0,
    "isSystem": false,
    "expenseCount": 20,
    "budgetCount": 5
  },
  {
    "id": 2,
    "name": "Transportation",
    "icon": "directions_car",
    "parentCategoryId": null,
    "parentCategory": null,
    "childCategories": [],
    "createdBy": "bob",
    "createdAt": "2025-01-02T00:00:00Z",
    "updatedAt": "2025-01-02T00:00:00Z",
    "version": 0,
    "isSystem": false,
    "expenseCount": 5,
    "budgetCount": 2
  }
]
```

**Response Schema**:
```typescript
interface CategoryDTO {
  id: number;
  name: string;
  icon?: string;
  parentCategoryId?: number;
  parentCategory?: CategoryDTO;  // Nested parent object
  childCategories?: CategoryDTO[]; // Nested children array
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  isSystem: boolean;
  expenseCount: number;   // Count of expenses in this category
  budgetCount: number;    // Count of budgets using this category
}
```

**Example curl**:
```bash
curl -X GET http://localhost:8080/api/categories \
  -H "X-Hass-User: alice"
```

---

### 3. Get Budgets (for auto-selection)

**Endpoint**: `GET /api/budgets`

**Description**: Fetch budgets to auto-select budget based on expense date. Frontend filters by date range.

**Authentication**: X-Hass-User header (required)

**Request Headers**:
```http
X-Hass-User: alice
```

**Request Parameters**: None (frontend filters client-side)

**Success Response (200 OK)**:
```json
[
  {
    "id": 10,
    "name": "December 2025",
    "startDate": "2025-12-01",
    "endDate": "2025-12-31",
    "totalBudget": 3000.00,
    "totalSpent": 1250.50,
    "remainingBudget": 1749.50,
    "categoryBudgets": {
      "1": 500.00,
      "5": 800.00
    },
    "createdBy": "alice",
    "createdAt": "2025-11-30T00:00:00Z",
    "updatedAt": "2025-12-22T14:30:00Z",
    "version": 12
  },
  {
    "id": 11,
    "name": "January 2026",
    "startDate": "2026-01-01",
    "endDate": "2026-01-31",
    "totalBudget": 3200.00,
    "totalSpent": 0.00,
    "remainingBudget": 3200.00,
    "categoryBudgets": {},
    "createdBy": "bob",
    "createdAt": "2025-12-28T00:00:00Z",
    "updatedAt": "2025-12-28T00:00:00Z",
    "version": 0
  }
]
```

**Response Schema**:
```typescript
interface BudgetDTO {
  id: number;
  name: string;
  startDate: string;       // ISO date (YYYY-MM-DD)
  endDate: string;         // ISO date (YYYY-MM-DD)
  totalBudget: number;
  totalSpent: number;      // Calculated from expenses
  remainingBudget: number; // totalBudget - totalSpent
  categoryBudgets: Record<number, number>; // categoryId -> budget amount
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}
```

**Frontend Usage**:
```typescript
// Filter budgets by expense date
const budget = budgets.find(b =>
  expenseDate >= b.startDate && expenseDate <= b.endDate
);

if (!budget) {
  // Show error: No budget found for this date
} else {
  // Auto-select this budget
  formState.budgetId = budget.id;
}
```

**Example curl**:
```bash
curl -X GET http://localhost:8080/api/budgets \
  -H "X-Hass-User: alice"
```

---

## Error Codes

| Status Code | Scenario | Frontend Handling |
|-------------|----------|-------------------|
| 200 OK | Successful GET request | Display data |
| 201 Created | Expense created successfully | Show success message, navigate to homepage |
| 400 Bad Request | Validation failed (amount <= 0, description empty, invalid categoryId) | Display inline field errors or Snackbar |
| 401 Unauthorized | X-Hass-User header missing | Show error "Authentication required" |
| 404 Not Found | budgetId or categoryId does not exist | Show error "Budget/Category not found" |
| 500 Internal Server Error | Database error, unexpected exception | Show error "Failed to create expense. Please try again." |

---

## Sequence Diagram

```
User                 Frontend              Backend               Database
  |                     |                     |                     |
  |--[Navigate to]----->|                     |                     |
  |  /expenses/new      |                     |                     |
  |                     |                     |                     |
  |                     |--GET /api/categories->|                   |
  |                     |                     |--SELECT categories->|
  |                     |                     |<---[categories]-----|
  |                     |<---[CategoryDTO[]]--|                     |
  |                     |                     |                     |
  |<--[Render form]-----|                     |                     |
  |  (date=today)       |                     |                     |
  |                     |                     |                     |
  |--[Enter amount]---->|                     |                     |
  |--[Enter desc]------>|                     |                     |
  |--[Select category]->|                     |                     |
  |--[Change date]----->|                     |                     |
  |                     |                     |                     |
  |                     |--GET /api/budgets--->|                   |
  |                     |                     |--SELECT budgets---->|
  |                     |                     |<---[budgets]--------|
  |                     |<---[BudgetDTO[]]-----|                   |
  |                     |                     |                     |
  |                     |--[Filter by date]-->|                     |
  |                     |--[Auto-select]----->|                     |
  |                     |                     |                     |
  |<--[Enable submit]---|                     |                     |
  |                     |                     |                     |
  |--[Click Submit]---->|                     |                     |
  |                     |--POST /api/expenses->|                   |
  |                     |  (X-Hass-User: alice)|                   |
  |                     |  CreateExpenseReq   |                     |
  |                     |                     |--INSERT expense---->|
  |                     |                     |--UPDATE budget----->|
  |                     |                     |<---[expense]--------|
  |                     |<---[ExpenseDTO]-----|                     |
  |                     |                     |                     |
  |<--[Success msg]-----|                     |                     |
  |<--[Navigate to /]---|                     |                     |
```

---

## Frontend Service Integration

### expenseService.ts (Existing)

```typescript
import api from './api';

export interface CreateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string;
  budgetId: number;
  categoryId?: number | null;
}

export interface ExpenseDTO {
  id: number;
  amount: number;
  description: string;
  expenseDate: string;
  budgetId: number;
  categoryId?: number | null;
  categoryName?: string;
  categoryIcon?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  warnings?: string[];
}

export const expenseService = {
  createExpense: async (request: CreateExpenseRequest): Promise<ExpenseDTO> => {
    const response = await api.post<ExpenseDTO>('/api/expenses', request);
    return response.data;
  },
};
```

### categoryService.ts (Existing)

```typescript
import api from './api';
import { CategoryDTO } from '@/types/category';

export const categoryService = {
  getCategories: async (): Promise<CategoryDTO[]> => {
    const response = await api.get<CategoryDTO[]>('/api/categories');
    return response.data;
  },
};
```

### budgetService.ts (Existing)

```typescript
import api from './api';

export interface BudgetDTO {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  totalBudget: number;
  totalSpent: number;
  remainingBudget: number;
  categoryBudgets: Record<number, number>;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export const budgetService = {
  getBudgets: async (): Promise<BudgetDTO[]> => {
    const response = await api.get<BudgetDTO[]>('/api/budgets');
    return response.data;
  },
};
```

---

## Testing Considerations

### Contract Testing

**Tools**: Not required for this feature (existing endpoints already tested in Feature 002)

**If implementing contract tests**:
- Use Pact or Spring Cloud Contract
- Verify `POST /api/expenses` request/response schema
- Verify `GET /api/categories` response schema
- Verify `GET /api/budgets` response schema

### Integration Testing

**Backend** (Feature 002 coverage):
- ✅ Test expense creation with valid data
- ✅ Test expense creation with invalid categoryId (404)
- ✅ Test expense creation with missing X-Hass-User (401)
- ✅ Test budget totalSpent increment after expense creation

**Frontend** (This feature):
- Test form submission with valid data → success message
- Test form submission with validation errors → inline errors
- Test budget auto-selection when date changes
- Test category dropdown population on mount
- Test error handling when backend returns 400/500

### Manual Testing Scenarios

1. **Happy Path**:
   - Navigate to `/expenses/new`
   - Verify date defaults to today
   - Enter amount: 50.00
   - Enter description: "Test expense"
   - Select category: "Groceries"
   - Click submit
   - Verify success message
   - Verify navigation to homepage

2. **Validation Errors**:
   - Leave amount empty → "Amount is required"
   - Enter negative amount → "Amount must be greater than 0"
   - Leave description empty → "Description is required"
   - Leave category empty → "Category is required"

3. **No Budget for Date**:
   - Change date to far future (e.g., 2030-01-01)
   - Verify error message "No budget found for 2030-01-01"
   - Verify submit button disabled

4. **Backend Error**:
   - Stop backend server
   - Try to submit expense
   - Verify error message "Failed to create expense. Please try again."

---

## Next Steps

API contract complete. Proceed to:
1. Create quickstart.md (integration scenarios)
2. Update agent context
