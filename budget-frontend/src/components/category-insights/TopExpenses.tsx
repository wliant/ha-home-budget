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
  Chip,
  CircularProgress,
} from '@mui/material';
import { formatCurrency, formatExpenseDate } from '@/utils/formatters';
import type { ExpenseDTO } from '@/services/expenseService';

interface TopExpensesProps {
  expenses: ExpenseDTO[] | null;
  loading: boolean;
}

export default function TopExpenses({ expenses, loading }: TopExpensesProps) {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (!expenses || expenses.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No expenses for this period.</Typography>
      </Box>
    );
  }

  return (
    <TableContainer>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>Date</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Category</TableCell>
            <TableCell align="right">Amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {expenses.map((exp, i) => (
            <TableRow key={exp.id} hover>
              <TableCell>
                <Typography variant="body2" color="text.secondary">{i + 1}</Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2">{formatExpenseDate(exp.expenseDate)}</Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" noWrap sx={{ maxWidth: 250 }}>{exp.description}</Typography>
              </TableCell>
              <TableCell>
                <Chip
                  size="small"
                  label={`${exp.categoryIcon ?? ''} ${exp.categoryName ?? ''}`}
                  variant="outlined"
                />
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" sx={{ fontWeight: 600 }}>{formatCurrency(exp.amount)}</Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
