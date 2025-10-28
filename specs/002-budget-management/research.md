# Research: Budget and Expense Management

**Feature**: 002-budget-management
**Date**: 2025-10-23
**Purpose**: Document technical decisions, patterns, and best practices for implementing budget tracking

## Overview

This document captures research decisions for implementing a household budget and expense tracking system using Next.js 14 frontend and Spring Boot 3.2 backend (established in Feature 001).

## Technical Decisions

### TD-001: JPA Entity Relationships for Budget-Expense-Category

**Decision**: Use JPA `@OneToMany` and `@ManyToOne` bidirectional relationships with cascade operations

**Rationale**:
- Budget → Expenses: One-to-Many (one budget has many expenses)
- Category → Expenses: One-to-Many (one category has many expenses)
- Bidirectional mapping enables efficient queries in both directions (find expenses for budget, find budget for expense)
- `CascadeType.ALL` on Budget→Expenses enables cascade delete when user confirms (FR-022)
- Fetch strategy: `LAZY` for collections to avoid N+1 queries, use JOIN FETCH in repositories when needed

**Alternatives Considered**:
- Unidirectional relationships: Rejected - would require manual queries for reverse lookups
- Embedded objects: Rejected - Budget and Expense are distinct entities with independent lifecycles
- Document database (MongoDB): Rejected - constitution mandates MySQL, relational model fits budget domain well

**Implementation Notes**:
```java
// Budget.java
@OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<Expense> expenses = new ArrayList<>();

// Expense.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "budget_id", nullable = false)
private Budget budget;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
private Category category;
```

---

### TD-002: Month Representation in Budget Entity

**Decision**: Use separate `year` (Integer) and `month` (Integer 1-12) fields with unique constraint

**Rationale**:
- Simple querying: Easy to filter by year, month, or range
- Database-friendly: Integer columns are efficient for indexing and sorting
- Validation: `@Min(1) @Max(12)` on month, `@Min(2000)` on year
- Unique constraint prevents duplicate budgets per month (FR-002)
- No timezone complexity unlike Date/LocalDate

**Alternatives Considered**:
- `LocalDate` (first day of month): Rejected - adds unnecessary day component, more complex queries
- `YearMonth` class: Rejected - not directly supported by JPA, requires AttributeConverter
- String "YYYY-MM": Rejected - loses type safety, harder to query ranges

**Implementation Notes**:
```java
@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}))
public class Budget {
    @Min(2000) @Max(9999)
    @Column(nullable = false)
    private Integer year;

    @Min(1) @Max(12)
    @Column(nullable = false)
    private Integer month;
}
```

---

### TD-003: Calculated Fields (Total Spending, Percentage) Strategy

**Decision**: Compute on-demand in service layer, do not store in database

**Rationale**:
- Data integrity: Eliminates risk of calculated fields becoming stale
- Simpler schema: No need for triggers or update cascades
- Acceptable performance: SUM aggregation on indexed budget_id is fast (<2 seconds for 200 expenses per SC-007)
- Accurate for FR requirement "zero calculation errors" (SC-008)

**Alternatives Considered**:
- Store total_spending column: Rejected - requires triggers/app logic to keep in sync, violates single source of truth
- Materialized views: Rejected - adds database complexity, MySQL support limited
- Caching with Redis: Rejected - premature optimization for household use (typically <100 budgets)

**Implementation Notes**:
```java
// BudgetService.java
public BigDecimal calculateTotalSpending(Long budgetId) {
    return expenseRepository.sumAmountByBudgetId(budgetId);
}

public BigDecimal calculateSpendingPercentage(Budget budget) {
    BigDecimal totalSpending = calculateTotalSpending(budget.getId());
    return totalSpending.divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100));
}
```

---

### TD-004: User Identity Tracking (X-Hass-User Integration)

**Decision**: Store username string from X-Hass-User header in Budget/Expense `createdBy` field

