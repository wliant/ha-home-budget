package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.BudgetValidationDTO;
import com.homebudget.dto.YearlyBudgetViewDTO;
import com.homebudget.dto.YearlyCategoryBudgetDTO;
import com.homebudget.dto.YearlyMonthlyBudgetDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
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
import java.util.HashMap;
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

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
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
            reassignParentExpensesToMonthlyBudgets(category, dto.getYear(), dto.getTotalAmount(), username);
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
     * Get budget by ID with expenses.
     *
     * @param id budget ID
     * @return budget summary with expenses list
     * @throws BudgetNotFoundException if budget not found
     */
    @Transactional(readOnly = true)
    public BudgetSummaryDTO getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        BudgetSummaryDTO summary = mapToBudgetSummary(budget);

        // Add expenses to the summary
        List<ExpenseDTO> expenseDTOs = budget.getExpenses().stream()
                .map(this::mapToExpenseDTO)
                .collect(Collectors.toList());
        summary.setExpenses(expenseDTOs);

        return summary;
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
        return mapToBudgetDTO(updated);
    }

    /**
     * Delete budget.
     * If budget has expenses, they will be cascade deleted (handled by JPA).
     *
     * @param id budget ID
     * @throws BudgetNotFoundException if budget not found
     */
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        budgetRepository.delete(budget);
    }

    /**
     * Calculate total spending for a budget.
     *
     * @param budgetId budget ID
     * @return total amount spent
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSpending(Long budgetId) {
        BigDecimal totalSpending = expenseRepository.sumAmountByBudgetId(budgetId);

        if (logger.isDebugEnabled()) {
            logger.debug("Calculated total spending for budgetId={}: amount={}", budgetId, totalSpending);
        }

        return totalSpending;
    }

    /**
     * Calculate spending percentage for a budget.
     *
     * @param budget the budget entity
     * @return percentage of budget spent (0-100+)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSpendingPercentage(Budget budget) {
        BigDecimal totalSpending = calculateTotalSpending(budget.getId());
        BigDecimal totalAmount = budget.getTotalAmount();

        if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Budget ID={} has zero spending", budget.getId());
            return BigDecimal.ZERO;
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Budget ID={} has zero total amount; returning 0% spending", budget.getId());
            return BigDecimal.ZERO;
        }

        BigDecimal percentage = totalSpending
                .divide(totalAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        if (logger.isDebugEnabled()) {
            logger.debug("Calculated spending percentage for budgetId={}: spent={}, budget={}, percentage={}%",
                    budget.getId(), totalSpending, budget.getTotalAmount(), percentage);
        }

        return percentage;
    }

    /**
     * Check if budget has expenses.
     *
     * @param budgetId budget ID
     * @return true if budget has expenses
     */
    @Transactional(readOnly = true)
    public boolean hasExpenses(Long budgetId) {
        return expenseRepository.countByBudgetId(budgetId) > 0;
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

    private void reassignParentExpensesToMonthlyBudgets(Category category, Integer year,
                                                        BigDecimal monthlyAmount, String username) {
        Budget parent = budgetRepository.findParentBudget(category.getId(), year).orElse(null);
        if (parent == null) {
            return;
        }

        List<Expense> parentExpenses = expenseRepository.findByBudgetId(parent.getId());
        if (parentExpenses.isEmpty()) {
            return;
        }

        List<Budget> monthlyBudgets = budgetRepository.findMonthlyBudgetsForCategory(category.getId(), year);
        Map<Integer, Budget> monthToBudget = new HashMap<>();
        for (Budget budget : monthlyBudgets) {
            if (budget.getMonth() != null) {
                monthToBudget.put(budget.getMonth(), budget);
            }
        }

        for (Expense expense : parentExpenses) {
            int expenseMonth = expense.getExpenseDate().getMonthValue();
            Budget monthly = monthToBudget.get(expenseMonth);
            if (monthly == null) {
                monthly = buildMonthlyBudget(category, year, expenseMonth, monthlyAmount, username,
                        "Auto-generated from existing expenses");
                monthly = budgetRepository.save(monthly);
                monthToBudget.put(expenseMonth, monthly);
            }
            expense.setBudget(monthly);
        }

        expenseRepository.saveAll(parentExpenses);
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

    private BudgetSummaryDTO mapToBudgetSummary(Budget budget) {
        BigDecimal totalSpending = calculateTotalSpending(budget.getId());
        BigDecimal spendingPercentage = calculateSpendingPercentage(budget);
        Long expenseCount = expenseRepository.countByBudgetId(budget.getId());

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

            // Parent category aggregation: include child category budgets and spending
            long childCount = categoryRepository.countByParentCategoryId(category.getId());
            if (childCount > 0 && budget.getMonth() != null) {
                summary.setIsParentCategory(true);
                BigDecimal childBudgetSum = defaultZero(
                        budgetRepository.sumBudgetsByChildCategoriesAndPeriod(category.getId(), budget.getYear(), budget.getMonth()));
                BigDecimal childSpending = defaultZero(
                        expenseRepository.sumExpensesByParentCategoryBudgets(category.getId(), budget.getYear(), budget.getMonth()));
                summary.setChildrenBudgetSum(childBudgetSum);
                summary.setChildrenSpending(childSpending);
                // Add child spending to total spending for display
                summary.setTotalSpending(totalSpending.add(childSpending));
                // Recalculate spending percentage with aggregated spending
                BigDecimal aggregatedSpending = totalSpending.add(childSpending);
                if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    summary.setSpendingPercentage(aggregatedSpending
                            .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP));
                }
            } else {
                summary.setIsParentCategory(childCount > 0);
            }
        }

        return summary;
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

        BigDecimal totalSpending = budgets.stream()
                .map(budget -> defaultZero(expenseRepository.sumAmountByBudgetId(budget.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long expenseCount = budgets.stream()
                .map(budget -> expenseRepository.countByBudgetId(budget.getId()))
                .reduce(0L, Long::sum);

        BigDecimal spendingPercentage = BigDecimal.ZERO;
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            spendingPercentage = totalSpending
                    .divide(totalAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

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
     */
    @Transactional(readOnly = true)
    public YearlyBudgetViewDTO getYearlyBudgetView(Integer year) {
        List<Budget> budgets = budgetRepository.findByYear(year);

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
            BigDecimal yearlySpending = BigDecimal.ZERO;

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

                if (monthlyBudget != null) {
                    BigDecimal spending = defaultZero(calculateTotalSpending(monthlyBudget.getId()));
                    monthDTO.setBudgetAmount(monthlyBudget.getTotalAmount());
                    monthDTO.setSpending(spending);
                    monthDTO.setRemaining(monthlyBudget.getTotalAmount().subtract(spending));
                    monthDTO.setHasBudget(true);

                    monthlySum = monthlySum.add(monthlyBudget.getTotalAmount());
                    yearlySpending = yearlySpending.add(spending);
                } else {
                    monthDTO.setBudgetAmount(BigDecimal.ZERO);
                    monthDTO.setSpending(BigDecimal.ZERO);
                    monthDTO.setRemaining(BigDecimal.ZERO);
                    monthDTO.setHasBudget(false);
                }

                categoryDTO.getMonths().add(monthDTO);
            }

            BigDecimal yearlyBudgetAmount = parentBudget != null ? parentBudget.getTotalAmount() : monthlySum;
            BigDecimal parentSpending = parentBudget != null ? defaultZero(calculateTotalSpending(parentBudget.getId())) : BigDecimal.ZERO;
            yearlySpending = yearlySpending.add(parentSpending);

            // For parent categories, aggregate child category spending
            long childCount = categoryRepository.countByParentCategoryId(category.getId());
            if (childCount > 0) {
                for (int m = 1; m <= 12; m++) {
                    BigDecimal childMonthSpending = defaultZero(
                            expenseRepository.sumExpensesByParentCategoryBudgets(category.getId(), year, m));
                    yearlySpending = yearlySpending.add(childMonthSpending);
                }
            }

            categoryDTO.setYearlyBudgetAmount(yearlyBudgetAmount);
            categoryDTO.setMonthlyBudgetSum(monthlySum);
            categoryDTO.setYearlySpending(yearlySpending);
            categoryDTO.setYearlyRemaining(yearlyBudgetAmount.subtract(yearlySpending));

            totalBudget = totalBudget.add(yearlyBudgetAmount);
            totalSpending = totalSpending.add(yearlySpending);

            view.getCategories().add(categoryDTO);
        }

        view.setTotalBudget(totalBudget);
        view.setTotalSpending(totalSpending);
        view.setTotalRemaining(totalBudget.subtract(totalSpending));

        return view;
    }

    private ExpenseDTO mapToExpenseDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setBudgetId(expense.getBudget().getId());

        if (expense.getCategory() != null) {
            CategoryDTO categoryDTO = new CategoryDTO(
                    expense.getCategory().getId(),
                    expense.getCategory().getName(),
                    expense.getCategory().getIcon()
            );
            dto.setCategory(categoryDTO);
            dto.setCategoryId(expense.getCategory().getId());
        }

        dto.setCreatedBy(expense.getCreatedBy());
        dto.setCreatedAt(expense.getCreatedAt());
        dto.setUpdatedAt(expense.getUpdatedAt());
        dto.setVersion(expense.getVersion());

        return dto;
    }

    // Legacy category parent/child budget validation removed in favor of yearly parent budgets.
}
