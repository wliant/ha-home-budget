# Data Model: Parent Category Budget Auto-Rollup

**Feature**: 017-parent-budget-rollup
**Date**: 2026-02-15
**Phase**: Phase 1 (Design)

## Overview

This feature leverages the **existing database schema** from Features 004 (Hierarchical Category Budgets) and 012 (Parent Category Budgets). No schema changes are required; only service-layer logic changes to implement automatic parent budget rollup.

## Entities

### Budget (Existing - No Schema Changes)

**Table**: `budgets`

**Purpose**: Stores budget allocations for categories by time period (monthly or yearly). Parent budgets are automatically created/updated when child category budgets change.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique budget identifier |
| category_id | BIGINT | NOT NULL, FK → categories.id | Category this budget applies to (parent or child) |
| year | INT | NOT NULL | Budget year (e.g., 2026) |
| month | INT | NULLABLE | Budget month (1-12), NULL = yearly budget |
| amount | DECIMAL(15,2) | NOT NULL, >= 0 | Budget amount (auto-calculated for parent budgets via rollup) |
| description | TEXT | NULLABLE | User-provided budget description |
| created_by | VARCHAR(100) | NOT NULL | X-Hass-User header value (Home Assistant user) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Budget creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last modification timestamp |
| version | INT | NOT NULL, DEFAULT 0 | Optimistic locking version |

**Indexes**:
- `idx_budgets_category_year_month` on (category_id, year, month) - for parent budget lookup
- `idx_budgets_year` on (year) - for yearly budget view queries

**Validation Rules**:
- `amount` must be >= 0 (enforced by database CHECK constraint)
- `month` must be NULL or 1-12 (enforced by application layer)
- Unique constraint on (category_id, year, month) - one budget per category per period

**Lifecycle**:
- **Create**: User creates child budget → service layer auto-creates/updates parent budget (atomic transaction)
- **Update**: User updates child budget → service layer adjusts parent budget by delta
- **Delete**: User deletes child budget → service layer decreases parent budget amount (parent record persists even at zero)

**Rollup Behavior** (New Logic - No Schema Change):
- When a **child category budget** is created/updated/deleted, the service layer:
  1. Calculates amount delta (create: +amount, update: newAmount - oldAmount, delete: -amount)
  2. Finds or creates parent category budget for same period (year, month)
  3. Adds delta to parent budget amount
  4. Saves parent budget in same transaction (@Transactional ensures atomicity)

### Category (Existing - No Changes)

**Table**: `categories`

**Purpose**: Defines budget categories in a 2-level hierarchy (parent-child relationships). Used to determine which budgets trigger parent rollup.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique category identifier |
| name | VARCHAR(255) | NOT NULL, UNIQUE | Category name (e.g., "Groceries", "Fresh Produce") |
| icon | VARCHAR(50) | NULLABLE | Emoji or icon identifier (e.g., "🛒") |
| parent_category_id | BIGINT | NULLABLE, FK → categories.id | Parent category ID (NULL = top-level/standalone category) |
| created_by | VARCHAR(100) | NOT NULL | X-Hass-User header value |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Category creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last modification timestamp |

**Hierarchy Rules**:
- **Standalone Category**: parent_category_id = NULL, no children → budget contributes directly to yearly total
- **Parent Category**: parent_category_id = NULL, has children → budget represents sum of children's budgets (auto-calculated)
- **Child Category**: parent_category_id != NULL → budget cascades to parent budget

**Cascade Logic Triggers**:
- When creating/updating/deleting a budget for a **child category** (parent_category_id != NULL), cascade to parent
- When creating/updating/deleting a budget for a **standalone or parent category**, no cascade (standalone) or no action (parent handles its own amount)

## Relationships

### Budget ↔ Category (Many-to-One)

**Relationship**: Each budget belongs to exactly one category. A category can have multiple budgets (different time periods).

**JPA Mapping**:
```java
@Entity
public class Budget {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}

@Entity
public class Category {
    // No bidirectional mapping needed (queries use repository methods)
}
```

**Queries**:
- Find parent budget for rollup: `budgetRepository.findByCategoryAndYearAndMonth(parentCategory, year, month)`
- Find all children for a parent category: `categoryRepository.findByParentCategoryId(parentCategoryId)`

### Category ↔ Category (Self-Referencing Hierarchy)

**Relationship**: Parent category has many child categories. Child category belongs to one parent category (or none if standalone).

**JPA Mapping**:
```java
@Entity
public class Category {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    // Bidirectional mapping not needed (use repository query instead)
}
```

**Hierarchy Constraints**:
- Maximum 2 levels (parent → child, no grandchildren)
- Enforced by application layer (database allows deeper nesting but app prevents it)

## Rollup Calculation Logic (Service Layer)

### Automatic Parent Budget Amount Calculation

When a child budget is created/updated/deleted, the parent budget amount is calculated as:

```
parent_budget.amount = SUM(child_budget.amount) FOR ALL children of parent category
```

**Example**:
```
Category Hierarchy:
- Groceries (parent_category_id = NULL)
  - Fresh Produce (parent_category_id = Groceries.id)
  - Pantry (parent_category_id = Groceries.id)
  - Frozen (parent_category_id = Groceries.id)

Budgets for January 2026:
- Fresh Produce: $500
- Pantry: $300
- Frozen: $200

Automatic Rollup Result:
- Groceries (parent): $1000 (auto-created/updated)
```

