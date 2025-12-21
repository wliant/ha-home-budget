# Feature Specification: Category Management UI Enhancements

**Feature Branch**: `005-category-ui`
**Created**: 2025-12-21
**Status**: Draft
**Input**: User description: "work on the category feature on the ui"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit Existing Categories (Priority: P1)

Users need to modify existing category details (name, icon, parent category) after creation. Currently, categories can only be created or deleted, but not edited. This is essential for correcting mistakes, reorganizing hierarchies, or updating category metadata as household spending patterns evolve.

**Why this priority**: This is foundational CRUD functionality that's currently missing. Without edit capability, users must delete and recreate categories to fix typos or change icons, risking data loss if the category has associated expenses. This delivers immediate value by enabling category maintenance.

**Independent Test**: Can be fully tested by creating a category, clicking edit, modifying its properties (name, icon, parent), saving, and verifying changes persist. Works independently without any other features.

**Acceptance Scenarios**:

1. **Given** I have a category "Grocieries" (typo) with icon 🛒, **When** I click edit and change the name to "Groceries", **Then** the category name is updated and the change is reflected in the category list
2. **Given** I have a category "Food" without an icon, **When** I edit it and add the 🍽️ icon, **Then** the icon appears next to the category name in all views
3. **Given** I have a root category "Snacks", **When** I edit it and select "Food" as parent, **Then** "Snacks" becomes a child of "Food" in the hierarchy view
4. **Given** I have a child category "Dining Out" under "Food", **When** I edit it and remove the parent, **Then** "Dining Out" becomes a root category
5. **Given** I have a category with associated budgets, **When** I attempt to change its parent category, **Then** the system prevents the change and shows error "Cannot change parent - category has active budgets"

---

### User Story 2 - Enhanced Category Card Information (Priority: P1)

Users need to see critical information about each category at a glance, including budget count, system category status, and creation metadata. Currently, only expense count is shown. This helps users understand category usage before editing or deleting.

**Why this priority**: Information visibility prevents errors and supports decision-making. Users need to know which categories are in use (budgets/expenses) before attempting deletion, and which are system categories (cannot be deleted). This is a quick win that significantly improves UX.

**Independent Test**: Can be fully tested by creating categories with various properties (system vs user, with/without budgets, with/without expenses) and verifying all metadata displays correctly on category cards.

**Acceptance Scenarios**:

1. **Given** I have a category with 5 expenses and 2 budgets, **When** I view the categories page, **Then** the category card shows "5 expenses" and "2 budgets"
2. **Given** I have a system category "Uncategorized", **When** I view the categories page, **Then** the card displays a "System Category" badge and the delete button is disabled
3. **Given** I have a category created by user "alice", **When** I view it as user "bob", **Then** the card shows "Created by alice"
4. **Given** I have a parent category with 3 child categories, **When** I view the hierarchy, **Then** the parent card shows "3 subcategories"
5. **Given** I have a category with creation date "2025-11-01", **When** I view the category card, **Then** it displays the formatted creation date

---

### User Story 3 - Category Search and Filtering (Priority: P2)

Users with many categories need to quickly find specific ones by searching or filtering. As households accumulate categories over time (potentially 50+ across multiple hierarchies), scrolling through all categories becomes inefficient.

**Why this priority**: While not blocking initial usage, search becomes critical as category count grows. This is independently valuable and doesn't depend on other features. Users with 10-20 categories can still find what they need by scrolling, but 50+ requires search.

**Independent Test**: Can be fully tested by creating 20+ categories, using the search box to filter by name, and verifying results update in real-time. Delivers value by reducing time to find categories.

**Acceptance Scenarios**:

1. **Given** I have categories "Groceries", "Gas", and "Gifts", **When** I type "G" in the search box, **Then** only categories starting with "G" are displayed
2. **Given** I am viewing search results for "Food", **When** I clear the search box, **Then** all categories are displayed again
3. **Given** I have parent category "Food" and child "Fast Food", **When** I search for "Food", **Then** both parent and child categories appear in results
4. **Given** I search for "xyz" with no matching categories, **When** the search completes, **Then** I see "No categories found" message with option to clear search
5. **Given** I am in hierarchy view with search active, **When** I search for a child category name, **Then** the matching child is shown with its parent for context

---

### User Story 4 - Inline Category Validation (Priority: P2)

