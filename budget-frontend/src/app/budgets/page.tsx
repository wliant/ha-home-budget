'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { Add as AddIcon, Clear as ClearIcon } from '@mui/icons-material';
import BudgetCard from '@/components/BudgetCard';
import { budgetService, BudgetSummaryDTO, formatBudgetPeriod } from '@/services/budgetService';
import { categoryService } from '@/services/categoryService';
import type { CategoryDTO } from '@/services/categoryService';

/**
 * Budgets list page - User Story 1: Create and View Budgets
 *
 * Features:
 * - Display all budgets in a grid layout
 * - Create new budget button
 * - View, edit, and delete actions for each budget
 * - Loading and error states
 * - Delete confirmation dialog
 */

export default function BudgetsPage() {
  const router = useRouter();
  const [budgets, setBudgets] = useState<BudgetSummaryDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [budgetToDelete, setBudgetToDelete] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [availableYears, setAvailableYears] = useState<number[]>([]);
  const [selectedYear, setSelectedYear] = useState<string>('');
  const [selectedMonth, setSelectedMonth] = useState<string>('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [selectedSort, setSelectedSort] = useState<string>('');
  const [selectedSortDirection, setSelectedSortDirection] = useState<'ASC' | 'DESC'>('DESC');

  const monthOptions = [
    { value: 1, label: 'January' },
    { value: 2, label: 'February' },
    { value: 3, label: 'March' },
    { value: 4, label: 'April' },
    { value: 5, label: 'May' },
    { value: 6, label: 'June' },
    { value: 7, label: 'July' },
    { value: 8, label: 'August' },
    { value: 9, label: 'September' },
    { value: 10, label: 'October' },
    { value: 11, label: 'November' },
    { value: 12, label: 'December' },
  ];

  const statusOptions = [
    { value: 'on-track', label: 'On track' },
    { value: 'good', label: 'Good' },
    { value: 'watch', label: 'Watch spending' },
    { value: 'near', label: 'Near limit' },
    { value: 'over', label: 'Over budget' },
  ];

  // Load budgets on mount
  useEffect(() => {
    loadBudgets();
  }, []);

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await categoryService.getCategoryHierarchy();
        setCategories(data);
      } catch (err: any) {
        console.error('Failed to load categories:', err);
      }
    };
    loadCategories();
  }, []);

  const loadBudgets = async () => {
    setIsLoading(true);
    setError('');

    try {
      const data = await budgetService.getAllBudgets();
      setBudgets(data);
      const years = Array.from(new Set(data.map((budget) => budget.year))).sort((a, b) => b - a);
      setAvailableYears(years);
    } catch (err: any) {
      console.error('Failed to load budgets:', err);
      setError('Failed to load budgets. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateNew = () => {
    router.push('/budgets/new');
  };

  const handleView = (id: number) => {
    router.push(`/budgets/${id}`);
  };

  const handleEdit = (id: number) => {
    router.push(`/budgets/${id}/edit`);
  };

  const handleDeleteClick = (id: number) => {
    setBudgetToDelete(id);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (budgetToDelete === null) return;

    setIsDeleting(true);

    try {
      await budgetService.deleteBudget(budgetToDelete);

      // Remove deleted budget from state
      setBudgets((prev) => prev.filter((b) => b.id !== budgetToDelete));

      // Close dialog
      setDeleteDialogOpen(false);
      setBudgetToDelete(null);
    } catch (err: any) {
      console.error('Failed to delete budget:', err);
      setError('Failed to delete budget. Please try again.');
      setDeleteDialogOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setBudgetToDelete(null);
  };

  const deletingBudget = budgets.find((b) => b.id === budgetToDelete);

  const getBudgetStatusKey = (percentage: number) => {
    if (percentage < 50) return 'on-track';
    if (percentage < 75) return 'good';
    if (percentage < 90) return 'watch';
    if (percentage < 100) return 'near';
    return 'over';
  };

  const getCategoryChildren = (category: CategoryDTO): CategoryDTO[] => {
    return category.childCategories ?? category.children ?? [];
  };

  const findCategoryById = (categoryList: CategoryDTO[], id: number): CategoryDTO | null => {
    for (const category of categoryList) {
      if (category.id === id) {
        return category;
      }
      const childMatch = findCategoryById(getCategoryChildren(category), id);
      if (childMatch) {
        return childMatch;
      }
    }
    return null;
  };

  const getDescendantIds = (category: CategoryDTO): number[] => {
    const children = getCategoryChildren(category);
    return children.flatMap((child) => [
      child.id!,
      ...getDescendantIds(child),
    ]);
  };

  const filteredBudgets = budgets.filter((budget) => {
    const yearMatches = selectedYear === '' || budget.year === Number(selectedYear);

    const monthMatches = selectedMonth === '' || budget.month === Number(selectedMonth);

    const statusMatches =
      selectedStatus === '' || getBudgetStatusKey(budget.spendingPercentage) === selectedStatus;

    const categoryMatches = (() => {
      if (selectedCategoryId === '') return true;
      if (!budget.categoryId) return false;
      const selectedId = Number(selectedCategoryId);
      const selectedCategory = findCategoryById(categories, selectedId);
      if (!selectedCategory) {
        return budget.categoryId === selectedId;
      }
      const allowedIds = new Set<number>([selectedId, ...getDescendantIds(selectedCategory)]);
      return allowedIds.has(budget.categoryId);
    })();

    return yearMatches && monthMatches && statusMatches && categoryMatches;
  });

  const getDateSortValue = (budget: BudgetSummaryDTO) => {
    const monthValue = budget.month ?? 0;
    return budget.year * 100 + monthValue;
  };

  const sortedBudgets = [...filteredBudgets].sort((a, b) => {
    if (selectedSort === 'available') {
      const diff = b.totalAmount - a.totalAmount;
      if (diff !== 0) return selectedSortDirection === 'ASC' ? -diff : diff;
    }

    if (selectedSort === 'remaining') {
      const remainingA = a.totalAmount - a.totalSpending;
      const remainingB = b.totalAmount - b.totalSpending;
      const diff = remainingB - remainingA;
      if (diff !== 0) return selectedSortDirection === 'ASC' ? -diff : diff;
    }

    return getDateSortValue(b) - getDateSortValue(a);
  });

  const categoryOptions: { id: number; label: string }[] = [];
  const buildCategoryOptions = (categoryList: CategoryDTO[], prefix: string) => {
    categoryList.forEach((category) => {
      if (category.id == null) return;
      const iconPrefix = category.icon ? `${category.icon} ` : '';
      categoryOptions.push({
        id: category.id,
        label: `${prefix}${iconPrefix}${category.name}`,
      });
      const children = getCategoryChildren(category);
      if (children.length > 0) {
        buildCategoryOptions(children, `${prefix}-- `);
      }
    });
  };

  buildCategoryOptions(categories, '');

  const handleYearChange = (event: SelectChangeEvent<string>) => {
    setSelectedYear(event.target.value);
  };

  const handleMonthChange = (event: SelectChangeEvent<string>) => {
    setSelectedMonth(event.target.value);
  };

  const handleCategoryChange = (event: SelectChangeEvent<string>) => {
    setSelectedCategoryId(event.target.value);
  };

  const handleStatusChange = (event: SelectChangeEvent<string>) => {
    setSelectedStatus(event.target.value);
  };

  const handleSortChange = (event: SelectChangeEvent<string>) => {
    setSelectedSort(event.target.value);
  };

  const handleSortDirectionChange = (event: SelectChangeEvent<string>) => {
    setSelectedSortDirection(event.target.value as 'ASC' | 'DESC');
  };

  const handleClearFilters = () => {
    setSelectedYear('');
    setSelectedMonth('');
    setSelectedCategoryId('');
    setSelectedStatus('');
    setSelectedSort('');
    setSelectedSortDirection('DESC');
  };

  const hasFilters =
    selectedYear !== '' ||
    selectedMonth !== '' ||
    selectedCategoryId !== '' ||
    selectedStatus !== '' ||
    selectedSort !== '';

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Budgets
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleCreateNew}
        >
          Create Budget
        </Button>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {/* Loading State */}
      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Empty State */}
      {!isLoading && budgets.length === 0 && (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No budgets yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Create your first budget to start tracking your expenses
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateNew}
          >
            Create Your First Budget
          </Button>
        </Box>
      )}

      {/* Filters */}
      {!isLoading && budgets.length > 0 && (
        <Box sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 2,
          mb: 3,
          alignItems: 'flex-start',
          '& > *': {
            minWidth: { xs: '100%', sm: 'auto' },
          },
        }}>
          <FormControl size="small" sx={{ minWidth: 130 }}>
            <InputLabel id="budget-year-filter-label" shrink>
              Year
            </InputLabel>
            <Select
              labelId="budget-year-filter-label"
              value={selectedYear}
              onChange={handleYearChange}
              label="Year"
              displayEmpty
            >
              <MenuItem value="">All years</MenuItem>
              {availableYears.map((year) => (
                <MenuItem key={year} value={String(year)}>
                  {year}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="budget-month-filter-label" shrink>
              Month
            </InputLabel>
            <Select
              labelId="budget-month-filter-label"
              value={selectedMonth}
              onChange={handleMonthChange}
              label="Month"
              displayEmpty
            >
              <MenuItem value="">All months</MenuItem>
              {monthOptions.map((option) => (
                <MenuItem key={option.value} value={String(option.value)}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="budget-category-filter-label" shrink>
              Category
            </InputLabel>
            <Select
              labelId="budget-category-filter-label"
              value={selectedCategoryId}
              onChange={handleCategoryChange}
              label="Category"
              displayEmpty
            >
              <MenuItem value="">All categories</MenuItem>
              {categoryOptions.map((option) => (
                <MenuItem key={option.id} value={String(option.id)}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 170 }}>
            <InputLabel id="budget-status-filter-label" shrink>
              Status
            </InputLabel>
            <Select
              labelId="budget-status-filter-label"
              value={selectedStatus}
              onChange={handleStatusChange}
              label="Status"
              displayEmpty
            >
              <MenuItem value="">All statuses</MenuItem>
              {statusOptions.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 190 }}>
            <InputLabel id="budget-sort-label" shrink>
              Sort By
            </InputLabel>
            <Select
              labelId="budget-sort-label"
              value={selectedSort}
              onChange={handleSortChange}
              label="Sort By"
              displayEmpty
            >
              <MenuItem value="">Date (latest first)</MenuItem>
              <MenuItem value="available">Available budget</MenuItem>
              <MenuItem value="remaining">Remaining budget</MenuItem>
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="budget-sort-direction-label" shrink>
              Direction
            </InputLabel>
            <Select
              labelId="budget-sort-direction-label"
              value={selectedSortDirection}
              onChange={handleSortDirectionChange}
              label="Direction"
              disabled={selectedSort === ''}
            >
              <MenuItem value="DESC">High to low</MenuItem>
              <MenuItem value="ASC">Low to high</MenuItem>
            </Select>
          </FormControl>

          {hasFilters && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<ClearIcon />}
              onClick={handleClearFilters}
              sx={{ height: 40 }}
            >
              Clear Filters
            </Button>
          )}
        </Box>
      )}

      {/* Budgets Grid */}
      {!isLoading && budgets.length > 0 && (
        <Grid container spacing={3}>
          {sortedBudgets.map((budget) => (
            <Grid item xs={12} sm={6} md={4} key={budget.id}>
              <BudgetCard
                budget={budget}
                onView={handleView}
                onEdit={handleEdit}
                onDelete={handleDeleteClick}
              />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">
          Delete Budget?
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete the budget for{' '}
            {deletingBudget && formatBudgetPeriod(deletingBudget.year, deletingBudget.month)}?
            {deletingBudget && deletingBudget.expenseCount > 0 && (
              <>
                <br /><br />
                <strong>Warning:</strong> This will also delete {deletingBudget.expenseCount}{' '}
                associated {deletingBudget.expenseCount === 1 ? 'expense' : 'expenses'}.
              </>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={isDeleting}>
            Cancel
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            disabled={isDeleting}
            autoFocus
          >
            {isDeleting ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
