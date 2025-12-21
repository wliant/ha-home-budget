package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Budget operations.
 * Handles business logic for budget management including spending calculations.
 */
@Service
@Transactional
public class BudgetService {

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

        // Check for duplicate budget (category + year + month uniqueness)
        if (budgetRepository.existsByCategoryIdAndYearAndMonth(dto.getCategoryId(), dto.getYear(), dto.getMonth())) {
            throw new DuplicateBudgetException(
                    String.format("Budget already exists for category '%s' in %d-%02d",
                            category.getName(), dto.getYear(), dto.getMonth())
            );
        }

        // If this is a child category, validate parent budget
        if (category.getParentCategory() != null) {
            validateParentBudgetOnCreate(category, dto.getYear(), dto.getMonth(), dto.getTotalAmount());
        }

        // If this is a parent category, validate child budget sum
        if (category.getParentCategory() == null) {
            validateChildBudgetSumOnCreate(category.getId(), dto.getYear(), dto.getMonth(), dto.getTotalAmount());
        }

        Budget budget = new Budget();
        budget.setYear(dto.getYear());
        budget.setMonth(dto.getMonth());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setDescription(dto.getDescription());
        budget.setCreatedBy(username);
        budget.setCategory(category);

        Budget saved = budgetRepository.save(budget);
        return mapToBudgetDTO(saved);
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

        // If amount is changing, validate parent/child budget constraints
        if (dto.getTotalAmount().compareTo(budget.getTotalAmount()) != 0) {
            // If this is a child category, validate parent budget
            if (category != null && category.getParentCategory() != null) {
                validateParentBudgetOnUpdate(id, category, budget.getYear(), budget.getMonth(),
                        dto.getTotalAmount(), budget.getTotalAmount());
            }

            // If this is a parent category, validate child budget sum
            if (category != null && category.getParentCategory() == null) {
                validateChildBudgetSumOnUpdate(id, category.getId(), budget.getYear(), budget.getMonth(),
                        dto.getTotalAmount());
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
        return expenseRepository.sumAmountByBudgetId(budgetId);
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

        if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalSpending
                .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
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

        return new BudgetSummaryDTO(
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
    }

    /**
     * Get current month's budget.
     * Used for dashboard display.
     */
    @Transactional(readOnly = true)
    public BudgetSummaryDTO getCurrentMonthBudget() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        return budgetRepository.findByYearAndMonth(year, month)
                .map(this::mapToBudgetSummary)
                .orElse(null);
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

    // ========== Parent Budget Validation Methods ==========

    /**
     * Validate parent budget when creating a child budget.
     * Ensures parent budget exists and sum of all child budgets equals parent.
     */
    private void validateParentBudgetOnCreate(Category childCategory, Integer year, Integer month, BigDecimal childAmount) {
        Category parent = childCategory.getParentCategory();

        // Check if parent budget exists
        Budget parentBudget = budgetRepository.findByCategoryIdAndYearAndMonth(parent.getId(), year, month)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Parent category '%s' must have a budget for %d-%02d before creating child budgets",
                                parent.getName(), year, month)
                ));

        // Calculate sum of all child budgets (including this new one)
        BigDecimal existingChildSum = budgetRepository.sumByParentCategoryAndPeriod(parent.getId(), year, month);
        if (existingChildSum == null) {
            existingChildSum = BigDecimal.ZERO;
        }
        BigDecimal newChildSum = existingChildSum.add(childAmount);

        // Validate sum equals parent budget
        if (newChildSum.compareTo(parentBudget.getTotalAmount()) != 0) {
            throw new ParentBudgetMismatchException(
                    String.format("Adding budget of %.2f to category '%s' would make total child budgets (%.2f) not equal parent budget (%.2f)",
                            childAmount, childCategory.getName(), newChildSum, parentBudget.getTotalAmount())
            );
        }
    }

    /**
     * Validate parent budget when updating a child budget amount.
     */
    private void validateParentBudgetOnUpdate(Long budgetId, Category childCategory, Integer year, Integer month,
                                              BigDecimal newAmount, BigDecimal oldAmount) {
        Category parent = childCategory.getParentCategory();

        // Get parent budget
        Budget parentBudget = budgetRepository.findByCategoryIdAndYearAndMonth(parent.getId(), year, month)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Parent category '%s' budget not found for %d-%02d",
                                parent.getName(), year, month)
                ));

        // Calculate current sum of child budgets (excluding this budget)
        BigDecimal otherChildSum = BigDecimal.ZERO;
        List<Budget> childBudgets = budgetRepository.findChildBudgets(parent.getId(), year, month);
        for (Budget child : childBudgets) {
            if (!child.getId().equals(budgetId)) {
                otherChildSum = otherChildSum.add(child.getTotalAmount());
            }
        }

        // Add the new amount for this budget
        BigDecimal newChildSum = otherChildSum.add(newAmount);

        // Validate sum equals parent budget
        if (newChildSum.compareTo(parentBudget.getTotalAmount()) != 0) {
            throw new ParentBudgetMismatchException(
                    String.format("Updating budget from %.2f to %.2f would make total child budgets (%.2f) not equal parent budget (%.2f)",
                            oldAmount, newAmount, newChildSum, parentBudget.getTotalAmount())
            );
        }
    }

    /**
     * Validate child budget sum when creating a parent budget.
     * Ensures sum of existing child budgets equals the parent budget being created.
     */
    private void validateChildBudgetSumOnCreate(Long parentCategoryId, Integer year, Integer month, BigDecimal parentAmount) {
        // Calculate sum of existing child budgets
        BigDecimal childSum = budgetRepository.sumByParentCategoryAndPeriod(parentCategoryId, year, month);

        // If there are child budgets, validate they equal parent
        if (childSum != null && childSum.compareTo(BigDecimal.ZERO) > 0) {
            if (childSum.compareTo(parentAmount) != 0) {
                throw new ParentBudgetMismatchException(
                        String.format("Parent budget amount (%.2f) must equal sum of existing child budgets (%.2f)",
                                parentAmount, childSum)
                );
            }
        }
    }

    /**
     * Validate child budget sum when updating a parent budget amount.
     * Ensures sum of child budgets equals the new parent budget amount.
     */
    private void validateChildBudgetSumOnUpdate(Long budgetId, Long parentCategoryId, Integer year, Integer month,
                                                BigDecimal newParentAmount) {
        // Calculate sum of child budgets
        BigDecimal childSum = budgetRepository.sumByParentCategoryAndPeriod(parentCategoryId, year, month);

        // If there are child budgets, validate they equal new parent amount
        if (childSum != null && childSum.compareTo(BigDecimal.ZERO) > 0) {
            if (childSum.compareTo(newParentAmount) != 0) {
                throw new ParentBudgetMismatchException(
                        String.format("Cannot update parent budget to %.2f: sum of child budgets is %.2f",
                                newParentAmount, childSum)
                );
            }
        }
    }
}
