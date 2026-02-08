package com.homebudget.service;

import com.homebudget.dto.BudgetDTO;
import com.homebudget.dto.BudgetSummaryDTO;
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
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(1L, 2025, 6)).thenReturn(false);
            when(budgetRepository.sumByParentCategoryAndPeriod(1L, 2025, 6)).thenReturn(null);
            when(budgetRepository.save(any(Budget.class))).thenReturn(sampleBudget);

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

            verify(budgetRepository).save(any(Budget.class));
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
        @DisplayName("should throw IllegalArgumentException when child category has no parent budget")
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
            // Parent budget does not exist
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(childBudgetDTO, "testuser"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parent category")
                    .hasMessageContaining("Food")
                    .hasMessageContaining("must have a budget");

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when child sum does not match parent")
        void createBudget_parentBudgetMismatch() {
            // Arrange
            BudgetDTO childBudgetDTO = new BudgetDTO();
            childBudgetDTO.setYear(2025);
            childBudgetDTO.setMonth(6);
            childBudgetDTO.setTotalAmount(new BigDecimal("300.00"));
            childBudgetDTO.setDescription("Dining out budget");
            childBudgetDTO.setCategoryId(20L);

            Budget parentBudget = createBudgetEntity(100L, 2025, 6,
                    new BigDecimal("500.00"), "Food budget", "testuser", parentCategory);

            when(categoryRepository.findById(20L)).thenReturn(Optional.of(childCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(false);
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(Optional.of(parentBudget));
            // Existing child budgets sum to 100, adding 300 = 400, but parent is 500
            when(budgetRepository.sumByParentCategoryAndPeriod(10L, 2025, 6)).thenReturn(new BigDecimal("100.00"));

            // Act & Assert
            assertThatThrownBy(() -> budgetService.createBudget(childBudgetDTO, "testuser"))
                    .isInstanceOf(ParentBudgetMismatchException.class);

            verify(budgetRepository, never()).save(any(Budget.class));
        }

        @Test
        @DisplayName("should throw ParentBudgetMismatchException when creating parent with mismatched existing children")
        void createBudget_parentCategoryWithChildSumMismatch() {
            // Arrange - creating budget for a parent category that already has child budgets
            BudgetDTO parentBudgetDTO = new BudgetDTO();
            parentBudgetDTO.setYear(2025);
            parentBudgetDTO.setMonth(6);
            parentBudgetDTO.setTotalAmount(new BigDecimal("1000.00"));
            parentBudgetDTO.setDescription("Food budget");
            parentBudgetDTO.setCategoryId(10L);

            when(categoryRepository.findById(10L)).thenReturn(Optional.of(parentCategory));
            when(budgetRepository.existsByCategoryIdAndYearAndMonth(10L, 2025, 6)).thenReturn(false);
            // Existing child budgets sum to 800, but parent amount is 1000
            when(budgetRepository.sumByParentCategoryAndPeriod(10L, 2025, 6)).thenReturn(new BigDecimal("800.00"));

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

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(sampleBudget));
            // rootCategory has no parent, so validateChildBudgetSumOnUpdate is called
            when(budgetRepository.sumByParentCategoryAndPeriod(1L, 2025, 6)).thenReturn(null);
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
            // Arrange - budget for child category, changing amount
            Budget childBudget = createBudgetEntity(5L, 2025, 6, new BigDecimal("200.00"),
                    "Dining out", "testuser", childCategory);

            Budget parentBudget = createBudgetEntity(100L, 2025, 6, new BigDecimal("500.00"),
                    "Food", "testuser", parentCategory);

            // Another child budget exists
            Category otherChild = new Category("Takeaway", "box", "testuser");
            otherChild.setId(21L);
            otherChild.setParentCategory(parentCategory);
            Budget otherChildBudget = createBudgetEntity(6L, 2025, 6, new BigDecimal("200.00"),
                    "Takeaway", "testuser", otherChild);

            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("400.00")); // 400 + 200 = 600 != 500 (parent)
            updateDTO.setDescription("Updated dining out");

            when(budgetRepository.findById(5L)).thenReturn(Optional.of(childBudget));
            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6))
                    .thenReturn(Optional.of(parentBudget));
            when(budgetRepository.findChildBudgets(10L, 2025, 6))
                    .thenReturn(List.of(childBudget, otherChildBudget));

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
            // Arrange - budget for parent category, changing amount when children exist
            Budget parentBudgetEntity = createBudgetEntity(100L, 2025, 6, new BigDecimal("500.00"),
                    "Food", "testuser", parentCategory);

            BudgetDTO updateDTO = new BudgetDTO();
            updateDTO.setTotalAmount(new BigDecimal("600.00")); // different from 500
            updateDTO.setDescription("Updated Food budget");

            when(budgetRepository.findById(100L)).thenReturn(Optional.of(parentBudgetEntity));
            // Child budgets sum to 500, but new parent amount is 600
            when(budgetRepository.sumByParentCategoryAndPeriod(10L, 2025, 6)).thenReturn(new BigDecimal("500.00"));

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
        @DisplayName("should return BudgetSummaryDTO for current year and month")
        void getCurrentMonthBudget_success() {
            // Arrange
            LocalDate now = LocalDate.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();

            Budget currentBudget = createBudgetEntity(50L, currentYear, currentMonth,
                    new BigDecimal("2000.00"), "Current month", "testuser", rootCategory);

            when(budgetRepository.findByYearAndMonth(currentYear, currentMonth))
                    .thenReturn(Optional.of(currentBudget));
            when(expenseRepository.sumAmountByBudgetId(50L)).thenReturn(new BigDecimal("500.00"));
            when(expenseRepository.countByBudgetId(50L)).thenReturn(10L);

            // Act
            BudgetSummaryDTO result = budgetService.getCurrentMonthBudget();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(50L);
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

            when(budgetRepository.findByYearAndMonth(currentYear, currentMonth))
                    .thenReturn(Optional.empty());

            // Act
            BudgetSummaryDTO result = budgetService.getCurrentMonthBudget();

            // Assert
            assertThat(result).isNull();
        }
    }
}
