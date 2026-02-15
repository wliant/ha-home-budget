'use client';

import React, { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useIngressRouter } from '../../../../lib/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Alert,
  CircularProgress,
} from '@mui/material';
import { ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import BudgetForm from '@/components/BudgetForm';
import {
  budgetService,
  CreateBudgetRequest,
  UpdateBudgetRequest,
  BudgetSummaryDTO,
  formatCurrency,
  getMonthName,
} from '@/services/budgetService';

export default function EditBudgetPage() {
  const router = useIngressRouter();
  const params = useParams();
  const budgetId = params?.id ? parseInt(params.id as string) : null;

  const [budget, setBudget] = useState<BudgetSummaryDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [successMessage, setSuccessMessage] = useState<string>('');

  useEffect(() => {
    if (budgetId) {
      loadBudget();
    }
  }, [budgetId]);

  const loadBudget = async () => {
    if (!budgetId) return;

    setIsLoading(true);
    setError('');

    try {
      const data = await budgetService.getBudgetById(budgetId);
      setBudget(data);
    } catch (err: any) {
      console.error('Failed to load budget:', err);
      if (err.response?.status === 404) {
        setError('Budget not found');
      } else {
        setError('Failed to load budget details. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (data: CreateBudgetRequest) => {
    if (!budgetId) return;

    try {
      const updateRequest: UpdateBudgetRequest = {
        totalAmount: data.totalAmount,
        description: data.description,
      };
      await budgetService.updateBudget(budgetId, updateRequest);

      setSuccessMessage('Budget updated successfully!');

      setTimeout(() => {
        router.push(`/budgets/${budgetId}`);
      }, 1500);
    } catch (error) {
      throw error;
    }
  };

  const handleCancel = () => {
    router.push(`/budgets/${budgetId}`);
  };

  if (isLoading) {
    return (
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error || !budget) {
    return (
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          {error || 'Budget not found'}
        </Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={() => router.push('/budgets')}>
          Back to Budgets
        </Button>
      </Container>
    );
  }

  const initialValues: Partial<CreateBudgetRequest> = {
    year: budget.year,
    month: budget.month || undefined,
    categoryId: budget.categoryId || undefined,
    totalAmount: budget.totalAmount,
    description: budget.description || '',
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleCancel}
          sx={{ mb: 2 }}
        >
          Back to Budget
        </Button>
        <Typography variant="h4" component="h1">
          Edit Budget
        </Typography>
      </Box>

      {successMessage && (
        <Alert severity="success" sx={{ mb: 3 }}>
          {successMessage}
        </Alert>
      )}

      <BudgetForm
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        initialValues={initialValues}
        isEdit={true}
      />
    </Container>
  );
}
