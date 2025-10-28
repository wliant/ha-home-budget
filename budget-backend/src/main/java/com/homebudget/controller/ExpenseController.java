package com.homebudget.controller;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API Controller for expense operations.
 *
 * Implements User Story 2: Record Expenses Against Budgets
 *
 * Endpoints:
 * - POST /api/expenses - Create new expense
 * - GET /api/expenses - List all expenses with optional filters
 * - GET /api/expenses/{id} - Get expense by ID
 * - PUT /api/expenses/{id} - Update expense
 * - DELETE /api/expenses/{id} - Delete expense
 *
 * Uses X-Hass-User header for user identification (provided by Home Assistant)
 */
@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";

    @Autowired
    private ExpenseService expenseService;

    /**
     * Create a new expense.
     *
     * @param dto Expense data (budgetId, amount, description, expenseDate, optional categoryId)
     * @param username User creating expense (from X-Hass-User header)
     * @return Created expense with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ExpenseDTO> createExpense(
            @Valid @RequestBody ExpenseDTO dto,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expenses - Creating expense for budget {}, user: {}", dto.getBudgetId(), username);

        ExpenseDTO created = expenseService.createExpense(dto, username);

        logger.info("Created expense ID: {} for budget ID: {}", created.getId(), created.getBudgetId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all expenses with optional filtering.
     *
     * Query parameters (all optional):
     * - budgetId: Filter by budget
     * - categoryId: Filter by category
     * - startDate: Filter by date range start (YYYY-MM-DD)
     * - endDate: Filter by date range end (YYYY-MM-DD)
     * - createdBy: Filter by user who created
     *
     * @return List of expenses matching filters
     */
    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getAllExpenses(
            @RequestParam(required = false) Long budgetId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String createdBy) {

        logger.info("GET /api/expenses - Filters: budgetId={}, categoryId={}, dateRange={}-{}, createdBy={}",
                   budgetId, categoryId, startDate, endDate, createdBy);

        List<ExpenseDTO> expenses = expenseService.getAllExpenses(
                budgetId, categoryId, startDate, endDate, createdBy);

        logger.info("Found {} expenses", expenses.size());
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get expense by ID.
     *
     * @param id Expense ID
     * @return Expense details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        logger.info("GET /api/expenses/{} - Finding expense", id);

        ExpenseDTO expense = expenseService.getExpenseById(id);

        return ResponseEntity.ok(expense);
    }

    /**
     * Update an existing expense.
     *
     * @param id Expense ID
     * @param dto Updated expense data
     * @return Updated expense
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDTO dto) {

        logger.info("PUT /api/expenses/{} - Updating expense", id);

        ExpenseDTO updated = expenseService.updateExpense(id, dto);

        logger.info("Updated expense ID: {}", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an expense.
     *
     * @param id Expense ID
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        logger.info("DELETE /api/expenses/{} - Deleting expense", id);

        expenseService.deleteExpense(id);

        logger.info("Deleted expense ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
