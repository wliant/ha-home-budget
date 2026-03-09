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
  categoryId?: number;
  category?: CategoryDTO;
  childrenBudgetSum?: number;
  childrenSpending?: number;
  isParentCategory?: boolean;
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
   * Expenses are not affected (decoupled from budgets).
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
// Re-export shared utilities for backward compatibility
// ============================================================================

export {
  getMonthName,
  formatBudgetPeriod,
  formatCurrency,
  getSpendingStatusColor,
  getSpendingStatusText,
} from '@/utils/formatters';

export default budgetService;
