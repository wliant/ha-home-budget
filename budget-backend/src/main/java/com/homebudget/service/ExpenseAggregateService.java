package com.homebudget.service;

import com.homebudget.dto.CategoryExpenseAggregateDTO;
import com.homebudget.dto.CategoryStatsDTO;
import com.homebudget.dto.DayOfWeekAggregateDTO;
import com.homebudget.dto.UserSpendingAggregateDTO;
import com.homebudget.model.Category;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for computing category-level expense aggregates.
 * Handles monthly and yearly aggregation with parent category rollup.
 *
 * Feature 016: Category Expense Aggregates
 */
@Service
public class ExpenseAggregateService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseAggregateService.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Get monthly expense aggregates per category with parent rollup.
     *
     * @param year  the year to aggregate
     * @param month optional month (1-12); if null, returns all months
     * @return list of category aggregate DTOs
     */
    public List<CategoryExpenseAggregateDTO> getMonthlyAggregates(Integer year, Integer month) {
        logger.info("Computing monthly aggregates for year={}, month={}", year, month);

        // Fetch raw aggregates from DB
        List<Object[]> rawAggregates;
        if (month != null) {
            rawAggregates = expenseRepository.getMonthlyAggregatesByYearAndMonth(year, month);
        } else {
            rawAggregates = expenseRepository.getMonthlyAggregatesByYear(year);
        }

        // Load all categories for metadata and hierarchy
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category c : allCategories) {
            categoryMap.put(c.getId(), c);
        }

        if (month != null) {
            return buildAggregatesForMonth(rawAggregates, categoryMap, year, month);
        } else {
            return buildAggregatesAllMonths(rawAggregates, categoryMap, year);
        }
    }

    /**
     * Get yearly expense aggregates per category with parent rollup.
     *
     * @param year the year to aggregate
     * @return list of category aggregate DTOs with month=null
     */
    public List<CategoryExpenseAggregateDTO> getYearlyAggregates(Integer year) {
        logger.info("Computing yearly aggregates for year={}", year);

        List<Object[]> rawAggregates = expenseRepository.getYearlyAggregatesByYear(year);

        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category c : allCategories) {
            categoryMap.put(c.getId(), c);
        }

        // Build direct amounts map: categoryId -> amount
        Map<Long, BigDecimal> directAmounts = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            BigDecimal amount = (BigDecimal) row[1];
            directAmounts.put(categoryId, amount);
        }

        return buildDTOsWithRollup(directAmounts, categoryMap, year, null);
    }

    /**
     * Get the spending map for a year: categoryId -> month -> spending amount.
     * Used by BudgetService for yearly budget view.
     */
    public Map<Long, Map<Integer, BigDecimal>> getMonthlySpendingMap(Integer year) {
        List<Object[]> rawAggregates = expenseRepository.getMonthlyAggregatesByYear(year);

        Map<Long, Map<Integer, BigDecimal>> result = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            Integer monthVal = ((Number) row[1]).intValue();
            BigDecimal amount = (BigDecimal) row[2];
            result.computeIfAbsent(categoryId, k -> new HashMap<>()).put(monthVal, amount);
        }
        return result;
    }

    /**
     * Get yearly spending map: categoryId -> total yearly spending.
     * Used by BudgetService for yearly budget view totals.
     */
    public Map<Long, BigDecimal> getYearlySpendingMap(Integer year) {
        List<Object[]> rawAggregates = expenseRepository.getYearlyAggregatesByYear(year);

        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            BigDecimal amount = (BigDecimal) row[1];
            result.put(categoryId, amount);
        }
        return result;
    }

    /**
     * Get daily expense aggregates per category with parent rollup for a specific month.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @return list of category aggregate DTOs with day field set
     */
    public List<CategoryExpenseAggregateDTO> getDailyAggregates(Integer year, Integer month) {
        logger.info("Computing daily aggregates for year={}, month={}", year, month);

        List<Object[]> rawAggregates = expenseRepository.getDailyAggregatesByYearAndMonth(year, month);

        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category c : allCategories) {
            categoryMap.put(c.getId(), c);
        }

        // Group by day: day -> categoryId -> amount
        Map<Integer, Map<Long, BigDecimal>> dailyDirectAmounts = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            Integer day = ((Number) row[1]).intValue();
            BigDecimal amount = (BigDecimal) row[2];
            dailyDirectAmounts
                    .computeIfAbsent(day, k -> new HashMap<>())
                    .put(categoryId, amount);
        }

        List<CategoryExpenseAggregateDTO> allResults = new ArrayList<>();
        for (Map.Entry<Integer, Map<Long, BigDecimal>> entry : dailyDirectAmounts.entrySet()) {
            Integer day = entry.getKey();
            List<CategoryExpenseAggregateDTO> dayDTOs = buildDTOsWithRollup(
                    entry.getValue(), categoryMap, year, month);
            for (CategoryExpenseAggregateDTO dto : dayDTOs) {
                dto.setDay(day);
            }
            allResults.addAll(dayDTOs);
        }

        return allResults;
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    private List<CategoryExpenseAggregateDTO> buildAggregatesForMonth(
            List<Object[]> rawAggregates, Map<Long, Category> categoryMap, int year, int month) {

        // For single-month query, rawAggregates has [categoryId, amount]
        Map<Long, BigDecimal> directAmounts = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            BigDecimal amount = (BigDecimal) row[1];
            directAmounts.put(categoryId, amount);
        }

        return buildDTOsWithRollup(directAmounts, categoryMap, year, month);
    }

    private List<CategoryExpenseAggregateDTO> buildAggregatesAllMonths(
            List<Object[]> rawAggregates, Map<Long, Category> categoryMap, int year) {

        // Group by month first, then build per-month aggregates
        Map<Integer, Map<Long, BigDecimal>> monthlyDirectAmounts = new HashMap<>();
        for (Object[] row : rawAggregates) {
            Long categoryId = ((Number) row[0]).longValue();
            Integer monthVal = ((Number) row[1]).intValue();
            BigDecimal amount = (BigDecimal) row[2];
            monthlyDirectAmounts
                    .computeIfAbsent(monthVal, k -> new HashMap<>())
                    .put(categoryId, amount);
        }

        List<CategoryExpenseAggregateDTO> allResults = new ArrayList<>();
        for (Map.Entry<Integer, Map<Long, BigDecimal>> entry : monthlyDirectAmounts.entrySet()) {
            allResults.addAll(buildDTOsWithRollup(entry.getValue(), categoryMap, year, entry.getKey()));
        }

        return allResults;
    }

    private List<CategoryExpenseAggregateDTO> buildDTOsWithRollup(
            Map<Long, BigDecimal> directAmounts, Map<Long, Category> categoryMap,
            int year, Integer month) {

        List<CategoryExpenseAggregateDTO> results = new ArrayList<>();

        // Track parent rollup: parentCategoryId -> sum of children's direct amounts
        Map<Long, BigDecimal> childrenRollup = new HashMap<>();

        // Process all categories that have direct spending
        for (Map.Entry<Long, BigDecimal> entry : directAmounts.entrySet()) {
            Long categoryId = entry.getKey();
            BigDecimal directAmount = entry.getValue();
            Category category = categoryMap.get(categoryId);

            if (category == null) continue;

            // If this is a child category, accumulate to parent
            if (category.getParentCategory() != null) {
                Long parentId = category.getParentCategory().getId();
                childrenRollup.merge(parentId, directAmount, BigDecimal::add);
            }
        }

        // Build DTOs for all categories with spending (direct or children)
        // Collect all relevant category IDs
        Map<Long, BigDecimal> allRelevantDirect = new HashMap<>(directAmounts);
        // Ensure parent categories with only children spending are included
        for (Long parentId : childrenRollup.keySet()) {
            allRelevantDirect.putIfAbsent(parentId, BigDecimal.ZERO);
        }

        for (Map.Entry<Long, BigDecimal> entry : allRelevantDirect.entrySet()) {
            Long categoryId = entry.getKey();
            BigDecimal directAmount = entry.getValue();
            Category category = categoryMap.get(categoryId);

            if (category == null) continue;

            BigDecimal childrenAmount = childrenRollup.getOrDefault(categoryId, BigDecimal.ZERO);
            BigDecimal totalAmount = directAmount.add(childrenAmount);

            CategoryExpenseAggregateDTO dto = new CategoryExpenseAggregateDTO();
            dto.setCategoryId(categoryId);
            dto.setCategoryName(category.getName());
            dto.setCategoryIcon(category.getIcon());
            dto.setParentCategoryId(category.getParentCategory() != null
                    ? category.getParentCategory().getId() : null);
            dto.setDirectAmount(directAmount);
            dto.setChildrenAmount(childrenAmount);
            dto.setTotalAmount(totalAmount);
            dto.setYear(year);
            dto.setMonth(month);

            results.add(dto);
        }

        return results;
    }

    // ========================================================================
    // Per-User Spending Aggregates
    // ========================================================================

    public List<UserSpendingAggregateDTO> getUserSpendingAggregates(Integer year, Integer month) {
        logger.info("Getting user spending aggregates for {}/{}", year, month);

        List<Object[]> rawData = expenseRepository.getMonthlyAggregatesByUserAndCategory(year, month);
        Map<Long, Category> categoryMap = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categoryMap.put(c.getId(), c));

        // Build per-user per-category DTOs and track user totals
        Map<String, BigDecimal> userTotals = new HashMap<>();
        List<UserSpendingAggregateDTO> results = new ArrayList<>();

        for (Object[] row : rawData) {
            String username = (String) row[0];
            Long categoryId = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            Category category = categoryMap.get(categoryId);
            if (category == null) continue;

            UserSpendingAggregateDTO dto = new UserSpendingAggregateDTO();
            dto.setUsername(username);
            dto.setCategoryId(categoryId);
            dto.setCategoryName(category.getName());
            dto.setCategoryIcon(category.getIcon());
            dto.setAmount(amount);
            results.add(dto);

            userTotals.merge(username, amount, BigDecimal::add);
        }

        // Set totalUserSpending on each DTO
        for (UserSpendingAggregateDTO dto : results) {
            dto.setTotalUserSpending(userTotals.getOrDefault(dto.getUsername(), BigDecimal.ZERO));
        }

        logger.info("Returning {} user spending aggregates for {} users", results.size(), userTotals.size());
        return results;
    }

    // ========================================================================
    // Category Statistics
    // ========================================================================

    public List<CategoryStatsDTO> getCategoryStats(Integer year, Integer month) {
        logger.info("Getting category stats for {}/{}", year, month);

        List<Object[]> rawData = expenseRepository.getCategoryStatsByYearAndMonth(year, month);
        Map<Long, Category> categoryMap = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categoryMap.put(c.getId(), c));

        List<CategoryStatsDTO> results = new ArrayList<>();
        for (Object[] row : rawData) {
            Long categoryId = (Long) row[0];
            Long count = (Long) row[1];
            BigDecimal avg = row[2] instanceof Double ? BigDecimal.valueOf((Double) row[2]) : (BigDecimal) row[2];
            BigDecimal min = (BigDecimal) row[3];
            BigDecimal max = (BigDecimal) row[4];
            BigDecimal total = (BigDecimal) row[5];

            Category category = categoryMap.get(categoryId);
            if (category == null) continue;

            CategoryStatsDTO dto = new CategoryStatsDTO();
            dto.setCategoryId(categoryId);
            dto.setCategoryName(category.getName());
            dto.setCategoryIcon(category.getIcon());
            dto.setExpenseCount(count);
            dto.setAverageAmount(avg);
            dto.setMinAmount(min);
            dto.setMaxAmount(max);
            dto.setTotalAmount(total);
            results.add(dto);
        }

        logger.info("Returning {} category stats", results.size());
        return results;
    }

    // ========================================================================
    // Day-of-Week Aggregates
    // ========================================================================

    private static final String[] DAY_NAMES = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    public List<DayOfWeekAggregateDTO> getDayOfWeekAggregates(Integer year, Integer month) {
        logger.info("Getting day-of-week aggregates for year={}, month={}", year, month);

        List<Object[]> rawData;
        if (month != null) {
            rawData = expenseRepository.getDayOfWeekAggregatesByYearAndMonth(year, month);
        } else {
            rawData = expenseRepository.getDayOfWeekAggregatesByYear(year);
        }

        List<DayOfWeekAggregateDTO> results = new ArrayList<>();
        for (Object[] row : rawData) {
            Integer dayOfWeek = (Integer) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            Long expenseCount = (Long) row[2];

            DayOfWeekAggregateDTO dto = new DayOfWeekAggregateDTO();
            dto.setDayOfWeek(dayOfWeek);
            dto.setDayName(dayOfWeek >= 1 && dayOfWeek <= 7 ? DAY_NAMES[dayOfWeek - 1] : "Unknown");
            dto.setTotalAmount(totalAmount);
            dto.setExpenseCount(expenseCount);
            dto.setAverageAmount(expenseCount > 0
                    ? totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            results.add(dto);
        }

        logger.info("Returning {} day-of-week aggregates", results.size());
        return results;
    }
}
