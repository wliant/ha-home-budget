# Implementation Tasks: Homepage Dashboard Update

**Feature**: Homepage Dashboard Update (006)
**Branch**: `006-homepage-update`
**Created**: 2025-12-22
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

## Overview

Transform the homepage from a basic feature showcase into an actionable dashboard. This feature is **frontend-only** - all required backend APIs already exist from Features 002 (Budget Management) and 004 (Hierarchical Categories).

**Key Components**:
- BudgetSummaryCard - Current month budget overview
- RecentActivityCard - 5 most recent expenses
- QuickActionsCard - Quick access buttons
- FeatureNavigationCard - Updated feature cards

**Technology Stack**:
- Next.js 14.x with TypeScript 5.x
- Material-UI v5 (existing)
- React 18.x hooks (useState, useEffect)
- Existing API services (budgetService, expenseService)

---

## Phase 1: Setup and Infrastructure

**Goal**: Prepare development environment and create component directory structure

**Independent Test**: Verify component directory structure exists and dev server runs

### Tasks

- [X] T001 Create homepage components directory at budget-frontend/src/components/home/
- [X] T002 [P] Verify existing services work correctly (budgetService.getCurrentMonthBudget, expenseService.getAllExpenses)

**Phase 1 Completion Criteria**:
✅ Component directory structure exists
✅ Existing API services verified and functional
✅ Dev server runs without errors

---

## Phase 2: Foundational Components

**Goal**: Create shared utilities and base component structure needed by all user stories

**Independent Test**: Each widget component renders skeleton UI without data

### Tasks

- [X] T003 [P] Create BudgetSummaryCard skeleton component in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T004 [P] Create RecentActivityCard skeleton component in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T005 [P] Create QuickActionsCard skeleton component in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T006 [P] Create FeatureNavigationCard skeleton component in budget-frontend/src/components/home/FeatureNavigationCard.tsx

**Phase 2 Completion Criteria**:
✅ All 4 widget components exist and export valid React components
✅ Each component renders a placeholder Card from Material-UI
✅ No runtime errors when importing components

---

## Phase 3: User Story 1 - Quick Budget Overview (P1)

**Goal**: Display current month budget summary with spending progress and status indicators

**Why P1**: Primary landing page experience - users need immediate budget awareness

**Independent Test**: Open homepage → See current month budget with amount, spent, remaining, progress bar, and status color (green/yellow/red based on spending %)

**Acceptance Criteria** (from spec.md):
1. Current month budget displays with amount, spent, remaining, progress indicator
2. No budget → empty state with "Create Budget" button
3. Overspent budget → red warning indicators

### Tasks

- [X] T007 [US1] Implement data fetching in BudgetSummaryCard using budgetService.getCurrentMonthBudget() in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T008 [US1] Add loading state with CircularProgress spinner in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T009 [US1] Add error state with Alert and retry button in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T010 [US1] Implement budget summary display with amount, spent, remaining in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T011 [US1] Add LinearProgress bar showing spending percentage in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T012 [US1] Implement status indicator logic (green ≤80%, yellow 81-100%, red >100%) in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T013 [US1] Add empty state for no budget with "Create Budget" button linking to /budgets/new in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T014 [US1] Format currency values using existing formatCurrency utility in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T015 [US1] Format period display as "Month YYYY" (e.g., "December 2025") in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx
- [X] T016 [US1] Add responsive styling (stack vertically on mobile) using MUI Grid breakpoints in BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx

**Phase 3 Completion Criteria**:
✅ Budget summary displays correctly with all required fields
✅ Progress bar visual matches spending percentage
✅ Color coding works (green/yellow/red based on spending)
✅ Empty state shows when no budget exists
✅ Error state shows with retry button on API failure
✅ Component is responsive on mobile/tablet/desktop

---

## Phase 4: User Story 3 - Quick Actions Dashboard (P1)

**Goal**: Provide one-click access to common tasks (Create Budget, Record Expense, View Categories, View Dashboard)

**Why P1**: Reduces friction for daily workflows - transforms homepage into actionable hub

**Independent Test**: Click each quick action button → Verify correct page navigation

**Acceptance Criteria** (from spec.md):
1. "Record Expense" button → navigates to /expenses/new
2. "Create Budget" button → navigates to /budgets/new
3. "View Categories" button → navigates to /categories
4. "View All Budgets" button → navigates to /budgets

