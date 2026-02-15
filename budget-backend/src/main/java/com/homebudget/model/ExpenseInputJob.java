package com.homebudget.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_input_jobs")
public class ExpenseInputJob {

    public enum Status {
        INIT,           // Initial state when job is created
        PROCESSING,     // Currently being processed
        RETRYABLE,      // Processing failed but can retry
        COMPLETED,      // Successfully processed
        FAILED          // Failed after max retries or non-retryable error
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Status status = Status.INIT;

    @NotNull
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @NotBlank
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @NotBlank
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<TemporaryExpenseRecord> temporaryRecords = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public java.util.List<TemporaryExpenseRecord> getTemporaryRecords() {
        return temporaryRecords;
    }

    public void setTemporaryRecords(java.util.List<TemporaryExpenseRecord> temporaryRecords) {
        this.temporaryRecords = temporaryRecords;
    }
}
