package com.homebudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseListResponse;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.exception.GlobalExceptionHandler;
import com.homebudget.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvcTest for ExpenseController.
 *
 * Tests all REST endpoints with mocked service layer.
 * Uses @WebMvcTest to load only the web layer (controller, filters, exception handlers).
 */
@WebMvcTest(ExpenseController.class)
@Import(GlobalExceptionHandler.class)
class ExpenseControllerTest {

    private static final String HASS_USER_HEADER = "X-Hass-User";
    private static final String TEST_USER = "testuser";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    /**
     * Helper to build a valid ExpenseDTO for request bodies.
     */
    private ExpenseDTO buildValidExpenseDTO() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("25.50"));
        dto.setDescription("Groceries");
        dto.setExpenseDate(LocalDate.of(2025, 3, 15));
        dto.setBudgetId(1L);
        dto.setCategoryId(10L);
        return dto;
    }

    /**
     * Helper to build a response ExpenseDTO (with id, timestamps, etc.).
     */
    private ExpenseDTO buildResponseExpenseDTO(Long id) {
        ExpenseDTO dto = new ExpenseDTO(
                id,
                new BigDecimal("25.50"),
                "Groceries",
                LocalDate.of(2025, 3, 15),
                1L,
                10L,
                TEST_USER,
                LocalDateTime.of(2025, 3, 15, 10, 0, 0),
                LocalDateTime.of(2025, 3, 15, 10, 0, 0),
                0L
        );
        dto.setCategoryName("Food");
        dto.setCategoryIcon("restaurant");
        return dto;
    }

    @Nested
    @DisplayName("POST /api/expenses")
    class CreateExpense {

        @Test
        @DisplayName("should create expense and return 201 with valid JSON body and X-Hass-User header")
        void createExpense_validBody_returns201() throws Exception {
            ExpenseDTO requestDto = buildValidExpenseDTO();
            ExpenseDTO responseDto = buildResponseExpenseDTO(1L);

            when(expenseService.createExpense(any(ExpenseDTO.class), eq(TEST_USER)))
                    .thenReturn(responseDto);

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.amount").value(25.50))
                    .andExpect(jsonPath("$.description").value("Groceries"))
                    .andExpect(jsonPath("$.budgetId").value(1))
                    .andExpect(jsonPath("$.categoryId").value(10))
                    .andExpect(jsonPath("$.categoryName").value("Food"))
                    .andExpect(jsonPath("$.createdBy").value(TEST_USER));

            verify(expenseService).createExpense(any(ExpenseDTO.class), eq(TEST_USER));
        }

        @Test
        @DisplayName("should return 400 when amount is missing")
        void createExpense_missingAmount_returns400() throws Exception {
            ExpenseDTO dto = buildValidExpenseDTO();
            dto.setAmount(null);

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.amount").value("Amount is required"));

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when description is blank")
        void createExpense_blankDescription_returns400() throws Exception {
            ExpenseDTO dto = buildValidExpenseDTO();
            dto.setDescription("");

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.description").value("Description is required"));

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when expenseDate is missing")
        void createExpense_missingDate_returns400() throws Exception {
            ExpenseDTO dto = buildValidExpenseDTO();
            dto.setExpenseDate(null);

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.expenseDate").value("Expense date is required"));

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when budgetId is missing")
        void createExpense_missingBudgetId_returns400() throws Exception {
            ExpenseDTO dto = buildValidExpenseDTO();
            dto.setBudgetId(null);

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.budgetId").value("Budget ID is required"));

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return error when X-Hass-User header is missing")
        void createExpense_missingHassUserHeader_returnsError() throws Exception {
            ExpenseDTO dto = buildValidExpenseDTO();

            mockMvc.perform(post("/api/expenses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(expenseService);
        }
    }

    @Nested
    @DisplayName("GET /api/expenses")
    class GetAllExpenses {

        @Test
        @DisplayName("should return all expenses with no filters")
        void getAllExpenses_noFilters_returnsOk() throws Exception {
            ExpenseDTO expense1 = buildResponseExpenseDTO(1L);
            ExpenseDTO expense2 = buildResponseExpenseDTO(2L);
            expense2.setDescription("Coffee");

            when(expenseService.getAllExpenses(isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of(expense1, expense2));

            mockMvc.perform(get("/api/expenses"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));

            verify(expenseService).getAllExpenses(isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("should pass budgetId filter to service")
        void getAllExpenses_withBudgetId_passesFilter() throws Exception {
            when(expenseService.getAllExpenses(eq(5L), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of(buildResponseExpenseDTO(1L)));

            mockMvc.perform(get("/api/expenses")
                            .param("budgetId", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(expenseService).getAllExpenses(eq(5L), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("should pass categoryId filter to service")
        void getAllExpenses_withCategoryId_passesFilter() throws Exception {
            when(expenseService.getAllExpenses(isNull(), eq(10L), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses")
                            .param("categoryId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(expenseService).getAllExpenses(isNull(), eq(10L), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("should pass date range filters to service")
        void getAllExpenses_withDateRange_passesFilter() throws Exception {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 1, 31);

            when(expenseService.getAllExpenses(isNull(), isNull(), eq(start), eq(end), isNull()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(expenseService).getAllExpenses(isNull(), isNull(), eq(start), eq(end), isNull());
        }

        @Test
        @DisplayName("should pass createdBy filter to service")
        void getAllExpenses_withCreatedBy_passesFilter() throws Exception {
            when(expenseService.getAllExpenses(isNull(), isNull(), isNull(), isNull(), eq("alice")))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses")
                            .param("createdBy", "alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(expenseService).getAllExpenses(isNull(), isNull(), isNull(), isNull(), eq("alice"));
        }

        @Test
        @DisplayName("should pass all filters to service when all provided")
        void getAllExpenses_withAllFilters_passesAllFilters() throws Exception {
            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);

            when(expenseService.getAllExpenses(eq(1L), eq(10L), eq(start), eq(end), eq("bob")))
                    .thenReturn(List.of(buildResponseExpenseDTO(1L)));

            mockMvc.perform(get("/api/expenses")
                            .param("budgetId", "1")
                            .param("categoryId", "10")
                            .param("startDate", "2025-03-01")
                            .param("endDate", "2025-03-31")
                            .param("createdBy", "bob"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(expenseService).getAllExpenses(eq(1L), eq(10L), eq(start), eq(end), eq("bob"));
        }

        @Test
        @DisplayName("should return empty list when no expenses found")
        void getAllExpenses_noResults_returnsEmptyList() throws Exception {
            when(expenseService.getAllExpenses(isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/list")
    class GetExpenseList {

        private ExpenseListResponse buildListResponse() {
            ExpenseDTO expense = buildResponseExpenseDTO(1L);
            return new ExpenseListResponse(
                    List.of(expense),
                    1L,
                    1,
                    0,
                    50,
                    new BigDecimal("25.50"),
                    "expenseDate",
                    "DESC"
            );
        }

        @Test
        @DisplayName("should return paginated expense list with required year param")
        void getExpenseList_withYear_returnsOk() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(50))
                    .andExpect(jsonPath("$.totalAmount").value(25.50))
                    .andExpect(jsonPath("$.sortBy").value("expenseDate"))
                    .andExpect(jsonPath("$.sortDirection").value("DESC"));
        }

        @Test
        @DisplayName("should return paginated list with year and month params")
        void getExpenseList_withYearAndMonth_returnsOk() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), eq(3), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("should return error when year param is missing")
        void getExpenseList_missingYear_returnsError() throws Exception {
            mockMvc.perform(get("/api/expenses/list"))
                    .andExpect(status().isInternalServerError());

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when month is 0 (below valid range)")
        void getExpenseList_monthZero_returns400() throws Exception {
            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when month is 13 (above valid range)")
        void getExpenseList_monthThirteen_returns400() throws Exception {
            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "13"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when month is negative")
        void getExpenseList_negativeMonth_returns400() throws Exception {
            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "-1"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should return 400 when minAmount exceeds maxAmount")
        void getExpenseList_minAmountExceedsMax_returns400() throws Exception {
            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("minAmount", "100.00")
                            .param("maxAmount", "50.00"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should fallback to expenseDate when invalid sortBy is provided")
        void getExpenseList_invalidSortBy_fallsBackToExpenseDate() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("sortBy", "invalidField"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortBy").value("expenseDate"));
        }

        @Test
        @DisplayName("should accept valid sortBy=description")
        void getExpenseList_validSortByDescription_returnsOk() throws Exception {
            ExpenseListResponse response = new ExpenseListResponse(
                    List.of(buildResponseExpenseDTO(1L)),
                    1L, 1, 0, 50,
                    new BigDecimal("25.50"),
                    "description", "ASC"
            );

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("description"), eq("ASC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("sortBy", "description")
                            .param("sortDirection", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortBy").value("description"));
        }

        @Test
        @DisplayName("should accept valid sortBy=amount")
        void getExpenseList_validSortByAmount_returnsOk() throws Exception {
            ExpenseListResponse response = new ExpenseListResponse(
                    List.of(buildResponseExpenseDTO(1L)),
                    1L, 1, 0, 50,
                    new BigDecimal("25.50"),
                    "amount", "DESC"
            );

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("amount"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("sortBy", "amount"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortBy").value("amount"));
        }

        @Test
        @DisplayName("should map categoryName sortBy to category.name entity field")
        void getExpenseList_categoryNameSortBy_mapsCorrectly() throws Exception {
            ExpenseListResponse response = new ExpenseListResponse(
                    List.of(buildResponseExpenseDTO(1L)),
                    1L, 1, 0, 50,
                    new BigDecimal("25.50"),
                    "categoryName", "ASC"
            );

            // The controller maps "categoryName" to "category.name" for the pageable,
            // but passes the original "categoryName" as sortBy to the service
            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("categoryName"), eq("ASC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("sortBy", "categoryName")
                            .param("sortDirection", "ASC"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should fallback to DESC when invalid sortDirection is provided")
        void getExpenseList_invalidSortDirection_fallsBackToDESC() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("sortDirection", "INVALID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortDirection").value("DESC"));
        }

        @Test
        @DisplayName("should pass all filter parameters to service")
        void getExpenseList_allFilters_passesAllToService() throws Exception {
            ExpenseListResponse response = new ExpenseListResponse(
                    Collections.emptyList(),
                    0L, 0, 0, 20,
                    BigDecimal.ZERO,
                    "amount", "ASC"
            );

            when(expenseService.getExpenseList(
                    eq(2025), eq(6), eq(10L),
                    eq(new BigDecimal("10.00")), eq(new BigDecimal("100.00")),
                    eq("alice"),
                    any(), eq("amount"), eq("ASC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "6")
                            .param("categoryId", "10")
                            .param("minAmount", "10.00")
                            .param("maxAmount", "100.00")
                            .param("createdBy", "alice")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sortBy", "amount")
                            .param("sortDirection", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.sortBy").value("amount"))
                    .andExpect(jsonPath("$.sortDirection").value("ASC"));
        }

        @Test
        @DisplayName("should accept month=1 (lower boundary)")
        void getExpenseList_monthOne_returnsOk() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), eq(1), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept month=12 (upper boundary)")
        void getExpenseList_monthTwelve_returnsOk() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), eq(12), isNull(), isNull(), isNull(), isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("month", "12"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should accept equal minAmount and maxAmount")
        void getExpenseList_equalMinMax_returnsOk() throws Exception {
            ExpenseListResponse response = buildListResponse();

            when(expenseService.getExpenseList(
                    eq(2025), isNull(), isNull(),
                    eq(new BigDecimal("50.00")), eq(new BigDecimal("50.00")),
                    isNull(),
                    any(), eq("expenseDate"), eq("DESC")))
                    .thenReturn(response);

            mockMvc.perform(get("/api/expenses/list")
                            .param("year", "2025")
                            .param("minAmount", "50.00")
                            .param("maxAmount", "50.00"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/years")
    class GetExpenseYears {

        @Test
        @DisplayName("should return list of distinct years")
        void getExpenseYears_returnsYears() throws Exception {
            when(expenseService.getDistinctYears()).thenReturn(List.of(2025, 2024, 2023));

            mockMvc.perform(get("/api/expenses/years"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0]").value(2025))
                    .andExpect(jsonPath("$[1]").value(2024))
                    .andExpect(jsonPath("$[2]").value(2023));

            verify(expenseService).getDistinctYears();
        }

        @Test
        @DisplayName("should return empty list when no years exist")
        void getExpenseYears_noData_returnsEmptyList() throws Exception {
            when(expenseService.getDistinctYears()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses/years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/creators")
    class GetExpenseCreators {

        @Test
        @DisplayName("should return list of distinct creators")
        void getExpenseCreators_returnsCreators() throws Exception {
            when(expenseService.getDistinctCreators()).thenReturn(List.of("alice", "bob"));

            mockMvc.perform(get("/api/expenses/creators"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0]").value("alice"))
                    .andExpect(jsonPath("$[1]").value("bob"));

            verify(expenseService).getDistinctCreators();
        }

        @Test
        @DisplayName("should return empty list when no creators exist")
        void getExpenseCreators_noData_returnsEmptyList() throws Exception {
            when(expenseService.getDistinctCreators()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/expenses/creators"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/{id}")
    class GetExpenseById {

        @Test
        @DisplayName("should return expense when found")
        void getExpenseById_found_returnsOk() throws Exception {
            ExpenseDTO response = buildResponseExpenseDTO(42L);

            when(expenseService.getExpenseById(42L)).thenReturn(response);

            mockMvc.perform(get("/api/expenses/42"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(42))
                    .andExpect(jsonPath("$.amount").value(25.50))
                    .andExpect(jsonPath("$.description").value("Groceries"))
                    .andExpect(jsonPath("$.budgetId").value(1))
                    .andExpect(jsonPath("$.categoryId").value(10))
                    .andExpect(jsonPath("$.categoryName").value("Food"))
                    .andExpect(jsonPath("$.categoryIcon").value("restaurant"))
                    .andExpect(jsonPath("$.createdBy").value(TEST_USER));

            verify(expenseService).getExpenseById(42L);
        }

        @Test
        @DisplayName("should return 404 when expense not found")
        void getExpenseById_notFound_returns404() throws Exception {
            when(expenseService.getExpenseById(999L))
                    .thenThrow(new ExpenseNotFoundException(999L));

            mockMvc.perform(get("/api/expenses/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Expense not found with ID: 999"));

            verify(expenseService).getExpenseById(999L);
        }
    }

    @Nested
    @DisplayName("PUT /api/expenses/{id}")
    class UpdateExpense {

        @Test
        @DisplayName("should update expense and return 200 with valid JSON body")
        void updateExpense_validBody_returnsOk() throws Exception {
            ExpenseDTO requestDto = buildValidExpenseDTO();
            requestDto.setDescription("Updated Groceries");
            requestDto.setAmount(new BigDecimal("30.00"));

            ExpenseDTO responseDto = buildResponseExpenseDTO(1L);
            responseDto.setDescription("Updated Groceries");
            responseDto.setAmount(new BigDecimal("30.00"));

            when(expenseService.updateExpense(eq(1L), any(ExpenseDTO.class)))
                    .thenReturn(responseDto);

            mockMvc.perform(put("/api/expenses/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.description").value("Updated Groceries"))
                    .andExpect(jsonPath("$.amount").value(30.00));

            verify(expenseService).updateExpense(eq(1L), any(ExpenseDTO.class));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent expense")
        void updateExpense_notFound_returns404() throws Exception {
            ExpenseDTO requestDto = buildValidExpenseDTO();

            when(expenseService.updateExpense(eq(999L), any(ExpenseDTO.class)))
                    .thenThrow(new ExpenseNotFoundException(999L));

            mockMvc.perform(put("/api/expenses/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Expense not found with ID: 999"));
        }

        @Test
        @DisplayName("should return 400 when update body has validation errors")
        void updateExpense_validationErrors_returns400() throws Exception {
            ExpenseDTO dto = new ExpenseDTO();
            // All required fields are missing

            mockMvc.perform(put("/api/expenses/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isNotEmpty());

            verifyNoInteractions(expenseService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/expenses/{id}")
    class DeleteExpense {

        @Test
        @DisplayName("should delete expense and return 204")
        void deleteExpense_found_returns204() throws Exception {
            doNothing().when(expenseService).deleteExpense(1L);

            mockMvc.perform(delete("/api/expenses/1"))
                    .andExpect(status().isNoContent());

            verify(expenseService).deleteExpense(1L);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent expense")
        void deleteExpense_notFound_returns404() throws Exception {
            doThrow(new ExpenseNotFoundException(999L)).when(expenseService).deleteExpense(999L);

            mockMvc.perform(delete("/api/expenses/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Expense not found with ID: 999"));

            verify(expenseService).deleteExpense(999L);
        }
    }
}
