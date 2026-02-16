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
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  ListSubheader,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  ChevronLeft,
  ChevronRight,
} from '@mui/icons-material';
import { expenseService, CategoryExpenseAggregate } from '@/services/expenseService';
import { categoryService } from '@/services/categoryService';
import type { CategoryDTO } from '@/types/category';
import SpendingTrendChart from '@/components/dashboard/SpendingTrendChart';
import type { CategoryGroupInfo } from '@/components/dashboard/SpendingTrendChart';
import BudgetAllocationChart from '@/components/dashboard/BudgetAllocationChart';
import type { BudgetAllocationItem } from '@/components/dashboard/BudgetAllocationChart';
import { budgetService, BudgetSummaryDTO } from '@/services/budgetService';

type Granularity = 'daily' | 'monthly' | 'yearly';

const MONTH_NAMES = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

const MONTH_FULL_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

function formatMonthLabel(value: unknown): string {
  const num = Number(value);
  if (num >= 1 && num <= 12) return MONTH_NAMES[num - 1];
  return String(value);
}

function formatDayLabel(value: unknown): string {
  return String(Number(value));
}

function formatYearLabel(value: unknown): string {
  return String(Number(value));
}

interface TransformedData {
  chartData: Record<string, unknown>[];
  categories: string[];
}

