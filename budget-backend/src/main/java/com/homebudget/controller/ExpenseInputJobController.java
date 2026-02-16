package com.homebudget.controller;

import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseInputJobDTO;
import com.homebudget.dto.RecordIdsRequest;
import com.homebudget.dto.TemporaryExpenseRecordDTO;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.service.ExpenseInputJobService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expense-input-jobs")
public class ExpenseInputJobController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseInputJobController.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";

    private final ExpenseInputJobService jobService;

    public ExpenseInputJobController(ExpenseInputJobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ExpenseInputJobDTO>> createJobs(
            @RequestPart("files") MultipartFile[] files,
            @RequestPart(value = "categoryId", required = false) String categoryId,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expense-input-jobs - {} files, categoryId={}", files.length, categoryId);
        Long parsedCategoryId = null;
        if (categoryId != null && !categoryId.isBlank()) {
            parsedCategoryId = Long.parseLong(categoryId);
        }
        List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(files), username, parsedCategoryId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseInputJobDTO>> getJobs() {
        return ResponseEntity.ok(jobService.getJobs());
    }

    @PatchMapping("/temporary-records/{recordId}")
    public ResponseEntity<TemporaryExpenseRecordDTO> updateTemporaryRecord(
            @PathVariable Long recordId,
            @Valid @RequestBody UpdateTemporaryExpenseRecordRequest request) {

        TemporaryExpenseRecordDTO updated = jobService.updateTemporaryRecord(recordId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/temporary-records/merge")
    public ResponseEntity<TemporaryExpenseRecordDTO> mergeRecords(
            @RequestBody RecordIdsRequest request) {

        logger.info("POST /api/expense-input-jobs/temporary-records/merge - {} records", request.getRecordIds().size());
        TemporaryExpenseRecordDTO merged = jobService.mergeTemporaryRecords(request.getRecordIds());
        return ResponseEntity.ok(merged);
    }

    @DeleteMapping("/temporary-records")
    public ResponseEntity<Void> deleteRecords(@RequestBody RecordIdsRequest request) {
        logger.info("DELETE /api/expense-input-jobs/temporary-records - {} records", request.getRecordIds().size());
        jobService.deleteTemporaryRecords(request.getRecordIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/complete")
    public ResponseEntity<ExpenseInputJobDTO> completeJob(
            @PathVariable Long jobId,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expense-input-jobs/{}/complete by {}", jobId, username);
        ExpenseInputJobDTO dto = jobService.completeJob(jobId, username);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ExpenseInputJobDTO> retryJob(@PathVariable Long jobId) {
        logger.info("POST /api/expense-input-jobs/{}/retry", jobId);
        ExpenseInputJobDTO dto = jobService.retryJob(jobId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteJobs(@RequestBody ConfirmExpenseInputJobsRequest request) {
        jobService.deleteJobs(request.getJobIds());
        return ResponseEntity.noContent().build();
    }
}
