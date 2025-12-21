# Tasks: Hierarchical Category Budgets

**Input**: Design documents from `/specs/004-hierarchical-category-budgets/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contract.md, quickstart.md

**Tests**: Tests are OPTIONAL per Principle VI (Test-Optional). Specification does not explicitly request automated tests, so test tasks are NOT included in this list.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `budget-backend/src/main/java/com/homebudget/`
- **Frontend**: `budget-frontend/src/`
- **Database**: `budget-backend/src/main/resources/db/changelog/changes/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema changes and exception infrastructure

- [X] T001 Create Liquibase migration changeset for category hierarchy in budget-backend/src/main/resources/db/changelog/changes/004-add-category-hierarchy.xml
- [X] T002 [P] Create ParentBudgetMismatchException in budget-backend/src/main/java/com/homebudget/exception/ParentBudgetMismatchException.java
- [X] T003 [P] Create CircularCategoryException in budget-backend/src/main/java/com/homebudget/exception/CircularCategoryException.java
- [X] T004 [P] Create CategoryInUseException in budget-backend/src/main/java/com/homebudget/exception/CategoryInUseException.java
- [X] T005 [P] Create HierarchyDepthException in budget-backend/src/main/java/com/homebudget/exception/HierarchyDepthException.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entity and repository extensions that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 Extend Category entity with parentCategory relationship in budget-backend/src/main/java/com/homebudget/model/Category.java
- [X] T007 Extend Budget entity with category relationship in budget-backend/src/main/java/com/homebudget/model/Budget.java
- [X] T008 [P] Extend CategoryDTO with parentCategoryId and children fields in budget-backend/src/main/java/com/homebudget/dto/CategoryDTO.java
- [X] T009 [P] Extend BudgetDTO with categoryId and category fields in budget-backend/src/main/java/com/homebudget/dto/BudgetDTO.java
- [X] T010 Add hierarchy query methods to CategoryRepository in budget-backend/src/main/java/com/homebudget/repository/CategoryRepository.java
- [X] T011 Add category filtering and sum calculation methods to BudgetRepository in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Hierarchical Category Management (Priority: P1) 🎯 MVP

**Goal**: Enable users to create parent-child category relationships with validation for circular references and hierarchy depth

**Independent Test**: Create categories with and without parents, view category hierarchy, verify parent-child relationships persist correctly, attempt circular references and verify rejection

### Implementation for User Story 1

- [ ] T012 [US1] Add circular reference detection logic to CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T013 [US1] Add hierarchy depth validation (2-level max) to CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T014 [US1] Add parent assignment validation (system category protection) to CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T015 [US1] Add child category check for deletion prevention in CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T016 [US1] Update createCategory method to accept parentCategoryId in CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T017 [US1] Update updateCategory method to handle parent changes with validation in CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T018 [US1] Update deleteCategory method to check for children and budgets in CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T019 [US1] Add getCategoryHierarchy method returning root categories with children in CategoryService in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T020 [US1] Update CategoryController POST endpoint to accept parentCategoryId in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T021 [US1] Update CategoryController PUT endpoint to handle parent changes in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T022 [US1] Update CategoryController DELETE endpoint to return detailed error for children/budgets in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T023 [US1] Add GET /api/categories/hierarchy endpoint in CategoryController in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T024 [P] [US1] Update Category TypeScript type with parentId and children fields in budget-frontend/src/types/category.ts
- [ ] T025 [P] [US1] Add getCategoryHierarchy API call to categoryService in budget-frontend/src/services/categoryService.ts
- [ ] T026 [US1] Update CategoryForm component to include parent category dropdown in budget-frontend/src/components/CategoryForm.tsx
- [ ] T027 [US1] Update CategoryList component to display hierarchy with nesting using TreeView in budget-frontend/src/components/CategoryList.tsx

**Checkpoint**: At this point, User Story 1 should be fully functional - users can create hierarchies, system prevents circular references and depth violations

---

## Phase 4: User Story 2 - Category-Based Budget Creation (Priority: P1) 🎯 MVP

**Goal**: Enable users to assign budgets to specific categories for tracking spending per category

**Independent Test**: Create budgets for different categories and months, verify each budget is correctly associated with its category and unique per (category, year, month)

### Implementation for User Story 2

