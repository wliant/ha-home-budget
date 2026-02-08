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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpenseRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Budget testBudget;

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

        testBudget = new Budget(2026, 2, new BigDecimal("1000.00"), "February", "testuser");
        testBudget.setCategory(testCategory);
        testBudget = budgetRepository.save(testBudget);
        budgetRepository.flush();
    }

    @Test
    @DisplayName("should save and find expense with budget and category")
    void saveAndFind() {
        Expense expense = new Expense(new BigDecimal("50.00"), "Weekly groceries",
                LocalDate.of(2026, 2, 15), testBudget, "testuser");
        expense.setCategory(testCategory);
        Expense saved = expenseRepository.save(expense);
        expenseRepository.flush();

        Optional<Expense> found = expenseRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(found.get().getDescription()).isEqualTo("Weekly groceries");
        assertThat(found.get().getExpenseDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(found.get().getBudget().getId()).isEqualTo(testBudget.getId());
        assertThat(found.get().getCategory().getId()).isEqualTo(testCategory.getId());
        assertThat(found.get().getCreatedBy()).isEqualTo("testuser");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find expenses by budget ID")
    void findByBudgetId() {
        expenseRepository.save(new Expense(new BigDecimal("30.00"), "Expense 1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("40.00"), "Expense 2",
                LocalDate.of(2026, 2, 11), testBudget, "testuser"));
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findByBudgetId(testBudget.getId());
        assertThat(expenses).hasSize(2);
    }

    @Test
    @DisplayName("should find expenses by category ID")
    void findByCategoryId() {
        Expense e = new Expense(new BigDecimal("30.00"), "Expense 1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser");
        e.setCategory(testCategory);
        expenseRepository.save(e);
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findByCategoryId(testCategory.getId());
        assertThat(expenses).hasSize(1);
    }

    @Test
    @DisplayName("should find expenses by date range")
    void findByExpenseDateBetween() {
        expenseRepository.save(new Expense(new BigDecimal("30.00"), "Feb 5",
                LocalDate.of(2026, 2, 5), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("40.00"), "Feb 15",
                LocalDate.of(2026, 2, 15), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("50.00"), "Feb 25",
                LocalDate.of(2026, 2, 25), testBudget, "testuser"));
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(
                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 20));
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getDescription()).isEqualTo("Feb 15");
    }

    @Test
    @DisplayName("should find expenses by createdBy")
    void findByCreatedBy() {
        expenseRepository.save(new Expense(new BigDecimal("30.00"), "User1 expense",
                LocalDate.of(2026, 2, 10), testBudget, "user1"));
        expenseRepository.save(new Expense(new BigDecimal("40.00"), "User2 expense",
                LocalDate.of(2026, 2, 11), testBudget, "user2"));
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findByCreatedBy("user1");
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getCreatedBy()).isEqualTo("user1");
    }

    @Test
    @DisplayName("should find all expenses ordered by date descending")
    void findAllOrderByExpenseDateDesc() {
        expenseRepository.save(new Expense(new BigDecimal("30.00"), "Early",
                LocalDate.of(2026, 2, 1), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("40.00"), "Late",
                LocalDate.of(2026, 2, 28), testBudget, "testuser"));
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findAllOrderByExpenseDateDesc();
        assertThat(expenses).hasSizeGreaterThanOrEqualTo(2);
        assertThat(expenses.get(0).getExpenseDate())
                .isAfterOrEqualTo(expenses.get(1).getExpenseDate());
    }

    @Test
    @DisplayName("should find expenses by budget and category")
    void findByBudgetIdAndCategoryId() {
        Expense e = new Expense(new BigDecimal("30.00"), "With category",
                LocalDate.of(2026, 2, 10), testBudget, "testuser");
        e.setCategory(testCategory);
        expenseRepository.save(e);

        Expense e2 = new Expense(new BigDecimal("40.00"), "No category",
                LocalDate.of(2026, 2, 11), testBudget, "testuser");
        expenseRepository.save(e2);
        expenseRepository.flush();

        List<Expense> expenses = expenseRepository.findByBudgetIdAndCategoryId(
                testBudget.getId(), testCategory.getId());
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getDescription()).isEqualTo("With category");
    }

    @Test
    @DisplayName("should sum expense amounts by budget ID")
    void sumAmountByBudgetId() {
        expenseRepository.save(new Expense(new BigDecimal("30.50"), "E1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("40.75"), "E2",
                LocalDate.of(2026, 2, 11), testBudget, "testuser"));
        expenseRepository.flush();

        BigDecimal sum = expenseRepository.sumAmountByBudgetId(testBudget.getId());
        assertThat(sum).isEqualByComparingTo(new BigDecimal("71.25"));
    }

    @Test
    @DisplayName("should return zero when no expenses for budget")
    void sumAmountByBudgetId_noExpenses() {
        BigDecimal sum = expenseRepository.sumAmountByBudgetId(testBudget.getId());
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should count expenses by budget ID")
    void countByBudgetId() {
        expenseRepository.save(new Expense(new BigDecimal("30.00"), "E1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser"));
        expenseRepository.save(new Expense(new BigDecimal("40.00"), "E2",
                LocalDate.of(2026, 2, 11), testBudget, "testuser"));
        expenseRepository.flush();

        assertThat(expenseRepository.countByBudgetId(testBudget.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("should count expenses by category ID")
    void countByCategoryId() {
        Expense e = new Expense(new BigDecimal("30.00"), "E1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser");
        e.setCategory(testCategory);
        expenseRepository.save(e);
        expenseRepository.flush();

        assertThat(expenseRepository.countByCategoryId(testCategory.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("should cascade delete expenses when budget is deleted")
    void cascadeDeleteExpenses() {
        Expense e1 = new Expense(new BigDecimal("30.00"), "E1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser");
        Expense e2 = new Expense(new BigDecimal("40.00"), "E2",
                LocalDate.of(2026, 2, 11), testBudget, "testuser");
        expenseRepository.saveAll(List.of(e1, e2));
        expenseRepository.flush();

        Long budgetId = testBudget.getId();
        assertThat(expenseRepository.countByBudgetId(budgetId)).isEqualTo(2);

        budgetRepository.delete(testBudget);
        budgetRepository.flush();

        assertThat(expenseRepository.countByBudgetId(budgetId)).isEqualTo(0);
    }

    @Test
    @DisplayName("should get category breakdown for budget")
    void getCategoryBreakdown() {
        Category cat2 = categoryRepository.save(new Category("Dining", "🍽️", "testuser"));
        categoryRepository.flush();

        Expense e1 = new Expense(new BigDecimal("30.00"), "Groceries 1",
                LocalDate.of(2026, 2, 10), testBudget, "testuser");
        e1.setCategory(testCategory);

        Expense e2 = new Expense(new BigDecimal("50.00"), "Groceries 2",
                LocalDate.of(2026, 2, 11), testBudget, "testuser");
        e2.setCategory(testCategory);

        Expense e3 = new Expense(new BigDecimal("70.00"), "Dining 1",
                LocalDate.of(2026, 2, 12), testBudget, "testuser");
        e3.setCategory(cat2);

        expenseRepository.saveAll(List.of(e1, e2, e3));
        expenseRepository.flush();

        List<Object[]> breakdown = expenseRepository.getCategoryBreakdown(testBudget.getId());
        assertThat(breakdown).hasSize(2);
    }
}
