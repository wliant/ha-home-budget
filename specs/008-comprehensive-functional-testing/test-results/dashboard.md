# Test Suite: Dashboard Testing

**Suite ID**: dashboard
**User Stories**: N/A (covers FR-023 through FR-028)
**Functional Requirements**: FR-023, FR-024, FR-025, FR-026, FR-027, FR-028
**Database Reset Required**: Yes
**Execution Order**: 5
**Status**: Not Started

## Overview

Validates homepage dashboard displays including current month budget summary, recent expenses, quick actions, empty states, system status, and progress indicators.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser configured with X-Hass-User header
- Browser Performance tab available for load time measurement

### Database State
- Will test both empty state (clean DB) and populated state (with test data)

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-057 | Current month budget summary display | P1 | NOT_RUN | - |
| TC-058 | Recent expenses display | P1 | NOT_RUN | - |
| TC-059 | Quick action buttons navigation | P1 | NOT_RUN | - |
| TC-060 | Empty states display | P1 | NOT_RUN | - |
| TC-061 | System status indicator | P1 | NOT_RUN | - |
| TC-062 | Budget progress indicators | P1 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 6
- Passed: 0
- Failed: 0
- Not Run: 6
- **Pass Rate**: 0%

## Test Case Details

---

### TC-057: Current month budget summary display

**Functional Requirements**: FR-023
**Priority**: P1

**Preconditions**:
- Budgets exist for current month (e.g., November 2025)
- Some expenses recorded against these budgets
- Example data:
  - "Groceries": Budgeted $300, Spent $125
  - "Utilities": Budgeted $150, Spent $75
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage/dashboard (http://localhost:3001)
2. Locate budget summary section
3. Examine each budget entry
4. Verify all displayed information

**Expected Outcome**:
- Dashboard displays "Current Month Budget Summary" or similar heading
- Shows current month and year (e.g., "November 2025")
- For each budget, displays:
  - **Amount**: Budgeted amount (e.g., $300.00)
  - **Spent**: Total spent so far (e.g., $125.00)
  - **Remaining**: Amount left (e.g., $175.00)
  - **Percentage**: Spending percentage (e.g., 41.67% or progress bar at 42%)
- Calculations are accurate to 2 decimal places
- All budgets for current month are displayed
- Budgets from other months are not shown in current month summary

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note if dashboard load time < 3 seconds)_

---

### TC-058: Recent expenses display

**Functional Requirements**: FR-024
**Priority**: P1

**Preconditions**:
- At least 5 expenses exist in database with varying dates
- Expenses created by different users (alice, bob)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage/dashboard
2. Locate "Recent Expenses" section
3. Count number of expenses displayed
4. Examine each expense entry details
5. Verify ordering (most recent first)

**Expected Outcome**:
- Dashboard displays "Recent Expenses" section
- Shows 5 most recent expenses (not more, not less, unless < 5 total)
- For each expense, displays:
  - **Date**: Expense date
  - **Amount**: Expense amount (e.g., $45.50)
  - **Description**: Expense description
  - **Category**: Associated category name
  - **Creator**: Username who created it (e.g., "alice" or "Created by: alice")
- Expenses ordered by date (most recent first)
- Formatting is clear and readable

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note if older expenses beyond top 5 are hidden or paginated)_

---

### TC-059: Quick action buttons navigation

**Functional Requirements**: FR-025
**Priority**: P1

**Preconditions**:
- Frontend running at http://localhost:3001
- On homepage/dashboard
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage
2. Locate quick action buttons section
3. Identify all quick action buttons (e.g., "Record Expense", "Create Budget", "Manage Categories")
4. **Test "Record Expense"**:
   - Click button
   - Verify navigation to expense recording page
   - Use browser back button to return
5. **Test "Create Budget"** (if exists):
   - Click button
   - Verify navigation to budget creation page
   - Use browser back button to return
6. **Test "Manage Categories"** (if exists):
   - Click button
   - Verify navigation to category management page

