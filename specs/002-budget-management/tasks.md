# Tasks: Budget and Expense Management

**Input**: Design documents from `/specs/002-budget-management/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are NOT explicitly requested in the specification. Manual testing via acceptance scenarios in quickstart.md. Test tasks omitted per constitution Principle VI.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend structure**: `budget-backend/src/main/java/com/homebudget/`
- **Frontend structure**: `budget-frontend/src/`
- **Database migrations**: `budget-backend/src/main/resources/db/changelog/changes/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema setup for all entities required across user stories

- [ ] T001 [P] Create Liquibase migration for budgets table in budget-backend/src/main/resources/db/changelog/changes/003-create-budgets-table.xml
- [ ] T002 [P] Create Liquibase migration for categories table with default "Uncategorized" in budget-backend/src/main/resources/db/changelog/changes/004-create-categories-table.xml
- [ ] T003 [P] Create Liquibase migration for expenses table in budget-backend/src/main/resources/db/changelog/changes/005-create-expenses-table.xml
- [ ] T004 Update Liquibase master changelog to include 003, 004, 005 migrations in budget-backend/src/main/resources/db/changelog/db.changelog-master.xml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core backend infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 [P] Create Budget JPA entity in budget-backend/src/main/java/com/homebudget/model/Budget.java
- [ ] T006 [P] Create Category JPA entity in budget-backend/src/main/java/com/homebudget/model/Category.java
- [ ] T007 Create Expense JPA entity with Budget and Category relationships in budget-backend/src/main/java/com/homebudget/model/Expense.java
- [ ] T008 [P] Create BudgetDTO in budget-backend/src/main/java/com/homebudget/dto/BudgetDTO.java
- [ ] T009 [P] Create ExpenseDTO in budget-backend/src/main/java/com/homebudget/dto/ExpenseDTO.java
- [ ] T010 [P] Create CategoryDTO in budget-backend/src/main/java/com/homebudget/dto/CategoryDTO.java
- [ ] T011 [P] Create BudgetSummaryDTO for list views in budget-backend/src/main/java/com/homebudget/dto/BudgetSummaryDTO.java
- [ ] T012 [P] Create BudgetNotFoundException in budget-backend/src/main/java/com/homebudget/exception/BudgetNotFoundException.java
- [ ] T013 [P] Create DuplicateBudgetException in budget-backend/src/main/java/com/homebudget/exception/DuplicateBudgetException.java
- [ ] T014 [P] Create CategoryInUseException in budget-backend/src/main/java/com/homebudget/exception/CategoryInUseException.java
- [ ] T015 [P] Create BudgetRepository interface in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java
- [ ] T016 [P] Create ExpenseRepository interface with custom queries in budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java
- [ ] T017 [P] Create CategoryRepository interface in budget-backend/src/main/java/com/homebudget/repository/CategoryRepository.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Create and View Budgets (Priority: P1) 🎯 MVP

**Goal**: Enable users to create monthly budgets with target amounts and view them in a list with spending status

**Independent Test**: Create budget for "October 2025" with $3000, view in list showing month, amount, $0 spent, 0%. Verify duplicate prevention and validation errors.

### Backend Implementation for User Story 1

- [ ] T018 [P] [US1] Implement BudgetService with create, findAll, findById methods in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T019 [P] [US1] Implement BudgetController with POST /api/budgets and GET /api/budgets endpoints in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T020 [US1] Add duplicate budget validation in BudgetService (check year+month uniqueness)
- [ ] T021 [US1] Add calculateTotalSpending method in BudgetService using ExpenseRepository aggregate query
- [ ] T022 [US1] Add calculateSpendingPercentage method in BudgetService
- [ ] T023 [US1] Implement GET /api/budgets/{id} endpoint in BudgetController with spending calculations
- [ ] T024 [US1] Add global exception handler @ControllerAdvice for Budget exceptions in budget-backend/src/main/java/com/homebudget/exception/GlobalExceptionHandler.java

### Frontend Implementation for User Story 1

- [ ] T025 [P] [US1] Create budgetService.ts with getAllBudgets, createBudget, getBudgetById methods in budget-frontend/src/services/budgetService.ts
- [ ] T026 [P] [US1] Create BudgetForm component with Material-UI fields in budget-frontend/src/components/budgets/BudgetForm.tsx
- [ ] T027 [P] [US1] Create BudgetCard component for list display in budget-frontend/src/components/budgets/BudgetCard.tsx
- [ ] T028 [P] [US1] Create BudgetSummary component showing spending status in budget-frontend/src/components/budgets/BudgetSummary.tsx
- [ ] T029 [US1] Create budget list page in budget-frontend/src/app/budgets/page.tsx
- [ ] T030 [US1] Create new budget page with BudgetForm in budget-frontend/src/app/budgets/new/page.tsx
- [ ] T031 [US1] Create budget detail page in budget-frontend/src/app/budgets/[id]/page.tsx
- [ ] T032 [US1] Add validation to BudgetForm (positive amount, year/month ranges) using React Hook Form
- [ ] T033 [US1] Add error handling and display for duplicate budget errors in BudgetForm
- [ ] T034 [US1] Add navigation link to Budgets page in main layout navigation

