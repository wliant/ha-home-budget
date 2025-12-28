# Test Suite: Expense Recording Testing

**Suite ID**: expense-recording
**User Stories**: N/A (covers FR-016 through FR-022)
**Functional Requirements**: FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-022
**Database Reset Required**: Yes
**Execution Order**: 4
**Status**: Not Started

## Overview

Validates expense creation with required fields, date handling, creator attribution, amount/description constraints, and budget tracking.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser configured with X-Hass-User header

### Database State
- Clean database with categories and budgets pre-created for expense testing

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-050 | Expense creation with required fields | P1 | NOT_RUN | - |
| TC-051 | Default date behavior (today) | P1 | NOT_RUN | - |
| TC-052 | Date editing for past and future dates | P1 | NOT_RUN | - |
| TC-053 | Creator attribution via X-Hass-User | P1 | NOT_RUN | - |
| TC-054 | Amount constraints validation | P1 | NOT_RUN | - |
| TC-055 | Description length limits | P1 | NOT_RUN | - |
| TC-056 | Expense counted against correct budget | P1 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 7
- Passed: 0
- Failed: 0
- Not Run: 7
- **Pass Rate**: 0%

## Test Case Details

---

### TC-050: Expense creation with required fields

**Functional Requirements**: FR-016
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists and has a budget
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to expense recording page
2. Fill in all required fields:
   - Date: (today's date or select specific date)
   - Amount: 89.99
   - Description: "Hardware store supplies"
   - Category: (select valid category with budget)
3. Submit the form
4. Verify success message
5. Check recent expenses list
6. Verify database:
   ```
   mysql -u root -p
   USE budget_db;
   SELECT * FROM expenses ORDER BY id DESC LIMIT 1;
   ```

**Expected Outcome**:
- Expense is created successfully
- Success message displayed: "Expense created successfully" or similar
- Expense appears in recent expenses with all fields:
  - Date: (as entered)
  - Amount: $89.99
  - Description: "Hardware store supplies"
  - Category: (selected category)
  - Creator: alice
- Database record contains all required field values
- No fields are null or empty

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-051: Default date behavior (today)

**Functional Requirements**: FR-017
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice
- Know today's date for verification

**Test Steps**:
1. Navigate to expense recording page
2. Observe the date field value when form loads
3. Verify date field is pre-filled
4. Fill in other fields without changing date:
   - Amount: 45.50
   - Description: "Office supplies"
   - Category: (select valid category)
5. Submit the form
6. View expense in recent expenses list or database
7. Verify expense date matches today's date

**Expected Outcome**:
- Date field is pre-filled with today's date (not empty)
- Date format is clear and user-friendly (e.g., "2025-01-15" or "Jan 15, 2025")
- Submitting without changing date saves expense with today's date
- Database expense record shows date = today

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note date format used)_

---

### TC-052: Date editing for past and future dates

**Functional Requirements**: FR-018
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. **Test past date**:
   - Navigate to expense recording page
   - Change date to 14 days ago
   - Fill in: Amount: 120.00, Description: "Past expense", Category: (valid)
   - Submit and verify success
2. **Test future date**:
   - Navigate to expense recording page
   - Change date to 7 days in the future
   - Fill in: Amount: 95.00, Description: "Planned expense", Category: (valid)
   - Submit and verify success
3. View expenses list and verify both expenses appear with correct dates

**Expected Outcome**:
- System allows selecting and editing dates to both past and future
- Date picker provides easy date selection interface
- Past expense (14 days ago) is saved with correct past date
- Future expense (7 days future) is saved with correct future date
- Both expenses appear in expenses list with correct dates
- No date range restrictions that prevent legitimate past/future dates

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note if future expenses have special visual indication)_

---

### TC-053: Creator attribution via X-Hass-User

**Functional Requirements**: FR-019
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Two browser sessions or ability to switch X-Hass-User header
- Session 1: X-Hass-User: alice
- Session 2: X-Hass-User: bob

