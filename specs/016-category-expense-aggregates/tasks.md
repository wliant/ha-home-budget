# Tasks: Category Expense Aggregates

**Input**: Design documents from `/specs/016-category-expense-aggregates/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/api-changes.yaml, quickstart.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks grouped by user story. US1 and US2 are both P1 but US2 depends on US1's entity cleanup. US3 and US4 (P2) depend on US2's aggregate infrastructure.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1, US2, US3, US4)
- Exact file paths relative to repository root

---

## Phase 1: Foundational — Database Migration & Entity Changes

**Purpose**: Schema migration and entity/DTO changes that all user stories depend on. These must be committed together so the app starts with consistent code + schema.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 Create Liquibase migration `budget-backend/src/main/resources/db/changelog/changes/010-remove-budget-fk-from-expenses.xml` with 4 changesets: (1) UPDATE expenses to populate category_id from budget's category where NULL, (2) ALTER expenses to make category_id NOT NULL, (3) DROP FK constraint fk_expenses_budget + DROP INDEX idx_expenses_budget_id + DROP COLUMN budget_id, (4) CREATE INDEX idx_expenses_category_date ON expenses(category_id, expense_date). Register in `budget-backend/src/main/resources/db/changelog/db.changelog-master.xml`.

- [X] T002 [P] Update Expense entity in `budget-backend/src/main/java/com/homebudget/model/Expense.java`: Remove the `budget` field (@ManyToOne Budget), its @JoinColumn, @NotNull, getter/setter, and constructor parameter. Change `category` field from optional to @NotNull with @JoinColumn(nullable = false). Remove `import com.homebudget.model.Budget` if unused.

- [X] T003 [P] Update Budget entity in `budget-backend/src/main/java/com/homebudget/model/Budget.java`: Remove the `expenses` list field (@OneToMany with CascadeType.ALL, orphanRemoval). Remove `addExpense()` and `removeExpense()` helper methods. Remove `import com.homebudget.model.Expense` and `java.util.ArrayList`/`java.util.List` if unused.

- [X] T004 [P] Update ExpenseDTO in `budget-backend/src/main/java/com/homebudget/dto/ExpenseDTO.java`: Remove `budgetId` field, its @NotNull validation, getter/setter. Remove `warnings` list field (was for budget date mismatch), its getter/setter/add method. Remove budgetId from constructor if present. Keep categoryId field (ensure it exists).

- [X] T005 [P] Create CategoryExpenseAggregateDTO in `budget-backend/src/main/java/com/homebudget/dto/CategoryExpenseAggregateDTO.java`: Fields: categoryId (Long), categoryName (String), categoryIcon (String), parentCategoryId (Long, nullable), directAmount (BigDecimal), childrenAmount (BigDecimal), totalAmount (BigDecimal), year (Integer), month (Integer, nullable for yearly). Manual getters/setters (no Lombok per project convention).

**Checkpoint**: Entity layer is clean — no budget-expense relationship in code or DB.

---

## Phase 2: US1 — Decouple Expenses from Budgets (Priority: P1) 🎯 MVP

**Goal**: Expenses can be created, updated, and deleted with only a category reference. Budget deletion does not affect expenses.

**Independent Test**: Create an expense by selecting a category (no budget). Verify it saves. Delete a budget and verify its former expenses still exist.

### Backend Changes

- [X] T006 [US1] Clean up ExpenseRepository in `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java`: Remove all budget-based query methods: findByBudgetIdOrderByExpenseDateDesc, findByBudgetId, findByBudgetIdAndCategoryId, findByBudgetIdAndExpenseDateBetween, findByBudgetIdAndCategoryIdAndExpenseDateBetweenAndCreatedBy, sumAmountByBudgetId, countByBudgetId, getCategoryBreakdown, sumExpensesByParentCategoryBudgets. Keep all category/date-based queries (findByFilters*, getFilteredTotalAmount*). Update findByFilters and findByFiltersPageable queries to remove any `e.budget.id` references.

- [X] T007 [US1] Rewrite ExpenseService in `budget-backend/src/main/java/com/homebudget/service/ExpenseService.java`: In createExpense(): remove call to resolveBudgetForExpense() and expense.setBudget(); make category required — if categoryId is null, look up the "Uncategorized" system category and assign it; remove checkDateMismatch() call. In updateExpense(): remove budget reassignment logic. In toDTO(): remove dto.setBudgetId(). In getAllExpenses(): remove any budgetId-based filtering branches. In getExpenseList(): ensure filtering works without budget references. Delete methods: resolveBudgetForExpense(), checkDateMismatch(). Remove unused imports (Budget, BudgetRepository, BudgetNotFoundException).

- [X] T008 [US1] Update ExpenseController in `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java`: Remove budget ID references from logging statements (e.g., `dto.getBudgetId()` in log messages). Update any parameter documentation referencing budgetId.

- [X] T009 [US1] Update BudgetService in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: Remove `reassignParentExpensesToMonthlyBudgets()` method entirely. In `getBudgetById()`: remove mapping of budget.getExpenses() to ExpenseDTO list (budget no longer has expenses). Remove `mapToExpenseDTO()` private method. In `createBudget()`: remove the section that reassigns parent budget expenses to the new monthly budget. In `deleteBudget()`: ensure it just deletes the budget record (no cascade since relationship removed). Remove unused ExpenseRepository/ExpenseDTO imports if all usages are removed from this service.

- [X] T010 [US1] Update ExpenseInputJobService in `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java`: Remove the `resolveBudgetId()` method. In `createExpenseFromTemporary()`: remove `dto.setBudgetId(resolveBudgetId(record))` line; ensure categoryId is set from the temporary record's category. Remove unused BudgetRepository import.

### Frontend Changes

- [X] T011 [P] [US1] Update frontend expense types and service in `budget-frontend/src/services/expenseService.ts` and `budget-frontend/src/types/expense.ts`: In expenseService.ts — remove `budgetId` from ExpenseDTO interface, CreateExpenseRequest, UpdateExpenseRequest; remove budgetId from filter params in getAllExpenses(). In types/expense.ts — remove `budgetId` from ExpenseFormState interface.

- [X] T012 [US1] Rewrite expense creation page in `budget-frontend/src/app/expenses/new/page.tsx`: Remove the entire budget auto-selection logic (useEffect that calls budgetService.getAllBudgets and matches by category/date). Remove budgetId from form state initialization and reset. Remove budget validation (errors.budget check). Remove budgetId from submission payload. Make category selection required — disable submit button if no category selected (instead of checking budgetId). Remove budgetService import.

- [X] T013 [P] [US1] Update ExpenseForm component in `budget-frontend/src/components/expenses/ExpenseForm.tsx`: Remove `budgetId` from props interface. Remove budgetId from the submission payload construction. The form should accept and pass: amount, description, expenseDate, categoryId only.

- [X] T014 [US1] Update budget detail page in `budget-frontend/src/app/budgets/[id]/page.tsx`: Remove budgetId query params from expense navigation links (lines like `router.push(\`/expenses/new?budgetId=...\`)`). Change "Add Expense" to navigate to `/expenses/new` without budgetId. Change expense view links to navigate to `/expenses/${expenseId}/edit` without budgetId.