### Verification for User Story 1

- [ ] T035 [US1] Manual test: Create budget via UI, verify appears in list
- [ ] T036 [US1] Manual test: Attempt duplicate budget (same month), verify error message
- [ ] T037 [US1] Manual test: Submit negative amount, verify validation error
- [ ] T038 [US1] Manual test: View budget detail, verify shows $0 spent, 0%

**Checkpoint**: At this point, User Story 1 should be fully functional - users can create and view budgets

---

## Phase 4: User Story 2 - Record Expenses Against Budgets (Priority: P2)

**Goal**: Enable users to record expenses with amount, description, date, and optional category, tracking which user recorded each expense

**Independent Test**: Create budget for October 2025 with $3000, record 3 expenses ($150, $60, $120.50), verify budget shows $330.50 spent (11%) with user attribution

### Backend Implementation for User Story 2

- [ ] T039 [P] [US2] Implement ExpenseService with create, findAll, findById, update, delete methods in budget-backend/src/main/java/com/homebudget/service/ExpenseService.java
- [ ] T040 [P] [US2] Implement ExpenseController with CRUD endpoints in budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java
- [ ] T041 [US2] Add expense date validation logic in ExpenseService (check if date outside budget month)
- [ ] T042 [US2] Add filtering support to GET /api/expenses (budgetId, categoryId, dateRange, createdBy)
- [ ] T043 [US2] Add date mismatch warning to ExpenseDTO when expense date outside budget month
- [ ] T044 [US2] Update BudgetController GET /{id} to include expenses list with eager fetching
- [ ] T045 [US2] Add X-Hass-User header extraction in ExpenseController and set createdBy field

### Frontend Implementation for User Story 2

- [ ] T046 [P] [US2] Create expenseService.ts with CRUD methods and filtering in budget-frontend/src/services/expenseService.ts
- [ ] T047 [P] [US2] Create ExpenseForm component with Material-UI fields in budget-frontend/src/components/expenses/ExpenseForm.tsx
- [ ] T048 [P] [US2] Create ExpenseList component for displaying expenses table in budget-frontend/src/components/expenses/ExpenseList.tsx
- [ ] T049 [P] [US2] Create ExpenseItem component for individual expense display in budget-frontend/src/components/expenses/ExpenseItem.tsx
- [ ] T050 [US2] Create expense list page with filtering in budget-frontend/src/app/expenses/page.tsx
- [ ] T051 [US2] Create new expense page in budget-frontend/src/app/expenses/new/page.tsx
- [ ] T052 [US2] Add "Add Expense" button to budget detail page linking to new expense form
- [ ] T053 [US2] Update BudgetSummary component to show totalSpending and spendingPercentage
- [ ] T054 [US2] Add expense list to budget detail page showing all budget expenses
- [ ] T055 [US2] Add validation to ExpenseForm (positive amount, required description, valid date)
- [ ] T056 [US2] Display date mismatch warning in ExpenseForm when expense date outside budget month
- [ ] T057 [US2] Display createdBy username for each expense in ExpenseList

### Verification for User Story 2

- [ ] T058 [US2] Manual test: Record expense via UI, verify appears in budget detail
- [ ] T059 [US2] Manual test: Record 3 expenses, verify budget totalSpending and percentage update correctly
- [ ] T060 [US2] Manual test: Record expense by different user (change X-Hass-User header), verify createdBy shows correct username
- [ ] T061 [US2] Manual test: Record expense with date outside budget month, verify warning displays
- [ ] T062 [US2] Manual test: Filter expenses by date range, verify filtering works

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - budgets created, expenses recorded with spending calculations

---

## Phase 5: User Story 3 - Manage Spending Categories (Priority: P3)

**Goal**: Enable users to create custom spending categories with icons, use them in expenses, and view spending breakdown by category

**Independent Test**: Create 3 categories (Groceries, Utilities, Transportation), record expenses using categories, view budget breakdown showing spending per category

### Backend Implementation for User Story 3

- [ ] T063 [P] [US3] Implement CategoryService with CRUD methods and expense count check in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T064 [P] [US3] Implement CategoryController with CRUD endpoints in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T065 [US3] Add category deletion prevention logic in CategoryService (check if expenses reference category)
- [ ] T066 [US3] Add category unique name validation in CategoryService
- [ ] T067 [US3] Create BudgetSummaryWithBreakdownDTO for category breakdown in budget-backend/src/main/java/com/homebudget/dto/BudgetSummaryWithBreakdownDTO.java
- [ ] T068 [US3] Implement GET /api/budgets/{id}/summary endpoint with category breakdown in BudgetController
- [ ] T069 [US3] Add category breakdown calculation method in BudgetService (group expenses by category, sum amounts)

