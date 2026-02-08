package com.homebudget.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Budget summary view with spending calculations.
 * Used for budget list views and detail views with spending statistics.
 */
public class BudgetSummaryDTO {

    private Long id;
    private Integer year;
    private Integer month;
    private BigDecimal totalAmount;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    // Calculated fields
    private BigDecimal totalSpending;
    private BigDecimal spendingPercentage;
    private Long expenseCount;

    // Category context (for filtering and display)
    private Long categoryId;
    private CategoryDTO category;

    // Parent category aggregation
    private BigDecimal childrenBudgetSum;
    private BigDecimal childrenSpending;
    private Boolean isParentCategory;

    // For detail view
    private List<ExpenseDTO> expenses = new ArrayList<>();

    // Constructors
    public BudgetSummaryDTO() {
    }

    public BudgetSummaryDTO(Long id, Integer year, Integer month, BigDecimal totalAmount,
                            String description, String createdBy, LocalDateTime createdAt,
                            LocalDateTime updatedAt, Long version, BigDecimal totalSpending,
                            BigDecimal spendingPercentage, Long expenseCount) {
        this.id = id;
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.totalSpending = totalSpending;
        this.spendingPercentage = spendingPercentage;
        this.expenseCount = expenseCount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(BigDecimal totalSpending) {
        this.totalSpending = totalSpending;
    }

    public BigDecimal getSpendingPercentage() {
        return spendingPercentage;
    }

    public void setSpendingPercentage(BigDecimal spendingPercentage) {
        this.spendingPercentage = spendingPercentage;
    }

    public Long getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(Long expenseCount) {
        this.expenseCount = expenseCount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public CategoryDTO getCategory() {
        return category;
    }

    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    public List<ExpenseDTO> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseDTO> expenses) {
        this.expenses = expenses;
    }

    public BigDecimal getChildrenBudgetSum() {
        return childrenBudgetSum;
    }

    public void setChildrenBudgetSum(BigDecimal childrenBudgetSum) {
        this.childrenBudgetSum = childrenBudgetSum;
    }

    public BigDecimal getChildrenSpending() {
        return childrenSpending;
    }

    public void setChildrenSpending(BigDecimal childrenSpending) {
        this.childrenSpending = childrenSpending;
    }

    public Boolean getIsParentCategory() {
        return isParentCategory;
    }

    public void setIsParentCategory(Boolean isParentCategory) {
        this.isParentCategory = isParentCategory;
    }
}
