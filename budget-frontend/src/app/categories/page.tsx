'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  Container,
  Typography,
  Box,
  Button,
  Grid,
  Card,
  CardContent,
  CardActions,
  Alert,
  CircularProgress,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Chip,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  ToggleButtonGroup,
  ToggleButton,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { categoryService, CategoryDTO, COMMON_CATEGORY_ICONS } from '@/services/categoryService';

/**
 * Categories management page - User Story 1: Hierarchical Category Management
 */

export default function CategoriesPage() {
  const router = useRouter();
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [hierarchy, setHierarchy] = useState<CategoryDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState({ name: '', icon: '', parentCategoryId: undefined as number | undefined });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [viewMode, setViewMode] = useState<'flat' | 'hierarchy'>('hierarchy');

  useEffect(() => {
    loadCategories();
  }, []);

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

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Category name is required');
      return;
    }

    setIsSubmitting(true);
    try {
      await categoryService.createCategory({
        name: formData.name.trim(),
        icon: formData.icon || '',
        parentCategoryId: formData.parentCategoryId,
      });
      setDialogOpen(false);
      setFormData({ name: '', icon: '', parentCategoryId: undefined });
      await loadCategories();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create category');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`Delete category "${name}"?`)) return;

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
      <Card sx={{ ml: isChild ? 2 : 0, borderLeft: isChild ? '4px solid #1976d2' : 'none' }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            {category.icon && (
              <Typography variant="h4">{category.icon}</Typography>
            )}
            <Typography variant="h6">{category.name}</Typography>
          </Box>
          {category.parentCategory && (
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
              Parent: {category.parentCategory.icon} {category.parentCategory.name}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary">
            {category.expenseCount || 0} {category.expenseCount === 1 ? 'expense' : 'expenses'}
          </Typography>
          {category.childCategories && category.childCategories.length > 0 && (
            <Typography variant="body2" color="primary" sx={{ mt: 0.5 }}>
              {category.childCategories.length} {category.childCategories.length === 1 ? 'subcategory' : 'subcategories'}
            </Typography>
          )}
          {category.createdBy && (
            <Typography variant="caption" color="text.secondary" display="block">
              Created by {category.createdBy}
            </Typography>
          )}
        </CardContent>
        <CardActions>
          {category.id && (
            <IconButton
              size="small"
              onClick={() => handleDelete(category.id!, category.name)}
              color="error"
            >
              <DeleteIcon />
            </IconButton>
          )}
        </CardActions>
      </Card>
    </Grid>
  );

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h1">
          Spending Categories
        </Typography>
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
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
            Add Category
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
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
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
            Create First Category
          </Button>
        </Box>
      ) : viewMode === 'hierarchy' ? (
        <Grid container spacing={3}>
          {hierarchy.map((parent) => (
            <React.Fragment key={parent.id}>
              {renderCategoryCard(parent, false)}
              {parent.childCategories?.map((child) => renderCategoryCard(child, true))}
            </React.Fragment>
          ))}
        </Grid>
      ) : (
        <Grid container spacing={3}>
          {categories.map((category) => renderCategoryCard(category, false))}
        </Grid>
      )}

      {/* Create Category Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Category</DialogTitle>
        <DialogContent>
          <TextField
            label="Category Name"
            fullWidth
            required
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            sx={{ mt: 2, mb: 2 }}
          />
          <TextField
            label="Icon (emoji)"
            fullWidth
            value={formData.icon}
            onChange={(e) => setFormData({ ...formData, icon: e.target.value })}
            placeholder="e.g., 🛒"
            sx={{ mb: 2 }}
          />
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Common icons:
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
            {COMMON_CATEGORY_ICONS.slice(0, 12).map((item) => (
              <Chip
                key={item.emoji}
                label={`${item.emoji} ${item.label}`}
                onClick={() => setFormData({ ...formData, icon: item.emoji })}
                variant={formData.icon === item.emoji ? "filled" : "outlined"}
              />
            ))}
          </Box>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Parent Category (Optional)</InputLabel>
            <Select
              value={formData.parentCategoryId || ''}
              onChange={(e) => setFormData({ ...formData, parentCategoryId: e.target.value ? Number(e.target.value) : undefined })}
              label="Parent Category (Optional)"
            >
              <MenuItem value="">
                <em>None (Root Category)</em>
              </MenuItem>
              {hierarchy.map((parent) => (
                <MenuItem key={parent.id} value={parent.id} disabled={parent.childCategories && parent.childCategories.length >= 1}>
                  {parent.icon} {parent.name}
                  {parent.childCategories && parent.childCategories.length >= 1 && ' (has children)'}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Alert severity="info" sx={{ mt: 2 }}>
            Categories support 2-level hierarchy. Parent categories cannot have their own parent.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={handleCreate} variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
