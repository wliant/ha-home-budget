'use client';

import React from 'react';
import { useIngressRouter } from '../../lib/navigation';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Button,
  Box,
} from '@mui/material';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';

/**
 * Feature Navigation Card Component
 * User Story 4: Active Feature Navigation (P2)
 *
 * Displays feature card with:
 * - Icon, title, description
 * - "View [Feature]" button with navigation
 * - No opacity effect (all fully visible)
 * - Responsive grid layout
 */
interface FeatureNavigationCardProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  path: string;
  color?: string;
}

export function FeatureNavigationCard({ title, description, icon, path, color = 'primary.main' }: FeatureNavigationCardProps) {
  const router = useIngressRouter();

  const handleNavigate = () => {
    router.push(path);
  };

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderLeft: `4px solid ${color}`,
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        },
      }}
    >
      <CardContent sx={{ flexGrow: 1 }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            mb: 2,
          }}
        >
          <Box
            sx={{
              fontSize: 40,
              mr: 2,
              display: 'flex',
              alignItems: 'center',
              color: 'primary.main',
            }}
          >
            {icon}
          </Box>
          <Typography variant="h6" component="div">
            {title}
          </Typography>
        </Box>

        <Typography variant="body2" color="text.secondary">
          {description}
        </Typography>
      </CardContent>

      <CardActions>
        <Button
          size="small"
          endIcon={<ArrowForwardIcon />}
          onClick={handleNavigate}
          sx={{ ml: 1 }}
          aria-label={`View ${title} page`}
        >
          View {title}
        </Button>
      </CardActions>
    </Card>
  );
}
