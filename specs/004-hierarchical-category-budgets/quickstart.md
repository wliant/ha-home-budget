# Quickstart: Hierarchical Category Budgets

**Feature**: 004-hierarchical-category-budgets
**Date**: 2025-11-11
**Purpose**: Integration testing guide and troubleshooting for feature validation

## Overview

This guide provides step-by-step integration tests for the Hierarchical Category Budgets feature. Each scenario tests a complete user story flow from the specification, using curl commands to validate API behavior.

**Prerequisites**:
- Backend service running on `http://localhost:8080`
- Database initialized with Liquibase migrations
- User authenticated with `X-Hass-User: test_user` header

---

## Integration Test Scenarios

### Scenario 1: Basic Category Hierarchy (User Story 1)

**Objective**: Create a two-level category hierarchy and verify parent-child relationships

**Steps**:

1. Create parent category "Food":

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "name": "Food",
    "icon": "🍔"
  }'
```

**Expected**: `201 Created` with response:
```json
{
  "id": 1,
  "name": "Food",
  "icon": "🍔",
  "parentCategoryId": null,
  "children": [],
  "isSystem": false
}
```

2. Create child category "Groceries":

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "name": "Groceries",
    "icon": "🛒",
    "parentCategoryId": 1
  }'
```

**Expected**: `201 Created` with `parentCategoryId: 1`

3. Create second child "Dining Out":

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "name": "Dining Out",
    "icon": "🍽️",
    "parentCategoryId": 1
  }'
```

**Expected**: `201 Created`

4. Verify hierarchy:

```bash
curl -X GET http://localhost:8080/api/categories/hierarchy \
  -H "X-Hass-User: test_user"
```

**Expected**: `200 OK` with nested structure showing "Food" parent with 2 children

**Validation**:
- ✅ Parent categories created without parents
- ✅ Child categories linked to parents
- ✅ Hierarchy endpoint returns correct tree structure

---

### Scenario 2: Circular Reference Prevention (User Story 1)

**Objective**: Verify system prevents circular category references

**Prerequisite**: Categories from Scenario 1 exist

**Steps**:

1. Attempt to make "Food" (parent) a child of "Groceries" (child):

```bash
curl -X PUT http://localhost:8080/api/categories/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "name": "Food",
    "icon": "🍔",
    "parentCategoryId": 2
  }'
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot set parent: circular reference detected. Category 'Food' (id=1) cannot be a parent of 'Groceries' (id=2) because 'Groceries' is an ancestor of 'Food'."
}
```

**Validation**:
- ✅ Circular references rejected
- ✅ Clear error message provided

---

### Scenario 3: Hierarchy Depth Limit (User Story 1)

**Objective**: Verify system enforces 2-level hierarchy maximum

**Prerequisite**: Categories from Scenario 1 exist

**Steps**:

1. Attempt to create grandchild under "Groceries":

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "name": "Organic Produce",
    "icon": "🥬",
    "parentCategoryId": 2
  }'
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot create child category: parent 'Groceries' is already a child category. Maximum hierarchy depth is 2 levels."
}
```

**Validation**:
- ✅ Three-level hierarchy rejected
- ✅ Hierarchy depth enforced

---

### Scenario 4: Category-Based Budget Creation (User Story 2)

**Objective**: Create budgets for categories and verify associations

**Prerequisite**: Categories from Scenario 1 exist

**Steps**:

1. Create budget for "Groceries" category:

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 300.00,
    "description": "Monthly grocery budget"
  }'
```

**Expected**: `201 Created` with warning header:
```http
HTTP/1.1 201 Created
X-Budget-Warning: Parent category 'Food' has no budget for this period. Consider creating a parent budget.
```

Response body includes `category` object with full details.

2. Create budget for "Dining Out":

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 3,
    "totalAmount": 200.00,
    "description": "Restaurant budget"
  }'
```

**Expected**: `201 Created` with same warning header

3. Verify budgets created:

```bash
curl -X GET "http://localhost:8080/api/budgets?year=2025&month=1" \
  -H "X-Hass-User: test_user"
```

**Expected**: `200 OK` with 2 budgets, both showing `category` with `parentCategory` nested

**Validation**:
- ✅ Budgets require category
- ✅ Budgets linked to categories correctly
- ✅ Warning shown for missing parent budget

---

### Scenario 5: Duplicate Budget Prevention (User Story 2)

**Objective**: Verify system prevents duplicate budgets for same category/period

**Prerequisite**: Budget from Scenario 4 exists

**Steps**:

