'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  CircularProgress,
  TextField,
  MenuItem,
  Chip,
} from '@mui/material';
import { budgetService, formatCurrency, YearlyBudgetViewDTO } from '@/services/budgetService';

const MONTH_LABELS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

export default function YearlyBudgetPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [data, setData] = useState<YearlyBudgetViewDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const yearOptions = useMemo(
    () => Array.from({ length: 6 }, (_, i) => currentYear - 1 + i),
    [currentYear],
  );

  useEffect(() => {
    const load = async () => {
      setIsLoading(true);
      setError('');
      try {
        const response = await budgetService.getYearlyBudgetView(year);
        setData(response);
      } catch (err) {
        console.error('Failed to load yearly budget view', err);
        setError('Failed to load yearly budget view. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [year]);

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Yearly Budget View
        </Typography>
        <TextField
          select
          label="Year"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          sx={{ width: 160 }}
        >
          {yearOptions.map((option) => (
            <MenuItem key={option} value={option}>
              {option}
            </MenuItem>
          ))}
        </TextField>
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
                <Typography variant="body2" color="text.secondary">
                  Total Budget
                </Typography>
                <Typography variant="h6">
                  {formatCurrency(data.totalBudget)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Total Spent
                </Typography>
                <Typography variant="h6">
                  {formatCurrency(data.totalSpending)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Total Available
                </Typography>
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
                  <TableCell sx={{ minWidth: 220 }}>Category</TableCell>
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
                {data.categories.map((category) => (
                  <TableRow key={category.categoryId} hover>
                    <TableCell>
                      <Box>
                        <Typography variant="body2" fontWeight={600}>
                          {category.categoryIcon ? `${category.categoryIcon} ` : ''}{category.categoryName}
                        </Typography>
                        {category.parentCategoryName && (
                          <Typography variant="caption" color="text.secondary">
                            {category.parentCategoryName}
                          </Typography>
                        )}
                      </Box>
                    </TableCell>
                    <TableCell>{formatCurrency(category.yearlyBudgetAmount)}</TableCell>
                    <TableCell>{formatCurrency(category.yearlySpending)}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={formatCurrency(category.yearlyRemaining)}
                        color={category.yearlyRemaining >= 0 ? 'success' : 'error'}
                      />
                    </TableCell>
                    {category.months.map((month) => (
                      <TableCell key={month.month} align="center">
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                          <Typography variant="caption">
                            {month.hasBudget ? formatCurrency(month.budgetAmount) : '—'}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {month.hasBudget ? formatCurrency(month.spending) : ''}
                          </Typography>
                        </Box>
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </Container>
  );
}
