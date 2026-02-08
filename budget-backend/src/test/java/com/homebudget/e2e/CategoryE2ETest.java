package com.homebudget.e2e;

import com.homebudget.config.AbstractIntegrationTest;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryE2ETest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

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
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Hass-User", "testuser");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("POST /api/categories - should create category with 201")
    void createCategory() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>(dto, headers()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("Groceries");
        assertThat(response.getBody().get("icon")).isEqualTo("🛒");
        assertThat(response.getBody().get("createdBy")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("GET /api/categories - should return category list with 200")
    void getAllCategories() {
        // Create a category first
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        restTemplate.exchange("/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        ResponseEntity<List> response = restTemplate.exchange(
                "/api/categories",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/categories/{id} - should return category with 200")
    void getCategoryById() {
        // Create
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResponse.getBody().get("id");

        // Get
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - should update category with 200")
    void updateCategory() {
        // Create
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResponse.getBody().get("id");

        // Update
        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("Food & Groceries");
        updateDTO.setIcon("🍕");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updateDTO, headers()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("Food & Groceries");
        assertThat(response.getBody().get("icon")).isEqualTo("🍕");
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - should return 204")
    void deleteCategory() {
        // Create
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResponse.getBody().get("id");

        // Delete
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.DELETE,
                new HttpEntity<>(headers()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("GET /api/categories/{id} after delete - should return 404")
    void getCategoryAfterDelete() {
        // Create
        CategoryDTO dto = new CategoryDTO();
        dto.setName("ToDelete");
        dto.setIcon("❌");
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResponse.getBody().get("id");

        // Delete
        restTemplate.exchange("/api/categories/" + id, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);

        // Get should 404
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/categories duplicate name - should return 409")
    void createCategoryDuplicateName() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        restTemplate.exchange("/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        // Duplicate
        CategoryDTO duplicate = new CategoryDTO();
        duplicate.setName("Groceries");
        duplicate.setIcon("🛒");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(duplicate, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST /api/categories with missing name - should return 400")
    void createCategoryMissingName() {
        CategoryDTO dto = new CategoryDTO();
        dto.setIcon("🛒");
        // name is null

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/categories/hierarchy - should return hierarchy with 200")
    void getCategoryHierarchy() {
        // Create parent
        CategoryDTO parentDTO = new CategoryDTO();
        parentDTO.setName("Food");
        parentDTO.setIcon("🍕");
        ResponseEntity<Map> parentResp = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(parentDTO, headers()), Map.class);
        Number parentId = (Number) parentResp.getBody().get("id");

        // Create child
        CategoryDTO childDTO = new CategoryDTO();
        childDTO.setName("Groceries");
        childDTO.setIcon("🛒");
        childDTO.setParentCategoryId(parentId.longValue());
        restTemplate.exchange("/api/categories", HttpMethod.POST, new HttpEntity<>(childDTO, headers()), Map.class);

        // Get hierarchy
        ResponseEntity<List> response = restTemplate.exchange(
                "/api/categories/hierarchy",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isGreaterThanOrEqualTo(1);

        // Find Food root and verify it has children
        @SuppressWarnings("unchecked")
        Map<String, Object> foodRoot = (Map<String, Object>) response.getBody().stream()
                .filter(item -> "Food".equals(((Map<?, ?>) item).get("name")))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) foodRoot.get("children");
        assertThat(children).isNotEmpty();
        assertThat(children.get(0).get("name")).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("GET /api/categories/{id}/expense-count - should return count with 200")
    void getExpenseCount() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, headers()), Map.class);
        Number id = (Number) createResp.getBody().get("id");

        ResponseEntity<Long> response = restTemplate.exchange(
                "/api/categories/" + id + "/expense-count",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Long.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);
    }

    @Test
    @DisplayName("createdBy should match X-Hass-User header value")
    void createdByMatchesHeader() {
        HttpHeaders customHeaders = new HttpHeaders();
        customHeaders.set("X-Hass-User", "alice");
        customHeaders.setContentType(MediaType.APPLICATION_JSON);

        CategoryDTO dto = new CategoryDTO();
        dto.setName("Alice Category");
        dto.setIcon("👩");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/categories", HttpMethod.POST, new HttpEntity<>(dto, customHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("createdBy")).isEqualTo("alice");
    }
}
