package com.homebudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homebudget.dto.ConfirmExpenseInputJobsRequest;
import com.homebudget.dto.ExpenseInputJobDTO;
import com.homebudget.dto.TemporaryExpenseRecordDTO;
import com.homebudget.dto.UpdateTemporaryExpenseRecordRequest;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.exception.GlobalExceptionHandler;
import com.homebudget.service.ExpenseInputJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseInputJobController.class)
@Import(GlobalExceptionHandler.class)
class ExpenseInputJobControllerTest {

    private static final String BASE_URL = "/api/expense-input-jobs";
    private static final String HASS_USER_HEADER = "X-Hass-User";
    private static final String TEST_USER = "testuser";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseInputJobService jobService;

    // --- Helper methods ---

    private ExpenseInputJobDTO createJobDTO(Long id, String status, String filename) {
        ExpenseInputJobDTO dto = new ExpenseInputJobDTO();
        dto.setId(id);
        dto.setStatus(status);
        dto.setOriginalFilename(filename);
        dto.setCreatedAt(LocalDateTime.of(2026, 2, 11, 10, 0, 0));
        dto.setUpdatedAt(LocalDateTime.of(2026, 2, 11, 10, 0, 1));
        return dto;
    }

    private ExpenseInputJobDTO createJobDTOWithRecord(Long id, String status, String filename) {
        ExpenseInputJobDTO dto = createJobDTO(id, status, filename);
        TemporaryExpenseRecordDTO record = new TemporaryExpenseRecordDTO();
        record.setId(100L);
        record.setAmount(new BigDecimal("25.50"));
        record.setDescription("Store purchase");
        record.setExpenseDate(LocalDate.of(2026, 2, 10));
        record.setCategoryId(5L);
        record.setCategoryName("Groceries");
        record.setCategoryIcon("shopping_cart");
        record.setConfirmed(false);
        dto.setTemporaryRecords(List.of(record));
        return dto;
    }

    private TemporaryExpenseRecordDTO createTemporaryRecordDTO() {
        TemporaryExpenseRecordDTO dto = new TemporaryExpenseRecordDTO();
        dto.setId(100L);
        dto.setAmount(new BigDecimal("42.99"));
        dto.setDescription("Updated description");
        dto.setExpenseDate(LocalDate.of(2026, 1, 15));
        dto.setCategoryId(3L);
        dto.setCategoryName("Utilities");
        dto.setCategoryIcon("bolt");
        dto.setConfirmed(false);
        return dto;
    }

    // --- Nested test classes for each endpoint ---

    @Nested
    @DisplayName("POST /api/expense-input-jobs - Create Jobs (Multipart Upload)")
    class CreateJobs {

        @Test
        @DisplayName("Should upload a single file and return created job")
        void shouldUploadSingleFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "receipt.pdf", "application/pdf", "pdf-content".getBytes());

            ExpenseInputJobDTO jobDTO = createJobDTO(1L, "PROCESSING", "receipt.pdf");