Users need real-time validation feedback when creating or editing categories to prevent errors before submission. Currently, errors only appear after clicking submit. Inline validation shows duplicate names, invalid parent selections, and circular references as users type.

**Why this priority**: Improves user experience by catching errors early, but core functionality works without it. Users can still create/edit categories; they just discover validation errors later. This is a UX polish item.

**Independent Test**: Can be fully tested by attempting various invalid inputs (duplicate names, circular parent references, invalid characters) and verifying validation messages appear immediately without submitting the form.

**Acceptance Scenarios**:

1. **Given** I am creating a category and type name "Food" (which already exists), **When** I finish typing, **Then** I see inline error "Category name already exists" before clicking create
2. **Given** I am editing category "Dining Out" (child of "Food"), **When** I attempt to select "Dining Out" as its own parent, **Then** the system shows error "Category cannot be its own parent"
3. **Given** I am creating a child category under parent "Transportation", **When** I attempt to add a parent to "Transportation" while in the same form, **Then** I see error "Cannot create 3-level hierarchy"
4. **Given** I am entering a category name, **When** I type special characters like "<script>", **Then** the system sanitizes input and shows "Only letters, numbers, and basic punctuation allowed"
5. **Given** I am editing a category name field, **When** the name length exceeds 100 characters, **Then** I see "Name must be 100 characters or less"

---

### User Story 5 - Bulk Category Operations (Priority: P3)

Users need to perform actions on multiple categories at once (delete unused categories, change parent for multiple children). This is valuable during category reorganization but not essential for daily use.

**Why this priority**: Nice-to-have for power users but not critical. Most users work with one category at a time. Single-category operations from P1 provide full functionality; this adds efficiency for bulk operations.

**Independent Test**: Can be fully tested by selecting multiple categories via checkboxes, choosing a bulk action (delete, change parent), and verifying the action applies to all selected categories.

**Acceptance Scenarios**:

1. **Given** I have 5 categories with no expenses or budgets, **When** I select all 5 and click "Delete Selected", **Then** all 5 categories are deleted after confirmation
2. **Given** I have 3 child categories under different parents, **When** I select all 3 and choose "Change Parent to: Food", **Then** all 3 become children of "Food"
3. **Given** I have selected 4 categories where 2 have expenses, **When** I attempt to delete, **Then** I see "Cannot delete 2 categories with expenses. Proceed with deleting 2 unused categories?"
4. **Given** I have selected categories for bulk action, **When** I click cancel in the confirmation dialog, **Then** no changes are made and selection is cleared
5. **Given** I am in bulk selection mode, **When** I click "Select All", **Then** all non-system categories are selected (system categories remain unselectable)

---

### User Story 6 - Category Usage Analytics (Priority: P3)

Users want to see category usage statistics (total spending, budget utilization, expense trends over time) to understand spending patterns. This provides insights but requires implementation of budgets and expenses to be meaningful.

**Why this priority**: Reporting feature that adds analytical value but isn't core to category management. P1 and P2 stories enable complete category CRUD operations. Analytics enhance decision-making but aren't required for basic functionality.

**Independent Test**: Can be fully tested by creating categories with expenses and budgets, navigating to the analytics view, and verifying charts and statistics display correctly with accurate calculations.

**Acceptance Scenarios**:

1. **Given** I have category "Groceries" with $500 budget and $350 spent, **When** I view category analytics, **Then** I see "70% utilized ($350 of $500)" with a progress bar
2. **Given** I have parent category "Food" with children "Groceries" and "Dining Out", **When** I view "Food" analytics, **Then** I see aggregated spending across all child categories
3. **Given** I have expenses in "Transportation" over the last 6 months, **When** I view the trend chart, **Then** I see monthly spending amounts as a line graph
4. **Given** I have no expenses in category "Pets", **When** I view its analytics, **Then** I see "No spending data available for this category"
5. **Given** I am viewing category analytics, **When** I select a different time range (last 3 months vs last year), **Then** the charts update to show data for the selected period

---

### Edge Cases

