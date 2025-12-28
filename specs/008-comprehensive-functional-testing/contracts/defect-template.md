# Defect Template

**Purpose**: Standard format for defect documentation in defects.md

**Location**: `test-results/defects.md`

**Version**: 1.0.0

## Defect Entry Template

```markdown
### DEF-{###}: {Brief Defect Title}

**Severity**: CRITICAL | HIGH | MEDIUM | LOW
**Status**: OPEN | IN_PROGRESS | FIXED | VERIFIED | CLOSED | WONT_FIX

**Affected Features**: {Feature area 1}, {Feature area 2}
**Functional Requirements**: FR-###, FR-###
**Test Cases**: TC-###, TC-###

**Description**:
{Detailed explanation of the defect - what is wrong and why it matters}

**Steps to Reproduce**:
1. **Given** {precondition or state}
2. **When** {action performed}
3. **Then** {actual behavior observed (incorrect)}

**Expected Behavior**:
{What should happen according to functional requirements}

**Actual Behavior**:
{What actually happens (the defect)}

**Browser/Platform**:
- {Browser name} {version} on {OS}
- {If reproduced in multiple browsers, list all}
- {If browser-specific, note "Not reproduced in {other browsers}"}

**Frequency**: Always | Often | Sometimes | Rare

**Test Environment**:
- Frontend URL: {URL}
- Backend URL: {URL}
- Database: MySQL {version}
- Test User: {username} (X-Hass-User header)

**Evidence**:
- Screenshot: {filename or path}
- Browser Console Log: {relevant errors}
- Network Log: {API request/response details}
- Video Recording: {filename if applicable}

**Workaround**:
{If a workaround exists, describe it. Otherwise, state "None"}

**Discovered By**: {Tester name}
**Discovered Date**: YYYY-MM-DD

**Notes**:
{Additional context, related defects, investigation findings}

**History**:
- YYYY-MM-DD: Defect created (OPEN)
- YYYY-MM-DD: {Status change or investigation update}

---
```

## Severity Definitions

### CRITICAL
- **Impact**: System crash, data loss, security vulnerability, complete feature failure
- **Examples**:
  - Application crashes when creating a budget
  - Data corruption: expenses disappear after refresh
  - Authentication bypass: can access without X-Hass-User header
  - SQL injection vulnerability in expense description field

- **Response**: Must be fixed immediately before any further testing

