# Data Model: Hierarchical Category Budgets

**Feature**: 004-hierarchical-category-budgets
**Date**: 2025-11-11
**Purpose**: Define entities, relationships, validation rules, and state transitions

## Entity Definitions

### Category (Extended)

**Purpose**: Represents a spending classification that can optionally have a parent category, forming a two-level hierarchy.

**Attributes**:
- `id` (Long): Primary key, auto-generated
- `name` (String): Category name, required, unique, max 100 characters
- `icon` (String): Icon identifier, optional, max 10 characters
- `parentCategory` (Category): Reference to parent category, optional (NULL for root categories)
- `children` (List<Category>): Collection of child categories, derived (not stored)
- `isSystem` (Boolean): Whether category is system-managed, default false
- `createdBy` (String): User who created category (from X-Hass-User header), required
- `createdAt` (LocalDateTime): Creation timestamp, auto-set, immutable

**Relationships**:
- **Self-referencing**: `parent_category_id` foreign key references `categories.id`
  - One parent can have many children (one-to-many)
  - One child has at most one parent (many-to-one, optional)
- **To Budget**: One category can have many budgets (one-to-many)
- **To Expense**: One category can have many expenses (one-to-many, existing relationship)

**Constraints**:
- `name` must be unique across all categories
- `parent_category_id` must reference existing category or be NULL
- Category cannot be its own ancestor (circular reference prevention)
- Maximum hierarchy depth: 2 levels (if category has parent, it cannot have children that have children)
- System categories (`isSystem = true`) cannot have parents assigned
- System categories cannot be deleted

**Indexes**:
- Primary: `id`
- Unique: `name`
- Foreign Key (auto-indexed): `parent_category_id`

---

### Budget (Extended)

**Purpose**: Represents allocated spending amount for a specific category during a specific time period (year-month).

**Attributes**:
- `id` (Long): Primary key, auto-generated
- `year` (Integer): Budget year, required, 2000-9999
- `month` (Integer): Budget month, required, 1-12
- `category` (Category): Associated spending category, **required for new budgets**, optional for legacy budgets
- `totalAmount` (BigDecimal): Allocated budget amount, required, must be positive, precision 10 scale 2
- `description` (String): Budget notes, optional, max 500 characters
- `createdBy` (String): User who created budget (from X-Hass-User header), required
- `createdAt` (LocalDateTime): Creation timestamp, auto-set, immutable
- `updatedAt` (LocalDateTime): Last update timestamp, auto-updated
- `version` (Long): Optimistic locking version, auto-managed

**Relationships**:
- **To Category**: Many budgets belong to one category (many-to-one)
- **To Expense**: One budget can have many expenses (one-to-many, existing relationship)

**Constraints**:
- `year` and `month` must represent valid calendar period
- `totalAmount` must be greater than zero
- **Unique**: One budget per `(category_id, year, month)` combination
  - Partial unique index: only enforced when `category_id IS NOT NULL`
  - Allows legacy budgets (category_id = NULL) to coexist for same period
- **Parent-Child Sum Validation** (enforced in service layer):
  - If category has parent: No additional constraint (child budget independent)
  - If category has children: `totalAmount` must equal SUM of all child category budgets for same (year, month)
- Category cannot be deleted if budgets reference it
- Category parent cannot be changed if budgets reference the category

**Indexes**:
- Primary: `id`
- Foreign Key: `category_id` (for join performance)
- Unique (partial): `(category_id, year, month)` WHERE `category_id IS NOT NULL`
- Composite: `(year, month)` for period-based queries

---

## Relationship Diagram

