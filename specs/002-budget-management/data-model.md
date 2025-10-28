# Data Model: Budget and Expense Management

**Feature**: 002-budget-management
**Date**: 2025-10-23
**Purpose**: Define entities, relationships, and validation rules for budget tracking (implementation-agnostic)

## Entity Overview

This feature introduces three core entities for household budget management:

1. **Budget**: A spending plan for a specific month with target amount
2. **Expense**: An individual spending transaction recorded against a budget
3. **Category**: A classification for organizing expenses (e.g., Groceries, Utilities)

## Entities

### Budget

Represents a household's spending plan for a specific calendar month.

**Attributes**:

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | Identifier | Primary key, auto-generated | Unique budget identifier |
| year | Integer | Required, min=2000, max=9999 | Calendar year (e.g., 2025) |
| month | Integer | Required, min=1, max=12 | Calendar month (1=Jan, 12=Dec) |
| totalAmount | Decimal | Required, min=0.01, precision=10, scale=2 | Planned spending limit (e.g., 3000.00) |
| description | Text | Optional, max length=500 | User-provided budget description (e.g., "Family budget for October") |
| createdBy | String | Required, max length=100 | Username from X-Hass-User header who created this budget |
| createdAt | Timestamp | Required, auto-set on creation | When budget was created |
| updatedAt | Timestamp | Required, auto-update on modification | Last modification timestamp |
| version | Integer | Required, default=0 | Optimistic locking version for concurrent updates |

**Uniqueness Constraints**:
- Combination of (year, month) must be unique - prevents duplicate budgets for same month (FR-002)

**Calculated Fields** (computed, not stored):
- `totalSpending`: Sum of all associated expense amounts
- `spendingPercentage`: (totalSpending / totalAmount) × 100

**Validation Rules**:
- V-001: totalAmount must be positive (>0) (FR-003)
- V-002: year must be between 2000 and 9999 (reasonable range)
- V-003: month must be between 1 and 12 (valid calendar month)
- V-004: Cannot create budget with duplicate (year, month) (FR-002)

**Business Rules**:
- BR-001: Budget can be edited (amount and description), but year/month cannot change after creation (FR-019)
- BR-002: Budget can only be deleted if no associated expenses exist, or user confirms cascade deletion (FR-022)
- BR-003: All expenses for a budget are deleted when budget is deleted (cascade)

---

### Expense

Represents a single spending transaction recorded by a household member.

**Attributes**:

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | Identifier | Primary key, auto-generated | Unique expense identifier |
| amount | Decimal | Required, min=0.01, precision=10, scale=2 | Expense amount (e.g., 150.50) |
| description | Text | Required, max length=500 | User-provided expense description (e.g., "Weekly groceries at Whole Foods") |
| expenseDate | Date | Required | Date when expense occurred (not necessarily when recorded) |
| budgetId | Identifier | Required, foreign key → Budget | Which budget this expense is tracked against |
| categoryId | Identifier | Optional, foreign key → Category | Spending category (null defaults to "Uncategorized") |
| createdBy | String | Required, max length=100 | Username from X-Hass-User header who recorded this expense |
| createdAt | Timestamp | Required, auto-set on creation | When expense was recorded in system |
| updatedAt | Timestamp | Required, auto-update on modification | Last modification timestamp |
| version | Integer | Required, default=0 | Optimistic locking version for concurrent updates |

**Validation Rules**:
- V-005: amount must be positive (>0)
- V-006: expenseDate must be valid calendar date (FR-014)
- V-007: description cannot be empty or whitespace-only
- V-008: budgetId must reference existing Budget
- V-009: categoryId (if provided) must reference existing Category

**Business Rules**:
- BR-004: Expense can be edited with all fields modifiable (FR-020)
- BR-005: Expense can be deleted at any time (FR-021)
- BR-006: If expenseDate falls outside budget's month, system should warn user but allow creation (FR-018)
- BR-007: If categoryId is null, system treats expense as "Uncategorized" category (FR-010)

**Derived Warnings** (not stored, computed for display):
- `dateOutsideMonth`: Boolean indicating if expenseDate is not in budget's (year, month)

---

### Category

Represents a classification for organizing expenses by type.

**Attributes**:

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | Identifier | Primary key, auto-generated | Unique category identifier |
| name | String | Required, max length=100, unique | Category name (e.g., "Groceries", "Utilities") |
| icon | String | Optional, max length=10 | Unicode emoji icon (e.g., "🛒", "🚗", "🏠") |
| createdBy | String | Required, max length=100 | Username from X-Hass-User header who created category |
| createdAt | Timestamp | Required, auto-set on creation | When category was created |
| isSystem | Boolean | Required, default=false | True for system-provided categories (e.g., "Uncategorized") |

**Validation Rules**:
- V-010: name cannot be empty or whitespace-only
- V-011: name must be unique (case-insensitive recommended)
- V-012: icon, if provided, must be valid Unicode emoji (optional validation)

**Business Rules**:
- BR-008: Category cannot be deleted if any expenses reference it (FR-011)
- BR-009: System category "Uncategorized" (isSystem=true) cannot be deleted or renamed
- BR-010: Categories are shared across all household members - any user can create/use any category (FR-009)

---

## Relationships

### Budget ↔ Expense (One-to-Many)

- **Cardinality**: One Budget has zero or many Expenses
- **Directionality**: Bidirectional
- **Ownership**: Budget owns the relationship
- **Cascade**: Delete Budget → Delete all associated Expenses (with confirmation per BR-002)
- **Fetch**: Lazy loading recommended (fetch expenses only when needed)

**Queries**:
- Find all expenses for a budget (for budget detail view)
- Find budget for an expense (for expense editing)
- Calculate total spending for a budget (aggregate sum)

