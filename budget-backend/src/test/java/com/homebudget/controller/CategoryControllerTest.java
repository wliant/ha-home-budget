package com.homebudget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.CategoryInUseException;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.DuplicateCategoryException;
import com.homebudget.exception.GlobalExceptionHandler;
import com.homebudget.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for CategoryController using @WebMvcTest.
 *
 * Tests all 7 endpoints:
 * - POST   /api/categories           (create)
 * - GET    /api/categories           (list all)
 * - GET    /api/categories/{id}      (get by ID)
 * - PUT    /api/categories/{id}      (update)
 * - DELETE /api/categories/{id}      (delete)
 * - GET    /api/categories/hierarchy (hierarchy)
 * - GET    /api/categories/{id}/expense-count (expense count)
 */
@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private static final String BASE_URL = "/api/categories";
    private static final String HASS_USER_HEADER = "X-Hass-User";
    private static final String TEST_USER = "testuser";

    // ========== Helper Methods ==========

    private CategoryDTO buildCategory(Long id, String name, String icon) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setIcon(icon);
        dto.setCreatedBy(TEST_USER);
        dto.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0, 0));
        dto.setIsSystem(false);
        dto.setExpenseCount(0L);
        return dto;
    }

    private CategoryDTO buildChildCategory(Long id, String name, String icon, Long parentId) {
        CategoryDTO dto = buildCategory(id, name, icon);
        dto.setParentCategoryId(parentId);
        return dto;
    }

    // ========== Nested Test Classes ==========

    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {

        @Test
        @DisplayName("should create category and return 201 with valid body and X-Hass-User header")
        void createCategory_validBody_returns201() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Groceries");
            requestDto.setIcon("shopping_cart");

            CategoryDTO responseDto = buildCategory(1L, "Groceries", "shopping_cart");

            when(categoryService.createCategory(any(CategoryDTO.class), eq(TEST_USER)))
                    .thenReturn(responseDto);

            mockMvc.perform(post(BASE_URL)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Groceries")))
                    .andExpect(jsonPath("$.icon", is("shopping_cart")))
                    .andExpect(jsonPath("$.createdBy", is(TEST_USER)));

            verify(categoryService).createCategory(any(CategoryDTO.class), eq(TEST_USER));
        }

        @Test
        @DisplayName("should create child category with parentCategoryId and return 201")
        void createCategory_withParent_returns201() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Fruits");
            requestDto.setIcon("apple");
            requestDto.setParentCategoryId(1L);

            CategoryDTO responseDto = buildChildCategory(2L, "Fruits", "apple", 1L);

            when(categoryService.createCategory(any(CategoryDTO.class), eq(TEST_USER)))
                    .thenReturn(responseDto);

            mockMvc.perform(post(BASE_URL)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.name", is("Fruits")))
                    .andExpect(jsonPath("$.parentCategoryId", is(1)));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void createCategory_blankName_returns400() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("");
            requestDto.setIcon("icon");

            mockMvc.perform(post(BASE_URL)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("should return 400 when name is null")
        void createCategory_nullName_returns400() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setIcon("icon");

            mockMvc.perform(post(BASE_URL)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("should return 409 when category name already exists")
        void createCategory_duplicateName_returns409() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Groceries");
            requestDto.setIcon("shopping_cart");

            when(categoryService.createCategory(any(CategoryDTO.class), eq(TEST_USER)))
                    .thenThrow(new DuplicateCategoryException("Groceries"));

            mockMvc.perform(post(BASE_URL)
                            .header(HASS_USER_HEADER, TEST_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Category with name 'Groceries' already exists")));
        }
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetAllCategories {

        @Test
        @DisplayName("should return list of all categories")
        void getAllCategories_returnsList() throws Exception {
            CategoryDTO cat1 = buildCategory(1L, "Groceries", "shopping_cart");
            CategoryDTO cat2 = buildCategory(2L, "Utilities", "bolt");
            List<CategoryDTO> categories = Arrays.asList(cat1, cat2);

            when(categoryService.getAllCategories()).thenReturn(categories);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Groceries")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Utilities")));

            verify(categoryService).getAllCategories();
        }

        @Test
        @DisplayName("should return empty list when no categories exist")
        void getAllCategories_empty_returnsEmptyList() throws Exception {
            when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class GetCategoryById {

        @Test
        @DisplayName("should return category when found")
        void getCategoryById_found_returns200() throws Exception {
            CategoryDTO category = buildCategory(1L, "Groceries", "shopping_cart");

            when(categoryService.getCategoryById(1L)).thenReturn(category);

            mockMvc.perform(get(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Groceries")))
                    .andExpect(jsonPath("$.icon", is("shopping_cart")))
                    .andExpect(jsonPath("$.createdBy", is(TEST_USER)));

            verify(categoryService).getCategoryById(1L);
        }

        @Test
        @DisplayName("should return 404 when category not found")
        void getCategoryById_notFound_returns404() throws Exception {
            when(categoryService.getCategoryById(999L))
                    .thenThrow(new CategoryNotFoundException(999L));

            mockMvc.perform(get(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Category not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("should update category and return 200")
        void updateCategory_validBody_returns200() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Groceries Updated");
            requestDto.setIcon("cart");

            CategoryDTO responseDto = buildCategory(1L, "Groceries Updated", "cart");

            when(categoryService.updateCategory(eq(1L), any(CategoryDTO.class)))
                    .thenReturn(responseDto);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Groceries Updated")))
                    .andExpect(jsonPath("$.icon", is("cart")));

            verify(categoryService).updateCategory(eq(1L), any(CategoryDTO.class));
        }

        @Test
        @DisplayName("should return 404 when category not found")
        void updateCategory_notFound_returns404() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Nonexistent");
            requestDto.setIcon("question");

            when(categoryService.updateCategory(eq(999L), any(CategoryDTO.class)))
                    .thenThrow(new CategoryNotFoundException(999L));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Category not found with ID: 999")));
        }

        @Test
        @DisplayName("should return 409 when updated name already exists")
        void updateCategory_duplicateName_returns409() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("Utilities");
            requestDto.setIcon("bolt");

            when(categoryService.updateCategory(eq(1L), any(CategoryDTO.class)))
                    .thenThrow(new DuplicateCategoryException("Utilities"));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Category with name 'Utilities' already exists")));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void updateCategory_blankName_returns400() throws Exception {
            CategoryDTO requestDto = new CategoryDTO();
            requestDto.setName("");
            requestDto.setIcon("icon");

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errors.name").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteCategory {

        @Test
        @DisplayName("should delete category and return 204")
        void deleteCategory_success_returns204() throws Exception {
            doNothing().when(categoryService).deleteCategory(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());

            verify(categoryService).deleteCategory(1L);
        }

        @Test
        @DisplayName("should return 404 when category not found")
        void deleteCategory_notFound_returns404() throws Exception {
            doThrow(new CategoryNotFoundException(999L))
                    .when(categoryService).deleteCategory(999L);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Category not found with ID: 999")));
        }

        @Test
        @DisplayName("should return 409 when category has associated expenses")
        void deleteCategory_inUseExpenses_returns409() throws Exception {
            doThrow(new CategoryInUseException(1L, 5L))
                    .when(categoryService).deleteCategory(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message").value(
                            "Cannot delete category (ID: 1) with 5 associated expenses. " +
                                    "Please reassign expenses to another category first."));
        }

        @Test
        @DisplayName("should return 409 when category has child categories")
        void deleteCategory_hasChildren_returns409() throws Exception {
            doThrow(new CategoryInUseException("Groceries", 3, "children"))
                    .when(categoryService).deleteCategory(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message").value(
                            "Cannot delete category 'Groceries': has 3 child categories. " +
                                    "Please reassign or delete children first."));
        }

        @Test
        @DisplayName("should return 409 when category has associated budgets")
        void deleteCategory_hasBudgets_returns409() throws Exception {
            doThrow(new CategoryInUseException("Groceries", 2, "budgets"))
                    .when(categoryService).deleteCategory(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message").value(
                            "Cannot delete category 'Groceries': has 2 associated budgets. " +
                                    "Please reassign budgets to another category first."));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/hierarchy")
    class GetCategoryHierarchy {

        @Test
        @DisplayName("should return category hierarchy with parent-child relationships")
        void getCategoryHierarchy_returnsHierarchy() throws Exception {
            CategoryDTO parent = buildCategory(1L, "Groceries", "shopping_cart");
            CategoryDTO child1 = buildChildCategory(2L, "Fruits", "apple", 1L);
            CategoryDTO child2 = buildChildCategory(3L, "Vegetables", "carrot", 1L);
            parent.setChildren(Arrays.asList(child1, child2));

            CategoryDTO rootOnly = buildCategory(4L, "Utilities", "bolt");
            rootOnly.setChildren(Collections.emptyList());

            List<CategoryDTO> hierarchy = Arrays.asList(parent, rootOnly);

            when(categoryService.getCategoryHierarchy()).thenReturn(hierarchy);

            mockMvc.perform(get(BASE_URL + "/hierarchy")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Groceries")))
                    .andExpect(jsonPath("$[0].children", hasSize(2)))
                    .andExpect(jsonPath("$[0].children[0].id", is(2)))
                    .andExpect(jsonPath("$[0].children[0].name", is("Fruits")))
                    .andExpect(jsonPath("$[0].children[0].parentCategoryId", is(1)))
                    .andExpect(jsonPath("$[0].children[1].id", is(3)))
                    .andExpect(jsonPath("$[0].children[1].name", is("Vegetables")))
                    .andExpect(jsonPath("$[1].id", is(4)))
                    .andExpect(jsonPath("$[1].name", is("Utilities")))
                    .andExpect(jsonPath("$[1].children", hasSize(0)));

            verify(categoryService).getCategoryHierarchy();
        }

        @Test
        @DisplayName("should return empty list when no categories exist")
        void getCategoryHierarchy_empty_returnsEmptyList() throws Exception {
            when(categoryService.getCategoryHierarchy()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/hierarchy")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}/expense-count")
    class GetExpenseCount {

        @Test
        @DisplayName("should return expense count for category")
        void getExpenseCount_returnsCount() throws Exception {
            when(categoryService.getExpenseCount(1L)).thenReturn(42L);

            mockMvc.perform(get(BASE_URL + "/1/expense-count")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(42)));

            verify(categoryService).getExpenseCount(1L);
        }

        @Test
        @DisplayName("should return zero when category has no expenses")
        void getExpenseCount_noExpenses_returnsZero() throws Exception {
            when(categoryService.getExpenseCount(1L)).thenReturn(0L);

            mockMvc.perform(get(BASE_URL + "/1/expense-count")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", is(0)));
        }
    }
}