```text
┌─────────────────────────────────────────┐
│ Category                                 │
│ ─────────────────────────────────────── │
│ id (PK)                                  │
│ name (UNIQUE)                            │
│ icon                                     │
│ parent_category_id (FK → categories.id) │◄─┐
│ is_system                                │  │ Self-referencing
│ created_by                               │  │ (parent-child)
│ created_at                               │  │
└─────────────────────────────────────────┘  │
       │                                       │
       │ 1                                     │
       │                                       │
       │                                       │
       │ *                                     │
       ▼                                       │
┌─────────────────────────────────────────┐  │
│ Budget                                   │  │
│ ─────────────────────────────────────── │  │
│ id (PK)                                  │  │
│ category_id (FK → categories.id) NULLABLE├──┘
│ year                                     │
│ month                                    │
│ total_amount                             │
│ description                              │
│ created_by                               │
│ created_at                               │
│ updated_at                               │
│ version                                  │
└─────────────────────────────────────────┘
       │
       │ 1
       │
       │
       │ *
       ▼
┌─────────────────────────────────────────┐
│ Expense (existing entity)                │
│ ─────────────────────────────────────── │
│ id (PK)                                  │
│ budget_id (FK)                           │
│ category_id (FK)                         │
│ amount                                   │
│ date                                     │
│ ...                                      │
└─────────────────────────────────────────┘
```

## Validation Rules

### Category Validation

**Create/Update**:
1. **Name Uniqueness**: `name` must not match any existing category (case-insensitive comparison recommended)
2. **Parent Existence**: If `parentCategoryId` provided, must reference existing category
3. **Circular Prevention**: Proposed parent must not be a descendant of the category being updated
4. **Hierarchy Depth**: If setting parent, verify parent is a root category (parent.parentCategory == null)
5. **System Category Protection**: Cannot set parent for system categories

**Delete**:
1. **Child Check**: Cannot delete if category has children (prompt user to reassign or delete children)
2. **Budget Check**: Cannot delete if category has associated budgets (prompt user to reassign budgets)
3. **Expense Check**: Cannot delete if category has associated expenses (existing validation)
4. **System Category Protection**: Cannot delete system categories

---

### Budget Validation

**Create**:
1. **Category Required**: `categoryId` must be provided (not nullable for new budgets)
2. **Category Existence**: `categoryId` must reference existing category
3. **Amount Positive**: `totalAmount` must be > 0
4. **Period Uniqueness**: No existing budget for same `(category_id, year, month)`
5. **Parent-Child Sum** (if category has children):
   - Calculate SUM of all child category budgets for same (year, month)
   - If sum exists and > 0, `totalAmount` must exactly equal sum
   - If no child budgets exist yet, allow parent budget creation (user will add children later)

**Update**:
1. **Category Immutability** (optional - could allow reassignment with validation):
   - Preferred: Prevent category changes for existing budgets
   - Alternative: Allow change but re-validate uniqueness and parent-child sums
2. **Amount Validation**: Same as create (positive value)
3. **Parent-Child Sum**: Re-validate if category has children or parent:
   - If updating parent budget: Must equal sum of children
   - If updating child budget: Verify parent budget (if exists) equals new sum of all children

---

## State Transitions

### Category Hierarchy States

**State 1: Root Category** (parentCategory = NULL)
- **Can transition to**: No state change (remains root)
- **Cannot transition to**: Child category (once children exist, cannot become child)
- **Allowed operations**: Add children, delete (if no children/budgets), update name/icon

**State 2: Child Category** (parentCategory != NULL, no children)
- **Can transition to**: Root category (set parentCategory = NULL, if no budgets or after budget reassignment)
- **Cannot transition to**: Parent of another category (hierarchy depth limit)
- **Allowed operations**: Change parent (if no budgets), update name/icon, delete (if no budgets)

**State 3: Parent Category with Children** (has children count > 0)
- **Can transition to**: Root category without children (after children removed/reassigned)
- **Cannot transition to**: Child category (cannot have both parent and children)
- **Allowed operations**: Add more children, update name/icon, delete children (individually), update budgets (with sum validation)

---

### Budget Category Association States

**State 1: Legacy Budget** (category = NULL)
- **Created**: Only exists from pre-feature budgets
- **Can transition to**: Categorized budget (assign category via update)
- **Cannot transition to**: N/A (only forward transition allowed)
- **Allowed operations**: View, update amount/description, assign category

