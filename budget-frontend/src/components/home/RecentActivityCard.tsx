'use client';

import React, { useEffect, useState } from 'react';
import { RecordExpenseDialog } from '@/components/expenses/RecordExpenseDialog';
import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Button,
  CircularProgress,
  Alert,
  Box,
  Chip,
} from '@mui/material';
import {
  Receipt as ReceiptIcon,
  Category as CategoryIcon,
  Person as PersonIcon,
} from '@mui/icons-material';
import {
  expenseService,
  ExpenseDTO,
  formatExpenseDate,
  formatExpenseAmount,
} from '@/services/expenseService';

/**
 * Recent Activity Card Component
 * User Story 2: Recent Activity Feed (P2)
 *
 * Displays 5 most recent expenses with:
 * - Date, amount, description
 * - Category name with icon
 * - Creator attribution
 * - Empty state for no expenses
 * - Error handling with retry
 */
export function RecentActivityCard() {
  const [expenses, setExpenses] = useState<ExpenseDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [expenseDialogOpen, setExpenseDialogOpen] = useState(false);

  useEffect(() => {
    loadRecentExpenses();
  }, []);

  const loadRecentExpenses = async () => {
    setIsLoading(true);
    setError('');
    try {
      // Use paginated endpoint to fetch only the 5 most recent expenses
      const result = await expenseService.getExpenseList({
        page: 0,
        size: 5,
        sortBy: 'expenseDate',
        sortDirection: 'desc',
      });

      setExpenses(result.content);
    } catch (err: any) {
      console.error('Failed to load recent expenses:', err);
      setError(err.response?.data?.message || 'Failed to load recent activity');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRecordExpense = () => {
    setExpenseDialogOpen(true);
  };

  const truncateDescription = (description: string, maxLength: number = 50): string => {
    if (description.length <= maxLength) return description;
    return description.substring(0, maxLength) + '...';
  };

  // Loading state
  if (isLoading) {
    return (
      <Card>
        <Box sx={{ background: 'linear-gradient(135deg, #7A3B1E 0%, #A0522D 100%)', px: 3, py: 2, borderRadius: '12px 12px 0 0' }}>
          <Typography variant="h6" sx={{ color: '#ffffff' }}>
            Recent Activity
          </Typography>
        </Box>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Error state
  if (error) {
    return (
      <Card>
        <Box sx={{ background: 'linear-gradient(135deg, #7A3B1E 0%, #A0522D 100%)', px: 3, py: 2, borderRadius: '12px 12px 0 0' }}>
          <Typography variant="h6" sx={{ color: '#ffffff' }}>
            Recent Activity
          </Typography>
        </Box>
        <CardContent>
          <Alert severity="error" sx={{ mt: 2 }}>
            <Typography variant="body2">{error}</Typography>
          </Alert>
          <Box sx={{ mt: 2, textAlign: 'center' }}>
            <Button variant="outlined" onClick={loadRecentExpenses} aria-label="Retry loading recent expenses">
              Retry
            </Button>
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Empty state
  if (expenses.length === 0) {
    return (
      <Card>
        <Box sx={{ background: 'linear-gradient(135deg, #7A3B1E 0%, #A0522D 100%)', px: 3, py: 2, borderRadius: '12px 12px 0 0' }}>
          <Typography variant="h6" sx={{ color: '#ffffff' }}>
            Recent Activity
          </Typography>
        </Box>
        <CardContent>
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <ReceiptIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography variant="body1" color="text.secondary" gutterBottom>
              No expenses recorded yet
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Start tracking your spending by recording your first expense
            </Typography>
            <Button variant="contained" startIcon={<ReceiptIcon />} onClick={handleRecordExpense} aria-label="Record your first expense">
              Record Expense
            </Button>
          </Box>
        </CardContent>
        <RecordExpenseDialog
          open={expenseDialogOpen}
          onClose={() => setExpenseDialogOpen(false)}
          onSuccess={loadRecentExpenses}
        />
      </Card>
    );
  }

  // Expenses list
  return (
    <Card>
      <Box sx={{ background: 'linear-gradient(135deg, #7A3B1E 0%, #A0522D 100%)', px: 3, py: 2, borderRadius: '12px 12px 0 0' }}>
        <Typography variant="h6" sx={{ color: '#ffffff' }}>
          Recent Activity
        </Typography>
      </Box>
      <CardContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Your 5 most recent expenses
        </Typography>

        <List sx={{ width: '100%' }}>
          {expenses.map((expense, index) => (
            <ListItem
              key={expense.id || index}
              sx={{
                borderBottom: index < expenses.length - 1 ? '1px solid' : 'none',
                borderColor: 'divider',
                px: 0,
                py: 1.5,
                '&:hover': {
                  bgcolor: 'rgba(160,82,45,0.04)',
                },
              }}
            >
              <ListItemIcon>
                {expense.categoryIcon ? (
                  <Box sx={{ fontSize: 24 }}>{expense.categoryIcon}</Box>
                ) : (
                  <CategoryIcon color="action" />
                )}
              </ListItemIcon>

              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                    <Typography variant="body1" component="span">
                      {truncateDescription(expense.description)}
                    </Typography>
                    <Typography variant="body1" component="span" fontWeight="bold" color="primary">
                      {formatExpenseAmount(expense.amount)}
                    </Typography>
                  </Box>
                }
                secondaryTypographyProps={{ component: 'div' }}
                secondary={
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, mt: 0.5 }}>
                    <Typography variant="caption" color="text.secondary">
                      {formatExpenseDate(expense.expenseDate)}
                    </Typography>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                      {expense.categoryName && (
                        <Chip
                          icon={<CategoryIcon />}
                          label={expense.categoryName}
                          size="small"
                          variant="outlined"
                          color="primary"
                        />
                      )}

                      {expense.createdBy && (
                        <Chip
                          icon={<PersonIcon />}
                          label={expense.createdBy}
                          size="small"
                          variant="outlined"
                          color="secondary"
                        />
                      )}
                    </Box>
                  </Box>
                }
              />
            </ListItem>
          ))}
        </List>

        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Button variant="text" size="small" onClick={handleRecordExpense} aria-label="Record a new expense">
            Record New Expense
          </Button>
        </Box>
      </CardContent>

      <RecordExpenseDialog
        open={expenseDialogOpen}
        onClose={() => setExpenseDialogOpen(false)}
        onSuccess={loadRecentExpenses}
      />
    </Card>
  );
}
