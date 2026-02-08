# Tasks: Parent Category Budget & Expense Support

**Input**: Design documents from `/specs/012-parent-category-budgets/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not requested. Manual testing only per spec.

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

**Purpose**: No new project setup needed. This is an enhancement to an existing codebase. Phase 1 is a no-op.

**Checkpoint**: Existing project compiles and runs — proceed to Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: DTO extensions and repository queries that ALL user stories depend on. These are shared infrastructure changes that must be in place before any user story implementation.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 [P] Add `createParentCategoryBudget` (Boolean) and `parentCategoryBudgetAmount` (BigDecimal) fields with getters/setters to `budget-backend/src/main/java/com/homebudget/dto/BudgetDTO.java`
- [X] T002 [P] Add `childrenBudgetSum` (BigDecimal), `childrenSpending` (BigDecimal), and `isParentCategory` (Boolean) fields with getters/setters to `budget-backend/src/main/java/com/homebudget/dto/BudgetSummaryDTO.java`
- [X] T003 [P] Add `parentCategoryBudgetExists` (boolean), `parentCategoryBudgetId` (Long), `parentCategoryBudgetAmount` (BigDecimal), and `parentCategoryName` (String) fields with getters/setters to `budget-backend/src/main/java/com/homebudget/dto/BudgetValidationDTO.java`
- [X] T004 [P] Add `parentCategoryBudgetUpdated` nested object fields (`parentCategoryName`, `previousAmount`, `newAmount`, `year`, `month`) to `budget-backend/src/main/java/com/homebudget/dto/BudgetDTO.java` for the create budget response — add a `ParentCategoryBudgetUpdateInfo` inner class or separate DTO
- [X] T005 [P] Add JPQL query `sumBudgetsByChildCategoriesAndPeriod` to `budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java`: `SELECT COALESCE(SUM(b.totalAmount), 0) FROM Budget b WHERE b.category.parentCategory.id = :parentCategoryId AND b.year = :year AND b.month = :month` — returns sum of child category budget amounts for a parent category and period
- [X] T006 [P] Add JPQL query `sumExpensesByParentCategoryBudgets` to `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java`: sum expense amounts across all budgets whose category's parent is the given parentCategoryId, for a given year and month
- [X] T007 [P] Add JPQL query `findByFiltersPageableWithCategoryIds` to `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java`: duplicate of `findByFiltersPageable` but accepting `List<Long> categoryIds` with `e.category.id IN :categoryIds` instead of single categoryId — also add matching `getFilteredTotalAmountWithCategoryIds` count query
- [X] T008 [P] Add `createParentCategoryBudget` (boolean) and `parentCategoryBudgetAmount` (number) fields to the `CreateBudgetRequest` interface in `budget-frontend/src/services/budgetService.ts`
- [X] T009 [P] Add `parentCategoryBudgetExists` (boolean), `parentCategoryBudgetId` (number), `parentCategoryBudgetAmount` (number), and `parentCategoryName` (string) fields to the `BudgetValidationDTO` interface in `budget-frontend/src/services/budgetService.ts`
- [X] T010 [P] Add `childrenBudgetSum` (number), `childrenSpending` (number), and `isParentCategory` (boolean) fields to the `BudgetSummaryDTO` interface (or the budget response type used by the budget list) in `budget-frontend/src/services/budgetService.ts`

**Checkpoint**: All DTOs and queries ready. User story implementation can now begin.

---

## Phase 3: User Story 1 — Create Budget on Parent Category (Priority: P1) 🎯 MVP

**Goal**: Remove the restriction that prevents budget creation on parent categories. Allow users to select any category (parent or child) in the budget creation form.

**Independent Test**: Create a budget on a parent category (e.g., "Food") and verify it appears in the budget list. The category dropdown should show both parent and child categories in a grouped select pattern.

### Implementation for User Story 1

- [X] T011 [US1] Remove the parent category restriction in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: delete lines 75-79 (`long childCategoryCount = categoryRepository.countByParentCategoryId(category.getId()); if (childCategoryCount > 0) { throw ... }`) in the `createBudget()` method
- [X] T012 [US1] Update `getBudgetValidation()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: after the existing parentBudget lookup, add logic to check if the selected category has a parent category — if so, look up the parent category's budget for the same period (year + month) and populate the new `parentCategoryBudgetExists`, `parentCategoryBudgetId`, `parentCategoryBudgetAmount`, and `parentCategoryName` fields on `BudgetValidationDTO`
- [X] T013 [US1] Replace `flattenLeafCategories()` function in `budget-frontend/src/components/BudgetForm.tsx` (lines 94-107) with a grouped category builder: iterate `categories` hierarchy, for each parent category with children emit a `<ListSubheader>` group header followed by a `<MenuItem>` for the parent itself (selectable, labeled with icon + name + " (All)") and `<MenuItem>` items for each child (indented). For root-level categories with no children, emit a plain `<MenuItem>`. Import `ListSubheader` from `@mui/material`. Replace the `leafCategories.map(...)` rendering block (lines 331-342) with the new grouped rendering.
- [X] T014 [US1] Update `formData` initial state in `budget-frontend/src/components/BudgetForm.tsx` to include `createParentCategoryBudget: false` and `parentCategoryBudgetAmount: undefined` — reset these fields when category or month changes

