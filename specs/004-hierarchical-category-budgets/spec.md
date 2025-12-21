# Feature Specification: Hierarchical Category Budgets

**Feature Branch**: `004-hierarchical-category-budgets`
**Created**: 2025-11-11
**Status**: Draft
**Input**: User description: "work on the category functionality. This should allow user to create categories of spend. the category of spend can have parent category. when adding a budget, user need to specify a category. if it is a child category, the parent must be configured and the budget cannot exceed the sum of all child budget."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hierarchical Category Management (Priority: P1)

Users need to organize spending categories in a hierarchical structure where categories can have parent categories. For example, a "Food" parent category can have child categories like "Groceries", "Dining Out", and "Snacks". This allows users to track spending at both granular and aggregate levels.

**Why this priority**: This is the foundation of the feature. Without hierarchical categories, category-based budgeting cannot function. This establishes the data model that all other user stories depend on.

**Independent Test**: Can be fully tested by creating categories with and without parents, viewing the category hierarchy, and verifying parent-child relationships persist correctly. Delivers value by enabling organized expense categorization even before budgets are assigned.

**Acceptance Scenarios**:

1. **Given** I am on the categories page, **When** I create a new category without selecting a parent, **Then** the category is created as a top-level (root) category
2. **Given** I have an existing "Food" category, **When** I create a "Groceries" category and select "Food" as the parent, **Then** "Groceries" becomes a child of "Food" and appears under it in the hierarchy
3. **Given** I have a category "Food" with child "Groceries", **When** I view the category list, **Then** I see "Food" as a parent category with "Groceries" nested underneath
4. **Given** I have a child category "Groceries" under "Food", **When** I attempt to edit "Groceries" to make "Groceries" a parent of "Food", **Then** the system prevents the circular reference and shows an error message
5. **Given** I have a parent category with children, **When** I attempt to delete the parent category, **Then** the system prevents deletion and suggests I must first reassign or delete child categories

---

### User Story 2 - Category-Based Budget Creation (Priority: P1)

Users need to assign budgets to specific categories for a given month and year. When creating a budget, they must select a category to allocate funds toward that spending area. This enables tracking whether spending stays within allocated amounts for each category.

**Why this priority**: This is the MVP-level functionality that delivers immediate value. Users can create budgets per category and track spending against those budgets. This is independently valuable even before parent budget validation is implemented.

**Independent Test**: Can be fully tested by creating budgets for different categories and months, and verifying that each budget is correctly associated with its category. Delivers value by enabling category-specific budget tracking.

**Acceptance Scenarios**:

1. **Given** I have categories "Food" and "Transportation", **When** I create a budget for January 2025 and select "Food" category with amount $500, **Then** the budget is created and linked to the "Food" category
2. **Given** I have a "Food" budget for January 2025, **When** I create another budget for the same month and category, **Then** the system prevents duplicate budgets and shows an error "A budget for this category and time period already exists"
3. **Given** I have created a budget for "Food" in January, **When** I view my budgets, **Then** I see the budget listed with the category name, allocated amount, and time period
4. **Given** I have multiple categories, **When** I create budgets for different categories in the same month, **Then** each category can have its own separate budget allocation
5. **Given** I have a root category budget, **When** I add expenses to that category, **Then** the spending tracks against the category budget

---

### User Story 3 - Parent Budget Validation (Priority: P2)

When a category has child categories, the parent category's budget must equal the sum of all child category budgets. This ensures budget consistency across the hierarchy - the parent represents the total allocation, and children represent how that total is distributed.

**Why this priority**: This enforces budgetary discipline and prevents inconsistencies. However, users can still get value from P1 and P2 without this validation - they just won't have the hierarchical constraint enforcement. This is an enhancement that adds rigor to the budgeting process.

**Independent Test**: Can be fully tested by creating parent and child budgets and verifying validation rules enforce the sum constraint. Delivers value by preventing budget mismatches and ensuring hierarchical integrity.

**Acceptance Scenarios**:

1. **Given** I have a parent category "Food" with child categories "Groceries" ($300) and "Dining Out" ($200) for January, **When** I attempt to create a parent budget for "Food" with $600, **Then** the system prevents creation and shows error "Parent budget ($600) must equal sum of child budgets ($500)"
2. **Given** I have a parent category "Food" with children "Groceries" ($300) and "Dining Out" ($200), **When** I create a parent budget for "Food" with $500, **Then** the budget is successfully created as it matches the sum
3. **Given** I have a parent budget "Food" ($500) and child budget "Groceries" ($300), **When** I attempt to update "Groceries" to $400, **Then** the system shows a warning "This change will make parent budget invalid. Parent needs adjustment from $500 to $600"
4. **Given** I have a parent budget for "Food", **When** I attempt to create the first child budget without adjusting the parent, **Then** the system requires me to either decrease the parent budget or confirm reallocation
5. **Given** I have child budgets but no parent budget, **When** I create a parent budget, **Then** the system pre-fills the amount with the sum of child budgets and allows me to proceed