- [X] T015 [US1] Update frontend budgetService.ts in `budget-frontend/src/services/budgetService.ts`: Remove `budgetId` from any duplicate ExpenseDTO interface defined in this file. Keep budget-related types intact.

**Checkpoint**: Expenses are fully decoupled from budgets. Creating, editing, deleting expenses works with category only. Budget deletion is safe.

---

## Phase 3: US2 — Monthly Expense Aggregates per Category (Priority: P1)

**Goal**: The system calculates monthly spending totals per category with parent category rollup. A new API endpoint exposes these aggregates.

**Independent Test**: Create expenses for child and parent categories in a specific month. Call GET /api/expenses/aggregates/monthly?year=2026&month=1 and verify totals, including parent rollup.

- [X] T016 [US2] Add aggregate query methods to ExpenseRepository in `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java`: Add JPQL query `getMonthlyAggregatesByYear(Integer year)` that returns List<Object[]> with [categoryId, month, SUM(amount)] grouped by category_id and MONTH(expense_date) filtered by YEAR(expense_date) = :year. Add `sumByCategoryAndMonth(Long categoryId, int year, int month)` returning BigDecimal for single-category monthly aggregate. Add `getMonthlyAggregatesByCategoryAndYear(Long categoryId, Integer year)` for all months of one category.

- [X] T017 [US2] Create ExpenseAggregateService in `budget-backend/src/main/java/com/homebudget/service/ExpenseAggregateService.java`: New @Service class. Method `getMonthlyAggregates(Integer year, Integer month)` returns List<CategoryExpenseAggregateDTO>: (1) query ExpenseRepository.getMonthlyAggregatesByYear (or filtered by month), (2) query CategoryRepository for all categories with hierarchy, (3) build DTOs with directAmount per category, (4) for parent categories: sum children's directAmounts into childrenAmount, compute totalAmount = directAmount + childrenAmount. Handle null month param (return all 12 months) vs specific month.

- [X] T018 [US2] Add aggregate endpoints to ExpenseController in `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java`: Add GET `/api/expenses/aggregates/monthly` endpoint with @RequestParam year (required) and month (optional). Calls ExpenseAggregateService.getMonthlyAggregates(). Returns List<CategoryExpenseAggregateDTO>. Log the request at INFO level.

