'use client';

import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TableSortLabel,
  Paper,
  Typography,
  Box,
  Chip,
  Skeleton,
  Tooltip,
} from '@mui/material';
import type { ExpenseListResponse } from '@/services/expenseService';
import { formatExpenseDate, formatExpenseAmount } from '@/services/expenseService';

type SortDirection = 'ASC' | 'DESC';

interface ExpenseListTableProps {
  data: ExpenseListResponse | null;
  loading: boolean;
  page: number;
  onPageChange: (newPage: number) => void;
  sortBy: string;
  sortDirection: SortDirection;
  onSortChange: (sortBy: string, sortDirection: SortDirection) => void;
}

const TABLE_COLUMNS: { id: string; label: string; align?: 'right'; sortable?: boolean }[] = [
  { id: 'expenseDate', label: 'Date' },
  { id: 'description', label: 'Description' },
  { id: 'categoryName', label: 'Category' },
  { id: 'amount', label: 'Amount', align: 'right' },
  { id: 'files', label: 'Files' },
  { id: 'createdBy', label: 'Created By' },
];

export default function ExpenseListTable({
  data,
  loading,
  page,
  onPageChange,
  sortBy,
  sortDirection,
  onSortChange,
}: ExpenseListTableProps) {
  const handleChangePage = (_event: unknown, newPage: number) => {
    onPageChange(newPage);
  };

  const handleSortClick = (columnId: string) => {
    if (columnId === sortBy) {
      onSortChange(columnId, sortDirection === 'ASC' ? 'DESC' : 'ASC');
    } else {
      onSortChange(columnId, 'ASC');
    }
  };

  const muiDirection = (col: string): 'asc' | 'desc' =>
    col === sortBy ? (sortDirection === 'ASC' ? 'asc' : 'desc') : 'asc';

  // Summary bar
  const renderSummary = () => {
    if (!data) return null;
    return (
      <Box sx={{ px: 2, py: 1.5, backgroundColor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
        <Typography variant="body2" color="text.secondary">
          {data.totalElements} expense{data.totalElements !== 1 ? 's' : ''} totaling{' '}
          <strong>{formatExpenseAmount(data.totalAmount)}</strong>
        </Typography>
      </Box>
    );
  };

  // Loading skeleton
  if (loading && !data) {
    return (
      <Paper sx={{ width: '100%', overflow: 'hidden' }}>
        <Box sx={{ px: 2, py: 1.5 }}>
          <Skeleton width={250} />
        </Box>
        <TableContainer sx={{ overflowX: 'auto' }}>
          <Table size="small" sx={{ minWidth: 600 }}>
            <TableHead>
              <TableRow>
                {TABLE_COLUMNS.map((col) => (
                  <TableCell key={col.id} align={col.align}>
                    <Typography variant="subtitle2">{col.label}</Typography>
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  {Array.from({ length: 5 }).map((_, j) => (
                    <TableCell key={j}><Skeleton /></TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    );
  }

  // Empty state
  if (!data || data.content.length === 0) {
    return (
      <Paper sx={{ width: '100%', overflow: 'hidden' }}>
        {renderSummary()}
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No expenses found
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Try adjusting your filters or selecting a different year.
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      {renderSummary()}
      <TableContainer sx={{ overflowX: 'auto' }}>
        <Table size="small" sx={{ minWidth: 600 }}>
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.100' }}>
              {TABLE_COLUMNS.map((col) => (
                <TableCell key={col.id} align={col.align} sortDirection={col.id === sortBy ? muiDirection(col.id) : false}>
                  {col.id === 'files' ? (
                    <Typography variant="subtitle2">{col.label}</Typography>
                  ) : (
                    <TableSortLabel
                      active={col.id === sortBy}
                      direction={muiDirection(col.id)}
                      onClick={() => handleSortClick(col.id)}
                    >
                      <Typography variant="subtitle2">{col.label}</Typography>
                    </TableSortLabel>
                  )}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {data.content.map((expense) => (
              <TableRow key={expense.id} hover sx={{ '&:hover': { bgcolor: 'rgba(160,82,45,0.04)' } }}>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>
                  {formatExpenseDate(expense.expenseDate)}
                </TableCell>
                <TableCell>{expense.description}</TableCell>
                <TableCell>
                  {expense.categoryName ? (
                    <Chip
                      label={expense.categoryName}
                      icon={expense.categoryIcon ? <span>{expense.categoryIcon}</span> : undefined}
                      size="small"
                      variant="outlined"
                    />
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      Uncategorized
                    </Typography>
                  )}
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  {formatExpenseAmount(expense.amount)}
                </TableCell>
                <TableCell>
                  {expense.files && expense.files.length > 0 ? (
                    <Tooltip
                      title={expense.files.map((file) => file.originalFilename).join(', ')}
                      arrow
                    >
                      <Chip
                        label={`${expense.files.length} file${expense.files.length > 1 ? 's' : ''}`}
                        size="small"
                        variant="outlined"
                      />
                    </Tooltip>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      —
                    </Typography>
                  )}
                </TableCell>
                <TableCell>{expense.createdBy}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={data.totalElements}
        page={page}
        onPageChange={handleChangePage}
        rowsPerPage={data.pageSize}
        rowsPerPageOptions={[50]}
      />
    </Paper>
  );
}
