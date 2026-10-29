
import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    mode: 'light',

    primary: {
      main: '#2563EB',
      dark: '#1D4ED8',
      light: '#DBEAFE',
      contrastText: '#FFFFFF',
    },

    secondary: {
      main: '#64748B',
      light: '#F1F5F9',
      dark: '#334155',
    },

    success: {
      main: '#16A34A',
      light: '#DCFCE7',
      dark: '#15803D',
    },

    warning: {
      main: '#D97706',
      light: '#FEF3C7',
      dark: '#B45309',
    },

    error: {
      main: '#DC2626',
      light: '#FEE2E2',
      dark: '#B91C1C',
    },

    background: {
      default: '#F8FAFC',
      paper: '#FFFFFF',
    },

    text: {
      primary: '#0F172A',
      secondary: '#64748B',
    },

    divider: '#E2E8F0',
  },

  typography: {
    fontFamily: '"Inter", "Roboto", "Arial", sans-serif',

    h1: {
      fontSize: '2rem',
      fontWeight: 700,
      lineHeight: 1.25,
      letterSpacing: '-0.025em',
    },

    h2: {
      fontSize: '1.5rem',
      fontWeight: 700,
      lineHeight: 1.3,
      letterSpacing: '-0.02em',
    },

    h3: {
      fontSize: '1.125rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },

    h4: {
      fontSize: '1rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },

    body1: {
      fontSize: '0.9375rem',
      lineHeight: 1.6,
    },

    body2: {
      fontSize: '0.875rem',
      lineHeight: 1.5,
    },

    caption: {
      fontSize: '0.75rem',
      lineHeight: 1.5,
    },

    button: {
      textTransform: 'none',
      fontWeight: 600,
    },
  },

  shape: {
    borderRadius: 10,
  },

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          margin: 0,
          minHeight: '100vh',
        },
      },
    },

    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #E2E8F0',
          borderRadius: 16,
          boxShadow: '0 1px 3px rgba(15, 23, 42, 0.04)',
          backgroundImage: 'none',
        },
      },
    },

    MuiAppBar: {
      defaultProps: {
        elevation: 0,
      },
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },

    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
        },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 500,
        },
      },
    },
  },
})

export default theme
