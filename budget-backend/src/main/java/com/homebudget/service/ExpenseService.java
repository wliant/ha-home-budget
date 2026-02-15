package com.homebudget.service;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.dto.ExpenseFileDTO;
import com.homebudget.dto.ExpenseListResponse;
import com.homebudget.exception.CategoryNotFoundException;
import com.homebudget.exception.ExpenseNotFoundException;
import com.homebudget.model.Category;
import com.homebudget.model.Expense;
import com.homebudget.model.ExpenseFile;
import com.homebudget.repository.CategoryRepository;
import com.homebudget.repository.ExpenseFileRepository;
import com.homebudget.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing expenses.
 *
 * Handles expense CRUD operations with category-based organization.
 * Expenses are associated with categories (not budgets directly).
 */
@Service
@Transactional
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseFileRepository expenseFileRepository;

    @Value("${EXPENSE_FILE_DIR:./data/expense-files}")
    private String expenseFileDir;

    /**
     * Create a new expense.
     *
     * @param dto Expense data (amount, description, expenseDate, categoryId)
     * @param username User creating the expense (from X-Hass-User header)
     * @return Created expense DTO
     * @throws CategoryNotFoundException if category not found
     */
    public ExpenseDTO createExpense(ExpenseDTO dto, String username) {
        logger.info("Creating expense for user: {}", username);

        // Create expense entity
        Expense expense = new Expense();
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setCreatedBy(username);

        // Set category - required; fall back to system "Uncategorized" if null
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));
            expense.setCategory(category);
            logger.debug("Category assigned: ID={}, name='{}'", category.getId(), category.getName());
        } else {
            Category uncategorized = categoryRepository.findByName("Uncategorized")
                    .orElseThrow(() -> new IllegalStateException("System 'Uncategorized' category not found"));
            expense.setCategory(uncategorized);
            logger.debug("No category provided, assigned system 'Uncategorized' category ID={}", uncategorized.getId());
        }

        logger.debug("Expense details: amount={}, date={}, description='{}'",
                dto.getAmount(), dto.getExpenseDate(), dto.getDescription());

        // Save expense
        Expense saved = expenseRepository.save(expense);
        logger.info("Created expense ID: {}", saved.getId());

        return toDTO(saved);
    }

    public ExpenseDTO createExpenseWithFiles(ExpenseDTO dto, List<MultipartFile> files, String username) {
        ExpenseDTO created = createExpense(dto, username);
        if (files != null && !files.isEmpty()) {
            Expense expense = expenseRepository.findById(created.getId())
                    .orElseThrow(() -> new ExpenseNotFoundException(created.getId()));
            saveExpenseFiles(expense, files);
        }
        return getExpenseById(created.getId());
    }

    /**
     * Get all expenses with optional filtering.
     *
     * @param categoryId Filter by category ID (optional)
     * @param startDate Filter by date range start (optional)
     * @param endDate Filter by date range end (optional)
     * @param createdBy Filter by user who created (optional)
     * @return List of expenses matching filters
     */
    @Transactional(readOnly = true)
    public List<ExpenseDTO> getAllExpenses(Long categoryId,
                                          LocalDate startDate, LocalDate endDate,
                                          String createdBy) {
        logger.info("Finding expenses with filters - categoryId: {}, dateRange: {}-{}, createdBy: {}",
                   categoryId, startDate, endDate, createdBy);

        List<Expense> expenses;

        // Apply filters based on what's provided
        if (categoryId != null) {
            List<Long> expandedIds = expandCategoryIds(categoryId);
            if (expandedIds != null) {
                expenses = new ArrayList<>();
                for (Long catId : expandedIds) {
                    expenses.addAll(expenseRepository.findByCategoryId(catId));
                }
            } else {
                expenses = expenseRepository.findByCategoryId(categoryId);
            }
        } else if (startDate != null && endDate != null) {
            expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate);
        } else if (createdBy != null) {
            expenses = expenseRepository.findByCreatedBy(createdBy);
        } else {
            expenses = expenseRepository.findAllOrderByExpenseDateDesc();
        }

        logger.info("Found {} expenses", expenses.size());

        return expenses.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get expense by ID.
     *
     * @param id Expense ID
     * @return Expense DTO
     * @throws ExpenseNotFoundException if expense not found
     */
    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long id) {
        logger.info("Finding expense by ID: {}", id);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        return toDTO(expense);
    }

    /**
     * Update an existing expense.
     *
     * @param id Expense ID
     * @param dto Updated expense data
     * @return Updated expense DTO
     * @throws ExpenseNotFoundException if expense not found
     * @throws CategoryNotFoundException if new category not found
     */
    public ExpenseDTO updateExpense(Long id, ExpenseDTO dto) {
        logger.info("Updating expense ID: {}", id);

        // Find existing expense
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        // Update fields
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setExpenseDate(dto.getExpenseDate());

        // Update category if changed
        if (dto.getCategoryId() != null) {
            if (expense.getCategory() == null || !expense.getCategory().getId().equals(dto.getCategoryId())) {
                Category newCategory = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));
                expense.setCategory(newCategory);
            }
        } else {
            // Fall back to system "Uncategorized" category
            Category uncategorized = categoryRepository.findByName("Uncategorized")
                    .orElseThrow(() -> new IllegalStateException("System 'Uncategorized' category not found"));
            expense.setCategory(uncategorized);
        }

        // Save updated expense
        Expense updated = expenseRepository.save(expense);
        logger.info("Updated expense ID: {}", id);

        return toDTO(updated);
    }

    public ExpenseDTO updateExpenseWithFiles(Long id, ExpenseDTO dto, List<MultipartFile> files) {
        ExpenseDTO updated = updateExpense(id, dto);
        if (files != null && !files.isEmpty()) {
            Expense expense = expenseRepository.findById(id)
                    .orElseThrow(() -> new ExpenseNotFoundException(id));
            saveExpenseFiles(expense, files);
        }
        return getExpenseById(id);
    }

    /**
     * Delete an expense.
     *
     * @param id Expense ID
     * @throws ExpenseNotFoundException if expense not found
     */
    public void deleteExpense(Long id) {
        logger.info("Deleting expense ID: {}", id);

        if (!expenseRepository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }

        expenseRepository.deleteById(id);
        logger.info("Deleted expense ID: {}", id);
    }

    /**
     * Get paginated, filtered, sorted expense list with aggregate summary.
     * Converts year/month to date range and delegates to repository.
     *
     * @param year required year filter
     * @param month optional month filter (1-12)
     * @param categoryId optional category filter
     * @param minAmount optional minimum amount filter (inclusive)
     * @param maxAmount optional maximum amount filter (inclusive)
     * @param createdBy optional creator filter
     * @param pageable pagination and sorting parameters
     * @param sortBy sort field name for response metadata
     * @param sortDirection sort direction for response metadata
     * @return ExpenseListResponse with paginated content and summary
     */
    @Transactional(readOnly = true)
    public ExpenseListResponse getExpenseList(int year, Integer month, Long categoryId,
                                               BigDecimal minAmount, BigDecimal maxAmount,
                                               String createdBy, Pageable pageable,
                                               String sortBy, String sortDirection) {
        logger.info("Getting expense list - year: {}, month: {}, categoryId: {}, amountRange: {}-{}, createdBy: {}",
                year, month, categoryId, minAmount, maxAmount, createdBy);

        // Convert year/month to date range
        LocalDate startDate;
        LocalDate endDate;
        if (month != null) {
            YearMonth ym = YearMonth.of(year, month);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        logger.debug("Date range: {} to {}", startDate, endDate);

        // Expand category filter to include child categories if parent category selected
        List<Long> expandedCategoryIds = expandCategoryIds(categoryId);

        // Get paginated results
        Page<Expense> page;
        BigDecimal totalAmount;
        if (expandedCategoryIds != null) {
            page = expenseRepository.findByFiltersPageableWithCategoryIds(
                    expandedCategoryIds, startDate, endDate, minAmount, maxAmount, createdBy, pageable);
            totalAmount = expenseRepository.getFilteredTotalAmountWithCategoryIds(
                    expandedCategoryIds, startDate, endDate, minAmount, maxAmount, createdBy);
        } else {
            page = expenseRepository.findByFiltersPageable(
                    categoryId, startDate, endDate, minAmount, maxAmount, createdBy, pageable);
            totalAmount = expenseRepository.getFilteredTotalAmount(
                    categoryId, startDate, endDate, minAmount, maxAmount, createdBy);
        }

        // Convert to DTOs
        List<ExpenseDTO> content = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        logger.info("Found {} expenses (page {} of {}), total amount: {}",
                page.getTotalElements(), page.getNumber(), page.getTotalPages(), totalAmount);

        return new ExpenseListResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                totalAmount,
                sortBy,
                sortDirection
        );
    }

    /**
     * Get distinct years that have expense data, sorted descending.
     *
     * @return list of years with expenses
     */
    @Transactional(readOnly = true)
    public List<Integer> getDistinctYears() {
        logger.info("Getting distinct expense years");
        List<Integer> years = expenseRepository.findDistinctYears();
        logger.info("Found {} distinct years", years.size());
        return years;
    }

    /**
     * Get distinct expense creators, sorted ascending.
     *
     * @return list of creator usernames
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCreators() {
        logger.info("Getting distinct expense creators");
        List<String> creators = expenseRepository.findDistinctCreators();
        logger.info("Found {} distinct creators", creators.size());
        return creators;
    }

    /**
     * Convert Expense entity to DTO.
     */
    private ExpenseDTO toDTO(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setCreatedBy(expense.getCreatedBy());
        dto.setCreatedAt(expense.getCreatedAt());
        dto.setUpdatedAt(expense.getUpdatedAt());
        dto.setVersion(expense.getVersion());

        // Include category if present
        if (expense.getCategory() != null) {
            dto.setCategoryId(expense.getCategory().getId());
            dto.setCategoryName(expense.getCategory().getName());
            dto.setCategoryIcon(expense.getCategory().getIcon());
        }

        List<ExpenseFileDTO> files = expenseFileRepository.findByExpenseIdOrderByIdAsc(expense.getId())
                .stream()
                .map(file -> new ExpenseFileDTO(file.getId(), file.getOriginalFilename()))
                .collect(Collectors.toList());
        dto.setFiles(files);

        return dto;
    }

    private void saveExpenseFiles(Expense expense, List<MultipartFile> files) {
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Maximum 5 files allowed per expense");
        }

        long existingCount = expenseFileRepository.countByExpenseId(expense.getId());
        if (existingCount + files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Maximum 5 files allowed per expense");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }

        Path baseDir = resolveExpenseDir(expense);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create expense file directory", e);
        }

        for (MultipartFile file : files) {
            ExpenseFile expenseFile = new ExpenseFile();
            expenseFile.setExpense(expense);
            expenseFile.setOriginalFilename(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());

            String storedName = expense.getId() + "_" + java.util.UUID.randomUUID();
            Path target = baseDir.resolve(storedName);
            expenseFile.setFilePath(target.toString());
            expenseFile = expenseFileRepository.save(expenseFile);

            String storedWithId = expense.getId() + "_" + expenseFile.getId();
            Path finalTarget = baseDir.resolve(storedWithId);
            try {
                file.transferTo(finalTarget);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store expense file", e);
            }
            if (!finalTarget.equals(target)) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException e) {
                    logger.warn("Failed to delete temp file {}", target, e);
                }
            }
            expenseFile.setFilePath(finalTarget.toString());
            expenseFileRepository.save(expenseFile);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        boolean isImage = contentType.startsWith("image/");
        boolean isPdf = "application/pdf".equals(contentType);
        if (!isImage && !isPdf) {
            throw new IllegalArgumentException("Only PDF and image files are allowed");
        }
    }

    private Path resolveExpenseDir(Expense expense) {
        int year = expense.getExpenseDate().getYear();
        String category = expense.getCategory() != null ? expense.getCategory().getName() : "uncategorized";
        String categorySlug = sanitizeFolderName(category);
        return Paths.get(expenseFileDir, String.valueOf(year), categorySlug);
    }

    ExpenseFile attachExistingFile(Expense expense, Path sourcePath, String originalFilename) {
        Path baseDir = resolveExpenseDir(expense);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create expense file directory", e);
        }

        ExpenseFile expenseFile = new ExpenseFile();
        expenseFile.setExpense(expense);
        expenseFile.setOriginalFilename(originalFilename == null ? "unnamed" : originalFilename);

        String tempName = expense.getId() + "_" + java.util.UUID.randomUUID();
        Path tempTarget = baseDir.resolve(tempName);
        expenseFile.setFilePath(tempTarget.toString());
        expenseFile = expenseFileRepository.save(expenseFile);

        String finalName = expense.getId() + "_" + expenseFile.getId();
        Path finalTarget = baseDir.resolve(finalName);
        try {
            Files.move(sourcePath, finalTarget);
        } catch (IOException e) {
            throw new RuntimeException("Failed to move expense file", e);
        }
        expenseFile.setFilePath(finalTarget.toString());
        return expenseFileRepository.save(expenseFile);
    }

    private String sanitizeFolderName(String input) {
        String normalized = input == null ? "uncategorized" : input.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "uncategorized" : normalized;
    }

    /**
     * If the given categoryId is a parent category (has children), expand to a list
     * containing the parent + all child category IDs. Returns null if not a parent category
     * or categoryId is null.
     */
    private List<Long> expandCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        long childCount = categoryRepository.countByParentCategoryId(categoryId);
        if (childCount == 0) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        List<Category> children = categoryRepository.findByParentCategoryId(categoryId);
        for (Category child : children) {
            ids.add(child.getId());
        }
        logger.debug("Expanded category filter: parent={} -> {} category IDs", categoryId, ids.size());
        return ids;
    }
}
