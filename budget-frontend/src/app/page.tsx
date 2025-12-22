'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Box,
  Typography,
  Paper,
  Grid,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import {
  AccountBalanceWallet as WalletIcon,
  Category as CategoryIcon,
  Dashboard as DashboardIcon,
  Receipt as ReceiptIcon,
} from '@mui/icons-material';
import { BudgetSummaryCard } from '@/components/home/BudgetSummaryCard';
import { QuickActionsCard } from '@/components/home/QuickActionsCard';
import { RecentActivityCard } from '@/components/home/RecentActivityCard';
import { FeatureNavigationCard } from '@/components/home/FeatureNavigationCard';

interface HealthStatus {
  status: string;
  service: string;
  version: string;
}

export default function Home() {
  const router = useRouter();
  const [backendHealth, setBackendHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const checkBackendHealth = async () => {
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
        const response = await fetch(`${apiUrl}/api/health`);

        if (!response.ok) {
          throw new Error(`Backend responded with status: ${response.status}`);
        }

        const data = await response.json();
        setBackendHealth(data);
        setError(null);
      } catch (err) {
        console.error('Failed to fetch backend health:', err);
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    checkBackendHealth();
  }, []);

  return (
    <Container maxWidth="lg">
      <Box sx={{ my: 4 }}>
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 6 }}>
          <Typography variant="h2" component="h1" gutterBottom>
            Home Budget Tracker
          </Typography>
          <Typography variant="h5" color="text.secondary" gutterBottom>
            Household Budget and Expense Tracking System
          </Typography>
        </Box>

        {/* Dashboard Widgets */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          {/* Budget Summary (left) */}
          <Grid item xs={12} md={6}>
            <BudgetSummaryCard />
          </Grid>

          {/* Recent Activity (right) */}
          <Grid item xs={12} md={6}>
            <RecentActivityCard />
          </Grid>

          {/* Quick Actions (full width) */}
          <Grid item xs={12}>
            <QuickActionsCard />
          </Grid>
        </Grid>

        {/* Features Navigation */}
        <Box sx={{ mb: 4 }}>
          <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
            Explore Features
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={3}>
              <FeatureNavigationCard
                title="Budgets"
                description="Create and manage monthly budgets for your household"
                icon={<WalletIcon />}
                path="/budgets"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <FeatureNavigationCard
                title="Categories"
                description="Organize spending with hierarchical category budgets"
                icon={<CategoryIcon />}
                path="/categories"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <FeatureNavigationCard
                title="Dashboard"
                description="View spending analytics and budget insights"
                icon={<DashboardIcon />}
                path="/dashboard"
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <FeatureNavigationCard
                title="Expenses"
                description="Record and track daily household expenses"
                icon={<ReceiptIcon />}
                path="/expenses/new"
              />
            </Grid>
          </Grid>
        </Box>

        {/* Backend Health Status */}
        <Paper elevation={3} sx={{ p: 4, mb: 4 }}>
          <Typography variant="h5" gutterBottom>
            System Status
          </Typography>

          {loading ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 2 }}>
              <CircularProgress size={24} />
              <Typography>Checking backend connection...</Typography>
            </Box>
          ) : error ? (
            <Alert severity="error" icon={<ErrorIcon />} sx={{ mt: 2 }}>
              <Typography variant="body1" gutterBottom>
                <strong>Backend Connection Failed</strong>
              </Typography>
              <Typography variant="body2">{error}</Typography>
            </Alert>
          ) : backendHealth ? (
            <Box sx={{ mt: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <CheckCircleIcon color="success" fontSize="large" />
                <Typography variant="h6" color="success.main">
                  Backend Connected
                </Typography>
              </Box>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <Box>
                    <Typography color="text.secondary" gutterBottom variant="body2">
                      Status
                    </Typography>
                    <Chip
                      label={backendHealth.status}
                      color={backendHealth.status === 'UP' ? 'success' : 'error'}
                      size="small"
                    />
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box>
                    <Typography color="text.secondary" gutterBottom variant="body2">
                      Service
                    </Typography>
                    <Typography variant="body1">
                      {backendHealth.service}
                    </Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Box>
                    <Typography color="text.secondary" gutterBottom variant="body2">
                      Version
                    </Typography>
                    <Typography variant="body1">
                      {backendHealth.version}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </Box>
          ) : null}
        </Paper>

        {/* Info */}
        <Paper elevation={1} sx={{ p: 3, mt: 4, bgcolor: 'background.default' }}>
          <Typography variant="body2" color="text.secondary">
            Authenticated via Home Assistant (X-Hass-User header)
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
}
