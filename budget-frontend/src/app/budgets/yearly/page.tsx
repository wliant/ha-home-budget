'use client';

import { Fragment, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Autocomplete,
  Box,
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

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

function getSpendingColor(spent: number, budget: number): string {
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
    category.months.map((month) => (
      <TableCell key={month.month} align="center">
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
          <Typography variant="caption" fontWeight={bold ? 600 : 400}>
            {month.hasBudget ? formatCurrency(month.budgetAmount) : '—'}
          </Typography>
          <Typography
            variant="caption"
            color={month.hasBudget && month.spending > 0 ? getSpendingColor(month.spending, month.budgetAmount) : 'text.secondary'}
            fontWeight={month.hasBudget && month.spending >= month.budgetAmount ? 700 : bold ? 600 : 400}
          >
            {month.hasBudget ? formatCurrency(month.spending) : ''}
          </Typography>
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

          <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
            <Table stickyHeader size="small" aria-label="yearly budget table">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ minWidth: 250 }}>Category</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Yearly Budget</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Spent</TableCell>
                  <TableCell sx={{ minWidth: 140 }}>Available</TableCell>
                  {MONTH_LABELS.map((label) => (
                    <TableCell key={label} align="center" sx={{ minWidth: 110 }}>
                      {label}
                    </TableCell>
                  ))}
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
