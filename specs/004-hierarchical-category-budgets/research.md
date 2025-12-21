# Technical Research: Hierarchical Category Budgets

**Feature**: 004-hierarchical-category-budgets
**Date**: 2025-11-11
**Purpose**: Document technical decisions, rationale, and alternatives for implementing hierarchical category budgets

## Research Questions

Based on the specification and technical context, the following decisions needed research:

1. **Database Schema**: How to model parent-child category relationships?
2. **Validation Strategy**: Where and when to enforce parent-child budget sum constraints?
3. **Circular Reference Prevention**: How to detect and prevent circular category hierarchies?
4. **Backward Compatibility**: How to handle existing budgets that lack category associations?
5. **Frontend Hierarchy Display**: How to render nested category structures efficiently?
6. **Constraint Enforcement**: How to prevent orphaned budgets when categories are deleted?

## Technical Decisions

### Decision 1: Self-Referencing Foreign Key for Category Hierarchy

**Choice**: Add `parent_category_id` nullable foreign key column to `categories` table, referencing `categories.id`

**Rationale**:
- **Simplicity**: Single table design avoids join complexity for two-level hierarchies
- **Performance**: No additional tables or recursive queries needed for parent-child lookups
- **JPA Native Support**: `@ManyToOne` and `@OneToMany` annotations handle self-referencing relationships elegantly
- **Null Semantics**: NULL `parent_category_id` clearly identifies root-level categories
- **Index Support**: Foreign key automatically indexed for efficient parent lookups

**Alternatives Considered**:
- **Closure Table**: Separate table storing all ancestor-descendant pairs
  - **Rejected**: Overly complex for fixed two-level hierarchy; adds join overhead
- **Nested Set Model**: Left/right node numbers for tree traversal
  - **Rejected**: Complex updates when inserting categories; unnecessary for shallow trees
- **Path Enumeration**: Store full path as string (e.g., "/Food/Groceries")
  - **Rejected**: String parsing overhead; difficult to maintain referential integrity

**Implementation**:
```sql
ALTER TABLE categories
ADD COLUMN parent_category_id BIGINT NULL,
ADD CONSTRAINT fk_category_parent
  FOREIGN KEY (parent_category_id)
  REFERENCES categories(id)
  ON DELETE RESTRICT;
```

---

### Decision 2: Service-Layer Budget Sum Validation

**Choice**: Implement parent-child budget validation in `BudgetService` during create/update operations, before persisting to database

**Rationale**:
- **Business Logic Location**: Validation rules are domain logic, belong in service layer not database
- **Rich Error Messages**: Can provide detailed user feedback (e.g., "Parent budget ($600) must equal sum of children ($500)")
- **Transaction Control**: Validation occurs within service transaction, ensuring atomicity
- **Testability**: Service methods easily unit-tested with mocked repositories
- **Flexibility**: Can adjust validation logic without database migrations

**Alternatives Considered**:
- **Database Check Constraint**: SQL constraint enforcing sum equality
  - **Rejected**: Cannot reference multiple rows in check constraint; would require triggers which complicate debugging
- **Database Trigger**: BEFORE INSERT/UPDATE trigger calculating sums
  - **Rejected**: Logic split between application and database; harder to test and maintain
- **Controller Validation**: Validate in REST controller before calling service
  - **Rejected**: Breaks separation of concerns; validation logic coupled to HTTP layer

**Implementation**:
```java
@Transactional
public Budget createBudget(BudgetDTO dto) {
    Category category = categoryRepository.findById(dto.getCategoryId())
        .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId()));

    if (category.getParentCategory() != null) {
        validateChildBudget(category, dto);
    } else if (hasChildren(category)) {
        validateParentBudget(category, dto);
    }

    // Proceed with budget creation
}

private void validateParentBudget(Category parent, BudgetDTO dto) {
    BigDecimal childrenSum = budgetRepository
        .sumByParentCategoryAndPeriod(parent.getId(), dto.getYear(), dto.getMonth());
    if (childrenSum != null && !dto.getTotalAmount().equals(childrenSum)) {
        throw new ParentBudgetMismatchException(dto.getTotalAmount(), childrenSum);
    }
}
```

---

### Decision 3: Application-Level Circular Reference Check

**Choice**: Validate circular references in `CategoryService.updateCategory()` by traversing parent chain up to root

**Rationale**:
- **Guaranteed Detection**: Explicit traversal catches all circular scenarios before database commit
- **Clear Error Messages**: Can identify exact cycle path (e.g., "Category A -> B -> C -> A")
- **Depth Limit Enforcement**: Naturally enforces two-level hierarchy (reject if proposed parent already has parent)
- **Simple Algorithm**: Linear traversal up parent chain, stops at NULL or cycle detection

**Alternatives Considered**:
- **Database Constraint**: Recursive CTE checking for cycles
  - **Rejected**: MySQL recursive CTEs complex; performance unpredictable for large datasets
