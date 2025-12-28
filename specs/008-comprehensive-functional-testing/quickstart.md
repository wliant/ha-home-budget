# Quickstart Guide: Comprehensive Functional Testing

**Feature**: 008-comprehensive-functional-testing
**Date**: 2025-12-28
**Audience**: QA testers, developers verifying fixes

## Overview

This guide provides step-by-step instructions for executing comprehensive manual functional testing of the home budget and expense tracking application. It covers environment setup, test execution workflows, and result documentation.

## Prerequisites

### Required Software

1. **Web Browsers** (latest stable versions):
   - Chrome
   - Firefox
   - Safari (macOS only)
   - Edge (Windows/macOS)
   - iOS Safari (iOS device or simulator)
   - Chrome Mobile (Android device or emulator)

2. **Application Access**:
   - Frontend running at `http://localhost:3001` (or deployed URL)
   - Backend running at `http://localhost:8080` (or deployed URL)
   - MySQL 8.0 database accessible

3. **Tools**:
   - Text editor for editing Markdown files (VS Code, Sublime, etc.)
   - Browser DevTools (built into all browsers)
   - Optional: Browser extension for header manipulation (ModHeader, Simple Modify Headers)
   - Optional: curl or Postman for API testing

### Application State

- Application deployed with all features 001-007 implemented
- Database schema initialized (budgets, expenses, categories tables exist)
- Backend API accessible and responding
- Frontend can communicate with backend

## Environment Setup

### Step 1: Verify Application is Running

**Check Frontend**:
```bash
curl http://localhost:3001
# Should return HTML (homepage)
```

**Check Backend**:
```bash
curl http://localhost:8080/api/health
# Should return 200 OK or health status JSON
```

**Check Database**:
```bash
mysql -u root -p -h localhost
> USE budget_db;
> SHOW TABLES;
# Should show: budgets, categories, expenses (and others)
> SELECT COUNT(*) FROM budgets;
# Note the count for later comparison
```

### Step 2: Set Up Browser for Multi-User Testing

**Option A: Browser Extension (Recommended)**

1. Install ModHeader extension (Chrome/Edge) or Simple Modify Headers (Firefox)
2. Configure header:
   - Name: `X-Hass-User`
   - Value: `alice` (or test username)
3. Enable extension
4. Reload application page
5. Verify header is sent (check Network tab in DevTools)

**Option B: Manual DevTools Method**

1. Open browser DevTools (F12 or Cmd+Option+I)
2. Go to Network tab
3. Load a page (e.g., homepage)
4. Right-click on request → "Edit and Resend" or "Copy as fetch"
5. Modify request to include `X-Hass-User` header
6. Note: This is more tedious, requires resending for each test

**Option C: curl for API Testing**

```bash
curl -H "X-Hass-User: alice" http://localhost:8080/api/budgets
```

### Step 3: Prepare Test Result Directories

```bash
cd specs/008-comprehensive-functional-testing
mkdir -p test-results
mkdir -p test-results/screenshots
```

### Step 4: Copy Test Suite Templates

```bash
# Copy template for each test suite
cp contracts/test-suite-template.md test-results/category-management.md
cp contracts/test-suite-template.md test-results/budget-management.md
cp contracts/test-suite-template.md test-results/expense-recording.md
cp contracts/test-suite-template.md test-results/dashboard.md
cp contracts/test-suite-template.md test-results/integration.md
cp contracts/test-suite-template.md test-results/ui-navigation.md
cp contracts/test-suite-template.md test-results/date-handling.md

# Create defect log
echo "# Defect Log\n\n**Feature**: 008-comprehensive-functional-testing\n\n## Summary\n\n| Defect ID | Severity | Title | Status |\n|-----------|----------|-------|--------|\n\n## Defect Details\n" > test-results/defects.md
```

## Database Reset Procedure

**Purpose**: Ensure clean state before each major test suite

**When to Reset**:
- Before Integration test suite
- Before Category Management test suite
- Before Budget Management test suite
- Before Expense Recording test suite
- Before Dashboard test suite

**How to Reset**:

**Method 1: SQL Script (Recommended)**

