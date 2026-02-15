# Tasks: Parent Category Budget Auto-Rollup

**Input**: Design documents from `/specs/017-parent-budget-rollup/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

**Tests**: Backend unit and integration tests included in this feature (from quickstart.md scenarios)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `budget-backend/src/`, `budget-frontend/src/`
- Backend: Java 17 + Spring Boot 3.2.0
- Frontend: TypeScript 5.x + Next.js 14.x

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify project structure supports automatic parent budget rollup (no schema changes needed)

- [X] T001 Verify existing Budget and Category entities support parent-child relationships in budget-backend/src/main/java/com/homebudget/model/
- [X] T002 [P] Review BudgetService architecture for cascade logic integration in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T003 [P] Review BudgetRepository query methods for parent budget lookup in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java

**Checkpoint**: Foundation verified - no schema changes needed, existing architecture supports cascade logic

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core cascade infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Add repository method findByCategoryAndYearAndMonth() to BudgetRepository in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java
- [X] T005 [P] Add repository method findByParentCategoryId() to CategoryRepository if not exists in budget-backend/src/main/java/com/homebudget/repository/CategoryRepository.java
- [X] T006 Implement private cascadeToParentBudget() method in BudgetService with @Transactional in budget-backend/src/main/java/com/homebudget/service/BudgetService.java

**Checkpoint**: Foundation ready - cascade infrastructure implemented, user story implementation can now begin

---

## Phase 3: User Story 1 - Automatic Parent Budget Creation (Priority: P1) 🎯 MVP

**Goal**: When a child category budget is created, automatically create or update the parent category budget with the same amount for the same period.

**Independent Test**: Create a budget for a child category (e.g., $500 for "Fresh Produce" in January 2026). Verify that a parent category budget for "Groceries" is automatically created with $500 for January 2026.

### Backend Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T007 [P] [US1] Unit test: createChildBudget_autoCreatesParentBudget() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [ ] T008 [P] [US1] Unit test: createSecondChildBudget_updatesParentBudgetAmount() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [ ] T009 [P] [US1] Unit test: createYearlyChildBudget_createsYearlyParentBudget() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [ ] T010 [P] [US1] Integration test: findByCategoryAndYearAndMonth_returnsParentBudget() in budget-backend/src/test/java/com/homebudget/repository/BudgetRepositoryTest.java

### Backend Implementation for User Story 1

- [X] T011 [US1] Modify createBudget() to call cascadeToParentBudget() after saving child budget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T012 [US1] Implement parent budget creation logic (find or create) within cascadeToParentBudget() in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T013 [US1] Add error handling and transaction rollback for cascade failures in budget-backend/src/main/java/com/homebudget/service/BudgetService.java

### Frontend Verification for User Story 1

- [X] T014 [US1] Verify budget creation form works without changes (cascade is transparent) in budget-frontend/src/app/budgets/new/page.tsx
- [X] T015 [US1] Manual test: Create child budget via UI and verify parent budget auto-created per quickstart.md Scenario 1

**Checkpoint**: At this point, creating a child budget should automatically create/update parent budget. Tests pass. MVP complete.

---

## Phase 4: User Story 2 - Correct Total Budget Calculation (Priority: P1)

**Goal**: The yearly budget view displays the total budget by summing only parent category budgets (not child budgets) to avoid double-counting.

**Independent Test**: Create budgets for parent "Groceries" ($1000) and children "Fresh Produce" ($500) and "Pantry" ($500). In the yearly budget view, verify the total budget counts only the parent $1000, not $1000 + $500 + $500 = $1800.

### Backend Tests for User Story 2

- [X] T016 [P] [US2] Unit test: getYearlyBudgetView_excludesChildCategoryBudgets() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [X] T017 [P] [US2] Unit test: getYearlyBudgetView_includesStandaloneCategoryBudgets() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [X] T018 [P] [US2] Unit test: getYearlyBudgetView_calculatesCorrectTotal() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java

### Backend Implementation for User Story 2

- [X] T019 [US2] Modify getYearlyBudgetView() to filter budgets by category hierarchy in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T020 [US2] Implement logic to exclude child category budgets (parent_category_id != NULL) from yearly total calculation in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T021 [US2] Ensure standalone categories (no parent, no children) are included in yearly total in budget-backend/src/main/java/com/homebudget/service/BudgetService.java

### Frontend Verification for User Story 2

- [X] T022 [US2] Verify yearly budget view displays correct filtered budget list in budget-frontend/src/app/budgets/page.tsx
- [X] T023 [US2] Manual test: Verify yearly view total excludes child budgets per quickstart.md Scenario 4

**Checkpoint**: At this point, yearly budget view correctly sums only parent + standalone budgets (no double-counting). Tests pass.

---

## Phase 5: User Story 3 - Budget Update Propagation (Priority: P2)

**Goal**: When a child category budget is updated or deleted, the parent category budget automatically adjusts to reflect the change.

**Independent Test**: Create a child budget of $500, then update it to $700. Verify the parent budget increases by $200. Then delete the child budget and verify the parent budget decreases by $700 (but persists at zero).

### Backend Tests for User Story 3

- [X] T024 [P] [US3] Unit test: updateChildBudget_adjustsParentByDelta() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [X] T025 [P] [US3] Unit test: deleteChildBudget_decreasesParentAmount() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [X] T026 [P] [US3] Unit test: deleteLastChildBudget_persistsParentAtZero() in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java

### Backend Implementation for User Story 3

- [X] T027 [US3] Modify updateBudget() to calculate delta and call cascadeToParentBudget() with delta in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T028 [US3] Modify deleteBudget() to call cascadeToParentBudget() with negative amount before deleting child in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T029 [US3] Ensure parent budget persists even when amount reaches zero (not deleted) in budget-backend/src/main/java/com/homebudget/service/BudgetService.java

### Frontend Verification for User Story 3

- [X] T030 [US3] Verify budget edit form triggers cascade (transparent to user) in budget-frontend/src/app/budgets/[id]/edit/page.tsx
- [X] T031 [US3] Manual test: Update child budget and verify parent adjusts per quickstart.md Scenario 5
- [X] T032 [US3] Manual test: Delete child budget and verify parent decreases per quickstart.md Scenario 6

**Checkpoint**: All user stories should now be independently functional. Budget create/update/delete operations all cascade to parent budgets.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T033 [P] Add logging for cascade operations (debug level) in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [X] T034 [P] Verify error messages for cascade failures are user-friendly in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [X] T035 Run all backend unit tests and verify coverage: ./mvnw test in budget-backend/
- [X] T036 [P] Manual test: Verify transaction atomicity (cascade failure rolls back child) per quickstart.md Scenario 9
- [X] T037 [P] Manual test: Verify manual parent budget + auto-rollup coexist per quickstart.md Scenario 7
- [X] T038 [P] Manual test: Verify standalone categories work normally per quickstart.md Scenario 8
- [X] T039 Code review: Verify cascadeToParentBudget() method is clean and well-documented
- [X] T040 Update CLAUDE.md if any new patterns or conventions established

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Story 1 (P1) can proceed first (MVP)
  - User Story 2 (P1) depends on User Story 1 (needs parent budgets to exist for filtering)
  - User Story 3 (P2) depends on User Story 1 (needs cascade infrastructure from US1)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1) - MVP**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Depends on User Story 1 (requires parent budgets to be auto-created before filtering can be tested)
- **User Story 3 (P2)**: Depends on User Story 1 (requires cascade infrastructure for update/delete operations)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Backend implementation before frontend verification
- Core cascade logic before error handling
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T002, T003)
- All Foundational tasks marked [P] can run in parallel (T005)
- All tests for a user story marked [P] can run in parallel
- Tests within US1: T007, T008, T009, T010 can run in parallel
- Tests within US2: T016, T017, T018 can run in parallel
- Tests within US3: T024, T025, T026 can run in parallel
- Polish tasks: T033, T034, T036, T037, T038 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test: createChildBudget_autoCreatesParentBudget()"
Task: "Unit test: createSecondChildBudget_updatesParentBudgetAmount()"
Task: "Unit test: createYearlyChildBudget_createsYearlyParentBudget()"
Task: "Integration test: findByCategoryAndYearAndMonth_returnsParentBudget()"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T006) - CRITICAL: blocks all stories
3. Complete Phase 3: User Story 1 (T007-T015)
4. **STOP and VALIDATE**: Test User Story 1 independently per quickstart.md Scenarios 1-3
5. Deploy/demo MVP: Child budgets now auto-create/update parent budgets

### Incremental Delivery

1. Complete Setup + Foundational → Cascade infrastructure ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP: auto-create parent budgets)
3. Add User Story 2 → Test independently → Deploy/Demo (yearly view correct totals)
4. Add User Story 3 → Test independently → Deploy/Demo (update/delete propagation)
5. Polish → Final validation → Full feature release

### Sequential Implementation (Recommended)

User stories have dependencies, so sequential implementation is recommended:

1. Team completes Setup + Foundational together (T001-T006)
2. Implement User Story 1 (T007-T015) → MVP checkpoint
3. Implement User Story 2 (T016-T023) → Depends on US1 parent budgets
4. Implement User Story 3 (T024-T032) → Depends on US1 cascade infrastructure
5. Polish (T033-T040) → Final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- User Story 2 depends on User Story 1 (needs parent budgets to filter)
- User Story 3 depends on User Story 1 (needs cascade infrastructure)
- Verify tests fail before implementing (TDD approach)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- **No schema changes**: Existing Budget and Category entities support this feature
- **Service-layer only**: All logic in BudgetService, no database triggers
- **Transaction safety**: Spring @Transactional ensures atomicity (child + parent succeed or fail together)
