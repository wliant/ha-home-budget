# API Behavior Changes: Parent Category Budget Auto-Rollup

**Feature**: 017-parent-budget-rollup
**Date**: 2026-02-15
**Phase**: Phase 1 (Contracts)

## Overview

This feature modifies the **behavior** of existing budget API endpoints to automatically cascade budget changes to parent categories. **No endpoint signatures change**; the auto-rollup is transparent to frontend clients.

## Affected Endpoints

### POST /api/budgets (Create Budget)

**Endpoint Signature**: No change
```http
POST /api/budgets
Content-Type: application/json
X-Hass-User: {username}

{
  "categoryId": 123,
  "year": 2026,
  "month": 1,          // nullable (null = yearly budget)
  "amount": 500.00,
  "description": "Monthly grocery budget"
}
```

**New Behavior** (Feature 017):
1. Create child budget with provided amount
2. **Automatic cascade**: If category is a child category (has parent_category_id):
   - Find or create parent category budget for same period (year, month)
   - Add child budget amount to parent budget amount
   - Save parent budget in same transaction
3. Return created child budget DTO (parent cascade is transparent)

**Response**: No change
```json
{
  "id": 456,
  "categoryId": 123,
  "categoryName": "Fresh Produce",
  "categoryIcon": "🥦",
  "year": 2026,
  "month": 1,
  "amount": 500.00,
  "description": "Monthly grocery budget",
  "createdBy": "john",
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z",
  "version": 0
}
```

**Error Handling**:
- If parent cascade fails (e.g., database constraint violation), entire transaction rolls back (child budget NOT created)
- HTTP 500 error returned: `{"error": "Failed to create budget and update parent category budget"}`

**Example Cascade Scenario**:
```
Request: Create budget for "Fresh Produce" (child of "Groceries") - $500, January 2026

Backend Actions:
1. Save child budget: Fresh Produce, $500, January 2026
2. Find parent budget: Groceries, January 2026 (not found)
3. Create parent budget: Groceries, $0, January 2026
4. Update parent amount: $0 + $500 = $500
5. Save parent budget: Groceries, $500, January 2026
6. Commit transaction (both saves succeed)

Response: Child budget DTO (parent cascade transparent to frontend)
```

---

### PUT /api/budgets/{id} (Update Budget)

**Endpoint Signature**: No change
```http
PUT /api/budgets/{id}
Content-Type: application/json
X-Hass-User: {username}

{
  "categoryId": 123,
  "year": 2026,
  "month": 1,
  "amount": 700.00,     // Updated from 500.00
  "description": "Increased grocery budget"
}
```

**New Behavior** (Feature 017):
1. Load existing child budget (to calculate delta)
2. Update child budget with new amount
3. **Automatic cascade**: If category is a child category:
   - Calculate delta: newAmount - oldAmount (e.g., 700 - 500 = +200)
   - Find parent category budget for same period
   - Add delta to parent budget amount (parent amount += 200)
   - Save parent budget in same transaction
4. Return updated child budget DTO

**Response**: No change (same structure as POST)

**Error Handling**:
- If parent budget not found (should never happen after Feature 017), throw HTTP 500: `{"error": "Parent budget not found for cascade"}`
- If update fails, entire transaction rolls back (child budget remains at old value)

**Example Cascade Scenario**:
```
Request: Update "Fresh Produce" budget from $500 to $700 (January 2026)

Backend Actions:
1. Load existing budget: Fresh Produce, $500, January 2026
2. Calculate delta: $700 - $500 = +$200
3. Update child budget: Fresh Produce, $700, January 2026
4. Find parent budget: Groceries, $800, January 2026 (assuming another child contributed $300)
5. Update parent amount: $800 + $200 = $1000
6. Save parent budget: Groceries, $1000, January 2026
7. Commit transaction

Response: Updated child budget DTO
```

---

### DELETE /api/budgets/{id} (Delete Budget)

**Endpoint Signature**: No change
```http
DELETE /api/budgets/{id}
X-Hass-User: {username}
```

