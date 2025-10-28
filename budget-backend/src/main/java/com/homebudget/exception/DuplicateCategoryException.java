package com.homebudget.exception;

/**
 * Exception thrown when attempting to create a category with a duplicate name.
 */
public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String categoryName) {
        super(String.format("Category with name '%s' already exists", categoryName));
    }

    public DuplicateCategoryException(String message, boolean useMessageDirectly) {
        super(message);
    }
}
