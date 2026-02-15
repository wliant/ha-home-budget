# Implementation Plan: Category Expense Aggregates

**Branch**: `016-category-expense-aggregates` | **Date**: 2026-02-15 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-category-expense-aggregates/spec.md`

## Summary

Decouple expenses from budgets by removing the `budget_id` foreign key from the expenses table and making expenses depend only on categories. Introduce category-level expense aggregation (monthly and yearly) with parent category rollup. The yearly budget view will source spending data from category-based aggregate queries instead of budget-linked expense sums.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Next.js 14.x, Material-UI v5, Axios
**Storage**: MySQL 8.0, Liquibase for migrations
**Testing**: Existing test suite (JUnit 5, Mockito, Testcontainers) — tests optional unless requested
**Target Platform**: Home Assistant add-on (Docker containers, private network)
**Project Type**: Web application (frontend + backend)
**Performance Goals**: Yearly budget view loads within 1 second; expense creation under 30 seconds
**Constraints**: Household-scale (dozens of categories, hundreds of expenses/year), on-the-fly aggregation sufficient
**Scale/Scope**: ~30 backend files impacted, ~15 frontend files impacted

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Evidence |
|------|--------|----------|
| Spec-first (Principle I) | PASS | spec.md completed and clarified |
| Clarify before plan (Principle II) | PASS | /speckit.clarify run — no critical ambiguities found |
| Story-based delivery (Principle III) | PASS | 4 user stories (2x P1, 2x P2), independently testable |
| Constitution gates (Principle IV) | PASS | This table; re-check after Phase 1 |
| Task traceability (Principle V) | N/A | Validated at /speckit.tasks phase |
| Test-optional (Principle VI) | PASS | No tests explicitly requested |
| Artifact consistency (Principle VII) | N/A | Validated at /speckit.analyze phase |
| Frontend: Next.js | PASS | Using existing Next.js 14.x frontend |
| Backend: Spring Boot (Java) | PASS | Using existing Spring Boot 3.2.0 backend |
| Auth: X-Hass-User header | PASS | Expense `createdBy` continues from X-Hass-User |
| Multi-user household | PASS | `createdBy` audit trail preserved on expenses |
| Private network deployment | PASS | No changes to deployment model |

## Project Structure

### Documentation (this feature)

```text
specs/016-category-expense-aggregates/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── model/
│   │   ├── Expense.java              # Remove budget FK, make category required
│   │   └── Budget.java               # Remove expenses list, remove cascade
│   ├── dto/
│   │   ├── ExpenseDTO.java           # Remove budgetId field
│   │   ├── CategoryExpenseAggregateDTO.java  # NEW: monthly/yearly aggregates
│   │   ├── YearlyCategoryBudgetDTO.java      # Update: spending from aggregates
│   │   └── YearlyMonthlyBudgetDTO.java       # Update: spending from aggregates
│   ├── repository/
│   │   ├── ExpenseRepository.java    # Replace budget-based queries with category-based
│   │   └── BudgetRepository.java     # Remove expense-related queries
│   ├── service/
│   │   ├── ExpenseService.java       # Remove budget resolution, simplify CRUD
│   │   ├── BudgetService.java        # Rewrite yearly view, remove expense cascade
│   │   └── ExpenseInputJobService.java # Remove budget resolution
│   └── controller/
│       ├── ExpenseController.java    # Update logging, remove budget refs
│       └── BudgetController.java     # Add aggregate endpoint
├── src/main/resources/
│   └── db/changelog/changes/
│       └── 010-remove-budget-fk-from-expenses.xml  # NEW: migration

budget-frontend/
├── src/
│   ├── services/
│   │   ├── expenseService.ts         # Remove budgetId from DTOs/requests
│   │   └── budgetService.ts          # Add aggregate API calls
│   ├── types/
│   │   └── expense.ts                # Remove budgetId from form state
│   ├── app/
│   │   ├── expenses/
│   │   │   └── new/page.tsx          # Remove budget auto-selection logic
│   │   └── budgets/
│   │       └── [id]/page.tsx         # Update detail view (no expense list by budget)
│   └── components/
│       ├── expenses/ExpenseForm.tsx   # Remove budgetId prop
│       └── home/BudgetSummaryCard.tsx # Use category aggregates for spending
```

**Structure Decision**: Existing web application structure (budget-backend + budget-frontend). No new projects or directories beyond the spec artifacts.

## Complexity Tracking

No constitution violations. No complexity justification required.