### Tasks

- [X] T017 [US3] Implement QuickActionsCard with 4 action buttons using MUI Grid in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T018 [US3] Add "Create Budget" button with AddIcon and onClick handler routing to /budgets/new in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T019 [US3] Add "Record Expense" button with ReceiptIcon and onClick handler routing to /expenses/new in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T020 [US3] Add "View Categories" button with CategoryIcon and onClick handler routing to /categories in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T021 [US3] Add "View Dashboard" button with DashboardIcon and onClick handler routing to /dashboard in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T022 [US3] Add responsive layout (2x2 grid on desktop, 1 column on mobile) using MUI Grid breakpoints in budget-frontend/src/components/home/QuickActionsCard.tsx
- [X] T023 [US3] Style buttons with fullWidth and variant="contained" for visual prominence in budget-frontend/src/components/home/QuickActionsCard.tsx

**Phase 4 Completion Criteria**:
✅ All 4 quick action buttons display correctly
✅ Each button navigates to correct page using Next.js router
✅ Buttons are responsive (2x2 on desktop, vertical stack on mobile)
✅ Icons display correctly for each action
✅ All navigation works with client-side routing (no page reload)

---

## Phase 5: User Story 2 - Recent Activity Feed (P2)

**Goal**: Display 5 most recent expenses with date, amount, description, category, and creator attribution

**Why P2**: Complements budget overview with spending detail - provides transaction awareness

**Independent Test**: Record expenses → Refresh homepage → See 5 most recent expenses in chronological order (newest first)

**Acceptance Criteria** (from spec.md):
1. Display 5 most recent expenses with all fields
2. No expenses → empty state with "Record Expense" button
3. Multi-user expenses show creator attribution

### Tasks

- [X] T024 [US2] Implement data fetching in RecentActivityCard using expenseService.getAllExpenses with limit=5 and sort=date,desc in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T025 [US2] Add loading state with CircularProgress spinner in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T026 [US2] Add error state with Alert and retry button in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T027 [US2] Implement expense list rendering with map over expenses array in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T028 [US2] Display expense date formatted using existing formatExpenseDate utility in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T029 [US2] Display expense amount formatted using existing formatExpenseAmount utility in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T030 [US2] Display expense description (truncate to 50 chars with ellipsis if longer) in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T031 [US2] Display category name with category icon if available in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T032 [US2] Display creator name (createdBy field) with attribution label in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T033 [US2] Add empty state for no expenses with "Record Expense" button linking to /expenses/new in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx
- [X] T034 [US2] Add responsive styling using MUI List and ListItem components in RecentActivityCard in budget-frontend/src/components/home/RecentActivityCard.tsx

**Phase 5 Completion Criteria**:
✅ Recent activity displays 5 most recent expenses
✅ Expenses sorted by date descending (newest first)
✅ All required fields display (date, amount, description, category, creator)
✅ Empty state shows when no expenses exist
✅ Error state shows with retry button on API failure
✅ Component is responsive on all screen sizes

---

## Phase 6: User Story 4 - Active Feature Navigation (P2)

**Goal**: Update feature cards to show active navigation for implemented features (remove "coming soon" placeholders)

**Why P2**: Improves UX by accurately reflecting current capabilities - enables feature discovery

**Independent Test**: View homepage → All feature cards have "View [Feature]" buttons → Click each → Navigate to correct page

**Acceptance Criteria** (from spec.md):
1. All feature cards are fully interactive with navigation buttons
2. Categories card shows "View Categories" button (not "Coming soon")
3. Dashboard card shows "View Dashboard" button (not "Coming soon")
4. No opacity effect on any feature cards (all fully visible)

### Tasks

- [X] T035 [US4] Create FeatureNavigationCard component accepting feature name, description, icon, and path props in budget-frontend/src/components/home/FeatureNavigationCard.tsx
- [X] T036 [US4] Implement card layout with icon, title, description using MUI Card components in budget-frontend/src/components/home/FeatureNavigationCard.tsx
- [X] T037 [US4] Add CardActions with "View [Feature]" button using router.push(path) in budget-frontend/src/components/home/FeatureNavigationCard.tsx
- [X] T038 [US4] Remove opacity styling from all feature cards in budget-frontend/src/components/home/FeatureNavigationCard.tsx
- [X] T039 [US4] Add responsive grid layout (2x2 on desktop, 1 column on mobile) for feature cards container in budget-frontend/src/components/home/FeatureNavigationCard.tsx

