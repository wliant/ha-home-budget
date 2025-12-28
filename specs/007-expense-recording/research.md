# Phase 0: Technical Research

**Feature**: Expense Recording
**Branch**: `007-expense-recording`
**Date**: 2025-12-22

## Technical Decisions

### 1. Frontend Framework & Architecture

**Decision**: Use Next.js 14.x App Router with TypeScript 5.x

**Rationale**:
- **Constitution Requirement**: Next.js is mandated for all frontend features (NON-NEGOTIABLE)
- **Existing Pattern**: Features 002, 005, and 006 already use Next.js App Router
- **App Router Benefits**: Server components, file-based routing, built-in data fetching
- **Path Structure**: `/expenses/new` follows established pattern (`/budgets/new`, `/categories`)

**Alternatives Considered**: None - Next.js is a constitutional requirement

---

### 2. UI Component Library

**Decision**: Material-UI v5 (MUI)

**Rationale**:
- **Consistency**: All existing features (budgets, categories, homepage) use Material-UI
- **Component Availability**: TextField, Button, Select, DatePicker components available
- **Theme Integration**: Existing theme configuration supports dark mode and consistent styling
- **TypeScript Support**: Strong typing for props and events

**Alternatives Considered**: None - maintaining consistency with existing codebase is critical

---

### 3. Form Management

**Decision**: React state with controlled components (useState)

**Rationale**:
- **Simplicity**: Form has only 4 fields (date, amount, description, category)
- **Existing Pattern**: Budget and category forms use this approach successfully
- **No Complex Validation**: Simple required field checks and number validation
- **React 18 Hooks**: useState for form state, useEffect for initialization

**Alternatives Considered**:
- **Formik/React Hook Form**: Rejected - overkill for 4-field form with minimal validation
- **Uncontrolled Components**: Rejected - less predictable state management

---

### 4. Date Handling

**Decision**: Material-UI DatePicker with date-fns adapter

**Rationale**:
- **MUI Integration**: DatePicker component already available in MUI ecosystem
- **Format Standard**: ISO 8601 (YYYY-MM-DD) matches backend ExpenseDTO.expenseDate
- **Default Value**: `getTodayISO()` utility already exists in expenseService.ts
- **Timezone Safety**: Date-only format avoids timezone conversion issues

**Alternatives Considered**:
- **Native HTML `<input type="date">`**: Rejected - inconsistent browser styling, less mobile-friendly
- **Custom date picker**: Rejected - reinventing the wheel

---

### 5. Category Selection

**Decision**: Material-UI Autocomplete component with categoryService API

**Rationale**:
- **Existing API**: `categoryService.getCategories()` already fetches all categories
- **Hierarchy Support**: CategoryDTO includes `parentCategory` and `childCategories`
- **Search Capability**: Autocomplete provides built-in filtering for long category lists
- **Accessibility**: MUI Autocomplete is ARIA-compliant

**Implementation Details**:
```typescript
// Fetch categories on component mount
useEffect(() => {
  categoryService.getCategories().then(setCategories);
}, []);

// Display with hierarchy (e.g., "Groceries > Food")
const getCategoryLabel = (category: CategoryDTO) => {
  return category.parentCategory
    ? `${category.parentCategory.name} > ${category.name}`
    : category.name;
};
```

**Alternatives Considered**:
- **Simple Select dropdown**: Rejected - no search capability for long lists
- **Multi-level nested Select**: Rejected - poor UX for mobile

---

### 6. Backend Integration

**Decision**: Use existing `expenseService.createExpense()` from Feature 002

**Rationale**:
- **API Already Exists**: POST `/api/expenses` endpoint implemented in Feature 002
- **DTO Contract**: `CreateExpenseRequest` interface matches backend expectations
- **Authentication**: Backend reads X-Hass-User header automatically (Feature 003)
- **Budget Association**: Backend handles budget lookup by date range automatically

**API Contract** (existing):
```typescript
interface CreateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string; // YYYY-MM-DD
  budgetId: number;
  categoryId?: number | null;
}
```

**No Backend Changes Required**: All necessary endpoints and business logic already implemented

---

### 7. Budget Selection Strategy

**Decision**: Auto-select budget based on expense date

**Rationale**:
- **User Story Focus**: Specification focuses on category selection, not budget selection
- **Implicit Requirement**: FR-011 states "System MUST associate expense with appropriate budget based on expense date"
- **Existing Logic**: Backend already has budget lookup by date range
- **UX Simplification**: Reduces form fields from 5 to 4 (date, amount, description, category)

**Implementation Approach**:
1. User selects expense date (defaults to today)
2. Frontend calls `budgetService.getBudgets()` filtered by date
3. If exactly one budget matches date range, auto-select it
4. If zero or multiple budgets match, show validation error with guidance
5. Submit expense with auto-selected budgetId

**Edge Cases**:
- **No budget for date**: Show error "No budget found for [date]. Please create a budget first."
- **Multiple budgets for date**: Show error "Multiple budgets found for [date]. Please contact administrator." (data integrity issue)

**Alternatives Considered**:
- **Manual budget dropdown**: Rejected - adds unnecessary user friction, contradicts specification focus
- **Always use current month budget**: Rejected - breaks when user enters past/future dates (User Story 3)

