'use client';

import React from 'react';
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
import { formatCurrency } from '@/utils/formatters';
import type { CategoryStats } from '@/services/expenseService';

interface CategoryStatsViewProps {
  stats: CategoryStats[];
  onCategoryClick?: (categoryId: number) => void;
  selectedCategoryId?: number | null;
}

export default function CategoryStatsView({ stats, onCategoryClick, selectedCategoryId }: CategoryStatsViewProps) {
  if (stats.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No expense statistics for this period.</Typography>
      </Box>
    );
  }

  const sorted = [...stats].sort((a, b) => b.totalAmount - a.totalAmount);

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Category</TableCell>
            <TableCell align="right">Count</TableCell>
            <TableCell align="right">Total</TableCell>
            <TableCell align="right">Average</TableCell>
            <TableCell align="right">Min</TableCell>
            <TableCell align="right">Max</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sorted.map((s) => (
            <TableRow
              key={s.categoryId}
              hover
              selected={selectedCategoryId === s.categoryId}
              onClick={() => onCategoryClick?.(s.categoryId)}
              sx={{ cursor: onCategoryClick ? 'pointer' : 'default' }}
            >
              <TableCell>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {s.categoryIcon && <Typography variant="body2">{s.categoryIcon}</Typography>}
                  <Typography variant="body2">{s.categoryName}</Typography>
                </Box>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2">{s.expenseCount}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" sx={{ fontWeight: 500 }}>{formatCurrency(s.totalAmount)}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2">{formatCurrency(s.averageAmount)}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">{formatCurrency(s.minAmount)}</Typography>
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">{formatCurrency(s.maxAmount)}</Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
