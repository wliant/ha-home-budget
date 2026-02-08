'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { Box, Typography, CircularProgress, Container } from '@mui/material';
import ExpenseListTable from '@/components/expenses/ExpenseListTable';
import ExpenseFilters from '@/components/expenses/ExpenseFilters';
import { expenseService } from '@/services/expenseService';
import type { ExpenseListResponse, ExpenseListFilters } from '@/services/expenseService';

const currentYear = new Date().getFullYear();

const defaultFilters: ExpenseListFilters = {
  year: currentYear,
  page: 0,
  size: 50,
  sortBy: 'expenseDate',
  sortDirection: 'DESC',
};

export default function ExpensesPage() {
  const [data, setData] = useState<ExpenseListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<ExpenseListFilters>(defaultFilters);
  const [page, setPage] = useState(0);

  const fetchExpenses = useCallback(async (currentFilters: ExpenseListFilters, pageNum: number) => {
    setLoading(true);
    try {
      const result = await expenseService.getExpenseList({
        ...currentFilters,
        page: pageNum,
      });
      setData(result);
    } catch (error) {
      console.error('Failed to fetch expenses:', error);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchExpenses(filters, page);
  }, [filters, page, fetchExpenses]);

  const handleFilterChange = (newFilters: ExpenseListFilters) => {
    setPage(0);
    setFilters(newFilters);
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handleSortChange = (newSortBy: string, newSortDirection: 'ASC' | 'DESC') => {
    setPage(0);
    setFilters((prev) => ({ ...prev, sortBy: newSortBy, sortDirection: newSortDirection }));
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box>
        <Typography variant="h5" sx={{ mb: 3 }}>
          Expenses
        </Typography>
        <ExpenseFilters filters={filters} onFilterChange={handleFilterChange} />
        {loading && !data && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        <ExpenseListTable
          data={data}
          loading={loading}
          page={page}
          onPageChange={handlePageChange}
          sortBy={filters.sortBy || 'expenseDate'}
          sortDirection={filters.sortDirection || 'DESC'}
          onSortChange={handleSortChange}
        />
      </Box>
    </Container>
  );
}
