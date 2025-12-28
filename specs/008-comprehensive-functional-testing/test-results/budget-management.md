# Test Suite: Budget Management Testing

**Suite ID**: budget-management
**User Stories**: N/A (covers FR-011 through FR-015)
**Functional Requirements**: FR-011, FR-012, FR-013, FR-014, FR-015
**Database Reset Required**: Yes
**Execution Order**: 3
**Status**: Not Started

## Overview

Validates budget creation, duplicate prevention, parent-child budget validation, category requirements, and amount constraints.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser configured with X-Hass-User header

### Database State
- Clean database with categories pre-created for budget testing

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-045 | Budget creation with category and period | P1 | NOT_RUN | - |
| TC-046 | Duplicate budget prevention | P1 | NOT_RUN | - |
| TC-047 | Parent budget validation (sum of children) | P1 | NOT_RUN | - |
| TC-048 | Budget without category rejection | P1 | NOT_RUN | - |
| TC-049 | Budget amount constraints validation | P1 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 5
- Passed: 0
- Failed: 0
- Not Run: 5
- **Pass Rate**: 0%

## Test Case Details

---

### TC-045: Budget creation with category and period

**Functional Requirements**: FR-011
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists (e.g., "Transportation")
- Database in clean state
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to budget creation page
2. Fill in budget form:
   - Category: "Transportation"
   - Period: June 2025 (using month-year picker)
   - Amount: 350.00
3. Submit the form
4. Verify success message
5. View budget list
6. Check database:
   ```
   mysql -u root -p
   USE budget_db;
   SELECT * FROM budgets WHERE category_name = 'Transportation';
   ```

**Expected Outcome**:
- Budget is created successfully
- Success message displayed: "Budget created successfully" or similar
- Budget appears in budget list with:
  - Category: "Transportation"
  - Period: June 2025
  - Amount: $350.00
- Database record contains correct category, period (year=2025, month=6), and amount
- Creator is "alice" (from X-Hass-User header)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-046: Duplicate budget prevention

**Functional Requirements**: FR-012
**Priority**: P1

**Preconditions**:
- Budget already exists: "Transportation" category, June 2025, $350

**Test Steps**:
1. Navigate to budget creation page
2. Attempt to create duplicate budget:
   - Category: "Transportation"
   - Period: June 2025 (same as existing)
   - Amount: 500.00 (different amount, but same category/period)
3. Submit the form
4. Observe response

**Expected Outcome**:
- Backend rejects the duplicate budget creation
- Error message displayed: "Budget already exists for this category and period" or similar
- HTTP status code 409 Conflict or 400 Bad Request
- Form remains on screen with entered data
- Original budget ($350) remains unchanged in database
- No duplicate record created

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-047: Parent budget validation (sum of children)

**Functional Requirements**: FR-013
**Priority**: P1

**Preconditions**:
- Category hierarchy exists: "Food" > "Groceries" ($300), "Dining Out" ($200)
- Both child budgets for July 2025
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Create budget for child category "Groceries":
   - Category: "Groceries"
   - Period: July 2025
   - Amount: 300.00
2. Create budget for child category "Dining Out":
   - Category: "Dining Out"
   - Period: July 2025
   - Amount: 200.00
3. Navigate to budget summary or category view
4. Examine parent category "Food" budget total
5. Verify calculation: $300 + $200 = $500

**Expected Outcome**:
- Parent category "Food" displays total budget of $500.00
- Budget calculation is automatic (no manual parent budget creation needed)
- Sum is accurate and updates when child budgets change
- Parent budget is read-only or calculated field (cannot be set independently of children)
- Visual indication that parent budget is a roll-up

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(verify if parent budget can be created separately or is auto-calculated)_

---

### TC-048: Budget without category rejection

**Functional Requirements**: FR-014
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to budget creation page
2. Fill in form without selecting category:
   - Category: (leave blank / not selected)
   - Period: August 2025
   - Amount: 450.00
3. Attempt to submit the form

**Expected Outcome**:
- Form submission prevented OR backend rejects request
- Validation error displayed: "Category is required" or similar
- Error appears near category field or in error summary
- No budget created in database without category
- Form allows user to correct and retry

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(note whether validation is client-side, server-side, or both)_

---

### TC-049: Budget amount constraints validation

**Functional Requirements**: FR-015
**Priority**: P1

**Preconditions**:
- Frontend and backend running
- At least one category exists
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. **Test negative amount**:
   - Category: (any valid category)
   - Period: September 2025
   - Amount: -100.00
   - Submit and observe error
2. **Test zero amount**:
   - Amount: 0.00
   - Submit and observe error
3. **Test excessive decimal places**:
   - Amount: 123.456 (3 decimal places)
   - Submit and observe result (should round or reject)
4. **Test extremely large amount**:
   - Amount: 999999999.99 (test maximum)
   - Submit and observe result
5. **Test valid amount with 2 decimals**:
   - Amount: 567.89
   - Submit and verify success

**Expected Outcome**:
- Negative amount rejected with error: "Amount must be greater than 0" or "Amount cannot be negative"
- Zero amount rejected with error: "Amount must be greater than 0"
- Amount with >2 decimal places either:
  - Rejected with error, OR
  - Automatically rounded to 2 decimal places
- Extremely large amounts either:
  - Accepted if within reasonable limit (e.g., $100,000), OR
  - Rejected with error: "Amount exceeds maximum allowed value"
- Valid amount (567.89) creates budget successfully
- Amounts stored with exactly 2 decimal places

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(document actual maximum amount allowed and decimal handling)_

---

## Suite Completion Notes

**Execution Summary**: (pending)
