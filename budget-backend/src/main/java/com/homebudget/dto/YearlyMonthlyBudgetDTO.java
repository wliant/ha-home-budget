package com.homebudget.dto;

import java.math.BigDecimal;

/**
 * DTO for monthly budget entries within a yearly view.
 */
public class YearlyMonthlyBudgetDTO {

    private Integer month;
    private BigDecimal budgetAmount;
    private BigDecimal spending;
    private BigDecimal remaining;
    private boolean hasBudget;

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getSpending() {
        return spending;
    }

    public void setSpending(BigDecimal spending) {
        this.spending = spending;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public boolean isHasBudget() {
        return hasBudget;
    }

    public void setHasBudget(boolean hasBudget) {
        this.hasBudget = hasBudget;
    }
}
