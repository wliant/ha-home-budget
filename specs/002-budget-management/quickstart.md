# Quickstart: Budget and Expense Management

**Feature**: 002-budget-management
**Date**: 2025-10-23
**Purpose**: Integration testing scenarios and manual acceptance testing guide

## Prerequisites

Before testing this feature, ensure Feature 001 (Project Scaffolding) is deployed:

```bash
# Verify services are running
docker-compose ps

# Expected output:
# homebudget-mysql      Up (healthy)
# homebudget-backend    Up
# homebudget-frontend   Up

# Verify ports
# Frontend: http://localhost:3001
# Backend: http://localhost:8081
# MySQL: localhost:3307
```

## Quick Reference

### API Base URL
- **Local**: `http://localhost:8081/api`
- **Docker**: `http://homebudget-backend:8080/api`

### Key Endpoints
- `GET /api/budgets` - List all budgets
- `POST /api/budgets` - Create budget
- `GET /api/budgets/{id}` - Budget details
- `POST /api/expenses` - Record expense
- `GET /api/categories` - List categories
- `POST /api/categories` - Create category

### Sample X-Hass-User Headers
```bash
-H "X-Hass-User: alice"
-H "X-Hass-User: bob"
```

---

## Integration Testing Scenarios

These scenarios map directly to user stories in spec.md. Execute in order for dependency management.

### Scenario 1: Create and View Budgets (User Story 1 - P1 MVP)

**Goal**: Verify budget creation, viewing, and duplicate prevention

**Steps**:

1. **Create first budget for October 2025**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "year": 2025,
    "month": 10,
    "totalAmount": 3000.00,
    "description": "Family budget for October"
  }'
```

**Expected**:
- Status: `201 Created`
- Response includes: `id`, `year: 2025`, `month: 10`, `totalAmount: 3000.00`, `totalSpending: 0.00`, `spendingPercentage: 0.00`, `createdBy: "alice"`

2. **Retrieve budget list**:
```bash
curl http://localhost:8081/api/budgets
```

**Expected**:
- Status: `200 OK`
- Array with 1 budget
- Budget shows `expenseCount: 0`

3. **Attempt duplicate budget (should fail)**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "year": 2025,
    "month": 10,
    "totalAmount": 2500.00,
    "description": "Duplicate attempt"
  }'
```

**Expected**:
- Status: `409 Conflict`
- Error message: "Budget for October 2025 already exists"

4. **Create budget for November 2025**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "year": 2025,
    "month": 11,
    "totalAmount": 2800.00,
    "description": "November household budget"
  }'
```

**Expected**:
- Status: `201 Created`
- `createdBy: "bob"`

5. **Verify negative amount is rejected**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "year": 2025,
    "month": 12,
    "totalAmount": -500.00
  }'
```

**Expected**:
- Status: `400 Bad Request`
- Error message includes validation error about positive amount

**Acceptance Criteria Verified**:
- ✅ Budget created with month, amount, zero spending
- ✅ Budget list displays month, amount, spending percentage
- ✅ Invalid amount rejected with error
- ✅ Duplicate budget prevented

---

### Scenario 2: Record Expenses Against Budgets (User Story 2 - P2)

**Goal**: Verify expense recording, spending calculation, and user accountability

**Prerequisites**: Budgets from Scenario 1 exist (October 2025, November 2025)

**Steps**:

1. **Record first expense (Groceries)**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "budgetId": 1,
    "amount": 150.00,
    "description": "Weekly groceries at Whole Foods",
    "expenseDate": "2025-10-15"
  }'
```

**Expected**:
- Status: `201 Created`
- Response includes: `id`, `amount: 150.00`, `createdBy: "alice"`
- `category: null` (defaults to Uncategorized)
- `warnings: []` (date is in October, matches budget)

2. **Record second expense (Gas)**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "budgetId": 1,
    "amount": 60.00,
    "description": "Gas station fill-up",
    "expenseDate": "2025-10-16"
  }'
```

