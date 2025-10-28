'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import BudgetCard from '@/components/BudgetCard';
import { budgetService, BudgetSummaryDTO } from '@/services/budgetService';

/**
 * Budgets list page - User Story 1: Create and View Budgets
 *
 * Features:
 * - Display all budgets in a grid layout
 * - Create new budget button
 * - View, edit, and delete actions for each budget
 * - Loading and error states
 * - Delete confirmation dialog
 */

export default function BudgetsPage() {
  const router = useRouter();
  const [budgets, setBudgets] = useState<BudgetSummaryDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [budgetToDelete, setBudgetToDelete] = useState<number | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load budgets on mount
  useEffect(() => {
    loadBudgets();
  }, []);

  const loadBudgets = async () => {
    setIsLoading(true);
    setError('');

    try {
      const data = await budgetService.getAllBudgets();
      setBudgets(data);
    } catch (err: any) {
      console.error('Failed to load budgets:', err);
      setError('Failed to load budgets. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateNew = () => {
    router.push('/budgets/new');
  };

  const handleView = (id: number) => {
    router.push(`/budgets/${id}`);
  };

  const handleEdit = (id: number) => {
    router.push(`/budgets/${id}/edit`);
  };

  const handleDeleteClick = (id: number) => {
    setBudgetToDelete(id);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (budgetToDelete === null) return;

    setIsDeleting(true);

    try {
      await budgetService.deleteBudget(budgetToDelete);

      // Remove deleted budget from state
      setBudgets((prev) => prev.filter((b) => b.id !== budgetToDelete));

      // Close dialog
      setDeleteDialogOpen(false);
      setBudgetToDelete(null);
    } catch (err: any) {
      console.error('Failed to delete budget:', err);
      setError('Failed to delete budget. Please try again.');
      setDeleteDialogOpen(false);
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setBudgetToDelete(null);
  };

  const deletingBudget = budgets.find((b) => b.id === budgetToDelete);

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Budgets
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleCreateNew}
        >
          Create Budget
        </Button>
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {/* Loading State */}
      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      )}

      {/* Empty State */}
      {!isLoading && budgets.length === 0 && (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No budgets yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Create your first budget to start tracking your expenses
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateNew}
          >
            Create Your First Budget
          </Button>
        </Box>
      )}

      {/* Budgets Grid */}
      {!isLoading && budgets.length > 0 && (
        <Grid container spacing={3}>
          {budgets.map((budget) => (
            <Grid item xs={12} sm={6} md={4} key={budget.id}>
              <BudgetCard
                budget={budget}
                onView={handleView}
                onEdit={handleEdit}
                onDelete={handleDeleteClick}
              />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">
          Delete Budget?
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete the budget for{' '}
            {deletingBudget && `${deletingBudget.year}-${String(deletingBudget.month).padStart(2, '0')}`}?
            {deletingBudget && deletingBudget.expenseCount > 0 && (
              <>
                <br /><br />
                <strong>Warning:</strong> This will also delete {deletingBudget.expenseCount}{' '}
                associated {deletingBudget.expenseCount === 1 ? 'expense' : 'expenses'}.
              </>
            )}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel} disabled={isDeleting}>
            Cancel
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            disabled={isDeleting}
            autoFocus
          >
            {isDeleting ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