```sql
-- Save as: test-results/reset-database.sql
USE budget_db;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE expenses;
TRUNCATE TABLE budgets;
TRUNCATE TABLE categories;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Database reset complete' AS status;
SELECT COUNT(*) AS expenses_count FROM expenses;
SELECT COUNT(*) AS budgets_count FROM budgets;
SELECT COUNT(*) AS categories_count FROM categories;
```

Execute:
```bash
mysql -u root -p < test-results/reset-database.sql
```

**Method 2: Backend API (If Available)**

```bash
curl -X POST -H "X-Hass-User: admin" http://localhost:8080/api/test/reset
# Only if backend provides test reset endpoint
```

**Method 3: Docker Restart (Nuclear Option)**

```bash
docker-compose down
docker-compose up -d
# Wait for services to start
sleep 10
```

**Verification**:
```sql
SELECT COUNT(*) FROM expenses;   -- Should be 0
SELECT COUNT(*) FROM budgets;    -- Should be 0
SELECT COUNT(*) FROM categories; -- Should be 0
```

## Test Execution Workflow

### Phase 1: Prepare Test Suite

1. **Open Test Suite File**:
   ```bash
   code test-results/integration.md  # Or category-management.md, etc.
   ```

2. **Fill Prerequisites Section**:
   - Document required database state
   - List test users (alice, bob, etc.)
   - Note browser versions being used

3. **Create Test Case Entries**:
   - One entry per functional requirement
   - Fill Expected Outcome based on spec.md acceptance scenarios
   - Set Status to NOT_RUN initially

### Phase 2: Execute Tests

1. **Reset Database** (if required by suite):
   ```bash
   mysql -u root -p < test-results/reset-database.sql
   ```

2. **Configure Browser**:
   - Set X-Hass-User header to test username (e.g., alice)
   - Open DevTools Network tab (for monitoring API calls)
   - Set screen size if testing responsiveness

3. **Execute Each Test Case**:
   - Follow test steps exactly as written
   - Observe actual behavior
   - Record Actual Outcome in test case entry
   - Update Status: PASS | FAIL | BLOCKED | SKIPPED
   - Capture screenshots for failures
   - Note any observations in Notes field

4. **For Failed Tests**:
   - Immediately create defect entry in defects.md
   - Assign severity (CRITICAL/HIGH/MEDIUM/LOW)
   - Document reproduction steps (Given/When/Then)
   - Capture evidence (screenshot, console log, network log)
   - Link defect ID in test case entry

### Phase 3: Complete Test Suite

1. **Calculate Summary Statistics**:
   - Count total test cases
   - Count PASS, FAIL, BLOCKED, SKIPPED
   - Calculate pass rate: (passed / total) * 100

2. **Fill Suite Completion Notes**:
   - Record start/end time and duration
   - Summarize key findings
   - List blockers encountered
   - Note recommendations

3. **Update Defect Log**:
   - Ensure all defects from this suite are logged
   - Update defects.md summary table

## Test Execution Order

Execute test suites in this sequence (database reset at each numbered step):

1. **Integration Testing** (`integration.md`)
   - Validates backend API contracts work
   - Tests X-Hass-User authentication
   - Verifies frontend-backend communication
   - **Database Reset**: Yes

2. **Category Management** (`category-management.md`)
   - Tests category creation (root and child)
   - Tests hierarchy constraints
   - Tests category deletion prevention
   - **Database Reset**: Yes (clean state for categories)

3. **Budget Management** (`budget-management.md`)
   - Tests budget creation with categories
   - Tests parent-child budget validation
   - Tests duplicate prevention
   - **Database Reset**: Yes (fresh categories for budgets)

4. **Expense Recording** (`expense-recording.md`)
   - Tests expense creation
   - Tests date handling
   - Tests budget tracking
   - **Database Reset**: Yes (fresh budgets for expenses)

5. **Dashboard** (`dashboard.md`)
   - Tests empty states
   - Populate data then test populated states
   - Tests current month calculations
   - **Database Reset**: Yes (control dashboard data)

6. **UI/Navigation** (`ui-navigation.md`) - Can run in parallel
   - Tests responsive layouts
   - Tests navigation flows
   - Tests loading states
   - **Database Reset**: No (uses existing data)

7. **Date Handling** (`date-handling.md`) - Can run in parallel
   - Tests date pickers
   - Tests month/year selection
   - Tests date-based filtering
   - **Database Reset**: No (uses existing data)

