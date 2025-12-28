---

description: "Task list for Feature 007: Expense Recording"
---

# Tasks: Expense Recording

**Input**: Design documents from `/specs/007-expense-recording/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Tests are OPTIONAL for this feature. No tests are included in this task list per the specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Frontend**: `budget-frontend/src/`
- **Backend**: No changes required (using existing APIs from Feature 002)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Install dependencies and verify existing infrastructure

- [X] T001 Install Material-UI date picker dependencies (@mui/x-date-pickers, date-fns) in budget-frontend/package.json
- [X] T002 [P] Verify expenseService.createExpense() exists in budget-frontend/src/services/expenseService.ts
- [X] T003 [P] Verify categoryService.getAllCategories() exists in budget-frontend/src/services/categoryService.ts
- [X] T004 [P] Verify budgetService.getAllBudgets() exists in budget-frontend/src/services/budgetService.ts

**Checkpoint**: Dependencies installed, all required services verified

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create reusable components that multiple user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create CategorySelect component in budget-frontend/src/components/expenses/CategorySelect.tsx (displays categories with parent hierarchy, integrates with categoryService)
- [X] T006 Create expense types file in budget-frontend/src/types/expense.ts (ExpenseFormState interface for form state management)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Quick Expense Entry (Priority: P1) 🎯 MVP

**Goal**: Enable household members to quickly record daily expenses with amount, description, category selection, and automatic date defaulting to today

**Independent Test**: User can navigate to /expenses/new, see form with date defaulted to today, enter amount and description, select a category, submit the form, and see success message with navigation to homepage

### Implementation for User Story 1

- [X] T007 [US1] Create expense form page directory budget-frontend/src/app/expenses/new/
- [X] T008 [US1] Implement basic expense form page in budget-frontend/src/app/expenses/new/page.tsx (form layout with Container, Paper, TextField for amount/description, CategorySelect, date field defaulted to today, submit button)
- [X] T009 [US1] Add form state management in budget-frontend/src/app/expenses/new/page.tsx (useState for amount, description, expenseDate with getTodayISO() default, categoryId, budgetId, loading, error, success states)
- [X] T010 [US1] Implement budget auto-selection logic in budget-frontend/src/app/expenses/new/page.tsx (useEffect that fetches budgets and filters by expenseDate, sets budgetId if exactly one match, shows error if zero or multiple matches)
- [X] T011 [US1] Add client-side form validation in budget-frontend/src/app/expenses/new/page.tsx (validateForm function: amount > 0, description 1-500 chars, category required, date required, budgetId required)
- [X] T012 [US1] Implement form submission handler in budget-frontend/src/app/expenses/new/page.tsx (handleSubmit calls expenseService.createExpense, handles success with Snackbar and navigation to /, handles errors with Snackbar)
- [X] T013 [US1] Add error handling UI in budget-frontend/src/app/expenses/new/page.tsx (inline errors for validation, Snackbar for network/backend errors, Alert for no budget found)
- [X] T014 [US1] Add success feedback UI in budget-frontend/src/app/expenses/new/page.tsx (Snackbar with "Expense created successfully!" message, automatic navigation to homepage after 2 seconds)

**Checkpoint**: At this point, User Story 1 should be fully functional - users can create expenses with amount, description, category, and today's date

---

## Phase 4: User Story 2 - Category-Based Expense Tracking (Priority: P1)

**Goal**: Enable users to categorize expenses to understand spending patterns and stay within category-specific budgets

**Independent Test**: User can select from existing categories when creating an expense. The expense is associated with the chosen category and counted against that category's budget allocation (verify by checking budget details page)

### Implementation for User Story 2

- [X] T015 [US2] Enhance CategorySelect to display category icons in budget-frontend/src/components/expenses/CategorySelect.tsx (add icon rendering in Autocomplete options using Material Icons)
- [X] T016 [US2] Add category hierarchy display in CategorySelect in budget-frontend/src/components/expenses/CategorySelect.tsx (getCategoryLabel function formats as "Parent > Child" for child categories)
- [X] T017 [US2] Add category search/filter capability in budget-frontend/src/components/expenses/CategorySelect.tsx (leverage built-in Autocomplete filtering, add placeholder "Search categories...")
- [X] T018 [US2] Update form submission to include categoryId in budget-frontend/src/app/expenses/new/page.tsx (ensure CreateExpenseRequest includes selected categoryId)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - users can select categories with hierarchy display and icons, expenses are associated with categories

---

## Phase 5: User Story 3 - Expense Date Flexibility (Priority: P2)

**Goal**: Enable users to record past expenses or future planned expenses with accurate dates by editing the date field

**Independent Test**: User can change the date field to any past or future date, save the expense, and verify it appears with the correct date in expense history (and is counted against the correct month's budget)

### Implementation for User Story 3

- [X] T019 [US3] Add Material-UI DatePicker component to form in budget-frontend/src/app/expenses/new/page.tsx (replace basic TextField with LocalizationProvider + DatePicker using AdapterDateFns)
- [X] T020 [US3] Implement date change handler in budget-frontend/src/app/expenses/new/page.tsx (handleDateChange updates expenseDate state, triggers budget auto-selection via useEffect)
- [X] T021 [US3] Add date validation in budget-frontend/src/app/expenses/new/page.tsx (validate date is valid ISO format, update validateForm function)
- [X] T022 [US3] Update budget auto-selection to react to date changes in budget-frontend/src/app/expenses/new/page.tsx (ensure useEffect dependency includes expenseDate, re-run budget lookup when date changes)
- [X] T023 [US3] Add user feedback for date-based budget selection in budget-frontend/src/app/expenses/new/page.tsx (show which budget was auto-selected, or error message if no budget found for selected date)

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work - users can select any date, see which budget applies, and create expenses with past/future dates

---

## Phase 6: User Story 4 - Multi-User Attribution (Priority: P2)

**Goal**: Enable household members to see who created each expense for accountability and tracking individual spending patterns

**Independent Test**: Two different users (via different X-Hass-User headers) create expenses. Each expense displays the creator's name in the expense list and activity feed

### Implementation for User Story 4

- [X] T024 [US4] Add user context display in form header in budget-frontend/src/app/expenses/new/page.tsx (show "Recording expense as: [username]" using X-Hass-User from request context or dev mode default)
- [X] T025 [US4] Add creator attribution to success message in budget-frontend/src/app/expenses/new/page.tsx (success Snackbar shows "Expense created for [username]!")
- [X] T026 [US4] Update homepage RecentActivityCard to display creator names in budget-frontend/src/components/home/RecentActivityCard.tsx (show createdBy field for each expense in recent activity)

**Checkpoint**: All user stories should now be independently functional - expenses show creator attribution in UI

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T027 [P] Add loading states to CategorySelect in budget-frontend/src/components/expenses/CategorySelect.tsx (show CircularProgress while categories are loading, disable dropdown until loaded)
- [X] T028 [P] Add responsive design for mobile in budget-frontend/src/app/expenses/new/page.tsx (ensure form works on small screens, test with viewport meta tag)
- [X] T029 [P] Add input formatting for amount field in budget-frontend/src/app/expenses/new/page.tsx (format as currency on blur, allow only numbers and decimal point)
- [X] T030 [P] Add character counter for description field in budget-frontend/src/app/expenses/new/page.tsx (show "X/500 characters" helper text)
- [X] T031 Add keyboard shortcuts in budget-frontend/src/app/expenses/new/page.tsx (Enter to submit form, Escape to cancel/clear)
- [X] T032 Add form reset after successful submission in budget-frontend/src/app/expenses/new/page.tsx (clear all fields, reset to defaults after navigation delay)
- [X] T033 [P] Update homepage navigation card for expenses in budget-frontend/src/app/page.tsx (verify "Expenses" FeatureNavigationCard points to /expenses/new and has correct icon/description)
- [X] T034 Add accessibility attributes in budget-frontend/src/app/expenses/new/page.tsx (ARIA labels, required field indicators, error announcements for screen readers)
- [X] T035 Verify quickstart.md integration scenarios in specs/007-expense-recording/quickstart.md (manually test Scenarios 1-6: happy path, past date, no budget, category hierarchy, multi-user, validation error)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User Story 1 (P1): Can start after Foundational - No dependencies on other stories
  - User Story 2 (P1): Can start after Foundational - Enhances US1 but independently testable
  - User Story 3 (P2): Can start after Foundational - Enhances US1 but independently testable
  - User Story 4 (P2): Can start after Foundational - Adds attribution to US1 but independently testable
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories ✅ INDEPENDENT
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Enhances category selection from US1 but can be tested independently ✅ INDEPENDENT
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Enhances date handling from US1 but can be tested independently ✅ INDEPENDENT
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) - Adds user attribution to US1 but can be tested independently ✅ INDEPENDENT

**Key Insight**: All user stories are independently implementable and testable after Foundational phase. They can be worked on in parallel by different developers or sequentially in priority order.

### Within Each User Story

- Core implementation before enhancements
- Validation before submission logic
- Error handling after happy path
- UI feedback after core functionality
- Story complete before moving to next priority

### Parallel Opportunities

- **Setup (Phase 1)**: T002, T003, T004 can run in parallel (verifying different services)
- **Foundational (Phase 2)**: T005 and T006 can run in parallel (different components)
- **Once Foundational completes**: All user stories (Phase 3, 4, 5, 6) can start in parallel if team capacity allows
- **Polish (Phase 7)**: T027, T028, T029, T030, T033, T034 can run in parallel (different files/concerns)

---

## Parallel Example: User Story 1

```bash
# After Foundational phase completes, launch all User Story 1 tasks:
# (Note: T007-T008 must complete before T009-T014, but within those groups parallel work is possible)

