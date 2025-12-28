'use client';

import { useState, useEffect } from 'react';
import {
  Autocomplete,
  TextField,
  CircularProgress,
  Box,
  Typography,
} from '@mui/material';
import * as Icons from '@mui/icons-material';
import { categoryService } from '@/services/categoryService';
import { CategoryDTO } from '@/types/category';

interface CategorySelectProps {
  value: number | null;
  onChange: (categoryId: number | null) => void;
  required?: boolean;
  error?: string;
  disabled?: boolean;
}

export function CategorySelect({
  value,
  onChange,
  required = false,
  error,
  disabled = false,
}: CategorySelectProps) {
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const data = await categoryService.getAllCategories();
        setCategories(data);
        setFetchError(null);
      } catch (err) {
        console.error('Failed to fetch categories:', err);
        setFetchError('Failed to load categories');
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);

  const getCategoryLabel = (category: CategoryDTO): string => {
    if (category.parentCategory) {
      return `${category.parentCategory.name} > ${category.name}`;
    }
    return category.name;
  };

  // Render category icon (T015)
  const getCategoryIcon = (iconName?: string) => {
    if (!iconName) return null;

    // Convert snake_case or kebab-case to PascalCase for Material Icons
    const pascalCase = iconName
      .split(/[-_]/)
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join('');

    const IconComponent = (Icons as any)[pascalCase];
    return IconComponent ? <IconComponent fontSize="small" /> : null;
  };

  const selectedCategory = categories.find((c) => c.id === value) || null;

  return (
    <Autocomplete
      options={categories}
      value={selectedCategory}
      onChange={(_, newValue) => onChange(newValue?.id ?? null)}
      getOptionLabel={getCategoryLabel}
      loading={loading}
      disabled={disabled || loading || !!fetchError}
      // Render options with icons (T015)
      renderOption={(props, option) => (
        <Box component="li" {...props} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {getCategoryIcon(option.icon)}
          <Typography>{getCategoryLabel(option)}</Typography>
        </Box>
      )}
      renderInput={(params) => (
        <TextField
          {...params}
          label="Category"
          placeholder="Search categories..." // Search placeholder (T017)
          required={required}
          error={!!error || !!fetchError}
          helperText={error || fetchError}
          InputProps={{
            ...params.InputProps,
            startAdornment: (
              <>
                {selectedCategory && getCategoryIcon(selectedCategory.icon)}
                {params.InputProps.startAdornment}
              </>
            ),
            endAdornment: (
              <>
                {loading ? <CircularProgress color="inherit" size={20} /> : null}
                {params.InputProps.endAdornment}
              </>
            ),
          }}
        />
      )}
      fullWidth
    />
  );
}
