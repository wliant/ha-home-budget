'use client';

import { useMemo, useState, useEffect } from 'react';
import {
  Autocomplete,
  TextField,
  CircularProgress,
  Box,
  Typography,
  Chip,
} from '@mui/material';
import * as Icons from '@mui/icons-material';
import { categoryService } from '@/services/categoryService';
import { expenseService, ExpenseDTO } from '@/services/expenseService';
import { CategoryDTO } from '@/types/category';

interface CategorySelectProps {
  value: number | null;
  onChange: (categoryId: number | null) => void;
  required?: boolean;
  error?: string;
  disabled?: boolean;
  year?: number;
}

export function CategorySelect({
  value,
  onChange,
  required = false,
  error,
  disabled = false,
  year,
}: CategorySelectProps) {
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);
  const [recentCategories, setRecentCategories] = useState<CategoryDTO[]>([]);
  const [topCategories, setTopCategories] = useState<CategoryDTO[]>([]);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsYear, setStatsYear] = useState<number | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const data = await categoryService.getAllCategories();
        setCategories(data);
        setFetchError(null);
      } catch (err) {
        console.error('Failed to fetch categories:', err);
        setFetchError('Failed to load categories');
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  const getCategoryLabel = (category: CategoryDTO): string => {
    if (category.parentCategory) {
      return `${category.parentCategory.name} > ${category.name}`;
    }
    return category.name;
  };

  // Render category icon (T015)
  const getCategoryIcon = (iconName?: string) => {
    if (!iconName) return null;

    // Convert snake_case or kebab-case to PascalCase for Material Icons
    const pascalCase = iconName
      .split(/[-_]/)
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join('');

    const IconComponent = (Icons as any)[pascalCase];
    return IconComponent ? <IconComponent fontSize="small" /> : null;
  };

  const selectedCategory = categories.find((c) => c.id === value) || null;
  const categoryById = useMemo(() => {
    const map = new Map<number, CategoryDTO>();
    categories.forEach((category) => map.set(category.id, category));
    return map;
  }, [categories]);

  useEffect(() => {
    if (categories.length === 0) {
      setRecentCategories([]);
      setTopCategories([]);
      return;
    }

    const fetchStats = async () => {
      try {
        setStatsLoading(true);
        const targetYear = year ?? new Date().getFullYear();
        const years = await expenseService.getExpenseYears();
        const hasTargetYear = years.includes(targetYear);
        const fallbackYear = years.length > 0 ? Math.max(...years) : targetYear;
        const effectiveYear = hasTargetYear ? targetYear : fallbackYear;
        const startDate = `${effectiveYear}-01-01`;
        const endDate = `${effectiveYear}-12-31`;
        const expenses = await expenseService.getAllExpenses({ startDate, endDate });

        const counts = new Map<number, number>();
        const sortedExpenses = [...expenses].sort((a, b) => {
          if (!a.expenseDate || !b.expenseDate) return 0;
          return a.expenseDate > b.expenseDate ? -1 : a.expenseDate < b.expenseDate ? 1 : 0;
        });

        const recent: CategoryDTO[] = [];
        const seenRecent = new Set<number>();

        sortedExpenses.forEach((expense: ExpenseDTO) => {
          if (!expense.categoryId) return;
          counts.set(expense.categoryId, (counts.get(expense.categoryId) ?? 0) + 1);
          if (seenRecent.has(expense.categoryId)) return;
          const category = categoryById.get(expense.categoryId);
          if (category) {
            recent.push(category);
            seenRecent.add(expense.categoryId);
          }
        });

        const top = [...counts.entries()]
          .sort((a, b) => b[1] - a[1])
          .map(([categoryId]) => categoryById.get(categoryId))
          .filter((category): category is CategoryDTO => Boolean(category))
          .slice(0, 5);

        setRecentCategories(recent.slice(0, 5));
        setTopCategories(top);
        setStatsYear(effectiveYear);
      } catch (err) {
        console.error('Failed to load category usage stats:', err);
        setRecentCategories([]);
        setTopCategories([]);
        setStatsYear(null);
      } finally {
        setStatsLoading(false);
      }
    };

    fetchStats();
  }, [categories, categoryById, year]);

  return (
    <>
      <Autocomplete
        options={categories}
        value={selectedCategory}
        onChange={(_, newValue) => onChange(newValue?.id ?? null)}
        getOptionLabel={getCategoryLabel}
        filterOptions={(options, state) => {
          const query = state.inputValue.trim().toLowerCase();
          if (!query) return options;
          return options.filter((option) => getCategoryLabel(option).toLowerCase().includes(query));
        }}
        loading={loading}
        disabled={disabled || loading || !!fetchError}
        ListboxProps={{ style: { maxHeight: 320 } }}
        // Render options with icons (T015)
        renderOption={(props, option) => (
          <Box component="li" {...props} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {getCategoryIcon(option.icon)}
            <Typography>{getCategoryLabel(option)}</Typography>
          </Box>
        )}
        renderInput={(params) => (
          <TextField
            {...params}
            label="Category"
            placeholder="Search categories..." // Search placeholder (T017)
            required={required}
            error={!!error || !!fetchError}
            helperText={error || fetchError}
            InputProps={{
              ...params.InputProps,
              startAdornment: (
                <>
                  {selectedCategory && getCategoryIcon(selectedCategory.icon)}
                  {params.InputProps.startAdornment}
                </>
              ),
              endAdornment: (
                <>
                  {loading ? <CircularProgress color="inherit" size={20} /> : null}
                  {params.InputProps.endAdornment}
                </>
              ),
            }}
          />
        )}
        fullWidth
      />
      {(recentCategories.length > 0 || topCategories.length > 0) && (
        <Box sx={{ mt: 1.5 }}>
          {recentCategories.length > 0 && (
            <Box sx={{ mb: 1 }}>
              <Typography variant="caption" color="text.secondary">
                Recently used
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75, mt: 0.5 }}>
                {recentCategories.map((category) => (
                  <Chip
                    key={`recent-${category.id}`}
                    size="small"
                    icon={getCategoryIcon(category.icon) ?? undefined}
                    label={category.name}
                    onClick={() => onChange(category.id)}
                    variant={category.id === value ? 'filled' : 'outlined'}
                    disabled={disabled || statsLoading}
                  />
                ))}
              </Box>
            </Box>
          )}
          {topCategories.length > 0 && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Top categories {statsYear ? `in ${statsYear}` : 'this year'}
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75, mt: 0.5 }}>
                {topCategories.map((category) => (
                  <Chip
                    key={`top-${category.id}`}
                    size="small"
                    icon={getCategoryIcon(category.icon) ?? undefined}
                    label={category.name}
                    onClick={() => onChange(category.id)}
                    variant={category.id === value ? 'filled' : 'outlined'}
                    disabled={disabled || statsLoading}
                  />
                ))}
              </Box>
            </Box>
          )}
        </Box>
      )}
    </>
  );
}