            when(jobService.createJobs(any(), eq(TEST_USER), any()))
                    .thenReturn(List.of(jobDTO));

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .header(HASS_USER_HEADER, TEST_USER))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].status").value("PROCESSING"))
                    .andExpect(jsonPath("$[0].originalFilename").value("receipt.pdf"));

            verify(jobService).createJobs(any(), eq(TEST_USER), any());
        }

        @Test
        @DisplayName("Should upload multiple files and return created jobs")
        void shouldUploadMultipleFiles() throws Exception {
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "receipt1.pdf", "application/pdf", "pdf-content-1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "receipt2.png", "image/png", "image-content".getBytes());

            ExpenseInputJobDTO job1 = createJobDTO(1L, "PROCESSING", "receipt1.pdf");
            ExpenseInputJobDTO job2 = createJobDTO(2L, "PROCESSING", "receipt2.png");

            when(jobService.createJobs(any(), eq(TEST_USER), any()))
                    .thenReturn(List.of(job1, job2));

            mockMvc.perform(multipart(BASE_URL)
                            .file(file1)
                            .file(file2)
                            .header(HASS_USER_HEADER, TEST_USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].originalFilename").value("receipt1.pdf"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].originalFilename").value("receipt2.png"));
        }

        @Test
        @DisplayName("Should return job with temporary record when already processed")
        void shouldReturnJobWithTemporaryRecord() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "receipt.jpg", "image/jpeg", "jpeg-content".getBytes());

            ExpenseInputJobDTO jobDTO = createJobDTOWithRecord(1L, "COMPLETED", "receipt.jpg");

            when(jobService.createJobs(any(), eq(TEST_USER), any()))
                    .thenReturn(List.of(jobDTO));

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .header(HASS_USER_HEADER, TEST_USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].temporaryRecords[0]").exists())
                    .andExpect(jsonPath("$[0].temporaryRecords[0].id").value(100))
                    .andExpect(jsonPath("$[0].temporaryRecords[0].amount").value(25.50))
                    .andExpect(jsonPath("$[0].temporaryRecords[0].description").value("Store purchase"))
                    .andExpect(jsonPath("$[0].temporaryRecords[0].categoryName").value("Groceries"));
        }

        @Test
        @DisplayName("Should return 400 when service throws IllegalArgumentException")
        void shouldReturn400WhenServiceThrowsIllegalArgument() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "bad.txt", "text/plain", "not-an-image".getBytes());

            when(jobService.createJobs(any(), eq(TEST_USER), any()))
                    .thenThrow(new IllegalArgumentException("Only PDF and image files are allowed"));

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .header(HASS_USER_HEADER, TEST_USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Only PDF and image files are allowed"));
        }

        @Test
        @DisplayName("Should return error when missing X-Hass-User header")
        void shouldReturnErrorWhenMissingHassUserHeader() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "receipt.pdf", "application/pdf", "pdf-content".getBytes());

            mockMvc.perform(multipart(BASE_URL)
                            .file(file))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(jobService);
        }
    }

    @Nested
    @DisplayName("GET /api/expense-input-jobs - List Jobs")
    class GetJobs {

        @Test
        @DisplayName("Should return list of jobs")
        void shouldReturnListOfJobs() throws Exception {
            ExpenseInputJobDTO job1 = createJobDTO(1L, "COMPLETED", "receipt1.pdf");
            ExpenseInputJobDTO job2 = createJobDTOWithRecord(2L, "COMPLETED", "receipt2.png");

            when(jobService.getJobs()).thenReturn(List.of(job1, job2));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$[0].originalFilename").value("receipt1.pdf"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].temporaryRecords[0]").exists())
                    .andExpect(jsonPath("$[1].temporaryRecords[0].amount").value(25.50));

            verify(jobService).getJobs();
        }

        @Test
        @DisplayName("Should return empty list when no jobs exist")
        void shouldReturnEmptyList() throws Exception {
            when(jobService.getJobs()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(jobService).getJobs();
        }

        @Test
        @DisplayName("Should return jobs with error messages")
        void shouldReturnJobsWithErrorMessages() throws Exception {
            ExpenseInputJobDTO failedJob = createJobDTO(3L, "FAILED", "corrupt.pdf");
            failedJob.setErrorMessage("Failed to store file");

            when(jobService.getJobs()).thenReturn(List.of(failedJob));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("FAILED"))
                    .andExpect(jsonPath("$[0].errorMessage").value("Failed to store file"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/expense-input-jobs/{jobId}/temporary-record - Update Temporary Record")
    class UpdateTemporaryRecord {

        @Test
        @DisplayName("Should update temporary record successfully")
        void shouldUpdateTemporaryRecord() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("42.99"));
            request.setDescription("Updated description");
            request.setExpenseDate(LocalDate.of(2026, 1, 15));
            request.setCategoryId(3L);

            TemporaryExpenseRecordDTO updatedRecord = createTemporaryRecordDTO();

            when(jobService.updateTemporaryRecord(eq(1L), any(UpdateTemporaryExpenseRecordRequest.class)))
                    .thenReturn(updatedRecord);

            mockMvc.perform(patch(BASE_URL + "/temporary-records/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.amount").value(42.99))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.expenseDate").value("2026-01-15"))
                    .andExpect(jsonPath("$.categoryId").value(3))
                    .andExpect(jsonPath("$.categoryName").value("Utilities"))
                    .andExpect(jsonPath("$.categoryIcon").value("bolt"))
                    .andExpect(jsonPath("$.confirmed").value(false));

            verify(jobService).updateTemporaryRecord(eq(1L), any(UpdateTemporaryExpenseRecordRequest.class));
        }

        @Test
        @DisplayName("Should update temporary record with null categoryId")
        void shouldUpdateTemporaryRecordWithNullCategory() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("No category");
            request.setExpenseDate(LocalDate.of(2026, 2, 1));
            request.setCategoryId(null);

            TemporaryExpenseRecordDTO updatedRecord = new TemporaryExpenseRecordDTO();
            updatedRecord.setId(101L);
            updatedRecord.setAmount(new BigDecimal("10.00"));
            updatedRecord.setDescription("No category");
            updatedRecord.setExpenseDate(LocalDate.of(2026, 2, 1));
            updatedRecord.setConfirmed(false);

            when(jobService.updateTemporaryRecord(eq(5L), any(UpdateTemporaryExpenseRecordRequest.class)))
                    .thenReturn(updatedRecord);

            mockMvc.perform(patch(BASE_URL + "/temporary-records/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(101))
                    .andExpect(jsonPath("$.categoryId").doesNotExist())
                    .andExpect(jsonPath("$.categoryName").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void shouldReturn404WhenJobNotFound() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("Some description");
            request.setExpenseDate(LocalDate.of(2026, 1, 1));

            when(jobService.updateTemporaryRecord(eq(999L), any(UpdateTemporaryExpenseRecordRequest.class)))
                    .thenThrow(new ExpenseNotFoundException(999L));

            mockMvc.perform(patch(BASE_URL + "/temporary-records/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Expense not found with ID: 999"));
        }

        @Test
        @DisplayName("Should return 400 when amount is null")
        void shouldReturn400WhenAmountIsNull() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(null);
            request.setDescription("Some description");
            request.setExpenseDate(LocalDate.of(2026, 1, 1));

            mockMvc.perform(patch(BASE_URL + "/temporary-records/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.amount").value("Amount is required"));

            verifyNoInteractions(jobService);
        }

        @Test
        @DisplayName("Should return 400 when description is blank")
        void shouldReturn400WhenDescriptionIsBlank() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("");
            request.setExpenseDate(LocalDate.of(2026, 1, 1));

            mockMvc.perform(patch(BASE_URL + "/temporary-records/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.description").value("Description is required"));

            verifyNoInteractions(jobService);
        }

        @Test
        @DisplayName("Should return 400 when expenseDate is null")
        void shouldReturn400WhenExpenseDateIsNull() throws Exception {
            UpdateTemporaryExpenseRecordRequest request = new UpdateTemporaryExpenseRecordRequest();
            request.setAmount(new BigDecimal("10.00"));
            request.setDescription("Valid description");
            request.setExpenseDate(null);

            mockMvc.perform(patch(BASE_URL + "/temporary-records/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.expenseDate").value("Expense date is required"));

            verifyNoInteractions(jobService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/expense-input-jobs - Delete Jobs")
    class DeleteJobs {

        @Test
        @DisplayName("Should delete jobs and return 204 No Content")
        void shouldDeleteJobsAndReturn204() throws Exception {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of(1L, 2L));

            doNothing().when(jobService).deleteJobs(List.of(1L, 2L));

            mockMvc.perform(delete(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(jobService).deleteJobs(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Should delete single job and return 204")
        void shouldDeleteSingleJobAndReturn204() throws Exception {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(List.of(5L));

            doNothing().when(jobService).deleteJobs(List.of(5L));

            mockMvc.perform(delete(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(jobService).deleteJobs(List.of(5L));
        }

        @Test
        @DisplayName("Should handle delete with empty job IDs list")
        void shouldHandleDeleteWithEmptyJobIds() throws Exception {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(Collections.emptyList());

            doNothing().when(jobService).deleteJobs(Collections.emptyList());

            mockMvc.perform(delete(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(jobService).deleteJobs(Collections.emptyList());
        }

        @Test
        @DisplayName("Should handle delete with null job IDs")
        void shouldHandleDeleteWithNullJobIds() throws Exception {
            ConfirmExpenseInputJobsRequest request = new ConfirmExpenseInputJobsRequest();
            request.setJobIds(null);

            doNothing().when(jobService).deleteJobs(null);

            mockMvc.perform(delete(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(jobService).deleteJobs(null);
        }
    }
}
