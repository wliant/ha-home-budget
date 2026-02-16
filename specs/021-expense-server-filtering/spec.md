# Feature Specification: Expense Server-Side Filtering

**Feature Branch**: `021-expense-server-filtering`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "expenses page enhancement to support comprehensive filtering. when any filter change happen, it is always a server side filtering. design the api such that it can support the filtering. the api should support passing a list of categories. it will fetch expense belonging to those categories only. no need to specifically include child categories. if the client wants to retrieve expenses belonging to all child categories of a parent as well, the client will need to include them in the list of categories."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Multi-Category Server-Side Filtering (Priority: P1)

A household member viewing the expenses page selects multiple categories using the category chip filter. When they select a parent category (e.g., "Food & Dining"), the system includes the parent and all its child category IDs in a single server request. The server returns only expenses belonging to those exact categories, with correct pagination, total count, and total amount.

**Why this priority**: This is the core feature. Currently, multi-category filtering is done client-side by fetching up to 10,000 records and filtering in the browser. This is inefficient and produces inaccurate pagination. Moving to server-side filtering fixes correctness and performance.

**Independent Test**: Can be tested by selecting multiple categories on the expenses page and verifying the returned expenses, total count, total amount, and pagination are all accurate and match server-side query results.

**Acceptance Scenarios**:

1. **Given** a user is on the expenses page with expenses across multiple categories, **When** they select a parent category chip (e.g., "Food & Dining"), **Then** the system sends a request with all relevant category IDs (parent + its children) and displays only matching expenses with correct totals and pagination.
2. **Given** a user has selected a parent category, **When** they deselect one child category chip (e.g., "Coffee"), **Then** the system sends a request excluding that child's ID and the results update accordingly.
3. **Given** a user has selected categories from multiple parent groups (e.g., "Food & Dining" children + "Housing" children), **When** the page loads, **Then** results include expenses from all selected categories with correct total count and total amount.
4. **Given** a user has selected categories, **When** they click "All" to clear the category filter, **Then** the system sends a request with no category filter and returns all expenses for the current period.

---

### User Story 2 - Combined Server-Side Filters (Priority: P1)

A household member uses multiple filters simultaneously: year, month, category selection, amount range, and created-by. Every filter change triggers a server-side request with all active filter parameters. The server applies all filters together and returns correctly paginated results.

**Why this priority**: Equally critical as US1 because filters must work together. The current system already supports year, month, amount, and created-by server-side, but category filtering is client-side. All filters must be unified server-side.

**Independent Test**: Can be tested by applying year + month + multiple categories + amount range filters together and verifying results match expected data.

**Acceptance Scenarios**:

1. **Given** a user has selected year 2026, month February, categories "Groceries" and "Restaurants", and min amount $10, **When** the page loads, **Then** only expenses matching ALL criteria are shown with correct totals.
2. **Given** a user has active category and amount filters, **When** they change the year filter, **Then** a single server request is made with all filters (including category IDs) and results update.
3. **Given** a user clicks "Clear Filters", **When** the page reloads, **Then** all filters are reset (including category selection) and the server returns unfiltered results for the default year.

---

### User Story 3 - Pagination with Server-Side Category Filter (Priority: P2)

When a user has category filters active and navigates between pages, each page request includes the category filter. The server returns the correct page of filtered results with accurate total count, total amount, and page numbers.

**Why this priority**: Pagination correctness is a natural consequence of server-side filtering but needs explicit validation to ensure seamless navigation.

**Independent Test**: Can be tested by selecting categories that match more than 50 expenses and navigating between pages to verify continuity and correct counts.

**Acceptance Scenarios**:

1. **Given** a user has selected categories with 120 matching expenses, **When** they view page 1, **Then** 50 expenses are shown, total count shows 120, and pagination shows 3 pages.
2. **Given** a user is on page 2 with category filters active, **When** they navigate to page 3, **Then** the server request includes the category IDs and page 3 data is returned correctly.
3. **Given** a user changes category selection while on page 3, **When** results update, **Then** the page resets to page 1 of the new filtered results.

---

### Edge Cases

- What happens when no categories are selected (empty list)? The system treats this as "no category filter" and returns all expenses for the period.
- What happens when selected categories have no matching expenses? The system returns an empty result set with total count 0 and total amount 0.
- What happens when the user selects a parent category that has no children? Only expenses filed directly under that parent category are returned.
- What happens when the category list contains invalid or non-existent IDs? The server ignores invalid IDs and filters by only the valid ones. If all IDs are invalid, it returns an empty result set.
- What happens when sort order is changed while category filters are active? The server applies sorting to the filtered results and returns the correct page.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The expense list endpoint MUST accept a list of category IDs as a filter parameter, in addition to the existing single category ID parameter.
- **FR-002**: When a list of category IDs is provided, the system MUST return only expenses whose category matches one of the provided IDs. No automatic child category expansion is performed.
- **FR-003**: When the category list is empty or not provided, the system MUST return expenses without any category filter (same as current behavior with no categoryId).
- **FR-004**: The server MUST compute total count, total amount, and pagination based on the filtered result set (including category list filter).
- **FR-005**: The frontend MUST send category IDs to the server on every filter change, including category selection, deselection, year/month change, and pagination.
- **FR-006**: The frontend MUST NOT perform client-side filtering of expenses by category. All category filtering MUST be server-side.
- **FR-007**: When a user selects a parent category chip, the frontend MUST include the parent's ID and all its children's IDs in the category list sent to the server.
- **FR-008**: When a user deselects a child category chip, the frontend MUST remove only that child's ID from the category list.
- **FR-009**: The frontend MUST reset to page 1 when category selection changes.
- **FR-010**: The existing single-category filter parameter MUST continue to work for backward compatibility (e.g., deep links, other pages).

### Key Entities

- **Expense**: An individual expense record with amount, date, description, and a single category assignment.
- **Category**: A classification for expenses. Categories form a two-level hierarchy (parent and children). An expense belongs to exactly one category (typically a child, but can be a parent with no children).
- **Category List Filter**: A set of category IDs sent by the client to filter expenses. The server matches expenses whose categoryId is in this set.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All category filter operations produce correct results with accurate total count and total amount, matching the actual database query results.
- **SC-002**: Pagination is accurate when category filters are active: navigating pages returns the correct sequential slice of filtered results.
- **SC-003**: Filter changes (category, year, month, amount, user) each produce a single server request with all active filter parameters. No client-side post-filtering is performed for categories.
- **SC-004**: Selecting 5+ categories with thousands of matching expenses returns results within normal page load time, comparable to unfiltered queries.
- **SC-005**: Existing expense page functionality (sorting, editing, deleting, file attachments) continues to work correctly with category filters active.

## Assumptions

- The frontend already has the category hierarchy available via `getCategoryHierarchy()` and can compute the full list of IDs (parent + children) when a parent chip is clicked.
- The backend already supports filtering by a single `categoryId` with automatic child expansion. This feature replaces that behavior with explicit client-provided category lists.
- The maximum number of categories is small (under 100), so passing a list of IDs in a query parameter is practical.
- The existing `categoryId` single-value parameter is retained for backward compatibility but the frontend expenses page will switch to using the new list parameter.
