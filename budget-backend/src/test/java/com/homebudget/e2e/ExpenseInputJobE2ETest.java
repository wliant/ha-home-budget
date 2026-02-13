package com.homebudget.e2e;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.model.ExpenseInputJob;
import com.homebudget.model.TemporaryExpenseRecord;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseInputJobRepository;
import com.homebudget.repository.ExpenseRepository;
import com.homebudget.repository.TemporaryExpenseRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseInputJobE2ETest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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

    private Number categoryId;
    private Number budgetId;

    @BeforeEach
    void cleanUp() {
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

        // Create test category via API
        CategoryDTO catDTO = new CategoryDTO();
        catDTO.setName("TestCategory");
        catDTO.setIcon("📦");
        ResponseEntity<Map> catResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(catDTO, jsonHeaders()), Map.class);
        categoryId = (Number) catResponse.getBody().get("id");

        // Create yearly parent budget
        BudgetDTO yearlyDTO = new BudgetDTO();
        yearlyDTO.setYear(2026);
        yearlyDTO.setTotalAmount(new BigDecimal("50000.00"));
        yearlyDTO.setCategoryId(categoryId.longValue());
        restTemplate.exchange("/api/budgets", HttpMethod.POST, new HttpEntity<>(yearlyDTO, jsonHeaders()), Map.class);

        // Create monthly budget
        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setYear(2026);
        budgetDTO.setMonth(2);
        budgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        budgetDTO.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> budgetResponse = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(budgetDTO, jsonHeaders()), Map.class);
        budgetId = (Number) budgetResponse.getBody().get("id");
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders multipartHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    @Test
    @DisplayName("POST /api/expense-input-jobs - should upload file and create job")
    void createJobsWithFileUpload() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createFileResource("receipt.pdf", "application/pdf", "pdf content"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expense-input-jobs",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) response.getBody().get(0);
        assertThat(job.get("id")).isNotNull();
        assertThat(job.get("originalFilename")).isEqualTo("receipt.pdf");
        assertThat(job.get("status")).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("GET /api/expense-input-jobs - should list jobs")
    void getJobs() {
        // Upload a file first
        uploadFile("receipt.pdf", "application/pdf", "content");

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expense-input-jobs",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("PATCH /api/expense-input-jobs/{jobId}/temporary-record - should update temp record")
    void updateTemporaryRecord() {
        // Upload and create a temporary record manually
        Long jobId = uploadAndCreateTempRecord();

        UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
        request.setAmount(new BigDecimal("75.50"));
        request.setDescription("Updated via E2E");
        request.setExpenseDate(LocalDate.of(2026, 2, 15));
        request.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expense-input-jobs/" + jobId + "/temporary-record",
                HttpMethod.PATCH,
                new HttpEntity<>(request, jsonHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(((Number) response.getBody().get("amount")).doubleValue()).isEqualTo(75.50);
        assertThat(response.getBody().get("description")).isEqualTo("Updated via E2E");
    }

    @Test
    @DisplayName("POST /api/expense-input-jobs/confirm - should confirm jobs and create expenses")
    void confirmJobs() throws Exception {
        // Upload and create temp record
        Long jobId = uploadAndCreateTempRecord();

        // Update temp record with proper data for confirming
        UpdateTemporaryExpenseRecordRequest updateReq = new UpdateTemporaryExpenseRecordRequest();
        updateReq.setAmount(new BigDecimal("50.00"));
        updateReq.setDescription("Confirmed expense");
        updateReq.setExpenseDate(LocalDate.of(2026, 2, 15));
        updateReq.setCategoryId(categoryId.longValue());
        restTemplate.exchange(
                "/api/expense-input-jobs/" + jobId + "/temporary-record",
                HttpMethod.PATCH,
                new HttpEntity<>(updateReq, jsonHeaders()),
                Map.class);

        // Confirm - use String response type to handle potential error bodies
        ConfirmExpenseInputJobsRequest confirmReq = new ConfirmExpenseInputJobsRequest();
        confirmReq.setJobIds(List.of(jobId));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/expense-input-jobs/confirm",
                HttpMethod.POST,
                new HttpEntity<>(confirmReq, jsonHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Parse the list of confirmed job IDs
        ObjectMapper mapper = new ObjectMapper();
        List<Long> confirmed = mapper.readValue(response.getBody(), new TypeReference<List<Long>>() {});
        assertThat(confirmed).containsExactly(jobId);

        // Verify expense was created
        ResponseEntity<List> expenses = restTemplate.exchange(
                "/api/expenses", HttpMethod.GET, new HttpEntity<>(jsonHeaders()), List.class);
        assertThat(expenses.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("DELETE /api/expense-input-jobs - should delete jobs")
    void deleteJobs() {
        Long jobId = uploadAndGetJobId();

        ConfirmExpenseInputJobsRequest deleteReq = new ConfirmExpenseInputJobsRequest();
        deleteReq.setJobIds(List.of(jobId));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/expense-input-jobs",
                HttpMethod.DELETE,
                new HttpEntity<>(deleteReq, jsonHeaders()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify job is gone
        ResponseEntity<List> getResponse = restTemplate.exchange(
                "/api/expense-input-jobs",
                HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()),
                List.class);
        assertThat(getResponse.getBody()).isEmpty();
    }

    @Test
    @DisplayName("Full workflow: upload → list → update → confirm → verify expense")
    void fullBulkUploadWorkflow() throws Exception {
        // Step 1: Upload file
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createFileResource("receipt.png", "image/png", "png data"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<List> uploadResp = restTemplate.exchange(
                "/api/expense-input-jobs", HttpMethod.POST,
                new HttpEntity<>(body, headers), List.class);
        assertThat(uploadResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadedJob = (Map<String, Object>) uploadResp.getBody().get(0);
        Long jobId = ((Number) uploadedJob.get("id")).longValue();

        // Step 2: List jobs
        ResponseEntity<List> listResp = restTemplate.exchange(
                "/api/expense-input-jobs", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), List.class);
        assertThat(listResp.getBody()).hasSize(1);

        // Step 3: Create temp record manually (simulating processPendingJobs)
        ExpenseInputJob job = jobRepository.findById(jobId).orElseThrow();
        TemporaryExpenseRecord record = new TemporaryExpenseRecord();
        record.setJob(job);
        record.setAmount(new BigDecimal("30.00"));
        record.setDescription("Auto-parsed");
        record.setExpenseDate(LocalDate.of(2026, 2, 10));
        record.setCategory(categoryRepository.findById(categoryId.longValue()).orElseThrow());
        tempRepository.save(record);
        tempRepository.flush();

        // Step 4: Update temp record
        UpdateTemporaryExpenseRecordRequest updateReq = new UpdateTemporaryExpenseRecordRequest();
        updateReq.setAmount(new BigDecimal("42.00"));
        updateReq.setDescription("Corrected amount");
        updateReq.setExpenseDate(LocalDate.of(2026, 2, 12));
        updateReq.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> updateResp = restTemplate.exchange(
                "/api/expense-input-jobs/" + jobId + "/temporary-record",
                HttpMethod.PATCH,
                new HttpEntity<>(updateReq, jsonHeaders()), Map.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) updateResp.getBody().get("amount")).doubleValue()).isEqualTo(42.0);

        // Step 5: Confirm
        ConfirmExpenseInputJobsRequest confirmReq = new ConfirmExpenseInputJobsRequest();
        confirmReq.setJobIds(List.of(jobId));

        ResponseEntity<String> confirmResp = restTemplate.exchange(
                "/api/expense-input-jobs/confirm", HttpMethod.POST,
                new HttpEntity<>(confirmReq, jsonHeaders()), String.class);
        assertThat(confirmResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ObjectMapper mapper = new ObjectMapper();
        List<Long> confirmedIds = mapper.readValue(confirmResp.getBody(), new TypeReference<List<Long>>() {});
        assertThat(confirmedIds).containsExactly(jobId);

        // Step 6: Verify expense exists
        ResponseEntity<List> expensesResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders()), List.class);
        assertThat(expensesResp.getBody()).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> expense = (Map<String, Object>) expensesResp.getBody().get(0);
        assertThat(expense.get("description")).isEqualTo("Corrected amount");
        assertThat(((Number) expense.get("amount")).doubleValue()).isEqualTo(42.0);
    }

    // --- Helper Methods ---

    private ByteArrayResource createFileResource(String filename, String contentType, String content) {
        return new ByteArrayResource(content.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private void uploadFile(String filename, String contentType, String content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createFileResource(filename, contentType, content));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        restTemplate.exchange("/api/expense-input-jobs", HttpMethod.POST,
                new HttpEntity<>(body, headers), List.class);
    }

    private Long uploadAndGetJobId() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createFileResource("receipt.pdf", "application/pdf", "content"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expense-input-jobs", HttpMethod.POST,
                new HttpEntity<>(body, headers), List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) response.getBody().get(0);
        return ((Number) job.get("id")).longValue();
    }

    private Long uploadAndCreateTempRecord() {
        Long jobId = uploadAndGetJobId();

        // Create temporary record manually (simulating what processPendingJobs does)
        ExpenseInputJob job = jobRepository.findById(jobId).orElseThrow();
        TemporaryExpenseRecord record = new TemporaryExpenseRecord();
        record.setJob(job);
        record.setAmount(new BigDecimal("25.00"));
        record.setDescription("Auto-parsed receipt");
        record.setExpenseDate(LocalDate.of(2026, 2, 15));
        record.setCategory(categoryRepository.findById(categoryId.longValue()).orElseThrow());
        tempRepository.save(record);
        tempRepository.flush();

        return jobId;
    }
}