# First wave (setup):
Task T007: "Create expense form page directory"
Task T008: "Implement basic expense form page"

# Second wave (once page.tsx exists):
Task T009: "Add form state management" (same file as T008, sequential)
Task T010: "Implement budget auto-selection logic" (same file, sequential after T009)
Task T011: "Add client-side form validation" (same file, sequential after T010)
Task T012: "Implement form submission handler" (same file, sequential after T011)
Task T013: "Add error handling UI" (same file, sequential after T012)
Task T014: "Add success feedback UI" (same file, sequential after T013)

# Note: User Story 1 tasks are mostly sequential due to same-file edits
# But User Story 2, 3, 4 can be worked on in parallel by other developers
```

---

## Parallel Example: Multiple User Stories

```bash
# After Foundational phase completes, different developers can work on different stories:

# Developer A: User Story 1 (Core expense entry)
Tasks T007-T014

# Developer B: User Story 2 (Category enhancements) - can start immediately after Foundational
Tasks T015-T018

# Developer C: User Story 3 (Date flexibility) - can start immediately after Foundational
Tasks T019-T023

# Developer D: User Story 4 (User attribution) - can start immediately after Foundational
Tasks T024-T026

# All stories integrate seamlessly because they're independently designed
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only - Both P1)

1. Complete Phase 1: Setup (install dependencies, verify services)
2. Complete Phase 2: Foundational (CategorySelect, expense types) - CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (core expense entry with basic fields)
4. Complete Phase 4: User Story 2 (category selection with hierarchy and icons)
5. **STOP and VALIDATE**: Test expense creation with categories independently
6. Deploy/demo if ready

