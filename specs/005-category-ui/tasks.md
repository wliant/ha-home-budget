---
description: "Task breakdown for Category Management UI Enhancements"
---

# Tasks: Category Management UI Enhancements

**Input**: Design documents from `/specs/005-category-ui/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests are OPTIONAL and not included per feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Frontend**: `budget-frontend/src/`
- **Tests**: `budget-frontend/tests/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and component structure

- [X] T001 Create component directory structure at budget-frontend/src/app/categories/components/
- [X] T002 Create hooks directory structure at budget-frontend/src/app/categories/hooks/
- [X] T003 Create utils directory for validation at budget-frontend/src/utils/categoryValidation.ts
- [X] T004 [P] Create test directory structure at budget-frontend/tests/categories/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core utilities and services that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Implement client-side validation rules in budget-frontend/src/utils/categoryValidation.ts
- [X] T006 Enhance categoryService with updateCategory method in budget-frontend/src/services/categoryService.ts
- [X] T007 [P] Add getBudgetCount method to categoryService in budget-frontend/src/services/categoryService.ts
- [X] T008 Create shared CategoryDTO TypeScript interface in budget-frontend/src/types/category.ts
- [X] T009 Create useCategoryForm custom hook skeleton in budget-frontend/src/app/categories/hooks/useCategoryForm.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Edit Existing Categories (Priority: P1) 🎯 MVP

**Goal**: Enable users to modify category properties (name, icon, parent) through an edit dialog

**Independent Test**: Create a category, click edit, modify properties, save, verify changes persist

### Implementation for User Story 1

- [X] T010 [P] [US1] Create CategoryDialog component for create/edit modal in budget-frontend/src/app/categories/components/CategoryDialog.tsx
- [X] T011 [P] [US1] Create CategoryCard component with edit button in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T012 [US1] Implement useCategoryForm hook with validation logic in budget-frontend/src/app/categories/hooks/useCategoryForm.ts
- [X] T013 [US1] Add dialog state management to main categories page in budget-frontend/src/app/categories/page.tsx
- [X] T014 [US1] Integrate edit button click handler to open dialog in edit mode in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T015 [US1] Implement form submission handler for update operation in budget-frontend/src/app/categories/components/CategoryDialog.tsx
- [X] T016 [US1] Add error handling and success feedback for edit operation in budget-frontend/src/app/categories/page.tsx
- [X] T017 [US1] Implement parent category change validation (prevent if has budgets) in budget-frontend/src/app/categories/components/CategoryDialog.tsx

**Checkpoint**: At this point, User Story 1 should be fully functional - users can edit categories

---

## Phase 4: User Story 2 - Enhanced Category Card Information (Priority: P1)

**Goal**: Display budget count, system category status, and creation metadata on category cards

**Independent Test**: Create categories with various properties and verify all metadata displays correctly

### Implementation for User Story 2

- [X] T018 [P] [US2] Add budget count display to CategoryCard component in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T019 [P] [US2] Add system category badge to CategoryCard component in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T020 [P] [US2] Add creation metadata (created by, created date) to CategoryCard in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T021 [US2] Disable delete button for system categories in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T022 [US2] Add subcategory count display for parent categories in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T023 [US2] Format creation date display using date formatting utility in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T024 [US2] Update page.tsx to fetch budget counts for all categories in budget-frontend/src/app/categories/page.tsx

**Checkpoint**: All metadata now visible on category cards - improved information visibility

---

## Phase 5: User Story 3 - Category Search and Filtering (Priority: P2)

**Goal**: Enable users to quickly find categories via real-time search

**Independent Test**: Create 20+ categories, use search box, verify results update in real-time

### Implementation for User Story 3

- [X] T025 [P] [US3] Create CategorySearch component with TextField in budget-frontend/src/app/categories/components/CategorySearch.tsx
- [X] T026 [P] [US3] Create useCategorySearch hook with filter logic in budget-frontend/src/app/categories/hooks/useCategorySearch.ts
- [X] T027 [US3] Integrate search component into main page layout in budget-frontend/src/app/categories/page.tsx
- [X] T028 [US3] Implement search filtering logic with debouncing in budget-frontend/src/app/categories/hooks/useCategorySearch.ts
- [X] T029 [US3] Add memoization for filtered results performance in budget-frontend/src/app/categories/hooks/useCategorySearch.ts
- [X] T030 [US3] Preserve parent context when searching for child categories in budget-frontend/src/app/categories/hooks/useCategorySearch.ts
- [X] T031 [US3] Add "No categories found" empty state in budget-frontend/src/app/categories/page.tsx
- [X] T032 [US3] Add clear search button to CategorySearch component in budget-frontend/src/app/categories/components/CategorySearch.tsx

**Checkpoint**: Search functionality working - users can quickly filter large category lists

