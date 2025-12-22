# Quickstart Guide - Homepage Dashboard Update

**Feature**: Homepage Dashboard Update (006)
**Date**: 2025-12-22
**Audience**: Developers and QA testers

## Overview

This guide provides step-by-step scenarios for testing the updated homepage dashboard. Each scenario corresponds to a user story from the specification and can be tested independently.

---

## Prerequisites

### Backend Setup

1. **Start backend services**:
   ```bash
   docker-compose up mysql backend -d
   ```

2. **Verify backend is running**:
   ```bash
   curl http://localhost:8081/api/health
   # Expected: {"status":"UP","service":"home-budget-backend","version":"1.0.0-SNAPSHOT"}
   ```

3. **Seed test data** (optional):
   ```bash
   # Create a budget for current month
   curl -X POST http://localhost:8081/api/budgets \
     -H "Content-Type: application/json" \
     -H "X-Hass-User: testuser" \
     -d '{
       "month": 12,
       "year": 2025,
       "amount": 5000,
       "categoryId": null
     }'

   # Record some expenses
   curl -X POST http://localhost:8081/api/expenses \
     -H "Content-Type: application/json" \
     -H "X-Hass-User: testuser" \
     -d '{
       "amount": 45.99,
       "description": "Grocery shopping",
       "expenseDate": "2025-12-21",
       "budgetId": 1,
       "categoryId": 1
     }'
   ```

### Frontend Setup

1. **Install dependencies**:
   ```bash
   cd budget-frontend
   npm install
   ```

2. **Configure environment**:
   ```bash
   # Create .env.local file
   echo "NEXT_PUBLIC_API_URL=http://localhost:8081" > .env.local
   echo "NEXT_PUBLIC_TEST_USER=testuser" >> .env.local
   ```

3. **Start dev server**:
   ```bash
   npm run dev
   # Server runs on http://localhost:3000
   ```

---

## Test Scenario 1: Quick Budget Overview (US1 - P1)

**Goal**: Verify homepage displays current month budget summary

### Test Case 1.1: Display Budget Summary

