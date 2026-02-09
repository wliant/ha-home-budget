# Tasks: Frontend Theme Redesign

**Input**: Design documents from `/specs/013-frontend-theme-redesign/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: No automated tests — manual visual verification via quickstart.md scenarios.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Frontend**: `budget-frontend/src/` (Next.js application)
- No backend changes in this feature

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Update the centralized theme file with the Warm Earth palette, typography, and component overrides. This is the foundation all other tasks depend on.

- [x] T001 Update the Warm Earth color palette (primary terracotta, secondary olive, info gold, success sage, warning amber, error burnt red), background cream (#FAF6F0), and text colors (primary #3E2723, secondary #5D4037) in budget-frontend/src/styles/theme.ts — replace all existing palette values with the hex values from research.md Decision 1
- [x] T002 Update typography in budget-frontend/src/styles/theme.ts — set h1-h3 fontWeight to 600, keep h4-h6 at 500, and configure text colors to use dark brown (#3E2723 primary, #5D4037 secondary) via palette.text overrides
- [x] T003 Add MUI component overrides to budget-frontend/src/styles/theme.ts — add styleOverrides for MuiAppBar (gradient background from primary.dark to primary.main), MuiCard (hover elevation transition with 0.3s ease, subtle warm border 1px solid rgba(160,82,45,0.08)), MuiTableRow (alternating nth-of-type(even) background #FAF6F0), MuiListItemButton (selected state with primary.light background at 15% opacity and 3px left border in primary.main), MuiLinearProgress (borderRadius 4px, track color grey[200]), MuiChip (borderRadius 16px), MuiPaper (warm-tinted boxShadow using rgba(160,82,45,0.08))

**Checkpoint**: Theme file complete. All subsequent tasks will inherit the new colors, typography, and component overrides automatically via MUI's ThemeProvider.

---

## Phase 2: User Story 1 — Visually Appealing Color Theme (Priority: P1) — MVP

**Goal**: Apply the Warm Earth palette consistently across all navigation, pages, and components so the app feels cohesive and warm instead of black-and-white.

**Independent Test**: Navigate through all 7 pages and verify terracotta/olive/gold colors appear on AppBar, sidebar, buttons, cards, and text. No blue (#1976d2) or default black-on-white remnants should remain.

### Implementation for User Story 1

- [x] T004 [US1] Update AppShell sidebar styling in budget-frontend/src/components/navigation/AppShell.tsx — set Drawer Paper background to warm cream (#F5EDE3), style active ListItemButton with primary light bg + 3px left border, set nav icon default color to text.secondary (brown) and active color to primary.main (terracotta), update AppBar to use the gradient defined in theme overrides (remove elevation:0 and border-bottom in favor of gradient)
- [x] T005 [P] [US1] Update Home page in budget-frontend/src/app/page.tsx — update page title and section headers to use themed colors, ensure system status section uses themed success/error colors, verify all Typography uses theme text colors
- [x] T006 [P] [US1] Update Dashboard page in budget-frontend/src/app/dashboard/page.tsx — update current month card border to use primary.main (terracotta) instead of literal 'primary.main' string, verify budget summary cards pick up theme card overrides, update warning Alert to use themed warning colors
- [x] T007 [P] [US1] Update Budgets page in budget-frontend/src/app/budgets/page.tsx — verify filter controls (Select, FormControl) render with themed colors, verify Add button uses primary theme color, verify delete dialog uses themed error color
- [x] T008 [P] [US1] Update Yearly View page in budget-frontend/src/app/budgets/yearly/page.tsx — verify table headers use themed colors, ensure sticky header background uses paper color, verify category filter Autocomplete uses themed primary
- [x] T009 [P] [US1] Update Categories page in budget-frontend/src/app/categories/page.tsx — verify toggle buttons use themed colors, verify search and filter controls are themed, ensure category cards pick up theme card overrides
- [x] T010 [P] [US1] Update Expenses page in budget-frontend/src/app/expenses/page.tsx — verify summary bar uses themed colors for totals, verify filter controls render with themed palette, verify pagination controls are themed

**Checkpoint**: All 7 pages display the Warm Earth theme consistently. The MVP is functional — the app looks warm and cohesive.

---

## Phase 3: User Story 2 — Meaningful Icons Throughout the Interface (Priority: P1)

**Goal**: Every navigation item, action button, and status indicator uses a contextually appropriate icon. Status badges use both color and icon for accessibility.

**Independent Test**: Review every page and verify all nav items have appropriate icons, action buttons show icons (add/edit/delete), and budget status chips display status icons alongside color.

### Implementation for User Story 2

- [x] T011 [US2] Update navigation icon imports in budget-frontend/src/components/navigation/navConfig.tsx — replace ListAlt import with ReceiptLong for Expenses item, replace Receipt import with AddCard for Record Expense item, update the navItems array with the new icon components
- [x] T012 [US2] Add status icons to BudgetCard in budget-frontend/src/components/BudgetCard.tsx — import CheckCircle, Warning, ErrorOutline icons; update the status Chip to include an icon prop based on spending status (success→CheckCircle, warning→Warning, error→ErrorOutline); ensure icon size is appropriate (small/fontSize)
- [x] T013 [P] [US2] Add status icons to BudgetSummaryCard in budget-frontend/src/components/home/BudgetSummaryCard.tsx — import CheckCircle, Warning, ErrorOutline; add icon to the status Chip matching the spending status pattern from BudgetCard
- [x] T014 [P] [US2] Update QuickActionsCard icons in budget-frontend/src/components/home/QuickActionsCard.tsx — verify each quick action button has a distinctive startIcon (Add for create budget, Receipt for record expense, Category for categories, Dashboard for dashboard), ensure icons are visually distinct with appropriate sizing
- [x] T015 [P] [US2] Update FeatureNavigationCard in budget-frontend/src/components/home/FeatureNavigationCard.tsx — ensure each feature card receives and displays a distinctive icon prop with themed color, verify icon size is consistent (40px) and uses primary or secondary color
- [x] T016 [P] [US2] Add page header icons to all page components — add matching navigation icon next to the page title Typography on: Home (Home icon) in budget-frontend/src/app/page.tsx, Dashboard (Dashboard icon) in budget-frontend/src/app/dashboard/page.tsx, Budgets (AccountBalanceWallet icon) in budget-frontend/src/app/budgets/page.tsx, Categories (Category icon) in budget-frontend/src/app/categories/page.tsx, Expenses (ReceiptLong icon) in budget-frontend/src/app/expenses/page.tsx, Record Expense (AddCard icon) in budget-frontend/src/app/expenses/new/page.tsx, Yearly View (CalendarMonth icon) in budget-frontend/src/app/budgets/yearly/page.tsx

**Checkpoint**: All navigation items show contextually accurate icons, status chips display icons alongside color, and page headers include matching icons.

---

## Phase 4: User Story 3 — Polished Card and Component Styling (Priority: P2)

**Goal**: Cards, tables, buttons, and other components have refined visual styling with hover effects, gradients, and improved visual hierarchy.

**Independent Test**: Interact with cards, buttons, and tables across all pages — verify hover elevation changes on cards, gradient on AppBar, alternating table rows, and themed header areas on summary cards.

### Implementation for User Story 3

- [x] T017 [US3] Enhance BudgetSummaryCard header in budget-frontend/src/components/home/BudgetSummaryCard.tsx — add a header Box with a gradient background (primary.dark to primary.main), white text for the card title "Budget Summary", and appropriate padding; move the title inside this header area
- [x] T018 [P] [US3] Enhance FeatureNavigationCard styling in budget-frontend/src/components/home/FeatureNavigationCard.tsx — add a colored left border accent (4px) using a color prop passed from the parent, add smooth hover transition for transform and boxShadow (0.3s ease), add subtle background tint on hover
- [x] T019 [P] [US3] Enhance QuickActionsCard button styling in budget-frontend/src/components/home/QuickActionsCard.tsx — wrap each button icon in a circular Avatar/Box with a themed light background (primary.light at 15% opacity), add hover effect that slightly intensifies the background, ensure consistent button height and spacing
- [x] T020 [P] [US3] Enhance RecentActivityCard styling in budget-frontend/src/components/home/RecentActivityCard.tsx — add a header Box with themed background matching BudgetSummaryCard pattern, style list items with subtle hover background, improve category/person Chip styling with themed variant colors
- [x] T021 [P] [US3] Add alternating row styling to ExpenseListTable in budget-frontend/src/components/expenses/ExpenseListTable.tsx — verify the MuiTableRow theme override applies correctly, add subtle hover background on table rows (primary.light at 8% opacity), ensure table header row uses a slightly darker background (grey[100] or similar warm tint)
- [x] T022 [P] [US3] Enhance BudgetCard styling in budget-frontend/src/components/BudgetCard.tsx — add hover elevation transition (verify theme override applies), add subtle top border using budget status color (2px), improve spacing between progress bar and status information
- [x] T023 [P] [US3] Enhance CategoryCard styling in budget-frontend/src/app/categories/components/CategoryCard.tsx — add hover elevation effect, improve the visual distinction between parent and child categories using themed colors (secondary.light background tint for child categories)
- [x] T024 [P] [US3] Enhance BudgetForm and ExpenseForm styling — in budget-frontend/src/components/BudgetForm.tsx add a subtle header area with themed background for the form title; in budget-frontend/src/components/expenses/ExpenseForm.tsx apply the same pattern; ensure form Paper elevation and padding are consistent with the theme

**Checkpoint**: All interactive components have polished hover states, cards show themed headers, and tables display clear row differentiation.

---

## Phase 5: User Story 4 — Hardcoded Color Cleanup (Priority: P2)

**Goal**: Replace all hardcoded hex color values with theme palette references so the Warm Earth palette is applied universally.

**Independent Test**: Search the codebase for raw hex color values (#xxx, #xxxxxx) in .tsx/.ts files — zero instances should remain in component code (theme.ts definition values are acceptable).

### Implementation for User Story 4

- [x] T025 [US4] Replace hardcoded colors in BudgetPieChart in budget-frontend/src/components/dashboard/BudgetPieChart.tsx — import and use the MUI useTheme hook; replace COLORS object values: spent '#1976d2' → theme.palette.primary.main, remaining '#e0e0e0' → theme.palette.grey[300], overBudget '#d32f2f' → theme.palette.error.main, budgetPortion '#ff9800' → theme.palette.warning.main; replace text fill='#666' → theme.palette.text.secondary
- [x] T026 [P] [US4] Replace hardcoded border color in CategoryCard in budget-frontend/src/app/categories/components/CategoryCard.tsx — import and use the MUI useTheme hook; replace borderLeft '4px solid #1976d2' → `4px solid ${theme.palette.primary.main}`
- [x] T027 [US4] Scan all .tsx and .ts files in budget-frontend/src/ for any remaining hardcoded hex color values (#xxx, #xxxxxx, rgb(), rgba()) that should reference the theme — fix any found instances (excluding theme.ts palette definitions and any CSS-in-JS that genuinely needs literal values like rgba for opacity)

**Checkpoint**: Zero hardcoded colors remain in component files. All visual colors derive from the centralized theme.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, loading state consistency, and overall quality check.

- [x] T028 Update loading/skeleton states across all pages to use themed colors — verify CircularProgress spinners use primary.main (terracotta), verify Skeleton components pick up themed background colors, check budget-frontend/src/app/expenses/page.tsx skeleton, budget-frontend/src/components/home/BudgetSummaryCard.tsx loading state, and any other loading indicators
- [x] T029 [P] Verify WCAG AA contrast compliance — check all text-on-background combinations: dark brown (#3E2723) on cream (#FAF6F0), white (#FFFFFF) on terracotta (#A0522D), white on olive (#6B7B3A), white on burnt red (#C0392B); verify all meet 4.5:1 for normal text and 3:1 for large text; adjust any failing combinations
- [x] T030 [P] Verify mobile responsive layout — open all 7 pages at mobile viewport (375px) and desktop (1200px), confirm theme colors render consistently, confirm no layout regressions from new shadows/borders/padding, verify drawer temporary mode on mobile still works with themed sidebar
- [x] T031 Run quickstart.md validation scenarios — execute all 8 verification scenarios from specs/013-frontend-theme-redesign/quickstart.md and confirm each passes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — T001 → T002 → T003 must run sequentially (same file: theme.ts)
- **Phase 2 (US1 Color Theme)**: Depends on Phase 1 completion. T004 sequential (AppShell). T005–T010 can run in parallel (different page files)
- **Phase 3 (US2 Icons)**: Depends on Phase 1 completion. Can run in parallel with Phase 2. T011 sequential (navConfig). T012 sequential (BudgetCard). T013–T016 can run in parallel
- **Phase 4 (US3 Component Polish)**: Depends on Phase 1 completion. Can run in parallel with Phases 2/3. T017–T024 can run in parallel (different component files)
- **Phase 5 (US4 Hardcoded Colors)**: Depends on Phase 1 completion. T025 sequential (BudgetPieChart). T026 parallel. T027 runs last (final scan)
- **Phase 6 (Polish)**: Depends on ALL previous phases. T028–T030 can run in parallel. T031 runs last

### User Story Dependencies

- **US1 (Color Theme)**: Depends only on Phase 1 — no cross-story dependencies
- **US2 (Icons)**: Depends only on Phase 1 — independent of US1
- **US3 (Component Polish)**: Depends only on Phase 1 — independent of US1/US2
- **US4 (Hardcoded Colors)**: Depends only on Phase 1 — independent, but best done after US1 to avoid conflicts

### Within Each User Story

- Theme file (Phase 1) must be complete first
- Page-level tasks can run in parallel (different files)
- Component tasks that modify the same file must run sequentially

### Parallel Opportunities

- **Phase 2**: T005, T006, T007, T008, T009, T010 can all run in parallel (6 different page files)
- **Phase 3**: T013, T014, T015, T016 can run in parallel (4 different component/page files)
- **Phase 4**: T017, T018, T019, T020, T021, T022, T023, T024 can all run in parallel (8 different files)
- **Phase 5**: T025 and T026 can run in parallel (2 different files)
- **Phase 6**: T028, T029, T030 can run in parallel

---

## Parallel Example: User Story 1

```bash
# After Phase 1 (theme.ts) is complete:

