# Feature Specification: Frontend Theme Redesign

**Feature Branch**: `013-frontend-theme-redesign`
**Created**: 2026-02-09
**Status**: Draft
**Input**: User description: "beautify the frontend ui, currently it is very black and white. use some theme and proper color scheme. use the right icon as well"

## Clarifications

### Session 2026-02-09

- Q: What color palette direction should the redesign use? → A: Warm Earth — primary warm brown/terracotta with olive and gold accents for a cozy home feel

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visually Appealing Color Theme (Priority: P1)

As a household member, I want the budget application to use a cohesive, modern color scheme instead of the current black-and-white appearance, so that the interface feels polished, professional, and pleasant to use daily.

Currently the application uses a minimal palette that feels stark and utilitarian. The redesign should introduce a warm, cozy color scheme appropriate for a home budget application — using warm browns/terracotta as the primary color with olive and gold accents that evoke a comfortable home feel. All pages (Home, Dashboard, Budgets, Categories, Expenses) should reflect the unified theme consistently.

**Why this priority**: The color theme is the foundation of the visual redesign. Every other visual improvement builds on having a coherent palette in place first. This delivers the highest-impact visual change.

**Independent Test**: Can be fully tested by navigating through all pages and verifying the new color scheme is applied consistently, with no remnants of the old black-and-white styling.

**Acceptance Scenarios**:

1. **Given** the application is loaded, **When** a user views any page, **Then** the page uses the new color palette with primary, secondary, and accent colors applied consistently to navigation, cards, buttons, and text.
2. **Given** the new theme is applied, **When** a user views the sidebar navigation, **Then** the navigation uses themed colors for the background, active item highlight, and icon/text colors instead of default black-on-white.
3. **Given** the new theme is applied, **When** a user views budget status indicators (on track, warning, over budget), **Then** the status colors (green, orange, red) are harmonized with the overall palette while remaining clearly distinguishable.
4. **Given** the new theme is applied, **When** a user views the application on mobile and desktop, **Then** the color scheme renders consistently across both screen sizes.

---

### User Story 2 - Meaningful Icons Throughout the Interface (Priority: P1)

As a household member, I want every action, navigation item, and visual element to use appropriate, intuitive icons so that I can quickly understand what each element does at a glance without reading labels.

The current application uses icons inconsistently — some areas have appropriate icons while others use generic or mismatched ones. Every interactive element, category indicator, status badge, and navigation item should use a contextually meaningful icon.

**Why this priority**: Icons are critical for usability and visual appeal. Combined with the color theme, they transform the application from a prototype look to a finished product. This is equally important as the color theme.

**Independent Test**: Can be tested by reviewing every page and verifying that all navigation items, action buttons, status indicators, and card headers use appropriate, recognizable icons.

**Acceptance Scenarios**:

1. **Given** the application is loaded, **When** a user views the sidebar navigation, **Then** each navigation item displays an icon that clearly represents its destination (e.g., a dashboard gauge icon for Dashboard, a wallet/money icon for Budgets, a tag/folder icon for Categories, a receipt icon for Expenses).
2. **Given** a user is on any page with action buttons, **When** they view create/edit/delete actions, **Then** each action button has an appropriate icon (e.g., plus for create, pencil for edit, trash for delete).
3. **Given** a user is on the Home page, **When** they view the quick action buttons and feature cards, **Then** each card and action uses a distinctive, descriptive icon that hints at its purpose.
4. **Given** a user views budget cards or expense items, **When** status indicators are shown, **Then** status badges use appropriate icons (e.g., checkmark for on-track, warning triangle for approaching limit, error circle for over budget).

---

### User Story 3 - Polished Card and Component Styling (Priority: P2)

As a household member, I want cards, tables, buttons, and other UI components to have refined visual styling — subtle shadows, rounded corners, hover effects, and visual hierarchy — so the interface feels modern and interactive.

The current components use basic Material-UI defaults. The redesign should enhance visual depth with subtle gradients or background tints for cards, improved hover/focus states for interactive elements, and better visual separation between content sections.

**Why this priority**: While the color theme and icons address the most visible issues, polished component styling elevates the overall quality. This is a refinement layer that makes the difference between "themed" and "beautiful."

**Independent Test**: Can be tested by interacting with cards, buttons, and tables across all pages, verifying improved hover states, shadows, visual hierarchy, and spacing.

**Acceptance Scenarios**:

1. **Given** the new styling is applied, **When** a user hovers over a card or button, **Then** a subtle visual feedback is displayed (e.g., elevation change, color shift, or scale effect).
2. **Given** the new styling is applied, **When** a user views the Dashboard page, **Then** budget summary cards have a visually distinct header area with the themed primary color or gradient.
3. **Given** the new styling is applied, **When** a user views expense tables, **Then** table rows have alternating row colors or subtle dividers for easier scanning.
4. **Given** the new styling is applied, **When** a user views the AppBar/header, **Then** it uses a gradient or branded color scheme instead of a flat single color.

---

### User Story 4 - Hardcoded Color Cleanup (Priority: P2)

As a household member, I want all colors in the application to come from the centralized theme so that the visual experience is perfectly consistent and any future theme adjustments apply everywhere automatically.

There are currently a few instances of hardcoded color values in components (e.g., pie chart colors, category card borders) that bypass the theme system. These should all reference theme palette values.

**Why this priority**: Cleaning up hardcoded colors ensures long-term maintainability and consistency. It's a technical hygiene task that directly impacts visual consistency.

**Independent Test**: Can be tested by verifying that no component uses hardcoded hex color values and that all visual colors derive from the theme palette.

**Acceptance Scenarios**:

1. **Given** the theme is applied, **When** a user views pie charts on the Dashboard, **Then** chart colors use theme-derived values instead of hardcoded hex codes.
2. **Given** the theme is applied, **When** a user views category cards with child indicators, **Then** the visual indicator uses a theme color rather than a hardcoded value.
3. **Given** any component is inspected, **When** checking for color values, **Then** all colors reference the theme palette (no raw hex codes like `#1976d2` or `#666`).

---

### Edge Cases

- What happens when text colors from the new palette are placed on colored backgrounds — is there sufficient contrast for readability?
- How does the theme appear for users with color vision deficiency — are status indicators distinguishable by more than just color (e.g., icons + color)?
- What happens when very long category names or budget descriptions wrap — does the styled card layout handle overflow gracefully?
- What happens with the loading/skeleton states — do they match the new theme colors?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application MUST use a Warm Earth color palette (warm brown/terracotta primary, olive and gold accents) that replaces the current default black-and-white appearance
- **FR-002**: The sidebar navigation MUST display contextually appropriate icons for each menu item and use themed background/highlight colors for the active item
- **FR-003**: All action buttons (create, edit, delete, view) MUST display appropriate icons alongside their text labels
- **FR-004**: Budget status indicators MUST use both color and icon to communicate status (on-track, warning, over-budget) for accessibility
- **FR-005**: All hardcoded color values in components MUST be replaced with theme palette references
- **FR-006**: Cards MUST have enhanced visual styling including themed header areas, improved shadows, and hover feedback effects
- **FR-007**: The AppBar/header MUST use the primary theme color or gradient to establish visual branding
- **FR-008**: Expense tables MUST have visual row differentiation (alternating colors or subtle dividers) for easier scanning
- **FR-009**: Pie charts and other data visualizations MUST use colors derived from the theme palette
- **FR-010**: The color theme MUST maintain a minimum contrast ratio of 4.5:1 for normal text and 3:1 for large text (WCAG AA compliance)
- **FR-011**: Loading states (spinners, skeletons) MUST use theme-consistent colors
- **FR-012**: Quick action buttons and feature cards on the Home page MUST each have a distinctive icon and themed color accent

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of pages (Home, Dashboard, Budgets, Yearly View, Categories, Expenses, Record Expense) display the new color theme consistently with zero instances of default black-and-white styling
- **SC-002**: 100% of navigation items, action buttons, and status indicators display contextually appropriate icons
- **SC-003**: Zero hardcoded hex color values remain in any component — all colors reference the centralized theme
- **SC-004**: All text-on-background color combinations meet WCAG AA contrast ratio (4.5:1 for normal text, 3:1 for large text)
- **SC-005**: All interactive elements (buttons, cards, navigation items) provide visible hover/focus feedback within 100ms of user interaction
- **SC-006**: The visual redesign introduces no layout regressions — all pages render correctly on both mobile (< 600px) and desktop (> 960px) viewports

## Assumptions

- The redesign targets the existing light theme only; dark mode is not in scope
- The color palette will use a Warm Earth scheme — primary warm brown/terracotta with olive and gold accents, conveying a cozy home feel
- Existing functionality and layout structure remain unchanged — this is a visual-only enhancement
- The Material-UI v5 theme system and existing `@mui/icons-material` library provide sufficient customization and icon coverage; no additional dependencies are needed
- Performance impact of visual changes (shadows, hover effects) is negligible
- The current icon choices that are already appropriate will be retained; only mismatched or missing icons will be updated