- [ ] T028 [US2] Add category existence validation to BudgetService.createBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T029 [US2] Add category requirement validation (non-null) for new budgets in BudgetService in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T030 [US2] Add unique constraint validation for (category, year, month) in BudgetService in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T031 [US2] Update BudgetService.createBudget to populate category relationship in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T032 [US2] Update BudgetController POST endpoint to require categoryId in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T033 [US2] Update BudgetController GET endpoint to return budgets with category details in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T034 [US2] Add category filtering query parameter to BudgetController GET endpoint in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T035 [P] [US2] Update Budget TypeScript type with categoryId and category fields in budget-frontend/src/types/budget.ts
- [ ] T036 [P] [US2] Update budgetService API calls to include category in budget-frontend/src/services/budgetService.ts
- [ ] T037 [US2] Update BudgetForm component to include category selection dropdown (required field) in budget-frontend/src/components/BudgetForm.tsx
- [ ] T038 [US2] Update BudgetList component to display category name with each budget in budget-frontend/src/components/BudgetList.tsx

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - users can create categories and assign budgets to them

---

## Phase 5: User Story 3 - Parent Budget Validation (Priority: P2)

**Goal**: Enforce parent budget must equal sum of child category budgets to ensure hierarchical budget consistency

**Independent Test**: Create parent and child budgets, verify validation enforces sum constraint, attempt mismatches and verify rejection with detailed error messages

### Implementation for User Story 3

- [ ] T039 [US3] Add child budget sum calculation method to BudgetRepository in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java
- [ ] T040 [US3] Add parent budget validation logic to BudgetService.validateParentBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T041 [US3] Add child budget validation logic to BudgetService.validateChildBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T042 [US3] Integrate parent-child validation into BudgetService.createBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T043 [US3] Integrate parent-child validation into BudgetService.updateBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T044 [US3] Add warning header for missing parent budget in BudgetController in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T045 [US3] Update BudgetController error responses to include detailed parent-child mismatch info in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T046 [P] [US3] Add parent-child validation error display to BudgetForm in budget-frontend/src/components/BudgetForm.tsx
- [ ] T047 [P] [US3] Add child sum pre-fill helper when creating parent budget in BudgetForm in budget-frontend/src/components/BudgetForm.tsx
- [ ] T048 [US3] Add warning notification display for missing parent budget in budget-frontend/src/components/BudgetForm.tsx

**Checkpoint**: All P1 and P2 user stories are complete - hierarchical budgeting with validation is fully functional

---

## Phase 6: User Story 4 - Category Budget Requirement (Priority: P2)

**Goal**: Ensure all budgets are properly categorized by making category selection mandatory

**Independent Test**: Attempt to create budgets with and without categories, verify validation works correctly, attempt to delete categories with budgets and verify prevention

### Implementation for User Story 4

- [ ] T049 [US4] Add budget existence check to CategoryService.deleteCategory in budget-backend/src/main/java/com/homebudget/service/CategoryService.java
- [ ] T050 [US4] Add category immutability validation to BudgetService.updateBudget in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T051 [US4] Update CategoryController DELETE error response to include budget count in budget-backend/src/main/java/com/homebudget/controller/CategoryController.java
- [ ] T052 [P] [US4] Update CategoryForm to show warning when attempting to delete category with budgets in budget-frontend/src/components/CategoryForm.tsx
- [ ] T053 [P] [US4] Add validation error display for required category field in BudgetForm in budget-frontend/src/components/BudgetForm.tsx

**Checkpoint**: Category requirement enforcement complete - all budgets must have categories, categories with budgets cannot be deleted

---

## Phase 7: User Story 5 - Budget Summary and Reporting (Priority: P3)

**Goal**: Provide hierarchical budget summaries showing individual category budgets and rolled-up parent totals

**Independent Test**: Create category hierarchy with budgets, verify summary displays correct amounts and calculations for both child and parent categories

### Implementation for User Story 5

- [ ] T054 [US5] Add hierarchical budget summary query to BudgetRepository in budget-backend/src/main/java/com/homebudget/repository/BudgetRepository.java
- [ ] T055 [US5] Add getBudgetSummaryByMonth method to BudgetService in budget-backend/src/main/java/com/homebudget/service/BudgetService.java
- [ ] T056 [US5] Add GET /api/budgets/summary endpoint to BudgetController in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T057 [P] [US5] Create BudgetSummary TypeScript type for hierarchical display in budget-frontend/src/types/budget.ts
- [ ] T058 [P] [US5] Add getBudgetSummary API call to budgetService in budget-frontend/src/services/budgetService.ts
- [ ] T059 [US5] Create BudgetSummary component with hierarchical view using TreeView in budget-frontend/src/components/BudgetSummary.tsx
- [ ] T060 [US5] Add expand/collapse functionality for parent categories in BudgetSummary in budget-frontend/src/components/BudgetSummary.tsx
- [ ] T061 [US5] Add spending amount and remaining budget display per category in BudgetSummary in budget-frontend/src/components/BudgetSummary.tsx

