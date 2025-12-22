'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Card,
  CardContent,
  Typography,
  LinearProgress,
  Box,
  CircularProgress,
  Alert,
  Button,
  Chip,
} from '@mui/material';
import { budgetService, BudgetSummaryDTO, formatBudgetPeriod, formatCurrency } from '@/services/budgetService';

/**
 * Budget Summary Card Component
 * User Story 1: Quick Budget Overview (P1)
 *
 * Displays current month budget summary with:
 * - Total amount, spent amount, remaining amount
 * - Progress bar with spending percentage
 * - Status indicator (green/yellow/red)
 * - Empty state for no budget
 * - Error handling with retry
 */
export function BudgetSummaryCard() {
  const router = useRouter();
  const [budgetData, setBudgetData] = useState<BudgetSummaryDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    loadBudgetData();
  }, []);

  const loadBudgetData = async () => {
    setIsLoading(true);
    setError('');
    try {
      const data = await budgetService.getCurrentMonthBudget();
      setBudgetData(data);
    } catch (err: any) {
      console.error('Failed to load budget:', err);
      if (err.response?.status === 404) {
        // No budget for current month - this is OK, show empty state
        setBudgetData(null);
      } else {
        setError(err.response?.data?.message || 'Failed to load budget summary');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleRetry = () => {
    loadBudgetData();
  };

  const handleCreateBudget = () => {
    router.push('/budgets/new');
  };

  // Calculate status color based on spending percentage
  const getStatusColor = (percentage: number): 'success' | 'warning' | 'error' => {
    if (percentage <= 80) return 'success';
    if (percentage <= 100) return 'warning';
    return 'error';
  };

  const getStatusText = (percentage: number): string => {
    if (percentage <= 80) return 'On Track';
    if (percentage <= 100) return 'Warning';
    return 'Overspent';
  };

  // Format period as "Month YYYY"
  const formatPeriod = (month: number, year: number): string => {
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return `${monthNames[month - 1]} ${year}`;
  };

  // Loading state
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Budget Summary
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Error state
  if (error) {
    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Budget Summary
          </Typography>
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
            <Box sx={{ mt: 2 }}>
              <Button variant="outlined" size="small" onClick={handleRetry} aria-label="Retry loading budget summary">
                Retry
              </Button>
            </Box>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  // Empty state - no budget for current month
  if (!budgetData) {
    const now = new Date();
    const currentPeriod = formatPeriod(now.getMonth() + 1, now.getFullYear());

    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Budget Summary
          </Typography>
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body1" color="text.secondary" gutterBottom>
              No budget found for {currentPeriod}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Create a budget to start tracking your spending
            </Typography>
            <Button variant="contained" onClick={handleCreateBudget} aria-label="Create a new monthly budget">
              Create Budget
            </Button>
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Calculate values
  const remaining = budgetData.totalAmount - budgetData.totalSpending;
  const statusColor = getStatusColor(budgetData.spendingPercentage);
  const statusText = getStatusText(budgetData.spendingPercentage);
  const period = formatPeriod(budgetData.month, budgetData.year);

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            Budget Summary
          </Typography>
          <Chip
            label={statusText}
            color={statusColor}
            size="small"
          />
        </Box>

        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          {period}
        </Typography>

        {/* Budget amounts */}
        <Box sx={{ mt: 3, mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Budget
            </Typography>
            <Typography variant="body2" fontWeight="medium">
              {formatCurrency(budgetData.totalAmount)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Spent
            </Typography>
            <Typography variant="body2" fontWeight="medium">
              {formatCurrency(budgetData.totalSpending)}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Remaining
            </Typography>
            <Typography
              variant="body2"
              fontWeight="medium"
              color={remaining < 0 ? 'error.main' : 'inherit'}
            >
              {formatCurrency(remaining)}
            </Typography>
          </Box>
        </Box>

        {/* Progress bar */}
        <Box sx={{ mt: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="caption" color="text.secondary">
              Spending Progress
            </Typography>
            <Typography variant="caption" fontWeight="medium">
              {budgetData.spendingPercentage.toFixed(1)}%
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={Math.min(budgetData.spendingPercentage, 100)}
            color={statusColor}
            sx={{ height: 8, borderRadius: 1 }}
          />
        </Box>

        {/* Expense count */}
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
          {budgetData.expenseCount} {budgetData.expenseCount === 1 ? 'expense' : 'expenses'} recorded
        </Typography>
      </CardContent>
    </Card>
  );
}