**Given**: Current month budget exists in database
**When**: User navigates to homepage (http://localhost:3000)
**Then**: Budget Summary Card displays:
- Current month and year (e.g., "December 2025")
- Total budget amount (e.g., "$5,000.00")
- Amount spent (e.g., "$3,200.50")
- Amount remaining (e.g., "$1,799.50")
- Progress bar showing spending percentage
- Status indicator (green = on track, yellow = warning, red = overspent)

**Verification Steps**:
1. Open http://localhost:3000
2. Locate "Budget Summary" card (should be in top-left)
3. Verify all fields match database values
4. Verify progress bar visual matches percentage
5. Verify color coding:
   - Green: ≤80% spent
   - Yellow: 81-100% spent
   - Red: >100% spent

**Expected Result**: Budget summary displays correctly with accurate data and visual indicators

---

### Test Case 1.2: No Budget Empty State

**Given**: No budget exists for current month
**When**: User navigates to homepage
**Then**: Budget Summary Card shows:
- Icon indicating empty state
- Message: "No budget found for [Current Month] [Year]"
- "Create Budget" button linking to /budgets/new

**Verification Steps**:
1. Delete current month budget from database
2. Refresh homepage
3. Verify empty state message displays
4. Click "Create Budget" button
5. Verify navigation to budget creation page

**Expected Result**: Empty state guides user to create first budget

---

### Test Case 1.3: Overspent Budget Warning

**Given**: Budget expenses exceed budget amount
**When**: User views homepage
**Then**: Budget Summary Card shows:
- Red progress bar (>100%)
- Red "Overspent" status indicator
- Negative remaining amount in red

**Verification Steps**:
1. Create budget: $1000
2. Add expenses totaling $1200
3. Refresh homepage
4. Verify red visual indicators
5. Verify remaining shows "-$200.00" in red

**Expected Result**: Overspent status clearly visible with warning indicators

---

## Test Scenario 2: Recent Activity Feed (US2 - P2)

**Goal**: Verify homepage displays recent expenses

### Test Case 2.1: Display Recent Expenses

**Given**: Multiple expenses exist in database
**When**: User navigates to homepage
**Then**: Recent Activity Card displays:
- 5 most recent expenses (sorted by date descending)
- Each expense shows: date, amount, description, category, creator

**Verification Steps**:
1. Create 10 expenses with different dates
2. Open homepage
3. Verify only 5 expenses display
4. Verify they are the 5 most recent by date
5. Verify sort order (newest first)
6. Verify each expense shows all required fields

**Expected Result**: 5 most recent expenses display in chronological order

---

### Test Case 2.2: No Expenses Empty State

**Given**: No expenses recorded
**When**: User navigates to homepage
**Then**: Recent Activity Card shows:
- Icon indicating empty state
- Message: "No expenses recorded yet"
- "Record Expense" button linking to /expenses/new

**Verification Steps**:
1. Delete all expenses from database
2. Refresh homepage
3. Verify empty state message displays
4. Click "Record Expense" button
5. Verify navigation to expense creation page

**Expected Result**: Empty state encourages first expense recording

---

### Test Case 2.3: Multi-User Attribution

**Given**: Expenses created by different household members
**When**: User views homepage
**Then**: Recent Activity Card shows creator for each expense

**Verification Steps**:
1. Create expense with X-Hass-User: "john"
2. Create expense with X-Hass-User: "jane"
3. Refresh homepage
4. Verify both expenses display
5. Verify "Created by: john" and "Created by: jane" labels

**Expected Result**: All household members' expenses visible with attribution

---

## Test Scenario 3: Quick Actions Dashboard (US3 - P1)

**Goal**: Verify quick action buttons work correctly

### Test Case 3.1: Create Budget Action

**When**: User clicks "Create Budget" button
**Then**: Navigates to /budgets/new

**Verification Steps**:
1. Open homepage
2. Locate Quick Actions card
3. Click "Create Budget" button
4. Verify URL changes to /budgets/new
5. Verify budget creation form displays

**Expected Result**: One-click navigation to budget creation

---

### Test Case 3.2: Record Expense Action

**When**: User clicks "Record Expense" button
**Then**: Navigates to /expenses/new

**Verification Steps**:
1. Open homepage
2. Click "Record Expense" button
3. Verify URL changes to /expenses/new
4. Verify expense creation form displays

**Expected Result**: One-click navigation to expense recording

---

### Test Case 3.3: View Categories Action

**When**: User clicks "View Categories" button
**Then**: Navigates to /categories

**Verification Steps**:
1. Open homepage
2. Click "View Categories" button
3. Verify URL changes to /categories
4. Verify categories list displays

**Expected Result**: One-click navigation to categories management

---

### Test Case 3.4: View Dashboard Action

**When**: User clicks "View Dashboard" button
**Then**: Navigates to /dashboard

**Verification Steps**:
1. Open homepage
2. Click "View Dashboard" button
3. Verify URL changes to /dashboard
4. Verify dashboard page displays

**Expected Result**: One-click navigation to analytics dashboard

---

## Test Scenario 4: Active Feature Navigation (US4 - P2)

**Goal**: Verify feature cards link to implemented pages

### Test Case 4.1: All Feature Cards Active

**When**: User views homepage
**Then**: All feature cards (Budgets, Categories, Dashboard, Expenses) have active navigation buttons

**Verification Steps**:
1. Open homepage
2. Scroll to "Features" section
3. Verify 4 feature cards display (no "coming soon" cards)
4. Verify each card has a "View [Feature]" button
5. Verify no cards have opacity effect (all fully visible)

**Expected Result**: All feature cards are active and clickable

---

### Test Case 4.2: Feature Card Navigation

**When**: User clicks feature card button
**Then**: Navigates to corresponding page

**Verification Steps**:
1. Click "View Budgets" → verify /budgets page
2. Click "View Categories" → verify /categories page
3. Click "View Dashboard" → verify /dashboard page
4. Return to homepage and test all cards

**Expected Result**: Each feature card navigates correctly

---

## Test Scenario 5: Error Handling

**Goal**: Verify graceful error handling

### Test Case 5.1: Backend Unavailable

**Given**: Backend is not running
**When**: User opens homepage
**Then**:
- System Status shows "Backend Connection Failed"
- Budget Summary Card shows error message with retry button
- Recent Activity Card shows error message with retry button
- Page remains functional (no crash)

**Verification Steps**:
1. Stop backend: `docker-compose down backend`
2. Refresh homepage
3. Verify error messages display
4. Verify retry buttons present
5. Restart backend: `docker-compose up backend -d`
6. Click retry buttons
7. Verify data loads successfully

**Expected Result**: Graceful degradation with recovery option

---

### Test Case 5.2: Slow API Response

**Given**: Backend has high latency
**When**: User opens homepage
**Then**: Loading indicators display while data fetches

**Verification Steps**:
1. Open browser DevTools Network tab
2. Throttle network to "Slow 3G"
3. Refresh homepage
4. Verify loading spinners display for each widget
5. Verify data appears after loading completes

**Expected Result**: Loading states provide feedback during slow loads

---

## Test Scenario 6: Responsive Design

**Goal**: Verify homepage works on all screen sizes

### Test Case 6.1: Mobile View (320px)

**When**: User views homepage on mobile device
**Then**: Layout stacks vertically, all content readable

**Verification Steps**:
1. Open Chrome DevTools
2. Toggle device toolbar (Cmd+Shift+M)
3. Select iPhone SE (320px width)
4. Verify cards stack in single column
5. Verify text doesn't overflow
6. Verify buttons remain tappable

**Expected Result**: Mobile-optimized layout

---

### Test Case 6.2: Tablet View (768px)

**When**: User views homepage on tablet
**Then**: Layout uses 2 columns where appropriate

**Verification Steps**:
1. Set viewport to iPad (768px width)
2. Verify Budget Summary and Recent Activity side-by-side
3. Verify Quick Actions in single row
4. Verify feature cards in 2x2 grid

**Expected Result**: Tablet-optimized layout

---

### Test Case 6.3: Desktop View (1920px)

**When**: User views homepage on desktop
**Then**: Layout uses full width efficiently

**Verification Steps**:
1. Set viewport to 1920px width
2. Verify cards use available space
3. Verify no excessive white space
4. Verify content centered with max-width constraint

**Expected Result**: Desktop-optimized layout

---

## Performance Testing

### Load Time Test

**Goal**: Verify homepage loads in under 2 seconds

**Steps**:
1. Open Chrome DevTools Performance tab
2. Click record
3. Navigate to homepage
4. Stop recording when page fully loaded
5. Analyze timeline:
   - Initial page load: <500ms
   - API requests (parallel): <200ms
   - Total render: <2000ms

**Expected Result**: Homepage fully interactive in under 2 seconds

---

### Concurrent User Test

**Goal**: Verify homepage works with multiple users

**Steps**:
1. Open homepage in 3 different browsers (Chrome, Firefox, Safari)
2. Set different X-Hass-User values (john, jane, bob)
3. Verify each user sees same budget (household-wide)
4. Verify each user's expenses are attributed correctly
5. Create expense as "john", refresh "jane" browser
6. Verify jane sees john's expense in recent activity

**Expected Result**: Multi-user household support works correctly

---

## Integration Testing

### End-to-End User Journey

**Scenario**: New user sets up first budget and records expense

**Steps**:
1. Open homepage (no data)
2. Verify "Create your first budget" prompt
3. Click "Create Budget"
4. Fill form: December 2025, $3000
5. Submit and return to homepage
6. Verify budget summary displays $3000
7. Click "Record Expense"
8. Fill form: $45.99, "Groceries", today
9. Submit and return to homepage
10. Verify recent activity shows new expense
11. Verify budget summary shows $45.99 spent

**Expected Result**: Complete flow works seamlessly from empty state to active usage

---

## Acceptance Criteria Checklist

**User Story 1 (Budget Overview)**:
- [ ] Budget summary displays for current month
- [ ] Empty state shows when no budget exists
- [ ] Overspent budgets show warning indicators

**User Story 2 (Recent Activity)**:
- [ ] 5 most recent expenses display
- [ ] Empty state shows when no expenses exist
- [ ] Multi-user attribution works correctly

**User Story 3 (Quick Actions)**:
- [ ] Create Budget button navigates correctly
- [ ] Record Expense button navigates correctly
- [ ] View Categories button navigates correctly
- [ ] View Dashboard button navigates correctly

**User Story 4 (Feature Navigation)**:
- [ ] All feature cards are active (no "coming soon")
- [ ] Feature card buttons navigate correctly

**Cross-cutting Concerns**:
- [ ] Responsive design works (320px-1920px)
- [ ] Error handling graceful
- [ ] Loading states display
- [ ] Performance under 2 seconds

---

## Troubleshooting

### Homepage shows "Backend unavailable"

**Solution**:
```bash
# Check backend status
docker-compose ps

# Restart backend if down
docker-compose up backend -d

# Check logs
docker-compose logs backend --tail=50
```

### Budget summary shows wrong month

**Solution**:
- Verify system date/time is correct
- Check database query for current month calculation
- Backend uses `MONTH(CURRENT_DATE)` and `YEAR(CURRENT_DATE)`

### Recent expenses empty but expenses exist

**Solution**:
- Check expense dates (must have `expenseDate` populated)
- Verify sort parameter: `?sort=expenseDate,desc`
- Check database: `SELECT * FROM expenses ORDER BY expense_date DESC LIMIT 5`

---

## Next Steps

After completing all test scenarios:

1. **Run /speckit.tasks** to generate task breakdown
2. **Run /speckit.analyze** to validate consistency
3. **Run /speckit.implement** to execute implementation

**Documentation Status**: ✅ Complete and ready for task generation