- [X] T019 [US2] Add frontend aggregate service methods in `budget-frontend/src/services/expenseService.ts`: Add interface `CategoryExpenseAggregate` with fields: categoryId, categoryName, categoryIcon, parentCategoryId, directAmount, childrenAmount, totalAmount, year, month. Add method `getMonthlyAggregates(year: number, month?: number): Promise<CategoryExpenseAggregate[]>` that calls GET /api/expenses/aggregates/monthly.

**Checkpoint**: Monthly aggregates available via API with parent rollup. Frontend service can call the endpoint.

---

## Phase 4: US3 — Yearly Budget View Using Category Aggregates (Priority: P2)

**Goal**: The yearly budget view sources spending data from category expense aggregates instead of budget-linked expense sums.

**Independent Test**: Create budgets and expenses for a year. Load the yearly budget view and verify spending columns show category aggregate totals.

**Depends on**: US1 (no budget-expense link), US2 (aggregate queries available)

- [X] T020 [US3] Rewrite BudgetService.getYearlyBudgetView() in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: Replace the current implementation that calls calculateTotalSpending(budgetId) and sumExpensesByParentCategoryBudgets(). New approach: (1) fetch all budgets for the year, (2) call ExpenseRepository.getMonthlyAggregatesByYear(year) once to get all category/month aggregates, (3) build a Map<Long,Map<Integer,BigDecimal>> of categoryId → month → spending, (4) for each category with a budget: look up monthly spending from the map, (5) for parent categories: include child category spending from the map. Inject ExpenseAggregateService or use ExpenseRepository directly. Keep YearlyCategoryBudgetDTO and YearlyMonthlyBudgetDTO response structures unchanged.

- [X] T021 [US3] Rewrite BudgetService.getMonthBudgetSummary() in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: Replace the loop that calls expenseRepository.sumAmountByBudgetId() per budget. New approach: (1) fetch budgets for the month, (2) for each budget's category, get monthly aggregate from ExpenseRepository (sumByCategoryAndMonth or from batch query), (3) sum spending across all budgeted categories. Also update calculateTotalSpending() to use category aggregates if still called elsewhere, or remove it if fully replaced.

- [X] T022 [US3] Update BudgetService.mapToSummary() in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: The method currently calls expenseRepository.sumAmountByBudgetId(budget.getId()) and expenseRepository.sumExpensesByParentCategoryBudgets(). Replace with category-based aggregate queries: sumByCategoryAndMonth for the budget's category and period. For parent categories, sum child categories' aggregates using the aggregate service or repository.

