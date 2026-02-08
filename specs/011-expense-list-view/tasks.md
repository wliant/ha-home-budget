# Tasks: Expense List View

**Input**: Design documents from `/specs/011-expense-list-view/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not requested in feature specification. Test tasks are excluded.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `budget-backend/src/main/java/com/homebudget/`
- **Frontend**: `budget-frontend/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create shared DTOs and response types used across multiple user stories

- [X] T001 [P] Create ExpenseListResponse DTO with fields (content, totalElements, totalPages, currentPage, pageSize, totalAmount, sortBy, sortDirection) in budget-backend/src/main/java/com/homebudget/dto/ExpenseListResponse.java
- [X] T002 [P] Add paginated filter query to ExpenseRepository: new `findByFiltersPageable` method accepting Pageable, minAmount, maxAmount parameters and returning Page<Expense>. Also add aggregate summary query `getFilteredSummary` returning [count, sum]. Also add `findDistinctYears` and `findDistinctCreators` queries in budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend service and controller methods that all frontend stories depend on

**CRITICAL**: No frontend work can begin until this phase is complete

- [X] T003 Add `getExpenseList` method to ExpenseService that accepts year (required), month, categoryId, minAmount, maxAmount, createdBy, Pageable params. Convert year/month to startDate/endDate range, call new paginated repository method, compute totalAmount from aggregate query, return ExpenseListResponse in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [X] T004 Add `getDistinctYears` method to ExpenseService that returns List<Integer> of distinct expense years sorted descending in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [X] T005 Add `getDistinctCreators` method to ExpenseService that returns List<String> of distinct createdBy values sorted ascending in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [X] T006 Add three new endpoints to ExpenseController: (1) GET /api/expenses/list with year, month, categoryId, minAmount, maxAmount, createdBy, page, size, sortBy, sortDirection params returning ExpenseListResponse; (2) GET /api/expenses/years returning List<Integer>; (3) GET /api/expenses/creators returning List<String>. Include input validation (minAmount <= maxAmount, month 1-12, sortBy whitelist). All endpoints use X-Hass-User header in budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java
- [X] T007 [P] Add frontend types and API methods to expenseService.ts: (1) ExpenseListResponse interface matching backend DTO; (2) ExpenseListFilters interface with year, month, categoryId, minAmount, maxAmount, createdBy, page, size, sortBy, sortDirection; (3) getExpenseList(filters) method calling GET /api/expenses/list; (4) getExpenseYears() method calling GET /api/expenses/years; (5) getExpenseCreators() method calling GET /api/expenses/creators in budget-frontend/src/services/expenseService.ts

**Checkpoint**: Backend API fully functional. Can be tested with curl commands from quickstart.md.

---

## Phase 3: User Story 1 - View Expenses for Current Year (Priority: P1) MVP

**Goal**: Display a paginated table of expenses for the current year with columns: date, description, category (with icon), amount, created by. Shows summary (count + total).

**Independent Test**: Navigate to /expenses and verify the table loads with current year expenses, correct columns, pagination, summary bar, and empty state.

### Implementation for User Story 1

- [X] T008 [US1] Create ExpenseListTable component with Material-UI Table displaying columns: Date (formatted human-readable), Description, Category (name + icon, "Uncategorized" for null), Amount (currency formatted), Created By. Include TablePagination with 50 items/page default. Include summary bar showing total count and total amount (e.g., "156 expenses totaling $4,523.75"). Include loading state (skeleton/spinner) and empty state ("No expenses found for the selected year"). Accept props: data (ExpenseListResponse), loading, onPageChange, page, and default sort (date descending) in budget-frontend/src/components/expenses/ExpenseListTable.tsx
- [X] T009 [US1] Create ExpenseListPage at /expenses route. On mount, fetch expenses for current year using getExpenseList({year: currentYear, page: 0, size: 50, sortBy: 'expenseDate', sortDirection: 'DESC'}). Manage state for page, loading, and data. Render ExpenseListTable with fetched data. Handle page changes by re-fetching with updated page number in budget-frontend/src/app/expenses/page.tsx
- [X] T010 [US1] Add "Expenses" nav item to navigation config (between "Categories" and "Record Expense") using ListAlt icon and href="/expenses". Add breadcrumb definition for /expenses route (Home > Expenses) in budget-frontend/src/components/navigation/navConfig.tsx

**Checkpoint**: Expense list page accessible from nav, showing current year expenses in a paginated table with summary. MVP complete.

---

## Phase 4: User Story 2 - Filter Expenses by Year (Priority: P1)

**Goal**: Add a mandatory year filter dropdown that defaults to current year and allows switching between years.

**Independent Test**: Change year dropdown to a different year and verify table updates to show only that year's expenses.

### Implementation for User Story 2

- [X] T011 [US2] Create ExpenseFilters component with year Select dropdown (mandatory, defaults to current year). On mount, call getExpenseYears() to populate dropdown options; always include current year even if not returned from API. Render year filter prominently. Accept props: filters state object, onFilterChange callback. The component will be extended in US3 with additional filters in budget-frontend/src/components/expenses/ExpenseFilters.tsx
- [X] T012 [US2] Integrate ExpenseFilters into ExpenseListPage. Add filters state (starting with {year: currentYear}). Pass filters to ExpenseFilters component. On year filter change, reset page to 0 and re-fetch expenses with new year. Auto-apply: call API immediately when year changes (no Apply button) in budget-frontend/src/app/expenses/page.tsx

**Checkpoint**: Year filter works, changing year updates table automatically. US1 + US2 complete.

---

## Phase 5: User Story 3 - Filter Expenses by Additional Criteria (Priority: P2)

**Goal**: Add optional filters (month, category, amount range, created by) with AND logic and auto-apply behavior. Includes clear-all functionality.

