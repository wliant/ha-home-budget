# Phase 0: Technical Research - Homepage Dashboard Update

**Feature**: Homepage Dashboard Update (006)
**Date**: 2025-12-22
**Researcher**: AI Planning Agent

## Overview

This document records technical decisions for transforming the homepage into an actionable dashboard. Since this feature builds on existing infrastructure (budget and expense APIs already implemented), research focuses on component architecture, data fetching strategies, and responsive layout patterns.

## Research Questions & Decisions

### 1. Component Architecture Strategy

**Question**: Should homepage widgets be embedded directly in page.tsx or extracted as separate components?

**Decision**: Extract as separate components in `src/components/home/`

**Rationale**:
- **Separation of concerns**: Each widget (budget summary, recent activity, quick actions) has distinct data requirements and UI logic
- **Reusability**: Components like BudgetSummaryCard could be reused in dashboard or budget detail pages
- **Testability**: Isolated components are easier to test independently
- **Maintainability**: Changes to one widget don't require touching others

**Alternatives Considered**:
- **Inline implementation**: Simpler initially but creates a monolithic 500+ line page.tsx that's hard to maintain
- **Hooks-based extraction**: Could use custom hooks for logic only, but still mixes concerns in single component
- **Rejected because**: As homepage complexity grows (future widgets, personalization), component extraction provides better scalability

### 2. Data Fetching Strategy

**Question**: How should homepage fetch data from multiple endpoints (budget summary, recent expenses)?

**Decision**: Parallel data fetching with independent loading states using React.useEffect + Promise.all

**Rationale**:
- **Performance**: Parallel requests reduce total load time (2 requests @ 200ms each = 200ms total, not 400ms sequential)
- **User experience**: Independent loading states allow partial rendering (show budget summary while expenses still loading)
- **Error resilience**: One API failure doesn't block the entire homepage (graceful degradation)
- **Simplicity**: No additional libraries needed (SWR, React Query) for this simple use case

**Implementation Pattern**:
```typescript
// Each widget manages its own loading/error state
const [budgetData, setBudgetData] = useState<BudgetSummaryDTO | null>(null);
const [budgetLoading, setBudgetLoading] = useState(true);
const [budgetError, setBudgetError] = useState<string>('');

useEffect(() => {
  const loadData = async () => {
    try {
      const data = await budgetService.getCurrentMonthBudget();
      setBudgetData(data);
    } catch (err) {
      setBudgetError('Failed to load budget');
    } finally {
      setBudgetLoading(false);
    }
  };
  loadData();
}, []);
```

**Alternatives Considered**:
- **SWR or React Query**: Better for complex caching/revalidation needs, but overkill for static homepage dashboard
- **Server-side rendering (SSR)**: Could use Next.js getServerSideProps, but adds complexity and doesn't improve UX for private home network
- **Rejected because**: Client-side fetching is simpler, works with existing API service pattern, and performance is acceptable for home network

### 3. Responsive Layout Approach

**Question**: How should the homepage adapt to different screen sizes (320px mobile to 1920px desktop)?

**Decision**: Material-UI Grid system with breakpoint-based column spans

**Rationale**:
- **Consistency**: Already using MUI Grid throughout application (budgets page, categories page)
- **Proven pattern**: MUI Grid handles responsive breakpoints automatically (xs, sm, md, lg, xl)
- **Accessibility**: Built-in ARIA attributes and keyboard navigation
- **Maintenance**: No custom CSS media queries to maintain

**Layout Strategy**:
```typescript
// Desktop (md+): 2 columns (budget left, recent activity right)
// Tablet (sm): 2 columns stacked
// Mobile (xs): 1 column full-width

<Grid container spacing={3}>
  <Grid item xs={12} md={6}>
    <BudgetSummaryCard />
  </Grid>
  <Grid item xs={12} md={6}>
    <RecentActivityCard />
  </Grid>
  <Grid item xs={12}>
    <QuickActionsCard />
  </Grid>
</Grid>
```

**Alternatives Considered**:
- **CSS Grid**: More flexible layout control but requires custom media queries
- **Flexbox only**: Simpler but harder to maintain complex layouts
- **Rejected because**: MUI Grid provides best balance of flexibility, consistency, and maintainability

### 4. Empty State Handling

**Question**: What should users see when no budget or expenses exist?

**Decision**: Contextual empty states with actionable prompts

**Rationale**:
- **User guidance**: New users need direction on what to do first (create budget before recording expenses)
- **Call to action**: Empty states should include buttons linking to creation pages
- **Visual hierarchy**: Use MUI Box + Typography for consistent empty state styling
- **Motivation**: Friendly messaging encourages first-time setup

**Empty State Patterns**:
- **No budget**: "Create your first monthly budget to start tracking spending" + "Create Budget" button
- **No expenses**: "No expenses recorded yet" + "Record Expense" button
- **Both empty**: Show budget prompt first (logical dependency: need budget before expenses make sense)

**Alternatives Considered**:
- **Generic "no data" message**: Less helpful, doesn't guide user action
- **Hide widgets entirely**: Creates confusing empty homepage
- **Rejected because**: Contextual prompts improve onboarding and reduce support needs

### 5. Error Handling Strategy

**Question**: How should homepage handle backend unavailability or API errors?

**Decision**: Per-widget error states with retry actions, preserve working widgets

**Rationale**:
- **Fault isolation**: If budget API fails, expense widget should still work
- **User feedback**: Clear error messages explain what failed and why
- **Recovery option**: Retry buttons allow users to manually refresh failed data
- **Graceful degradation**: System status widget shows backend health separately

