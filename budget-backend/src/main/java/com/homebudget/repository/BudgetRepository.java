package com.homebudget.repository;

import com.homebudget.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
