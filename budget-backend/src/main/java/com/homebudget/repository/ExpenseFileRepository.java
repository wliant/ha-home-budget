package com.homebudget.repository;

import com.homebudget.model.ExpenseFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseFileRepository extends JpaRepository<ExpenseFile, Long> {
    List<ExpenseFile> findByExpenseIdOrderByIdAsc(Long expenseId);

    long countByExpenseId(Long expenseId);
}
