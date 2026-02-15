# Research: Parent Category Budget Auto-Rollup

**Feature**: 017-parent-budget-rollup
**Date**: 2026-02-15
**Phase**: Phase 0 (Technical Research)

## Overview

This document captures technical research decisions for implementing automatic parent category budget rollup. When a child category budget is created, updated, or deleted, the system must automatically adjust the parent category budget to maintain consistency.

## Decision 1: Budget Cascade Implementation Pattern

**Context**: Need to automatically create/update parent budgets when child budgets change.

**Decision**: Implement cascade logic in **BudgetService layer** using Spring's `@Transactional` to ensure atomicity.

**Rationale**:
- Service layer provides transactional boundaries (child + parent operations succeed or fail together)
- Avoids database triggers (keeps business logic in application code for maintainability)
- Allows validation and error handling in Java before persisting
- Consistent with existing project architecture (Feature 012: parent category budgets)

**Alternatives Considered**:
1. **Database triggers** - Rejected: Logic hidden in database, harder to test, version control complexity
2. **JPA lifecycle callbacks (@PostPersist, @PostUpdate)** - Rejected: Limited access to repositories, transaction scope unclear
3. **Spring Data JPA events** - Rejected: Event ordering guarantees unclear, debugging complexity

**Implementation Approach**:
```java
@Service
@Transactional
public class BudgetService {

    // Called by createBudget(), updateBudget(), deleteBudget()
    private void cascadeToParentBudget(Budget childBudget, BigDecimal amountDelta) {
        Category childCategory = childBudget.getCategory();
        if (childCategory.getParentCategory() == null) {
            return; // Standalone category, no cascade needed
        }

        Category parentCategory = childCategory.getParentCategory();
        Integer year = childBudget.getYear();
        Integer month = childBudget.getMonth(); // nullable (null = yearly)

        // Find or create parent budget
        Budget parentBudget = budgetRepository
            .findByCategoryAndYearAndMonth(parentCategory, year, month)
            .orElseGet(() -> createParentBudget(parentCategory, year, month));

        // Update parent amount
        BigDecimal newAmount = parentBudget.getAmount().add(amountDelta);
        parentBudget.setAmount(newAmount);
        budgetRepository.save(parentBudget);
    }
}
```

## Decision 2: Parent Budget Creation Strategy

**Context**: When child budget is created and no parent budget exists for that period.

**Decision**: Auto-create parent budget with **zero initial amount**, then apply child's amount delta.

**Rationale**:
- Separates "ensure parent exists" from "update parent amount" concerns
- Consistent creation logic whether parent exists or not
- Simplifies delete handling (parent remains at zero, not deleted)
- Audit trail preserved (parent budget has creation timestamp)

**Alternatives Considered**:
1. **Create parent with child's amount directly** - Rejected: Complicates multi-child scenarios (second child must detect existing parent)
2. **Lazy creation only when needed** - Rejected: Violates FR-001 requirement for immediate parent budget existence

**Edge Case Handling**:
- **Multiple children created in parallel**: Spring @Transactional with default isolation (READ_COMMITTED) prevents lost updates
- **Parent budget deleted manually**: Next child budget creation recreates parent (acceptable per FR-009)
- **Parent amount reaches zero**: Budget record persists with amount=0 (not deleted per FR-009)

## Decision 3: Budget Amount Delta Calculation

**Context**: Need to track amount changes for create/update/delete operations to adjust parent budgets.

**Decision**: Calculate delta in service layer before cascading:
- **Create**: delta = +childAmount (new budget)
- **Update**: delta = newAmount - oldAmount (difference only)
- **Delete**: delta = -childAmount (remove contribution)

**Rationale**:
- Simple arithmetic, no complex state tracking
- Works for all operations with single cascade method
- Easy to unit test (pure function)

**Alternatives Considered**:
1. **Store parent/child relationships in separate table** - Rejected: Over-engineering for 2-level hierarchy
2. **Use event sourcing for budget changes** - Rejected: Adds complexity without clear benefit for household-scale data

**Implementation Note**:
```java
// Update operation example
public BudgetDTO updateBudget(Long id, UpdateBudgetRequest request) {
    Budget existingBudget = budgetRepository.findById(id).orElseThrow();
    BigDecimal oldAmount = existingBudget.getAmount();
    BigDecimal newAmount = request.getAmount();
    BigDecimal delta = newAmount.subtract(oldAmount);

    existingBudget.setAmount(newAmount);
    // ... other field updates

    cascadeToParentBudget(existingBudget, delta); // Pass delta
    return toDTO(budgetRepository.save(existingBudget));
}
```

## Decision 4: Yearly Budget View Total Calculation

**Context**: FR-006/FR-007 require summing only parent category budgets (not child budgets) to avoid double-counting.

**Decision**: Modify `BudgetService.getYearlyBudgetView()` to filter budgets by category hierarchy before summing.

**Rationale**:
- Frontend receives pre-filtered budget list (no client-side logic needed)
- Consistent with existing backend aggregation pattern (Feature 016: category expense aggregates)
- Single source of truth for "what counts as total budget"

