package com.homebudget.exception;

/**
 * Exception thrown when a budget is not found by ID.
 */
public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(Long id) {
        super("Budget not found with id: " + id);
    }

    public BudgetNotFoundException(String message) {
        super(message);
    }
}