- [X] T023 [US3] Update budget detail endpoint - BudgetService.getBudgetById() in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: Instead of returning a list of expenses, return the budget info plus categorySpending (aggregate spending for the budget's category and period). Add categorySpending and spendingPercentage fields to BudgetSummaryDTO (or create a dedicated BudgetDetailDTO) to convey: budget amount, category aggregate spending, and percentage.

- [X] T024 [US3] Update frontend budget detail page in `budget-frontend/src/app/budgets/[id]/page.tsx`: Replace the expenses list section with a spending summary showing: budget amount vs. category aggregate spending, spending percentage, and a link to the expense list page filtered by the budget's category and date period (e.g., `/expenses?categoryId=X&year=Y&month=Z`).

**Checkpoint**: Yearly budget view and budget detail page use category aggregates for all spending data.

---

## Phase 5: US4 — Yearly Expense Aggregate per Category (Priority: P2)

**Goal**: Yearly spending totals per category available via API for dashboards and summary views.

**Independent Test**: Create expenses across multiple months. Call GET /api/expenses/aggregates/yearly?year=2026 and verify totals match sum of monthly aggregates.

**Can run in parallel with US3** (different endpoints, different service methods).

- [X] T025 [P] [US4] Add yearly aggregate query to ExpenseRepository in `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java`: Add JPQL query `getYearlyAggregatesByYear(Integer year)` returning List<Object[]> with [categoryId, SUM(amount)] grouped by category_id filtered by YEAR(expense_date) = :year. Add `sumByCategoryAndYear(Long categoryId, Integer year)` returning BigDecimal for single-category yearly aggregate.

- [X] T026 [P] [US4] Add yearly aggregate method to ExpenseAggregateService in `budget-backend/src/main/java/com/homebudget/service/ExpenseAggregateService.java`: Method `getYearlyAggregates(Integer year)` returns List<CategoryExpenseAggregateDTO> with month=null. Build from getYearlyAggregatesByYear query, apply parent category rollup (same logic as monthly).

- [X] T027 [US4] Add yearly aggregate endpoint to ExpenseController in `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java`: Add GET `/api/expenses/aggregates/yearly` endpoint with @RequestParam year (required). Calls ExpenseAggregateService.getYearlyAggregates(). Returns List<CategoryExpenseAggregateDTO>.

- [X] T028 [US4] Add frontend yearly aggregate service method in `budget-frontend/src/services/expenseService.ts`: Add method `getYearlyAggregates(year: number): Promise<CategoryExpenseAggregate[]>` that calls GET /api/expenses/aggregates/yearly.

**Checkpoint**: Yearly aggregates available via API. Frontend service can call the endpoint.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Dashboard updates and dead code cleanup.

- [X] T029 Update dashboard BudgetSummaryCard in `budget-frontend/src/components/home/BudgetSummaryCard.tsx`: If this component displays spending data from the budget summary API, verify it works correctly with the updated getMonthBudgetSummary() that now uses category aggregates. No frontend logic change needed if the API response shape is unchanged — just verify rendering.

- [X] T030 Remove dead code across backend: Search for any remaining references to `budget.getExpenses()`, `expense.getBudget()`, `sumAmountByBudgetId`, `findByBudgetId`, `resolveBudgetForExpense`, `checkDateMismatch`, `reassignParentExpensesToMonthlyBudgets` across all Java files. Remove any orphaned imports. Verify no compile errors.

- [X] T031 Update BudgetService.createBudget() in `budget-backend/src/main/java/com/homebudget/service/BudgetService.java`: Remove the code block that reassigns expenses from parent budget to monthly budgets when a new monthly budget is created (was in reassignParentExpensesToMonthlyBudgets, already removed in T009). Verify the auto-create-children and auto-create-parent budget logic still works (these are budget-to-budget operations, not expense-related). Remove any remaining references to expense reassignment.

- [X] T032 Verify migration and quickstart scenarios per `specs/016-category-expense-aggregates/quickstart.md`: Run the app, verify Liquibase migration completes. Test: (1) create expense with category only, (2) view monthly aggregates, (3) view yearly budget, (4) delete budget and confirm expenses unaffected, (5) edit expense category and confirm aggregates update.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Foundational)**: No dependencies — start immediately. BLOCKS all user stories.
- **Phase 2 (US1)**: Depends on Phase 1 completion.
- **Phase 3 (US2)**: Depends on Phase 2 (US1) — needs clean repository without budget queries.
- **Phase 4 (US3)**: Depends on Phase 3 (US2) — needs aggregate service/queries.
- **Phase 5 (US4)**: Depends on Phase 3 (US2) — needs aggregate service. **Can run in parallel with Phase 4 (US3).**
- **Phase 6 (Polish)**: Depends on all user stories complete.

### User Story Dependencies

- **US1 (P1)**: Foundational only — no dependencies on other stories
- **US2 (P1)**: Depends on US1 (clean entity layer, budget-free repository)
- **US3 (P2)**: Depends on US2 (aggregate queries and service)
- **US4 (P2)**: Depends on US2 (aggregate queries and service) — parallel with US3

### Within Each User Story

- Repository changes before service changes
- Service changes before controller changes
- Backend changes before frontend changes (API contract must be stable)

### Parallel Opportunities

Within Phase 1 (Foundational):
```
Parallel: T002 + T003 + T004 + T005 (different files, no dependencies)
```

Within Phase 2 (US1):
```
Parallel: T011 + T013 (different frontend files)
Sequential: T006 → T007 → T008 (repo before service before controller)
Sequential: T011 → T012 (types before page that uses them)
```

Within Phase 5 (US4):
```
Parallel: T025 + T026 (repo and service can be developed together if repo interface is known)
```

Across Phases:
```
Parallel: Phase 4 (US3) and Phase 5 (US4) after Phase 3 (US2) completes
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Foundational (migration + entities)
2. Complete Phase 2: US1 (decouple expenses from budgets)
3. **STOP and VALIDATE**: Create expense with category only, delete budget safely
4. This is functional but spending aggregates not yet available

### Incremental Delivery

1. Phase 1 (Foundational) → Entity layer clean
2. Phase 2 (US1) → Expenses decoupled → **Validate**
3. Phase 3 (US2) → Monthly aggregates available → **Validate**
4. Phase 4 + 5 (US3 + US4 in parallel) → Yearly view + yearly aggregates → **Validate**
5. Phase 6 (Polish) → Dashboard verified, dead code removed → **Done**

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [US#] label maps task to user story for traceability
- All entity/DTO changes (Phase 1) must be committed together with migration for app consistency
- Budget-to-budget relationships (yearly parent ↔ monthly children) are UNCHANGED
- The "Uncategorized" system category must exist in the database before expense creation
- Aggregates are on-the-fly JPQL queries, not materialized views
