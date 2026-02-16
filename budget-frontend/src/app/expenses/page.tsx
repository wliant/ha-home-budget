'use client';

import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'next/navigation';
import { Box, Typography, CircularProgress, Container } from '@mui/material';
import { ReceiptLong as ReceiptLongIcon } from '@mui/icons-material';
import ExpenseListTable from '@/components/expenses/ExpenseListTable';
import ExpenseFilters from '@/components/expenses/ExpenseFilters';
import { expenseService } from '@/services/expenseService';
import { userService } from '@/services/api';
import { useIngressRouter } from '@/lib/navigation';
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
  const router = useIngressRouter();
  const searchParams = useSearchParams();

  const initialFilters = (): ExpenseListFilters => {
    const f = { ...defaultFilters };
    const yearParam = searchParams.get('year');
    if (yearParam) f.year = Number(yearParam);
    const monthParam = searchParams.get('month');
    if (monthParam) f.month = Number(monthParam);
    const categoryParam = searchParams.get('categoryId');
    if (categoryParam) f.categoryId = Number(categoryParam);
    return f;
  };

  const [data, setData] = useState<ExpenseListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<ExpenseListFilters>(initialFilters);
  const [page, setPage] = useState(0);
  const [currentUser, setCurrentUser] = useState<string>('');
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    userService.getCurrentUser().then(setCurrentUser).catch(() => {});
  }, []);

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

  // Client-side filter by selected categories
  const filteredData = useMemo(() => {
    if (!data || selectedCategoryIds.size === 0) return data;
    const filtered = data.content.filter(
      (expense) => expense.categoryId != null && selectedCategoryIds.has(expense.categoryId)
    );
    return {
      ...data,
      content: filtered,
      totalElements: filtered.length,
    };
  }, [data, selectedCategoryIds]);

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

  const handleEdit = (id: number) => {
    router.push(`/expenses/${id}/edit`);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this expense?')) return;
    try {
      await expenseService.deleteExpense(id);
      fetchExpenses(filters, page);
    } catch (error) {
      console.error('Failed to delete expense:', error);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
          <ReceiptLongIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h5">
            Expenses
          </Typography>
        </Box>
        <ExpenseFilters
          filters={filters}
          onFilterChange={handleFilterChange}
          selectedCategoryIds={selectedCategoryIds}
          onCategorySelectionChange={setSelectedCategoryIds}
        />
        {loading && !data && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        <ExpenseListTable
          data={filteredData}
          loading={loading}
          page={page}
          onPageChange={handlePageChange}
          sortBy={filters.sortBy || 'expenseDate'}
          sortDirection={filters.sortDirection || 'DESC'}
          onSortChange={handleSortChange}
          currentUser={currentUser}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </Box>
    </Container>
  );
}
