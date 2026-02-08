'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Alert,
} from '@mui/material';
import { ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import BudgetForm from '@/components/BudgetForm';
import { budgetService, CreateBudgetRequest, formatCurrency, getMonthName } from '@/services/budgetService';

/**
 * New budget page - User Story 1: Create and View Budgets
 *
 * Features:
 * - Form to create a new budget
 * - Validation and error handling
 * - Redirect to budgets list on success
 */

export default function NewBudgetPage() {
  const router = useRouter();
  const [successMessage, setSuccessMessage] = React.useState<string>('');

  const [parentCatUpdateMessage, setParentCatUpdateMessage] = React.useState<string>('');

  const handleSubmit = async (data: CreateBudgetRequest) => {
    try {
      const created = await budgetService.createBudget(data);

      setSuccessMessage('Budget created successfully!');

      if (created.parentCategoryBudgetUpdated) {
        const info = created.parentCategoryBudgetUpdated;
        const period = info.month ? `${getMonthName(info.month)} ${info.year}` : `${info.year}`;
        if (info.previousAmount > 0) {
          setParentCatUpdateMessage(
            `Budget for '${info.parentCategoryName}' has been updated from ${formatCurrency(info.previousAmount)} to ${formatCurrency(info.newAmount)} for ${period}.`
          );
        } else {
          setParentCatUpdateMessage(
            `Budget for '${info.parentCategoryName}' has been created with ${formatCurrency(info.newAmount)} for ${period}.`
          );
        }
      }

      // Redirect to budgets list after a short delay
      setTimeout(() => {
        router.push('/budgets');
      }, 2500);
    } catch (error) {
      // Error handling is done in BudgetForm component
      throw error;
    }
  };

  const handleCancel = () => {
    router.push('/budgets');
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleCancel}
          sx={{ mb: 2 }}
        >
          Back to Budgets
        </Button>
        <Typography variant="h4" component="h1">
          Create New Budget
        </Typography>
      </Box>

      {/* Success Message */}
      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {successMessage}
        </Alert>
      )}

      {/* Parent Category Budget Update Message */}
      {parentCatUpdateMessage && (
        <Alert severity="info" sx={{ mb: 3 }}>
          {parentCatUpdateMessage}
        </Alert>
      )}

      {/* Budget Form */}
      <BudgetForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
      />
    </Container>
  );
}
