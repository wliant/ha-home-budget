# API Contract: Hierarchical Category Budgets

**Feature**: 004-hierarchical-category-budgets
**Date**: 2025-11-11
**Purpose**: Define REST API changes, request/response formats, and error handling

## Overview

This feature extends existing Category and Budget REST APIs to support hierarchical relationships and category-based budgeting. All endpoints maintain backward compatibility with existing clients while adding new fields for hierarchy management.

## Base URL

```
http://localhost:8080/api
```

## Authentication

All endpoints require Home Assistant authentication via `X-Hass-User` header:

```http
X-Hass-User: household_member_name
```

## API Changes Summary

| Endpoint | Method | Change Type | Description |
|----------|--------|-------------|-------------|
| `/categories` | GET | EXTENDED | Returns categories with parent/children info |
| `/categories` | POST | EXTENDED | Accepts optional parentCategoryId |
| `/categories/{id}` | GET | EXTENDED | Returns category with parent/children |
| `/categories/{id}` | PUT | EXTENDED | Allows parent assignment/removal |
| `/categories/{id}` | DELETE | ENHANCED | Validates no children/budgets exist |
| `/categories/hierarchy` | GET | NEW | Returns full category tree structure |
| `/budgets` | GET | EXTENDED | Returns budgets with category info |
| `/budgets` | POST | EXTENDED | Requires categoryId, validates parent sum |
| `/budgets/{id}` | GET | EXTENDED | Returns budget with category details |
| `/budgets/{id}` | PUT | EXTENDED | Validates parent-child sum on update |
| `/budgets/{id}` | DELETE | UNCHANGED | No changes to deletion logic |

---

## Category API

### 1. Get All Categories

**Endpoint**: `GET /api/categories`

**Query Parameters**:
- `includeChildren` (boolean, optional, default: true) - Include children in response

**Response**: `200 OK`

```json
[
  {
    "id": 1,
    "name": "Food",
    "icon": "🍔",
    "parentCategoryId": null,
    "parentCategory": null,
    "children": [
      {
        "id": 2,
        "name": "Groceries",
        "icon": "🛒",
        "parentCategoryId": 1,
        "parentCategory": {
          "id": 1,
          "name": "Food"
        },
        "children": [],
        "isSystem": false,
        "createdBy": "alice",
        "createdAt": "2025-01-15T10:30:00"
      },
      {
        "id": 3,
        "name": "Dining Out",
        "icon": "🍽️",
        "parentCategoryId": 1,
        "parentCategory": {
          "id": 1,
          "name": "Food"
        },
        "children": [],
        "isSystem": false,
        "createdBy": "alice",
        "createdAt": "2025-01-15T10:35:00"
      }
    ],
    "isSystem": false,
    "createdBy": "alice",
    "createdAt": "2025-01-15T10:25:00"
  },
  {
    "id": 4,
    "name": "Transportation",
    "icon": "🚗",
    "parentCategoryId": null,
    "parentCategory": null,
    "children": [],
    "isSystem": false,
    "createdBy": "bob",
    "createdAt": "2025-01-15T11:00:00"
  }
]
```

**Field Descriptions**:
- `parentCategoryId`: ID of parent category (NULL for root categories)
- `parentCategory`: Embedded parent category summary (id, name only)
- `children`: Array of child categories (empty array if no children)

---

### 2. Get Category Hierarchy

**Endpoint**: `GET /api/categories/hierarchy`

**Purpose**: Returns full category tree with nesting structure optimized for tree rendering

**Response**: `200 OK`

```json
{
  "rootCategories": [
    {
      "id": 1,
      "name": "Food",
      "icon": "🍔",
      "children": [
        {
          "id": 2,
          "name": "Groceries",
          "icon": "🛒",
          "childCount": 0
        },
        {
          "id": 3,
          "name": "Dining Out",
          "icon": "🍽️",
          "childCount": 0
        }
      ],
      "childCount": 2
    },
    {
      "id": 4,
      "name": "Transportation",
      "icon": "🚗",
      "children": [],
      "childCount": 0
    }
  ],
  "totalCategories": 4,
  "maxDepth": 2
}
```

---

### 3. Create Category

**Endpoint**: `POST /api/categories`

**Request Body**:

```json
{
  "name": "Groceries",
  "icon": "🛒",
  "parentCategoryId": 1
}
```