**New Behavior** (Feature 017):
1. Load child budget (to get amount for cascade)
2. **Automatic cascade**: If category is a child category:
   - Calculate delta: -childBudget.amount (e.g., -$700)
   - Find parent category budget for same period
   - Add delta to parent budget amount (parent amount -= 700)
   - Save parent budget in same transaction
   - **Note**: Parent budget persists even if amount reaches zero (not deleted)
3. Delete child budget
4. Return HTTP 204 No Content

**Response**: No change (HTTP 204 No Content on success)

**Error Handling**:
- If parent cascade fails, transaction rolls back (child budget NOT deleted)
- HTTP 500 error returned

**Example Cascade Scenario**:
```
Request: Delete "Fresh Produce" budget ($700, January 2026)

Backend Actions:
1. Load budget: Fresh Produce, $700, January 2026
2. Calculate delta: -$700
3. Find parent budget: Groceries, $1000, January 2026
4. Update parent amount: $1000 + (-$700) = $300
5. Save parent budget: Groceries, $300, January 2026
6. Delete child budget: Fresh Produce (deleted)
7. Commit transaction

Response: HTTP 204 No Content
```

**Zero Amount Handling**:
If deleting the child budget causes parent amount to reach zero, the parent budget record **persists** (not deleted):
```
Before: Parent "Groceries" = $500 (only child: "Fresh Produce" $500)
Action: Delete "Fresh Produce" budget
After: Parent "Groceries" = $0 (budget record still exists in database)
```

---

### GET /api/budgets/yearly-view?year={year} (Yearly Budget View)

**Endpoint Signature**: No change
```http
GET /api/budgets/yearly-view?year=2026
X-Hass-User: {username}
```

**New Behavior** (Feature 017):
- Response now **excludes child category budgets** from the returned list
- **Total budget** calculated by summing only parent category budgets + standalone category budgets (no children)
- **Filtering logic**: Budgets for categories with parent_category_id != NULL are excluded from the list

**Response Structure**: No change
```json
{
  "year": 2026,
  "totalBudget": 3500.00,     // Sum of parent + standalone categories only
  "totalSpending": 2100.00,
  "budgetCount": 3,            // Count excludes child category budgets
  "budgets": [
    {
      "id": 100,
      "categoryId": 1,
      "categoryName": "Groceries",        // Parent category (has children)
      "amount": 1000.00,
      "totalSpending": 850.00,
      "spendingPercentage": 85.0
    },
    {
      "id": 101,
      "categoryId": 5,
      "categoryName": "Transportation",    // Parent category
      "amount": 2000.00,
      "totalSpending": 1200.00,
      "spendingPercentage": 60.0
    },
    {
      "id": 102,
      "categoryId": 10,
      "categoryName": "Entertainment",     // Standalone category (no children)
      "amount": 500.00,
      "totalSpending": 50.00,
      "spendingPercentage": 10.0
    }
    // NOTE: Child categories (Fresh Produce, Pantry, etc.) NOT included in list
  ]
}
```

**Filtering Example**:
```
Database State (January 2026):
- Groceries (parent, ID=1): $1000
  - Fresh Produce (child, ID=2, parent_id=1): $500    ← EXCLUDED from view
  - Pantry (child, ID=3, parent_id=1): $500           ← EXCLUDED from view
- Transportation (parent, ID=5): $2000
  - Car (child, ID=6, parent_id=5): $1500             ← EXCLUDED from view
  - Public Transit (child, ID=7, parent_id=5): $500   ← EXCLUDED from view
- Entertainment (standalone, ID=10): $500

Yearly View Response:
- budgets: [Groceries $1000, Transportation $2000, Entertainment $500]
- totalBudget: $3500 (NOT $6000 which would double-count children)
```

**Why This Matters** (Double-Counting Prevention):
```
❌ WRONG (old behavior): Sum all budgets
totalBudget = Groceries $1000 + Fresh Produce $500 + Pantry $500 + Transportation $2000 + ...
            = $6000 (double-counts children since Groceries $1000 already includes them)

✅ CORRECT (new behavior): Sum parent + standalone only
totalBudget = Groceries $1000 + Transportation $2000 + Entertainment $500
            = $3500 (no double-counting)
```

