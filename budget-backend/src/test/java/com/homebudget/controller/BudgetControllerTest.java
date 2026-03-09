package com.homebudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homebudget.config.AuthHeaderInterceptor;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.BudgetValidationDTO;
import com.homebudget.dto.YearlyBudgetViewDTO;
import com.homebudget.exception.BudgetNotFoundException;
import com.homebudget.exception.GlobalExceptionHandler;

import com.homebudget.service.BudgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BudgetController using @WebMvcTest.
 * Only the web layer is loaded; BudgetService is mocked.
 */
@WebMvcTest(BudgetController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    /**
     * MockBean for AuthHeaderInterceptor to avoid @Value resolution issues
     * in the sliced test context (it requires app.dev-mode and app.default-dev-user properties).
     */
    @MockBean
    private AuthHeaderInterceptor authHeaderInterceptor;

    // ---------------------------------------------------------------
    // Helper methods for building test DTOs
    // ---------------------------------------------------------------

    private BudgetDTO buildBudgetDTO() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2025);
        dto.setMonth(6);
        dto.setTotalAmount(new BigDecimal("1500.00"));
        dto.setDescription("Test budget");
        dto.setCategoryId(1L);
        return dto;
    }

    private BudgetDTO buildSavedBudgetDTO() {
        BudgetDTO dto = new BudgetDTO(
                1L, 2025, 6, new BigDecimal("1500.00"), "Test budget",
                "testuser", LocalDateTime.of(2025, 6, 1, 10, 0),
                LocalDateTime.of(2025, 6, 1, 10, 0), 0L
        );
        dto.setCategoryId(1L);
        return dto;
    }

    private BudgetSummaryDTO buildBudgetSummaryDTO() {
        return new BudgetSummaryDTO(
                1L, 2025, 6, new BigDecimal("1500.00"), "Test budget",
                "testuser", LocalDateTime.of(2025, 6, 1, 10, 0),
                LocalDateTime.of(2025, 6, 1, 10, 0), 0L,
                new BigDecimal("350.00"), new BigDecimal("23.33"), 5L
        );
    }

    // ---------------------------------------------------------------
    // POST /api/budgets
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/budgets - Create Budget")
    class CreateBudget {

        @Test
        @DisplayName("Should create budget and return 201 with X-Hass-User header")
        void createBudget_validBody_returns201() throws Exception {
            BudgetDTO requestDTO = buildBudgetDTO();
            BudgetDTO savedDTO = buildSavedBudgetDTO();

            when(budgetService.createBudget(any(BudgetDTO.class), eq("testuser")))
                    .thenReturn(savedDTO);

            mockMvc.perform(post("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Hass-User", "testuser")
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.month").value(6))
                    .andExpect(jsonPath("$.totalAmount").value(1500.00))
                    .andExpect(jsonPath("$.description").value("Test budget"))
                    .andExpect(jsonPath("$.createdBy").value("testuser"))
                    .andExpect(jsonPath("$.categoryId").value(1));

            verify(budgetService).createBudget(any(BudgetDTO.class), eq("testuser"));
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void createBudget_missingRequiredFields_returns400() throws Exception {
            // Empty DTO - missing year, totalAmount, categoryId
            BudgetDTO emptyDTO = new BudgetDTO();

            mockMvc.perform(post("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Hass-User", "testuser")
                            .content(objectMapper.writeValueAsString(emptyDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(budgetService);
        }

        @Test
        @DisplayName("Should return error when X-Hass-User header is missing")
        void createBudget_missingHassUserHeader_returnsError() throws Exception {
            BudgetDTO requestDTO = buildBudgetDTO();

            mockMvc.perform(post("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(budgetService);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets - List All Budgets")
    class GetAllBudgets {

        @Test
        @DisplayName("Should return list of budget summaries with 200")
        void getAllBudgets_returnsList() throws Exception {
            BudgetSummaryDTO summary = buildBudgetSummaryDTO();
            when(budgetService.getAllBudgets()).thenReturn(List.of(summary));

            mockMvc.perform(get("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].year").value(2025))
                    .andExpect(jsonPath("$[0].month").value(6))
                    .andExpect(jsonPath("$[0].totalAmount").value(1500.00))
                    .andExpect(jsonPath("$[0].totalSpending").value(350.00))
                    .andExpect(jsonPath("$[0].spendingPercentage").value(23.33))
                    .andExpect(jsonPath("$[0].expenseCount").value(5));

            verify(budgetService).getAllBudgets();
        }

        @Test
        @DisplayName("Should return empty list when no budgets exist")
        void getAllBudgets_empty_returnsEmptyList() throws Exception {
            when(budgetService.getAllBudgets()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/budgets")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(budgetService).getAllBudgets();
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets/{id}
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets/{id} - Get Budget By ID")
    class GetBudgetById {

        @Test
        @DisplayName("Should return budget summary when found")
        void getBudgetById_found_returns200() throws Exception {
            BudgetSummaryDTO summary = buildBudgetSummaryDTO();
            when(budgetService.getBudgetById(1L)).thenReturn(summary);

            mockMvc.perform(get("/api/budgets/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.month").value(6))
                    .andExpect(jsonPath("$.totalAmount").value(1500.00))
                    .andExpect(jsonPath("$.totalSpending").value(350.00))
                    .andExpect(jsonPath("$.expenseCount").value(5));

            verify(budgetService).getBudgetById(1L);
        }

        @Test
        @DisplayName("Should return 404 when budget not found")
        void getBudgetById_notFound_returns404() throws Exception {
            when(budgetService.getBudgetById(999L))
                    .thenThrow(new BudgetNotFoundException(999L));

            mockMvc.perform(get("/api/budgets/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Budget not found with id: 999"));

            verify(budgetService).getBudgetById(999L);
        }
    }

    // ---------------------------------------------------------------
    // PUT /api/budgets/{id}
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/budgets/{id} - Update Budget")
    class UpdateBudget {

        @Test
        @DisplayName("Should update budget and return 200")
        void updateBudget_valid_returns200() throws Exception {
            BudgetDTO requestDTO = buildBudgetDTO();
            requestDTO.setTotalAmount(new BigDecimal("2000.00"));
            requestDTO.setDescription("Updated budget");

            BudgetDTO updatedDTO = buildSavedBudgetDTO();
            updatedDTO.setTotalAmount(new BigDecimal("2000.00"));
            updatedDTO.setDescription("Updated budget");

            when(budgetService.updateBudget(eq(1L), any(BudgetDTO.class)))
                    .thenReturn(updatedDTO);

            mockMvc.perform(put("/api/budgets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.totalAmount").value(2000.00))
                    .andExpect(jsonPath("$.description").value("Updated budget"));

            verify(budgetService).updateBudget(eq(1L), any(BudgetDTO.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent budget")
        void updateBudget_notFound_returns404() throws Exception {
            BudgetDTO requestDTO = buildBudgetDTO();

            when(budgetService.updateBudget(eq(999L), any(BudgetDTO.class)))
                    .thenThrow(new BudgetNotFoundException(999L));

            mockMvc.perform(put("/api/budgets/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Budget not found with id: 999"));

            verify(budgetService).updateBudget(eq(999L), any(BudgetDTO.class));
        }

        @Test
        @DisplayName("Should return 400 when request body fails validation")
        void updateBudget_invalidBody_returns400() throws Exception {
            BudgetDTO invalidDTO = new BudgetDTO();
            // Missing required fields: year, totalAmount, categoryId

            mockMvc.perform(put("/api/budgets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verifyNoInteractions(budgetService);
        }
    }

    // ---------------------------------------------------------------
    // DELETE /api/budgets/{id}
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/budgets/{id} - Delete Budget")
    class DeleteBudget {

        @Test
        @DisplayName("Should delete budget and return 204 No Content")
        void deleteBudget_exists_returns204() throws Exception {
            doNothing().when(budgetService).deleteBudget(1L);

            mockMvc.perform(delete("/api/budgets/1"))
                    .andExpect(status().isNoContent());

            verify(budgetService).deleteBudget(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent budget")
        void deleteBudget_notFound_returns404() throws Exception {
            doThrow(new BudgetNotFoundException(999L))
                    .when(budgetService).deleteBudget(999L);

            mockMvc.perform(delete("/api/budgets/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Budget not found with id: 999"));

            verify(budgetService).deleteBudget(999L);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets/current
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets/current - Current Month Budget")
    class GetCurrentMonthBudget {

        @Test
        @DisplayName("Should return current month budget when found")
        void getCurrentMonthBudget_found_returns200() throws Exception {
            BudgetSummaryDTO summary = buildBudgetSummaryDTO();
            when(budgetService.getCurrentMonthBudget()).thenReturn(summary);

            mockMvc.perform(get("/api/budgets/current")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.month").value(6))
                    .andExpect(jsonPath("$.totalAmount").value(1500.00));

            verify(budgetService).getCurrentMonthBudget();
        }

        @Test
        @DisplayName("Should return 404 when no current month budget exists")
        void getCurrentMonthBudget_null_returns404() throws Exception {
            when(budgetService.getCurrentMonthBudget()).thenReturn(null);

            mockMvc.perform(get("/api/budgets/current")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(budgetService).getCurrentMonthBudget();
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets/monthly-summary
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets/monthly-summary - Monthly Budget Summary")
    class GetMonthlyBudgetSummary {

        @Test
        @DisplayName("Should return monthly budget summary when found")
        void getMonthlyBudgetSummary_found_returns200() throws Exception {
            BudgetSummaryDTO summary = buildBudgetSummaryDTO();
            when(budgetService.getMonthBudgetSummary(2025, 6)).thenReturn(summary);

            mockMvc.perform(get("/api/budgets/monthly-summary")
                            .param("year", "2025")
                            .param("month", "6")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.month").value(6))
                    .andExpect(jsonPath("$.totalAmount").value(1500.00))
                    .andExpect(jsonPath("$.totalSpending").value(350.00));

            verify(budgetService).getMonthBudgetSummary(2025, 6);
        }

        @Test
        @DisplayName("Should return 404 when no monthly summary exists")
        void getMonthlyBudgetSummary_null_returns404() throws Exception {
            when(budgetService.getMonthBudgetSummary(2025, 1)).thenReturn(null);

            mockMvc.perform(get("/api/budgets/monthly-summary")
                            .param("year", "2025")
                            .param("month", "1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(budgetService).getMonthBudgetSummary(2025, 1);
        }

        @Test
        @DisplayName("Should return error when required params are missing")
        void getMonthlyBudgetSummary_missingParams_returnsError() throws Exception {
            mockMvc.perform(get("/api/budgets/monthly-summary")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(budgetService);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets/validation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets/validation - Budget Validation Hints")
    class GetBudgetValidation {

        @Test
        @DisplayName("Should return validation hints for category/year/month")
        void getBudgetValidation_withMonth_returns200() throws Exception {
            BudgetValidationDTO validation = new BudgetValidationDTO();
            validation.setDuplicate(false);
            validation.setParentBudgetExists(true);
            validation.setParentBudgetId(10L);
            validation.setParentBudgetAmount(new BigDecimal("12000.00"));
            validation.setMonthlyBudgetSum(new BigDecimal("3000.00"));
            validation.setMonthlyBudgetsExist(true);

            when(budgetService.getBudgetValidation(1L, 2025, 6)).thenReturn(validation);

            mockMvc.perform(get("/api/budgets/validation")
                            .param("categoryId", "1")
                            .param("year", "2025")
                            .param("month", "6")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.duplicate").value(false))
                    .andExpect(jsonPath("$.parentBudgetExists").value(true))
                    .andExpect(jsonPath("$.parentBudgetId").value(10))
                    .andExpect(jsonPath("$.parentBudgetAmount").value(12000.00))
                    .andExpect(jsonPath("$.monthlyBudgetSum").value(3000.00))
                    .andExpect(jsonPath("$.monthlyBudgetsExist").value(true));

            verify(budgetService).getBudgetValidation(1L, 2025, 6);
        }

        @Test
        @DisplayName("Should return validation hints without month (yearly budget)")
        void getBudgetValidation_withoutMonth_returns200() throws Exception {
            BudgetValidationDTO validation = new BudgetValidationDTO();
            validation.setDuplicate(true);
            validation.setDuplicateMessage("Yearly budget already exists for category 'Food' in 2025");
            validation.setParentBudgetExists(false);
            validation.setMonthlyBudgetsExist(false);
            validation.setMonthlyBudgetSum(BigDecimal.ZERO);

            when(budgetService.getBudgetValidation(1L, 2025, null)).thenReturn(validation);

            mockMvc.perform(get("/api/budgets/validation")
                            .param("categoryId", "1")
                            .param("year", "2025")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.duplicate").value(true))
                    .andExpect(jsonPath("$.duplicateMessage").value(
                            "Yearly budget already exists for category 'Food' in 2025"))
                    .andExpect(jsonPath("$.parentBudgetExists").value(false))
                    .andExpect(jsonPath("$.monthlyBudgetsExist").value(false));

            verify(budgetService).getBudgetValidation(1L, 2025, null);
        }

        @Test
        @DisplayName("Should return error when required params are missing")
        void getBudgetValidation_missingParams_returnsError() throws Exception {
            mockMvc.perform(get("/api/budgets/validation")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(budgetService);
        }
    }

    // ---------------------------------------------------------------
    // GET /api/budgets/yearly
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/budgets/yearly - Yearly Budget View")
    class GetYearlyBudgetView {

        @Test
        @DisplayName("Should return yearly budget view for given year")
        void getYearlyBudgetView_returns200() throws Exception {
            YearlyBudgetViewDTO view = new YearlyBudgetViewDTO();
            view.setYear(2025);
            view.setTotalBudget(new BigDecimal("50000.00"));
            view.setTotalSpending(new BigDecimal("15000.00"));
            view.setTotalRemaining(new BigDecimal("35000.00"));

            when(budgetService.getYearlyBudgetView(2025)).thenReturn(view);

            mockMvc.perform(get("/api/budgets/yearly")
                            .param("year", "2025")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.year").value(2025))
                    .andExpect(jsonPath("$.totalBudget").value(50000.00))
                    .andExpect(jsonPath("$.totalSpending").value(15000.00))
                    .andExpect(jsonPath("$.totalRemaining").value(35000.00))
                    .andExpect(jsonPath("$.categories").isArray());

            verify(budgetService).getYearlyBudgetView(2025);
        }

        @Test
        @DisplayName("Should return error when year param is missing")
        void getYearlyBudgetView_missingYear_returnsError() throws Exception {
            mockMvc.perform(get("/api/budgets/yearly")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(budgetService);
        }
    }
}
