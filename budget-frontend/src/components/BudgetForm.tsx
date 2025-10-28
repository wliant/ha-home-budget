'use client';

import React, { useState } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Paper,
  Alert,
} from '@mui/material';
import { CreateBudgetRequest, getMonthName } from '@/services/budgetService';

/**
 * BudgetForm component for creating and editing budgets.
 *
 * Features:
 * - Year and month selection
 * - Budget amount input with validation
 * - Optional description
 * - Client-side validation
 */

interface BudgetFormProps {
  onSubmit: (data: CreateBudgetRequest) => Promise<void>;
  onCancel?: () => void;
  initialValues?: Partial<CreateBudgetRequest>;
  isEdit?: boolean;
}

export const BudgetForm: React.FC<BudgetFormProps> = ({
  onSubmit,
  onCancel,
  initialValues,
  isEdit = false,
}) => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [formData, setFormData] = useState<CreateBudgetRequest>({
    year: initialValues?.year || currentYear,
    month: initialValues?.month || currentMonth,
    totalAmount: initialValues?.totalAmount || 0,
    description: initialValues?.description || '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>('');

  // Generate year options (current year and next 5 years)
  const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear + i);

  // Generate month options
  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: getMonthName(i + 1),
  }));

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (formData.totalAmount <= 0) {
      newErrors.totalAmount = 'Budget amount must be greater than 0';
    }

    if (formData.totalAmount > 999999.99) {
      newErrors.totalAmount = 'Budget amount must be less than $999,999.99';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (field: keyof CreateBudgetRequest) => (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = event.target.value;
    setFormData((prev) => ({
      ...prev,
      [field]: field === 'totalAmount' ? parseFloat(value) || 0 : value,
    }));

    // Clear field error when user types
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }

    // Clear submit error
    if (submitError) {
      setSubmitError('');
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError('');

    try {
      await onSubmit(formData);
    } catch (error: any) {
      // Handle different error types
      if (error.response?.status === 409) {
        setSubmitError('A budget already exists for this month');
      } else if (error.response?.data?.message) {
        setSubmitError(error.response.data.message);
      } else {
        setSubmitError('Failed to save budget. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Paper elevation={2} sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        {isEdit ? 'Edit Budget' : 'Create New Budget'}
      </Typography>

      {submitError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {submitError}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
          <TextField
            select
            label="Year"
            value={formData.year}
            onChange={handleChange('year')}
            disabled={isEdit}
            fullWidth
            required
          >
            {yearOptions.map((year) => (
              <MenuItem key={year} value={year}>
                {year}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            label="Month"
            value={formData.month}
            onChange={handleChange('month')}
            disabled={isEdit}
            fullWidth
            required
          >
            {monthOptions.map((month) => (
              <MenuItem key={month.value} value={month.value}>
                {month.label}
              </MenuItem>
            ))}
          </TextField>
        </Box>

        <TextField
          label="Budget Amount"
          type="number"
          value={formData.totalAmount || ''}
          onChange={handleChange('totalAmount')}
          error={!!errors.totalAmount}
          helperText={errors.totalAmount || 'Enter your total budget for this month'}
          fullWidth
          required
          inputProps={{
            min: 0,
            step: 0.01,
          }}
          sx={{ mb: 2 }}
        />

        <TextField
          label="Description (Optional)"
          value={formData.description}
          onChange={handleChange('description')}
          fullWidth
          multiline
          rows={3}
          placeholder="Add notes about this budget..."
          sx={{ mb: 3 }}
        />

        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          {onCancel && (
            <Button
              onClick={onCancel}
              disabled={isSubmitting}
              variant="outlined"
            >
              Cancel
            </Button>
          )}
          <Button
            type="submit"
            variant="contained"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Saving...' : isEdit ? 'Update Budget' : 'Create Budget'}
          </Button>
        </Box>
      </Box>
    </Paper>
  );
};

export default BudgetForm;