**Checkpoint**: All user stories (P1-P3) are complete - full hierarchical category budgeting with reporting is functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T062 [P] Add optimistic locking conflict handling to BudgetController in budget-backend/src/main/java/com/homebudget/controller/BudgetController.java
- [ ] T063 [P] Add global exception handler for custom exceptions in budget-backend/src/main/java/com/homebudget/exception/GlobalExceptionHandler.java
- [ ] T064 [P] Add detailed logging for validation failures in CategoryService and BudgetService in budget-backend/src/main/java/com/homebudget/service/
- [ ] T065 [P] Add client-side validation for category hierarchy in CategoryForm in budget-frontend/src/components/CategoryForm.tsx
- [ ] T066 [P] Add client-side validation for budget amounts in BudgetForm in budget-frontend/src/components/BudgetForm.tsx
- [ ] T067 [P] Add loading states and error handling to all frontend components in budget-frontend/src/components/
- [ ] T068 Run integration tests from quickstart.md Scenario 1-12 to validate all user stories
- [ ] T069 Performance test category hierarchy rendering with 100 categories per quickstart.md
- [ ] T070 Performance test budget sum validation with 20 child budgets per quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User Story 1 (P1): Can start after Foundational - No dependencies on other stories
  - User Story 2 (P1): Can start after Foundational - Integrates with US1 but independently testable
  - User Story 3 (P2): Depends on US1 and US2 (needs categories and budgets to exist)
  - User Story 4 (P2): Depends on US2 (needs budgets to exist)
  - User Story 5 (P3): Depends on US1, US2, US3 (needs full hierarchy with budgets)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Requires US1 categories to exist for budget assignment
- **User Story 3 (P2)**: Depends on US1 and US2 - Validates parent-child budget relationships
- **User Story 4 (P2)**: Depends on US2 - Enforces category requirement for budgets
- **User Story 5 (P3)**: Depends on US1, US2, US3 - Displays hierarchical budget summaries with validation

### Within Each User Story

- Backend entity extensions before service logic
- Service validation logic before controller endpoints
- Repository query methods before service usage
- Backend API endpoints before frontend integration
- TypeScript types before components
- API service calls before component usage
- Core components before enhanced components

### Parallel Opportunities

**Phase 1 (Setup)**:
- T002, T003, T004, T005 can run in parallel (different exception files)

**Phase 2 (Foundational)**:
- T008, T009 can run in parallel (different DTO files)
- After T006, T007: T010 and T011 can run in parallel (different repository files)

**Phase 3 (User Story 1)**:
- T024, T025 can run in parallel (different TypeScript files)

**Phase 4 (User Story 2)**:
- T035, T036 can run in parallel (different TypeScript files)

**Phase 5 (User Story 3)**:
- T046, T047 can run in parallel (same file but independent changes)

**Phase 6 (User Story 4)**:
- T052, T053 can run in parallel (different component files)

**Phase 7 (User Story 5)**:
- T057, T058 can run in parallel (different TypeScript files)

**Phase 8 (Polish)**:
- T062, T063, T064, T065, T066, T067 can all run in parallel (different files)

---

## Parallel Example: User Story 1

```bash
# Launch parallel tasks for User Story 1:

# Backend parallel work (after foundational complete):
Task T012-T019: "Implement CategoryService validation methods" (same file, sequential)
Task T020-T023: "Update CategoryController endpoints" (same file, sequential)

# Frontend parallel work (after backend API ready):
Task T024: "Update Category TypeScript type in budget-frontend/src/types/category.ts"
Task T025: "Add getCategoryHierarchy API call in budget-frontend/src/services/categoryService.ts"
# Then sequential:
Task T026: "Update CategoryForm component"
Task T027: "Update CategoryList component"
```

---

## Parallel Example: User Story 2

