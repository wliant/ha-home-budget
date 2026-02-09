'use client';

import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Grid,
  Alert,
  CircularProgress,
  ToggleButtonGroup,
  ToggleButton,
} from '@mui/material';
import {
  Add as AddIcon,
  Category as CategoryIcon,
} from '@mui/icons-material';
import { categoryService } from '@/services/categoryService';
import { CategoryDTO } from '@/types/category';
import { CategoryCard } from './components/CategoryCard';
import { CategoryDialog } from './components/CategoryDialog';
import { CategorySearch } from './components/CategorySearch';
import { useCategoryForm } from './hooks/useCategoryForm';
import { useCategorySearch } from './hooks/useCategorySearch';

/**
 * Categories management page - User Story 1: Hierarchical Category Management
 */

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [hierarchy, setHierarchy] = useState<CategoryDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<'create' | 'edit'>('create');
  const [editingCategory, setEditingCategory] = useState<CategoryDTO | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize view mode from localStorage
  const [viewMode, setViewMode] = useState<'flat' | 'hierarchy'>(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('categoryViewMode');
      return (stored === 'flat' || stored === 'hierarchy') ? stored : 'hierarchy';
    }
    return 'hierarchy';
  });

  // Category form hook for validation and state management
  const categoryForm = useCategoryForm({
    mode: dialogMode,
    initialCategory: editingCategory,
    existingCategories: categories,
    allCategories: categories,
  });

  // Category search hook for filtering
  const categorySearch = useCategorySearch({
    categories,
    hierarchy,
    viewMode,
  });

  useEffect(() => {
    loadCategories();
  }, []);

  // Persist view mode to localStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('categoryViewMode', viewMode);
    }
  }, [viewMode]);

  const loadCategories = async () => {
    setIsLoading(true);
    try {
      const [allCategories, hierarchyData] = await Promise.all([
        categoryService.getAllCategories(),
        categoryService.getCategoryHierarchy(),
      ]);
      setCategories(allCategories);
      setHierarchy(hierarchyData);
    } catch (err: any) {
      console.error('Failed to load categories:', err);
      setError('Failed to load categories');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenCreateDialog = () => {
    setDialogMode('create');
    setEditingCategory(undefined);
    categoryForm.resetForm();
    setDialogOpen(true);
  };

  const handleOpenEditDialog = (category: CategoryDTO) => {
    setDialogMode('edit');
    setEditingCategory(category);
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingCategory(undefined);
    categoryForm.resetForm();
  };

  const handleSubmit = async () => {
    // Validate form
    if (!categoryForm.validateForm()) {
      return;
    }

    setIsSubmitting(true);
    try {
      if (dialogMode === 'create') {
        await categoryService.createCategory({
          name: categoryForm.formState.name.trim(),
          icon: categoryForm.formState.icon || '',
          parentCategoryId: categoryForm.formState.parentCategoryId,
        });
      } else if (dialogMode === 'edit' && editingCategory?.id) {
        await categoryService.updateCategory(editingCategory.id, {
          name: categoryForm.formState.name.trim(),
          icon: categoryForm.formState.icon || '',
          parentCategoryId: categoryForm.formState.parentCategoryId,
        });
      }

      handleCloseDialog();
      await loadCategories();
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        `Failed to ${dialogMode} category`
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCategory = (category: CategoryDTO) => {
    if (!category.id) return;

    if (!confirm(`Delete category "${category.name}"?`)) return;

    handleDelete(category.id, category.name);
  };

  const handleDelete = async (id: number, name: string) => {
    try {
      await categoryService.deleteCategory(id);
      await loadCategories();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete category. It may have associated expenses.');
    }
  };

  // Render a single category card
  const renderCategoryCard = (category: CategoryDTO, isChild: boolean = false) => (
    <Grid item xs={12} sm={6} md={4} key={category.id}>
      <CategoryCard
        category={category}
        isChild={isChild}
        onEdit={handleOpenEditDialog}
        onDelete={handleDeleteCategory}
      />
    </Grid>
  );

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <CategoryIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Typography variant="h4" component="h1">
            Spending Categories
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <ToggleButtonGroup
            value={viewMode}
            exclusive
            onChange={(e, newMode) => newMode && setViewMode(newMode)}
            size="small"
          >
            <ToggleButton value="hierarchy">Hierarchy</ToggleButton>
            <ToggleButton value="flat">Flat</ToggleButton>
          </ToggleButtonGroup>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreateDialog}>
            Add Category
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {/* Search Bar */}
      {!isLoading && categories.length > 0 && (
        <CategorySearch
          searchQuery={categorySearch.searchQuery}
          onSearchChange={categorySearch.setSearchQuery}
          onClearSearch={categorySearch.clearSearch}
        />
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : categories.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No categories yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Create your first category to organize expenses
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreateDialog}>
            Create First Category
          </Button>
        </Box>
      ) : !categorySearch.hasResults ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No categories found
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            No categories match &quot;{categorySearch.searchQuery}&quot;
          </Typography>
          <Button variant="outlined" onClick={categorySearch.clearSearch}>
            Clear Search
          </Button>
        </Box>
      ) : viewMode === 'hierarchy' ? (
        <Grid container spacing={3}>
          {categorySearch.filteredHierarchy.map((parent) => (
            <React.Fragment key={parent.id}>
              {renderCategoryCard(parent, false)}
              {parent.childCategories?.map((child) => renderCategoryCard(child, true))}
            </React.Fragment>
          ))}
        </Grid>
      ) : (
        <Grid container spacing={3}>
          {categorySearch.filteredCategories.map((category) => renderCategoryCard(category, false))}
        </Grid>
      )}

      {/* Create/Edit Category Dialog */}
      <CategoryDialog
        open={dialogOpen}
        mode={dialogMode}
        category={editingCategory}
        allCategories={categories}
        hierarchyRoots={hierarchy}
        formState={categoryForm.formState}
        isSubmitting={isSubmitting}
        onClose={handleCloseDialog}
        onSubmit={handleSubmit}
        onNameChange={categoryForm.setName}
        onIconChange={categoryForm.setIcon}
        onParentChange={categoryForm.setParentCategoryId}
      />
    </Container>
  );
}
