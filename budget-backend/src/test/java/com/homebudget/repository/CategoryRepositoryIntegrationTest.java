package com.homebudget.repository;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        // System category (id=1, "Uncategorized") is seeded by Liquibase
        // Only delete non-system categories we created
        categoryRepository.findAll().stream()
                .filter(c -> c.getIsSystem() == null || !c.getIsSystem())
                .filter(c -> c.getId() != 1L)
                .forEach(c -> {
                    c.setParentCategory(null);
                    categoryRepository.save(c);
                });
        categoryRepository.findAll().stream()
                .filter(c -> (c.getIsSystem() == null || !c.getIsSystem()) && c.getId() != 1L)
                .forEach(c -> categoryRepository.deleteById(c.getId()));
        categoryRepository.flush();
    }

    @Test
    @DisplayName("should save and find category")
    void saveAndFind() {
        Category category = new Category("Groceries", "🛒", "testuser");
        Category saved = categoryRepository.save(category);
        categoryRepository.flush();

        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Groceries");
        assertThat(found.get().getIcon()).isEqualTo("🛒");
        assertThat(found.get().getCreatedBy()).isEqualTo("testuser");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find category by name")
    void findByName() {
        categoryRepository.save(new Category("Utilities", "⚡", "testuser"));
        categoryRepository.flush();

        Optional<Category> found = categoryRepository.findByName("Utilities");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Utilities");
    }

    @Test
    @DisplayName("should check name exists case insensitively")
    void existsByNameIgnoreCase() {
        categoryRepository.save(new Category("Groceries", "🛒", "testuser"));
        categoryRepository.flush();

        assertThat(categoryRepository.existsByNameIgnoreCase("groceries")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("GROCERIES")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Groceries")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("NonExistent")).isFalse();
    }

    @Test
    @DisplayName("should find all categories ordered by name ascending")
    void findAllByOrderByNameAsc() {
        categoryRepository.save(new Category("Utilities", "⚡", "testuser"));
        categoryRepository.save(new Category("Groceries", "🛒", "testuser"));
        categoryRepository.save(new Category("Entertainment", "🎬", "testuser"));
        categoryRepository.flush();

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        // Should include the seeded "Uncategorized" too
        assertThat(categories.size()).isGreaterThanOrEqualTo(3);
        // Check ordering - find our non-system categories
        List<String> names = categories.stream().map(Category::getName).toList();
        int entIdx = names.indexOf("Entertainment");
        int groIdx = names.indexOf("Groceries");
        int utlIdx = names.indexOf("Utilities");
        assertThat(entIdx).isLessThan(groIdx);
        assertThat(groIdx).isLessThan(utlIdx);
    }

    @Test
    @DisplayName("should find only root categories (no parent)")
    void findByParentCategoryIsNullOrderByNameAsc() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child = new Category("Groceries", "🛒", "testuser");
        child.setParentCategory(parent);
        categoryRepository.save(child);
        categoryRepository.flush();

        List<Category> roots = categoryRepository.findByParentCategoryIsNullOrderByNameAsc();
        List<String> rootNames = roots.stream().map(Category::getName).toList();

        assertThat(rootNames).contains("Food");
        assertThat(rootNames).doesNotContain("Groceries");
    }

    @Test
    @DisplayName("should find child categories by parent ID")
    void findByParentCategoryId() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child1 = new Category("Groceries", "🛒", "testuser");
        child1.setParentCategory(parent);
        categoryRepository.save(child1);

        Category child2 = new Category("Dining Out", "🍽️", "testuser");
        child2.setParentCategory(parent);
        categoryRepository.save(child2);
        categoryRepository.flush();

        List<Category> children = categoryRepository.findByParentCategoryId(parent.getId());
        assertThat(children).hasSize(2);
        assertThat(children.stream().map(Category::getName).toList())
                .containsExactlyInAnyOrder("Dining Out", "Groceries");
    }

    @Test
    @DisplayName("should count child categories")
    void countByParentCategoryId() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child = new Category("Groceries", "🛒", "testuser");
        child.setParentCategory(parent);
        categoryRepository.save(child);
        categoryRepository.flush();

        assertThat(categoryRepository.countByParentCategoryId(parent.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("should save category with parent (hierarchy persistence)")
    void saveCategoryWithParent() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child = new Category("Groceries", "🛒", "testuser");
        child.setParentCategory(parent);
        Category saved = categoryRepository.save(child);
        categoryRepository.flush();

        Category found = categoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getParentCategory()).isNotNull();
        assertThat(found.getParentCategory().getId()).isEqualTo(parent.getId());
        assertThat(found.getParentCategory().getName()).isEqualTo("Food");
    }

    @Test
    @DisplayName("should enforce unique name constraint at DB level")
    void uniqueNameConstraint() {
        categoryRepository.save(new Category("Groceries", "🛒", "testuser"));
        categoryRepository.flush();

        assertThatThrownBy(() -> {
            categoryRepository.save(new Category("Groceries", "🛒", "other"));
            categoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("should have Uncategorized system category seeded by Liquibase")
    void systemCategorySeedData() {
        Optional<Category> uncategorized = categoryRepository.findById(1L);
        assertThat(uncategorized).isPresent();
        assertThat(uncategorized.get().getName()).isEqualTo("Uncategorized");
        assertThat(uncategorized.get().getIsSystem()).isTrue();
    }
}
