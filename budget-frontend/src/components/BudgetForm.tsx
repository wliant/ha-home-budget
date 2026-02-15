'use client';

import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  TextField,
  MenuItem,
  Typography,
  Paper,
  Alert,
  FormControl,
  InputLabel,
  Select,
  FormControlLabel,
  Checkbox,
  FormHelperText,
  Divider,
  ListSubheader,
} from '@mui/material';
import { CreateBudgetRequest, getMonthName, budgetService, BudgetValidationDTO } from '@/services/budgetService';
import { categoryService, CategoryDTO } from '@/services/categoryService';

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
    month: initialValues?.month ?? null,
    totalAmount: initialValues?.totalAmount || 0,
    description: initialValues?.description || '',
    categoryId: initialValues?.categoryId || 0,
    autoCreateChildren: false,
    createParentBudget: false,
    extendParentBudget: false,
    parentTotalAmount: undefined,
    createParentCategoryBudget: false,
    parentCategoryBudgetAmount: undefined,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string>('');
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [validation, setValidation] = useState<BudgetValidationDTO | null>(null);
  const [parentAmountTouched, setParentAmountTouched] = useState(false);
  const [parentCatAmountTouched, setParentCatAmountTouched] = useState(false);

  // Load categories on mount
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await categoryService.getCategoryHierarchy();
        setCategories(data);
      } catch (error) {
        console.error('Failed to load categories:', error);
        setSubmitError('Failed to load categories. Please refresh the page.');
      } finally {
        setIsLoadingCategories(false);
      }
    };
    loadCategories();
  }, []);

  // Generate year options (5 years back and 5 years forward)
  const yearOptions = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i);

  // Generate month options
  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: getMonthName(i + 1),
  }));

  const buildGroupedCategoryItems = (nodes: CategoryDTO[]): React.ReactNode[] => {
    const items: React.ReactNode[] = [];
    nodes.forEach((node) => {
      const children = node.children || node.childCategories || [];
      if (children.length > 0) {
        items.push(
          <ListSubheader key={`header-${node.id}`} sx={{ lineHeight: '36px', backgroundColor: 'background.paper' }}>
            {node.icon && `${node.icon} `}{node.name}
          </ListSubheader>
        );
        items.push(
          <MenuItem key={node.id} value={node.id} sx={{ pl: 3 }}>
            {node.icon && `${node.icon} `}{node.name} (All)
          </MenuItem>
        );
        children.forEach((child) => {
          items.push(
            <MenuItem key={child.id} value={child.id} sx={{ pl: 4 }}>
              {child.icon && `${child.icon} `}{child.name}
            </MenuItem>
          );
        });
      } else {
        items.push(
          <MenuItem key={node.id} value={node.id}>
            {node.icon && `${node.icon} `}{node.name}
          </MenuItem>
        );
      }
    });
    return items;
  };

  const hasCategories = categories.length > 0;

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.categoryId || formData.categoryId === 0) {
      newErrors.categoryId = 'Please select a category';
    }

    if (formData.totalAmount <= 0) {
      newErrors.totalAmount = 'Budget amount must be greater than 0';
    }

    if (formData.totalAmount > 999999.99) {
      newErrors.totalAmount = 'Budget amount must be less than $999,999.99';
    }

    if (formData.month && validation && !validation.parentBudgetExists && !formData.createParentBudget) {
      newErrors.parentBudget = 'Parent yearly budget is required for monthly budgets';
    }

    if (formData.month && shouldShowParentExtend && !formData.extendParentBudget) {
      newErrors.parentBudget = 'Monthly budgets exceed parent yearly budget. Extend parent budget or reduce amount.';
    }

    if ((formData.createParentBudget || formData.extendParentBudget) && (!formData.parentTotalAmount || formData.parentTotalAmount <= 0)) {
      newErrors.parentTotalAmount = 'Parent budget amount must be greater than 0';
    }

    if (formData.createParentCategoryBudget && (!formData.parentCategoryBudgetAmount || formData.parentCategoryBudgetAmount <= 0)) {
      newErrors.parentCategoryBudgetAmount = 'Parent category budget amount must be greater than 0';
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
      } else if (error.response?.data?.errors) {
        setErrors(error.response.data.errors);
        setSubmitError(error.response.data.message || 'Validation failed');
      } else if (error.response?.data?.message) {
        setSubmitError(error.response.data.message);
      } else {
        setSubmitError('Failed to save budget. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    const shouldValidate = formData.categoryId && formData.year;
    if (!shouldValidate) {
      setValidation(null);
      return;
    }

    const loadValidation = async () => {
      try {
        const response = await budgetService.getBudgetValidation(
          formData.categoryId,
          formData.year,
          formData.month ?? null
        );
        setValidation(response);
      } catch (error) {
        console.error('Failed to validate budget:', error);
      } finally {
        // no-op
      }
    };

    loadValidation();
  }, [formData.categoryId, formData.year, formData.month]);

  const shouldShowParentCreate = !!formData.month && validation && !validation.parentBudgetExists;
  const shouldShowParentExtend = !!formData.month
    && validation
    && validation.parentBudgetExists
    && formData.totalAmount > 0
    && validation.parentBudgetAmount !== undefined
    && formData.totalAmount + validation.monthlyBudgetSum > validation.parentBudgetAmount;

  // Parent category budget: show checkbox when child category selected and no parent category budget exists
  const selectedCategoryIsChild = validation?.parentCategoryName !== undefined && validation?.parentCategoryName !== null;
  const shouldShowParentCatCreate = selectedCategoryIsChild && validation && !validation.parentCategoryBudgetExists;
  const parentCatBudgetWillAutoIncrement = selectedCategoryIsChild && validation && validation.parentCategoryBudgetExists;

  // Default the parent category budget checkbox to checked when it first appears
  useEffect(() => {
    if (shouldShowParentCatCreate && formData.createParentCategoryBudget === false) {
      setFormData((prev) => ({
        ...prev,
        createParentCategoryBudget: true,
        parentCategoryBudgetAmount: prev.totalAmount || 0,
      }));
      setParentCatAmountTouched(false);
    }
  }, [shouldShowParentCatCreate]);

  useEffect(() => {
    if (shouldShowParentCatCreate && formData.createParentCategoryBudget && !parentCatAmountTouched) {
      setFormData((prev) => ({ ...prev, parentCategoryBudgetAmount: prev.totalAmount || 0 }));
    }
  }, [shouldShowParentCatCreate, formData.createParentCategoryBudget, formData.totalAmount, parentCatAmountTouched]);

  useEffect(() => {
    if (shouldShowParentCreate && !formData.parentTotalAmount) {
      if (!parentAmountTouched) {
        setFormData((prev) => ({ ...prev, parentTotalAmount: prev.totalAmount || 0 }));
      }
    }
  }, [shouldShowParentCreate, formData.totalAmount, formData.parentTotalAmount, parentAmountTouched]);

  useEffect(() => {
    if (shouldShowParentExtend && validation?.parentBudgetAmount !== undefined && !formData.parentTotalAmount) {
      if (!parentAmountTouched) {
        setFormData((prev) => ({ ...prev, parentTotalAmount: validation.parentBudgetAmount }));
      }
    }
  }, [shouldShowParentExtend, validation, formData.parentTotalAmount, parentAmountTouched]);

  useEffect(() => {
    if (formData.createParentBudget && !parentAmountTouched) {
      setFormData((prev) => ({ ...prev, parentTotalAmount: prev.totalAmount || 0 }));
    }
  }, [formData.createParentBudget, formData.totalAmount, parentAmountTouched]);

  useEffect(() => {
    if (formData.extendParentBudget && validation?.parentBudgetAmount !== undefined && !parentAmountTouched) {
      setFormData((prev) => ({ ...prev, parentTotalAmount: validation.parentBudgetAmount }));
    }
  }, [formData.extendParentBudget, validation, parentAmountTouched]);

  return (
    <Paper elevation={2} sx={{ p: 3, pt: 0, overflow: 'hidden' }}>
      <Box sx={{ bgcolor: 'rgba(192,120,80,0.08)', px: 3, py: 2, borderRadius: '8px 8px 0 0', mb: 2, mx: -3 }}>
        <Typography variant="h5">
          {isEdit ? 'Edit Budget' : 'Create New Budget'}
        </Typography>
      </Box>

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
            label="Month (Optional)"
            value={formData.month ?? ''}
            onChange={(event) => {
              const value = event.target.value;
              setFormData((prev) => ({
                ...prev,
                month: value === '' ? null : Number(value),
                autoCreateChildren: false,
                createParentBudget: false,
                extendParentBudget: false,
                parentTotalAmount: undefined,
                createParentCategoryBudget: false,
                parentCategoryBudgetAmount: undefined,
              }));
              setParentAmountTouched(false);
            }}
            disabled={isEdit}
            fullWidth
          >
            <MenuItem value="">
              <em>Yearly (no month)</em>
            </MenuItem>
            {monthOptions.map((month) => (
              <MenuItem key={month.value} value={month.value}>
                {month.label}
              </MenuItem>
            ))}
          </TextField>
        </Box>

        <FormControl fullWidth sx={{ mb: 2 }} error={!!errors.categoryId} required>
          <InputLabel>Category</InputLabel>
          <Select
            value={formData.categoryId || ''}
            onChange={(e) => {
              setFormData({ ...formData, categoryId: Number(e.target.value), createParentCategoryBudget: false, parentCategoryBudgetAmount: undefined });
              if (errors.categoryId) {
                setErrors((prev) => {
                  const newErrors = { ...prev };
                  delete newErrors.categoryId;
                  return newErrors;
                });
              }
            }}
            label="Category"
            disabled={isEdit || isLoadingCategories}
          >
            {!hasCategories ? (
              <MenuItem value="" disabled>
                <em>No categories available. Please create a category first.</em>
              </MenuItem>
            ) : (
              buildGroupedCategoryItems(categories)
            )}
          </Select>
          {errors.categoryId && (
            <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 1.5 }}>
              {errors.categoryId}
            </Typography>
          )}
        </FormControl>

        <TextField
          label="Budget Amount"
          type="number"
          value={formData.totalAmount || ''}
          onChange={handleChange('totalAmount')}
          error={!!errors.totalAmount}
          helperText={errors.totalAmount || (formData.month ? 'Enter your total budget for this month' : 'Enter your total budget for this year')}
          fullWidth
          required
          inputProps={{
            min: 0,
            step: 0.01,
          }}
          sx={{ mb: 2 }}
        />

        {validation?.duplicate && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            {validation.duplicateMessage || 'A budget already exists for this period.'}
          </Alert>
        )}

        {formData.month ? (
          <>
            <Divider sx={{ mb: 2 }} />
            {shouldShowParentCreate && (
              <Box sx={{ mb: 2 }}>
                <FormControlLabel
                  control={(
                    <Checkbox
                      checked={!!formData.createParentBudget}
                      onChange={(e) => {
                        setFormData((prev) => ({
                          ...prev,
                          createParentBudget: e.target.checked,
                          parentTotalAmount: e.target.checked ? (prev.parentTotalAmount ?? (prev.totalAmount || 0)) : undefined,
                        }));
                        setParentAmountTouched(false);
                      }}
                    />
                  )}
                  label="Create a yearly parent budget for this category"
                />
                {formData.createParentBudget && (
                  <TextField
                    label="Parent Yearly Budget Amount"
                    type="number"
                    value={formData.parentTotalAmount ?? ''}
                    onChange={(e) => {
                      setFormData((prev) => ({
                        ...prev,
                        parentTotalAmount: parseFloat(e.target.value) || 0,
                      }));
                      setParentAmountTouched(true);
                    }}
                    error={!!errors.parentTotalAmount}
                    helperText={errors.parentTotalAmount || 'Defaults to the monthly budget amount'}
                    fullWidth
                    inputProps={{ min: 0, step: 0.01 }}
                    sx={{ mt: 1 }}
                  />
                )}
              </Box>
            )}

            {shouldShowParentExtend && (
              <Box sx={{ mb: 2 }}>
                <FormControlLabel
                  control={(
                    <Checkbox
                      checked={!!formData.extendParentBudget}
                      onChange={(e) => {
                        setFormData((prev) => ({
                          ...prev,
                          extendParentBudget: e.target.checked,
                          parentTotalAmount: e.target.checked ? (prev.parentTotalAmount ?? validation.parentBudgetAmount) : undefined,
                        }));
                        setParentAmountTouched(false);
                      }}
                    />
                  )}
                  label="Extend the yearly parent budget"
                />
                {formData.extendParentBudget && (
                  <TextField
                    label="Updated Parent Yearly Budget Amount"
                    type="number"
                    value={formData.parentTotalAmount ?? ''}
                    onChange={(e) => {
                      setFormData((prev) => ({
                        ...prev,
                        parentTotalAmount: parseFloat(e.target.value) || 0,
                      }));
                      setParentAmountTouched(true);
                    }}
                    error={!!errors.parentTotalAmount}
                    helperText={errors.parentTotalAmount || 'Set a new yearly budget total'}
                    fullWidth
                    inputProps={{ min: 0, step: 0.01 }}
                    sx={{ mt: 1 }}
                  />
                )}
              </Box>
            )}

            {shouldShowParentCatCreate && (
              <Box sx={{ mb: 2 }}>
                <FormControlLabel
                  control={(
                    <Checkbox
                      checked={formData.createParentCategoryBudget ?? true}
                      onChange={(e) => {
                        setFormData((prev) => ({
                          ...prev,
                          createParentCategoryBudget: e.target.checked,
                          parentCategoryBudgetAmount: e.target.checked ? (prev.parentCategoryBudgetAmount ?? (prev.totalAmount || 0)) : undefined,
                        }));
                        setParentCatAmountTouched(false);
                      }}
                    />
                  )}
                  label={`Also create budget for parent category '${validation?.parentCategoryName}'`}
                />
                {formData.createParentCategoryBudget && (
                  <TextField
                    label={`Parent Category '${validation?.parentCategoryName}' Budget Amount`}
                    type="number"
                    value={formData.parentCategoryBudgetAmount ?? ''}
                    onChange={(e) => {
                      setFormData((prev) => ({
                        ...prev,
                        parentCategoryBudgetAmount: parseFloat(e.target.value) || 0,
                      }));
                      setParentCatAmountTouched(true);
                    }}
                    error={!!errors.parentCategoryBudgetAmount}
                    helperText={errors.parentCategoryBudgetAmount || 'Budget amount for the parent category'}
                    fullWidth
                    inputProps={{ min: 0, step: 0.01 }}
                    sx={{ mt: 1 }}
                  />
                )}
              </Box>
            )}

            {parentCatBudgetWillAutoIncrement && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Budget for &apos;{validation?.parentCategoryName}&apos; will be automatically increased by the child budget amount.
              </Alert>
            )}

            {errors.parentBudget && (
              <FormHelperText error sx={{ mb: 2 }}>
                {errors.parentBudget}
              </FormHelperText>
            )}
          </>
        ) : (
          <Box sx={{ mb: 2 }}>
            <FormControlLabel
              control={(
                <Checkbox
                  checked={!!formData.autoCreateChildren}
                  onChange={(e) => setFormData((prev) => ({
                    ...prev,
                    autoCreateChildren: e.target.checked,
                  }))}
                />
              )}
              label="Automatically create monthly budgets for all 12 months"
            />
            <FormHelperText>
              Monthly budgets will be created with evenly distributed amounts.
            </FormHelperText>
          </Box>
        )}

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
