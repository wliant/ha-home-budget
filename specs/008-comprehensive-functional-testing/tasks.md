# Tasks: Comprehensive Functional Testing

**Input**: Design documents from `/specs/008-comprehensive-functional-testing/`
**Prerequisites**: plan.md (completed), spec.md (completed), research.md (completed), data-model.md (completed), contracts/ (completed), quickstart.md (completed)

**Tests**: This is a TESTING feature - tasks involve manual test execution and documentation (no code implementation).

**Organization**: Tasks are grouped by user story to enable independent test execution of each testing area.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different test suites, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths for test result documentation

## Path Conventions

- **Test Documentation**: `specs/008-comprehensive-functional-testing/test-results/`
- **Test Templates**: `specs/008-comprehensive-functional-testing/contracts/`
- **Application URLs**: Frontend `http://localhost:3001`, Backend `http://localhost:8080`

## Phase 1: Setup (Test Environment Preparation)

**Purpose**: Prepare test environment, documentation templates, and tools

- [X] T001 Create test-results directory structure in specs/008-comprehensive-functional-testing/test-results/
- [X] T002 Create screenshots subdirectory in specs/008-comprehensive-functional-testing/test-results/screenshots/
- [ ] T003 [P] Verify application is running (frontend at :3001, backend at :8080)
- [ ] T004 [P] Verify MySQL database is accessible and contains budgets, categories, expenses tables
- [ ] T005 [P] Install browser extension for X-Hass-User header (ModHeader or Simple Modify Headers)
- [ ] T006 [P] Verify latest stable browsers available (Chrome, Firefox, Safari, Edge)
- [X] T007 Create database reset SQL script in specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [X] T008 Create test suite files from templates: category-management.md, budget-management.md, expense-recording.md, dashboard.md, integration.md, ui-navigation.md, date-handling.md in specs/008-comprehensive-functional-testing/test-results/
- [X] T009 Create defect log file specs/008-comprehensive-functional-testing/test-results/defects.md
- [X] T010 Create coverage summary file specs/008-comprehensive-functional-testing/test-results/coverage-summary.md

---

## Phase 2: Foundational (Integration Testing Prerequisites)

**Purpose**: Validate backend API contracts and authentication before feature testing

**⚠️ CRITICAL**: No feature testing can begin until backend integration is validated

- [ ] T011 Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T012 Configure browser with X-Hass-User header set to "alice"
- [ ] T013 Open browser DevTools Network tab for API monitoring

**Checkpoint**: Backend integration validated - feature test suites can now proceed in parallel

---

## Phase 3: User Story 7 - Backend Integration Testing (Priority: P1) 🎯 Critical Infrastructure

**Goal**: Validate REST API communication, X-Hass-User authentication, error handling, and data consistency between Next.js frontend and Spring Boot backend

**Independent Test**: Execute integration test suite, verify all API endpoints return expected responses with correct headers, confirm error handling works without crashes

### Test Execution for User Story 7

- [ ] T014 [US7] Open test suite file specs/008-comprehensive-functional-testing/test-results/integration.md
- [ ] T015 [US7] Fill Prerequisites section with environment details (URLs, database state, test user)
- [ ] T016 [US7] Create test case TC-001: Verify X-Hass-User header sent from frontend to backend for expense creation
- [ ] T017 [US7] Create test case TC-002: Verify backend reads X-Hass-User header and stores creator username
- [ ] T018 [US7] Create test case TC-003: Verify dashboard API endpoint returns JSON with budget/expense data
- [ ] T019 [US7] Create test case TC-004: Verify frontend displays backend validation errors without crashing
- [ ] T020 [US7] Create test case TC-005: Verify backend returns 500 error for database failures, frontend handles gracefully
- [ ] T021 [US7] Create test case TC-006: Verify frontend updates UI after successful backend response
- [ ] T022 [US7] Execute test cases TC-001 through TC-006, record actual outcomes and status in specs/008-comprehensive-functional-testing/test-results/integration.md
- [ ] T023 [US7] For any failed tests, create defect entries in specs/008-comprehensive-functional-testing/test-results/defects.md with severity and reproduction steps
- [ ] T024 [US7] Calculate summary statistics (total tests, pass/fail counts, pass rate) in specs/008-comprehensive-functional-testing/test-results/integration.md
- [ ] T025 [US7] Fill suite completion notes with duration, findings, blockers in specs/008-comprehensive-functional-testing/test-results/integration.md

