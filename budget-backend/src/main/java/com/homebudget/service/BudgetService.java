package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.BudgetValidationDTO;
import com.homebudget.dto.YearlyBudgetViewDTO;
import com.homebudget.dto.YearlyCategoryBudgetDTO;
import com.homebudget.dto.YearlyMonthlyBudgetDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for Budget operations.
 * Handles business logic for budget management including spending calculations.
 */
@Service
@Transactional
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseAggregateService expenseAggregateService;

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository,
                         CategoryRepository categoryRepository, ExpenseAggregateService expenseAggregateService) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.expenseAggregateService = expenseAggregateService;
    }

    /**
     * Create a new budget.
     * Requires categoryId and validates:
     * - Category exists
     * - No duplicate budget for same category/year/month
     * - Parent budget validation (if child category)
     *
     * @param dto budget data (must include categoryId)
     * @param username user from X-Hass-User header
     * @return created budget as DTO
     * @throws CategoryNotFoundException if category not found
     * @throws DuplicateBudgetException if budget already exists for category/period
     * @throws ParentBudgetMismatchException if parent budget validation fails
     */
    public BudgetDTO createBudget(BudgetDTO dto, String username) {
        // Validate categoryId is provided
        if (dto.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID is required for budget creation");
        }

        // Validate category exists
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

        if (dto.getMonth() == null) {
            if (budgetRepository.existsParentBudget(dto.getCategoryId(), dto.getYear())) {
                throw new DuplicateBudgetException(
                        String.format("Yearly budget already exists for category '%s' in %d",
                                category.getName(), dto.getYear())
                );
            }

            Budget parentBudget = buildBudgetEntity(dto, username, category);
            parentBudget.setMonth(null);
            Budget savedParent = budgetRepository.save(parentBudget);

            if (Boolean.TRUE.equals(dto.getAutoCreateChildren())) {
                long existingMonthly = budgetRepository.countMonthlyBudgetsForCategory(dto.getCategoryId(), dto.getYear());
                if (existingMonthly > 0) {
                    throw new DuplicateBudgetException(
                            String.format("Monthly budgets already exist for category '%s' in %d",
                                    category.getName(), dto.getYear())
                    );
                }
                createMonthlyBudgetsForYear(category, dto.getYear(), dto.getTotalAmount(), username);
            }

            return mapToBudgetDTO(savedParent);
        }

        if (budgetRepository.existsByCategoryIdAndYearAndMonth(dto.getCategoryId(), dto.getYear(), dto.getMonth())) {
            throw new DuplicateBudgetException(
                    String.format("Budget already exists for category '%s' in %d-%02d",
                            category.getName(), dto.getYear(), dto.getMonth())
            );
        }

        Budget parentBudget = budgetRepository.findParentBudget(dto.getCategoryId(), dto.getYear()).orElse(null);
        BigDecimal parentAmount = parentBudget != null ? parentBudget.getTotalAmount() : null;

        if (parentBudget == null && !Boolean.TRUE.equals(dto.getCreateParentBudget())) {
            throw new ParentBudgetMismatchException(
                    String.format("Parent yearly budget is required for category '%s' in %d",
                            category.getName(), dto.getYear())
            );
        }

        BigDecimal monthlySum = defaultZero(budgetRepository.sumMonthlyBudgetsForCategory(dto.getCategoryId(), dto.getYear()));
        BigDecimal newSum = monthlySum.add(dto.getTotalAmount());

        if (parentBudget == null && Boolean.TRUE.equals(dto.getCreateParentBudget())) {
            BigDecimal requestedParentAmount = dto.getParentTotalAmount() != null
                    ? dto.getParentTotalAmount()
                    : dto.getTotalAmount();
            if (newSum.compareTo(requestedParentAmount) > 0) {
                throw new ParentBudgetMismatchException(
                        String.format("Parent yearly budget %.2f is less than required total %.2f",
                                requestedParentAmount, newSum)
                );
            }
            Budget newParent = buildBudgetEntity(dto, username, category);
            newParent.setMonth(null);
            newParent.setTotalAmount(requestedParentAmount);
            parentBudget = budgetRepository.save(newParent);
            parentAmount = parentBudget.getTotalAmount();
        }

        if (parentAmount == null) {
            throw new ParentBudgetMismatchException("Parent budget amount is required for monthly budgets");
        }

        if (newSum.compareTo(parentAmount) > 0) {
            if (Boolean.TRUE.equals(dto.getExtendParentBudget())) {
                BigDecimal requestedParentAmount = dto.getParentTotalAmount();
                if (requestedParentAmount == null) {
                    throw new ParentBudgetMismatchException("Parent budget amount is required to extend parent budget");
                }
                if (requestedParentAmount.compareTo(newSum) < 0) {
                    throw new ParentBudgetMismatchException(
                            String.format("Parent budget amount %.2f is less than required total %.2f",
                                    requestedParentAmount, newSum)
                    );
                }
                parentBudget.setTotalAmount(requestedParentAmount);
                budgetRepository.save(parentBudget);
            } else {
                throw new ParentBudgetMismatchException(
                        String.format("Total monthly budgets (%.2f) exceed parent yearly budget (%.2f)",
                                newSum, parentAmount)
                );
            }
        }

        Budget budget = buildBudgetEntity(dto, username, category);
        Budget saved = budgetRepository.save(budget);

        if (dto.getMonth() != null) {
            ensureMonthlyBudgetsForRemainingMonths(category, dto.getYear(), dto.getMonth(), dto.getTotalAmount(), username);
            alignParentBudgetWithMonthlySum(category.getId(), dto.getYear());
        }

        BudgetDTO result = mapToBudgetDTO(saved);

        // Handle parent category budget (category hierarchy)
        if (category.getParentCategory() != null) {
            Category parentCat = category.getParentCategory();
            Long parentCatId = parentCat.getId();

            // Look up parent category budget for the same period
            Budget parentCatBudget;
            if (dto.getMonth() != null) {
                parentCatBudget = budgetRepository.findByCategoryIdAndYearAndMonth(parentCatId, dto.getYear(), dto.getMonth()).orElse(null);
            } else {
                parentCatBudget = budgetRepository.findParentBudget(parentCatId, dto.getYear()).orElse(null);
            }

            if (parentCatBudget != null) {
                // Auto-increment existing parent category budget
                BigDecimal previousAmount = parentCatBudget.getTotalAmount();
                BigDecimal newAmount = previousAmount.add(dto.getTotalAmount());
                parentCatBudget.setTotalAmount(newAmount);
                budgetRepository.save(parentCatBudget);

                result.setParentCategoryBudgetUpdated(new BudgetDTO.ParentCategoryBudgetUpdateInfo(
                        parentCat.getName(), previousAmount, newAmount, dto.getYear(), dto.getMonth()));

                logger.info("Auto-incremented parent category budget for '{}' from {} to {} for {}/{}",
                        parentCat.getName(), previousAmount, newAmount, dto.getYear(), dto.getMonth());
            } else if (Boolean.TRUE.equals(dto.getCreateParentCategoryBudget())) {
                // Create new parent category budget
                BigDecimal parentCatAmount = dto.getParentCategoryBudgetAmount() != null
                        ? dto.getParentCategoryBudgetAmount()
                        : dto.getTotalAmount();

                Budget newParentCatBudget = new Budget();
                newParentCatBudget.setYear(dto.getYear());
                newParentCatBudget.setMonth(dto.getMonth());
                newParentCatBudget.setTotalAmount(parentCatAmount);
                newParentCatBudget.setDescription("Auto-created parent category budget");
                newParentCatBudget.setCreatedBy(username);
                newParentCatBudget.setCategory(parentCat);
                budgetRepository.save(newParentCatBudget);

                result.setParentCategoryBudgetUpdated(new BudgetDTO.ParentCategoryBudgetUpdateInfo(
                        parentCat.getName(), BigDecimal.ZERO, parentCatAmount, dto.getYear(), dto.getMonth()));

                logger.info("Created parent category budget for '{}' with amount {} for {}/{}",
                        parentCat.getName(), parentCatAmount, dto.getYear(), dto.getMonth());
            }
        }

        return result;
    }

    /**
     * Get all budgets ordered by date (newest first).
     *
     * @return list of budget summaries with spending calculations
     */
    @Transactional(readOnly = true)
    public List<BudgetSummaryDTO> getAllBudgets() {
        List<Budget> budgets = budgetRepository.findAllByOrderByYearDescMonthDesc();
        return budgets.stream()
                .map(this::mapToBudgetSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get budget by ID.
     * Returns budget info with spending calculated from category expense aggregates.
     *
     * @param id budget ID
     * @return budget summary with spending from category aggregates
     * @throws BudgetNotFoundException if budget not found
     */
    @Transactional(readOnly = true)
    public BudgetSummaryDTO getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        return mapToBudgetSummaryWithSpending(budget);
    }

    /**
     * Update budget (amount and description, year/month/category immutable).
     * Validates parent budget constraints when amount changes.
     *
     * @param id budget ID
     * @param dto updated budget data
     * @return updated budget
     * @throws BudgetNotFoundException if budget not found
     * @throws ParentBudgetMismatchException if parent budget validation fails
     */
    public BudgetDTO updateBudget(Long id, BudgetDTO dto) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        Category category = budget.getCategory();

        // Feature 017: Calculate delta for parent budget cascade
        BigDecimal oldAmount = budget.getTotalAmount();
        BigDecimal newAmount = dto.getTotalAmount();
        BigDecimal amountDelta = newAmount.subtract(oldAmount);

        // If amount is changing, validate yearly parent/child budget constraints
        if (dto.getTotalAmount().compareTo(budget.getTotalAmount()) != 0) {
            if (budget.getMonth() == null) {
                BigDecimal monthlySum = defaultZero(
                        budgetRepository.sumMonthlyBudgetsForCategory(category.getId(), budget.getYear()));
                if (dto.getTotalAmount().compareTo(monthlySum) < 0) {
                    throw new ParentBudgetMismatchException(
                            String.format("Cannot update yearly budget to %.2f: sum of monthly budgets is %.2f",
                                    dto.getTotalAmount(), monthlySum)
                    );
                }
            } else {
                Budget parentBudget = budgetRepository.findParentBudget(category.getId(), budget.getYear())
                        .orElseThrow(() -> new ParentBudgetMismatchException(
                                String.format("Parent yearly budget is required for category '%s' in %d",
                                        category.getName(), budget.getYear())));
                BigDecimal monthlySum = defaultZero(
                        budgetRepository.sumMonthlyBudgetsForCategory(category.getId(), budget.getYear()));
                BigDecimal newSum = monthlySum.subtract(budget.getTotalAmount()).add(dto.getTotalAmount());
                if (newSum.compareTo(parentBudget.getTotalAmount()) > 0) {
                    throw new ParentBudgetMismatchException(
                            String.format("Total monthly budgets (%.2f) exceed parent yearly budget (%.2f)",
                                    newSum, parentBudget.getTotalAmount())
                    );
                }
            }
        }

        // Only allow updating amount and description (year/month/category are immutable)
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setDescription(dto.getDescription());

        Budget updated = budgetRepository.save(budget);

        // Feature 017: Cascade amount change to parent category budget (if amount changed)
        if (amountDelta.compareTo(BigDecimal.ZERO) != 0) {
            cascadeToParentBudget(updated, amountDelta);
        }

        return mapToBudgetDTO(updated);
    }

    /**
     * Delete budget.
     *
     * @param id budget ID
     * @throws BudgetNotFoundException if budget not found
     */
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        // Feature 017: Cascade budget deletion to parent category (subtract child's amount)
        // Must be done BEFORE deleting the budget (need category relationship intact)
        BigDecimal amountDelta = budget.getTotalAmount().negate(); // Negative delta
        cascadeToParentBudget(budget, amountDelta);

        budgetRepository.delete(budget);
    }

    /**
     * Calculate total spending for a budget using category expense aggregates.
     * For monthly budgets: sum of expenses for the budget's category in that month.
     * For yearly budgets: sum of expenses for the budget's category in that year.
     * For parent categories: includes children's spending.
     *
     * @param budgetId budget ID
     * @return total amount spent
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSpending(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget == null || budget.getCategory() == null) {
            return BigDecimal.ZERO;
        }
        return getCategorySpending(budget.getCategory(), budget.getYear(), budget.getMonth());
    }

    /**
     * Calculate spending percentage for a budget using category expense aggregates.
     *
     * @param budget the budget entity
     * @return percentage of budget spent
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSpendingPercentage(Budget budget) {
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal spending = getCategorySpending(budget.getCategory(), budget.getYear(), budget.getMonth());
        return spending.multiply(new BigDecimal("100"))
                .divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP);
    }

    /**
     * Check if budget's category has expenses in the budget's period.
     *
     * @param budgetId budget ID
     * @return true if category has expenses in the budget period
     */
    @Transactional(readOnly = true)
    public boolean hasExpenses(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget == null || budget.getCategory() == null) {
            return false;
        }
        BigDecimal spending = getCategorySpending(budget.getCategory(), budget.getYear(), budget.getMonth());
        return spending.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get spending for a category in a given period, including children's spending for parent categories.
     */
    private BigDecimal getCategorySpending(Category category, Integer year, Integer month) {
        if (category == null) return BigDecimal.ZERO;

        BigDecimal directSpending;
        if (month != null) {
            directSpending = defaultZero(expenseRepository.sumByCategoryAndMonth(category.getId(), year, month));
        } else {
            directSpending = defaultZero(expenseRepository.sumByCategoryAndYear(category.getId(), year));
        }

        // Add children's spending for parent categories
        List<Category> children = categoryRepository.findByParentCategoryId(category.getId());
        BigDecimal childrenSpending = BigDecimal.ZERO;
        for (Category child : children) {
            if (month != null) {
                childrenSpending = childrenSpending.add(
                        defaultZero(expenseRepository.sumByCategoryAndMonth(child.getId(), year, month)));
            } else {
                childrenSpending = childrenSpending.add(
                        defaultZero(expenseRepository.sumByCategoryAndYear(child.getId(), year)));
            }
        }

        return directSpending.add(childrenSpending);
    }

    /**
     * Cascade budget changes to parent category budget (Feature 017: Parent Budget Rollup).
     * When a child category budget is created, updated, or deleted, automatically adjusts
     * the parent category budget by the specified amount delta.
     *
     * @param childBudget the child category budget being modified
     * @param amountDelta the amount change to apply to parent budget
     *                    (+amount for create, +/-delta for update, -amount for delete)
     */
    private void cascadeToParentBudget(Budget childBudget, BigDecimal amountDelta) {
        Category childCategory = childBudget.getCategory();

        // Only cascade if child category has a parent (not standalone)
        if (childCategory.getParentCategory() == null) {
            return;
        }

        Category parentCategory = childCategory.getParentCategory();
        Integer year = childBudget.getYear();
        Integer month = childBudget.getMonth(); // nullable (null = yearly budget)

        // Find or create parent category budget for same period
        Budget parentBudget = budgetRepository
                .findByCategoryAndYearAndMonth(parentCategory, year, month)
                .orElseGet(() -> {
                    // Create new parent budget with zero amount (will be updated below)
                    Budget newParent = new Budget();
                    newParent.setCategory(parentCategory);
                    newParent.setYear(year);
                    newParent.setMonth(month);
                    newParent.setTotalAmount(BigDecimal.ZERO);
                    newParent.setDescription("Auto-created parent category budget");
                    newParent.setCreatedBy(childBudget.getCreatedBy());
                    return budgetRepository.save(newParent);
                });

        // Update parent budget amount by adding delta
        BigDecimal oldAmount = parentBudget.getTotalAmount();
        BigDecimal newAmount = oldAmount.add(amountDelta);
        parentBudget.setTotalAmount(newAmount);
        budgetRepository.save(parentBudget);

        logger.debug("Cascaded budget change to parent category '{}': {} + {} = {} for {}/{}",
                parentCategory.getName(), oldAmount, amountDelta, newAmount, year, month);
    }

    private Budget buildBudgetEntity(BudgetDTO dto, String username, Category category) {
        Budget budget = new Budget();
        budget.setYear(dto.getYear());
        budget.setMonth(dto.getMonth());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setDescription(dto.getDescription());
        budget.setCreatedBy(username);
        budget.setCategory(category);
        return budget;
    }

    private void createMonthlyBudgetsForYear(Category category, Integer year, BigDecimal yearlyAmount, String username) {
        BigDecimal monthlyBase = yearlyAmount
                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAllocated = monthlyBase.multiply(new BigDecimal("11"));
        BigDecimal lastMonthAmount = yearlyAmount.subtract(totalAllocated);

        for (int month = 1; month <= 12; month++) {
            Budget monthly = new Budget();
            monthly.setYear(year);
            monthly.setMonth(month);
            monthly.setTotalAmount(month == 12 ? lastMonthAmount : monthlyBase);
            monthly.setDescription("Auto-generated from yearly budget");
            monthly.setCreatedBy(username);
            monthly.setCategory(category);
            budgetRepository.save(monthly);
        }
    }

    private void ensureMonthlyBudgetsForRemainingMonths(Category category, Integer year, Integer startMonth,
                                                        BigDecimal monthlyAmount, String username) {
        for (int month = startMonth; month <= 12; month++) {
            if (budgetRepository.existsByCategoryIdAndYearAndMonth(category.getId(), year, month)) {
                continue;
            }
            Budget monthly = buildMonthlyBudget(category, year, month, monthlyAmount, username,
                    "Auto-generated monthly budget");
            budgetRepository.save(monthly);
        }
    }

    private void alignParentBudgetWithMonthlySum(Long categoryId, Integer year) {
        Budget parent = budgetRepository.findParentBudget(categoryId, year).orElse(null);
        if (parent == null) {
            return;
        }
        BigDecimal monthlySum = defaultZero(budgetRepository.sumMonthlyBudgetsForCategory(categoryId, year));
        if (parent.getTotalAmount().compareTo(monthlySum) < 0) {
            parent.setTotalAmount(monthlySum);
            budgetRepository.save(parent);
        }
    }

    private Budget buildMonthlyBudget(Category category, Integer year, Integer month, BigDecimal amount,
                                      String username, String description) {
        Budget budget = new Budget();
        budget.setYear(year);
        budget.setMonth(month);
        budget.setTotalAmount(amount);
        budget.setDescription(description);
        budget.setCreatedBy(username);
        budget.setCategory(category);
        return budget;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Mapping methods

    private BudgetDTO mapToBudgetDTO(Budget budget) {
        BudgetDTO dto = new BudgetDTO(
                budget.getId(),
                budget.getYear(),
                budget.getMonth(),
                budget.getTotalAmount(),
                budget.getDescription(),
                budget.getCreatedBy(),
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                budget.getVersion()
        );

        // Include category information
        if (budget.getCategory() != null) {
            Category category = budget.getCategory();
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(category.getId());
            categoryDTO.setName(category.getName());
            categoryDTO.setIcon(category.getIcon());

            // Include parent category if exists
            if (category.getParentCategory() != null) {
                CategoryDTO parentDTO = new CategoryDTO();
                parentDTO.setId(category.getParentCategory().getId());
                parentDTO.setName(category.getParentCategory().getName());
                categoryDTO.setParentCategory(parentDTO);
                categoryDTO.setParentCategoryId(category.getParentCategory().getId());
            }

            dto.setCategory(categoryDTO);
            dto.setCategoryId(category.getId());
        }

        return dto;
    }

    /**
     * Map budget to summary DTO with real spending from category expense aggregates.
     */
    private BudgetSummaryDTO mapToBudgetSummaryWithSpending(Budget budget) {
        BigDecimal totalSpending = BigDecimal.ZERO;
        long expenseCount = 0L;

        if (budget.getCategory() != null) {
            totalSpending = getCategorySpending(budget.getCategory(), budget.getYear(), budget.getMonth());
            // Count expenses for the category in this period
            expenseCount = countCategoryExpenses(budget.getCategory(), budget.getYear(), budget.getMonth());
        }

        BigDecimal spendingPercentage = BigDecimal.ZERO;
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            spendingPercentage = totalSpending.multiply(new BigDecimal("100"))
                    .divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP);
        }

        return buildBudgetSummary(budget, totalSpending, spendingPercentage, expenseCount);
    }

    /**
     * Map budget to summary DTO using pre-computed spending values.
     * Used in batch scenarios where spending is already calculated (e.g., yearly view).
     */
    private BudgetSummaryDTO mapToBudgetSummary(Budget budget, BigDecimal totalSpending) {
        BigDecimal spendingPercentage = BigDecimal.ZERO;
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            spendingPercentage = totalSpending.multiply(new BigDecimal("100"))
                    .divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP);
        }
        return buildBudgetSummary(budget, totalSpending, spendingPercentage, 0L);
    }

    /**
     * Map budget to summary DTO (default: computes spending from aggregates).
     */
    private BudgetSummaryDTO mapToBudgetSummary(Budget budget) {
        return mapToBudgetSummaryWithSpending(budget);
    }

    private BudgetSummaryDTO buildBudgetSummary(Budget budget, BigDecimal totalSpending,
                                                  BigDecimal spendingPercentage, long expenseCount) {
        BudgetSummaryDTO summary = new BudgetSummaryDTO(
                budget.getId(),
                budget.getYear(),
                budget.getMonth(),
                budget.getTotalAmount(),
                budget.getDescription(),
                budget.getCreatedBy(),
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                budget.getVersion(),
                totalSpending,
                spendingPercentage,
                expenseCount
        );

        if (budget.getCategory() != null) {
            Category category = budget.getCategory();
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(category.getId());
            categoryDTO.setName(category.getName());
            categoryDTO.setIcon(category.getIcon());

            if (category.getParentCategory() != null) {
                CategoryDTO parentDTO = new CategoryDTO();
                parentDTO.setId(category.getParentCategory().getId());
                parentDTO.setName(category.getParentCategory().getName());
                categoryDTO.setParentCategory(parentDTO);
                categoryDTO.setParentCategoryId(category.getParentCategory().getId());
            }

            summary.setCategoryId(category.getId());
            summary.setCategory(categoryDTO);

            long childCount = categoryRepository.countByParentCategoryId(category.getId());
            summary.setIsParentCategory(childCount > 0);
        }

        return summary;
    }

    /**
     * Count expenses for a category in a given period (including children for parent categories).
     */
    private long countCategoryExpenses(Category category, Integer year, Integer month) {
        // Use a simple date range approach for counting
        LocalDate startDate;
        LocalDate endDate;
        if (month != null) {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        long count = expenseRepository.countByFiltersPageable(
                category.getId(), startDate, endDate, null, null, null, null);

        // Add children's expenses for parent categories
        List<Category> children = categoryRepository.findByParentCategoryId(category.getId());
        for (Category child : children) {
            count += expenseRepository.countByFiltersPageable(
                    child.getId(), startDate, endDate, null, null, null, null);
        }

        return count;
    }

    /**
     * Get current month's budget.
     * Used for dashboard display.
     */
    @Transactional(readOnly = true)
    public BudgetSummaryDTO getCurrentMonthBudget() {
        LocalDate now = LocalDate.now();
        return getMonthBudgetSummary(now.getYear(), now.getMonthValue());
    }

    /**
     * Get budget summary for a specific month.
     * Aggregates all budgets for the given year/month into a single summary.
     * Spending is calculated from category expense aggregates.
     */
    @Transactional(readOnly = true)
    public BudgetSummaryDTO getMonthBudgetSummary(int year, int month) {
        List<Budget> budgets = budgetRepository.findByYearAndMonthOrderByCategoryIdAsc(year, month);
        if (budgets.isEmpty()) {
            return null;
        }

        BigDecimal totalAmount = budgets.stream()
                .map(Budget::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total spending across all budgeted categories for this month
        BigDecimal totalSpending = BigDecimal.ZERO;
        for (Budget budget : budgets) {
            if (budget.getCategory() != null) {
                totalSpending = totalSpending.add(
                        getCategorySpending(budget.getCategory(), year, month));
            }
        }

        BigDecimal spendingPercentage = BigDecimal.ZERO;
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            spendingPercentage = totalSpending.multiply(new BigDecimal("100"))
                    .divide(totalAmount, 2, RoundingMode.HALF_UP);
        }

        // Count total expenses across all categories for this month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        long expenseCount = expenseRepository.countByFiltersPageable(
                null, startDate, endDate, null, null, null, null);

        BudgetSummaryDTO summary = new BudgetSummaryDTO(
                null,
                year,
                month,
                totalAmount,
                "All categories",
                "system",
                null,
                null,
                null,
                totalSpending,
                spendingPercentage,
                expenseCount
        );

        return summary;
    }

    /**
     * Return validation hints for budget creation UI.
     */
    @Transactional(readOnly = true)
    public BudgetValidationDTO getBudgetValidation(Long categoryId, Integer year, Integer month) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        BudgetValidationDTO result = new BudgetValidationDTO();

        if (month == null) {
            boolean duplicate = budgetRepository.existsParentBudget(categoryId, year);
            result.setDuplicate(duplicate);
            if (duplicate) {
                result.setDuplicateMessage(String.format(
                        "Yearly budget already exists for category '%s' in %d", category.getName(), year));
            }
        } else {
            boolean duplicate = budgetRepository.existsByCategoryIdAndYearAndMonth(categoryId, year, month);
            result.setDuplicate(duplicate);
            if (duplicate) {
                result.setDuplicateMessage(String.format(
                        "Budget already exists for category '%s' in %d-%02d", category.getName(), year, month));
            }
        }

        Budget parentBudget = budgetRepository.findParentBudget(categoryId, year).orElse(null);
        result.setParentBudgetExists(parentBudget != null);
        if (parentBudget != null) {
            result.setParentBudgetId(parentBudget.getId());
            result.setParentBudgetAmount(parentBudget.getTotalAmount());
        }

        BigDecimal monthlySum = defaultZero(budgetRepository.sumMonthlyBudgetsForCategory(categoryId, year));
        result.setMonthlyBudgetSum(monthlySum);
        result.setMonthlyBudgetsExist(budgetRepository.countMonthlyBudgetsForCategory(categoryId, year) > 0);

        // Parent category budget info (category hierarchy)
        if (category.getParentCategory() != null) {
            Long parentCatId = category.getParentCategory().getId();
            result.setParentCategoryName(category.getParentCategory().getName());

            if (month != null) {
                Budget parentCatBudget = budgetRepository.findByCategoryIdAndYearAndMonth(parentCatId, year, month).orElse(null);
                if (parentCatBudget != null) {
                    result.setParentCategoryBudgetExists(true);
                    result.setParentCategoryBudgetId(parentCatBudget.getId());
                    result.setParentCategoryBudgetAmount(parentCatBudget.getTotalAmount());
                } else {
                    result.setParentCategoryBudgetExists(false);
                }
            } else {
                Budget parentCatBudget = budgetRepository.findParentBudget(parentCatId, year).orElse(null);
                if (parentCatBudget != null) {
                    result.setParentCategoryBudgetExists(true);
                    result.setParentCategoryBudgetId(parentCatBudget.getId());
                    result.setParentCategoryBudgetAmount(parentCatBudget.getTotalAmount());
                } else {
                    result.setParentCategoryBudgetExists(false);
                }
            }
        }

        return result;
    }

    /**
     * Get yearly budget view for all categories and months.
     * Spending data sourced from category expense aggregates.
     */
    @Transactional(readOnly = true)
    public YearlyBudgetViewDTO getYearlyBudgetView(Integer year) {
        List<Budget> budgets = budgetRepository.findByYear(year);

        // Batch-fetch all monthly spending aggregates for the year
        Map<Long, Map<Integer, BigDecimal>> monthlySpendingMap = expenseAggregateService.getMonthlySpendingMap(year);
        // Batch-fetch yearly spending totals
        Map<Long, BigDecimal> yearlySpendingMap = expenseAggregateService.getYearlySpendingMap(year);

        YearlyBudgetViewDTO view = new YearlyBudgetViewDTO();
        view.setYear(year);

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpending = BigDecimal.ZERO;

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        for (Category category : categories) {
            YearlyCategoryBudgetDTO categoryDTO = new YearlyCategoryBudgetDTO();
            categoryDTO.setCategoryId(category.getId());
            categoryDTO.setCategoryName(category.getName());
            categoryDTO.setCategoryIcon(category.getIcon());
            if (category.getParentCategory() != null) {
                categoryDTO.setParentCategoryId(category.getParentCategory().getId());
                categoryDTO.setParentCategoryName(category.getParentCategory().getName());
            }

            Budget parentBudget = budgets.stream()
                    .filter(b -> b.getCategory() != null
                            && b.getCategory().getId().equals(category.getId())
                            && b.getMonth() == null)
                    .findFirst()
                    .orElse(null);

            BigDecimal monthlySum = BigDecimal.ZERO;

            // Get children IDs for parent rollup
            List<Category> children = categoryRepository.findByParentCategoryId(category.getId());

            // Calculate yearly spending: direct + children's
            BigDecimal directYearlySpending = yearlySpendingMap.getOrDefault(category.getId(), BigDecimal.ZERO);
            BigDecimal childrenYearlySpending = BigDecimal.ZERO;
            for (Category child : children) {
                childrenYearlySpending = childrenYearlySpending.add(
                        yearlySpendingMap.getOrDefault(child.getId(), BigDecimal.ZERO));
            }
            BigDecimal yearlySpending = directYearlySpending.add(childrenYearlySpending);

            for (int month = 1; month <= 12; month++) {
                final int monthValue = month;
                Budget monthlyBudget = budgets.stream()
                        .filter(b -> b.getCategory() != null
                                && b.getCategory().getId().equals(category.getId())
                                && b.getMonth() != null
                                && b.getMonth() == monthValue)
                        .findFirst()
                        .orElse(null);

                YearlyMonthlyBudgetDTO monthDTO = new YearlyMonthlyBudgetDTO();
                monthDTO.setMonth(monthValue);

                // Get monthly spending from aggregates: direct + children
                BigDecimal directMonthSpending = BigDecimal.ZERO;
                Map<Integer, BigDecimal> catMonthly = monthlySpendingMap.get(category.getId());
                if (catMonthly != null) {
                    directMonthSpending = catMonthly.getOrDefault(monthValue, BigDecimal.ZERO);
                }
                BigDecimal childrenMonthSpending = BigDecimal.ZERO;
                for (Category child : children) {
                    Map<Integer, BigDecimal> childMonthly = monthlySpendingMap.get(child.getId());
                    if (childMonthly != null) {
                        childrenMonthSpending = childrenMonthSpending.add(
                                childMonthly.getOrDefault(monthValue, BigDecimal.ZERO));
                    }
                }
                BigDecimal monthSpending = directMonthSpending.add(childrenMonthSpending);

                if (monthlyBudget != null) {
                    monthDTO.setBudgetAmount(monthlyBudget.getTotalAmount());
                    monthDTO.setSpending(monthSpending);
                    monthDTO.setRemaining(monthlyBudget.getTotalAmount().subtract(monthSpending));
                    monthDTO.setHasBudget(true);

                    monthlySum = monthlySum.add(monthlyBudget.getTotalAmount());
                } else {
                    monthDTO.setBudgetAmount(BigDecimal.ZERO);
                    monthDTO.setSpending(monthSpending);
                    monthDTO.setRemaining(BigDecimal.ZERO.subtract(monthSpending));
                    monthDTO.setHasBudget(false);
                }

                categoryDTO.getMonths().add(monthDTO);
            }

            BigDecimal yearlyBudgetAmount = parentBudget != null ? parentBudget.getTotalAmount() : monthlySum;

            categoryDTO.setYearlyBudgetAmount(yearlyBudgetAmount);
            categoryDTO.setMonthlyBudgetSum(monthlySum);
            categoryDTO.setYearlySpending(yearlySpending);
            categoryDTO.setYearlyRemaining(yearlyBudgetAmount.subtract(yearlySpending));

            // Feature 017: Only count parent categories + standalone categories in yearly total
            // Exclude child categories to avoid double-counting (parent budget already includes children)
            boolean isStandalone = category.getParentCategory() == null && children.isEmpty();
            boolean isParent = category.getParentCategory() == null && !children.isEmpty();
            boolean shouldCountInTotal = isStandalone || isParent;

            if (shouldCountInTotal) {
                totalBudget = totalBudget.add(yearlyBudgetAmount);
                totalSpending = totalSpending.add(yearlySpending);
            }

            view.getCategories().add(categoryDTO);
        }

        view.setTotalBudget(totalBudget);
        view.setTotalSpending(totalSpending);
        view.setTotalRemaining(totalBudget.subtract(totalSpending));

        return view;
    }

    // Legacy category parent/child budget validation removed in favor of yearly parent budgets.
}
