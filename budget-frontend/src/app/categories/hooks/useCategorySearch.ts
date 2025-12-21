/**
 * Custom hook for category search and filtering
 * Feature 005: Category Management UI Enhancements - User Story 3
 */

import { useState, useMemo, useCallback } from 'react';
import { CategoryDTO } from '../../../types/category';

interface UseCategorySearchProps {
  categories: CategoryDTO[];
  hierarchy: CategoryDTO[];
  viewMode: 'flat' | 'hierarchy';
}

interface UseCategorySearchReturn {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  clearSearch: () => void;
  filteredCategories: CategoryDTO[];
  filteredHierarchy: CategoryDTO[];
  hasResults: boolean;
}

/**
 * Hook for managing category search state and filtering logic
 */
export const useCategorySearch = ({
  categories,
  hierarchy,
  viewMode,
}: UseCategorySearchProps): UseCategorySearchReturn => {
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Clear search
  const clearSearch = useCallback(() => {
    setSearchQuery('');
  }, []);

  // Filter categories by search query
  const filteredCategories = useMemo(() => {
    if (!searchQuery.trim()) {
      return categories;
    }

    const query = searchQuery.toLowerCase().trim();

    return categories.filter(category =>
      category.name.toLowerCase().includes(query)
    );
  }, [categories, searchQuery]);

  // Filter hierarchy by search query
  // Preserve parent context when searching for child categories
  const filteredHierarchy = useMemo(() => {
    if (!searchQuery.trim()) {
      return hierarchy;
    }

    const query = searchQuery.toLowerCase().trim();
    const result: CategoryDTO[] = [];

    hierarchy.forEach(parent => {
      // Check if parent matches
      const parentMatches = parent.name.toLowerCase().includes(query);

      // Check if any child matches
      const matchingChildren =
        parent.childCategories?.filter(child =>
          child.name.toLowerCase().includes(query)
        ) || [];

      // If parent matches, include it with all children
      if (parentMatches) {
        result.push({
          ...parent,
          childCategories: parent.childCategories,
        });
      }
      // If only children match, include parent with matching children
      else if (matchingChildren.length > 0) {
        result.push({
          ...parent,
          childCategories: matchingChildren,
        });
      }
    });

    return result;
  }, [hierarchy, searchQuery]);

  // Check if there are any results
  const hasResults = viewMode === 'hierarchy'
    ? filteredHierarchy.length > 0
    : filteredCategories.length > 0;

  return {
    searchQuery,
    setSearchQuery,
    clearSearch,
    filteredCategories,
    filteredHierarchy,
    hasResults,
  };
};
