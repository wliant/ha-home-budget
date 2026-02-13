package com.homebudget.e2e;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseE2ETest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Number categoryId;
    private Number budgetId;

    @BeforeEach
    void cleanUp() {
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

        // Create a test category via API
        CategoryDTO catDTO = new CategoryDTO();
        catDTO.setName("TestCategory");
        catDTO.setIcon("📦");
        ResponseEntity<Map> catResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(catDTO, headers()), Map.class);
        categoryId = (Number) catResponse.getBody().get("id");

        // Create yearly parent budget (required before monthly budgets)
        BudgetDTO yearlyDTO = new BudgetDTO();
        yearlyDTO.setYear(2026);
        yearlyDTO.setTotalAmount(new BigDecimal("50000.00"));
        yearlyDTO.setCategoryId(categoryId.longValue());
        restTemplate.exchange("/api/budgets", HttpMethod.POST, new HttpEntity<>(yearlyDTO, headers()), Map.class);

        // Create a test monthly budget via API
        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setYear(2026);
        budgetDTO.setMonth(2);
        budgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        budgetDTO.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> budgetResponse = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(budgetDTO, headers()), Map.class);
        budgetId = (Number) budgetResponse.getBody().get("id");
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("POST /api/expenses - should create expense with 201")
    void createExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Weekly groceries");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        dto.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(((Number) response.getBody().get("amount")).doubleValue()).isEqualTo(50.0);
        assertThat(response.getBody().get("description")).isEqualTo("Weekly groceries");
        assertThat(response.getBody().get("createdBy")).isEqualTo("testuser");
        assertThat(response.getBody().get("budgetId")).isEqualTo(budgetId.intValue());
    }

    @Test
    @DisplayName("POST /api/expenses with date outside budget month - should return 201 with warnings")
    void createExpenseWithDateMismatch() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("March expense in Feb budget");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 3, 15)); // March, but budget is February
        dto.setBudgetId(budgetId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) response.getBody().get("warnings");
        assertThat(warnings).isNotEmpty();
        assertThat(warnings.get(0)).contains("Warning");
    }

    @Test
    @DisplayName("GET /api/expenses - should return expense list with 200")
    void getAllExpenses() {
        // Create expense
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Test expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/expenses?budgetId={id} - should return filtered expenses")
    void getExpensesByBudgetId() {
        // Create expense
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Test expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses?budgetId=" + budgetId, HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/expenses?startDate=X&endDate=Y - should return date-filtered expenses")
    void getExpensesByDateRange() {
        // Create expense on Feb 15
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Feb expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        // Query date range that includes Feb 15
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses?startDate=2026-02-10&endDate=2026-02-20",
                HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        // Query date range that excludes Feb 15
        ResponseEntity<List> emptyResponse = restTemplate.exchange(
                "/api/expenses?startDate=2026-02-20&endDate=2026-02-28",
                HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyResponse.getBody()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/expenses?createdBy=user - should return user-filtered expenses")
    void getExpensesByCreatedBy() {
        // Create expense as testuser
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Testuser expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        // Create expense as bob
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-Hass-User", "bob");
        bobHeaders.setContentType(MediaType.APPLICATION_JSON);
        ExpenseDTO dto2 = new ExpenseDTO();
        dto2.setAmount(new BigDecimal("30.00"));
        dto2.setDescription("Bob expense");
        dto2.setExpenseDate(java.time.LocalDate.of(2026, 2, 16));
        dto2.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto2, bobHeaders), Map.class);

        // Filter by testuser
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses?createdBy=testuser", HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/expenses/{id} - should return expense with 200")
    void getExpenseById() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Test expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses/" + id, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("description")).isEqualTo("Test expense");
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} - should update expense with 200")
    void updateExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Original");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ExpenseDTO updateDTO = new ExpenseDTO();
        updateDTO.setAmount(new BigDecimal("75.00"));
        updateDTO.setDescription("Updated");
        updateDTO.setExpenseDate(java.time.LocalDate.of(2026, 2, 20));
        updateDTO.setBudgetId(budgetId.longValue());
        updateDTO.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses/" + id, HttpMethod.PUT, new HttpEntity<>(updateDTO, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("description")).isEqualTo("Updated");
        assertThat(((Number) response.getBody().get("amount")).doubleValue()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} - should return 204")
    void deleteExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("To delete");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/expenses/" + id, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("GET /api/expenses/{id} after delete - should return 404")
    void getExpenseAfterDelete() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("To delete");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        restTemplate.exchange("/api/expenses/" + id, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses/" + id, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/expenses/list - should return paginated list with 200")
    void getExpenseList() {
        // Create expense
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Paginated test");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        dto.setCategoryId(categoryId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses/list?year=2026&month=2&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    @DisplayName("GET /api/expenses/years - should return year list with 200")
    void getExpenseYears() {
        // Create expense so at least one year exists
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Year test");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses/years", HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains(2026);
    }

    @Test
    @DisplayName("GET /api/expenses/creators - should return creator list with 200")
    void getExpenseCreators() {
        // Create expense so at least one creator exists
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Creator test");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        restTemplate.exchange("/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/expenses/creators", HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("testuser");
    }

    @Test
    @DisplayName("POST /api/expenses with missing required fields - should return 400")
    void createExpenseMissingFields() {
        ExpenseDTO dto = new ExpenseDTO();
        // All required fields missing: amount, description, expenseDate, budgetId

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/expenses with non-existent budgetId - should return 404")
    void createExpenseNonExistentBudget() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Test");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(999999L);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("createdBy should match X-Hass-User header")
    void createdByMatchesHeader() {
        HttpHeaders customHeaders = new HttpHeaders();
        customHeaders.set("X-Hass-User", "alice");
        customHeaders.setContentType(MediaType.APPLICATION_JSON);

        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Alice expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, customHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("createdBy")).isEqualTo("alice");
    }

    @Test
    @DisplayName("Complete workflow: create expense → verify budget spending → delete → verify reset")
    void completeWorkflow() {
        // Create expense
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("250.00"));
        dto.setDescription("Workflow expense");
        dto.setExpenseDate(java.time.LocalDate.of(2026, 2, 15));
        dto.setBudgetId(budgetId.longValue());
        dto.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> expenseResp = restTemplate.exchange(
                "/api/expenses", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number expenseId = (Number) expenseResp.getBody().get("id");

        // Verify budget spending reflects the expense
        ResponseEntity<Map> budgetResp = restTemplate.exchange(
                "/api/budgets/" + budgetId, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(budgetResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) budgetResp.getBody().get("totalSpending")).doubleValue()).isEqualTo(250.0);
        assertThat(((Number) budgetResp.getBody().get("expenseCount")).intValue()).isEqualTo(1);

        // Delete expense
        restTemplate.exchange("/api/expenses/" + expenseId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);

        // Verify budget spending is reset
        ResponseEntity<Map> budgetAfterDelete = restTemplate.exchange(
                "/api/budgets/" + budgetId, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(budgetAfterDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) budgetAfterDelete.getBody().get("totalSpending")).doubleValue()).isEqualTo(0.0);
        assertThat(((Number) budgetAfterDelete.getBody().get("expenseCount")).intValue()).isEqualTo(0);
    }
}
