'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CardActions,
  Chip,
  CircularProgress,
  Alert,
  Button,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';

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
                  <Card variant="outlined">
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Status
                      </Typography>
                      <Chip
                        label={backendHealth.status}
                        color={backendHealth.status === 'UP' ? 'success' : 'error'}
                        size="small"
                      />
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Service
                      </Typography>
                      <Typography variant="body1">
                        {backendHealth.service}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} sm={4}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Version
                      </Typography>
                      <Typography variant="body1">
                        {backendHealth.version}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </Box>
          ) : null}
        </Paper>

        {/* Features */}
        <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
          Features
        </Typography>

        <Grid container spacing={3}>
          {/* Budgets Feature */}
          <Grid item xs={12} md={6}>
            <Card elevation={2}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <AccountBalanceWalletIcon color="primary" fontSize="large" />
                  <Typography variant="h6">
                    Budgets
                  </Typography>
                </Box>
                <Typography variant="body1" paragraph>
                  Create and manage monthly budgets. Track your spending against your budget
                  and see how much you have left in real-time.
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Features:
                </Typography>
                <Box component="ul" sx={{ mt: 1, pl: 2 }}>
                  <Typography component="li" variant="body2" color="text.secondary">
                    Create monthly budgets
                  </Typography>
                  <Typography component="li" variant="body2" color="text.secondary">
                    View spending progress
                  </Typography>
                  <Typography component="li" variant="body2" color="text.secondary">
                    Track expenses per budget
                  </Typography>
                </Box>
              </CardContent>
              <CardActions>
                <Button
                  size="medium"
                  variant="contained"
                  onClick={() => router.push('/budgets')}
                  fullWidth
                >
                  View Budgets
                </Button>
              </CardActions>
            </Card>
          </Grid>

          {/* Coming Soon Cards */}
          <Grid item xs={12} md={6}>
            <Card elevation={2} sx={{ opacity: 0.6 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Expenses
                </Typography>
                <Typography variant="body1" color="text.secondary">
                  Record and categorize expenses. Coming soon...
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card elevation={2} sx={{ opacity: 0.6 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Categories
                </Typography>
                <Typography variant="body1" color="text.secondary">
                  Manage spending categories. Coming soon...
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={6}>
            <Card elevation={2} sx={{ opacity: 0.6 }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Dashboard
                </Typography>
                <Typography variant="body1" color="text.secondary">
                  View insights and analytics. Coming soon...
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

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
