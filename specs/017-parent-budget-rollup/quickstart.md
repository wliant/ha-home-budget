# Quickstart: Parent Category Budget Auto-Rollup

**Feature**: 017-parent-budget-rollup
**Date**: 2026-02-15
**Phase**: Phase 1 (Integration Scenarios)

## Overview

This document provides step-by-step integration test scenarios to validate the parent category budget auto-rollup feature. These scenarios cover all user stories and functional requirements from the specification.

## Prerequisites

- Backend running: `cd budget-backend && ./mvnw spring-boot:run`
- Frontend running: `cd budget-frontend && npm run dev`
- Database populated with category hierarchy:
  ```sql
  -- Parent categories
  INSERT INTO categories (id, name, icon, parent_category_id) VALUES
  (1, 'Groceries', '🛒', NULL),
  (5, 'Transportation', '🚗', NULL),
  (10, 'Entertainment', '🎬', NULL);

  -- Child categories
  INSERT INTO categories (id, name, icon, parent_category_id) VALUES
  (2, 'Fresh Produce', '🥦', 1),
  (3, 'Pantry', '🥫', 1),
  (4, 'Frozen', '🧊', 1),
  (6, 'Car Maintenance', '🔧', 5),
  (7, 'Public Transit', '🚌', 5);
  ```

## Test Scenario 1: Automatic Parent Budget Creation (US1 - P1)

**Goal**: Verify that creating a child category budget automatically creates the parent category budget.

### Steps

1. **Navigate to Budget Creation Page**
   - Open browser: http://localhost:3000/budgets/new
   - Login via Home Assistant (X-Hass-User header set by nginx)

2. **Create First Child Budget**
   - Select category: **Fresh Produce** (child of Groceries)
   - Select period: **January 2026** (monthly)
   - Enter amount: **$500**
   - Enter description: "Monthly grocery budget"
   - Click **Create Budget**

3. **Verify Child Budget Created**
   - Backend logs should show: `Created budget for Fresh Produce: $500, January 2026`
   - Frontend redirects to budget list
   - Budget list shows: **Fresh Produce - January 2026 - $500**

4. **Verify Parent Budget Auto-Created**
   - Navigate to budget list: http://localhost:3000/budgets
   - Filter by category: **Groceries** (parent)
   - Filter by period: **January 2026**
   - **Expected**: Parent budget exists with amount **$500**

5. **Verify Database State**
   ```sql
   SELECT b.id, c.name, b.year, b.month, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE b.year = 2026 AND b.month = 1
     AND c.id IN (1, 2);  -- Groceries (1), Fresh Produce (2)

   -- Expected Results:
   -- | id | name          | year | month | amount |
   -- |----|---------------|------|-------|--------|
   -- | 1  | Fresh Produce | 2026 | 1     | 500.00 |
   -- | 2  | Groceries     | 2026 | 1     | 500.00 | ← AUTO-CREATED
   ```

### Acceptance Criteria

- ✅ Child budget created with user-provided amount
- ✅ Parent budget auto-created for same period (year, month)
- ✅ Parent budget amount equals child budget amount ($500)
- ✅ Both budgets visible in frontend budget list

---

## Test Scenario 2: Parent Budget Amount Aggregation (US1 - P1)

**Goal**: Verify that creating additional child budgets increases the parent budget amount.

### Steps

1. **Create Second Child Budget**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Pantry** (child of Groceries)
   - Select period: **January 2026** (same as scenario 1)
   - Enter amount: **$300**
   - Click **Create Budget**

2. **Verify Parent Budget Updated**
   - Navigate to budget list, filter: **Groceries - January 2026**
   - **Expected amount**: **$800** ($500 from Fresh Produce + $300 from Pantry)

3. **Create Third Child Budget**
   - Select category: **Frozen** (child of Groceries)
   - Select period: **January 2026**
   - Enter amount: **$200**
   - Click **Create Budget**