- What happens when a user edits a category name to match an existing category? System prevents duplicate and shows inline error "Category name already exists"
- What happens when a user tries to edit a system category? Edit button is disabled and category card shows "System Category - Cannot Edit" badge
- What happens when searching with special characters or SQL injection attempts? Input is sanitized and treated as literal search string with no special processing
- What happens when deleting a parent category that has budgets but children do not? System shows error "Cannot delete - category has active budgets. Please reassign budgets first."
- What happens when network connection is lost during category edit? Form retains unsaved changes, shows "Connection lost" error, and allows retry when connection returns
- What happens when two users edit the same category simultaneously? Last save wins; first user's changes are overwritten. Optional: Show "Category was modified by another user" warning if version conflict detected
- What happens when viewing hierarchy with deeply nested categories created by API? Display only shows 2 levels (parent-child); any deeper nesting is flattened to 2 levels with warning
- What happens when category has 1000+ expenses and loading count is slow? Show "Loading..." spinner on category card until count loads; don't block page render

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide edit functionality for all user-created categories (name, icon, parent category)
- **FR-002**: System MUST prevent editing of system categories (e.g., "Uncategorized")
- **FR-003**: System MUST display budget count on each category card (e.g., "2 budgets")
- **FR-004**: System MUST display expense count on each category card (already implemented)
- **FR-005**: System MUST visually distinguish system categories from user categories (badge or styling)
- **FR-006**: System MUST disable delete button for system categories
- **FR-007**: System MUST show creation metadata (created by, created date) on category cards
- **FR-008**: System MUST provide real-time search filtering by category name
- **FR-009**: System MUST preserve hierarchy relationships in search results (show parent context for children)
- **FR-010**: System MUST validate category name uniqueness in real-time (before form submission)
- **FR-011**: System MUST validate parent category selection prevents circular references
- **FR-012**: System MUST validate parent category selection prevents 3+ level hierarchies
- **FR-013**: System MUST sanitize category name input to prevent special characters and XSS
- **FR-014**: System MUST enforce 100-character maximum for category names
- **FR-015**: System MUST prevent parent category changes for categories with active budgets
- **FR-016**: System MUST show inline validation errors without requiring form submission
- **FR-017**: System MUST support bulk selection of multiple categories via checkboxes
- **FR-018**: System MUST support bulk delete operation with confirmation dialog
- **FR-019**: System MUST support bulk parent category change operation
- **FR-020**: System MUST prevent bulk operations on system categories
- **FR-021**: System MUST show category usage analytics including budget utilization percentage
- **FR-022**: System MUST show spending trend chart for categories with historical expenses
- **FR-023**: System MUST support filtering analytics by time range (month, quarter, year)
- **FR-024**: System MUST aggregate child category statistics when viewing parent analytics
- **FR-025**: System MUST handle network errors gracefully with retry capability
- **FR-026**: System MUST persist unsaved form data during network interruptions
- **FR-027**: System MUST refresh category list after create/edit/delete operations
- **FR-028**: System MUST maintain view mode (flat vs hierarchy) across page refreshes
- **FR-029**: System MUST display loading states during async operations (create, edit, delete, search)
- **FR-030**: System MUST show empty state with helpful message when no categories exist

### Key Entities

- **Category UI State**: Client-side state management for categories; includes view mode (flat/hierarchy), search query, selected categories for bulk operations, loading/error states, form data for create/edit dialogs

- **Category Card**: Visual representation of a category; displays name, icon, parent relationship, expense count, budget count, creation metadata, system category badge, action buttons (edit, delete)

- **Category Form**: Dialog component for create/edit operations; includes fields for name, icon selection, parent category dropdown, inline validation messages, submit/cancel actions

- **Category Analytics**: Read-only view showing usage statistics; includes budget utilization chart, spending trend over time, aggregated totals for parent categories, time range selector

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can edit category properties (name, icon, parent) in under 30 seconds including form display and save
- **SC-002**: Category search returns filtered results in under 500 milliseconds for up to 100 categories
- **SC-003**: Inline validation provides feedback within 300 milliseconds of user input
- **SC-004**: Users successfully create/edit categories without validation errors on 90% of first attempts
- **SC-005**: Category cards display all relevant metadata (expenses, budgets, creator, system status) without requiring additional clicks
- **SC-006**: Bulk operations complete within 5 seconds for up to 20 selected categories
- **SC-007**: Category analytics load and render charts within 2 seconds for 12 months of data
- **SC-008**: Page maintains responsiveness with 100+ categories displayed (no UI lag or freezing)
- **SC-009**: Users can find specific categories via search in under 10 seconds on average
- **SC-010**: Error recovery (network failures, validation errors) allows users to retry without losing form data

