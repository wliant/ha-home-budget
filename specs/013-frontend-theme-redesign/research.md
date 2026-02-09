# Research: Frontend Theme Redesign

**Feature**: 013-frontend-theme-redesign
**Date**: 2026-02-09

## Decision 1: Warm Earth Color Palette — Specific Hex Values

**Decision**: Use the following Warm Earth palette as the MUI theme colors:

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Primary | Terracotta | `#A0522D` (main), `#C07850` (light), `#7A3B1E` (dark) | AppBar, sidebar active, primary buttons, links |
| Secondary | Olive | `#6B7B3A` (main), `#8A9E50` (light), `#4E5C2B` (dark) | Secondary buttons, accents, badges |
| Accent/Info | Gold | `#C49A3C` (main), `#D4B060` (light), `#9A7A2E` (dark) | Highlights, feature cards, info states |
| Success | Sage Green | `#5B8C5A` (main), `#7DAF7C` (light), `#3D6B3C` (dark) | On-track budgets, positive indicators |
| Warning | Amber | `#D4A030` (main), `#E0B850` (light), `#B08020` (dark) | Budget warnings, approaching limits |
| Error | Burnt Red | `#C0392B` (main), `#D95548` (light), `#9A2D22` (dark) | Over-budget, errors, delete actions |
| Background | Warm Cream | `#FAF6F0` (default), `#FFFFFF` (paper) | Page background, card backgrounds |
| Text | Dark Brown | `#3E2723` (primary), `#5D4037` (secondary) | Body text, labels |

**Rationale**: These colors evoke a cozy, warm home feel appropriate for a household budget app. Terracotta and olive are the core identity colors while gold provides accent warmth. The status colors (success/warning/error) are chosen to harmonize with the earth tones while remaining clearly distinguishable for budget status indicators. All combinations meet WCAG AA contrast requirements (4.5:1 for normal text on cream background).

**Alternatives Considered**:
- Teal/Green finance palette — too corporate/cold for a home application
- Blue/Indigo palette — too similar to the current default MUI blue; wouldn't feel like a meaningful redesign
- Pure brown monochrome — insufficient differentiation between interactive elements

## Decision 2: Icon Mapping Strategy

**Decision**: Audit all current icons and replace mismatched ones. Use filled icon variants for active/selected states and outlined variants for default states where distinction matters.

### Navigation Icon Mapping (Current → Updated)

| Navigation Item | Current Icon | Updated Icon | Rationale |
|----------------|-------------|-------------|-----------|
| Home | `Home` | `Home` | Already appropriate — keep |
| Dashboard | `Dashboard` | `Dashboard` | Already appropriate — keep |
| Budgets | `AccountBalanceWallet` | `AccountBalanceWallet` | Already appropriate — keep |
| Yearly View | `CalendarMonth` | `CalendarMonth` | Already appropriate — keep |
| Categories | `Category` | `Category` | Already appropriate — keep |
| Expenses | `ListAlt` | `ReceiptLong` | `ListAlt` is too generic; `ReceiptLong` better represents expense records |
| Record Expense | `Receipt` | `AddCard` | `Receipt` duplicates concept with expenses list; `AddCard` clearly indicates "add new expense" |

### Page-Level Icon Additions

| Location | Current State | Addition |
|----------|---------------|----------|
| Page headers (all pages) | Text-only titles | Add page icon next to title matching nav icon |
| Budget status chips | Color only | Add `CheckCircle`/`Warning`/`Error` icons to status chips |
| Empty states | Text-only messages | Add contextual empty state icons |

**Rationale**: The current icons are mostly well-chosen. Only 2 navigation icons need updating to avoid conceptual overlap and improve clarity. Adding icons to status indicators improves accessibility (not relying on color alone).

**Alternatives Considered**:
- Using custom SVG icons — unnecessary complexity when MUI icons provide comprehensive coverage
- Using emoji instead of icons — inconsistent rendering across platforms

## Decision 3: Component Enhancement Strategy

