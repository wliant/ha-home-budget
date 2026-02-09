package com.homebudget.repository;

import com.homebudget.model.TemporaryExpenseRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporaryExpenseRecordRepository extends JpaRepository<TemporaryExpenseRecord, Long> {
    Optional<TemporaryExpenseRecord> findByJobId(Long jobId);

    List<TemporaryExpenseRecord> findByJobIdIn(List<Long> jobIds);
}
