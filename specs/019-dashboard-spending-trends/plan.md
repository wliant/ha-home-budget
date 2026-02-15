# Implementation Plan: Dashboard Spending Trends

**Branch**: `019-dashboard-spending-trends` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/019-dashboard-spending-trends/spec.md`

## Summary

Replace the existing dashboard pie chart with a multi-line trend chart showing spending per category over time. The chart supports three granularity levels (daily, monthly, yearly) with user-selectable toggling. Uses Recharts LineChart (already installed) for visualization. Requires one new backend endpoint for daily aggregates; monthly and yearly aggregates reuse existing Feature 016 endpoints.

## Technical Context

**Language/Version**: TypeScript 5.x (frontend), Java 17 (backend)
**Primary Dependencies**: Next.js 14.x, Material-UI v5, Recharts 3.7.0 (existing), Spring Boot 3.2.0, Spring Data JPA
**Storage**: MySQL 8.0 (existing database with expenses and categories tables)
**Testing**: Manual testing (no automated tests unless requested)
**Target Platform**: Home Assistant add-on (private home network), desktop and mobile browsers
**Project Type**: Web application (frontend + backend)
**Performance Goals**: Chart renders within 3 seconds on standard connection; granularity switch updates in under 2 seconds
**Constraints**: Responsive down to 375px width (mobile); household-scale data (hundreds to low thousands of expenses)
**Scale/Scope**: Single dashboard page replacement; 1 new backend endpoint; 1 new frontend component

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | spec.md written and validated (16/16 checklist) |
| II. Clarify Before Planning | PASS | `/speckit.clarify` completed with 1 question resolved |
| III. Incremental Story-Based Delivery | PASS | 3 user stories (P1: monthly trends, P2: granularity switch, P3: category toggle) |
| IV. Constitution Gates | PASS | This check; re-check after Phase 1 below |
| V. Task Traceability | PENDING | Will be validated in `/speckit.tasks` |
| VI. Test-Optional | PASS | No tests requested in spec |
| VII. Artifact Consistency | PENDING | Will be validated in `/speckit.analyze` |
| Technical Stack: Next.js frontend | PASS | Frontend changes in Next.js app |
| Technical Stack: Spring Boot backend | PASS | New daily aggregate endpoint in Spring Boot |
| Authentication: X-Hass-User | PASS | Aggregate endpoints are read-only, no auth changes needed |
| Multi-User Household | PASS | Aggregates are shared (all household expenses combined) |

**Post Phase 1 Re-check**: All gates PASS. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/019-dashboard-spending-trends/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── api.md           # API contract documentation
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── controller/
│   │   └── ExpenseController.java        # Add daily aggregate endpoint
│   ├── dto/
│   │   └── CategoryExpenseAggregateDTO.java  # Add day field
│   ├── repository/
│   │   └── ExpenseRepository.java        # Add daily aggregate query
│   └── service/
│       └── ExpenseAggregateService.java  # Add daily aggregate method

budget-frontend/
├── src/
│   ├── app/dashboard/
│   │   └── page.tsx                      # Replace entirely with trend chart
│   ├── components/dashboard/
│   │   ├── BudgetPieChart.tsx            # No longer imported (leave file)
│   │   └── SpendingTrendChart.tsx        # NEW: Line chart component
│   └── services/
│       └── expenseService.ts             # Add getDailyAggregates method
```

**Structure Decision**: Web application structure (existing). Backend adds one query + service method + controller endpoint. Frontend replaces dashboard page content and adds a new chart component.

## Architecture Overview

### Data Flow

```
ExpenseRepository (JPQL query)
    ↓
ExpenseAggregateService (parent rollup logic)
    ↓
ExpenseController (REST endpoint)
    ↓
expenseService.ts (API client)
    ↓
DashboardPage (state management, granularity selection)
    ↓
SpendingTrendChart (Recharts LineChart rendering)
```

### Granularity Handling

| Granularity | API Call | X-Axis | Navigation |
|-------------|----------|--------|------------|
| Daily | `GET /aggregates/daily?year=Y&month=M` | Days 1–31 | Prev/Next month |
| Monthly | `GET /aggregates/monthly?year=Y` | Months Jan–Dec | Prev/Next year |
| Yearly | `GET /years` + `GET /aggregates/yearly?year=Y` per year | Years | None (shows all) |

### Frontend Component Design

**DashboardPage** (`page.tsx`):
- State: `granularity` (daily/monthly/yearly), `selectedYear`, `selectedMonth`, `hiddenCategories`
- Fetches aggregate data based on granularity + period
- Renders granularity selector (ToggleButtonGroup) + period navigation + chart

**SpendingTrendChart** (new component):
- Receives: transformed time-series data, hidden categories set, toggle callback
- Renders: Recharts LineChart with one Line per category
- Handles: Legend click → toggle callback, Tooltip formatting, responsive sizing

### Data Transformation

The aggregate API returns `CategoryExpenseAggregateDTO[]` (flat list with category + time bucket). The frontend transforms this into Recharts-compatible format:

```typescript
// Input: [{ categoryId: 1, categoryName: "Food", totalAmount: 100, month: 1 }, ...]
// Output: [{ month: 1, "Food": 100, "Housing": 200 }, { month: 2, "Food": 150, "Housing": 180 }, ...]
```

Each object represents one time point, with dynamic keys for each category name.

## Complexity Tracking

> No constitution violations to justify.
