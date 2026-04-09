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
  Remove as NoChangeIcon,
} from '@mui/icons-material';
import { formatCurrency, getMonthName } from '@/utils/formatters';
import type { CategoryExpenseAggregate } from '@/services/expenseService';

interface MonthOverMonthTableProps {
  currentMonth: { month: number; year: number; aggregates: CategoryExpenseAggregate[] };
  previousMonth: { month: number; year: number; aggregates: CategoryExpenseAggregate[] } | null;
  onCategoryClick?: (categoryId: number) => void;
  selectedCategoryId?: number | null;
}

interface MoMRow {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  currentAmount: number;
  previousAmount: number;
  changeDollar: number;
  changePercent: number | null;
  absChange: number;
}

export default function MonthOverMonthTable({
  currentMonth,
  previousMonth,
  onCategoryClick,
  selectedCategoryId,
}: MonthOverMonthTableProps) {
  const rows = useMemo(() => {
    const currentParents = currentMonth.aggregates.filter((a) => !a.parentCategoryId);
    const prevMap = new Map<number, number>();
    if (previousMonth) {
      for (const a of previousMonth.aggregates.filter((a) => !a.parentCategoryId)) {
        prevMap.set(a.categoryId, a.totalAmount);
      }
    }

    const result: MoMRow[] = currentParents.map((a) => {
      const prev = prevMap.get(a.categoryId) ?? 0;
      const change = a.totalAmount - prev;
      return {
        categoryId: a.categoryId,
        categoryName: a.categoryName,
        categoryIcon: a.categoryIcon,
        currentAmount: a.totalAmount,
        previousAmount: prev,
        changeDollar: change,
        changePercent: prev > 0 ? (change / prev) * 100 : null,
        absChange: Math.abs(change),
      };
    });

    // Include categories that existed last month but not this month
    if (previousMonth) {
      const currentIds = new Set(currentParents.map((a) => a.categoryId));
      for (const a of previousMonth.aggregates.filter((a) => !a.parentCategoryId)) {
        if (!currentIds.has(a.categoryId)) {
          result.push({
            categoryId: a.categoryId,
            categoryName: a.categoryName,
            categoryIcon: a.categoryIcon,
            currentAmount: 0,
            previousAmount: a.totalAmount,
            changeDollar: -a.totalAmount,
            changePercent: -100,
            absChange: a.totalAmount,
          });
        }
      }
    }

    return result.sort((a, b) => b.absChange - a.absChange);
  }, [currentMonth, previousMonth]);

  if (rows.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 6 }}>
        <Typography color="text.secondary">No spending data to compare.</Typography>
      </Box>
    );
  }

  const currentLabel = `${getMonthName(currentMonth.month)} ${currentMonth.year}`;
  const previousLabel = previousMonth
    ? `${getMonthName(previousMonth.month)} ${previousMonth.year}`
    : 'Previous';

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Category</TableCell>
            <TableCell align="right">{currentLabel}</TableCell>
            <TableCell align="right">{previousLabel}</TableCell>
            <TableCell align="right">Change ($)</TableCell>
            <TableCell align="right">Change (%)</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => {
            const isIncrease = row.changeDollar > 0;
            const isDecrease = row.changeDollar < 0;
            const isSignificant = row.changePercent !== null && Math.abs(row.changePercent) > 20;

            return (
              <TableRow
                key={row.categoryId}
                hover
                onClick={() => onCategoryClick?.(row.categoryId)}
                selected={selectedCategoryId === row.categoryId}
                sx={{
                  cursor: onCategoryClick ? 'pointer' : 'default',
                  bgcolor: isSignificant
                    ? isIncrease
                      ? 'rgba(192, 57, 43, 0.04)'
                      : 'rgba(74, 122, 73, 0.04)'
                    : undefined,
                }}
              >
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {row.categoryIcon && (
                      <Typography variant="body2">{row.categoryIcon}</Typography>
                    )}
                    <Typography variant="body2">{row.categoryName}</Typography>
                  </Box>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>
                    {formatCurrency(row.currentAmount)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" color="text.secondary">
                    {previousMonth ? formatCurrency(row.previousAmount) : '-'}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  {previousMonth ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 0.5 }}>
                      {isIncrease ? (
                        <ArrowUpIcon sx={{ fontSize: 16, color: 'error.main' }} />
                      ) : isDecrease ? (
                        <ArrowDownIcon sx={{ fontSize: 16, color: 'success.main' }} />
                      ) : (
                        <NoChangeIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                      )}
                      <Typography
                        variant="body2"
                        sx={{
                          fontWeight: 500,
                          color: isIncrease ? 'error.main' : isDecrease ? 'success.main' : 'text.secondary',
                        }}
                      >
                        {isIncrease ? '+' : ''}{formatCurrency(row.changeDollar)}
                      </Typography>
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">-</Typography>
                  )}
                </TableCell>
                <TableCell align="right">
                  {previousMonth && row.changePercent !== null ? (
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: isSignificant ? 600 : 400,
                        color: isIncrease ? 'error.main' : isDecrease ? 'success.main' : 'text.secondary',
                      }}
                    >
                      {isIncrease ? '+' : ''}{row.changePercent.toFixed(1)}%
                    </Typography>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      {previousMonth ? 'New' : '-'}
                    </Typography>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
