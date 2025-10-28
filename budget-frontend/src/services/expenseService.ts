import api from './api';

/**
 * Expense service - API client for expense operations
 *
 * Implements User Story 2: Record Expenses Against Budgets
 */

export interface ExpenseDTO {
  id?: number;
  amount: number;
  description: string;
  expenseDate: string; // ISO date string (YYYY-MM-DD)
  budgetId: number;
  categoryId?: number | null;
  categoryName?: string;
  categoryIcon?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  warnings?: string[];
}

export interface CreateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string;
  budgetId: number;
  categoryId?: number | null;
}

export interface UpdateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string;
  budgetId: number;
  categoryId?: number | null;
}

export interface ExpenseFilters {
  budgetId?: number;
  categoryId?: number;
  startDate?: string; // YYYY-MM-DD
  endDate?: string; // YYYY-MM-DD
  createdBy?: string;
}

export const expenseService = {
  /**
   * Get all expenses with optional filters
   */
  getAllExpenses: async (filters?: ExpenseFilters): Promise<ExpenseDTO[]> => {
    const params = new URLSearchParams();

    if (filters?.budgetId) params.append('budgetId', filters.budgetId.toString());
    if (filters?.categoryId) params.append('categoryId', filters.categoryId.toString());
    if (filters?.startDate) params.append('startDate', filters.startDate);
    if (filters?.endDate) params.append('endDate', filters.endDate);
    if (filters?.createdBy) params.append('createdBy', filters.createdBy);

    const queryString = params.toString();
    const url = queryString ? `/api/expenses?${queryString}` : '/api/expenses';

    const response = await api.get<ExpenseDTO[]>(url);
    return response.data;
  },

  /**
   * Get expense by ID
   */
  getExpenseById: async (id: number): Promise<ExpenseDTO> => {
    const response = await api.get<ExpenseDTO>(`/api/expenses/${id}`);
    return response.data;
  },

  /**
   * Create new expense
   */
  createExpense: async (request: CreateExpenseRequest): Promise<ExpenseDTO> => {
    const response = await api.post<ExpenseDTO>('/api/expenses', request);
    return response.data;
  },

  /**
   * Update existing expense
   */
  updateExpense: async (id: number, request: UpdateExpenseRequest): Promise<ExpenseDTO> => {
    const response = await api.put<ExpenseDTO>(`/api/expenses/${id}`, request);
    return response.data;
  },

  /**
   * Delete expense
   */
  deleteExpense: async (id: number): Promise<void> => {
    await api.delete(`/api/expenses/${id}`);
  },
};

/**
 * Format expense date for display
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
 * Format expense amount for display
 */
export const formatExpenseAmount = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
};

/**
 * Get today's date in YYYY-MM-DD format
 */
export const getTodayISO = (): string => {
  const today = new Date();
  return today.toISOString().split('T')[0];
};

/**
 * Check if expense has date mismatch warning
 */
export const hasDateMismatchWarning = (expense: ExpenseDTO): boolean => {
  return expense.warnings && expense.warnings.length > 0;
};

/**
 * Get the first warning message if any
 */
export const getWarningMessage = (expense: ExpenseDTO): string | null => {
  if (expense.warnings && expense.warnings.length > 0) {
    return expense.warnings[0];
  }
  return null;
};
