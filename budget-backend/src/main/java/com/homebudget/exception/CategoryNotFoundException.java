package com.homebudget.exception;

/**
 * Exception thrown when a category is not found.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long categoryId) {
        super(String.format("Category not found with ID: %d", categoryId));
    }

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
