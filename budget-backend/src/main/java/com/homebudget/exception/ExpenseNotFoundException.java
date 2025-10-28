package com.homebudget.exception;

/**
 * Exception thrown when an expense is not found.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Long expenseId) {
        super(String.format("Expense not found with ID: %d", expenseId));
    }

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
