# Implementation Plan: Parent Category Budget Auto-Rollup

**Branch**: `017-parent-budget-rollup` | **Date**: 2026-02-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/017-parent-budget-rollup/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

When a budget is created or modified for a child category, the system automatically creates or updates the parent category budget with the same amount for the same period (monthly or yearly). This ensures parent budgets always reflect their children's total allocations. The yearly budget view calculates totals by summing only parent category budgets (not child budgets) to avoid double-counting, since parent budgets already include their children's amounts through automatic rollup.

## Technical Context

**Language/Version**:
- Backend: Java 17 with Spring Boot 3.2.0
- Frontend: TypeScript 5.x with Next.js 14.x

**Primary Dependencies**:
- Backend: Spring Data JPA, Hibernate, Liquibase (migrations), Spring MVC (REST)
- Frontend: React 18.x, Material-UI v5, Axios (API client)

**Storage**: MySQL 8.0 (existing database with budgets, categories, expenses tables)

**Testing**:
- Backend: JUnit 5, Mockito, AssertJ, Testcontainers (MySQL module)
- Frontend: Manual testing (no automated frontend tests required for this feature)

**Target Platform**: Home Assistant Add-on (containerized deployment on private home network)

**Project Type**: Web application (Next.js frontend + Spring Boot backend API)

**Performance Goals**:
- Budget creation/update response time <500ms (household-scale data)
- Yearly budget view aggregation <1000ms (typically <100 categories)

**Constraints**:
- Must use Home Assistant authentication (`X-Hass-User` header)
- Budget cascade must execute atomically (parent update fails if child operation fails)
- No network latency concerns (private home network deployment)

