package com.homebudget.service;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseListResponse;
import com.homebudget.exception.BudgetNotFoundException;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing expenses.
 *
 * Implements User Story 2: Record Expenses Against Budgets
 * - Create, read, update, delete expenses
 * - Associate expenses with budgets
 * - Track expense dates and categories
 * - Validate expense dates against budget months
 */
@Service
@Transactional
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Create a new expense.
     *
     * @param dto Expense data
     * @param username User creating the expense (from X-Hass-User header)
     * @return Created expense DTO with date mismatch warning if applicable
     * @throws BudgetNotFoundException if budget not found
     * @throws CategoryNotFoundException if category specified but not found
     */
    public ExpenseDTO createExpense(ExpenseDTO dto, String username) {
        logger.info("Creating expense for budget ID: {}, user: {}", dto.getBudgetId(), username);

        // Validate budget exists
        Budget budget = budgetRepository.findById(dto.getBudgetId())
                .orElseThrow(() -> new BudgetNotFoundException(dto.getBudgetId()));

        logger.debug("Budget attribution: selected budgetId={}, period={}-{}, amount={}",
                budget.getId(), budget.getYear(), budget.getMonth(), budget.getTotalAmount());

        // Create expense entity
        Expense expense = new Expense();
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setBudget(budget);
        expense.setCreatedBy(username);

        logger.debug("Expense details: amount={}, date={}, description='{}'",
                dto.getAmount(), dto.getExpenseDate(), dto.getDescription());

        // Set category if provided
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));
            expense.setCategory(category);
            logger.debug("Category assigned: ID={}, name='{}'", category.getId(), category.getName());
        }

        // Save expense
        Expense saved = expenseRepository.save(expense);
        logger.info("Created expense ID: {} for budget ID: {}", saved.getId(), budget.getId());

        // Convert to DTO with date mismatch check
        ExpenseDTO result = toDTO(saved);
        checkDateMismatch(result, budget);

        return result;
    }

    /**
     * Get all expenses with optional filtering.
     *
     * @param budgetId Filter by budget ID (optional)
     * @param categoryId Filter by category ID (optional)
     * @param startDate Filter by date range start (optional)
     * @param endDate Filter by date range end (optional)
     * @param createdBy Filter by user who created (optional)
     * @return List of expenses matching filters
     */
    @Transactional(readOnly = true)
    public List<ExpenseDTO> getAllExpenses(Long budgetId, Long categoryId,
                                          LocalDate startDate, LocalDate endDate,
                                          String createdBy) {
        logger.info("Finding expenses with filters - budgetId: {}, categoryId: {}, dateRange: {}-{}, createdBy: {}",
                   budgetId, categoryId, startDate, endDate, createdBy);

        List<Expense> expenses;

        // Apply filters based on what's provided
        if (budgetId != null && categoryId != null && startDate != null && endDate != null && createdBy != null) {
            expenses = expenseRepository.findByBudgetIdAndCategoryIdAndExpenseDateBetweenAndCreatedBy(
                    budgetId, categoryId, startDate, endDate, createdBy);
        } else if (budgetId != null && startDate != null && endDate != null) {
            expenses = expenseRepository.findByBudgetIdAndExpenseDateBetween(budgetId, startDate, endDate);
        } else if (budgetId != null && categoryId != null) {
            expenses = expenseRepository.findByBudgetIdAndCategoryId(budgetId, categoryId);
        } else if (budgetId != null && createdBy != null) {
            expenses = expenseRepository.findByBudgetIdAndCreatedBy(budgetId, createdBy);
        } else if (budgetId != null) {
            expenses = expenseRepository.findByBudgetId(budgetId);
        } else if (categoryId != null) {
            expenses = expenseRepository.findByCategoryId(categoryId);
        } else if (startDate != null && endDate != null) {
            expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate);
        } else if (createdBy != null) {
            expenses = expenseRepository.findByCreatedBy(createdBy);
        } else {
            expenses = expenseRepository.findAllOrderByExpenseDateDesc();
        }

        logger.info("Found {} expenses", expenses.size());

        return expenses.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get expense by ID.
     *
     * @param id Expense ID
     * @return Expense DTO
     * @throws ExpenseNotFoundException if expense not found
     */
    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long id) {
        logger.info("Finding expense by ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        return toDTO(expense);
    }

    /**
     * Update an existing expense.
     *
     * @param id Expense ID
     * @param dto Updated expense data
     * @return Updated expense DTO
     * @throws ExpenseNotFoundException if expense not found
     * @throws BudgetNotFoundException if new budget not found
     * @throws CategoryNotFoundException if new category not found
     */
    public ExpenseDTO updateExpense(Long id, ExpenseDTO dto) {
        logger.info("Updating expense ID: {}", id);

        // Find existing expense
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        // Update fields
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());

        // Update budget if changed
        if (!expense.getBudget().getId().equals(dto.getBudgetId())) {
            Budget newBudget = budgetRepository.findById(dto.getBudgetId())
                    .orElseThrow(() -> new BudgetNotFoundException(dto.getBudgetId()));
            expense.setBudget(newBudget);
        }

        // Update category if changed
        if (dto.getCategoryId() != null) {
            if (expense.getCategory() == null || !expense.getCategory().getId().equals(dto.getCategoryId())) {
                Category newCategory = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));
                expense.setCategory(newCategory);
            }
        } else {
            expense.setCategory(null);
        }

        // Save updated expense
        Expense updated = expenseRepository.save(expense);
        logger.info("Updated expense ID: {}", id);

        // Convert to DTO with date mismatch check
        ExpenseDTO result = toDTO(updated);
        checkDateMismatch(result, updated.getBudget());

        return result;
    }

    /**
     * Delete an expense.
     *
     * @param id Expense ID
     * @throws ExpenseNotFoundException if expense not found
     */
    public void deleteExpense(Long id) {
        logger.info("Deleting expense ID: {}", id);

        if (!expenseRepository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }

        expenseRepository.deleteById(id);
        logger.info("Deleted expense ID: {}", id);
    }

    /**
     * Get paginated, filtered, sorted expense list with aggregate summary.
     * Converts year/month to date range and delegates to repository.
     *
     * @param year required year filter
     * @param month optional month filter (1-12)
     * @param categoryId optional category filter
     * @param minAmount optional minimum amount filter (inclusive)
     * @param maxAmount optional maximum amount filter (inclusive)
     * @param createdBy optional creator filter
     * @param pageable pagination and sorting parameters
     * @param sortBy sort field name for response metadata
     * @param sortDirection sort direction for response metadata
     * @return ExpenseListResponse with paginated content and summary
     */
    @Transactional(readOnly = true)
    public ExpenseListResponse getExpenseList(int year, Integer month, Long categoryId,
                                               BigDecimal minAmount, BigDecimal maxAmount,
                                               String createdBy, Pageable pageable,
                                               String sortBy, String sortDirection) {
        logger.info("Getting expense list - year: {}, month: {}, categoryId: {}, amountRange: {}-{}, createdBy: {}",
                year, month, categoryId, minAmount, maxAmount, createdBy);

        // Convert year/month to date range
        LocalDate startDate;
        LocalDate endDate;
        if (month != null) {
            YearMonth ym = YearMonth.of(year, month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        logger.debug("Date range: {} to {}", startDate, endDate);

        // Get paginated results
        Page<Expense> page = expenseRepository.findByFiltersPageable(
                categoryId, startDate, endDate, minAmount, maxAmount, createdBy, pageable);

        // Get aggregate total amount for all matching expenses
        BigDecimal totalAmount = expenseRepository.getFilteredTotalAmount(
                categoryId, startDate, endDate, minAmount, maxAmount, createdBy);

        // Convert to DTOs
        List<ExpenseDTO> content = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        logger.info("Found {} expenses (page {} of {}), total amount: {}",
                page.getTotalElements(), page.getNumber(), page.getTotalPages(), totalAmount);

        return new ExpenseListResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                totalAmount,
                sortBy,
                sortDirection
        );
    }

    /**
     * Get distinct years that have expense data, sorted descending.
     *
     * @return list of years with expenses
     */
    @Transactional(readOnly = true)
    public List<Integer> getDistinctYears() {
        logger.info("Getting distinct expense years");
        List<Integer> years = expenseRepository.findDistinctYears();
        logger.info("Found {} distinct years", years.size());
        return years;
    }

    /**
     * Get distinct expense creators, sorted ascending.
     *
     * @return list of creator usernames
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCreators() {
        logger.info("Getting distinct expense creators");
        List<String> creators = expenseRepository.findDistinctCreators();
        logger.info("Found {} distinct creators", creators.size());
        return creators;
    }

    /**
     * Convert Expense entity to DTO.
     */
    private ExpenseDTO toDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setBudgetId(expense.getBudget().getId());
        dto.setCreatedBy(expense.getCreatedBy());
        dto.setCreatedAt(expense.getCreatedAt());
        dto.setUpdatedAt(expense.getUpdatedAt());
        dto.setVersion(expense.getVersion());

        // Include category if present
        if (expense.getCategory() != null) {
            dto.setCategoryId(expense.getCategory().getId());
            dto.setCategoryName(expense.getCategory().getName());
            dto.setCategoryIcon(expense.getCategory().getIcon());
        }

        return dto;
    }

    /**
     * Check if expense date falls outside budget month and set warning.
     *
     * Implements FR-018: Warn users when expense dates don't fall within budget's month
     */
    private void checkDateMismatch(ExpenseDTO dto, Budget budget) {
        if (budget.getMonth() == null) {
            return;
        }
        YearMonth budgetMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        YearMonth expenseMonth = YearMonth.from(dto.getExpenseDate());

        logger.debug("Checking date match: expense date={} ({}), budget period={} ({})",
                dto.getExpenseDate(), expenseMonth, budgetMonth, budgetMonth);

        if (!budgetMonth.equals(expenseMonth)) {
            String warning = String.format(
                "Warning: Expense date %s does not fall within budget month %s",
                dto.getExpenseDate(),
                budgetMonth
            );
            dto.setDateMismatchWarning(warning);
            logger.warn("Date mismatch for expense ID {}: {}", dto.getId(), warning);
        } else {
            logger.debug("Date match confirmed: expense falls within budget period");
        }
    }
}