## Multi-User Testing Scenarios

**Simulate Concurrent Users**:

1. **Option A: Two Browser Windows**:
   - Window 1: Set X-Hass-User to `alice` (via ModHeader)
   - Window 2: Set X-Hass-User to `bob` (via ModHeader)
   - Perform actions in both windows to simulate concurrent operations

2. **Option B: Incognito + Normal**:
   - Normal window: alice
   - Incognito window: bob (configure ModHeader in incognito mode)

3. **Option C: Different Browsers**:
   - Chrome: alice
   - Firefox: bob

**Test Concurrent Budget Creation** (Example):

1. Window 1 (alice): Navigate to create budget page
2. Window 2 (bob): Navigate to create budget page
3. Both: Select same category "Food" and month "January 2025"
4. Window 1: Click Save (should succeed)
5. Window 2: Click Save (should fail with "Budget already exists")
6. Verify: Only one budget created in database

## Browser Testing

**Desktop Browsers**:

1. **Chrome**:
   ```bash
   # Check version
   chrome://version
   ```
   - Test all P1 scenarios
   - Record version in test results

2. **Firefox**:
   ```bash
   # Check version
   about:support
   ```
   - Test all P1 scenarios
   - Note any browser-specific behaviors

3. **Safari** (macOS):
   - Test responsive layouts
   - Test date pickers (Safari date input differs)

4. **Edge**:
   - Test at least P1 scenarios
   - Chromium-based, should behave like Chrome

**Mobile Browsers**:

1. **DevTools Device Emulation**:
   - Chrome DevTools → Toggle Device Toolbar (Cmd+Shift+M)
   - Select device: iPhone 12 Pro, iPad Air, Pixel 5
   - Test responsive breakpoints: 320px, 768px, 1024px

2. **Real Devices** (if available):
   - iOS Safari on iPhone
   - Chrome Mobile on Android
   - Test touch interactions, keyboard behavior

## Performance Testing

**Measure Dashboard Load Time**:

1. Open Chrome DevTools → Performance tab
2. Click Record
3. Navigate to homepage dashboard
4. Stop recording
5. Check "Load" event time
6. Verify < 3 seconds (Success Criterion SC-006)

**Measure API Response Time**:

1. Open DevTools → Network tab
2. Filter by "XHR" or "Fetch"
3. Trigger API call (e.g., create budget)
4. Check request timing
5. Verify < 2 seconds (Success Criterion SC-003)

## Edge Case Testing

**Test Backend Unavailability**:

1. Stop backend service:
   ```bash
   docker-compose stop budget-backend
   ```

2. Attempt to load dashboard in browser
3. Observe error handling (should show error message, not crash)
4. Restart backend:
   ```bash
   docker-compose start budget-backend
   ```

**Test Invalid Authentication**:

1. Remove X-Hass-User header (disable ModHeader)
2. Attempt to access application
3. Verify error handling (should show auth error)

**Test XSS Attack**:

1. Create expense with description: `<script>alert('XSS')</script>`
2. Save expense
3. View expense in dashboard
4. Verify script is NOT executed (should be escaped/sanitized)

## Defect Workflow

### When Test Fails

1. **Stop and document immediately**:
   - Do not continue testing other cases if defect is CRITICAL
   - Create defect entry while details are fresh

2. **Assign Defect ID**:
   - Use next sequential number: DEF-001, DEF-002, etc.

3. **Determine Severity**:
   - CRITICAL: System crash, data loss, security issue
   - HIGH: Major feature broken, no workaround
   - MEDIUM: Works with workaround, minor impact
   - LOW: Cosmetic, no functional impact

4. **Document Thoroughly**:
   - Exact reproduction steps (Given/When/Then)
   - Expected vs actual behavior
   - Browser version and environment details
   - Screenshot or video if possible

5. **Link Bidirectionally**:
   - In defects.md: List test case IDs that found defect
   - In test-results/{suite}.md: List defect ID in test case entry

### Defect Resolution

1. **Developer fixes defect** → Status: IN_PROGRESS → FIXED

2. **Tester re-runs test case**:
   - Execute same test steps
   - Verify expected behavior now occurs
   - If PASS: Update defect status to VERIFIED
   - If still FAIL: Update defect with new findings, status stays FIXED