## Assumptions *(mandatory)*

1. **Backend API Support**: All required backend endpoints exist and are functional:
   - `PUT /api/categories/{id}` for updates
   - `GET /api/categories/{id}/budget-count` for budget count (or budget count returned in category DTO)
   - `GET /api/categories/hierarchy` for hierarchical view
   - Category DTOs include all necessary fields (isSystem, createdBy, createdAt, budgetCount, expenseCount)

2. **Existing Category Page**: The current categories page (`budget-frontend/src/app/categories/page.tsx`) serves as the foundation and will be enhanced rather than rewritten

3. **Material-UI Components**: UI uses Material-UI v5 components for consistency with existing design

4. **Authentication**: User identification via X-Hass-User header is handled by existing authentication infrastructure

5. **View Mode Persistence**: Current view mode (flat/hierarchy) is stored in localStorage or component state, not server-side

6. **Icon Rendering**: Category icons are emoji characters (Unicode) that render consistently across browsers; no custom icon library needed

7. **Search Implementation**: Client-side search filtering (no backend search endpoint required for initial implementation)

8. **Budget Count Endpoint**: Backend provides budget count similar to expense count endpoint, or budget count is included in category DTO

9. **Form Validation**: Frontend validation mirrors backend validation rules (100 char limit, no duplicates, no circular refs)

10. **Error Handling**: Existing error handling patterns (try/catch with user-friendly messages) are used throughout

11. **No Optimistic Updates**: UI updates only after successful backend response (no optimistic UI updates that might fail)

12. **Single User Session**: No real-time collaboration features; changes by other users require manual page refresh to see

## Out of Scope

The following are explicitly not included in this feature:

1. **Category Import/Export**: Importing categories from CSV/JSON files or exporting for external use
2. **Category Templates**: Predefined category sets users can install (e.g., "Standard Household Budget")
3. **Category Merging**: Combining two categories into one and reassigning all expenses/budgets
4. **Category Archive**: Soft-delete functionality to hide categories without losing historical data
5. **Category Permissions**: Role-based access control for who can edit/delete categories
6. **Category Colors**: Custom color coding for categories beyond icons
7. **Category Descriptions**: Long-form text descriptions explaining category purpose
8. **Category Tags**: Additional taxonomy system for cross-cutting categorization
9. **Category Notes**: Free-form notes attached to categories
10. **Category Reordering**: Manual drag-and-drop sorting of categories in list view
11. **Category Favorites**: Pinning frequently-used categories to top of list
12. **Category History**: Audit trail showing all changes made to a category over time
13. **Category Suggestions**: AI-powered suggestions for category assignment based on expense description
14. **Multi-Currency Support**: Different currencies for different categories
15. **Category Budgets at Category Level**: Budgets are managed separately; not embedded in category management UI
16. **Mobile App**: Enhancements are web-only; mobile optimization is future work
17. **Keyboard Shortcuts**: Advanced keyboard navigation and shortcuts for power users
18. **Undo/Redo**: Reverting category changes after they're saved

## Dependencies

1. **Feature 004 Backend**: Hierarchical category budgets backend implementation (parent-child relationships, validation logic)
2. **Existing Category Entity**: Category model with parent-child relationships, system category flag, metadata fields
3. **Existing Category Service**: Backend service layer with CRUD operations and hierarchy support
4. **Existing Category Controller**: REST API endpoints for category operations
5. **Budget-Category Integration**: Budget entity must have category association for budget count to be meaningful
6. **Material-UI v5**: Frontend component library for consistent UI
7. **Next.js 14**: Frontend framework and routing
8. **Existing API Client**: Axios-based API service (`budget-frontend/src/services/api.ts`)
9. **Existing Category Service**: Frontend service layer (`budget-frontend/src/services/categoryService.ts`)
10. **Authentication Infrastructure**: X-Hass-User header injection from Feature 003

## Related Features

- **Feature 002**: Budget and Expense Management - provides the data (budgets, expenses) that categories organize
- **Feature 004**: Hierarchical Category Budgets - provides backend support for parent-child relationships and validation
- **Feature 003**: Dev Mode Default Header - provides authentication for category ownership tracking
