package com.homebudget.service;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.CategoryDTO;
import com.homebudget.exception.DuplicateBudgetException;
import com.homebudget.exception.ParentBudgetMismatchException;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BudgetServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private CategoryService categoryService;

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

        testCategory = categoryRepository.save(new Category("Groceries", "🛒", "testuser"));
        categoryRepository.flush();
    }

    @Test
    @DisplayName("should create budget with real category and verify persistence")
    void createBudgetWithCategory() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setDescription("February groceries");
        dto.setCategoryId(testCategory.getId());

        BudgetDTO created = budgetService.createBudget(dto, "testuser");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getYear()).isEqualTo(2026);
        assertThat(created.getMonth()).isEqualTo(2);
        assertThat(created.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(created.getCreatedBy()).isEqualTo("testuser");
        assertThat(created.getCategoryId()).isEqualTo(testCategory.getId());
        assertThat(created.getCategory()).isNotNull();
        assertThat(created.getCategory().getName()).isEqualTo("Groceries");
    }

    @Test
    @DisplayName("should throw DuplicateBudgetException for duplicate category/year/month with real DB")
    void createDuplicateBudget() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(testCategory.getId());
        budgetService.createBudget(dto, "testuser");

        BudgetDTO duplicate = new BudgetDTO();
        duplicate.setYear(2026);
        duplicate.setMonth(2);
        duplicate.setTotalAmount(new BigDecimal("2000.00"));
        duplicate.setCategoryId(testCategory.getId());

        assertThatThrownBy(() -> budgetService.createBudget(duplicate, "testuser"))
                .isInstanceOf(DuplicateBudgetException.class);
    }

    @Test
    @DisplayName("should get budget by ID with expenses and verify spending calculation")
    void getBudgetByIdWithExpenses() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(testCategory.getId());
        BudgetDTO created = budgetService.createBudget(dto, "testuser");

        // Add expenses directly via repository
        Budget budget = budgetRepository.findById(created.getId()).orElseThrow();
        Expense e1 = new Expense(new BigDecimal("200.00"), "Expense 1",
                LocalDate.of(2026, 2, 10), budget, "testuser");
        Expense e2 = new Expense(new BigDecimal("300.00"), "Expense 2",
                LocalDate.of(2026, 2, 15), budget, "testuser");
        expenseRepository.saveAll(List.of(e1, e2));
        expenseRepository.flush();

        BudgetSummaryDTO summary = budgetService.getBudgetById(created.getId());

        assertThat(summary.getTotalSpending()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(summary.getSpendingPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(summary.getExpenseCount()).isEqualTo(2);
        assertThat(summary.getExpenses()).hasSize(2);
    }

    @Test
    @DisplayName("should cascade delete expenses when budget is deleted")
    void deleteBudgetCascade() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(testCategory.getId());
        BudgetDTO created = budgetService.createBudget(dto, "testuser");

        Budget budget = budgetRepository.findById(created.getId()).orElseThrow();
        Expense e1 = new Expense(new BigDecimal("100.00"), "Expense 1",
                LocalDate.of(2026, 2, 10), budget, "testuser");
        Expense e2 = new Expense(new BigDecimal("200.00"), "Expense 2",
                LocalDate.of(2026, 2, 15), budget, "testuser");
        expenseRepository.saveAll(List.of(e1, e2));
        expenseRepository.flush();

        Long budgetId = created.getId();
        assertThat(expenseRepository.countByBudgetId(budgetId)).isEqualTo(2);

        budgetService.deleteBudget(budgetId);
        budgetRepository.flush();
        expenseRepository.flush();

        assertThat(budgetRepository.findById(budgetId)).isEmpty();
        assertThat(expenseRepository.countByBudgetId(budgetId)).isEqualTo(0);
    }

    @Test
    @DisplayName("should validate parent-child budget with real category hierarchy")
    void parentChildBudgetValidation() {
        Category parent = categoryRepository.save(new Category("Food", "🍕", "testuser"));
        categoryRepository.flush();

        Category child1 = new Category("Groceries", "🛒", "testuser");
        child1.setParentCategory(parent);
        child1 = categoryRepository.save(child1);
        categoryRepository.flush();

        // Create parent budget of 1000
        BudgetDTO parentBudgetDTO = new BudgetDTO();
        parentBudgetDTO.setYear(2026);
        parentBudgetDTO.setMonth(3);
        parentBudgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        parentBudgetDTO.setCategoryId(parent.getId());
        budgetService.createBudget(parentBudgetDTO, "testuser");

        // Create child budget that exactly matches parent (valid)
        BudgetDTO childBudgetDTO = new BudgetDTO();
        childBudgetDTO.setYear(2026);
        childBudgetDTO.setMonth(3);
        childBudgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        childBudgetDTO.setCategoryId(child1.getId());
        BudgetDTO childCreated = budgetService.createBudget(childBudgetDTO, "testuser");
        assertThat(childCreated).isNotNull();
    }

    @Test
    @DisplayName("should calculate total spending with real expenses")
    void calculateTotalSpending() {
        BudgetDTO dto = new BudgetDTO();
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setCategoryId(testCategory.getId());
        BudgetDTO created = budgetService.createBudget(dto, "testuser");

        Budget budget = budgetRepository.findById(created.getId()).orElseThrow();
        expenseRepository.save(new Expense(new BigDecimal("150.00"), "E1",
                LocalDate.of(2026, 2, 5), budget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("250.50"), "E2",
                LocalDate.of(2026, 2, 10), budget, "testuser"));
        expenseRepository.flush();

        BigDecimal total = budgetService.calculateTotalSpending(created.getId());
        assertThat(total).isEqualByComparingTo(new BigDecimal("400.50"));
    }
}
