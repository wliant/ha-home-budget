'use client';

import React, { useEffect, useState, useCallback, useMemo, useRef } from 'react';
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
const PAGE_SIZE = 50;

const defaultFilters: ExpenseListFilters = {
  year: currentYear,
  page: 0,
  size: PAGE_SIZE,
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

  // Track whether we're in "fetch all" mode for client-side category filtering
  const hasCategoryFilter = selectedCategoryIds.size > 0;
  const prevHasCategoryFilter = useRef(hasCategoryFilter);

  useEffect(() => {
    userService.getCurrentUser().then(setCurrentUser).catch(() => {});
  }, []);

  // Re-fetch when toggling between chip filter on/off
  useEffect(() => {
    if (prevHasCategoryFilter.current !== hasCategoryFilter) {
      prevHasCategoryFilter.current = hasCategoryFilter;
      setPage(0);
    }
  }, [hasCategoryFilter]);

  const fetchExpenses = useCallback(async (currentFilters: ExpenseListFilters, pageNum: number, fetchAll: boolean) => {
    setLoading(true);
    try {
      const result = await expenseService.getExpenseList({
        ...currentFilters,
        // When category chips are active, fetch all data for client-side filtering
        page: fetchAll ? 0 : pageNum,
        size: fetchAll ? 10000 : PAGE_SIZE,
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
    fetchExpenses(filters, page, hasCategoryFilter);
  }, [filters, page, hasCategoryFilter, fetchExpenses]);

  // Client-side filter + pagination when category chips are active
  const filteredData = useMemo(() => {
    if (!data) return data;
    if (!hasCategoryFilter) return data;

    // Filter all fetched expenses by selected categories
    const allFiltered = data.content.filter(
      (expense) => expense.categoryId != null && selectedCategoryIds.has(expense.categoryId)
    );

    const totalAmount = allFiltered.reduce((sum, expense) => sum + expense.amount, 0);

    // Client-side pagination
    const start = page * PAGE_SIZE;
    const pageContent = allFiltered.slice(start, start + PAGE_SIZE);

    return {
      ...data,
      content: pageContent,
      totalElements: allFiltered.length,
      totalAmount,
      totalPages: Math.ceil(allFiltered.length / PAGE_SIZE),
      currentPage: page,
      pageSize: PAGE_SIZE,
    };
  }, [data, selectedCategoryIds, hasCategoryFilter, page]);

  const handleFilterChange = (newFilters: ExpenseListFilters) => {
    setPage(0);
    setFilters(newFilters);
  };

  const handleCategorySelectionChange = (ids: Set<number>) => {
    setSelectedCategoryIds(ids);
    setPage(0);
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
      fetchExpenses(filters, page, hasCategoryFilter);
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
          onCategorySelectionChange={handleCategorySelectionChange}
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
