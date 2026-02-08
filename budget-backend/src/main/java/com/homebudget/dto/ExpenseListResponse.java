package com.homebudget.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response wrapper for the paginated expense list endpoint.
 * Contains the current page of expenses plus aggregate summary data.
 */
public class ExpenseListResponse {

    private List<ExpenseDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private BigDecimal totalAmount;
    private String sortBy;
    private String sortDirection;

    public ExpenseListResponse() {
    }

    public ExpenseListResponse(List<ExpenseDTO> content, long totalElements, int totalPages,
                                int currentPage, int pageSize, BigDecimal totalAmount,
                                String sortBy, String sortDirection) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalAmount = totalAmount;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    public List<ExpenseDTO> getContent() {
        return content;
    }

    public void setContent(List<ExpenseDTO> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
