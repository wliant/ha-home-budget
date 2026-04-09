'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Container,
  Typography,
  Box,
  CircularProgress,
  Alert,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  IconButton,
} from '@mui/material';
import {
  Insights as InsightsIcon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
} from '@mui/icons-material';
import { expenseService, CategoryExpenseAggregate } from '@/services/expenseService';
import type { ExpenseDTO, UserSpendingAggregate, CategoryStats, DayOfWeekAggregate } from '@/services/expenseService';
import { budgetService, YearlyBudgetViewDTO } from '@/services/budgetService';
import { categoryService } from '@/services/categoryService';
import type { CategoryDTO } from '@/types/category';
import { getMonthName } from '@/utils/formatters';
import SpendingDistributionTreemap from '@/components/category-insights/SpendingDistributionTreemap';
import BudgetHealthBars from '@/components/category-insights/BudgetHealthBars';
import MonthOverMonthTable from '@/components/category-insights/MonthOverMonthTable';
import CategoryDeepDive from '@/components/category-insights/CategoryDeepDive';
import SpendingForecast from '@/components/category-insights/SpendingForecast';
import UserSpendingView from '@/components/category-insights/UserSpendingView';
import CategoryStatsView from '@/components/category-insights/CategoryStatsView';
import TopExpenses from '@/components/category-insights/TopExpenses';
import DayOfWeekChart from '@/components/category-insights/DayOfWeekChart';
import YearOverYearComparison from '@/components/category-insights/YearOverYearComparison';

