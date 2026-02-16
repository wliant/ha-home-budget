# Tasks: Expense Server-Side Filtering

**Input**: Design documents from `/specs/021-expense-server-filtering/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/expense-list-api.yaml, quickstart.md

**Tests**: Not requested. No test tasks included.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No setup needed. Existing project structure, dependencies, and database schema are unchanged.

*(No tasks — all infrastructure already exists)*

---

## Phase 2: Foundational (Backend)

**Purpose**: Add `categoryIds` list parameter to the backend API so all frontend user stories can use server-side multi-category filtering.

**CRITICAL**: No frontend user story work can begin until this phase is complete.

- [X] T001 Add `@RequestParam(required = false) List<Long> categoryIds` parameter to the `/list` endpoint in `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java`. Pass it to `expenseService.getExpenseList()`. Update the logger line to include `categoryIds`. No other controller changes needed — existing `categoryId` parameter stays for backward compatibility.

- [X] T002 Add `List<Long> categoryIds` parameter to `getExpenseList()` in `budget-backend/src/main/java/com/homebudget/service/ExpenseService.java`. Implement precedence logic: (1) if `categoryIds` is non-null and non-empty, use it directly with `findByFiltersPageableWithCategoryIds()` and `getFilteredTotalAmountWithCategoryIds()` — skip `expandCategoryIds`; (2) if `categoryIds` is null/empty and `categoryId` is provided, use existing `expandCategoryIds()` behavior; (3) if neither provided, no category filter. Update the method signature and all call sites.

**Checkpoint**: Backend now accepts `categoryIds=1,2,3` on `/api/expenses/list`. Existing single `categoryId` parameter still works. Can be tested with curl.

---

## Phase 3: User Story 1 — Multi-Category Server-Side Filtering (Priority: P1) — MVP

**Goal**: When a user selects category chips on the expenses page, the frontend sends category IDs to the server and displays correctly filtered, paginated results.

**Independent Test**: Select multiple categories → verify expenses, total count, total amount, and pagination all come from the server and are correct.

### Implementation for User Story 1

- [X] T003 [P] [US1] Add `categoryIds?: number[]` to the `ExpenseListFilters` interface in `budget-frontend/src/services/expenseService.ts`. In `getExpenseList()`, when `filters.categoryIds` is defined and non-empty, append `categoryIds` as a comma-separated string to `URLSearchParams` (e.g., `params.append('categoryIds', filters.categoryIds.join(','))`). Do not send `categoryIds` when the array is empty or undefined.

- [X] T004 [US1] Refactor `budget-frontend/src/app/expenses/page.tsx` to replace client-side category filtering with server-side `categoryIds`:
  - **Remove**: `hasCategoryFilter` const, `prevHasCategoryFilter` ref, the `useEffect` that watches `hasCategoryFilter`, the `filteredData` useMemo, and the `fetchAll` parameter from `fetchExpenses`.
  - **Change `fetchExpenses`**: Always use `PAGE_SIZE` for size. Convert `selectedCategoryIds` Set to `number[]` and include as `categoryIds` in the request. Add `selectedCategoryIds` to the `useCallback` dependency array.
  - **Change the data `useEffect`**: Watch `[filters, page, fetchExpenses]` (remove `hasCategoryFilter`).
  - **Change `ExpenseListTable` data prop**: Pass `data` directly instead of `filteredData`.
  - **Keep**: `handleCategorySelectionChange` with `setPage(0)`, `handleFilterChange` with `setPage(0)`.

**Checkpoint**: User Story 1 fully functional. Category chip selections trigger server-side filtered requests with correct totals and pagination.

---

## Phase 4: User Story 2 — Combined Server-Side Filters (Priority: P1)

**Goal**: All filter changes (year, month, category, amount, created-by, clear) produce a single server request with all active filter parameters including `categoryIds`.

**Independent Test**: Apply year + month + categories + amount range together → verify a single server request returns correct combined results.

### Implementation for User Story 2

- [X] T005 [US2] Verify and ensure combined filter flow in `budget-frontend/src/app/expenses/page.tsx`: When `handleFilterChange` is called (year/month/amount/created-by change), the re-fetch must include current `categoryIds` from `selectedCategoryIds`. Since `fetchExpenses` captures `selectedCategoryIds` in its closure (from T004), this should work automatically. Verify that `ExpenseFilters.handleClearFilters` (which calls `onCategorySelectionChange(new Set())`) properly clears both `selectedCategoryIds` and triggers a re-fetch without `categoryIds`. If any wiring gap exists, fix it.

**Checkpoint**: All filter combinations work together server-side. Clear filters resets everything correctly.

---

## Phase 5: User Story 3 — Pagination with Server-Side Category Filter (Priority: P2)

**Goal**: Page navigation with active category filters works correctly — each page request includes `categoryIds`, and the server returns accurate page slices with correct totals.

**Independent Test**: Select categories with 100+ matching expenses → navigate pages → verify correct sequential data, total count, and total amount on each page.

### Implementation for User Story 3

- [X] T006 [US3] Verify pagination correctness in `budget-frontend/src/app/expenses/page.tsx`: Ensure `handlePageChange` triggers a re-fetch that includes `categoryIds` (automatic if `fetchExpenses` closure captures `selectedCategoryIds` from T004). Verify that `ExpenseListTable` receives server-provided `totalElements`, `totalPages`, `totalAmount`, and `currentPage` directly from the response — no client-side recomputation. Confirm `handleCategorySelectionChange` resets page to 0 (already implemented). If `handleDelete` re-fetch needs updating (currently passes `hasCategoryFilter`), update to use the new signature.

**Checkpoint**: Pagination is seamless with or without category filters active.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup.

- [X] T007 Validate all integration scenarios from `specs/021-expense-server-filtering/quickstart.md`: test multi-category filter request, no-category filter, single `categoryId` backward compatibility, and `categoryIds` precedence over `categoryId`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately. BLOCKS all frontend work.
- **US1 (Phase 3)**: Depends on Phase 2 completion. T003 and T004 are on different files so T003 is parallel, but T004 depends on T003 (needs the interface change).
- **US2 (Phase 4)**: Depends on Phase 3 (T004 must be complete).
- **US3 (Phase 5)**: Depends on Phase 3 (T004 must be complete). Can run in parallel with Phase 4.
- **Polish (Phase 6)**: Depends on all user stories being complete.

### Within Each User Story

- Backend (T001→T002) must complete before frontend (T003→T004)
- T003 (service interface) before T004 (page refactor)
- T004 completes before T005/T006 (verification tasks)

### Parallel Opportunities

```
Phase 2: T001 → T002 (sequential, same call chain)

Phase 3: T003 can start as soon as Phase 2 is done
         T004 starts after T003

Phase 4-5: T005 and T006 can run in parallel after T004
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Backend (T001, T002)
2. Complete Phase 3: Frontend core (T003, T004)
3. **STOP and VALIDATE**: Test category chip filtering with server-side results
4. Proceed to Phases 4-5 for combined filter and pagination verification

### Incremental Delivery

1. Backend changes (T001-T002) → Backend ready, backward compatible
2. Frontend service + page refactor (T003-T004) → MVP functional
3. Verify combined filters (T005) → Full filter integration
4. Verify pagination (T006) → Complete feature
5. Integration validation (T007) → Ship-ready

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No new repository methods needed — `findByFiltersPageableWithCategoryIds()` and `getFilteredTotalAmountWithCategoryIds()` already exist
- No database schema changes
- Backward compatibility preserved: existing `categoryId` single-value parameter continues to work
- T005 and T006 may require no code changes if T004 is implemented correctly — they serve as verification checkpoints
