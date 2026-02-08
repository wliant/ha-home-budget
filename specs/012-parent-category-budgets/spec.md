# Feature Specification: Parent Category Budget & Expense Support

**Feature Branch**: `012-parent-category-budgets`
**Created**: 2026-02-09
**Status**: Draft
**Input**: User description: "Allow budgets and expenses on parent categories with automatic parent budget creation and aggregation when child categories are selected"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Budget on Parent Category (Priority: P1)

A household member wants to set a budget directly on a parent category (e.g., "Food") to track overall spending across all child categories (e.g., "Groceries", "Dining Out", "Snacks"). Currently, the system prevents creating budgets on parent categories. After this change, users can select any category — parent or child — when creating a budget.

**Why this priority**: This is the foundational change that removes the parent category restriction. All other stories depend on this capability being available.

**Independent Test**: Can be fully tested by creating a budget on a parent category and verifying it appears in the budget list. Delivers value by allowing top-level budget tracking.

**Acceptance Scenarios**:

1. **Given** a parent category "Food" with children "Groceries" and "Dining Out", **When** the user opens the budget creation form, **Then** the category dropdown shows both parent categories and child categories as selectable options.
2. **Given** a parent category "Food" is selected in the budget form, **When** the user submits the budget with year, month, and amount, **Then** a budget is created for the parent category successfully.
3. **Given** a parent category "Food" already has a monthly budget for January 2026, **When** the user tries to create another budget for "Food" for January 2026, **Then** the system rejects it with an appropriate error (duplicate budget prevention).

---

### User Story 2 - Automatic Parent Budget Creation When Setting Child Budget (Priority: P1)

When a household member creates a budget for a child category (e.g., "Groceries" under "Food"), the system should check whether the parent category ("Food") already has a budget for that period. If no parent budget exists, the budget creation form shows a pre-selected checkbox offering to create the parent category budget. If a parent budget already exists, the system automatically increases the parent budget amount by the child budget amount and displays an informational message.

**Why this priority**: This ensures parent-child budget consistency and prevents orphaned child budgets without parent tracking. This is the core workflow enhancement requested.

**Independent Test**: Can be tested by creating a child category budget and verifying the parent budget behavior (checkbox or auto-increment).

**Acceptance Scenarios**:

1. **Given** child category "Groceries" (parent: "Food") is selected and no budget exists for "Food" in January 2026, **When** the user fills in the budget form, **Then** a checkbox labeled "Also create budget for parent category 'Food'" appears, checked by default, with the same amount pre-filled.
2. **Given** the parent budget checkbox is checked with amount $500, **When** the user submits the child budget of $300, **Then** both a "Groceries" budget ($300) and a "Food" budget ($500) are created for that period.
3. **Given** the parent budget checkbox is unchecked, **When** the user submits the child budget of $300, **Then** only the "Groceries" budget is created, and no parent budget is created.
4. **Given** child category "Dining Out" (parent: "Food") is selected and a budget of $500 already exists for "Food" in January 2026, **When** the user submits a $200 budget for "Dining Out", **Then** the "Food" parent budget is automatically increased from $500 to $700, and an informational message is displayed: "Budget for 'Food' has been updated from $500 to $700 for January 2026."
5. **Given** a child category with no parent category is selected, **When** the user creates a budget, **Then** no parent budget checkbox or auto-increment behavior occurs (existing behavior preserved).

---

### User Story 3 - Expense Aggregation: Child Expenses Count Toward Parent Category (Priority: P1)

In all views (dashboard, expense list, budget summary), expenses recorded against a child category must also be counted toward the parent category's totals. For example, if a $50 expense is recorded under "Groceries" (child of "Food"), the "Food" parent category should reflect that $50 in its spending.

**Why this priority**: Without aggregation, parent category budgets would show no spending even though child categories have expenses, making the parent budget meaningless.

**Independent Test**: Can be tested by creating expenses on child categories and verifying the parent category's spending total includes them in all views.

**Acceptance Scenarios**:

1. **Given** "Food" has a budget of $500 and child "Groceries" has an expense of $80, **When** viewing the budget summary for "Food", **Then** the spending for "Food" shows $80 (aggregated from child expenses).
2. **Given** "Food" has a budget of $500, "Groceries" has expenses totaling $80, and "Dining Out" has expenses totaling $40, **When** viewing the budget summary for "Food", **Then** the spending for "Food" shows $120 (sum of all child category expenses).
3. **Given** "Food" has its own direct expense of $20 and child categories have expenses totaling $100, **When** viewing the budget summary for "Food", **Then** the spending for "Food" shows $120 (direct + aggregated child expenses).
4. **Given** a user views the expense list filtered by parent category "Food", **When** results are displayed, **Then** the list includes expenses from "Food" directly AND from all child categories ("Groceries", "Dining Out").

---

### User Story 4 - Record Expense on Parent Category (Priority: P2)

A household member wants to record an expense directly against a parent category when the expense does not fit neatly into any child category. For example, a generic "Food" purchase that does not belong to "Groceries" or "Dining Out" specifically.

**Why this priority**: Provides flexibility for uncategorized spending within a parent's scope. Lower priority because most expenses will be on child categories.

**Independent Test**: Can be tested by recording an expense directly on a parent category and verifying it appears correctly in all views.

**Acceptance Scenarios**:

1. **Given** a parent category "Food" exists with a budget for January 2026, **When** the user creates an expense and selects "Food" as the category, **Then** the expense is created successfully and linked to the "Food" budget.
2. **Given** the expense form is open, **When** the user views the category selection, **Then** both parent and child categories are available for selection.