---

## Backward Compatibility

### Frontend Impact

**Minimal Changes Required**:
- ✅ Budget creation/edit forms: **No changes** (cascade is backend-only)
- ✅ Budget detail pages: **No changes** (show individual budget regardless of cascade)
- ⚠️ Yearly budget view: **Already implemented correctly** (backend filters child budgets before sending response)

**Frontend Code Pattern** (remains unchanged):
```typescript
// Budget creation - no knowledge of cascade needed
const createBudget = async (request: CreateBudgetRequest) => {
  const response = await api.post<BudgetDTO>('/api/budgets', request);
  return response.data; // Child budget returned, parent cascade transparent
};

// Yearly view - backend filters child budgets, frontend just displays list
const yearlyView = await budgetService.getYearlyBudgetView(2026);
// yearlyView.budgets contains only parent + standalone categories
// yearlyView.totalBudget = sum of parent + standalone (no double-counting)
```

### API Contract Guarantees

1. **Atomicity**: Child budget create/update/delete and parent cascade succeed or fail together (no partial states)
2. **Idempotency**: Repeating same request produces same result (cascade logic is deterministic)
3. **Transparency**: Frontend clients need not be aware of cascade logic; behavior is automatic
4. **Error Propagation**: Cascade failures result in HTTP 500 with descriptive error messages

---

## Testing Endpoints

### Manual Test Scenarios (via REST API)

**Scenario 1: Create Child Budget → Parent Auto-Created**
```bash
# 1. Create child budget
POST /api/budgets
{
  "categoryId": 2,  # "Fresh Produce" (child of "Groceries" ID=1)
  "year": 2026,
  "month": 1,
  "amount": 500.00,
  "description": "January grocery budget"
}

# 2. Verify parent budget auto-created
GET /api/budgets?categoryId=1&year=2026&month=1
# Expected: Parent "Groceries" budget with amount=$500
```

**Scenario 2: Update Child Budget → Parent Adjusts**
```bash
# 1. Update child budget amount
PUT /api/budgets/456
{
  "categoryId": 2,
  "year": 2026,
  "month": 1,
  "amount": 700.00,  # Increased from 500
  "description": "Increased budget"
}

# 2. Verify parent budget increased
GET /api/budgets?categoryId=1&year=2026&month=1
# Expected: Parent "Groceries" amount=$700 (increased by $200)
```

**Scenario 3: Delete Child Budget → Parent Decreases**
```bash
# 1. Delete child budget
DELETE /api/budgets/456

# 2. Verify parent budget decreased
GET /api/budgets?categoryId=1&year=2026&month=1
# Expected: Parent "Groceries" amount=$0 (or reduced by child's contribution)
# NOTE: Parent budget record still exists even at zero
```

**Scenario 4: Yearly View Excludes Child Budgets**
```bash
# 1. Create budgets for parent and children
POST /api/budgets { "categoryId": 1, "year": 2026, "amount": 1000 }  # Parent "Groceries"
POST /api/budgets { "categoryId": 2, "year": 2026, "amount": 500 }   # Child "Fresh Produce"
POST /api/budgets { "categoryId": 3, "year": 2026, "amount": 500 }   # Child "Pantry"

# 2. Get yearly view
GET /api/budgets/yearly-view?year=2026

# 3. Verify response
# Expected:
# - budgets list contains only parent "Groceries" ($1000), NOT children
# - totalBudget = $1000 (not $2000 which would double-count)
```

---

## Summary

The parent category budget auto-rollup feature modifies **behavior only** (not signatures) of existing budget API endpoints. Budget create/update/delete operations automatically cascade to parent category budgets in the same transaction. The yearly budget view filters child category budgets from the response to prevent double-counting. Frontend clients require **no code changes** as the cascade logic is transparent.
