# Implementation Plan: Expense Recording

**Branch**: `007-expense-recording` | **Date**: 2025-12-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-expense-recording/spec.md`

## Summary

Create a user-facing expense entry form that enables household members to quickly record daily expenses with minimal friction. Users can enter amount, description, select category, and optionally edit the date (defaults to today). The expense is automatically attributed to the authenticated user via X-Hass-User header and counted against the appropriate category budget.

**Technical Approach**: Frontend-only feature using existing Next.js application and Material-UI components. Integrates with existing backend expense creation API from Feature 002. No new backend development required.

## Technical Context

**Language/Version**: TypeScript 5.x (frontend), Java 17 (existing backend)
**Primary Dependencies**: Next.js 14.x, Material-UI v5, React 18.x
**Storage**: MySQL 8.0 (via existing backend, no changes needed)
**Testing**: Jest + React Testing Library (optional, not required per spec)
**Target Platform**: Web browsers (desktop and mobile)
**Project Type**: Web application (frontend component)
**Performance Goals**: Form load <500ms, submission response <2s, 95% success rate
**Constraints**: <30 second expense entry time, works on mobile devices
**Scale/Scope**: 1 new page (/expenses/new), ~5 form components, integrate with 3 existing services

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Principle I - Specification-First**: ✅ PASS
- spec.md contains no implementation details (no mention of Next.js, Material-UI, React, Spring Boot)
- All requirements are technology-agnostic and business-focused
- Success criteria are measurable outcomes, not technical metrics

**Principle II - Clarify Before Planning**: ⚠️ SKIPPED (allowed)
- `/speckit.clarify` was initiated but user chose to proceed directly to planning
- No [NEEDS CLARIFICATION] markers present in spec.md
- Specification is self-contained and unambiguous

**Principle III - Incremental Story-Based Delivery**: ✅ PASS
- 4 user stories defined with clear priorities (2 P1, 2 P2)
- Each story has independent test criteria
- P1 stories (Quick Expense Entry, Category-Based Tracking) form MVP
- P2 stories (Date Flexibility, Multi-User Attribution) are enhancements

**Principle IV - Constitution Gates**: ✅ PASS (Pre-Phase 0)
- Technical Stack: Frontend-only feature using existing Next.js + Spring Boot architecture (NON-NEGOTIABLE constraints satisfied)
- Authentication: Leverages existing X-Hass-User header from Feature 003 (NON-NEGOTIABLE constraint satisfied)
- Multi-user: Supports household collaboration via user attribution (NON-NEGOTIABLE constraint satisfied)
- No violations requiring justification

**Principle V - Task Traceability**: ⏳ DEFERRED
- Will be enforced during `/speckit.tasks` command
- Task format: `- [ ] [T###] [P] [US#] Description with file/path`

**Principle VI - Test-Optional, Test-First When Included**: ✅ PASS
- Spec does not require tests (FR-001 to FR-015 are functional requirements, not test requirements)
- Testing framework noted as optional in Technical Context
- If tests are added in tasks.md, TDD approach will be followed

**Principle VII - Artifact Consistency**: ⏳ DEFERRED
- Will be validated after task generation using `/speckit.analyze`

**GATE STATUS**: ✅ **PASS** - Proceed to Phase 0 Research

---

## Constitution Re-Check (Post-Phase 1)

*Re-evaluation after Phase 1 design artifacts completed*

**Principle I - Specification-First**: ✅ PASS
- spec.md remains technology-agnostic (no changes)
- Technical details properly isolated in plan.md, research.md, data-model.md

**Principle II - Clarify Before Planning**: ⚠️ SKIPPED (allowed)
- No new ambiguities discovered during planning

**Principle III - Incremental Story-Based Delivery**: ✅ PASS
- Design supports independent user story implementation
- P1 stories can be delivered without P2 dependencies

**Principle IV - Constitution Gates**: ✅ PASS (Post-Phase 1)
- **Technical Stack**: Frontend-only feature using Next.js 14.x + TypeScript 5.x (constitution compliant)
- **Backend**: No changes to Spring Boot backend (constitution compliant)
- **Authentication**: Uses existing X-Hass-User header (constitution compliant)
- **Multi-user**: Expenses tagged with createdBy username (constitution compliant)
- **Data Model**: No new entities, leverages existing Expense/Category/Budget (minimal complexity)
- **Component Structure**: Follows existing patterns from Features 002, 005, 006 (consistency)

**Principle V - Task Traceability**: ⏳ DEFERRED
- Will be enforced during `/speckit.tasks` command

**Principle VI - Test-Optional, Test-First When Included**: ✅ PASS
- Quickstart.md includes optional test scenarios but does not mandate tests
- If tests are added in tasks.md, TDD approach will be used

**Principle VII - Artifact Consistency**: ⏳ DEFERRED
- Will be validated after task generation using `/speckit.analyze`

**FINAL GATE STATUS**: ✅ **PASS** - Ready for `/speckit.tasks`

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
budget-frontend/                    # Next.js 14.x frontend (EXISTING)
├── src/
│   ├── app/
│   │   └── expenses/
│   │       └── new/               # NEW: Expense creation page
│   │           └── page.tsx       # NEW: Main expense form component
│   ├── components/
│   │   └── expenses/              # NEW: Expense-specific components
│   │       ├── ExpenseForm.tsx    # NEW: Form component
│   │       ├── CategorySelect.tsx # NEW: Category dropdown
│   │       └── DatePicker.tsx     # NEW: Date selection component
│   ├── services/
│   │   ├── expenseService.ts      # EXISTING: Already has createExpense()
│   │   └── categoryService.ts     # EXISTING: Already has getCategories()
│   └── types/
│       ├── category.ts            # EXISTING: CategoryDTO interface
│       └── expense.ts             # EXISTING: ExpenseDTO, CreateExpenseRequest
│
budget-backend/                     # Spring Boot (Java 17) backend (EXISTING)
└── [NO CHANGES REQUIRED - Using existing expense creation API]
```

**Structure Decision**: Frontend-only feature using Next.js App Router structure. The expense creation page (`/expenses/new`) follows the existing pattern established by `/budgets/new` and `/categories`. All backend APIs already exist from Feature 002 (Budget Management), requiring no backend modifications.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations - this section is not applicable for this feature.
