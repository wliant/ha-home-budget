package com.homebudget.service;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseInputJobDTO;
import com.homebudget.dto.TemporaryExpenseRecordDTO;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.ExpenseInputJob;
import com.homebudget.model.TemporaryExpenseRecord;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseInputJobRepository;
import com.homebudget.repository.ExpenseRepository;
import com.homebudget.repository.TemporaryExpenseRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ExpenseInputJobServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseInputJobService jobService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseInputJobRepository jobRepository;

    @Autowired
    private TemporaryExpenseRecordRepository tempRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    private Category testCategory;
    private Budget testBudget;

    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Clean up in correct order to respect FK constraints
        tempRepository.deleteAll();
        jobRepository.deleteAll();
        expenseRepository.deleteAll();
        budgetRepository.deleteAll();
        categoryRepository.findAll().stream()
                .filter(c -> c.getIsSystem() == null || !c.getIsSystem())
                .forEach(c -> {
                    c.setParentCategory(null);
                    categoryRepository.save(c);
                });
        categoryRepository.flush();
        categoryRepository.findAll().stream()
                .filter(c -> (c.getIsSystem() == null || !c.getIsSystem()))
                .forEach(c -> categoryRepository.deleteById(c.getId()));
        categoryRepository.flush();

        // Set temp dir for file storage on BOTH services
        ReflectionTestUtils.setField(jobService, "expenseFileDir", tempDir.toString());
        ReflectionTestUtils.setField(expenseService, "expenseFileDir", tempDir.toString());

        // Create test category
        testCategory = categoryRepository.save(new Category("TestCategory", "📦", TEST_USERNAME));
        categoryRepository.flush();

        // Create yearly parent budget (required by budget validation)
        Budget yearlyBudget = new Budget();
        yearlyBudget.setYear(2026);
        yearlyBudget.setMonth(null);
        yearlyBudget.setTotalAmount(new BigDecimal("50000.00"));
        yearlyBudget.setCategory(testCategory);
        yearlyBudget.setCreatedBy(TEST_USERNAME);
        budgetRepository.save(yearlyBudget);

        // Create monthly budget for confirming expenses
        testBudget = new Budget();
        testBudget.setYear(2026);
        testBudget.setMonth(2);
        testBudget.setTotalAmount(new BigDecimal("1000.00"));
        testBudget.setCategory(testCategory);
        testBudget.setCreatedBy(TEST_USERNAME);
        testBudget = budgetRepository.save(testBudget);
        budgetRepository.flush();
    }

    @Nested
    @DisplayName("createJobs")
    class CreateJobs {

        @Test
        @DisplayName("should create job and persist to real DB")
        void happyPath() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "pdf content".getBytes());

            List<ExpenseInputJobDTO> result = jobService.createJobs(List.of(file), TEST_USERNAME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isNotNull();
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("receipt.pdf");
            assertThat(result.get(0).getStatus()).isEqualTo("PROCESSING");

            // Verify persisted in DB
            List<ExpenseInputJob> dbJobs = jobRepository.findAllByOrderByCreatedAtDesc();
            assertThat(dbJobs).hasSize(1);
            assertThat(dbJobs.get(0).getCreatedBy()).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("should throw when file list is empty")
        void emptyFileList() {
            assertThatThrownBy(() -> jobService.createJobs(List.of(), TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one file is required");
        }
    }

    @Nested
    @DisplayName("getJobs")
    class GetJobs {

        @Test
        @DisplayName("should return persisted jobs")
        void returnsJobs() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            jobService.createJobs(List.of(file), TEST_USERNAME);

            List<ExpenseInputJobDTO> result = jobService.getJobs();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("receipt.pdf");
        }

        @Test
        @DisplayName("should return empty list when no jobs")
        void returnsEmpty() {
            List<ExpenseInputJobDTO> result = jobService.getJobs();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateTemporaryRecord")
    class UpdateTemporaryRecord {

        @Test
        @DisplayName("should update record in real DB")
        void happyPath() {
            // Create job
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(file), TEST_USERNAME);
            Long jobId = jobs.get(0).getId();

            // Create temporary record via processPendingJobs workaround (set createdAt in past)
            setJobCreatedAtInPast(jobId);
            jobService.processPendingJobs();

            // Verify temp record was created
            assertThat(tempRepository.findByJobId(jobId)).isPresent();

            // Update the record
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("99.99"));
            request.setDescription("Updated in integration test");
            request.setExpenseDate(LocalDate.of(2026, 2, 20));
            request.setCategoryId(testCategory.getId());

            TemporaryExpenseRecordDTO result = jobService.updateTemporaryRecord(jobId, request);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(result.getDescription()).isEqualTo("Updated in integration test");
            assertThat(result.getExpenseDate()).isEqualTo(LocalDate.of(2026, 2, 20));
            assertThat(result.getCategoryId()).isEqualTo(testCategory.getId());
            assertThat(result.getCategoryName()).isEqualTo("TestCategory");
        }
    }

    @Nested
    @DisplayName("confirmJobs")
    class ConfirmJobs {

        @Test
        @DisplayName("should create real expenses from temporary records")
        void happyPath() {
            // Create job
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(file), TEST_USERNAME);
            Long jobId = jobs.get(0).getId();

            // Create temporary record via processPendingJobs
            setJobCreatedAtInPast(jobId);
            jobService.processPendingJobs();

            // Update temp record to have specific values for the confirm
            UpdateTemporaryExpenseRecordRequest updateReq = new UpdateTemporaryExpenseRecordRequest();
            updateReq.setAmount(new BigDecimal("55.00"));
            updateReq.setDescription("Grocery receipt");
            updateReq.setExpenseDate(LocalDate.of(2026, 2, 15));
            updateReq.setCategoryId(testCategory.getId());
            jobService.updateTemporaryRecord(jobId, updateReq);

            // Confirm
            ConfirmExpenseInputJobsRequest confirmReq = new ConfirmExpenseInputJobsRequest();
            confirmReq.setJobIds(List.of(jobId));

            List<Long> confirmed = jobService.confirmJobs(confirmReq, TEST_USERNAME);

            assertThat(confirmed).containsExactly(jobId);

            // Verify expense was created in DB
            assertThat(expenseRepository.count()).isGreaterThanOrEqualTo(1);

            // Verify record is now confirmed
            TemporaryExpenseRecord updatedRecord = tempRepository.findByJobId(jobId).orElseThrow();
            assertThat(updatedRecord.isConfirmed()).isTrue();
            assertThat(updatedRecord.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when no job IDs provided")
        void emptyJobIds() {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of());

            List<Long> result = jobService.confirmJobs(request, TEST_USERNAME);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteJobs")
    class DeleteJobs {

        @Test
        @DisplayName("should remove jobs from real DB")
        void happyPath() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(file), TEST_USERNAME);
            Long jobId = jobs.get(0).getId();

            assertThat(jobRepository.findById(jobId)).isPresent();

            jobService.deleteJobs(List.of(jobId));

            assertThat(jobRepository.findById(jobId)).isEmpty();
        }

        @Test
        @DisplayName("should do nothing for empty list")
        void emptyList() {
            jobService.deleteJobs(List.of());
            // No exception thrown
        }
    }

    @Nested
    @DisplayName("processPendingJobs")
    class ProcessPendingJobs {

        @Test
        @DisplayName("should process PROCESSING job and create temporary record")
        void processesJob() {
            // Create job
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(file), TEST_USERNAME);
            Long jobId = jobs.get(0).getId();

            // Verify job is in PROCESSING status
            ExpenseInputJob job = jobRepository.findById(jobId).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(ExpenseInputJob.Status.PROCESSING);

            // Use JdbcTemplate to set createdAt in the past (JPA updatable=false prevents normal update)
            setJobCreatedAtInPast(jobId);

            // Run the scheduled method directly
            jobService.processPendingJobs();

            // Verify temporary record was created
            TemporaryExpenseRecord record = tempRepository.findByJobId(jobId).orElse(null);
            assertThat(record).isNotNull();
            assertThat(record.getAmount()).isNotNull();
            assertThat(record.getDescription()).isNotNull();
            assertThat(record.getExpenseDate()).isNotNull();

            // Verify job status updated to COMPLETED
            ExpenseInputJob updatedJob = jobRepository.findById(jobId).orElseThrow();
            assertThat(updatedJob.getStatus()).isEqualTo(ExpenseInputJob.Status.COMPLETED);
        }

        @Test
        @DisplayName("should skip job that already has a temporary record")
        void skipsExistingRecord() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "receipt.pdf", "application/pdf", "content".getBytes());
            List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(file), TEST_USERNAME);
            Long jobId = jobs.get(0).getId();

            // Create temporary record via processPendingJobs
            setJobCreatedAtInPast(jobId);
            jobService.processPendingJobs();

            // Verify record exists
            TemporaryExpenseRecord firstRecord = tempRepository.findByJobId(jobId).orElseThrow();
            String originalDescription = firstRecord.getDescription();

            // Run processPendingJobs again - should skip since record exists
            jobService.processPendingJobs();

            // Verify the record is unchanged
            TemporaryExpenseRecord unchanged = tempRepository.findByJobId(jobId).orElseThrow();
            assertThat(unchanged.getDescription()).isEqualTo(originalDescription);
        }

        @Test
        @DisplayName("should do nothing when no pending jobs")
        void noPendingJobs() {
            // No jobs in DB
            jobService.processPendingJobs();
            // Should not throw
            assertThat(jobRepository.count()).isZero();
        }
    }

    /**
     * Helper: Set job's createdAt to 30 seconds ago via native SQL.
     * Needed because the JPA column has updatable=false and processPendingJobs
     * has a time-based delay check.
     */
    private void setJobCreatedAtInPast(Long jobId) {
        jdbcTemplate.update(
                "UPDATE expense_input_jobs SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(30), jobId);
    }
}
