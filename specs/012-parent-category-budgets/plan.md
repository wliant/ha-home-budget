# Implementation Plan: Parent Category Budget & Expense Support

**Branch**: `012-parent-category-budgets` | **Date**: 2026-02-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-parent-category-budgets/spec.md`

## Summary

Remove the restriction that prevents budgets from being created on parent categories. Enable parent categories to have their own budgets and expenses. When creating a child category budget, automatically offer to create or increment the parent category budget. In all views, aggregate child category expenses into parent category spending totals. Display parent budget rows with an "including children" subtotal.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Next.js 14.x, Material-UI v5, React 18.x, Axios
**Storage**: MySQL 8.0 (existing database with categories, budgets, expenses tables)
**Testing**: Manual testing (no automated tests unless requested)
**Target Platform**: Home Assistant Add-on (containerized, private network)
**Project Type**: Web application (frontend + backend)
**Performance Goals**: Standard web app expectations (<1s page load for budget/expense views)
**Constraints**: 2-level category hierarchy max, private home network, trust X-Hass-User header
**Scale/Scope**: Small household (2-5 users), hundreds of budgets/expenses per year

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Specification-First | PASS | spec.md completed with 5 user stories, 13 FRs, clarifications resolved |
| II. Clarify Before Planning | PASS | /speckit.clarify completed 2026-02-09 with 2 clarifications |
| III. Incremental Story-Based Delivery | PASS | 5 user stories with P1/P2 priorities, independently testable |
| IV. Constitution Gates | PASS | This section validates gates |
| V. Task Traceability | DEFERRED | Validated at /speckit.tasks phase |
| VI. Test-Optional | PASS | No tests requested; manual testing only |
| VII. Artifact Consistency | DEFERRED | Validated at /speckit.analyze phase |
| Technical Stack: Next.js frontend | PASS | Frontend changes in Next.js 14.x |
| Technical Stack: Spring Boot backend | PASS | Backend changes in Spring Boot 3.2.0 (Java 17) |
| Authentication: X-Hass-User | PASS | No auth changes; existing header trust preserved |
| Multi-User Household | PASS | Shared visibility; createdBy audit trail unchanged |

## Project Structure

### Documentation (this feature)

```text
specs/012-parent-category-budgets/
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
│   │   └── Budget.java                    # No entity changes needed
│   ├── dto/
│   │   ├── BudgetDTO.java                 # Add createParentCategoryBudget, parentCategoryBudgetAmount fields
│   │   ├── BudgetSummaryDTO.java          # Add childrenBudgetSum, childrenSpending fields
│   │   └── BudgetValidationDTO.java       # Add parentCategoryBudgetExists, parentCategoryBudgetAmount fields
│   ├── service/
│   │   ├── BudgetService.java             # Remove parent restriction, add aggregation, auto-increment
│   │   └── ExpenseService.java            # Update category filter to include children
│   ├── repository/
│   │   ├── BudgetRepository.java          # Add queries for parent category budgets
│   │   └── ExpenseRepository.java         # Add queries for child category expenses
│   └── controller/
│       └── BudgetController.java          # No changes needed (service layer handles logic)

budget-frontend/
├── src/
│   ├── components/
│   │   └── BudgetForm.tsx                 # Replace leaf-only filter with grouped select, add parent category budget checkbox
│   ├── app/
│   │   ├── budgets/
│   │   │   └── page.tsx                   # Update display to show "including children" subtotal
│   │   └── expenses/
│   │       └── page.tsx                   # Update category filter to include child expenses
│   └── services/
│       └── budgetService.ts               # Add new DTO fields for parent category budget
```

**Structure Decision**: Existing web application structure with `budget-backend/` and `budget-frontend/` directories. No new directories or projects needed.

## Complexity Tracking

No constitution violations. All changes fit within existing architecture.

## Architecture & Design

### Change Summary

This feature requires changes across 3 layers:

**Layer 1: Backend Service Logic (BudgetService.java)**
- Remove the `countByParentCategoryId > 0` validation that blocks parent category budgets (lines 75-79)
- Add new `createBudgetWithParentCategoryHandling()` logic: when creating a child budget, check if parent category has a budget for the period; if yes, auto-increment; if no, optionally create
- Update `mapToBudgetSummary()` to aggregate child category expenses into parent budget spending
- Update `getCurrentMonthBudget()` to avoid double-counting: when both parent and child category budgets exist for the same month, the dashboard should only sum budget amounts once (not parent + child if parent already includes children)
- Update `getYearlyBudgetView()` to include parent categories (currently skips them at line 634)
- Update `getBudgetValidation()` to return parent category budget info (not just yearly parent budget)

**Layer 2: Backend Repository Queries**
- Add query to find budget by parent category ID and period (to check if parent category has a budget)
- Add query to sum expenses across child categories (for aggregation)
- Update expense filter queries to expand category ID to include child category IDs

**Layer 3: Frontend Budget Form (BudgetForm.tsx)**
- Replace `flattenLeafCategories()` with grouped category select showing parent categories as group headers AND selectable items
- Add new parent category budget section: checkbox to create parent category budget (when no parent budget exists) with editable amount field
- When parent category budget exists, show info message after submission about auto-increment
- Update `CreateBudgetRequest` and `BudgetValidationDTO` types for new fields

**Layer 4: Frontend Budget List & Expense List**
- Update budget list to show "including children" subtotal on parent category budget rows
- Update expense list category filter to include child expenses when parent category selected

### Key Design Decisions

1. **Parent category budget ≠ yearly parent budget**: The existing concept of "parent budget" (yearly budget for a category, month=null) is distinct from "parent category budget" (budget for a parent category). The new feature adds a budget where the category itself is a parent (has children). The yearly parent budget logic (month=null) is unchanged.

2. **Aggregation is computed, not stored**: Child expense totals for parent categories are computed at query time, not denormalized into storage. This avoids data consistency issues.

3. **Auto-increment is server-side**: When a child category budget is created and the parent category already has a budget, the server auto-increments the parent category budget amount. The frontend receives the updated amount and old amount in the response to display the info message.

4. **Dashboard double-counting prevention**: The `getCurrentMonthBudget()` sums all monthly budgets. With parent category budgets now possible, we must ensure the dashboard doesn't double-count when both "Food" ($500) and "Groceries" ($300) budgets exist. The parent category budget represents a separate envelope, so both ARE counted (they're independent budgets). The spending aggregation ensures child expenses appear in parent totals for display, but each expense is only linked to one budget.

5. **Expense attribution unchanged**: Expenses are still linked to a specific budget via `budget_id`. When an expense is on a child category, its `budget_id` points to the child's budget. The parent category's spending is computed by summing expenses from its own budget + all child category budgets.
