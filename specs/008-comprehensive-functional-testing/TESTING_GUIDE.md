# Comprehensive Functional Testing - Getting Started Guide

**Feature**: 008-comprehensive-functional-testing
**Status**: Test infrastructure complete, ready for execution
**Created**: 2025-12-28

## Current Status

### ✅ Completed (Phase 1 - Infrastructure Setup)

**6 of 10 Phase 1 tasks complete:**

1. ✅ **T001-T002**: Test results directory structure created
2. ✅ **T007**: Database reset script created (`reset-database.sql`)
3. ✅ **T008**: All 7 test suite files created with **62 detailed test cases**:
   - `integration.md` - 25 test cases (TC-001 to TC-025)
   - `category-management.md` - 7 test cases (TC-038 to TC-044)
   - `ui-navigation.md` - 6 test cases (TC-026 to TC-031)
   - `date-handling.md` - 6 test cases (TC-032 to TC-037)
   - `budget-management.md` - 5 test cases (TC-045 to TC-049)
   - `expense-recording.md` - 7 test cases (TC-050 to TC-056)
   - `dashboard.md` - 6 test cases (TC-057 to TC-062)
4. ✅ **T009**: Defect tracking log created (`defects.md`)
5. ✅ **T010**: Coverage summary created (`coverage-summary.md`)

**Documentation Created:**
- ✅ Specification (spec.md) - 7 user stories, 43 functional requirements
- ✅ Implementation plan (plan.md)
- ✅ Research decisions (research.md)
- ✅ Data model (data-model.md)
- ✅ Task breakdown (tasks.md) - 150 tasks
- ✅ Quickstart guide (quickstart.md)
- ✅ Test templates (contracts/)
- ✅ Tester guide (test-results/README.md)

### ⏳ Pending (Phase 1 - Manual Verification)

**4 tasks require manual action before testing can begin:**

- [ ] **T003**: Verify application is running
- [ ] **T004**: Verify MySQL database is accessible
- [ ] **T005**: Install browser extension for X-Hass-User header
- [ ] **T006**: Verify latest stable browsers available

### 📋 Remaining Work

**144 manual test execution tasks (T011-T150)**

All tasks require a human tester to execute test cases and document results.

---

## How to Start Testing

### Step 1: Start the Application

**Current Status**: ❌ Application not running (Docker daemon stopped)

**Action Required**:

```bash
# 1. Start Docker Desktop (ensure Docker daemon is running)

# 2. Navigate to project root
cd /Users/wliant/workspace/github/ha-hello

# 3. Start all services
docker-compose up -d

# 4. Verify services are running
docker-compose ps

# Expected output:
# - mysql: Up
# - backend: Up (port 8080)
# - frontend: Up (port 3001)

# 5. Check application accessibility
curl http://localhost:3001  # Should return HTML
curl http://localhost:8080  # Should return response
```

**Alternative - Start services individually:**

```bash
# Start MySQL only
docker-compose up -d mysql

# Build and start backend
cd budget-backend
mvn clean install
mvn spring-boot:run

# Start frontend (in separate terminal)
cd budget-tracker-ui
npm install
npm run dev
```

### Step 2: Verify Database

**Task T004: Verify MySQL database accessible**

```bash
# Connect to MySQL
mysql -u root -p -h localhost

# Enter password when prompted (check docker-compose.yml for password)

# Verify database exists
mysql> SHOW DATABASES;
# Should see: budget_db

# Check tables exist
mysql> USE budget_db;
mysql> SHOW TABLES;
# Should see: budgets, categories, expenses

# Exit MySQL
mysql> exit;
```

**Expected Tables:**
- `budgets` (id, category_id, year, month, amount, created_by)
- `categories` (id, name, parent_id)
- `expenses` (id, amount, description, date, category_id, created_by)

### Step 3: Install Browser Extension

**Task T005: Install browser extension for X-Hass-User header**

**Chrome/Edge:**
1. Open Chrome Web Store
2. Search for "ModHeader"
3. Click "Add to Chrome/Edge"
4. Configure header:
   - Name: `X-Hass-User`
   - Value: `alice`

**Firefox:**
1. Open Firefox Add-ons
2. Search for "Simple Modify Headers"
3. Click "Add to Firefox"
4. Configure header:
   - Header name: `X-Hass-User`
   - Header value: `alice`

**Verify header is being sent:**
1. Open browser DevTools → Network tab
2. Navigate to http://localhost:3001
3. Click any API request
4. Check Request Headers for `X-Hass-User: alice`

### Step 4: Verify Browsers

**Task T006: Verify latest stable browsers available**

Check you have these browsers installed:
- ✅ Chrome (latest stable)
- ✅ Firefox (latest stable)
- ✅ Safari (latest stable)
- ✅ Edge (latest stable)
- 🔲 iOS Safari (optional - for mobile testing)
- 🔲 Chrome Mobile (optional - for mobile testing)

```bash
# Check browser versions (macOS)
/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --version
/Applications/Firefox.app/Contents/MacOS/firefox --version
/Applications/Safari.app/Contents/version.plist  # Check in About Safari
```

### Step 5: Reset Database and Begin Testing

**Phase 2: Foundational Testing (T011-T013)**

