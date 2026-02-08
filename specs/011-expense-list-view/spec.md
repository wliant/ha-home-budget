# Feature Specification: Expense List View

**Feature Branch**: `011-expense-list-view`
**Created**: 2026-02-09
**Status**: Draft
**Input**: User description: "implement the expense list view. it should be able to show all the expenses. minimal, a year selection filter is mandatory and default to current year, but can be changed. other filters are month, categories, amount range, created by. the list of expenses should contain date, description, categories, amount, created by. sorting functionality should be allowed"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Expenses for Current Year (Priority: P1)

As a household member, I want to see a list of all expenses for the current year so that I can review household spending at a glance.

When I navigate to the expense list view, the system automatically loads all expenses for the current year. Each expense row shows the date, description, category (with icon), amount, and who created it. This gives me an immediate overview of all household spending without needing to configure any filters.

**Why this priority**: This is the core functionality — viewing expenses is the primary purpose of the feature. Without this, no other functionality is useful.

**Independent Test**: Can be fully tested by navigating to the expense list page and verifying that expenses for the current year are displayed in a tabular format with all required columns.

**Acceptance Scenarios**:

1. **Given** expenses exist for the current year, **When** I open the expense list view, **Then** I see all expenses for the current year displayed in a table with columns: date, description, category, amount, and created by.
2. **Given** no expenses exist for the current year, **When** I open the expense list view, **Then** I see a clear empty state message indicating no expenses were found for the selected year.
3. **Given** expenses exist across multiple years, **When** I open the expense list view, **Then** only expenses for the current year are shown by default.

---

### User Story 2 - Filter Expenses by Year (Priority: P1)

As a household member, I want to change the year filter so that I can view expenses from previous years.

The year filter is always visible and mandatory. It defaults to the current year but I can select any year that has expense data. When I change the year, the expense list updates to show only expenses from the selected year.

**Why this priority**: Year filtering is explicitly required as mandatory and is essential for accessing historical expense data.

**Independent Test**: Can be fully tested by changing the year selector and verifying the list updates to show expenses only from the selected year.

**Acceptance Scenarios**:

1. **Given** I am viewing expenses for 2026, **When** I change the year filter to 2025, **Then** the list updates to show only expenses from 2025.
2. **Given** the year filter is displayed, **When** I look at the filter, **Then** it shows the current year as the default selection.
3. **Given** I select a year with no expenses, **When** the list updates, **Then** I see an empty state message for that year.

---

### User Story 3 - Filter Expenses by Additional Criteria (Priority: P2)

As a household member, I want to narrow down the expense list using optional filters (month, category, amount range, created by) so that I can find specific expenses quickly.

In addition to the mandatory year filter, I can optionally filter by month, one or more categories, a minimum and/or maximum amount, and which household member created the expense. These filters work together with the year filter. I can apply multiple filters simultaneously and clear them individually or all at once.

**Why this priority**: While the year filter provides basic navigation, additional filters are needed for finding specific expenses in a potentially long list.

**Independent Test**: Can be fully tested by applying various filter combinations and verifying that only matching expenses appear in the list.

**Acceptance Scenarios**:

1. **Given** I am viewing expenses for 2026, **When** I select month "March", **Then** only expenses from March 2026 are shown.
2. **Given** I am viewing expenses, **When** I select the category "Groceries", **Then** only expenses categorized as Groceries are shown.
3. **Given** I am viewing expenses, **When** I set a minimum amount of 50 and a maximum amount of 200, **Then** only expenses with amounts between 50 and 200 (inclusive) are shown.
4. **Given** I am viewing expenses, **When** I select a household member from the "created by" filter, **Then** only expenses created by that member are shown.
5. **Given** I have multiple filters active (year 2026, month January, category Groceries), **When** I clear all filters, **Then** the year resets to the current year and all optional filters are removed.
6. **Given** I have the month filter set to "June", **When** I also set a category filter, **Then** both filters are applied together and only matching expenses are shown.

---

### User Story 4 - Sort Expense List (Priority: P2)

As a household member, I want to sort the expense list by different columns so that I can organize the data in a way that helps me find what I'm looking for.

I can click on column headers to sort the expense list by that column. Clicking the same column header toggles between ascending and descending order. A visual indicator shows which column is currently sorted and in which direction.

**Why this priority**: Sorting complements filtering to help users organize and locate expenses. It is a standard expectation for any list/table view.

**Independent Test**: Can be fully tested by clicking column headers and verifying the list reorders correctly with visual sort indicators.

**Acceptance Scenarios**:

