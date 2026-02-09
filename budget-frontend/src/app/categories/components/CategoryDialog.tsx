/**
 * CategoryDialog - Create/Edit modal form for categories
 * Feature 005: Category Management UI Enhancements - User Story 1
 */

'use client';

import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Typography,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  FormHelperText,
} from '@mui/material';
import { CategoryDTO } from '../../../types/category';
import { COMMON_CATEGORY_ICONS } from '../../../services/categoryService';

interface CategoryDialogProps {
  open: boolean;
  mode: 'create' | 'edit';
  category?: CategoryDTO;
  allCategories: CategoryDTO[];
  hierarchyRoots: CategoryDTO[];
  formState: {
    name: string;
    icon: string;
    parentCategoryId?: number;
    errors: Record<string, string>;
  };
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: () => void;
  onNameChange: (name: string) => void;
  onIconChange: (icon: string) => void;
  onParentChange: (parentId?: number) => void;
}

/**
 * Dialog component for creating and editing categories
 */
export const CategoryDialog: React.FC<CategoryDialogProps> = ({
  open,
  mode,
  category,
  allCategories,
  hierarchyRoots,
  formState,
  isSubmitting,
  onClose,
  onSubmit,
  onNameChange,
  onIconChange,
  onParentChange,
}) => {
  const handleIconClick = (emoji: string) => {
    onIconChange(emoji);
  };

  const handleParentChange = (value: string | number) => {
    if (value === '' || value === 'none') {
      onParentChange(undefined);
    } else {
      onParentChange(Number(value));
    }
  };

  // Get available parent categories (roots only, excluding self in edit mode)
  const availableParents = hierarchyRoots.filter(root => {
    // In edit mode, exclude the category itself
    if (mode === 'edit' && category && root.id === category.id) {
      return false;
    }
    return true;
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {mode === 'create' ? 'Create New Category' : 'Edit Category'}
      </DialogTitle>

      <DialogContent>
        <TextField
          label="Category Name"
          fullWidth
          required
          value={formState.name}
          onChange={(e) => onNameChange(e.target.value)}
          error={!!formState.errors.name}
          helperText={formState.errors.name}
          sx={{ mt: 2, mb: 2 }}
          autoFocus
        />

        <TextField
          label="Icon (emoji)"
          fullWidth
          value={formState.icon}
          onChange={(e) => onIconChange(e.target.value)}
          error={!!formState.errors.icon}
          helperText={formState.errors.icon || 'Optional - choose an emoji to represent this category'}
          placeholder="e.g., 🛒"
          sx={{ mb: 2 }}
        />

        <Typography variant="body2" color="text.secondary" gutterBottom>
          Common icons:
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2, maxHeight: 160, overflowY: 'auto', pr: 0.5 }}>
          {COMMON_CATEGORY_ICONS.map((item) => (
            <Chip
              key={item.emoji}
              label={`${item.emoji} ${item.label}`}
              onClick={() => handleIconClick(item.emoji)}
              variant={formState.icon === item.emoji ? 'filled' : 'outlined'}
              color={formState.icon === item.emoji ? 'primary' : 'default'}
            />
          ))}
        </Box>

        <FormControl
          fullWidth
          sx={{ mb: 2 }}
          error={!!formState.errors.parentCategoryId}
        >
          <InputLabel>Parent Category (Optional)</InputLabel>
          <Select
            value={formState.parentCategoryId || ''}
            onChange={(e) => handleParentChange(e.target.value)}
            label="Parent Category (Optional)"
          >
            <MenuItem value="">
              <em>None (Root Category)</em>
            </MenuItem>
            {availableParents.map((parent) => (
              <MenuItem
                key={parent.id}
                value={parent.id}
                disabled={parent.parentCategoryId != null}
              >
                {parent.icon} {parent.name}
                {parent.parentCategoryId != null && ' (already has parent)'}
              </MenuItem>
            ))}
          </Select>
          {formState.errors.parentCategoryId && (
            <FormHelperText>{formState.errors.parentCategoryId}</FormHelperText>
          )}
        </FormControl>

        <Alert severity="info" sx={{ mt: 2 }}>
          Categories support 2-level hierarchy. A category can either be a root category or have one parent.
        </Alert>

        {mode === 'edit' && category?.budgetCount && category.budgetCount > 0 && (
          <Alert severity="warning" sx={{ mt: 1 }}>
            This category has {category.budgetCount} active {category.budgetCount === 1 ? 'budget' : 'budgets'}.
            Changing the parent category is not allowed.
          </Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} disabled={isSubmitting}>
          Cancel
        </Button>
        <Button
          onClick={onSubmit}
          variant="contained"
          disabled={isSubmitting || Object.values(formState.errors).some(e => e !== '')}
        >
          {isSubmitting
            ? (mode === 'create' ? 'Creating...' : 'Updating...')
            : (mode === 'create' ? 'Create' : 'Update')
          }
        </Button>
      </DialogActions>
    </Dialog>
  );
};