**Checkpoint**: Backend integration working - can now test feature-specific functionality

---

## Phase 4: User Story 1 - End-to-End Budget Lifecycle Testing (Priority: P1) 🎯 MVP

**Goal**: Validate complete workflow from category creation → budget creation → expense recording → dashboard display, ensuring core budget tracking functionality works end-to-end

**Independent Test**: Execute end-to-end workflow test suite, create category hierarchy, create budgets, record expenses, verify dashboard shows correct totals and spending calculations

### Test Execution for User Story 1

- [ ] T026 [US1] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T027 [US1] Open test suite file specs/008-comprehensive-functional-testing/test-results/integration.md (reuse for E2E workflow tests)
- [ ] T028 [US1] Create test case TC-007: Create parent category "Food" and child categories "Groceries" and "Dining Out"
- [ ] T029 [US1] Create test case TC-008: Create budgets for January 2025 - Groceries ($300), Dining Out ($200), Food ($500) with parent-child validation
- [ ] T030 [US1] Create test case TC-009: Record expense of $50 for "Groceries" category, verify X-Hass-User attribution
- [ ] T031 [US1] Create test case TC-010: View homepage dashboard, verify current month budget summary and recent expenses display
- [ ] T032 [US1] Create test case TC-011: Record multiple expenses across categories, verify parent category totals correctly sum child spending
- [ ] T033 [US1] Execute test cases TC-007 through TC-011, record actual outcomes and status
- [ ] T034 [US1] Capture screenshots for dashboard views in specs/008-comprehensive-functional-testing/test-results/screenshots/
- [ ] T035 [US1] For any failed tests, create defect entries with CRITICAL or HIGH severity (blocks core functionality)
- [ ] T036 [US1] Calculate summary statistics and fill suite completion notes
- [ ] T037 [US1] Verify all 5 acceptance scenarios from spec.md are covered by test cases

**Checkpoint**: Core budget tracking workflow validated - application MVP is functional

---

## Phase 5: User Story 2 - Multi-User Household Testing (Priority: P1)

**Goal**: Validate multi-user scenarios with concurrent operations, shared data visibility, and correct user attribution via X-Hass-User header

**Independent Test**: Execute multi-user test suite using two different X-Hass-User values (alice and bob), verify shared data visibility and creator attribution work correctly

### Test Execution for User Story 2

- [ ] T038 [P] [US2] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T039 [US2] Create separate test documentation section in specs/008-comprehensive-functional-testing/test-results/integration.md for multi-user tests
- [ ] T040 [US2] Configure browser window 1 with X-Hass-User: alice
- [ ] T041 [P] [US2] Configure browser window 2 (or incognito) with X-Hass-User: bob
- [ ] T042 [US2] Create test case TC-012: User alice creates budget for "Groceries", verify user bob sees budget in budget list
- [ ] T043 [US2] Create test case TC-013: User alice records expense, verify user bob sees expense on homepage with alice as creator
- [ ] T044 [US2] Create test case TC-014: Both users create expenses concurrently, verify dashboard shows all household expenses aggregated
- [ ] T045 [US2] Create test case TC-015: User alice creates category, verify user bob can select category when creating budget
- [ ] T046 [US2] Create test case TC-016: Test missing X-Hass-User header, verify graceful error handling without security bypass
- [ ] T047 [US2] Create test case TC-017: Test concurrent budget creation for same category/month by alice and bob, verify one succeeds with duplicate error for other
- [ ] T048 [US2] Execute test cases TC-012 through TC-017, record outcomes
- [ ] T049 [US2] Verify no data corruption from concurrent operations (check database record counts)
- [ ] T050 [US2] For any failed tests, create defect entries with appropriate severity
- [ ] T051 [US2] Calculate summary statistics and fill suite completion notes

