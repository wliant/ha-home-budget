'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Snackbar,
  Alert,
  Box,
} from '@mui/material';
import { AddCard as AddCardIcon } from '@mui/icons-material';
import { CategorySelect } from '@/components/expenses/CategorySelect';
import { expenseService, getTodayISO } from '@/services/expenseService';
import { budgetService } from '@/services/budgetService';
import { ExpenseFormState } from '@/types/expense';

/**
 * New expense page - Feature 007: Expense Recording
 *
 * Features:
 * - Standalone expense entry form
 * - Budget auto-selection based on date
 * - Date defaults to today (editable)
 * - Category selection
 * - Client-side validation
 * - Success/error feedback
 */

export default function NewExpensePage() {
  const router = useRouter();

  // Form state (T009)
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

  // Budget auto-selection logic (T010)
  useEffect(() => {
    const selectBudget = async () => {
      if (!formState.expenseDate) {
        return;
      }

      if (!formState.categoryId) {
        setFormState((prev) => ({
          ...prev,
          budgetId: null,
          errorMessage: null,
        }));
        return;
      }

      try {
        const budgets = await budgetService.getAllBudgets();
        const expenseDate = new Date(formState.expenseDate);
        const year = expenseDate.getFullYear();
        const month = expenseDate.getMonth() + 1;

        const categoryBudgets = budgets.filter((b) => b.categoryId === formState.categoryId);
        const monthlyBudget = categoryBudgets.find(
          (b) => b.year === year && b.month === month,
        );
        const yearlyBudget = categoryBudgets.find(
          (b) => b.year === year && !b.month,
        );

        if (monthlyBudget) {
          setFormState((prev) => ({
            ...prev,
            budgetId: monthlyBudget.id!,
            errorMessage: null,
          }));
          return;
        }

        if (yearlyBudget) {
          setFormState((prev) => ({
            ...prev,
            budgetId: yearlyBudget.id!,
            errorMessage: null,
          }));
          return;
        }

        setFormState((prev) => ({
          ...prev,
          budgetId: null,
          errorMessage: `No budget found for ${formState.expenseDate}. Please create a budget first.`,
        }));
      } catch (err) {
        console.error('Failed to fetch budgets:', err);
        setFormState((prev) => ({
          ...prev,
          budgetId: null,
          errorMessage: 'Failed to load budgets. Please try again.',
        }));
      }
    };

    selectBudget();
  }, [formState.expenseDate, formState.categoryId]);

  // Client-side form validation (T011)
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    const amountNum = parseFloat(formState.amount);
    if (!formState.amount || isNaN(amountNum) || amountNum <= 0) {
      errors.amount = 'Amount must be greater than 0';
    }

    if (!formState.description.trim()) {
      errors.description = 'Description is required';
    } else if (formState.description.length > 500) {
      errors.description = 'Description must be 500 characters or less';
    }

    if (!formState.categoryId) {
      errors.category = 'Category is required';
    }

    if (!formState.expenseDate) {
      errors.date = 'Date is required';
    }

    if (!formState.budgetId) {
      errors.budget = 'A budget must exist for the selected date';
    }

    setFormState((prev) => ({ ...prev, errors }));
    return Object.keys(errors).length === 0;
  };

  // Keyboard shortcuts (T031)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Escape to clear/cancel
      if (e.key === 'Escape') {
        if (confirm('Cancel and clear the form?')) {
          router.push('/');
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [router]);

  // Form submission handler (T012)
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setFormState((prev) => ({ ...prev, loading: true }));

    try {
      await expenseService.createExpense({
        amount: parseFloat(formState.amount),
        description: formState.description.trim(),
        expenseDate: formState.expenseDate,
        budgetId: formState.budgetId!,
        categoryId: formState.categoryId,
      });

      // Success feedback (T014, T025)
      setFormState((prev) => ({
        ...prev,
        loading: false,
        successMessage: 'Expense created for Current User!',
        errorMessage: null,
      }));

      // Form reset after successful submission (T032)
      setTimeout(() => {
        setFormState({
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
      }, 1500);

      // Navigate to homepage after 2 seconds
      setTimeout(() => {
        router.push('/');
      }, 2000);
    } catch (err: any) {
      console.error('Failed to create expense:', err);
      // Error handling (T013)
      const errorMsg = err.response?.data?.message || 'Failed to create expense. Please try again.';
      setFormState((prev) => ({
        ...prev,
        loading: false,
        errorMessage: errorMsg,
      }));
    }
  };

  const updateField = (field: keyof ExpenseFormState, value: any) => {
    setFormState((prev) => ({ ...prev, [field]: value }));
  };

  return (
    <Container maxWidth="sm" sx={{ px: { xs: 2, sm: 3 } }}> {/* Responsive padding (T028) */}
      <Paper sx={{ p: { xs: 3, sm: 4 }, mt: { xs: 2, sm: 4 } }}> {/* Responsive spacing (T028) */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <AddCardIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1" gutterBottom>
            Record Expense
          </Typography>
        </Box>

        {/* User context display (T024) */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Recording expense as: Current User
        </Typography>

        {/* Error alert for no budget found (T013) */}
        {formState.errorMessage && !formState.budgetId && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {formState.errorMessage}
          </Alert>
        )}

        {/* Budget selection feedback (T023) */}
        {formState.budgetId && !formState.errorMessage && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Budget auto-selected for {formState.expenseDate}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3 }} aria-label="Expense entry form"> {/* Accessibility (T034) */}
          <TextField
            fullWidth
            type="date"
            label="Date"
            value={formState.expenseDate}
            onChange={(e) => updateField('expenseDate', e.target.value)}
            required
            error={!!formState.errors.date}
            helperText={formState.errors.date}
            margin="normal"
            InputLabelProps={{ shrink: true }}
            inputProps={{
              'aria-label': 'Expense date', // Accessibility (T034)
            }}
          />

          {/* Amount field (T008, T029) */}
          <TextField
            fullWidth
            type="number"
            label="Amount"
            value={formState.amount}
            onChange={(e) => updateField('amount', e.target.value)}
            onBlur={(e) => {
              // Format as currency on blur (T029)
              const num = parseFloat(e.target.value);
              if (!isNaN(num) && num > 0) {
                updateField('amount', num.toFixed(2));
              }
            }}
            required
            error={!!formState.errors.amount}
            helperText={formState.errors.amount}
            margin="normal"
            inputProps={{
              step: '0.01',
              min: '0',
              'aria-label': 'Expense amount', // Accessibility (T034)
            }}
          />

          {/* Description field (T008, T030) */}
          <TextField
            fullWidth
            label="Description"
            value={formState.description}
            onChange={(e) => updateField('description', e.target.value)}
            required
            error={!!formState.errors.description}
            helperText={formState.errors.description || `${formState.description.length}/500 characters`} // Character counter (T030)
            margin="normal"
            inputProps={{
              maxLength: 500,
              'aria-label': 'Expense description', // Accessibility (T034)
            }}
          />

          {/* Category selection (T008) */}
          <Box sx={{ mt: 2 }}>
            <CategorySelect
              value={formState.categoryId}
              onChange={(id) => updateField('categoryId', id)}
              required
              error={formState.errors.category}
            />
          </Box>

          {/* Submit button (T008, T034) */}
          <Button
            type="submit"
            variant="contained"
            fullWidth
            disabled={formState.loading || !formState.budgetId}
            sx={{ mt: 3 }}
            aria-label={formState.loading ? 'Creating expense' : 'Create expense'} // Accessibility (T034)
            aria-busy={formState.loading} // Accessibility (T034)
          >
            {formState.loading ? 'Creating...' : 'Create Expense'}
          </Button>
        </Box>
      </Paper>

      {/* Success message (T014) */}
      <Snackbar
        open={!!formState.successMessage}
        autoHideDuration={2000}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity="success" sx={{ width: '100%' }}>
          {formState.successMessage}
        </Alert>
      </Snackbar>

      {/* Error message (T013) */}
      <Snackbar
        open={!!formState.errorMessage && !!formState.budgetId}
        autoHideDuration={6000}
        onClose={() => updateField('errorMessage', null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity="error" sx={{ width: '100%' }} onClose={() => updateField('errorMessage', null)}>
          {formState.errorMessage}
        </Alert>
      </Snackbar>
    </Container>
  );
}
