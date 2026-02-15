package com.homebudget.dto;

import java.util.List;

public class OcrResponseDTO {

    private List<OcrExpenseDTO> expenses;

    public List<OcrExpenseDTO> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<OcrExpenseDTO> expenses) {
        this.expenses = expenses;
    }
}