**Checkpoint**: Multi-user household functionality validated - concurrent user scenarios work correctly

---

## Phase 6: User Story 3 - Data Validation and Error Handling Testing (Priority: P1)

**Goal**: Validate all input validation rules, error handling for invalid data, edge cases, and system failure scenarios to ensure data integrity

**Independent Test**: Execute validation test suite, attempt various invalid operations (negative amounts, circular references, missing fields, backend failures), verify appropriate errors shown without crashes or data corruption

### Test Execution for User Story 3

- [ ] T052 [P] [US3] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T053 [US3] Open test suite file specs/008-comprehensive-functional-testing/test-results/integration.md (add validation section)
- [ ] T054 [US3] Create test case TC-018: Enter negative expense amount, verify validation error prevents submission
- [ ] T055 [US3] Create test case TC-019: Attempt circular category reference (Food child of Groceries), verify error "Circular reference not allowed"
- [ ] T056 [US3] Create test case TC-020: Attempt parent budget not matching sum of child budgets, verify error message
- [ ] T057 [US3] Create test case TC-021: Attempt duplicate budget for same category/month, verify error "Budget already exists"
- [ ] T058 [US3] Create test case TC-022: Stop backend service, attempt to load homepage, verify frontend shows error state without crash
- [ ] T059 [US3] Create test case TC-023: Attempt to remove category from existing budget, verify prevention with error message
- [ ] T060 [US3] Create test case TC-024: Enter extremely large amount ($999,999,999), verify formatting or maximum limit error
- [ ] T061 [US3] Create test case TC-025: Enter 500+ character description, verify truncation or character limit warning
- [ ] T062 [US3] Execute test cases TC-018 through TC-025, record outcomes
- [ ] T063 [US3] Test all 12 edge cases from spec.md Edge Cases section, document expected vs actual graceful degradation
- [ ] T064 [US3] Restart backend service after test TC-022 to restore normal state
- [ ] T065 [US3] For any failed tests (especially graceful degradation failures), create defect entries
- [ ] T066 [US3] Calculate summary statistics and fill suite completion notes

**Checkpoint**: Data validation and error handling verified - application handles invalid inputs and failures gracefully

---

## Phase 7: User Story 4 - UI Responsiveness and Navigation Testing (Priority: P2)

**Goal**: Validate responsive layouts across screen sizes (mobile, tablet, desktop), navigation flows, quick actions, and loading states

**Independent Test**: Execute UI/navigation test suite on multiple device sizes, verify layouts adapt correctly and all navigation links work without breaking application state

### Test Execution for User Story 4

- [ ] T067 [P] [US4] Open test suite file specs/008-comprehensive-functional-testing/test-results/ui-navigation.md
- [ ] T068 [P] [US4] Fill Prerequisites section (can reuse existing database data from previous tests)
- [ ] T069 [US4] Open Chrome DevTools, enable Device Toolbar (Cmd+Shift+M)
- [ ] T070 [US4] Create test case TC-026: Load homepage at 320px width (iPhone SE), verify vertical stacking and content accessibility
- [ ] T071 [US4] Create test case TC-027: Click "Record Expense" quick action, verify navigation to expense page
- [ ] T072 [US4] Create test case TC-028: Click "View Categories" from feature card, verify navigation to categories page
- [ ] T073 [US4] Create test case TC-029: Use browser back button from categories page, verify return to homepage without state break
- [ ] T074 [US4] Create test case TC-030: Click expense item in recent activity, verify detail view or navigation
- [ ] T075 [US4] Create test case TC-031: Simulate slow network (DevTools throttling), verify loading indicators display
- [ ] T076 [US4] Execute test cases TC-026 through TC-031 on Chrome (latest stable)
- [ ] T077 [P] [US4] Execute same test cases on Firefox (latest stable), note any browser-specific behaviors
- [ ] T078 [P] [US4] Execute same test cases on Safari (latest stable, macOS), note any browser-specific behaviors
- [ ] T079 [P] [US4] Test responsive breakpoints: 320px (mobile), 768px (tablet), 1024px (desktop)
- [ ] T080 [US4] Test on real iOS device (if available) or iOS Safari simulator
- [ ] T081 [P] [US4] Test on real Android device (if available) or Chrome Mobile emulator
- [ ] T082 [US4] For any failed tests or layout issues, create defect entries with screenshots
- [ ] T083 [US4] Calculate summary statistics and fill suite completion notes
- [ ] T084 [US4] Document browser version details in test execution section

