# Implementation Plan: Expense Server-Side Filtering

**Branch**: `021-expense-server-filtering` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/021-expense-server-filtering/spec.md`

## Summary

Replace client-side category filtering on the expenses page with server-side filtering by adding a `categoryIds` query parameter (comma-separated list of IDs) to the existing `/api/expenses/list` endpoint. The backend already has repository methods that accept a `List<Long> categoryIds` — the work is wiring a new controller parameter through the service layer and updating the frontend to send category IDs as a server-side filter instead of fetching all data and filtering in the browser.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Next.js 14.x, Material-UI v5, Axios
**Storage**: MySQL 8.0 (existing database with expenses, categories tables)
**Testing**: N/A (tests not requested)
**Target Platform**: Private home network, containerized
**Project Type**: Web application (frontend + backend)
**Performance Goals**: Standard page load time; server-side filtering must be comparable to existing single-category filter
**Constraints**: Backward compatibility with existing `categoryId` single-value parameter
**Scale/Scope**: Under 100 categories, household-scale expense data

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | spec.md written and clarified before planning |
| II. Clarify Before Planning | PASS | /speckit.clarify ran, no critical ambiguities found |
| III. Incremental Story-Based | PASS | 3 user stories with P1/P2 priorities |
| IV. Constitution Gates | PASS | This check |
| V. Task Traceability | N/A | Tasks not yet generated |
| VI. Test-Optional | PASS | Tests not requested |
| VII. Artifact Consistency | N/A | /speckit.analyze runs after tasks |
| Frontend: Next.js | PASS | Using existing Next.js frontend |
| Backend: Spring Boot (Java) | PASS | Using existing Spring Boot backend |
| Auth: X-Hass-User header | PASS | No auth changes needed |
| Deployment: Private network | PASS | No deployment changes |

## Project Structure

### Documentation (this feature)

```text
specs/021-expense-server-filtering/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── expense-list-api.yaml
└── tasks.md             # Phase 2 output (not yet created)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── controller/ExpenseController.java    # Add categoryIds parameter
│   ├── service/ExpenseService.java          # Route categoryIds to repository
│   └── repository/ExpenseRepository.java    # Already has multi-category methods
│
budget-frontend/
├── src/
│   ├── app/expenses/page.tsx                # Remove client-side filtering logic
│   ├── services/expenseService.ts           # Add categoryIds to filters + API call
│   └── components/
│       ├── expenses/ExpenseFilters.tsx       # Wire chip selection to filters
│       └── CategoryChipFilter.tsx           # No changes needed
```

**Structure Decision**: Existing web application structure. Changes span both backend (controller + service) and frontend (page + service + filters).

## Implementation Approach

### Backend Changes

1. **ExpenseController.java**: Add `@RequestParam(required = false) List<Long> categoryIds` parameter to the `/list` endpoint. Pass it to the service.

2. **ExpenseService.getExpenseList()**: When `categoryIds` is provided and non-empty, use it directly (no `expandCategoryIds` call). When only `categoryId` is provided, use existing `expandCategoryIds` logic. When neither is provided, no category filter.

3. **ExpenseRepository**: No changes needed. `findByFiltersPageableWithCategoryIds()` and `getFilteredTotalAmountWithCategoryIds()` already accept `List<Long>`.

### Frontend Changes

1. **expenseService.ts**: Add `categoryIds?: number[]` to `ExpenseListFilters`. In `getExpenseList()`, append `categoryIds` as comma-separated values to the query string.

2. **expenses/page.tsx**: Remove all client-side filtering logic (`filteredData` memo, `hasCategoryFilter`, `prevHasCategoryFilter` ref, fetch-all mode). Instead, include `selectedCategoryIds` in `filters` as `categoryIds` array and let the server handle everything.

3. **ExpenseFilters.tsx**: When `selectedCategoryIds` changes, include the IDs in the filter change callback so they flow to the server request. The `CategoryChipFilter` component itself needs no changes.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
