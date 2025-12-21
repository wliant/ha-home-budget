package com.homebudget.service;

import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Category;
import com.homebudget.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * @param dto Category data (name, optional icon, optional parentCategoryId)
     * @param username User creating the category (from X-Hass-User header)
     * @return Created category DTO
     * @throws DuplicateCategoryException if category name already exists
     * @throws HierarchyDepthException if parent is already a child category
     * @throws CategoryNotFoundException if parent category not found
     */
    public CategoryDTO createCategory(CategoryDTO dto, String username) {
        logger.info("Creating category '{}', user: {}, parent: {}", dto.getName(), username, dto.getParentCategoryId());

        // Validate unique name
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateCategoryException(dto.getName());
        }

        // Create category entity
        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : "");
        category.setCreatedBy(username);

        // Set parent category if provided
        if (dto.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(dto.getParentCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(dto.getParentCategoryId()));

            // Validate hierarchy depth (parent must be root)
            validateHierarchyDepth(parent);

            // Validate system category protection
            validateSystemCategoryProtection(category);

            category.setParentCategory(parent);
        }

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
     * @throws CircularCategoryException if parent change creates circular reference
     * @throws CategoryInUseException if category has budgets and parent is being changed
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

        // Handle parent category changes
        if (dto.getParentCategoryId() != null) {
            // Check if parent is changing
            Long currentParentId = category.getParentCategory() != null ?
                    category.getParentCategory().getId() : null;

            if (!dto.getParentCategoryId().equals(currentParentId)) {
                // Validate parent can be changed (no budgets)
                validateParentChange(category);

                // Get new parent
                Category newParent = categoryRepository.findById(dto.getParentCategoryId())
                        .orElseThrow(() -> new CategoryNotFoundException(dto.getParentCategoryId()));

                // Validate circular reference
                validateCircularReference(id, dto.getParentCategoryId());

                // Validate hierarchy depth
                validateHierarchyDepth(newParent);

                category.setParentCategory(newParent);
            }
        } else if (category.getParentCategory() != null) {
            // Removing parent (moving to root)
            validateParentChange(category);
            category.setParentCategory(null);
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
     * Prevents deletion if category has associated expenses, budgets, or children.
     *
     * @param id Category ID
     * @throws CategoryNotFoundException if category not found
     * @throws CategoryInUseException if category has associated data
     */
    public void deleteCategory(Long id) {
        logger.info("Attempting to delete category ID: {}", id);

        // Find category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        // Check if category has children
        long childCount = categoryRepository.countByParentCategoryId(id);
        if (childCount > 0) {
            logger.warn("Cannot delete category ID: {} - has {} child categories", id, childCount);
            throw new CategoryInUseException(category.getName(), (int) childCount, "children");
        }

        // Check if category has budgets
        long budgetCount = categoryRepository.countBudgetsByCategoryId(id);
        if (budgetCount > 0) {
            logger.warn("Cannot delete category ID: {} - has {} associated budgets", id, budgetCount);
            throw new CategoryInUseException(category.getName(), (int) budgetCount, "budgets");
        }

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
     * Get category hierarchy (all root categories with children).
     *
     * @return List of root categories with nested children
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryHierarchy() {
        logger.info("Getting category hierarchy");

        List<Category> rootCategories = categoryRepository.findByParentCategoryIsNullOrderByNameAsc();

        return rootCategories.stream()
                .map(this::toDTOWithChildren)
                .collect(Collectors.toList());
    }

    // ========== Validation Methods (T012-T015) ==========

    /**
     * T012: Validate circular reference detection.
     * Checks if setting newParentId as parent would create a cycle.
     */
    private void validateCircularReference(Long categoryId, Long newParentId) {
        if (newParentId == null) return;

        // Traverse up the parent chain from new parent
        Category current = categoryRepository.findById(newParentId).orElse(null);
        Set<Long> visited = new HashSet<>();

        while (current != null) {
            if (current.getId().equals(categoryId)) {
                throw new CircularCategoryException(categoryId, newParentId);
            }
            if (!visited.add(current.getId())) {
                throw new CircularCategoryException("Cycle detected in existing category data");
            }
            current = current.getParentCategory();
        }
    }

    /**
     * T013: Validate hierarchy depth (2-level maximum).
     * Parent must be a root category (no parent itself).
     */
    private void validateHierarchyDepth(Category parent) {
        if (parent.getParentCategory() != null) {
            throw new HierarchyDepthException(parent.getName(), parent.getParentCategory().getName());
        }
    }

    /**
     * T014: Validate system category protection.
     * System categories cannot have parents assigned.
     */
    private void validateSystemCategoryProtection(Category category) {
        if (category.getIsSystem() != null && category.getIsSystem()) {
            throw new IllegalArgumentException("Cannot assign parent to system category");
        }
    }

    /**
     * T015 + T018: Validate parent change is allowed.
     * Cannot change parent if category has active budgets.
     */
    private void validateParentChange(Category category) {
        long budgetCount = categoryRepository.countBudgetsByCategoryId(category.getId());
        if (budgetCount > 0) {
            throw new CategoryInUseException(
                    String.format("Cannot change parent category: %d active budgets reference this category. " +
                            "Please reassign budgets first.", budgetCount));
        }
    }

    // ========== Helper Methods ==========

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
        dto.setIsSystem(category.getIsSystem());

        // Set parent category ID
        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getId());

            // Set minimal parent info
            CategoryDTO parentDTO = new CategoryDTO();
            parentDTO.setId(category.getParentCategory().getId());
            parentDTO.setName(category.getParentCategory().getName());
            dto.setParentCategory(parentDTO);
        }

        // Include expense count
        long expenseCount = categoryRepository.countExpensesByCategoryId(category.getId());
        dto.setExpenseCount(expenseCount);

        return dto;
    }

    /**
     * Convert Category to DTO with children loaded.
     * Used for hierarchy display.
     */
    private CategoryDTO toDTOWithChildren(Category category) {
        CategoryDTO dto = toDTO(category);

        // Load and set children
        List<Category> children = categoryRepository.findByParentCategoryId(category.getId());
        List<CategoryDTO> childDTOs = children.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        dto.setChildren(childDTOs);

        return dto;
    }
}
