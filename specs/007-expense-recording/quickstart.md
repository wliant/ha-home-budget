# Quickstart: Expense Recording Integration

**Feature**: 007-expense-recording
**Date**: 2025-12-22
**Audience**: Developers implementing expense recording form

---

## Overview

This quickstart guide shows how to integrate the expense recording feature into the existing Next.js application. Since all backend APIs already exist from Feature 002, this is a **frontend-only integration**.

---

## Prerequisites

- ✅ Feature 002 (Budget Management) deployed (provides expense creation API)
- ✅ Feature 003 (Dev Mode Headers) deployed (provides X-Hass-User authentication)
- ✅ Feature 005 (Category Management) deployed (provides category selection API)
- ✅ Next.js 14.x frontend running on port 3001
- ✅ Spring Boot backend running on port 8080
- ✅ MySQL database running

---

## Quick Start (5 minutes)

### Step 1: Create the Expense Form Page

Create new directory and page file:

```bash
mkdir -p budget-frontend/src/app/expenses/new
touch budget-frontend/src/app/expenses/new/page.tsx
```

### Step 2: Implement Basic Form (Minimal Example)

**File**: `budget-frontend/src/app/expenses/new/page.tsx`

```typescript
'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Paper,
  TextField,
  Button,
  Autocomplete,
  Snackbar,
  Alert,
} from '@mui/material';
import { expenseService, getTodayISO } from '@/services/expenseService';
import { categoryService } from '@/services/categoryService';
import { budgetService } from '@/services/budgetService';
import { CategoryDTO } from '@/types/category';

export default function NewExpensePage() {
  const router = useRouter();

  // Form state
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [expenseDate, setExpenseDate] = useState(getTodayISO());
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [budgetId, setBudgetId] = useState<number | null>(null);

  // Data state
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // Load categories on mount
  useEffect(() => {
    categoryService.getCategories().then(setCategories);
  }, []);

  // Auto-select budget when date changes
  useEffect(() => {
    budgetService.getBudgets().then((budgets) => {
      const budget = budgets.find(
        (b) => expenseDate >= b.startDate && expenseDate <= b.endDate
      );
      if (budget) {
        setBudgetId(budget.id);
        setError(null);
      } else {
        setBudgetId(null);
        setError(`No budget found for ${expenseDate}`);
      }
    });
  }, [expenseDate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!budgetId) {
      setError('Cannot create expense without a budget');
      return;
    }

    setLoading(true);
    try {
      await expenseService.createExpense({
        amount: parseFloat(amount),
        description: description.trim(),
        expenseDate,
        budgetId,
        categoryId,
      });

      setSuccess(true);
      setTimeout(() => router.push('/'), 2000);
    } catch (err) {
      setError('Failed to create expense. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm">
      <Paper sx={{ p: 4, mt: 4 }}>
        <h1>Record Expense</h1>
        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            type="date"
            label="Date"
            value={expenseDate}
            onChange={(e) => setExpenseDate(e.target.value)}
            margin="normal"
          />

          <TextField
            fullWidth
            type="number"
            label="Amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
            margin="normal"
          />

          <TextField
            fullWidth
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            margin="normal"
          />

          <Autocomplete
            options={categories}
            getOptionLabel={(cat) => cat.name}
            onChange={(_, value) => setCategoryId(value?.id ?? null)}
            renderInput={(params) => (
              <TextField {...params} label="Category" required />
            )}
          />

          <Button
            type="submit"
            variant="contained"
            fullWidth
            disabled={loading || !budgetId}
            sx={{ mt: 2 }}
          >
            {loading ? 'Creating...' : 'Create Expense'}
          </Button>
        </form>
      </Paper>

      <Snackbar open={!!error} autoHideDuration={6000}>
        <Alert severity="error">{error}</Alert>
      </Snackbar>

      <Snackbar open={success} autoHideDuration={2000}>
        <Alert severity="success">Expense created successfully!</Alert>
      </Snackbar>
    </Container>
  );
}
```

### Step 3: Test the Integration

```bash
# Start backend (if not running)
cd budget-backend
./mvnw spring-boot:run

# Start frontend (if not running)
cd budget-frontend
npm run dev

# Navigate to http://localhost:3001/expenses/new
```

**Test Checklist**:
- [ ] Form loads with today's date
- [ ] Category dropdown populates
- [ ] Entering expense and clicking submit creates expense
- [ ] Success message appears
- [ ] Redirected to homepage after 2 seconds

---

## Integration Scenarios

### Scenario 1: Simple Expense Entry (Happy Path)

**User Story**: User Story 1 - Quick Expense Entry