3. **Update test case**:
   - Change Status from FAIL to PASS
   - Add note: "Defect DEF-XXX fixed and verified"

## Completion Checklist

### Before Marking Test Suite Complete

- [ ] All test cases have status (none are NOT_RUN)
- [ ] All FAIL test cases have linked defects
- [ ] All test execution details filled (Tested By, Date, Browser)
- [ ] Summary statistics calculated
- [ ] Pass rate calculated
- [ ] Suite Completion Notes filled
- [ ] Screenshots captured for failures
- [ ] Defects logged in defects.md

### Before Marking All Testing Complete

- [ ] All 7 test suites completed
- [ ] All P1 user stories have test cases executed
- [ ] Coverage summary generated (all 43 functional requirements tested)
- [ ] No CRITICAL or HIGH defects in OPEN status (or explicitly accepted)
- [ ] Overall test report written (summary of findings)
- [ ] Recommendations documented for next steps

## Troubleshooting

### Problem: X-Hass-User Header Not Working

**Symptom**: Backend returns authentication error

**Solution**:
1. Verify header name is exactly `X-Hass-User` (case-sensitive)
2. Check browser extension is enabled
3. Verify header shows in Network tab request headers
4. Try curl to test backend directly:
   ```bash
   curl -H "X-Hass-User: alice" http://localhost:8080/api/budgets
   ```

### Problem: Database Reset Fails

**Symptom**: Foreign key constraint errors

**Solution**:
1. Ensure `SET FOREIGN_KEY_CHECKS = 0;` in reset script
2. Check table order (truncate child tables before parents)
3. Nuclear option: Drop and recreate database:
   ```sql
   DROP DATABASE budget_db;
   CREATE DATABASE budget_db;
   # Re-run schema migrations
   ```

### Problem: Test Data Persists After Reset

**Symptom**: "Clean state" has existing records

**Solution**:
1. Verify correct database connection (not production!)
2. Check TRUNCATE statements executed successfully
3. Run SELECT COUNT(*) to verify zero records
4. If using Docker, ensure volumes cleared:
   ```bash
   docker-compose down -v
   ```

### Problem: Cannot Reproduce Defect

**Symptom**: Defect description says FAIL but retesting shows PASS

**Solution**:
1. Verify exact same preconditions (database state, user, browser)
2. Check if defect was already fixed (ask developer)
3. Document inability to reproduce in defect notes
4. Consider changing severity to LOW or WONT_FIX if not critical

## Example Workflow

### Complete Test Suite Execution (Category Management)

```bash
# 1. Reset database
mysql -u root -p < test-results/reset-database.sql

# 2. Open test suite file
code test-results/category-management.md

# 3. Configure browser
# - Set X-Hass-User to alice via ModHeader
# - Open DevTools (F12)

# 4. Execute TC-001: Create root category "Food"
# - Navigate to http://localhost:3001/categories
# - Click "Add Category"
# - Enter name: "Food"
# - Don't select parent
# - Click Save
# - Observe: Category created successfully ✅ PASS

# 5. Execute TC-002: Create child category "Groceries"
# - Click "Add Category"
# - Enter name: "Groceries"
# - Select parent: "Food"
# - Click Save
# - Observe: Category created, shows under Food ✅ PASS

# 6. Execute TC-003: Test circular reference prevention
# - Edit category "Food"
# - Try to set parent to "Groceries"
# - Observe: Should show error... but doesn't! ❌ FAIL
# - Create defect DEF-001 (HIGH severity)
# - Take screenshot: test-results/screenshots/DEF-001-circular-ref.png
# - Document in defects.md

# 7. Continue through remaining test cases...

# 8. Calculate summary
# - Total: 10 test cases
# - PASS: 9
# - FAIL: 1
# - Pass rate: 90%

# 9. Fill completion notes
# - Duration: 45 minutes
# - Defects found: 1 (HIGH)
# - Recommendation: Fix DEF-001 before release
```

## References

- Feature Specification: [spec.md](./spec.md)
- Implementation Plan: [plan.md](./plan.md)
- Research & Decisions: [research.md](./research.md)
- Data Model: [data-model.md](./data-model.md)
- Test Suite Template: [contracts/test-suite-template.md](./contracts/test-suite-template.md)
- Defect Template: [contracts/defect-template.md](./contracts/defect-template.md)
