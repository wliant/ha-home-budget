'use client';

import React, { useState } from 'react';
import {
  Box,
  Button,
  TextField,
  Paper,
  Typography,
  Alert,
} from '@mui/material';
import { CreateExpenseRequest, UpdateExpenseRequest, getTodayISO } from '@/services/expenseService';

/**
 * ExpenseForm component for creating/editing expenses
 *
 * Features:
 * - Amount input with validation (positive numbers only)
 * - Description text field
 * - Date picker (defaults to today)
 * - Optional category selection (to be added in US3)
 * - Budget ID (passed as prop or hidden field)
 * - Form validation
 * - Error handling
 */

interface ExpenseFormProps {
  onSubmit: (data: CreateExpenseRequest | UpdateExpenseRequest) => Promise<void>;
  onCancel: () => void;
  budgetId: number;
  initialValues?: {
    amount?: number;
    description?: string;
    expenseDate?: string;
    categoryId?: number | null;
  };
  isEdit?: boolean;
}

export const ExpenseForm: React.FC<ExpenseFormProps> = ({
  onSubmit,
  onCancel,
  budgetId,
  initialValues,
  isEdit = false,
}) => {
  const [formData, setFormData] = useState({
    amount: initialValues?.amount?.toString() || '',
    description: initialValues?.description || '',
    expenseDate: initialValues?.expenseDate || getTodayISO(),
    categoryId: initialValues?.categoryId || null,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>('');

  const handleChange = (field: string) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [field]: event.target.value,
    });

    // Clear field error when user types
    if (errors[field]) {
      setErrors({
        ...errors,
        [field]: '',
      });
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Validate amount
    const amount = parseFloat(formData.amount);
    if (!formData.amount || isNaN(amount)) {
      newErrors.amount = 'Amount is required';
    } else if (amount <= 0) {
      newErrors.amount = 'Amount must be greater than 0';
    } else if (amount > 999999.99) {
      newErrors.amount = 'Amount must be less than $999,999.99';
    }

    // Validate description
    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    } else if (formData.description.length > 255) {
      newErrors.description = 'Description must be less than 255 characters';
    }

    // Validate expense date
    if (!formData.expenseDate) {
      newErrors.expenseDate = 'Expense date is required';
    } else {
      const expenseDate = new Date(formData.expenseDate);
      if (isNaN(expenseDate.getTime())) {
        newErrors.expenseDate = 'Invalid date';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitError('');

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const requestData = {
        amount: parseFloat(formData.amount),
        description: formData.description.trim(),
        expenseDate: formData.expenseDate,
        budgetId: budgetId,
        categoryId: formData.categoryId,
      };

      await onSubmit(requestData);
    } catch (error: any) {
      console.error('Form submission error:', error);
      setSubmitError(
        error.response?.data?.message ||
        error.message ||
        'Failed to save expense. Please try again.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        {isEdit ? 'Edit Expense' : 'Add New Expense'}
      </Typography>

      {submitError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {submitError}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit} noValidate>
        {/* Amount */}
        <TextField
          label="Amount"
          type="number"
          fullWidth
          required
          value={formData.amount}
          onChange={handleChange('amount')}
          error={!!errors.amount}
          helperText={errors.amount}
          inputProps={{
            step: '0.01',
            min: '0',
          }}
          sx={{ mb: 2 }}
        />

        {/* Description */}
        <TextField
          label="Description"
          fullWidth
          required
          value={formData.description}
          onChange={handleChange('description')}
          error={!!errors.description}
          helperText={errors.description}
          placeholder="e.g., Groceries at Whole Foods"
          sx={{ mb: 2 }}
        />

        {/* Expense Date */}
        <TextField
          label="Expense Date"
          type="date"
          fullWidth
          required
          value={formData.expenseDate}
          onChange={handleChange('expenseDate')}
          error={!!errors.expenseDate}
          helperText={errors.expenseDate}
          InputLabelProps={{
            shrink: true,
          }}
          sx={{ mb: 3 }}
        />

        {/* Category - placeholder for US3 */}
        {/* Will add category selector in User Story 3 */}

        {/* Form Actions */}
        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          <Button
            variant="outlined"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Saving...' : (isEdit ? 'Update' : 'Add Expense')}
          </Button>
        </Box>
      </Box>
    </Paper>
  );
};

export default ExpenseForm;
