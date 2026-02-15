# Research: Category Expense Aggregates

**Feature**: 016-category-expense-aggregates
**Date**: 2026-02-15

## Decision 1: Expense-Budget Relationship Removal Strategy

**Decision**: Remove the `budget_id` column from the expenses table via a Liquibase migration, with a pre-migration step that copies the budget's category to any expenses that have a budget but no category.

**Rationale**: The spec requires complete decoupling. A clean column drop is simpler than making the FK nullable and leaving orphaned references. The migration must be safe: ensure category_id is populated on all expenses before dropping budget_id.

**Alternatives considered**:
- Make budget_id nullable instead of dropping: Rejected because it adds ambiguity — code would need to handle both linked and unlinked expenses indefinitely.
- Soft-delete via a flag: Over-engineered for a household app with clean migration path.

## Decision 2: Category Expense Aggregation Approach

**Decision**: Use on-the-fly JPQL aggregate queries (`SUM`, `GROUP BY`) on the expenses table, grouped by `category_id` and `YEAR(expense_date)`/`MONTH(expense_date)`. Parent category rollup computed in the service layer by querying child category IDs and summing their aggregates.

**Rationale**: Household-scale data (hundreds of expenses per year) doesn't warrant materialized views or caching. JPQL queries with proper indexes are fast enough. The existing `expense_date` and `category_id` columns provide the grouping dimensions.

**Alternatives considered**:
- Materialized view / summary table: Over-engineered for household scale. Adds maintenance complexity and staleness risk.
- Application-level caching (Redis/Caffeine): Unnecessary complexity for a private-network app with single-digit concurrent users.
- Database view (MySQL CREATE VIEW): Could work but adds a DB dependency that's harder to manage in Liquibase and harder to evolve.

## Decision 3: Budget-Expense Cascade Removal

**Decision**: Remove the `Budget.expenses` bidirectional relationship entirely. Budget deletion no longer cascades to expenses. The `BudgetService.deleteBudget()` method simply deletes the budget record.

**Rationale**: After decoupling, expenses have no FK to budgets. The JPA `CascadeType.ALL` and `orphanRemoval = true` on `Budget.expenses` must be removed. Budget deletion cannot affect expenses because there's no relationship.

**Alternatives considered**:
- Keep a soft reference (denormalized budget_id on expense for audit): Rejected — adds dead references that are never useful and confuses queries.

## Decision 4: Expense Category Requirement

**Decision**: Make `category_id` NOT NULL on the expenses table. The migration sets any null category_id values by copying from the expense's current budget's category. After migration, the Expense entity enforces `@NotNull` on category.

**Rationale**: The spec requires every expense to have a category (FR-003). The "Uncategorized" system category serves as fallback in the UI, but the DB constraint ensures data integrity.

**Alternatives considered**:
- Keep category optional and default at query time: Rejected — complicates every aggregate query with null handling.

## Decision 5: New Aggregate Repository Queries

**Decision**: Add the following JPQL queries to `ExpenseRepository`:

1. `sumByCategoryAndMonth(categoryId, year, month)` — Monthly aggregate for a single category
2. `sumByCategoryAndYear(categoryId, year)` — Yearly aggregate for a single category
3. `getMonthlyAggregatesByYear(year)` — All categories' monthly aggregates in one query (GROUP BY category_id, MONTH)
4. `getYearlyAggregatesByYear(year)` — All categories' yearly aggregates (GROUP BY category_id)

Parent rollup is done in the service layer by fetching child category IDs from `CategoryRepository` and summing child aggregates.

**Rationale**: A single GROUP BY query for all categories is more efficient than N+1 queries per category. The service layer handles the 2-level hierarchy rollup which is simple given the max-2-level constraint.

**Alternatives considered**:
- Single massive query with JOINs for parent rollup: MySQL doesn't support recursive CTEs efficiently for this pattern, and the 2-level hierarchy is simple enough for application-level aggregation.

## Decision 6: Budget Detail Page After Decoupling

**Decision**: The budget detail page (`/budgets/[id]`) shows budget info alongside the category aggregate spending for the budget's category and time period. Instead of listing expenses by budget_id, it shows the aggregate spending and a link to the expense list filtered by category + period.

**Rationale**: Users still need to see budget vs. actual spending when viewing a budget. The category aggregate provides the "actual" figure. A link to the filtered expense list lets users drill into details.

**Alternatives considered**:
- Remove budget detail page entirely: Too aggressive — users expect to view individual budget details.
- Embed the full expense list in the budget detail page: Over-complicates the page; the expense list page already supports category/period filtering.

## Decision 7: Dashboard BudgetSummaryCard Update

**Decision**: Update the dashboard's `BudgetSummaryCard` to source spending from category-based aggregate queries instead of `sumAmountByBudgetId()`. The `getMonthBudgetSummary()` method will sum expenses by category and month instead of by budget.

**Rationale**: Consistency with the yearly view approach. All spending figures should come from category aggregates after this feature.

## Decision 8: ExpenseInputJobService Update

**Decision**: Remove the `resolveBudgetId()` method from `ExpenseInputJobService`. The bulk import flow will create expenses with only category assignment, matching the new simplified flow.

**Rationale**: The input job service duplicated the budget resolution logic from ExpenseService. Both become unnecessary after decoupling.

## Decision 9: Migration Safety

**Decision**: The Liquibase migration runs in 3 changesets:
1. **Populate categories**: `UPDATE expenses e JOIN budgets b ON e.budget_id = b.id SET e.category_id = b.category_id WHERE e.category_id IS NULL`
2. **Make category_id NOT NULL**: `ALTER TABLE expenses MODIFY category_id BIGINT NOT NULL`
3. **Drop budget_id**: Drop FK constraint, drop index, drop column

**Rationale**: Sequential changesets ensure each step can be verified. If the category population step fails, the migration rolls back before any destructive changes.

**Alternatives considered**:
- Single changeset: Riskier — if any step fails, the entire migration state is unclear.
- Application-level migration: Requires running the app in a special mode; Liquibase is the established migration tool.

## Decision 10: Removal of Budget-Expense Auto-Assignment Logic

**Decision**: Remove the following methods entirely:
- `ExpenseService.resolveBudgetForExpense()` — budget auto-selection based on date/category
- `ExpenseService.checkDateMismatch()` — budget month vs expense date warning
- `BudgetService.reassignParentExpensesToMonthlyBudgets()` — expense reassignment when creating monthly budgets
- All `findByBudgetId*` queries in ExpenseRepository that are no longer called

**Rationale**: These methods exist solely because of the expense-budget link. After decoupling, they serve no purpose and would be dead code.