**Steps**:
1. User navigates to `/expenses/new`
2. Form loads with date defaulted to today (e.g., "2025-12-22")
3. Backend auto-selects budget where `2025-12-22 BETWEEN startDate AND endDate`
4. User enters amount: `42.50`
5. User enters description: `Groceries at Whole Foods`
6. User selects category: `Groceries` from dropdown
7. User clicks "Create Expense"
8. Frontend calls `POST /api/expenses` with `{ amount: 42.50, description: "Groceries at Whole Foods", expenseDate: "2025-12-22", budgetId: 10, categoryId: 5 }`
9. Backend creates expense, increments budget totalSpent, returns `ExpenseDTO`
10. Frontend shows success message: "Expense created successfully!"
11. Frontend navigates to `/` after 2 seconds

**Expected Result**: ✅ Expense created, user sees success message, redirected to homepage

---

### Scenario 2: Past Date Expense

**User Story**: User Story 3 - Expense Date Flexibility

**Steps**:
1. User navigates to `/expenses/new`
2. User changes date to `2025-11-15` (last month)
3. Frontend auto-selects November budget (budgetId: 9)
4. User enters amount: `25.00`
5. User enters description: `Forgot to record this last month`
6. User selects category: `Dining Out`
7. User clicks "Create Expense"
8. Backend creates expense with `expenseDate: "2025-11-15"` and `budgetId: 9`
9. Backend increments November budget's totalSpent (not current month)

**Expected Result**: ✅ Expense correctly assigned to November budget, not December

---

### Scenario 3: No Budget for Date (Error Handling)

**User Story**: Edge case - No budget exists for selected date

**Steps**:
1. User navigates to `/expenses/new`
2. User changes date to `2030-01-01` (far future, no budget exists)
3. Frontend fetches budgets, finds no match for date range
4. Frontend sets `budgetId = null`
5. Frontend displays error message: "No budget found for 2030-01-01"
6. Submit button is disabled

**Expected Result**: ❌ User cannot submit expense, clear error guidance provided

**Mitigation**: User must create a budget for 2030-01-01 first, then return to expense form

---

### Scenario 4: Category with Parent Hierarchy

**User Story**: User Story 2 - Category-Based Expense Tracking

**Setup**:
- Parent category: `Food` (id: 1)
- Child category: `Groceries` (id: 5, parentCategoryId: 1)

**Steps**:
1. User navigates to `/expenses/new`
2. Category dropdown shows:
   - "Food" (parent)
   - "Food > Groceries" (child with parent prefix)
   - "Transportation" (parent)
3. User selects "Food > Groceries"
4. User enters amount: `50.00`, description: `Weekly groceries`
5. User clicks "Create Expense"
6. Backend creates expense with `categoryId: 5`
7. Backend increments both:
   - `categoryBudgets[5]` (Groceries) by 50.00
   - `categoryBudgets[1]` (Food parent) by 50.00

**Expected Result**: ✅ Expense counted against both child and parent category budgets

---

### Scenario 5: Multi-User Attribution

**User Story**: User Story 4 - Multi-User Attribution

**Setup**:
- User "alice" is authenticated (X-Hass-User: alice)
- User "bob" is authenticated (X-Hass-User: bob)

**Steps**:
1. Alice navigates to `/expenses/new`
2. Alice creates expense: $30, "Lunch", category: Dining Out
3. Backend reads `X-Hass-User: alice`, sets `createdBy: "alice"`
4. Bob navigates to `/expenses/new`
5. Bob creates expense: $15, "Coffee", category: Dining Out
6. Backend reads `X-Hass-User: bob`, sets `createdBy: "bob"`
7. Expense list shows:
   - $30 "Lunch" - Created by alice
   - $15 "Coffee" - Created by bob

**Expected Result**: ✅ Each expense correctly attributed to creator

---

### Scenario 6: Backend Validation Error

**User Story**: Edge case - Invalid category ID

**Steps**:
1. User navigates to `/expenses/new`
2. User enters amount: `100.00`, description: `Test`
3. User selects category from dropdown (categoryId: 5)
4. **Meanwhile**, another user deletes category 5 via different browser
5. User clicks "Create Expense"
6. Backend validates categoryId, finds category 5 no longer exists
7. Backend returns `404 Not Found` with message: "Category with ID 5 does not exist"
8. Frontend displays error: "Category not found. Please refresh and try again."

**Expected Result**: ❌ Expense not created, user informed of stale data

**Mitigation**: Frontend could re-fetch categories on error, or show "Refresh" button

---

## Component Architecture

### Page Component (`/app/expenses/new/page.tsx`)

**Responsibilities**:
- Route handling (`/expenses/new`)
- Layout (Container, Paper)
- Form submission logic
- Navigation after success
- Global loading/error states

**Dependencies**:
- ExpenseForm component (delegated)
- expenseService, categoryService, budgetService

---

### ExpenseForm Component (`/components/expenses/ExpenseForm.tsx`)

**Responsibilities**:
- Form state management (useState)
- Input validation
- API calls (createExpense)
- Success/error feedback

