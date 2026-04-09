'use client';

import React, { useMemo } from 'react';
import {
  Box,
  Typography,
  LinearProgress,
  Chip,
  Alert,
} from '@mui/material';
import { formatCurrency, getSpendingStatusColor } from '@/utils/formatters';
import type { CategoryExpenseAggregate } from '@/services/expenseService';
import type { YearlyBudgetViewDTO } from '@/services/budgetService';

interface SpendingForecastProps {
  currentMonthAggregates: CategoryExpenseAggregate[];
  yearlyBudgetView: YearlyBudgetViewDTO | null;
  selectedMonth: number;
  selectedYear: number;
}

interface ForecastRow {
  categoryName: string;
  categoryIcon?: string;
  spent: number;
  budget: number;
  projected: number;
  projectedPct: number;
  status: 'on-track' | 'warning' | 'over-budget';
}

export default function SpendingForecast({
  currentMonthAggregates,
  yearlyBudgetView,
  selectedMonth,
  selectedYear,
}: SpendingForecastProps) {
  const forecast = useMemo(() => {
    const now = new Date();
    const isCurrentMonth = now.getFullYear() === selectedYear && now.getMonth() + 1 === selectedMonth;
    const daysInMonth = new Date(selectedYear, selectedMonth, 0).getDate();
    const dayElapsed = isCurrentMonth ? now.getDate() : daysInMonth;
    const fractionElapsed = dayElapsed / daysInMonth;

    if (!yearlyBudgetView) return { rows: [], totalSpent: 0, totalBudget: 0, totalProjected: 0, fractionElapsed, daysInMonth, dayElapsed, isCurrentMonth };

    const parentAggs = currentMonthAggregates.filter((a) => !a.parentCategoryId);
    const totalSpent = parentAggs.reduce((s, a) => s + a.totalAmount, 0);

    const rows: ForecastRow[] = [];
    let totalBudget = 0;

    for (const cat of yearlyBudgetView.categories.filter((c) => !c.parentCategoryId)) {
      const monthData = cat.months[selectedMonth - 1];
      if (!monthData || !monthData.hasBudget) continue;

      const agg = parentAggs.find((a) => a.categoryId === cat.categoryId);
      const spent = agg ? agg.totalAmount : 0;
      const budget = monthData.budgetAmount;
      const projected = fractionElapsed > 0 ? spent / fractionElapsed : 0;
      const projectedPct = budget > 0 ? (projected / budget) * 100 : 0;

      totalBudget += budget;

      rows.push({
        categoryName: cat.categoryName,
        categoryIcon: cat.categoryIcon,
        spent,
        budget,
        projected,
        projectedPct,
        status: projectedPct >= 100 ? 'over-budget' : projectedPct >= 80 ? 'warning' : 'on-track',
      });
    }

    rows.sort((a, b) => b.projectedPct - a.projectedPct);
    const totalProjected = fractionElapsed > 0 ? totalSpent / fractionElapsed : 0;

    return { rows, totalSpent, totalBudget, totalProjected, fractionElapsed, daysInMonth, dayElapsed, isCurrentMonth };
  }, [currentMonthAggregates, yearlyBudgetView, selectedMonth, selectedYear]);

  if (forecast.rows.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No budgets set for this month to forecast against.</Typography>
      </Box>
    );
  }

  const overBudgetCount = forecast.rows.filter((r) => r.status === 'over-budget').length;
  const totalPct = forecast.totalBudget > 0 ? (forecast.totalProjected / forecast.totalBudget) * 100 : 0;

  return (
    <Box>
      {/* Overall summary */}
      <Alert
        severity={totalPct >= 100 ? 'error' : totalPct >= 80 ? 'warning' : 'success'}
        sx={{ mb: 2 }}
      >
        <Typography variant="body2">
          {forecast.isCurrentMonth
            ? `Day ${forecast.dayElapsed} of ${forecast.daysInMonth} (${(forecast.fractionElapsed * 100).toFixed(0)}% of month). `
            : `Full month completed. `}
          Spent {formatCurrency(forecast.totalSpent)} of {formatCurrency(forecast.totalBudget)} budget.
          {forecast.isCurrentMonth && (
            <> Projected: <strong>{formatCurrency(forecast.totalProjected)}</strong> ({totalPct.toFixed(0)}% of budget).
            {overBudgetCount > 0 && ` ${overBudgetCount} categor${overBudgetCount === 1 ? 'y' : 'ies'} on pace to overspend.`}</>
          )}
        </Typography>
      </Alert>

      {/* Per-category forecasts */}
      {forecast.rows.map((row) => (
        <Box key={row.categoryName} sx={{ mb: 1.5, px: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="body2">
              {row.categoryIcon ? `${row.categoryIcon} ` : ''}{row.categoryName}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="caption" color="text.secondary">
                {formatCurrency(row.spent)} / {formatCurrency(row.budget)}
              </Typography>
              {forecast.isCurrentMonth && (
                <Chip
                  size="small"
                  variant="outlined"
                  color={row.status === 'over-budget' ? 'error' : row.status === 'warning' ? 'warning' : 'success'}
                  label={`${formatCurrency(row.projected)} projected`}
                />
              )}
            </Box>
          </Box>
          <LinearProgress
            variant="determinate"
            value={Math.min(row.projectedPct, 100)}
            color={getSpendingStatusColor(row.projectedPct) as 'success' | 'info' | 'warning' | 'error'}
            sx={{ height: 6, borderRadius: 3 }}
          />
        </Box>
      ))}
    </Box>
  );
}