1. Attempt to create duplicate budget for "Groceries" in January 2025:

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 350.00,
    "description": "Duplicate budget"
  }'
```

**Expected**: `409 Conflict` with error:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A budget for category 'Groceries' already exists for January 2025",
  "details": {
    "existingBudgetId": 1,
    "year": 2025,
    "month": 1,
    "categoryId": 2
  }
}
```

**Validation**:
- ✅ Duplicate budgets rejected
- ✅ Unique constraint enforced per (category, year, month)

---

### Scenario 6: Parent Budget Sum Validation - Success (User Story 3)

**Objective**: Create parent budget that correctly equals sum of children

**Prerequisite**: Child budgets from Scenario 4 exist (Groceries: $300, Dining Out: $200)

**Steps**:

1. Create parent budget with correct sum ($500):

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 1,
    "totalAmount": 500.00,
    "description": "Total food budget"
  }'
```

**Expected**: `201 Created` with no warnings

2. Verify all budgets:

```bash
curl -X GET "http://localhost:8080/api/budgets?year=2025&month=1" \
  -H "X-Hass-User: test_user"
```

**Expected**: `200 OK` with 3 budgets (Food: $500, Groceries: $300, Dining Out: $200)

**Validation**:
- ✅ Parent budget creation succeeds when sum matches
- ✅ Validation calculation correct

---

### Scenario 7: Parent Budget Sum Validation - Failure (User Story 3)

**Objective**: Reject parent budget that doesn't match child sum

**Prerequisite**: Child budgets from Scenario 4 exist

**Steps**:

1. Attempt to create parent budget with incorrect amount:

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 2,
    "categoryId": 1,
    "totalAmount": 600.00,
    "description": "Incorrect parent budget"
  }'
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Parent budget amount ($600.00) must equal sum of child budgets ($0.00)",
  "details": {
    "categoryId": 1,
    "categoryName": "Food",
    "requestedAmount": 600.00,
    "requiredAmount": 0.00,
    "childBudgets": []
  }
}
```

(No child budgets exist for February, so sum is $0)

**Validation**:
- ✅ Parent budget mismatch rejected
- ✅ Detailed error includes expected sum

---

### Scenario 8: Child Budget Update with Parent Validation (User Story 3)

**Objective**: Verify updating child budget validates against parent constraint

**Prerequisite**: Complete budget hierarchy from Scenario 6 exists

**Steps**:

1. Attempt to update "Groceries" budget without adjusting parent:

```bash
curl -X PUT http://localhost:8080/api/budgets/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 350.00,
    "description": "Increased grocery budget",
    "version": 0
  }'
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Updating this child budget will cause parent budget mismatch. Parent 'Food' budget ($500.00) will not equal new sum ($550.00).",
  "details": {
    "parentCategoryId": 1,
    "parentCategoryName": "Food",
    "parentBudgetAmount": 500.00,
    "newChildrenSum": 550.00,
    "difference": 50.00
  }
}
```

2. First update parent budget:

```bash
curl -X PUT http://localhost:8080/api/budgets/3 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 1,
    "totalAmount": 550.00,
    "description": "Updated total food budget",
    "version": 0
  }'
```

**Expected**: `200 OK`

3. Retry child budget update:

```bash
curl -X PUT http://localhost:8080/api/budgets/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 350.00,
    "description": "Increased grocery budget",
    "version": 0
  }'
```

**Expected**: `200 OK` with updated budget

**Validation**:
- ✅ Child update validates against parent
- ✅ Parent must be adjusted before child
- ✅ Two-step update workflow enforced

---

### Scenario 9: Category Deletion Protection (User Story 4)

**Objective**: Verify categories with budgets cannot be deleted

**Prerequisite**: Budget for "Groceries" exists

**Steps**:

1. Attempt to delete "Groceries" category with active budget:

```bash
curl -X DELETE http://localhost:8080/api/categories/2 \
  -H "X-Hass-User: test_user"
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete category 'Groceries': has 1 associated budgets. Please reassign budgets to another category first.",
  "details": {
    "budgetCount": 1
  }
}
```

2. Attempt to delete parent "Food" category with children:

```bash
curl -X DELETE http://localhost:8080/api/categories/1 \
  -H "X-Hass-User: test_user"
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete category 'Food': has 2 child categories. Please reassign or delete children first.",
  "details": {
    "childCount": 2,
    "childCategories": [
      {"id": 2, "name": "Groceries"},
      {"id": 3, "name": "Dining Out"}
    ]
  }
}
```