---

### User Story 4 - Category Budget Requirement (Priority: P2)

When creating or editing a budget, users must select a category. Budgets without categories are not allowed, ensuring all spending plans are properly classified and trackable within the category hierarchy.

**Why this priority**: This ensures data consistency and enables proper budget tracking. While important, P1 and P2 already establish the core workflow. This story adds a validation layer that can be tested independently.

**Independent Test**: Can be fully tested by attempting to create budgets with and without categories, verifying the validation works correctly. Delivers value by ensuring all budgets are properly categorized for tracking and reporting.

**Acceptance Scenarios**:

1. **Given** I am creating a new budget, **When** I attempt to save without selecting a category, **Then** the system shows error "Category is required" and prevents submission
2. **Given** I am editing an existing budget, **When** I attempt to remove the category selection, **Then** the system shows error "Category cannot be removed from existing budget"
3. **Given** I have a category assigned to a budget, **When** I attempt to delete that category, **Then** the system prevents deletion and shows "Cannot delete category with active budgets. Please reassign budgets first"
4. **Given** I am creating a budget, **When** I select a category from the dropdown, **Then** I see both root and child categories available for selection

---

### User Story 5 - Budget Summary and Reporting (Priority: P3)

Users need to view budget summaries that show both individual category budgets and rolled-up totals for parent categories. This provides insights into spending patterns at different levels of the hierarchy.

**Why this priority**: This enhances usability and provides valuable insights, but the core budgeting functionality works without it. Users can manually calculate totals if needed. This is a reporting/UX enhancement.

**Independent Test**: Can be fully tested by creating a category hierarchy with budgets and verifying the summary displays correct amounts and calculations. Delivers value through improved visibility and decision-making support.

**Acceptance Scenarios**:

1. **Given** I have parent category "Food" with child budgets "Groceries" ($300) and "Dining Out" ($200), **When** I view the budget summary, **Then** I see both child budgets individually and "Food" total ($500)
2. **Given** I have multiple parent categories with children, **When** I view the monthly budget overview, **Then** I see a hierarchical view showing parent totals and expandable child details
3. **Given** I have budgets across multiple months, **When** I select a specific month, **Then** the summary shows only budgets for that time period with accurate hierarchy totals
4. **Given** I have expenses recorded against child categories, **When** I view the budget summary, **Then** I see spending amounts for each category and remaining budget calculations

---

### Edge Cases

- What happens when a user tries to create a child category under another child (nested beyond 2 levels)? System should limit hierarchy to 2 levels (parent-child only) to maintain simplicity
- What happens when editing a parent category that has child budgets but no parent budget exists? System allows this but shows a notification that parent budget should be created for complete tracking
- What happens when a user deletes all child budgets for a parent - does the parent budget remain valid? Yes, parent budget remains but displays a warning "No child budgets allocated"
- What happens when user attempts to change a child category to a different parent mid-month with active budgets? System prevents parent change if active budgets exist, requiring budget reassignment first
- What happens when user creates multiple child budgets that exceed a parent budget amount? System prevents the last child budget creation that would violate the sum constraint
- What happens to orphaned child budgets when parent category is deleted (if deletion is forced)? System reassigns child categories to root level before allowing parent deletion

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support parent-child relationships between categories (maximum 2 levels deep: parent and child only, no grandchildren)
- **FR-002**: System MUST allow creation of root-level categories (categories without parents)
- **FR-003**: System MUST prevent circular references in category hierarchy (category cannot be its own ancestor)
- **FR-004**: System MUST require category selection when creating or editing budgets
- **FR-005**: System MUST prevent deletion of parent categories that have child categories unless children are reassigned or deleted first
- **FR-006**: System MUST enforce unique constraint: one budget per category per time period (year-month combination)
- **FR-007**: System MUST validate that parent category budgets equal the sum of all child category budgets for the same time period
- **FR-008**: System MUST allow child category budgets to exist without parent category budget (showing warning notification)
- **FR-009**: System MUST prevent parent category budget from being less than the sum of existing child budgets
- **FR-010**: System MUST prevent parent category budget from being greater than the sum of existing child budgets
- **FR-011**: System MUST display validation errors before saving when parent-child budget sum mismatch occurs
- **FR-012**: System MUST show current sum of child budgets when creating/editing parent budget
- **FR-013**: System MUST prevent deletion of categories that have associated budgets unless budgets are reassigned first
- **FR-014**: System MUST display category hierarchy in budget creation/edit forms (showing parent-child relationships)
- **FR-015**: System MUST persist parent-child category relationships across sessions
- **FR-016**: System MUST track budget metadata: category, time period (year-month), allocated amount, owner
- **FR-017**: System MUST support updating child budgets with validation against parent constraints
- **FR-018**: System MUST prevent changing category parent if active budgets exist for that category

### Key Entities *(include if feature involves data)*

