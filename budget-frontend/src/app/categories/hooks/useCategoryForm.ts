/**
 * Custom hook for category form state management and validation
 * Feature 005: Category Management UI Enhancements
 */

import { useState, useEffect, useCallback } from 'react';
import { CategoryDTO, CategoryFormState } from '../../../types/category';
import {
  validateCategoryName,
  validateCategoryIcon,
  validateParentCategory,
  checkDuplicateName,
  canChangeParent,
  sanitizeCategoryName,
} from '../../../utils/categoryValidation';

interface UseCategoryFormProps {
  mode: 'create' | 'edit';
  initialCategory?: CategoryDTO;
  existingCategories: CategoryDTO[];
  allCategories: CategoryDTO[];
}

interface UseCategoryFormReturn {
  formState: CategoryFormState;
  setName: (name: string) => void;
  setIcon: (icon: string) => void;
  setParentCategoryId: (id?: number) => void;
  validateForm: () => boolean;
  resetForm: () => void;
  hasErrors: boolean;
}

/**
 * Hook for managing category form state with inline validation
 */
export const useCategoryForm = ({
  mode,
  initialCategory,
  existingCategories,
  allCategories,
}: UseCategoryFormProps): UseCategoryFormReturn => {
  const [formState, setFormState] = useState<CategoryFormState>({
    name: initialCategory?.name || '',
    icon: initialCategory?.icon || '',
    parentCategoryId: initialCategory?.parentCategoryId,
    errors: {},
  });

  // Reset form when initial category changes
  useEffect(() => {
    if (initialCategory) {
      setFormState({
        name: initialCategory.name,
        icon: initialCategory.icon || '',
        parentCategoryId: initialCategory.parentCategoryId,
        errors: {},
      });
    }
  }, [initialCategory]);

  // Validate name field
  const validateNameField = useCallback(
    (name: string): string | undefined => {
      // Basic validation
      const basicValidation = validateCategoryName(name);
      if (!basicValidation.valid) {
        return basicValidation.error;
      }

      // Duplicate check
      const duplicateCheck = checkDuplicateName(
        name,
        existingCategories,
        initialCategory?.id
      );
      if (!duplicateCheck.valid) {
        return duplicateCheck.error;
      }

      return undefined;
    },
    [existingCategories, initialCategory?.id]
  );

  // Validate icon field
  const validateIconField = useCallback((icon: string): string | undefined => {
    const validation = validateCategoryIcon(icon);
    return validation.valid ? undefined : validation.error;
  }, []);

  // Validate parent category field
  const validateParentField = useCallback(
    (parentId?: number): string | undefined => {
      if (!parentId) {
        return undefined;
      }

      // Find parent category from all categories
      const parentCategory = allCategories.find(cat => cat.id === parentId);

      // Check circular reference and hierarchy depth
      const hierarchyValidation = validateParentCategory(
        initialCategory?.id,
        parentId,
        parentCategory
      );
      if (!hierarchyValidation.valid) {
        return hierarchyValidation.error;
      }

      // Check if parent can be changed (for edit mode)
      if (mode === 'edit' && initialCategory) {
        const budgetCount = initialCategory.budgetCount || 0;
        const changeValidation = canChangeParent(
          budgetCount,
          true,
          initialCategory.parentCategoryId,
          parentId
        );
        if (!changeValidation.valid) {
          return changeValidation.error;
        }
      }

      return undefined;
    },
    [allCategories, initialCategory, mode]
  );

  // Set name with validation
  const setName = useCallback(
    (name: string) => {
      const sanitized = sanitizeCategoryName(name);
      const error = validateNameField(sanitized);

      setFormState(prev => ({
        ...prev,
        name: sanitized,
        errors: {
          ...prev.errors,
          name: error || '',
        },
      }));
    },
    [validateNameField]
  );

  // Set icon with validation
  const setIcon = useCallback(
    (icon: string) => {
      const error = validateIconField(icon);

      setFormState(prev => ({
        ...prev,
        icon,
        errors: {
          ...prev.errors,
          icon: error || '',
        },
      }));
    },
    [validateIconField]
  );

  // Set parent category with validation
  const setParentCategoryId = useCallback(
    (id?: number) => {
      const error = validateParentField(id);

      setFormState(prev => ({
        ...prev,
        parentCategoryId: id,
        errors: {
          ...prev.errors,
          parentCategoryId: error || '',
        },
      }));
    },
    [validateParentField]
  );

  // Validate entire form
  const validateForm = useCallback((): boolean => {
    const nameError = validateNameField(formState.name);
    const iconError = validateIconField(formState.icon);
    const parentError = validateParentField(formState.parentCategoryId);

    const errors: Record<string, string> = {};
    if (nameError) errors.name = nameError;
    if (iconError) errors.icon = iconError;
    if (parentError) errors.parentCategoryId = parentError;

    setFormState(prev => ({ ...prev, errors }));

    return Object.keys(errors).length === 0;
  }, [formState.name, formState.icon, formState.parentCategoryId, validateNameField, validateIconField, validateParentField]);

  // Reset form
  const resetForm = useCallback(() => {
    setFormState({
      name: initialCategory?.name || '',
      icon: initialCategory?.icon || '',
      parentCategoryId: initialCategory?.parentCategoryId,
      errors: {},
    });
  }, [initialCategory]);

  // Check if there are any errors
  const hasErrors = Object.values(formState.errors).some(error => error !== '');

  return {
    formState,
    setName,
    setIcon,
    setParentCategoryId,
    validateForm,
    resetForm,
    hasErrors,
  };
};
