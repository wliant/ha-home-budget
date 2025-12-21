'use client';

import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Box,
  LinearProgress,
  Chip,
  Button,
  IconButton,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import {
  BudgetSummaryDTO,
  formatBudgetPeriod,
  formatCurrency,
  getSpendingStatusColor,
  getSpendingStatusText,
} from '@/services/budgetService';

/**
 * BudgetCard component for displaying budget summary.
 *
 * Features:
 * - Shows budget period, amount, and spending status
 * - Visual progress bar for spending
 * - Color-coded status based on spending percentage
 * - Quick action buttons (view, edit, delete)
 */

interface BudgetCardProps {
  budget: BudgetSummaryDTO;
  onView?: (id: number) => void;
  onEdit?: (id: number) => void;
  onDelete?: (id: number) => void;
}

export const BudgetCard: React.FC<BudgetCardProps> = ({
  budget,
  onView,
  onEdit,
  onDelete,
}) => {
  const statusColor = getSpendingStatusColor(budget.spendingPercentage);
  const statusText = getSpendingStatusText(budget.spendingPercentage);
  const remaining = budget.totalAmount - budget.totalSpending;

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        {/* Budget Period */}
        <Typography variant="h6" component="div" gutterBottom>
          {formatBudgetPeriod(budget.year, budget.month)}
        </Typography>

        {/* Category */}
        {budget.category && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {budget.category.icon && `${budget.category.icon} `}
            {budget.category.name}
            {budget.category.parentCategory && ` (${budget.category.parentCategory.name})`}
          </Typography>
        )}

        {/* Status Chip */}
        <Box sx={{ mb: 2 }}>
          <Chip
            label={statusText}
            color={statusColor as any}
            size="small"
          />
        </Box>

        {/* Budget Amount */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Budget
          </Typography>
          <Typography variant="h5" component="div">
            {formatCurrency(budget.totalAmount)}
          </Typography>
        </Box>

        {/* Spending Info */}
        <Box sx={{ mb: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              Spent
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {budget.spendingPercentage.toFixed(1)}%
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={Math.min(budget.spendingPercentage, 100)}
            color={statusColor as any}
            sx={{ height: 8, borderRadius: 4 }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
            <Typography variant="body2">
              {formatCurrency(budget.totalSpending)}
            </Typography>
            <Typography
              variant="body2"
              color={remaining >= 0 ? 'success.main' : 'error.main'}
            >
              {remaining >= 0 ? 'Remaining: ' : 'Over by: '}
              {formatCurrency(Math.abs(remaining))}
            </Typography>
          </Box>
        </Box>

        {/* Expense Count */}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          {budget.expenseCount} {budget.expenseCount === 1 ? 'expense' : 'expenses'}
        </Typography>

        {/* Description */}
        {budget.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1, fontStyle: 'italic' }}>
            {budget.description}
          </Typography>
        )}
      </CardContent>

      <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
        <Box>
          {onView && (
            <Button
              size="small"
              startIcon={<VisibilityIcon />}
              onClick={() => onView(budget.id)}
            >
              View
            </Button>
          )}
          {onEdit && (
            <IconButton
              size="small"
              onClick={() => onEdit(budget.id)}
              color="primary"
              aria-label="edit budget"
            >
              <EditIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
        {onDelete && (
          <IconButton
            size="small"
            onClick={() => onDelete(budget.id)}
            color="error"
            aria-label="delete budget"
          >
            <DeleteIcon fontSize="small" />
          </IconButton>
        )}
      </CardActions>
    </Card>
  );
};

export default BudgetCard;