**Why stop here**: User Stories 1 + 2 (both P1) provide full MVP functionality - users can create expenses with all required fields and category tracking. User Stories 3 + 4 (both P2) are enhancements.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → MVP baseline (basic expense entry)
3. Add User Story 2 → Test independently → MVP complete (category tracking) 🚀 DEPLOY
4. Add User Story 3 → Test independently → Enhancement (date flexibility) 🚀 DEPLOY
5. Add User Story 4 → Test independently → Enhancement (user attribution) 🚀 DEPLOY
6. Add Polish (Phase 7) → Test end-to-end → Production-ready 🚀 DEPLOY

Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (Phases 1-2)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T007-T014) - 8 tasks
   - **Developer B**: User Story 2 (T015-T018) - 4 tasks
   - **Developer C**: User Story 3 (T019-T023) - 5 tasks
   - **Developer D**: User Story 4 (T024-T026) - 3 tasks
3. Stories complete and integrate independently
4. Team converges on Polish phase (T027-T035)

**Note**: In practice, User Story 2 enhances User Story 1 (CategorySelect), so coordination between A and B is recommended, but they can still work in parallel on their respective tasks.

---

## Notes

- [P] tasks = different files, no dependencies (can run in parallel)
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Most tasks in same file (page.tsx) are sequential, but different stories are parallel
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- No backend changes required - all APIs exist from Feature 002
- All date handling uses ISO format (YYYY-MM-DD) per existing backend contract
- Budget auto-selection is transparent to user - happens automatically based on date
- Category hierarchy display follows pattern: "Parent > Child"
- Multi-user attribution uses X-Hass-User header (Feature 003)

---

## Task Summary

**Total Tasks**: 35

**Tasks by Phase**:
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 2 tasks
- Phase 3 (User Story 1 - P1): 8 tasks 🎯 MVP Baseline
- Phase 4 (User Story 2 - P1): 4 tasks 🎯 MVP Complete
- Phase 5 (User Story 3 - P2): 5 tasks
- Phase 6 (User Story 4 - P2): 3 tasks
- Phase 7 (Polish): 9 tasks

**Parallel Opportunities**:
- 3 tasks in Phase 1 (verifying services)
- 2 tasks in Phase 2 (different components)
- 4 user stories can work in parallel after Foundational
- 6 tasks in Phase 7 (different concerns)

**MVP Scope**: User Stories 1 + 2 (12 implementation tasks after setup/foundational) - Both are P1 priority and deliver core value

**Independent Test Criteria**:
- ✅ User Story 1: Can create expense with amount, description, category, today's date
- ✅ User Story 2: Can select categories with hierarchy, expenses associated with categories
- ✅ User Story 3: Can change date to past/future, expense counted against correct budget
- ✅ User Story 4: Expenses show creator username, multi-user attribution works

**Format Validation**: ✅ All tasks follow checklist format: `- [ ] [ID] [P?] [Story?] Description with file path`