**Expected**:
- Status: `201 Created`
- `createdBy: "bob"` (different user)

3. **Record third expense (Electric bill)**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "budgetId": 1,
    "amount": 120.50,
    "description": "October electric bill",
    "expenseDate": "2025-10-17"
  }'
```

4. **Verify budget shows updated spending**:
```bash
curl http://localhost:8081/api/budgets/1
```

**Expected**:
- Status: `200 OK`
- `totalSpending: 330.50` (150 + 60 + 120.50)
- `spendingPercentage: 11.02` (330.50 / 3000 * 100)
- `expenseCount: 3`
- `expenses` array contains all 3 expenses
- Each expense shows `createdBy` field (alice, bob, alice)

5. **Record expense with date mismatch (November date against October budget)**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "budgetId": 1,
    "amount": 75.00,
    "description": "Late recorded expense",
    "expenseDate": "2025-11-01"
  }'
```

**Expected**:
- Status: `201 Created` (expense is created)
- `warnings: ["Expense date is outside budget month"]`

6. **Filter expenses by user**:
```bash
curl "http://localhost:8081/api/expenses?createdBy=alice"
```

**Expected**:
- Status: `200 OK`
- Array contains only expenses created by alice (should be 3 expenses: groceries, electric, late recorded)

**Acceptance Criteria Verified**:
- ✅ Expense recorded, appears in budget, increases spending
- ✅ Multiple users can record expenses, each shows who recorded it
- ✅ Expense without category defaults to "Uncategorized"
- ✅ Date mismatch warning displayed

---

### Scenario 3: Manage Spending Categories (User Story 3 - P3)

**Goal**: Verify category creation, usage, deletion prevention, and spending breakdown

**Steps**:

1. **Create "Groceries" category**:
```bash
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "name": "Groceries",
    "icon": "🛒"
  }'
```

**Expected**:
- Status: `201 Created`
- Response: `{"id": 2, "name": "Groceries", "icon": "🛒", "createdBy": "alice", "isSystem": false}`
- Note: ID 2 because ID 1 is reserved for system "Uncategorized" category

2. **Create more categories**:
```bash
# Utilities
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{"name": "Utilities", "icon": "💡"}'

# Transportation
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{"name": "Transportation", "icon": "🚗"}'
```

3. **List all categories**:
```bash
curl http://localhost:8081/api/categories
```

**Expected**:
- Status: `200 OK`
- Array with 4 categories: Uncategorized (system), Groceries, Utilities, Transportation

4. **Update expense to use Groceries category**:
```bash
# First, get expense ID 1 details
curl http://localhost:8081/api/expenses/1

# Then update with categoryId
curl -X PUT http://localhost:8081/api/expenses/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "budgetId": 1,
    "amount": 150.00,
    "description": "Weekly groceries at Whole Foods",
    "expenseDate": "2025-10-15",
    "categoryId": 2,
    "version": 1
  }'
```

**Expected**:
- Status: `200 OK`
- Response includes: `category: {"id": 2, "name": "Groceries", "icon": "🛒"}`

5. **Record new expense with category directly**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "budgetId": 1,
    "amount": 120.50,
    "description": "October water bill",
    "expenseDate": "2025-10-18",
    "categoryId": 3
  }'
```

**Expected**:
- Status: `201 Created`
- `category` shows Utilities

6. **Get budget spending breakdown by category**:
```bash
curl http://localhost:8081/api/budgets/1/summary
```

**Expected**:
- Status: `200 OK`
- `categoryBreakdown` array showing:
  - Groceries: $150.00 (X%)
  - Utilities: $241.00 (Y%) (electric $120.50 + water $120.50)
  - Transportation: $60.00 (Z%) (gas)
  - Uncategorized: $75.00 (W%) (late recorded expense)

7. **Attempt to delete category with expenses (should fail)**:
```bash
curl -X DELETE http://localhost:8081/api/categories/2
```

**Expected**:
- Status: `400 Bad Request`
- Error: "Cannot delete category with N expenses. Please reassign expenses to another category first."

8. **Create unused category and delete successfully**:
```bash
# Create Entertainment category
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{"name": "Entertainment", "icon": "🎬"}'

