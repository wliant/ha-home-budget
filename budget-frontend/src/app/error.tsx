'use client';

import { useEffect } from 'react';
import { Box, Button, Container, Typography } from '@mui/material';
import { ErrorOutline as ErrorIcon } from '@mui/icons-material';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Application error:', error);
  }, [error]);

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          textAlign: 'center',
          gap: 2,
        }}
      >
        <ErrorIcon sx={{ fontSize: 64, color: 'error.main' }} />
        <Typography variant="h5" component="h1">
          Something went wrong
        </Typography>
        <Typography variant="body1" color="text.secondary">
          An unexpected error occurred. Please try again.
        </Typography>
        <Button variant="contained" onClick={reset} sx={{ mt: 2 }}>
          Try Again
        </Button>
      </Box>
    </Container>
  );
}
