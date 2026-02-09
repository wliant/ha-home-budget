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
  Chip,
} from '@mui/material';
import { budgetService, BudgetSummaryDTO, formatBudgetPeriod, formatCurrency, getSpendingStatusColor, getSpendingStatusText } from '@/services/budgetService';
import BudgetPieChart from '@/components/dashboard/BudgetPieChart';

/**
 * Dashboard page - User Story 4: Budget Dashboard and Insights
 *
 * Shows budget progress for the current month and 2 previous months.
 */

interface MonthEntry {
  year: number;
  month: number;
}

function getPast3Months(): MonthEntry[] {
  const now = new Date();
  const months: MonthEntry[] = [];
  for (let i = 0; i < 3; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    months.push({ year: d.getFullYear(), month: d.getMonth() + 1 });
  }
  return months;
}

export default function DashboardPage() {
  const [monthlyBudgets, setMonthlyBudgets] = useState<(BudgetSummaryDTO | null)[]>([]);
  const [months] = useState<MonthEntry[]>(getPast3Months);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    loadBudgets();
  }, []);

  const loadBudgets = async () => {
    setIsLoading(true);
    try {
      const results = await Promise.all(
        months.map(async ({ year, month }) => {
          try {
            return await budgetService.getMonthlyBudgetSummary(year, month);
          } catch (err: any) {
            if (err.response?.status === 404) {
              return null;
            }
            throw err;
          }
        })
      );
      setMonthlyBudgets(results);
      if (results.every((r) => r === null)) {
        setError('No budgets found for the past 3 months');
      }
    } catch (err: any) {
      console.error('Failed to load budgets:', err);
      setError('Failed to load dashboard data');
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

      {error && monthlyBudgets.every((b) => b === null) && (
        <Alert severity="info" sx={{ mb: 3 }}>
          {error}. Create a budget to see dashboard insights.
        </Alert>
      )}

      <Grid container spacing={3}>
        {months.map((entry, index) => {
          const budget = monthlyBudgets[index];
          if (!budget) return null;

          const remaining = budget.totalAmount - budget.totalSpending;
          const isCurrentMonth = index === 0;

          return (
            <Grid item xs={12} md={4} key={`${entry.year}-${entry.month}`}>
              <Card sx={isCurrentMonth ? { border: 2, borderColor: 'primary.main' } : {}}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {isCurrentMonth ? 'Current Month' : formatBudgetPeriod(entry.year, entry.month)}
                    {isCurrentMonth && (
                      <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                        {formatBudgetPeriod(entry.year, entry.month)}
                      </Typography>
                    )}
                  </Typography>

                  <Box sx={{ mb: 2 }}>
                    <Chip
                      label={getSpendingStatusText(budget.spendingPercentage)}
                      color={getSpendingStatusColor(budget.spendingPercentage) as any}
                      size="small"
                    />
                  </Box>

                  <BudgetPieChart
                    totalBudget={budget.totalAmount}
                    totalSpending={budget.totalSpending}
                  />

                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary">Budget</Typography>
                      <Typography variant="subtitle2">{formatCurrency(budget.totalAmount)}</Typography>
                    </Box>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="body2" color="text.secondary">Spent</Typography>
                      <Typography variant="subtitle2">{formatCurrency(budget.totalSpending)}</Typography>
                    </Box>
                    <Box sx={{ textAlign: 'right' }}>
                      <Typography variant="body2" color="text.secondary">
                        {remaining >= 0 ? 'Remaining' : 'Over'}
                      </Typography>
                      <Typography variant="subtitle2" color={remaining >= 0 ? 'success.main' : 'error.main'}>
                        {formatCurrency(Math.abs(remaining))}
                      </Typography>
                    </Box>
                  </Box>

                  {budget.spendingPercentage >= 90 && (
                    <Alert severity="warning" sx={{ mt: 2 }}>
                      {budget.spendingPercentage.toFixed(1)}% of budget used!
                    </Alert>
                  )}

                  <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                    {budget.expenseCount} {budget.expenseCount === 1 ? 'expense' : 'expenses'} recorded
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          );
        })}
      </Grid>
    </Container>
  );
}
