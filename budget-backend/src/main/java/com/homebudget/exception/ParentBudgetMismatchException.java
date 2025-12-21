package com.homebudget.exception;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Exception thrown when a parent budget amount doesn't equal the sum of child budgets.
 */
public class ParentBudgetMismatchException extends RuntimeException {

    private final BigDecimal parentAmount;
    private final BigDecimal requiredAmount;
    private final Map<String, Object> details;

    public ParentBudgetMismatchException(BigDecimal parentAmount, BigDecimal requiredAmount) {
        super(String.format("Parent budget amount (%.2f) must equal sum of child budgets (%.2f)",
                parentAmount, requiredAmount));
        this.parentAmount = parentAmount;
        this.requiredAmount = requiredAmount;
        this.details = null;
    }

    public ParentBudgetMismatchException(BigDecimal parentAmount, BigDecimal requiredAmount,
                                          Map<String, Object> details) {
        super(String.format("Parent budget amount (%.2f) must equal sum of child budgets (%.2f)",
                parentAmount, requiredAmount));
        this.parentAmount = parentAmount;
        this.requiredAmount = requiredAmount;
        this.details = details;
    }

    public ParentBudgetMismatchException(String message) {
        super(message);
        this.parentAmount = null;
        this.requiredAmount = null;
        this.details = null;
    }

    public BigDecimal getParentAmount() {
        return parentAmount;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
