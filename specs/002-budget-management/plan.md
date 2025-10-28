# Implementation Plan: Budget and Expense Management

**Branch**: `002-budget-management` | **Date**: 2025-10-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-budget-management/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature implements a household budget and expense tracking system with four prioritized user stories:
- **P1 (MVP)**: Create and view monthly budgets with spending totals
- **P2**: Record expenses against budgets with user accountability and category tracking
- **P3**: Manage custom spending categories with icons
- **P4**: Dashboard with budget insights, trends, and analytics

Technical approach leverages existing Next.js 14 frontend and Spring Boot 3.2 backend from Feature 001 (Project Scaffolding). Backend provides REST APIs for budget/expense CRUD operations with MySQL persistence via JPA. Frontend implements Material-UI forms and data tables for budget management. User identity extracted from X-Hass-User header per Home Assistant authentication model.

## Technical Context

**Language/Version**:
- Frontend: TypeScript 5.x with Next.js 14
- Backend: Java 17 with Spring Boot 3.2

**Primary Dependencies**:
- Frontend: Next.js 14, React 18, Material-UI v5, Axios
- Backend: Spring Web, Spring Data JPA, Liquibase, Hibernate Validator, MySQL Connector

**Storage**: MySQL 8.0 (via Docker Compose from Feature 001)

**Testing**:
- Frontend: Jest, React Testing Library
- Backend: JUnit 5, MockMvc, H2 in-memory database for tests

**Target Platform**:
- Frontend: Docker container (Node.js 18 Alpine base)
- Backend: Docker container (Maven 3.9 + Java 17 base)
- Deployment: Home Assistant Add-on via nginx proxy

**Project Type**: Web application (existing structure from Feature 001)

**Performance Goals**:
- Budget list rendering: <1 second (SC-003)
- Expense entry: <20 seconds end-to-end (SC-002)
- Category breakdown calculation: <2 seconds for 200 expenses (SC-007)
- Support 500 expenses per budget without degradation (SC-004)

**Constraints**:
- Multi-user concurrent access required (FR-016)
- Data persistence across restarts (FR-015)
- Zero calculation errors for totals/percentages (SC-008)
- Updates visible within 5 seconds across users (SC-005)

**Scale/Scope**:
- 4 main entities (Budget, Expense, Category, User reference)
- ~12 REST API endpoints (CRUD for 3 entities + analytics)
- 6 frontend pages/views (budget list, budget detail, expense form, category management, dashboard, expense list)
- Estimated 3-4 database tables with relationships

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Specification-First ✅ PASS
- **Requirement**: Features begin with technology-agnostic spec describing WHAT and WHY
- **Status**: PASS - spec.md contains no implementation details, focuses on user needs
- **Evidence**: Spec defines budgets, expenses, categories without mentioning JPA, MySQL, Next.js

### Principle II: Clarify Before Planning ✅ PASS
- **Requirement**: Specifications must be clarified before planning
- **Status**: PASS - spec.md contains zero [NEEDS CLARIFICATION] markers
- **Evidence**: All requirements are concrete and measurable (per requirements.md checklist)

### Principle III: Incremental, Story-Based Delivery ✅ PASS
- **Requirement**: Features decomposed into prioritized user stories (P1, P2, P3...)
- **Status**: PASS - 4 prioritized user stories, P1 identified as MVP
- **Evidence**:
  - P1 (MVP): Create/View Budgets - independently testable
  - P2: Record Expenses - builds on P1
  - P3: Manage Categories - enhances P2
  - P4: Dashboard - aggregates P1-P3

### Principle IV: Constitution Gates ✅ PASS
- **Requirement**: Plans must pass validation before Phase 0, re-validate after Phase 1
- **Status**: PASS - completing validation now before Phase 0 research
- **Evidence**: This Constitution Check section

### Principle V: Task Traceability ⏳ DEFERRED
- **Requirement**: Tasks must follow format `[T###] [P] [US#] Description`
- **Status**: DEFERRED - tasks.md created in `/speckit.tasks` command (not yet executed)
- **Evidence**: Will be verified during task generation phase

### Principle VI: Test-Optional, Test-First When Included ⚠️ REVIEW NEEDED
- **Requirement**: Tests only when explicitly requested; TDD when tests included
- **Status**: REVIEW NEEDED - spec does not explicitly request tests
- **Decision**: **No automated tests for MVP (P1-P2)**, manual testing via acceptance scenarios sufficient for initial household use. Tests may be added in future iterations if needed.
- **Rationale**: Per Principle VI, tests are optional. This is a home budget tracker for private household use (not production SaaS). Acceptance scenarios provide clear manual test cases. Focus effort on user value over test infrastructure for MVP.

