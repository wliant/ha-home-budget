'use client';

import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Grid,
  Card,
  CardContent,
  CircularProgress,
  Alert,
  LinearProgress,
  Chip,
} from '@mui/material';
import { budgetService, BudgetSummaryDTO, formatBudgetPeriod, formatCurrency, getSpendingStatusColor, getSpendingStatusText } from '@/services/budgetService';

/**
 * Dashboard page - User Story 4: Budget Dashboard and Insights
 *
 * Simplified dashboard showing current month budget progress
 */

export default function DashboardPage() {
  const [currentBudget, setCurrentBudget] = useState<BudgetSummaryDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    loadCurrentBudget();
  }, []);

  const loadCurrentBudget = async () => {
    setIsLoading(true);
    try {
      const response = await budgetService.getCurrentMonthBudget();
      setCurrentBudget(response);
    } catch (err: any) {
      console.error('Failed to load current budget:', err);
      if (err.response?.status === 404) {
        setError('No budget found for current month');
      } else {
        setError('Failed to load dashboard data');
      }
    } finally {
      setIsLoading(false);
    }
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

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Dashboard
      </Typography>

      {error && !currentBudget && (
        <Alert severity="info" sx={{ mb: 3 }}>
          {error}. Create a budget for this month to see dashboard insights.
        </Alert>
      )}

      {currentBudget && (
        <Grid container spacing={3}>
          {/* Current Month Budget */}
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Current Month: {formatBudgetPeriod(currentBudget.year, currentBudget.month)}
                </Typography>

                <Box sx={{ mb: 2 }}>
                  <Chip
                    label={getSpendingStatusText(currentBudget.spendingPercentage)}
                    color={getSpendingStatusColor(currentBudget.spendingPercentage) as any}
                  />
                </Box>

                <Grid container spacing={2}>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="body2" color="text.secondary">Budget</Typography>
                    <Typography variant="h5">{formatCurrency(currentBudget.totalAmount)}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="body2" color="text.secondary">Spent</Typography>
                    <Typography variant="h5">{formatCurrency(currentBudget.totalSpending)}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <Typography variant="body2" color="text.secondary">Remaining</Typography>
                    <Typography variant="h5" color={currentBudget.totalAmount - currentBudget.totalSpending >= 0 ? 'success.main' : 'error.main'}>
                      {formatCurrency(Math.abs(currentBudget.totalAmount - currentBudget.totalSpending))}
                    </Typography>
                  </Grid>
                </Grid>

                <Box sx={{ mt: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Budget Used</Typography>
                    <Typography variant="body2">{currentBudget.spendingPercentage.toFixed(1)}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(currentBudget.spendingPercentage, 100)}
                    color={getSpendingStatusColor(currentBudget.spendingPercentage) as any}
                    sx={{ height: 10, borderRadius: 5 }}
                  />
                </Box>

                {currentBudget.spendingPercentage >= 90 && (
                  <Alert severity="warning" sx={{ mt: 2 }}>
                    Warning: You've used {currentBudget.spendingPercentage.toFixed(1)}% of your budget!
                  </Alert>
                )}

                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  {currentBudget.expenseCount} {currentBudget.expenseCount === 1 ? 'expense' : 'expenses'} recorded
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Container>
  );
}
