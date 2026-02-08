package com.homebudget.e2e;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.CategoryDTO;
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
class BudgetE2ETest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Number categoryId;

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
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("POST /api/budgets - should create budget with 201")
    void createBudget() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setDescription("February budget");
        dto.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("year")).isEqualTo(2026);
        assertThat(response.getBody().get("month")).isEqualTo(2);
        assertThat(response.getBody().get("createdBy")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("GET /api/budgets - should return budget list with 200")
    void getAllBudgets() {
        // Create budget
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(categoryId.longValue());
        restTemplate.exchange("/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/budgets", HttpMethod.GET, new HttpEntity<>(headers()), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/budgets/{id} - should return budget with spending summary with 200")
    void getBudgetById() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets/" + id, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalSpending")).isNotNull();
        assertThat(response.getBody().get("spendingPercentage")).isNotNull();
        assertThat(response.getBody().get("expenseCount")).isNotNull();
    }

    @Test
    @DisplayName("PUT /api/budgets/{id} - should update amount/description with 200")
    void updateBudget() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setDescription("Original");
        dto.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        BudgetDTO updateDTO = new BudgetDTO();
        updateDTO.setYear(2026);
        updateDTO.setMonth(2);
        updateDTO.setTotalAmount(new BigDecimal("1500.00"));
        updateDTO.setDescription("Updated");
        updateDTO.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets/" + id, HttpMethod.PUT, new HttpEntity<>(updateDTO, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("description")).isEqualTo("Updated");
    }

    @Test
    @DisplayName("DELETE /api/budgets/{id} - should return 204")
    void deleteBudget() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/budgets/" + id, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("GET /api/budgets/{id} after delete - should return 404")
    void getBudgetAfterDelete() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        restTemplate.exchange("/api/budgets/" + id, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets/" + id, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/budgets duplicate category/year/month - should return 409")
    void createDuplicateBudget() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(categoryId.longValue());
        restTemplate.exchange("/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        BudgetDTO duplicate = new BudgetDTO();
        duplicate.setYear(2026);
        duplicate.setMonth(2);
        duplicate.setTotalAmount(new BigDecimal("2000.00"));
        duplicate.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(duplicate, headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST /api/budgets with missing categoryId - should return 400")
    void createBudgetMissingCategory() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        // categoryId is null

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/budgets/current - should return 200 or 404")
    void getCurrentMonthBudget() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets/current", HttpMethod.GET, new HttpEntity<>(headers()), Map.class);

        // Either 200 (if there's a budget for current month) or 404
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("createdBy should match X-Hass-User header")
    void createdByMatchesHeader() {
        HttpHeaders customHeaders = new HttpHeaders();
        customHeaders.set("X-Hass-User", "bob");
        customHeaders.setContentType(MediaType.APPLICATION_JSON);

        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(3);
        dto.setTotalAmount(new BigDecimal("500.00"));
        dto.setCategoryId(categoryId.longValue());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(dto, customHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("createdBy")).isEqualTo("bob");
    }

    @Test
    @DisplayName("DELETE /api/budgets/{id} should cascade delete expenses")
    void deleteBudgetCascadeExpenses() {
        // Create budget
        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setYear(2026);
        budgetDTO.setMonth(4);
        budgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        budgetDTO.setCategoryId(categoryId.longValue());
        ResponseEntity<Map> budgetResp = restTemplate.exchange(
                "/api/budgets", HttpMethod.POST, new HttpEntity<>(budgetDTO, headers()), Map.class);
        Number budgetId = (Number) budgetResp.getBody().get("id");

        // Create expense
        Map<String, Object> expenseBody = Map.of(
                "amount", 50.0,
                "description", "Test expense",
                "expenseDate", "2026-04-15",
                "budgetId", budgetId
        );
        restTemplate.exchange("/api/expenses", HttpMethod.POST,
                new HttpEntity<>(expenseBody, headers()), Map.class);

        // Delete budget
        restTemplate.exchange("/api/budgets/" + budgetId, HttpMethod.DELETE,
                new HttpEntity<>(headers()), Void.class);

        // Verify expenses are gone
        ResponseEntity<List> expensesResp = restTemplate.exchange(
                "/api/expenses?budgetId=" + budgetId, HttpMethod.GET,
                new HttpEntity<>(headers()), List.class);
        assertThat(expensesResp.getBody()).isEmpty();
    }
}
