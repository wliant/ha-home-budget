'use client';

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  IconButton,
  LinearProgress,
  Typography,
} from '@mui/material';
import {
  KeyboardArrowDown as ExpandIcon,
  KeyboardArrowRight as CollapseIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import {
  BudgetSummaryDTO,
  formatCurrency,
  getMonthName,
  getSpendingStatusColor,
} from '@/services/budgetService';

interface CategoryBudgetGroup {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  parentCategoryId?: number;
  yearlyBudget?: BudgetSummaryDTO;
  monthlyBudgets: BudgetSummaryDTO[];
  childCategoryGroups: CategoryBudgetGroup[];
}

interface BudgetGroupProps {
  group: CategoryBudgetGroup;
  isChild?: boolean;
  onView: (id: number) => void;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
}

const MonthlyBudgetRow: React.FC<{
  budget: BudgetSummaryDTO;
  onView: (id: number) => void;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
}> = ({ budget, onView, onEdit, onDelete }) => {
  const remaining = budget.totalAmount - budget.totalSpending;
  const statusColor = getSpendingStatusColor(budget.spendingPercentage);

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        py: 0.75,
        px: 1.5,
        borderRadius: 1,
        '&:hover': { bgcolor: 'action.hover' },
      }}
    >
      <Typography variant="body2" sx={{ minWidth: 80, fontWeight: 500 }}>
        {getMonthName(budget.month!)}
      </Typography>
      <Typography variant="body2" sx={{ minWidth: 90, textAlign: 'right' }}>
        {formatCurrency(budget.totalAmount)}
      </Typography>
      <Box sx={{ flexGrow: 1, maxWidth: 120, mx: 1 }}>
        <LinearProgress
          variant="determinate"
          value={Math.min(budget.spendingPercentage, 100)}
          color={statusColor as any}
          sx={{ height: 6, borderRadius: 3 }}
        />
      </Box>
      <Typography
        variant="body2"
        color={budget.totalSpending !== 0 ? `${statusColor}.main` : 'text.secondary'}
        sx={{ minWidth: 90, textAlign: 'right' }}
      >
        {budget.totalSpending !== 0 ? formatCurrency(budget.totalSpending) : '—'}
      </Typography>
      <Typography
        variant="caption"
        color={remaining >= 0 ? 'success.main' : 'error.main'}
        sx={{ minWidth: 90, textAlign: 'right' }}
      >
        {remaining >= 0 ? `${formatCurrency(remaining)} left` : `${formatCurrency(Math.abs(remaining))} over`}
      </Typography>
      <Box sx={{ display: 'flex', ml: 'auto' }}>
        <IconButton size="small" onClick={() => onView(budget.id)} title="View">
          <ViewIcon fontSize="small" />
        </IconButton>
        <IconButton size="small" onClick={() => onEdit(budget.id)} color="primary" title="Edit">
          <EditIcon fontSize="small" />
        </IconButton>
        <IconButton size="small" onClick={() => onDelete(budget.id)} color="error" title="Delete">
          <DeleteIcon fontSize="small" />
        </IconButton>
      </Box>
    </Box>
  );
};

