package com.homebudget.repository;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BudgetRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        budgetRepository.deleteAll();
        // Don't delete system categories
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

        testCategory = categoryRepository.save(new Category("TestCategory", "📦", "testuser"));
        categoryRepository.flush();
    }

    @Test
    @DisplayName("should save and find budget with category")
    void saveAndFind() {
        Budget budget = new Budget(2026, 2, new BigDecimal("1000.00"), "February budget", "testuser");
        budget.setCategory(testCategory);
        Budget saved = budgetRepository.save(budget);
        budgetRepository.flush();

        Optional<Budget> found = budgetRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getYear()).isEqualTo(2026);
        assertThat(found.get().getMonth()).isEqualTo(2);
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(found.get().getDescription()).isEqualTo("February budget");
        assertThat(found.get().getCreatedBy()).isEqualTo("testuser");
        assertThat(found.get().getCategory().getId()).isEqualTo(testCategory.getId());
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
        assertThat(found.get().getVersion()).isNotNull();
    }

    @Test
    @DisplayName("should find budgets by year and month")
    void findByYearAndMonth() {
        Budget budget = new Budget(2026, 3, new BigDecimal("800.00"), "March", "testuser");
        budget.setCategory(testCategory);
        budgetRepository.save(budget);
        budgetRepository.flush();

        Optional<Budget> found = budgetRepository.findByYearAndMonth(2026, 3);
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAmount()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("should find all budgets ordered by year desc, month desc")
    void findAllByOrderByYearDescMonthDesc() {
        Budget b1 = new Budget(2026, 1, new BigDecimal("500.00"), "Jan", "testuser");
        b1.setCategory(testCategory);

        Category cat2 = categoryRepository.save(new Category("Cat2", "📦", "testuser"));
        Budget b2 = new Budget(2026, 3, new BigDecimal("700.00"), "Mar", "testuser");
        b2.setCategory(cat2);

        Category cat3 = categoryRepository.save(new Category("Cat3", "📦", "testuser"));
        Budget b3 = new Budget(2025, 12, new BigDecimal("900.00"), "Dec", "testuser");
        b3.setCategory(cat3);

        budgetRepository.saveAll(List.of(b1, b2, b3));
        budgetRepository.flush();

        List<Budget> budgets = budgetRepository.findAllByOrderByYearDescMonthDesc();
        assertThat(budgets).hasSizeGreaterThanOrEqualTo(3);

        // First should be 2026-03, then 2026-01, then 2025-12
        assertThat(budgets.get(0).getYear()).isEqualTo(2026);
        assertThat(budgets.get(0).getMonth()).isEqualTo(3);
        assertThat(budgets.get(1).getYear()).isEqualTo(2026);
        assertThat(budgets.get(1).getMonth()).isEqualTo(1);
        assertThat(budgets.get(2).getYear()).isEqualTo(2025);
        assertThat(budgets.get(2).getMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("should find budget by category, year, and month")
    void findByCategoryIdAndYearAndMonth() {
        Budget budget = new Budget(2026, 2, new BigDecimal("1000.00"), "Test", "testuser");
        budget.setCategory(testCategory);
        budgetRepository.save(budget);
        budgetRepository.flush();

        Optional<Budget> found = budgetRepository.findByCategoryIdAndYearAndMonth(
                testCategory.getId(), 2026, 2);
        assertThat(found).isPresent();
        assertThat(found.get().getCategory().getId()).isEqualTo(testCategory.getId());
    }

    @Test
    @DisplayName("should check existence by category, year, and month")
    void existsByCategoryIdAndYearAndMonth() {
        Budget budget = new Budget(2026, 2, new BigDecimal("1000.00"), "Test", "testuser");
        budget.setCategory(testCategory);
        budgetRepository.save(budget);
        budgetRepository.flush();

        assertThat(budgetRepository.existsByCategoryIdAndYearAndMonth(testCategory.getId(), 2026, 2))
                .isTrue();
        assertThat(budgetRepository.existsByCategoryIdAndYearAndMonth(testCategory.getId(), 2026, 3))
                .isFalse();
    }

    @Test
    @DisplayName("should calculate sum of child budgets by parent category and period")
    void sumByParentCategoryAndPeriod() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child1 = new Category("Groceries", "🛒", "testuser");
        child1.setParentCategory(parent);
        child1 = categoryRepository.save(child1);

        Category child2 = new Category("Dining", "🍽️", "testuser");
        child2.setParentCategory(parent);
        child2 = categoryRepository.save(child2);
        categoryRepository.flush();

        Budget b1 = new Budget(2026, 2, new BigDecimal("600.00"), "Groceries", "testuser");
        b1.setCategory(child1);
        Budget b2 = new Budget(2026, 2, new BigDecimal("400.00"), "Dining", "testuser");
        b2.setCategory(child2);
        budgetRepository.saveAll(List.of(b1, b2));
        budgetRepository.flush();

        BigDecimal sum = budgetRepository.sumByParentCategoryAndPeriod(parent.getId(), 2026, 2);
        assertThat(sum).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("should find child budgets by parent category")
    void findChildBudgets() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child1 = new Category("Groceries", "🛒", "testuser");
        child1.setParentCategory(parent);
        child1 = categoryRepository.save(child1);
        categoryRepository.flush();

        Budget childBudget = new Budget(2026, 2, new BigDecimal("600.00"), "Groceries", "testuser");
        childBudget.setCategory(child1);
        budgetRepository.save(childBudget);
        budgetRepository.flush();

        List<Budget> children = budgetRepository.findChildBudgets(parent.getId(), 2026, 2);
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getCategory().getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("should find budget by year and month with expenses eagerly loaded")
    void findByYearAndMonthWithExpenses() {
        Budget budget = new Budget(2026, 2, new BigDecimal("1000.00"), "Test", "testuser");
        budget.setCategory(testCategory);
        budget = budgetRepository.save(budget);
        budgetRepository.flush();

        Expense expense = new Expense(new BigDecimal("50.00"), "Test expense",
                LocalDate.of(2026, 2, 15), budget, "testuser");
        expenseRepository.save(expense);
        expenseRepository.flush();

        Optional<Budget> found = budgetRepository.findByYearAndMonthWithExpenses(2026, 2);
        assertThat(found).isPresent();
        assertThat(found.get().getExpenses()).hasSize(1);
        assertThat(found.get().getExpenses().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }
}