# Sequential (AppShell has sidebar + AppBar):
Task T004: "Update AppShell sidebar and AppBar styling"

# Then all pages in parallel:
Task T005: "Update Home page theme colors"
Task T006: "Update Dashboard page theme colors"
Task T007: "Update Budgets page theme colors"
Task T008: "Update Yearly View page theme colors"
Task T009: "Update Categories page theme colors"
Task T010: "Update Expenses page theme colors"
```

---

## Implementation Strategy

### MVP First (Phase 1 + User Story 1)

1. Complete Phase 1: Theme file with Warm Earth palette (T001–T003)
2. Complete Phase 2: Apply theme to AppShell + all pages (T004–T010)
3. **STOP and VALIDATE**: All pages show warm earth colors, no black-and-white remnants
4. This alone delivers the biggest visual impact

### Incremental Delivery

1. Phase 1 (Setup) → Theme foundation ready
2. Phase 2 (US1 Colors) → MVP! App looks warm and cohesive
3. Phase 3 (US2 Icons) → Navigation and status indicators improved
4. Phase 4 (US3 Polish) → Cards, tables, and components feel premium
5. Phase 5 (US4 Cleanup) → Technical hygiene, full theme consistency
6. Phase 6 (Polish) → Loading states, accessibility, final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All changes are in `budget-frontend/src/` — no backend modifications
- Theme file (T001–T003) is the critical path — all other tasks depend on it
- Phases 2–5 can theoretically run in parallel after Phase 1, but sequential priority order (P1 → P2) is recommended for a single developer
- Commit after each phase for easy rollback
- Stop at any checkpoint to validate independently
