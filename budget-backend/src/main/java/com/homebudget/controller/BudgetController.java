package com.homebudget.controller;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Budget operations.
 * Handles HTTP requests for budget management.
 */
@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    private static final String HASS_USER_HEADER = "X-Hass-User";

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Create a new budget.
     *
     * @param dto budget data
     * @param username user from X-Hass-User header
     * @return created budget with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<BudgetDTO> createBudget(
            @Valid @RequestBody BudgetDTO dto,
            @RequestHeader(HASS_USER_HEADER) String username) {

        BudgetDTO created = budgetService.createBudget(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all budgets with spending summaries.
     * Returns budgets ordered by date (newest first).
     *
     * @return list of budget summaries
     */
    @GetMapping
    public ResponseEntity<List<BudgetSummaryDTO>> getAllBudgets() {
        List<BudgetSummaryDTO> budgets = budgetService.getAllBudgets();
        return ResponseEntity.ok(budgets);
    }

    /**
     * Get budget by ID with expenses.
     *
     * @param id budget ID
     * @return budget summary with expenses list
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetSummaryDTO> getBudgetById(@PathVariable Long id) {
        BudgetSummaryDTO budget = budgetService.getBudgetById(id);
        return ResponseEntity.ok(budget);
    }

    /**
     * Update budget (amount and description only).
     * Year and month are immutable.
     *
     * @param id budget ID
     * @param dto updated budget data
     * @return updated budget
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetDTO> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetDTO dto) {

        BudgetDTO updated = budgetService.updateBudget(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete budget.
     * Cascade deletes all associated expenses.
     *
     * @param id budget ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
