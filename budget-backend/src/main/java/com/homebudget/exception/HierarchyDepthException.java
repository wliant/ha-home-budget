package com.homebudget.exception;

/**
 * Exception thrown when attempting to create a category hierarchy deeper than the allowed maximum.
 */
public class HierarchyDepthException extends RuntimeException {

    public HierarchyDepthException() {
        super("Cannot create child category: maximum hierarchy depth is 2 levels (parent and child only).");
    }

    public HierarchyDepthException(String categoryName, String parentName) {
        super(String.format("Cannot create child category: parent '%s' is already a child category. " +
                "Maximum hierarchy depth is 2 levels.", parentName));
    }

    public HierarchyDepthException(String message) {
        super(message);
    }
}
