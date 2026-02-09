# Implementation Plan: Frontend Theme Redesign

**Branch**: `013-frontend-theme-redesign` | **Date**: 2026-02-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-frontend-theme-redesign/spec.md`

## Summary

Redesign the frontend visual theme from the default MUI blue/black-and-white palette to a Warm Earth color scheme (terracotta primary, olive secondary, gold accents) with warm brown text, cream backgrounds, and improved component styling. Update icons for contextual accuracy, add hover effects and visual hierarchy to cards/tables, and replace all hardcoded color values with theme references. Frontend-only changes — no backend or data model modifications required.

## Technical Context

**Language/Version**: TypeScript 5.3.3 (frontend only)
**Primary Dependencies**: Next.js 14.0.4, Material-UI 5.14.20, @mui/icons-material 5.14.19, Recharts 3.7.0, React 18.2.0
**Storage**: N/A (no data changes)
**Testing**: Manual visual testing (no automated tests — per spec assumptions)
**Target Platform**: Web browser (desktop + mobile responsive)
**Project Type**: Web application (frontend only — no backend changes)
**Performance Goals**: Hover/focus feedback within 100ms; no rendering regressions
**Constraints**: WCAG AA contrast (4.5:1 normal text, 3:1 large text); no new dependencies
**Scale/Scope**: 7 pages, ~25 components, 1 theme file, 6 hardcoded color instances

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | Spec written and clarified before planning |
| II. Clarify Before Planning | PASS | `/speckit.clarify` completed — 1 question resolved (palette direction) |
| III. Incremental Story-Based Delivery | PASS | 4 user stories (P1×2, P2×2), each independently testable |
| IV. Constitution Gates | PASS | This check; re-check after Phase 1 |
| V. Task Traceability | DEFERRED | Will be validated at `/speckit.tasks` phase |
| VI. Test-Optional | PASS | No tests specified; manual verification via quickstart.md |
| VII. Artifact Consistency | DEFERRED | Will be validated via `/speckit.analyze` |
| Technical Stack: Next.js frontend | PASS | All changes in budget-frontend/ (Next.js) |
| Technical Stack: Spring Boot backend | PASS | No backend changes required |
| Auth: X-Hass-User | PASS | No auth changes; existing flow untouched |
| Multi-user household | PASS | Visual-only changes; no user-specific behavior affected |

**Gate Result**: PASS — no violations. Proceeding to Phase 0.

### Post-Phase 1 Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I–VII | PASS | No changes from pre-Phase 0 check |
| Technical Stack | PASS | Frontend-only; no new dependencies added |
| No constitution violations | PASS | All design decisions use existing MUI capabilities |

## Project Structure

### Documentation (this feature)

```text
specs/013-frontend-theme-redesign/
├── plan.md              # This file
├── research.md          # Phase 0: palette decisions, icon mapping, component strategy
├── data-model.md        # Phase 1: no data changes (documented)
├── quickstart.md        # Phase 1: 8 verification scenarios
├── contracts/           # Phase 1: no API changes (documented)
│   └── README.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (files to be modified)

```text
budget-frontend/src/
├── styles/
│   └── theme.ts                          # Core: palette, typography, component overrides
├── components/
│   ├── navigation/
│   │   ├── AppShell.tsx                  # AppBar gradient, sidebar styling
│   │   └── navConfig.tsx                 # Icon updates (Expenses, Record Expense)
│   ├── BudgetCard.tsx                    # Status icon additions, hover effects
│   ├── BudgetForm.tsx                    # Form styling consistency
│   ├── dashboard/
│   │   └── BudgetPieChart.tsx            # Replace 5 hardcoded colors with theme refs
│   ├── expenses/
│   │   ├── ExpenseListTable.tsx          # Alternating row colors
│   │   ├── ExpenseFilters.tsx            # Filter control styling
│   │   └── ExpenseList.tsx               # Action button icons
│   └── home/
│       ├── BudgetSummaryCard.tsx          # Header gradient, status icons
│       ├── FeatureNavigationCard.tsx      # Colored accent borders
│       ├── QuickActionsCard.tsx           # Icon backgrounds, themed buttons
│       └── RecentActivityCard.tsx         # Activity item styling
├── app/
│   ├── page.tsx                          # Home page icon/color updates
│   ├── dashboard/page.tsx                # Dashboard card styling
│   ├── budgets/page.tsx                  # Budget list styling
│   ├── budgets/yearly/page.tsx           # Yearly view table styling
│   ├── categories/
│   │   ├── components/CategoryCard.tsx   # Replace hardcoded border color
│   │   └── page.tsx                      # Categories page styling
│   └── expenses/page.tsx                 # Expenses page styling
└── services/
    └── budgetService.ts                  # Status color mapping (already uses MUI color names)
```

**Structure Decision**: Frontend-only modifications. All changes are in `budget-frontend/src/`. The theme file (`styles/theme.ts`) is the central point of change, with component files updated to consume theme values and add enhanced styling.

## Design Decisions

### Warm Earth Palette (from research.md)

| Role | Main | Light | Dark |
|------|------|-------|------|
| Primary (Terracotta) | `#A0522D` | `#C07850` | `#7A3B1E` |
| Secondary (Olive) | `#6B7B3A` | `#8A9E50` | `#4E5C2B` |
| Info (Gold) | `#C49A3C` | `#D4B060` | `#9A7A2E` |
| Success (Sage) | `#5B8C5A` | `#7DAF7C` | `#3D6B3C` |
| Warning (Amber) | `#D4A030` | `#E0B850` | `#B08020` |
| Error (Burnt Red) | `#C0392B` | `#D95548` | `#9A2D22` |
| Background | `#FAF6F0` | — | — |
| Text Primary | `#3E2723` | — | — |
| Text Secondary | `#5D4037` | — | — |

### Icon Changes (2 navigation icons)

- Expenses: `ListAlt` → `ReceiptLong`
- Record Expense: `Receipt` → `AddCard`
- Status chips: Add `CheckCircle`/`Warning`/`ErrorOutline` icons

### Component Overrides (theme.ts)

- **MuiAppBar**: Gradient from `primary.dark` to `primary.main`
- **MuiCard**: Hover elevation transition, subtle warm border
- **MuiButton**: Themed hover states
- **MuiTableRow**: Alternating warm cream rows
- **MuiListItemButton**: Active state with left border + primary light background
- **MuiLinearProgress**: Rounded bar, themed track
- **MuiChip**: Softer radius, themed colors

### Hardcoded Color Fixes (6 instances)

All in `BudgetPieChart.tsx` (5) and `CategoryCard.tsx` (1) — replace with `theme.palette.*` references.

## Complexity Tracking

> No constitution violations — this section is empty.