4. **Verify Final Parent Budget Amount**
   - Budget list, filter: **Groceries - January 2026**
   - **Expected amount**: **$1000** ($500 + $300 + $200)

5. **Verify Database Aggregation**
   ```sql
   SELECT c.name, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE b.year = 2026 AND b.month = 1
     AND c.id IN (1, 2, 3, 4)
   ORDER BY c.id;

   -- Expected Results:
   -- | name          | amount  |
   -- |---------------|---------|
   -- | Groceries     | 1000.00 | ← Parent (sum of children)
   -- | Fresh Produce | 500.00  |
   -- | Pantry        | 300.00  |
   -- | Frozen        | 200.00  |
   ```

### Acceptance Criteria

- ✅ Multiple child budgets can be created for same parent
- ✅ Parent budget amount = sum of all children's amounts
- ✅ Each child budget operation triggers parent update in same transaction

---

## Test Scenario 3: Yearly Budget Creation (US1 - P1)

**Goal**: Verify that yearly budgets (month=null) cascade to parent yearly budgets separately from monthly budgets.

### Steps

1. **Create Yearly Child Budget**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Car Maintenance** (child of Transportation)
   - Select period: **Yearly** (deselect month checkbox)
   - Enter year: **2026**
   - Enter amount: **$10,000**
   - Click **Create Budget**

2. **Verify Parent Yearly Budget Auto-Created**
   - Budget list, filter: **Transportation - 2026 (Yearly)**
   - **Expected amount**: **$10,000**

3. **Create Monthly Child Budget for Same Category**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Car Maintenance**
   - Select period: **January 2026** (monthly)
   - Enter amount: **$500**
   - Click **Create Budget**

4. **Verify Separate Parent Monthly Budget Created**
   - Budget list, filter: **Transportation - January 2026**
   - **Expected amount**: **$500**

5. **Verify Both Parent Budgets Exist Independently**
   ```sql
   SELECT c.name, b.year, b.month, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE c.id = 5  -- Transportation (parent)
     AND b.year = 2026;

   -- Expected Results:
   -- | name           | year | month | amount   |
   -- |----------------|------|-------|----------|
   -- | Transportation | 2026 | NULL  | 10000.00 | ← Yearly budget
   -- | Transportation | 2026 | 1     | 500.00   | ← January monthly budget
   ```

### Acceptance Criteria

- ✅ Yearly budgets (month=null) cascade to parent yearly budgets
- ✅ Monthly budgets cascade to parent monthly budgets
- ✅ Yearly and monthly parent budgets maintained separately (no cross-period aggregation)

---

## Test Scenario 4: Correct Total Budget Calculation (US2 - P1)

**Goal**: Verify that the yearly budget view sums only parent category budgets (not child budgets) to avoid double-counting.

### Steps

1. **Setup: Create Budget Hierarchy**
   - **Groceries** (parent): $1000 (auto-calculated from children)
     - Fresh Produce (child): $500
     - Pantry (child): $500
   - **Transportation** (parent): $2000 (auto-calculated)
     - Car Maintenance (child): $1500
     - Public Transit (child): $500
   - **Entertainment** (standalone, no children): $500

2. **Navigate to Yearly Budget View**
   - Open: http://localhost:3000/budgets?year=2026
   - Select year: **2026**

3. **Verify Budget List Excludes Children**
   - **Expected budgets shown**:
     - Groceries: $1000
     - Transportation: $2000
     - Entertainment: $500
   - **NOT shown**: Fresh Produce, Pantry, Car Maintenance, Public Transit

4. **Verify Total Budget Calculation**
   - Total budget displayed: **$3500**
   - **Correct calculation**: $1000 (Groceries) + $2000 (Transportation) + $500 (Entertainment)
   - **Incorrect (avoided)**: $1000 + $500 + $500 + $2000 + $1500 + $500 + $500 = $6500 (double-counts)

