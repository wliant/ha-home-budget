package com.homebudget.repository;

import com.homebudget.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity.
 * Provides database access methods for category operations.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by name.
     * Used for duplicate name validation.
     *
     * @param name the category name
     * @return Optional containing the category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Check if a category exists with the given name.
     *
     * @param name the category name
     * @return true if category exists
     */
    boolean existsByName(String name);

    /**
     * Find all categories ordered by name.
     *
     * @return list of categories
     */
    List<Category> findAllByOrderByNameAsc();

    /**
     * Find all non-system categories.
     * Used for user-created category management.
     *
     * @return list of non-system categories
     */
    List<Category> findByIsSystemFalseOrderByNameAsc();

    /**
     * Find categories created by a specific user.
     *
     * @param createdBy username
     * @return list of categories
     */
    List<Category> findByCreatedByOrderByNameAsc(String createdBy);

    /**
     * Check if a category exists with the given name (case insensitive).
     *
     * @param name the category name
     * @return true if category exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Count expenses associated with a category.
     * Used to check if category can be deleted.
     *
     * @param categoryId the category ID
     * @return count of expenses
     */
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.category.id = :categoryId")
    long countExpensesByCategoryId(@Param("categoryId") Long categoryId);
}