**Independent Test**: Apply various filter combinations and verify only matching expenses appear. Clear filters resets to current year defaults.

### Implementation for User Story 3

- [X] T013 [US3] Extend ExpenseFilters component with: (1) Month Select dropdown (optional, options January-December mapped to 1-12); (2) Category Select dropdown (optional, fetch categories from existing categoryService.getAllCategories(), show name + icon); (3) Amount range inputs: two TextField (type="number", min amount and max amount, validate min <= max with error message); (4) Created By Select dropdown (optional, populated from getExpenseCreators()); (5) "Clear Filters" button that resets year to current year and clears all optional filters. All filter changes trigger onFilterChange callback immediately in budget-frontend/src/components/expenses/ExpenseFilters.tsx
- [X] T014 [US3] Update ExpenseListPage to handle all filter parameters. Extend filters state with month, categoryId, minAmount, maxAmount, createdBy. On any filter change, reset page to 0 and re-fetch with all active filters. Clear filters handler resets all state to defaults in budget-frontend/src/app/expenses/page.tsx

**Checkpoint**: All filters work with AND logic, auto-apply, clear functionality. US1 + US2 + US3 complete.

---

## Phase 6: User Story 4 - Sort Expense List (Priority: P2)

**Goal**: Enable column-header sorting with visual indicators and server-side sort.

**Independent Test**: Click column headers to sort, verify visual indicator appears and data reorders correctly via server-side API calls.

### Implementation for User Story 4

- [X] T015 [US4] Enhance ExpenseListTable with sortable column headers using Material-UI TableSortLabel on all five columns (Date→expenseDate, Description→description, Category→categoryName, Amount→amount, Created By→createdBy). Track current sortBy and sortDirection in state. Default: expenseDate DESC. On column click: if same column toggle direction, if different column set ASC. Visual arrow indicator on active sort column. Accept new props: sortBy, sortDirection, onSortChange callback in budget-frontend/src/components/expenses/ExpenseListTable.tsx
- [X] T016 [US4] Update ExpenseListPage to manage sort state (sortBy, sortDirection). Pass sort props to ExpenseListTable. On sort change, reset page to 0, re-fetch with new sort params. Include sort params in every API call in budget-frontend/src/app/expenses/page.tsx

**Checkpoint**: All 4 user stories complete. Full filtering, sorting, pagination working end-to-end.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Responsive design, edge cases, and final integration quality

- [X] T017 [P] Add responsive styling to ExpenseListTable for screens as small as 375px wide: use MUI Table sx props for horizontal scroll on small screens, adjust typography sizes, ensure filter bar stacks vertically on mobile in budget-frontend/src/components/expenses/ExpenseListTable.tsx
- [X] T018 [P] Add responsive styling to ExpenseFilters: stack filters vertically on mobile (use MUI Grid or flexbox wrap), ensure dropdowns and inputs are full-width on small screens in budget-frontend/src/components/expenses/ExpenseFilters.tsx
- [X] T019 Validate end-to-end integration by running quickstart.md scenarios: verify backend API responds correctly to curl commands, verify frontend page loads and filters/sorts/paginates correctly

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001, T002). T003-T006 depend on T001+T002. T007 is parallel (frontend, no backend dependency)
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion (T003-T007)
- **User Story 2 (Phase 4)**: Depends on Phase 3 (US1 provides the table to filter)
- **User Story 3 (Phase 5)**: Depends on Phase 4 (US2 provides the filter component to extend)
- **User Story 4 (Phase 6)**: Depends on Phase 3 (US1 provides the table to enhance). Can run in parallel with US2/US3
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Backend Phase 2 must be complete. Core MVP.
- **User Story 2 (P1)**: Depends on US1 (extends the page with year filter)
- **User Story 3 (P2)**: Depends on US2 (extends the filter component)
- **User Story 4 (P2)**: Depends on US1 only (enhances the table). Can run parallel with US2/US3 if managed carefully

### Within Each User Story

- Backend (repository → service → controller) is sequential
- Frontend components can be built in parallel if they don't share the same file
- Page integration tasks depend on component tasks

### Parallel Opportunities

- T001 and T002 can run in parallel (different files)
- T007 can run in parallel with T003-T006 (frontend vs backend)
- T017 and T018 can run in parallel (different component files)
- US4 (T015-T016) can potentially run in parallel with US2/US3 if on separate branches

---

## Parallel Example: Phase 1

```bash
# Launch both setup tasks together:
Task: "Create ExpenseListResponse DTO in budget-backend/.../dto/ExpenseListResponse.java"
Task: "Add paginated queries to ExpenseRepository in budget-backend/.../repository/ExpenseRepository.java"
```

## Parallel Example: Phase 2

```bash
# Launch frontend service update alongside backend service work:
Task: "Add frontend types and API methods in budget-frontend/src/services/expenseService.ts"
# (runs parallel with T003-T006 backend tasks)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T007)
3. Complete Phase 3: User Story 1 (T008-T010)
4. **STOP and VALIDATE**: Navigate to /expenses, verify table loads with current year data
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Backend API ready
2. Add User Story 1 → Paginated table with navigation → Deploy/Demo (MVP!)
3. Add User Story 2 → Year filter working → Deploy/Demo
4. Add User Story 3 → All filters working → Deploy/Demo
5. Add User Story 4 → Column sorting working → Deploy/Demo
6. Polish → Responsive design, edge cases → Final Deploy

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Tests not included (not requested in specification)
- Existing GET /api/expenses endpoint is preserved unchanged for backward compatibility
- The new endpoint GET /api/expenses/list is additive, not a replacement
- ExpenseListTable is a NEW component separate from existing ExpenseList.tsx (which is used in budget detail views)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
