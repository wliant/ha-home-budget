/**
 * Shared TypeScript interfaces for category management
 * Feature 005: Category Management UI Enhancements
 */

/**
 * Category Data Transfer Object
 * Matches backend CategoryDTO structure
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
  budgetCount?: number;
  parentCategoryId?: number;
  parentCategory?: CategoryDTO;
  childCategories?: CategoryDTO[];
}

/**
 * UI State for categories page
 */
export interface CategoryUIState {
  categories: CategoryDTO[];
  hierarchy: CategoryDTO[];
  viewMode: 'flat' | 'hierarchy';
  searchQuery: string;
  selectedIds: Set<number>;
  dialogState: {
    open: boolean;
    mode: 'create' | 'edit';
    category?: CategoryDTO;
  };
  loading: boolean;
  error?: string;
}

/**
 * Category form state
 */
export interface CategoryFormState {
  name: string;
  icon: string;
  parentCategoryId?: number;
  errors: Record<string, string>;
}

/**
 * Bulk action state
 */
export interface BulkActionState {
  selectedCategories: CategoryDTO[];
  actionType?: 'delete' | 'changeParent';
  targetParentId?: number;
}

/**
 * Category analytics data
 */
export interface AnalyticsData {
  categoryId: number;
  budgetTotal: number;
  spentTotal: number;
  utilization: number; // 0-100 percentage
  monthlyTrend: Array<{
    month: string;
    amount: number;
  }>;
}

/**
 * Request DTOs for API calls
 */
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
