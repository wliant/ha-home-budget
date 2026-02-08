# Implementation Plan: Expense List View

**Branch**: `011-expense-list-view` | **Date**: 2026-02-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/011-expense-list-view/spec.md`

## Summary

Add a dedicated expense list view with a paginated, sortable table displaying all expenses. A mandatory year filter (defaulting to the current year) and optional filters (month, category, amount range, created by) allow users to locate specific expenses. The backend API will be enhanced with server-side pagination, sorting, amount range filtering, and aggregate summary (count + total). The frontend will use Material-UI Table components with auto-apply filter behavior.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Next.js 14.x, Material-UI v5, Axios
**Storage**: MySQL 8.0 (existing database with expenses, budgets, categories tables)
**Testing**: JUnit 5, Mockito (backend); Jest, React Testing Library (frontend)
**Target Platform**: Home Assistant add-on (private home network), containerized
**Project Type**: Web application (separate frontend + backend)
**Performance Goals**: Page load <2s, filter/sort response <1s (SC-001, SC-002, SC-003)
**Constraints**: Server-side pagination (50 items/page), server-side sorting, auto-apply filters
**Scale/Scope**: Household-scale (~hundreds to low thousands of expenses per year)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Specification-First (I) | PASS | spec.md completed with clarifications |
| Clarify Before Planning (II) | PASS | 4 clarifications resolved in session 2026-02-09 |
| Incremental Story-Based Delivery (III) | PASS | 4 user stories (P1: view + year filter, P2: filters + sorting) |
| Constitution Gates (IV) | PASS | This check |
| Task Traceability (V) | N/A | Validated at task generation |
| Test-Optional (VI) | PASS | Tests not explicitly requested in spec |
| Artifact Consistency (VII) | N/A | Validated after task generation |
| Frontend: Next.js | PASS | Using existing Next.js 14 frontend |
| Backend: Spring Boot (Java) | PASS | Using existing Spring Boot 3.2.0 backend |
| Auth: X-Hass-User header | PASS | Existing filter infrastructure, no changes needed |
| Multi-user household | PASS | "created by" column and filter support multi-user |
| Private network deployment | PASS | No public exposure, containerized |

**All gates PASS. Proceeding to Phase 0.**

## Project Structure

### Documentation (this feature)

```text
specs/011-expense-list-view/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── expense-list-api.yaml
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── controller/
│   │   └── ExpenseController.java          # Enhanced: add paginated list endpoint
│   ├── service/
│   │   └── ExpenseService.java             # Enhanced: paginated query with new filters
│   ├── repository/
│   │   └── ExpenseRepository.java          # Enhanced: paginated findByFilters + aggregates
│   ├── dto/
│   │   ├── ExpenseDTO.java                 # Existing (no changes)
│   │   └── ExpenseListResponse.java        # NEW: paginated response wrapper
│   └── model/
│       └── Expense.java                    # Existing (no changes)

budget-frontend/
├── src/
│   ├── app/
│   │   └── expenses/
│   │       └── page.tsx                    # NEW: expense list page
│   ├── components/
│   │   └── expenses/
│   │       ├── ExpenseListTable.tsx         # NEW: sortable, paginated table
│   │       ├── ExpenseFilters.tsx           # NEW: filter bar component
│   │       └── ExpenseList.tsx              # Existing (no changes - used in budget detail)
│   └── services/
│       └── expenseService.ts               # Enhanced: add paginated list method + new filter types
```

**Structure Decision**: Web application (Option 2). Extends existing `budget-backend` and `budget-frontend` directories following established patterns from Feature 007 (expense recording).

## Complexity Tracking

> No constitution violations. No entries needed.
