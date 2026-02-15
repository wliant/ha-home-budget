# Feature Specification: Category Expense Aggregates

**Feature Branch**: `016-category-expense-aggregates`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "there is a need to enhance the expense tracking capability. 1. when an expense is entered, it doesn't need to link to a budget. it only need to link to a category. remove the budget id foreign key. create a month expense aggregate on categories and yearly expense aggregate on categories. If the expense is tie to a child category, it should be aggregated to the parent category as well. The yearly budget view, should be using this aggregate."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Decouple Expenses from Budgets (Priority: P1)

A household member records an expense by selecting a category and entering amount, date, and description. The system no longer requires linking the expense to a specific budget. The expense is persisted with only a category reference.

**Why this priority**: This is the foundational change. All other stories depend on expenses being category-linked rather than budget-linked. Without this, aggregates cannot be built on categories alone.

**Independent Test**: Can be fully tested by creating an expense with only a category (no budget selection) and verifying it is saved and retrievable. Existing expenses with budget links continue to work.

**Acceptance Scenarios**:

1. **Given** a user is on the expense creation form, **When** they select a category, enter an amount, date, and description, **Then** the expense is saved successfully without requiring a budget selection.
2. **Given** an expense exists that was previously linked to a budget, **When** the system is updated, **Then** the existing expense retains its category and remains queryable (budget link is removed).
3. **Given** a user creates an expense for a child category, **When** the expense is saved, **Then** the expense is linked to that child category and the child's parent category relationship is preserved for aggregation.
4. **Given** a user deletes a budget, **When** expenses exist that were formerly linked to that budget, **Then** the expenses are NOT deleted (no cascade deletion through budgets).

---

### User Story 2 - Monthly Expense Aggregates per Category (Priority: P1)

The system calculates and provides monthly spending totals for each category. When a category has child categories, the parent category's monthly aggregate includes the sum of all its children's expenses plus any expenses directly on the parent. These aggregates are available for display in budget views and dashboards.

**Why this priority**: Monthly aggregates are required for the yearly budget view (US3) and provide the core spending visibility per category. Co-equal with US1 as the aggregate mechanism.

**Independent Test**: Can be tested by creating expenses across multiple categories and months, then querying the monthly aggregate and verifying totals match expected sums, including parent rollup.

**Acceptance Scenarios**:

1. **Given** 3 expenses totaling $150 exist for "Groceries > Fresh Produce" in January 2026, **When** the monthly aggregate for January 2026 is retrieved, **Then** "Fresh Produce" shows $150 and "Groceries" (parent) includes $150 in its aggregate.
2. **Given** a parent category "Groceries" has $50 in direct expenses and child categories totaling $200 in January, **When** the monthly aggregate is retrieved, **Then** "Groceries" shows $250 total ($50 direct + $200 from children).
3. **Given** a standalone category (no parent, no children) has $100 in expenses for March, **When** the monthly aggregate is retrieved, **Then** it shows exactly $100.
4. **Given** no expenses exist for a category in a given month, **When** the monthly aggregate is retrieved, **Then** the category shows $0 for that month.

---

### User Story 3 - Yearly Budget View Using Category Aggregates (Priority: P2)

The yearly budget view displays budget amounts alongside actual spending per category. Spending figures are sourced from the category expense aggregates rather than from budget-to-expense links. For each category with a yearly budget, the view shows the budgeted amount vs. actual aggregate spending for the year. Monthly breakdowns show budget vs. actual per month.

**Why this priority**: This is the primary consumer of the aggregates and delivers the core user value of comparing budgets to actual spending. Depends on US1 and US2 being complete.

**Independent Test**: Can be tested by setting up budgets and expenses for a year, then viewing the yearly budget page and verifying that spending columns reflect category aggregate totals (not budget-linked expense sums).

**Acceptance Scenarios**:

1. **Given** a yearly budget of $1200 exists for "Groceries" and $800 in expenses are aggregated for the year, **When** the yearly budget view is loaded, **Then** "Groceries" shows $1200 budget, $800 spent, $400 remaining.
2. **Given** a parent category "Groceries" has child categories with their own monthly budgets and expenses, **When** the yearly budget view is loaded, **Then** the parent row shows aggregated spending from all children plus direct expenses.
3. **Given** monthly budgets exist for "Utilities" ($100/month for Jan-Jun), **When** the yearly view shows monthly breakdown, **Then** each month shows budget amount vs. actual spending from category aggregates.
4. **Given** expenses exist for a category that has no budget, **When** the yearly budget view is loaded, **Then** that category does not appear in the budget view (budgets still define which categories are tracked).

---