**Error Patterns**:
```typescript
// Widget-level error handling
{error ? (
  <Alert severity="error">
    {error}
    <Button onClick={handleRetry}>Retry</Button>
  </Alert>
) : (
  // Normal content
)}
```

**Alternatives Considered**:
- **Full-page error**: One failure blocks entire homepage (poor UX)
- **Silent failure**: Hides errors from users (no recovery path)
- **Toast notifications**: Easy to miss, don't persist
- **Rejected because**: Per-widget errors provide best user awareness and recovery options

### 6. Quick Actions Design

**Question**: Which actions should be included and how should they be organized?

**Decision**: 4 primary actions in horizontal card layout with icons

**Actions Selected**:
1. **Create Budget** - Primary P1 action (required before anything else)
2. **Record Expense** - Primary P1 action (most frequent user task)
3. **View Categories** - Supporting P2 action (setup task)
4. **View Dashboard** - Supporting P2 action (analytics/insights)

**Rationale**:
- **Frequency**: Create budget and record expense are daily/weekly tasks
- **User journey**: Actions map to common workflows identified in analytics
- **Discoverability**: Quick actions reduce navigation depth (homepage → action vs homepage → menu → page → action)
- **Visual weight**: 4 actions fit well in horizontal layout on all screen sizes

**Layout**:
```typescript
<Grid container spacing={2}>
  <Grid item xs={6} sm={3}>
    <Button fullWidth variant="contained" startIcon={<AddIcon />}>
      Create Budget
    </Button>
  </Grid>
  {/* ...other actions */}
</Grid>
```

**Alternatives Considered**:
- **More actions (6-8)**: Clutters interface, reduces visibility of each action
- **Dropdown menu**: Hides actions behind click, reduces discoverability
- **Floating action button**: Good for single primary action, not 4 actions
- **Rejected because**: 4 visible buttons balance discoverability with simplicity

### 7. Feature Navigation Update

**Question**: How should feature cards be updated to reflect implemented features?

**Decision**: Replace "coming soon" opacity effect with active navigation buttons for all implemented features

**Features Status**:
- **Budgets**: ✅ Implemented (existing)
- **Categories**: ✅ Implemented (005-category-ui)
- **Dashboard**: ✅ Implemented (existing)
- **Expenses**: ✅ Implemented (existing)

**Pattern**:
```typescript
// Before (placeholder):
<Card elevation={2} sx={{ opacity: 0.6 }}>
  <Typography>Categories - Coming soon...</Typography>
</Card>

// After (active):
<Card elevation={2}>
  <CardActions>
    <Button variant="contained" onClick={() => router.push('/categories')}>
      View Categories
    </Button>
  </CardActions>
</Card>
```

**Alternatives Considered**:
- **Remove feature cards entirely**: Loses feature discovery affordance
- **Keep "coming soon" for future features**: No future features planned currently
- **Rejected because**: Active navigation cards serve as feature directory and quick access

## Technology Stack Confirmation

### Frontend
- **Framework**: Next.js 14.x (App Router)
- **UI Library**: Material-UI v5.14.20
- **Language**: TypeScript 5.x
- **State Management**: React hooks (useState, useEffect)
- **Routing**: Next.js client-side router

### Backend
- **No changes required**: All APIs already exist
  - `GET /api/budgets/current` - Current month budget summary
  - `GET /api/expenses?limit=5&sort=date,desc` - Recent expenses

### Testing
- **Frontend**: Optional (not specified in requirements)
- **Backend**: N/A (no backend changes)

## Performance Considerations

### Load Time Optimization
- **Parallel API calls**: Budget + expenses fetch simultaneously
- **Progressive rendering**: Show page shell immediately, data loads async
- **Code splitting**: Homepage components stay in main bundle (most visited page)

### Bundle Size Impact
- **New components**: ~8KB total (4 widgets @ ~2KB each)
- **No new dependencies**: Uses existing MUI, React, Next.js
- **Expected**: Homepage bundle increases from 150KB to 158KB gzipped

### Runtime Performance
- **No expensive operations**: Simple list rendering, no heavy computations
- **Memoization**: Not needed initially (small data sets, infrequent re-renders)
- **Optimization strategy**: Measure first, optimize if load time exceeds 2s target

## Security Considerations

### Authentication
- **X-Hass-User header**: Already handled by api.ts service
- **No changes needed**: Homepage uses same auth as other pages

### Data Exposure
- **Household-wide visibility**: All users see same budget/expense data (per constitution)
- **No sensitive data masking**: Budget amounts and expense details visible to all household members
- **Privacy**: Acceptable for private home network (not public internet)

## Accessibility

### WCAG 2.1 Compliance
- **Material-UI built-in**: ARIA labels, keyboard navigation, focus management
- **Color contrast**: MUI default theme meets AA standards
- **Screen readers**: Semantic HTML + ARIA attributes for dynamic content
- **Keyboard shortcuts**: Not required initially (future enhancement)

## Migration Strategy

### Rollout Plan
- **No database migration**: Uses existing schema
- **No API changes**: Uses existing endpoints
- **Frontend-only update**: Replace src/app/page.tsx + add widget components
- **Zero downtime**: Deploy frontend, no backend restart needed

### Rollback Plan
- **Git revert**: Single commit rollback
- **Feature flag**: Not needed (simple UI change, low risk)

## References

- Next.js App Router Documentation: https://nextjs.org/docs/app
- Material-UI Grid: https://mui.com/material-ui/react-grid/
- React useEffect Hook: https://react.dev/reference/react/useEffect
- Existing codebase patterns: budget-frontend/src/app/budgets/page.tsx (reference implementation)

## Research Completion

**Status**: ✅ Complete
**No unresolved questions**: All technical context defined
**Ready for Phase 1**: Data model and contracts generation
