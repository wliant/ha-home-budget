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
  categoryId: number;
  categoryName?: string;
  categoryIcon?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  files?: ExpenseFileDTO[];
}

export interface ExpenseFileDTO {
  id: number;
  originalFilename: string;
}

export interface CreateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string;
  categoryId: number;
  commonExpense?: boolean;
}

export interface UpdateExpenseRequest {
  amount: number;
  description: string;
  expenseDate: string;
  categoryId: number;
}

export interface ExpenseFilters {
  categoryId?: number;
  startDate?: string; // YYYY-MM-DD
  endDate?: string; // YYYY-MM-DD
  createdBy?: string;
}

export interface ExpenseListFilters {
  year: number;
  month?: number;
  categoryId?: number;
  minAmount?: number;
  maxAmount?: number;
  createdBy?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface ExpenseListResponse {
  content: ExpenseDTO[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  totalAmount: number;
  sortBy: string;
  sortDirection: string;
}

export interface CategoryExpenseAggregate {
  categoryId: number;
  categoryName: string;
  categoryIcon?: string;
  parentCategoryId?: number | null;
  directAmount: number;
  childrenAmount: number;
  totalAmount: number;
  year: number;
  month?: number | null;
}

export const expenseService = {
  /**
   * Get all expenses with optional filters
   */
  getAllExpenses: async (filters?: ExpenseFilters): Promise<ExpenseDTO[]> => {
    const params = new URLSearchParams();

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
  createExpense: async (request: CreateExpenseRequest, files?: File[]): Promise<ExpenseDTO> => {
    if (files && files.length > 0) {
      const formData = new FormData();
      formData.append('expense', new Blob([JSON.stringify(request)], { type: 'application/json' }));
      files.forEach((file) => formData.append('files', file));
      const response = await api.post<ExpenseDTO>('/api/expenses', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return response.data;
    }
    const response = await api.post<ExpenseDTO>('/api/expenses', request);
    return response.data;
  },

  /**
   * Update existing expense
   */
  updateExpense: async (id: number, request: UpdateExpenseRequest, files?: File[]): Promise<ExpenseDTO> => {
    if (files && files.length > 0) {
      const formData = new FormData();
      formData.append('expense', new Blob([JSON.stringify(request)], { type: 'application/json' }));
      files.forEach((file) => formData.append('files', file));
      const response = await api.put<ExpenseDTO>(`/api/expenses/${id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return response.data;
    }
    const response = await api.put<ExpenseDTO>(`/api/expenses/${id}`, request);
    return response.data;
  },

  /**
   * Delete expense
   */
  deleteExpense: async (id: number): Promise<void> => {
    await api.delete(`/api/expenses/${id}`);
  },

  /**
   * Get paginated, filtered, sorted expense list (Feature 011)
   */
  getExpenseList: async (filters: ExpenseListFilters): Promise<ExpenseListResponse> => {
    const params = new URLSearchParams();
    params.append('year', filters.year.toString());

    if (filters.month != null) params.append('month', filters.month.toString());
    if (filters.categoryId != null) params.append('categoryId', filters.categoryId.toString());
    if (filters.minAmount != null) params.append('minAmount', filters.minAmount.toString());
    if (filters.maxAmount != null) params.append('maxAmount', filters.maxAmount.toString());
    if (filters.createdBy) params.append('createdBy', filters.createdBy);
    if (filters.page != null) params.append('page', filters.page.toString());
    if (filters.size != null) params.append('size', filters.size.toString());
    if (filters.sortBy) params.append('sortBy', filters.sortBy);
    if (filters.sortDirection) params.append('sortDirection', filters.sortDirection);

    const response = await api.get<ExpenseListResponse>(`/api/expenses/list?${params.toString()}`);
    return response.data;
  },

  /**
   * Get distinct years with expenses (Feature 011)
   */
  getExpenseYears: async (): Promise<number[]> => {
    const response = await api.get<number[]>('/api/expenses/years');
    return response.data;
  },

  /**
   * Get distinct expense creators (Feature 011)
   */
  getExpenseCreators: async (): Promise<string[]> => {
    const response = await api.get<string[]>('/api/expenses/creators');
    return response.data;
  },

  /**
   * Get monthly expense aggregates per category (Feature 016)
   */
  getMonthlyAggregates: async (year: number, month?: number): Promise<CategoryExpenseAggregate[]> => {
    const params = new URLSearchParams({ year: year.toString() });
    if (month != null) params.append('month', month.toString());
    const response = await api.get<CategoryExpenseAggregate[]>(`/api/expenses/aggregates/monthly?${params.toString()}`);
    return response.data;
  },

  /**
   * Get yearly expense aggregates per category (Feature 016)
   */
  getYearlyAggregates: async (year: number): Promise<CategoryExpenseAggregate[]> => {
    const response = await api.get<CategoryExpenseAggregate[]>(`/api/expenses/aggregates/yearly?year=${year}`);
    return response.data;
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