### Frontend Implementation for User Story 3

- [ ] T070 [P] [US3] Create categoryService.ts with CRUD methods in budget-frontend/src/services/categoryService.ts
- [ ] T071 [P] [US3] Create CategoryForm component with name and icon fields in budget-frontend/src/components/categories/CategoryForm.tsx
- [ ] T072 [P] [US3] Create CategoryList component for displaying categories in budget-frontend/src/components/categories/CategoryList.tsx
- [ ] T073 [P] [US3] Create CategoryBadge component for displaying category with icon in budget-frontend/src/components/categories/CategoryBadge.tsx
- [ ] T074 [US3] Create category management page in budget-frontend/src/app/categories/page.tsx
- [ ] T075 [US3] Update ExpenseForm to include category dropdown selection
- [ ] T076 [US3] Display category badge in ExpenseItem component
- [ ] T077 [US3] Add category breakdown table/chart to budget detail page
- [ ] T078 [US3] Handle category deletion errors (show error if category has expenses)
- [ ] T079 [US3] Add emoji picker or icon input to CategoryForm

### Verification for User Story 3

- [ ] T080 [US3] Manual test: Create category via UI, verify appears in list
- [ ] T081 [US3] Manual test: Select category when recording expense, verify expense tagged correctly
- [ ] T082 [US3] Manual test: View budget detail, verify category breakdown shows correct totals
- [ ] T083 [US3] Manual test: Attempt to delete category with expenses, verify error message
- [ ] T084 [US3] Manual test: Create expense without category, verify defaults to "Uncategorized"

**Checkpoint**: All core user stories (US1, US2, US3) should now be independently functional

---

## Phase 6: User Story 4 - Budget Dashboard and Insights (Priority: P4)

**Goal**: Provide dashboard with current month progress, spending trends, and top categories

**Independent Test**: Create budgets for Oct, Nov, Dec 2025 with expenses, view dashboard showing current month, trends, top 5 categories

### Backend Implementation for User Story 4

- [ ] T085 [P] [US4] Add getCurrentMonthBudget method in BudgetService
- [ ] T086 [P] [US4] Add getSpendingTrends method in BudgetService (last N months)
- [ ] T087 [P] [US4] Add getTopCategories method in CategoryService (by spending amount)
- [ ] T088 [US4] Create DashboardController with GET /api/dashboard endpoint in budget-backend/src/main/java/com/homebudget/controller/DashboardController.java
- [ ] T089 [US4] Create DashboardDTO combining current month, trends, top categories in budget-backend/src/main/java/com/homebudget/dto/DashboardDTO.java

### Frontend Implementation for User Story 4

- [ ] T090 [P] [US4] Create dashboardService.ts in budget-frontend/src/services/dashboardService.ts
- [ ] T091 [US4] Create dashboard page in budget-frontend/src/app/dashboard/page.tsx
- [ ] T092 [US4] Add current month budget progress card to dashboard
- [ ] T093 [US4] Add spending trends chart component (consider Chart.js or Recharts)
- [ ] T094 [US4] Add top categories table/chart to dashboard
- [ ] T095 [US4] Add warning indicator when spending exceeds 90% of budget
- [ ] T096 [US4] Add navigation link to Dashboard in main layout

### Verification for User Story 4

- [ ] T097 [US4] Manual test: View dashboard with multiple months of data, verify current month shown
- [ ] T098 [US4] Manual test: Verify trends chart displays spending over time correctly
- [ ] T099 [US4] Manual test: Verify top 5 categories shown with correct amounts
- [ ] T100 [US4] Manual test: Create budget with >90% spending, verify warning displays

**Checkpoint**: All user stories (US1-US4) complete with full feature functionality

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and finalize the feature

