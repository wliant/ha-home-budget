package com.homebudget.controller;

import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseInputJobDTO;
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
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expense-input-jobs - {} files", files.length);
        List<ExpenseInputJobDTO> jobs = jobService.createJobs(List.of(files), username);
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

    @PostMapping("/confirm")
    public ResponseEntity<List<Long>> confirmJobs(
            @RequestHeader(HASS_USER_HEADER) String username,
            @RequestBody ConfirmExpenseInputJobsRequest request) {

        List<Long> confirmed = jobService.confirmJobs(request, username);
        return ResponseEntity.ok(confirmed);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteJobs(@RequestBody ConfirmExpenseInputJobsRequest request) {
        jobService.deleteJobs(request.getJobIds());
        return ResponseEntity.noContent().build();
    }
}
