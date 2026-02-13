package com.homebudget.repository;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.model.ExpenseInputJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ExpenseInputJobRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseInputJobRepository jobRepository;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        jobRepository.flush();
    }

    private ExpenseInputJob createJob(String filename, ExpenseInputJob.Status status) {
        ExpenseInputJob job = new ExpenseInputJob();
        job.setOriginalFilename(filename);
        job.setFilePath("/uploads/" + filename);
        job.setCreatedBy("testuser");
        job.setStatus(status);
        return jobRepository.save(job);
    }

    @Test
    @DisplayName("should save and retrieve ExpenseInputJob")
    void saveAndRetrieve() {
        ExpenseInputJob job = createJob("receipt.pdf", ExpenseInputJob.Status.PENDING);
        jobRepository.flush();

        assertThat(job.getId()).isNotNull();
        assertThat(job.getOriginalFilename()).isEqualTo("receipt.pdf");
        assertThat(job.getStatus()).isEqualTo(ExpenseInputJob.Status.PENDING);
        assertThat(job.getCreatedAt()).isNotNull();
        assertThat(job.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find all jobs ordered by createdAt descending")
    void findAllByOrderByCreatedAtDesc() throws InterruptedException {
        createJob("first.pdf", ExpenseInputJob.Status.COMPLETED);
        jobRepository.flush();
        Thread.sleep(1100); // MySQL DATETIME has second-level precision
        createJob("second.pdf", ExpenseInputJob.Status.PENDING);
        jobRepository.flush();

        List<ExpenseInputJob> jobs = jobRepository.findAllByOrderByCreatedAtDesc();

        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getOriginalFilename()).isEqualTo("second.pdf");
        assertThat(jobs.get(1).getOriginalFilename()).isEqualTo("first.pdf");
    }

    @Test
    @DisplayName("should find jobs by PENDING status")
    void findByStatus_pending() {
        createJob("pending.pdf", ExpenseInputJob.Status.PENDING);
        createJob("completed.pdf", ExpenseInputJob.Status.COMPLETED);
        createJob("failed.pdf", ExpenseInputJob.Status.FAILED);
        jobRepository.flush();

        List<ExpenseInputJob> pending = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.PENDING);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getOriginalFilename()).isEqualTo("pending.pdf");
    }

    @Test
    @DisplayName("should find jobs by COMPLETED status")
    void findByStatus_completed() {
        createJob("pending.pdf", ExpenseInputJob.Status.PENDING);
        createJob("completed1.pdf", ExpenseInputJob.Status.COMPLETED);
        createJob("completed2.pdf", ExpenseInputJob.Status.COMPLETED);
        jobRepository.flush();

        List<ExpenseInputJob> completed = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.COMPLETED);

        assertThat(completed).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when no jobs match status")
    void findByStatus_noMatch() {
        createJob("pending.pdf", ExpenseInputJob.Status.PENDING);
        jobRepository.flush();

        List<ExpenseInputJob> failed = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.FAILED);

        assertThat(failed).isEmpty();
    }

    @Test
    @DisplayName("should persist all status enum values")
    void statusEnumPersistence() {
        for (ExpenseInputJob.Status status : ExpenseInputJob.Status.values()) {
            ExpenseInputJob job = createJob("file-" + status.name() + ".pdf", status);
            jobRepository.flush();

            ExpenseInputJob retrieved = jobRepository.findById(job.getId()).orElseThrow();
            assertThat(retrieved.getStatus()).isEqualTo(status);
        }
    }
}