5. **Verify API Response**
   ```bash
   curl -H "X-Hass-User: john" http://localhost:8080/api/budgets/yearly-view?year=2026
   ```

   **Expected JSON**:
   ```json
   {
     "year": 2026,
     "totalBudget": 3500.00,
     "budgets": [
       { "categoryName": "Groceries", "amount": 1000.00 },
       { "categoryName": "Transportation", "amount": 2000.00 },
       { "categoryName": "Entertainment", "amount": 500.00 }
     ]
   }
   ```

### Acceptance Criteria

- ✅ Yearly budget view excludes child category budgets from list
- ✅ Total budget = sum of parent budgets + standalone budgets only
- ✅ No double-counting (children not added separately since parents already include them)

---

## Test Scenario 5: Budget Update Propagation (US3 - P2)

**Goal**: Verify that updating a child budget amount adjusts the parent budget by the delta.

### Steps

1. **Setup: Create Initial Child Budget**
   - Create budget: **Fresh Produce - January 2026 - $500**
   - Verify parent: **Groceries - January 2026 - $500**

2. **Update Child Budget (Increase)**
   - Navigate to budget detail: http://localhost:3000/budgets/{id}
   - Click **Edit Budget**
   - Change amount: **$500 → $700** (+$200 delta)
   - Click **Save**

3. **Verify Parent Budget Increased by Delta**
   - Navigate to parent budget: **Groceries - January 2026**
   - **Expected amount**: **$700** (was $500, increased by $200)

4. **Update Child Budget (Decrease)**
   - Edit **Fresh Produce** budget again
   - Change amount: **$700 → $300** (-$400 delta)
   - Click **Save**

5. **Verify Parent Budget Decreased by Delta**
   - **Expected parent amount**: **$300** (was $700, decreased by $400)

6. **Verify Database State**
   ```sql
   SELECT c.name, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE b.year = 2026 AND b.month = 1
     AND c.id IN (1, 2);

   -- Expected Results:
   -- | name          | amount |
   -- |---------------|--------|
   -- | Groceries     | 300.00 | ← Updated by delta
   -- | Fresh Produce | 300.00 |
   ```

### Acceptance Criteria

- ✅ Updating child budget amount triggers parent budget adjustment
- ✅ Parent budget changes by delta (newAmount - oldAmount), not replaced entirely
- ✅ Works for both increases and decreases

---

## Test Scenario 6: Budget Deletion Propagation (US3 - P2)

**Goal**: Verify that deleting a child budget decreases the parent budget amount, and the parent record persists even at zero.

### Steps

1. **Setup: Create Multiple Child Budgets**
   - Create: **Fresh Produce - January 2026 - $500**
   - Create: **Pantry - January 2026 - $300**
   - Verify parent: **Groceries - January 2026 - $800**

2. **Delete First Child Budget**
   - Navigate to: http://localhost:3000/budgets (list view)
   - Find **Fresh Produce - January 2026**
   - Click **Delete**
   - Confirm deletion

3. **Verify Parent Budget Decreased**
   - **Expected parent amount**: **$300** (was $800, decreased by $500)

4. **Delete Remaining Child Budget**
   - Delete **Pantry - January 2026 - $300**

5. **Verify Parent Budget Persists at Zero**
   - Navigate to: **Groceries - January 2026**
   - **Expected**: Budget still exists in database/UI
   - **Expected amount**: **$0.00**
   - **NOT expected**: Parent budget deleted from database

6. **Verify Database State**
   ```sql
   SELECT c.name, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE b.year = 2026 AND b.month = 1 AND c.id = 1;

   -- Expected Result:
   -- | name      | amount |
   -- |-----------|--------|
   -- | Groceries | 0.00   | ← Still exists (not deleted)
   ```

### Acceptance Criteria

- ✅ Deleting child budget decreases parent budget by child's amount
- ✅ Parent budget record persists even when amount reaches zero (not deleted)
- ✅ Audit trail preserved (createdAt, updatedAt timestamps maintained)

---

## Test Scenario 7: Manual Parent Budget Creation (Edge Case)

