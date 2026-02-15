# Feature Specification: Parent Category Budget Auto-Rollup

**Feature Branch**: `017-parent-budget-rollup`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "When a budget is added to a subcategory (child category), the system should automatically add the budget amount to the parent category's budget for the same period (monthly or yearly). If the parent category budget doesn't exist for that period, create it automatically. In the yearly budget view, the total budget should be calculated by summing only parent category budgets, not subcategory budgets, to avoid double-counting since parent budgets already include their children's amounts."

## User Scenarios & Testing

### User Story 1 - Automatic Parent Budget Creation (Priority: P1)

When a household member creates a budget for a child category (e.g., "Groceries → Fresh Produce"), the system automatically creates or updates the parent category budget ("Groceries") with the same amount for the same period. This ensures parent categories always reflect their children's total budget allocations.

**Why this priority**: Core functionality that establishes the automatic rollup mechanism. Without this, the entire feature doesn't work.

**Independent Test**: Create a budget for a child category (e.g., $500 for "Fresh Produce" in January 2026). Verify that a parent category budget for "Groceries" is automatically created with $500 for January 2026.

**Acceptance Scenarios**:

1. **Given** a parent category "Groceries" with no budget for January 2026, **When** user creates a monthly budget of $500 for child category "Fresh Produce" for January 2026, **Then** the system automatically creates a parent budget for "Groceries" with $500 for January 2026
2. **Given** a parent category "Groceries" with an existing monthly budget of $300 for January 2026, **When** user creates a monthly budget of $200 for child category "Pantry" for January 2026, **Then** the system updates the parent budget to $500 ($300 + $200)
3. **Given** a parent category "Transportation" with no yearly budget for 2026, **When** user creates a yearly budget of $10,000 for child category "Car Maintenance" for 2026, **Then** the system automatically creates a parent yearly budget for "Transportation" with $10,000 for 2026

---

### User Story 2 - Correct Total Budget Calculation (Priority: P1)

The yearly budget view displays the total budget by summing only parent category budgets, not child category budgets. This prevents double-counting since parent budgets already include their children's amounts.

**Why this priority**: Critical for accurate financial reporting. Without this, users would see inflated budget totals.

**Independent Test**: Create budgets for parent "Groceries" ($1000) and children "Fresh Produce" ($500) and "Pantry" ($300). In the yearly budget view, verify the total budget counts only the parent $1000, not $1000 + $500 + $300 = $1800.

**Acceptance Scenarios**:

1. **Given** budgets exist for parent category "Groceries" ($1000) and two child categories "Fresh Produce" ($500) and "Pantry" ($500), **When** user views the yearly budget summary for 2026, **Then** the total budget includes only the parent budget amount ($1000), not child budgets
2. **Given** budgets exist for multiple parent categories (Groceries: $1000, Transportation: $2000, Utilities: $500) and their children, **When** user views the yearly budget view, **Then** the total budget is $3500 (sum of parent budgets only)
3. **Given** a standalone category with no children ("Entertainment": $500) and a parent category with children ("Groceries": $1000), **When** user views the yearly budget view, **Then** the total budget is $1500 (both standalone and parent budgets are counted)

---

### User Story 3 - Budget Update Propagation (Priority: P2)

When a child category budget is updated or deleted, the parent category budget automatically adjusts to reflect the change. This maintains consistency between parent and child budget totals.

**Why this priority**: Important for data integrity but less critical than initial creation. Users can manually adjust if needed.

**Independent Test**: Create a child budget of $500, then update it to $700. Verify the parent budget increases by $200. Then delete the child budget and verify the parent budget decreases by $700.

**Acceptance Scenarios**:

1. **Given** a child category "Fresh Produce" has a budget of $500 for January 2026 (parent "Groceries" budget is $800), **When** user updates the child budget to $700, **Then** the parent budget updates to $1000 ($800 - $500 + $700)
2. **Given** a child category "Pantry" has a budget of $300 for January 2026 (parent "Groceries" budget is $1000), **When** user deletes the child budget, **Then** the parent budget updates to $700 ($1000 - $300)
3. **Given** the only child category budget is deleted, **When** the parent budget amount becomes zero, **Then** the parent budget record remains (not deleted) with zero amount