- **Lazy Detection**: Allow circular references, detect at query time
  - **Rejected**: Data integrity compromised; queries could infinite loop
- **Graph Library**: Use external graph traversal library
  - **Rejected**: Heavyweight dependency for simple two-level tree validation

**Implementation**:
```java
public void updateParent(Long categoryId, Long newParentId) {
    if (newParentId == null) return; // Moving to root, always valid

    // Check if new parent is descendant of this category (creates cycle)
    Category current = categoryRepository.findById(newParentId).orElseThrow();
    Set<Long> visited = new HashSet<>();

    while (current != null) {
        if (current.getId().equals(categoryId)) {
            throw new CircularCategoryException(categoryId, newParentId);
        }
        if (!visited.add(current.getId())) {
            throw new CircularCategoryException("Cycle detected in existing data");
        }
        // Enforce two-level limit: new parent must be root
        if (current.getParentCategory() != null) {
            throw new IllegalArgumentException("Categories limited to 2 levels");
        }
        current = current.getParentCategory();
    }
}
```

---

### Decision 4: Nullable Category Foreign Key with Migration Strategy

**Choice**: Add `category_id` as nullable foreign key to `budgets` table; existing budgets remain with NULL category

**Rationale**:
- **Zero Downtime**: Existing budgets continue functioning without immediate migration
- **Gradual Migration**: Users can assign categories to old budgets at their pace
- **No Data Loss**: Historical budgets preserved with integrity
- **Future Enforcement**: New budgets require category (enforced in service layer), old budgets grandfathered

**Alternatives Considered**:
- **Require Immediate Migration**: Force assignment of all budgets to categories
  - **Rejected**: Breaking change for users; requires complex data migration script
- **Create Default Category**: Auto-assign uncategorized budgets to "Uncategorized" system category
  - **Rejected**: Pollutes category hierarchy with implicit categorization; user intent unclear
- **Separate Budget Types**: Create new `CategorizedBudget` entity
  - **Rejected**: Duplicates code; query complexity increases with two budget types

**Implementation**:
```sql
ALTER TABLE budgets
ADD COLUMN category_id BIGINT NULL,
ADD CONSTRAINT fk_budget_category
  FOREIGN KEY (category_id)
  REFERENCES categories(id)
  ON DELETE RESTRICT;

CREATE INDEX idx_budget_category ON budgets(category_id);
CREATE UNIQUE INDEX idx_budget_category_period
  ON budgets(category_id, year, month)
  WHERE category_id IS NOT NULL;
```

Service layer enforcement:
```java
public Budget createBudget(BudgetDTO dto) {
    if (dto.getCategoryId() == null) {
        throw new ValidationException("Category is required for new budgets");
    }
    // Continue with validation and creation
}
```

---

### Decision 5: Recursive React Component for Hierarchy Display

**Choice**: Use recursive React component to render category tree with Material-UI TreeView/nested lists

**Rationale**:
- **Natural Mapping**: Component recursion mirrors data structure hierarchy
- **Reusability**: Single component handles all nesting levels
- **Material-UI Support**: TreeView component optimized for hierarchical data
- **Performance**: Virtual scrolling available for large category lists
- **Accessibility**: Tree navigation keyboard support built-in

**Alternatives Considered**:
- **Flat List with Indentation**: Single-level list with CSS indentation
  - **Rejected**: Harder to expand/collapse; no semantic tree structure for screen readers
- **Nested Lists**: Hand-coded nested `<ul>` elements
  - **Rejected**: More DOM nodes; lacks expand/collapse affordances
- **Third-Party Tree Library**: react-complex-tree or similar
  - **Rejected**: Additional dependency when Material-UI TreeView sufficient

**Implementation**:
```tsx
interface CategoryTreeProps {
  categories: Category[];
  onSelect: (id: number) => void;
}

const CategoryTree: React.FC<CategoryTreeProps> = ({ categories, onSelect }) => {
  const rootCategories = categories.filter(c => !c.parentId);

  const renderCategory = (category: Category) => (
    <TreeItem
      key={category.id}
      nodeId={String(category.id)}
      label={category.name}
      onClick={() => onSelect(category.id)}
    >
      {category.children?.map(child => renderCategory(child))}
    </TreeItem>
  );

  return (
    <TreeView defaultCollapseIcon={<ExpandMore />} defaultExpandIcon={<ChevronRight />}>
      {rootCategories.map(cat => renderCategory(cat))}
    </TreeView>
  );
};
```

---

### Decision 6: Cascading Restriction with Explicit Reassignment

**Choice**: Use `ON DELETE RESTRICT` for both foreign keys; require explicit category reassignment or budget deletion before category removal