export default function CategoryInsightsPage() {
  const now = new Date();

  // Period selection
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [availableYears, setAvailableYears] = useState<number[]>([]);

  // Core data (loaded on year change)
  const [monthlyAggregates, setMonthlyAggregates] = useState<CategoryExpenseAggregate[]>([]);
  const [yearlyBudgetView, setYearlyBudgetView] = useState<YearlyBudgetViewDTO | null>(null);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [prevYearAggregates, setPrevYearAggregates] = useState<CategoryExpenseAggregate[]>([]);
  const [prevYearLoaded, setPrevYearLoaded] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // New data (loaded on month change)
  const [userAggregates, setUserAggregates] = useState<UserSpendingAggregate[]>([]);
  const [categoryStats, setCategoryStats] = useState<CategoryStats[]>([]);
  const [dowAggregates, setDowAggregates] = useState<DayOfWeekAggregate[]>([]);
  const [topExpenses, setTopExpenses] = useState<ExpenseDTO[] | null>(null);
  const [topExpensesLoading, setTopExpensesLoading] = useState(false);

  // Deep-dive
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [deepDiveExpenses, setDeepDiveExpenses] = useState<ExpenseDTO[] | null>(null);
  const [deepDiveLoading, setDeepDiveLoading] = useState(false);

  // Load categories and years once
  useEffect(() => {
    categoryService.getCategoryHierarchy()
      .then(setCategories)
      .catch((err) => console.error('Failed to load categories:', err));

    expenseService.getExpenseYears()
      .then((years) => {
        setAvailableYears(years);
        if (years.length > 0 && !years.includes(now.getFullYear())) {
          setSelectedYear(years[years.length - 1]);
        }
      })
      .catch((err) => console.error('Failed to load years:', err));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Load core data when year changes
  useEffect(() => {
    setIsLoading(true);
    setError('');
    setSelectedCategoryId(null);

    Promise.all([
      expenseService.getMonthlyAggregates(selectedYear),
      budgetService.getYearlyBudgetView(selectedYear),
    ])
      .then(([aggs, budget]) => {
        setMonthlyAggregates(aggs);
        setYearlyBudgetView(budget);
      })
      .catch((err) => {
        console.error('Failed to load data:', err);
        setError('Failed to load category insight data.');
      })
      .finally(() => setIsLoading(false));
  }, [selectedYear]);

  // Load month-specific data
  useEffect(() => {
    if (isLoading) return;

    // Fire all month-dependent fetches in parallel
    Promise.all([
      expenseService.getUserSpendingAggregates(selectedYear, selectedMonth).catch(() => []),
      expenseService.getCategoryStats(selectedYear, selectedMonth).catch(() => []),
      expenseService.getDayOfWeekAggregates(selectedYear, selectedMonth).catch(() => []),
    ]).then(([users, stats, dow]) => {
      setUserAggregates(users);
      setCategoryStats(stats);
      setDowAggregates(dow);
    });

    // Top expenses
    setTopExpensesLoading(true);
    expenseService.getExpenseList({
      year: selectedYear,
      month: selectedMonth,
      size: 10,
      sortBy: 'amount',
      sortDirection: 'DESC',
    })
      .then((res) => setTopExpenses(res.content))
      .catch(() => setTopExpenses([]))
      .finally(() => setTopExpensesLoading(false));
  }, [selectedYear, selectedMonth, isLoading]);

  // Load previous year aggregates for MoM and YoY
  useEffect(() => {
    const prevYear = selectedYear - 1;
    if (prevYearLoaded === prevYear) return;
    expenseService.getMonthlyAggregates(prevYear)
      .then((aggs) => {
        setPrevYearAggregates(aggs);
        setPrevYearLoaded(prevYear);
      })
      .catch(() => {
        setPrevYearAggregates([]);
        setPrevYearLoaded(prevYear);
      });
  }, [selectedYear, prevYearLoaded]);

  // Load expenses when deep-dive category changes
  useEffect(() => {
    if (selectedCategoryId == null) {
      setDeepDiveExpenses(null);
      return;
    }

    setDeepDiveLoading(true);
    const categoryIds = [selectedCategoryId];
    for (const parent of categories) {
      if (parent.id === selectedCategoryId && parent.childCategories) {
        for (const child of parent.childCategories) {
          if (child.id != null) categoryIds.push(child.id);
        }
      }
    }

    expenseService
      .getExpenseList({
        categoryIds,
        year: selectedYear,
        month: selectedMonth,
        size: 5,
        sortBy: 'expenseDate',
        sortDirection: 'DESC',
      })
      .then((res) => setDeepDiveExpenses(res.content))
      .catch(() => setDeepDiveExpenses([]))
      .finally(() => setDeepDiveLoading(false));
  }, [selectedCategoryId, selectedYear, selectedMonth, categories]);

  // Computed data
  const currentMonthAggregates = useMemo(
    () => monthlyAggregates.filter((a) => a.month === selectedMonth),
    [monthlyAggregates, selectedMonth],
  );

  const previousMonthData = useMemo(() => {
    if (selectedMonth > 1) {
      return {
        month: selectedMonth - 1,
        year: selectedYear,
        aggregates: monthlyAggregates.filter((a) => a.month === selectedMonth - 1),
      };
    }
    if (prevYearAggregates.length > 0) {
      return {
        month: 12,
        year: selectedYear - 1,
        aggregates: prevYearAggregates.filter((a) => a.month === 12),
      };
    }
    return null;
  }, [selectedMonth, selectedYear, monthlyAggregates, prevYearAggregates]);

  const selectedCategory = useMemo(() => {
    if (!selectedCategoryId) return null;
    for (const parent of categories) {
      if (parent.id === selectedCategoryId) return parent;
      for (const child of parent.childCategories ?? []) {
        if (child.id === selectedCategoryId) return child;
      }
    }
    return null;
  }, [selectedCategoryId, categories]);

  const deepDiveBudgetCategory = useMemo(
    () => yearlyBudgetView?.categories.find((c) => c.categoryId === selectedCategoryId),
    [yearlyBudgetView, selectedCategoryId],
  );

  const deepDiveChildAggregates = useMemo(() => {
    if (!selectedCategoryId) return [];
    return currentMonthAggregates.filter((a) => a.parentCategoryId === selectedCategoryId);
  }, [currentMonthAggregates, selectedCategoryId]);

  const handleCategoryClick = useCallback((categoryId: number) => {
    setSelectedCategoryId((prev) => (prev === categoryId ? null : categoryId));
  }, []);

  const handleMonthPrev = () => {
    if (selectedMonth > 1) {
      setSelectedMonth(selectedMonth - 1);
    } else {
      const prevYear = selectedYear - 1;
      if (availableYears.includes(prevYear)) {
        setSelectedYear(prevYear);
        setSelectedMonth(12);
      }
    }
  };

  const handleMonthNext = () => {
    if (selectedMonth < 12) {
      setSelectedMonth(selectedMonth + 1);
    } else {
      const nextYear = selectedYear + 1;
      if (availableYears.includes(nextYear)) {
        setSelectedYear(nextYear);
        setSelectedMonth(1);
      }
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <InsightsIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1">
            Category Insights
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FormControl size="small" sx={{ minWidth: 100 }}>
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
          <IconButton size="small" onClick={handleMonthPrev}>
            <ChevronLeftIcon />
          </IconButton>
          <Typography variant="h6" sx={{ minWidth: 100, textAlign: 'center' }}>
            {getMonthName(selectedMonth)}
          </Typography>
          <IconButton size="small" onClick={handleMonthNext}>
            <ChevronRightIcon />
          </IconButton>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          {/* Spending Forecast */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Spending Forecast</Typography>
            <SpendingForecast
              currentMonthAggregates={currentMonthAggregates}
              yearlyBudgetView={yearlyBudgetView}
              selectedMonth={selectedMonth}
              selectedYear={selectedYear}
            />
          </Paper>

          {/* Spending Distribution */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Spending Distribution</Typography>
            <SpendingDistributionTreemap
              aggregates={currentMonthAggregates}
              onCategoryClick={handleCategoryClick}
              selectedCategoryId={selectedCategoryId}
            />
          </Paper>

          {/* Budget Health */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Budget Health</Typography>
            {yearlyBudgetView ? (
              <BudgetHealthBars
                budgetCategories={yearlyBudgetView.categories}
                selectedMonth={selectedMonth}
                onCategoryClick={handleCategoryClick}
                selectedCategoryId={selectedCategoryId}
              />
            ) : (
              <Typography color="text.secondary">No budget data available.</Typography>
            )}
          </Paper>

          {/* Per-User Spending */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Household Member Spending</Typography>
            <UserSpendingView aggregates={userAggregates} />
          </Paper>

          {/* Category Statistics */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Category Statistics</Typography>
            <CategoryStatsView
              stats={categoryStats}
              onCategoryClick={handleCategoryClick}
              selectedCategoryId={selectedCategoryId}
            />
          </Paper>

          {/* Top Expenses */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Top 10 Expenses</Typography>
            <TopExpenses expenses={topExpenses} loading={topExpensesLoading} />
          </Paper>

          {/* Month-over-Month */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Month-over-Month Changes</Typography>
            <MonthOverMonthTable
              currentMonth={{ month: selectedMonth, year: selectedYear, aggregates: currentMonthAggregates }}
              previousMonth={previousMonthData}
              onCategoryClick={handleCategoryClick}
              selectedCategoryId={selectedCategoryId}
            />
          </Paper>

          {/* Year-over-Year */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Year-over-Year Comparison</Typography>
            <YearOverYearComparison
              currentYearAggregates={monthlyAggregates}
              previousYearAggregates={prevYearAggregates}
              selectedMonth={selectedMonth}
              selectedYear={selectedYear}
            />
          </Paper>

          {/* Day-of-Week Patterns */}
          <Paper sx={{ p: { xs: 2, sm: 3 }, mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>Spending by Day of Week</Typography>
            <DayOfWeekChart aggregates={dowAggregates} />
          </Paper>

          {/* Category Deep-Dive */}
          {selectedCategoryId != null && selectedCategory && (
            <CategoryDeepDive
              categoryId={selectedCategoryId}
              categoryName={selectedCategory.name}
              categoryIcon={selectedCategory.icon}
              monthlyAggregates={monthlyAggregates}
              yearlyBudgetCategory={deepDiveBudgetCategory}
              childAggregates={deepDiveChildAggregates}
              recentExpenses={deepDiveExpenses}
              expensesLoading={deepDiveLoading}
              selectedYear={selectedYear}
              selectedMonth={selectedMonth}
              onClose={() => setSelectedCategoryId(null)}
            />
          )}
        </>
      )}
    </Container>
  );
}
