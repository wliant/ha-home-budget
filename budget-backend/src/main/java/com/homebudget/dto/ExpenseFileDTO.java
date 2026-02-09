package com.homebudget.dto;

public class ExpenseFileDTO {
    private Long id;
    private String originalFilename;

    public ExpenseFileDTO() {}

    public ExpenseFileDTO(Long id, String originalFilename) {
        this.id = id;
        this.originalFilename = originalFilename;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
}
