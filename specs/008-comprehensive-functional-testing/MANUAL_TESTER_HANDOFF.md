# Manual Tester Handoff Guide: Feature 008

**Feature**: Comprehensive Functional Testing
**Status**: Ready for Manual Test Execution
**Date**: 2025-12-28
**Estimated Testing Time**: 20-30 hours

---

## Quick Start for Manual Testers

Welcome! This guide will help you set up your testing environment and begin executing the comprehensive functional test suite for the Home Budget Tracker application.

### What You'll Be Testing

You'll be executing **144 manual test tasks** organized across **7 test suites** with **62 detailed test cases** covering:
- Backend API integration
- Multi-user workflows
- Category hierarchy management
- Budget and expense creation
- Dashboard calculations
- Date handling and validation
- UI navigation and responsive design

---

## Prerequisites Setup (15 minutes)

### Step 1: Verify Application is Running

**Task**: T003 - Verify application accessibility

1. **Check Frontend**:
   ```bash
   curl http://localhost:3001
   ```
   Expected: HTML response with "Home Budget Tracker"

2. **Check Backend API**:
   ```bash
   curl -H "X-Hass-User: alice" http://localhost:8080/api/budgets
   ```
   Expected: JSON array (may be empty if no budgets exist)

3. **Open in Browser**:
   - Navigate to: http://localhost:3001
   - You should see the homepage with "Budget Summary" section
   - No compilation errors or error overlays

✅ **Success Criteria**: All three checks pass without errors

---

### Step 2: Verify Database Access

**Task**: T004 - Verify MySQL database connectivity

1. **Connect to MySQL**:
   ```bash
   mysql -h localhost -P 3307 -u root -p
   ```
   Password: (check docker-compose.yml or project documentation)

2. **Verify Database and Tables**:
   ```sql
   USE homebudget;
   SHOW TABLES;
   ```
   Expected tables: `budgets`, `categories`, `expenses`, `users` (or similar)

3. **Test Query**:
   ```sql
   SELECT COUNT(*) FROM budgets;
   ```
   Should return a count (may be 0 if fresh database)

4. **Exit MySQL**:
   ```sql
   EXIT;
   ```

✅ **Success Criteria**: Can connect and query database successfully

---

### Step 3: Install Browser Header Extension

**Task**: T005 - Install HTTP header modification extension

The application uses Home Assistant authentication via the `X-Hass-User` HTTP header. You need a browser extension to simulate different users during multi-user testing.

#### Option A: ModHeader (Recommended - Chrome/Edge)

1. **Install ModHeader**:
   - Chrome: https://chrome.google.com/webstore (search "ModHeader")
   - Edge: https://microsoftedge.microsoft.com/addons (search "ModHeader")

2. **Configure for Testing**:
   - Click ModHeader icon in browser toolbar
   - Add Request Header:
     - Name: `X-Hass-User`
     - Value: `alice`
   - Click "Save"

3. **Test Multi-User Switching**:
   - Set value to `alice` → reload http://localhost:3001
   - Set value to `bob` → reload http://localhost:3001
   - Data should be isolated per user (if implemented correctly)

#### Option B: Simple Modify Headers (Firefox)

1. **Install Extension**:
   - Firefox: https://addons.mozilla.org (search "Simple Modify Headers")

2. **Configure**:
   - Add header: `X-Hass-User` with value `alice`
   - Enable the modification

#### Verification

Open browser console (F12) and run:
```javascript
fetch('/api/budgets')
  .then(r => r.json())
  .then(console.log)
```
Check Network tab to confirm `X-Hass-User: alice` header is present in request.

✅ **Success Criteria**: Header is visible in Network tab requests

---

### Step 4: Verify Browser Compatibility

**Task**: T006 - Ensure latest browser versions available

Test on at least **two browsers** from this list:
- ✅ Google Chrome (latest version)
- ✅ Mozilla Firefox (latest version)
- ✅ Safari (macOS - latest version)
- ✅ Microsoft Edge (latest version)

**Check Browser Versions**:
- Chrome/Edge: `chrome://version` or `edge://version`
- Firefox: `about:support`
- Safari: Safari → About Safari

✅ **Success Criteria**: At least 2 browsers installed and up-to-date

