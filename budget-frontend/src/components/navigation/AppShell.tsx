'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  AppBar,
  Box,
  Breadcrumbs,
  Button,
  Divider,
  Drawer,
  IconButton,
  Link as MuiLink,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import { useTheme } from '@mui/material/styles';
import { getBreadcrumbs, navItems } from './navConfig';

const drawerWidth = 240;

type AppShellProps = {
  children: React.ReactNode;
};

export default function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const breadcrumbs = useMemo(() => getBreadcrumbs(pathname), [pathname]);

  const handleDrawerToggle = () => {
    setMobileOpen((open) => !open);
  };

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 2, py: 2 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Navigation
        </Typography>
      </Box>
      <Divider />
      <List sx={{ px: 1 }}>
        {navItems.map((item) => {
          const isSelected = item.match ? item.match(pathname) : pathname === item.href;
          return (
            <ListItemButton
              key={item.href}
              component={Link}
              href={item.href}
              selected={isSelected}
              aria-current={isSelected ? 'page' : undefined}
              onClick={() => {
                if (!isDesktop) {
                  setMobileOpen(false);
                }
              }}
            >
              <ListItemIcon sx={{ color: isSelected ? 'primary.main' : 'text.secondary' }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          );
        })}
      </List>
      <Box sx={{ mt: 'auto', px: 2, pb: 2 }}>
        <Typography variant="caption" color="text.secondary">
          Home Budget Tracker
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: (t) => t.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ gap: 2 }}>
          {!isDesktop && (
            <IconButton
              aria-label="open navigation"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ color: '#ffffff' }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Typography
            variant="h6"
            component={Link}
            href="/"
            sx={{
              textDecoration: 'none',
              color: '#ffffff',
              fontWeight: 600,
              flexGrow: 1,
            }}
          >
            Home Budget Tracker
          </Typography>
          <Button
            component={Link}
            href="/expenses/new"
            variant="contained"
            sx={{
              bgcolor: 'rgba(255,255,255,0.15)',
              color: '#ffffff',
              '&:hover': { bgcolor: 'rgba(255,255,255,0.25)' },
            }}
          >
            Record Expense
          </Button>
        </Toolbar>
      </AppBar>

      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
        aria-label="primary navigation"
      >
        <Drawer
          variant={isDesktop ? 'permanent' : 'temporary'}
          open={isDesktop ? true : mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: drawerWidth,
              boxSizing: 'border-box',
              bgcolor: '#F5EDE3',
              borderRight: '1px solid rgba(160,82,45,0.1)',
            },
          }}
        >
          <Toolbar />
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${drawerWidth}px)` },
        }}
      >
        <Toolbar />
        <Box sx={{ px: { xs: 2, md: 4 }, py: { xs: 2, md: 3 } }}>
          {breadcrumbs.length > 0 && (
            <Box sx={{ mb: 3 }}>
              <nav aria-label="breadcrumbs">
                <Breadcrumbs>
                  {breadcrumbs.map((crumb, index) => {
                    const isLast = index === breadcrumbs.length - 1;
                    if (crumb.href && !isLast) {
                      return (
                        <MuiLink
                          key={`${crumb.label}-${crumb.href}`}
                          component={Link}
                          href={crumb.href}
                          color="inherit"
                          underline="hover"
                        >
                          {crumb.label}
                        </MuiLink>
                      );
                    }

                    return (
                      <Typography key={`${crumb.label}-${index}`} color="text.primary">
                        {crumb.label}
                      </Typography>
                    );
                  })}
                </Breadcrumbs>
              </nav>
            </Box>
          )}
          {children}
        </Box>
      </Box>
    </Box>
  );
}
