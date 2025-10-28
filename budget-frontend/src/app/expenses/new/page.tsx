'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Alert,
  Paper,
  Breadcrumbs,
  Link,
} from '@mui/material';
import { ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import ExpenseForm from '@/components/expenses/ExpenseForm';
import { expenseService, CreateExpenseRequest } from '@/services/expenseService';
import { budgetService, formatBudgetPeriod } from '@/services/budgetService';

/**
 * New expense page - User Story 2: Record Expenses Against Budgets
 *
 * Features:
 * - Form to create a new expense
 * - Budget ID from query parameter
 * - Validation and error handling
 * - Redirect to budget detail on success
 */

export default function NewExpensePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const budgetIdParam = searchParams?.get('budgetId');
  const budgetId = budgetIdParam ? parseInt(budgetIdParam) : null;

  const [successMessage, setSuccessMessage] = useState<string>('');
  const [budgetInfo, setBudgetInfo] = useState<{ year: number; month: number } | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (budgetId) {
      loadBudgetInfo();
    }
  }, [budgetId]);

  const loadBudgetInfo = async () => {
    if (!budgetId) return;

    try {
      const budget = await budgetService.getBudgetById(budgetId);
      setBudgetInfo({ year: budget.year, month: budget.month });
    } catch (err: any) {
      console.error('Failed to load budget:', err);
      setError('Failed to load budget information');
    }
  };

  const handleSubmit = async (data: CreateExpenseRequest) => {
    if (!budgetId) {
      setError('Budget ID is required');
      return;
    }

    try {
      const created = await expenseService.createExpense({
        ...data,
        budgetId,
      });

      setSuccessMessage('Expense added successfully!');

      // Redirect to budget detail after a short delay
      setTimeout(() => {
        router.push(`/budgets/${budgetId}`);
      }, 1500);
    } catch (error: any) {
      // Error handling is done in ExpenseForm component
      throw error;
    }
  };

  const handleCancel = () => {
    if (budgetId) {
      router.push(`/budgets/${budgetId}`);
    } else {
      router.push('/budgets');
    }
  };

  if (!budgetId) {
    return (
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          Budget ID is required. Please select a budget first.
        </Alert>
        <Button onClick={() => router.push('/budgets')}>
          Go to Budgets
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      {/* Breadcrumbs */}
      <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 2 }}>
        <Link underline="hover" color="inherit" href="/budgets" onClick={(e) => { e.preventDefault(); router.push('/budgets'); }}>
          Budgets
        </Link>
        {budgetInfo && (
          <Link
            underline="hover"
            color="inherit"
            href={`/budgets/${budgetId}`}
            onClick={(e) => { e.preventDefault(); router.push(`/budgets/${budgetId}`); }}
          >
            {formatBudgetPeriod(budgetInfo.year, budgetInfo.month)}
          </Link>
        )}
        <Typography color="text.primary">Add Expense</Typography>
      </Breadcrumbs>

      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleCancel}
          sx={{ mb: 2 }}
        >
          Back to Budget
        </Button>
        <Typography variant="h4" component="h1">
          Add New Expense
        </Typography>
        {budgetInfo && (
          <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>
            For {formatBudgetPeriod(budgetInfo.year, budgetInfo.month)}
          </Typography>
        )}
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Success Message */}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {successMessage}
        </Alert>
      )}

      {/* Expense Form */}
      <ExpenseForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        budgetId={budgetId}
      />
    </Container>
  );
}
