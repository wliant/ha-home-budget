import type { ReactNode } from 'react';
import {
  Home as HomeIcon,
  Dashboard as DashboardIcon,
  AccountBalanceWallet as WalletIcon,
  Category as CategoryIcon,
  Receipt as ReceiptIcon,
  CalendarMonth as CalendarIcon,
  ListAlt as ListAltIcon,
} from '@mui/icons-material';

export type NavItem = {
  label: string;
  href: string;
  icon: ReactNode;
  match?: (pathname: string) => boolean;
};

export type BreadcrumbItem = {
  label: string;
  href?: string;
};

export const navItems: NavItem[] = [
  {
    label: 'Home',
    href: '/',
    icon: <HomeIcon />,
    match: (pathname) => pathname === '/',
  },
  {
    label: 'Dashboard',
    href: '/dashboard',
    icon: <DashboardIcon />,
    match: (pathname) => pathname === '/dashboard',
  },
  {
    label: 'Budgets',
    href: '/budgets',
    icon: <WalletIcon />,
    match: (pathname) => pathname === '/budgets' || /^\/budgets\/\d+/.test(pathname) || pathname === '/budgets/new',
  },
  {
    label: 'Yearly View',
    href: '/budgets/yearly',
    icon: <CalendarIcon />,
    match: (pathname) => pathname === '/budgets/yearly',
  },
  {
    label: 'Categories',
    href: '/categories',
    icon: <CategoryIcon />,
    match: (pathname) => pathname === '/categories',
  },
  {
    label: 'Expenses',
    href: '/expenses',
    icon: <ListAltIcon />,
    match: (pathname) => pathname === '/expenses',
  },
  {
    label: 'Record Expense',
    href: '/expenses/new',
    icon: <ReceiptIcon />,
    match: (pathname) => pathname === '/expenses/new',
  },
];

const breadcrumbDefinitions: Array<{ regex: RegExp; crumbs: BreadcrumbItem[] }> = [
  {
    regex: /^\/dashboard$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Dashboard' },
    ],
  },
  {
    regex: /^\/budgets$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Budgets' },
    ],
  },
  {
    regex: /^\/budgets\/new$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Budgets', href: '/budgets' },
      { label: 'New Budget' },
    ],
  },
  {
    regex: /^\/budgets\/yearly$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Budgets', href: '/budgets' },
      { label: 'Yearly View' },
    ],
  },
  {
    regex: /^\/budgets\/\d+$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Budgets', href: '/budgets' },
      { label: 'Budget Details' },
    ],
  },
  {
    regex: /^\/categories$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Categories' },
    ],
  },
  {
    regex: /^\/expenses$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Expenses' },
    ],
  },
  {
    regex: /^\/expenses\/new$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Expenses', href: '/expenses' },
      { label: 'Record Expense' },
    ],
  },
];

const toTitleCase = (value: string) =>
  value
    .replace(/-/g, ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase());

export const getBreadcrumbs = (pathname: string): BreadcrumbItem[] => {
  const normalized = pathname.replace(/\?.*$/, '').replace(/\/$/, '') || '/';

  if (normalized === '/') {
    return [];
  }

  const matched = breadcrumbDefinitions.find((entry) => entry.regex.test(normalized));
  if (matched) {
    return matched.crumbs;
  }

  const segments = normalized.split('/').filter(Boolean);
  const crumbs: BreadcrumbItem[] = [{ label: 'Home', href: '/' }];
  let currentPath = '';

  segments.forEach((segment) => {
    currentPath += `/${segment}`;
    if (/^\d+$/.test(segment)) {
      crumbs.push({ label: 'Details' });
    } else {
      crumbs.push({ label: toTitleCase(segment), href: currentPath });
    }
  });

  return crumbs;
};