**State 2: Categorized Budget** (category != NULL)
- **Created**: All new budgets start in this state
- **Can transition to**: Potentially different category (if reassignment allowed) or deleted
- **Cannot transition to**: Legacy budget (category cannot be removed once set)
- **Allowed operations**: View, update amount (with parent-child validation), update description, delete

---

### Parent-Child Budget Validation States

**State 1: Parent Budget Without Children**
- **Condition**: Category has children but no child category budgets for this period
- **Status**: Valid (warning notification shown to user)
- **Allowed operations**: Add child budgets, update amount, delete parent budget

**State 2: Parent Budget Matching Children Sum**
- **Condition**: Parent `totalAmount` == SUM(child budgets for period)
- **Status**: Valid (fully consistent hierarchy)
- **Allowed operations**: Add more children (must adjust parent), update child amounts (must adjust parent or other children), delete (with cascade consideration)

**State 3: Parent Budget Mismatched Children Sum** (INVALID - prevented)
- **Condition**: Parent `totalAmount` != SUM(child budgets for period)
- **Status**: **Invalid** - blocked by service layer validation
- **Prevention**: Create/update operations rejected with `ParentBudgetMismatchException`
- **Recovery**: User must adjust parent budget or child budgets to match sum

**State 4: Child Budget Without Parent**
- **Condition**: Category has parent but parent category has no budget for this period
- **Status**: Valid (warning notification shown to user)
- **Allowed operations**: Add parent budget (pre-filled with children sum), update child amount, delete child budget

---

## Migration Considerations

### Schema Changes

**Backward Compatible Additions**:
1. Add `parent_category_id` column to `categories` (nullable)
2. Add `category_id` column to `budgets` (nullable for legacy data)
3. Add foreign key constraints with `ON DELETE RESTRICT`
4. Add indexes for query performance
5. Add partial unique index for budget uniqueness

**No Destructive Changes**:
- Existing categories remain root-level (parent_category_id = NULL)
- Existing budgets remain uncategorized (category_id = NULL)
- All existing data remains queryable and functional

### Data Migration

**Phase 1: Schema Deployment**
- Run Liquibase changesets to add columns and constraints
- No data modification required
- Zero downtime deployment

**Phase 2: Optional Data Migration** (user-driven)
- Users can manually assign categories to legacy budgets
- Users can create parent-child category relationships
- System provides UI tools for bulk category assignment

---

## Query Patterns

### Common Queries

**1. Get Category Hierarchy (all root categories with children)**:
```sql
-- Root categories
SELECT * FROM categories WHERE parent_category_id IS NULL ORDER BY name;

-- Children for specific parent
SELECT * FROM categories WHERE parent_category_id = ? ORDER BY name;
```

**2. Get All Budgets for Month with Category Info**:
```sql
SELECT b.*, c.name as category_name, c.parent_category_id
FROM budgets b
LEFT JOIN categories c ON b.category_id = c.id
WHERE b.year = ? AND b.month = ?
ORDER BY c.parent_category_id NULLS FIRST, c.name;
```

**3. Calculate Parent Budget Sum for Validation**:
```sql
SELECT SUM(b.total_amount)
FROM budgets b
JOIN categories c ON b.category_id = c.id
WHERE c.parent_category_id = ?
  AND b.year = ?
  AND b.month = ?;
```

**4. Check Category Has Children**:
```sql
SELECT COUNT(*) FROM categories WHERE parent_category_id = ?;
```

**5. Check Category Has Budgets**:
```sql
SELECT COUNT(*) FROM budgets WHERE category_id = ?;
```

---

## Summary

Data model extends existing Category and Budget entities with minimal schema changes:
- **Category**: Add self-referencing parent foreign key
- **Budget**: Add category foreign key (nullable for backward compatibility)
- **Validation**: Enforce business rules in service layer, not database
- **Migration**: Zero-downtime additive schema changes
- **Queries**: Simple joins and aggregations, no complex recursion needed

All entities, relationships, and validation rules fully defined. Ready for contract generation.
