# Research: Parent Category Budget & Expense Support

**Feature**: 012-parent-category-budgets
**Date**: 2026-02-09

## Research Areas

### R1: Parent Category Budget Restriction Removal

**Decision**: Remove the `countByParentCategoryId > 0` check in `BudgetService.createBudget()` (lines 75-79) that prevents budget creation on parent categories.

**Rationale**: The existing restriction was designed under the assumption that budgets should only exist on leaf categories. With the new requirement to allow parent categories to have budgets, this restriction must be removed. The rest of the budget creation flow (yearly/monthly logic, duplicate checks, parent yearly budget handling) works correctly for any category.

**Alternatives Considered**:
- Add a separate `createParentCategoryBudget()` method: Rejected because the existing `createBudget()` flow handles all the necessary logic once the restriction is removed. Splitting would duplicate code.
- Keep restriction but add override flag: Rejected because the restriction is being fully removed, not conditionally bypassed.

### R2: Parent Category Budget Auto-Increment vs Separate API

**Decision**: Extend the existing `createBudget()` flow to handle parent category budget creation/increment as part of the child budget creation request. New DTO fields `createParentCategoryBudget` and `parentCategoryBudgetAmount` control this behavior.

**Rationale**: The existing `createBudget()` already handles "create parent yearly budget" (`createParentBudget` flag) and "extend parent yearly budget" (`extendParentBudget` flag) patterns. Adding similar fields for parent *category* budget follows the established pattern. The client sends the request with the child budget + parent category budget intent, and the server handles both atomically in one transaction.

**Alternatives Considered**:
- Two separate API calls (create child budget, then create/update parent category budget): Rejected because it requires the client to orchestrate two requests without transactional guarantees. If the second fails, inconsistent state results.
- New dedicated endpoint `POST /api/budgets/with-parent`: Rejected because the existing endpoint and DTO pattern already supports additional flags.

### R3: Expense Aggregation Strategy

**Decision**: Compute child category expense aggregation at query time using JPA queries that join budgets with categories and their children. No denormalization or materialized views.

**Rationale**: The household scale (hundreds of expenses per year, 2-5 categories deep at most) makes real-time aggregation trivially fast. Denormalization would add complexity (triggers, event-driven updates) for negligible performance gain at this scale.

**Alternatives Considered**:
- Denormalized `parent_total_spending` column on budgets table: Rejected because it introduces data consistency challenges (must update on every expense CRUD operation).
- Database view or stored procedure: Rejected because the project uses JPA/Spring Data exclusively; raw SQL views would break the abstraction.

### R4: Category Dropdown UI Pattern

**Decision**: Use Material-UI's `<ListSubheader>` within `<Select>` to create grouped category options. Parent categories appear as both group headers (non-selectable) and selectable items within their own group.

**Rationale**: Material-UI's Select component supports `<ListSubheader>` for group headers. This is the standard MUI pattern for grouped selects. The parent category appears as a selectable `<MenuItem>` within its own group (before its children), providing clear visual hierarchy.

**Alternatives Considered**:
- Autocomplete with groupBy: Rejected because BudgetForm uses a standard Select, not Autocomplete. Changing would require significant refactoring.
- Flat list with indentation: Rejected per clarification — user chose grouped select pattern.

### R5: Budget Validation DTO Extension for Parent Category

**Decision**: Extend `BudgetValidationDTO` with fields for parent *category* budget existence and amount (separate from the existing yearly parent budget fields). The frontend uses these to determine whether to show the "create parent category budget" checkbox or the "will auto-increment" info.

**Rationale**: The existing `BudgetValidationDTO` already returns `parentBudgetExists` (for yearly parent budget). The new fields `parentCategoryBudgetExists` and `parentCategoryBudgetAmount` serve a different purpose — they indicate whether the selected category's *parent category* has a budget for the same period.

**Alternatives Considered**:
- Reuse existing `parentBudgetExists` field with overloaded semantics: Rejected because it would break existing yearly parent budget logic.
- Separate validation endpoint: Rejected because extending the existing DTO is simpler and the endpoint already accepts categoryId/year/month.

### R6: "Including Children" Subtotal in Budget List

**Decision**: Add `childrenBudgetSum` and `childrenSpending` fields to `BudgetSummaryDTO`. When serving the budget list, if the budget's category is a parent category, compute the sum of child category budgets and child category expenses for the same period.

**Rationale**: The budget list view already renders `BudgetSummaryDTO` objects. Adding computed fields keeps the display logic simple — the frontend just checks if `childrenBudgetSum > 0` to show the subtotal line.

**Alternatives Considered**:
- Frontend-side computation (fetch all budgets, group by parent): Rejected because this duplicates category hierarchy logic in the frontend and requires fetching all budgets (not paginated-friendly).
- Nested DTO with child budget list: Rejected because it's over-engineered for a simple subtotal display.

### R7: Expense List Category Filter with Children

**Decision**: When the expense list is filtered by a category that has children, expand the filter to include the parent category ID and all child category IDs. This is done in `ExpenseService.getExpenseList()` by resolving child categories and passing multiple category IDs to the repository query.

**Rationale**: The existing `findByFiltersPageable` query filters by a single `categoryId`. Expanding to a list of IDs using `IN` clause is a minimal change that covers both parent and child expense filtering.

**Alternatives Considered**:
- Client-side: frontend sends multiple requests per child category: Rejected because it fragments pagination and aggregation.
- Database-level: subquery on category hierarchy: Rejected because it couples the query to the category table structure unnecessarily.
