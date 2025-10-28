package com.homebudget.service;

import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.CategoryInUseException;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.DuplicateCategoryException;
import com.homebudget.model.Category;
import com.homebudget.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing spending categories.
 *
 * Implements User Story 3: Manage Spending Categories
 * - Create, read, update, delete categories
 * - Validate unique category names
 * - Prevent deletion of categories with expenses
 * - Track category usage across household
 */
@Service
@Transactional
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Create a new category.
     *
     * @param dto Category data (name, optional icon)
     * @param username User creating the category (from X-Hass-User header)
     * @return Created category DTO
     * @throws DuplicateCategoryException if category name already exists
     */
    public CategoryDTO createCategory(CategoryDTO dto, String username) {
        logger.info("Creating category '{}', user: {}", dto.getName(), username);

        // Validate unique name
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateCategoryException(dto.getName());
        }

        // Create category entity
        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : "");
        category.setCreatedBy(username);

        // Save category
        Category saved = categoryRepository.save(category);
        logger.info("Created category ID: {} - '{}'", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    /**
     * Get all categories.
     *
     * @return List of all categories ordered by name
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        logger.info("Finding all categories");

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        logger.info("Found {} categories", categories.size());

        return categories.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID.
     *
     * @param id Category ID
     * @return Category DTO
     * @throws CategoryNotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        logger.info("Finding category by ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        return toDTO(category);
    }

    /**
     * Update an existing category.
     *
     * @param id Category ID
     * @param dto Updated category data
     * @return Updated category DTO
     * @throws CategoryNotFoundException if category not found
     * @throws DuplicateCategoryException if new name already exists
     */
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        logger.info("Updating category ID: {}", id);

        // Find existing category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        // Check if new name is different and already exists
        if (!category.getName().equalsIgnoreCase(dto.getName())) {
            if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
                throw new DuplicateCategoryException(dto.getName());
            }
        }

        // Update fields
        category.setName(dto.getName());
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : "");

        // Save updated category
        Category updated = categoryRepository.save(category);
        logger.info("Updated category ID: {} - '{}'", id, updated.getName());

        return toDTO(updated);
    }

    /**
     * Delete a category.
     * Prevents deletion if category has associated expenses.
     *
     * @param id Category ID
     * @throws CategoryNotFoundException if category not found
     * @throws CategoryInUseException if category has associated expenses
     */
    public void deleteCategory(Long id) {
        logger.info("Attempting to delete category ID: {}", id);

        // Find category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        // Check if category has expenses
        long expenseCount = categoryRepository.countExpensesByCategoryId(id);
        if (expenseCount > 0) {
            logger.warn("Cannot delete category ID: {} - has {} associated expenses", id, expenseCount);
            throw new CategoryInUseException(category.getName(), (int) expenseCount);
        }

        // Delete category
        categoryRepository.deleteById(id);
        logger.info("Deleted category ID: {} - '{}'", id, category.getName());
    }

    /**
     * Get expense count for a category.
     * Used to determine if category can be deleted.
     *
     * @param id Category ID
     * @return Number of expenses using this category
     */
    @Transactional(readOnly = true)
    public long getExpenseCount(Long id) {
        return categoryRepository.countExpensesByCategoryId(id);
    }

    /**
     * Convert Category entity to DTO.
     */
    private CategoryDTO toDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setCreatedBy(category.getCreatedBy());
        dto.setCreatedAt(category.getCreatedAt());
        // Note: Category entity doesn't have updatedAt and version fields

        // Include expense count
        long expenseCount = categoryRepository.countExpensesByCategoryId(category.getId());
        dto.setExpenseCount(expenseCount);

        return dto;
    }
}
