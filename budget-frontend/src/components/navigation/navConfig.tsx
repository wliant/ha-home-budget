import type { ReactNode } from 'react';
import {
  Home as HomeIcon,
  Dashboard as DashboardIcon,
  AccountBalanceWallet as WalletIcon,
  Category as CategoryIcon,
  ReceiptLong as ReceiptLongIcon,
  CalendarMonth as CalendarIcon,
  AddCard as AddCardIcon,
  CloudUpload as CloudUploadIcon,
  TrendingUp as TrendingUpIcon,
  Insights as InsightsIcon,
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
    label: 'Spending Trends',
    href: '/spending-trends',
    icon: <TrendingUpIcon />,
    match: (pathname) => pathname === '/spending-trends',
  },
  {
    label: 'Category Insights',
    href: '/category-insights',
    icon: <InsightsIcon />,
    match: (pathname) => pathname === '/category-insights',
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
    icon: <ReceiptLongIcon />,
    match: (pathname) => pathname === '/expenses',
  },
  {
    label: 'Bulk Upload',
    href: '/expenses/bulk-upload',
    icon: <CloudUploadIcon />,
    match: (pathname) => pathname === '/expenses/bulk-upload',
  },
  {
    label: 'Record Expense',
    href: '/expenses/new',
    icon: <AddCardIcon />,
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
    regex: /^\/spending-trends$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Spending Trends' },
    ],
  },
  {
    regex: /^\/category-insights$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Category Insights' },
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
    regex: /^\/expenses\/bulk-upload$/,
    crumbs: [
      { label: 'Home', href: '/' },
      { label: 'Expenses', href: '/expenses' },
      { label: 'Bulk Upload' },
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
