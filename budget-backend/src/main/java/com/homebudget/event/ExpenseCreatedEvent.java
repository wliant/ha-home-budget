package com.homebudget.event;

import com.homebudget.dto.ExpenseDTO;

public class ExpenseCreatedEvent {

    private final ExpenseDTO expense;

    public ExpenseCreatedEvent(ExpenseDTO expense) {
        this.expense = expense;
    }

    public ExpenseDTO getExpense() {
        return expense;
    }
}
