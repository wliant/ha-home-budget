package com.homebudget.repository;

import com.homebudget.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Budget entity.
 * Provides database access methods for budget operations.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Find a budget by year and month.
     * Used for duplicate budget validation.
     *
     * @param year the year
     * @param month the month (1-12)
     * @return Optional containing the budget if found
     */
    Optional<Budget> findByYearAndMonth(Integer year, Integer month);

    /**
     * Check if a budget exists for a given year and month.
     *
     * @param year the year
     * @param month the month (1-12)
     * @return true if budget exists
     */
    boolean existsByYearAndMonth(Integer year, Integer month);

    /**
     * Find all budgets ordered by year and month descending (newest first).
     *
     * @return list of budgets
     */
    List<Budget> findAllByOrderByYearDescMonthDesc();

    /**
     * Find budgets created by a specific user.
     *
     * @param createdBy username
     * @return list of budgets
     */
    List<Budget> findByCreatedByOrderByYearDescMonthDesc(String createdBy);

    /**
     * Find budget by year and month with expenses eagerly loaded.
     * Prevents N+1 query problem when accessing expenses.
     *
     * @param year the year
     * @param month the month
     * @return Optional containing budget with expenses
     */
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.expenses WHERE b.year = :year AND b.month = :month")
    Optional<Budget> findByYearAndMonthWithExpenses(Integer year, Integer month);

    /**
     * Find budget by category, year, and month.
     * Used for uniqueness validation.
     *
     * @param categoryId the category ID
     * @param year the year
     * @param month the month
     * @return Optional containing the budget if found
     */
    @Query("SELECT b FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month = :month")
    Optional<Budget> findByCategoryIdAndYearAndMonth(@Param("categoryId") Long categoryId,
                                                       @Param("year") Integer year,
                                                       @Param("month") Integer month);

    /**
     * Find parent (yearly) budget by category and year (month is null).
     */
    @Query("SELECT b FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month IS NULL")
    Optional<Budget> findParentBudget(@Param("categoryId") Long categoryId,
                                      @Param("year") Integer year);

    /**
     * Check if a parent (yearly) budget exists for category and year.
     */
    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month IS NULL")
    boolean existsParentBudget(@Param("categoryId") Long categoryId,
                               @Param("year") Integer year);

    /**
     * Check if a budget exists for a specific category, year, and month.
     *
     * @param categoryId the category ID
     * @param year the year
     * @param month the month
     * @return true if budget exists
     */
    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month = :month")
    boolean existsByCategoryIdAndYearAndMonth(@Param("categoryId") Long categoryId,
                                               @Param("year") Integer year,
                                               @Param("month") Integer month);

    /**
     * Find all budgets for a specific category.
     *
     * @param categoryId the category ID
     * @return list of budgets
     */
    List<Budget> findByCategoryIdOrderByYearDescMonthDesc(Long categoryId);

    /**
     * Find all budgets for a specific year and month.
     * Used for period-based queries and reporting.
     *
     * @param year the year
     * @param month the month
     * @return list of budgets
     */
    List<Budget> findByYearAndMonthOrderByCategoryIdAsc(Integer year, Integer month);

    /**
     * Calculate sum of child category budgets for a specific parent category and period.
     * Used for parent budget validation.
     *
     * @param parentCategoryId the parent category ID
     * @param year the year
     * @param month the month
     * @return sum of child budgets, or null if no child budgets exist
     */
    @Query("SELECT SUM(b.totalAmount) FROM Budget b JOIN b.category c " +
           "WHERE c.parentCategory.id = :parentCategoryId AND b.year = :year AND b.month = :month")
    BigDecimal sumByParentCategoryAndPeriod(@Param("parentCategoryId") Long parentCategoryId,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month);

    /**
     * Find all budgets where category has a specific parent.
     * Used to get all child budgets for validation.
     *
     * @param parentCategoryId the parent category ID
     * @param year the year
     * @param month the month
     * @return list of child budgets
     */
    @Query("SELECT b FROM Budget b JOIN b.category c " +
           "WHERE c.parentCategory.id = :parentCategoryId AND b.year = :year AND b.month = :month")
    List<Budget> findChildBudgets(@Param("parentCategoryId") Long parentCategoryId,
                                   @Param("year") Integer year,
                                   @Param("month") Integer month);

    /**
     * Find all budgets for a specific year (including yearly budgets where month is null).
     */
    List<Budget> findByYear(Integer year);

    /**
     * Find all monthly budgets for a category in a specific year.
     */
    @Query("SELECT b FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month IS NOT NULL")
    List<Budget> findMonthlyBudgetsForCategory(@Param("categoryId") Long categoryId,
                                               @Param("year") Integer year);

    /**
     * Sum all monthly budgets for a category in a given year.
     */
    @Query("SELECT SUM(b.totalAmount) FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month IS NOT NULL")
    BigDecimal sumMonthlyBudgetsForCategory(@Param("categoryId") Long categoryId,
                                            @Param("year") Integer year);

    /**
     * Count monthly budgets for a category in a given year.
     */
    @Query("SELECT COUNT(b) FROM Budget b WHERE b.category.id = :categoryId AND b.year = :year AND b.month IS NOT NULL")
    long countMonthlyBudgetsForCategory(@Param("categoryId") Long categoryId,
                                        @Param("year") Integer year);
}