**Field Constraints**:
- `name`: Required, 1-100 characters, unique across all categories
- `icon`: Optional, max 10 characters
- `parentCategoryId`: Optional, must reference existing category

**Success Response**: `201 Created`

```json
{
  "id": 2,
  "name": "Groceries",
  "icon": "🛒",
  "parentCategoryId": 1,
  "parentCategory": {
    "id": 1,
    "name": "Food"
  },
  "children": [],
  "isSystem": false,
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:30:00"
}
```

**Error Response**: `400 Bad Request` - Duplicate name

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Category name 'Groceries' already exists",
  "path": "/api/categories"
}
```

**Error Response**: `404 Not Found` - Parent not found

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Parent category with id 999 not found",
  "path": "/api/categories"
}
```

**Error Response**: `400 Bad Request` - Hierarchy depth violation

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot create child category: parent 'Groceries' is already a child category. Maximum hierarchy depth is 2 levels.",
  "path": "/api/categories"
}
```

---

### 4. Update Category

**Endpoint**: `PUT /api/categories/{id}`

**Request Body**:

```json
{
  "name": "Grocery Shopping",
  "icon": "🛒",
  "parentCategoryId": 1
}
```

**Success Response**: `200 OK`

```json
{
  "id": 2,
  "name": "Grocery Shopping",
  "icon": "🛒",
  "parentCategoryId": 1,
  "parentCategory": {
    "id": 1,
    "name": "Food"
  },
  "children": [],
  "isSystem": false,
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:30:00"
}
```

**Error Response**: `400 Bad Request` - Circular reference

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot set parent: circular reference detected. Category 'Food' (id=1) cannot be a parent of 'Groceries' (id=2) because 'Groceries' is an ancestor of 'Food'.",
  "path": "/api/categories/1"
}
```

**Error Response**: `400 Bad Request` - Cannot change parent with active budgets

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot change parent category: 3 active budgets reference this category. Please reassign budgets first.",
  "path": "/api/categories/2",
  "details": {
    "affectedBudgetCount": 3
  }
}
```

**Error Response**: `403 Forbidden` - System category modification

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Cannot assign parent to system category 'Uncategorized'",
  "path": "/api/categories/100"
}
```

---

### 5. Delete Category

**Endpoint**: `DELETE /api/categories/{id}`

**Success Response**: `204 No Content`

**Error Response**: `400 Bad Request` - Has children

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete category 'Food': has 2 child categories. Please reassign or delete children first.",
  "path": "/api/categories/1",
  "details": {
    "childCount": 2,
    "childCategories": [
      {"id": 2, "name": "Groceries"},
      {"id": 3, "name": "Dining Out"}
    ]
  }
}
```

**Error Response**: `400 Bad Request` - Has budgets

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete category 'Groceries': has 5 associated budgets. Please reassign budgets to another category first.",
  "path": "/api/categories/2",
  "details": {
    "budgetCount": 5
  }
}
```