**Goal**: Verify that users can manually create parent category budgets, and auto-rollup adds to the manual amount.

### Steps

1. **Manually Create Parent Budget**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Groceries** (parent category)
   - Select period: **February 2026**
   - Enter amount: **$200** (manual allocation)
   - Click **Create Budget**

2. **Create Child Budget for Same Period**
   - Select category: **Fresh Produce** (child)
   - Select period: **February 2026**
   - Enter amount: **$500**
   - Click **Create Budget**

3. **Verify Parent Budget Combines Manual + Rollup**
   - Navigate to: **Groceries - February 2026**
   - **Expected amount**: **$700** ($200 manual + $500 from child)

4. **Verify Database State**
   ```sql
   SELECT c.name, b.amount, b.description
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE b.year = 2026 AND b.month = 2
     AND c.id IN (1, 2);

   -- Expected Results:
   -- | name          | amount | description        |
   -- |---------------|--------|--------------------|
   -- | Groceries     | 700.00 | (manual or empty)  | ← Manual $200 + rollup $500
   -- | Fresh Produce | 500.00 | (user-provided)    |
   ```

### Acceptance Criteria

- ✅ Users can create parent category budgets manually
- ✅ Auto-rollup adds to manual parent budget amount (not replaces)
- ✅ Manual and auto-rollup amounts coexist in single parent budget

---

## Test Scenario 8: Standalone Category Budget (Edge Case)

**Goal**: Verify that standalone categories (no children) behave normally and are included in yearly budget totals.

### Steps

1. **Create Standalone Category Budget**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Entertainment** (no children)
   - Select period: **2026 (Yearly)**
   - Enter amount: **$500**
   - Click **Create Budget**

2. **Verify No Cascade Attempted**
   - Backend logs should NOT show: "Cascading to parent budget"
   - Only one budget created: **Entertainment - $500**

3. **Verify Standalone Budget Included in Yearly Total**
   - Navigate to yearly view: http://localhost:3000/budgets?year=2026
   - Verify **Entertainment** appears in budget list
   - Verify total budget includes $500 from Entertainment

4. **Verify Database State**
   ```sql
   SELECT c.name, c.parent_category_id, b.amount
   FROM budgets b
   JOIN categories c ON b.category_id = c.id
   WHERE c.id = 10;  -- Entertainment (standalone)

   -- Expected Result:
   -- | name          | parent_category_id | amount |
   -- |---------------|--------------------|--------|
   -- | Entertainment | NULL               | 500.00 |
   ```

### Acceptance Criteria

- ✅ Standalone categories work normally (no cascade attempted)
- ✅ Standalone category budgets included in yearly budget view totals
- ✅ No parent budget created for standalone categories

---

## Test Scenario 9: Transaction Atomicity (Error Handling)

**Goal**: Verify that if parent budget cascade fails, the entire transaction rolls back (child budget not created).

### Steps

1. **Setup: Simulate Database Constraint Violation**
   - Temporarily modify database to reject parent budget creation (e.g., add unique constraint violation)
   - Alternative: Use integration test with mocked repository that throws exception

2. **Attempt Child Budget Creation**
   - Navigate to: http://localhost:3000/budgets/new
   - Select category: **Fresh Produce**
   - Enter amount: **$500**
   - Click **Create Budget**

3. **Verify Transaction Rollback**
   - Frontend shows error: **"Failed to create budget"**
   - Backend logs show: `@Transactional rollback: cascade failed`
   - **Expected**: Child budget NOT created in database

4. **Verify Database Consistency**
   ```sql
   SELECT COUNT(*) FROM budgets
   WHERE category_id = 2  -- Fresh Produce
     AND year = 2026 AND month = 1;

   -- Expected Result: 0 (child budget rolled back)
   ```

### Acceptance Criteria

- ✅ If parent cascade fails, entire transaction rolls back
- ✅ Child budget NOT saved to database (atomicity guaranteed)
- ✅ User receives clear error message
- ✅ Database remains consistent (no orphaned child budgets)

