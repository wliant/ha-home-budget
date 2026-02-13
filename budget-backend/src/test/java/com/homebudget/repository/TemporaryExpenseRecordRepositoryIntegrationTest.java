package com.homebudget.repository;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.model.Category;
import com.homebudget.model.ExpenseInputJob;
import com.homebudget.model.TemporaryExpenseRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TemporaryExpenseRecordRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TemporaryExpenseRecordRepository tempRepository;

    @Autowired
    private ExpenseInputJobRepository jobRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        tempRepository.deleteAll();
        jobRepository.deleteAll();
        categoryRepository.findAll().stream()
                .filter(c -> c.getIsSystem() == null || !c.getIsSystem())
                .forEach(c -> {
                    c.setParentCategory(null);
                    categoryRepository.save(c);
                });
        categoryRepository.flush();
        categoryRepository.findAll().stream()
                .filter(c -> c.getIsSystem() == null || !c.getIsSystem())
                .forEach(c -> categoryRepository.deleteById(c.getId()));
        categoryRepository.flush();

        testCategory = categoryRepository.save(new Category("TestCat", "icon", "testuser"));
        categoryRepository.flush();
    }

    private ExpenseInputJob createJob(String filename) {
        ExpenseInputJob job = new ExpenseInputJob();
        job.setOriginalFilename(filename);
        job.setFilePath("/uploads/" + filename);
        job.setCreatedBy("testuser");
        job.setStatus(ExpenseInputJob.Status.COMPLETED);
        return jobRepository.save(job);
    }

    private TemporaryExpenseRecord createRecord(ExpenseInputJob job) {
        TemporaryExpenseRecord record = new TemporaryExpenseRecord();
        record.setJob(job);
        record.setAmount(new BigDecimal("25.50"));
        record.setDescription("Test record");
        record.setExpenseDate(LocalDate.of(2026, 2, 15));
        record.setCategory(testCategory);
        record.setConfirmed(false);
        return tempRepository.save(record);
    }

    @Test
    @DisplayName("should save and retrieve TemporaryExpenseRecord")
    void saveAndRetrieve() {
        ExpenseInputJob job = createJob("receipt.pdf");
        jobRepository.flush();

        TemporaryExpenseRecord record = createRecord(job);
        tempRepository.flush();

        assertThat(record.getId()).isNotNull();
        assertThat(record.getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(record.getDescription()).isEqualTo("Test record");
        assertThat(record.getExpenseDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(record.isConfirmed()).isFalse();
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find record by job ID")
    void findByJobId() {
        ExpenseInputJob job = createJob("receipt.pdf");
        jobRepository.flush();
        createRecord(job);
        tempRepository.flush();

        Optional<TemporaryExpenseRecord> found = tempRepository.findByJobId(job.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test record");
    }

    @Test
    @DisplayName("should return empty for nonexistent job ID")
    void findByJobId_nonexistent() {
        Optional<TemporaryExpenseRecord> found = tempRepository.findByJobId(99999L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find records by multiple job IDs")
    void findByJobIdIn() {
        ExpenseInputJob job1 = createJob("receipt1.pdf");
        ExpenseInputJob job2 = createJob("receipt2.pdf");
        jobRepository.flush();

        createRecord(job1);
        createRecord(job2);
        tempRepository.flush();

        List<TemporaryExpenseRecord> records = tempRepository.findByJobIdIn(
                List.of(job1.getId(), job2.getId()));

        assertThat(records).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list for nonexistent job IDs")
    void findByJobIdIn_nonexistent() {
        List<TemporaryExpenseRecord> records = tempRepository.findByJobIdIn(
                List.of(99998L, 99999L));
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("should return partial results when some IDs exist")
    void findByJobIdIn_partial() {
        ExpenseInputJob job1 = createJob("receipt1.pdf");
        jobRepository.flush();
        createRecord(job1);
        tempRepository.flush();

        List<TemporaryExpenseRecord> records = tempRepository.findByJobIdIn(
                List.of(job1.getId(), 99999L));

        assertThat(records).hasSize(1);
    }
}