**Checkpoint**: UI responsiveness and navigation validated across browsers and screen sizes

---

## Phase 8: User Story 5 - Date and Time Handling Testing (Priority: P2)

**Goal**: Validate date picker functionality, month-year budget selection, past/future date handling, and date-based filtering/calculations

**Independent Test**: Execute date handling test suite, create budgets for different months, record expenses with various dates, verify date-based filtering and period calculations work correctly

### Test Execution for User Story 5

- [ ] T085 [P] [US5] Open test suite file specs/008-comprehensive-functional-testing/test-results/date-handling.md
- [ ] T086 [P] [US5] Fill Prerequisites section (can reuse existing database data)
- [ ] T087 [US5] Create test case TC-032: Create budget for January 2025, verify month-year association
- [ ] T088 [US5] Create test case TC-033: Record expense with default date (today), verify correct date saved
- [ ] T089 [US5] Create test case TC-034: Record expense with past date (last week), verify chronological order in expense list
- [ ] T090 [US5] Create test case TC-035: Record expense with future date, verify saved as planned expense
- [ ] T091 [US5] Create test case TC-036: Create budgets for January and February, view homepage in January, verify January budget shown
- [ ] T092 [US5] Create test case TC-037: Record expense dated in January with both Jan and Feb budgets, verify counts only against January budget
- [ ] T093 [US5] Execute test cases TC-032 through TC-037, record outcomes
- [ ] T094 [US5] Test date picker UI on different browsers (Chrome, Firefox, Safari - Safari date input differs)
- [ ] T095 [US5] Verify timezone handling if system clock differs from data timestamps
- [ ] T096 [US5] For any failed tests or date calculation errors, create defect entries
- [ ] T097 [US5] Calculate summary statistics and fill suite completion notes

**Checkpoint**: Date and time handling validated - budget periods and expense dating work correctly

---

## Phase 9: User Story 6 - Category Hierarchy Testing (Priority: P2)

**Goal**: Validate hierarchical category management, parent-child constraints, circular reference prevention, deletion dependency handling, and budget roll-ups

**Independent Test**: Execute category hierarchy test suite, create multi-level categories, attempt invalid operations, verify hierarchy display and parent-child budget calculations work correctly

### Test Execution for User Story 6

- [ ] T098 [P] [US6] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T099 [P] [US6] Open test suite file specs/008-comprehensive-functional-testing/test-results/category-management.md
- [ ] T100 [US6] Fill Prerequisites section with clean database state requirement
- [ ] T101 [US6] Create test case TC-038: Create root category "Food" without parent, verify appears as top-level
- [ ] T102 [US6] Create test case TC-039: Create child category "Groceries" under "Food", verify nested display
- [ ] T103 [US6] Create test case TC-040: Attempt to make "Food" child of "Groceries", verify circular reference prevention
- [ ] T104 [US6] Create test case TC-041: Attempt to delete "Food" with child "Groceries", verify deletion prevented with message
- [ ] T105 [US6] Create test case TC-042: Create budgets for "Groceries" ($300) and "Dining Out" ($200) under "Food", verify parent total $500
- [ ] T106 [US6] Create test case TC-043: Attempt to create 3-level hierarchy (grandchild), verify 2-level depth limit enforced
- [ ] T107 [US6] Create test case TC-044: Test hierarchy with 10+ child categories, verify UI layout and performance
- [ ] T108 [US6] Execute test cases TC-038 through TC-044, record outcomes
- [ ] T109 [US6] Capture screenshots of hierarchy display in specs/008-comprehensive-functional-testing/test-results/screenshots/
- [ ] T110 [US6] For any failed tests (especially constraint violations), create defect entries
- [ ] T111 [US6] Calculate summary statistics and fill suite completion notes