---

## Automated Test Coverage (Backend Unit/Integration Tests)

### Unit Tests (BudgetServiceTest.java)

```java
@Test
void createChildBudget_autoCreatesParentBudget() {
    // Given: Child category with parent
    Category parent = new Category("Groceries", null);
    Category child = new Category("Fresh Produce", parent);
    CreateBudgetRequest request = new CreateBudgetRequest(child.getId(), 2026, 1, 500.00);

    // When: Create child budget
    BudgetDTO result = budgetService.createBudget(request);

    // Then: Child budget created
    assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

    // And: Parent budget auto-created with same amount
    Budget parentBudget = budgetRepository
        .findByCategoryAndYearAndMonth(parent, 2026, 1)
        .orElseThrow();
    assertThat(parentBudget.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
}

@Test
void updateChildBudget_adjustsParentByDelta() {
    // Given: Existing child budget ($500) with parent budget ($500)
    Budget childBudget = createTestBudget(childCategory, 500.00);
    Budget parentBudget = createTestBudget(parentCategory, 500.00);

    // When: Update child amount to $700
    UpdateBudgetRequest request = new UpdateBudgetRequest(700.00);
    budgetService.updateBudget(childBudget.getId(), request);

    // Then: Parent budget increased by delta ($200)
    Budget updatedParent = budgetRepository.findById(parentBudget.getId()).orElseThrow();
    assertThat(updatedParent.getAmount()).isEqualByComparingTo(new BigDecimal("700.00"));
}

@Test
void deleteChildBudget_decreasesParentAmount_persistsParentAtZero() {
    // Given: Only child budget ($500) with parent budget ($500)
    Budget childBudget = createTestBudget(childCategory, 500.00);
    Budget parentBudget = createTestBudget(parentCategory, 500.00);

    // When: Delete child budget
    budgetService.deleteBudget(childBudget.getId());

    // Then: Parent budget amount = $0 (not deleted)
    Budget remainingParent = budgetRepository.findById(parentBudget.getId()).orElseThrow();
    assertThat(remainingParent.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
}

@Test
void getYearlyBudgetView_excludesChildCategoryBudgets() {
    // Given: Parent budget ($1000) and child budgets ($500 + $500)
    createTestBudget(parentCategory, 1000.00);
    createTestBudget(childCategory1, 500.00);
    createTestBudget(childCategory2, 500.00);

    // When: Get yearly budget view
    YearlyBudgetViewDTO result = budgetService.getYearlyBudgetView(2026);

    // Then: Only parent budget included in list
    assertThat(result.getBudgets()).hasSize(1);
    assertThat(result.getBudgets().get(0).getCategoryName()).isEqualTo("Groceries");
    // And: Total budget = $1000 (not $2000 which would double-count)
    assertThat(result.getTotalBudget()).isEqualByComparingTo(new BigDecimal("1000.00"));
}
```

### Integration Tests (BudgetRepositoryTest.java)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class BudgetRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void findByCategoryAndYearAndMonth_returnsParentBudget() {
        // Given: Parent budget exists
        Budget parentBudget = budgetRepository.save(
            new Budget(parentCategory, 2026, 1, new BigDecimal("500.00"))
        );

        // When: Query by category, year, month
        Optional<Budget> result = budgetRepository
            .findByCategoryAndYearAndMonth(parentCategory, 2026, 1);

        // Then: Parent budget found
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(parentBudget.getId());
    }
}
```

---

## Summary

These integration scenarios validate all user stories and functional requirements for the parent category budget auto-rollup feature. Each scenario provides step-by-step instructions for manual testing through the frontend UI and includes database verification queries. Automated backend tests (unit + integration) provide regression coverage for service-layer logic and repository queries.

**Next Steps**:
1. Execute manual test scenarios to validate end-to-end behavior
2. Run backend test suite: `cd budget-backend && ./mvnw test`
3. If all tests pass, proceed to `/speckit.tasks` to generate implementation task breakdown