**Test Steps**:
1. **Session 1 (alice)**:
   - Set X-Hass-User header to "alice"
   - Navigate to expense recording page
   - Create expense:
     - Amount: 65.00
     - Description: "Alice's expense"
     - Category: (valid category)
   - Submit
2. **Session 2 (bob)**:
   - Set X-Hass-User header to "bob"
   - Navigate to expense recording page
   - Create expense:
     - Amount: 80.00
     - Description: "Bob's expense"
     - Category: (valid category)
   - Submit
3. Check database:
   ```
   SELECT id, description, amount, created_by FROM expenses WHERE description LIKE '%expense%';
   ```
4. Optionally check frontend expense list for creator display

**Expected Outcome**:
- Expense "Alice's expense" has created_by = "alice" in database
- Expense "Bob's expense" has created_by = "bob" in database
- Creator attribution is automatic based on X-Hass-User header
- Frontend may display creator name (e.g., "Created by alice")
- Each user's expense correctly attributed to them

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note if creator is visible in UI)_

---

### TC-054: Amount constraints validation

**Functional Requirements**: FR-020
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. **Test negative amount**:
   - Amount: -50.00
   - Submit and observe error
2. **Test zero amount**:
   - Amount: 0.00
   - Submit and observe error
3. **Test excessive decimal places**:
   - Amount: 99.999 (3 decimal places)
   - Submit and observe result
4. **Test extremely large amount**:
   - Amount: 9999999.99
   - Submit and observe result
5. **Test valid amount with 2 decimals**:
   - Amount: 123.45
   - Description: "Valid expense"
   - Category: (valid)
   - Submit and verify success

**Expected Outcome**:
- Negative amount rejected: "Amount must be greater than 0"
- Zero amount rejected: "Amount must be greater than 0"
- Excessive decimal places either rejected or automatically rounded to 2 decimals
- Extremely large amounts either accepted (if reasonable) or rejected with maximum limit error
- Valid amount (123.45) creates expense successfully
- All amounts stored with exactly 2 decimal places
- Amounts display correctly with 2 decimal places in UI

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document actual maximum amount and decimal handling)_

---

### TC-055: Description length limits

**Functional Requirements**: FR-021
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. **Test very short description**:
   - Description: "Hi"
   - Amount: 10.00
   - Category: (valid)
   - Submit and verify (should succeed)
2. **Test maximum length (500 characters)**:
   - Description: (generate 500-character string)
   - Amount: 25.00
   - Category: (valid)
   - Submit and verify success
3. **Test exceeding maximum (501+ characters)**:
   - Description: (generate 501-character string)
   - Amount: 30.00
   - Category: (valid)
   - Submit and observe result

**Expected Outcome**:
- Short description (2 chars) is accepted
- Description up to 500 characters is accepted
- Description exceeding 500 characters is either:
  - Rejected with error: "Description must be 500 characters or less", OR
  - Automatically truncated to 500 characters
- Character count may be displayed in form (e.g., "450/500")
- Database stores description correctly up to limit

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note actual maximum length and truncation behavior)_

---

### TC-056: Expense counted against correct budget

**Functional Requirements**: FR-022
**Priority**: P1

**Preconditions**:
- Budget exists: "Groceries" category, October 2025, $400 budgeted
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Note initial budget state:
   - Budgeted: $400
   - Spent: $0 (or known starting amount)
   - Remaining: $400
2. Create expense:
   - Amount: 125.50
   - Description: "Weekly groceries"
   - Date: (date in October 2025)
   - Category: "Groceries"
   - Submit
3. Navigate to budget summary or dashboard
4. Examine "Groceries" budget for October 2025
5. Verify calculations

**Expected Outcome**:
- Expense ($125.50) is counted against "Groceries" budget
- Budget summary shows:
  - Budgeted: $400.00
  - Spent: $125.50 (or previous + 125.50)
  - Remaining: $274.50 (or adjusted based on previous spending)
- Calculations are accurate to 2 decimal places
- Budget progress indicator updates (percentage, progress bar)
- Expense appears in budget's expense list or breakdown

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(verify calculation accuracy and UI update)_

---

## Suite Completion Notes

**Execution Summary**: (pending)
