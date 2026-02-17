package com.homebudget.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ExpenseInputJobService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseInputJobService.class);
    private static final int MAX_FILES = 20;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_RETRIES = 3;

    private final ExpenseInputJobRepository jobRepository;
    private final TemporaryExpenseRecordRepository tempRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final OcrProcessorClient ocrProcessorClient;
    private final StorageService storageService;

    public ExpenseInputJobService(ExpenseInputJobRepository jobRepository,
                                  TemporaryExpenseRecordRepository tempRepository,
                                  CategoryRepository categoryRepository,
                                  ExpenseService expenseService,
                                  ExpenseRepository expenseRepository,
                                  OcrProcessorClient ocrProcessorClient,
                                  StorageService storageService) {
        this.jobRepository = jobRepository;
        this.tempRepository = tempRepository;
        this.categoryRepository = categoryRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.ocrProcessorClient = ocrProcessorClient;
        this.storageService = storageService;
    }

    public List<ExpenseInputJobDTO> createJobs(List<MultipartFile> files, String username, Long defaultCategoryId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Maximum 20 files allowed per upload");
        }

        Category defaultCategory = null;
        if (defaultCategoryId != null) {
            defaultCategory = categoryRepository.findById(defaultCategoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(defaultCategoryId));
        }

        List<ExpenseInputJob> jobs = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);
            ExpenseInputJob job = new ExpenseInputJob();
            job.setStatus(ExpenseInputJob.Status.UPLOADED);
            job.setRetryCount(0);
            job.setOriginalFilename(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
            job.setCreatedBy(username);
            job.setFilePath("temp");
            job.setDefaultCategory(defaultCategory);
            job = jobRepository.save(job);

            String objectKey = "input-jobs/" + LocalDate.now().getYear() + "/job-" + job.getId();
            try {
                String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                storageService.putObject(objectKey, file.getBytes(), contentType);
            } catch (IOException e) {
                job.setStatus(ExpenseInputJob.Status.FAILED);
                job.setErrorMessage("Failed to store file");
                jobRepository.save(job);
                continue;
            }

            job.setFilePath(objectKey);
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

    public TemporaryExpenseRecordDTO updateTemporaryRecord(Long recordId, UpdateTemporaryExpenseRecordRequest request) {
        TemporaryExpenseRecord record = tempRepository.findById(recordId)
                .orElseThrow(() -> new ExpenseNotFoundException(recordId));

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

    public ExpenseInputJobDTO retryJob(Long jobId) {
        ExpenseInputJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ExpenseNotFoundException(jobId));

        if (job.getStatus() != ExpenseInputJob.Status.RETRYABLE) {
            throw new IllegalStateException("Job is not in RETRYABLE status");
        }

        job.setStatus(ExpenseInputJob.Status.UPLOADED);
        job.setErrorMessage(null);
        jobRepository.save(job);
        logger.info("Job {} queued for retry (attempt {})", jobId, job.getRetryCount() + 1);
        return toDTO(job);
    }

    public ExpenseInputJobDTO completeJob(Long jobId, String username) {
        ExpenseInputJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ExpenseNotFoundException(jobId));

        if (job.getStatus() != ExpenseInputJob.Status.PROCESSED) {
            throw new IllegalStateException("Job must be in PROCESSED status to complete");
        }

        List<TemporaryExpenseRecord> records = tempRepository.findByJobId(jobId);
        if (records.isEmpty()) {
            throw new IllegalStateException("No temporary records to finalize");
        }

        for (TemporaryExpenseRecord record : records) {
            if (!record.isConfirmed()) {
                createExpenseFromTemporary(record, job, username);
                record.setConfirmed(true);
                record.setConfirmedAt(LocalDateTime.now());
                tempRepository.save(record);
            }
        }

        job.setStatus(ExpenseInputJob.Status.COMPLETED);
        jobRepository.save(job);
        logger.info("Job {} completed by {} with {} record(s)", jobId, username, records.size());
        return toDTO(job);
    }

    public TemporaryExpenseRecordDTO mergeTemporaryRecords(List<Long> recordIds) {
        if (recordIds == null || recordIds.size() < 2) {
            throw new IllegalArgumentException("At least 2 records are required to merge");
        }

        List<TemporaryExpenseRecord> records = tempRepository.findAllById(recordIds);
        if (records.size() != recordIds.size()) {
            throw new ExpenseNotFoundException(0L);
        }

        ExpenseInputJob job = records.get(0).getJob();
        for (TemporaryExpenseRecord record : records) {
            if (!record.getJob().getId().equals(job.getId())) {
                throw new IllegalArgumentException("All records must belong to the same job");
            }
        }
        if (job.getStatus() == ExpenseInputJob.Status.COMPLETED) {
            throw new IllegalStateException("Cannot merge records of a completed job");
        }

        records.sort((a, b) -> a.getId().compareTo(b.getId()));

        LocalDate earliestDate = records.stream()
                .map(TemporaryExpenseRecord::getExpenseDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        String mergedDescription = records.stream()
                .map(TemporaryExpenseRecord::getDescription)
                .collect(Collectors.joining("\n"));

        BigDecimal totalAmount = records.stream()
                .map(TemporaryExpenseRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TemporaryExpenseRecord merged = new TemporaryExpenseRecord();
        merged.setJob(job);
        merged.setExpenseDate(earliestDate);
        merged.setDescription(mergedDescription);
        merged.setAmount(totalAmount);
        merged.setCategory(records.get(0).getCategory());

        merged = tempRepository.save(merged);
        tempRepository.deleteByIdIn(recordIds);

        logger.info("Merged {} records into record {} for job {}", recordIds.size(), merged.getId(), job.getId());
        return toTemporaryDTO(merged);
    }

    public void deleteTemporaryRecords(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return;
        }

        List<TemporaryExpenseRecord> records = tempRepository.findAllById(recordIds);
        if (!records.isEmpty()) {
            ExpenseInputJob job = records.get(0).getJob();
            if (job.getStatus() == ExpenseInputJob.Status.COMPLETED) {
                throw new IllegalStateException("Cannot delete records of a completed job");
            }
        }

        tempRepository.deleteByIdIn(recordIds);
        logger.info("Deleted {} temporary records", recordIds.size());
    }

    public void deleteJobs(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }
        List<ExpenseInputJob> jobs = jobRepository.findAllById(jobIds);
        for (ExpenseInputJob job : jobs) {
            List<TemporaryExpenseRecord> records = tempRepository.findByJobId(job.getId());
            boolean anyConfirmed = records.stream().anyMatch(TemporaryExpenseRecord::isConfirmed);

            for (TemporaryExpenseRecord record : records) {
                tempRepository.delete(record);
            }

            if (!anyConfirmed) {
                storageService.deleteObject(job.getFilePath());
            }

            jobRepository.delete(job);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 2000)
    public void processPendingJobs() {
        // Get jobs in INIT, PROCESSING, or RETRYABLE status
        List<ExpenseInputJob> initJobs = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.UPLOADED);
        List<ExpenseInputJob> processingJobs = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.PROCESSING);
        List<ExpenseInputJob> retryableJobs = jobRepository.findByStatusOrderByCreatedAtAsc(ExpenseInputJob.Status.RETRYABLE);

        List<ExpenseInputJob> jobs = new ArrayList<>();
        jobs.addAll(initJobs);
        jobs.addAll(processingJobs);
        jobs.addAll(retryableJobs);

        if (jobs.isEmpty()) {
            return;
        }

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        if (categories.isEmpty()) {
            logger.warn("No categories found; skipping OCR processing");
            return;
        }

        for (ExpenseInputJob job : jobs) {
            // Skip if already has records (avoid duplicate processing)
            List<TemporaryExpenseRecord> existingRecords = tempRepository.findByJobId(job.getId());
            if (!existingRecords.isEmpty()) {
                continue;
            }

            // Transition from INIT or RETRYABLE to PROCESSING
            if (job.getStatus() == ExpenseInputJob.Status.UPLOADED ||
                job.getStatus() == ExpenseInputJob.Status.RETRYABLE) {
                job.setStatus(ExpenseInputJob.Status.PROCESSING);
                jobRepository.save(job);
            }

            String objectKey = job.getFilePath();
            byte[] fileBytes = storageService.getObject(objectKey);
            if (fileBytes == null) {
                logger.error("File not found in storage for job {}: {}", job.getId(), objectKey);
                job.setStatus(ExpenseInputJob.Status.FAILED);
                job.setErrorMessage("Uploaded file not found");
                jobRepository.save(job);
                continue;
            }

            try {
                OcrProcessorClient.OcrResult result = ocrProcessorClient.processReceipt(
                        fileBytes, job.getOriginalFilename(), categories, job.getDefaultCategory());

                if (result.isSuccess()) {
                    Map<Long, Category> categoryMap = categories.stream()
                            .collect(Collectors.toMap(Category::getId, c -> c));

                    for (var ocrExpense : result.getResponse().getExpenses()) {
                        TemporaryExpenseRecord record = new TemporaryExpenseRecord();
                        record.setJob(job);
                        record.setAmount(ocrExpense.getAmount() != null
                                ? ocrExpense.getAmount()
                                : BigDecimal.ZERO);
                        record.setDescription(ocrExpense.getDescription() != null
                                ? ocrExpense.getDescription()
                                : "Receipt scan");
                        record.setExpenseDate(ocrExpense.getExpenseDate() != null
                                ? ocrExpense.getExpenseDate()
                                : LocalDate.now());

                        if (ocrExpense.getCategoryId() != null) {
                            Category cat = categoryMap.get(ocrExpense.getCategoryId());
                            if (cat != null) {
                                record.setCategory(cat);
                            }
                        }
                        // Fall back to job's default category if OCR didn't extract one
                        if (record.getCategory() == null && job.getDefaultCategory() != null) {
                            record.setCategory(job.getDefaultCategory());
                        }

                        tempRepository.save(record);
                    }

                    job.setStatus(ExpenseInputJob.Status.PROCESSED);
                    job.setErrorMessage(null); // Clear any previous error
                    jobRepository.save(job);
                    logger.info("Job {} processed with {} expense(s)", job.getId(),
                            result.getResponse().getExpenses().size());

                } else if (result.isRetryable()) {
                    // Increment retry count
                    job.setRetryCount(job.getRetryCount() + 1);

                    if (job.getRetryCount() >= MAX_RETRIES) {
                        // Max retries reached, mark as FAILED
                        logger.error("Job {} failed after {} retries: {}",
                                job.getId(), MAX_RETRIES, result.getErrorMessage());
                        job.setStatus(ExpenseInputJob.Status.FAILED);
                        job.setErrorMessage(String.format("Failed after %d retries: %s",
                                MAX_RETRIES, result.getErrorMessage()));
                    } else {
                        // Mark as RETRYABLE to retry later
                        logger.warn("Retryable error for job {} (attempt {}/{}): {}",
                                job.getId(), job.getRetryCount(), MAX_RETRIES, result.getErrorMessage());
                        job.setStatus(ExpenseInputJob.Status.RETRYABLE);
                        job.setErrorMessage(String.format("Attempt %d/%d failed: %s",
                                job.getRetryCount(), MAX_RETRIES, result.getErrorMessage()));
                    }
                    jobRepository.save(job);

                } else {
                    // Non-retryable error, mark as FAILED immediately
                    logger.error("Non-retryable error for job {}: {}", job.getId(), result.getErrorMessage());
                    job.setStatus(ExpenseInputJob.Status.FAILED);
                    job.setErrorMessage(result.getErrorMessage());
                    jobRepository.save(job);
                }

            } catch (Exception e) {
                // Unexpected exception - treat as retryable
                logger.error("Failed to process job {}", job.getId(), e);

                job.setRetryCount(job.getRetryCount() + 1);

                if (job.getRetryCount() >= MAX_RETRIES) {
                    job.setStatus(ExpenseInputJob.Status.FAILED);
                    job.setErrorMessage(String.format("Failed after %d retries: %s",
                            MAX_RETRIES, e.getMessage()));
                } else {
                    job.setStatus(ExpenseInputJob.Status.RETRYABLE);
                    job.setErrorMessage(String.format("Attempt %d/%d failed: %s",
                            job.getRetryCount(), MAX_RETRIES, e.getMessage()));
                }
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

        String sourceKey = job.getFilePath();
        if (storageService.objectExists(sourceKey)) {
            try {
                expenseService.attachExistingFile(expense, sourceKey, job.getOriginalFilename());
            } catch (RuntimeException e) {
                logger.warn("Failed to attach file for job {}. Continuing without file.", job.getId(), e);
                job.setErrorMessage("File attachment failed");
                jobRepository.save(job);
            }
        } else {
            logger.warn("Missing source file for job {} at {}. Continuing without file.", job.getId(), sourceKey);
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
            throw new IllegalArgumentException("File size exceeds 10MB limit");
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

    private ExpenseInputJobDTO toDTO(ExpenseInputJob job) {
        ExpenseInputJobDTO dto = new ExpenseInputJobDTO();
        dto.setId(job.getId());
        dto.setStatus(job.getStatus().name());
        dto.setRetryCount(job.getRetryCount());
        dto.setOriginalFilename(job.getOriginalFilename());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        if (job.getDefaultCategory() != null) {
            dto.setDefaultCategoryId(job.getDefaultCategory().getId());
        }

        List<TemporaryExpenseRecord> records = job.getTemporaryRecords();
        if (records == null || records.isEmpty()) {
            records = tempRepository.findByJobId(job.getId());
        }
        if (records != null && !records.isEmpty()) {
            dto.setTemporaryRecords(records.stream()
                    .map(this::toTemporaryDTO)
                    .collect(Collectors.toList()));
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
}
