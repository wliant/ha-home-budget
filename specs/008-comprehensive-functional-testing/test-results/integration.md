# Test Suite: Backend Integration and End-to-End Testing

**Suite ID**: integration
**User Stories**: US7 (Backend Integration), US1 (End-to-End Lifecycle), US2 (Multi-User), US3 (Validation/Error Handling)
**Functional Requirements**: FR-001, FR-002, FR-003, FR-004, FR-005, FR-029, FR-030, FR-031, FR-032, FR-033, FR-039, FR-040, FR-041, FR-042, FR-043
**Database Reset Required**: Yes
**Execution Order**: 1
**Status**: Not Started

## Overview

Comprehensive integration testing covering backend API communication, X-Hass-User authentication, end-to-end feature workflows, multi-user scenarios, and validation/error handling. This is the foundational test suite that must pass before testing individual features.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser DevTools available (Chrome, Firefox, or Safari)

### Database State
- Clean database (zero records in budgets, categories, expenses tables)
- Database reset performed using `reset-database.sql`

### Test Data Preparation
- None required for integration tests (will be created during test execution)

### User Authentication
- Simulated user: alice (via X-Hass-User header)
- Browser extension configured (ModHeader or Simple Modify Headers)

## Test Case Summary

### User Story 7: Backend Integration (TC-001 to TC-006)

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-001 | X-Hass-User header sent from frontend | P1 | NOT_RUN | - |
| TC-002 | Backend reads X-Hass-User and stores creator | P1 | NOT_RUN | - |
| TC-003 | Dashboard API returns JSON data | P1 | NOT_RUN | - |
| TC-004 | Frontend displays backend validation errors | P1 | NOT_RUN | - |
| TC-005 | Backend returns 500 for DB failures | P1 | NOT_RUN | - |
| TC-006 | Frontend updates UI on success response | P1 | NOT_RUN | - |

### User Story 1: End-to-End Lifecycle (TC-007 to TC-011)

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-007 | Create category hierarchy (parent and children) | P1 | NOT_RUN | - |
| TC-008 | Create budgets for categories | P1 | NOT_RUN | - |
| TC-009 | Record expenses against budgets | P1 | NOT_RUN | - |
| TC-010 | Verify dashboard displays budget status | P1 | NOT_RUN | - |
| TC-011 | Verify parent budget roll-up calculations | P1 | NOT_RUN | - |

### User Story 2: Multi-User (TC-012 to TC-016)

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-012 | Alice creates budget, Bob views it | P1 | NOT_RUN | - |
| TC-013 | Bob records expense against Alice's budget | P1 | NOT_RUN | - |
| TC-014 | Expense attribution to correct creator | P1 | NOT_RUN | - |
| TC-015 | Concurrent budget creation by two users | P1 | NOT_RUN | - |
| TC-016 | Missing X-Hass-User header handling | P1 | NOT_RUN | - |

### User Story 3: Validation/Error Handling (TC-017 to TC-025)

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-017 | Empty required field validation | P1 | NOT_RUN | - |
| TC-018 | Duplicate budget prevention | P1 | NOT_RUN | - |
| TC-019 | Circular reference prevention in categories | P1 | NOT_RUN | - |
| TC-020 | Negative budget amount rejection | P1 | NOT_RUN | - |
| TC-021 | Backend unavailability graceful handling | P1 | NOT_RUN | - |
| TC-022 | Invalid category ID in expense creation | P1 | NOT_RUN | - |
| TC-023 | Database connection failure handling | P1 | NOT_RUN | - |
| TC-024 | Concurrent duplicate budget creation | P1 | NOT_RUN | - |
| TC-025 | Category deletion with dependencies | P1 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 25
- Passed: 0
- Failed: 0
- Blocked: 0
- Skipped: 0
- Not Run: 25
- **Pass Rate**: 0%

## Test Case Details

---

### TC-001: X-Hass-User header sent from frontend

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-003, FR-040

**Preconditions**:
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser extension configured with X-Hass-User: alice
- Browser DevTools Network tab open

**Test Steps**:
1. Open browser and navigate to http://localhost:3001
2. Open Browser DevTools → Network tab
3. Click "Record Expense" or navigate to any feature that makes an API call
4. In Network tab, select the API request to backend (e.g., POST /api/expenses)
5. Examine request headers

**Expected Outcome**:
- Request headers include `X-Hass-User: alice`
- Header is present in all API requests from frontend to backend

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-002: Backend reads X-Hass-User and stores creator

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-003, FR-040