---

### 8. Form Validation

**Decision**: Client-side validation with real-time feedback

**Validation Rules**:
- **Amount**: Required, positive number, max 2 decimal places
- **Description**: Required, 1-500 characters
- **Category**: Required, must exist in category list
- **Date**: Required, valid ISO date format
- **Budget**: Must exist for selected date (auto-validated during budget lookup)

**Implementation**:
```typescript
const validateForm = (): boolean => {
  const errors: Record<string, string> = {};

  if (!amount || amount <= 0) {
    errors.amount = "Amount must be greater than 0";
  }

  if (!description.trim()) {
    errors.description = "Description is required";
  } else if (description.length > 500) {
    errors.description = "Description must be 500 characters or less";
  }

  if (!categoryId) {
    errors.category = "Category is required";
  }

  if (!expenseDate) {
    errors.date = "Date is required";
  }

  setFormErrors(errors);
  return Object.keys(errors).length === 0;
};
```

**Alternatives Considered**:
- **Backend-only validation**: Rejected - poor UX, unnecessary round-trips
- **Third-party validation library**: Rejected - simple rules don't justify dependency

---

### 9. Post-Submission Navigation

**Decision**: Navigate to `/expenses` (expense list page)

**Rationale**:
- **Immediate Feedback**: User sees their newly created expense in the list (SC-004)
- **Consistent Pattern**: Budget creation navigates to `/budgets`, category to `/categories`
- **Enables FR-014**: "System MUST navigate users to appropriate view after successful creation"

**Note**: The `/expenses` list page is **out of scope** for this feature (Feature 007). This feature only creates the expense entry form. The expense list page will be implemented in a future feature.

**Workaround for Feature 007**: Navigate to homepage (`/`) after successful creation, showing success message. The homepage QuickActionsCard already has an "Add Expense" button, creating a logical loop.

**Updated Decision**: Navigate to `/` (homepage) with success toast notification

---

### 10. Error Handling

**Decision**: Material-UI Snackbar for success/error notifications

**Error Scenarios**:
1. **Validation errors**: Inline field errors (red text under input)
2. **No budget found**: Snackbar error with guidance message
3. **Network failure**: Snackbar error "Failed to create expense. Please try again."
4. **Backend 400/500 errors**: Snackbar error with backend error message
5. **Category fetch failure**: Snackbar error, disable category field

**Success Scenario**:
- Snackbar success message: "Expense created successfully!"
- Clear form fields
- Navigate to homepage

**Alternatives Considered**:
- **Alert dialogs**: Rejected - more intrusive, requires dismissal click
- **Inline error messages only**: Rejected - doesn't cover network/backend errors

---

## Technology Stack Summary

| Layer | Technology | Version | Justification |
|-------|-----------|---------|---------------|
| Frontend Framework | Next.js | 14.x | Constitution requirement, existing pattern |
| Language | TypeScript | 5.x | Type safety, existing codebase standard |
| UI Library | Material-UI (MUI) | v5 | Consistency with existing features |
| State Management | React useState | 18.x | Simple form, no global state needed |
| Date Handling | date-fns + MUI DatePicker | Latest | MUI integration, ISO format support |
| HTTP Client | Axios (via api.ts) | Existing | Centralized API configuration |
| Backend API | Spring Boot REST | Existing | Feature 002 expense endpoints |
| Authentication | X-Hass-User header | Existing | Feature 003 dev mode, constitution req |

---

## Dependencies on Existing Features

| Feature | Dependency | What We Use |
|---------|-----------|-------------|
| **Feature 002** | Budget Management API | `POST /api/expenses`, `GET /api/budgets` endpoints |
| **Feature 003** | Dev Mode Headers | X-Hass-User authentication header |
| **Feature 005** | Category Management | `GET /api/categories` endpoint, CategoryDTO interface |
| **Feature 006** | Homepage Update | Navigation target (homepage) for post-submission |

---

## Open Questions / Risks

### Resolved Questions

1. **How to handle budget selection?**
   ✅ **Resolved**: Auto-select budget based on expense date (see Decision #7)

2. **Where to navigate after submission?**
   ✅ **Resolved**: Navigate to homepage with success message (see Decision #9)

3. **Should we support parent category budget tracking?**
   ✅ **Resolved**: Backend already handles this (Feature 004). No frontend changes needed.

### Remaining Risks

1. **No expense list page exists yet** (out of scope for Feature 007)
   - **Mitigation**: Navigate to homepage instead, defer list page to future feature
   - **Impact**: Users can create expenses but can't view them in a dedicated list (only in budget details)

2. **Multiple budgets for same date range** (data integrity issue)
   - **Mitigation**: Show error message, guidance to contact administrator
   - **Impact**: Rare edge case, should not occur in normal use

3. **Category hierarchy display in dropdown** (UX complexity)
   - **Mitigation**: Use Autocomplete with custom label formatting ("Parent > Child")
   - **Impact**: May be cluttered with many categories, but functional

---

## Next Steps

Phase 0 complete. Proceed to **Phase 1**:
1. Create data-model.md (entity relationships)
2. Create contracts/ (API specifications)
3. Create quickstart.md (integration scenarios)
4. Update agent context with technology choices
5. Re-evaluate Constitution Check
