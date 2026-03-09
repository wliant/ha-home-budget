'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { useIngressRouter } from '../../lib/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { Add as AddIcon, Clear as ClearIcon, AccountBalanceWallet as WalletIcon } from '@mui/icons-material';
import { budgetService, BudgetSummaryDTO, formatBudgetPeriod } from '@/services/budgetService';
import { categoryService } from '@/services/categoryService';
import type { CategoryDTO } from '@/services/categoryService';
import CategoryChipFilter from '@/components/CategoryChipFilter';
import BudgetGroup from './components/BudgetGroup';
import type { CategoryBudgetGroup } from './components/BudgetGroup';
import { MONTHS } from '@/utils/constants';

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
  const router = useIngressRouter();
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
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<Set<number>>(new Set());
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [selectedSort, setSelectedSort] = useState<string>('');
  const [selectedSortDirection, setSelectedSortDirection] = useState<'ASC' | 'DESC'>('DESC');

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

  const filteredBudgets = budgets.filter((budget) => {
    const yearMatches = selectedYear === '' || budget.year === Number(selectedYear);

    const monthMatches = selectedMonth === '' || budget.month === Number(selectedMonth);

    const statusMatches =
      selectedStatus === '' || getBudgetStatusKey(budget.spendingPercentage) === selectedStatus;

    const categoryMatches = (() => {
      if (selectedCategoryIds.size === 0) return true;
      if (!budget.categoryId) return false;
      return selectedCategoryIds.has(budget.categoryId);
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

  const handleCategorySelectionChange = (ids: Set<number>) => {
    setSelectedCategoryIds(ids);
  };

  const handleYearChange = (event: SelectChangeEvent<string>) => {
    setSelectedYear(event.target.value);
  };

  const handleMonthChange = (event: SelectChangeEvent<string>) => {
    setSelectedMonth(event.target.value);
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
    setSelectedCategoryIds(new Set());
    setSelectedStatus('');
    setSelectedSort('');
    setSelectedSortDirection('DESC');
  };

  const hasFilters =
    selectedYear !== '' ||
    selectedMonth !== '' ||
    selectedCategoryIds.size > 0 ||
    selectedStatus !== '' ||
    selectedSort !== '';

  // Group budgets by year, then by category hierarchy
  const groupedByYear = useMemo(() => {
    // 1. Group by year
    const yearMap = new Map<number, BudgetSummaryDTO[]>();
    for (const budget of filteredBudgets) {
      const list = yearMap.get(budget.year) || [];
      list.push(budget);
      yearMap.set(budget.year, list);
    }

    // 2. For each year, build category groups
    const result: { year: number; groups: CategoryBudgetGroup[] }[] = [];

    const sortedYears = [...yearMap.keys()].sort((a, b) => b - a);
    for (const year of sortedYears) {
      const yearBudgets = yearMap.get(year)!;

      // Group by categoryId
      const catMap = new Map<number, { yearly?: BudgetSummaryDTO; monthly: BudgetSummaryDTO[] }>();
      for (const b of yearBudgets) {
        const catId = b.categoryId ?? 0;
        const entry = catMap.get(catId) || { monthly: [] };
        if (!b.month) {
          entry.yearly = b;
        } else {
          entry.monthly.push(b);
        }
        catMap.set(catId, entry);
      }

      // Build CategoryBudgetGroup for each category
      const catGroupMap = new Map<number, CategoryBudgetGroup>();
      for (const [catId, entry] of catMap) {
        const sampleBudget = entry.yearly || entry.monthly[0];
        const cat = sampleBudget?.category;
        catGroupMap.set(catId, {
          categoryId: catId,
          categoryName: cat?.name ?? 'Unknown',
          categoryIcon: cat?.icon,
          parentCategoryId: cat?.parentCategoryId,
          yearlyBudget: entry.yearly,
          monthlyBudgets: entry.monthly,
          childCategoryGroups: [],
        });
      }

      // Nest child categories under their parent
      const rootGroups: CategoryBudgetGroup[] = [];
      for (const [catId, group] of catGroupMap) {
        const parentId = group.parentCategoryId;
        if (parentId && catGroupMap.has(parentId)) {
          catGroupMap.get(parentId)!.childCategoryGroups.push(group);
        } else {
          rootGroups.push(group);
        }
      }

      // Sort: groups with children first, then alphabetically
      rootGroups.sort((a, b) => {
        const aHasChildren = a.childCategoryGroups.length > 0 ? 0 : 1;
        const bHasChildren = b.childCategoryGroups.length > 0 ? 0 : 1;
        if (aHasChildren !== bHasChildren) return aHasChildren - bHasChildren;
        return a.categoryName.localeCompare(b.categoryName);
      });

      result.push({ year, groups: rootGroups });
    }

    return result;
  }, [filteredBudgets]);

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <WalletIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1">
            Budgets
          </Typography>
        </Box>
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
              {MONTHS.map((option) => (
                <MenuItem key={option.value} value={String(option.value)}>
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

      {/* Category Filter Chips */}
      {!isLoading && budgets.length > 0 && categories.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <CategoryChipFilter
            categories={categories}
            selectedCategoryIds={selectedCategoryIds}
            onSelectionChange={handleCategorySelectionChange}
          />
        </Box>
      )}

      {/* Budgets Grouped by Year & Category */}
      {!isLoading && budgets.length > 0 && (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {groupedByYear.map(({ year, groups }) => (
            <Box key={year}>
              <Divider sx={{ mb: 2 }}>
                <Typography variant="h5" fontWeight={700} color="primary">
                  {year}
                </Typography>
              </Divider>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {groups.map((group) => (
                  <BudgetGroup
                    key={group.categoryId}
                    group={group}
                    onView={handleView}
                    onEdit={handleEdit}
                    onDelete={handleDeleteClick}
                  />
                ))}
              </Box>
            </Box>
          ))}
        </Box>
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
