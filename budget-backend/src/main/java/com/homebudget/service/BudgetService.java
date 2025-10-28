package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.BudgetNotFoundException;
import com.homebudget.exception.DuplicateBudgetException;
import com.homebudget.model.Budget;
import com.homebudget.model.Expense;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public BudgetService(BudgetRepository budgetRepository, ExpenseRepository expenseRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Create a new budget.
     * Validates that no budget exists for the same month.
     *
     * @param dto budget data
     * @param username user from X-Hass-User header
     * @return created budget as DTO
     * @throws DuplicateBudgetException if budget already exists for the month
     */
    public BudgetDTO createBudget(BudgetDTO dto, String username) {
        // Check for duplicate budget (year + month uniqueness)
        if (budgetRepository.existsByYearAndMonth(dto.getYear(), dto.getMonth())) {
            throw new DuplicateBudgetException(dto.getYear(), dto.getMonth());
        }

        Budget budget = new Budget();
        budget.setYear(dto.getYear());
        budget.setMonth(dto.getMonth());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setDescription(dto.getDescription());
        budget.setCreatedBy(username);

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
     * Update budget (amount and description only, year/month immutable).
     *
     * @param id budget ID
     * @param dto updated budget data
     * @return updated budget
     * @throws BudgetNotFoundException if budget not found
     */
    public BudgetDTO updateBudget(Long id, BudgetDTO dto) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));

        // Only allow updating amount and description (year/month are immutable)
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
        return new BudgetDTO(
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
}