**Checkpoint**: Category hierarchy functionality validated - constraints and roll-ups work correctly

---

## Phase 10: Budget and Expense Management Testing

**Goal**: Validate budget creation, expense recording, and budget tracking not covered by other user stories

**Independent Test**: Execute budget and expense test suites for scenarios not covered in E2E or other stories

### Budget Management Test Execution

- [ ] T112 [P] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T113 [P] Open test suite file specs/008-comprehensive-functional-testing/test-results/budget-management.md
- [ ] T114 Create test cases for FR-011 through FR-015 (budget creation, duplicate prevention, parent validation, category requirement, amount constraints)
- [ ] T115 Execute budget management test cases, record outcomes
- [ ] T116 For any failed tests, create defect entries
- [ ] T117 Calculate summary statistics and fill suite completion notes in specs/008-comprehensive-functional-testing/test-results/budget-management.md

### Expense Recording Test Execution

- [ ] T118 [P] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T119 [P] Open test suite file specs/008-comprehensive-functional-testing/test-results/expense-recording.md
- [ ] T120 Create test cases for FR-016 through FR-022 (expense creation, date defaults, creator attribution, amount/description constraints, budget tracking)
- [ ] T121 Execute expense recording test cases, record outcomes
- [ ] T122 For any failed tests, create defect entries
- [ ] T123 Calculate summary statistics and fill suite completion notes in specs/008-comprehensive-functional-testing/test-results/expense-recording.md

**Checkpoint**: All budget and expense functionality validated

---

## Phase 11: Dashboard Testing

**Goal**: Validate homepage dashboard displays current month budget summary, recent expenses, quick actions, and handles empty states correctly

**Independent Test**: Execute dashboard test suite with both empty and populated states, verify data accuracy and UI completeness

### Dashboard Test Execution

- [ ] T124 [P] Reset database to clean state using specs/008-comprehensive-functional-testing/test-results/reset-database.sql
- [ ] T125 [P] Open test suite file specs/008-comprehensive-functional-testing/test-results/dashboard.md
- [ ] T126 Create test case for empty state (no budgets or expenses), verify appropriate messaging
- [ ] T127 Populate test data (categories, budgets, expenses) for dashboard testing
- [ ] T128 Create test cases for FR-023 through FR-028 (budget summary display, recent expenses, quick actions, empty states, system status, progress indicators)
- [ ] T129 Execute dashboard test cases, record outcomes
- [ ] T130 Measure dashboard load time using browser Performance tab, verify < 3 seconds (SC-006)
- [ ] T131 Measure API response times in Network tab, verify < 2 seconds (SC-003)
- [ ] T132 Capture screenshots of dashboard states in specs/008-comprehensive-functional-testing/test-results/screenshots/
- [ ] T133 For any failed tests or performance issues, create defect entries
- [ ] T134 Calculate summary statistics and fill suite completion notes in specs/008-comprehensive-functional-testing/test-results/dashboard.md

**Checkpoint**: Dashboard functionality validated with performance metrics

---

## Phase 12: Coverage Analysis and Reporting

**Purpose**: Verify all functional requirements tested, generate coverage summary, assess defect impact