function transformAggregates(
  aggregates: CategoryExpenseAggregate[],
  granularity: Granularity,
  daysInMonth?: number,
  maxMonth?: number,
): TransformedData {
  const categories = new Set<string>();
  const bucketMap: Record<number, Record<string, number>> = {};

  for (const agg of aggregates) {
    categories.add(agg.categoryName);
    let bucket: number;
    if (granularity === 'daily') {
      bucket = agg.day ?? 0;
    } else if (granularity === 'monthly') {
      bucket = agg.month ?? 0;
    } else {
      bucket = agg.year;
    }
    if (!bucketMap[bucket]) bucketMap[bucket] = {};
    bucketMap[bucket][agg.categoryName] = agg.totalAmount;
  }

  const categoryList = Array.from(categories).sort();
  const chartData: Record<string, unknown>[] = [];

  if (granularity === 'daily') {
    const days = daysInMonth ?? 31;
    for (let d = 1; d <= days; d++) {
      const point: Record<string, unknown> = { day: d };
      for (const cat of categoryList) {
        point[cat] = bucketMap[d]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  } else if (granularity === 'monthly') {
    const endMonth = maxMonth ?? 12;
    for (let m = 1; m <= endMonth; m++) {
      const point: Record<string, unknown> = { month: m };
      for (const cat of categoryList) {
        point[cat] = bucketMap[m]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  } else {
    // Yearly: use only years that exist in data
    const years = Object.keys(bucketMap).map(Number).sort();
    for (const y of years) {
      const point: Record<string, unknown> = { year: y };
      for (const cat of categoryList) {
        point[cat] = bucketMap[y]?.[cat] ?? 0;
      }
      chartData.push(point);
    }
  }

  return { chartData, categories: categoryList };
}

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

export default function DashboardPage() {
  const now = new Date();
  const [granularity, setGranularity] = useState<Granularity>('monthly');
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [aggregates, setAggregates] = useState<CategoryExpenseAggregate[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [hiddenCategories, setHiddenCategories] = useState<Set<string>>(new Set());
  const [categoryHierarchy, setCategoryHierarchy] = useState<CategoryDTO[]>([]);

  // Budget allocation pie chart state
  type PieGranularity = 'yearly' | 'monthly';
  const [pieGranularity, setPieGranularity] = useState<PieGranularity>('monthly');
  const [pieYear, setPieYear] = useState(now.getFullYear());
  const [pieMonth, setPieMonth] = useState(now.getMonth() + 1);
  const [allBudgets, setAllBudgets] = useState<BudgetSummaryDTO[]>([]);
  const [pieBudgetsLoading, setPieBudgetsLoading] = useState(true);
  const [pieExpenseAggregates, setPieExpenseAggregates] = useState<CategoryExpenseAggregate[]>([]);
  const [pieExpenseLoading, setPieExpenseLoading] = useState(true);

  // Month trend across years state
  const [trendCategoryId, setTrendCategoryId] = useState<number | ''>('');
  const [trendYears, setTrendYears] = useState<number[]>([]);
  const [trendAggregatesMap, setTrendAggregatesMap] = useState<Record<number, CategoryExpenseAggregate[]>>({});
  const [trendHiddenYears, setTrendHiddenYears] = useState<Set<string>>(new Set());
  const [trendLoading, setTrendLoading] = useState(false);

  useEffect(() => {
    categoryService.getCategoryHierarchy()
      .then(setCategoryHierarchy)
      .catch((err) => console.error('Failed to load categories:', err));
  }, []);

  useEffect(() => {
    setPieBudgetsLoading(true);
    budgetService.getAllBudgets()
      .then(setAllBudgets)
      .catch((err) => console.error('Failed to load budgets:', err))
      .finally(() => setPieBudgetsLoading(false));
  }, []);

  // Load expense aggregates for pie chart when period changes
  useEffect(() => {
    setPieExpenseLoading(true);
    const loadExpensePie = async () => {
      try {
        let data: CategoryExpenseAggregate[];
        if (pieGranularity === 'monthly') {
          data = await expenseService.getMonthlyAggregates(pieYear, pieMonth);
        } else {
          data = await expenseService.getYearlyAggregates(pieYear);
        }
        setPieExpenseAggregates(data);
      } catch (err) {
        console.error('Failed to load expense aggregates for pie:', err);
      } finally {
        setPieExpenseLoading(false);
      }
    };
    loadExpensePie();
  }, [pieYear, pieMonth, pieGranularity]);

  // Load expense years for month trend
  useEffect(() => {
    expenseService.getExpenseYears()
      .then((years) => {
        setTrendYears(years);
        // Auto-select first parent category
        if (categoryHierarchy.length > 0 && trendCategoryId === '' && categoryHierarchy[0].id != null) {
          setTrendCategoryId(categoryHierarchy[0].id);
        }
      })
      .catch((err) => console.error('Failed to load expense years:', err));
  }, [categoryHierarchy]);

  // Load monthly aggregates for all years (for month trend chart)
  useEffect(() => {
    if (trendYears.length === 0) return;
    setTrendLoading(true);
    Promise.all(
      trendYears.map(async (year) => {
        const data = await expenseService.getMonthlyAggregates(year);
        return { year, data };
      })
    ).then((results) => {
      const map: Record<number, CategoryExpenseAggregate[]> = {};
      for (const { year, data } of results) {
        map[year] = data;
      }
      setTrendAggregatesMap(map);
    }).catch((err) => {
      console.error('Failed to load trend data:', err);
    }).finally(() => {
      setTrendLoading(false);
    });
  }, [trendYears]);

  const categoryGroups = useMemo<CategoryGroupInfo[]>(() => {
    return categoryHierarchy.map((parent) => ({
      parentName: parent.name,
      children: (parent.childCategories ?? []).map((c) => c.name),
    }));
  }, [categoryHierarchy]);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      let data: CategoryExpenseAggregate[];
      if (granularity === 'daily') {
        data = await expenseService.getDailyAggregates(selectedYear, selectedMonth);
      } else if (granularity === 'monthly') {
        data = await expenseService.getMonthlyAggregates(selectedYear);
      } else {
        // Yearly: get all years, then aggregate each
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

  useEffect(() => {
    loadData();
  }, [loadData]);

  const daysInMonth = useMemo(
    () => getDaysInMonth(selectedYear, selectedMonth),
    [selectedYear, selectedMonth]
  );

  // For monthly view of current year, limit to latest expense month + 1
  const maxMonth = useMemo(() => {
    if (granularity !== 'monthly' || selectedYear !== now.getFullYear()) return undefined;
    let latestMonth = 0;
    for (const agg of aggregates) {
      if (agg.month != null && agg.month > latestMonth) {
        latestMonth = agg.month;
      }
    }
    if (latestMonth === 0) return now.getMonth() + 1; // current month if no expenses
    return Math.min(latestMonth + 1, 12);
  }, [granularity, selectedYear, aggregates, now]);

  const { chartData, categories } = useMemo(
    () => transformAggregates(aggregates, granularity, daysInMonth, maxMonth),
    [aggregates, granularity, daysInMonth, maxMonth]
  );

  const handleGranularityChange = (_: React.MouseEvent<HTMLElement>, value: Granularity | null) => {
    if (value) {
      setGranularity(value);
    }
  };

  const handlePrev = () => {
    if (granularity === 'daily') {
      if (selectedMonth === 1) {
        setSelectedMonth(12);
        setSelectedYear((y) => y - 1);
      } else {
        setSelectedMonth((m) => m - 1);
      }
    } else if (granularity === 'monthly') {
      setSelectedYear((y) => y - 1);
    }
  };

  const handleNext = () => {
    if (granularity === 'daily') {
      if (selectedMonth === 12) {
        setSelectedMonth(1);
        setSelectedYear((y) => y + 1);
      } else {
        setSelectedMonth((m) => m + 1);
      }
    } else if (granularity === 'monthly') {
      setSelectedYear((y) => y + 1);
    }
  };

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

  const handleSelectAll = () => {
    setHiddenCategories(new Set());
  };

  const handleDeselectAll = () => {
    setHiddenCategories(new Set(categories));
  };

  const getPeriodLabel = (): string => {
    if (granularity === 'daily') {
      return `${MONTH_FULL_NAMES[selectedMonth - 1]} ${selectedYear}`;
    } else if (granularity === 'monthly') {
      return String(selectedYear);
    }
    return 'All Years';
  };

  const xAxisKey = granularity === 'daily' ? 'day' : granularity === 'monthly' ? 'month' : 'year';
  const xAxisFormatter = granularity === 'daily' ? formatDayLabel : granularity === 'monthly' ? formatMonthLabel : formatYearLabel;

  // Budget allocation pie chart data
  const { pieData, pieTotalBudget } = useMemo(() => {
    let filtered: BudgetSummaryDTO[];
    if (pieGranularity === 'yearly') {
      filtered = allBudgets.filter(b => b.year === pieYear && !b.month);
    } else {
      filtered = allBudgets.filter(b => b.year === pieYear && b.month === pieMonth);
    }

    const items: BudgetAllocationItem[] = filtered
      .filter(b => b.totalAmount > 0 && !b.category?.parentCategoryId)
      .map(b => ({
        name: b.category?.name ?? 'Unknown',
        icon: b.category?.icon,
        amount: b.totalAmount,
      }));

    const total = items.reduce((sum, item) => sum + item.amount, 0);
    return { pieData: items, pieTotalBudget: total };
  }, [allBudgets, pieGranularity, pieYear, pieMonth]);

  // Expense pie chart data
  const { expensePieData, expensePieTotal } = useMemo(() => {
    const parentOnly = pieExpenseAggregates.filter(agg => !agg.parentCategoryId);
    const items: BudgetAllocationItem[] = parentOnly
      .filter(agg => agg.totalAmount > 0)
      .map(agg => ({
        name: agg.categoryName,
        icon: agg.categoryIcon,
        amount: agg.totalAmount,
      }));
    const total = items.reduce((sum, item) => sum + item.amount, 0);
    return { expensePieData: items, expensePieTotal: total };
  }, [pieExpenseAggregates]);

  // Month trend chart data
  const { trendChartData, trendYearStrings } = useMemo(() => {
    if (trendCategoryId === '' || trendYears.length === 0) {
      return { trendChartData: [], trendYearStrings: [] };
    }

    // Find selected category name
    let selectedCatName = '';
    for (const parent of categoryHierarchy) {
      if (parent.id === trendCategoryId) {
        selectedCatName = parent.name;
        break;
      }
      for (const child of parent.childCategories ?? []) {
        if (child.id === trendCategoryId) {
          selectedCatName = child.name;
          break;
        }
      }
      if (selectedCatName) break;
    }

    const yearStrings = trendYears.map(String);
    const chartData: Record<string, unknown>[] = [];

    for (let m = 1; m <= 12; m++) {
      const point: Record<string, unknown> = { month: m };
      for (const year of trendYears) {
        const yearAggs = trendAggregatesMap[year] ?? [];
        const match = yearAggs.find(
          agg => agg.categoryName === selectedCatName && agg.month === m
        );
        point[String(year)] = match?.totalAmount ?? 0;
      }
      chartData.push(point);
    }

    return { trendChartData: chartData, trendYearStrings: yearStrings };
  }, [trendCategoryId, trendYears, trendAggregatesMap, categoryHierarchy]);

  const handleTrendToggleYear = (yearStr: string) => {
    setTrendHiddenYears((prev) => {
      const next = new Set(prev);
      if (next.has(yearStr)) next.delete(yearStr);
      else next.add(yearStr);
      return next;
    });
  };

  const handleTrendSelectAll = () => setTrendHiddenYears(new Set());
  const handleTrendDeselectAll = () => setTrendHiddenYears(new Set(trendYearStrings));

  const piePeriodLabel = pieGranularity === 'monthly'
    ? `${MONTH_FULL_NAMES[pieMonth - 1]} ${pieYear}`
    : String(pieYear);

  const handlePiePrev = () => {
    if (pieGranularity === 'monthly') {
      if (pieMonth === 1) {
        setPieMonth(12);
        setPieYear(y => y - 1);
      } else {
        setPieMonth(m => m - 1);
      }
    } else {
      setPieYear(y => y - 1);
    }
  };

  const handlePieNext = () => {
    if (pieGranularity === 'monthly') {
      if (pieMonth === 12) {
        setPieMonth(1);
        setPieYear(y => y + 1);
      } else {
        setPieMonth(m => m + 1);
      }
    } else {
      setPieYear(y => y + 1);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
        <DashboardIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Typography variant="h4" component="h1">
          Dashboard
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Paper sx={{ p: { xs: 2, sm: 3 } }}>
        <Box sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          alignItems: { xs: 'stretch', sm: 'center' },
          justifyContent: 'space-between',
          gap: 2,
          mb: 2,
        }}>
          <Typography variant="h6">
            Spending Trends
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

        {granularity !== 'yearly' && (
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
            <IconButton onClick={handlePrev} size="small">
              <ChevronLeft />
            </IconButton>
            <Typography variant="subtitle1" sx={{ minWidth: 150, textAlign: 'center' }}>
              {getPeriodLabel()}
            </Typography>
            <IconButton onClick={handleNext} size="small">
              <ChevronRight />
            </IconButton>
          </Box>
        )}

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <SpendingTrendChart
            chartData={chartData}
            xAxisKey={xAxisKey}
            categories={categories}
            xAxisFormatter={xAxisFormatter}
            granularity={granularity}
            hiddenCategories={hiddenCategories}
            onToggleCategory={handleToggleCategory}
            onSelectAll={handleSelectAll}
            onDeselectAll={handleDeselectAll}
            categoryGroups={categoryGroups}
            selectedYear={selectedYear}
            selectedMonth={selectedMonth}
          />
        )}
      </Paper>

      {/* Budget Allocation Pie Chart */}
      <Paper sx={{ p: { xs: 2, sm: 3 }, mt: 3 }}>
        <Box sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          alignItems: { xs: 'stretch', sm: 'center' },
          justifyContent: 'space-between',
          gap: 2,
          mb: 2,
        }}>
          <Typography variant="h6">
            Budget Allocation
          </Typography>

          <ToggleButtonGroup
            value={pieGranularity}
            exclusive
            onChange={(_, val) => { if (val) setPieGranularity(val); }}
            size="small"
          >
            <ToggleButton value="monthly">Monthly</ToggleButton>
            <ToggleButton value="yearly">Yearly</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
          <IconButton onClick={handlePiePrev} size="small">
            <ChevronLeft />
          </IconButton>
          <Typography variant="subtitle1" sx={{ minWidth: 150, textAlign: 'center' }}>
            {piePeriodLabel}
          </Typography>
          <IconButton onClick={handlePieNext} size="small">
            <ChevronRight />
          </IconButton>
        </Box>

        {pieBudgetsLoading && pieExpenseLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, gap: 2 }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" textAlign="center" color="text.secondary" sx={{ mb: 1 }}>
                Budget
              </Typography>
              <BudgetAllocationChart data={pieData} totalBudget={pieTotalBudget} label="total budget" />
            </Box>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" textAlign="center" color="text.secondary" sx={{ mb: 1 }}>
                Expenses
              </Typography>
              <BudgetAllocationChart data={expensePieData} totalBudget={expensePieTotal} label="total spent" />
            </Box>
          </Box>
        )}
      </Paper>
      {/* Monthly Trend Across Years */}
      <Paper sx={{ p: { xs: 2, sm: 3 }, mt: 3 }}>
        <Box sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          alignItems: { xs: 'stretch', sm: 'center' },
          justifyContent: 'space-between',
          gap: 2,
          mb: 2,
        }}>
          <Typography variant="h6">
            Monthly Trend Across Years
          </Typography>

          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Category</InputLabel>
            <Select
              value={trendCategoryId}
              label="Category"
              onChange={(e) => setTrendCategoryId(e.target.value as number)}
            >
              {categoryHierarchy.map((parent) => [
                <ListSubheader key={`header-${parent.id}`}>{parent.icon} {parent.name}</ListSubheader>,
                <MenuItem key={parent.id} value={parent.id} sx={{ pl: 3 }}>
                  {parent.icon} {parent.name} (all)
                </MenuItem>,
                ...(parent.childCategories ?? []).map((child) => (
                  <MenuItem key={child.id} value={child.id} sx={{ pl: 4 }}>
                    {child.icon} {child.name}
                  </MenuItem>
                )),
              ])}
            </Select>
          </FormControl>
        </Box>

        {trendLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
            <CircularProgress />
          </Box>
        ) : (
          <SpendingTrendChart
            chartData={trendChartData}
            xAxisKey="month"
            categories={trendYearStrings}
            xAxisFormatter={formatMonthLabel}
            granularity="monthly"
            hiddenCategories={trendHiddenYears}
            onToggleCategory={handleTrendToggleYear}
            onSelectAll={handleTrendSelectAll}
            onDeselectAll={handleTrendDeselectAll}
          />
        )}
      </Paper>
    </Container>
  );
}