**Validation**:
- ✅ Categories with budgets cannot be deleted
- ✅ Categories with children cannot be deleted
- ✅ Clear error messages guide user

---

### Scenario 10: Missing Category Validation (User Story 4)

**Objective**: Verify budget creation requires category

**Steps**:

1. Attempt to create budget without category:

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 3,
    "totalAmount": 100.00,
    "description": "Budget without category"
  }'
```

**Expected**: `400 Bad Request` with error:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Category is required for new budgets"
}
```

2. Attempt with non-existent category:

```bash
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2025,
    "month": 3,
    "categoryId": 999,
    "totalAmount": 100.00,
    "description": "Budget with invalid category"
  }'
```

**Expected**: `404 Not Found` with error:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Category with id 999 not found"
}
```

**Validation**:
- ✅ Budget creation requires valid category
- ✅ Category existence validated

---

### Scenario 11: Optimistic Locking (Concurrency Control)

**Objective**: Verify concurrent budget updates handled correctly

**Prerequisite**: Budget from Scenario 4 exists

**Steps**:

1. Get current budget version:

```bash
curl -X GET http://localhost:8080/api/budgets/1 \
  -H "X-Hass-User: test_user"
```

**Expected**: `200 OK` with `"version": 0`

2. Update budget (User A):

```bash
curl -X PUT http://localhost:8080/api/budgets/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: user_a" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 310.00,
    "description": "Updated by User A",
    "version": 0
  }'
```

**Expected**: `200 OK` with `"version": 1`

3. Attempt concurrent update with stale version (User B):

```bash
curl -X PUT http://localhost:8080/api/budgets/1 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: user_b" \
  -d '{
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "totalAmount": 320.00,
    "description": "Updated by User B",
    "version": 0
  }'
```

**Expected**: `409 Conflict` with error:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Budget has been modified by another user. Please refresh and try again.",
  "details": {
    "requestedVersion": 0,
    "currentVersion": 1
  }
}
```

**Validation**:
- ✅ Optimistic locking prevents lost updates
- ✅ Stale version rejected

---

### Scenario 12: Legacy Budget Compatibility

**Objective**: Verify existing budgets without categories remain functional

**Steps**:

1. Simulate legacy budget (created before this feature):

```sql
-- Execute in database console
INSERT INTO budgets (year, month, category_id, total_amount, description, created_by, created_at, updated_at, version)
VALUES (2024, 12, NULL, 1000.00, 'Legacy budget', 'test_user', NOW(), NOW(), 0);
```

2. Retrieve legacy budget via API:

```bash
curl -X GET "http://localhost:8080/api/budgets?year=2024&month=12" \
  -H "X-Hass-User: test_user"
```

**Expected**: `200 OK` with budget:
```json
{
  "id": 10,
  "year": 2024,
  "month": 12,
  "categoryId": null,
  "category": null,
  "totalAmount": 1000.00,
  "description": "Legacy budget"
}
```

3. Update legacy budget to add category:

```bash
curl -X PUT http://localhost:8080/api/budgets/10 \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{
    "year": 2024,
    "month": 12,
    "categoryId": 1,
    "totalAmount": 1000.00,
    "description": "Legacy budget now categorized",
    "version": 0
  }'
```

**Expected**: `200 OK` with category assigned

**Validation**:
- ✅ Legacy budgets readable
- ✅ Legacy budgets can be updated to add category
- ✅ Backward compatibility maintained

---

## Troubleshooting Guide

### Issue 1: Parent Budget Validation Fails Unexpectedly

**Symptom**: Creating parent budget fails even though sum appears correct

**Debugging Steps**:

1. Query child budgets for the period:

```bash
curl -X GET "http://localhost:8080/api/budgets?year=2025&month=1" \
  -H "X-Hass-User: test_user" | jq '.[] | select(.category.parentCategoryId == 1)'
```

2. Calculate sum manually from response
3. Verify decimal precision matches (2 decimal places)
4. Check for hidden child budgets (deleted but not committed)

**Common Causes**:
- Child budgets exist that user forgot about
- Decimal rounding differences (use exact amounts)
- Transaction not committed (refresh data)

---

### Issue 2: Circular Reference Error When None Expected

**Symptom**: Circular reference error when creating seemingly valid hierarchy

**Debugging Steps**:

1. Trace full parent chain:

```bash
curl -X GET http://localhost:8080/api/categories/hierarchy \
  -H "X-Hass-User: test_user"
```

2. Check if proposed parent is actually a child

**Common Causes**:
- Category relationships cached in client
- Misunderstanding of current hierarchy state
- Previous failed updates left partial state