- [ ] T135 Review all test suite files to ensure all 43 functional requirements have corresponding test cases
- [ ] T136 Update coverage summary in specs/008-comprehensive-functional-testing/test-results/coverage-summary.md with FR ID, test case IDs, pass/fail counts, coverage status
- [ ] T137 Calculate overall test statistics (total tests executed, pass rate, P1 pass rate, P2 pass rate)
- [ ] T138 Review defects.md to categorize defects by severity (CRITICAL, HIGH, MEDIUM, LOW)
- [ ] T139 Identify any CRITICAL or HIGH severity defects in OPEN status that block release
- [ ] T140 Verify all test suites have completion notes filled
- [ ] T141 Create overall test summary report in specs/008-comprehensive-functional-testing/test-results/test-summary.md
- [ ] T142 Document recommendations for next steps (fix defects, retest, release decision)

**Checkpoint**: Testing complete, coverage verified, recommendations documented

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation improvements

- [ ] T143 [P] Review all test result Markdown files for formatting consistency
- [ ] T144 [P] Verify all screenshots are properly named and referenced
- [ ] T145 [P] Run spell check on all test documentation
- [ ] T146 [P] Verify all defect entries have reproduction steps in Given/When/Then format
- [ ] T147 Update spec.md Success Criteria section with actual test results (SC-001 through SC-014)
- [ ] T148 Archive test results with timestamp for future regression testing
- [ ] T149 Create quickstart validation checklist based on actual test execution experience
- [ ] T150 Document any deviations from planned test approach in plan.md notes section

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user story testing
- **User Story 7 (Phase 3)**: Depends on Foundational (backend integration must work first)
- **User Stories 1-6 (Phases 4-9)**: All depend on Foundational completion
  - Can proceed in parallel if multiple testers available
  - Or sequentially in priority order (P1 → P2)
- **Budget/Expense/Dashboard (Phases 10-11)**: Can run in parallel with User Stories 4-6
- **Coverage Analysis (Phase 12)**: Depends on all test suites being executed
- **Polish (Phase 13)**: Depends on Coverage Analysis completion

### User Story Dependencies

- **User Story 7 (Backend Integration) - P1**: MUST complete first - validates API layer works
- **User Story 1 (End-to-End Lifecycle) - P1**: Can start after US7 - validates core workflow
- **User Story 2 (Multi-User) - P1**: Can start after US7 - independent of US1
- **User Story 3 (Validation/Error Handling) - P1**: Can start after US7 - independent of US1/US2
- **User Story 4 (UI/Navigation) - P2**: Can run in parallel with other stories
- **User Story 5 (Date Handling) - P2**: Can run in parallel with other stories
- **User Story 6 (Category Hierarchy) - P2**: Can run in parallel with other stories

### Within Each User Story

- Setup test suite file before creating test cases
- Create all test case entries with expected outcomes before execution
- Execute test cases in order
- Document failures immediately (create defect entries)
- Calculate summary statistics after all tests executed
- Fill completion notes before marking story complete

### Parallel Opportunities

- **Setup Phase**: Tasks T003, T004, T005, T006 can run in parallel (different verification activities)
- **User Stories**: All P2 user stories (US4, US5, US6) can run in parallel after P1 stories complete
- **Budget/Expense/Dashboard**: Can run in parallel with P2 user stories
- **Test Suite Files**: Independent test suites (T067/T068, T085/T086, T098/T099) can be prepared in parallel
- **Browser Testing**: Tasks T077, T078 (Firefox, Safari) can run in parallel with Chrome testing
- **Polish Phase**: Tasks T143, T144, T145, T146 can run in parallel (different documentation reviews)

---

## Parallel Example: User Story 1 (End-to-End Testing)

```bash
# These tasks can run in parallel (different test preparation):
Task T026: "Reset database to clean state"
Task T027: "Open test suite file"

# These test case creation tasks can run in parallel (different scenarios):
Task T028: "Create test case TC-007: Category creation"
Task T029: "Create test case TC-008: Budget creation"
Task T030: "Create test case TC-009: Expense recording"
Task T031: "Create test case TC-010: Dashboard verification"
Task T032: "Create test case TC-011: Multi-category spending"

# These must run sequentially (test execution depends on data from previous tests):
Task T033: "Execute test cases TC-007 through TC-011" (sequential)
```