**Props**:
```typescript
interface ExpenseFormProps {
  onSuccess?: (expense: ExpenseDTO) => void;
  onCancel?: () => void;
}
```

**Children Components**:
- CategorySelect (Autocomplete dropdown)
- DatePicker (MUI DatePicker)
- AmountInput (TextField with number validation)
- DescriptionInput (TextField with character count)

---

### CategorySelect Component (`/components/expenses/CategorySelect.tsx`)

**Responsibilities**:
- Fetch categories on mount
- Display hierarchy ("Parent > Child")
- Handle selection
- Search/filter (built into Autocomplete)

**Props**:
```typescript
interface CategorySelectProps {
  value: number | null;
  onChange: (categoryId: number | null) => void;
  required?: boolean;
  error?: string;
}
```

**Implementation**:
```typescript
export function CategorySelect({ value, onChange, required, error }: CategorySelectProps) {
  const [categories, setCategories] = useState<CategoryDTO[]>([]);

  useEffect(() => {
    categoryService.getCategories().then(setCategories);
  }, []);

  const getCategoryLabel = (category: CategoryDTO) => {
    return category.parentCategory
      ? `${category.parentCategory.name} > ${category.name}`
      : category.name;
  };

  return (
    <Autocomplete
      options={categories}
      getOptionLabel={getCategoryLabel}
      value={categories.find((c) => c.id === value) || null}
      onChange={(_, newValue) => onChange(newValue?.id ?? null)}
      renderInput={(params) => (
        <TextField
          {...params}
          label="Category"
          required={required}
          error={!!error}
          helperText={error}
        />
      )}
    />
  );
}
```

---

### DatePicker Component (MUI Built-in)

**Usage**:
```typescript
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';

<LocalizationProvider dateAdapter={AdapterDateFns}>
  <DatePicker
    label="Expense Date"
    value={new Date(expenseDate)}
    onChange={(date) => setExpenseDate(date?.toISOString().split('T')[0] || getTodayISO())}
    slotProps={{
      textField: { fullWidth: true, required: true },
    }}
  />
</LocalizationProvider>
```

**Dependencies**: Add to `package.json`:
```json
{
  "@mui/x-date-pickers": "^6.0.0",
  "date-fns": "^2.30.0"
}
```

---

## State Management

### Local Component State (Recommended)

**Why**: Simple form with no global state needs

**Implementation**:
```typescript
const [formState, setFormState] = useState<ExpenseFormState>({
  amount: '',
  description: '',
  expenseDate: getTodayISO(),
  categoryId: null,
  budgetId: null,
  errors: {},
  loading: false,
  successMessage: null,
  errorMessage: null,
});

// Update single field
const updateField = (field: keyof ExpenseFormState, value: any) => {
  setFormState((prev) => ({ ...prev, [field]: value }));
};
```

**Alternatives Considered**:
- ❌ Redux: Overkill for single-page form
- ❌ Context API: Not shared across multiple pages
- ❌ React Hook Form: Simple validation doesn't justify dependency

---

## Error Handling Strategy

### Client-Side Validation Errors

**Display**: Inline under input field (TextField `error` and `helperText` props)

**Example**:
```typescript
<TextField
  label="Amount"
  value={amount}
  error={!!errors.amount}
  helperText={errors.amount}
  onChange={handleAmountChange}
/>
```

**Validation Rules**:
- Amount: `required`, `> 0`, `max 2 decimals`
- Description: `required`, `1-500 chars`
- Category: `required`
- Date: `required`, `valid date`

---

### Backend Validation Errors (400)

**Display**: Snackbar notification with backend error message

**Example**:
```typescript
try {
  await expenseService.createExpense(request);
} catch (err: any) {
  if (err.response?.status === 400) {
    const errors = err.response.data.errors || [];
    const message = errors.map((e: any) => e.message).join(', ');
    setError(message);
  }
}
```

---

### Network Errors (500, timeout)

**Display**: Snackbar with generic message

**Example**:
```typescript
setError('Failed to create expense. Please try again.');
```

---

### No Budget Found

**Display**: Alert box above form + disabled submit button

**Example**:
```typescript
{!budgetId && expenseDate && (
  <Alert severity="error" sx={{ mb: 2 }}>
    No budget found for {expenseDate}. Please create a budget first.
  </Alert>
)}

<Button disabled={!budgetId || loading}>Create Expense</Button>
```

---

## Testing Guide

### Unit Tests (Optional)

**File**: `budget-frontend/src/components/expenses/ExpenseForm.test.tsx`

