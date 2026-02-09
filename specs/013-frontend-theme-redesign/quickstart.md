# Quickstart: Frontend Theme Redesign

**Feature**: 013-frontend-theme-redesign
**Date**: 2026-02-09

## Overview

This feature redesigns the frontend visual theme from the default black-and-white MUI palette to a Warm Earth color scheme (terracotta, olive, gold) with improved icons, component styling, and hardcoded color cleanup.

## Prerequisites

- Node.js and npm installed
- Frontend dev server: `cd budget-frontend && npm run dev`
- Backend running (for live data testing)

## Verification Scenarios

### Scenario 1: Theme Color Consistency

1. Start the frontend dev server
2. Navigate to each page: Home, Dashboard, Budgets, Yearly View, Categories, Expenses, Record Expense
3. **Verify**: AppBar shows terracotta gradient, sidebar has warm cream background, buttons use terracotta/olive colors, text is dark brown (not black)
4. **Verify**: No remnants of the old blue (#1976d2) primary color appear anywhere

### Scenario 2: Navigation Icons and Active State

1. Click through each navigation item in the sidebar
2. **Verify**: Each item has a contextually appropriate icon
3. **Verify**: Active navigation item has a left border indicator and primary-tinted background
4. **Verify**: Active item icon color is terracotta, inactive is brown

### Scenario 3: Budget Status Indicators

1. Navigate to Dashboard or Budgets page
2. View budgets with different spending percentages (0-50%, 50-75%, 75-90%, 90%+, 100%+)
3. **Verify**: Each status level shows both a color indicator AND an icon (checkmark, warning, error)
4. **Verify**: Status colors harmonize with the Warm Earth palette

### Scenario 4: Card Hover Effects

1. Navigate to Home page
2. Hover over Feature Navigation cards and Quick Action buttons
3. **Verify**: Cards show subtle elevation change or visual feedback on hover
4. **Verify**: Hover transitions are smooth (not abrupt)

### Scenario 5: Expense Table Styling

1. Navigate to Expenses page (with existing expense data)
2. **Verify**: Table rows have alternating background colors for easier scanning
3. **Verify**: Row colors use warm cream tints (not grey)

### Scenario 6: Pie Chart Theme Integration

1. Navigate to Dashboard page
2. View the spending breakdown pie charts
3. **Verify**: Chart colors use theme palette values (not hardcoded hex)
4. **Verify**: Chart text labels use brown text color (not #666 grey)

### Scenario 7: Mobile Responsiveness

1. Open browser DevTools, switch to mobile viewport (375px width)
2. Navigate through all pages
3. **Verify**: Theme colors apply consistently on mobile
4. **Verify**: No layout regressions from styling changes

### Scenario 8: Contrast Accessibility

1. Use browser DevTools accessibility checker or manual inspection
2. Check text-on-background combinations on all pages
3. **Verify**: Normal text meets 4.5:1 contrast ratio
4. **Verify**: Large text meets 3:1 contrast ratio
5. **Verify**: Status indicators are distinguishable without relying on color alone (have icons)