**Error Response**: `403 Forbidden` - System category

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Cannot delete system category 'Uncategorized'",
  "path": "/api/categories/100"
}
```

---

## Budget API

### 6. Get All Budgets

**Endpoint**: `GET /api/budgets`

**Query Parameters**:
- `year` (integer, optional) - Filter by year
- `month` (integer, optional) - Filter by month (1-12)
- `categoryId` (long, optional) - Filter by category

**Response**: `200 OK`

```json
[
  {
    "id": 1,
    "year": 2025,
    "month": 1,
    "categoryId": 2,
    "category": {
      "id": 2,
      "name": "Groceries",
      "icon": "🛒",
      "parentCategoryId": 1,
      "parentCategory": {
        "id": 1,
        "name": "Food"
      }
    },
    "totalAmount": 300.00,
    "description": "Monthly grocery budget",
    "createdBy": "alice",
    "createdAt": "2025-01-15T10:40:00",
    "updatedAt": "2025-01-15T10:40:00",
    "version": 0
  },
  {
    "id": 2,
    "year": 2025,
    "month": 1,
    "categoryId": 3,
    "category": {
      "id": 3,
      "name": "Dining Out",
      "icon": "🍽️",
      "parentCategoryId": 1,
      "parentCategory": {
        "id": 1,
        "name": "Food"
      }
    },
    "totalAmount": 200.00,
    "description": "Restaurants and takeout",
    "createdBy": "alice",
    "createdAt": "2025-01-15T10:45:00",
    "updatedAt": "2025-01-15T10:45:00",
    "version": 0
  },
  {
    "id": 3,
    "year": 2025,
    "month": 1,
    "categoryId": 1,
    "category": {
      "id": 1,
      "name": "Food",
      "icon": "🍔",
      "parentCategoryId": null,
      "parentCategory": null
    },
    "totalAmount": 500.00,
    "description": "Total food budget (parent category)",
    "createdBy": "alice",
    "createdAt": "2025-01-15T10:50:00",
    "updatedAt": "2025-01-15T10:50:00",
    "version": 0
  }
]
```

**Field Descriptions**:
- `categoryId`: ID of associated category (REQUIRED for new budgets, nullable for legacy)
- `category`: Embedded category details with parent info
- `version`: Optimistic locking version for concurrent updates

---

### 7. Create Budget

**Endpoint**: `POST /api/budgets`

**Request Body**:

```json
{
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "totalAmount": 300.00,
  "description": "Monthly grocery budget"
}
```

**Field Constraints**:
- `year`: Required, 2000-9999
- `month`: Required, 1-12
- `categoryId`: **Required**, must reference existing category
- `totalAmount`: Required, must be positive (> 0), max 10 digits with 2 decimal places
- `description`: Optional, max 500 characters

**Success Response**: `201 Created`

```json
{
  "id": 1,
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "category": {
    "id": 2,
    "name": "Groceries",
    "icon": "🛒",
    "parentCategoryId": 1,
    "parentCategory": {
      "id": 1,
      "name": "Food"
    }
  },
  "totalAmount": 300.00,
  "description": "Monthly grocery budget",
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:40:00",
  "updatedAt": "2025-01-15T10:40:00",
  "version": 0
}
```

**Error Response**: `400 Bad Request` - Missing category

```json
{
  "timestamp": "2025-01-15T10:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Category is required for new budgets",
  "path": "/api/budgets"
}
```

**Error Response**: `404 Not Found` - Category not found

```json
{
  "timestamp": "2025-01-15T10:40:00",
  "status": 404,
  "error": "Not Found",
  "message": "Category with id 999 not found",
  "path": "/api/budgets"
}
```

**Error Response**: `409 Conflict` - Duplicate budget

```json
{
  "timestamp": "2025-01-15T10:40:00",
  "status": 409,
  "error": "Conflict",
  "message": "A budget for category 'Groceries' already exists for January 2025",
  "path": "/api/budgets",
  "details": {
    "existingBudgetId": 5,
    "year": 2025,
    "month": 1,
    "categoryId": 2
  }
}
```

**Error Response**: `400 Bad Request` - Parent budget mismatch

```json
{
  "timestamp": "2025-01-15T10:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Parent budget amount ($600.00) must equal sum of child budgets ($500.00)",
  "path": "/api/budgets",
  "details": {
    "categoryId": 1,
    "categoryName": "Food",
    "requestedAmount": 600.00,
    "requiredAmount": 500.00,
    "childBudgets": [
      {"categoryId": 2, "categoryName": "Groceries", "amount": 300.00},
      {"categoryId": 3, "categoryName": "Dining Out", "amount": 200.00}
    ]
  }
}
```

**Warning Response**: `201 Created` with warning header - Child budgets without parent

```http
HTTP/1.1 201 Created
X-Budget-Warning: Parent category 'Food' has no budget for this period. Consider creating a parent budget.
Content-Type: application/json