**Phase 6 Completion Criteria**:
✅ All feature cards display without "coming soon" messaging
✅ All feature cards have active "View [Feature]" buttons
✅ No feature cards have opacity effect (all fully visible)
✅ Clicking feature card buttons navigates to correct pages
✅ Feature cards are responsive

---

## Phase 7: Homepage Integration

**Goal**: Integrate all widgets into homepage (page.tsx) with proper layout and system status section

**Independent Test**: Open http://localhost:3000 → See complete homepage with all widgets, system status, and responsive layout

### Tasks

- [X] T040 Update homepage at budget-frontend/src/app/page.tsx to import all widget components
- [X] T041 Implement responsive Grid layout with MUI Grid system in budget-frontend/src/app/page.tsx
- [X] T042 Add BudgetSummaryCard in left column (xs=12, md=6) in budget-frontend/src/app/page.tsx
- [X] T043 Add RecentActivityCard in right column (xs=12, md=6) in budget-frontend/src/app/page.tsx
- [X] T044 Add QuickActionsCard in full-width row below summary/activity in budget-frontend/src/app/page.tsx
- [X] T045 Update Features section to use FeatureNavigationCard for Budgets, Categories, Dashboard, Expenses in budget-frontend/src/app/page.tsx
- [X] T046 Retain existing System Status section (backend health check) in budget-frontend/src/app/page.tsx
- [X] T047 Remove old "coming soon" placeholder cards in budget-frontend/src/app/page.tsx
- [X] T048 Test responsive layout at 320px (mobile), 768px (tablet), and 1920px (desktop) widths in budget-frontend/src/app/page.tsx

**Phase 7 Completion Criteria**:
✅ All widgets display on homepage
✅ Layout is responsive across all screen sizes
✅ System status section retained and functional
✅ No "coming soon" cards remain
✅ Page header and title retained
✅ All components render without errors

---

## Phase 8: Polish and Cross-Cutting Concerns

**Goal**: Final refinements for performance, accessibility, and user experience

**Independent Test**: Full homepage loads in <2s, keyboard navigation works, no console errors

### Tasks

- [X] T049 [P] Add aria-label attributes to all interactive elements (buttons, links) in all widget components
- [X] T050 [P] Verify color contrast meets WCAG AA standards for all text on colored backgrounds across all components
- [ ] T051 Test homepage load time with browser DevTools Performance tab (target: <2s total)
- [ ] T052 Test keyboard navigation (Tab through all interactive elements, Enter to activate) across all components
- [X] T053 [P] Test error handling by stopping backend and verifying graceful degradation in all widgets
- [X] T054 [P] Test empty states by clearing database and verifying appropriate messages display
- [X] T055 Verify all currency formatting uses consistent format ($X,XXX.XX) across BudgetSummaryCard and RecentActivityCard
- [X] T056 Verify all date formatting uses consistent format across RecentActivityCard
- [ ] T057 Test multi-user scenario (create expenses as different X-Hass-User values, verify attribution displays)
- [X] T058 Run build command (npm run build) and verify no TypeScript errors or warnings
- [X] T059 Clear browser console warnings and errors (clean console on homepage load)
- [ ] T060 Test on actual mobile device (or Chrome DevTools device emulator) for touch interactions

**Phase 8 Completion Criteria**:
✅ Homepage loads in under 2 seconds
✅ All WCAG AA accessibility requirements met
✅ Keyboard navigation works for all interactive elements
✅ Error handling works gracefully
✅ Empty states display appropriately
✅ No console errors or warnings
✅ Production build succeeds
✅ Mobile experience is smooth

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**Recommended MVP**: Phase 3 (User Story 1) + Phase 4 (User Story 3) + Phase 7 (Integration)

This delivers:
- ✅ Budget summary at a glance (primary value)
- ✅ Quick access to common actions (workflow improvement)
- ✅ Functional homepage ready for users

**Rationale**: P1 user stories (Budget Overview + Quick Actions) deliver core value. Recent Activity and Feature Navigation are enhancements that can follow.

### Incremental Delivery Path

1. **Week 1**: Phases 1-4 (Setup + P1 stories) → MVP ready
2. **Week 2**: Phases 5-6 (P2 stories) → Full feature set
3. **Week 3**: Phases 7-8 (Integration + Polish) → Production ready

