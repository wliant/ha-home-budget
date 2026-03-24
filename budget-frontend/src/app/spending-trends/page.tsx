'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Container,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Paper,
  ToggleButtonGroup,
  ToggleButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  FormControlLabel,
  Switch,
} from '@mui/material';
import { TrendingUp as TrendingUpIcon, Search as SearchIcon } from '@mui/icons-material';
import { expenseService, CategoryExpenseAggregate } from '@/services/expenseService';
import { categoryService } from '@/services/categoryService';
import type { CategoryDTO } from '@/types/category';
import SpendingTrendChart from '@/components/dashboard/SpendingTrendChart';
import type { CategoryGroupInfo } from '@/components/dashboard/SpendingTrendChart';
import CategoryChipFilter from '@/components/CategoryChipFilter';
import {
  Granularity,
  MONTH_FULL_NAMES,
  formatMonthLabel,
  formatDayLabel,
  formatYearLabel,
  transformAggregates,
  getDaysInMonth,
} from '@/utils/spendingTrends';

export default function SpendingTrendsPage() {
  const now = new Date();

  // Filter state
  const [granularity, setGranularity] = useState<Granularity>('monthly');
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<Set<number>>(new Set());

  // Data state
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [availableYears, setAvailableYears] = useState<number[]>([]);
  const [aggregates, setAggregates] = useState<CategoryExpenseAggregate[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [hasSearched, setHasSearched] = useState(false);

  const [aggregateTotal, setAggregateTotal] = useState(false);

  // Chart legend state
  const [hiddenCategories, setHiddenCategories] = useState<Set<string>>(new Set());

  // Load categories and available years on mount
  useEffect(() => {
    categoryService.getCategoryHierarchy()
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err));
  }, []);

  useEffect(() => {
    expenseService.getExpenseYears()
      .then((years) => {
        setAvailableYears(years);
        if (years.length > 0 && !years.includes(selectedYear)) {
          setSelectedYear(years[years.length - 1]);
        }
      })
      .catch((err) => console.error('Failed to load expense years:', err));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Reset search state when granularity changes
  const handleGranularityChange = (_: React.MouseEvent<HTMLElement>, value: Granularity | null) => {
    if (value) {
      setGranularity(value);
      setHasSearched(false);
      setAggregates([]);
      setHiddenCategories(new Set());
    }
  };

  // Build set of selected category names for filtering aggregates
  const selectedCategoryNames = useMemo(() => {
    if (selectedCategoryIds.size === 0) return null; // null = all
    const names = new Set<string>();
    for (const parent of categories) {
      if (parent.id != null && selectedCategoryIds.has(parent.id)) {
        names.add(parent.name);
      }
      for (const child of parent.childCategories ?? []) {
        if (child.id != null && selectedCategoryIds.has(child.id)) {
          names.add(child.name);
        }
      }
    }
    return names;
  }, [selectedCategoryIds, categories]);

  const handleSearch = useCallback(async () => {
    setIsLoading(true);
    setError('');
    setHasSearched(true);
    setHiddenCategories(new Set());
    try {
      let data: CategoryExpenseAggregate[];
      if (granularity === 'daily') {
        data = await expenseService.getDailyAggregates(selectedYear, selectedMonth);
      } else if (granularity === 'monthly') {
        data = await expenseService.getMonthlyAggregates(selectedYear);
      } else {
        const years = await expenseService.getExpenseYears();
        if (years.length === 0) {
          setAggregates([]);
          setIsLoading(false);
          return;
        }
        const allAggregates = await Promise.all(
          years.map((y) => expenseService.getYearlyAggregates(y))
        );
        data = allAggregates.flat();
      }
      setAggregates(data);
    } catch (err: unknown) {
      console.error('Failed to load spending data:', err);
      setError('Failed to load spending data');
    } finally {
      setIsLoading(false);
    }
  }, [granularity, selectedYear, selectedMonth]);

  // Filter aggregates by selected categories
  const filteredAggregates = useMemo(() => {
    if (!selectedCategoryNames) return aggregates;
    return aggregates.filter((agg) => selectedCategoryNames.has(agg.categoryName));
  }, [aggregates, selectedCategoryNames]);

  // Aggregate all categories into a single "Total" line when toggle is on
  // Parent categories already include child amounts, so skip children whose parent is also present
  const displayAggregates = useMemo(() => {
    if (!aggregateTotal) return filteredAggregates;
    // Find parent category names present in the data to avoid double-counting
    const parentNames = new Set(
      filteredAggregates.filter((a) => !a.parentCategoryId).map((a) => a.categoryName)
    );
    const bucketMap: Record<string, CategoryExpenseAggregate> = {};
    for (const agg of filteredAggregates) {
      // Skip child categories whose parent is already included (parent totalAmount includes children)
      if (agg.parentCategoryId) {
        const parent = categories.find((c) => c.id === agg.parentCategoryId);
        if (parent && parentNames.has(parent.name)) continue;
      }
      const key = `${agg.year}-${agg.month ?? 0}-${agg.day ?? 0}`;
      if (!bucketMap[key]) {
        bucketMap[key] = { ...agg, categoryName: 'Total', categoryId: -1, parentCategoryId: null, directAmount: 0, childrenAmount: 0, totalAmount: 0 };
      }
      bucketMap[key].totalAmount += agg.totalAmount;
    }
    return Object.values(bucketMap);
  }, [filteredAggregates, aggregateTotal, categories]);

  const daysInMonth = useMemo(
    () => getDaysInMonth(selectedYear, selectedMonth),
    [selectedYear, selectedMonth]
  );

  const { chartData, categories: chartCategories } = useMemo(
    () => transformAggregates(displayAggregates, granularity, daysInMonth),
    [displayAggregates, granularity, daysInMonth]
  );

  const categoryGroups = useMemo<CategoryGroupInfo[]>(() => {
    return categories.map((parent) => ({
      parentName: parent.name,
      children: (parent.childCategories ?? []).map((c) => c.name),
    }));
  }, [categories]);

  const xAxisKey = granularity === 'daily' ? 'day' : granularity === 'monthly' ? 'month' : 'year';
  const xAxisFormatter = granularity === 'daily' ? formatDayLabel : granularity === 'monthly' ? formatMonthLabel : formatYearLabel;

  const handleToggleCategory = (categoryName: string) => {
    setHiddenCategories((prev) => {
      const next = new Set(prev);
      if (next.has(categoryName)) {
        next.delete(categoryName);
      } else {
        next.add(categoryName);
      }
      return next;
    });
  };

  const handleSelectAll = () => setHiddenCategories(new Set());
  const handleDeselectAll = () => setHiddenCategories(new Set(chartCategories));

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        <TrendingUpIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Typography variant="h4" component="h1">
          Spending Trends
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Filter Controls */}
      <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Filters
        </Typography>

        {/* Granularity Toggle */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            View By
          </Typography>
          <ToggleButtonGroup
            value={granularity}
            exclusive
            onChange={handleGranularityChange}
            size="small"
          >
            <ToggleButton value="daily">Daily</ToggleButton>
            <ToggleButton value="monthly">Monthly</ToggleButton>
            <ToggleButton value="yearly">Yearly</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {/* Date Selectors */}
        {granularity !== 'yearly' && (
          <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Year</InputLabel>
              <Select
                value={selectedYear}
                label="Year"
                onChange={(e) => setSelectedYear(Number(e.target.value))}
              >
                {availableYears.map((year) => (
                  <MenuItem key={year} value={year}>{year}</MenuItem>
                ))}
              </Select>
            </FormControl>

            {granularity === 'daily' && (
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Month</InputLabel>
                <Select
                  value={selectedMonth}
                  label="Month"
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                >
                  {MONTH_FULL_NAMES.map((name, idx) => (
                    <MenuItem key={idx + 1} value={idx + 1}>{name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          </Box>
        )}

        {/* Category Filter */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Categories
          </Typography>
          <CategoryChipFilter
            categories={categories}
            selectedCategoryIds={selectedCategoryIds}
            onSelectionChange={setSelectedCategoryIds}
          />
        </Box>

        <FormControlLabel control={<Switch checked={aggregateTotal} onChange={(e) => setAggregateTotal(e.target.checked)} size="small" />} label="Aggregate total" sx={{ mb: 2, display: 'block' }} />

        {/* Search Button */}
        <Button
          variant="contained"
          startIcon={<SearchIcon />}
          onClick={handleSearch}
          disabled={isLoading}
          size="large"
        >
          Search
        </Button>
      </Paper>

      {/* Chart Area */}
      <Paper sx={{ p: { xs: 2, sm: 3 } }}>
        {!hasSearched ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <Typography variant="body1" color="text.secondary">
              Configure filters above and click Search to view spending trends.
            </Typography>
          </Box>
        ) : isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <SpendingTrendChart
            chartData={chartData}
            xAxisKey={xAxisKey}
            categories={chartCategories}
            xAxisFormatter={xAxisFormatter}
            granularity={granularity}
            hiddenCategories={hiddenCategories}
            onToggleCategory={handleToggleCategory}
            onSelectAll={handleSelectAll}
            onDeselectAll={handleDeselectAll}
            categoryGroups={aggregateTotal ? [] : categoryGroups}
            selectedYear={selectedYear}
            selectedMonth={selectedMonth}
            height={{ xs: 700, sm: 900 }}
          />
        )}
      </Paper>
    </Container>
  );
}