---

### User Story 5 - Parent Budget Yearly Logic Unchanged (Priority: P2)

The existing yearly budget logic (parent budget at the yearly level) remains unchanged. Yearly budgets continue to function as the annual envelope for monthly budgets within a category. The parent category budget changes apply only at the monthly level.

**Why this priority**: Preserves backward compatibility for the existing yearly budget rollup mechanism.

**Independent Test**: Can be tested by verifying that yearly budgets for categories (both parent and child) continue to behave as they do today.

**Acceptance Scenarios**:

1. **Given** a category has monthly budgets totaling $3,600 across the year, **When** the yearly budget summary is viewed, **Then** the yearly budget reflects the sum of monthly budgets (existing behavior preserved).
2. **Given** a parent category budget is created for a month, **When** no yearly budget exists for that parent category, **Then** a yearly budget is automatically created following existing yearly budget creation rules.

---

### Edge Cases

- What happens when a parent category budget is deleted but child category budgets still reference it? The child budgets remain unaffected; parent budget deletion does not cascade to children.
- What happens when a child category is moved to a different parent? The budget auto-increment does not retroactively adjust. The parent budget for the new parent is not automatically modified.
- What happens when all child categories under a parent are deleted? The parent category budget remains intact and continues to function independently.
- What happens when the auto-increment would result in a parent budget exceeding a very large amount? No upper limit is enforced; the system allows any valid amount.
- What happens when multiple child budgets are created simultaneously for the same parent? Each child budget creation independently checks and increments the parent budget.
- What happens when a user deletes a child budget that previously auto-incremented the parent? The parent budget is NOT automatically decremented. The user must manually adjust the parent budget if desired.

## Clarifications

### Session 2026-02-09

- Q: Should the parent category budget row in the budget list include child budgets' amounts in its total? → A: Parent budget row shows its own amount (e.g., $500) with a separate "including children" subtotal line that sums the parent's own budget plus all child category budgets (e.g., "Including children: $800").
- Q: How should parent and child categories be presented in the budget creation dropdown? → A: Grouped select — parent categories appear as group headers with children listed underneath. Parent categories also appear as selectable items within their own group.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow budget creation on parent categories (categories that have children), removing the existing restriction.
- **FR-002**: System MUST allow expense recording directly on parent categories.
- **FR-003**: System MUST show all categories (both parent and child) in the budget creation category dropdown using a grouped select pattern: parent categories appear as group headers with their children listed underneath, and parent categories are also selectable items within their own group.
- **FR-004**: When a child category is selected for budget creation and the parent category does NOT have a budget for the selected period, the system MUST display a checkbox (default: checked) offering to create a parent category budget.
- **FR-005**: The parent budget creation checkbox MUST allow the user to specify the parent budget amount, pre-filled with the same amount as the child budget.
- **FR-006**: When a child category is selected for budget creation and the parent category ALREADY has a budget for the selected period, the system MUST automatically increase the parent budget by the child budget amount upon submission.
- **FR-007**: When the parent budget is auto-incremented (FR-006), the system MUST display an informational message indicating the parent category name, the previous amount, the new amount, and the period (year/month).
- **FR-008**: In all views (dashboard, budget list, expense list), expenses recorded on child categories MUST be aggregated into the parent category's spending totals.
- **FR-013**: In the budget list view, parent category budget rows MUST display the parent's own budget amount as the primary value and a separate "including children" subtotal line that sums the parent's own budget plus all child category budgets for the same period.
- **FR-009**: When filtering expenses by a parent category, the system MUST include expenses from all child categories in the results.
- **FR-010**: The existing yearly budget logic (automatic yearly budget creation, yearly budget as sum of monthly budgets) MUST remain unchanged for both parent and child categories.
- **FR-011**: Deleting a parent category budget MUST NOT cascade to or affect child category budgets.
- **FR-012**: The parent budget auto-increment (FR-006) MUST only occur during budget creation, not during budget editing or deletion.

### Key Entities

- **Category**: Represents a spending classification. Has optional parent-child hierarchy (max 2 levels). Now supports budgets and expenses at both parent and child levels.
- **Budget**: Tracks spending allocation for a category over a time period (year + optional month). Now linkable to parent categories in addition to child categories. Monthly and yearly budget logic preserved.
- **Expense**: Records individual spending transactions. Can be linked to any category (parent or child). Child category expenses aggregate into parent category totals for display purposes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create a budget on any category (parent or child) within the same workflow and time as before (under 30 seconds).
- **SC-002**: When creating a child budget without an existing parent budget, 100% of users see the parent budget creation option without additional navigation.
- **SC-003**: Parent category budget views accurately reflect the sum of direct expenses plus all child category expenses, with zero discrepancy.
- **SC-004**: Existing budget and expense workflows for child categories continue to function identically (zero regression).
- **SC-005**: The informational message about parent budget changes is displayed within the same page interaction (no separate page or popup required beyond inline notification).

## Assumptions

- The 2-level category hierarchy limit (parent → child, no grandchildren) remains in place. This feature does not introduce deeper nesting.
- The parent budget creation checkbox and auto-increment behavior only apply to the monthly budget level; yearly budget behavior is unchanged.
- The parent budget amount in the checkbox form is editable — the user can change it from the pre-filled default.
- There is no automatic decrement of parent budgets when child budgets are deleted or reduced.
- Shared visibility rules apply: all household members can see and interact with parent category budgets, same as child category budgets.
- The informational message about parent budget auto-increment is a transient notification (e.g., snackbar/toast), not a persistent record.