**Checkpoint**: Users can now create budgets on parent categories. Category dropdown shows grouped select with parent categories as selectable items. Verify by creating a budget on a parent category.

---

## Phase 4: User Story 2 — Automatic Parent Budget Creation When Setting Child Budget (Priority: P1)

**Goal**: When creating a child budget, the system checks the parent category's budget status and either offers to create it (checkbox) or auto-increments it.

**Independent Test**: Create a child budget when parent category has no budget → checkbox appears. Create another child budget when parent category already has a budget → auto-increment with info message.

### Implementation for User Story 2

- [X] T015 [US2] Add parent category budget creation logic to `createBudget()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: after the existing restriction removal (T011), add a block that checks if the category has a parent category — if `category.getParentCategory() != null` and `dto.getCreateParentCategoryBudget() == true`, check if a budget exists for the parent category (same year, same month) — if not, create one with `dto.getParentCategoryBudgetAmount()`. Set the `parentCategoryBudgetUpdated` info on the response DTO.
- [X] T016 [US2] Add parent category budget auto-increment logic to `createBudget()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: when creating a child category budget and the parent category already has a budget for the same period (same year, same month), automatically increment the parent category budget's `totalAmount` by `dto.getTotalAmount()`. Record the previous and new amounts in a `ParentCategoryBudgetUpdateInfo` on the response DTO.
- [X] T017 [US2] Add parent category budget checkbox UI to `budget-frontend/src/components/BudgetForm.tsx`: when a child category is selected (has `parentCategory`) and `validation.parentCategoryBudgetExists === false`, show a new section with a `<Checkbox>` labeled "Also create budget for parent category '{parentCategoryName}'" (checked by default), and an editable `<TextField>` for the parent category budget amount (pre-filled with the child budget amount). Wire the checkbox to `formData.createParentCategoryBudget` and the amount to `formData.parentCategoryBudgetAmount`.
- [X] T018 [US2] Add auto-increment info message to `budget-frontend/src/components/BudgetForm.tsx` (or the budget creation page `budget-frontend/src/app/budgets/new/page.tsx`): after successful budget creation, if the response contains `parentCategoryBudgetUpdated`, display a Snackbar/Alert with the message "Budget for '{name}' has been updated from ${previous} to ${new} for {month} {year}."
- [X] T019 [US2] Update validation logic in `budget-frontend/src/components/BudgetForm.tsx`: when `createParentCategoryBudget` is checked and `parentCategoryBudgetAmount` is not set or <= 0, add validation error. Sync `parentCategoryBudgetAmount` with `totalAmount` when user hasn't manually touched it (similar to existing `parentAmountTouched` pattern).

**Checkpoint**: Creating a child budget with no parent category budget shows checkbox → creates both. Creating a child budget with existing parent category budget auto-increments and shows info message.

---

## Phase 5: User Story 3 — Expense Aggregation: Child Expenses Count Toward Parent Category (Priority: P1)

**Goal**: In all views, expenses on child categories are aggregated into parent category spending totals.

**Independent Test**: Create expenses on child categories, then view the parent category budget — spending should include child expenses. Filter expenses by parent category — results should include child category expenses.

### Implementation for User Story 3

