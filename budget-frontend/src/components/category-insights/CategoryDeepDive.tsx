'use client';

import React, { useMemo } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  CircularProgress,
  Chip,
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  Cell,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import type { CategoryExpenseAggregate } from '@/services/expenseService';
import type { YearlyCategoryBudgetDTO } from '@/services/budgetService';
import type { ExpenseDTO } from '@/services/expenseService';
import SpendingTrendChart from '@/components/dashboard/SpendingTrendChart';
import { formatCurrency, formatCurrencyCompact, formatExpenseDate, getMonthName } from '@/utils/formatters';
import { transformAggregates, formatMonthLabel } from '@/utils/spendingTrends';

const CHART_COLORS = [
  '#2196F3', '#4CAF50', '#FF9800', '#E91E63', '#9C27B0',
  '#00BCD4', '#FF5722', '#795548', '#607D8B', '#3F51B5',
];

interface CategoryDeepDiveProps {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  monthlyAggregates: CategoryExpenseAggregate[];
  yearlyBudgetCategory?: YearlyCategoryBudgetDTO;
  childAggregates: CategoryExpenseAggregate[];
  recentExpenses: ExpenseDTO[] | null;
  expensesLoading: boolean;
  selectedYear: number;
  selectedMonth: number;
  onClose: () => void;
}

export default function CategoryDeepDive({
  categoryName,
  categoryIcon,
  monthlyAggregates,
  yearlyBudgetCategory,
  childAggregates,
  recentExpenses,
  expensesLoading,
  selectedYear,
  selectedMonth,
  onClose,
}: CategoryDeepDiveProps) {
  const theme = useTheme();

  // 12-month trend data for SpendingTrendChart
  const { chartData, categories: trendCategories } = useMemo(() => {
    const catAggs = monthlyAggregates.filter(
      (a) => a.categoryName === categoryName && a.month != null
    );
    return transformAggregates(catAggs, 'monthly');
  }, [monthlyAggregates, categoryName]);

  // Budget vs Actual bar chart data
  const budgetVsActualData = useMemo(() => {
    if (!yearlyBudgetCategory) return [];
    return yearlyBudgetCategory.months.map((m) => ({
      month: m.month,
      label: getMonthName(m.month),
      budget: m.budgetAmount,
      actual: m.spending,
    }));
  }, [yearlyBudgetCategory]);

  // Child category breakdown for selected month
  const childBreakdownData = useMemo(() => {
    return childAggregates
      .filter((a) => a.totalAmount > 0)
      .sort((a, b) => b.totalAmount - a.totalAmount)
      .map((a, i) => ({
        name: a.categoryName,
        icon: a.categoryIcon,
        amount: a.totalAmount,
        color: CHART_COLORS[i % CHART_COLORS.length],
      }));
  }, [childAggregates]);

  return (
    <Paper
      elevation={2}
      sx={{
        p: { xs: 2, sm: 3 },
        mb: 3,
        border: '2px solid',
        borderColor: 'primary.light',
        position: 'relative',
      }}
    >
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {categoryIcon && <Typography variant="h5">{categoryIcon}</Typography>}
          <Typography variant="h6">{categoryName}</Typography>
          <Chip label="Deep Dive" size="small" color="primary" variant="outlined" />
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </Box>

      <Grid container spacing={3}>
        {/* 12-Month Spending Trend */}
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            12-Month Spending Trend ({selectedYear})
          </Typography>
          {chartData.length > 0 ? (
            <SpendingTrendChart
              chartData={chartData}
              xAxisKey="month"
              categories={trendCategories}
              xAxisFormatter={formatMonthLabel}
              granularity="monthly"
              height={{ xs: 250, sm: 280 }}
            />
          ) : (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <Typography color="text.secondary" variant="body2">No trend data available.</Typography>
            </Box>
          )}
        </Grid>

        {/* Budget vs Actual */}
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            Budget vs Actual ({selectedYear})
          </Typography>
          {budgetVsActualData.length > 0 ? (
            <Box sx={{ width: '100%', height: { xs: 250, sm: 280 } }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={budgetVsActualData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                  <XAxis
                    dataKey="month"
                    tickFormatter={(v) => formatMonthLabel(v)}
                    tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
                  />
                  <YAxis
                    tickFormatter={(v) => formatCurrencyCompact(v)}
                    tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
                    width={70}
                  />
                  <Tooltip
                    formatter={(value, name) => [
                      formatCurrency(Number(value)),
                      name === 'budget' ? 'Budget' : 'Actual',
                    ]}
                    labelFormatter={(label) => formatMonthLabel(label)}
                    contentStyle={{
                      backgroundColor: theme.palette.background.paper,
                      border: `1px solid ${theme.palette.divider}`,
                      borderRadius: 8,
                    }}
                  />
                  <Legend />
                  <Bar dataKey="budget" name="Budget" fill={theme.palette.info.main} radius={[4, 4, 0, 0]} />
                  <Bar
                    dataKey="actual"
                    name="Actual"
                    radius={[4, 4, 0, 0]}
                  >
                    {budgetVsActualData.map((entry, index) => (
                      <Cell
                        key={index}
                        fill={
                          entry.budget > 0 && entry.actual > entry.budget
                            ? theme.palette.error.main
                            : theme.palette.success.main
                        }
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Box>
          ) : (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <Typography color="text.secondary" variant="body2">No budget data for this category.</Typography>
            </Box>
          )}
        </Grid>

        {/* Child Category Breakdown */}
        {childBreakdownData.length > 0 && (
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
              Sub-Category Breakdown ({getMonthName(selectedMonth)})
            </Typography>
            <Box sx={{ width: '100%', height: { xs: 200, sm: 250 } }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={childBreakdownData}
                  layout="vertical"
                  margin={{ top: 5, right: 20, left: 10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
                  <XAxis
                    type="number"
                    tickFormatter={(v) => formatCurrencyCompact(v)}
                    tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
                  />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={100}
                    tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
                  />
                  <Tooltip
                    formatter={(value) => formatCurrency(Number(value))}
                    contentStyle={{
                      backgroundColor: theme.palette.background.paper,
                      border: `1px solid ${theme.palette.divider}`,
                      borderRadius: 8,
                    }}
                  />
                  <Bar dataKey="amount" name="Spending" radius={[0, 4, 4, 0]}>
                    {childBreakdownData.map((entry, index) => (
                      <Cell key={index} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Box>
          </Grid>
        )}

        {/* Recent Expenses */}
        <Grid item xs={12} md={childBreakdownData.length > 0 ? 6 : 12}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            Recent Expenses ({getMonthName(selectedMonth)} {selectedYear})
          </Typography>
          {expensesLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={24} />
            </Box>
          ) : recentExpenses && recentExpenses.length > 0 ? (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {recentExpenses.map((exp) => (
                  <TableRow key={exp.id}>
                    <TableCell>
                      <Typography variant="body2">{formatExpenseDate(exp.expenseDate)}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>
                        {exp.description}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {formatCurrency(exp.amount)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <Typography color="text.secondary" variant="body2">No expenses this month.</Typography>
            </Box>
          )}
        </Grid>
      </Grid>
    </Paper>
  );
}
