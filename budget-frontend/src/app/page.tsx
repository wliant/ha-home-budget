'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  Container,
  Box,
  Typography,
  Paper,
  Grid,
  LinearProgress,
  CircularProgress,
  Chip,
  IconButton,
} from '@mui/material';
import {
  AccountBalanceWallet as WalletIcon,
  Category as CategoryIcon,
  Dashboard as DashboardIcon,
  Receipt as ReceiptIcon,
  Home as HomeIcon,
  KeyboardArrowDown as ExpandIcon,
  KeyboardArrowRight as CollapseIcon,
} from '@mui/icons-material';
import { useIngressRouter } from '@/lib/navigation';
import { QuickActionsCard } from '@/components/home/QuickActionsCard';
import { RecentActivityCard } from '@/components/home/RecentActivityCard';
import { FeatureNavigationCard } from '@/components/home/FeatureNavigationCard';
import {
  budgetService,
  BudgetSummaryDTO,
  formatCurrency,
  getSpendingStatusColor,
  getMonthName,
} from '@/services/budgetService';

function getSpendingColor(pct: number): string {
  if (pct >= 100) return 'error.main';
  if (pct >= 90) return 'warning.main';
  if (pct < 50) return 'success.main';
  return 'text.primary';
}

interface MonthlyBudgetSectionProps {
  title: string;
  budgets: BudgetSummaryDTO[];
  defaultExpanded?: boolean;
  onSpentClick: (budget: BudgetSummaryDTO) => void;
}

function MonthlyBudgetSection({ title, budgets, defaultExpanded = true, onSpentClick }: MonthlyBudgetSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  if (budgets.length === 0) return null;

  const totalBudget = budgets.reduce((s, b) => s + b.totalAmount, 0);
  const totalSpent = budgets.reduce((s, b) => s + b.totalSpending, 0);
  const totalRemaining = totalBudget - totalSpent;
  const overallPct = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0;

  return (
    <Paper sx={{ mb: 3, overflow: 'hidden' }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          px: 2,
          py: 1.5,
          cursor: 'pointer',
          bgcolor: 'grey.50',
          borderBottom: expanded ? '1px solid' : 'none',
          borderColor: 'divider',
        }}
        onClick={() => setExpanded(!expanded)}
      >
        <IconButton size="small" tabIndex={-1}>
          {expanded ? <ExpandIcon /> : <CollapseIcon />}
        </IconButton>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          {title}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box sx={{ textAlign: 'right' }}>
            <Typography variant="caption" color="text.secondary">Budget</Typography>
            <Typography variant="body2" fontWeight={600}>{formatCurrency(totalBudget)}</Typography>
          </Box>
          <Box sx={{ textAlign: 'right' }}>
            <Typography variant="caption" color="text.secondary">Spent</Typography>
            <Typography variant="body2" fontWeight={600} color={getSpendingColor(overallPct)}>
              {formatCurrency(totalSpent)}
            </Typography>
          </Box>
          <Chip
            size="small"
            label={formatCurrency(totalRemaining)}
            color={totalRemaining >= 0 ? 'success' : 'error'}
          />
        </Box>
      </Box>

      {expanded && (
        <Box sx={{ px: 2, py: 1 }}>
          {budgets
            .sort((a, b) => (a.category?.name ?? '').localeCompare(b.category?.name ?? ''))
            .map((budget) => {
              const remaining = budget.totalAmount - budget.totalSpending;
              const pct = budget.totalAmount > 0 ? (budget.totalSpending / budget.totalAmount) * 100 : 0;
              const statusColor = getSpendingStatusColor(budget.spendingPercentage);

              return (
                <Box
                  key={budget.id}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    py: 1,
                    px: 1,
                    borderRadius: 1,
                    '&:hover': { bgcolor: 'action.hover' },
                    '&:not(:last-child)': { borderBottom: '1px solid', borderColor: 'divider' },
                  }}
                >
                  {/* Category icon + name */}
                  <Typography variant="body2" sx={{ minWidth: 24, textAlign: 'center' }}>
                    {budget.category?.icon ?? ''}
                  </Typography>
                  <Typography variant="body2" fontWeight={500} sx={{ minWidth: 140, flexShrink: 0 }}>
                    {budget.category?.name ?? 'Unknown'}
                  </Typography>

                  {/* Budget amount */}
                  <Typography variant="body2" sx={{ minWidth: 90, textAlign: 'right' }}>
                    {formatCurrency(budget.totalAmount)}
                  </Typography>

                  {/* Progress bar */}
                  <Box sx={{ flexGrow: 1, maxWidth: 160, mx: 1 }}>
                    <LinearProgress
                      variant="determinate"
                      value={Math.min(pct, 100)}
                      color={statusColor as any}
                      sx={{ height: 6, borderRadius: 3 }}
                    />
                  </Box>

                  {/* Spent (clickable) */}
                  <Typography
                    variant="body2"
                    color={budget.totalSpending !== 0 ? `${statusColor}.main` : 'text.secondary'}
                    sx={{
                      minWidth: 90,
                      textAlign: 'right',
                      cursor: budget.totalSpending !== 0 ? 'pointer' : 'default',
                      '&:hover': budget.totalSpending !== 0 ? { textDecoration: 'underline' } : {},
                    }}
                    onClick={budget.totalSpending !== 0 ? () => onSpentClick(budget) : undefined}
                  >
                    {budget.totalSpending !== 0 ? formatCurrency(budget.totalSpending) : '—'}
                  </Typography>

                  {/* Remaining */}
                  <Typography
                    variant="body2"
                    color={remaining >= 0 ? 'success.main' : 'error.main'}
                    sx={{ minWidth: 100, textAlign: 'right' }}
                  >
                    {remaining >= 0 ? `${formatCurrency(remaining)} left` : `${formatCurrency(Math.abs(remaining))} over`}
                  </Typography>
                </Box>
              );
            })}
        </Box>
      )}
    </Paper>
  );
}

