package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
import com.homebudget.dto.BudgetValidationDTO;
import com.homebudget.dto.YearlyBudgetViewDTO;
import com.homebudget.dto.YearlyCategoryBudgetDTO;
import com.homebudget.exception.BudgetNotFoundException;
import com.homebudget.exception.CategoryNotFoundException;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BudgetService budgetService;

    // Common test data
    private Category parentCategory;
    private Category childCategory;
    private Category rootCategory;
    private Budget sampleBudget;
    private BudgetDTO sampleBudgetDTO;

    @BeforeEach
    void setUp() {
        // Root category (no parent)
        rootCategory = new Category("Groceries", "cart", "testuser");
        rootCategory.setId(1L);
        rootCategory.setParentCategory(null);

        // Parent category (no parent)
        parentCategory = new Category("Food", "utensils", "testuser");
        parentCategory.setId(10L);
        parentCategory.setParentCategory(null);

        // Child category (has parent)
        childCategory = new Category("Dining Out", "restaurant", "testuser");
        childCategory.setId(20L);
        childCategory.setParentCategory(parentCategory);

        // Sample budget entity
        sampleBudget = new Budget();
        sampleBudget.setId(1L);
        sampleBudget.setYear(2025);
        sampleBudget.setMonth(6);
        sampleBudget.setTotalAmount(new BigDecimal("1000.00"));
        sampleBudget.setDescription("June budget");
        sampleBudget.setCreatedBy("testuser");
        sampleBudget.setCreatedAt(LocalDateTime.of(2025, 6, 1, 10, 0));
        sampleBudget.setUpdatedAt(LocalDateTime.of(2025, 6, 1, 10, 0));
        sampleBudget.setVersion(0L);
        sampleBudget.setCategory(rootCategory);
        sampleBudget.setExpenses(new ArrayList<>());

        // Sample budget DTO for create/update requests
        sampleBudgetDTO = new BudgetDTO();
        sampleBudgetDTO.setYear(2025);
        sampleBudgetDTO.setMonth(6);
        sampleBudgetDTO.setTotalAmount(new BigDecimal("1000.00"));
        sampleBudgetDTO.setDescription("June budget");
        sampleBudgetDTO.setCategoryId(1L);
    }

    // ========== Helper Methods ==========

    private Budget createBudgetEntity(Long id, Integer year, Integer month, BigDecimal amount,
                                       String description, String createdBy, Category category) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setYear(year);
        budget.setMonth(month);
        budget.setTotalAmount(amount);
        budget.setDescription(description);
        budget.setCreatedBy(createdBy);
        budget.setCreatedAt(LocalDateTime.of(year, month, 1, 10, 0));
        budget.setUpdatedAt(LocalDateTime.of(year, month, 1, 10, 0));
        budget.setVersion(0L);
        budget.setCategory(category);
        budget.setExpenses(new ArrayList<>());
        return budget;
    }

    private Expense createExpense(Long id, BigDecimal amount, Budget budget, Category category) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setAmount(amount);
        expense.setDescription("Test expense");
        expense.setExpenseDate(LocalDate.of(budget.getYear(), budget.getMonth(), 15));
        expense.setBudget(budget);
        expense.setCategory(category);
        expense.setCreatedBy("testuser");
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
        expense.setVersion(0L);
        return expense;
    }

    // ========== createBudget Tests ==========

    @Nested
    @DisplayName("createBudget")
    class CreateBudgetTests {

        @Test
        @DisplayName("should create budget successfully when category exists and no duplicate")
        void createBudget_success() {
            // Arrange - sampleBudgetDTO has month=6 (monthly budget)
            // Monthly path requires a parent yearly budget
            Budget parentYearlyBudget = new Budget();
            parentYearlyBudget.setId(99L);
            parentYearlyBudget.setYear(2025);
            parentYearlyBudget.setMonth(null);
            parentYearlyBudget.setTotalAmount(new BigDecimal("12000.00"));
            parentYearlyBudget.setCreatedBy("testuser");
            parentYearlyBudget.setCategory(rootCategory);
            parentYearlyBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            // Monthly path: findParentBudget → parent yearly budget exists
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentYearlyBudget));
            // sumMonthlyBudgetsForCategory → no existing monthly budgets
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);
            // ensureMonthlyBudgetsForRemainingMonths: checks existsByCategoryIdAndYearAndMonth for months 6-12
            // (already stubbed month=6 to false, rest default to false → creates budgets for each)
            // reassignParentExpensesToMonthlyBudgets: calls findByBudgetId on parent → empty
            when(expenseRepository.findByBudgetId(99L)).thenReturn(Collections.emptyList());

            // Act
            BudgetDTO result = budgetService.createBudget(sampleBudgetDTO, "testuser");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getMonth()).isEqualTo(6);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.getDescription()).isEqualTo("June budget");
            assertThat(result.getCreatedBy()).isEqualTo("testuser");
            assertThat(result.getCategoryId()).isEqualTo(1L);
            assertThat(result.getCategory()).isNotNull();
            assertThat(result.getCategory().getName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void createBudget_categoryNotFound() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(sampleBudgetDTO, "testuser"))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw DuplicateBudgetException when budget already exists for category and period")
        void createBudget_duplicateBudget() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(sampleBudgetDTO, "testuser"))
                    .isInstanceOf(DuplicateBudgetException.class)
                    .hasMessageContaining("Groceries")
                    .hasMessageContaining("2025");

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when child category has no parent yearly budget")
        void createBudget_childCategoryWithoutParentBudget() {
            // Arrange
            BudgetDTO childBudgetDTO = new BudgetDTO();
            childBudgetDTO.setYear(2025);
            childBudgetDTO.setMonth(6);
            childBudgetDTO.setTotalAmount(new BigDecimal("300.00"));
            childBudgetDTO.setDescription("Dining out budget");
            childBudgetDTO.setCategoryId(20L);

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            // Monthly path: findParentBudget(20L, 2025) → no yearly budget exists for this category
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.empty());

            // Act & Assert - throws ParentBudgetMismatchException (not IllegalArgumentException)
            // because production code checks parentBudget == null && !createParentBudget
            assertThatThrownBy(() -> budgetService.createBudget(childBudgetDTO, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class)
                    .hasMessageContaining("Parent yearly budget is required");

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when monthly sum exceeds parent yearly budget")
        void createBudget_parentBudgetMismatch() {
            // Arrange - creating monthly budget for child category
            BudgetDTO childBudgetDTO = new BudgetDTO();
            childBudgetDTO.setYear(2025);
            childBudgetDTO.setMonth(6);
            childBudgetDTO.setTotalAmount(new BigDecimal("300.00"));
            childBudgetDTO.setDescription("Dining out budget");
            childBudgetDTO.setCategoryId(20L);

            // Parent yearly budget for category 20 (the budget's own yearly parent)
            Budget parentYearlyBudget = new Budget();
            parentYearlyBudget.setId(100L);
            parentYearlyBudget.setYear(2025);
            parentYearlyBudget.setMonth(null);
            parentYearlyBudget.setTotalAmount(new BigDecimal("500.00"));
            parentYearlyBudget.setCreatedBy("testuser");
            parentYearlyBudget.setCategory(childCategory);
            parentYearlyBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            // Monthly path: findParentBudget(20L, 2025) → yearly parent budget with amount 500
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.of(parentYearlyBudget));
            // Existing monthly budgets sum to 300, adding 300 = 600 > 500 (parent yearly)
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(new BigDecimal("300.00"));

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(childBudgetDTO, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class);

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when monthly sum exceeds yearly budget for parent category")
        void createBudget_parentCategoryWithChildSumMismatch() {
            // Arrange - creating monthly budget for parent category (id=10)
            // The monthly path requires a yearly parent budget for this category
            BudgetDTO parentBudgetDTO = new BudgetDTO();
            parentBudgetDTO.setYear(2025);
            parentBudgetDTO.setMonth(6);
            parentBudgetDTO.setTotalAmount(new BigDecimal("1000.00"));
            parentBudgetDTO.setDescription("Food budget");
            parentBudgetDTO.setCategoryId(10L);

            // Yearly parent budget for category 10, with amount 800
            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(200L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("800.00"));
            yearlyBudget.setCreatedBy("testuser");
            yearlyBudget.setCategory(parentCategory);
            yearlyBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(parentCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(false);
            // Monthly path: findParentBudget(10L, 2025) → yearly budget with amount 800
            when(budgetRepository.findParentBudget(10L, 2025)).thenReturn(Optional.of(yearlyBudget));
            // Existing monthly sum = 0, adding 1000 = 1000 > 800 (yearly) → mismatch
            when(budgetRepository.sumMonthlyBudgetsForCategory(10L, 2025)).thenReturn(BigDecimal.ZERO);

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(parentBudgetDTO, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class)
                    .hasMessageContaining("1000.00")
                    .hasMessageContaining("800.00");

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when categoryId is null")
        void createBudget_nullCategoryId() {
            // Arrange
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setCategoryId(null);

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(dto, "testuser"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category ID is required");

            verify(budgetRepository, never()).save(any(Budget.class));
        }
    }

    // ========== getAllBudgets Tests ==========

    @Nested
    @DisplayName("getAllBudgets")
    class GetAllBudgetsTests {

        @Test
        @DisplayName("should return list of BudgetSummaryDTO ordered by year desc, month desc")
        void getAllBudgets_returnsList() {
            // Arrange
            Budget budget1 = createBudgetEntity(1L, 2025, 6, new BigDecimal("1000.00"),
                    "June", "testuser", rootCategory);
            Budget budget2 = createBudgetEntity(2L, 2025, 5, new BigDecimal("800.00"),
                    "May", "testuser", rootCategory);

            when(budgetRepository.findAllByOrderByYearDescMonthDesc())
                    .thenReturn(List.of(budget1, budget2));

            // Stub for mapToBudgetSummary -> calculateTotalSpending and countByBudgetId
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("250.00"));
            when(expenseRepository.sumAmountByBudgetId(2L)).thenReturn(new BigDecimal("400.00"));
            when(expenseRepository.countByBudgetId(1L)).thenReturn(3L);
            when(expenseRepository.countByBudgetId(2L)).thenReturn(5L);

            // Act
            List<BudgetSummaryDTO> result = budgetService.getAllBudgets();

            // Assert
            assertThat(result).hasSize(2);

            // First budget (June 2025)
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getYear()).isEqualTo(2025);
            assertThat(result.get(0).getMonth()).isEqualTo(6);
            assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.get(0).getTotalSpending()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(result.get(0).getSpendingPercentage()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.get(0).getExpenseCount()).isEqualTo(3L);

            // Second budget (May 2025)
            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getYear()).isEqualTo(2025);
            assertThat(result.get(1).getMonth()).isEqualTo(5);
            assertThat(result.get(1).getTotalSpending()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(result.get(1).getSpendingPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.get(1).getExpenseCount()).isEqualTo(5L);

            verify(budgetRepository).findAllByOrderByYearDescMonthDesc();
        }

        @Test
        @DisplayName("should return empty list when no budgets exist")
        void getAllBudgets_emptyList() {
            // Arrange
            when(budgetRepository.findAllByOrderByYearDescMonthDesc()).thenReturn(Collections.emptyList());

            // Act
            List<BudgetSummaryDTO> result = budgetService.getAllBudgets();

            // Assert
            assertThat(result).isEmpty();
            verify(budgetRepository).findAllByOrderByYearDescMonthDesc();
        }
    }

    // ========== getBudgetById Tests ==========

    @Nested
    @DisplayName("getBudgetById")
    class GetBudgetByIdTests {

        @Test
        @DisplayName("should return BudgetSummaryDTO with expenses when budget exists")
        void getBudgetById_success() {
            // Arrange
            Expense expense = createExpense(10L, new BigDecimal("50.00"), sampleBudget, rootCategory);
            sampleBudget.getExpenses().add(expense);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("50.00"));
            when(expenseRepository.countByBudgetId(1L)).thenReturn(1L);

            // Act
            BudgetSummaryDTO result = budgetService.getBudgetById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getMonth()).isEqualTo(6);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.getTotalSpending()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getSpendingPercentage()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.getExpenseCount()).isEqualTo(1L);
            assertThat(result.getExpenses()).hasSize(1);
            assertThat(result.getExpenses().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

            verify(budgetRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw BudgetNotFoundException when budget does not exist")
        void getBudgetById_notFound() {
            // Arrange
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.getBudgetById(999L))
                    .isInstanceOf(BudgetNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ========== updateBudget Tests ==========

    @Nested
    @DisplayName("updateBudget")
    class UpdateBudgetTests {

        @Test
        @DisplayName("should update totalAmount and description successfully")
        void updateBudget_success() {
            // Arrange
            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("1500.00"));
            updateDTO.setDescription("Updated June budget");

            Budget updatedBudget = createBudgetEntity(1L, 2025, 6, new BigDecimal("1500.00"),
                    "Updated June budget", "testuser", rootCategory);

            // sampleBudget has month=6 (monthly), so updateBudget enters the monthly path:
            // calls findParentBudget(categoryId=1, year=2025) and sumMonthlyBudgetsForCategory
            Budget parentYearlyBudget = new Budget();
            parentYearlyBudget.setId(99L);
            parentYearlyBudget.setYear(2025);
            parentYearlyBudget.setMonth(null);
            parentYearlyBudget.setTotalAmount(new BigDecimal("12000.00"));
            parentYearlyBudget.setDescription("Yearly budget");
            parentYearlyBudget.setCreatedBy("testuser");
            parentYearlyBudget.setCategory(rootCategory);
            parentYearlyBudget.setExpenses(new ArrayList<>());

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentYearlyBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(new BigDecimal("1000.00"));
            when(budgetRepository.save(any(Budget.class))).thenReturn(updatedBudget);

            // Act
            BudgetDTO result = budgetService.updateBudget(1L, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(result.getDescription()).isEqualTo("Updated June budget");

            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw BudgetNotFoundException when budget does not exist")
        void updateBudget_notFound() {
            // Arrange
            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("1500.00"));
            updateDTO.setDescription("Updated");

            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.updateBudget(999L, updateDTO))
                    .isInstanceOf(BudgetNotFoundException.class)
                    .hasMessageContaining("999");

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when amount change violates child budget constraint")
        void updateBudget_parentBudgetMismatchOnChildCategory() {
            // Arrange - budget for child category (monthly), changing amount
            Budget childBudget = createBudgetEntity(5L, 2025, 6, new BigDecimal("200.00"),
                    "Dining out", "testuser", childCategory);

            // Parent yearly budget for the same category (id=20)
            Budget parentYearlyBudget = new Budget();
            parentYearlyBudget.setId(100L);
            parentYearlyBudget.setYear(2025);
            parentYearlyBudget.setMonth(null);
            parentYearlyBudget.setTotalAmount(new BigDecimal("500.00"));
            parentYearlyBudget.setDescription("Yearly dining");
            parentYearlyBudget.setCreatedBy("testuser");
            parentYearlyBudget.setCategory(childCategory);
            parentYearlyBudget.setExpenses(new ArrayList<>());

            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("400.00")); // changing from 200 to 400
            updateDTO.setDescription("Updated dining out");

            when(budgetRepository.findById(5L)).thenReturn(Optional.of(childBudget));
            // Monthly path: findParentBudget(categoryId=20, year=2025) returns yearly budget
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.of(parentYearlyBudget));
            // sumMonthlyBudgetsForCategory returns current sum of all monthly budgets
            // existing sum=400 (two budgets of 200 each), subtract old (200) + add new (400) = 600 > 500 (parent)
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(new BigDecimal("400.00"));

            // Act & Assert
            assertThatThrownBy(() -> budgetService.updateBudget(5L, updateDTO))
                    .isInstanceOf(ParentBudgetMismatchException.class);

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should not validate parent budget when amount does not change")
        void updateBudget_sameAmount_noValidation() {
            // Arrange - same amount, only description changes
            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("1000.00")); // same as sampleBudget
            updateDTO.setDescription("Updated description only");

            Budget savedBudget = createBudgetEntity(1L, 2025, 6, new BigDecimal("1000.00"),
                    "Updated description only", "testuser", rootCategory);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

            // Act
            BudgetDTO result = budgetService.updateBudget(1L, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getDescription()).isEqualTo("Updated description only");

            // Should not call parent budget validation repos
            verify(budgetRepository, never()).findByCategoryIdAndYearAndMonth(anyLong(), anyInt(), anyInt());
            verify(budgetRepository, never()).findChildBudgets(anyLong(), anyInt(), anyInt());
            verify(budgetRepository, never()).sumByParentCategoryAndPeriod(anyLong(), anyInt(), anyInt());
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when parent amount change mismatches child sum")
        void updateBudget_parentCategoryAmountMismatchesChildSum() {
            // Arrange - yearly budget (month=null) for parent category, changing amount when monthly children exist
            Budget parentBudgetEntity = new Budget();
            parentBudgetEntity.setId(100L);
            parentBudgetEntity.setYear(2025);
            parentBudgetEntity.setMonth(null);
            parentBudgetEntity.setTotalAmount(new BigDecimal("500.00"));
            parentBudgetEntity.setDescription("Food yearly");
            parentBudgetEntity.setCreatedBy("testuser");
            parentBudgetEntity.setCreatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            parentBudgetEntity.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));
            parentBudgetEntity.setVersion(0L);
            parentBudgetEntity.setCategory(parentCategory);
            parentBudgetEntity.setExpenses(new ArrayList<>());

            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("400.00")); // less than monthly sum of 500
            updateDTO.setDescription("Updated Food budget");

            when(budgetRepository.findById(100L)).thenReturn(Optional.of(parentBudgetEntity));
            // Yearly path (month==null): calls sumMonthlyBudgetsForCategory(10L, 2025)
            // Monthly sum = 500, new yearly amount = 400, 400 < 500 → throws exception
            when(budgetRepository.sumMonthlyBudgetsForCategory(10L, 2025)).thenReturn(new BigDecimal("500.00"));

            // Act & Assert
            assertThatThrownBy(() -> budgetService.updateBudget(100L, updateDTO))
                    .isInstanceOf(ParentBudgetMismatchException.class);

            verify(budgetRepository, never()).save(any(Budget.class));
        }
    }

    // ========== deleteBudget Tests ==========

    @Nested
    @DisplayName("deleteBudget")
    class DeleteBudgetTests {

        @Test
        @DisplayName("should delete budget successfully when it exists")
        void deleteBudget_success() {
            // Arrange
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));

            // Act
            budgetService.deleteBudget(1L);

            // Assert
            verify(budgetRepository).delete(sampleBudget);
        }

        @Test
        @DisplayName("should throw BudgetNotFoundException when budget does not exist")
        void deleteBudget_notFound() {
            // Arrange
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.deleteBudget(999L))
                    .isInstanceOf(BudgetNotFoundException.class)
                    .hasMessageContaining("999");

            verify(budgetRepository, never()).delete(any(Budget.class));
        }
    }

    // ========== calculateTotalSpending Tests ==========

    @Nested
    @DisplayName("calculateTotalSpending")
    class CalculateTotalSpendingTests {

        @Test
        @DisplayName("should return sum from expenseRepository")
        void calculateTotalSpending_returnsSum() {
            // Arrange
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("350.75"));

            // Act
            BigDecimal result = budgetService.calculateTotalSpending(1L);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("350.75"));
            verify(expenseRepository).sumAmountByBudgetId(1L);
        }

        @Test
        @DisplayName("should return zero when no expenses exist")
        void calculateTotalSpending_noExpenses() {
            // Arrange - COALESCE in the query returns 0
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(BigDecimal.ZERO);

            // Act
            BigDecimal result = budgetService.calculateTotalSpending(1L);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ========== calculateSpendingPercentage Tests ==========

    @Nested
    @DisplayName("calculateSpendingPercentage")
    class CalculateSpendingPercentageTests {

        @Test
        @DisplayName("should return BigDecimal.ZERO when spending is zero")
        void calculateSpendingPercentage_zeroSpending() {
            // Arrange
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(BigDecimal.ZERO);

            // Act
            BigDecimal result = budgetService.calculateSpendingPercentage(sampleBudget);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return correct percentage for positive spending")
        void calculateSpendingPercentage_positiveSpending() {
            // Arrange - sampleBudget totalAmount = 1000.00
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("250.00"));

            // Act
            BigDecimal result = budgetService.calculateSpendingPercentage(sampleBudget);

            // Assert - 250/1000 * 100 = 25.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("should return 100% when spending equals budget")
        void calculateSpendingPercentage_fullSpending() {
            // Arrange
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("1000.00"));

            // Act
            BigDecimal result = budgetService.calculateSpendingPercentage(sampleBudget);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should return percentage greater than 100 when overspent")
        void calculateSpendingPercentage_overspent() {
            // Arrange
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("1500.00"));

            // Act
            BigDecimal result = budgetService.calculateSpendingPercentage(sampleBudget);

            // Assert - 1500/1000 * 100 = 150.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should handle fractional percentages with 2 decimal places")
        void calculateSpendingPercentage_fractional() {
            // Arrange - sampleBudget totalAmount = 1000.00
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("333.33"));

            // Act
            BigDecimal result = budgetService.calculateSpendingPercentage(sampleBudget);

            // Assert - 333.33/1000 * 100 = 33.33 (with rounding)
            BigDecimal expected = new BigDecimal("333.33")
                    .divide(new BigDecimal("1000.00"), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            assertThat(result).isEqualByComparingTo(expected);
        }
    }

    // ========== hasExpenses Tests ==========

    @Nested
    @DisplayName("hasExpenses")
    class HasExpensesTests {

        @Test
        @DisplayName("should return true when count is greater than 0")
        void hasExpenses_true() {
            // Arrange
            when(expenseRepository.countByBudgetId(1L)).thenReturn(5L);

            // Act
            boolean result = budgetService.hasExpenses(1L);

            // Assert
            assertThat(result).isTrue();
            verify(expenseRepository).countByBudgetId(1L);
        }

        @Test
        @DisplayName("should return false when count is 0")
        void hasExpenses_false() {
            // Arrange
            when(expenseRepository.countByBudgetId(1L)).thenReturn(0L);

            // Act
            boolean result = budgetService.hasExpenses(1L);

            // Assert
            assertThat(result).isFalse();
            verify(expenseRepository).countByBudgetId(1L);
        }
    }

    // ========== getCurrentMonthBudget Tests ==========

    @Nested
    @DisplayName("getCurrentMonthBudget")
    class GetCurrentMonthBudgetTests {

        @Test
        @DisplayName("should return aggregated BudgetSummaryDTO for current year and month")
        void getCurrentMonthBudget_success() {
            // Arrange
            LocalDate now = LocalDate.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();

            Budget currentBudget = createBudgetEntity(50L, currentYear, currentMonth,
                    new BigDecimal("2000.00"), "Current month", "testuser", rootCategory);

            // getCurrentMonthBudget delegates to getMonthBudgetSummary which uses
            // findByYearAndMonthOrderByCategoryIdAsc (returns List<Budget>)
            when(budgetRepository.findByYearAndMonthOrderByCategoryIdAsc(currentYear, currentMonth))
                    .thenReturn(List.of(currentBudget));
            when(expenseRepository.sumAmountByBudgetId(50L)).thenReturn(new BigDecimal("500.00"));
            when(expenseRepository.countByBudgetId(50L)).thenReturn(10L);

            // Act
            BudgetSummaryDTO result = budgetService.getCurrentMonthBudget();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(currentYear);
            assertThat(result.getMonth()).isEqualTo(currentMonth);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.getTotalSpending()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getSpendingPercentage()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(result.getExpenseCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should return null when no budget exists for current month")
        void getCurrentMonthBudget_notFound() {
            // Arrange
            LocalDate now = LocalDate.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();

            when(budgetRepository.findByYearAndMonthOrderByCategoryIdAsc(currentYear, currentMonth))
                    .thenReturn(Collections.emptyList());

            // Act
            BudgetSummaryDTO result = budgetService.getCurrentMonthBudget();

            // Assert
            assertThat(result).isNull();
        }
    }

    // ========== getBudgetValidation Tests ==========

    @Nested
    @DisplayName("getBudgetValidation")
    class GetBudgetValidationTests {

        @Test
        @DisplayName("should return validation hints for monthly budget - no duplicate, no parent")
        void getBudgetValidation_monthly_noDuplicate_noParent() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(0L);

            BudgetValidationDTO result = budgetService.getBudgetValidation(1L, 2025, 6);

            assertThat(result).isNotNull();
            assertThat(result.isDuplicate()).isFalse();
            assertThat(result.getDuplicateMessage()).isNull();
            assertThat(result.isParentBudgetExists()).isFalse();
            assertThat(result.getMonthlyBudgetSum()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.isMonthlyBudgetsExist()).isFalse();
        }

        @Test
        @DisplayName("should detect duplicate monthly budget")
        void getBudgetValidation_monthly_duplicate() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(true);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(0L);

            BudgetValidationDTO result = budgetService.getBudgetValidation(1L, 2025, 6);

            assertThat(result.isDuplicate()).isTrue();
            assertThat(result.getDuplicateMessage()).contains("Groceries").contains("2025-06");
        }

        @Test
        @DisplayName("should return parent budget info when it exists")
        void getBudgetValidation_withParentBudget() {
            Budget parentBudget = new Budget();
            parentBudget.setId(99L);
            parentBudget.setTotalAmount(new BigDecimal("12000.00"));

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(new BigDecimal("3000.00"));
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(3L);

            BudgetValidationDTO result = budgetService.getBudgetValidation(1L, 2025, 6);

            assertThat(result.isParentBudgetExists()).isTrue();
            assertThat(result.getParentBudgetId()).isEqualTo(99L);
            assertThat(result.getParentBudgetAmount()).isEqualByComparingTo(new BigDecimal("12000.00"));
            assertThat(result.getMonthlyBudgetSum()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.isMonthlyBudgetsExist()).isTrue();
        }

        @Test
        @DisplayName("should detect duplicate yearly budget (month is null)")
        void getBudgetValidation_yearly_duplicate() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsParentBudget(1L, 2025)).thenReturn(true);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(0L);

            BudgetValidationDTO result = budgetService.getBudgetValidation(1L, 2025, null);

            assertThat(result.isDuplicate()).isTrue();
            assertThat(result.getDuplicateMessage()).contains("Groceries").contains("2025");
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void getBudgetValidation_categoryNotFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.getBudgetValidation(999L, 2025, 6))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        @DisplayName("should include parent category budget info for child category with monthly budget")
        void getBudgetValidation_childCategory_withParentCategoryBudget() {
            Budget parentCatBudget = new Budget();
            parentCatBudget.setId(200L);
            parentCatBudget.setTotalAmount(new BigDecimal("5000.00"));

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(20L, 2025)).thenReturn(0L);
            // Parent category budget lookup for month=6
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(Optional.of(parentCatBudget));

            BudgetValidationDTO result = budgetService.getBudgetValidation(20L, 2025, 6);

            assertThat(result.getParentCategoryName()).isEqualTo("Food");
            assertThat(result.isParentCategoryBudgetExists()).isTrue();
            assertThat(result.getParentCategoryBudgetId()).isEqualTo(200L);
            assertThat(result.getParentCategoryBudgetAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("should indicate no parent category budget for child category when none exists")
        void getBudgetValidation_childCategory_noParentCategoryBudget() {
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(20L, 2025)).thenReturn(0L);
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(Optional.empty());

            BudgetValidationDTO result = budgetService.getBudgetValidation(20L, 2025, 6);

            assertThat(result.getParentCategoryName()).isEqualTo("Food");
            assertThat(result.isParentCategoryBudgetExists()).isFalse();
        }

        @Test
        @DisplayName("should include parent category yearly budget info for child category when month is null")
        void getBudgetValidation_childCategory_yearlyParentCategoryBudget() {
            Budget parentCatYearlyBudget = new Budget();
            parentCatYearlyBudget.setId(300L);
            parentCatYearlyBudget.setTotalAmount(new BigDecimal("60000.00"));

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsParentBudget(20L, 2025)).thenReturn(false);
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.countMonthlyBudgetsForCategory(20L, 2025)).thenReturn(0L);
            // Yearly parent category budget lookup (month null → findParentBudget for parentCatId)
            when(budgetRepository.findParentBudget(10L, 2025)).thenReturn(Optional.of(parentCatYearlyBudget));

            BudgetValidationDTO result = budgetService.getBudgetValidation(20L, 2025, null);

            assertThat(result.getParentCategoryName()).isEqualTo("Food");
            assertThat(result.isParentCategoryBudgetExists()).isTrue();
            assertThat(result.getParentCategoryBudgetId()).isEqualTo(300L);
            assertThat(result.getParentCategoryBudgetAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
        }
    }

    // ========== getYearlyBudgetView Tests ==========

    @Nested
    @DisplayName("getYearlyBudgetView")
    class GetYearlyBudgetViewTests {

        @Test
        @DisplayName("should return yearly view with categories and monthly breakdowns")
        void getYearlyBudgetView_withData() {
            // Yearly parent budget for rootCategory
            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(100L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("12000.00"));
            yearlyBudget.setCategory(rootCategory);

            // Monthly budget for January
            Budget janBudget = new Budget();
            janBudget.setId(101L);
            janBudget.setYear(2025);
            janBudget.setMonth(1);
            janBudget.setTotalAmount(new BigDecimal("1000.00"));
            janBudget.setCategory(rootCategory);

            when(budgetRepository.findByYear(2025)).thenReturn(List.of(yearlyBudget, janBudget));
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(rootCategory));
            when(categoryRepository.countByParentCategoryId(1L)).thenReturn(0L);
            // Spending for January budget
            when(expenseRepository.sumAmountByBudgetId(101L)).thenReturn(new BigDecimal("250.00"));
            // Spending for yearly parent budget
            when(expenseRepository.sumAmountByBudgetId(100L)).thenReturn(new BigDecimal("50.00"));

            YearlyBudgetViewDTO result = budgetService.getYearlyBudgetView(2025);

            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getCategories()).hasSize(1);
            assertThat(result.getTotalBudget()).isEqualByComparingTo(new BigDecimal("12000.00"));

            YearlyCategoryBudgetDTO cat = result.getCategories().get(0);
            assertThat(cat.getCategoryId()).isEqualTo(1L);
            assertThat(cat.getCategoryName()).isEqualTo("Groceries");
            assertThat(cat.getYearlyBudgetAmount()).isEqualByComparingTo(new BigDecimal("12000.00"));
            assertThat(cat.getMonthlyBudgetSum()).isEqualByComparingTo(new BigDecimal("1000.00"));
            // January has budget
            assertThat(cat.getMonths().get(0).isHasBudget()).isTrue();
            assertThat(cat.getMonths().get(0).getBudgetAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(cat.getMonths().get(0).getSpending()).isEqualByComparingTo(new BigDecimal("250.00"));
            // February has no budget
            assertThat(cat.getMonths().get(1).isHasBudget()).isFalse();
        }

        @Test
        @DisplayName("should return empty view when no categories exist")
        void getYearlyBudgetView_noCategories() {
            when(budgetRepository.findByYear(2025)).thenReturn(Collections.emptyList());
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(Collections.emptyList());

            YearlyBudgetViewDTO result = budgetService.getYearlyBudgetView(2025);

            assertThat(result).isNotNull();
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getCategories()).isEmpty();
            assertThat(result.getTotalBudget()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalSpending()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should include parent category with child spending aggregation")
        void getYearlyBudgetView_parentCategoryWithChildren() {
            Budget parentCatBudget = new Budget();
            parentCatBudget.setId(200L);
            parentCatBudget.setYear(2025);
            parentCatBudget.setMonth(null);
            parentCatBudget.setTotalAmount(new BigDecimal("5000.00"));
            parentCatBudget.setCategory(parentCategory);

            when(budgetRepository.findByYear(2025)).thenReturn(List.of(parentCatBudget));
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(parentCategory));
            // parentCategory has children
            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(2L);
            when(expenseRepository.sumAmountByBudgetId(200L)).thenReturn(BigDecimal.ZERO);
            // Child spending for each month
            for (int m = 1; m <= 12; m++) {
                when(expenseRepository.sumExpensesByParentCategoryBudgets(10L, 2025, m))
                        .thenReturn(m == 1 ? new BigDecimal("100.00") : BigDecimal.ZERO);
            }

            YearlyBudgetViewDTO result = budgetService.getYearlyBudgetView(2025);

            assertThat(result.getCategories()).hasSize(1);
            YearlyCategoryBudgetDTO cat = result.getCategories().get(0);
            assertThat(cat.getCategoryName()).isEqualTo("Food");
            // Yearly spending includes child spending from month 1 (100.00)
            assertThat(cat.getYearlySpending()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should include parent category info in category DTO")
        void getYearlyBudgetView_childCategoryIncludesParentInfo() {
            when(budgetRepository.findByYear(2025)).thenReturn(Collections.emptyList());
            when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(childCategory));
            when(categoryRepository.countByParentCategoryId(20L)).thenReturn(0L);

            YearlyBudgetViewDTO result = budgetService.getYearlyBudgetView(2025);

            assertThat(result.getCategories()).hasSize(1);
            YearlyCategoryBudgetDTO cat = result.getCategories().get(0);
            assertThat(cat.getParentCategoryId()).isEqualTo(10L);
            assertThat(cat.getParentCategoryName()).isEqualTo("Food");
        }
    }

    // ========== createBudget Yearly Path Tests ==========

    @Nested
    @DisplayName("createBudget - yearly path")
    class CreateBudgetYearlyTests {

        @Test
        @DisplayName("should create yearly budget (month=null) successfully")
        void createBudget_yearly_success() {
            BudgetDTO yearlyDTO = new BudgetDTO();
            yearlyDTO.setYear(2025);
            yearlyDTO.setMonth(null);
            yearlyDTO.setTotalAmount(new BigDecimal("12000.00"));
            yearlyDTO.setDescription("Yearly groceries");
            yearlyDTO.setCategoryId(1L);

            Budget savedBudget = new Budget();
            savedBudget.setId(50L);
            savedBudget.setYear(2025);
            savedBudget.setMonth(null);
            savedBudget.setTotalAmount(new BigDecimal("12000.00"));
            savedBudget.setDescription("Yearly groceries");
            savedBudget.setCreatedBy("testuser");
            savedBudget.setCreatedAt(LocalDateTime.now());
            savedBudget.setUpdatedAt(LocalDateTime.now());
            savedBudget.setVersion(0L);
            savedBudget.setCategory(rootCategory);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsParentBudget(1L, 2025)).thenReturn(false);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

            BudgetDTO result = budgetService.createBudget(yearlyDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.getMonth()).isNull();
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("12000.00"));
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw DuplicateBudgetException for duplicate yearly budget")
        void createBudget_yearly_duplicate() {
            BudgetDTO yearlyDTO = new BudgetDTO();
            yearlyDTO.setYear(2025);
            yearlyDTO.setMonth(null);
            yearlyDTO.setTotalAmount(new BigDecimal("12000.00"));
            yearlyDTO.setCategoryId(1L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsParentBudget(1L, 2025)).thenReturn(true);

            assertThatThrownBy(() -> budgetService.createBudget(yearlyDTO, "testuser"))
                    .isInstanceOf(DuplicateBudgetException.class)
                    .hasMessageContaining("Yearly budget already exists");
        }

        @Test
        @DisplayName("should create yearly budget with auto-create children")
        void createBudget_yearly_autoCreateChildren() {
            BudgetDTO yearlyDTO = new BudgetDTO();
            yearlyDTO.setYear(2025);
            yearlyDTO.setMonth(null);
            yearlyDTO.setTotalAmount(new BigDecimal("12000.00"));
            yearlyDTO.setDescription("Yearly groceries");
            yearlyDTO.setCategoryId(1L);
            yearlyDTO.setAutoCreateChildren(true);

            Budget savedBudget = new Budget();
            savedBudget.setId(50L);
            savedBudget.setYear(2025);
            savedBudget.setMonth(null);
            savedBudget.setTotalAmount(new BigDecimal("12000.00"));
            savedBudget.setDescription("Yearly groceries");
            savedBudget.setCreatedBy("testuser");
            savedBudget.setCreatedAt(LocalDateTime.now());
            savedBudget.setUpdatedAt(LocalDateTime.now());
            savedBudget.setVersion(0L);
            savedBudget.setCategory(rootCategory);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsParentBudget(1L, 2025)).thenReturn(false);
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(0L);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

            BudgetDTO result = budgetService.createBudget(yearlyDTO, "testuser");

            assertThat(result).isNotNull();
            // 1 yearly + 12 monthly = 13 saves
            verify(budgetRepository, atLeastOnce()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw when auto-create children but monthly budgets already exist")
        void createBudget_yearly_autoCreateChildren_duplicateMonthly() {
            BudgetDTO yearlyDTO = new BudgetDTO();
            yearlyDTO.setYear(2025);
            yearlyDTO.setMonth(null);
            yearlyDTO.setTotalAmount(new BigDecimal("12000.00"));
            yearlyDTO.setCategoryId(1L);
            yearlyDTO.setAutoCreateChildren(true);

            Budget savedYearly = new Budget();
            savedYearly.setId(50L);
            savedYearly.setYear(2025);
            savedYearly.setMonth(null);
            savedYearly.setTotalAmount(new BigDecimal("12000.00"));
            savedYearly.setCategory(rootCategory);
            savedYearly.setCreatedBy("testuser");
            savedYearly.setCreatedAt(LocalDateTime.now());
            savedYearly.setUpdatedAt(LocalDateTime.now());
            savedYearly.setVersion(0L);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsParentBudget(1L, 2025)).thenReturn(false);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedYearly);
            when(budgetRepository.countMonthlyBudgetsForCategory(1L, 2025)).thenReturn(3L);

            assertThatThrownBy(() -> budgetService.createBudget(yearlyDTO, "testuser"))
                    .isInstanceOf(DuplicateBudgetException.class)
                    .hasMessageContaining("Monthly budgets already exist");
        }
    }

    // ========== createBudget with createParentBudget Tests ==========

    @Nested
    @DisplayName("createBudget - createParentBudget path")
    class CreateBudgetWithParentCreationTests {

        @Test
        @DisplayName("should create parent budget when createParentBudget is true and no parent exists")
        void createBudget_createParentBudget_success() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setDescription("Monthly budget");
            dto.setCategoryId(1L);
            dto.setCreateParentBudget(true);
            dto.setParentTotalAmount(new BigDecimal("12000.00"));

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);

            Budget savedParent = new Budget();
            savedParent.setId(99L);
            savedParent.setYear(2025);
            savedParent.setMonth(null);
            savedParent.setTotalAmount(new BigDecimal("12000.00"));
            savedParent.setCategory(rootCategory);
            savedParent.setCreatedBy("testuser");
            savedParent.setExpenses(new ArrayList<>());

            Budget savedMonthly = new Budget();
            savedMonthly.setId(100L);
            savedMonthly.setYear(2025);
            savedMonthly.setMonth(6);
            savedMonthly.setTotalAmount(new BigDecimal("1000.00"));
            savedMonthly.setDescription("Monthly budget");
            savedMonthly.setCreatedBy("testuser");
            savedMonthly.setCreatedAt(LocalDateTime.now());
            savedMonthly.setUpdatedAt(LocalDateTime.now());
            savedMonthly.setVersion(0L);
            savedMonthly.setCategory(rootCategory);

            // First save → parent, subsequent → monthly + auto-created monthly budgets
            when(budgetRepository.save(any(Budget.class)))
                    .thenReturn(savedParent)
                    .thenReturn(savedMonthly);

            BudgetDTO result = budgetService.createBudget(dto, "testuser");

            assertThat(result).isNotNull();
            verify(budgetRepository, atLeastOnce()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw when creating parent budget but amount is insufficient")
        void createBudget_createParentBudget_insufficientAmount() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setCategoryId(1L);
            dto.setCreateParentBudget(true);
            dto.setParentTotalAmount(new BigDecimal("500.00")); // less than 1000 monthly

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(BigDecimal.ZERO);

            assertThatThrownBy(() -> budgetService.createBudget(dto, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class)
                    .hasMessageContaining("less than required total");
        }
    }

    // ========== createBudget with extendParentBudget Tests ==========

    @Nested
    @DisplayName("createBudget - extendParentBudget path")
    class CreateBudgetExtendParentTests {

        @Test
        @DisplayName("should extend parent budget when extendParentBudget is true and sum exceeds parent")
        void createBudget_extendParentBudget_success() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setDescription("Monthly budget");
            dto.setCategoryId(1L);
            dto.setExtendParentBudget(true);
            dto.setParentTotalAmount(new BigDecimal("15000.00"));

            Budget parentBudget = new Budget();
            parentBudget.setId(99L);
            parentBudget.setYear(2025);
            parentBudget.setMonth(null);
            parentBudget.setTotalAmount(new BigDecimal("10000.00")); // less than sum
            parentBudget.setCategory(rootCategory);
            parentBudget.setCreatedBy("testuser");
            parentBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentBudget));
            // Existing monthly sum: 10000, adding 1000 = 11000 > 10000 parent → triggers extend path
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(new BigDecimal("10000.00"));

            Budget savedMonthly = new Budget();
            savedMonthly.setId(100L);
            savedMonthly.setYear(2025);
            savedMonthly.setMonth(6);
            savedMonthly.setTotalAmount(new BigDecimal("1000.00"));
            savedMonthly.setDescription("Monthly budget");
            savedMonthly.setCreatedBy("testuser");
            savedMonthly.setCreatedAt(LocalDateTime.now());
            savedMonthly.setUpdatedAt(LocalDateTime.now());
            savedMonthly.setVersion(0L);
            savedMonthly.setCategory(rootCategory);

            when(budgetRepository.save(any(Budget.class))).thenReturn(savedMonthly);
            when(expenseRepository.findByBudgetId(99L)).thenReturn(Collections.emptyList());

            BudgetDTO result = budgetService.createBudget(dto, "testuser");

            assertThat(result).isNotNull();
            // Parent budget should have been extended to 15000
            verify(budgetRepository, atLeastOnce()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw when extend parent but parentTotalAmount is null")
        void createBudget_extendParentBudget_nullParentAmount() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setCategoryId(1L);
            dto.setExtendParentBudget(true);
            dto.setParentTotalAmount(null);

            Budget parentBudget = new Budget();
            parentBudget.setId(99L);
            parentBudget.setYear(2025);
            parentBudget.setMonth(null);
            parentBudget.setTotalAmount(new BigDecimal("500.00"));
            parentBudget.setCategory(rootCategory);
            parentBudget.setCreatedBy("testuser");
            parentBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(new BigDecimal("500.00"));

            assertThatThrownBy(() -> budgetService.createBudget(dto, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class)
                    .hasMessageContaining("Parent budget amount is required");
        }

        @Test
        @DisplayName("should throw when extend parent but new amount still insufficient")
        void createBudget_extendParentBudget_insufficientNewAmount() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("1000.00"));
            dto.setCategoryId(1L);
            dto.setExtendParentBudget(true);
            dto.setParentTotalAmount(new BigDecimal("800.00")); // less than 500+1000=1500

            Budget parentBudget = new Budget();
            parentBudget.setId(99L);
            parentBudget.setYear(2025);
            parentBudget.setMonth(null);
            parentBudget.setTotalAmount(new BigDecimal("500.00"));
            parentBudget.setCategory(rootCategory);
            parentBudget.setCreatedBy("testuser");
            parentBudget.setExpenses(new ArrayList<>());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(1L, 2025)).thenReturn(Optional.of(parentBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(1L, 2025)).thenReturn(new BigDecimal("500.00"));

            assertThatThrownBy(() -> budgetService.createBudget(dto, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class)
                    .hasMessageContaining("less than required total");
        }
    }

    // ========== createBudget with parent category budget Tests ==========

    @Nested
    @DisplayName("createBudget - parent category budget handling")
    class CreateBudgetParentCategoryTests {

        @Test
        @DisplayName("should auto-increment existing parent category budget")
        void createBudget_autoIncrementParentCategoryBudget() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("300.00"));
            dto.setDescription("Dining out budget");
            dto.setCategoryId(20L);

            // Parent yearly budget for category 20 (the budget's own yearly parent)
            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(100L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("5000.00"));
            yearlyBudget.setCategory(childCategory);
            yearlyBudget.setCreatedBy("testuser");
            yearlyBudget.setExpenses(new ArrayList<>());

            // Existing parent category budget for "Food" at month 6
            Budget parentCatBudget = new Budget();
            parentCatBudget.setId(200L);
            parentCatBudget.setYear(2025);
            parentCatBudget.setMonth(6);
            parentCatBudget.setTotalAmount(new BigDecimal("1000.00"));
            parentCatBudget.setCategory(parentCategory);

            Budget savedMonthly = new Budget();
            savedMonthly.setId(101L);
            savedMonthly.setYear(2025);
            savedMonthly.setMonth(6);
            savedMonthly.setTotalAmount(new BigDecimal("300.00"));
            savedMonthly.setDescription("Dining out budget");
            savedMonthly.setCreatedBy("testuser");
            savedMonthly.setCreatedAt(LocalDateTime.now());
            savedMonthly.setUpdatedAt(LocalDateTime.now());
            savedMonthly.setVersion(0L);
            savedMonthly.setCategory(childCategory);

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.of(yearlyBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedMonthly);
            when(expenseRepository.findByBudgetId(100L)).thenReturn(Collections.emptyList());
            // Parent category budget lookup
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6))
                    .thenReturn(Optional.of(parentCatBudget));

            BudgetDTO result = budgetService.createBudget(dto, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getParentCategoryBudgetUpdated()).isNotNull();
            assertThat(result.getParentCategoryBudgetUpdated().getParentCategoryName()).isEqualTo("Food");
        }

        @Test
        @DisplayName("should create new parent category budget when createParentCategoryBudget is true")
        void createBudget_createNewParentCategoryBudget() {
            BudgetDTO dto = new BudgetDTO();
            dto.setYear(2025);
            dto.setMonth(6);
            dto.setTotalAmount(new BigDecimal("300.00"));
            dto.setDescription("Dining out budget");
            dto.setCategoryId(20L);
            dto.setCreateParentCategoryBudget(true);
            dto.setParentCategoryBudgetAmount(new BigDecimal("2000.00"));

            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(100L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("5000.00"));
            yearlyBudget.setCategory(childCategory);
            yearlyBudget.setCreatedBy("testuser");
            yearlyBudget.setExpenses(new ArrayList<>());

            Budget savedMonthly = new Budget();
            savedMonthly.setId(101L);
            savedMonthly.setYear(2025);
            savedMonthly.setMonth(6);
            savedMonthly.setTotalAmount(new BigDecimal("300.00"));
            savedMonthly.setDescription("Dining out budget");
            savedMonthly.setCreatedBy("testuser");
            savedMonthly.setCreatedAt(LocalDateTime.now());
            savedMonthly.setUpdatedAt(LocalDateTime.now());
            savedMonthly.setVersion(0L);
            savedMonthly.setCategory(childCategory);

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findParentBudget(20L, 2025)).thenReturn(Optional.of(yearlyBudget));
            when(budgetRepository.sumMonthlyBudgetsForCategory(20L, 2025)).thenReturn(BigDecimal.ZERO);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedMonthly);
            when(expenseRepository.findByBudgetId(100L)).thenReturn(Collections.emptyList());
            // No existing parent category budget
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(Optional.empty());

            BudgetDTO result = budgetService.createBudget(dto, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getParentCategoryBudgetUpdated()).isNotNull();
        }
    }

    // ========== calculateSpendingPercentage - zero total amount ==========

    @Nested
    @DisplayName("calculateSpendingPercentage - edge cases")
    class CalculateSpendingPercentageEdgeCases {

        @Test
        @DisplayName("should return zero when totalAmount is zero")
        void calculateSpendingPercentage_zeroTotalAmount() {
            Budget zeroBudget = new Budget();
            zeroBudget.setId(1L);
            zeroBudget.setTotalAmount(BigDecimal.ZERO);

            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("100.00"));

            BigDecimal result = budgetService.calculateSpendingPercentage(zeroBudget);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return zero when totalAmount is null")
        void calculateSpendingPercentage_nullTotalAmount() {
            Budget nullBudget = new Budget();
            nullBudget.setId(1L);
            nullBudget.setTotalAmount(null);

            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("100.00"));

            BigDecimal result = budgetService.calculateSpendingPercentage(nullBudget);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ========== getMonthBudgetSummary - multiple budgets ==========

    @Nested
    @DisplayName("getMonthBudgetSummary")
    class GetMonthBudgetSummaryTests {

        @Test
        @DisplayName("should aggregate multiple budgets for same month")
        void getMonthBudgetSummary_multipleBudgets() {
            Budget budget1 = createBudgetEntity(1L, 2025, 6, new BigDecimal("1000.00"),
                    "Budget 1", "testuser", rootCategory);
            Budget budget2 = createBudgetEntity(2L, 2025, 6, new BigDecimal("2000.00"),
                    "Budget 2", "testuser", parentCategory);

            when(budgetRepository.findByYearAndMonthOrderByCategoryIdAsc(2025, 6))
                    .thenReturn(List.of(budget1, budget2));
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("200.00"));
            when(expenseRepository.sumAmountByBudgetId(2L)).thenReturn(new BigDecimal("300.00"));
            when(expenseRepository.countByBudgetId(1L)).thenReturn(3L);
            when(expenseRepository.countByBudgetId(2L)).thenReturn(5L);

            BudgetSummaryDTO result = budgetService.getMonthBudgetSummary(2025, 6);

            assertThat(result).isNotNull();
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.getTotalSpending()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getExpenseCount()).isEqualTo(8L);
            // 500/3000 * 100 = 16.67
            assertThat(result.getSpendingPercentage()).isPositive();
        }
    }

    // ========== mapToBudgetSummary - parent category aggregation ==========

    @Nested
    @DisplayName("mapToBudgetSummary - parent category")
    class MapToBudgetSummaryParentCategoryTests {

        @Test
        @DisplayName("should set isParentCategory for category with children and monthly budget")
        void mapToBudgetSummary_parentCategoryWithChildrenAndMonthlyBudget() {
            Budget monthlyBudget = createBudgetEntity(1L, 2025, 6, new BigDecimal("5000.00"),
                    "Food budget", "testuser", parentCategory);

            when(budgetRepository.findAllByOrderByYearDescMonthDesc())
                    .thenReturn(List.of(monthlyBudget));
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(new BigDecimal("1000.00"));
            when(expenseRepository.countByBudgetId(1L)).thenReturn(10L);
            // Parent category with 2 children
            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(2L);
            when(budgetRepository.sumBudgetsByChildCategoriesAndPeriod(10L, 2025, 6))
                    .thenReturn(new BigDecimal("2000.00"));
            when(expenseRepository.sumExpensesByParentCategoryBudgets(10L, 2025, 6))
                    .thenReturn(new BigDecimal("500.00"));

            List<BudgetSummaryDTO> result = budgetService.getAllBudgets();

            assertThat(result).hasSize(1);
            BudgetSummaryDTO summary = result.get(0);
            assertThat(summary.getIsParentCategory()).isTrue();
            assertThat(summary.getChildrenBudgetSum()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(summary.getChildrenSpending()).isEqualByComparingTo(new BigDecimal("500.00"));
            // Total spending includes own (1000) + children (500) = 1500
            assertThat(summary.getTotalSpending()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should set isParentCategory true but no aggregation for yearly budget with children")
        void mapToBudgetSummary_parentCategoryYearlyBudget() {
            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(1L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("60000.00"));
            yearlyBudget.setDescription("Yearly food");
            yearlyBudget.setCreatedBy("testuser");
            yearlyBudget.setCreatedAt(LocalDateTime.now());
            yearlyBudget.setUpdatedAt(LocalDateTime.now());
            yearlyBudget.setVersion(0L);
            yearlyBudget.setCategory(parentCategory);
            yearlyBudget.setExpenses(new ArrayList<>());

            when(budgetRepository.findAllByOrderByYearDescMonthDesc())
                    .thenReturn(List.of(yearlyBudget));
            when(expenseRepository.sumAmountByBudgetId(1L)).thenReturn(BigDecimal.ZERO);
            when(expenseRepository.countByBudgetId(1L)).thenReturn(0L);
            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(2L);

            List<BudgetSummaryDTO> result = budgetService.getAllBudgets();

            assertThat(result).hasSize(1);
            BudgetSummaryDTO summary = result.get(0);
            // childCount > 0 but month is null → goes to else branch, just sets isParentCategory=true
            assertThat(summary.getIsParentCategory()).isTrue();
            // No child spending aggregation for yearly budgets
            assertThat(summary.getChildrenBudgetSum()).isNull();
        }
    }
}