### Principle VII: Artifact Consistency ⏳ DEFERRED
- **Requirement**: Validate consistency via `/speckit.analyze` after task generation
- **Status**: DEFERRED - will run `/speckit.analyze` after `/speckit.tasks`
- **Evidence**: Analysis command runs after task generation per workflow

### Project-Specific Constraints ✅ PASS

**Technical Stack Compliance**:
- ✅ Frontend: Next.js (as required)
- ✅ Backend: Spring Boot Java (as required)
- ✅ Authentication: X-Hass-User header integration (as required)
- ✅ Multi-user support: FR-016, FR-017, FR-006 address household collaboration

**GATE RESULT**: ✅ **PASS** - Proceed to Phase 0 Research

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── model/
│   │   ├── Budget.java
│   │   ├── Expense.java
│   │   └── Category.java
│   ├── repository/
│   │   ├── BudgetRepository.java
│   │   ├── ExpenseRepository.java
│   │   └── CategoryRepository.java
│   ├── service/
│   │   ├── BudgetService.java
│   │   ├── ExpenseService.java
│   │   └── CategoryService.java
│   ├── controller/
│   │   ├── BudgetController.java
│   │   ├── ExpenseController.java
│   │   └── CategoryController.java
│   ├── dto/
│   │   ├── BudgetDTO.java
│   │   ├── ExpenseDTO.java
│   │   ├── CategoryDTO.java
│   │   └── BudgetSummaryDTO.java
│   └── exception/
│       ├── BudgetNotFoundException.java
│       ├── DuplicateBudgetException.java
│       └── CategoryInUseException.java
├── src/main/resources/
│   └── db/changelog/changes/
│       ├── 003-create-budgets-table.xml
│       ├── 004-create-categories-table.xml
│       └── 005-create-expenses-table.xml
└── src/test/java/com/homebudget/
    └── (optional - manual testing via acceptance scenarios)

budget-frontend/
├── src/app/
│   ├── budgets/
│   │   ├── page.tsx                    # Budget list view
│   │   ├── [id]/page.tsx               # Budget detail view
│   │   └── new/page.tsx                # Create budget form
│   ├── expenses/
│   │   ├── page.tsx                    # Expense list view
│   │   └── new/page.tsx                # Add expense form
│   ├── categories/
│   │   └── page.tsx                    # Category management
│   └── dashboard/
│       └── page.tsx                    # P4: Dashboard (future)
├── src/components/
│   ├── budgets/
│   │   ├── BudgetCard.tsx
│   │   ├── BudgetForm.tsx
│   │   └── BudgetSummary.tsx
│   ├── expenses/
│   │   ├── ExpenseList.tsx
│   │   ├── ExpenseForm.tsx
│   │   └── ExpenseItem.tsx
│   └── categories/
│       ├── CategoryList.tsx
│       ├── CategoryForm.tsx
│       └── CategoryBadge.tsx
└── src/services/
    ├── budgetService.ts
    ├── expenseService.ts
    └── categoryService.ts
```

**Structure Decision**: Web application structure (Option 2) leveraging existing `budget-backend/` and `budget-frontend/` directories from Feature 001 (Project Scaffolding). New code adds domain models, repositories, services, controllers, DTOs for budget management. Frontend adds new pages under Next.js App Router structure and reusable Material-UI components.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitution violations. All principles satisfied.

---

## Phase 1 Design Complete - Constitution Re-Check

**Date**: 2025-10-23
**Status**: ✅ **PASS** - All design artifacts generated, no violations introduced

### Post-Design Validation

After completing Phase 1 design (research.md, data-model.md, contracts/, quickstart.md):

- ✅ **Data Model**: Implementation-agnostic entity definitions with relationships
- ✅ **API Contracts**: RESTful design following OpenAPI 3.0 standard
- ✅ **Research Decisions**: All technical choices documented with rationale and alternatives
- ✅ **Quickstart**: Integration test scenarios map to user stories in spec.md

### Constitution Compliance Review

1. **Principle I (Specification-First)**: ✅ Maintained
   - Spec remains technology-agnostic
   - Plan/research contain implementation details (proper separation)

2. **Principle III (Incremental Delivery)**: ✅ Maintained
   - Quickstart scenarios test each user story independently
   - P1 (Budget CRUD) can be implemented and deployed standalone

3. **Principle IV (Constitution Gates)**: ✅ PASS
   - Initial gate passed before Phase 0
   - Phase 1 design introduces no violations
   - Ready to proceed to `/speckit.tasks`

4. **Project Constraints**: ✅ Maintained
   - All contracts use Next.js (frontend) + Spring Boot (backend) stack
   - X-Hass-User header present in all write operations
   - Multi-user support designed into data model (createdBy tracking)

**GATE RESULT**: ✅ **PASS** - Proceed to `/speckit.tasks` for task generation
