package com.homebudget.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for yearly budget summary by category.
 */
public class YearlyCategoryBudgetDTO {

    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private Long parentCategoryId;
    private String parentCategoryName;
    private BigDecimal yearlyBudgetAmount;
    private BigDecimal monthlyBudgetSum;
    private BigDecimal yearlySpending;
    private BigDecimal yearlyRemaining;
    private List<YearlyMonthlyBudgetDTO> months = new ArrayList<>();

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public void setParentCategoryName(String parentCategoryName) {
        this.parentCategoryName = parentCategoryName;
    }

    public BigDecimal getYearlyBudgetAmount() {
        return yearlyBudgetAmount;
    }

    public void setYearlyBudgetAmount(BigDecimal yearlyBudgetAmount) {
        this.yearlyBudgetAmount = yearlyBudgetAmount;
    }

    public BigDecimal getMonthlyBudgetSum() {
        return monthlyBudgetSum;
    }

    public void setMonthlyBudgetSum(BigDecimal monthlyBudgetSum) {
        this.monthlyBudgetSum = monthlyBudgetSum;
    }

    public BigDecimal getYearlySpending() {
        return yearlySpending;
    }

    public void setYearlySpending(BigDecimal yearlySpending) {
        this.yearlySpending = yearlySpending;
    }

    public BigDecimal getYearlyRemaining() {
        return yearlyRemaining;
    }

    public void setYearlyRemaining(BigDecimal yearlyRemaining) {
        this.yearlyRemaining = yearlyRemaining;
    }

    public List<YearlyMonthlyBudgetDTO> getMonths() {
        return months;
    }

    public void setMonths(List<YearlyMonthlyBudgetDTO> months) {
        this.months = months;
    }
}
