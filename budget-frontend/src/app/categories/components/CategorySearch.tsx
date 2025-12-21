/**
 * CategorySearch - Search bar component for filtering categories
 * Feature 005: Category Management UI Enhancements - User Story 3
 */

'use client';

import React from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
} from '@mui/material';
import {
  Search as SearchIcon,
  Clear as ClearIcon,
} from '@mui/icons-material';

interface CategorySearchProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onClearSearch: () => void;
}

/**
 * Search component for real-time category filtering
 */
export const CategorySearch: React.FC<CategorySearchProps> = ({
  searchQuery,
  onSearchChange,
  onClearSearch,
}) => {
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onSearchChange(event.target.value);
  };

  return (
    <TextField
      fullWidth
      placeholder="Search categories..."
      value={searchQuery}
      onChange={handleChange}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon />
          </InputAdornment>
        ),
        endAdornment: searchQuery && (
          <InputAdornment position="end">
            <IconButton
              size="small"
              onClick={onClearSearch}
              edge="end"
              title="Clear search"
            >
              <ClearIcon />
            </IconButton>
          </InputAdornment>
        ),
      }}
      sx={{ mb: 3 }}
    />
  );
};
