/**
 * Client-side validation rules for category management
 * Implements validation logic from Feature 005 specification
 */

export interface ValidationResult {
  valid: boolean;
  error?: string;
}

/**
 * Validate category name
 * Rules:
 * - Required (non-empty after trim)
 * - Max length 100 characters
 * - Sanitized (no HTML special characters)
 */
export const validateCategoryName = (name: string): ValidationResult => {
  // Check if name is provided and not empty
  const trimmedName = name?.trim() || '';

  if (trimmedName.length === 0) {
    return {
      valid: false,
      error: 'Category name is required'
    };
  }

  // Check max length
  if (trimmedName.length > 100) {
    return {
      valid: false,
      error: 'Name must be 100 characters or less'
    };
  }

  // Check for special characters that should be sanitized
  if (/<|>/.test(trimmedName)) {
    return {
      valid: false,
      error: 'Only letters, numbers, and basic punctuation allowed'
    };
  }

  return { valid: true };
};

/**
 * Sanitize category name by removing/encoding HTML special characters
 */
export const sanitizeCategoryName = (name: string): string => {
  return name
    .replace(/</g, '')
    .replace(/>/g, '')
    .trim();
};

/**
 * Validate category icon (emoji)
 * Rules:
 * - Optional field
 * - If provided, should be a valid emoji character
 */
export const validateCategoryIcon = (icon?: string): ValidationResult => {
  // Icon is optional
  if (!icon || icon.trim().length === 0) {
    return { valid: true };
  }

  // Simple check: emoji should be 1-2 characters (including multi-byte Unicode)
  // This is a basic validation - actual emoji validation is complex
  const trimmedIcon = icon.trim();
  if (trimmedIcon.length > 10) {
    return {
      valid: false,
      error: 'Icon should be a single emoji character'
    };
  }

  return { valid: true };
};

/**
 * Validate parent category selection
 * Rules:
 * - No circular reference (parent cannot be self)
 * - No 3-level hierarchy (parent's parent must be null)
 */
export const validateParentCategory = (
  currentCategoryId: number | undefined,
  parentCategoryId: number | undefined,
  parentCategory?: { parentCategoryId?: number | null }
): ValidationResult => {
  // Parent is optional
  if (!parentCategoryId) {
    return { valid: true };
  }

  // Check circular reference (editing mode only)
  if (currentCategoryId && currentCategoryId === parentCategoryId) {
    return {
      valid: false,
      error: 'Category cannot be its own parent'
    };
  }

  // Check 3-level hierarchy prevention
  if (parentCategory && parentCategory.parentCategoryId != null) {
    return {
      valid: false,
      error: 'Cannot create 3-level hierarchy. Selected parent already has a parent.'
    };
  }

  return { valid: true };
};

/**
 * Check if category name is unique (async validation)
 * This should be called with actual categories list
 */
export const checkDuplicateName = (
  name: string,
  existingCategories: Array<{ id?: number; name: string }>,
  currentCategoryId?: number
): ValidationResult => {
  const trimmedName = name.trim().toLowerCase();

  const duplicate = existingCategories.find(
    cat =>
      cat.name.toLowerCase() === trimmedName &&
      cat.id !== currentCategoryId
  );

  if (duplicate) {
    return {
      valid: false,
      error: 'Category name already exists'
    };
  }

  return { valid: true };
};

/**
 * Validate if parent category can be changed
 * Rule: Cannot change parent if category has active budgets
 */
export const canChangeParent = (
  budgetCount: number,
  isEditMode: boolean,
  originalParentId?: number,
  newParentId?: number
): ValidationResult => {
  // In create mode, always allowed
  if (!isEditMode) {
    return { valid: true };
  }

  // If parent hasn't changed, no validation needed
  if (originalParentId === newParentId) {
    return { valid: true };
  }

  // If has budgets, cannot change parent
  if (budgetCount > 0) {
    return {
      valid: false,
      error: 'Cannot change parent - category has active budgets'
    };
  }

  return { valid: true };
};
