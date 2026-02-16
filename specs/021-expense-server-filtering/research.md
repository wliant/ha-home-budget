# Research: Expense Server-Side Filtering

## Decision 1: How to pass multiple category IDs from frontend to backend

**Decision**: Use a `categoryIds` query parameter with comma-separated values (e.g., `categoryIds=1,2,3`). Spring Boot automatically binds `@RequestParam List<Long> categoryIds` from comma-separated values.

**Rationale**: This is the standard Spring MVC pattern for passing lists in query parameters. No custom parsing needed. Axios can serialize arrays as comma-separated values with `params.append` for each value or by joining manually.

**Alternatives considered**:
- Repeated parameter (`categoryIds=1&categoryIds=2&categoryIds=3`) - Also works with Spring, but less readable in logs.
- POST body for filters - Breaks REST conventions for read operations and complicates caching/bookmarking.
- Comma-separated string with manual parsing - Unnecessary; Spring handles this natively.

## Decision 2: Parameter precedence (categoryIds vs categoryId)

**Decision**: When `categoryIds` is provided and non-empty, it takes full precedence. The `categoryId` single-value parameter is ignored. When only `categoryId` is provided, existing behavior (with `expandCategoryIds`) is preserved.

**Rationale**: The `categoryIds` parameter is the explicit, client-controlled list. The `categoryId` parameter remains for backward compatibility (deep links, other pages). They should not be combined (union) to avoid confusing behavior.

**Alternatives considered**:
- Union of both parameters - Complex and confusing; no use case requires it.
- Deprecate `categoryId` entirely - Would break deep links and other pages that use single-category filtering.

## Decision 3: Repository method reuse

**Decision**: Reuse existing `findByFiltersPageableWithCategoryIds()` and `getFilteredTotalAmountWithCategoryIds()` methods. No new repository methods needed.

**Rationale**: These methods already accept `List<Long> categoryIds` and use `e.category.id IN :categoryIds` JPQL. They were originally created for parent category expansion but work identically for any list of category IDs.

**Alternatives considered**:
- Create new repository methods - Unnecessary duplication since the existing methods have the exact same signature and query structure needed.

## Decision 4: Frontend integration approach

**Decision**: Remove all client-side filtering logic from `expenses/page.tsx`. Instead, include `categoryIds` in the `ExpenseListFilters` and pass it to the backend with every request. The `selectedCategoryIds` Set from the chip filter is converted to an array of numbers.

**Rationale**: This is the core goal of the feature — all filtering server-side. Eliminates the `hasCategoryFilter` toggle, `prevHasCategoryFilter` ref, `filteredData` memo, and the 10,000-item fetch workaround.

**Alternatives considered**:
- Keep hybrid approach (client-side for chips, server for others) - This is what we're replacing. It's the root cause of the pagination and total-amount bugs.
