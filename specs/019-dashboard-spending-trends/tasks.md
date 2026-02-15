# Tasks: Dashboard Spending Trends

**Input**: Design documents from `/specs/019-dashboard-spending-trends/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No new project setup required. This feature works within the existing budget-backend (Spring Boot) and budget-frontend (Next.js) projects. Recharts 3.7.0 is already installed.

*No tasks in this phase.*

---

## Phase 2: Foundational

**Purpose**: No blocking foundational work required. US1 uses existing aggregate APIs. US2 backend work is scoped to its own phase. US3 is frontend-only.

*No tasks in this phase.*

**Checkpoint**: User story implementation can begin immediately.

---

## Phase 3: User Story 1 — View Monthly Spending Trends by Category (Priority: P1) MVP

**Goal**: Replace the existing dashboard pie chart with a multi-line trend chart showing monthly spending per category for the current year. Each category with expenses appears as a separate colored line.

**Independent Test**: Navigate to `/dashboard` with existing expense data across multiple months and categories. A line chart renders with one line per category, monthly data points on the X-axis, and spending amounts on the Y-axis. Empty state shows when no data exists.

### Implementation for User Story 1

- [x] T001 [US1] Create SpendingTrendChart component in `budget-frontend/src/components/dashboard/SpendingTrendChart.tsx` — Recharts LineChart with ResponsiveContainer, XAxis, YAxis, CartesianGrid, Tooltip (currency-formatted), Legend. Props: `chartData` (array of objects with time key + category amount keys), `xAxisKey` (string), `categories` (array of category names), `xAxisFormatter` (optional label formatter). Assign distinct colors from a predefined palette. Render one `Line` per category.
- [x] T002 [US1] Replace dashboard page content in `budget-frontend/src/app/dashboard/page.tsx` — Remove BudgetPieChart import and all pie chart/monthly summary card rendering. Add state for `selectedYear` (default: current year). Fetch monthly aggregates on mount via `expenseService.getMonthlyAggregates(selectedYear)`. Transform flat `CategoryExpenseAggregate[]` into Recharts format: `[{month: 1, "Food": 100, "Housing": 200}, ...]` filling in zero for months with no data. Extract unique category names. Render SpendingTrendChart with `xAxisKey="month"` and month name formatter. Show empty state message when no data. Keep Dashboard header icon and title.

**Checkpoint**: Dashboard shows monthly spending trend chart for current year. Fully functional MVP.

---

## Phase 4: User Story 2 — Switch Time Granularity (Priority: P1)

**Goal**: Allow users to switch the chart between daily, monthly, and yearly granularity. Daily view shows a specific month's daily data with prev/next month navigation. Monthly view shows a year's monthly data with prev/next year navigation. Yearly view shows all years with expense data.

**Independent Test**: With the trend chart visible, switch between Daily, Monthly, and Yearly views. Verify X-axis labels and data aggregation change accordingly. Navigate between periods using prev/next buttons.

### Implementation for User Story 2

- [x] T003 [P] [US2] Add `day` field (Integer, nullable, with getter/setter) to `budget-backend/src/main/java/com/homebudget/dto/CategoryExpenseAggregateDTO.java`
- [x] T004 [P] [US2] Add daily aggregate JPQL query `getDailyAggregatesByYearAndMonth(year, month)` to `budget-backend/src/main/java/com/homebudget/repository/ExpenseRepository.java` — Query: `SELECT e.category.id, DAY(e.expenseDate), COALESCE(SUM(e.amount), 0) FROM Expense e WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month GROUP BY e.category.id, DAY(e.expenseDate)`. Returns `List<Object[]>` with [categoryId, day, amount].
- [x] T005 [US2] Add `getDailyAggregates(Integer year, Integer month)` method to `budget-backend/src/main/java/com/homebudget/service/ExpenseAggregateService.java` — Follow existing monthly aggregate pattern: fetch raw aggregates from repo, load categories, build DTOs with parent rollup. Set `day` field on each DTO.
- [x] T006 [US2] Add `GET /api/expenses/aggregates/daily` endpoint to `budget-backend/src/main/java/com/homebudget/controller/ExpenseController.java` — Parameters: `year` (required), `month` (required, validate 1-12). Calls `expenseAggregateService.getDailyAggregates(year, month)`. Returns `ResponseEntity<List<CategoryExpenseAggregateDTO>>`.
- [x] T007 [P] [US2] Add `day` field to frontend `CategoryExpenseAggregate` type and add `getDailyAggregates(year, month)` method to `budget-frontend/src/services/expenseService.ts` — New method calls `GET /api/expenses/aggregates/daily?year=${year}&month=${month}`.
- [x] T008 [US2] Add granularity selector and period navigation to `budget-frontend/src/app/dashboard/page.tsx` — Add state: `granularity` ('daily'|'monthly'|'yearly'), `selectedMonth` (default: current month). Render MUI ToggleButtonGroup (Daily/Monthly/Yearly) above the chart. Add prev/next navigation buttons: for daily view show prev/next month, for monthly view show prev/next year, for yearly view show no navigation. Update data fetching: daily calls `getDailyAggregates(year, month)`, monthly calls `getMonthlyAggregates(year)`, yearly calls `getExpenseYears()` then `getYearlyAggregates(year)` for each year. Transform data appropriately per granularity (xAxisKey: 'day'|'month'|'year').
- [x] T009 [US2] Update SpendingTrendChart to support different X-axis formats in `budget-frontend/src/components/dashboard/SpendingTrendChart.tsx` — Accept optional `granularity` prop. Format X-axis labels: daily shows day numbers (1-31), monthly shows month abbreviations (Jan-Dec), yearly shows year numbers. Adjust tooltip label formatting per granularity.

**Checkpoint**: Dashboard supports switching between daily, monthly, and yearly trend views with period navigation. All three granularities functional.

---

## Phase 5: User Story 3 — Toggle Category Visibility (Priority: P2)

**Goal**: Allow users to toggle individual category lines on and off by clicking the chart legend. Hidden lines are removed and the Y-axis rescales to fit remaining visible data.

**Independent Test**: Click a category in the chart legend to hide its line, click again to show it. Verify the Y-axis rescales when the highest-spending category is hidden.

### Implementation for User Story 3

- [x] T010 [US3] Add hidden categories state and legend toggle handler in `budget-frontend/src/app/dashboard/page.tsx` — Add `hiddenCategories: Set<string>` state. Create `handleToggleCategory(categoryName)` callback that adds/removes from set. Pass `hiddenCategories` and `onToggleCategory` props to SpendingTrendChart.
- [x] T011 [US3] Update SpendingTrendChart to support category toggling in `budget-frontend/src/components/dashboard/SpendingTrendChart.tsx` — Accept `hiddenCategories` (Set<string>) and `onToggleCategory` (callback) props. Set `hide={true}` on Line components whose category name is in `hiddenCategories`. Add `onClick` handler to Legend that calls `onToggleCategory`. Style hidden legend items with muted/strikethrough appearance.

**Checkpoint**: All user stories complete. Category lines can be toggled on/off via legend clicks.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Responsive design and validation

- [x] T012 Ensure responsive layout for mobile (375px) in `budget-frontend/src/components/dashboard/SpendingTrendChart.tsx` and `budget-frontend/src/app/dashboard/page.tsx` — Verify chart is usable at small widths, legend wraps appropriately, granularity selector stacks vertically on mobile, navigation buttons remain accessible.
- [x] T013 Run quickstart.md validation scenarios against running application to verify all 6 integration scenarios pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Skipped — no setup needed
- **Foundational (Phase 2)**: Skipped — no blocking prerequisites
- **US1 (Phase 3)**: Can start immediately — uses existing APIs only
- **US2 (Phase 4)**: Can start after US1 (extends dashboard page and chart component)
- **US3 (Phase 5)**: Can start after US1 (extends dashboard page and chart component); independent of US2
- **Polish (Phase 6)**: After all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies — uses existing `getMonthlyAggregates` API
- **User Story 2 (P1)**: Depends on US1 (extends the chart and page created in US1); requires new backend endpoint
- **User Story 3 (P2)**: Depends on US1 (extends the chart and page created in US1); independent of US2

### Within User Story 2

- T003 (DTO) and T004 (repo query) — parallel [P], different files
- T005 (service) — depends on T004
- T006 (controller) — depends on T005
- T007 (frontend service) — parallel [P] with T003-T006, different project
- T008 (page update) — depends on T006 and T007
- T009 (chart update) — depends on T008 (needs to know granularity context)

### Parallel Opportunities

```text
# US2 backend + frontend service can run in parallel:
T003 [P] Add day field to DTO          ─┐
T004 [P] Add daily aggregate query      ├── parallel (different files)
T007 [P] Add frontend service method   ─┘

# Then sequential:
T005 → T006 → T008 → T009
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete T001-T002: Monthly trend chart on dashboard
2. **STOP and VALIDATE**: Navigate to `/dashboard`, verify chart renders with monthly data
3. This delivers the core value — spending trends visible at a glance

### Incremental Delivery

1. T001-T002 → Monthly trend chart (MVP)
2. T003-T009 → Daily + Yearly views with navigation
3. T010-T011 → Category toggle via legend
4. T012-T013 → Polish and validation
5. Each increment adds value without breaking previous functionality

---

## Notes

- Recharts 3.7.0 is already installed — no dependency changes needed
- Existing `BudgetPieChart.tsx` can remain in codebase (just not imported by dashboard)
- Monthly and yearly aggregate APIs already exist (Feature 016) — only daily is new
- All aggregate endpoints are read-only; no authentication changes needed
- The `CategoryExpenseAggregate` type's new `day` field is nullable and backward-compatible