---

### Category ↔ Expense (One-to-Many)

- **Cardinality**: One Category has zero or many Expenses
- **Directionality**: Bidirectional
- **Ownership**: Category owns the relationship
- **Cascade**: No cascade delete - must check for expenses before deleting category (BR-008)
- **Fetch**: Lazy loading

**Queries**:
- Find all expenses for a category (for category spending breakdown)
- Find category for an expense (for expense display)
- Count expenses per category (for preventing deletion)

---

## Entity Relationship Diagram (Text)

```text
┌──────────────────────┐
│      Budget          │
├──────────────────────┤
│ id (PK)              │
│ year                 │
│ month                │
│ totalAmount          │
│ description          │
│ createdBy            │
│ createdAt            │
│ updatedAt            │
│ version              │
└──────────────────────┘
         │
         │ 1
         │
         │
         │ *
         ▼
┌──────────────────────┐       ┌──────────────────────┐
│      Expense         │       │     Category         │
├──────────────────────┤       ├──────────────────────┤
│ id (PK)              │       │ id (PK)              │
│ amount               │   *   │ name (UNIQUE)        │
│ description          │◄──────┤ icon                 │
│ expenseDate          │   1   │ createdBy            │
│ budgetId (FK)        │       │ createdAt            │
│ categoryId (FK)      │       │ isSystem             │
│ createdBy            │       └──────────────────────┘
│ createdAt            │
│ updatedAt            │
│ version              │
└──────────────────────┘

Relationships:
- Budget → Expense: 1:* (cascade delete)
- Category → Expense: 1:* (prevent delete if expenses exist)
- Expense.categoryId can be NULL (defaults to "Uncategorized" system category)
```

---

## State Transitions

### Budget Lifecycle

```text
[Created] → [Active] → [Deleted]
    ↑          │
    └──────────┘
     (Edited - amount/description only)
```

- **Created**: User creates budget with year, month, totalAmount
- **Active**: Budget exists and can have expenses recorded
- **Edited**: totalAmount or description updated (year/month immutable per BR-001)
- **Deleted**: Budget and all expenses removed (requires confirmation if expenses exist per BR-002)

### Expense Lifecycle

```text
[Created] → [Active] → [Deleted]
    ↑          │
    └──────────┘
     (Edited - any field can change)
```

- **Created**: User records expense with amount, description, date, optional category
- **Active**: Expense counts toward budget's total spending
- **Edited**: Any field (amount, description, date, category) can be modified (FR-020)
- **Deleted**: Expense removed, budget's total spending recalculated (FR-021)

### Category Lifecycle

```text
[Created] → [Active] → [Deleted]
    ↑          │          │
    └──────────┘          ▼
     (Edited)       [Blocked if expenses exist]
```

- **Created**: User creates category with name and optional icon
- **Active**: Category available for expense classification
- **Edited**: Name or icon can be changed (except system categories per BR-009)
- **Deleted**: Only allowed if no expenses reference this category (FR-011)
- **Blocked**: Deletion prevented if expenses exist, user must reassign first

---

## Indexes (Performance Optimization)

Recommended indexes for query performance (implementation-specific but documented here):

1. **budgets table**:
   - Primary key: `id`
   - Unique index: `(year, month)` - for duplicate prevention and month lookups
   - Index: `created_by` - if filtering by user

2. **expenses table**:
   - Primary key: `id`
   - Index: `budget_id` - for fast "expenses for budget" queries
   - Index: `category_id` - for category breakdown queries
   - Index: `expense_date` - for date range filtering (FR-012)
   - Index: `created_by` - for user-based filtering (FR-012)

3. **categories table**:
   - Primary key: `id`
   - Unique index: `name` - for duplicate prevention

---

## Validation Summary by Requirement

| Requirement ID | Validation Rules | Enforced By |
|----------------|-----------------|-------------|
| FR-001 | V-001 (amount > 0), V-002 (year range), V-003 (month 1-12) | Budget entity validation |
| FR-002 | V-004 (unique year+month) | Database unique constraint |
| FR-003 | V-001 (amount > 0) | Budget entity validation |
| FR-005 | V-005 (expense amount > 0), V-006 (valid date), V-007 (description not empty) | Expense entity validation |
| FR-009 | V-010 (name not empty), V-011 (name unique) | Category entity validation |
| FR-011 | BR-008 (check expense count before delete) | Category service business logic |
| FR-014 | V-006 (valid calendar date) | Expense entity validation |
| FR-018 | BR-006 (warn if date outside month) | Expense service business logic |
| FR-019 | BR-001 (only amount/description editable) | Budget service business logic |
| FR-022 | BR-002 (confirm cascade delete) | Budget service business logic |

---

## Assumptions

1. **Single Currency**: All amounts are in same currency (no multi-currency support)
2. **Month Granularity**: Budgets are monthly, not weekly or yearly
3. **Shared Data**: All household members see all budgets/expenses/categories (no per-user isolation)
4. **No Soft Deletes**: Deleted entities are permanently removed from database
5. **No Audit History**: Edits overwrite previous values (no change history tracking)
6. **Username Denormalization**: createdBy stores username string, not User foreign key

---

## Future Enhancements (Not in Current Scope)

- Budget templates (recurring monthly budgets)
- Income tracking (vs. expense tracking only)
- Budget categories (allocate totalAmount across categories)
- Expense attachments (receipt images)
- Budget alerts (notifications when approaching limit)
- Multi-currency support
- Expense recurrence (recurring bills)
- Budget rollover (unused amount to next month)

---

## References

- Feature Spec: [spec.md](spec.md)
- Research: [research.md](research.md)
- Functional Requirements: spec.md sections FR-001 through FR-022
