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

    // Parent category budget fields (category hierarchy)
    private boolean parentCategoryBudgetExists;
    private Long parentCategoryBudgetId;
    private BigDecimal parentCategoryBudgetAmount;
    private String parentCategoryName;

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

    public boolean isParentCategoryBudgetExists() {
        return parentCategoryBudgetExists;
    }

    public void setParentCategoryBudgetExists(boolean parentCategoryBudgetExists) {
        this.parentCategoryBudgetExists = parentCategoryBudgetExists;
    }

    public Long getParentCategoryBudgetId() {
        return parentCategoryBudgetId;
    }

    public void setParentCategoryBudgetId(Long parentCategoryBudgetId) {
        this.parentCategoryBudgetId = parentCategoryBudgetId;
    }

    public BigDecimal getParentCategoryBudgetAmount() {
        return parentCategoryBudgetAmount;
    }

    public void setParentCategoryBudgetAmount(BigDecimal parentCategoryBudgetAmount) {
        this.parentCategoryBudgetAmount = parentCategoryBudgetAmount;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public void setParentCategoryName(String parentCategoryName) {
        this.parentCategoryName = parentCategoryName;
    }
}