**Expected Outcome**:
- Quick action buttons are clearly visible and labeled
- Clicking "Record Expense" navigates to /expenses/new or expense recording page
- Clicking "Create Budget" navigates to /budgets/new or budget creation page
- Clicking "Manage Categories" navigates to /categories page
- All buttons navigate to correct destination without errors
- Button styling is consistent and user-friendly

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(list all quick action buttons found)_

---

### TC-060: Empty states display

**Functional Requirements**: FR-026
**Priority**: P1

**Preconditions**:
- Database in clean state (zero budgets, zero expenses)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Reset database to clean state using reset-database.sql
2. Navigate to homepage/dashboard
3. Observe budget summary section
4. Observe recent expenses section
5. Verify empty state messages and visual indicators

**Expected Outcome**:
- **Budget Summary Empty State**:
  - Message displayed: "No budgets for current month" or "Get started by creating your first budget"
  - Optionally: Call-to-action button "Create Budget"
  - No empty list or broken UI
- **Recent Expenses Empty State**:
  - Message displayed: "No expenses recorded yet" or "Start tracking by recording an expense"
  - Optionally: Call-to-action button "Record Expense"
  - No empty list or broken UI
- Empty states are user-friendly and guide users to next action
- No errors or broken layouts when data is absent

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(screenshot empty states for documentation)_

---

### TC-061: System status indicator

**Functional Requirements**: FR-027
**Priority**: P1

**Preconditions**:
- Frontend running at http://localhost:3001
- Ability to stop/start backend service
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. **Test healthy state**:
   - Ensure backend is running
   - Navigate to homepage
   - Observe system status indicator
2. **Test unhealthy state**:
   - Stop backend: `docker-compose stop backend`
   - Refresh homepage or wait for status check
   - Observe system status indicator
3. **Test recovery**:
   - Start backend: `docker-compose start backend`
   - Refresh homepage or wait for status check
   - Observe system status indicator returns to healthy

**Expected Outcome**:
- **Healthy state**:
  - Status indicator shows: Green dot/icon + "Connected" or "System OK"
  - OR indicator is hidden when system is healthy
- **Unhealthy state**:
  - Status indicator shows: Red dot/icon + "Backend unavailable" or error message
  - User is informed of connection issue
- **Recovery**:
  - Status indicator returns to healthy state after backend restart
  - Automatic recovery without user intervention
- Status indicator is visible but not intrusive

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document status indicator location and behavior)_

---

### TC-062: Budget progress indicators

**Functional Requirements**: FR-028
**Priority**: P1

**Preconditions**:
- Multiple budgets with different spending levels:
  - Budget A: Budgeted $500, Spent $150 (30% - on track)
  - Budget B: Budgeted $300, Spent $250 (83% - warning)
  - Budget C: Budgeted $200, Spent $225 (112.5% - overspent)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Create test budgets with varying spending levels as specified
2. Navigate to homepage/dashboard
3. Examine budget progress indicators for each budget
4. Verify visual indicators and status labels

**Expected Outcome**:
- Each budget displays progress indicator showing spending status:
  - **On Track** (< 75% spent):
    - Progress bar: Green or neutral color
    - Label: "On track" or no warning
    - Visual: Progress bar fills to actual percentage (30%)
  - **Warning** (75-100% spent):
    - Progress bar: Yellow/orange color
    - Label: "Warning" or "Almost spent"
    - Visual: Progress bar fills to 83%, yellow/orange color
  - **Overspent** (> 100% spent):
    - Progress bar: Red color
    - Label: "Overspent" or "Over budget"
    - Visual: Progress bar at 100% (or 112%), red color
- Progress percentages are calculated correctly
- Visual distinctions are clear and intuitive
- Color coding or icons help users quickly identify budget status

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document threshold values and visual indicators used)_

---

## Suite Completion Notes

**Execution Summary**: (pending)

**Performance Metrics**:
- Dashboard Load Time: (to be measured, target < 3 seconds)
- API Response Time: (to be measured, target < 2 seconds)
