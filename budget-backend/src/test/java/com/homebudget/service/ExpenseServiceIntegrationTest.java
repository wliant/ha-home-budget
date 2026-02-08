package com.homebudget.service;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
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
class ExpenseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category testCategory;
    private BudgetDTO createdBudget;

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

        BudgetDTO budgetDTO = new BudgetDTO();
        budgetDTO.setYear(2026);
        budgetDTO.setMonth(2);
        budgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        budgetDTO.setCategoryId(testCategory.getId());
        createdBudget = budgetService.createBudget(budgetDTO, "testuser");
    }

    @Test
    @DisplayName("should create expense with real budget and category")
    void createExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Weekly groceries");
        dto.setExpenseDate(LocalDate.of(2026, 2, 15));
        dto.setBudgetId(createdBudget.getId());
        dto.setCategoryId(testCategory.getId());

        ExpenseDTO created = expenseService.createExpense(dto, "testuser");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(created.getDescription()).isEqualTo("Weekly groceries");
        assertThat(created.getBudgetId()).isEqualTo(createdBudget.getId());
        assertThat(created.getCategoryId()).isEqualTo(testCategory.getId());
        assertThat(created.getCategoryName()).isEqualTo("Groceries");
        assertThat(created.getCreatedBy()).isEqualTo("testuser");
        assertThat(created.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("should add date mismatch warning with real budget month")
    void createExpenseDateMismatch() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("March expense in Feb budget");
        dto.setExpenseDate(LocalDate.of(2026, 3, 15)); // March, but budget is February
        dto.setBudgetId(createdBudget.getId());

        ExpenseDTO created = expenseService.createExpense(dto, "testuser");

        assertThat(created.getWarnings()).isNotEmpty();
        assertThat(created.getWarnings().get(0)).contains("Warning");
        assertThat(created.getWarnings().get(0)).contains("2026-03-15");
    }

    @Test
    @DisplayName("should get all expenses with filter combinations against real data")
    void getAllExpensesWithFilters() {
        // Create multiple expenses
        ExpenseDTO dto1 = new ExpenseDTO();
        dto1.setAmount(new BigDecimal("30.00"));
        dto1.setDescription("Expense by user1");
        dto1.setExpenseDate(LocalDate.of(2026, 2, 5));
        dto1.setBudgetId(createdBudget.getId());
        dto1.setCategoryId(testCategory.getId());
        expenseService.createExpense(dto1, "user1");

        ExpenseDTO dto2 = new ExpenseDTO();
        dto2.setAmount(new BigDecimal("40.00"));
        dto2.setDescription("Expense by user2");
        dto2.setExpenseDate(LocalDate.of(2026, 2, 20));
        dto2.setBudgetId(createdBudget.getId());
        expenseService.createExpense(dto2, "user2");

        // No filter
        List<ExpenseDTO> all = expenseService.getAllExpenses(null, null, null, null, null);
        assertThat(all).hasSize(2);

        // Filter by budget
        List<ExpenseDTO> byBudget = expenseService.getAllExpenses(
                createdBudget.getId(), null, null, null, null);
        assertThat(byBudget).hasSize(2);

        // Filter by createdBy
        List<ExpenseDTO> byUser = expenseService.getAllExpenses(
                null, null, null, null, "user1");
        assertThat(byUser).hasSize(1);
        assertThat(byUser.get(0).getCreatedBy()).isEqualTo("user1");

        // Filter by date range
        List<ExpenseDTO> byDate = expenseService.getAllExpenses(
                null, null, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 25), null);
        assertThat(byDate).hasSize(1);
        assertThat(byDate.get(0).getDescription()).isEqualTo("Expense by user2");
    }

    @Test
    @DisplayName("should update expense with budget/category reassignment")
    void updateExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("Original");
        dto.setExpenseDate(LocalDate.of(2026, 2, 15));
        dto.setBudgetId(createdBudget.getId());
        ExpenseDTO created = expenseService.createExpense(dto, "testuser");

        // Update the expense
        ExpenseDTO updateDTO = new ExpenseDTO();
        updateDTO.setAmount(new BigDecimal("75.00"));
        updateDTO.setDescription("Updated");
        updateDTO.setExpenseDate(LocalDate.of(2026, 2, 20));
        updateDTO.setBudgetId(createdBudget.getId());
        updateDTO.setCategoryId(testCategory.getId());

        ExpenseDTO updated = expenseService.updateExpense(created.getId(), updateDTO);

        assertThat(updated.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(updated.getDescription()).isEqualTo("Updated");
        assertThat(updated.getCategoryId()).isEqualTo(testCategory.getId());
    }

    @Test
    @DisplayName("should delete expense and verify removal from DB")
    void deleteExpense() {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setAmount(new BigDecimal("50.00"));
        dto.setDescription("To delete");
        dto.setExpenseDate(LocalDate.of(2026, 2, 15));
        dto.setBudgetId(createdBudget.getId());
        ExpenseDTO created = expenseService.createExpense(dto, "testuser");

        expenseService.deleteExpense(created.getId());

        assertThat(expenseRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("should track expense count on budget after create and delete")
    void expenseCountOnBudget() {
        // Create 2 expenses
        for (int i = 0; i < 2; i++) {
            ExpenseDTO dto = new ExpenseDTO();
            dto.setAmount(new BigDecimal("50.00"));
            dto.setDescription("Expense " + i);
            dto.setExpenseDate(LocalDate.of(2026, 2, 10 + i));
            dto.setBudgetId(createdBudget.getId());
            expenseService.createExpense(dto, "testuser");
        }

        assertThat(expenseRepository.countByBudgetId(createdBudget.getId())).isEqualTo(2);

        // Delete one expense
        List<ExpenseDTO> expenses = expenseService.getAllExpenses(
                createdBudget.getId(), null, null, null, null);
        expenseService.deleteExpense(expenses.get(0).getId());

        assertThat(expenseRepository.countByBudgetId(createdBudget.getId())).isEqualTo(1);
    }
}
