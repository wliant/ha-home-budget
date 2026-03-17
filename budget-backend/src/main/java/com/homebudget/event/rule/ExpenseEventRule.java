package com.homebudget.event.rule;

import com.homebudget.dto.ExpenseDTO;

public interface ExpenseEventRule {

    boolean shouldEmit(ExpenseDTO expense);

    String ruleName();
}
