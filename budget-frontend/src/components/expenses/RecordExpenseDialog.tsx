'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  TextField,
  Alert,
  Box,
  ButtonGroup,
  Tooltip,
  Chip,
  FormControlLabel,
  Checkbox,
  IconButton,
} from '@mui/material';
import { AddCard as AddCardIcon, Add, Remove, AttachFile, CameraAlt, Close } from '@mui/icons-material';
import { CategorySelect } from '@/components/expenses/CategorySelect';
import { expenseService, getTodayISO } from '@/services/expenseService';
import { ExpenseFormState } from '@/types/expense';

interface RecordExpenseDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function RecordExpenseDialog({ open, onClose, onSuccess }: RecordExpenseDialogProps) {
  const [formState, setFormState] = useState<ExpenseFormState>({
    amount: '',
    description: '',
    expenseDate: getTodayISO(),
    categoryId: null,
    commonExpense: false,
    files: [],
    errors: {},
    loading: false,
    successMessage: null,
    errorMessage: null,
  });

  const maxFiles = 5;
  const maxFileSize = 5 * 1024 * 1024;

  useEffect(() => {
    if (open) {
      setFormState({
        amount: '',
        description: '',
        expenseDate: getTodayISO(),
        categoryId: null,
        commonExpense: false,
        files: [],
        errors: {},
        loading: false,
        successMessage: null,
        errorMessage: null,
      });
    }
  }, [open]);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};
    const amountNum = parseFloat(formState.amount);
    if (!formState.amount || isNaN(amountNum) || amountNum === 0) {
      errors.amount = 'Amount must not be zero';
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
    if (formState.files.length > maxFiles) {
      errors.files = 'Maximum 5 files allowed';
    }
    setFormState((prev) => ({ ...prev, errors }));
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    setFormState((prev) => ({ ...prev, loading: true }));
    try {
      await expenseService.createExpense(
        {
          amount: parseFloat(formState.amount),
          description: formState.description.trim(),
          expenseDate: formState.expenseDate,
          categoryId: formState.categoryId!,
          commonExpense: formState.commonExpense,
        },
        formState.files
      );
      setFormState((prev) => ({
        ...prev,
        loading: false,
        successMessage: 'Expense recorded successfully!',
        errorMessage: null,
      }));
      setTimeout(() => {
        onSuccess?.();
        onClose();
      }, 1000);
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to create expense. Please try again.';
      setFormState((prev) => ({ ...prev, loading: false, errorMessage: errorMsg }));
    }
  };

  const updateField = (field: keyof ExpenseFormState, value: any) => {
    setFormState((prev) => ({ ...prev, [field]: value }));
  };

  const addFiles = (selected: File[]) => {
    const combined = [...formState.files, ...selected];
    const errors: Record<string, string> = { ...formState.errors };
    if (combined.length > maxFiles) {
      errors.files = `Maximum ${maxFiles} files allowed`;
    } else {
      delete errors.files;
    }
    const invalidType = combined.find((file) => !(file.type === 'application/pdf' || file.type.startsWith('image/')));
    if (invalidType) errors.files = 'Only PDF and image files are allowed';
    const tooLarge = combined.find((file) => file.size > maxFileSize);
    if (tooLarge) errors.files = 'Each file must be 5MB or less';
    setFormState((prev) => ({ ...prev, files: combined.slice(0, maxFiles), errors }));
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files || []);
    if (selected.length === 0) return;
    addFiles(selected);
    event.target.value = '';
  };

  const removeFile = (index: number) => {
    setFormState((prev) => ({
      ...prev,
      files: prev.files.filter((_, i) => i !== index),
      errors: { ...prev.errors, files: '' },
    }));
  };

  const parseLocalDate = (value: string) => {
    const [year, month, day] = value.split('-').map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
  };

  const formatLocalDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const shiftExpenseDate = (deltaDays: number) => {
    const base = formState.expenseDate || getTodayISO();
    const parsed = parseLocalDate(base);
    if (!parsed) {
      updateField('expenseDate', getTodayISO());
      return;
    }
    parsed.setDate(parsed.getDate() + deltaDays);
    updateField('expenseDate', formatLocalDate(parsed));
  };

  const handleClose = () => {
    if (!formState.loading) onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AddCardIcon sx={{ color: 'primary.main' }} />
          <Typography variant="h6">Record Expense</Typography>
        </Box>
        <IconButton size="small" onClick={handleClose} disabled={formState.loading}>
          <Close />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers>
        {formState.successMessage && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {formState.successMessage}
          </Alert>
        )}
        {formState.errorMessage && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {formState.errorMessage}
          </Alert>
        )}
        <Box component="form" id="record-expense-dialog-form" onSubmit={handleSubmit}>
          <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 1, mt: 1 }}>
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
            />
            <ButtonGroup variant="outlined" size="small" sx={{ mb: 0.5, height: 40 }}>
              <Tooltip title="Previous day">
                <Button onClick={() => shiftExpenseDate(-1)} sx={{ minWidth: 40, width: 40, height: 40, px: 0 }}>
                  <Remove fontSize="small" />
                </Button>
              </Tooltip>
              <Tooltip title="Next day">
                <Button onClick={() => shiftExpenseDate(1)} sx={{ minWidth: 40, width: 40, height: 40, px: 0 }}>
                  <Add fontSize="small" />
                </Button>
              </Tooltip>
            </ButtonGroup>
          </Box>

          <TextField
            fullWidth
            type="number"
            label="Amount"
            value={formState.amount}
            onChange={(e) => updateField('amount', e.target.value)}
            onBlur={(e) => {
              const num = parseFloat(e.target.value);
              if (!isNaN(num) && num !== 0) updateField('amount', num.toFixed(2));
            }}
            required
            error={!!formState.errors.amount}
            helperText={formState.errors.amount}
            margin="normal"
            inputProps={{ step: '0.01' }}
          />

          <TextField
            fullWidth
            label="Description"
            value={formState.description}
            onChange={(e) => updateField('description', e.target.value)}
            required
            error={!!formState.errors.description}
            helperText={formState.errors.description || `${formState.description.length}/500 characters`}
            margin="normal"
            inputProps={{ maxLength: 500 }}
          />

          <Box sx={{ mt: 2 }}>
            <CategorySelect
              value={formState.categoryId}
              onChange={(id) => updateField('categoryId', id)}
              required
              error={formState.errors.category}
              year={formState.expenseDate ? new Date(formState.expenseDate).getFullYear() : undefined}
            />
          </Box>

          <Box sx={{ mt: 2 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={formState.commonExpense}
                  onChange={(e) => updateField('commonExpense', e.target.checked)}
                />
              }
              label="Common household expense"
            />
          </Box>

          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              Attach files (optional)
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <Button
                variant="outlined"
                component="label"
                startIcon={<AttachFile />}
                disabled={formState.files.length >= maxFiles}
                size="small"
              >
                Add files
                <input type="file" hidden multiple accept="application/pdf,image/*" onChange={handleFileChange} />
              </Button>
              <Button
                variant="outlined"
                component="label"
                startIcon={<CameraAlt />}
                disabled={formState.files.length >= maxFiles}
                size="small"
              >
                Camera
                <input type="file" hidden accept="image/*" capture="environment" onChange={handleFileChange} />
              </Button>
            </Box>
            <Typography variant="caption" color="text.secondary">
              Up to {maxFiles} files, 5MB each
            </Typography>
            {formState.errors.files && (
              <Typography variant="caption" color="error" display="block" sx={{ mt: 0.5 }}>
                {formState.errors.files}
              </Typography>
            )}
            {formState.files.length > 0 && (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
                {formState.files.map((file, index) => (
                  <Chip
                    key={`${file.name}-${index}`}
                    label={file.name}
                    onDelete={() => removeFile(index)}
                    variant="outlined"
                    size="small"
                  />
                ))}
              </Box>
            )}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={formState.loading}>
          Cancel
        </Button>
        <Button
          type="submit"
          form="record-expense-dialog-form"
          variant="contained"
          disabled={formState.loading || !formState.categoryId}
        >
          {formState.loading ? 'Creating...' : 'Create Expense'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
