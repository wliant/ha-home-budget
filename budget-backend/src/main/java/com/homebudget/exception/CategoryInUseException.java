package com.homebudget.exception;

/**
 * Exception thrown when attempting to delete or modify a category that has associated data.
 * This includes expenses, budgets, or child categories.
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

    public CategoryInUseException(String categoryName, int childCount, String type) {
        super(buildMessage(categoryName, childCount, type));
    }

    private static String buildMessage(String categoryName, int childCount, String type) {
        if ("children".equals(type)) {
            return String.format("Cannot delete category '%s': has %d child categories. " +
                    "Please reassign or delete children first.", categoryName, childCount);
        } else if ("budgets".equals(type)) {
            return String.format("Cannot delete category '%s': has %d associated budgets. " +
                    "Please reassign budgets to another category first.", categoryName, childCount);
        } else {
            return String.format("Cannot delete category '%s': has %d associated %s.",
                    categoryName, childCount, type);
        }
    }

    public CategoryInUseException(String message) {
        super(message);
    }
}
