package com.homebudget.dto;

import java.math.BigDecimal;

public class CategoryStatsDTO {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private Long expenseCount;
    private BigDecimal averageAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal totalAmount;

    public CategoryStatsDTO() {}

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public Long getExpenseCount() { return expenseCount; }
    public void setExpenseCount(Long expenseCount) { this.expenseCount = expenseCount; }

    public BigDecimal getAverageAmount() { return averageAmount; }
    public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