**Rationale**:
- Constitution requirement: User identity from X-Hass-User header
- Simple implementation: No User table needed, header provides username directly
- Audit trail: Satisfies FR-006 "associate expense with user who recorded it"
- Denormalized by design: Username changes are rare in home environment
- Leverages existing `HassUserHeaderFilter` from Feature 001

**Alternatives Considered**:
- Separate User table with foreign keys: Rejected - overengineering for household with static users
- Spring Security principal: Rejected - unnecessary auth layer when Home Assistant provides auth
- User ID instead of username: Rejected - username is more readable in UI (FR-017)

**Implementation Notes**:
```java
// Budget.java, Expense.java
@Column(name = "created_by", nullable = false, length = 100)
private String createdBy;

// BudgetService.java
public Budget createBudget(BudgetDTO dto, String username) {
    Budget budget = new Budget();
    budget.setCreatedBy(username); // from X-Hass-User header
    // ...
}
```

---

### TD-005: Category Icon Storage

**Decision**: Store as VARCHAR(10) to support Unicode emoji characters

**Rationale**:
- Simple UX: Users input emoji directly (🛒, 🚗, 🏠)
- Database efficiency: VARCHAR(10) sufficient for multi-byte emoji sequences
- No external dependencies: No icon font library required
- Cross-platform: Emojis render consistently across modern browsers and devices

**Alternatives Considered**:
- Icon font class names (Material Icons): Rejected - adds frontend dependency, less intuitive for users
- Image URLs: Rejected - requires asset management, slower loading
- SVG strings: Rejected - overcomplicates simple categorization feature

**Implementation Notes**:
```java
@Column(length = 10)
private String icon;  // Stores emoji like "🛒" or "🏠"
```

---

### TD-006: Concurrent Access and Optimistic Locking

**Decision**: Use JPA `@Version` for optimistic locking on Budget and Expense entities

**Rationale**:
- FR-016 requires concurrent access support
- Optimistic locking prevents lost updates when multiple household members edit simultaneously
- Better performance than pessimistic locks for read-heavy workload (viewing budgets)
- Graceful handling: Return 409 Conflict if version mismatch, prompt user to refresh

**Alternatives Considered**:
- Pessimistic locking: Rejected - unnecessary overhead, household editing conflicts are rare
- No locking: Rejected - violates FR-016, risks data loss
- Database transactions only: Rejected - insufficient for long-running user sessions

**Implementation Notes**:
```java
@Entity
public class Budget {
    @Version
    private Long version;
}

// Service layer catches OptimisticLockException, returns conflict error
```

---

### TD-007: Default "Uncategorized" Category Strategy

**Decision**: Seed database with default category via Liquibase migration, reference by special ID or name constant

**Rationale**:
- FR-010 requires default "Uncategorized" category
- Consistent across all households
- Prevents deletion (FK constraints from expenses)
- No application logic to create on-the-fly

**Alternatives Considered**:
- Null category on Expense: Rejected - complicates queries, no FK constraint
- Create dynamically on first expense: Rejected - race conditions in concurrent access
- Hardcoded category ID = 1: Rejected - fragile if migration order changes

**Implementation Notes**:
```xml
<!-- 004-create-categories-table.xml -->
<insert tableName="categories">
    <column name="id" value="1"/>
    <column name="name" value="Uncategorized"/>
    <column name="icon" value="📦"/>
    <column name="created_by" value="system"/>
    <column name="is_system" valueBoolean="true"/>
</insert>
```

---

### TD-008: Expense Date Validation and Budget Month Mismatch

**Decision**: Allow expenses with any valid date, display warning in UI if date outside budget month