---

## Understanding the Test Organization

### Test Execution Phases

Tests are organized into **12 phases** with strict execution order:

| Phase | Name | Tasks | Priority | Est. Time |
|-------|------|-------|----------|-----------|
| 1 | Setup (Infrastructure) | T001-T010 | ✅ DONE | - |
| 2 | Foundational (Pre-checks) | T011-T013 | **START HERE** | 30 min |
| 3 | US7: Backend Integration (P1) 🎯 | T014-T025 | **CRITICAL** | 2-3 hrs |
| 4 | US1: End-to-End Lifecycle (P1) 🎯 | T026-T037 | P1 | 2-3 hrs |
| 5 | US2: Multi-User Testing (P1) 🎯 | T038-T051 | P1 | 2-3 hrs |
| 6 | US3: Validation/Errors (P1) 🎯 | T052-T066 | P1 | 2-3 hrs |
| 7 | US4: UI/Navigation (P2) | T067-T084 | P2 | 2-3 hrs |
| 8 | US5: Date Handling (P2) | T085-T097 | P2 | 2-3 hrs |
| 9 | US6: Category Hierarchy (P2) | T098-T111 | P2 | 2-3 hrs |
| 10-11 | Budget/Expense/Dashboard | T112-T134 | P2/P3 | 3-4 hrs |
| 12 | Coverage Analysis (Final) | T135-T142 | Final | 2-3 hrs |

**Execution Rules**:
1. **Phase 2 MUST be completed first** - these are foundational checks
2. **Phase 3 MUST pass before continuing** - backend integration is critical path
3. Phases 4-6 are P1 priority (core functionality)
4. Phases 7-9 are P2 priority (secondary features)
5. Phase 12 is final coverage analysis and reporting

---

## Test Suite Files

All test cases are pre-written in these files:

📁 `specs/008-comprehensive-functional-testing/test-results/`

| File | Test Cases | Coverage |
|------|------------|----------|
| `integration.md` | TC-001 to TC-025 (25 cases) | Backend API, authentication, error handling |
| `category-management.md` | TC-038 to TC-044 (7 cases) | Category hierarchy, parent-child validation |
| `ui-navigation.md` | TC-026 to TC-031 (6 cases) | Page navigation, responsive design |
| `date-handling.md` | TC-032 to TC-037 (6 cases) | Date inputs, month/year selection |
| `budget-management.md` | TC-045 to TC-049 (5 cases) | Budget CRUD operations |
| `expense-recording.md` | TC-050 to TC-056 (7 cases) | Expense creation workflow |
| `dashboard.md` | TC-057 to TC-062 (6 cases) | Dashboard display and calculations |

**Total**: 62 test cases

---

## How to Execute Tests

### Workflow for Each Test Case

1. **Read the Test Case** in the test suite file:
   - Test ID (e.g., TC-001)
   - Title
   - Prerequisites
   - Test Steps (numbered)
   - Expected Result
   - Actual Result (you fill this)
   - Status (you mark PASS/FAIL)

2. **Prepare Environment**:
   - Reset database if needed: `mysql < reset-database.sql`
   - Clear browser cache/cookies if testing fresh state
   - Set `X-Hass-User` header to correct user

3. **Execute Steps**:
   - Follow each numbered step exactly as written
   - Take screenshots if defect found (save to `test-results/screenshots/`)
   - Record any deviations from expected behavior

4. **Document Results**:
   - Update "Actual Result" field with what actually happened
   - Mark "Status" as PASS or FAIL
   - If FAIL, note "Defect ID" (create defect in defects.md)

5. **Log Defects** (if any):
   - Use template from `contracts/defect-template.md`
   - Add entry to `test-results/defects.md`
   - Assign severity: CRITICAL, HIGH, MEDIUM, LOW
   - Include reproduction steps and screenshot path

---

## Test Execution Example

**Example from `integration.md`**:

