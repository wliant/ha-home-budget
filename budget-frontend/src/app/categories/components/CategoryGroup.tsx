'use client';

import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  IconButton,
  Chip,
  Grid,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  AdminPanelSettings as SystemIcon,
} from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { CategoryDTO } from '../../../types/category';
import { CategoryCard } from './CategoryCard';

interface CategoryGroupProps {
  parent: CategoryDTO;
  onEdit: (category: CategoryDTO) => void;
  onDelete: (category: CategoryDTO) => void;
}

/**
 * Grouped view for a parent category with its children displayed inline.
 * Shows the parent as a section header with children as a sub-grid.
 */
export const CategoryGroup: React.FC<CategoryGroupProps> = React.memo(({
  parent,
  onEdit,
  onDelete,
}) => {
  const theme = useTheme();
  const children = parent.childCategories ?? [];

  return (
    <Card sx={{ boxShadow: theme.shadows[3] }}>
      <CardContent sx={{ pb: 1 }}>
        {/* Parent header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
          {parent.icon && (
            <Typography variant="h4">{parent.icon}</Typography>
          )}
          <Typography variant="h6" sx={{ flexGrow: 1 }}>{parent.name}</Typography>
          {parent.isSystem && (
            <Chip
              icon={<SystemIcon />}
              label="System"
              size="small"
              color="warning"
              variant="outlined"
            />
          )}
          <IconButton
            size="small"
            onClick={() => onEdit(parent)}
            color="primary"
            title={parent.isSystem ? 'System categories cannot be edited' : 'Edit category'}
            disabled={parent.isSystem}
          >
            <EditIcon />
          </IconButton>
          {parent.id && (
            <IconButton
              size="small"
              onClick={() => onDelete(parent)}
              color="error"
              title={parent.isSystem ? 'System categories cannot be deleted' : 'Delete category'}
              disabled={parent.isSystem}
            >
              <DeleteIcon />
            </IconButton>
          )}
        </Box>

        {/* Parent metadata */}
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 0.5 }}>
          <Typography variant="body2" color="primary">
            {children.length} {children.length === 1 ? 'subcategory' : 'subcategories'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {parent.expenseCount || 0} {parent.expenseCount === 1 ? 'expense' : 'expenses'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {parent.budgetCount || 0} {parent.budgetCount === 1 ? 'budget' : 'budgets'}
          </Typography>
        </Box>

        {/* Child category chips for at-a-glance view */}
        {children.length > 0 && (
          <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap', mt: 1 }}>
            {children.map((child) => (
              <Chip
                key={child.id}
                label={`${child.icon ? child.icon + ' ' : ''}${child.name}`}
                size="small"
                variant="outlined"
                color="primary"
              />
            ))}
          </Box>
        )}
      </CardContent>

      {/* Children sub-grid */}
      <Box sx={{ px: 2, pb: 2 }}>
        <Grid container spacing={2}>
          {children.map((child) => (
            <Grid item xs={12} sm={6} md={4} key={child.id}>
              <CategoryCard
                category={child}
                isChild
                onEdit={onEdit}
                onDelete={onDelete}
              />
            </Grid>
          ))}
        </Grid>
      </Box>
    </Card>
  );
});

CategoryGroup.displayName = 'CategoryGroup';