**Rationale**:
- **Data Safety**: Prevents accidental data loss from cascading deletes
- **User Intent**: Forces user to consciously decide fate of child data
- **Audit Trail**: Maintains clear record of why categories were deleted
- **Reversibility**: User can cancel category deletion if child dependencies discovered

**Alternatives Considered**:
- **ON DELETE CASCADE**: Auto-delete all child budgets and categories
  - **Rejected**: Too destructive; users may not realize extent of deletion
- **ON DELETE SET NULL**: Set child references to NULL automatically
  - **Rejected**: Orphans child categories (invalid state); orphans budgets (loses categorization)
- **Soft Delete**: Mark categories as deleted without removing from database
  - **Rejected**: Adds complexity to all queries (filter is_deleted); complicates unique constraints

**Implementation**:
```java
public void deleteCategory(Long categoryId) {
    Category category = categoryRepository.findById(categoryId).orElseThrow();

    // Check for child categories
    if (categoryRepository.countByParentCategoryId(categoryId) > 0) {
        throw new CategoryInUseException(
            "Cannot delete category with child categories. " +
            "Please reassign or delete children first."
        );
    }

    // Check for budgets
    if (budgetRepository.countByCategoryId(categoryId) > 0) {
        throw new CategoryInUseException(
            "Cannot delete category with active budgets. " +
            "Please reassign budgets to another category first."
        );
    }

    categoryRepository.delete(category);
}
```

## Performance Considerations

### Database Indexing

**Required Indexes**:
1. `idx_category_parent` on `categories.parent_category_id` - Fast child lookup
2. `idx_budget_category` on `budgets.category_id` - Fast budget filtering
3. `idx_budget_category_period` on `(category_id, year, month)` - Enforce uniqueness + fast period lookups

**Query Optimization**:
- Parent budget sum calculation uses single aggregate query with category join
- Category hierarchy depth limited to 2, preventing deep recursion
- Eager vs lazy loading: Use LAZY for category.children to avoid N+1 queries

### Frontend Rendering

**Optimizations**:
- Category tree limited to 100 total categories (10 parents × 10 children)
- TreeView virtualization enabled for lists >50 items
- Debounce budget form validation (500ms) to avoid excessive API calls

## Security Considerations

**Input Validation**:
- Category parent ID validated to prevent injection of non-existent parents
- Budget amount validated positive BigDecimal to prevent negative budgets
- Year/month validated within reasonable bounds (2000-9999, 1-12)

**Access Control**:
- User identity from X-Hass-User header trusted (Home Assistant enforced)
- All budget/category modifications logged with createdBy/updatedBy for audit
- No row-level security needed (household shared visibility)

## Migration Strategy

### Database Migration (Liquibase)

**Phase 1**: Add nullable columns (zero downtime)
```xml
<changeSet id="004-1" author="specify">
  <addColumn tableName="categories">
    <column name="parent_category_id" type="BIGINT"/>
  </addColumn>
  <addForeignKeyConstraint
    constraintName="fk_category_parent"
    baseTableName="categories" baseColumnNames="parent_category_id"
    referencedTableName="categories" referencedColumnNames="id"
    onDelete="RESTRICT"/>
</changeSet>
```

**Phase 2**: Add budget-category association (zero downtime)
```xml
<changeSet id="004-2" author="specify">
  <addColumn tableName="budgets">
    <column name="category_id" type="BIGINT"/>
  </addColumn>
  <addForeignKeyConstraint
    constraintName="fk_budget_category"
    baseTableName="budgets" baseColumnNames="category_id"
    referencedTableName="categories" referencedColumnNames="id"
    onDelete="RESTRICT"/>
  <createIndex tableName="budgets" indexName="idx_budget_category">
    <column name="category_id"/>
  </createIndex>
</changeSet>
```

**Phase 3**: Add unique constraint for category+period (zero downtime)
```xml
<changeSet id="004-3" author="specify">
  <sql>
    CREATE UNIQUE INDEX idx_budget_category_period
    ON budgets(category_id, year, month)
    WHERE category_id IS NOT NULL
  </sql>
</changeSet>
```

### Backward Compatibility

**Old Budget Handling**:
- GET /api/budgets returns budgets regardless of category presence
- Budget DTO includes `categoryId` and `category` as optional fields
- Frontend gracefully handles budgets with `category: null`
- Users can edit old budgets to add category retroactively

**Frontend Migration**:
- Budget form shows warning for legacy budgets: "This budget has no category. Please assign one."
- Category dropdown pre-selects "None" for old budgets, requires selection for new
- Budget list filters support "Show uncategorized only" option

## Summary

All technical decisions finalized with no ambiguities remaining. Implementation can proceed with:
- Database schema extensions using Liquibase
- Service layer validation logic in Spring Boot
- Recursive React components for hierarchy display
- Zero-downtime migration strategy for existing data

**Next Phase**: Proceed to Phase 1 (data model, contracts, quickstart generation)