**Implementation Strategy**:
```java
public YearlyBudgetViewDTO getYearlyBudgetView(int year) {
    List<BudgetSummaryDTO> allBudgets = getBudgetsForYear(year);

    // Load category hierarchy
    Map<Long, Category> categoryMap = categoryRepository.findAll()
        .stream().collect(Collectors.toMap(Category::getId, c -> c));

    // Filter: include only standalone categories + parent categories
    List<BudgetSummaryDTO> countableBudgets = allBudgets.stream()
        .filter(b -> {
            Category category = categoryMap.get(b.getCategoryId());
            // Include if: (1) no parent (standalone) OR (2) has children (parent)
            return category.getParentCategory() == null
                || categoryRepository.findByParentCategoryId(category.getId()).size() > 0;
        })
        .collect(Collectors.toList());

    BigDecimal totalBudget = countableBudgets.stream()
        .map(BudgetSummaryDTO::getTotalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // ... return DTO with filtered budgets and totalBudget
}
```

**Alternatives Considered**:
1. **Frontend filters budgets** - Rejected: Duplicates logic, inconsistent totals if frontend/backend diverge
2. **Add "isCountable" flag to Budget entity** - Rejected: Redundant with category hierarchy data
3. **Separate parent/child budget tables** - Rejected: Over-engineering, breaks existing schema

## Decision 5: Testing Strategy

**Context**: Need to verify cascade logic, transaction atomicity, and edge cases.

**Decision**: Multi-layer testing approach:
1. **Unit tests** (BudgetServiceTest.java): Mock repositories, verify cascade method logic
2. **Integration tests** (BudgetRepositoryTest.java with @DataJpaTest): Real database, verify parent creation/update queries
3. **Manual testing** (quickstart.md scenarios): End-to-end budget creation through REST API

**Rationale**:
- Unit tests catch logic errors early (fast feedback)
- Integration tests verify JPA queries and transactions work correctly
- Manual tests validate user-facing behavior matches specification

**Test Coverage Requirements**:
- ✅ Create child budget → parent created automatically (FR-001)
- ✅ Create second child → parent amount increases (FR-002)
- ✅ Update child amount → parent adjusts by delta (FR-003)
- ✅ Delete child → parent amount decreases (FR-004)
- ✅ Monthly vs yearly separation (FR-005)
- ✅ Yearly view sums only parent budgets (FR-006, FR-007)
- ✅ Manual parent budget + auto-rollup coexist (FR-008)
- ✅ Parent budget persists at zero (FR-009)

**Alternatives Considered**:
1. **Only manual testing** - Rejected: Regression risk too high for core budget logic
2. **Testcontainers for all tests** - Rejected: Slower, unnecessary for unit-testable service logic
3. **Property-based testing** - Rejected: Overkill for household-scale deterministic operations

## Decision 6: Migration Strategy

**Context**: Existing budgets in production (Features 001-016) need to remain valid.

**Decision**: **No database schema changes required.** The `budgets` table schema supports this feature as-is:
- `category_id` (existing): Links budget to category
- `parent_category_id` in `categories` table (existing): Defines hierarchy
- `year` and `month` (existing): Period identification
- `amount` (existing): Budget amount (will be auto-calculated for parent budgets)

**Rationale**:
- Feature 012 already established parent category budgets (schema supports it)
- Feature 004 established 2-level category hierarchy (parent-child relationships exist)
- No new data fields needed; only business logic changes in BudgetService

**Migration Approach**:
- **No Liquibase migration needed** (schema unchanged)
- **Backward compatibility**: Existing budgets work as-is
- **Data backfill**: Not required (new auto-rollup only affects future budget operations)

**Alternatives Considered**:
1. **Add "auto_generated" flag to budgets table** - Rejected: Not needed for functionality, adds complexity
2. **Backfill parent budgets for existing children** - Rejected: Out of scope (FR applies to new operations only)
3. **Add "rollup_source" JSON column** - Rejected: Over-engineering; category hierarchy provides source information

## Decision 7: Error Handling

**Context**: Cascade operations might fail (DB constraints, validation errors).

**Decision**: Use Spring @Transactional rollback on any cascade failure. Return clear error messages to frontend.

**Rationale**:
- Atomicity guaranteed: child budget not saved if parent cascade fails
- User sees failure immediately (no partial state)
- Consistent with existing error handling pattern (BudgetController returns 400/500 status codes)

**Error Scenarios**:
- **Parent category not found**: Should never happen (Category entity enforces FK constraint), but throw IllegalStateException if detected
- **Amount calculation overflow**: Unlikely for household budgets, but catch ArithmeticException if BigDecimal operations fail
- **Concurrent modification**: Spring @Transactional with default isolation handles this (last-write-wins for parent amount)

**Implementation**:
```java
@Transactional
public BudgetDTO createBudget(CreateBudgetRequest request) {
    try {
        Budget budget = new Budget(/* ... */);
        budget = budgetRepository.save(budget);
        cascadeToParentBudget(budget, budget.getAmount()); // Throws on failure
        return toDTO(budget);
    } catch (Exception e) {
        // Spring rolls back transaction automatically
        logger.error("Budget creation failed: {}", e.getMessage(), e);
        throw new BudgetCreationException("Failed to create budget and update parent", e);
    }
}
```

## Summary

The implementation uses **service-layer cascade logic** with Spring @Transactional to maintain parent budget consistency. No schema changes required (existing tables support the feature). The yearly budget view filters budgets by category hierarchy before summing totals. Testing covers unit, integration, and manual scenarios per quickstart.md. All decisions align with existing architecture patterns established in Features 001-016.