**Rationale**:
- Flexibility: Users may enter expenses late (e.g., forgot to log yesterday's purchase)
- FR-018: Warn, don't block - user knows best when expense occurred
- Business logic separation: Validation in service, warning display in frontend

**Alternatives Considered**:
- Strict validation (block mismatches): Rejected - too restrictive, frustrates users
- Auto-correct date to budget month: Rejected - data integrity issue, loses actual expense date
- Ignore date entirely: Rejected - FR-005 requires date field for filtering

**Implementation Notes**:
```java
// ExpenseService.java
public ExpenseDTO createExpense(ExpenseDTO dto, Long budgetId) {
    Budget budget = budgetRepository.findById(budgetId)...;

    boolean dateOutsideMonth = !isDateInBudgetMonth(dto.getDate(), budget);

    ExpenseDTO response = // ... create expense
    response.setWarning(dateOutsideMonth ? "Expense date outside budget month" : null);

    return response;
}
```

---

### TD-009: Category Deletion with Associated Expenses

**Decision**: Implement soft delete check - return 409 Conflict if category has expenses, suggest reassignment

**Rationale**:
- FR-011: Prevent deletion of categories with expenses
- Data integrity: Avoid orphaned expenses or cascading deletes
- User guidance: Error message suggests reassigning expenses first
- Future enhancement: Could add "reassign and delete" workflow

**Alternatives Considered**:
- Cascade delete expenses: Rejected - data loss, violates user expectations
- Soft delete (flag): Rejected - adds complexity, clutters category list
- Automatic reassign to "Uncategorized": Rejected - user may want different category

**Implementation Notes**:
```java
// CategoryService.java
public void deleteCategory(Long categoryId) {
    long expenseCount = expenseRepository.countByCategoryId(categoryId);

    if (expenseCount > 0) {
        throw new CategoryInUseException(
            "Cannot delete category with " + expenseCount + " expenses. " +
            "Please reassign expenses to another category first."
        );
    }

    categoryRepository.deleteById(categoryId);
}
```

---

### TD-010: Frontend State Management

**Decision**: Use React hooks (useState, useEffect) with API service layer, no global state library

**Rationale**:
- Simple use case: Budget data is page-scoped, no complex shared state
- Next.js server components: Can fetch data server-side for initial render
- Performance: Sufficient for household use (<100 budgets)
- Alignment with Next.js 14 App Router patterns

**Alternatives Considered**:
- Redux/Zustand: Rejected - overengineering for page-based navigation
- React Context: Rejected - unnecessary for non-shared data
- SWR/React Query: Considered for future if caching needed, deferred for MVP

**Implementation Notes**:
```typescript
// budgets/page.tsx
'use client';
export default function BudgetsPage() {
    const [budgets, setBudgets] = useState<Budget[]>([]);

    useEffect(() => {
        budgetService.getAllBudgets().then(setBudgets);
    }, []);

    // render budget list
}
```

---

### TD-011: Material-UI Form Validation

**Decision**: Use React Hook Form with Material-UI integration for client-side validation

**Rationale**:
- Type-safe: Works well with TypeScript
- Built-in validation rules (required, min, max, pattern)
- Error handling: Integrates with Material-UI TextField error display
- Performance: Uncontrolled components reduce re-renders

**Alternatives Considered**:
- Formik: Rejected - heavier bundle size, React Hook Form is lighter
- Manual validation: Rejected - reinvents wheel, error-prone
- Server-side only: Rejected - poor UX, unnecessary round trips

**Implementation Notes**:
```typescript
import { useForm } from 'react-hook-form';

const { register, handleSubmit, formState: { errors } } = useForm<BudgetForm>();

<TextField
    {...register('totalAmount', {
        required: 'Amount is required',
        min: { value: 0.01, message: 'Amount must be positive' }
    })}
    error={!!errors.totalAmount}
    helperText={errors.totalAmount?.message}
/>
```

---

## Best Practices Applied

### Spring Boot Backend

1. **DTO Pattern**: Separate entity models from API representations to decouple database schema from API contracts
2. **Service Layer**: Business logic in services, controllers remain thin (validation, mapping, HTTP concerns only)
3. **Repository Pattern**: Spring Data JPA repositories for data access abstraction
4. **Exception Handling**: Global `@ControllerAdvice` for consistent error responses
5. **Validation**: Bean Validation (`@NotNull`, `@Min`, `@Max`) on DTOs and entities
6. **Liquibase**: Database migrations in version-controlled XML changesets

### Next.js Frontend

1. **Component Composition**: Small, reusable components (BudgetCard, ExpenseList, CategoryBadge)
2. **Type Safety**: TypeScript interfaces for all API responses and component props
3. **API Service Layer**: Centralized axios clients in `services/` directory
4. **Error Boundaries**: Try-catch in async operations, display user-friendly error messages
5. **Loading States**: Display skeleton loaders or spinners during data fetching
6. **Accessibility**: Proper ARIA labels, semantic HTML, keyboard navigation

### Data Modeling

1. **Normalization**: Proper 3NF design - no redundant data except denormalized username for simplicity
2. **Indexes**: Index foreign keys (budget_id, category_id) for query performance
3. **Constraints**: Database-level constraints for data integrity (unique, not null, foreign keys)
4. **Audit Fields**: created_at, updated_at timestamps on all entities
5. **Soft Deletes**: Not used - hard deletes with FK constraints and business validation

## Performance Considerations

1. **Database Queries**:
   - Use `JOIN FETCH` for Budget with expenses to avoid N+1 queries
   - Index on `(year, month)` for budget lookups
   - Index on `budget_id` and `category_id` for expense queries

2. **Frontend Optimization**:
   - Lazy load dashboard charts (P4 feature)
   - Pagination for expense lists if >50 items (deferred, not in MVP)
   - Debounce search/filter inputs (if implemented)

3. **Caching Strategy**:
   - Not needed for MVP - household workload is light
   - Future: Consider HTTP caching headers (ETag, Cache-Control) if needed

## Security Considerations

1. **Authentication**: X-Hass-User header from Home Assistant nginx proxy (per constitution)
2. **Authorization**: All household members have equal access (shared budget data per constitution)
3. **Input Validation**: Server-side validation on all endpoints (don't trust client)
4. **SQL Injection**: Mitigated by JPA parameterized queries
5. **XSS**: React/Next.js escapes by default, no `dangerouslySetInnerHTML` used

## Testing Strategy (Optional per Principle VI)

Manual testing via acceptance scenarios defined in spec.md. Each user story has 4 acceptance scenarios that serve as test cases. No automated tests for MVP per Constitution Check decision.

**Future Testing Considerations** (if added later):
- Backend: JUnit 5 tests with H2 in-memory database, MockMvc for controller tests
- Frontend: Jest for component unit tests, React Testing Library for integration tests
- E2E: Playwright for critical user journeys (deferred)

## Migration from Feature 001

This feature builds on the development environment from Feature 001:
- ✅ Docker Compose setup (MySQL, backend, frontend)
- ✅ Spring Boot application structure
- ✅ Next.js 14 with App Router
- ✅ Liquibase migration framework
- ✅ HassUserHeaderFilter for authentication
- ✅ CORS configuration
- ✅ Material-UI theme

**New additions for Feature 002**:
- JPA entity models (Budget, Expense, Category)
- Spring Data JPA repositories
- Service layer with business logic
- REST API controllers
- Liquibase migrations for new tables
- Frontend pages and components for budget management
- API service clients (budgetService.ts, expenseService.ts, categoryService.ts)

## References

- [Spring Data JPA Best Practices](https://docs.spring.io/spring-data/jpa/reference/jpa.html)
- [Next.js 14 App Router Documentation](https://nextjs.org/docs/app)
- [Material-UI Forms](https://mui.com/material-ui/react-text-field/)
- [React Hook Form](https://react-hook-form.com/)
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)
- Feature 001: Project Scaffolding (specs/001-project-scaffolding/)