# Delete it (no expenses yet)
curl -X DELETE http://localhost:8081/api/categories/5
```

**Expected**:
- Status: `204 No Content`

**Acceptance Criteria Verified**:
- ✅ Category created with icon, available for all users
- ✅ Expense tagged with category
- ✅ Budget displays spending breakdown by category
- ✅ Category deletion prevented when expenses exist

---

### Scenario 4: Budget Editing and Deletion (Additional FR Coverage)

**Goal**: Verify budget update constraints and cascade deletion

**Steps**:

1. **Update budget amount and description (allowed per FR-019)**:
```bash
curl -X PUT http://localhost:8081/api/budgets/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "totalAmount": 3500.00,
    "description": "Updated October family budget",
    "version": 1
  }'
```

**Expected**:
- Status: `200 OK`
- `totalAmount: 3500.00`, `description` updated
- `spendingPercentage` recalculated: 646.00 / 3500 * 100 = 18.46%

2. **Attempt to delete budget with expenses (without confirmation)**:
```bash
curl -X DELETE "http://localhost:8081/api/budgets/1"
```

**Expected**:
- Status: `400 Bad Request`
- Error: "Cannot delete budget with expenses without confirmation"

3. **Delete budget with expenses (with confirmation)**:
```bash
curl -X DELETE "http://localhost:8081/api/budgets/1?confirm=true"
```

**Expected**:
- Status: `204 No Content`
- Budget and all associated expenses deleted (cascade)

4. **Verify expenses are gone**:
```bash
curl "http://localhost:8081/api/expenses?budgetId=1"
```

**Expected**:
- Status: `200 OK`
- Empty array `[]`

5. **Delete empty budget (November - no expenses)**:
```bash
curl -X DELETE http://localhost:8081/api/budgets/2
```

**Expected**:
- Status: `204 No Content` (no confirmation needed)

**Acceptance Criteria Verified**:
- ✅ Budget amount/description editable (FR-019)
- ✅ Deletion prevented without confirmation when expenses exist (FR-022)
- ✅ Cascade deletion works with confirmation

---

### Scenario 5: Concurrent Access and Optimistic Locking (FR-016)

**Goal**: Verify concurrent updates are handled correctly

**Steps**:

1. **Create new budget for testing**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "year": 2025,
    "month": 12,
    "totalAmount": 2000.00,
    "description": "December budget"
  }'
```

Let's assume this returns `id: 3`, `version: 0`

2. **Simulate concurrent update - User 1 updates successfully**:
```bash
curl -X PUT http://localhost:8081/api/budgets/3 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "totalAmount": 2200.00,
    "description": "December budget - updated by Alice",
    "version": 0
  }'
```

**Expected**:
- Status: `200 OK`
- `version: 1` (incremented)

3. **Simulate concurrent update - User 2 uses stale version (should fail)**:
```bash
curl -X PUT http://localhost:8081/api/budgets/3 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "totalAmount": 2100.00,
    "description": "December budget - updated by Bob",
    "version": 0
  }'
```

**Expected**:
- Status: `409 Conflict`
- Error: "Optimistic locking conflict - budget was modified by another user. Please refresh and try again."

4. **User 2 fetches latest version and retries**:
```bash
# Get latest
curl http://localhost:8081/api/budgets/3

# Retry with correct version
curl -X PUT http://localhost:8081/api/budgets/3 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "totalAmount": 2300.00,
    "description": "December budget - updated by Bob after refresh",
    "version": 1
  }'
```

**Expected**:
- Status: `200 OK`
- `version: 2`

**Acceptance Criteria Verified**:
- ✅ Concurrent access supported (FR-016)
- ✅ Optimistic locking prevents lost updates
- ✅ Users prompted to refresh on conflict

---

### Scenario 6: Frontend Integration Testing (Manual UI Testing)

**Goal**: Verify frontend-backend integration works end-to-end