- **Category**: Represents a spending classification; key attributes include name, icon, parent category reference (optional), system category flag, creation metadata. Categories form a tree structure with maximum depth of 2 levels.

- **Category Budget**: Represents allocated spending amount for a specific category and time period; key attributes include category reference, year-month period, allocated amount, creation metadata. Must have exactly one category and one time period, creating a unique budget allocation.

- **Hierarchical Relationship**: The parent-child association between categories; enforces sum validation where parent budget must equal sum of all child budgets for the same time period. Maximum one parent per category.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create category hierarchies (parent-child relationships) in under 30 seconds per category
- **SC-002**: System prevents 100% of invalid budget allocations where parent-child sums don't match
- **SC-003**: Users can view complete budget breakdown (parent and child categories) in a single screen without navigation
- **SC-004**: Budget creation with category selection completes in under 1 minute including validation
- **SC-005**: System validates parent-child budget constraints in under 2 seconds when saving budgets
- **SC-006**: 95% of users successfully create hierarchical category budgets without encountering validation errors on first attempt
- **SC-007**: Users can reorganize spending categories (add/edit parent-child relationships) at any time without data loss
- **SC-008**: Category hierarchy supports at minimum 10 parent categories each with up to 10 child categories without performance degradation

## Assumptions *(mandatory)*

1. **Two-Level Hierarchy**: The system will support only parent-child relationships (2 levels maximum). No grandchildren or deeper nesting. This keeps the UI simple and validation logic straightforward.

2. **Time Period Definition**: Budgets are scoped to monthly periods (year-month combination). Annual or weekly budgets are not supported in this iteration.

3. **Single Currency**: All budget amounts are in the same currency (no multi-currency support). Currency is defined at system level.

4. **Budget Ownership**: Budgets are user-scoped (created by specific user identified by X-Hass-User header). Budget visibility and editing permissions follow existing authentication model.

5. **Existing Category Model**: The current Category entity has basic fields (name, icon, isSystem, createdBy). This feature extends it with parentCategoryId foreign key.

6. **New Budget-Category Relationship**: Current Budget entity does not have category association. This feature adds a new many-to-one relationship where many budgets can reference one category.

7. **Expense Categorization**: Existing expenses are linked to categories. This feature does not change expense-category relationships but enables budget tracking per category.

8. **System Categories**: System categories (like "Uncategorized") cannot be deleted or have parents assigned. They remain root-level categories.

9. **Validation Timing**: Parent-child budget sum validation occurs at budget save time (create/update), not continuously. Users may temporarily have inconsistent budgets if they save child budgets without immediately updating parent.

10. **Parent Budget Optional**: Having child budgets without a parent budget is allowed (with warning notification). Users may choose to track only subcategories without tracking parent total.

11. **Budget Reassignment**: When category relationships change (parent assignment), existing budgets remain unchanged. Users must manually update budget assignments if they want to reflect new hierarchy.

12. **UI Availability**: Category management UI already exists (from Feature 002). This feature extends it with parent category selection field.

## Out of Scope

The following are explicitly not included in this feature:

1. **Multi-level Hierarchy**: Support for grandchildren categories or deeper than 2-level trees
2. **Budget Templates**: Creating reusable budget templates or copying budgets across months
3. **Budget Rollover**: Automatically rolling over unused budget amounts to next month
4. **Budget Alerts**: Notifications when spending approaches or exceeds budget limits
5. **Budget Forecasting**: Predicting future spending based on historical patterns
6. **Shared Budgets**: Multiple users collaborating on the same budget
7. **Budget Approval Workflow**: Requiring approval before budgets take effect
8. **Historical Budget Tracking**: Viewing budget changes over time or audit trail
9. **Budget Comparison**: Comparing budgets across different time periods
10. **Percentage-based Budgeting**: Allocating budgets as percentages of total income
11. **Category Merging**: Combining multiple categories into one
12. **Bulk Budget Operations**: Creating multiple category budgets at once
13. **Budget Import/Export**: Importing budgets from external files or exporting for analysis
14. **Custom Time Periods**: Supporting weekly, bi-weekly, quarterly, or annual budget periods
15. **Zero-based Budgeting**: Forcing allocation of all income across categories

## Dependencies

1. **Existing Category Entity**: Feature 002 (Budget and Expense Management) implemented the Category model. This feature extends it with parent-child relationships.

2. **Existing Budget Entity**: Feature 002 implemented the Budget model for monthly household budgets. This feature adds category association to budgets.

3. **Authentication**: Development mode default header feature (Feature 003) provides user identification for budget/category ownership.

4. **Database Schema**: Requires schema migrations to add:
   - `parent_category_id` foreign key to categories table
   - `category_id` foreign key to budgets table
   - Unique constraint on `(category_id, year, month)` in budgets table

5. **Frontend Category Management**: Feature 002 implemented category CRUD UI. This feature enhances it with parent selection and hierarchy display.
