package com.homebudget.dto;

import java.math.BigDecimal;

/**
 * DTO for budget validation hints used by the UI.
 */
public class BudgetValidationDTO {

    private boolean duplicate;
    private String duplicateMessage;
    private boolean parentBudgetExists;
    private Long parentBudgetId;
    private BigDecimal parentBudgetAmount;
    private BigDecimal monthlyBudgetSum;
    private boolean monthlyBudgetsExist;

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getDuplicateMessage() {
        return duplicateMessage;
    }

    public void setDuplicateMessage(String duplicateMessage) {
        this.duplicateMessage = duplicateMessage;
    }

    public boolean isParentBudgetExists() {
        return parentBudgetExists;
    }

    public void setParentBudgetExists(boolean parentBudgetExists) {
        this.parentBudgetExists = parentBudgetExists;
    }

    public Long getParentBudgetId() {
        return parentBudgetId;
    }

    public void setParentBudgetId(Long parentBudgetId) {
        this.parentBudgetId = parentBudgetId;
    }

    public BigDecimal getParentBudgetAmount() {
        return parentBudgetAmount;
    }

    public void setParentBudgetAmount(BigDecimal parentBudgetAmount) {
        this.parentBudgetAmount = parentBudgetAmount;
    }

    public BigDecimal getMonthlyBudgetSum() {
        return monthlyBudgetSum;
    }

    public void setMonthlyBudgetSum(BigDecimal monthlyBudgetSum) {
        this.monthlyBudgetSum = monthlyBudgetSum;
    }

    public boolean isMonthlyBudgetsExist() {
        return monthlyBudgetsExist;
    }

    public void setMonthlyBudgetsExist(boolean monthlyBudgetsExist) {
        this.monthlyBudgetsExist = monthlyBudgetsExist;
    }
}