---

## Phase 6: User Story 4 - Inline Category Validation (Priority: P2)

**Goal**: Provide real-time validation feedback as users type in category forms

**Independent Test**: Attempt invalid inputs and verify validation messages appear immediately

### Implementation for User Story 4

- [X] T033 [P] [US4] Implement duplicate name validation in useCategoryForm hook in budget-frontend/src/app/categories/hooks/useCategoryForm.ts
- [X] T034 [P] [US4] Implement circular reference validation in useCategoryForm hook in budget-frontend/src/app/categories/hooks/useCategoryForm.ts
- [X] T035 [P] [US4] Implement 3-level hierarchy prevention in useCategoryForm hook in budget-frontend/src/app/categories/hooks/useCategoryForm.ts
- [X] T036 [US4] Add input sanitization for special characters in budget-frontend/src/utils/categoryValidation.ts
- [X] T037 [US4] Add name length validation (100 chars max) in budget-frontend/src/utils/categoryValidation.ts
- [X] T038 [US4] Display inline error messages in CategoryDialog form fields in budget-frontend/src/app/categories/components/CategoryDialog.tsx
- [X] T039 [US4] Implement async validation for duplicate names via API call in budget-frontend/src/app/categories/hooks/useCategoryForm.ts
- [X] T040 [US4] Disable submit button when validation errors exist in budget-frontend/src/app/categories/components/CategoryDialog.tsx

**Checkpoint**: Real-time validation working - errors caught before submission

---

## Phase 7: User Story 5 - Bulk Category Operations (Priority: P3)

**Goal**: Enable multi-select operations for deleting or changing parent of multiple categories

**Independent Test**: Select multiple categories, perform bulk action, verify applied to all

### Implementation for User Story 5

- [ ] T041 [P] [US5] Create BulkActionsBar component in budget-frontend/src/app/categories/components/BulkActionsBar.tsx
- [ ] T042 [P] [US5] Create useBulkOperations hook in budget-frontend/src/app/categories/hooks/useBulkOperations.ts
- [ ] T043 [US5] Add checkbox to CategoryCard for multi-select in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [ ] T044 [US5] Implement selection state management in useBulkOperations hook in budget-frontend/src/app/categories/hooks/useBulkOperations.ts
- [ ] T045 [US5] Add "Select All" functionality to BulkActionsBar in budget-frontend/src/app/categories/components/BulkActionsBar.tsx
- [ ] T046 [US5] Implement bulk delete with confirmation dialog in budget-frontend/src/app/categories/components/BulkActionsBar.tsx
- [ ] T047 [US5] Implement bulk parent change operation in budget-frontend/src/app/categories/components/BulkActionsBar.tsx
- [ ] T048 [US5] Add validation to prevent bulk operations on system categories in budget-frontend/src/app/categories/hooks/useBulkOperations.ts
- [ ] T049 [US5] Show partial success message when some operations fail in budget-frontend/src/app/categories/components/BulkActionsBar.tsx
- [ ] T050 [US5] Integrate BulkActionsBar into main page layout in budget-frontend/src/app/categories/page.tsx

**Checkpoint**: Bulk operations working - users can efficiently manage multiple categories

---

## Phase 8: User Story 6 - Category Usage Analytics (Priority: P3)

**Goal**: Display category spending statistics and trends for insights

**Independent Test**: Create categories with expenses/budgets, view analytics, verify accurate calculations

### Implementation for User Story 6

- [ ] T051 [P] [US6] Create CategoryAnalytics component in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T052 [P] [US6] Add analytics data fetching to categoryService in budget-frontend/src/services/categoryService.ts
- [ ] T053 [US6] Implement budget utilization display with progress bar in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T054 [US6] Add chart library integration (based on research.md decision) in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T055 [US6] Implement monthly spending trend chart in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T056 [US6] Add time range selector (3 months, 6 months, 1 year) in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T057 [US6] Implement aggregated statistics for parent categories in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T058 [US6] Add "No data available" empty state for categories without expenses in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx
- [ ] T059 [US6] Add analytics view trigger (button or modal) to CategoryCard in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [ ] T060 [US6] Lazy-load chart library to optimize bundle size in budget-frontend/src/app/categories/components/CategoryAnalytics.tsx

