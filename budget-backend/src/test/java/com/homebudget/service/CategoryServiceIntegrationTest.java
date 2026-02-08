package com.homebudget.service;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.*;
import com.homebudget.model.Category;
import com.homebudget.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        // Clean non-system categories
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

    @Test
    @DisplayName("should create category and retrieve via getCategoryById")
    void createAndRetrieve() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");

        CategoryDTO created = categoryService.createCategory(dto, "testuser");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Groceries");
        assertThat(created.getCreatedBy()).isEqualTo("testuser");

        CategoryDTO found = categoryService.getCategoryById(created.getId());
        assertThat(found.getName()).isEqualTo("Groceries");
        assertThat(found.getIcon()).isEqualTo("🛒");
    }

    @Test
    @DisplayName("should throw DuplicateCategoryException for duplicate name with real DB")
    void createDuplicate() {
        CategoryDTO dto = new CategoryDTO();
        dto.setName("Groceries");
        dto.setIcon("🛒");
        categoryService.createCategory(dto, "testuser");

        CategoryDTO duplicate = new CategoryDTO();
        duplicate.setName("groceries"); // case-insensitive
        duplicate.setIcon("🛒");

        assertThatThrownBy(() -> categoryService.createCategory(duplicate, "testuser"))
                .isInstanceOf(DuplicateCategoryException.class);
    }

    @Test
    @DisplayName("should create category with parent hierarchy (2-level max enforcement)")
    void createWithParentHierarchy() {
        CategoryDTO parentDTO = new CategoryDTO();
        parentDTO.setName("Food");
        parentDTO.setIcon("🍕");
        CategoryDTO parent = categoryService.createCategory(parentDTO, "testuser");

        CategoryDTO childDTO = new CategoryDTO();
        childDTO.setName("Groceries");
        childDTO.setIcon("🛒");
        childDTO.setParentCategoryId(parent.getId());
        CategoryDTO child = categoryService.createCategory(childDTO, "testuser");

        assertThat(child.getParentCategoryId()).isEqualTo(parent.getId());

        // Attempt to create grandchild should fail (max 2 levels)
        CategoryDTO grandchildDTO = new CategoryDTO();
        grandchildDTO.setName("Organic");
        grandchildDTO.setIcon("🌱");
        grandchildDTO.setParentCategoryId(child.getId());

        assertThatThrownBy(() -> categoryService.createCategory(grandchildDTO, "testuser"))
                .isInstanceOf(HierarchyDepthException.class);
    }

    @Test
    @DisplayName("should detect circular reference with persisted data")
    void circularReferenceDetection() {
        CategoryDTO parentDTO = new CategoryDTO();
        parentDTO.setName("ParentCat");
        parentDTO.setIcon("📁");
        CategoryDTO parent = categoryService.createCategory(parentDTO, "testuser");

        CategoryDTO childDTO = new CategoryDTO();
        childDTO.setName("ChildCat");
        childDTO.setIcon("📄");
        childDTO.setParentCategoryId(parent.getId());
        CategoryDTO child = categoryService.createCategory(childDTO, "testuser");

        // Try to set parent's parent to child => circular
        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("ParentCat");
        updateDTO.setIcon("📁");
        updateDTO.setParentCategoryId(child.getId());

        assertThatThrownBy(() -> categoryService.updateCategory(parent.getId(), updateDTO))
                .isInstanceOf(CircularCategoryException.class);
    }

    @Test
    @DisplayName("should throw CategoryInUseException when deleting category with children")
    void deleteCategoryWithChildren() {
        CategoryDTO parentDTO = new CategoryDTO();
        parentDTO.setName("Food");
        parentDTO.setIcon("🍕");
        CategoryDTO parent = categoryService.createCategory(parentDTO, "testuser");

        CategoryDTO childDTO = new CategoryDTO();
        childDTO.setName("Groceries");
        childDTO.setIcon("🛒");
        childDTO.setParentCategoryId(parent.getId());
        categoryService.createCategory(childDTO, "testuser");

        assertThatThrownBy(() -> categoryService.deleteCategory(parent.getId()))
                .isInstanceOf(CategoryInUseException.class)
                .hasMessageContaining("child categories");
    }

    @Test
    @DisplayName("should get category hierarchy with real parent-child data")
    void getCategoryHierarchy() {
        CategoryDTO parentDTO = new CategoryDTO();
        parentDTO.setName("Food");
        parentDTO.setIcon("🍕");
        CategoryDTO parent = categoryService.createCategory(parentDTO, "testuser");

        CategoryDTO child1DTO = new CategoryDTO();
        child1DTO.setName("Groceries");
        child1DTO.setIcon("🛒");
        child1DTO.setParentCategoryId(parent.getId());
        categoryService.createCategory(child1DTO, "testuser");

        CategoryDTO child2DTO = new CategoryDTO();
        child2DTO.setName("Dining");
        child2DTO.setIcon("🍽️");
        child2DTO.setParentCategoryId(parent.getId());
        categoryService.createCategory(child2DTO, "testuser");

        List<CategoryDTO> hierarchy = categoryService.getCategoryHierarchy();
        // Should include Food (with children) and Uncategorized (system)
        CategoryDTO foodRoot = hierarchy.stream()
                .filter(c -> "Food".equals(c.getName()))
                .findFirst()
                .orElseThrow();

        assertThat(foodRoot.getChildren()).hasSize(2);
        assertThat(foodRoot.getChildren().stream().map(CategoryDTO::getName).toList())
                .containsExactlyInAnyOrder("Dining", "Groceries");
    }

    @Test
    @DisplayName("should update category name with uniqueness check")
    void updateCategoryName() {
        CategoryDTO dto1 = new CategoryDTO();
        dto1.setName("Groceries");
        dto1.setIcon("🛒");
        CategoryDTO cat1 = categoryService.createCategory(dto1, "testuser");

        CategoryDTO dto2 = new CategoryDTO();
        dto2.setName("Utilities");
        dto2.setIcon("⚡");
        categoryService.createCategory(dto2, "testuser");

        // Try to rename Groceries to Utilities => should fail
        CategoryDTO updateDTO = new CategoryDTO();
        updateDTO.setName("Utilities");
        updateDTO.setIcon("🛒");

        assertThatThrownBy(() -> categoryService.updateCategory(cat1.getId(), updateDTO))
                .isInstanceOf(DuplicateCategoryException.class);
    }
}
