'use client';

import { Fragment, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Container,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { CalendarMonth as CalendarIcon } from '@mui/icons-material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import { budgetService, formatCurrency, YearlyBudgetViewDTO, YearlyCategoryBudgetDTO } from '@/services/budgetService';
import { useIngressRouter } from '@/lib/navigation';

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function getSpendingColor(spent: number, budget: number): string {
  if (spent < 0) return 'success.main';
  if (budget <= 0) return spent > 0 ? 'error.main' : 'text.primary';
  const ratio = spent / budget;
  if (ratio >= 1) return 'error.main';
  if (ratio >= 0.9) return 'warning.main';
  if (ratio < 0.5) return 'success.main';
  return 'text.primary';
}

interface CategoryGroup {
  parent: YearlyCategoryBudgetDTO;
  children: YearlyCategoryBudgetDTO[];
}

export default function YearlyBudgetPage() {
  const router = useIngressRouter();
  const currentYear = new Date().getFullYear();
  const minYear = 2019;
  const maxYear = 2200;
  const [year, setYear] = useState(currentYear);
  const [yearInput, setYearInput] = useState(String(currentYear));
  const [data, setData] = useState<YearlyBudgetViewDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedGroups, setExpandedGroups] = useState<Set<number>>(new Set());
  const [selectedCategories, setSelectedCategories] = useState<YearlyCategoryBudgetDTO[]>([]);
  const [hiddenMonths, setHiddenMonths] = useState<Set<number>>(() => {
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem('yearlyView.hiddenMonths');
        if (stored) return new Set(JSON.parse(stored));
      } catch {}
    }
    return new Set();
  });

  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('yearlyView.hiddenMonths', JSON.stringify([...hiddenMonths]));
    }
  }, [hiddenMonths]);

  const toggleMonth = (month: number) => {
    setHiddenMonths(prev => {
      const next = new Set(prev);
      if (next.has(month)) {
        next.delete(month);
      } else {
        next.add(month);
      }
      return next;
    });
  };

  const yearOptions = useMemo(
    () => Array.from({ length: maxYear - minYear + 1 }, (_, i) => String(minYear + i)),
    [minYear, maxYear],
  );

  useEffect(() => {
    setYearInput(String(year));
    const load = async () => {
      setIsLoading(true);
      setError('');
      try {
        const response = await budgetService.getYearlyBudgetView(year);
        setData(response);
        // Expand all parent groups by default
        const parentIds = new Set<number>();
        response.categories.forEach(cat => {
          if (!cat.parentCategoryId) {
            const hasChildren = response.categories.some(c => c.parentCategoryId === cat.categoryId);
            if (hasChildren) {
              parentIds.add(cat.categoryId);
            }
          }
        });
        setExpandedGroups(parentIds);
      } catch (err) {
        console.error('Failed to load yearly budget view', err);
        setError('Failed to load yearly budget view. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [year]);

  const handleYearInputChange = (value: string) => {
    const digitsOnly = value.replace(/\D/g, '');
    const normalized = digitsOnly.length > 4 ? digitsOnly.slice(-4) : digitsOnly;
    setYearInput(normalized);
    if (normalized.length !== 4) return;
    const parsed = Number(normalized);
    if (!Number.isInteger(parsed)) return;
    if (parsed < minYear || parsed > maxYear) return;
    setYear(parsed);
  };

  const yearInputError = (() => {
    if (!yearInput) return '';
    const parsed = Number(yearInput);
    if (!Number.isInteger(parsed)) return 'Enter a valid year';
    if (parsed < minYear || parsed > maxYear) return `Year must be between ${minYear} and ${maxYear}`;
    return '';
  })();

  // Group categories by parent
  const { groups, filterOptions } = useMemo(() => {
    if (!data) return { groups: [], filterOptions: [] };

    const categories = data.categories;
    const childMap = new Map<number, YearlyCategoryBudgetDTO[]>();
    const rootCategories: YearlyCategoryBudgetDTO[] = [];

    categories.forEach(cat => {
      if (cat.parentCategoryId) {
        const siblings = childMap.get(cat.parentCategoryId) || [];
        siblings.push(cat);
        childMap.set(cat.parentCategoryId, siblings);
      } else {
        rootCategories.push(cat);
      }
    });

    const result: CategoryGroup[] = rootCategories.map(root => ({
      parent: root,
      children: childMap.get(root.categoryId) || [],
    }));

    return { groups: result, filterOptions: rootCategories };
  }, [data]);

  // Apply category filter
  const filteredGroups = useMemo(() => {
    if (selectedCategories.length === 0) return groups;
    const selectedIds = new Set(selectedCategories.map(c => c.categoryId));
    return groups.filter(g => selectedIds.has(g.parent.categoryId));
  }, [groups, selectedCategories]);

  const goToExpenses = (categoryId: number, month?: number) => {
    const params = new URLSearchParams({ year: String(year), categoryId: String(categoryId) });
    if (month != null) params.set('month', String(month));
    router.push(`/expenses?${params.toString()}`);
  };

  const toggleGroup = (categoryId: number) => {
    setExpandedGroups(prev => {
      const next = new Set(prev);
      if (next.has(categoryId)) {
        next.delete(categoryId);
      } else {
        next.add(categoryId);
      }
      return next;
    });
  };

  const renderMonthCells = (category: YearlyCategoryBudgetDTO, bold: boolean) =>
    category.months
      .filter((month) => !hiddenMonths.has(month.month))
      .map((month) => (
        <TableCell key={month.month} align="center">
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
            <Typography variant="caption" fontWeight={bold ? 600 : 400}>
              {month.hasBudget ? formatCurrency(month.budgetAmount) : '—'}
            </Typography>
            {month.spending !== 0 ? (
              <Typography
                variant="caption"
                color={month.hasBudget ? getSpendingColor(month.spending, month.budgetAmount) : 'text.secondary'}
                fontWeight={month.hasBudget && month.spending >= month.budgetAmount ? 700 : bold ? 600 : 400}
                onClick={(e) => { e.stopPropagation(); goToExpenses(category.categoryId, month.month); }}
                sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
              >
                {formatCurrency(month.spending)}
              </Typography>
            ) : (
              <Typography variant="caption" color="text.secondary">&nbsp;</Typography>
            )}
          </Box>
        </TableCell>
      ));

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <CalendarIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1">
            Yearly Budget View
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <Autocomplete
            multiple
            size="small"
            options={filterOptions}
            value={selectedCategories}
            onChange={(_, newValue) => setSelectedCategories(newValue)}
            getOptionLabel={(option) =>
              option.categoryIcon ? `${option.categoryIcon} ${option.categoryName}` : option.categoryName
            }
            isOptionEqualToValue={(option, value) => option.categoryId === value.categoryId}
            disableCloseOnSelect
            renderOption={(props, option, { selected }) => (
              <li {...props}>
                <Checkbox
                  icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
                  checkedIcon={<CheckBoxIcon fontSize="small" />}
                  style={{ marginRight: 8 }}
                  checked={selected}
                />
                {option.categoryIcon ? `${option.categoryIcon} ` : ''}{option.categoryName}
              </li>
            )}
            renderInput={(params) => (
              <TextField {...params} label="Filter Categories" placeholder="All categories" />
            )}
            sx={{ minWidth: 280 }}
          />
          <Autocomplete
            size="small"
            freeSolo
            options={yearOptions}
            value={String(year)}
            inputValue={yearInput}
            onInputChange={(_, newValue, reason) => {
              if (reason === 'reset') return;
              handleYearInputChange(newValue);
            }}
            onChange={(_, newValue) => {
              if (!newValue) return;
              const parsed = Number(newValue);
              if (!Number.isInteger(parsed)) return;
              if (parsed < minYear || parsed > maxYear) return;
              setYear(parsed);
              setYearInput(String(parsed));
            }}
            getOptionLabel={(option) => String(option)}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Year"
                error={Boolean(yearInputError)}
                helperText={yearInputError || ' '}
                sx={{ width: 200 }}
                inputProps={{
                  ...params.inputProps,
                  inputMode: 'numeric',
                  pattern: '[0-9]*',
                  maxLength: 4,
                  onFocus: (event) => event.currentTarget.select(),
                }}
              />
            )}
          />
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      )}

      {!isLoading && data && (
        <>
          <Paper sx={{ p: 2, mb: 3 }}>
            <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
              <Box>
                <Typography variant="body2" color="text.secondary">Total Budget</Typography>
                <Typography variant="h6">{formatCurrency(data.totalBudget)}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Total Spent</Typography>
                <Typography variant="h6">{formatCurrency(data.totalSpending)}</Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">Total Available</Typography>
                <Typography variant="h6" color={data.totalRemaining >= 0 ? 'success.main' : 'error.main'}>
                  {formatCurrency(data.totalRemaining)}
                </Typography>
              </Box>
            </Box>
          </Paper>

          {/* Month visibility toggles */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2, flexWrap: 'wrap' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mr: 0.5 }}>
              Months:
            </Typography>
            {MONTH_LABELS.map((label, idx) => {
              const monthNum = idx + 1;
              const isHidden = hiddenMonths.has(monthNum);
              return (
                <Chip
                  key={label}
                  label={label}
                  size="small"
                  color={isHidden ? 'default' : 'primary'}
                  variant={isHidden ? 'outlined' : 'filled'}
                  onClick={() => toggleMonth(monthNum)}
                  sx={isHidden ? { opacity: 0.5 } : {}}
                />
              );
            })}
            {hiddenMonths.size > 0 && (
              <Button size="small" variant="text" onClick={() => setHiddenMonths(new Set())}>
                Show All
              </Button>
            )}
          </Box>

          <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
            <Table stickyHeader size="small" aria-label="yearly budget table">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 250 }}>Category</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Yearly Budget</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Spent</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Available</TableCell>
                  {MONTH_LABELS.map((label, idx) => {
                    if (hiddenMonths.has(idx + 1)) return null;
                    return (
                      <TableCell
                        key={label}
                        align="center"
                        sx={{ minWidth: 110, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
                        onClick={() => {
                          const params = new URLSearchParams({ year: String(year), month: String(idx + 1) });
                          router.push(`/expenses?${params.toString()}`);
                        }}
                      >
                        <Typography variant="subtitle2" sx={{ '&:hover': { textDecoration: 'underline' } }}>
                          {label}
                        </Typography>
                      </TableCell>
                    );
                  })}
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredGroups.map((group) => {
                  const hasChildren = group.children.length > 0;
                  const isExpanded = expandedGroups.has(group.parent.categoryId);

                  return (
                    <Fragment key={group.parent.categoryId}>
                      {/* Parent / standalone row */}
                      <TableRow
                        hover
                        onClick={hasChildren ? () => toggleGroup(group.parent.categoryId) : undefined}
                        sx={{
                          cursor: hasChildren ? 'pointer' : 'default',
                          ...(hasChildren && { backgroundColor: 'grey.50' }),
                        }}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            {hasChildren ? (
                              <IconButton size="small" sx={{ mr: 0.5 }} tabIndex={-1}>
                                {isExpanded ? (
                                  <KeyboardArrowDownIcon fontSize="small" />
                                ) : (
                                  <KeyboardArrowRightIcon fontSize="small" />
                                )}
                              </IconButton>
                            ) : (
                              <Box sx={{ width: 34 }} />
                            )}
                            <Typography variant="body2" fontWeight={hasChildren ? 700 : 600}>
                              {group.parent.categoryIcon ? `${group.parent.categoryIcon} ` : ''}{group.parent.categoryName}
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" fontWeight={hasChildren ? 700 : 400}>
                            {formatCurrency(group.parent.yearlyBudgetAmount)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography
                            variant="body2"
                            color={getSpendingColor(group.parent.yearlySpending, group.parent.yearlyBudgetAmount)}
                            fontWeight={group.parent.yearlySpending >= group.parent.yearlyBudgetAmount ? 700 : hasChildren ? 700 : 400}
                            onClick={(e) => { e.stopPropagation(); goToExpenses(group.parent.categoryId); }}
                            sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                          >
                            {formatCurrency(group.parent.yearlySpending)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={formatCurrency(group.parent.yearlyRemaining)}
                            color={group.parent.yearlyRemaining >= 0 ? 'success' : 'error'}
                          />
                        </TableCell>
                        {renderMonthCells(group.parent, hasChildren)}
                      </TableRow>

                      {/* Child rows (shown when expanded) */}
                      {hasChildren && isExpanded && group.children.map((child) => (
                        <TableRow key={child.categoryId} hover>
                          <TableCell>
                            <Box sx={{ pl: 5 }}>
                              <Typography variant="body2">
                                {child.categoryIcon ? `${child.categoryIcon} ` : ''}{child.categoryName}
                              </Typography>
                            </Box>
                          </TableCell>
                          <TableCell>{formatCurrency(child.yearlyBudgetAmount)}</TableCell>
                          <TableCell>
                            <Typography
                              variant="body2"
                              color={getSpendingColor(child.yearlySpending, child.yearlyBudgetAmount)}
                              fontWeight={child.yearlySpending >= child.yearlyBudgetAmount ? 700 : 400}
                              onClick={() => goToExpenses(child.categoryId)}
                              sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                            >
                              {formatCurrency(child.yearlySpending)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              label={formatCurrency(child.yearlyRemaining)}
                              color={child.yearlyRemaining >= 0 ? 'success' : 'error'}
                            />
                          </TableCell>
                          {renderMonthCells(child, false)}
                        </TableRow>
                      ))}
                    </Fragment>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Container>
  );
}
