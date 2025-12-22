# Implementation Plan: Homepage Dashboard Update

**Branch**: `006-homepage-update` | **Date**: 2025-12-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-homepage-update/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Transform the homepage from a basic feature showcase into an actionable dashboard that provides household members with immediate visibility into their budget status, recent spending activity, and quick access to common tasks. The updated homepage will display current month budget summary, the 5 most recent expenses, quick action buttons for common workflows, and updated feature navigation reflecting all implemented features.

## Technical Context

**Language/Version**: TypeScript 5.x (frontend), Java 17 (backend)
**Primary Dependencies**: Next.js 14.x, Material-UI v5, Spring Boot 3.2.0, React 18.x
**Storage**: MySQL 8.0 (existing database with budgets and expenses tables)
**Testing**: Jest with React Testing Library (frontend - optional), JUnit 5 (backend - optional)
**Target Platform**: Web application (responsive design for desktop, tablet, mobile)
**Project Type**: Web application (Next.js frontend + Spring Boot backend)
**Performance Goals**: Homepage load under 2 seconds, API responses under 500ms
**Constraints**: Client-side routing, responsive design 320px-1920px width, graceful degradation on backend failure
**Scale/Scope**: Single household (2-10 users), <100 budgets/year, <1000 expenses/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Technical Stack Compliance ✅

- **Frontend**: Next.js - COMPLIANT (project requirement)
- **Backend**: Spring Boot (Java) - COMPLIANT (project requirement)
- **Authentication**: X-Hass-User header - COMPLIANT (existing mechanism, no changes needed)
- **Deployment**: Containerized for Home Assistant - COMPLIANT (no deployment changes required)

### Workflow Principles Compliance ✅

- **I. Specification-First**: ✅ spec.md is technology-agnostic, focused on user needs
- **II. Clarify Before Planning**: ✅ No [NEEDS CLARIFICATION] markers in spec
- **III. Incremental Delivery**: ✅ 4 user stories with clear priorities (P1, P2)
- **IV. Constitution Gates**: ✅ This check performed before Phase 0
- **V. Task Traceability**: ✅ Will be enforced in tasks.md generation
- **VI. Test-Optional**: ✅ Tests not required by spec (marking as optional)
- **VII. Artifact Consistency**: ✅ Will run /speckit.analyze before implementation

### Multi-User Support ✅

- Budget summary shows household-wide data (all users see same current month budget)
- Recent expenses display creator attribution (X-Hass-User captured in expense records)
- Quick actions available to all household members
- No user-level data isolation (shared visibility per constitution)

### Gate Result: **PASS** - No violations, proceed to Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/006-homepage-update/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/homebudget/
│   ├── controller/          # Existing REST controllers
│   │   ├── BudgetController.java
│   │   ├── ExpenseController.java
│   │   └── (no new controllers needed)
│   ├── service/             # Existing services
│   │   ├── BudgetService.java
│   │   ├── ExpenseService.java
│   │   └── (no new services needed)
│   ├── repository/          # Existing JPA repositories
│   │   ├── BudgetRepository.java
│   │   ├── ExpenseRepository.java
│   │   └── (no new repositories needed)
│   └── dto/                 # Existing DTOs
│       ├── BudgetSummaryDTO.java
│       ├── ExpenseDTO.java
│       └── (no new DTOs needed)
└── src/test/java/           # Optional tests

frontend/
├── src/
│   ├── app/
│   │   └── page.tsx                    # MODIFY - Homepage component
│   ├── components/
│   │   ├── home/                       # NEW - Homepage-specific components
│   │   │   ├── BudgetSummaryCard.tsx   # Budget overview widget
│   │   │   ├── RecentActivityCard.tsx  # Recent expenses widget
│   │   │   ├── QuickActionsCard.tsx    # Quick action buttons
│   │   │   └── FeatureNavigationCard.tsx # Feature card with navigation
│   │   └── common/                     # Existing shared components
│   ├── services/
│   │   ├── budgetService.ts            # EXISTS - getCurrentMonthBudget()
│   │   ├── expenseService.ts           # EXISTS - getAllExpenses()
│   │   └── api.ts                      # EXISTS - HTTP client
│   └── types/
│       ├── budget.ts                   # EXISTS
│       └── expense.ts                  # EXISTS
└── src/tests/                          # Optional component tests
```

**Structure Decision**: Web application structure (Option 2). This feature only modifies frontend (src/app/page.tsx) and creates new presentational components. No backend changes required - all necessary API endpoints already exist from previous features (002-budget-management, 004-hierarchical-category-budgets).

## Phase 1 Completion

### Generated Artifacts ✅

- **research.md**: Technical decisions documented (component architecture, data fetching, responsive design)
- **data-model.md**: Existing entities mapped to homepage requirements (no new entities needed)
- **contracts/api-contracts.md**: API contracts specified (reuses existing endpoints)
- **quickstart.md**: Test scenarios and integration guide created
- **Agent context**: Updated CLAUDE.md with technology stack

### Post-Design Constitution Re-Check ✅

**Technical Stack Compliance**: ✅ PASS
- Frontend: Next.js 14.x with TypeScript - COMPLIANT
- Backend: Spring Boot 3.2.0 (Java 17) - COMPLIANT
- Database: MySQL 8.0 - COMPLIANT
- Authentication: X-Hass-User header - COMPLIANT

**Design Decisions Review**:
- Component extraction (BudgetSummaryCard, RecentActivityCard, QuickActionsCard, FeatureNavigationCard) - COMPLIANT with React/Next.js patterns
- Parallel data fetching with independent loading states - COMPLIANT with performance goals
- Material-UI Grid responsive layout - COMPLIANT with existing UI patterns
- No new backend endpoints or database schema changes - COMPLIANT with minimal complexity principle

**Final Gate Result**: **PASS** - Ready for task generation

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations detected - table not applicable.
