package com.homebudget.controller;

import com.homebudget.dto.CategoryExpenseAggregateDTO;
import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseListResponse;
import com.homebudget.service.ExpenseAggregateService;
import com.homebudget.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * REST API Controller for expense operations.
 *
 * Endpoints:
 * - POST /api/expenses - Create new expense
 * - GET /api/expenses - List all expenses with optional filters
 * - GET /api/expenses/{id} - Get expense by ID
 * - PUT /api/expenses/{id} - Update expense
 * - DELETE /api/expenses/{id} - Delete expense
 *
 * Uses X-Hass-User header for user identification (provided by Home Assistant)
 */
@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "expenseDate", "description", "amount", "createdBy", "category.name");

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseAggregateService expenseAggregateService;

    /**
     * Create a new expense.
     *
     * @param dto Expense data (amount, description, expenseDate, categoryId)
     * @param username User creating expense (from X-Hass-User header)
     * @return Created expense with HTTP 201 status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExpenseDTO> createExpense(
            @Valid @RequestBody ExpenseDTO dto,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expenses - Creating expense, user: {}", username);

        ExpenseDTO created = expenseService.createExpense(dto, username);

        logger.info("Created expense ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseDTO> createExpenseWithFiles(
            @RequestPart("expense") @Valid ExpenseDTO dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("POST /api/expenses (multipart) - Creating expense with files, user: {}", username);
        ExpenseDTO created = expenseService.createExpenseWithFiles(dto, files == null ? List.of() : List.of(files), username);

        logger.info("Created expense ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all expenses with optional filtering.
     *
     * Query parameters (all optional):
     * - categoryId: Filter by category
     * - startDate: Filter by date range start (YYYY-MM-DD)
     * - endDate: Filter by date range end (YYYY-MM-DD)
     * - createdBy: Filter by user who created
     *
     * @return List of expenses matching filters
     */
    @GetMapping
    public ResponseEntity<List<ExpenseDTO>> getAllExpenses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String createdBy) {

        logger.info("GET /api/expenses - Filters: categoryId={}, dateRange={}-{}, createdBy={}",
                   categoryId, startDate, endDate, createdBy);

        List<ExpenseDTO> expenses = expenseService.getAllExpenses(
                categoryId, startDate, endDate, createdBy);

        logger.info("Found {} expenses", expenses.size());
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get paginated, filtered, sorted expense list with aggregate summary.
     * Feature 011: Expense List View
     *
     * @param year required year filter
     * @param month optional month filter (1-12)
     * @param categoryId optional category filter
     * @param minAmount optional minimum amount (inclusive)
     * @param maxAmount optional maximum amount (inclusive)
     * @param createdBy optional creator filter
     * @param page page number (0-based, default 0)
     * @param size page size (default 50, max 100)
     * @param sortBy sort field (default expenseDate)
     * @param sortDirection ASC or DESC (default DESC)
     * @return paginated expense list with summary
     */
    @GetMapping("/list")
    public ResponseEntity<?> getExpenseList(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        logger.info("GET /api/expenses/list - year={}, month={}, categoryId={}, amount={}-{}, createdBy={}, page={}, size={}, sort={}:{}",
                year, month, categoryId, minAmount, maxAmount, createdBy, page, size, sortBy, sortDirection);

        // Validate month
        if (month != null && (month < 1 || month > 12)) {
            return ResponseEntity.badRequest().body("Month must be between 1 and 12");
        }

        // Validate amount range
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            return ResponseEntity.badRequest().body("minAmount must not exceed maxAmount");
        }

        // Validate and cap page size
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        // Map frontend sortBy to entity field, validate
        String entitySortField = "categoryName".equals(sortBy) ? "category.name" : sortBy;
        if (!ALLOWED_SORT_FIELDS.contains(entitySortField)) {
            entitySortField = "expenseDate";
            sortBy = "expenseDate";
        }

        // Validate sort direction
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            direction = Sort.Direction.DESC;
            sortDirection = "DESC";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, entitySortField));

        ExpenseListResponse response = expenseService.getExpenseList(
                year, month, categoryId, minAmount, maxAmount, createdBy,
                pageable, sortBy, sortDirection);

        return ResponseEntity.ok(response);
    }

    /**
     * Get distinct years that have expenses.
     * Feature 011: Used to populate year filter dropdown.
     *
     * @return list of years sorted descending
     */
    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getExpenseYears() {
        logger.info("GET /api/expenses/years");
        List<Integer> years = expenseService.getDistinctYears();
        return ResponseEntity.ok(years);
    }

    /**
     * Get distinct expense creators.
     * Feature 011: Used to populate created-by filter dropdown.
     *
     * @return list of creator usernames sorted ascending
     */
    @GetMapping("/creators")
    public ResponseEntity<List<String>> getExpenseCreators() {
        logger.info("GET /api/expenses/creators");
        List<String> creators = expenseService.getDistinctCreators();
        return ResponseEntity.ok(creators);
    }

    /**
     * Get expense by ID.
     *
     * @param id Expense ID
     * @return Expense details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        logger.info("GET /api/expenses/{} - Finding expense", id);

        ExpenseDTO expense = expenseService.getExpenseById(id);

        return ResponseEntity.ok(expense);
    }

    /**
     * Update an existing expense.
     *
     * @param id Expense ID
     * @param dto Updated expense data
     * @return Updated expense
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDTO dto,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("PUT /api/expenses/{} - Updating expense, user: {}", id, username);

        ExpenseDTO updated = expenseService.updateExpense(id, dto, username);

        logger.info("Updated expense ID: {}", id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseDTO> updateExpenseWithFiles(
            @PathVariable Long id,
            @RequestPart("expense") @Valid ExpenseDTO dto,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestHeader(HASS_USER_HEADER) String username) {

        logger.info("PUT /api/expenses/{} (multipart) - Updating expense with files, user: {}", id, username);
        ExpenseDTO updated = expenseService.updateExpenseWithFiles(id, dto, files == null ? List.of() : List.of(files), username);

        logger.info("Updated expense ID: {}", id);
        return ResponseEntity.ok(updated);
    }

    // ========================================================================
    // Aggregate Endpoints (Feature 016)
    // ========================================================================

    /**
     * Get monthly expense aggregates per category with parent rollup.
     *
     * @param year  required year
     * @param month optional month (1-12); if omitted, returns all months
     * @return list of category expense aggregates
     */
    @GetMapping("/aggregates/monthly")
    public ResponseEntity<List<CategoryExpenseAggregateDTO>> getMonthlyAggregates(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {

        logger.info("GET /api/expenses/aggregates/monthly - year={}, month={}", year, month);

        if (month != null && (month < 1 || month > 12)) {
            return ResponseEntity.badRequest().build();
        }

        List<CategoryExpenseAggregateDTO> aggregates = expenseAggregateService.getMonthlyAggregates(year, month);
        logger.info("Returning {} monthly aggregates", aggregates.size());
        return ResponseEntity.ok(aggregates);
    }

    /**
     * Get yearly expense aggregates per category with parent rollup.
     *
     * @param year required year
     * @return list of category expense aggregates (month=null)
     */
    @GetMapping("/aggregates/yearly")
    public ResponseEntity<List<CategoryExpenseAggregateDTO>> getYearlyAggregates(
            @RequestParam int year) {

        logger.info("GET /api/expenses/aggregates/yearly - year={}", year);

        List<CategoryExpenseAggregateDTO> aggregates = expenseAggregateService.getYearlyAggregates(year);
        logger.info("Returning {} yearly aggregates", aggregates.size());
        return ResponseEntity.ok(aggregates);
    }

    /**
     * Get daily expense aggregates per category with parent rollup for a specific month.
     *
     * @param year  required year
     * @param month required month (1-12)
     * @return list of category expense aggregates with day field populated
     */
    @GetMapping("/aggregates/daily")
    public ResponseEntity<List<CategoryExpenseAggregateDTO>> getDailyAggregates(
            @RequestParam int year,
            @RequestParam int month) {

        logger.info("GET /api/expenses/aggregates/daily - year={}, month={}", year, month);

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }

        List<CategoryExpenseAggregateDTO> aggregates = expenseAggregateService.getDailyAggregates(year, month);
        logger.info("Returning {} daily aggregates", aggregates.size());
        return ResponseEntity.ok(aggregates);
    }

    /**
     * Delete an expense.
     *
     * @param id Expense ID
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            @RequestHeader(HASS_USER_HEADER) String username) {
        logger.info("DELETE /api/expenses/{} - Deleting expense, user: {}", id, username);

        expenseService.deleteExpense(id, username);

        logger.info("Deleted expense ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
