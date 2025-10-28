package com.homebudget.exception;

/**
 * Exception thrown when attempting to create a budget for a month that already has a budget.
 */
public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException(Integer year, Integer month) {
        super(String.format("Budget already exists for %d-%02d", year, month));
    }

    public DuplicateBudgetException(String message) {
        super(message);
    }
}
