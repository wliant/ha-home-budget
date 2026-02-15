package com.homebudget.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExpenseInputJobDTO {
    private Long id;
    private String status;
    private Integer retryCount;
    private String originalFilename;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TemporaryExpenseRecordDTO> temporaryRecords;

    public ExpenseInputJobDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public List<TemporaryExpenseRecordDTO> getTemporaryRecords() {
        return temporaryRecords;
    }

    public void setTemporaryRecords(List<TemporaryExpenseRecordDTO> temporaryRecords) {
        this.temporaryRecords = temporaryRecords;
    }
}