```bash
# Launch parallel tasks for User Story 2:

# Backend parallel work:
Task T028-T031: "Implement BudgetService validation" (same file, sequential)
Task T032-T034: "Update BudgetController endpoints" (same file, sequential)

# Frontend parallel work (after backend API ready):
Task T035: "Update Budget TypeScript type in budget-frontend/src/types/budget.ts"
Task T036: "Update budgetService API calls in budget-frontend/src/services/budgetService.ts"
# Then sequential:
Task T037: "Update BudgetForm component"
Task T038: "Update BudgetList component"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (T001-T005) - Database schema and exceptions
2. Complete Phase 2: Foundational (T006-T011) - Entity and repository extensions
3. Complete Phase 3: User Story 1 (T012-T027) - Hierarchical categories
4. Complete Phase 4: User Story 2 (T028-T038) - Category-based budgets
5. **STOP and VALIDATE**: Test User Stories 1 and 2 independently using quickstart.md Scenarios 1-5
6. Deploy/demo if ready

### Incremental Delivery

1. **Foundation** (Phases 1-2): Setup + Foundational → Database and core entities ready
2. **MVP** (Phases 3-4): US1 + US2 → Categories and budgets working → Deploy/Demo
3. **Enhanced** (Phase 5): US3 → Parent budget validation enforced → Deploy/Demo
4. **Complete** (Phases 6-7): US4 + US5 → Category requirement + reporting → Deploy/Demo
5. **Polished** (Phase 8): Cross-cutting improvements → Final release

### Sequential Priority-Based Strategy

For single developer or small team:

1. Complete Setup + Foundational (Phases 1-2)
2. Complete User Story 1 (Phase 3) → Test independently
3. Complete User Story 2 (Phase 4) → Test independently
4. Complete User Story 3 (Phase 5) → Test independently
5. Complete User Story 4 (Phase 6) → Test independently
6. Complete User Story 5 (Phase 7) → Test independently
7. Complete Polish (Phase 8)

### Parallel Team Strategy

With multiple developers:

1. **Team completes Setup + Foundational together** (Phases 1-2)
2. **Once Foundational is done**:
   - Developer A: User Story 1 (Phase 3) - Category hierarchy
   - Developer B: Start on User Story 2 TypeScript types (T035-T036)
3. **After US1 complete**:
   - Developer A: User Story 3 (Phase 5) - Parent budget validation
   - Developer B: Complete User Story 2 (Phase 4) - Budget creation
4. **After US2 complete**:
   - Developer A: User Story 4 (Phase 6) - Category requirement
   - Developer B: User Story 5 (Phase 7) - Budget summary
5. **Both developers**: Polish (Phase 8) in parallel

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests are OPTIONAL per specification (not included in this task list)
- Use quickstart.md integration test scenarios to validate each user story
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Migration must run before any code changes (T001 first in Phase 1)
- All custom exceptions must exist before service layer uses them
- Entity extensions must complete before repository query methods
- Repository methods must exist before service layer uses them
- Backend API endpoints must work before frontend integration
- TypeScript types must be defined before components use them

---

## Task Summary

**Total Tasks**: 70
- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (User Story 1 - P1): 16 tasks
- Phase 4 (User Story 2 - P1): 11 tasks
- Phase 5 (User Story 3 - P2): 10 tasks
- Phase 6 (User Story 4 - P2): 5 tasks
- Phase 7 (User Story 5 - P3): 8 tasks
- Phase 8 (Polish): 9 tasks

**Parallel Opportunities**: 18 tasks marked [P] for parallel execution

**MVP Scope**: Phases 1-4 (38 tasks) deliver User Stories 1 and 2 - hierarchical categories and category-based budgets

**User Story Breakdown**:
- US1 (P1): 16 tasks - Hierarchical category management with validation
- US2 (P1): 11 tasks - Category-based budget creation with uniqueness
- US3 (P2): 10 tasks - Parent-child budget sum validation
- US4 (P2): 5 tasks - Category requirement enforcement
- US5 (P3): 8 tasks - Budget summary and reporting

**Independent Test Criteria**:
- US1: Create/view hierarchies, verify circular reference prevention
- US2: Create budgets per category, verify uniqueness constraint
- US3: Validate parent budget equals child sum, verify mismatch rejection
- US4: Verify category required, verify deletion prevention
- US5: View hierarchical summaries with correct calculations

**Format Validation**: ✅ All 70 tasks follow required checklist format with task ID, priority markers, story labels, and file paths
