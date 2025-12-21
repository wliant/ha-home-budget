import api from './api';

/**
 * Category service - API client for category operations
 *
 * Implements User Story 3: Manage Spending Categories
 */

export interface CategoryDTO {
  id?: number;
  name: string;
  icon?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
  isSystem?: boolean;
  expenseCount?: number;
  parentCategoryId?: number;
  parentCategory?: CategoryDTO;
  childCategories?: CategoryDTO[];
  budgetCount?: number;
}

export interface CreateCategoryRequest {
  name: string;
  icon?: string;
  parentCategoryId?: number;
}

export interface UpdateCategoryRequest {
  name: string;
  icon?: string;
  parentCategoryId?: number;
}

export const categoryService = {
  /**
   * Get all categories
   */
  getAllCategories: async (): Promise<CategoryDTO[]> => {
    const response = await api.get<CategoryDTO[]>('/api/categories');
    return response.data;
  },

  /**
   * Get category by ID
   */
  getCategoryById: async (id: number): Promise<CategoryDTO> => {
    const response = await api.get<CategoryDTO>(`/api/categories/${id}`);
    return response.data;
  },

  /**
   * Create new category
   */
  createCategory: async (request: CreateCategoryRequest): Promise<CategoryDTO> => {
    const response = await api.post<CategoryDTO>('/api/categories', request);
    return response.data;
  },

  /**
   * Update existing category
   */
  updateCategory: async (id: number, request: UpdateCategoryRequest): Promise<CategoryDTO> => {
    const response = await api.put<CategoryDTO>(`/api/categories/${id}`, request);
    return response.data;
  },

  /**
   * Delete category
   */
  deleteCategory: async (id: number): Promise<void> => {
    await api.delete(`/api/categories/${id}`);
  },

  /**
   * Get expense count for category
   */
  getExpenseCount: async (id: number): Promise<number> => {
    const response = await api.get<number>(`/api/categories/${id}/expense-count`);
    return response.data;
  },

  /**
   * Get category hierarchy (root categories with children)
   */
  getCategoryHierarchy: async (): Promise<CategoryDTO[]> => {
    const response = await api.get<CategoryDTO[]>('/api/categories/hierarchy');
    return response.data;
  },
};

/**
 * Format category display with icon
 */
export const formatCategoryDisplay = (category: CategoryDTO): string => {
  return category.icon ? `${category.icon} ${category.name}` : category.name;
};

/**
 * Common category icons/emojis
 */
export const COMMON_CATEGORY_ICONS = [
  { emoji: '🛒', label: 'Groceries' },
  { emoji: '⚡', label: 'Utilities' },
  { emoji: '🚗', label: 'Transportation' },
  { emoji: '🏠', label: 'Housing' },
  { emoji: '🍽️', label: 'Dining' },
  { emoji: '🎬', label: 'Entertainment' },
  { emoji: '👕', label: 'Clothing' },
  { emoji: '💊', label: 'Healthcare' },
  { emoji: '📚', label: 'Education' },
  { emoji: '🎁', label: 'Gifts' },
  { emoji: '📱', label: 'Technology' },
  { emoji: '✈️', label: 'Travel' },
  { emoji: '🏋️', label: 'Fitness' },
  { emoji: '🐕', label: 'Pets' },
  { emoji: '🔧', label: 'Maintenance' },
  { emoji: '💰', label: 'Savings' },
  { emoji: '📝', label: 'Other' },
];