### Parallel Execution Opportunities

**Phase 2 (Foundational)**: All 4 tasks (T003-T006) can run in parallel - creating different component files

**Phase 3 (US1)**: Tasks within BudgetSummaryCard are sequential (data fetching before display logic)

**Phase 4 (US3)**: All button implementation tasks (T018-T021) can run in parallel after T017 completes

**Phase 5 (US2)**: Tasks within RecentActivityCard are sequential (data fetching before display logic)

**Phase 6 (US4)**: Tasks T036-T039 can run in parallel after T035 creates component structure

**Phase 8 (Polish)**: Tasks with [P] marker (T049, T050, T053, T054) can run in parallel

---

## Dependencies

### User Story Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational) ← Must complete before any user story
    ↓
Phase 3 (US1 - Budget Overview) ← Independent
    ↓
Phase 4 (US3 - Quick Actions) ← Independent
    ↓
Phase 5 (US2 - Recent Activity) ← Independent
    ↓
Phase 6 (US4 - Feature Navigation) ← Independent
    ↓
Phase 7 (Integration) ← Requires all user story phases complete
    ↓
Phase 8 (Polish) ← Requires integration complete
```

**Key Insight**: Phases 3-6 (all user stories) are **independent** and can be implemented in any order after Phase 2. Priority (P1/P2) determines recommended sequence, not technical dependencies.

### External Dependencies

- **Backend APIs**: Already exist (no backend work required)
  - GET /api/budgets/current (Feature 002)
  - GET /api/expenses?sort=expenseDate,desc&limit=5 (Feature 002)
  - GET /api/health (Feature 001)

- **Frontend Services**: Already exist
  - budgetService.getCurrentMonthBudget()
  - expenseService.getAllExpenses()
  - Existing formatting utilities (formatCurrency, formatExpenseDate, formatExpenseAmount)

---

## Testing Strategy

**Tests**: Optional (not specified in requirements)

**Manual Testing**: Use quickstart.md test scenarios

**Recommended Testing Approach**:
1. **Smoke test after each phase**: Verify component renders without errors
2. **Integration test after Phase 7**: Test full homepage workflow
3. **Acceptance test after Phase 8**: Verify all acceptance criteria from spec.md

**Test Scenarios** (from quickstart.md):
- Test Case 1.1: Display Budget Summary (with data)
- Test Case 1.2: No Budget Empty State
- Test Case 1.3: Overspent Budget Warning
- Test Case 2.1: Display Recent Expenses
- Test Case 2.2: No Expenses Empty State
- Test Case 2.3: Multi-User Attribution
- Test Case 3.1-3.4: Quick Action Navigation
- Test Case 4.1-4.2: Feature Card Navigation
- Test Case 5.1-5.2: Error Handling
- Test Case 6.1-6.3: Responsive Design

---

## Task Summary

**Total Tasks**: 60

**By Phase**:
- Phase 1 (Setup): 2 tasks
- Phase 2 (Foundational): 4 tasks
- Phase 3 (US1 - Budget Overview): 10 tasks
- Phase 4 (US3 - Quick Actions): 7 tasks
- Phase 5 (US2 - Recent Activity): 11 tasks
- Phase 6 (US4 - Feature Navigation): 5 tasks
- Phase 7 (Integration): 9 tasks
- Phase 8 (Polish): 12 tasks

**By User Story**:
- US1 (Budget Overview - P1): 10 tasks
- US2 (Recent Activity - P2): 11 tasks
- US3 (Quick Actions - P1): 7 tasks
- US4 (Feature Navigation - P2): 5 tasks
- Infrastructure/Integration: 27 tasks

**Parallel Opportunities**: 15 tasks marked [P] can run in parallel with other tasks in their phase

**Estimated Effort**:
- MVP (Phases 1-4 + Phase 7 partial): ~8-12 hours
- Full Feature (All Phases): ~16-24 hours
- Per Task Average: ~20-30 minutes

---

## Validation

**Format Check**: ✅ All tasks follow format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

**Traceability**: ✅ All tasks mapped to:
- Functional requirements (FR-001 through FR-014)
- User stories (US1, US2, US3, US4)
- Design decisions (from research.md)

**Completeness**: ✅ Each user story has:
- Independent test criteria
- All required implementation tasks
- Clear acceptance criteria

**Ready for**: `/speckit.implement` execution
