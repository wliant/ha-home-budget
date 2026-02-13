package com.homebudget.repository;

import com.homebudget.config.AbstractIntegrationTest;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.model.ExpenseFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ExpenseFileRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseFileRepository expenseFileRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Expense testExpense;

    @BeforeEach
    void setUp() {
        expenseFileRepository.deleteAll();
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
                .filter(c -> c.getIsSystem() == null || !c.getIsSystem())
                .forEach(c -> categoryRepository.deleteById(c.getId()));
        categoryRepository.flush();

        Category category = categoryRepository.save(new Category("TestCat", "icon", "testuser"));
        categoryRepository.flush();

        Budget budget = new Budget(2026, 2, new BigDecimal("1000.00"), "Test", "testuser");
        budget.setCategory(category);
        budget = budgetRepository.save(budget);
        budgetRepository.flush();

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDescription("Test expense");
        expense.setExpenseDate(LocalDate.of(2026, 2, 15));
        expense.setBudget(budget);
        expense.setCategory(category);
        expense.setCreatedBy("testuser");
        testExpense = expenseRepository.save(expense);
        expenseRepository.flush();
    }

    @Test
    @DisplayName("should save and retrieve ExpenseFile")
    void saveAndRetrieve() {
        ExpenseFile file = new ExpenseFile();
        file.setExpense(testExpense);
        file.setFilePath("/uploads/receipt.pdf");
        file.setOriginalFilename("receipt.pdf");

        ExpenseFile saved = expenseFileRepository.save(file);
        expenseFileRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFilePath()).isEqualTo("/uploads/receipt.pdf");
        assertThat(saved.getOriginalFilename()).isEqualTo("receipt.pdf");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should find files by expense ID in order")
    void findByExpenseIdOrderByIdAsc() {
        ExpenseFile file1 = new ExpenseFile();
        file1.setExpense(testExpense);
        file1.setFilePath("/uploads/file1.pdf");
        file1.setOriginalFilename("file1.pdf");
        expenseFileRepository.save(file1);

        ExpenseFile file2 = new ExpenseFile();
        file2.setExpense(testExpense);
        file2.setFilePath("/uploads/file2.png");
        file2.setOriginalFilename("file2.png");
        expenseFileRepository.save(file2);
        expenseFileRepository.flush();

        List<ExpenseFile> files = expenseFileRepository.findByExpenseIdOrderByIdAsc(testExpense.getId());

        assertThat(files).hasSize(2);
        assertThat(files.get(0).getOriginalFilename()).isEqualTo("file1.pdf");
        assertThat(files.get(1).getOriginalFilename()).isEqualTo("file2.png");
        assertThat(files.get(0).getId()).isLessThan(files.get(1).getId());
    }

    @Test
    @DisplayName("should return empty list for nonexistent expense ID")
    void findByExpenseIdOrderByIdAsc_nonexistent() {
        List<ExpenseFile> files = expenseFileRepository.findByExpenseIdOrderByIdAsc(99999L);
        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("should count files by expense ID")
    void countByExpenseId() {
        ExpenseFile file1 = new ExpenseFile();
        file1.setExpense(testExpense);
        file1.setFilePath("/uploads/f1.pdf");
        file1.setOriginalFilename("f1.pdf");
        expenseFileRepository.save(file1);

        ExpenseFile file2 = new ExpenseFile();
        file2.setExpense(testExpense);
        file2.setFilePath("/uploads/f2.pdf");
        file2.setOriginalFilename("f2.pdf");
        expenseFileRepository.save(file2);
        expenseFileRepository.flush();

        long count = expenseFileRepository.countByExpenseId(testExpense.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should return zero count for nonexistent expense ID")
    void countByExpenseId_nonexistent() {
        long count = expenseFileRepository.countByExpenseId(99999L);
        assertThat(count).isZero();
    }
}