**Checkpoint**: Analytics working - users can view spending insights per category

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T061 [P] Add loading states and spinners for all async operations in budget-frontend/src/app/categories/page.tsx
- [X] T062 [P] Implement error recovery with retry capability in budget-frontend/src/app/categories/page.tsx
- [X] T063 [P] Add React.memo() to CategoryCard for performance in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T064 Add ARIA labels and keyboard navigation support across all components in budget-frontend/src/app/categories/
- [X] T065 [P] Add focus management for dialogs in budget-frontend/src/app/categories/components/CategoryDialog.tsx
- [X] T066 Implement view mode persistence in localStorage in budget-frontend/src/app/categories/page.tsx
- [X] T067 [P] Add toast/snackbar notifications for success/error feedback in budget-frontend/src/app/categories/page.tsx
- [X] T068 Verify WCAG AA color contrast for system category badges in budget-frontend/src/app/categories/components/CategoryCard.tsx
- [X] T069 Test with 100+ categories to verify performance targets in budget-frontend/src/app/categories/page.tsx
- [X] T070 Code cleanup and remove any console.log statements across all new files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (US1 → US2 → US3 → US4 → US5 → US6)
- **Polish (Phase 9)**: Depends on desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of US1 but builds on same CategoryCard
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Independent search functionality
- **User Story 4 (P2)**: Extends US1's form - should complete after US1 for cleaner integration
- **User Story 5 (P3)**: Independent bulk operations - requires CategoryCard from US1/US2
- **User Story 6 (P3)**: Independent analytics - can start after Foundational

### Within Each User Story

- Tasks with [P] markers can run in parallel (different files)
- Sequential tasks must complete in order (same file modifications)
- Dialog and form components before integration into main page
- Validation logic before form submission handlers

### Parallel Opportunities

#### Phase 1 - Setup
- T001, T002, T003, T004 can all run in parallel (different directories)

#### Phase 2 - Foundational
- T005, T006, T007, T008, T009 have some parallelism:
  - T005, T006, T007, T008 can run in parallel (different files)
  - T009 depends on T008 (needs CategoryDTO interface)

#### Phase 3 - User Story 1
- T010, T011 can run in parallel (different components)
- T012 can run in parallel with T010, T011 (different hook file)
- T013-T017 must run sequentially (integration work)

#### Phase 4 - User Story 2
- T018, T019, T020 can run in parallel (different sections of CategoryCard)
- T021, T022, T023 must run sequentially (builds on previous changes)
- T024 can run in parallel (different file - page.tsx)

#### Phase 5 - User Story 3
- T025, T026 can run in parallel (different files)
- T027-T032 include page integration (sequential)

#### Phase 6 - User Story 4
- T033, T034, T035 can run in parallel (different validation rules in hook)
- T036, T037 can run in parallel (different validation functions)
- T038-T040 must run sequentially (form integration)

#### Phase 7 - User Story 5
- T041, T042 can run in parallel (component + hook)
- T043-T050 have dependencies (need base components first)

#### Phase 8 - User Story 6
- T051, T052 can run in parallel (component + service)
- T053-T060 include visualization work (some sequential)

#### Phase 9 - Polish
- T061, T062, T063, T064, T065, T067, T068 can run in parallel (different concerns)
- T066, T069, T070 should run after core tasks

---

## Parallel Example: User Story 1 (Edit Categories)

```bash
# Launch components in parallel:
Task T010: "Create CategoryDialog component for create/edit modal"
Task T011: "Create CategoryCard component with edit button"
Task T012: "Implement useCategoryForm hook with validation logic"

# After parallel tasks complete, integrate sequentially:
Task T013: "Add dialog state management to main categories page"
Task T014: "Integrate edit button click handler"
Task T015: "Implement form submission handler"
Task T016: "Add error handling and success feedback"
Task T017: "Implement parent category change validation"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Edit functionality)
4. Complete Phase 4: User Story 2 (Enhanced card info)
5. **STOP and VALIDATE**: Test US1 + US2 independently
6. Deploy/demo MVP with full CRUD + improved visibility

**MVP Rationale**: US1 + US2 are both P1 priority and deliver complete category management (create, read, update, delete) with improved information display. This is a fully functional increment.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Edit functionality working → Test independently
3. Add User Story 2 → All metadata visible → Test independently → **Deploy MVP**
4. Add User Story 3 → Search working → Test independently → Deploy
5. Add User Story 4 → Validation enhanced → Test independently → Deploy
6. Add User Story 5 → Bulk operations working → Deploy (optional)
7. Add User Story 6 → Analytics available → Deploy (optional)
8. Complete Polish phase → Production-ready

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 + User Story 4 (edit + validation)
   - Developer B: User Story 2 + User Story 3 (cards + search)
   - Developer C: User Story 5 (bulk operations)
   - Developer D: User Story 6 (analytics)
3. Stories complete and integrate independently
4. Polish phase done collaboratively

---

## Notes

- [P] tasks = different files, no dependencies - can run in parallel
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- All tasks include exact file paths for clarity
- US1 + US2 form the MVP (both P1 priority)
- US3-US6 are value-add enhancements (P2 and P3)
- Tests omitted per spec (not explicitly requested)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Total: 70 tasks across 6 user stories + setup + polish
