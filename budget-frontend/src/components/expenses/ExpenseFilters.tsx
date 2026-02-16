'use client';

import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { Clear as ClearIcon } from '@mui/icons-material';
import { expenseService } from '@/services/expenseService';
import { categoryService } from '@/services/categoryService';
import type { ExpenseListFilters } from '@/services/expenseService';
import type { CategoryDTO } from '@/types/category';
import CategoryChipFilter from '@/components/CategoryChipFilter';

interface ExpenseFiltersProps {
  filters: ExpenseListFilters;
  onFilterChange: (filters: ExpenseListFilters) => void;
  selectedCategoryIds: Set<number>;
  onCategorySelectionChange: (ids: Set<number>) => void;
}

const currentYear = new Date().getFullYear();

const MONTHS = [
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

export default function ExpenseFilters({
  filters,
  onFilterChange,
  selectedCategoryIds,
  onCategorySelectionChange,
}: ExpenseFiltersProps) {
  const [availableYears, setAvailableYears] = useState<number[]>([currentYear]);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [creators, setCreators] = useState<string[]>([]);
  const [amountError, setAmountError] = useState<string | null>(null);

  useEffect(() => {
    const loadFilterOptions = async () => {
      try {
        const [years, cats, crtrs] = await Promise.all([
          expenseService.getExpenseYears(),
          categoryService.getCategoryHierarchy(),
          expenseService.getExpenseCreators(),
        ]);
        const yearSet = new Set([currentYear, ...years]);
        setAvailableYears(Array.from(yearSet).sort((a, b) => b - a));
        setCategories(cats);
        setCreators(crtrs);
      } catch (error) {
        console.error('Failed to load filter options:', error);
        setAvailableYears([currentYear]);
      }
    };
    loadFilterOptions();
  }, []);

  const handleYearChange = (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    onFilterChange({ ...filters, year: value === '' ? undefined : Number(value) });
  };

  const handleMonthChange = (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    onFilterChange({
      ...filters,
      month: value === '' ? undefined : Number(value),
    });
  };

  const handleCategorySelectionChange = (ids: Set<number>) => {
    onCategorySelectionChange(ids);
  };

  const handleCreatedByChange = (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    onFilterChange({
      ...filters,
      createdBy: value === '' ? undefined : value,
    });
  };

  const handleMinAmountChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    const minAmount = value === '' ? undefined : Number(value);
    const maxAmount = filters.maxAmount;

    if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
      setAmountError('Min must not exceed max');
    } else {
      setAmountError(null);
      onFilterChange({ ...filters, minAmount });
    }
  };

  const handleMaxAmountChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    const maxAmount = value === '' ? undefined : Number(value);
    const minAmount = filters.minAmount;

    if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
      setAmountError('Min must not exceed max');
    } else {
      setAmountError(null);
      onFilterChange({ ...filters, maxAmount });
    }
  };

  const handleClearFilters = () => {
    setAmountError(null);
    onFilterChange({
      year: currentYear,
      size: 50,
      sortBy: filters.sortBy,
      sortDirection: filters.sortDirection,
    });
    onCategorySelectionChange(new Set());
  };

  const hasOptionalFilters =
    filters.month != null ||
    selectedCategoryIds.size > 0 ||
    filters.minAmount != null ||
    filters.maxAmount != null ||
    filters.createdBy != null ||
    filters.year == null ||
    filters.year !== currentYear;

  return (
    <>
    <Box sx={{
      display: 'flex',
      flexWrap: 'wrap',
      gap: 2,
      mb: 2,
      alignItems: 'flex-start',
      '& > *': {
        minWidth: { xs: '100%', sm: 'auto' },
      },
    }}>
      <FormControl size="small" sx={{ minWidth: 100 }}>
        <InputLabel id="year-filter-label" shrink>Year</InputLabel>
        <Select
          labelId="year-filter-label"
          value={filters.year != null ? String(filters.year) : ''}
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

      <FormControl size="small" sx={{ minWidth: 130 }}>
        <InputLabel id="month-filter-label" shrink>
          Month
        </InputLabel>
        <Select
          labelId="month-filter-label"
          value={filters.month != null ? String(filters.month) : ''}
          onChange={handleMonthChange}
          label="Month"
          displayEmpty
        >
          <MenuItem value="">All months</MenuItem>
          {MONTHS.map((m) => (
            <MenuItem key={m.value} value={String(m.value)}>
              {m.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <TextField
        size="small"
        label="Min Amount"
        type="number"
        value={filters.minAmount ?? ''}
        onChange={handleMinAmountChange}
        error={!!amountError}
        inputProps={{ min: 0, step: '0.01' }}
        sx={{ minWidth: 120 }}
      />

      <TextField
        size="small"
        label="Max Amount"
        type="number"
        value={filters.maxAmount ?? ''}
        onChange={handleMaxAmountChange}
        error={!!amountError}
        helperText={amountError}
        inputProps={{ min: 0, step: '0.01' }}
        sx={{ minWidth: 120 }}
      />

      <FormControl size="small" sx={{ minWidth: 140 }}>
        <InputLabel id="created-by-filter-label" shrink>
          Created By
        </InputLabel>
        <Select
          labelId="created-by-filter-label"
          value={filters.createdBy ?? ''}
          onChange={handleCreatedByChange}
          label="Created By"
          displayEmpty
        >
          <MenuItem value="">All users</MenuItem>
          {creators.map((creator) => (
            <MenuItem key={creator} value={creator}>
              {creator}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {hasOptionalFilters && (
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

      {amountError && (
        <Typography variant="caption" color="error" sx={{ width: '100%' }}>
          {amountError}
        </Typography>
      )}
    </Box>
    {categories.length > 0 && (
      <Box sx={{ mb: 2 }}>
        <CategoryChipFilter
          categories={categories}
          selectedCategoryIds={selectedCategoryIds}
          onSelectionChange={handleCategorySelectionChange}
        />
      </Box>
    )}
    </>
  );
}
