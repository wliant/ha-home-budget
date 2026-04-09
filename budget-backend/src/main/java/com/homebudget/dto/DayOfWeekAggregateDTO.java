package com.homebudget.dto;

import java.math.BigDecimal;

public class DayOfWeekAggregateDTO {
    private Integer dayOfWeek;
    private String dayName;
    private BigDecimal totalAmount;
    private Long expenseCount;
    private BigDecimal averageAmount;

    public DayOfWeekAggregateDTO() {}

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getExpenseCount() { return expenseCount; }
    public void setExpenseCount(Long expenseCount) { this.expenseCount = expenseCount; }

    public BigDecimal getAverageAmount() { return averageAmount; }
    public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }
}
