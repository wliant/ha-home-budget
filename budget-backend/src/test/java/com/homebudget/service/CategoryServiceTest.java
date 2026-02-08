package com.homebudget.service;

import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Category;
import com.homebudget.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    // ========== Helper Methods ==========

    private Category createCategory(Long id, String name, String icon, String createdBy) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        category.setCreatedBy(createdBy);
        category.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        category.setIsSystem(false);
        return category;
    }

    private CategoryDTO createCategoryDTO(String name, String icon, Long parentCategoryId) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setIcon(icon);
        dto.setParentCategoryId(parentCategoryId);
        return dto;
    }

    /**
     * Stubs the countExpensesByCategoryId call that toDTO() always invokes.
     */
    private void stubExpenseCount(Long categoryId, long count) {
        when(categoryRepository.countExpensesByCategoryId(categoryId)).thenReturn(count);
    }

    // ========================================================================
    // createCategory
    // ========================================================================

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("should create root category successfully when name is unique")
        void createCategory_success_noParent() {
            // Arrange
            CategoryDTO inputDTO = createCategoryDTO("Groceries", "cart", null);
            Category saved = createCategory(1L, "Groceries", "cart", "alice");

            when(categoryRepository.existsByNameIgnoreCase("Groceries")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            stubExpenseCount(1L, 0L);

            // Act
            CategoryDTO result = categoryService.createCategory(inputDTO, "alice");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Groceries");
            assertThat(result.getIcon()).isEqualTo("cart");
            assertThat(result.getExpenseCount()).isEqualTo(0L);
            assertThat(result.getParentCategoryId()).isNull();

            verify(categoryRepository).existsByNameIgnoreCase("Groceries");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw DuplicateCategoryException when name already exists (case insensitive)")
        void createCategory_duplicateName() {
            // Arrange
            CategoryDTO inputDTO = createCategoryDTO("groceries", null, null);

            when(categoryRepository.existsByNameIgnoreCase("groceries")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(inputDTO, "alice"))
                    .isInstanceOf(DuplicateCategoryException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should create child category when parent exists and is root")
        void createCategory_withParent_success() {
            // Arrange
            Category parent = createCategory(10L, "Food", "utensils", "alice");
            // parent has no parentCategory => it is a root

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", 10L);

            Category saved = createCategory(2L, "Snacks", "cookie", "alice");
            saved.setParentCategory(parent);

            when(categoryRepository.existsByNameIgnoreCase("Snacks")).thenReturn(false);
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            stubExpenseCount(2L, 0L);

            // Act
            CategoryDTO result = categoryService.createCategory(inputDTO, "alice");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getParentCategoryId()).isEqualTo(10L);
            assertThat(result.getParentCategory()).isNotNull();
            assertThat(result.getParentCategory().getId()).isEqualTo(10L);
            assertThat(result.getParentCategory().getName()).isEqualTo("Food");

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategory()).isEqualTo(parent);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when parent does not exist")
        void createCategory_parentNotFound() {
            // Arrange
            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", 999L);

            when(categoryRepository.existsByNameIgnoreCase("Snacks")).thenReturn(false);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(inputDTO, "alice"))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw HierarchyDepthException when parent is already a child category")
        void createCategory_parentIsChild() {
            // Arrange
            Category grandparent = createCategory(5L, "Root", "folder", "alice");
            Category parent = createCategory(10L, "Child", "file", "alice");
            parent.setParentCategory(grandparent); // parent already has a parent

            CategoryDTO inputDTO = createCategoryDTO("Grandchild", "leaf", 10L);

            when(categoryRepository.existsByNameIgnoreCase("Grandchild")).thenReturn(false);
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(inputDTO, "alice"))
                    .isInstanceOf(HierarchyDepthException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    // ========================================================================
    // getAllCategories
    // ========================================================================

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategories {

        @Test
        @DisplayName("should return all categories ordered by name")
        void getAllCategories_success() {
            // Arrange
            Category cat1 = createCategory(1L, "Entertainment", "film", "alice");
            Category cat2 = createCategory(2L, "Groceries", "cart", "bob");

            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(cat1, cat2));
            stubExpenseCount(1L, 3L);
            stubExpenseCount(2L, 7L);

            // Act
            List<CategoryDTO> result = categoryService.getAllCategories();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Entertainment");
            assertThat(result.get(0).getExpenseCount()).isEqualTo(3L);
            assertThat(result.get(1).getName()).isEqualTo("Groceries");
            assertThat(result.get(1).getExpenseCount()).isEqualTo(7L);

            verify(categoryRepository).findAllByOrderByNameAsc();
        }

        @Test
        @DisplayName("should return empty list when no categories exist")
        void getAllCategories_empty() {
            // Arrange
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

            // Act
            List<CategoryDTO> result = categoryService.getAllCategories();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ========================================================================
    // getCategoryById
    // ========================================================================

    @Nested
    @DisplayName("getCategoryById")
    class GetCategoryById {

        @Test
        @DisplayName("should return category DTO when found")
        void getCategoryById_success() {
            // Arrange
            Category category = createCategory(1L, "Groceries", "cart", "alice");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            stubExpenseCount(1L, 5L);

            // Act
            CategoryDTO result = categoryService.getCategoryById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Groceries");
            assertThat(result.getIcon()).isEqualTo("cart");
            assertThat(result.getCreatedBy()).isEqualTo("alice");
            assertThat(result.getExpenseCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when not found")
        void getCategoryById_notFound() {
            // Arrange
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    // ========================================================================
    // updateCategory
    // ========================================================================

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("should update name and icon successfully")
        void updateCategory_success() {
            // Arrange
            Category existing = createCategory(1L, "Groceries", "cart", "alice");
            Category updated = createCategory(1L, "Food & Groceries", "basket", "alice");

            CategoryDTO inputDTO = createCategoryDTO("Food & Groceries", "basket", null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameIgnoreCase("Food & Groceries")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(updated);
            stubExpenseCount(1L, 0L);

            // Act
            CategoryDTO result = categoryService.updateCategory(1L, inputDTO);

            // Assert
            assertThat(result.getName()).isEqualTo("Food & Groceries");
            assertThat(result.getIcon()).isEqualTo("basket");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void updateCategory_notFound() {
            // Arrange
            CategoryDTO inputDTO = createCategoryDTO("Updated", "icon", null);

            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(999L, inputDTO))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should throw DuplicateCategoryException when new name already exists")
        void updateCategory_duplicateName() {
            // Arrange
            Category existing = createCategory(1L, "Groceries", "cart", "alice");
            CategoryDTO inputDTO = createCategoryDTO("Utilities", "bolt", null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameIgnoreCase("Utilities")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(1L, inputDTO))
                    .isInstanceOf(DuplicateCategoryException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should allow update when name is same (case insensitive) - no duplicate check")
        void updateCategory_sameName_noDuplicateCheck() {
            // Arrange
            Category existing = createCategory(1L, "Groceries", "cart", "alice");
            Category saved = createCategory(1L, "groceries", "basket", "alice");

            CategoryDTO inputDTO = createCategoryDTO("groceries", "basket", null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            // existsByNameIgnoreCase should NOT be called because names match case-insensitively
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            stubExpenseCount(1L, 0L);

            // Act
            CategoryDTO result = categoryService.updateCategory(1L, inputDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(categoryRepository, never()).existsByNameIgnoreCase(anyString());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("should change parent category when valid")
        void updateCategory_changeParent() {
            // Arrange
            Category existing = createCategory(1L, "Snacks", "cookie", "alice");
            // existing is currently a root category (no parent)

            Category newParent = createCategory(10L, "Food", "utensils", "alice");
            // newParent is also root (no parent) => valid

            Category saved = createCategory(1L, "Snacks", "cookie", "alice");
            saved.setParentCategory(newParent);

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", 10L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(newParent));
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            stubExpenseCount(1L, 0L);

            // Act
            CategoryDTO result = categoryService.updateCategory(1L, inputDTO);

            // Assert
            assertThat(result.getParentCategoryId()).isEqualTo(10L);
            assertThat(result.getParentCategory().getName()).isEqualTo("Food");

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategory()).isEqualTo(newParent);
        }

        @Test
        @DisplayName("should throw CategoryInUseException when changing parent with active budgets")
        void updateCategory_changeParent_withBudgets() {
            // Arrange
            Category existing = createCategory(1L, "Snacks", "cookie", "alice");
            // existing is currently root

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", 10L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(3L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(1L, inputDTO))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("active budgets");

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should remove parent (move to root) when parentCategoryId is null and had parent")
        void updateCategory_removeParent() {
            // Arrange
            Category parent = createCategory(10L, "Food", "utensils", "alice");
            Category existing = createCategory(1L, "Snacks", "cookie", "alice");
            existing.setParentCategory(parent);

            Category saved = createCategory(1L, "Snacks", "cookie", "alice");
            // saved has no parent

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            stubExpenseCount(1L, 0L);

            // Act
            CategoryDTO result = categoryService.updateCategory(1L, inputDTO);

            // Assert
            assertThat(result.getParentCategoryId()).isNull();

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getParentCategory()).isNull();
        }

        @Test
        @DisplayName("should throw CategoryInUseException when removing parent with active budgets")
        void updateCategory_removeParent_withBudgets() {
            // Arrange
            Category parent = createCategory(10L, "Food", "utensils", "alice");
            Category existing = createCategory(1L, "Snacks", "cookie", "alice");
            existing.setParentCategory(parent);

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(2L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(1L, inputDTO))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("active budgets");

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should validate hierarchy depth when changing parent")
        void updateCategory_changeParent_hierarchyDepthViolation() {
            // Arrange
            Category grandparent = createCategory(5L, "Root", "folder", "alice");
            Category newParent = createCategory(10L, "Child", "file", "alice");
            newParent.setParentCategory(grandparent); // newParent is already a child

            Category existing = createCategory(1L, "Snacks", "cookie", "alice");

            CategoryDTO inputDTO = createCategoryDTO("Snacks", "cookie", 10L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(newParent));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(1L, inputDTO))
                    .isInstanceOf(HierarchyDepthException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("should validate circular reference when changing parent")
        void updateCategory_changeParent_circularReference() {
            // Arrange
            // Category 1 is the parent of category 10.
            // Now we try to set category 10 as parent of category 1 => circular.
            Category child = createCategory(10L, "Child", "file", "alice");

            Category existing = createCategory(1L, "Parent", "folder", "alice");
            child.setParentCategory(existing); // child's parent is existing

            CategoryDTO inputDTO = createCategoryDTO("Parent", "folder", 10L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            // findById(10L) is called twice: once for validateParentChange->getNewParent, once for validateCircularReference
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(child));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(1L, inputDTO))
                    .isInstanceOf(CircularCategoryException.class);

            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    // ========================================================================
    // deleteCategory
    // ========================================================================

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("should delete category when no children, budgets, or expenses")
        void deleteCategory_success() {
            // Arrange
            Category category = createCategory(1L, "Groceries", "cart", "alice");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.countExpensesByCategoryId(1L)).thenReturn(0L);

            // Act
            categoryService.deleteCategory(1L);

            // Assert
            verify(categoryRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void deleteCategory_notFound() {
            // Arrange
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should throw CategoryInUseException when category has children")
        void deleteCategory_hasChildren() {
            // Arrange
            Category category = createCategory(1L, "Food", "utensils", "alice");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentCategoryId(1L)).thenReturn(3L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("child categories");

            verify(categoryRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should throw CategoryInUseException when category has budgets")
        void deleteCategory_hasBudgets() {
            // Arrange
            Category category = createCategory(1L, "Food", "utensils", "alice");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(2L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("budgets");

            verify(categoryRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should throw CategoryInUseException when category has expenses")
        void deleteCategory_hasExpenses() {
            // Arrange
            Category category = createCategory(1L, "Food", "utensils", "alice");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.countByParentCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.countBudgetsByCategoryId(1L)).thenReturn(0L);
            when(categoryRepository.countExpensesByCategoryId(1L)).thenReturn(5L);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                    .isInstanceOf(CategoryInUseException.class)
                    .hasMessageContaining("expenses");

            verify(categoryRepository, never()).deleteById(anyLong());
        }
    }

    // ========================================================================
    // getExpenseCount
    // ========================================================================

    @Nested
    @DisplayName("getExpenseCount")
    class GetExpenseCount {

        @Test
        @DisplayName("should return expense count from repository")
        void getExpenseCount_success() {
            // Arrange
            when(categoryRepository.countExpensesByCategoryId(1L)).thenReturn(42L);

            // Act
            long result = categoryService.getExpenseCount(1L);

            // Assert
            assertThat(result).isEqualTo(42L);
            verify(categoryRepository).countExpensesByCategoryId(1L);
        }

        @Test
        @DisplayName("should return zero when no expenses exist")
        void getExpenseCount_zero() {
            // Arrange
            when(categoryRepository.countExpensesByCategoryId(1L)).thenReturn(0L);

            // Act
            long result = categoryService.getExpenseCount(1L);

            // Assert
            assertThat(result).isZero();
        }
    }

    // ========================================================================
    // getCategoryHierarchy
    // ========================================================================

    @Nested
    @DisplayName("getCategoryHierarchy")
    class GetCategoryHierarchy {

        @Test
        @DisplayName("should return root categories with children loaded")
        void getCategoryHierarchy_success() {
            // Arrange
            Category root1 = createCategory(1L, "Food", "utensils", "alice");
            Category root2 = createCategory(2L, "Transport", "car", "bob");

            Category child1 = createCategory(10L, "Snacks", "cookie", "alice");
            child1.setParentCategory(root1);
            Category child2 = createCategory(11L, "Dining", "plate", "alice");
            child2.setParentCategory(root1);

            when(categoryRepository.findByParentCategoryIsNullOrderByNameAsc())
                    .thenReturn(List.of(root1, root2));

            // toDTO for root1
            stubExpenseCount(1L, 2L);
            // findByParentCategoryId for root1's children
            when(categoryRepository.findByParentCategoryId(1L))
                    .thenReturn(List.of(child1, child2));
            // toDTO for each child
            stubExpenseCount(10L, 1L);
            stubExpenseCount(11L, 0L);

            // toDTO for root2
            stubExpenseCount(2L, 5L);
            // findByParentCategoryId for root2's children (none)
            when(categoryRepository.findByParentCategoryId(2L))
                    .thenReturn(Collections.emptyList());

            // Act
            List<CategoryDTO> result = categoryService.getCategoryHierarchy();

            // Assert
            assertThat(result).hasSize(2);

            // First root: Food
            CategoryDTO foodDTO = result.get(0);
            assertThat(foodDTO.getName()).isEqualTo("Food");
            assertThat(foodDTO.getExpenseCount()).isEqualTo(2L);
            assertThat(foodDTO.getChildren()).hasSize(2);
            assertThat(foodDTO.getChildren().get(0).getName()).isEqualTo("Snacks");
            assertThat(foodDTO.getChildren().get(0).getParentCategoryId()).isEqualTo(1L);
            assertThat(foodDTO.getChildren().get(1).getName()).isEqualTo("Dining");

            // Second root: Transport
            CategoryDTO transportDTO = result.get(1);
            assertThat(transportDTO.getName()).isEqualTo("Transport");
            assertThat(transportDTO.getExpenseCount()).isEqualTo(5L);
            assertThat(transportDTO.getChildren()).isEmpty();

            verify(categoryRepository).findByParentCategoryIsNullOrderByNameAsc();
            verify(categoryRepository).findByParentCategoryId(1L);
            verify(categoryRepository).findByParentCategoryId(2L);
        }

        @Test
        @DisplayName("should return empty list when no root categories exist")
        void getCategoryHierarchy_empty() {
            // Arrange
            when(categoryRepository.findByParentCategoryIsNullOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            // Act
            List<CategoryDTO> result = categoryService.getCategoryHierarchy();

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
