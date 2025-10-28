'use client';

import React, { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Paper,
  Grid,
  Alert,
  CircularProgress,
  Chip,
  LinearProgress,
  Divider,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import {
  budgetService,
  BudgetSummaryDTO,
  formatBudgetPeriod,
  formatCurrency,
  getSpendingStatusColor,
  getSpendingStatusText,
} from '@/services/budgetService';

/**
 * Budget detail page - User Story 1: Create and View Budgets
 *
 * Features:
 * - Display budget details with spending breakdown
 * - Show list of expenses (if any)
 * - Edit budget button
 * - Loading and error states
 */

export default function BudgetDetailPage() {
  const router = useRouter();
  const params = useParams();
  const budgetId = params?.id ? parseInt(params.id as string) : null;

  const [budget, setBudget] = useState<BudgetSummaryDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (budgetId) {
      loadBudget();
    }
  }, [budgetId]);

  const loadBudget = async () => {
    if (!budgetId) return;

    setIsLoading(true);
    setError('');

    try {
      const data = await budgetService.getBudgetById(budgetId);
      setBudget(data);
    } catch (err: any) {
      console.error('Failed to load budget:', err);
      if (err.response?.status === 404) {
        setError('Budget not found');
      } else {
        setError('Failed to load budget details. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    router.push('/budgets');
  };

  const handleEdit = () => {
    router.push(`/budgets/${budgetId}/edit`);
  };

  if (isLoading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error || !budget) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          {error || 'Budget not found'}
        </Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={handleBack}>
          Back to Budgets
        </Button>
      </Container>
    );
  }

  const statusColor = getSpendingStatusColor(budget.spendingPercentage);
  const statusText = getSpendingStatusText(budget.spendingPercentage);
  const remaining = budget.totalAmount - budget.totalSpending;

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button startIcon={<ArrowBackIcon />} onClick={handleBack} sx={{ mb: 2 }}>
          Back to Budgets
        </Button>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h4" component="h1">
            {formatBudgetPeriod(budget.year, budget.month)}
          </Typography>
          <Button
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={handleEdit}
          >
            Edit Budget
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Budget Overview */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Budget Overview
            </Typography>

            {/* Status */}
            <Box sx={{ mb: 3 }}>
              <Chip label={statusText} color={statusColor as any} />
            </Box>

            {/* Budget Amount */}
            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary">
                Total Budget
              </Typography>
              <Typography variant="h4">
                {formatCurrency(budget.totalAmount)}
              </Typography>
            </Box>

            {/* Description */}
            {budget.description && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Description
                </Typography>
                <Typography variant="body1">
                  {budget.description}
                </Typography>
              </Box>
            )}

            {/* Metadata */}
            <Divider sx={{ my: 2 }} />
            <Box>
              <Typography variant="body2" color="text.secondary">
                Created by: {budget.createdBy}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Created: {new Date(budget.createdAt).toLocaleDateString()}
              </Typography>
            </Box>
          </Paper>
        </Grid>

        {/* Spending Summary */}
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Spending Summary
            </Typography>

            {/* Spent Amount */}
            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Total Spent
              </Typography>
              <Typography variant="h4">
                {formatCurrency(budget.totalSpending)}
              </Typography>
            </Box>

            {/* Progress Bar */}
            <Box sx={{ mb: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  Budget Used
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {budget.spendingPercentage.toFixed(1)}%
                </Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={Math.min(budget.spendingPercentage, 100)}
                color={statusColor as any}
                sx={{ height: 10, borderRadius: 5 }}
              />
            </Box>

            {/* Remaining/Over Amount */}
            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary">
                {remaining >= 0 ? 'Remaining' : 'Over Budget'}
              </Typography>
              <Typography
                variant="h4"
                color={remaining >= 0 ? 'success.main' : 'error.main'}
              >
                {formatCurrency(Math.abs(remaining))}
              </Typography>
            </Box>

            {/* Expense Count */}
            <Divider sx={{ my: 2 }} />
            <Typography variant="body2" color="text.secondary">
              {budget.expenseCount} {budget.expenseCount === 1 ? 'expense' : 'expenses'} recorded
            </Typography>
          </Paper>
        </Grid>

        {/* Expenses List */}
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Expenses
            </Typography>

            {budget.expenses && budget.expenses.length > 0 ? (
              <Box>
                {budget.expenses.map((expense) => (
                  <Box
                    key={expense.id}
                    sx={{
                      py: 2,
                      borderBottom: '1px solid',
                      borderColor: 'divider',
                      '&:last-child': { borderBottom: 'none' },
                    }}
                  >
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="body1">
                        {expense.description || 'No description'}
                      </Typography>
                      <Typography variant="h6">
                        {formatCurrency(expense.amount)}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', gap: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        {new Date(expense.expenseDate).toLocaleDateString()}
                      </Typography>
                      {expense.category && (
                        <Typography variant="body2" color="text.secondary">
                          {expense.category.icon} {expense.category.name}
                        </Typography>
                      )}
                    </Box>
                  </Box>
                ))}
              </Box>
            ) : (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <Typography variant="body1" color="text.secondary">
                  No expenses recorded yet
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Start adding expenses to track your spending
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
}