```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ExpenseForm } from './ExpenseForm';
import { expenseService } from '@/services/expenseService';

jest.mock('@/services/expenseService');

test('submits form with valid data', async () => {
  const mockCreate = jest.spyOn(expenseService, 'createExpense').mockResolvedValue({
    id: 123,
    amount: 42.50,
    description: 'Test',
    expenseDate: '2025-12-22',
    budgetId: 10,
    categoryId: 5,
  });

  render(<ExpenseForm />);

  fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '42.50' } });
  fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Test' } });
  fireEvent.click(screen.getByText('Create Expense'));

  await waitFor(() => {
    expect(mockCreate).toHaveBeenCalledWith({
      amount: 42.50,
      description: 'Test',
      expenseDate: expect.any(String),
      budgetId: expect.any(Number),
      categoryId: expect.any(Number),
    });
  });
});
```

---

### Integration Tests (Recommended)

**File**: `budget-frontend/src/app/expenses/new/page.test.tsx`

```typescript
import { render, screen } from '@testing-library/react';
import NewExpensePage from './page';

test('renders expense form', () => {
  render(<NewExpensePage />);
  expect(screen.getByText('Record Expense')).toBeInTheDocument();
  expect(screen.getByLabelText('Amount')).toBeInTheDocument();
  expect(screen.getByLabelText('Description')).toBeInTheDocument();
  expect(screen.getByLabelText('Category')).toBeInTheDocument();
});
```

---

### Manual Testing Checklist

- [ ] Form loads with today's date
- [ ] Amount accepts decimals (42.50)
- [ ] Amount rejects negative numbers
- [ ] Amount rejects non-numeric input
- [ ] Description accepts 1-500 characters
- [ ] Description rejects empty input
- [ ] Category dropdown populates with all categories
- [ ] Category shows parent hierarchy ("Food > Groceries")
- [ ] Date picker allows past date selection
- [ ] Date picker allows future date selection
- [ ] Budget auto-selects when date changes
- [ ] Error shown when no budget for date
- [ ] Submit button disabled when no budget
- [ ] Submit button disabled during submission (loading state)
- [ ] Success message shown after successful submission
- [ ] Navigates to homepage after success
- [ ] Backend error shown in Snackbar (test by stopping backend)
- [ ] Form clears after successful submission
- [ ] Works on mobile viewport (responsive design)

---

## Deployment Checklist

### Frontend

- [ ] Install date picker dependencies: `npm install @mui/x-date-pickers date-fns`
- [ ] Create `/app/expenses/new/page.tsx`
- [ ] Create `/components/expenses/ExpenseForm.tsx`
- [ ] Create `/components/expenses/CategorySelect.tsx`
- [ ] Verify `expenseService.ts` has `createExpense()` method
- [ ] Verify `categoryService.ts` has `getCategories()` method
- [ ] Verify `budgetService.ts` has `getBudgets()` method
- [ ] Add navigation link to homepage (already exists from Feature 006)
- [ ] Build frontend: `npm run build`
- [ ] Start frontend: `npm run start`

### Backend

- [ ] No backend changes required
- [ ] Verify `POST /api/expenses` endpoint works (Feature 002)
- [ ] Verify `GET /api/categories` endpoint works (Feature 005)
- [ ] Verify `GET /api/budgets` endpoint works (Feature 002)
- [ ] Verify X-Hass-User header authentication works (Feature 003)

### Integration Testing

- [ ] Create budget for current month (if not exists)
- [ ] Create at least one category (if not exists)
- [ ] Test expense creation via UI
- [ ] Verify expense appears in budget details
- [ ] Verify budget totalSpent increments
- [ ] Test with multiple users (different X-Hass-User headers)

---

## Troubleshooting

### Issue: "No budget found for [date]"

**Cause**: No budget exists with `startDate <= expenseDate <= endDate`

**Solution**: Create a budget covering the expense date via `/budgets/new`

---

### Issue: Submit button always disabled

**Cause**: Budget auto-selection failing

**Debug**:
1. Open browser console
2. Check network tab for `GET /api/budgets` response
3. Verify budgets array has entries
4. Verify date range logic: `expenseDate >= startDate && expenseDate <= endDate`

**Solution**: Check date format (YYYY-MM-DD), verify budget date ranges

---

### Issue: Category dropdown empty

**Cause**: `GET /api/categories` request failing

**Debug**:
1. Open browser console network tab
2. Check for failed request or empty response
3. Verify backend is running and accessible

**Solution**: Create categories via `/categories` page, restart backend if needed

---

### Issue: "Authentication required" error

**Cause**: X-Hass-User header missing

**Debug**:
1. Open browser console network tab
2. Inspect `POST /api/expenses` request headers
3. Verify `X-Hass-User` header present

**Solution**:
- Production: Verify nginx proxy configuration adds header
- Development: Verify Feature 003 dev mode default header is configured

---

## Next Steps

1. Implement expense form UI components
2. Add validation and error handling
3. Test integration with existing features
4. Deploy to production

For task breakdown and implementation order, run:
```bash
/speckit.tasks
```