```bash
# 1. Reset database to clean state
mysql -u root -p < specs/008-comprehensive-functional-testing/test-results/reset-database.sql

# 2. Verify reset
mysql -u root -p
mysql> USE budget_db;
mysql> SELECT COUNT(*) FROM budgets;    # Should be 0
mysql> SELECT COUNT(*) FROM categories; # Should be 0
mysql> SELECT COUNT(*) FROM expenses;   # Should be 0
mysql> exit;
```

**3. Configure browser and open DevTools:**
- Ensure X-Hass-User header is enabled (alice)
- Open Browser DevTools → Network tab
- Navigate to http://localhost:3001

**4. Begin test execution:**

Open `specs/008-comprehensive-functional-testing/test-results/integration.md` and start with TC-001.

---

## Test Execution Workflow

### Phase 3: User Story 7 - Backend Integration (P1 - CRITICAL)

**Must complete first before any other testing**

**File**: `test-results/integration.md`
**Test Cases**: TC-001 through TC-006
**Tasks**: T014-T025

1. Open `integration.md`
2. Review Prerequisites section
3. Execute each test case (TC-001 to TC-006):
   - Follow test steps exactly
   - Record actual outcomes
   - Mark status (PASS/FAIL)
   - If FAIL, create defect in `defects.md`
4. Calculate summary statistics
5. Fill suite completion notes
6. Mark tasks T014-T025 as complete in `tasks.md`

### Phase 4-11: Feature Testing (Can run in parallel after Phase 3)

Once backend integration is validated, these can be executed in any order:

**Priority P1 (Critical - Do First):**
- Phase 4: User Story 1 - End-to-End Lifecycle (T026-T037)
- Phase 5: User Story 2 - Multi-User (T038-T051)
- Phase 6: User Story 3 - Validation/Error Handling (T052-T066)

**Priority P2 (Important - Do After P1):**
- Phase 7: User Story 4 - UI/Navigation (T067-T084)
- Phase 8: User Story 5 - Date Handling (T085-T097)
- Phase 9: User Story 6 - Category Hierarchy (T098-T111)

**Additional Coverage:**
- Phase 10-11: Budget/Expense/Dashboard specific tests (T112-T134)

### Phase 12: Coverage Analysis (Final)

**Tasks**: T135-T142

Generate final test report, assess defects, document recommendations.

---

## Quick Reference

### Important Files

| File | Purpose |
|------|---------|
| `spec.md` | What to test and why (7 user stories, 43 FRs) |
| `plan.md` | Technical context and architecture |
| `tasks.md` | 150 tasks organized by phase/user story |
| `quickstart.md` | Detailed testing procedures |
| `test-results/README.md` | Tester guide and next steps |
| `test-results/integration.md` | Backend integration tests (START HERE) |
| `test-results/reset-database.sql` | Database reset script |
| `test-results/defects.md` | Defect tracking log |
| `test-results/coverage-summary.md` | Requirement coverage tracker |

### Key URLs

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080
- **Database**: localhost:3306 (MySQL 8.0)

### Test Data Users

- **alice**: Primary test user (X-Hass-User: alice)
- **bob**: Secondary test user for multi-user scenarios (X-Hass-User: bob)

### Success Criteria Quick Check

✅ All P1 tests (US1, US2, US3, US7) pass
✅ Zero CRITICAL/HIGH defects in OPEN status
✅ Dashboard loads < 3 seconds
✅ API responses < 2 seconds
✅ 100% of 43 FRs have test coverage
✅ All test results documented in Markdown
✅ Defects logged with severity and repro steps

---

## Troubleshooting

### Application Won't Start

```bash
# Check Docker is running
docker ps

# If error: "Cannot connect to Docker daemon"
# → Start Docker Desktop application

# Check logs
docker-compose logs mysql
docker-compose logs backend
docker-compose logs frontend

# Rebuild if needed
docker-compose down
docker-compose build
docker-compose up -d
```

### Database Connection Issues

```bash
# Check MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Try connecting with different host
mysql -u root -p -h 127.0.0.1
```

### Browser Extension Not Working

1. Check extension is enabled (icon should be colored, not gray)
2. Verify header configuration:
   - Header name: `X-Hass-User` (exact spelling)
   - Header value: `alice` (lowercase)
3. Refresh page after enabling extension
4. Check Network tab → Request Headers to confirm header is sent

### Tests Failing Unexpectedly

1. Reset database: `mysql -u root -p < test-results/reset-database.sql`
2. Clear browser cache and cookies
3. Restart application: `docker-compose restart`
4. Verify you're on correct branch: `git branch` (should show feature branch or main)
5. Check application logs for errors

---

## Need Help?

- **Feature Spec**: `spec.md` - Requirements and acceptance criteria
- **Technical Plan**: `plan.md` - Architecture and constraints
- **Testing Guide**: `quickstart.md` - Detailed procedures
- **Research Decisions**: `research.md` - Why we made certain choices
- **Tester README**: `test-results/README.md` - Quick start for testers

**Status Question?** Check `tasks.md` for current task completion status.

---

**Last Updated**: 2025-12-28
**Ready to Start**: ⏳ Pending application startup and manual verification (T003-T006)