1. **Given** I am viewing an expense list, **When** I click the "Date" column header, **Then** the list is sorted by date in ascending order.
2. **Given** the list is sorted by date ascending, **When** I click the "Date" column header again, **Then** the sort order toggles to descending.
3. **Given** I am viewing an expense list, **When** I click the "Amount" column header, **Then** the list is sorted by amount.
4. **Given** a column is being sorted, **When** I look at the column header, **Then** I see a visual indicator showing the current sort direction (ascending or descending).
5. **Given** the list is sorted by amount, **When** I click the "Description" column header, **Then** the list re-sorts by description and the previous sort indicator is removed.

---

### Edge Cases

- What happens when the expense list contains thousands of entries? The system paginates the results to maintain performance and usability.
- What happens when a user applies filters that result in zero matches? A clear "no results" message is shown with guidance to adjust filters.
- What happens when an expense has no category assigned? The category column shows "Uncategorized" or a dash.
- What happens when the amount range filter has the minimum greater than the maximum? The system prevents this invalid state with appropriate validation.
- What happens when the user's screen is small (mobile)? The table adapts to smaller screens with a responsive layout.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a dedicated expense list view accessible from the main navigation.
- **FR-002**: System MUST show expenses in a tabular format with columns: date, description, category (with icon), amount, and created by.
- **FR-003**: System MUST provide a mandatory year filter that defaults to the current year.
- **FR-004**: System MUST allow the user to change the year filter to view expenses from other years.
- **FR-005**: System MUST provide an optional month filter to narrow expenses within the selected year.
- **FR-006**: System MUST provide an optional category filter allowing selection of one or more categories.
- **FR-007**: System MUST provide an optional amount range filter with minimum and/or maximum amount inputs.
- **FR-008**: System MUST provide an optional "created by" filter to show expenses by a specific household member.
- **FR-009**: System MUST apply all active filters together (AND logic) and update the expense list immediately when any filter value changes (auto-apply, no separate "Apply" button).
- **FR-010**: System MUST allow sorting by any displayed column (date, description, category, amount, created by) using server-side sorting to ensure correct ordering across paginated results.
- **FR-011**: System MUST toggle sort direction (ascending/descending) when the same column header is clicked again.
- **FR-012**: System MUST display a visual indicator on the currently sorted column showing sort direction.
- **FR-013**: System MUST show a meaningful empty state when no expenses match the current filters.
- **FR-014**: System MUST format amounts as currency and dates in a human-readable format.
- **FR-015**: System MUST display "Uncategorized" or equivalent for expenses without a category.
- **FR-016**: System MUST provide a way to clear all optional filters (year resets to current year).
- **FR-017**: System MUST paginate the expense list with 50 items per page to handle large datasets efficiently.
- **FR-018**: System MUST validate that the amount range minimum does not exceed the maximum.
- **FR-019**: System MUST display a summary showing the total count of matching expenses and the sum of their amounts (e.g., "23 expenses totaling $1,245.00"), updated whenever filters or sorting change.

### Key Entities

- **Expense**: A recorded spending item with amount, description, date, optional category, and the household member who created it. This is the primary entity displayed in the list.
- **Category**: A classification for expenses (e.g., Groceries, Utilities). Supports hierarchical parent-child relationships and includes an icon. Used as a filter option.
- **Household Member**: A user identified by their username (from the home network proxy). Used for the "created by" column and filter.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view the full expense list for a selected year within 2 seconds of page load.
- **SC-002**: Users can apply or change any filter and see updated results within 1 second.
- **SC-003**: Users can sort by any column and see reordered results within 1 second.
- **SC-004**: The expense list correctly displays all five required columns (date, description, category, amount, created by) for every expense entry.
- **SC-005**: The year filter defaults to the current year on every fresh page load.
- **SC-006**: All filter combinations produce accurate results (no expenses shown that don't match active filters).
- **SC-007**: The expense list remains usable and readable on screens as small as 375px wide.

## Clarifications

### Session 2026-02-09

- Q: How many expenses per page for pagination? → A: 50 items per page
- Q: Should filters auto-apply or require a manual "Apply" button? → A: Auto-apply immediately on change
- Q: Should sorting be server-side or client-side? → A: Server-side (accurate across paginated pages)
- Q: Should the list show a total count and/or sum of amounts? → A: Both count and total amount (e.g., "23 expenses totaling $1,245.00")

## Assumptions

- The existing expense data model and API endpoints (which already support filtering by date range, category, and created by) will serve as the foundation for this feature.
- The year filter will offer years derived from existing expense data (i.e., years that have at least one expense recorded), plus the current year.
- Amount range filtering and sorting will require backend API enhancements since the current API supports date range, category, and created by filters but not amount range or server-side sorting.
- Pagination will use a standard page-based approach (e.g., page number and page size) rather than infinite scrolling.
- The default sort order is by date descending (most recent first), matching the current backend default.
- Category filter displays all available categories (including hierarchical ones shown in a flat list with parent context).
- The "created by" filter options are populated from distinct creators found in expense data rather than a separate user registry.
