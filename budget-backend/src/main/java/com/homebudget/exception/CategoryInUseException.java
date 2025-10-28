package com.homebudget.exception;

/**
 * Exception thrown when attempting to delete a category that has associated expenses.
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(Long categoryId, Long expenseCount) {
        super(String.format("Cannot delete category (ID: %d) with %d associated expenses. " +
                "Please reassign expenses to another category first.", categoryId, expenseCount));
    }

    public CategoryInUseException(String categoryName, int expenseCount) {
        super(String.format("Cannot delete category '%s' with %d associated expenses. " +
                "Please reassign expenses to another category first.", categoryName, expenseCount));
    }

    public CategoryInUseException(String message) {
        super(message);
    }
}
