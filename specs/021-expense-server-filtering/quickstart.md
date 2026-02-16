# Quickstart: Expense Server-Side Filtering

## Integration Scenarios

### Scenario 1: Multi-Category Filter Request

**Request**:
```
GET /api/expenses/list?year=2026&month=2&categoryIds=5,6,7,8&page=0&size=50&sortBy=expenseDate&sortDirection=DESC
```

**Expected Response**:
```json
{
  "content": [
    { "id": 101, "amount": 45.00, "description": "Restaurant dinner", "expenseDate": "2026-02-14", "categoryId": 6, "categoryName": "Restaurants", ... },
    { "id": 98, "amount": 120.00, "description": "Weekly groceries", "expenseDate": "2026-02-10", "categoryId": 5, "categoryName": "Groceries", ... }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 50,
  "totalAmount": 450.50,
  "sortBy": "expenseDate",
  "sortDirection": "DESC"
}
```

### Scenario 2: No Category Filter (All categories)

**Request**:
```
GET /api/expenses/list?year=2026&page=0&size=50
```

**Behavior**: Returns all expenses for 2026, no category restriction.

### Scenario 3: Single categoryId (backward compatible)

**Request**:
```
GET /api/expenses/list?year=2026&categoryId=3
```

**Behavior**: If category 3 is a parent with children 5,6,7, returns expenses for categories 3,5,6,7 (existing parent-expansion behavior preserved).

### Scenario 4: categoryIds takes precedence over categoryId

**Request**:
```
GET /api/expenses/list?year=2026&categoryId=3&categoryIds=5,6
```

**Behavior**: `categoryIds=5,6` takes precedence. Only expenses for categories 5 and 6 are returned. `categoryId=3` is ignored.

## Frontend Flow

1. User opens expenses page → frontend sends `GET /api/expenses/list?year=2026&size=50`
2. User clicks "Food & Dining" parent chip → frontend computes IDs: [3, 5, 6, 7, 8] → sends `GET /api/expenses/list?year=2026&categoryIds=3,5,6,7,8&size=50`
3. User deselects "Coffee" (ID 7) → frontend sends `GET /api/expenses/list?year=2026&categoryIds=3,5,6,8&size=50`
4. User clicks "All" chip → frontend sends `GET /api/expenses/list?year=2026&size=50` (no categoryIds parameter)
5. User navigates to page 2 → frontend sends `GET /api/expenses/list?year=2026&categoryIds=3,5,6,8&page=1&size=50`