---

### Issue 3: Cannot Delete Category

**Symptom**: Category deletion fails with "in use" error

**Debugging Steps**:

1. Check for child categories:

```bash
curl -X GET http://localhost:8080/api/categories \
  -H "X-Hass-User: test_user" | jq '.[] | select(.parentCategoryId == 2)'
```

2. Check for associated budgets:

```bash
curl -X GET "http://localhost:8080/api/budgets?categoryId=2" \
  -H "X-Hass-User: test_user"
```

3. Check for associated expenses (if applicable):

```bash
curl -X GET "http://localhost:8080/api/expenses?categoryId=2" \
  -H "X-Hass-User: test_user"
```

**Resolution**:
- Reassign child categories to different parent or NULL
- Reassign budgets to different category
- Delete expenses or reassign to different category

---

### Issue 4: Optimistic Locking Failures

**Symptom**: Frequent 409 Conflict errors when updating budgets

**Debugging Steps**:

1. Check current version:

```bash
curl -X GET http://localhost:8080/api/budgets/1 \
  -H "X-Hass-User: test_user" | jq '.version'
```

2. Ensure PUT request includes latest version

**Common Causes**:
- Multiple users editing simultaneously
- Client cached stale data
- Auto-refresh not implemented in UI

**Resolution**:
- Implement optimistic retry logic in client
- Add version refresh before updates
- Show "modified by another user" notification

---

### Issue 5: Warning Headers Not Visible

**Symptom**: Missing parent budget warnings not shown to user

**Debugging Steps**:

1. Inspect response headers:

```bash
curl -i -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{"year": 2025, "month": 1, "categoryId": 2, "totalAmount": 300.00}'
```

2. Look for `X-Budget-Warning` header

**Common Causes**:
- Client not reading custom headers
- Proxy stripping custom headers
- Frontend not displaying warnings

**Resolution**:
- Check network tab in browser devtools
- Verify header handling in API client code
- Add warning display component in UI

---

## Performance Testing

### Load Test: Category Hierarchy Rendering

**Scenario**: Measure response time for hierarchy with 100 categories

**Setup**:

```bash
# Create 10 parent categories
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/categories \
    -H "Content-Type: application/json" \
    -H "X-Hass-User: test_user" \
    -d "{\"name\": \"Parent$i\", \"icon\": \"📁\"}"
done

# Create 10 children for each parent (100 total children)
for parent in {1..10}; do
  for child in {1..10}; do
    curl -X POST http://localhost:8080/api/categories \
      -H "Content-Type: application/json" \
      -H "X-Hass-User: test_user" \
      -d "{\"name\": \"Parent${parent}_Child${child}\", \"icon\": \"📄\", \"parentCategoryId\": $parent}"
  done
done
```

**Test**:

```bash
time curl -X GET http://localhost:8080/api/categories/hierarchy \
  -H "X-Hass-User: test_user" -w "\nTime: %{time_total}s\n"
```

**Success Criteria**: Response time < 500ms for 100 categories

---

### Load Test: Budget Sum Validation

**Scenario**: Measure validation time when creating parent budget with many children

**Setup**:

```bash
# Create parent category
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{"name": "TestParent", "icon": "🔖"}'

# Create 20 child categories
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/categories \
    -H "Content-Type: application/json" \
    -H "X-Hass-User: test_user" \
    -d "{\"name\": \"Child$i\", \"icon\": \"📌\", \"parentCategoryId\": 101}"
done

# Create child budgets
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/budgets \
    -H "Content-Type: application/json" \
    -H "X-Hass-User: test_user" \
    -d "{\"year\": 2025, \"month\": 4, \"categoryId\": $((100+i)), \"totalAmount\": 50.00}"
done
```

**Test**:

```bash
time curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: test_user" \
  -d '{"year": 2025, "month": 4, "categoryId": 101, "totalAmount": 1000.00}' \
  -w "\nTime: %{time_total}s\n"
```

**Success Criteria**: Validation completes in < 2 seconds

---

## Summary

Integration test scenarios cover all user stories:
- ✅ User Story 1: Category hierarchy creation and validation
- ✅ User Story 2: Category-based budget creation
- ✅ User Story 3: Parent-child sum validation
- ✅ User Story 4: Category requirement enforcement
- ✅ User Story 5: Budget summary queries (via GET endpoints)

**Total Scenarios**: 12 integration tests
**Coverage**: All acceptance criteria from specification
**Performance Tests**: 2 load tests for critical paths

**Ready for**: Phase 2 task generation via `/speckit.tasks`
