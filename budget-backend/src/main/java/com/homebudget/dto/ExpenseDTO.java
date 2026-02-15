package com.homebudget.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Expense entity.
 * Used for API requests and responses.
 */
public class ExpenseDTO {

    private Long id;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private CategoryDTO category;

    // Convenience fields for category display (populated from Category entity)
    private String categoryName;
    private String categoryIcon;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long version;

    private List<ExpenseFileDTO> files = new ArrayList<>();

    private boolean commonExpense;

    // Constructors
    public ExpenseDTO() {
    }

    public ExpenseDTO(Long id, BigDecimal amount, String description, LocalDate expenseDate,
                      Long categoryId, String createdBy, LocalDateTime createdAt,
                      LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.categoryId = categoryId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
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

    public List<ExpenseFileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<ExpenseFileDTO> files) {
        this.files = files;
    }

    public boolean isCommonExpense() {
        return commonExpense;
    }

    public void setCommonExpense(boolean commonExpense) {
        this.commonExpense = commonExpense;
    }
}
