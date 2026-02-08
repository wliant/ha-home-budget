package com.homebudget.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for yearly budget view.
 */
public class YearlyBudgetViewDTO {

    private Integer year;
    private BigDecimal totalBudget;
    private BigDecimal totalSpending;
    private BigDecimal totalRemaining;
    private List<YearlyCategoryBudgetDTO> categories = new ArrayList<>();

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(BigDecimal totalSpending) {
        this.totalSpending = totalSpending;
    }

    public BigDecimal getTotalRemaining() {
        return totalRemaining;
    }

    public void setTotalRemaining(BigDecimal totalRemaining) {
        this.totalRemaining = totalRemaining;
    }

    public List<YearlyCategoryBudgetDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<YearlyCategoryBudgetDTO> categories) {
        this.categories = categories;
    }
}