### HIGH
- **Impact**: Major functionality broken, no workaround available, significantly impacts user
- **Examples**:
  - Cannot create budgets at all (button doesn't work)
  - Dashboard shows incorrect totals (wrong calculations)
  - Multi-user data not visible (alice can't see bob's expenses)
  - Backend returns 500 error for valid requests

- **Response**: Must be fixed before feature can be considered functional

### MEDIUM
- **Impact**: Functionality works with workaround, minor user impact, edge case failure
- **Examples**:
  - Validation error message unclear (says "Error" instead of specific issue)
  - Date picker defaults to wrong month (user can still select correct date)
  - Category dropdown not sorted alphabetically (user can still find categories)
  - Dashboard load time 5 seconds (>3 second target but still functional)

- **Response**: Should be fixed but doesn't block feature release

### LOW
- **Impact**: Cosmetic issue, minor UI inconsistency, no functional impact
- **Examples**:
  - Button alignment slightly off
  - Font size inconsistent between pages
  - Console warning (no user-visible impact)
  - Extra whitespace in layout

- **Response**: Nice to fix but low priority

## Status Definitions

- **OPEN**: Defect discovered and documented, not yet assigned or being worked on
- **IN_PROGRESS**: Being investigated or fixed by development team
- **FIXED**: Code changes made, awaiting verification testing
- **VERIFIED**: Fix confirmed by retesting, defect no longer reproducible
- **CLOSED**: Resolved and verified, no further action needed
- **WONT_FIX**: Acknowledged but will not be addressed (with justification)

## Example Defect Entries

### Example 1: Critical Severity

```markdown
### DEF-001: Budget Creation Crashes Application When Category is Deleted Mid-Form

**Severity**: CRITICAL
**Status**: OPEN

**Affected Features**: Budget Management, Category Management
**Functional Requirements**: FR-011, FR-014
**Test Cases**: TC-023, TC-048

**Description**:
When a user has the budget creation form open and another user deletes the selected category, attempting to submit the budget causes the application to crash with an unhandled exception. This results in a blank page and requires browser refresh to recover.

**Steps to Reproduce**:
1. **Given** User Alice is on the budget creation page with category "Food" selected
2. **When** User Bob deletes category "Food" while Alice's form is still open
3. **And** Alice clicks "Save Budget" button
4. **Then** Frontend crashes with "Cannot read property 'id' of null" error in console

**Expected Behavior**:
System should detect category no longer exists and show validation error: "Selected category has been deleted. Please choose another category or refresh the page."

**Actual Behavior**:
Application displays blank white page. Browser console shows uncaught TypeError. User must manually refresh browser to recover.

**Browser/Platform**:
- Chrome 120.0.6099.109 on macOS 14.1
- Also reproduced in Firefox 121.0 on macOS 14.1

**Frequency**: Always (100% reproduction rate in testing)

**Test Environment**:
- Frontend URL: http://localhost:3001
- Backend URL: http://localhost:8080
- Database: MySQL 8.0.35
- Test Users: alice and bob (X-Hass-User headers)

**Evidence**:
- Screenshot: `screenshots/DEF-001-blank-page.png`
- Browser Console Log: `TypeError: Cannot read property 'id' of null at BudgetForm.handleSubmit (BudgetForm.tsx:142)`
- Network Log: GET /api/categories/123 returns 404, but frontend doesn't handle it

**Workaround**: None. User must refresh browser and re-enter all form data.

**Discovered By**: Jane Smith
**Discovered Date**: 2025-12-28

**Notes**:
This is a race condition issue. If category exists when form loads but is deleted before submit, frontend state becomes inconsistent. Backend returns 404 for category ID, but frontend doesn't validate before submitting budget.

**History**:
- 2025-12-28: Defect created (OPEN)
```

### Example 2: Medium Severity

```markdown
### DEF-002: Budget Dashboard Shows "Loading..." Indefinitely When Backend is Slow

**Severity**: MEDIUM
**Status**: OPEN

**Affected Features**: Dashboard
**Functional Requirements**: FR-023, FR-036
**Test Cases**: TC-067

**Description**:
When the backend API response takes longer than 10 seconds (simulated by network throttling), the dashboard shows "Loading..." indefinitely without timeout or error message. The data eventually loads when the API responds, but there's no user feedback during the delay.

**Steps to Reproduce**:
1. **Given** Browser DevTools throttling set to "Slow 3G"
2. **When** User navigates to homepage dashboard
3. **Then** "Loading..." spinner shows for 15+ seconds with no timeout or error message

**Expected Behavior**:
- Show loading indicator for up to 10 seconds
- After 10 seconds, display error message: "Dashboard is taking longer than expected to load. Please check your connection or try again."
- Provide "Retry" button

**Actual Behavior**:
- "Loading..." spinner shows indefinitely
- No timeout mechanism
- Eventually loads after 15+ seconds when API responds
- No error handling for slow connections

**Browser/Platform**:
- Chrome 120.0.6099.109 on macOS 14.1 (with DevTools throttling)

**Frequency**: Always (when API response > 10 seconds)

**Test Environment**:
- Frontend URL: http://localhost:3001
- Backend URL: http://localhost:8080 (artificially slowed via DevTools)
- Database: MySQL 8.0.35
- Test User: alice

**Evidence**:
- Screenshot: `screenshots/DEF-002-loading-spinner.png`
- Network Log: GET /api/dashboard takes 15.2 seconds to complete

**Workaround**: Disable network throttling, or manually refresh page after waiting.

**Discovered By**: Jane Smith
**Discovered Date**: 2025-12-28

**Notes**:
This is not a critical issue because the data eventually loads and the system doesn't crash. However, it violates Success Criterion SC-006 (dashboard loads < 3 seconds). The lack of timeout and error handling impacts user experience on slow connections.

Related to FR-036 (loading states during async operations).

**History**:
- 2025-12-28: Defect created (OPEN)
```

## Usage Instructions

1. **Create Defect Entry**:
   - When test case Status = FAIL, create defect in defects.md
   - Assign next sequential defect ID (DEF-001, DEF-002, etc.)
   - Use template above and fill all required fields

2. **Assign Severity**:
   - Use definitions above to determine CRITICAL/HIGH/MEDIUM/LOW
   - If unsure, start with higher severity and adjust after review
   - Consider impact on user, data integrity, security

3. **Link to Test Cases**:
   - List all test case IDs that discovered this defect
   - Update test case entries to reference defect ID
   - Maintain bidirectional traceability

4. **Gather Evidence**:
   - Take screenshots of error states
   - Copy browser console errors
   - Save network logs showing API failures
   - Record reproduction steps exactly

5. **Update Status**:
   - Add history entry when status changes
   - Include date and brief note (e.g., "Fix deployed", "Verified passing")
   - Keep OPEN defects at top of defects.md file for visibility

## Validation Checklist

Before finalizing defect entry:
- [ ] Defect ID is unique and sequential
- [ ] Severity assigned using definitions above
- [ ] Steps to reproduce are detailed and reproducible
- [ ] Expected vs actual behavior clearly stated
- [ ] At least one test case ID linked
- [ ] At least one functional requirement ID linked
- [ ] Affected features listed
- [ ] Discovered by and date filled
- [ ] Evidence captured (screenshot or log)
- [ ] Workaround documented (or "None" if none exists)