{
  "id": 1,
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "category": {
    "id": 2,
    "name": "Groceries",
    "icon": "🛒",
    "parentCategoryId": 1,
    "parentCategory": {
      "id": 1,
      "name": "Food"
    }
  },
  "totalAmount": 300.00,
  "description": "Monthly grocery budget",
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:40:00",
  "updatedAt": "2025-01-15T10:40:00",
  "version": 0
}
```

---

### 8. Update Budget

**Endpoint**: `PUT /api/budgets/{id}`

**Request Body**:

```json
{
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "totalAmount": 350.00,
  "description": "Increased grocery budget",
  "version": 0
}
```

**Success Response**: `200 OK`

```json
{
  "id": 1,
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "category": {
    "id": 2,
    "name": "Groceries",
    "icon": "🛒",
    "parentCategoryId": 1,
    "parentCategory": {
      "id": 1,
      "name": "Food"
    }
  },
  "totalAmount": 350.00,
  "description": "Increased grocery budget",
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:40:00",
  "updatedAt": "2025-01-15T11:30:00",
  "version": 1
}
```

**Error Response**: `409 Conflict` - Optimistic locking failure

```json
{
  "timestamp": "2025-01-15T11:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Budget has been modified by another user. Please refresh and try again.",
  "path": "/api/budgets/1",
  "details": {
    "requestedVersion": 0,
    "currentVersion": 1
  }
}
```

**Error Response**: `400 Bad Request` - Parent budget validation after child update

```json
{
  "timestamp": "2025-01-15T11:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Updating this child budget will cause parent budget mismatch. Parent 'Food' budget ($500.00) will not equal new sum ($550.00).",
  "path": "/api/budgets/1",
  "details": {
    "parentCategoryId": 1,
    "parentCategoryName": "Food",
    "parentBudgetAmount": 500.00,
    "newChildrenSum": 550.00,
    "difference": 50.00,
    "affectedChildBudgets": [
      {"categoryId": 2, "categoryName": "Groceries", "currentAmount": 300.00, "newAmount": 350.00},
      {"categoryId": 3, "categoryName": "Dining Out", "amount": 200.00}
    ]
  }
}
```

---

### 9. Get Budget by ID

**Endpoint**: `GET /api/budgets/{id}`

**Response**: `200 OK`

```json
{
  "id": 1,
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "category": {
    "id": 2,
    "name": "Groceries",
    "icon": "🛒",
    "parentCategoryId": 1,
    "parentCategory": {
      "id": 1,
      "name": "Food"
    }
  },
  "totalAmount": 300.00,
  "description": "Monthly grocery budget",
  "createdBy": "alice",
  "createdAt": "2025-01-15T10:40:00",
  "updatedAt": "2025-01-15T10:40:00",
  "version": 0,
  "validation": {
    "isParentCategory": false,
    "hasParentBudget": true,
    "parentBudgetAmount": 500.00,
    "siblingBudgets": [
      {"categoryId": 3, "categoryName": "Dining Out", "amount": 200.00}
    ],
    "childrenSum": null
  }
}
```

**Enhanced Field**:
- `validation`: Additional context for budget validation (included when `includeValidation=true` query param)

---

## Error Response Formats

### Standard Error Structure

All error responses follow this structure:

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable error message",
  "path": "/api/endpoint",
  "details": {
    "additionalContextKey": "additionalContextValue"
  }
}
```

### Custom Exception Types

| Exception | HTTP Status | Error Code | Description |
|-----------|-------------|------------|-------------|
| `ParentBudgetMismatchException` | 400 | `PARENT_BUDGET_MISMATCH` | Parent budget doesn't equal child sum |
| `CircularCategoryException` | 400 | `CIRCULAR_CATEGORY_REFERENCE` | Category hierarchy contains cycle |
| `CategoryInUseException` | 400 | `CATEGORY_IN_USE` | Category has children/budgets |
| `CategoryNotFoundException` | 404 | `CATEGORY_NOT_FOUND` | Referenced category doesn't exist |
| `DuplicateBudgetException` | 409 | `DUPLICATE_BUDGET` | Budget already exists for period |
| `SystemCategoryException` | 403 | `SYSTEM_CATEGORY_PROTECTED` | Cannot modify system category |
| `HierarchyDepthException` | 400 | `HIERARCHY_DEPTH_EXCEEDED` | Exceeds 2-level limit |

---

## Backward Compatibility

### Legacy Budget Handling

**Existing budgets** (created before this feature) have `categoryId = NULL`:

```json
{
  "id": 100,
  "year": 2024,
  "month": 12,
  "categoryId": null,
  "category": null,
  "totalAmount": 1000.00,
  "description": "Legacy budget without category",
  "createdBy": "alice",
  "createdAt": "2024-12-01T10:00:00",
  "updatedAt": "2024-12-01T10:00:00",
  "version": 0
}
```

**Clients must handle**:
- `category` field may be `null`
- `categoryId` field may be `null`
- New budgets **require** `categoryId` (cannot create with NULL)

### Deprecation Notices

No endpoints are deprecated. All changes are additive:
- New fields added to existing DTOs
- New validation rules enforce data integrity
- New endpoints provide enhanced functionality

---

## Example API Flows

### Flow 1: Create Category Hierarchy

**Step 1**: Create parent category