- [ ] T101 [P] Add budget edit (amount/description only) functionality - PUT /api/budgets/{id} endpoint
- [ ] T102 [P] Add budget delete with cascade confirmation - DELETE /api/budgets/{id}?confirm=true
- [ ] T103 [P] Add expense edit functionality - PUT /api/expenses/{id}
- [ ] T104 [P] Add expense delete functionality - DELETE /api/expenses/{id}
- [ ] T105 [P] Add category edit functionality - PUT /api/categories/{id}
- [ ] T106 [P] Add category delete functionality with validation - DELETE /api/categories/{id}
- [ ] T107 [P] Add optimistic locking version handling to Budget and Expense update endpoints
- [ ] T108 [P] Add pagination support to expense list (if >50 expenses)
- [ ] T109 [P] Add sorting options to budget list (by date, amount, spending %)
- [ ] T110 [P] Add loading states and skeleton loaders to all frontend data fetching
- [ ] T111 [P] Add error boundaries to frontend pages for graceful error handling
- [ ] T112 [P] Add form reset and cancel buttons to all forms
- [ ] T113 [P] Add confirmation dialogs for delete operations
- [ ] T114 [P] Improve mobile responsiveness for all pages (Material-UI Grid/breakpoints)
- [ ] T115 [P] Add accessibility labels (ARIA) to all interactive elements
- [ ] T116 Run full quickstart.md integration testing scenarios (Scenarios 1-6)
- [ ] T117 Verify performance goals: budget list <1s, expense entry <20s, breakdown <2s for 200 expenses
- [ ] T118 Test concurrent access: multiple browser tabs, verify optimistic locking prevents lost updates
- [ ] T119 Verify data persistence: restart Docker containers, confirm budgets/expenses persist
- [ ] T120 Final validation: Execute all 4 user story acceptance scenarios from spec.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4)
- **Polish (Phase 7)**: Depends on at least US1-US2 being complete for core functionality

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Depends on Budget entity from US1 but adds independent expense tracking
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on Expense entity from US2 to add category relationship
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Aggregates data from US1-US3 for analytics

### Within Each User Story

- Backend before frontend (APIs must exist before UI calls them)
- Core services before controllers
- DTOs and exceptions before usage in services/controllers
- Components before pages that use them

### Parallel Opportunities

- **Phase 1 (Setup)**: All 3 migration tasks (T001-T003) can run in parallel
- **Phase 2 (Foundational)**: Most tasks marked [P] can run in parallel (entities, DTOs, exceptions, repositories - 13 of 13 tasks)
- **User Story 1**: 8 of 21 tasks marked [P] can run in parallel (backend service, controller separate from frontend components)
- **User Story 2**: 7 of 24 tasks marked [P] can run in parallel
- **User Story 3**: 7 of 22 tasks marked [P] can run in parallel
- **User Story 4**: 3 of 16 tasks marked [P] can run in parallel
- **Phase 7 (Polish)**: 15 of 20 tasks marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members after Foundational is complete

---

## Parallel Example: User Story 1 Backend + Frontend

```bash
# Backend team (can work in parallel)
Task T018: Implement BudgetService
Task T019: Implement BudgetController

# Frontend team (can work in parallel)
Task T025: Create budgetService.ts
Task T026: Create BudgetForm component
Task T027: Create BudgetCard component
Task T028: Create BudgetSummary component

# Then sequential integration tasks
Task T029: Create budget list page (needs T025, T027, T028)
Task T030: Create new budget page (needs T025, T026)
Task T031: Create budget detail page (needs T025, T028)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (4 tasks)
2. Complete Phase 2: Foundational (13 tasks) - CRITICAL blocking phase
3. Complete Phase 3: User Story 1 (21 tasks)
4. **STOP and VALIDATE**: Run T035-T038 manual tests
5. This is the MVP - users can now create and view budgets

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (17 tasks)
2. Add User Story 1 → Test independently → **MVP delivered** (38 tasks total)
3. Add User Story 2 → Test independently → Expense tracking enabled (62 tasks total)
4. Add User Story 3 → Test independently → Category management enabled (84 tasks total)
5. Add User Story 4 → Test independently → Analytics/dashboard enabled (100 tasks total)
6. Add Polish → Production-ready feature (120 tasks total)
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (17 tasks)
2. Once Foundational is done:
   - Developer A: User Story 1 (Backend focus)
   - Developer B: User Story 1 (Frontend focus)
   - Or split: Developer A: US1, Developer B: US2, Developer C: US3
3. Stories complete and integrate independently
4. Team converges on Polish phase together

---

## Task Summary

- **Total Tasks**: 120
- **Phase 1 (Setup)**: 4 tasks (3 parallelizable)
- **Phase 2 (Foundational)**: 13 tasks (all parallelizable)
- **Phase 3 (US1 - Create/View Budgets)**: 21 tasks (8 parallelizable) 🎯 MVP
- **Phase 4 (US2 - Record Expenses)**: 24 tasks (7 parallelizable)
- **Phase 5 (US3 - Manage Categories)**: 22 tasks (7 parallelizable)
- **Phase 6 (US4 - Dashboard/Insights)**: 16 tasks (3 parallelizable)
- **Phase 7 (Polish)**: 20 tasks (15 parallelizable)
- **Parallel Opportunities**: 58 of 120 tasks (48%) can run in parallel
- **MVP Scope**: 38 tasks (Setup + Foundational + US1)

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests are NOT included per constitution Principle VI (not explicitly requested in spec)
- Manual testing via acceptance scenarios in spec.md and quickstart.md
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Use quickstart.md as integration test suite for final validation
