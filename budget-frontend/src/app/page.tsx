'use client';

import { useEffect, useState } from 'react';
import {
  Container,
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';

interface HealthStatus {
  status: string;
  service: string;
  version: string;
}

export default function Home() {
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

        {/* Welcome Message */}
        <Paper elevation={2} sx={{ p: 4 }}>
          <Typography variant="h5" gutterBottom>
            Welcome to Your Budget Tracker
          </Typography>
          <Typography variant="body1" paragraph>
            This application helps you manage your household budget and track expenses.
            All members of your household can collaborate to:
          </Typography>
          <Box component="ul" sx={{ mt: 2 }}>
            <Typography component="li" variant="body1" paragraph>
              Create and manage budgets
            </Typography>
            <Typography component="li" variant="body1" paragraph>
              Track expenses and categorize spending
            </Typography>
            <Typography component="li" variant="body1" paragraph>
              Configure spending categories
            </Typography>
            <Typography component="li" variant="body1" paragraph>
              View dashboard and spending analytics
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 3 }}>
            Authenticated via Home Assistant (X-Hass-User header)
          </Typography>
        </Paper>
      </Box>
    </Container>
  );
}