---

## Parallel Example: Browser Testing (User Story 4)

```bash
# Once test cases are defined, browser testing can run in parallel:
Task T076: "Execute on Chrome (latest stable)"
Task T077: "Execute on Firefox (latest stable)"
Task T078: "Execute on Safari (latest stable)"
Task T080: "Test on iOS Safari"
Task T081: "Test on Chrome Mobile"

# All browser tests validate same scenarios on different platforms
```

---

## Implementation Strategy

### MVP First (P1 User Stories Only)

1. Complete Phase 1: Setup (prepare test environment)
2. Complete Phase 2: Foundational (validate backend integration)
3. Complete Phase 3: User Story 7 (backend integration tests)
4. Complete Phase 4: User Story 1 (end-to-end lifecycle tests)
5. Complete Phase 5: User Story 2 (multi-user tests)
6. Complete Phase 6: User Story 3 (validation/error handling tests)
7. **STOP and VALIDATE**: Review P1 test results, assess critical defects
8. Decision point: Fix critical defects before P2 testing, or proceed with P2 in parallel

### Incremental Testing Delivery

1. Complete Setup + Foundational → Backend validated
2. Execute User Story 7 → API integration validated (MVP infrastructure)
3. Execute User Story 1 → Core workflow validated (MVP feature)
4. Execute User Stories 2 & 3 → Critical quality validated (MVP release-ready)
5. Execute User Stories 4, 5, 6 → Enhanced testing coverage (P2 features)
6. Execute Budget/Expense/Dashboard → Comprehensive coverage
7. Generate Coverage Analysis → Test completion verified

### Parallel Tester Strategy

With multiple testers available:

1. All testers complete Setup together
2. One tester validates Foundational (backend integration)
3. Once Foundational complete:
   - Tester A: User Story 1 (End-to-End)
   - Tester B: User Story 2 (Multi-User)
   - Tester C: User Story 3 (Validation)
4. After P1 complete:
   - Tester A: User Story 4 (UI/Navigation)
   - Tester B: User Story 5 (Date Handling)
   - Tester C: User Story 6 (Category Hierarchy)
5. All testers collaborate on Coverage Analysis

---

## Notes

- [P] tasks = different test suites or browsers, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently testable (can execute US2 without executing US1)
- Database resets ensure test isolation within each suite
- Capture screenshots for failures immediately
- Document defects while details are fresh
- Mark test cases PASS/FAIL immediately after execution
- Stop testing if CRITICAL defects found (fix before continuing)
- Avoid: skipping database resets, not documenting failures, testing without isolation

## Task Summary

- **Total Tasks**: 150
- **Setup Phase**: 10 tasks (T001-T010)
- **Foundational Phase**: 3 tasks (T011-T013)
- **User Story 7 (Backend Integration - P1)**: 12 tasks (T014-T025)
- **User Story 1 (End-to-End - P1)**: 12 tasks (T026-T037)
- **User Story 2 (Multi-User - P1)**: 14 tasks (T038-T051)
- **User Story 3 (Validation - P1)**: 15 tasks (T052-T066)
- **User Story 4 (UI/Navigation - P2)**: 18 tasks (T067-T084)
- **User Story 5 (Date Handling - P2)**: 13 tasks (T085-T097)
- **User Story 6 (Category Hierarchy - P2)**: 14 tasks (T098-T111)
- **Budget Management**: 6 tasks (T112-T117)
- **Expense Recording**: 6 tasks (T118-T123)
- **Dashboard Testing**: 11 tasks (T124-T134)
- **Coverage Analysis**: 8 tasks (T135-T142)
- **Polish & Cross-Cutting**: 8 tasks (T143-T150)

**Parallel Opportunities**: 25+ tasks marked [P] can run concurrently
**Independent Stories**: All 7 user stories can be tested independently after Foundational phase
**MVP Scope**: Phases 1-6 (Setup + Foundational + P1 User Stories = 56 tasks for critical testing coverage)
