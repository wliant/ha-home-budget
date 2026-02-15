package com.homebudget.repository;

import com.homebudget.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Find all expenses for a specific category.
     *
     * @param categoryId the category ID
     * @return list of expenses
     */
    List<Expense> findByCategoryIdOrderByExpenseDateDesc(Long categoryId);

    /**
     * Alias for findByCategoryIdOrderByExpenseDateDesc
     */
    List<Expense> findByCategoryId(Long categoryId);

    /**
     * Find expenses created by a specific user.
     *
     * @param createdBy username
     * @return list of expenses
     */
    List<Expense> findByCreatedByOrderByExpenseDateDesc(String createdBy);

    /**
     * Alias for findByCreatedByOrderByExpenseDateDesc
     */
    List<Expense> findByCreatedBy(String createdBy);

    /**
     * Find expenses within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of expenses
     */
    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Alias for findByExpenseDateBetweenOrderByExpenseDateDesc
     */
    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find all expenses ordered by date descending.
     */
    @Query("SELECT e FROM Expense e ORDER BY e.expenseDate DESC")
    List<Expense> findAllOrderByExpenseDateDesc();

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
     * @param categoryId optional category filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param createdBy optional user filter
     * @return list of matching expenses
     */
    @Query("SELECT e FROM Expense e WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy) " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findByFilters(@Param("categoryId") Long categoryId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("createdBy") String createdBy);

    /**
     * Paginated version of findByFilters with amount range support.
     * Used by the expense list view endpoint.
     *
     * @param categoryId optional category filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param minAmount optional minimum amount filter (inclusive)
     * @param maxAmount optional maximum amount filter (inclusive)
     * @param createdBy optional user filter
     * @param pageable pagination and sorting parameters
     * @return page of matching expenses
     */
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.category WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    Page<Expense> findByFiltersPageable(@Param("categoryId") Long categoryId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("minAmount") BigDecimal minAmount,
                                         @Param("maxAmount") BigDecimal maxAmount,
                                         @Param("createdBy") String createdBy,
                                         Pageable pageable);

    /**
     * Count query for paginated findByFiltersPageable (required by Spring Data for LEFT JOIN FETCH).
     */
    @Query("SELECT COUNT(e) FROM Expense e WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    long countByFiltersPageable(@Param("categoryId") Long categoryId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("minAmount") BigDecimal minAmount,
                                @Param("maxAmount") BigDecimal maxAmount,
                                @Param("createdBy") String createdBy);

    /**
     * Aggregate summary for filtered expenses: returns total amount.
     * Count is available from Page.getTotalElements().
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    BigDecimal getFilteredTotalAmount(@Param("categoryId") Long categoryId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("minAmount") BigDecimal minAmount,
                                      @Param("maxAmount") BigDecimal maxAmount,
                                      @Param("createdBy") String createdBy);

    /**
     * Get distinct years from expense dates, sorted descending.
     * Used to populate the year filter dropdown.
     */
    @Query("SELECT DISTINCT YEAR(e.expenseDate) FROM Expense e ORDER BY 1 DESC")
    List<Integer> findDistinctYears();

    /**
     * Get distinct creators, sorted ascending.
     * Used to populate the created-by filter dropdown.
     */
    @Query("SELECT DISTINCT e.createdBy FROM Expense e ORDER BY e.createdBy ASC")
    List<String> findDistinctCreators();

    /**
     * Paginated expense query filtering by multiple category IDs.
     * Used when filtering by a parent category (includes parent + all child category IDs).
     */
    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.category WHERE " +
            "e.category.id IN :categoryIds AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    Page<Expense> findByFiltersPageableWithCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate,
                                                        @Param("minAmount") BigDecimal minAmount,
                                                        @Param("maxAmount") BigDecimal maxAmount,
                                                        @Param("createdBy") String createdBy,
                                                        Pageable pageable);

    /**
     * Count query for findByFiltersPageableWithCategoryIds.
     */
    @Query("SELECT COUNT(e) FROM Expense e WHERE " +
            "e.category.id IN :categoryIds AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    long countByFiltersPageableWithCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("minAmount") BigDecimal minAmount,
                                                @Param("maxAmount") BigDecimal maxAmount,
                                                @Param("createdBy") String createdBy);

    /**
     * Aggregate total amount for expenses filtered by multiple category IDs.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE " +
            "e.category.id IN :categoryIds AND " +
            "(:startDate IS NULL OR e.expenseDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.expenseDate <= :endDate) AND " +
            "(:minAmount IS NULL OR e.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR e.amount <= :maxAmount) AND " +
            "(:createdBy IS NULL OR e.createdBy = :createdBy)")
    BigDecimal getFilteredTotalAmountWithCategoryIds(@Param("categoryIds") List<Long> categoryIds,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("minAmount") BigDecimal minAmount,
                                                      @Param("maxAmount") BigDecimal maxAmount,
                                                      @Param("createdBy") String createdBy);

    // ========================================================================
    // Category Expense Aggregate Queries (Feature 016)
    // ========================================================================

    /**
     * Monthly aggregate: SUM(amount) grouped by category_id and month for a given year.
     * Returns Object[] with [categoryId (Long), month (Integer), totalAmount (BigDecimal)].
     */
    @Query("SELECT e.category.id, MONTH(e.expenseDate), COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e WHERE YEAR(e.expenseDate) = :year " +
            "GROUP BY e.category.id, MONTH(e.expenseDate)")
    List<Object[]> getMonthlyAggregatesByYear(@Param("year") Integer year);

    /**
     * Monthly aggregate for a specific month: SUM(amount) grouped by category_id.
     * Returns Object[] with [categoryId (Long), totalAmount (BigDecimal)].
     */
    @Query("SELECT e.category.id, COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "GROUP BY e.category.id")
    List<Object[]> getMonthlyAggregatesByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    /**
     * Single category monthly aggregate.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.category.id = :categoryId AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    BigDecimal sumByCategoryAndMonth(@Param("categoryId") Long categoryId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    /**
     * Yearly aggregate: SUM(amount) grouped by category_id for a given year.
     * Returns Object[] with [categoryId (Long), totalAmount (BigDecimal)].
     */
    @Query("SELECT e.category.id, COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e WHERE YEAR(e.expenseDate) = :year " +
            "GROUP BY e.category.id")
    List<Object[]> getYearlyAggregatesByYear(@Param("year") Integer year);

    /**
     * Single category yearly aggregate.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.category.id = :categoryId AND YEAR(e.expenseDate) = :year")
    BigDecimal sumByCategoryAndYear(@Param("categoryId") Long categoryId,
                                     @Param("year") int year);
}
