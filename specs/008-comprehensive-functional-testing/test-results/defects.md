# Defect Log

**Feature**: 008-comprehensive-functional-testing
**Created**: 2025-12-28
**Purpose**: Track all defects discovered during comprehensive functional testing

## Defect Summary

| Defect ID | Severity | Title | Status | Affected Features | Test Case(s) |
|-----------|----------|-------|--------|-------------------|--------------|
| DEF-001 | CRITICAL | App fails to compile due to missing MUI date picker modules | FIXED | Expenses, Navigation | PW-004, PW-001, PW-002, PW-003 |

**Defect Count by Severity**:
- CRITICAL: 1
- HIGH: 0
- MEDIUM: 0
- LOW: 0
- **Total**: 1

**Defect Count by Status**:
- OPEN: 0
- IN_PROGRESS: 0
- FIXED: 1
- VERIFIED: 0
- CLOSED: 0
- WONT_FIX: 0

## Defect Details

### DEF-001: App fails to compile due to missing MUI date picker modules

**Severity**: CRITICAL
**Status**: FIXED

**Affected Features**: Expenses, Navigation
**Functional Requirements**: FR-001, FR-003, FR-004, FR-024
**Test Cases**: PW-001, PW-002, PW-003, PW-004

**Description**:
Next.js shows a blocking "Failed to compile" overlay for `@mui/x-date-pickers` imports. The error prevents all routes from rendering, so the UI cannot be used for any workflow.

**Steps to Reproduce**:
1. **Given** the frontend is running in dev mode
2. **When** the browser loads `http://localhost:3001/`
3. **Then** a "Failed to compile" overlay appears and no page content renders

**Expected Behavior**:
Application compiles successfully and renders the requested page, including the expense form when navigating to `/expenses/new`.

**Actual Behavior**:
Next.js reports module resolution errors for `@mui/x-date-pickers/LocalizationProvider`, `DatePicker`, and `AdapterDateFns`, and shows a blocking error overlay.

**Browser/Platform**:
- Playwright Chromium on macOS (via MCP)

**Frequency**: Always

**Test Environment**:
- Frontend URL: http://localhost:3001
- Backend URL: http://localhost:8080
- Database: MySQL (dev)
- Test User: alice (X-Hass-User header)

**Evidence**:
- Browser Console Log: `Module not found: Can't resolve '@mui/x-date-pickers/LocalizationProvider'`

**Workaround**:
None. The overlay blocks all pages in dev mode.

**Discovered By**: Codex (Playwright MCP)
**Discovered Date**: 2025-12-28

**Notes**:
The error occurs even when loading the homepage, which suggests the build is failing globally during dev compile.

**Resolution**:
Root cause: Docker container for frontend was built with outdated node_modules before `@mui/x-date-pickers` and `date-fns` dependencies were added to package.json.

Fix applied:
1. Ran `npm install` in budget-frontend to update package-lock.json with new dependencies
2. Stopped Docker containers: `docker-compose down`
3. Rebuilt frontend container: `docker-compose build frontend`
4. Restarted all containers: `docker-compose up -d`
5. Verified application renders correctly on http://localhost:3001 with functional date pickers

**Fixed By**: Claude Sonnet 4.5
**Fixed Date**: 2025-12-28

**History**:
- 2025-12-28: Defect created (OPEN)
- 2025-12-28: Root cause identified - outdated Docker container node_modules (IN_PROGRESS)
- 2025-12-28: Fixed by rebuilding Docker container with updated dependencies (FIXED)

---

## Severity Definitions (Reference)

- **CRITICAL**: System crash, data loss, security vulnerability, complete feature failure
- **HIGH**: Major functionality broken, no workaround available, significantly impacts user
- **MEDIUM**: Functionality works with workaround, minor user impact, edge case failure
- **LOW**: Cosmetic issue, minor UI inconsistency, no functional impact

## Status Definitions (Reference)

- **OPEN**: Defect discovered, not yet assigned or being worked on
- **IN_PROGRESS**: Being investigated or fixed
- **FIXED**: Code changes made, awaiting verification
- **VERIFIED**: Fix confirmed by retesting
- **CLOSED**: Resolved and verified, no further action
- **WONT_FIX**: Acknowledged but will not be addressed

---

_Use the defect template from contracts/defect-template.md when creating new defect entries_
