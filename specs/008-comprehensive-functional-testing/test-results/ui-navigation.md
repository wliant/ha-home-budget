# Test Suite: UI Responsiveness and Navigation Testing

**Suite ID**: ui-navigation
**User Stories**: US4
**Functional Requirements**: FR-034, FR-035, FR-036, FR-037, FR-038
**Database Reset Required**: No
**Execution Order**: 6
**Status**: Not Started

## Overview

Validates responsive layouts across screen sizes (mobile, tablet, desktop), navigation flows, quick actions, loading states, and browser compatibility.

## Prerequisites

### Environment Setup
- Frontend running at http://localhost:3001
- Multiple browsers available: Chrome, Firefox, Safari, Edge (latest stable versions)
- Browser DevTools with Device Toolbar for responsive testing
- iOS Safari and Chrome Mobile (real devices or emulators)

### Database State
- Can reuse existing data from previous test suites

### User Authentication
- Simulated user: alice (via X-Hass-User header)

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-026 | Mobile layout (320px width) | P2 | NOT_RUN | - |
| TC-027 | "Record Expense" quick action navigation | P2 | NOT_RUN | - |
| TC-028 | "View Categories" navigation | P2 | NOT_RUN | - |
| TC-029 | Browser back button navigation | P2 | NOT_RUN | - |
| TC-030 | Expense item detail navigation | P2 | NOT_RUN | - |
| TC-031 | Loading indicators (slow network) | P2 | NOT_RUN | - |

**Summary Statistics**:
- Total Test Cases: 6
- Passed: 0
- Failed: 0
- Not Run: 6
- **Pass Rate**: 0%

**Browser Coverage**:
- Chrome: (pending)
- Firefox: (pending)
- Safari: (pending)
- Edge: (pending)
- iOS Safari: (pending)
- Chrome Mobile: (pending)

## Test Case Details

---

### TC-026: Mobile layout (320px width)

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-034

**Preconditions**:
- Frontend running at http://localhost:3001
- Browser with responsive design mode/Device Toolbar available
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Open Browser DevTools
2. Enable Device Toolbar / Responsive Design Mode
3. Set viewport width to 320px (minimum mobile width)
4. Navigate to homepage
5. Scroll through entire page
6. Test navigation to different pages (categories, budgets, expenses)
7. Observe layout behavior

**Expected Outcome**:
- Layout adapts to 320px width without horizontal scrolling
- Content stacks vertically
- All elements remain accessible and readable
- Text doesn't overflow containers
- Buttons and interactive elements are large enough for touch interaction
- Navigation menu collapses or transforms appropriately (e.g., hamburger menu)
- No overlapping elements or broken layouts

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(test on multiple browsers: Chrome, Firefox, Safari)_

---

### TC-027: "Record Expense" quick action navigation

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-035, FR-025

**Preconditions**:
- Frontend running at http://localhost:3001
- On homepage/dashboard
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage (http://localhost:3001)
2. Locate "Record Expense" quick action button on dashboard
3. Click the button
4. Observe navigation

**Expected Outcome**:
- Page navigates to expense recording page
- URL updates accordingly (e.g., /expenses/new or /record-expense)
- Expense form loads correctly
- No navigation errors or broken links
- Browser back button returns to homepage

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-028: "View Categories" navigation

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-035, FR-025

**Preconditions**:
- Frontend running at http://localhost:3001
- On homepage/dashboard
- Categories feature card visible

**Test Steps**:
1. Navigate to homepage
2. Locate "Categories" feature card
3. Click "View Categories" or similar link/button
4. Observe navigation

**Expected Outcome**:
- Page navigates to category management page
- URL updates to categories route (e.g., /categories)
- Category list/management interface loads correctly
- No navigation errors
- Browser back button returns to homepage

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-029: Browser back button navigation

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-037

**Preconditions**:
- Frontend running at http://localhost:3001
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Navigate to homepage
2. Click to navigate to another page (e.g., budget creation)
3. Fill in some form data (don't submit)
4. Click browser back button
5. Navigate forward again to the form page
6. Observe application state

**Expected Outcome**:
- Browser back button navigates to previous page correctly
- Application state is preserved or restored appropriately
- No application crashes or errors
- Navigation history works correctly with browser forward/back buttons
- Form data handling follows standard browser behavior (may or may not be preserved depending on implementation)
- URL and page content remain synchronized

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(optional observations)_

---

### TC-030: Expense item detail navigation

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-035

**Preconditions**:
- Frontend running at http://localhost:3001
- At least one expense exists in database
- Homepage displays recent expenses

**Test Steps**:
1. Navigate to homepage
2. Locate "Recent Expenses" section
3. Click on an expense item in the list
4. Observe navigation behavior

**Expected Outcome**:
- Clicking on expense item triggers navigation OR displays expense details
- If expense details/management page exists, it loads correctly showing full expense information
- If not implemented, clicking has no broken behavior
- Navigation is clear and intuitive
- Browser back button works correctly

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(behavior depends on whether expense detail view is implemented)_

---

### TC-031: Loading indicators (slow network)

**User Story**: US4 - UI Responsiveness and Navigation Testing
**Priority**: P2
**Functional Requirements**: FR-036

**Preconditions**:
- Frontend running at http://localhost:3001
- Browser DevTools Network throttling available
- Browser configured with X-Hass-User: alice

**Test Steps**:
1. Open Browser DevTools → Network tab
2. Enable network throttling (e.g., "Slow 3G" or "Fast 3G")
3. Navigate to homepage (or refresh if already there)
4. Observe dashboard loading behavior
5. Navigate to budget creation page
6. Observe form loading behavior
7. Submit a form (create budget/expense)
8. Observe submission loading behavior
9. Disable network throttling

**Expected Outcome**:
- Loading indicators are displayed during async operations:
  - Spinner, skeleton screens, or progress indicators shown while data loads
  - "Loading..." text or visual feedback provided
- Instead of blank/empty content, users see loading state
- Forms show loading state during submission (e.g., button becomes disabled with "Saving..." text)
- No indefinite blank screens
- User is aware that data is loading, not that the app is broken
- After loading completes, content appears correctly

**Actual Outcome**: _(to be filled during execution)_

**Status**: NOT_RUN
**Defect ID**: _(if failed)_
**Notes**: _(test with different throttling profiles: Slow 3G, Fast 3G, 4G)_

---

## Suite Completion Notes

**Execution Summary**: (pending)

**Browser-Specific Findings**: (pending)