const BudgetGroup: React.FC<BudgetGroupProps> = ({
  group,
  isChild = false,
  onView,
  onEdit,
  onDelete,
}) => {
  const theme = useTheme();
  const [expanded, setExpanded] = useState(true);

  const yearlyBudget = group.yearlyBudget;
  const hasMonthly = group.monthlyBudgets.length > 0;
  const hasChildren = group.childCategoryGroups.length > 0;
  const hasContent = hasMonthly || hasChildren;

  // Summary values from yearly budget or sum of monthly
  const totalAmount = yearlyBudget?.totalAmount ?? group.monthlyBudgets.reduce((s, b) => s + b.totalAmount, 0);
  const totalSpending = yearlyBudget?.totalSpending ?? group.monthlyBudgets.reduce((s, b) => s + b.totalSpending, 0);
  const spendingPct = totalAmount > 0 ? (totalSpending / totalAmount) * 100 : 0;
  const remaining = totalAmount - totalSpending;
  const statusColor = getSpendingStatusColor(spendingPct);

  return (
    <Card
      sx={{
        ml: isChild ? 3 : 0,
        borderLeft: isChild ? `4px solid ${theme.palette.primary.main}` : 'none',
        ...(!isChild && { boxShadow: theme.shadows[3] }),
      }}
    >
      <CardContent sx={{ pb: hasContent && expanded ? 0 : undefined }}>
        {/* Category header */}
        <Box
          sx={{ display: 'flex', alignItems: 'center', gap: 1, cursor: hasContent ? 'pointer' : 'default' }}
          onClick={hasContent ? () => setExpanded(!expanded) : undefined}
        >
          {hasContent && (
            <IconButton size="small" tabIndex={-1}>
              {expanded ? <ExpandIcon /> : <CollapseIcon />}
            </IconButton>
          )}
          {!hasContent && <Box sx={{ width: 34 }} />}

          {group.categoryIcon && (
            <Typography variant="h5">{group.categoryIcon}</Typography>
          )}
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            {group.categoryName}
          </Typography>

          {/* Yearly summary */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Box sx={{ textAlign: 'right' }}>
              <Typography variant="body2" color="text.secondary">Budget</Typography>
              <Typography variant="body1" fontWeight={600}>{formatCurrency(totalAmount)}</Typography>
            </Box>
            <Box sx={{ textAlign: 'right' }}>
              <Typography variant="body2" color="text.secondary">Spent</Typography>
              <Typography variant="body1" color={`${statusColor}.main`} fontWeight={600}>
                {formatCurrency(totalSpending)}
              </Typography>
            </Box>
            <Chip
              size="small"
              label={formatCurrency(remaining)}
              color={remaining >= 0 ? 'success' : 'error'}
            />
          </Box>

          {/* Actions for yearly budget */}
          {yearlyBudget && (
            <Box sx={{ display: 'flex', ml: 1 }}>
              <IconButton size="small" onClick={(e) => { e.stopPropagation(); onView(yearlyBudget.id); }} title="View yearly">
                <ViewIcon fontSize="small" />
              </IconButton>
              <IconButton size="small" onClick={(e) => { e.stopPropagation(); onEdit(yearlyBudget.id); }} color="primary" title="Edit yearly">
                <EditIcon fontSize="small" />
              </IconButton>
              <IconButton size="small" onClick={(e) => { e.stopPropagation(); onDelete(yearlyBudget.id); }} color="error" title="Delete yearly">
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Box>
          )}
        </Box>

        {/* Monthly budget chips for at-a-glance */}
        {hasMonthly && (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1, ml: hasContent ? 4.25 : 0 }}>
            {group.monthlyBudgets.map((mb) => {
              const mbStatus = getSpendingStatusColor(mb.spendingPercentage);
              return (
                <Chip
                  key={mb.id}
                  label={`${getMonthName(mb.month!).slice(0, 3)}: ${formatCurrency(mb.totalAmount)}`}
                  size="small"
                  variant="outlined"
                  color={mbStatus as any}
                />
              );
            })}
          </Box>
        )}

        {/* Child category chips for at-a-glance */}
        {hasChildren && (
          <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 1, ml: hasContent ? 4.25 : 0 }}>
            {group.childCategoryGroups.map((child) => (
              <Chip
                key={child.categoryId}
                label={`${child.categoryIcon ? child.categoryIcon + ' ' : ''}${child.categoryName}`}
                size="small"
                variant="outlined"
                color="primary"
              />
            ))}
          </Box>
        )}
      </CardContent>

      {/* Expanded content */}
      {expanded && hasContent && (
        <Box sx={{ px: 2, pb: 2 }}>
          {/* Monthly budget detail rows */}
          {hasMonthly && (
            <Box sx={{ mt: 1, ml: 2.25 }}>
              {group.monthlyBudgets
                .sort((a, b) => (a.month ?? 0) - (b.month ?? 0))
                .map((mb) => (
                  <MonthlyBudgetRow
                    key={mb.id}
                    budget={mb}
                    onView={onView}
                    onEdit={onEdit}
                    onDelete={onDelete}
                  />
                ))}
            </Box>
          )}

          {/* Child category budget groups */}
          {hasChildren && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
              {group.childCategoryGroups.map((child) => (
                <BudgetGroup
                  key={child.categoryId}
                  group={child}
                  isChild
                  onView={onView}
                  onEdit={onEdit}
                  onDelete={onDelete}
                />
              ))}
            </Box>
          )}
        </Box>
      )}
    </Card>
  );
};

export default BudgetGroup;
export type { CategoryBudgetGroup };
