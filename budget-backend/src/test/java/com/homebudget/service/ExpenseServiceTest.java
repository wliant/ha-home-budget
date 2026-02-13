package com.homebudget.service;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseListResponse;
import com.homebudget.exception.BudgetNotFoundException;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.model.Budget;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.repository.BudgetRepository;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseFileRepository;
import com.homebudget.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseFileRepository expenseFileRepository;

    @InjectMocks
    private ExpenseService expenseService;

    // Common test data
    private Budget budget;
    private Category category;
    private Expense expense;
    private ExpenseDTO expenseDTO;

    @BeforeEach
    void setUp() {
        budget = new Budget();
        budget.setId(1L);
        budget.setYear(2025);
        budget.setMonth(6);
        budget.setTotalAmount(new BigDecimal("1000.00"));
        budget.setDescription("June Budget");
        budget.setCreatedBy("testuser");

        category = new Category();
        category.setId(10L);
        category.setName("Groceries");
        category.setIcon("cart");
        category.setCreatedBy("testuser");

        expense = new Expense();
        expense.setId(100L);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDescription("Weekly groceries");
        expense.setExpenseDate(LocalDate.of(2025, 6, 15));
        expense.setBudget(budget);
        expense.setCreatedBy("testuser");
        expense.setCreatedAt(LocalDateTime.of(2025, 6, 15, 10, 0));
        expense.setUpdatedAt(LocalDateTime.of(2025, 6, 15, 10, 0));
        expense.setVersion(0L);

        expenseDTO = new ExpenseDTO();
        expenseDTO.setAmount(new BigDecimal("50.00"));
        expenseDTO.setDescription("Weekly groceries");
        expenseDTO.setExpenseDate(LocalDate.of(2025, 6, 15));
        expenseDTO.setBudgetId(1L);
    }

    // -----------------------------------------------------------------------
    // createExpense
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createExpense")
    class CreateExpense {

        @Test
        @DisplayName("should create expense without category and return DTO")
        void createExpense_success_noCategory() {
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getDescription()).isEqualTo("Weekly groceries");
            assertThat(result.getExpenseDate()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(result.getBudgetId()).isEqualTo(1L);
            assertThat(result.getCreatedBy()).isEqualTo("testuser");
            assertThat(result.getCategoryId()).isNull();
            assertThat(result.getWarnings()).isEmpty();

            verify(expenseRepository).save(any(Expense.class));
            verify(categoryRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should create expense with category when categoryId provided")
        void createExpense_success_withCategory() {
            expenseDTO.setCategoryId(10L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getCategoryId()).isEqualTo(10L);
            assertThat(result.getCategoryName()).isEqualTo("Groceries");
            assertThat(result.getCategoryIcon()).isEqualTo("cart");

            ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("should add date mismatch warning when expense date outside budget month")
        void createExpense_success_dateMismatch() {
            // Expense date in July, budget is June
            expenseDTO.setExpenseDate(LocalDate.of(2025, 7, 5));

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result.getWarnings()).isNotEmpty();
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("Warning");
            assertThat(result.getWarnings().get(0)).contains("2025-07-05");
            assertThat(result.getWarnings().get(0)).contains("2025-06");
        }

        @Test
        @DisplayName("should throw BudgetNotFoundException when budget does not exist")
        void createExpense_budgetNotFound() {
            when(budgetRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(expenseDTO, "testuser"))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when category does not exist")
        void createExpense_categoryNotFound() {
            expenseDTO.setCategoryId(999L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(expenseDTO, "testuser"))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // getAllExpenses
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getAllExpenses")
    class GetAllExpenses {

        private List<Expense> singleExpenseList;

        @BeforeEach
        void setUpList() {
            singleExpenseList = List.of(expense);
        }

        @Test
        @DisplayName("should return all expenses when no filters provided")
        void getAllExpenses_noFilters() {
            when(expenseRepository.findAllOrderByExpenseDateDesc()).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, null, null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
            verify(expenseRepository).findAllOrderByExpenseDateDesc();
        }

        @Test
        @DisplayName("should filter by budgetId only")
        void getAllExpenses_budgetIdOnly() {
            when(expenseRepository.findByBudgetId(1L)).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(1L, null, null, null, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByBudgetId(1L);
        }

        @Test
        @DisplayName("should filter by categoryId only")
        void getAllExpenses_categoryIdOnly() {
            when(expenseRepository.findByCategoryId(10L)).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, 10L, null, null, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByCategoryId(10L);
        }

        @Test
        @DisplayName("should filter by date range only")
        void getAllExpenses_dateRangeOnly() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);
            when(expenseRepository.findByExpenseDateBetween(start, end)).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, null, start, end, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByExpenseDateBetween(start, end);
        }

        @Test
        @DisplayName("should filter by createdBy only")
        void getAllExpenses_createdByOnly() {
            when(expenseRepository.findByCreatedBy("testuser")).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, null, null, null, "testuser");

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByCreatedBy("testuser");
        }

        @Test
        @DisplayName("should filter by budgetId and categoryId")
        void getAllExpenses_budgetIdAndCategoryId() {
            when(expenseRepository.findByBudgetIdAndCategoryId(1L, 10L)).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(1L, 10L, null, null, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByBudgetIdAndCategoryId(1L, 10L);
        }

        @Test
        @DisplayName("should filter by budgetId and createdBy")
        void getAllExpenses_budgetIdAndCreatedBy() {
            when(expenseRepository.findByBudgetIdAndCreatedBy(1L, "testuser")).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(1L, null, null, null, "testuser");

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByBudgetIdAndCreatedBy(1L, "testuser");
        }

        @Test
        @DisplayName("should filter by budgetId and date range")
        void getAllExpenses_budgetIdAndDateRange() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);
            when(expenseRepository.findByBudgetIdAndExpenseDateBetween(1L, start, end))
                    .thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(1L, null, start, end, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByBudgetIdAndExpenseDateBetween(1L, start, end);
        }

        @Test
        @DisplayName("should filter by all parameters")
        void getAllExpenses_allFilters() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);
            when(expenseRepository.findByBudgetIdAndCategoryIdAndExpenseDateBetweenAndCreatedBy(
                    1L, 10L, start, end, "testuser")).thenReturn(singleExpenseList);

            List<ExpenseDTO> result = expenseService.getAllExpenses(1L, 10L, start, end, "testuser");

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByBudgetIdAndCategoryIdAndExpenseDateBetweenAndCreatedBy(
                    1L, 10L, start, end, "testuser");
        }

        @Test
        @DisplayName("should return empty list when no expenses found")
        void getAllExpenses_emptyResult() {
            when(expenseRepository.findAllOrderByExpenseDateDesc()).thenReturn(Collections.emptyList());

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, null, null, null, null);

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseById
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseById")
    class GetExpenseById {

        @Test
        @DisplayName("should return expense DTO when found")
        void getExpenseById_success() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));

            ExpenseDTO result = expenseService.getExpenseById(100L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getDescription()).isEqualTo("Weekly groceries");
            assertThat(result.getExpenseDate()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(result.getBudgetId()).isEqualTo(1L);
            assertThat(result.getCreatedBy()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should include category info when expense has category")
        void getExpenseById_withCategory() {
            expense.setCategory(category);
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));

            ExpenseDTO result = expenseService.getExpenseById(100L);

            assertThat(result.getCategoryId()).isEqualTo(10L);
            assertThat(result.getCategoryName()).isEqualTo("Groceries");
            assertThat(result.getCategoryIcon()).isEqualTo("cart");
        }

        @Test
        @DisplayName("should throw ExpenseNotFoundException when not found")
        void getExpenseById_notFound() {
            when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpenseById(999L))
                    .isInstanceOf(ExpenseNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateExpense
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateExpense")
    class UpdateExpense {

        private ExpenseDTO updateDTO;

        @BeforeEach
        void setUpUpdateDTO() {
            updateDTO = new ExpenseDTO();
            updateDTO.setAmount(new BigDecimal("75.00"));
            updateDTO.setDescription("Updated groceries");
            updateDTO.setExpenseDate(LocalDate.of(2025, 6, 20));
            updateDTO.setBudgetId(1L); // same budget
        }

        @Test
        @DisplayName("should update amount, description, and expenseDate")
        void updateExpense_success() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            // resolveBudgetForExpense: categoryId is null, so falls back to findById(budgetId)
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExpenseDTO result = expenseService.updateExpense(100L, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(result.getDescription()).isEqualTo("Updated groceries");
            assertThat(result.getExpenseDate()).isEqualTo(LocalDate.of(2025, 6, 20));

            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("should throw ExpenseNotFoundException when expense does not exist")
        void updateExpense_notFound() {
            when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense(999L, updateDTO))
                    .isInstanceOf(ExpenseNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should change budget when new budgetId is different")
        void updateExpense_changeBudget() {
            Budget newBudget = new Budget();
            newBudget.setId(2L);
            newBudget.setYear(2025);
            newBudget.setMonth(7);
            newBudget.setTotalAmount(new BigDecimal("2000.00"));
            newBudget.setCreatedBy("testuser");

            updateDTO.setBudgetId(2L);

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            when(budgetRepository.findById(2L)).thenReturn(Optional.of(newBudget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExpenseDTO result = expenseService.updateExpense(100L, updateDTO);

            assertThat(result.getBudgetId()).isEqualTo(2L);
            verify(budgetRepository).findById(2L);
        }

        @Test
        @DisplayName("should change category when new categoryId is different")
        void updateExpense_changeCategory() {
            // Existing expense has no category
            Category newCategory = new Category();
            newCategory.setId(20L);
            newCategory.setName("Utilities");
            newCategory.setIcon("bolt");
            newCategory.setCreatedBy("testuser");

            updateDTO.setCategoryId(20L);

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            // resolveBudgetForExpense: categoryId=20, date=2025-06-20, tries findByCategoryIdAndYearAndMonth first
            when(budgetRepository.findByCategoryIdAndYearAndMonth(20L, 2025, 6)).thenReturn(Optional.of(budget));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExpenseDTO result = expenseService.updateExpense(100L, updateDTO);

            assertThat(result.getCategoryId()).isEqualTo(20L);
            assertThat(result.getCategoryName()).isEqualTo("Utilities");
            assertThat(result.getCategoryIcon()).isEqualTo("bolt");
            verify(categoryRepository).findById(20L);
        }

        @Test
        @DisplayName("should remove category when dto categoryId is null")
        void updateExpense_removeCategory() {
            // Existing expense has a category
            expense.setCategory(category);
            updateDTO.setCategoryId(null);

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            // resolveBudgetForExpense: categoryId is null, falls back to findById(budgetId)
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExpenseDTO result = expenseService.updateExpense(100L, updateDTO);

            assertThat(result.getCategoryId()).isNull();
            assertThat(result.getCategoryName()).isNull();

            ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isNull();
        }

        @Test
        @DisplayName("should throw BudgetNotFoundException when new budget does not exist")
        void updateExpense_newBudgetNotFound() {
            updateDTO.setBudgetId(999L);

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense(100L, updateDTO))
                    .isInstanceOf(BudgetNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CategoryNotFoundException when new category does not exist")
        void updateExpense_newCategoryNotFound() {
            updateDTO.setCategoryId(999L);

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            // resolveBudgetForExpense: categoryId=999, date=2025-06-20 → findByCategoryIdAndYearAndMonth(999,2025,6) empty
            // → findParentBudget(999,2025) empty → fallback to findById(1L)
            when(budgetRepository.findByCategoryIdAndYearAndMonth(999L, 2025, 6)).thenReturn(Optional.empty());
            when(budgetRepository.findParentBudget(999L, 2025)).thenReturn(Optional.empty());
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.updateExpense(100L, updateDTO))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should add date mismatch warning when updated date outside budget month")
        void updateExpense_dateMismatchWarning() {
            // Update expense date to August, but budget is June
            updateDTO.setExpenseDate(LocalDate.of(2025, 8, 10));

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(expense));
            // resolveBudgetForExpense: categoryId is null, falls back to findById(budgetId)
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ExpenseDTO result = expenseService.updateExpense(100L, updateDTO);

            assertThat(result.getWarnings()).isNotEmpty();
            assertThat(result.getWarnings().get(0)).contains("Warning");
            assertThat(result.getWarnings().get(0)).contains("2025-08-10");
            assertThat(result.getWarnings().get(0)).contains("2025-06");
        }
    }

    // -----------------------------------------------------------------------
    // deleteExpense
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteExpense")
    class DeleteExpense {

        @Test
        @DisplayName("should delete expense when it exists")
        void deleteExpense_success() {
            when(expenseRepository.existsById(100L)).thenReturn(true);

            expenseService.deleteExpense(100L);

            verify(expenseRepository).deleteById(100L);
        }

        @Test
        @DisplayName("should throw ExpenseNotFoundException when expense does not exist")
        void deleteExpense_notFound() {
            when(expenseRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> expenseService.deleteExpense(999L))
                    .isInstanceOf(ExpenseNotFoundException.class);

            verify(expenseRepository, never()).deleteById(any());
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseList
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseList")
    class GetExpenseList {

        @Test
        @DisplayName("should return paginated expense list with year and month filters")
        void getExpenseList_withYearAndMonth() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);

            when(expenseRepository.findByFiltersPageable(
                    isNull(), eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30)),
                    isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(expenseRepository.getFilteredTotalAmount(
                    isNull(), eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30)),
                    isNull(), isNull(), isNull()))
                    .thenReturn(new BigDecimal("50.00"));

            ExpenseListResponse result = expenseService.getExpenseList(
                    2025, 6, null, null, null, null, pageable, "expenseDate", "desc");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(result.getSortBy()).isEqualTo("expenseDate");
            assertThat(result.getSortDirection()).isEqualTo("desc");
        }

        @Test
        @DisplayName("should return paginated expense list with year only (full year range)")
        void getExpenseList_yearOnly() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Expense> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(expenseRepository.findByFiltersPageable(
                    isNull(), eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31)),
                    isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(expenseRepository.getFilteredTotalAmount(
                    isNull(), eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31)),
                    isNull(), isNull(), isNull()))
                    .thenReturn(BigDecimal.ZERO);

            ExpenseListResponse result = expenseService.getExpenseList(
                    2025, null, null, null, null, null, pageable, "expenseDate", "desc");

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should use expanded category IDs when parent category filter is provided")
        void getExpenseList_withParentCategoryFilter() {
            Pageable pageable = PageRequest.of(0, 10);

            // Parent category with children
            Category parentCat = new Category();
            parentCat.setId(10L);
            parentCat.setName("Food");

            Category childCat1 = new Category();
            childCat1.setId(11L);
            childCat1.setName("Dining");

            Category childCat2 = new Category();
            childCat2.setId(12L);
            childCat2.setName("Groceries");

            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(2L);
            when(categoryRepository.findByParentCategoryId(10L))
                    .thenReturn(List.of(childCat1, childCat2));

            Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);
            when(expenseRepository.findByFiltersPageableWithCategoryIds(
                    eq(List.of(10L, 11L, 12L)),
                    eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30)),
                    isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(expenseRepository.getFilteredTotalAmountWithCategoryIds(
                    eq(List.of(10L, 11L, 12L)),
                    eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30)),
                    isNull(), isNull(), isNull()))
                    .thenReturn(new BigDecimal("50.00"));

            ExpenseListResponse result = expenseService.getExpenseList(
                    2025, 6, 10L, null, null, null, pageable, "expenseDate", "desc");

            assertThat(result.getContent()).hasSize(1);
            verify(expenseRepository).findByFiltersPageableWithCategoryIds(
                    eq(List.of(10L, 11L, 12L)), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should use regular query when category is leaf (no children)")
        void getExpenseList_withLeafCategoryFilter() {
            Pageable pageable = PageRequest.of(0, 10);

            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(0L);

            Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);
            when(expenseRepository.findByFiltersPageable(
                    eq(10L), any(), any(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(expenseRepository.getFilteredTotalAmount(
                    eq(10L), any(), any(), isNull(), isNull(), isNull()))
                    .thenReturn(new BigDecimal("50.00"));

            ExpenseListResponse result = expenseService.getExpenseList(
                    2025, 6, 10L, null, null, null, pageable, "expenseDate", "desc");

            assertThat(result.getContent()).hasSize(1);
            verify(expenseRepository).findByFiltersPageable(
                    eq(10L), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should pass amount filters to repository")
        void getExpenseList_withAmountFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            BigDecimal min = new BigDecimal("10.00");
            BigDecimal max = new BigDecimal("100.00");

            Page<Expense> page = new PageImpl<>(List.of(expense), pageable, 1);
            when(expenseRepository.findByFiltersPageable(
                    isNull(), any(), any(), eq(min), eq(max), isNull(), eq(pageable)))
                    .thenReturn(page);
            when(expenseRepository.getFilteredTotalAmount(
                    isNull(), any(), any(), eq(min), eq(max), isNull()))
                    .thenReturn(new BigDecimal("50.00"));

            ExpenseListResponse result = expenseService.getExpenseList(
                    2025, 6, null, min, max, null, pageable, "amount", "asc");

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // -----------------------------------------------------------------------
    // getDistinctYears / getDistinctCreators
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getDistinctYears and getDistinctCreators")
    class MetadataQueries {

        @Test
        @DisplayName("should return distinct years")
        void getDistinctYears() {
            when(expenseRepository.findDistinctYears()).thenReturn(List.of(2025, 2024));

            List<Integer> result = expenseService.getDistinctYears();

            assertThat(result).containsExactly(2025, 2024);
            verify(expenseRepository).findDistinctYears();
        }

        @Test
        @DisplayName("should return empty list when no years exist")
        void getDistinctYears_empty() {
            when(expenseRepository.findDistinctYears()).thenReturn(Collections.emptyList());

            List<Integer> result = expenseService.getDistinctYears();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return distinct creators")
        void getDistinctCreators() {
            when(expenseRepository.findDistinctCreators()).thenReturn(List.of("alice", "bob"));

            List<String> result = expenseService.getDistinctCreators();

            assertThat(result).containsExactly("alice", "bob");
            verify(expenseRepository).findDistinctCreators();
        }

        @Test
        @DisplayName("should return empty list when no creators exist")
        void getDistinctCreators_empty() {
            when(expenseRepository.findDistinctCreators()).thenReturn(Collections.emptyList());

            List<String> result = expenseService.getDistinctCreators();

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // resolveBudgetForExpense - category-based resolution
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("resolveBudgetForExpense via createExpense")
    class ResolveBudgetForExpense {

        @Test
        @DisplayName("should resolve monthly budget by categoryId and expense date")
        void resolveBudget_monthlyByCategoryAndDate() {
            expenseDTO.setCategoryId(10L);
            // expense date in June → should find monthly budget for June

            Budget monthlyBudget = new Budget();
            monthlyBudget.setId(5L);
            monthlyBudget.setYear(2025);
            monthlyBudget.setMonth(6);
            monthlyBudget.setTotalAmount(new BigDecimal("1000.00"));
            monthlyBudget.setCreatedBy("testuser");

            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6))
                    .thenReturn(Optional.of(monthlyBudget));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense saved = inv.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getBudgetId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should fall back to parent yearly budget when no monthly budget exists")
        void resolveBudget_parentYearlyFallback() {
            expenseDTO.setCategoryId(10L);

            Budget parentBudget = new Budget();
            parentBudget.setId(50L);
            parentBudget.setYear(2025);
            parentBudget.setMonth(null);
            parentBudget.setTotalAmount(new BigDecimal("12000.00"));
            parentBudget.setCreatedBy("testuser");

            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6))
                    .thenReturn(Optional.empty());
            when(budgetRepository.findParentBudget(10L, 2025))
                    .thenReturn(Optional.of(parentBudget));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense saved = inv.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getBudgetId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("should fall back to budgetId when no category-based budget found")
        void resolveBudget_fallbackToBudgetId() {
            expenseDTO.setCategoryId(10L);

            when(budgetRepository.findByCategoryIdAndYearAndMonth(10L, 2025, 6))
                    .thenReturn(Optional.empty());
            when(budgetRepository.findParentBudget(10L, 2025))
                    .thenReturn(Optional.empty());
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense saved = inv.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getBudgetId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw when no budgetId and no category-based budget")
        void resolveBudget_noBudgetId_noCategoryBudget() {
            expenseDTO.setCategoryId(null);
            expenseDTO.setBudgetId(null);

            assertThatThrownBy(() -> expenseService.createExpense(expenseDTO, "testuser"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget ID is required");
        }
    }

    // -----------------------------------------------------------------------
    // getAllExpenses - category with children expansion
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getAllExpenses - parent category expansion")
    class GetAllExpensesParentCategory {

        @Test
        @DisplayName("should expand parent category to include children when filtering by categoryId")
        void getAllExpenses_parentCategoryExpansion() {
            Category child1 = new Category();
            child1.setId(11L);
            child1.setName("Dining");

            Category child2 = new Category();
            child2.setId(12L);
            child2.setName("Groceries");

            when(categoryRepository.countByParentCategoryId(10L)).thenReturn(2L);
            when(categoryRepository.findByParentCategoryId(10L)).thenReturn(List.of(child1, child2));
            when(expenseRepository.findByCategoryId(10L)).thenReturn(List.of(expense));
            when(expenseRepository.findByCategoryId(11L)).thenReturn(Collections.emptyList());
            when(expenseRepository.findByCategoryId(12L)).thenReturn(Collections.emptyList());

            List<ExpenseDTO> result = expenseService.getAllExpenses(null, 10L, null, null, null);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByCategoryId(10L);
            verify(expenseRepository).findByCategoryId(11L);
            verify(expenseRepository).findByCategoryId(12L);
        }
    }

    // -----------------------------------------------------------------------
    // createExpense - no date mismatch for yearly budget
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createExpense - yearly budget (no date mismatch check)")
    class CreateExpenseYearlyBudget {

        @Test
        @DisplayName("should not add date mismatch warning when budget month is null (yearly budget)")
        void createExpense_yearlyBudget_noDateMismatch() {
            Budget yearlyBudget = new Budget();
            yearlyBudget.setId(50L);
            yearlyBudget.setYear(2025);
            yearlyBudget.setMonth(null);
            yearlyBudget.setTotalAmount(new BigDecimal("12000.00"));
            yearlyBudget.setCreatedBy("testuser");

            expenseDTO.setBudgetId(50L);

            when(budgetRepository.findById(50L)).thenReturn(Optional.of(yearlyBudget));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
                Expense saved = inv.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setVersion(0L);
                return saved;
            });

            ExpenseDTO result = expenseService.createExpense(expenseDTO, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getWarnings()).isEmpty();
        }
    }
}
