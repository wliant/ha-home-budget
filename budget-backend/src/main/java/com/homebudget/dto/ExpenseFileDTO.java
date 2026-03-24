package com.homebudget.dto;

public class ExpenseFileDTO {
    private Long id;
    private String originalFilename;
    private String filePath;

    public ExpenseFileDTO() {}

    public ExpenseFileDTO(Long id, String originalFilename, String filePath) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