```markdown
### TC-001: Backend API responds to health check

**Test ID**: TC-001
**Priority**: P1
**Prerequisites**: Backend running on http://localhost:8080

**Test Steps**:
1. Send GET request to http://localhost:8080/api/health
2. Verify response status is 200 OK
3. Verify response body contains "status": "UP"

**Expected Result**: API returns 200 OK with health status

**Actual Result**: ___[TESTER FILLS THIS]___

**Status**: ⬜ PASS / ⬜ FAIL
**Defect ID**: ___[if failed]___
**Tested By**: ___[Your Name]___
**Date**: ___[YYYY-MM-DD]___
```

**You would fill**:
```markdown
**Actual Result**: API returned 200 OK, body: {"status":"UP","timestamp":1703772000}

**Status**: ✅ PASS
**Tested By**: Alice Tester
**Date**: 2025-12-28
```

---

## Using the Database Reset Script

**When to Reset**:
- Before starting a new test suite
- When test data becomes inconsistent
- When testing requires "fresh" state

**How to Reset**:
```bash
cd /Users/wliant/workspace/github/ha-hello/specs/008-comprehensive-functional-testing/test-results
mysql -h localhost -P 3307 -u root -p homebudget < reset-database.sql
```

**What It Does**:
- Clears all budgets, expenses, categories
- Resets auto-increment counters
- Does NOT drop tables (schema remains intact)

---

## Defect Logging Process

### When to Log a Defect

Log a defect when:
- Expected result does not match actual result
- Application crashes or shows error
- Data is incorrect or inconsistent
- UI is broken or unresponsive
- Performance is significantly slower than expected

### Severity Classification

- **CRITICAL**: System crash, data loss, security vulnerability, complete feature failure
- **HIGH**: Major functionality broken, no workaround, significantly impacts user
- **MEDIUM**: Functionality works with workaround, minor user impact, edge case
- **LOW**: Cosmetic issue, minor UI inconsistency, no functional impact

### Defect Template

1. Open `test-results/defects.md`
2. Copy template from `contracts/defect-template.md`
3. Fill all required fields:
   - Defect ID (next sequential: DEF-002, DEF-003, etc.)
   - Severity (CRITICAL/HIGH/MEDIUM/LOW)
   - Title (brief description)
   - Status (OPEN)
   - Affected Features
   - Test Case(s) that failed
   - Description (what went wrong)
   - Steps to Reproduce (numbered, detailed)
   - Expected Behavior
   - Actual Behavior
   - Browser/Platform
   - Test Environment details
   - Screenshot paths (if applicable)
4. Add entry to summary table at top of defects.md
5. Save file

---

## Screenshot Guidelines

**When to Capture**:
- Every defect found (visual evidence)
- UI layout issues
- Error messages
- Unexpected behavior

**Where to Save**:
```
specs/008-comprehensive-functional-testing/test-results/screenshots/
```

**Naming Convention**:
```
DEF-###-description.png
TC-###-step-#-issue.png
```

**Examples**:
- `DEF-002-broken-layout.png`
- `TC-015-step-3-missing-button.png`

**Tool Recommendations**:
- macOS: Cmd+Shift+4 (native screenshot)
- Windows: Snipping Tool or Snip & Sketch
- Linux: gnome-screenshot or scrot
- Browser DevTools: Right-click → "Capture screenshot"

---

## Multi-User Testing

### Test Users

Use these usernames with `X-Hass-User` header:
- **alice** - Primary test user
- **bob** - Secondary test user for multi-user scenarios

### User Isolation Tests

Some test cases (TC-038 to TC-051) verify that:
- User A cannot see User B's budgets/expenses
- User B cannot modify User A's data
- Each user has independent data scope

**Setup for Multi-User Tests**:
1. Set header to `alice` → create budget/expense
2. Switch header to `bob` → verify cannot access alice's data
3. Create bob's own budget/expense
4. Switch back to `alice` → verify alice's data intact

---

## Test Execution Workflow Summary

### Daily Testing Session

1. **Start of Day** (10 min):
   - Verify application running (Step 1 from prerequisites)
   - Review today's test suite and tasks
   - Reset database if needed
   - Set browser header to `alice`

2. **Execute Tests** (2-4 hours):
   - Follow test cases sequentially
   - Document results in test suite files
   - Log defects immediately when found
   - Take screenshots for evidence

3. **End of Day** (10 min):
   - Review defect log for completeness
   - Update coverage-summary.md with progress
   - Save all test suite files
   - Note any blockers or questions

