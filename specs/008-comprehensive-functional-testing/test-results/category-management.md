# Test Suite: Category Management Testing

**Suite ID**: category-management
**User Stories**: US6
**Functional Requirements**: FR-006, FR-007, FR-008, FR-009, FR-010
**Database Reset Required**: Yes
**Execution Order**: 2
**Status**: Not Started

## Overview

Validates hierarchical category management including parent-child constraints, circular reference prevention, deletion dependency handling, and budget roll-ups.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Backend running at http://localhost:8080
- Browser configured with X-Hass-User header

### Database State
- Clean database (zero records in all tables)
- Database reset performed using `reset-database.sql`

### Test Data Preparation
- Test will create categories during execution

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-038 | Category creation without parent | P2 | NOT_RUN | - |
| TC-039 | Category creation with parent | P2 | NOT_RUN | - |
| TC-040 | Circular reference prevention | P2 | NOT_RUN | - |
| TC-041 | Category deletion with child prevention | P2 | NOT_RUN | - |
| TC-042 | Budget roll-up for parent categories | P2 | NOT_RUN | - |
| TC-043 | 2-level hierarchy depth limit | P2 | NOT_RUN | - |
| TC-044 | Hierarchical category display | P2 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 7
- Passed: 0
- Failed: 0
- Blocked: 0
- Skipped: 0
- Not Run: 7
- **Pass Rate**: 0%

## Test Case Details

---

### TC-038: Category creation without parent

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-006, FR-010

**Preconditions**:
- Frontend and backend running
- Database in clean state or with some existing categories
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to category management page
2. Click "Create Category" or similar button
3. Fill in form:
   - Name: "Food"
   - Parent: (leave blank/select "None")
4. Submit the form
5. View category list

**Expected Outcome**:
- Category "Food" is created successfully
- Success message displayed
- "Food" appears in category list as a top-level (root) category
- No parent association in database
- Visual indication that it's a root category (no indentation)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-039: Category creation with parent

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-006, FR-010

**Preconditions**:
- Parent category "Food" exists (from TC-038 or created separately)
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to category management page
2. Click "Create Category"
3. Fill in form:
   - Name: "Groceries"
   - Parent: "Food"
4. Submit the form
5. View category list/hierarchy

**Expected Outcome**:
- Category "Groceries" is created successfully
- Success message displayed
- "Groceries" appears nested/indented under "Food" in hierarchy view
- Parent-child relationship stored correctly in database
- Visual hierarchy indication (indentation, tree structure, or icon)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-040: Circular reference prevention

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-007

**Preconditions**:
- Category hierarchy exists: "Food" (parent) > "Groceries" (child)

**Test Steps**:
1. Navigate to category edit page for "Food"
2. Attempt to change "Food"'s parent to "Groceries" (its own child)
3. Submit the form
4. Observe system response

**Expected Outcome**:
- System prevents the circular reference
- Error message displayed: "Cannot create circular reference" or "Category cannot be its own ancestor"
- Category hierarchy remains unchanged
- Database integrity maintained (no circular reference created)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-041: Category deletion with child prevention

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-009

**Preconditions**:
- Category "Food" exists with child category "Groceries"

**Test Steps**:
1. Navigate to category management page
2. Locate category "Food"
3. Click delete button for "Food"
4. Observe system response

**Expected Outcome**:
- System prevents deletion
- Error message displayed: "Cannot delete category with child categories" or "Must reassign or delete child categories first"
- Category "Food" remains in database
- Child category "Groceries" remains intact
- Data integrity maintained

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-042: Budget roll-up for parent categories

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-013

**Preconditions**:
- Category hierarchy: "Food" > "Groceries" ($300), "Dining Out" ($200)
- Budgets exist for both child categories for the same period

**Test Steps**:
1. Navigate to budget summary or category view
2. Locate the parent category "Food"
3. Examine the budget total displayed for "Food"
4. Verify calculation: $300 + $200 = $500

**Expected Outcome**:
- Parent category "Food" shows total budget of $500.00 (sum of children)
- Budget roll-up calculation is automatic and accurate
- If expenses exist against child categories, parent also shows total spent
- Visual indication that parent total is a roll-up (e.g., different styling, icon, or label)

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-043: 2-level hierarchy depth limit

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-008

**Preconditions**:
- 2-level hierarchy exists: "Food" (level 1) > "Groceries" (level 2)

**Test Steps**:
1. Navigate to category creation page
2. Attempt to create a 3rd level (grandchild) category:
   - Name: "Organic Groceries"
   - Parent: "Groceries"
3. Submit the form
4. Observe system response

**Expected Outcome**:
- System prevents 3rd level creation
- Error message displayed: "Maximum hierarchy depth (2 levels) reached" or "Cannot create category: parent already has a parent"
- No 3rd level category created in database
- Hierarchy remains at 2 levels maximum

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-044: Hierarchical category display

**User Story**: US6 - Category Hierarchy Testing
**Priority**: P2
**Functional Requirements**: FR-010

**Preconditions**:
- Multiple categories exist with mixed hierarchy:
  - "Food" > "Groceries", "Dining Out"
  - "Transportation" (no children)
  - "Utilities" (no children)

**Test Steps**:
1. Navigate to category list/management page
2. Observe how categories are displayed
3. Verify visual hierarchy indicators

**Expected Outcome**:
- Categories are displayed in hierarchical format:
  - Parent categories clearly identified (e.g., bold, icon, or no indentation)
  - Child categories nested/indented under their parents
  - Flat categories (no parent/children) shown at root level
- Visual distinction between parent and child categories
- Easy to understand hierarchy at a glance
- Possible visual elements: indentation, tree lines, expand/collapse icons, or parent badges

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

## Suite Completion Notes

**Execution Summary**: (pending)

**Overall Assessment**: (pending)

**Defects Found**: 0