- [X] T020 [US3] Update `mapToBudgetSummary()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: after computing `totalSpending` for the budget, check if the budget's category is a parent category (`categoryRepository.countByParentCategoryId(category.getId()) > 0`). If yes, query child category budgets' spending for the same period using the new `sumExpensesByParentCategoryBudgets` repository query and add to `childrenSpending`. Also query child category budgets' total amounts using `sumBudgetsByChildCategoriesAndPeriod` and set `childrenBudgetSum`. Set `isParentCategory = true`. Add `childrenSpending` to `totalSpending` for the parent budget's display spending.
- [X] T021 [US3] Update `getYearlyBudgetView()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: remove the `if (childCount > 0) { continue; }` skip at line 634 so parent categories are included in the yearly view. For parent categories, aggregate child spending into the parent's yearly spending totals.
- [X] T022 [US3] Update `getExpenseList()` in `budget-backend/src/main/java/com/homebudget/service/ExpenseService.java`: when `categoryId` is provided, check if the category has children (`categoryRepository.countByParentCategoryId(categoryId) > 0`). If yes, collect the parent category ID + all child category IDs into a `List<Long>` and use the new `findByFiltersPageableWithCategoryIds` query instead of `findByFiltersPageable`. Also use `getFilteredTotalAmountWithCategoryIds` for the aggregate total.
- [X] T023 [US3] Update `getAllExpenses()` in `budget-backend/src/main/java/com/homebudget/service/ExpenseService.java`: apply the same category expansion logic — when filtering by a category that has children, include child category IDs in the filter.
- [X] T024 [US3] Update budget list display in `budget-frontend/src/app/budgets/page.tsx`: for budget cards where `isParentCategory === true` and `childrenBudgetSum > 0`, render an additional line below the budget amount showing "Including children: ${budget.totalAmount + budget.childrenBudgetSum}" in a smaller, secondary typography. Also show aggregated spending that includes `childrenSpending`.

**Checkpoint**: Parent category budgets show aggregated spending from child categories. Budget list shows "including children" subtotal. Expense list filtered by parent category includes child expenses.

---

## Phase 6: User Story 4 — Record Expense on Parent Category (Priority: P2)

**Goal**: Allow expenses to be recorded directly against parent categories (the expense form already supports this — verify and ensure it works with the new parent category budgets).

**Independent Test**: Record an expense with a parent category selected, verify it links to the parent category's budget and appears in all views.

### Implementation for User Story 4

- [X] T025 [US4] Update `resolveBudgetForExpense()` in `budget-backend/src/main/java/com/homebudget/service/ExpenseService.java`: ensure that when an expense is created with a parent category, the budget resolution finds the parent category's monthly budget (or yearly budget) correctly — the existing logic should work since it looks up by categoryId + date, but verify and handle the case where a parent category has both monthly and yearly budgets.
- [X] T026 [US4] Verify the expense form `budget-frontend/src/components/expenses/ExpenseForm.tsx` already shows parent categories in its category dropdown — it uses `categoryService.getAllCategories()` which returns all categories. No changes needed if parent categories are already selectable. If the dropdown does not visually distinguish parent from child, optionally add hierarchy display (e.g., bold parent names or indentation).

**Checkpoint**: Expenses can be recorded directly on parent categories. The expense links to the correct budget.

---

## Phase 7: User Story 5 — Parent Budget Yearly Logic Unchanged (Priority: P2)

**Goal**: Verify and ensure the existing yearly budget logic (month=null as annual envelope) continues to work correctly for parent categories.

**Independent Test**: Create a parent category budget for a month, verify a yearly budget is auto-created. Verify yearly budget sum equals monthly sum.

### Implementation for User Story 5

- [X] T027 [US5] Verify yearly budget auto-creation in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: when a monthly budget is created for a parent category, the existing logic for creating yearly parent budget (month=null) and `ensureMonthlyBudgetsForRemainingMonths()` should still work. Test by creating a monthly budget for a parent category and confirming the yearly budget is created with the correct amount.
- [X] T028 [US5] Verify `updateBudget()` validation in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: ensure that when updating a parent category's yearly budget, the monthly sum validation still works (yearly amount >= sum of monthly amounts). No code changes expected — this is a verification task.

