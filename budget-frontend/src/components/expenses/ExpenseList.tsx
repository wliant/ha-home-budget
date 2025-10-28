'use client';

import React from 'react';
import {
  Box,
  Typography,
  IconButton,
  Alert,
  Chip,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import {
  ExpenseDTO,
  formatExpenseAmount,
  formatExpenseDate,
  hasDateMismatchWarning,
  getWarningMessage,
} from '@/services/expenseService';

/**
 * ExpenseList component for displaying list of expenses
 *
 * Features:
 * - Display expense amount, description, date
 * - Show category (if assigned)
 * - Show who created the expense
 * - Display date mismatch warnings
 * - Edit and delete actions
 */

interface ExpenseListProps {
  expenses: ExpenseDTO[];
  onEdit?: (id: number) => void;
  onDelete?: (id: number) => void;
  showActions?: boolean;
}

export const ExpenseList: React.FC<ExpenseListProps> = ({
  expenses,
  onEdit,
  onDelete,
  showActions = true,
}) => {
  if (expenses.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="body1" color="text.secondary">
          No expenses recorded yet
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Start adding expenses to track your spending
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      {expenses.map((expense) => (
        <Box
          key={expense.id}
          sx={{
            py: 2,
            px: 1,
            borderBottom: '1px solid',
            borderColor: 'divider',
            '&:last-child': { borderBottom: 'none' },
          }}
        >
          {/* Date Mismatch Warning */}
          {hasDateMismatchWarning(expense) && (
            <Alert severity="warning" sx={{ mb: 1 }}>
              {getWarningMessage(expense)}
            </Alert>
          )}

          {/* Main Expense Info */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 0.5 }}>
            <Box sx={{ flexGrow: 1 }}>
              {/* Description and Amount */}
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                <Typography variant="body1" fontWeight="medium">
                  {expense.description}
                </Typography>
                <Typography variant="h6" sx={{ ml: 2 }}>
                  {formatExpenseAmount(expense.amount)}
                </Typography>
              </Box>

              {/* Date, Category, and Created By */}
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  {formatExpenseDate(expense.expenseDate)}
                </Typography>

                {expense.categoryName && (
                  <>
                    <Typography variant="body2" color="text.secondary">
                      •
                    </Typography>
                    <Chip
                      label={`${expense.categoryIcon || ''} ${expense.categoryName}`}
                      size="small"
                      variant="outlined"
                    />
                  </>
                )}

                {expense.createdBy && (
                  <>
                    <Typography variant="body2" color="text.secondary">
                      •
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Added by {expense.createdBy}
                    </Typography>
                  </>
                )}
              </Box>
            </Box>

            {/* Action Buttons */}
            {showActions && (
              <Box sx={{ display: 'flex', ml: 2 }}>
                {onEdit && expense.id && (
                  <IconButton
                    size="small"
                    onClick={() => onEdit(expense.id!)}
                    color="primary"
                    aria-label="edit expense"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                )}
                {onDelete && expense.id && (
                  <IconButton
                    size="small"
                    onClick={() => onDelete(expense.id!)}
                    color="error"
                    aria-label="delete expense"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                )}
              </Box>
            )}
          </Box>
        </Box>
      ))}
    </Box>
  );
};

export default ExpenseList;
