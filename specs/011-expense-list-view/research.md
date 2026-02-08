# Research: Expense List View

**Feature**: 011-expense-list-view
**Date**: 2026-02-09

## Decision 1: Server-Side Pagination Approach

**Decision**: Use Spring Data JPA `Pageable` with `Page<T>` return type for paginated queries.

**Rationale**: Spring Data JPA has built-in pagination support via `Pageable` parameter in repository methods. The existing `ExpenseRepository` extends `JpaRepository` which already supports `Page<T> findAll(Specification<T>, Pageable)`. This avoids adding external pagination libraries. The `Page` object provides `totalElements`, `totalPages`, `content`, `number`, `size` — exactly what the frontend needs.

**Alternatives considered**:
- **Manual LIMIT/OFFSET queries**: More verbose, doesn't provide total count in same query. Rejected — unnecessary complexity.
- **Keyset pagination (cursor-based)**: Better for infinite scroll. Rejected — spec requires page-based navigation with 50 items/page.
- **Spring Data JPA Specification API**: Provides programmatic query building. Considered alongside `@Query` approach but the existing `findByFilters` pattern with `@Query` is simpler for this use case.

## Decision 2: Dynamic Filtering Strategy

**Decision**: Enhance the existing `@Query`-based `findByFilters` method to accept `Pageable` and add amount range parameters. Use a new overloaded method signature returning `Page<Expense>`.

**Rationale**: The existing `findByFilters` method in `ExpenseRepository` already handles dynamic null-safe filtering for budgetId, categoryId, date range, and createdBy. Adding `minAmount`/`maxAmount` parameters and a `Pageable` parameter is a minimal extension. The `@Query` approach with `IS NULL OR` pattern is well-established in this codebase and readable.

**Alternatives considered**:
- **JPA Specification API (Criteria API)**: More flexible for dynamic filters but introduces new patterns not used elsewhere in the codebase. Rejected — consistency with existing approach.
- **QueryDSL**: Powerful type-safe queries but requires adding a new dependency and build plugin. Rejected — overkill for this use case.
- **Multiple separate repository methods**: Current approach in `ExpenseService.getAllExpenses()` uses if/else chains for different filter combinations. Rejected — doesn't scale with new filter parameters.

## Decision 3: Summary Aggregation (Count + Total Amount)

**Decision**: Add a separate `@Query` method for aggregating count and sum, using the same filter parameters. Return as a lightweight DTO alongside the paginated results.

**Rationale**: `Page.getTotalElements()` provides the count, but the total sum across all matching expenses (not just the current page) requires a separate aggregate query. A single repository method returning `[count, sum]` as `Object[]` (or a projection) keeps it efficient — one extra query per filter change. This mirrors the existing `sumAmountByBudgetId` pattern in the repository.

**Alternatives considered**:
- **Calculate sum from `Page.getContent()`**: Only sums the current page, not all matching expenses. Rejected — misleading for users.
- **Fetch all matching expenses and compute client-side**: Defeats purpose of pagination. Rejected.
- **Database view or stored procedure**: Too complex for household-scale data. Rejected.

## Decision 4: Year/Month Filter Implementation

**Decision**: Convert year and month parameters to `startDate`/`endDate` range on the backend, reusing the existing date range filter infrastructure.

**Rationale**: The existing `findByFilters` already supports `startDate`/`endDate`. Converting year=2026, month=3 to startDate=2026-03-01, endDate=2026-03-31 is straightforward and avoids adding new JPQL conditions. Year-only filter becomes startDate=2026-01-01, endDate=2026-12-31.

**Alternatives considered**:
- **EXTRACT(YEAR FROM e.expenseDate)**: Database-specific function, less portable. Rejected.
- **Separate year/month columns on Expense entity**: Would require schema migration for existing data. Rejected.

## Decision 5: Available Years for Year Filter

**Decision**: Add a new lightweight endpoint `GET /api/expenses/years` that returns distinct years from expense data, used to populate the year dropdown.

**Rationale**: The frontend needs to know which years have expenses to populate the year filter. A dedicated endpoint with `SELECT DISTINCT YEAR(e.expense_date) FROM expenses ORDER BY 1 DESC` is efficient and cacheable. The current year is always included (handled by frontend).

**Alternatives considered**:
- **Hardcoded year range (e.g., 2020-current)**: Shows empty years. Rejected — clutters UI for new installations.
- **Frontend calculates from first expense**: Requires fetching data first. Rejected — chicken-and-egg problem.

## Decision 6: Distinct Creators for Filter

**Decision**: Add a new lightweight endpoint `GET /api/expenses/creators` that returns distinct `createdBy` values from expense data.

**Rationale**: The "created by" filter dropdown needs to know which household members have expenses. A simple `SELECT DISTINCT created_by FROM expenses` query is efficient and reuses existing auth patterns.

**Alternatives considered**:
- **Fetch from Home Assistant user API**: Adds external dependency and complexity. Rejected.
- **Return creators as metadata in paginated response**: Mixes concerns. Rejected.

## Decision 7: Frontend Table Component

**Decision**: Use Material-UI's `Table`, `TableSortLabel`, and `TablePagination` components for the expense list table.

**Rationale**: Material-UI v5 is already installed (`@mui/material: ^5.14.20`). The built-in Table components provide sortable headers (`TableSortLabel`), pagination (`TablePagination`), and responsive support out of the box. This is consistent with the existing UI patterns (Material-UI throughout).

**Alternatives considered**:
- **MUI X DataGrid**: Full-featured data grid but adds a new dependency (`@mui/x-data-grid`). Rejected — overkill for this feature and introduces dependency.
- **Custom table implementation**: More control but significantly more code. Rejected — Material-UI Table covers all requirements.
- **AG Grid or React Table**: External libraries not in use. Rejected — consistency with Material-UI.

## Decision 8: Sort State Persistence

**Decision**: Maintain sort state in React component state (URL query params not required). Default sort: date descending.

**Rationale**: The expense list is a single-page view. Sort and filter state can live in React state. URL persistence (query params) is a nice-to-have but adds complexity not requested in the spec. The default sort (date descending) matches the current backend default and user expectations.

**Alternatives considered**:
- **URL query params for all filter/sort state**: Enables shareable links and back-button behavior. Rejected as scope creep — can be added later.
- **localStorage persistence**: Remembers user preferences across sessions. Rejected — not in spec requirements.
