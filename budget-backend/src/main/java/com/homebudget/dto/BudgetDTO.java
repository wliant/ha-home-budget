package com.homebudget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Budget entity.
 * Used for API requests and responses.
 */
public class BudgetDTO {

    private Long id;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 9999, message = "Year must be 9999 or earlier")
    private Integer year;

    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Total amount is required")
    @Min(value = 0, message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private String description;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long version;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private CategoryDTO category;

    // Creation options (optional)
    private Boolean autoCreateChildren;
    private Boolean createParentBudget;
    private Boolean extendParentBudget;
    private BigDecimal parentTotalAmount;

    // Constructors
    public BudgetDTO() {
    }

    public BudgetDTO(Long id, Integer year, Integer month, BigDecimal totalAmount, String description,
                     String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.year = year;
        this.month = month;
        this.totalAmount = totalAmount;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
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

    public Boolean getAutoCreateChildren() {
        return autoCreateChildren;
    }

    public void setAutoCreateChildren(Boolean autoCreateChildren) {
        this.autoCreateChildren = autoCreateChildren;
    }

    public Boolean getCreateParentBudget() {
        return createParentBudget;
    }

    public void setCreateParentBudget(Boolean createParentBudget) {
        this.createParentBudget = createParentBudget;
    }

    public Boolean getExtendParentBudget() {
        return extendParentBudget;
    }

    public void setExtendParentBudget(Boolean extendParentBudget) {
        this.extendParentBudget = extendParentBudget;
    }

    public BigDecimal getParentTotalAmount() {
        return parentTotalAmount;
    }

    public void setParentTotalAmount(BigDecimal parentTotalAmount) {
        this.parentTotalAmount = parentTotalAmount;
    }
}