---

### Edge Cases

- What happens when a user tries to create a budget for a parent category directly (without going through child categories)?
  - Allow it. Parent budgets can be created manually and will increase when child budgets are added.
- What happens when a parent category has both manually created budget and auto-rollup amounts?
  - The system maintains a single parent budget that combines manual amounts and child rollup amounts.
- What happens when a child category budget exceeds available parent budget (if parent was manually set)?
  - The parent budget automatically adjusts upward to accommodate the child budget. No validation prevents this.
- What happens when deleting all child budgets from a parent?
  - The parent budget amount becomes zero but the budget record remains (not deleted).
- What happens with standalone categories (no parent)?
  - They are counted normally in the yearly budget view total since they are top-level categories.
- What happens when a child category is moved to a different parent?
  - Out of scope - category hierarchy changes are separate from budget rollup.

## Requirements

### Functional Requirements

- **FR-001**: System MUST automatically create a parent category budget when a child category budget is created and no parent budget exists for that period (monthly or yearly)
- **FR-002**: System MUST automatically add the child budget amount to an existing parent category budget when a child budget is created
- **FR-003**: System MUST update the parent category budget when a child category budget amount is modified (increase or decrease the parent budget by the delta)
- **FR-004**: System MUST decrease the parent category budget amount when a child category budget is deleted
- **FR-005**: System MUST maintain separate parent budgets for monthly and yearly periods (e.g., adding a January child budget affects only the January parent budget, not the yearly parent budget)
- **FR-006**: The yearly budget view MUST calculate total budget by summing only parent category budgets (categories with children) and standalone category budgets (categories with no children)
- **FR-007**: The yearly budget view MUST exclude child category budgets from the total budget calculation to avoid double-counting
- **FR-008**: System MUST preserve manually created parent category budgets and allow them to coexist with auto-rollup amounts
- **FR-009**: Parent category budget records MUST persist even when their amount reaches zero (after all child budgets are deleted)

### Key Entities

- **Budget**: Existing entity with category relationship. The auto-rollup logic will maintain parent category budgets based on child category budget changes.
- **Category**: Existing entity with parent-child relationships (2-level hierarchy). The rollup follows the existing parent-child structure.

## Success Criteria

### Measurable Outcomes

- **SC-001**: When a child category budget is created, the parent category budget is automatically created or updated within the same transaction (no manual intervention required)
- **SC-002**: The yearly budget view total reflects only parent and standalone category budgets, preventing double-counting of child category budgets
- **SC-003**: Parent category budget amounts always equal the sum of their children's budget amounts (within the same period)
- **SC-004**: Users can view accurate budget hierarchies where parent budgets aggregate their children's allocations without manual calculation

## Assumptions

- The existing 2-level category hierarchy (parent-child) is sufficient and will not expand to more levels
- Budget cascade logic from Feature 012 (parent category budgets) already exists but needs enhancement for automatic rollup
- The current budget creation workflow supports category selection and period selection (monthly/yearly)
- Users understand that creating a child budget will affect the parent budget automatically
- Deleting a child budget will reduce the parent budget but not delete the parent budget record
- The yearly budget view already has access to category hierarchy information to distinguish parent from child categories

## Dependencies

- Feature 004: Hierarchical Category Budgets (category parent-child relationships)
- Feature 012: Parent Category Budgets (existing parent budget infrastructure)
- Feature 016: Category Expense Aggregates (spending calculations use category hierarchy)

## Out of Scope

- Changing the category hierarchy (moving child categories to different parents) - this is a separate category management concern
- Budget validation rules (e.g., preventing child budgets from exceeding parent budgets) - auto-rollup eliminates this need
- Multi-level category hierarchies beyond 2 levels (current system supports only parent-child)
- Budget allocation suggestions or recommendations based on historical data
- Notifications when parent budgets are automatically adjusted
