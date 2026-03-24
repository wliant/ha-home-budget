'use client';

import React, { useState, useCallback } from 'react';
import { Box, Chip, Typography, IconButton, Collapse } from '@mui/material';
import { ExpandMore, ExpandLess } from '@mui/icons-material';
import type { CategoryDTO } from '@/types/category';

interface CategoryChipFilterProps {
  categories: CategoryDTO[]; // hierarchy (root categories with childCategories)
  selectedCategoryIds: Set<number>;
  onSelectionChange: (ids: Set<number>) => void;
}

export default function CategoryChipFilter({
  categories,
  selectedCategoryIds,
  onSelectionChange,
}: CategoryChipFilterProps) {
  const [expanded, setExpanded] = useState(false);

  const getChildren = (cat: CategoryDTO): CategoryDTO[] =>
    cat.childCategories ?? [];

  const allCategoryIds = new Set<number>();
  for (const parent of categories) {
    if (parent.id != null) allCategoryIds.add(parent.id);
    for (const child of getChildren(parent)) {
      if (child.id != null) allCategoryIds.add(child.id);
    }
  }

  const isAllSelected = selectedCategoryIds.size === 0;

  const handleAllClick = () => {
    onSelectionChange(new Set());
  };

  const handleParentClick = useCallback((parent: CategoryDTO) => {
    const parentId = parent.id!;
    const next = new Set(selectedCategoryIds);
    if (next.has(parentId)) {
      next.delete(parentId);
    } else {
      next.add(parentId);
    }
    onSelectionChange(next);
  }, [selectedCategoryIds, onSelectionChange]);

  const handleChildClick = useCallback((childId: number) => {
    const next = new Set(selectedCategoryIds);
    if (next.has(childId)) {
      next.delete(childId);
    } else {
      next.add(childId);
    }
    onSelectionChange(next);
  }, [selectedCategoryIds, onSelectionChange]);

  // Summary text for collapsed state
  const selectedCount = selectedCategoryIds.size;
  const totalCount = allCategoryIds.size;
  const summaryText = isAllSelected
    ? 'All categories'
    : `${selectedCount} of ${totalCount} categories`;

  return (
    <Box sx={{ width: '100%' }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 0.5,
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onClick={() => setExpanded(!expanded)}
      >
        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 500 }}>
          Category:
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {summaryText}
        </Typography>
        <IconButton size="small" sx={{ p: 0, ml: 0.5 }}>
          {expanded ? <ExpandLess fontSize="small" /> : <ExpandMore fontSize="small" />}
        </IconButton>
        {!expanded && (
          <Chip
            label="All"
            size="small"
            color={isAllSelected ? 'primary' : 'default'}
            variant={isAllSelected ? 'filled' : 'outlined'}
            onClick={(e) => { e.stopPropagation(); handleAllClick(); }}
            sx={{ ml: 1, ...(isAllSelected ? {} : { opacity: 0.6 }) }}
          />
        )}
      </Box>
      <Collapse in={expanded}>
        <Box sx={{ mt: 0.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5, flexWrap: 'wrap' }}>
            <Chip
              label="All"
              size="small"
              color={isAllSelected ? 'primary' : 'default'}
              variant={isAllSelected ? 'filled' : 'outlined'}
              onClick={handleAllClick}
              sx={isAllSelected ? {} : { opacity: 0.6 }}
            />
          </Box>
          {categories.map((parent) => {
            const children = getChildren(parent);
            const parentId = parent.id!;
            const isParentSelected = !isAllSelected && selectedCategoryIds.has(parentId);

            return (
              <Box key={parentId} sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5, flexWrap: 'wrap' }}>
                <Chip
                  label={`${parent.icon ? parent.icon + ' ' : ''}${parent.name}`}
                  size="small"
                  color={isParentSelected ? 'primary' : 'default'}
                  variant={isParentSelected ? 'filled' : 'outlined'}
                  onClick={() => handleParentClick(parent)}
                  sx={{
                    fontWeight: 600,
                    ...(!isAllSelected && !isParentSelected ? { opacity: 0.5 } : {}),
                  }}
                />
                {children.map((child) => {
                  const childId = child.id!;
                  const isSelected = !isAllSelected && selectedCategoryIds.has(childId);
                  return (
                    <Chip
                      key={childId}
                      label={`${child.icon ? child.icon + ' ' : ''}${child.name}`}
                      size="small"
                      color={isSelected ? 'primary' : 'default'}
                      variant={isSelected ? 'filled' : 'outlined'}
                      onClick={() => handleChildClick(childId)}
                      sx={!isAllSelected && !isSelected ? { opacity: 0.5 } : {}}
                    />
                  );
                })}
              </Box>
            );
          })}
        </Box>
      </Collapse>
    </Box>
  );
}