**Preconditions**:
- TC-001 passed (X-Hass-User header confirmed sent)
- Database reset completed
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to expense recording page
2. Fill in expense form:
   - Description: "Test expense for alice"
   - Amount: 50.00
   - Date: (today's date)
   - Category: (select any available category)
3. Submit the form
4. Connect to MySQL database:
   ```
   mysql -u root -p -h localhost
   USE budget_db;
   SELECT * FROM expenses WHERE description = 'Test expense for alice';
   ```
5. Examine the `created_by` or `username` field

**Expected Outcome**:
- Backend processes the request successfully
- Database record shows `created_by` = "alice"
- Frontend displays success message

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-003: Dashboard API returns JSON data

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-039, FR-041

**Preconditions**:
- Frontend and backend running
- Some test data exists in database (at least 1 budget, 1 expense)
- Browser DevTools Network tab open

**Test Steps**:
1. Navigate to homepage (http://localhost:3001)
2. Open Browser DevTools → Network tab
3. Refresh the page
4. In Network tab, locate API calls to Spring Boot backend (e.g., GET /api/dashboard, GET /api/budgets)
5. Click on the request and examine:
   - Response status code
   - Response headers (Content-Type)
   - Response body (Preview or Response tab)

**Expected Outcome**:
- API returns HTTP 200 status code
- Content-Type header is `application/json`
- Response body contains valid JSON data with expected structure (budgets, expenses, etc.)
- Frontend successfully renders dashboard with data from API response

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-004: Frontend displays backend validation errors

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-043

**Preconditions**:
- Frontend and backend running
- At least one budget exists (e.g., "Food" category, January 2025, $500)

**Test Steps**:
1. Navigate to budget creation page
2. Attempt to create a duplicate budget:
   - Category: "Food"
   - Period: January 2025
   - Amount: 300
3. Submit the form
4. Observe frontend behavior

**Expected Outcome**:
- Backend returns validation error (e.g., HTTP 400 or 409 with error message "Budget already exists for this category and period")
- Frontend displays the error message to the user without crashing
- Form remains on screen with entered data intact
- User can correct the error and retry

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-005: Backend returns 500 for DB failures

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-043

**Preconditions**:
- Frontend and backend running
- Ability to stop MySQL database temporarily

**Test Steps**:
1. Stop MySQL database:
   ```
   docker-compose stop mysql
   ```
2. In browser, navigate to homepage or attempt to create a budget/expense
3. Observe Network tab response
4. Observe frontend behavior
5. Restart MySQL database:
   ```
   docker-compose start mysql
   ```

**Expected Outcome**:
- Backend returns HTTP 500 Internal Server Error or 503 Service Unavailable
- Error response includes appropriate message (e.g., "Service temporarily unavailable")
- Frontend handles the error gracefully (shows error message, doesn't crash)
- After database restart, application recovers and functions normally

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-006: Frontend updates UI on success response

**User Story**: US7 - Backend Integration Testing
**Priority**: P1
**Functional Requirements**: FR-041

**Preconditions**:
- Frontend and backend running
- Database in clean state

**Test Steps**:
1. Navigate to category creation page
2. Create a new category:
   - Name: "Utilities"
   - Parent: (none)
3. Submit the form
4. Observe Network tab (should show successful POST request with HTTP 200/201)
5. Observe frontend UI update

**Expected Outcome**:
- Backend returns success response (HTTP 200 or 201)
- Frontend receives the success response
- Frontend updates UI accordingly:
  - Shows success message (e.g., "Category created successfully")
  - New category appears in category list
  - Form is cleared or user is redirected
- Data consistency: viewing categories shows the newly created "Utilities" category

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-007: Create category hierarchy (parent and children)

**User Story**: US1 - End-to-End Budget Lifecycle Testing
**Priority**: P1
**Functional Requirements**: FR-001, FR-006, FR-010

**Preconditions**:
- Frontend and backend running
- Database reset completed (zero categories)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to category management page
2. Create parent category:
   - Name: "Food"
   - Parent: (none)
   - Submit
3. Create first child category:
   - Name: "Groceries"
   - Parent: "Food"
   - Submit
4. Create second child category:
   - Name: "Dining Out"
   - Parent: "Food"
   - Submit
5. View category list/hierarchy

**Expected Outcome**:
- All three categories are created successfully
- Category hierarchy is displayed correctly with:
  - "Food" as top-level (parent) category
  - "Groceries" nested/indented under "Food"
  - "Dining Out" nested/indented under "Food"
- Visual indication of hierarchy (indentation, tree view, or similar)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-008: Create budgets for categories

**User Story**: US1 - End-to-End Budget Lifecycle Testing
**Priority**: P1
**Functional Requirements**: FR-001, FR-011

**Preconditions**:
- TC-007 passed (categories "Food", "Groceries", "Dining Out" exist)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to budget creation page
2. Create budget for "Groceries":
   - Category: "Groceries"
   - Period: January 2025
   - Amount: 300.00
   - Submit
3. Create budget for "Dining Out":
   - Category: "Dining Out"
   - Period: January 2025
   - Amount: 200.00
   - Submit
4. View budget list

**Expected Outcome**:
- Both budgets are created successfully
- Budget list shows:
  - "Groceries" budget: $300.00 for January 2025
  - "Dining Out" budget: $200.00 for January 2025
- Success confirmation messages displayed after each creation

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-009: Record expenses against budgets

**User Story**: US1 - End-to-End Budget Lifecycle Testing
**Priority**: P1
**Functional Requirements**: FR-001, FR-016, FR-022

**Preconditions**:
- TC-008 passed (budgets for "Groceries" $300 and "Dining Out" $200 exist for January 2025)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to expense recording page
2. Record first expense:
   - Description: "Weekly groceries"
   - Amount: 125.00
   - Date: (current date in January 2025)
   - Category: "Groceries"
   - Submit
3. Record second expense:
   - Description: "Restaurant dinner"
   - Amount: 75.50
   - Date: (current date in January 2025)
   - Category: "Dining Out"
   - Submit
4. Verify expenses appear in recent expenses list

**Expected Outcome**:
- Both expenses are recorded successfully
- Success messages displayed after each submission
- Expenses appear in recent expenses list with correct details
- Each expense is counted against the correct budget (verified in dashboard)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-010: Verify dashboard displays budget status

**User Story**: US1 - End-to-End Budget Lifecycle Testing
**Priority**: P1
**Functional Requirements**: FR-001, FR-023, FR-024

**Preconditions**:
- TC-009 passed (budgets and expenses created)
- Expected state:
  - "Groceries" budget: $300, spent: $125
  - "Dining Out" budget: $200, spent: $75.50

**Test Steps**:
1. Navigate to homepage/dashboard (http://localhost:3001)
2. Examine budget summary section
3. Examine recent expenses section
4. Verify calculations

**Expected Outcome**:
- Dashboard displays budget summaries:
  - "Groceries": Budgeted $300.00, Spent $125.00, Remaining $175.00
  - "Dining Out": Budgeted $200.00, Spent $75.50, Remaining $124.50
- Recent expenses section shows:
  - "Weekly groceries" - $125.00
  - "Restaurant dinner" - $75.50
- All amounts calculated correctly
- Visual indicators (progress bars, percentage) reflect actual spending

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-011: Verify parent budget roll-up calculations

**User Story**: US1 - End-to-End Budget Lifecycle Testing
**Priority**: P1
**Functional Requirements**: FR-001, FR-013

**Preconditions**:
- TC-010 passed
- Category hierarchy exists: "Food" > "Groceries", "Dining Out"
- Child budgets: Groceries $300, Dining Out $200

**Test Steps**:
1. Navigate to budget summary or category view
2. Locate the parent category "Food"
3. Examine the rolled-up budget total

**Expected Outcome**:
- Parent category "Food" displays total budget of $500.00 (sum of children: $300 + $200)
- If expenses exist, parent also shows total spent across all children
- Roll-up calculation is accurate and automatic

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-012: Alice creates budget, Bob views it

**User Story**: US2 - Multi-User Testing
**Priority**: P1
**Functional Requirements**: FR-002

**Preconditions**:
- Frontend and backend running
- Database reset completed
- At least one category exists (e.g., "Food")
- Two browser sessions or browser extension allowing header switching

**Test Steps**:
1. **Session 1 (Alice)**:
   - Set X-Hass-User header to "alice"
   - Navigate to budget creation page
   - Create budget:
     - Category: "Food"
     - Period: February 2025
     - Amount: 400.00
     - Submit
2. **Session 2 (Bob)**:
   - Set X-Hass-User header to "bob"
   - Navigate to homepage/budget list
   - Observe budget list

**Expected Outcome**:
- Budget created by Alice appears in Bob's view
- Budget shows correct details (Food, February 2025, $400)
- Multi-user data sharing works correctly
- Both users see the same shared budget data

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-013: Bob records expense against Alice's budget

**User Story**: US2 - Multi-User Testing
**Priority**: P1
**Functional Requirements**: FR-002

**Preconditions**:
- TC-012 passed (budget created by Alice exists)
- Browser configured with X-Hass-User: bob

**Test Steps**:
1. **Session (Bob)**:
   - Ensure X-Hass-User header is set to "bob"
   - Navigate to expense recording page
   - Record expense:
     - Description: "Bob's grocery shopping"
     - Amount: 85.00
     - Date: (current date in February 2025)
     - Category: "Food"
     - Submit
2. Verify expense is recorded
3. Check database to confirm creator attribution

**Expected Outcome**:
- Bob can successfully record an expense against Alice's budget
- Expense is counted against the shared "Food" budget
- Database shows expense creator as "bob" (not "alice")
- Dashboard reflects the expense in budget calculations

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-014: Expense attribution to correct creator

**User Story**: US2 - Multi-User Testing
**Priority**: P1
**Functional Requirements**: FR-002, FR-019

**Preconditions**:
- TC-013 passed (Bob's expense recorded)
- Database contains expenses from both Alice and Bob

**Test Steps**:
1. Connect to MySQL database:
   ```
   mysql -u root -p -h localhost
   USE budget_db;
   SELECT id, description, amount, created_by FROM expenses ORDER BY id;
   ```
2. Examine the `created_by` field for each expense

**Expected Outcome**:
- Expenses created by Alice show `created_by = 'alice'`
- Expenses created by Bob show `created_by = 'bob'`
- Attribution is accurate regardless of which budget the expense was recorded against
- Frontend may display creator name in expense list (e.g., "Expense created by alice")

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-015: Concurrent budget creation by two users

**User Story**: US2 - Multi-User Testing
**Priority**: P1
**Functional Requirements**: FR-002, FR-012, FR-033

**Preconditions**:
- Frontend and backend running
- Category "Transportation" exists
- Two browser sessions/windows open simultaneously
- Session 1: X-Hass-User: alice
- Session 2: X-Hass-User: bob

**Test Steps**:
1. **Both sessions simultaneously**:
   - Navigate to budget creation page
   - Fill in identical budget details:
     - Category: "Transportation"
     - Period: March 2025
     - Amount: 250.00
2. **Simultaneously (as close as possible)**:
   - Click Submit in both sessions at nearly the same time
3. Observe results in both sessions
4. Check database:
   ```
   SELECT * FROM budgets WHERE category_name = 'Transportation' AND period = '2025-03';
   ```

**Expected Outcome**:
- One submission succeeds (HTTP 200/201)
- Other submission fails with error "Budget already exists for this category and period" (HTTP 409)
- Database contains exactly ONE budget record for Transportation/March 2025 (no duplicates)
- Both frontends handle the response appropriately (success message for one, error message for other)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-016: Missing X-Hass-User header handling

**User Story**: US2 - Multi-User Testing
**Priority**: P1
**Functional Requirements**: FR-003, FR-031

**Preconditions**:
- Frontend and backend running
- Ability to remove X-Hass-User header from requests

**Test Steps**:
1. Disable browser extension or remove X-Hass-User header configuration
2. Navigate to homepage (http://localhost:3001)
3. Attempt to create a budget or expense
4. Observe Network tab response
5. Observe frontend behavior

**Expected Outcome**:
- Backend rejects request with authentication error (HTTP 401 Unauthorized or 403 Forbidden)
- Error response includes message like "Missing authentication header" or "User not authenticated"
- Frontend displays authentication error message
- Application does not crash or create records with null/undefined user
- User is prompted to authenticate or shown clear error

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-017: Empty required field validation

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-004, FR-030

**Preconditions**:
- Frontend and backend running
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to budget creation page
2. Leave required fields empty:
   - Category: (not selected)
   - Period: (not selected)
   - Amount: (empty)
3. Attempt to submit the form
4. Observe validation errors

**Expected Outcome**:
- Frontend prevents form submission OR backend rejects with validation error
- Clear error messages displayed for each missing field:
  - "Category is required"
  - "Period is required"
  - "Amount is required"
- Form remains on screen with ability to correct errors
- No invalid data is saved to database

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-018: Duplicate budget prevention

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-004, FR-012

**Preconditions**:
- Budget already exists: "Food" category, April 2025, $500

**Test Steps**:
1. Navigate to budget creation page
2. Attempt to create duplicate budget:
   - Category: "Food"
   - Period: April 2025
   - Amount: 600.00 (different amount, but same category/period)
3. Submit the form

**Expected Outcome**:
- Backend rejects the request with validation error
- Error message: "Budget already exists for this category and period" or similar
- Frontend displays the error message clearly
- Database still contains only the original budget ($500, not $600)
- No duplicate records created

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-019: Circular reference prevention in categories

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-004, FR-007

**Preconditions**:
- Category hierarchy exists: "Food" (parent) > "Groceries" (child)

**Test Steps**:
1. Navigate to category edit page for "Food"
2. Attempt to set "Food"'s parent to "Groceries" (its own child)
3. Submit the form

**Expected Outcome**:
- Backend rejects the request with validation error
- Error message: "Cannot create circular reference" or "Category cannot be its own ancestor"
- Frontend displays the error clearly
- Category hierarchy remains unchanged (no circular reference created)
- Database integrity maintained

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-020: Negative budget amount rejection

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-004, FR-015

**Preconditions**:
- Frontend and backend running
- At least one category exists

**Test Steps**:
1. Navigate to budget creation page
2. Fill in form with negative amount:
   - Category: (any valid category)
   - Period: May 2025
   - Amount: -100.00
3. Submit the form

**Expected Outcome**:
- Frontend prevents submission (client-side validation) OR backend rejects (server-side validation)
- Error message: "Amount must be greater than 0" or "Amount cannot be negative"
- No budget record created with negative amount
- Form allows user to correct and resubmit

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-021: Backend unavailability graceful handling

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-029

**Preconditions**:
- Frontend running
- Ability to stop Spring Boot backend

**Test Steps**:
1. Stop the Spring Boot backend:
   ```
   docker-compose stop backend
   ```
2. In browser, navigate to homepage
3. Attempt to load dashboard
4. Attempt to create a budget/expense
5. Observe frontend behavior
6. Restart backend:
   ```
   docker-compose start backend
   ```

**Expected Outcome**:
- Frontend detects backend unavailability
- User-friendly error message displayed: "Service temporarily unavailable. Please try again later."
- Frontend UI remains responsive (no infinite loading, no crash)
- After backend restart, application recovers and functions normally
- No data corruption or loss

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-022: Invalid category ID in expense creation

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-004, FR-030

**Preconditions**:
- Frontend and backend running
- At least one valid category exists

**Test Steps**:
1. Open Browser DevTools → Console
2. Navigate to expense recording page
3. Use browser console to manipulate form data or intercept request:
   - Modify category ID to non-existent value (e.g., 99999)
4. Submit the form

**Expected Outcome**:
- Backend validates category existence
- Request rejected with error: "Category not found" or "Invalid category ID"
- Frontend displays error message
- No expense record created with invalid category reference
- Database referential integrity maintained

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-023: Database connection failure handling

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-032

**Preconditions**:
- Frontend and backend running
- Ability to stop MySQL database

**Test Steps**:
1. Stop MySQL database:
   ```
   docker-compose stop mysql
   ```
2. Attempt to load homepage (triggers dashboard data fetch)
3. Attempt to create a budget
4. Observe Network tab responses and frontend behavior
5. Restart MySQL:
   ```
   docker-compose start mysql
   ```
6. Retry operations

**Expected Outcome**:
- Backend returns HTTP 500 or 503 error for database operations
- Error response includes appropriate message (not raw database exception)
- Frontend displays: "Service temporarily unavailable" or similar user-friendly message
- Frontend does not crash or enter broken state
- After database restart, application recovers automatically
- No data corruption

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-024: Concurrent duplicate budget creation

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-033

**Preconditions**:
- Same as TC-015 (duplicate of concurrent test for completeness)

**Test Steps**:
- Same as TC-015

**Expected Outcome**:
- Same as TC-015 (one succeeds, one fails with duplicate error)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(This test case duplicates TC-015 but is listed separately for US3 coverage completeness)_

---

### TC-025: Category deletion with dependencies

**User Story**: US3 - Validation and Error Handling Testing
**Priority**: P1
**Functional Requirements**: FR-009

**Preconditions**:
- Category "Food" exists with child category "Groceries"
- OR category "Food" has active budgets

**Test Steps**:
1. Navigate to category management page
2. Locate category "Food"
3. Attempt to delete "Food" category
4. Observe system response

**Expected Outcome**:
- System prevents deletion
- Error message displayed: "Cannot delete category with child categories" or "Cannot delete category with active budgets. Please reassign or delete dependencies first."
- Category "Food" remains in database
- Child categories/budgets remain intact
- Data integrity maintained

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

## Suite Completion Notes

**Execution Summary**:
- Started: (pending)
- Completed: (pending)
- Duration: (pending)
- Tester(s): (pending)

**Overall Assessment**: (pending)

**Blockers Encountered**: None

**Recommendations**: (pending)

**Defects Found**: 0 (CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0)
