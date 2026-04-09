package com.homebudget.dto;

import java.math.BigDecimal;

public class UserSpendingAggregateDTO {
    private String username;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private BigDecimal amount;
    private BigDecimal totalUserSpending;

    public UserSpendingAggregateDTO() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getTotalUserSpending() { return totalUserSpending; }
    public void setTotalUserSpending(BigDecimal totalUserSpending) { this.totalUserSpending = totalUserSpending; }
}
