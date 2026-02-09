'use client';

import { createTheme } from '@mui/material/styles';

// Warm Earth color palette for Home Budget Tracker
// Primary: Terracotta, Secondary: Olive, Accent: Gold
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#A0522D', // Terracotta - AppBar, sidebar active, primary buttons
      light: '#C07850',
      dark: '#7A3B1E',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#6B7B3A', // Olive - secondary buttons, accents, badges
      light: '#8A9E50',
      dark: '#4E5C2B',
      contrastText: '#ffffff',
    },
    info: {
      main: '#C49A3C', // Gold - highlights, feature cards, info states
      light: '#D4B060',
      dark: '#9A7A2E',
      contrastText: '#ffffff',
    },
    success: {
      main: '#4A7A49', // Sage Green - on-track budgets, positive indicators
      light: '#7DAF7C',
      dark: '#3D6B3C',
      contrastText: '#ffffff',
    },
    warning: {
      main: '#D4A030', // Amber - budget warnings, approaching limits
      light: '#E0B850',
      dark: '#B08020',
      contrastText: '#3E2723',
    },
    error: {
      main: '#C0392B', // Burnt Red - over-budget, errors, delete actions
      light: '#D95548',
      dark: '#9A2D22',
    },
    background: {
      default: '#FAF6F0', // Warm cream
      paper: '#ffffff',
    },
    text: {
      primary: '#3E2723', // Dark brown
      secondary: '#5D4037', // Medium brown
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 500,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
    },
    button: {
      textTransform: 'none',
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          background: 'linear-gradient(135deg, #7A3B1E 0%, #A0522D 100%)',
          boxShadow: '0 2px 8px rgba(160,82,45,0.15)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 16px',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(160,82,45,0.08)',
          border: '1px solid rgba(160,82,45,0.08)',
          transition: 'box-shadow 0.3s ease, transform 0.3s ease',
          '&:hover': {
            boxShadow: '0 4px 16px rgba(160,82,45,0.15)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          boxShadow: '0 2px 8px rgba(160,82,45,0.08)',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:nth-of-type(even)': {
            backgroundColor: '#FAF6F0',
          },
          '&:hover': {
            backgroundColor: 'rgba(192,120,80,0.08) !important',
          },
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: '#F5EDE3',
            fontWeight: 600,
            color: '#3E2723',
          },
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          '&.Mui-selected': {
            backgroundColor: 'rgba(192,120,80,0.15)',
            borderLeft: '3px solid #A0522D',
            '&:hover': {
              backgroundColor: 'rgba(192,120,80,0.2)',
            },
          },
          '&:hover': {
            backgroundColor: 'rgba(192,120,80,0.08)',
          },
        },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: {
          borderRadius: 4,
          height: 8,
        },
        bar: {
          borderRadius: 4,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 16,
        },
      },
    },
  },
});

export default theme;
