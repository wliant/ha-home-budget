import api from './api';

/**
 * Budget service for managing budgets.
 * Provides typed methods for all budget-related API operations.
 */

// ============================================================================
// Type Definitions
// ============================================================================

export interface ParentCategoryBudgetUpdateInfo {
  parentCategoryName: string;
  previousAmount: number;
  newAmount: number;
  year: number;
  month?: number | null;
}

export interface BudgetDTO {
  id?: number;
  year: number;
  month?: number | null; // 1-12 or null for yearly
  totalAmount: number;
  description?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  categoryId?: number;
  category?: CategoryDTO;
  autoCreateChildren?: boolean;
  createParentBudget?: boolean;
  extendParentBudget?: boolean;
  parentTotalAmount?: number;
  parentCategoryBudgetUpdated?: ParentCategoryBudgetUpdateInfo;
}

export interface BudgetSummaryDTO {
  id: number;
  year: number;
  month?: number | null;
  totalAmount: number;
  description?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  totalSpending: number;
  spendingPercentage: number;
  expenseCount: number;
  expenses?: ExpenseDTO[];
  categoryId?: number;
  category?: CategoryDTO;
  childrenBudgetSum?: number;
  childrenSpending?: number;
  isParentCategory?: boolean;
}

export interface ExpenseDTO {
  id?: number;
  amount: number;
  description: string;
  expenseDate: string; // ISO date string
  budgetId: number;
  categoryId?: number | null;
  category?: CategoryDTO;
  categoryName?: string;
  categoryIcon?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  warnings?: string[];
  files?: { id: number; originalFilename: string }[];
}

export interface CategoryDTO {
  id: number;
  name: string;
  icon?: string;
  isSystem?: boolean;
  expenseCount?: number;
  parentCategoryId?: number;
  parentCategory?: CategoryDTO;
  childCategories?: CategoryDTO[];
  children?: CategoryDTO[];
}

export interface CreateBudgetRequest {
  year: number;
  month?: number | null;
  totalAmount: number;
  description?: string;
  categoryId: number;
  autoCreateChildren?: boolean;
  createParentBudget?: boolean;
  extendParentBudget?: boolean;
  parentTotalAmount?: number;
  createParentCategoryBudget?: boolean;
  parentCategoryBudgetAmount?: number;
}

export interface UpdateBudgetRequest {
  totalAmount: number;
  description?: string;
}

export interface BudgetValidationDTO {
  duplicate: boolean;
  duplicateMessage?: string;
  parentBudgetExists: boolean;
  parentBudgetId?: number;
  parentBudgetAmount?: number;
  monthlyBudgetSum: number;
  monthlyBudgetsExist: boolean;
  parentCategoryBudgetExists: boolean;
  parentCategoryBudgetId?: number;
  parentCategoryBudgetAmount?: number;
  parentCategoryName?: string;
}

export interface YearlyMonthlyBudgetDTO {
  month: number;
  budgetAmount: number;
  spending: number;
  remaining: number;
  hasBudget: boolean;
}

export interface YearlyCategoryBudgetDTO {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  parentCategoryId?: number;
  parentCategoryName?: string;
  yearlyBudgetAmount: number;
  monthlyBudgetSum: number;
  yearlySpending: number;
  yearlyRemaining: number;
  months: YearlyMonthlyBudgetDTO[];
}

export interface YearlyBudgetViewDTO {
  year: number;
  totalBudget: number;
  totalSpending: number;
  totalRemaining: number;
  categories: YearlyCategoryBudgetDTO[];
}

// ============================================================================
// Budget Service
// ============================================================================

export const budgetService = {
  /**
   * Get all budgets with spending summaries.
   * Returns budgets ordered by date (newest first).
   */
  getAllBudgets: async (): Promise<BudgetSummaryDTO[]> => {
    const response = await api.get<BudgetSummaryDTO[]>('/api/budgets');
    return response.data;
  },

  /**
   * Get budget by ID with expenses.
   */
  getBudgetById: async (id: number): Promise<BudgetSummaryDTO> => {
    const response = await api.get<BudgetSummaryDTO>(`/api/budgets/${id}`);
    return response.data;
  },

  /**
   * Create a new budget.
   * Validates that no budget exists for the same month.
   */
  createBudget: async (request: CreateBudgetRequest): Promise<BudgetDTO> => {
    const response = await api.post<BudgetDTO>('/api/budgets', request);
    return response.data;
  },

  /**
   * Update budget (amount and description only).
   * Year and month are immutable.
   */
  updateBudget: async (id: number, request: UpdateBudgetRequest): Promise<BudgetDTO> => {
    const response = await api.put<BudgetDTO>(`/api/budgets/${id}`, request);
    return response.data;
  },

  /**
   * Delete budget.
   * Cascade deletes all associated expenses.
   */
  deleteBudget: async (id: number): Promise<void> => {
    await api.delete(`/api/budgets/${id}`);
  },

  getCurrentMonthBudget: async (): Promise<BudgetSummaryDTO> => {
    const response = await api.get<BudgetSummaryDTO>('/api/budgets/current');
    return response.data;
  },

  getMonthlyBudgetSummary: async (year: number, month: number): Promise<BudgetSummaryDTO> => {
    const response = await api.get<BudgetSummaryDTO>(`/api/budgets/monthly-summary?year=${year}&month=${month}`);
    return response.data;
  },

  getBudgetValidation: async (categoryId: number, year: number, month?: number | null): Promise<BudgetValidationDTO> => {
    const params = new URLSearchParams({ categoryId: String(categoryId), year: String(year) });
    if (month) {
      params.append('month', String(month));
    }
    const response = await api.get<BudgetValidationDTO>(`/api/budgets/validation?${params.toString()}`);
    return response.data;
  },

  getYearlyBudgetView: async (year: number): Promise<YearlyBudgetViewDTO> => {
    const response = await api.get<YearlyBudgetViewDTO>(`/api/budgets/yearly?year=${year}`);
    return response.data;
  },
};

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Get month name from month number (1-12).
 */
export const getMonthName = (month: number): string => {
  const monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ];
  return monthNames[month - 1] || '';
};

/**
 * Format budget period (e.g., "January 2025").
 */
export const formatBudgetPeriod = (year: number, month?: number | null): string => {
  if (!month) {
    return `${year} (Yearly)`;
  }
  return `${getMonthName(month)} ${year}`;
};

/**
 * Format currency amount.
 */
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

/**
 * Get spending status color based on percentage.
 */
export const getSpendingStatusColor = (percentage: number): string => {
  if (percentage < 50) return 'success'; // Green
  if (percentage < 75) return 'info'; // Blue
  if (percentage < 90) return 'warning'; // Orange
  return 'error'; // Red
};

/**
 * Get spending status text.
 */
export const getSpendingStatusText = (percentage: number): string => {
  if (percentage < 50) return 'On track';
  if (percentage < 75) return 'Good';
  if (percentage < 90) return 'Watch spending';
  if (percentage < 100) return 'Near limit';
  return 'Over budget';
};

export default budgetService;