export default function Home() {
  const router = useIngressRouter();
  const [budgets, setBudgets] = useState<BudgetSummaryDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    budgetService.getAllBudgets()
      .then(setBudgets)
      .catch((err) => console.error('Failed to load budgets:', err))
      .finally(() => setLoading(false));
  }, []);

  const now = new Date();
  const currentMonth = now.getMonth() + 1;
  const currentYear = now.getFullYear();
  const prevMonth = currentMonth === 1 ? 12 : currentMonth - 1;
  const prevYear = currentMonth === 1 ? currentYear - 1 : currentYear;
  const prevPrevMonth = prevMonth === 1 ? 12 : prevMonth - 1;
  const prevPrevYear = prevMonth === 1 ? prevYear - 1 : prevYear;

  const currentMonthBudgets = useMemo(
    () => budgets.filter((b) => b.month === currentMonth && b.year === currentYear),
    [budgets, currentMonth, currentYear],
  );

  const prevMonthBudgets = useMemo(
    () => budgets.filter((b) => b.month === prevMonth && b.year === prevYear),
    [budgets, prevMonth, prevYear],
  );

  const prevPrevMonthBudgets = useMemo(
    () => budgets.filter((b) => b.month === prevPrevMonth && b.year === prevPrevYear),
    [budgets, prevPrevMonth, prevPrevYear],
  );

  const handleSpentClick = (budget: BudgetSummaryDTO) => {
    const params = new URLSearchParams({
      year: String(budget.year),
      month: String(budget.month!),
      categoryId: String(budget.categoryId ?? budget.category?.id ?? ''),
    });
    router.push(`/expenses?${params.toString()}`);
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ my: 4 }}>
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, justifyContent: 'center' }}>
            <HomeIcon sx={{ fontSize: 32, color: 'primary.main' }} />
            <Typography variant="h3" component="h1" gutterBottom>
              Home Budget Tracker
            </Typography>
          </Box>
        </Box>

        {/* Quick Actions */}
        <Box sx={{ mb: 4 }}>
          <QuickActionsCard />
        </Box>

        {/* Monthly Budget Views */}
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <MonthlyBudgetSection
              title={`${getMonthName(currentMonth)} ${currentYear}`}
              budgets={currentMonthBudgets}
              defaultExpanded={true}
              onSpentClick={handleSpentClick}
            />
            <MonthlyBudgetSection
              title={`${getMonthName(prevMonth)} ${prevYear}`}
              budgets={prevMonthBudgets}
              defaultExpanded={false}
              onSpentClick={handleSpentClick}
            />
            <MonthlyBudgetSection
              title={`${getMonthName(prevPrevMonth)} ${prevPrevYear}`}
              budgets={prevPrevMonthBudgets}
              defaultExpanded={false}
              onSpentClick={handleSpentClick}
            />
            {currentMonthBudgets.length === 0 && prevMonthBudgets.length === 0 && prevPrevMonthBudgets.length === 0 && (
              <Paper sx={{ p: 4, textAlign: 'center', mb: 3 }}>
                <Typography variant="body1" color="text.secondary">
                  No monthly budgets configured. Create monthly budgets to see them here.
                </Typography>
              </Paper>
            )}
          </>
        )}

        {/* Recent Activity + Feature Navigation side by side */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} md={6}>
            <RecentActivityCard />
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>
              Explore
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <FeatureNavigationCard
                  title="Budgets"
                  description="Create and manage budgets"
                  icon={<WalletIcon />}
                  path="/budgets"
                />
              </Grid>
              <Grid item xs={6}>
                <FeatureNavigationCard
                  title="Categories"
                  description="Organize spending categories"
                  icon={<CategoryIcon />}
                  path="/categories"
                />
              </Grid>
              <Grid item xs={6}>
                <FeatureNavigationCard
                  title="Yearly View"
                  description="Budget overview by year"
                  icon={<DashboardIcon />}
                  path="/budgets/yearly"
                />
              </Grid>
              <Grid item xs={6}>
                <FeatureNavigationCard
                  title="Expenses"
                  description="Record and track expenses"
                  icon={<ReceiptIcon />}
                  path="/expenses"
                />
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      </Box>
    </Container>
  );
}
