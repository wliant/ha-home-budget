/**
 * CategoryCard - Enhanced card component with metadata and actions
 * Feature 005: Category Management UI Enhancements - User Story 1 & 2
 */

'use client';

import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Box,
  IconButton,
  Chip,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  AdminPanelSettings as SystemIcon,
} from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { CategoryDTO } from '../../../types/category';

interface CategoryCardProps {
  category: CategoryDTO;
  isChild?: boolean;
  onEdit: (category: CategoryDTO) => void;
  onDelete: (category: CategoryDTO) => void;
}

/**
 * Card component displaying category information with edit/delete actions
 * Memoized for performance optimization
 */
export const CategoryCard: React.FC<CategoryCardProps> = React.memo(function CategoryCard({
  category,
  isChild = false,
  onEdit,
  onDelete,
}) {
  const theme = useTheme();

  const handleEdit = () => {
    onEdit(category);
  };

  const handleDelete = () => {
    onDelete(category);
  };

  return (
    <Card
      sx={{
        ml: isChild ? 2 : 0,
        borderLeft: isChild ? `4px solid ${theme.palette.primary.main}` : 'none',
        bgcolor: isChild ? 'rgba(138,158,80,0.06)' : 'background.paper',
        fontWeight: isChild ? 'normal' : 'bold',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        ...(!isChild && {
          boxShadow: theme.shadows[3],
        }),
        '&:hover': isChild ? {
          bgcolor: 'rgba(138,158,80,0.12)',
          transition: 'background-color 0.2s ease',
        } : undefined,
      }}
    >
      <CardContent sx={{ flexGrow: 1 }}>
        {/* Category name, icon, and system badge */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1, flexWrap: 'wrap' }}>
          {category.icon && (
            <Typography variant="h4">{category.icon}</Typography>
          )}
          <Typography variant="h6" sx={{ flexGrow: 1 }}>{category.name}</Typography>
          {category.isSystem && (
            <Chip
              icon={<SystemIcon />}
              label="System"
              size="small"
              color="warning"
              variant="outlined"
            />
          )}
        </Box>

        {/* Parent category info (for child categories) */}
        {category.parentCategory && (
          <Typography
            variant="caption"
            color="text.secondary"
            display="block"
            sx={{ mb: 1 }}
          >
            Parent: {category.parentCategory.icon} {category.parentCategory.name}
          </Typography>
        )}

        {/* Expense count and budget count */}
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Typography variant="body2" color="text.secondary">
            {category.expenseCount || 0}{' '}
            {category.expenseCount === 1 ? 'expense' : 'expenses'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {category.budgetCount || 0}{' '}
            {category.budgetCount === 1 ? 'budget' : 'budgets'}
          </Typography>
        </Box>

        {/* Subcategories count (for parent categories) */}
        {category.childCategories && category.childCategories.length > 0 && (
          <Typography variant="body2" color="primary" sx={{ mt: 0.5 }}>
            {category.childCategories.length}{' '}
            {category.childCategories.length === 1 ? 'subcategory' : 'subcategories'}
          </Typography>
        )}

        {/* Creation metadata */}
        {category.createdBy && (
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
            Created by {category.createdBy}
            {category.createdAt && ` • ${new Date(category.createdAt).toLocaleDateString()}`}
          </Typography>
        )}
      </CardContent>

      <CardActions>
        {/* Edit button - disabled for system categories */}
        <IconButton
          size="small"
          onClick={handleEdit}
          color="primary"
          title={category.isSystem ? 'System categories cannot be edited' : 'Edit category'}
          disabled={category.isSystem}
        >
          <EditIcon />
        </IconButton>

        {/* Delete button - disabled for system categories */}
        {category.id && (
          <IconButton
            size="small"
            onClick={handleDelete}
            color="error"
            title={category.isSystem ? 'System categories cannot be deleted' : 'Delete category'}
            disabled={category.isSystem}
          >
            <DeleteIcon />
          </IconButton>
        )}
      </CardActions>
    </Card>
  );
});