**Scale/Scope**:
- Household-scale: ~10-50 categories, ~10-100 budgets per year
- Multiple household users (2-10 users typical)
- Transaction volume: ~10-100 budget operations per month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Core Principles Compliance

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Specification-First | ✅ PASS | spec.md completed with technology-agnostic requirements, written for business stakeholders |
| II. Clarify Before Planning | ✅ PASS | Specification validated (no [NEEDS CLARIFICATION] markers), checklist complete |
| III. Incremental Delivery | ✅ PASS | 3 prioritized user stories (US1: P1, US2: P1, US3: P2), each independently testable |
| IV. Constitution Gates | ✅ PASS | Running pre-Phase 0 validation now, will re-validate after Phase 1 |
| V. Task Traceability | ⏳ PENDING | Tasks.md will be generated in /speckit.tasks with [T###] [P] [US#] format |
| VI. Test-Optional | ✅ PASS | Tests not explicitly requested; backend unit/integration tests optional for this feature |
| VII. Artifact Consistency | ⏳ PENDING | Will validate via /speckit.analyze after task generation |

### Project-Specific Constraints Compliance

| Constraint | Status | Evidence |
|------------|--------|----------|
| Frontend: Next.js | ✅ PASS | Using Next.js 14.x (existing frontend stack) |
| Backend: Spring Boot (Java) | ✅ PASS | Using Spring Boot 3.2.0, Java 17 (existing backend stack) |
| Home Assistant Auth | ✅ PASS | X-Hass-User header integration already established (Feature 015) |
| Private Network Deployment | ✅ PASS | No changes to deployment model; budget operations stay server-side |
| Multi-User Household | ✅ PASS | Budget creation captures createdBy (X-Hass-User); parent rollup transparent to all users |

### Gate Result: ✅ PASS (All Non-Negotiable Requirements Met)

**No violations to justify.** This feature extends existing budget management functionality using the established tech stack (Spring Boot + Next.js) and authentication model (Home Assistant X-Hass-User). The automatic rollup mechanism will be implemented in the backend BudgetService layer, maintaining transactional integrity through Spring's @Transactional support.

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
budget-backend/                          # Spring Boot 3.2.0 backend
├── src/main/java/com/homebudget/
│   ├── model/
│   │   ├── Budget.java                  # Core entity - budget rollup source
│   │   ├── Category.java                # Category hierarchy (parent-child relationships)
│   │   └── Expense.java                 # Expense entity (Feature 016: decoupled from budgets)
│   ├── repository/
│   │   ├── BudgetRepository.java        # Budget queries (will add parent lookup methods)
│   │   └── CategoryRepository.java      # Category hierarchy queries
│   ├── service/
│   │   ├── BudgetService.java           # *** PRIMARY: Implement auto-rollup logic here ***
│   │   └── CategoryService.java         # Category hierarchy utilities
│   ├── dto/
│   │   ├── BudgetDTO.java               # Budget transfer object
│   │   ├── BudgetSummaryDTO.java        # Budget + spending summary (Feature 016)
│   │   └── BudgetValidationDTO.java     # UI validation metadata (Feature 012)
│   └── controller/
│       └── BudgetController.java        # REST API endpoints (no changes needed)
├── src/main/resources/
│   └── db/changelog/changes/
│       └── 011-*.xml                    # *** NEW: Liquibase migration if schema changes ***
└── src/test/java/com/homebudget/
    ├── service/
    │   └── BudgetServiceTest.java       # *** NEW: Unit tests for rollup logic ***
    └── repository/
        └── BudgetRepositoryTest.java    # *** NEW: Integration tests for parent queries ***

budget-frontend/                         # Next.js 14.x frontend
├── src/
│   ├── app/
│   │   ├── budgets/
│   │   │   ├── page.tsx                 # *** MODIFY: Yearly budget view total calculation ***
│   │   │   ├── new/
│   │   │   │   └── page.tsx             # Budget creation form (auto-rollup transparent)
│   │   │   └── [id]/
│   │   │       ├── page.tsx             # Budget detail view (no changes)
│   │   │       └── edit/
│   │   │           └── page.tsx         # *** MODIFY: Budget edit triggers rollup ***
│   │   └── categories/
│   │       └── page.tsx                 # Category management (no changes)
│   ├── services/
│   │   ├── budgetService.ts             # API client (no changes; rollup is backend concern)
│   │   └── categoryService.ts           # Category hierarchy utilities
│   ├── components/
│   │   └── budgets/
│   │       └── BudgetForm.tsx           # Budget form component (no changes)
│   └── types/
│       └── budget.ts                    # TypeScript interfaces (no changes)
```

**Structure Decision**: Web application (existing) with automatic parent budget rollup implemented entirely in the **backend BudgetService layer**. Frontend changes minimal: only the yearly budget view calculation logic needs modification to sum parent budgets only (filtering out child category budgets). Budget creation/edit forms remain unchanged since auto-rollup is transparent to users.

## Complexity Tracking

**No violations to track.** All constitution principles and project-specific constraints are satisfied.

---

## Phase 0: Research (COMPLETED)

**Output**: [research.md](./research.md)

**Key Decisions**:
1. **Budget Cascade Pattern**: Service-layer implementation with Spring @Transactional
2. **Parent Budget Creation**: Auto-create with zero initial amount, then apply delta
3. **Delta Calculation**: Simple arithmetic (create: +amount, update: delta, delete: -amount)
4. **Yearly View Filtering**: Backend filters child budgets before sending to frontend
5. **Testing Strategy**: Multi-layer (unit, integration, manual scenarios)
6. **Migration Strategy**: No schema changes required (existing tables support feature)
7. **Error Handling**: @Transactional rollback on cascade failure

---

## Phase 1: Design & Contracts (COMPLETED)

**Outputs**:
- [data-model.md](./data-model.md) - Budget and Category entities with rollup relationships
- [contracts/api-behavior.md](./contracts/api-behavior.md) - API behavior changes (no signature changes)
- [quickstart.md](./quickstart.md) - 9 integration test scenarios covering all user stories

**Key Design Decisions**:
- **No database schema changes**: Existing `budgets` and `categories` tables support feature as-is
- **Service-layer cascade**: BudgetService implements `cascadeToParentBudget()` method called by create/update/delete operations
- **Transaction atomicity**: Child and parent budget operations succeed/fail together via Spring @Transactional
- **API transparency**: Existing REST endpoints unchanged; cascade behavior transparent to frontend clients

---

## Constitution Check (Post-Phase 1 Re-Evaluation)

### Core Principles Compliance (Re-Validated)

| Principle | Status | Post-Design Evidence |
|-----------|--------|----------------------|
| I. Specification-First | ✅ PASS | No implementation details leaked into spec.md during design |
| II. Clarify Before Planning | ✅ PASS | Specification remained unambiguous throughout design phase |
| III. Incremental Delivery | ✅ PASS | Design supports independent implementation of US1 (P1), US2 (P1), US3 (P2) |
| IV. Constitution Gates | ✅ PASS | Re-validation confirms no new violations introduced during design |
| V. Task Traceability | ⏳ PENDING | Tasks.md generation pending (/speckit.tasks) |
| VI. Test-Optional | ✅ PASS | Backend unit/integration tests documented in quickstart.md (optional execution) |
| VII. Artifact Consistency | ⏳ PENDING | Validation pending (/speckit.analyze after task generation) |

### Project-Specific Constraints Compliance (Re-Validated)

| Constraint | Status | Post-Design Evidence |
|------------|--------|----------------------|
| Frontend: Next.js | ✅ PASS | Design uses existing Next.js 14.x frontend (minimal changes: yearly view filtering) |
| Backend: Spring Boot (Java) | ✅ PASS | Design uses Spring Boot 3.2.0 service layer for cascade logic (BudgetService) |
| Home Assistant Auth | ✅ PASS | No changes to authentication; X-Hass-User header handling unchanged |
| Private Network Deployment | ✅ PASS | No deployment changes; all logic server-side (budget cascade in BudgetService) |
| Multi-User Household | ✅ PASS | createdBy field captures X-Hass-User; parent rollup transparent to all users |

### Post-Design Gate Result: ✅ PASS

**No new violations introduced during design phase.** The implementation plan uses:
- Existing tech stack (Spring Boot backend, Next.js frontend)
- Existing authentication model (X-Hass-User header)
- Existing database schema (no migrations needed)
- Service-layer cascade logic with Spring @Transactional (standard pattern)

The design maintains full backward compatibility and requires minimal frontend changes (yearly budget view filtering logic already implemented per research.md Decision 4).

---

## Next Steps

**Planning Phase Complete.** Ready for:
1. **Task Generation**: Run `/speckit.tasks` to generate dependency-ordered task breakdown
2. **Consistency Validation**: Run `/speckit.analyze` to verify artifact consistency (recommended before implementation)
3. **Implementation**: Run `/speckit.implement` to execute tasks phase-by-phase
