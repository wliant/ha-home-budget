'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useIngressRouter } from '@/lib/navigation';
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Snackbar,
  Alert,
  Box,
  CircularProgress,
} from '@mui/material';
import { Edit as EditIcon } from '@mui/icons-material';
import { CategorySelect } from '@/components/expenses/CategorySelect';
import { expenseService, getTodayISO } from '@/services/expenseService';
import type { ExpenseDTO } from '@/services/expenseService';

export default function EditExpensePage() {
  const params = useParams();
  const router = useIngressRouter();
  const expenseId = Number(params.id);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form fields
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [expenseDate, setExpenseDate] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!expenseId) return;
    expenseService
      .getExpenseById(expenseId)
      .then((expense: ExpenseDTO) => {
        setAmount(expense.amount.toString());
        setDescription(expense.description);
        setExpenseDate(expense.expenseDate);
        setCategoryId(expense.categoryId);
        setLoading(false);
      })
      .catch((err) => {
        setError('Failed to load expense');
        setLoading(false);
      });
  }, [expenseId]);

  const validateForm = (): boolean => {
    const errs: Record<string, string> = {};
    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum) || amountNum === 0) {
      errs.amount = 'Amount must not be zero';
    }
    if (!description.trim()) {
      errs.description = 'Description is required';
    } else if (description.length > 500) {
      errs.description = 'Description must be 500 characters or less';
    }
    if (!categoryId) {
      errs.category = 'Category is required';
    }
    if (!expenseDate) {
      errs.date = 'Date is required';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSaving(true);
    try {
      await expenseService.updateExpense(expenseId, {
        amount: parseFloat(amount),
        description: description.trim(),
        expenseDate,
        categoryId: categoryId!,
      });
      setSuccessMessage('Expense updated successfully');
      setTimeout(() => router.push('/expenses'), 1500);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to update expense';
      setError(msg);
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Container maxWidth="sm" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress />
      </Container>
    );
  }

  return (
    <Container maxWidth="sm" sx={{ px: { xs: 2, sm: 3 } }}>
      <Paper sx={{ p: { xs: 3, sm: 4 }, mt: { xs: 2, sm: 4 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <EditIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1" gutterBottom>
            Edit Expense
          </Typography>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 3 }}>
          <TextField
            fullWidth
            type="date"
            label="Date"
            value={expenseDate}
            onChange={(e) => setExpenseDate(e.target.value)}
            required
            error={!!errors.date}
            helperText={errors.date}
            margin="normal"
            InputLabelProps={{ shrink: true }}
          />

          <TextField
            fullWidth
            type="number"
            label="Amount"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            onBlur={(e) => {
              const num = parseFloat(e.target.value);
              if (!isNaN(num) && num !== 0) {
                setAmount(num.toFixed(2));
              }
            }}
            required
            error={!!errors.amount}
            helperText={errors.amount}
            margin="normal"
            inputProps={{ step: '0.01' }}
          />

          <TextField
            fullWidth
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            error={!!errors.description}
            helperText={errors.description || `${description.length}/500 characters`}
            margin="normal"
            inputProps={{ maxLength: 500 }}
          />

          <Box sx={{ mt: 2 }}>
            <CategorySelect
              value={categoryId}
              onChange={(id) => setCategoryId(id)}
              required
              error={errors.category}
              year={expenseDate ? new Date(expenseDate).getFullYear() : undefined}
            />
          </Box>

          <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
            <Button
              variant="outlined"
              onClick={() => router.push('/expenses')}
              disabled={saving}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              fullWidth
              disabled={saving || !categoryId}
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </Box>
        </Box>
      </Paper>

      <Snackbar
        open={!!successMessage}
        autoHideDuration={2000}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity="success" sx={{ width: '100%' }}>
          {successMessage}
        </Alert>
      </Snackbar>
    </Container>
  );
}
