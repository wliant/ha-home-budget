package com.homebudget.exception;

/**
 * Exception thrown when attempting to create a circular reference in the category hierarchy.
 */
public class CircularCategoryException extends RuntimeException {

    public CircularCategoryException(Long categoryId, Long newParentId) {
        super(String.format("Cannot set parent: circular reference detected. " +
                "Category with ID %d cannot be a parent of category with ID %d " +
                "because it would create a cycle in the hierarchy.",
                newParentId, categoryId));
    }

    public CircularCategoryException(String categoryName, String newParentName) {
        super(String.format("Cannot set parent: circular reference detected. " +
                "Category '%s' cannot be a parent of '%s' " +
                "because '%s' is an ancestor of '%s'.",
                newParentName, categoryName, categoryName, newParentName));
    }

    public CircularCategoryException(String message) {
        super(message);
    }
}