**Decision**: Apply enhancements through MUI theme component overrides (centralized) rather than per-component inline styles, with targeted per-component additions for unique behaviors.

### Theme-Level Overrides (in theme.ts)

| Component | Enhancement |
|-----------|-------------|
| MuiAppBar | Gradient background using primary colors |
| MuiCard | Enhanced shadow, hover elevation transition, subtle border |
| MuiButton | Hover color shift, consistent padding |
| MuiTableRow | Alternating background with warm cream tints |
| MuiListItemButton | Active state with primary light background and left border |
| MuiLinearProgress | Rounded ends, themed track color |
| MuiChip | Softer border radius, themed variant colors |
| MuiPaper | Subtle warm-tinted shadow |

### Per-Component Enhancements

| Component | Enhancement |
|-----------|-------------|
| FeatureNavigationCard | Colored left border accent per card |
| BudgetCard | Themed progress bar colors from budget status |
| QuickActionsCard | Icon with themed circular background |
| BudgetSummaryCard | Header area with primary gradient |

**Rationale**: Centralizing overrides in theme.ts ensures consistency and makes future adjustments trivial. Per-component enhancements are reserved for unique visual treatments that don't apply globally.

**Alternatives Considered**:
- CSS-in-JS with styled-components — adds dependency; MUI's sx prop and theme overrides are sufficient
- Tailwind CSS — would conflict with MUI's styling system
- Per-component inline sx only — leads to inconsistency and harder maintenance

## Decision 4: Hardcoded Color Remediation

**Decision**: Replace all hardcoded hex values with theme palette references.

| File | Hardcoded Value | Replacement |
|------|----------------|-------------|
| `BudgetPieChart.tsx` | `#1976d2` (spent) | `theme.palette.primary.main` |
| `BudgetPieChart.tsx` | `#e0e0e0` (remaining) | `theme.palette.grey[300]` |
| `BudgetPieChart.tsx` | `#d32f2f` (overBudget) | `theme.palette.error.main` |
| `BudgetPieChart.tsx` | `#ff9800` (budgetPortion) | `theme.palette.warning.main` |
| `BudgetPieChart.tsx` | `#666` (text) | `theme.palette.text.secondary` |
| `CategoryCard.tsx` | `#1976d2` (border) | `theme.palette.primary.main` |

**Rationale**: Theme references ensure that when the palette is updated, all visual elements update automatically. This is essential for the Warm Earth theme to be applied consistently.

## Decision 5: Typography and Font

**Decision**: Keep Roboto as the primary font family. No font change needed — Roboto is neutral and works well with any color palette. Adjust heading weights and colors for warmth.

### Typography Updates

| Element | Current | Updated |
|---------|---------|---------|
| h1-h3 | weight: 500 | weight: 600 (slightly bolder for warmth) |
| h4-h6 | weight: 500 | weight: 500 (keep) |
| body text color | MUI default (#000000de) | `#3E2723` (dark brown) |
| secondary text | MUI default (#00000099) | `#5D4037` (medium brown) |

**Rationale**: Warm brown text colors instead of pure black soften the visual appearance significantly and harmonize with the earth tone palette. Slightly bolder headings add visual weight.

## Decision 6: Sidebar Navigation Styling

**Decision**: Style the sidebar with a warm background and clear active state indicator.

| Element | Current Style | New Style |
|---------|--------------|-----------|
| Sidebar background | White (#fff) | Warm cream (`#F5EDE3`) |
| Active nav item | Default MUI highlight | Primary light bg + 3px left border in primary color |
| Nav icon color (default) | MUI default grey | `text.secondary` (brown) |
| Nav icon color (active) | MUI primary blue | `primary.main` (terracotta) |
| Nav text color | Default black | `text.primary` (dark brown) |
| Divider | Default | `primary.light` with reduced opacity |

**Rationale**: A tinted sidebar creates visual warmth and separates navigation from content. The left border active indicator is a well-established pattern for sidebar navigation.
