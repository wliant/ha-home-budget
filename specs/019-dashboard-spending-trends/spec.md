# Feature Specification: Dashboard Spending Trends

**Feature Branch**: `019-dashboard-spending-trends`
**Created**: 2026-02-16
**Status**: Draft
**Input**: Enhancement on the dashboard page. Replace existing pie chart with a line chart showing spending trends. X-axis supports daily/monthly/yearly granularity (user-selectable). Y-axis shows aggregate spending amount per category. Each category is a separate line that can be toggled on/off.

## Clarifications

### Session 2026-02-16

- Q: How should users navigate between years in monthly view? → A: Monthly view has prev/next year navigation (similar to daily view's prev/next month)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Monthly Spending Trends by Category (Priority: P1)

A household member navigates to the dashboard page and sees a line chart showing their spending trends over time. By default, the chart displays monthly aggregates for the current year. Each category with expenses is plotted as a separate colored line, making it easy to see which categories are growing or shrinking over time.

**Why this priority**: This is the core value of the feature - replacing the static pie chart with a dynamic trend visualization that reveals spending patterns over time.

**Independent Test**: Navigate to the dashboard page with existing expense data across multiple months and categories. The line chart renders with one line per category, each point representing the total spending for that category in that month.

**Acceptance Scenarios**:

1. **Given** a user has expenses in 3+ categories across 6+ months, **When** they visit the dashboard, **Then** a line chart displays with one colored line per category, monthly data points on the X-axis, and spending amounts on the Y-axis.
2. **Given** a user visits the dashboard, **When** the chart loads, **Then** the default view shows monthly granularity for the current year.
3. **Given** a category has no expenses in a particular month, **When** the chart renders, **Then** that data point shows as zero for that category.
4. **Given** a user has no expense data at all, **When** they visit the dashboard, **Then** the chart area displays an informative empty state message instead of a blank chart.

---

### User Story 2 - Switch Time Granularity (Priority: P1)

A household member wants to analyze spending at different time scales. They can switch the X-axis between daily, monthly, and yearly views to zoom in on short-term spending or zoom out for long-term patterns.

**Why this priority**: Granularity selection is essential for the chart to be useful at different time horizons and was explicitly requested.

**Independent Test**: With the trend chart visible, switch between daily, monthly, and yearly views and verify the X-axis labels and data aggregation change accordingly.

**Acceptance Scenarios**:

1. **Given** the chart is displayed in monthly view, **When** the user selects "Daily", **Then** the X-axis changes to show individual days and data points are aggregated per day.
2. **Given** the chart is displayed in monthly view, **When** the user selects "Yearly", **Then** the X-axis changes to show years and data points are aggregated per year.
3. **Given** the user switches to daily view, **When** viewing a single month's daily data, **Then** the chart shows up to 31 data points along the X-axis for that month.
4. **Given** the user selects daily granularity, **When** the chart renders, **Then** it defaults to showing the current month's daily data.
5. **Given** the user selects yearly granularity, **When** the chart renders, **Then** it shows all years that have expense data.
6. **Given** the user is in monthly view for 2026, **When** the user clicks "previous year", **Then** the chart updates to show monthly data for 2025.

---

### User Story 3 - Toggle Category Visibility (Priority: P2)

A household member wants to focus on specific categories. They can toggle individual category lines on and off to reduce visual clutter or compare specific categories side-by-side.

**Why this priority**: Category toggling enhances usability but the chart is still valuable without it. This builds on the core chart from US1.

**Independent Test**: Click on a category in the chart legend to hide its line, click again to show it. Verify the Y-axis rescales appropriately.

**Acceptance Scenarios**:

1. **Given** the chart shows 5 category lines, **When** the user clicks a category in the legend, **Then** that category's line is hidden from the chart.
2. **Given** a category line is hidden, **When** the user clicks the same category in the legend again, **Then** the line reappears on the chart.
3. **Given** a user hides the highest-spending category, **When** the line is hidden, **Then** the Y-axis rescales to fit the remaining visible data.
4. **Given** all categories are hidden, **When** no lines are visible, **Then** the chart shows an empty state with axis labels still visible.

---

### Edge Cases

- What happens when there are many categories (e.g., 15+)? The chart should still be readable with distinct colors and a scrollable or wrapped legend.
- What happens when a category has only one data point? A single dot is shown instead of a line segment.
- What happens when daily view is selected but data spans many months? The date range should be bounded (e.g., show current month by default for daily view) with the ability to navigate to other periods.
- What happens when expense amounts vary dramatically between categories (e.g., $5 vs $5000)? The Y-axis should scale to accommodate, and low-value lines may appear near the bottom.
- What happens on small screens (mobile)? The chart should be responsive and remain usable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The dashboard page MUST display a line chart showing spending trends over time, replacing the existing pie chart visualization.
- **FR-002**: The chart MUST plot one line per expense category, where each data point represents the aggregate spending for that category within the selected time bucket.
- **FR-003**: The X-axis MUST support three granularity options: daily, monthly, and yearly.
- **FR-004**: Users MUST be able to switch between daily, monthly, and yearly granularity via a visible selector control on the dashboard.
- **FR-005**: The default view MUST be monthly granularity showing the current year.
- **FR-006**: For daily granularity, the default date range MUST be the current month.
- **FR-007**: For yearly granularity, the chart MUST show all years that contain expense data.
- **FR-008**: Each category line MUST have a distinct color to differentiate it from other categories.
- **FR-009**: Users MUST be able to toggle individual category lines on and off via the chart legend.
- **FR-010**: When category lines are hidden, the Y-axis MUST rescale to fit the remaining visible data.
- **FR-011**: The chart MUST display an informative empty state when no expense data exists.
- **FR-012**: The chart MUST show currency-formatted values in tooltips when hovering over data points.
- **FR-013**: For daily view, users MUST be able to navigate to different months (e.g., previous/next month controls).
- **FR-014**: The chart MUST be responsive and usable on both desktop and mobile screen sizes.
- **FR-015**: For monthly view, users MUST be able to navigate to different years via previous/next year controls.

### Key Entities

- **Expense**: Existing entity with amount, expense date, and category. The chart aggregates expense amounts by category and time bucket.
- **Category**: Existing entity with name and icon. Each category with expenses becomes a line on the chart.
- **Time Bucket**: A computed grouping (day, month, or year) that determines how expenses are aggregated on the X-axis.

## Assumptions

- The existing expense data and category data are sufficient for this feature; no new database entities are required.
- Parent categories are not aggregated separately from child categories; each category that has direct expenses gets its own line.
- The pie chart and its associated monthly summary cards on the current dashboard page will be fully removed.
- Currency formatting follows the existing application conventions.
- Color assignment to categories is automatic (system-chosen) and does not need to be user-configurable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can identify spending trends across at least 3 categories over a 6-month period within 10 seconds of viewing the dashboard.
- **SC-002**: Users can switch between daily, monthly, and yearly views in under 2 seconds with the chart updating immediately.
- **SC-003**: Users can isolate specific categories by toggling others off, with the chart updating in under 1 second.
- **SC-004**: The dashboard loads and renders the trend chart within 3 seconds on a standard connection.
- **SC-005**: The chart remains readable and interactive on screens as small as 375px wide (mobile).
