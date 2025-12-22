'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import {
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Box,
} from '@mui/material';
import {
  Add as AddIcon,
  Receipt as ReceiptIcon,
  Category as CategoryIcon,
  Dashboard as DashboardIcon,
  AccountBalanceWallet as WalletIcon,
} from '@mui/icons-material';

/**
 * Quick Actions Card Component
 * User Story 3: Quick Actions Dashboard (P1)
 *
 * Provides one-click access to common tasks:
 * - Create Budget
 * - Record Expense
 * - View Categories
 * - View Dashboard
 */
export function QuickActionsCard() {
  const router = useRouter();

  const handleCreateBudget = () => {
    router.push('/budgets/new');
  };

  const handleRecordExpense = () => {
    router.push('/expenses/new');
  };

  const handleViewCategories = () => {
    router.push('/categories');
  };

  const handleViewDashboard = () => {
    router.push('/dashboard');
  };

  const handleViewBudgets = () => {
    router.push('/budgets');
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Quick Actions
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Common tasks for managing your household budget
        </Typography>

        <Grid container spacing={2}>
          {/* Create Budget */}
          <Grid item xs={6} sm={3}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleCreateBudget}
              sx={{ py: 1.5 }}
              aria-label="Create a new monthly budget"
            >
              Create Budget
            </Button>
          </Grid>

          {/* Record Expense */}
          <Grid item xs={6} sm={3}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<ReceiptIcon />}
              onClick={handleRecordExpense}
              sx={{ py: 1.5 }}
              aria-label="Record a new expense"
            >
              Record Expense
            </Button>
          </Grid>

          {/* View Categories */}
          <Grid item xs={6} sm={3}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<CategoryIcon />}
              onClick={handleViewCategories}
              sx={{ py: 1.5 }}
              aria-label="View spending categories"
            >
              View Categories
            </Button>
          </Grid>

          {/* View Dashboard */}
          <Grid item xs={6} sm={3}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<DashboardIcon />}
              onClick={handleViewDashboard}
              sx={{ py: 1.5 }}
              aria-label="View budget dashboard and analytics"
            >
              View Dashboard
            </Button>
          </Grid>
        </Grid>

        {/* Optional: View All Budgets link */}
        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Button
            variant="text"
            size="small"
            startIcon={<WalletIcon />}
            onClick={handleViewBudgets}
            aria-label="View all budgets list"
          >
            View All Budgets
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}
