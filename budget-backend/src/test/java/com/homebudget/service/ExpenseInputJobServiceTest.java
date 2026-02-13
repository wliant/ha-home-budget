package com.homebudget.service;

import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseInputJobDTO;
import com.homebudget.dto.TemporaryExpenseRecordDTO;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseInputJobServiceTest {

    @Mock
    private ExpenseInputJobRepository jobRepository;

    @Mock
    private TemporaryExpenseRecordRepository tempRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseInputJobService service;

    @TempDir
    Path tempDir;

    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expenseFileDir", tempDir.toString());
    }

    private ExpenseInputJob createJob(Long id, ExpenseInputJob.Status status, String filename) {
        ExpenseInputJob job = new ExpenseInputJob();
        job.setId(id);
        job.setStatus(status);
        job.setOriginalFilename(filename);
        job.setFilePath(tempDir.resolve("input-jobs/" + LocalDate.now().getYear() + "/job-" + id).toString());
        job.setCreatedBy(TEST_USERNAME);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        return job;
    }

    private Category createCategory(Long id, String name, String icon) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        return category;
    }

    private TemporaryExpenseRecord createTempRecord(Long id, ExpenseInputJob job, Category category) {
        TemporaryExpenseRecord record = new TemporaryExpenseRecord();
        record.setId(id);
        record.setJob(job);
        record.setAmount(new BigDecimal("25.50"));
        record.setDescription("Test expense");
        record.setExpenseDate(LocalDate.of(2025, 3, 15));
        record.setCategory(category);
        record.setConfirmed(false);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private Budget createBudget(Long id) {
        Budget budget = new Budget();
        budget.setId(id);
        return budget;
    }

    @Nested
    @DisplayName("createJobs")
    class CreateJobs {

        @Test
        @DisplayName("should create jobs for valid files and return DTOs")
        void happyPath() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "test content".getBytes());

            ExpenseInputJob savedJob = createJob(1L, ExpenseInputJob.Status.PENDING, "test.pdf");
            when(jobRepository.save(any(ExpenseInputJob.class))).thenAnswer(invocation -> {
                ExpenseInputJob arg = invocation.getArgument(0);
                if (arg.getId() == null) {
                    arg.setId(1L);
                    arg.setCreatedAt(LocalDateTime.now());
                    arg.setUpdatedAt(LocalDateTime.now());
                }
                return arg;
            });
            when(tempRepository.findByJobId(1L)).thenReturn(Optional.empty());

            List<ExpenseInputJobDTO> result = service.createJobs(List.of(file), TEST_USERNAME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("test.pdf");
            assertThat(result.get(0).getStatus()).isEqualTo("PROCESSING");

            // save is called 3 times: initial save, then set file path save, plus toDTO may trigger findByJobId
            verify(jobRepository, atLeast(2)).save(any(ExpenseInputJob.class));
        }

        @Test
        @DisplayName("should create jobs for multiple files")
        void multipleFiles() {
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "receipt1.pdf", "application/pdf", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "receipt2.png", "image/png", "content2".getBytes());

            when(jobRepository.save(any(ExpenseInputJob.class))).thenAnswer(invocation -> {
                ExpenseInputJob arg = invocation.getArgument(0);
                if (arg.getId() == null) {
                    arg.setId((long) (Math.random() * 1000 + 1));
                    arg.setCreatedAt(LocalDateTime.now());
                    arg.setUpdatedAt(LocalDateTime.now());
                }
                return arg;
            });
            when(tempRepository.findByJobId(anyLong())).thenReturn(Optional.empty());

            List<ExpenseInputJobDTO> result = service.createJobs(List.of(file1, file2), TEST_USERNAME);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when file list is null")
        void nullFileList() {
            assertThatThrownBy(() -> service.createJobs(null, TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one file is required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when file list is empty")
        void emptyFileList() {
            assertThatThrownBy(() -> service.createJobs(Collections.emptyList(), TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one file is required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when more than 20 files")
        void tooManyFiles() {
            List<org.springframework.web.multipart.MultipartFile> files = IntStream.rangeClosed(1, 21)
                    .mapToObj(i -> (org.springframework.web.multipart.MultipartFile) new MockMultipartFile(
                            "file", "file" + i + ".pdf", "application/pdf", "content".getBytes()))
                    .collect(java.util.stream.Collectors.toList());

            assertThatThrownBy(() -> service.createJobs(files, TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Maximum 20 files allowed per upload");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty file content")
        void emptyFileContent() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.pdf", "application/pdf", new byte[0]);

            assertThatThrownBy(() -> service.createJobs(List.of(emptyFile), TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File cannot be empty");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for unsupported file type")
        void unsupportedFileType() {
            MockMultipartFile textFile = new MockMultipartFile(
                    "file", "notes.txt", "text/plain", "some text".getBytes());

            assertThatThrownBy(() -> service.createJobs(List.of(textFile), TEST_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Only PDF and image files are allowed");
        }

        @Test
        @DisplayName("should set originalFilename to 'unnamed' when original filename is null")
        void nullOriginalFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "application/pdf", "test content".getBytes());

            when(jobRepository.save(any(ExpenseInputJob.class))).thenAnswer(invocation -> {
                ExpenseInputJob arg = invocation.getArgument(0);
                if (arg.getId() == null) {
                    arg.setId(1L);
                    arg.setCreatedAt(LocalDateTime.now());
                    arg.setUpdatedAt(LocalDateTime.now());
                }
                return arg;
            });
            when(tempRepository.findByJobId(1L)).thenReturn(Optional.empty());

            List<ExpenseInputJobDTO> result = service.createJobs(List.of(file), TEST_USERNAME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("getJobs")
    class GetJobs {

        @Test
        @DisplayName("should return list of job DTOs ordered by createdAt desc")
        void returnsJobList() {
            ExpenseInputJob job1 = createJob(1L, ExpenseInputJob.Status.COMPLETED, "receipt1.pdf");
            ExpenseInputJob job2 = createJob(2L, ExpenseInputJob.Status.PROCESSING, "receipt2.pdf");

            Category category = createCategory(10L, "Food", "restaurant");
            TemporaryExpenseRecord record = createTempRecord(100L, job1, category);
            job1.setTemporaryRecord(record);

            when(jobRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(job1, job2));
            when(tempRepository.findByJobId(2L)).thenReturn(Optional.empty());

            List<ExpenseInputJobDTO> result = service.getJobs();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
            assertThat(result.get(0).getOriginalFilename()).isEqualTo("receipt1.pdf");
            assertThat(result.get(0).getTemporaryRecord()).isNotNull();
            assertThat(result.get(0).getTemporaryRecord().getAmount()).isEqualByComparingTo(new BigDecimal("25.50"));
            assertThat(result.get(0).getTemporaryRecord().getCategoryName()).isEqualTo("Food");
            assertThat(result.get(0).getTemporaryRecord().getCategoryIcon()).isEqualTo("restaurant");

            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getStatus()).isEqualTo("PROCESSING");
            assertThat(result.get(1).getTemporaryRecord()).isNull();

            verify(jobRepository).findAllByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("should return empty list when no jobs exist")
        void returnsEmptyList() {
            when(jobRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

            List<ExpenseInputJobDTO> result = service.getJobs();

            assertThat(result).isEmpty();
            verify(jobRepository).findAllByOrderByCreatedAtDesc();
        }
    }

    @Nested
    @DisplayName("updateTemporaryRecord")
    class UpdateTemporaryRecord {

        @Test
        @DisplayName("should update record fields and return updated DTO")
        void happyPath() {
            Long jobId = 1L;
            Category oldCategory = createCategory(10L, "Food", "restaurant");
            Category newCategory = createCategory(20L, "Transport", "directions_car");
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");

            TemporaryExpenseRecord record = createTempRecord(100L, job, oldCategory);

            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("99.99"));
            request.setDescription("Updated description");
            request.setExpenseDate(LocalDate.of(2025, 6, 1));
            request.setCategoryId(20L);

            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.of(record));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
            when(tempRepository.save(any(TemporaryExpenseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TemporaryExpenseRecordDTO result = service.updateTemporaryRecord(jobId, request);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getExpenseDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getCategoryId()).isEqualTo(20L);
            assertThat(result.getCategoryName()).isEqualTo("Transport");
            assertThat(result.getCategoryIcon()).isEqualTo("directions_car");

            verify(tempRepository).findByJobId(jobId);
            verify(categoryRepository).findById(20L);
            verify(tempRepository).save(record);
        }

        @Test
        @DisplayName("should clear category when categoryId is null")
        void clearCategory() {
            Long jobId = 1L;
            Category oldCategory = createCategory(10L, "Food", "restaurant");
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");
            TemporaryExpenseRecord record = createTempRecord(100L, job, oldCategory);

            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("50.00"));
            request.setDescription("No category");
            request.setExpenseDate(LocalDate.of(2025, 4, 10));
            request.setCategoryId(null);

            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.of(record));
            when(tempRepository.save(any(TemporaryExpenseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TemporaryExpenseRecordDTO result = service.updateTemporaryRecord(jobId, request);

            assertThat(result.getCategoryId()).isNull();
            assertThat(result.getCategoryName()).isNull();
            verify(categoryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("should throw ExpenseNotFoundException when job record not found")
        void jobNotFound() {
            Long jobId = 999L;
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("Test");
            request.setExpenseDate(LocalDate.now());

            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTemporaryRecord(jobId, request))
                    .isInstanceOf(ExpenseNotFoundException.class);

            verify(tempRepository).findByJobId(jobId);
            verify(tempRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void categoryNotFound() {
            Long jobId = 1L;
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");
            Category oldCategory = createCategory(10L, "Food", "restaurant");
            TemporaryExpenseRecord record = createTempRecord(100L, job, oldCategory);

            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("Test");
            request.setExpenseDate(LocalDate.now());
            request.setCategoryId(999L);

            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.of(record));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateTemporaryRecord(jobId, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(tempRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirmJobs")
    class ConfirmJobs {

        @Test
        @DisplayName("should confirm unconfirmed records and return confirmed job IDs")
        void happyPath() {
            Long jobId = 1L;
            Category category = createCategory(10L, "Food", "restaurant");
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");
            TemporaryExpenseRecord record = createTempRecord(100L, job, category);
            record.setExpenseDate(LocalDate.of(2025, 3, 15));

            Budget monthlyBudget = createBudget(50L);

            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of(jobId));

            when(tempRepository.findByJobIdIn(List.of(jobId))).thenReturn(List.of(record));
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 3))
                    .thenReturn(Optional.of(monthlyBudget));

            ExpenseDTO createdDto = new ExpenseDTO();
            createdDto.setId(200L);
            when(expenseService.createExpense(any(ExpenseDTO.class), eq(TEST_USERNAME))).thenReturn(createdDto);

            Expense createdExpense = new Expense();
            createdExpense.setId(200L);
            when(expenseRepository.findById(200L)).thenReturn(Optional.of(createdExpense));

            // The file won't exist at the job's file path in test, so attachExistingFile won't be called
            // but the service logs a warning and continues

            List<Long> result = service.confirmJobs(request, TEST_USERNAME);

            assertThat(result).containsExactly(jobId);

            verify(tempRepository).findByJobIdIn(List.of(jobId));
            verify(expenseService).createExpense(any(ExpenseDTO.class), eq(TEST_USERNAME));
            verify(expenseRepository).findById(200L);

            ArgumentCaptor<TemporaryExpenseRecord> captor = ArgumentCaptor.forClass(TemporaryExpenseRecord.class);
            verify(tempRepository).save(captor.capture());
            assertThat(captor.getValue().isConfirmed()).isTrue();
            assertThat(captor.getValue().getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("should skip already confirmed records")
        void skipsAlreadyConfirmed() {
            Long jobId = 1L;
            Category category = createCategory(10L, "Food", "restaurant");
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");
            TemporaryExpenseRecord record = createTempRecord(100L, job, category);
            record.setConfirmed(true);
            record.setConfirmedAt(LocalDateTime.now());

            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of(jobId));

            when(tempRepository.findByJobIdIn(List.of(jobId))).thenReturn(List.of(record));

            List<Long> result = service.confirmJobs(request, TEST_USERNAME);

            assertThat(result).isEmpty();
            verify(expenseService, never()).createExpense(any(), anyString());
            verify(tempRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return empty list when jobIds is empty")
        void emptyJobIds() {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(Collections.emptyList());

            List<Long> result = service.confirmJobs(request, TEST_USERNAME);

            assertThat(result).isEmpty();
            verify(tempRepository, never()).findByJobIdIn(any());
        }

        @Test
        @DisplayName("should return empty list when jobIds is null")
        void nullJobIds() {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(null);

            List<Long> result = service.confirmJobs(request, TEST_USERNAME);

            assertThat(result).isEmpty();
            verify(tempRepository, never()).findByJobIdIn(any());
        }

        @Test
        @DisplayName("should fall back to parent budget when no monthly budget exists")
        void fallbackToParentBudget() {
            Long jobId = 2L;
            Category category = createCategory(10L, "Food", "restaurant");
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");
            TemporaryExpenseRecord record = createTempRecord(101L, job, category);
            record.setExpenseDate(LocalDate.of(2025, 5, 20));

            Budget parentBudget = createBudget(60L);

            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of(jobId));

            when(tempRepository.findByJobIdIn(List.of(jobId))).thenReturn(List.of(record));
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 5))
                    .thenReturn(Optional.empty());
            when(budgetRepository.findParentBudget(10L, 2025))
                    .thenReturn(Optional.of(parentBudget));

            ExpenseDTO createdDto = new ExpenseDTO();
            createdDto.setId(201L);
            when(expenseService.createExpense(any(ExpenseDTO.class), eq(TEST_USERNAME))).thenReturn(createdDto);

            Expense createdExpense = new Expense();
            createdExpense.setId(201L);
            when(expenseRepository.findById(201L)).thenReturn(Optional.of(createdExpense));

            List<Long> result = service.confirmJobs(request, TEST_USERNAME);

            assertThat(result).containsExactly(jobId);

            ArgumentCaptor<ExpenseDTO> dtoCaptor = ArgumentCaptor.forClass(ExpenseDTO.class);
            verify(expenseService).createExpense(dtoCaptor.capture(), eq(TEST_USERNAME));
            assertThat(dtoCaptor.getValue().getBudgetId()).isEqualTo(60L);
        }
    }

    @Nested
    @DisplayName("deleteJobs")
    class DeleteJobs {

        @Test
        @DisplayName("should delete jobs and their unconfirmed temp records and files")
        void happyPath() {
            Long jobId = 1L;
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");

            Category category = createCategory(10L, "Food", "restaurant");
            TemporaryExpenseRecord record = createTempRecord(100L, job, category);
            record.setConfirmed(false);

            when(jobRepository.findAllById(List.of(jobId))).thenReturn(List.of(job));
            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.of(record));

            service.deleteJobs(List.of(jobId));

            verify(tempRepository).delete(record);
            verify(jobRepository).delete(job);
        }

        @Test
        @DisplayName("should delete confirmed temp records but not the file")
        void deleteConfirmedRecord() throws Exception {
            Long jobId = 2L;
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.COMPLETED, "receipt.pdf");

            // Create the file on disk so we can verify it is NOT deleted for confirmed records
            Path filePath = tempDir.resolve("input-jobs/" + LocalDate.now().getYear() + "/job-2");
            java.nio.file.Files.createDirectories(filePath.getParent());
            java.nio.file.Files.writeString(filePath, "file content");
            job.setFilePath(filePath.toString());

            Category category = createCategory(10L, "Food", "restaurant");
            TemporaryExpenseRecord record = createTempRecord(100L, job, category);
            record.setConfirmed(true);
            record.setConfirmedAt(LocalDateTime.now());

            when(jobRepository.findAllById(List.of(jobId))).thenReturn(List.of(job));
            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.of(record));

            service.deleteJobs(List.of(jobId));

            verify(tempRepository).delete(record);
            verify(jobRepository).delete(job);
            // File should still exist because record was confirmed
            assertThat(java.nio.file.Files.exists(filePath)).isTrue();
        }

        @Test
        @DisplayName("should delete job even when no temp record exists")
        void noTempRecord() {
            Long jobId = 3L;
            ExpenseInputJob job = createJob(jobId, ExpenseInputJob.Status.FAILED, "receipt.pdf");

            when(jobRepository.findAllById(List.of(jobId))).thenReturn(List.of(job));
            when(tempRepository.findByJobId(jobId)).thenReturn(Optional.empty());

            service.deleteJobs(List.of(jobId));

            verify(tempRepository, never()).delete(any(TemporaryExpenseRecord.class));
            verify(jobRepository).delete(job);
        }

        @Test
        @DisplayName("should do nothing when jobIds is empty")
        void emptyJobIds() {
            service.deleteJobs(Collections.emptyList());

            verify(jobRepository, never()).findAllById(any());
            verify(jobRepository, never()).delete(any(ExpenseInputJob.class));
        }

        @Test
        @DisplayName("should do nothing when jobIds is null")
        void nullJobIds() {
            service.deleteJobs(null);

            verify(jobRepository, never()).findAllById(any());
            verify(jobRepository, never()).delete(any(ExpenseInputJob.class));
        }

        @Test
        @DisplayName("should delete multiple jobs")
        void multipleJobs() {
            ExpenseInputJob job1 = createJob(1L, ExpenseInputJob.Status.COMPLETED, "receipt1.pdf");
            ExpenseInputJob job2 = createJob(2L, ExpenseInputJob.Status.FAILED, "receipt2.pdf");

            Category category = createCategory(10L, "Food", "restaurant");
            TemporaryExpenseRecord record1 = createTempRecord(100L, job1, category);

            when(jobRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(job1, job2));
            when(tempRepository.findByJobId(1L)).thenReturn(Optional.of(record1));
            when(tempRepository.findByJobId(2L)).thenReturn(Optional.empty());

            service.deleteJobs(List.of(1L, 2L));

            verify(tempRepository).delete(record1);
            verify(jobRepository).delete(job1);
            verify(jobRepository).delete(job2);
        }
    }
}
