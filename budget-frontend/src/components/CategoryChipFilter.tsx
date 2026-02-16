'use client';

import React from 'react';
import { Box, Chip, Typography } from '@mui/material';
import type { CategoryDTO } from '@/types/category';

interface CategoryChipFilterProps {
  categories: CategoryDTO[]; // hierarchy (root categories with childCategories)
  selectedCategoryId?: number;
  onSelect: (categoryId?: number) => void;
}

export default function CategoryChipFilter({
  categories,
  selectedCategoryId,
  onSelect,
}: CategoryChipFilterProps) {
  const getChildren = (cat: CategoryDTO): CategoryDTO[] =>
    cat.childCategories ?? [];

  const handleClick = (id: number) => {
    onSelect(selectedCategoryId === id ? undefined : id);
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1, flexWrap: 'wrap' }}>
        <Typography variant="caption" color="text.secondary" sx={{ mr: 0.5 }}>
          Category:
        </Typography>
        <Chip
          label="All"
          size="small"
          color={selectedCategoryId == null ? 'primary' : 'default'}
          variant={selectedCategoryId == null ? 'filled' : 'outlined'}
          onClick={() => onSelect(undefined)}
          sx={selectedCategoryId != null ? { opacity: 0.6 } : {}}
        />
      </Box>
      {categories.map((parent) => {
        const children = getChildren(parent);
        const parentId = parent.id!;
        const isParentSelected = selectedCategoryId === parentId;

        return (
          <Box key={parentId} sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5, flexWrap: 'wrap' }}>
            <Chip
              label={`${parent.icon ? parent.icon + ' ' : ''}${parent.name}`}
              size="small"
              color={isParentSelected ? 'primary' : 'default'}
              variant={isParentSelected ? 'filled' : 'outlined'}
              onClick={() => handleClick(parentId)}
              sx={{
                fontWeight: 600,
                ...(selectedCategoryId != null && !isParentSelected && !children.some(c => c.id === selectedCategoryId)
                  ? { opacity: 0.6 }
                  : {}),
              }}
            />
            {children.map((child) => {
              const childId = child.id!;
              const isChildSelected = selectedCategoryId === childId;
              return (
                <Chip
                  key={childId}
                  label={`${child.icon ? child.icon + ' ' : ''}${child.name}`}
                  size="small"
                  color={isChildSelected ? 'primary' : 'default'}
                  variant={isChildSelected ? 'filled' : 'outlined'}
                  onClick={() => handleClick(childId)}
                  sx={selectedCategoryId != null && !isChildSelected ? { opacity: 0.6 } : {}}
                />
              );
            })}
          </Box>
        );
      })}
    </Box>
  );
}
