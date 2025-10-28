package com.homebudget.controller;

import com.homebudget.dto.CategoryDTO;
import com.homebudget.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for category operations.
 *
 * Implements User Story 3: Manage Spending Categories
 *
 * Endpoints:
 * - POST /api/categories - Create new category
 * - GET /api/categories - List all categories
 * - GET /api/categories/{id} - Get category by ID
 * - PUT /api/categories/{id} - Update category
 * - DELETE /api/categories/{id} - Delete category
 *
 * Uses X-Hass-User header for user identification (provided by Home Assistant)
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";

    @Autowired
    private CategoryService categoryService;

    /**
     * Create a new category.
     *
     * @param dto Category data (name, optional icon)
     * @param username User creating category (from X-Hass-User header)
     * @return Created category with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @Valid @RequestBody CategoryDTO dto,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/categories - Creating category '{}', user: {}", dto.getName(), username);

        CategoryDTO created = categoryService.createCategory(dto, username);

        logger.info("Created category ID: {} - '{}'", created.getId(), created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all categories.
     *
     * @return List of all categories ordered by name
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        logger.info("GET /api/categories - Finding all categories");

        List<CategoryDTO> categories = categoryService.getAllCategories();

        logger.info("Found {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID.
     *
     * @param id Category ID
     * @return Category details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        logger.info("GET /api/categories/{} - Finding category", id);

        CategoryDTO category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(category);
    }

    /**
     * Update an existing category.
     *
     * @param id Category ID
     * @param dto Updated category data
     * @return Updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {

        logger.info("PUT /api/categories/{} - Updating category", id);

        CategoryDTO updated = categoryService.updateCategory(id, dto);

        logger.info("Updated category ID: {} - '{}'", id, updated.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a category.
     * Will fail if category has associated expenses.
     *
     * @param id Category ID
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        logger.info("DELETE /api/categories/{} - Deleting category", id);

        categoryService.deleteCategory(id);

        logger.info("Deleted category ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get expense count for a category.
     * Useful for checking if category can be deleted.
     *
     * @param id Category ID
     * @return Expense count
     */
    @GetMapping("/{id}/expense-count")
    public ResponseEntity<Long> getExpenseCount(@PathVariable Long id) {
        logger.info("GET /api/categories/{}/expense-count", id);

        long count = categoryService.getExpenseCount(id);

        return ResponseEntity.ok(count);
    }
}