**Checkpoint**: Yearly budget logic works identically for parent and child categories. No regressions.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final integration verification and edge case handling.

- [X] T029 Verify dashboard `getCurrentMonthBudget()` in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java` correctly sums both parent and child category budgets for the current month without double-counting expenses — each expense is linked to exactly one budget, so the existing sum-by-budgetId approach should be correct
- [X] T030 Verify budget deletion in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: deleting a parent category budget does NOT cascade to or affect child category budgets (existing `deleteBudget()` only deletes the specified budget and its own expenses via JPA cascade)
- [X] T031 Run full quickstart.md validation scenarios 1-7 from `specs/012-parent-category-budgets/quickstart.md` to verify end-to-end functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No-op — existing project
- **Phase 2 (Foundational)**: DTOs and queries — can start immediately. BLOCKS all user stories.
- **Phase 3 (US1)**: Depends on Phase 2. Remove restriction + grouped select.
- **Phase 4 (US2)**: Depends on Phase 2 and Phase 3 (T011 restriction removal, T012 validation). Parent category budget creation/auto-increment.
- **Phase 5 (US3)**: Depends on Phase 2. Expense aggregation. Can run in parallel with US2 (different files).
- **Phase 6 (US4)**: Depends on Phase 2 and Phase 3 (parent budgets must exist). Expense on parent category.
- **Phase 7 (US5)**: Depends on Phase 3 (restriction removal). Yearly logic verification.
- **Phase 8 (Polish)**: Depends on all user stories.

### User Story Dependencies

- **US1 (P1)**: No story dependencies. Foundational change.
- **US2 (P1)**: Depends on US1 (restriction must be removed for parent category budgets to be created).
- **US3 (P1)**: No story dependencies. Aggregation logic is independent of budget creation flow.
- **US4 (P2)**: Depends on US1 (parent category budgets must be creatable for expenses to link).
- **US5 (P2)**: Depends on US1 (yearly logic verification requires parent category budgets).

### Within Each User Story

- Backend DTOs/queries (Phase 2) → Backend service logic → Frontend UI
- Repository queries before service methods that use them
- Service changes before frontend that depends on response shape

### Parallel Opportunities

**Phase 2 (all 10 tasks are [P])**:
```
T001, T002, T003, T004 — Backend DTOs (4 different files)
T005, T006, T007 — Repository queries (2 different files)
T008, T009, T010 — Frontend types (1 file, but independent sections)
```

**Phase 3 + Phase 5 (after Phase 2)**:
```
US1 (T011-T014) and US3 (T020-T024) can run in parallel:
- US1 modifies: BudgetService.createBudget(), BudgetForm.tsx
- US3 modifies: BudgetService.mapToBudgetSummary()/getYearlyBudgetView(), ExpenseService, budgets/page.tsx
- No file conflicts between US1 and US3 (except BudgetService.java — but different methods)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational DTOs and queries
2. Complete Phase 3: US1 — Remove restriction, grouped select
3. **STOP and VALIDATE**: Create a budget on a parent category
4. Delivers immediate value: parent category budgets are possible

### Incremental Delivery

1. Phase 2 → Foundation ready
2. Phase 3: US1 → Parent category budgets possible → Validate
3. Phase 4: US2 → Auto-create/increment parent category budget → Validate
4. Phase 5: US3 → Expense aggregation in all views → Validate
5. Phase 6: US4 → Direct expense on parent categories → Validate
6. Phase 7: US5 → Yearly logic verified → Validate
7. Phase 8 → End-to-end validation

### Recommended Execution Order (Single Developer)

Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6 (US4) → Phase 7 (US5) → Phase 8

---

## Notes

- [P] tasks = different files, no dependencies
- [USn] label maps task to specific user story for traceability
- US1 is the MVP — removing the parent category restriction and updating the form
- US2 and US3 are the core value — auto-create/increment and expense aggregation
- US4 and US5 are verification/polish — expense on parent and yearly logic preservation
- No database schema changes needed — all changes are service-layer and UI
- Existing `createParentBudget`/`extendParentBudget` fields are for YEARLY parent budget (month=null); new `createParentCategoryBudget`/`parentCategoryBudgetAmount` fields are for PARENT CATEGORY budget (category hierarchy)
