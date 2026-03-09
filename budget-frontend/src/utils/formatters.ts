/**
 * Shared formatting and display utilities used across the frontend application.
 */

/**
 * Format currency amount (e.g., "$1,234.56").
 */
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

/**
 * Format currency for chart axes (no decimal places).
 */
export const formatCurrencyCompact = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
};

/**
 * Get month name from month number (1-12).
 */
export const getMonthName = (month: number): string => {
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];
  return monthNames[month - 1] || '';
};

/**
 * Format budget period (e.g., "January 2025" or "2025 (Yearly)").
 */
export const formatBudgetPeriod = (year: number, month?: number | null): string => {
  if (!month) {
    return `${year} (Yearly)`;
  }
  return `${getMonthName(month)} ${year}`;
};

/**
 * Format expense date for display (e.g., "Jan 15, 2025").
 */
export const formatExpenseDate = (dateString: string): string => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Format expense amount for display. Alias for formatCurrency.
 */
export const formatExpenseAmount = (amount: number): string => {
  return formatCurrency(amount);
};

/**
 * Get spending status color based on percentage.
 */
export const getSpendingStatusColor = (percentage: number): string => {
  if (percentage < 50) return 'success';
  if (percentage < 75) return 'info';
  if (percentage < 90) return 'warning';
  return 'error';
};

/**
 * Get spending status text based on percentage.
 */
export const getSpendingStatusText = (percentage: number): string => {
  if (percentage < 50) return 'On track';
  if (percentage < 75) return 'Good';
  if (percentage < 90) return 'Watch spending';
  if (percentage < 100) return 'Near limit';
  return 'Over budget';
};

/**
 * Get today's date in YYYY-MM-DD format.
 */
export const getTodayISO = (): string => {
  const today = new Date();
  return today.toISOString().split('T')[0];
};
