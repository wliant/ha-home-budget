package com.homebudget.repository;

import com.homebudget.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Expense entity.
 * Provides database access methods for expense operations.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Find all expenses for a specific budget.
     *
     * @param budgetId the budget ID
     * @return list of expenses
     */
    List<Expense> findByBudgetIdOrderByExpenseDateDesc(Long budgetId);

    /**
     * Find all expenses for a specific category.
     *
     * @param categoryId the category ID
     * @return list of expenses
     */
    List<Expense> findByCategoryIdOrderByExpenseDateDesc(Long categoryId);

    /**
     * Find expenses created by a specific user.
     *
     * @param createdBy username
     * @return list of expenses
     */
    List<Expense> findByCreatedByOrderByExpenseDateDesc(String createdBy);

    /**
     * Find expenses within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of expenses
     */
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Calculate total spending for a budget.
     *
     * @param budgetId the budget ID
     * @return total amount spent, or BigDecimal.ZERO if no expenses
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.budget.id = :budgetId")
    BigDecimal sumAmountByBudgetId(@Param("budgetId") Long budgetId);

    /**
     * Count expenses for a specific budget.
     *
     * @param budgetId the budget ID
     * @return count of expenses
     */
    long countByBudgetId(Long budgetId);

    /**
     * Count expenses for a specific category.
     *
     * @param categoryId the category ID
     * @return count of expenses
     */
    long countByCategoryId(Long categoryId);

    /**
     * Find expenses by multiple filter criteria.
     * Used for filtering API with optional parameters.
     *
     * @param budgetId optional budget filter
     * @param categoryId optional category filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param createdBy optional user filter
     * @return list of matching expenses
     */
    @Query("SELECT e FROM Expense e WHERE " +
            "(:budgetId IS NULL OR e.budget.id = :budgetId) AND " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy) " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findByFilters(@Param("budgetId") Long budgetId,
                                 @Param("categoryId") Long categoryId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("createdBy") String createdBy);

    /**
     * Get spending breakdown by category for a budget.
     *
     * @param budgetId the budget ID
     * @return list of [categoryId, totalAmount] pairs
     */
    @Query("SELECT e.category.id, SUM(e.amount) FROM Expense e " +
            "WHERE e.budget.id = :budgetId " +
            "GROUP BY e.category.id")
    List<Object[]> getCategoryBreakdown(@Param("budgetId") Long budgetId);
}