### Recommended Schedule

| Day | Phases | Tasks | Time |
|-----|--------|-------|------|
| Day 1 | Phase 2-3 | T011-T025 | 3-4 hrs |
| Day 2 | Phase 4 | T026-T037 | 2-3 hrs |
| Day 3 | Phase 5 | T038-T051 | 2-3 hrs |
| Day 4 | Phase 6 | T052-T066 | 2-3 hrs |
| Day 5 | Phase 7-8 | T067-T097 | 4-5 hrs |
| Day 6 | Phase 9-11 | T098-T134 | 4-5 hrs |
| Day 7 | Phase 12 | T135-T142 | 2-3 hrs |

**Total**: Approximately 20-30 hours over 7 days

---

## Important Files Reference

### Test Documentation
- **Test Suites**: `test-results/*.md` (7 files, 62 test cases)
- **Defect Log**: `test-results/defects.md`
- **Coverage Summary**: `test-results/coverage-summary.md`
- **Database Reset**: `test-results/reset-database.sql`

### Guidance Documents
- **Testing Guide**: `TESTING_GUIDE.md` (comprehensive procedures)
- **Feature Spec**: `spec.md` (7 user stories, 43 requirements)
- **Implementation Plan**: `plan.md` (technical context)
- **Task List**: `tasks.md` (all 150 tasks with dependencies)

### Templates
- **Test Suite Template**: `contracts/test-suite-template.md`
- **Defect Template**: `contracts/defect-template.md`

---

## Test Environment Details

### Application URLs
- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080
- **Database**: localhost:3307 (MySQL 8.0)

### Technology Stack
- Frontend: Next.js 14.0.4, React 18.x, Material-UI v5
- Backend: Spring Boot 3.2.0, Java 17
- Database: MySQL 8.0
- Authentication: Home Assistant (X-Hass-User header)

### Docker Commands (if needed)

**Check Status**:
```bash
docker-compose ps
```

**View Logs**:
```bash
docker-compose logs frontend
docker-compose logs backend
docker-compose logs mysql
```

**Restart Services**:
```bash
docker-compose restart frontend
docker-compose restart backend
```

**Full Restart**:
```bash
docker-compose down
docker-compose up -d
```

---

## Getting Help

### Common Issues

**Issue**: Application shows "Failed to compile" error
**Solution**: This was DEF-001, already fixed. Ensure containers are rebuilt:
```bash
docker-compose down
docker-compose build frontend
docker-compose up -d
```

**Issue**: API returns 401 Unauthorized
**Solution**: Check `X-Hass-User` header is set in browser extension

**Issue**: Database connection failed
**Solution**: Verify MySQL container running: `docker-compose ps mysql`

**Issue**: Test case unclear or ambiguous
**Solution**: Document question in test notes, make reasonable interpretation, flag for review

### Reporting Problems

If you encounter issues with:
- **Test case clarity**: Note in test suite file, continue with best interpretation
- **Application bugs**: Log as defect in defects.md
- **Environment issues**: Contact development team
- **Missing test data**: Use database reset script or create manually

---

## Success Criteria for Manual Testing

Your testing is complete when:
- ✅ All 144 manual tasks (T011-T150) executed
- ✅ All 62 test cases documented with PASS/FAIL status
- ✅ All defects logged with severity and reproduction steps
- ✅ Coverage summary shows 100% test execution
- ✅ No CRITICAL or HIGH severity defects remain open (or acknowledged as known issues)
- ✅ Final test execution report generated

---

## Next Steps After Testing Complete

1. **Generate Final Report**:
   - Use `TEST_EXECUTION_REPORT_TEMPLATE.md` to create summary
   - Include pass/fail statistics
   - List all defects found
   - Provide recommendations

2. **Review with Team**:
   - Present findings to project manager
   - Prioritize defect fixes
   - Discuss any blockers or concerns

3. **Close Feature**:
   - If all P1 tests pass and no blocking defects: feature can be released
   - If defects found: development team fixes, retest affected areas

---

**Ready to Begin?**

Start with Phase 2 (Tasks T011-T013) in the TESTING_GUIDE.md.

Good luck with testing! 🚀
