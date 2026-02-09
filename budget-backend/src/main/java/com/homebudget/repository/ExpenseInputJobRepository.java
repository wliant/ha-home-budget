package com.homebudget.repository;

import com.homebudget.model.ExpenseInputJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseInputJobRepository extends JpaRepository<ExpenseInputJob, Long> {
    List<ExpenseInputJob> findAllByOrderByCreatedAtDesc();

    List<ExpenseInputJob> findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status status);
}