**Service Layer Implementation** (pseudo-code):
```java
@Transactional
void cascadeToParentBudget(Budget childBudget, BigDecimal amountDelta) {
    Category childCategory = childBudget.getCategory();
    if (childCategory.getParentCategory() == null) return;

    Category parentCategory = childCategory.getParentCategory();
    Budget parentBudget = budgetRepository
        .findByCategoryAndYearAndMonth(parentCategory, childBudget.getYear(), childBudget.getMonth())
        .orElseGet(() -> createParentBudget(parentCategory, childBudget.getYear(), childBudget.getMonth()));

    parentBudget.setAmount(parentBudget.getAmount().add(amountDelta));
    budgetRepository.save(parentBudget);
}
```

### Yearly Budget View Total Calculation (Service Layer)

The yearly budget view sums only **parent category budgets** and **standalone category budgets** to avoid double-counting:

```
total_budget = SUM(budget.amount)
WHERE budget.year = {year}
  AND (
    budget.category.parent_category_id IS NULL  -- Standalone categories
    OR
    EXISTS (SELECT 1 FROM categories c WHERE c.parent_category_id = budget.category_id) -- Parent categories
  )
```

**Filtering Logic** (pseudo-code):
```java
List<BudgetSummaryDTO> getYearlyBudgetView(int year) {
    List<Budget> allBudgets = budgetRepository.findByYear(year);
    Map<Long, Category> categoryMap = categoryRepository.findAll().stream()
        .collect(Collectors.toMap(Category::getId, c -> c));

    // Filter: include only standalone + parent categories, exclude child categories
    List<Budget> countableBudgets = allBudgets.stream()
        .filter(b -> {
            Category category = categoryMap.get(b.getCategoryId());
            boolean isStandalone = category.getParentCategory() == null;
            boolean isParent = !categoryRepository.findByParentCategoryId(category.getId()).isEmpty();
            return isStandalone || isParent; // Exclude child categories
        })
        .collect(Collectors.toList());

    BigDecimal totalBudget = countableBudgets.stream()
        .map(Budget::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new YearlyBudgetViewDTO(year, countableBudgets, totalBudget);
}
```

## State Transitions

### Budget Lifecycle with Parent Cascade

```
[User Creates Child Budget]
    ↓
[BudgetService.createBudget()]
    ↓
[Save child budget to database]
    ↓
[cascadeToParentBudget(childBudget, +childAmount)]
    ↓
[Find or create parent budget for same period]
    ↓
[Add childAmount to parent budget amount]
    ↓
[Save parent budget to database]
    ↓
[@Transactional commits both saves atomically]
    ↓
[Return success to user]
```

**Rollback Scenario**:
```
[User Creates Child Budget]
    ↓
[BudgetService.createBudget()]
    ↓
[Save child budget to database] ← Success
    ↓
[cascadeToParentBudget(childBudget, +childAmount)]
    ↓
[Find or create parent budget for same period]
    ↓
[Add childAmount to parent budget amount]
    ↓
[Save parent budget to database] ← DATABASE ERROR (constraint violation)
    ↓
[@Transactional rolls back entire transaction]
    ↓
[Child budget NOT saved (rolled back)]
    ↓
[Return error to user: "Failed to create budget"]
```

### Parent Budget Amount Updates

| Operation | Amount Delta | Parent Budget Behavior |
|-----------|--------------|------------------------|
| Create child budget ($500) | +$500 | Parent amount += $500 (create parent if missing) |
| Update child budget ($500 → $700) | +$200 | Parent amount += $200 |
| Update child budget ($700 → $300) | -$400 | Parent amount -= $400 |
| Delete child budget ($300) | -$300 | Parent amount -= $300 (parent record persists even at zero) |

## Validation Rules

### Budget Validation (Enforced in BudgetService)

1. **Amount Validation**: amount >= 0 (positive budgets only)
2. **Period Validation**: month is NULL (yearly) or 1-12 (monthly)
3. **Category Validation**: category must exist and be active
4. **Duplicate Prevention**: Only one budget per (category_id, year, month) combination

### Parent Cascade Validation

1. **Atomicity**: Child budget save and parent cascade must succeed or fail together (@Transactional)
2. **Idempotency**: Re-running cascade with same delta produces same result (addition is commutative)
3. **Zero Preservation**: Parent budget with amount=0 is NOT deleted (persists for audit trail)

## Migration Notes

**Schema Changes**: None required. Existing `budgets` and `categories` tables support this feature as-is.

**Data Backfill**: Not required. Existing budgets remain valid. Automatic rollup only affects **new** budget operations (create/update/delete) going forward.

**Backward Compatibility**: ✅ Fully compatible. Existing budget creation workflows continue to work; auto-rollup is transparent to users and frontend code.

## Summary

The data model for parent category budget auto-rollup uses the **existing** `budgets` and `categories` tables with no schema changes. The rollup mechanism is implemented entirely in the **BudgetService layer** using Spring @Transactional to ensure atomicity. Parent budgets are automatically created/updated based on child budget operations, and the yearly budget view filters budgets by category hierarchy to sum only parent and standalone categories (avoiding double-counting).
