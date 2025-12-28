# Test Suite: Date and Time Handling Testing

**Suite ID**: date-handling
**User Stories**: US5
**Functional Requirements**: FR-011, FR-016, FR-017, FR-018
**Database Reset Required**: No
**Execution Order**: 7
**Status**: Not Started

## Overview

Validates date picker functionality, month-year budget selection, past/future date handling, and date-based filtering and calculations.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser configured with X-Hass-User header

### Database State
- Can reuse existing data from previous test suites

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-032 | Budget month-year association | P2 | NOT_RUN | - |
| TC-033 | Expense default date (today) | P2 | NOT_RUN | - |
| TC-034 | Past date expense chronological order | P2 | NOT_RUN | - |
| TC-035 | Future date planned expense | P2 | NOT_RUN | - |
| TC-036 | Current month dashboard display | P2 | NOT_RUN | - |
| TC-037 | Expense counts against correct period | P2 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 6
- Passed: 0
- Failed: 0
- Not Run: 6
- **Pass Rate**: 0%

## Test Case Details

---

### TC-032: Budget month-year association

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-011

**Preconditions**:
- Frontend and backend running
- At least one category exists (e.g., "Food")
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to budget creation page
2. Fill in budget form:
   - Category: "Food"
   - Period: Select January 2025 using month-year picker
   - Amount: 500.00
3. Submit the form
4. View budget list/summary
5. Verify database:
   ```
   mysql -u root -p
   USE budget_db;
   SELECT * FROM budgets WHERE category_name = 'Food';
   ```

**Expected Outcome**:
- Budget is created successfully
- Budget is associated with January 2025 period
- Budget list displays "January 2025" or "2025-01" for this budget
- Database stores period correctly (e.g., year=2025, month=1 or period='2025-01')
- Month-year picker is intuitive and works correctly

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-033: Expense default date (today)

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-017

**Preconditions**:
- Frontend and backend running
- At least one category and budget exist
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to expense recording page
2. Observe the date field when form loads
3. Fill in expense without changing the date:
   - Description: "Test expense"
   - Amount: 25.00
   - Category: (select valid category)
   - Date: (leave as default)
4. Submit the form
5. Verify the expense date in database or recent expenses list

**Expected Outcome**:
- Date field defaults to today's date when form loads
- Default date is pre-filled (not empty)
- Submitting without changing date saves expense with today's date
- Expense appears in recent expenses with today's date

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(verify date format matches user's locale or application standard)_

---

### TC-034: Past date expense chronological order

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-018

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice
- Current date is assumed to be in January 2025

**Test Steps**:
1. Navigate to expense recording page
2. Record expense with past date:
   - Description: "Last week's groceries"
   - Amount: 50.00
   - Date: Change to 7 days ago (e.g., if today is Jan 15, select Jan 8)
   - Category: (select valid category)
3. Submit the form
4. View recent expenses list
5. Verify chronological ordering

**Expected Outcome**:
- Expense is saved with the past date (7 days ago)
- Expense appears in recent expenses list
- Expenses are ordered chronologically (most recent first OR oldest first - consistent ordering)
- Past expense appears in correct position relative to other expenses
- No errors when selecting past dates

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document the chronological order used: ascending or descending)_

---

### TC-035: Future date planned expense

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-018

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to expense recording page
2. Record expense with future date:
   - Description: "Planned car maintenance"
   - Amount: 200.00
   - Date: Select a future date (e.g., 7 days from now)
   - Category: (select valid category)
3. Submit the form
4. View expenses list/dashboard
5. Verify expense is saved

**Expected Outcome**:
- System allows future date selection
- Expense is saved as a planned/future expense
- Expense appears in expenses list with future date clearly indicated
- No errors when submitting future-dated expense
- May have visual distinction for future expenses (e.g., different color, "Planned" label)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note any visual indicators for future expenses)_

---

### TC-036: Current month dashboard display

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-023, FR-011

**Preconditions**:
- Budgets exist for both January 2025 and February 2025
- Current date is in January 2025 (or adjust test accordingly)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage/dashboard
2. Observe which budget period is displayed
3. Verify budget summary shows January 2025 data
4. If possible, change system date or wait until February
5. Refresh dashboard and verify it shows February 2025 data

**Expected Outcome**:
- Dashboard automatically displays current month budget summary
- Shows "January 2025" heading or indicator
- Budget and expense data shown is for the current month period
- When month changes, dashboard updates to show new current month
- Clear indication of which period is being displayed

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document how current period is determined and displayed)_

---

### TC-037: Expense counts against correct period

**User Story**: US5 - Date and Time Handling Testing
**Priority**: P2
**Functional Requirements**: FR-022, FR-011

**Preconditions**:
- Budget exists for January 2025: "Food" category, $500
- Budget exists for February 2025: "Food" category, $400
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Record expense dated in January:
   - Description: "January groceries"
   - Amount: 100.00
   - Date: January 15, 2025
   - Category: "Food"
2. Record expense dated in February:
   - Description: "February groceries"
   - Amount: 75.00
   - Date: February 10, 2025
   - Category: "Food"
3. View budget summary for January 2025
4. View budget summary for February 2025
5. Verify expense attribution

**Expected Outcome**:
- January expense ($100) counts against January 2025 budget only
  - January budget shows: Spent $100 / Budgeted $500
- February expense ($75) counts against February 2025 budget only
  - February budget shows: Spent $75 / Budgeted $400
- Expenses are correctly attributed to budget periods based on expense date, not creation date
- No cross-period contamination

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(critical for budget accuracy - verify calculations)_

---

## Suite Completion Notes

**Execution Summary**: (pending)

**Date Picker Compatibility**: (to test across Chrome, Firefox, Safari)