### User Story 4 - Yearly Expense Aggregate per Category (Priority: P2)

The system provides yearly spending totals per category, calculated as the sum of all 12 monthly aggregates. This yearly total is available for display in dashboards and summary views.

**Why this priority**: Yearly aggregates are a natural extension of monthly aggregates and provide summary-level data for dashboards. Lower priority than the monthly aggregates which are the building blocks.

**Independent Test**: Can be tested by creating expenses across multiple months for a category, then querying the yearly aggregate and verifying it matches the sum of monthly totals.

**Acceptance Scenarios**:

1. **Given** expenses totaling $1500 exist across all months of 2026 for "Transportation", **When** the yearly aggregate is retrieved, **Then** it shows $1500 for the year.
2. **Given** a parent category with children has combined expenses of $3000 for the year, **When** the yearly aggregate is retrieved, **Then** the parent shows $3000 (sum of direct + child expenses).

---

### Edge Cases

- What happens when an expense is moved from one category to another? The aggregates for both the old and new categories must update correctly.
- What happens when a child category is reassigned to a different parent? The aggregates for the old parent decrease and the new parent increases.
- What happens when a category with expenses is deleted? Expenses should be reassigned to the "Uncategorized" system category and aggregates recalculated.
- What happens when an expense has no category? It should be assigned to the "Uncategorized" system category.
- How are existing expenses migrated? Existing expenses retain their category link; the budget_id column is dropped after migration. Expenses that had a budget but no category should inherit the category from their linked budget before the column is removed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow expenses to be created with only a category reference (no budget required).
- **FR-002**: System MUST remove the mandatory budget foreign key from the expense entity. The budget_id column should be dropped from the expenses table.
- **FR-003**: System MUST require every expense to have a category. If no category is explicitly chosen, the system assigns the "Uncategorized" system category.
- **FR-004**: System MUST calculate monthly expense aggregates per category as the sum of all expense amounts for that category in a given month/year.
- **FR-005**: System MUST roll up child category expenses to the parent category aggregate. A parent category's aggregate equals direct expenses + sum of all children's expenses.
- **FR-006**: System MUST calculate yearly expense aggregates per category as the sum of all 12 monthly aggregates.
- **FR-007**: The yearly budget view MUST source its spending data from category expense aggregates rather than budget-to-expense relationships.
- **FR-008**: System MUST NOT cascade-delete expenses when a budget is deleted. Budgets and expenses become independent entities linked only through their shared category.
- **FR-009**: System MUST migrate existing expense data: expenses that have a budget but no category should inherit the category from their linked budget before the budget_id column is dropped.
- **FR-010**: System MUST update the expense creation/edit forms to remove any budget selection UI and make category selection required.
- **FR-011**: System MUST update the expense list view to no longer display or filter by budget.

### Key Entities

- **Expense**: Tracks a single spending event. Key attributes: amount, description, date, category (required), created_by. No longer linked to a budget.
- **Category**: Organizes expenses hierarchically. Key attributes: name, icon, parent_category (optional, max 2 levels). Provides the grouping dimension for expense aggregates.
- **Budget**: Defines a spending plan for a category and time period. Key attributes: category, year, month (optional), total_amount. No longer directly linked to expenses.
- **Monthly Category Aggregate**: Derived data representing the total spending for a category in a given month/year. Includes rollup to parent categories.
- **Yearly Category Aggregate**: Derived data representing the total spending for a category across an entire year. Calculated as sum of monthly aggregates.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can record an expense in under 30 seconds by selecting only a category, amount, date, and description (no budget selection step).
- **SC-002**: The yearly budget view displays accurate spending totals that match the sum of all expenses per category within 1 second of page load.
- **SC-003**: Parent category spending totals correctly include all child category expenses with 100% accuracy.
- **SC-004**: Deleting a budget does not delete any associated expenses — 0 data loss from budget operations.
- **SC-005**: All existing expenses are preserved during migration with their category assignments intact.
- **SC-006**: Monthly and yearly aggregate totals are consistent: yearly aggregate equals the sum of 12 monthly aggregates for the same category.

## Assumptions

- The "Uncategorized" system category already exists in the database and will be used as the fallback for expenses without an explicit category.
- The 2-level category hierarchy (parent/child) is retained; no deeper nesting is required.
- Aggregates are calculated on-the-fly via database queries (not pre-computed materialized views), which is sufficient for a household-scale application.
- The expense creation form currently has a budget selection step that will be removed; the form will default to requiring a category.
- Existing budget CRUD operations (create, update, delete budgets) continue to work independently of expenses.
- The monthly budget summary and dashboard widgets will also use category aggregates for spending data (consistent with the yearly view approach).
