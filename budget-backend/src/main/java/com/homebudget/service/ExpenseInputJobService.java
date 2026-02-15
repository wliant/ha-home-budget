package com.homebudget.service;

import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseInputJobDTO;
import com.homebudget.dto.TemporaryExpenseRecordDTO;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.model.ExpenseInputJob;
import com.homebudget.model.TemporaryExpenseRecord;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseInputJobRepository;
import com.homebudget.repository.ExpenseRepository;
import com.homebudget.repository.TemporaryExpenseRecordRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ExpenseInputJobService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseInputJobService.class);
    private static final int MAX_FILES = 20;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private final ExpenseInputJobRepository jobRepository;
    private final TemporaryExpenseRecordRepository tempRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    @Value("${EXPENSE_FILE_DIR:./data/expense-files}")
    private String expenseFileDir;

    public ExpenseInputJobService(ExpenseInputJobRepository jobRepository,
                                  TemporaryExpenseRecordRepository tempRepository,
                                  CategoryRepository categoryRepository,
                                  ExpenseService expenseService,
                                  ExpenseRepository expenseRepository) {
        this.jobRepository = jobRepository;
        this.tempRepository = tempRepository;
        this.categoryRepository = categoryRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseInputJobDTO> createJobs(List<MultipartFile> files, String username) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Maximum 20 files allowed per upload");
        }

        List<ExpenseInputJob> jobs = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            ExpenseInputJob job = new ExpenseInputJob();
            job.setStatus(ExpenseInputJob.Status.PENDING);
            job.setOriginalFilename(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
            job.setCreatedBy(username);
            job.setFilePath("temp");
            job = jobRepository.save(job);

            Path target = resolveJobPath(job);
            try {
                Files.createDirectories(target.getParent());
                file.transferTo(target);
            } catch (IOException e) {
                job.setStatus(ExpenseInputJob.Status.FAILED);
                job.setErrorMessage("Failed to store file");
                jobRepository.save(job);
                continue;
            }

            job.setFilePath(target.toString());
            job.setStatus(ExpenseInputJob.Status.PROCESSING);
            jobRepository.save(job);
            jobs.add(job);

        }

        return jobs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseInputJobDTO> getJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TemporaryExpenseRecordDTO updateTemporaryRecord(Long jobId, UpdateTemporaryExpenseRecordRequest request) {
        TemporaryExpenseRecord record = tempRepository.findByJobId(jobId)
                .orElseThrow(() -> new ExpenseNotFoundException(jobId));

        record.setAmount(request.getAmount());
        record.setDescription(request.getDescription());
        record.setExpenseDate(request.getExpenseDate());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            record.setCategory(category);
        } else {
            record.setCategory(null);
        }

        TemporaryExpenseRecord saved = tempRepository.save(record);
        return toTemporaryDTO(saved);
    }

    public List<Long> confirmJobs(ConfirmExpenseInputJobsRequest request, String username) {
        List<Long> jobIds = request.getJobIds() == null ? List.of() : request.getJobIds();
        if (jobIds.isEmpty()) {
            return List.of();
        }

        List<TemporaryExpenseRecord> records = tempRepository.findByJobIdIn(jobIds);
        List<Long> confirmed = new ArrayList<>();
        for (TemporaryExpenseRecord record : records) {
            if (record.isConfirmed()) {
                continue;
            }
            ExpenseInputJob job = record.getJob();
            Expense expense = createExpenseFromTemporary(record, job, username);
            record.setConfirmed(true);
            record.setConfirmedAt(LocalDateTime.now());
            tempRepository.save(record);
            confirmed.add(job.getId());
        }
        return confirmed;
    }

    public void deleteJobs(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }
        List<ExpenseInputJob> jobs = jobRepository.findAllById(jobIds);
        for (ExpenseInputJob job : jobs) {
            TemporaryExpenseRecord record = tempRepository.findByJobId(job.getId()).orElse(null);
            if (record != null && !record.isConfirmed()) {
                tempRepository.delete(record);
            } else if (record != null) {
                tempRepository.delete(record);
            }

            if (record == null || !record.isConfirmed()) {
                deleteFileIfExists(job.getFilePath());
            }

            jobRepository.delete(job);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 2000)
    public void processPendingJobs() {
        List<ExpenseInputJob> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.PROCESSING);
        if (jobs.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (ExpenseInputJob job : jobs) {
            if (tempRepository.findByJobId(job.getId()).isPresent()) {
                continue;
            }
            int delaySeconds = 3 + Math.toIntExact(job.getId() % 8);
            Duration elapsed = Duration.between(job.getCreatedAt(), now);
            if (elapsed.getSeconds() < delaySeconds) {
                continue;
            }

            try {
                Random random = new Random(job.getId());
                TemporaryExpenseRecord record = new TemporaryExpenseRecord();
                record.setJob(job);
                record.setAmount(new BigDecimal(String.format("%.2f", 5 + random.nextDouble() * 250)));
                record.setDescription(randomDescription(random));
                record.setExpenseDate(LocalDate.now().minusDays(random.nextInt(120)));

                List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
                if (!categories.isEmpty()) {
                    Category category = categories.get(random.nextInt(categories.size()));
                    record.setCategory(category);
                }

                tempRepository.save(record);
                job.setStatus(ExpenseInputJob.Status.COMPLETED);
                jobRepository.save(job);
            } catch (Exception e) {
                logger.error("Failed to process job {}", job.getId(), e);
                job.setStatus(ExpenseInputJob.Status.FAILED);
                job.setErrorMessage("Processing failed");
                jobRepository.save(job);
            }
        }
    }

    private Expense createExpenseFromTemporary(TemporaryExpenseRecord record, ExpenseInputJob job, String username) {
        com.homebudget.dto.ExpenseDTO dto = new com.homebudget.dto.ExpenseDTO();
        dto.setAmount(record.getAmount());
        dto.setDescription(record.getDescription());
        dto.setExpenseDate(record.getExpenseDate());
        if (record.getCategory() != null) {
            dto.setCategoryId(record.getCategory().getId());
        }

        com.homebudget.dto.ExpenseDTO created = expenseService.createExpense(dto, username);
        Expense expense = expenseRepository.findById(created.getId())
                .orElseThrow(() -> new ExpenseNotFoundException(created.getId()));

        Path source = Paths.get(job.getFilePath());
        if (Files.exists(source)) {
            try {
                expenseService.attachExistingFile(expense, source, job.getOriginalFilename());
            } catch (RuntimeException e) {
                logger.warn("Failed to attach file for job {}. Continuing without file.", job.getId(), e);
                job.setErrorMessage("File attachment failed");
                jobRepository.save(job);
            }
        } else {
            logger.warn("Missing source file for job {} at {}. Continuing without file.", job.getId(), source);
            job.setErrorMessage("Source file missing");
            jobRepository.save(job);
        }
        return expense;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        boolean isImage = contentType.startsWith("image/");
        boolean isPdf = "application/pdf".equals(contentType);
        if (!isImage && !isPdf) {
            throw new IllegalArgumentException("Only PDF and image files are allowed");
        }
    }

    private Path resolveJobPath(ExpenseInputJob job) {
        int year = LocalDate.now().getYear();
        String fileName = "job-" + job.getId();
        return Paths.get(expenseFileDir, "input-jobs", String.valueOf(year), fileName);
    }

    private void deleteFileIfExists(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            logger.warn("Failed to delete job file {}", path, e);
        }
    }

    private ExpenseInputJobDTO toDTO(ExpenseInputJob job) {
        ExpenseInputJobDTO dto = new ExpenseInputJobDTO();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus().name());
        dto.setOriginalFilename(job.getOriginalFilename());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        TemporaryExpenseRecord record = job.getTemporaryRecord();
        if (record == null) {
            record = tempRepository.findByJobId(job.getId()).orElse(null);
        }
        if (record != null) {
            dto.setTemporaryRecord(toTemporaryDTO(record));
        }
        return dto;
    }

    private TemporaryExpenseRecordDTO toTemporaryDTO(TemporaryExpenseRecord record) {
        TemporaryExpenseRecordDTO dto = new TemporaryExpenseRecordDTO();
        dto.setId(record.getId());
        dto.setAmount(record.getAmount());
        dto.setDescription(record.getDescription());
        dto.setExpenseDate(record.getExpenseDate());
        if (record.getCategory() != null) {
            dto.setCategoryId(record.getCategory().getId());
            dto.setCategoryName(record.getCategory().getName());
            dto.setCategoryIcon(record.getCategory().getIcon());
        }
        dto.setConfirmed(record.isConfirmed());
        dto.setConfirmedAt(record.getConfirmedAt());
        return dto;
    }

    private String randomDescription(Random random) {
        String[] options = {
                "Receipt scan",
                "Store purchase",
                "Online order",
                "Travel expense",
                "Food receipt",
                "Service payment",
                "Subscription",
                "Medical receipt"
        };
        return options[random.nextInt(options.length)];
    }
}
