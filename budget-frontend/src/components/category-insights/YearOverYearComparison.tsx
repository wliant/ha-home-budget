'use client';

import React, { useMemo } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import {
  ArrowUpward as ArrowUpIcon,
  ArrowDownward as ArrowDownIcon,
} from '@mui/icons-material';
import { formatCurrency, getMonthName } from '@/utils/formatters';
import type { CategoryExpenseAggregate } from '@/services/expenseService';

interface YearOverYearComparisonProps {
  currentYearAggregates: CategoryExpenseAggregate[];
  previousYearAggregates: CategoryExpenseAggregate[];
  selectedMonth: number;
  selectedYear: number;
}

export default function YearOverYearComparison({
  currentYearAggregates,
  previousYearAggregates,
  selectedMonth,
  selectedYear,
}: YearOverYearComparisonProps) {
  const rows = useMemo(() => {
    const currentMonth = currentYearAggregates
      .filter((a) => a.month === selectedMonth && !a.parentCategoryId);
    const prevMonth = previousYearAggregates
      .filter((a) => a.month === selectedMonth && !a.parentCategoryId);

    const prevMap = new Map<number, number>();
    for (const a of prevMonth) {
      prevMap.set(a.categoryId, a.totalAmount);
    }

    const result = currentMonth.map((a) => {
      const prevAmount = prevMap.get(a.categoryId) ?? 0;
      const change = a.totalAmount - prevAmount;
      const changePct = prevAmount > 0 ? (change / prevAmount) * 100 : null;
      return {
        categoryId: a.categoryId,
        categoryName: a.categoryName,
        categoryIcon: a.categoryIcon,
        currentAmount: a.totalAmount,
        previousAmount: prevAmount,
        change,
        changePct,
      };
    });

    return result.sort((a, b) => Math.abs(b.change) - Math.abs(a.change));
  }, [currentYearAggregates, previousYearAggregates, selectedMonth]);

  if (rows.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No data to compare year-over-year.</Typography>
      </Box>
    );
  }

  const currentTotal = rows.reduce((s, r) => s + r.currentAmount, 0);
  const prevTotal = rows.reduce((s, r) => s + r.previousAmount, 0);
  const totalChange = currentTotal - prevTotal;
  const totalChangePct = prevTotal > 0 ? (totalChange / prevTotal) * 100 : null;

  return (
    <Box>
      {/* Summary */}
      <Box sx={{ display: 'flex', gap: 3, mb: 2, flexWrap: 'wrap' }}>
        <Box>
          <Typography variant="caption" color="text.secondary">{getMonthName(selectedMonth)} {selectedYear}</Typography>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>{formatCurrency(currentTotal)}</Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary">{getMonthName(selectedMonth)} {selectedYear - 1}</Typography>
          <Typography variant="h6" color="text.secondary">{formatCurrency(prevTotal)}</Typography>
        </Box>
        {totalChangePct !== null && (
          <Box>
            <Typography variant="caption" color="text.secondary">Change</Typography>
            <Typography variant="h6" sx={{ color: totalChange > 0 ? 'error.main' : 'success.main', fontWeight: 600 }}>
              {totalChange > 0 ? '+' : ''}{totalChangePct.toFixed(1)}%
            </Typography>
          </Box>
        )}
      </Box>

      {/* Per-category table */}
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Category</TableCell>
              <TableCell align="right">{selectedYear}</TableCell>
              <TableCell align="right">{selectedYear - 1}</TableCell>
              <TableCell align="right">Change</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => {
              const isUp = row.change > 0;
              const isDown = row.change < 0;
              return (
                <TableRow key={row.categoryId} hover>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {row.categoryIcon && <Typography variant="body2">{row.categoryIcon}</Typography>}
                      <Typography variant="body2">{row.categoryName}</Typography>
                    </Box>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>{formatCurrency(row.currentAmount)}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" color="text.secondary">{formatCurrency(row.previousAmount)}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 0.5 }}>
                      {isUp && <ArrowUpIcon sx={{ fontSize: 14, color: 'error.main' }} />}
                      {isDown && <ArrowDownIcon sx={{ fontSize: 14, color: 'success.main' }} />}
                      <Typography variant="body2" sx={{ color: isUp ? 'error.main' : isDown ? 'success.main' : 'text.secondary' }}>
                        {row.changePct !== null ? `${isUp ? '+' : ''}${row.changePct.toFixed(1)}%` : 'New'}
                      </Typography>
                    </Box>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