```http
POST /api/categories
Content-Type: application/json
X-Hass-User: alice

{
  "name": "Food",
  "icon": "🍔"
}
```

Response: `201 Created` with `id: 1`

**Step 2**: Create child category

```http
POST /api/categories
Content-Type: application/json
X-Hass-User: alice

{
  "name": "Groceries",
  "icon": "🛒",
  "parentCategoryId": 1
}
```

Response: `201 Created` with `id: 2`

---

### Flow 2: Create Budget with Parent-Child Validation

**Step 1**: Create child budget (Groceries)

```http
POST /api/budgets
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "totalAmount": 300.00,
  "description": "Grocery budget"
}
```

Response: `201 Created` with warning header about missing parent budget

**Step 2**: Create child budget (Dining Out)

```http
POST /api/budgets
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 1,
  "categoryId": 3,
  "totalAmount": 200.00,
  "description": "Dining budget"
}
```

Response: `201 Created` with warning header

**Step 3**: Create parent budget (Food) - must equal child sum

```http
POST /api/budgets
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 1,
  "categoryId": 1,
  "totalAmount": 500.00,
  "description": "Total food budget"
}
```

Response: `201 Created` (success - amount equals 300 + 200)

**Step 4**: Attempt to create parent with wrong amount

```http
POST /api/budgets
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 2,
  "categoryId": 1,
  "totalAmount": 600.00,
  "description": "Total food budget"
}
```

Response: `400 Bad Request` - Parent budget mismatch error (no children exist for Feb yet)

---

### Flow 3: Update Child Budget

**Update child budget amount**:

```http
PUT /api/budgets/1
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 1,
  "categoryId": 2,
  "totalAmount": 350.00,
  "description": "Increased grocery budget",
  "version": 0
}
```

Response: `400 Bad Request` - Validation error explaining parent budget needs adjustment

**Fix**: First update parent budget, then child:

```http
PUT /api/budgets/3
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 1,
  "categoryId": 1,
  "totalAmount": 550.00,
  "description": "Updated total food budget",
  "version": 0
}
```

Response: `200 OK`

Then retry child update - now succeeds.

---

## Validation Rules Summary

### Category Validation

| Rule | Enforcement | Error Response |
|------|-------------|----------------|
| Name uniqueness | Database unique constraint | 400 Bad Request |
| Parent existence | Foreign key + service check | 404 Not Found |
| Circular reference | Service layer traversal | 400 Bad Request |
| Hierarchy depth (max 2) | Service layer check | 400 Bad Request |
| System category protection | Service layer check | 403 Forbidden |
| No deletion with children | Service layer check | 400 Bad Request |
| No deletion with budgets | Service layer check | 400 Bad Request |

### Budget Validation

| Rule | Enforcement | Error Response |
|------|-------------|----------------|
| Category required | Service layer check | 400 Bad Request |
| Category existence | Foreign key + service check | 404 Not Found |
| Amount positive | Service layer check | 400 Bad Request |
| Period uniqueness | Partial unique index | 409 Conflict |
| Parent = sum of children | Service layer calculation | 400 Bad Request |
| Optimistic locking | JPA @Version | 409 Conflict |

---

## Testing Considerations

### Contract Tests

1. **Schema Validation**: All responses match JSON schema definitions
2. **Error Format Consistency**: All errors follow standard structure
3. **Backward Compatibility**: Legacy budgets (categoryId=NULL) handled correctly
4. **Parent-Child Validation**: All sum mismatch scenarios return correct errors
5. **Optimistic Locking**: Concurrent updates properly rejected
6. **Warning Headers**: Non-blocking warnings included in response headers

### Integration Tests

1. **Happy Path**: Create hierarchy, create budgets, verify sum validation
2. **Circular Reference**: Attempt to create cycle, verify rejection
3. **Depth Violation**: Attempt 3-level hierarchy, verify rejection
4. **Concurrent Updates**: Simulate race conditions with optimistic locking
5. **Legacy Budget Migration**: Update old budget to add category
6. **Category Deletion Protection**: Verify cascading checks work

---

## Summary

All REST API contracts defined with:
- ✅ Request/response formats for all endpoints
- ✅ Error responses with detailed messages and codes
- ✅ Validation rule enforcement points
- ✅ Backward compatibility handling for legacy budgets
- ✅ Example API flows for common scenarios
- ✅ Testing considerations for contract validation

**Next Phase**: Ready for quickstart generation with integration test scenarios.
