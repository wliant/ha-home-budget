'use client';

import React, { useMemo } from 'react';
import {
  Box,
  Typography,
  LinearProgress,
  Chip,
} from '@mui/material';
import type { YearlyCategoryBudgetDTO } from '@/services/budgetService';
import { formatCurrency, getSpendingStatusColor } from '@/utils/formatters';

interface BudgetHealthBarsProps {
  budgetCategories: YearlyCategoryBudgetDTO[];
  selectedMonth: number;
  onCategoryClick?: (categoryId: number) => void;
  selectedCategoryId?: number | null;
}

interface CategoryBudgetRow {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  budgetAmount: number;
  spending: number;
  utilization: number;
  hasBudget: boolean;
}

export default function BudgetHealthBars({
  budgetCategories,
  selectedMonth,
  onCategoryClick,
  selectedCategoryId,
}: BudgetHealthBarsProps) {
  const { withBudget, withoutBudget } = useMemo(() => {
    const rows: CategoryBudgetRow[] = budgetCategories
      .filter((c) => !c.parentCategoryId)
      .map((cat) => {
        const monthData = cat.months[selectedMonth - 1];
        if (!monthData) {
          return {
            categoryId: cat.categoryId,
            categoryName: cat.categoryName,
            categoryIcon: cat.categoryIcon,
            budgetAmount: 0,
            spending: 0,
            utilization: 0,
            hasBudget: false,
          };
        }
        return {
          categoryId: cat.categoryId,
          categoryName: cat.categoryName,
          categoryIcon: cat.categoryIcon,
          budgetAmount: monthData.budgetAmount,
          spending: monthData.spending,
          utilization: monthData.budgetAmount > 0
            ? (monthData.spending / monthData.budgetAmount) * 100
            : 0,
          hasBudget: monthData.hasBudget,
        };
      });

    const withBudget = rows
      .filter((r) => r.hasBudget)
      .sort((a, b) => b.utilization - a.utilization);

    const withoutBudget = rows
      .filter((r) => !r.hasBudget && r.spending > 0)
      .sort((a, b) => b.spending - a.spending);

    return { withBudget, withoutBudget };
  }, [budgetCategories, selectedMonth]);

  if (withBudget.length === 0 && withoutBudget.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 6 }}>
        <Typography color="text.secondary">No budget data for this period.</Typography>
      </Box>
    );
  }

  const renderRow = (row: CategoryBudgetRow) => {
    const statusColor = getSpendingStatusColor(row.utilization);
    const remaining = row.budgetAmount - row.spending;

    return (
      <Box
        key={row.categoryId}
        onClick={() => onCategoryClick?.(row.categoryId)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          py: 1,
          px: 1.5,
          borderRadius: 1,
          cursor: onCategoryClick ? 'pointer' : 'default',
          bgcolor: selectedCategoryId === row.categoryId ? 'action.selected' : 'transparent',
          '&:hover': onCategoryClick ? { bgcolor: 'action.hover' } : {},
        }}
      >
        <Typography variant="body2" sx={{ minWidth: 24, textAlign: 'center' }}>
          {row.categoryIcon || ''}
        </Typography>
        <Typography variant="body2" sx={{ minWidth: 120, flexShrink: 0 }}>
          {row.categoryName}
        </Typography>
        <Box sx={{ flexGrow: 1, mx: 1 }}>
          <LinearProgress
            variant="determinate"
            value={Math.min(row.utilization, 100)}
            color={statusColor as 'success' | 'info' | 'warning' | 'error'}
            sx={{ height: 8, borderRadius: 4 }}
          />
        </Box>
        <Typography variant="body2" sx={{ minWidth: 100, textAlign: 'right', fontWeight: 500 }}>
          {formatCurrency(row.spending)}
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ minWidth: 20, textAlign: 'center' }}>
          /
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ minWidth: 100, textAlign: 'right' }}>
          {formatCurrency(row.budgetAmount)}
        </Typography>
        <Chip
          label={remaining >= 0 ? formatCurrency(remaining) : formatCurrency(Math.abs(remaining))}
          size="small"
          color={remaining >= 0 ? 'success' : 'error'}
          variant="outlined"
          sx={{ minWidth: 80, fontWeight: 500 }}
        />
      </Box>
    );
  };

  return (
    <Box>
      {withBudget.length > 0 && (
        <Box>
          {withBudget.map(renderRow)}
        </Box>
      )}

      {withoutBudget.length > 0 && (
        <Box sx={{ mt: 2 }}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1, px: 1.5 }}>
            No Budget Set
          </Typography>
          {withoutBudget.map((row) => (
            <Box
              key={row.categoryId}
              onClick={() => onCategoryClick?.(row.categoryId)}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                py: 1,
                px: 1.5,
                borderRadius: 1,
                cursor: onCategoryClick ? 'pointer' : 'default',
                bgcolor: selectedCategoryId === row.categoryId ? 'action.selected' : 'transparent',
                '&:hover': onCategoryClick ? { bgcolor: 'action.hover' } : {},
              }}
            >
              <Typography variant="body2" sx={{ minWidth: 24, textAlign: 'center' }}>
                {row.categoryIcon || ''}
              </Typography>
              <Typography variant="body2" sx={{ flexGrow: 1 }}>
                {row.categoryName}
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {formatCurrency(row.spending)} spent
              </Typography>
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
}
