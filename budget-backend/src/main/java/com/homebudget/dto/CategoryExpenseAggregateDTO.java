package com.homebudget.dto;

import java.math.BigDecimal;

/**
 * DTO for category-level expense aggregation.
 * Represents monthly or yearly spending totals per category with parent rollup.
 */
public class CategoryExpenseAggregateDTO {

    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private Long parentCategoryId;
    private BigDecimal directAmount;
    private BigDecimal childrenAmount;
    private BigDecimal totalAmount;
    private Integer year;
    private Integer month; // null for yearly aggregates
    private Integer day; // null for monthly/yearly aggregates

    public CategoryExpenseAggregateDTO() {
    }

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

    public BigDecimal getDirectAmount() {
        return directAmount;
    }

    public void setDirectAmount(BigDecimal directAmount) {
        this.directAmount = directAmount;
    }

    public BigDecimal getChildrenAmount() {
        return childrenAmount;
    }

    public void setChildrenAmount(BigDecimal childrenAmount) {
        this.childrenAmount = childrenAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }
}