**Steps**:

1. **Access frontend**:
   - Open browser: `http://localhost:3001`

2. **Navigate to Budgets page**:
   - Click "Budgets" in navigation (or visit `http://localhost:3001/budgets`)

3. **Create budget via UI**:
   - Click "Create New Budget" button
   - Fill form: Year=2025, Month=10, Amount=3000, Description="Test budget"
   - Submit form

**Expected**:
- Budget appears in list
- Shows: October 2025, $3000.00, $0.00 spent, 0%

4. **Add expense via UI**:
   - Click on budget to view details
   - Click "Add Expense" button
   - Fill form: Amount=150, Description="Test expense", Date=2025-10-15
   - Submit

**Expected**:
- Expense appears in budget detail
- Budget spending updates: $150.00 spent, 5%
- Shows "Created by: [your-username]"

5. **Test category dropdown**:
   - Go to Categories page
   - Create category: Name="Test Category", Icon="✅"
   - Return to Add Expense
   - Verify "Test Category" appears in dropdown

6. **Test validation**:
   - Try creating budget with negative amount
   - Expected: Error message "Amount must be positive"

**Acceptance Criteria Verified**:
- ✅ Frontend can communicate with backend APIs
- ✅ Forms work with Material-UI components
- ✅ Client-side validation displays errors
- ✅ Lists update after creation
- ✅ User identity passed via X-Hass-User header

---

## Performance Verification

### Test: Category Breakdown with 200 Expenses (SC-007)

```bash
# Script to create 200 expenses
for i in {1..200}; do
  curl -X POST http://localhost:8081/api/expenses \
    -H "Content-Type: application/json" \
    -H "X-Hass-User: testuser" \
    -d "{
      \"budgetId\": 3,
      \"amount\": $((RANDOM % 100 + 10)).$(( RANDOM % 100 )),
      \"description\": \"Test expense $i\",
      \"expenseDate\": \"2025-12-$(printf %02d $((RANDOM % 28 + 1)))\",
      \"categoryId\": $((RANDOM % 3 + 2))
    }"
done

# Measure breakdown performance
time curl http://localhost:8081/api/budgets/3/summary
```

**Expected**:
- Response time: <2 seconds (per SC-007)
- Category breakdown calculates correctly

### Test: 500 Expenses Performance (SC-004)

Create budget with 500 expenses and verify list/detail pages load without degradation.

---

## Cleanup

```bash
# Stop services
docker-compose down

# Remove volumes (if testing fresh state)
docker-compose down -v

# Restart clean
docker-compose up -d
```

---

## Troubleshooting

### Issue: 404 Not Found on API calls

**Solution**: Check backend is running on correct port (8081 for local, 8080 for Docker internal)

### Issue: CORS errors in browser console

**Solution**: Verify CorsConfig from Feature 001 allows `http://localhost:3001` origin

### Issue: X-Hass-User header not working locally

**Solution**: For local testing, manually include header. In production, Home Assistant nginx adds it automatically.

### Issue: Optimistic locking errors frequently

**Solution**: Ensure UI fetches latest version before updates. Check `version` field is included in PUT requests.

---

## Success Metrics Validation

After completing all scenarios, verify:

- ✅ **SC-001**: Budget creation takes <30 seconds (including form fill)
- ✅ **SC-002**: Expense recording takes <20 seconds
- ✅ **SC-003**: Budget list renders in <1 second
- ✅ **SC-004**: 500 expenses supported without degradation
- ✅ **SC-005**: Updates visible within 5 seconds (test with multiple browser tabs)
- ✅ **SC-006**: First-time users can create budget without help
- ✅ **SC-007**: Category breakdown loads in <2 seconds with 200 expenses
- ✅ **SC-008**: Zero calculation errors in totals/percentages

---

## Next Steps

After validating this feature:

1. Run `/speckit.analyze` to check spec-plan-tasks consistency
2. Proceed to User Story 4 (Dashboard) implementation (P4)
3. Consider adding automated tests if feature becomes critical
