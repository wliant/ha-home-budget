# API Contracts - Homepage Dashboard Update

**Feature**: Homepage Dashboard Update (006)
**Date**: 2025-12-22
**API Version**: v1

## Overview

This feature uses **existing API endpoints** from Features 002 (Budget Management) and 004 (Hierarchical Category Budgets). No new endpoints are required. This document specifies the exact API calls the homepage will make.

---

## Endpoint 1: Get Current Month Budget Summary

**Purpose**: Fetch budget summary for current month to display in Budget Summary Card

### Request

**Method**: `GET`
**Path**: `/api/budgets/current`
**Headers**:
```
X-Hass-User: {username}  (injected by nginx proxy)
Content-Type: application/json
```

**Query Parameters**: None

**Request Body**: None

### Response

**Success (200 OK)**:
```json
{
  "id": 123,
  "month": 12,
  "year": 2025,
  "amount": 5000.00,
  "spent": 3200.50,
  "remaining": 1799.50,
  "spendingPercentage": 64.01,
  "categoryId": null,
  "categoryName": null,
  "createdBy": "john",
  "createdAt": "2025-12-01T10:30:00",
  "updatedAt": "2025-12-15T14:22:00"
}
```

**Not Found (404)**:
```json
{
  "timestamp": "2025-12-22T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "No budget found for current month",
  "path": "/api/budgets/current"
}
```

**Error (500)**:
```json
{
  "timestamp": "2025-12-22T10:15:30",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch budget",
  "path": "/api/budgets/current"
}
```

### Frontend Handling

**Success**: Display budget summary in BudgetSummaryCard
**404**: Show empty state "Create your first budget for December 2025"
**500**: Show error alert with retry button

---

## Endpoint 2: Get Recent Expenses

**Purpose**: Fetch 5 most recent expenses to display in Recent Activity Card

### Request

**Method**: `GET`
**Path**: `/api/expenses`
**Headers**:
```
X-Hass-User: {username}  (injected by nginx proxy)
Content-Type: application/json
```

**Query Parameters**:
```
sort=expenseDate,desc  (sort by date descending)
page=0                  (first page)
size=5                  (limit to 5 results)
```

**Full URL**: `/api/expenses?sort=expenseDate,desc&page=0&size=5`

**Request Body**: None

### Response

**Success (200 OK)**:
```json
{
  "content": [
    {
      "id": 456,
      "amount": 45.99,
      "description": "Grocery shopping",
      "expenseDate": "2025-12-21",
      "budgetId": 123,
      "categoryId": 10,
      "categoryName": "Groceries",
      "categoryIcon": "🛒",
      "createdBy": "jane",
      "createdAt": "2025-12-21T18:30:00",
      "updatedAt": "2025-12-21T18:30:00",
      "version": 1,
      "warnings": null
    },
    {
      "id": 455,
      "amount": 12.50,
      "description": "Coffee",
      "expenseDate": "2025-12-21",
      "budgetId": 123,
      "categoryId": 11,
      "categoryName": "Dining",
      "categoryIcon": "🍽️",
      "createdBy": "john",
      "createdAt": "2025-12-21T09:15:00",
      "updatedAt": "2025-12-21T09:15:00",
      "version": 1,
      "warnings": null
    }
    // ... 3 more expenses
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 247,
  "totalPages": 50,
  "last": false,
  "size": 5,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "numberOfElements": 5,
  "first": true,
  "empty": false
}
```

**Empty Response (200 OK - No Expenses)**:
```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5,
    "offset": 0,
    "paged": true
  },
  "totalElements": 0,
  "totalPages": 0,
  "last": true,
  "size": 5,
  "number": 0,
  "numberOfElements": 0,
  "first": true,
  "empty": true
}
```

**Error (500)**:
```json
{
  "timestamp": "2025-12-22T10:15:30",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to fetch expenses",
  "path": "/api/expenses"
}
```

### Frontend Handling

**Success (with data)**: Display expenses in RecentActivityCard
**Success (empty)**: Show empty state "No expenses recorded yet"
**500**: Show error alert with retry button

**Data Extraction**:
```typescript
// Extract expenses array from paginated response
const expenses = response.data.content;

// Use only first 5 (already limited by size=5 parameter)
const recentExpenses = expenses.slice(0, 5);
```

---

## Endpoint 3: Get System Health

**Purpose**: Check backend connection status for System Status section

### Request

**Method**: `GET`
**Path**: `/api/health`
**Headers**:
```
Content-Type: application/json
```

**Query Parameters**: None
**Request Body**: None

### Response

**Success (200 OK)**:
```json
{
  "status": "UP",
  "service": "home-budget-backend",
  "version": "1.0.0-SNAPSHOT"
}
```

**Error (Any non-200 status)**:
Connection failed - backend unavailable

### Frontend Handling

**Success**: Show green check "Backend Connected"
**Error**: Show red X "Backend Connection Failed"

---

## Authentication

### X-Hass-User Header

**Source**: Injected by Home Assistant nginx proxy
**Value**: Username of logged-in Home Assistant user
**Backend Behavior**:
- Reads header to identify current user
- Uses for `createdBy` fields
- No additional authentication required (trusted proxy)

**Frontend Note**:
In development mode, header can be set via environment variable:
```typescript
// In api.ts interceptor
if (process.env.NODE_ENV === 'development') {
  const testUser = process.env.NEXT_PUBLIC_TEST_USER;
  if (testUser) {
    config.headers['X-Hass-User'] = testUser;
  }
}
```

---

## Error Handling Standards

### HTTP Status Codes

- **200 OK**: Successful request
- **404 Not Found**: Resource doesn't exist (e.g., no current month budget)
- **500 Internal Server Error**: Backend error

### Frontend Error Recovery

**Network Errors**:
```typescript
try {
  const data = await apiCall();
  // Success handling
} catch (err) {
  if (err.response) {
    // Server responded with error status
    setError(err.response.data.message || 'Failed to load data');
  } else if (err.request) {
    // Request made but no response (network issue)
    setError('Backend is unavailable');
  } else {
    // Something else went wrong
    setError('An unexpected error occurred');
  }
}
```

**Retry Mechanism**:
- All widgets provide "Retry" button on error
- Retry calls same endpoint again
- No automatic retry (user-initiated only)

---

## Performance Expectations

### Response Times (Target)

- **GET /api/budgets/current**: <100ms (simple query, indexed)
- **GET /api/expenses**: <150ms (sorted query with limit, indexed)
- **GET /api/health**: <50ms (no database query)

**Total Homepage Load**: <200ms for all API calls (parallel requests)

### Rate Limiting

**Not implemented**: Private home network, low user count (2-10 household members)

---

## API Versioning

**Current Version**: v1 (implicit in path `/api/*`)

**Breaking Changes**: None in this feature (reuses existing endpoints)

**Future Versioning**: If endpoints change incompatibly, use `/api/v2/*` paths

---

## Testing Contracts

### Manual Testing

**Budget Summary**:
```bash
# Test current month budget exists
curl -H "X-Hass-User: testuser" http://localhost:8081/api/budgets/current

# Expected: 200 OK with budget data
```

**Recent Expenses**:
```bash
# Test recent expenses retrieval
curl -H "X-Hass-User: testuser" \
  "http://localhost:8081/api/expenses?sort=expenseDate,desc&page=0&size=5"

# Expected: 200 OK with paginated expense array
```

**System Health**:
```bash
# Test backend health
curl http://localhost:8081/api/health

# Expected: 200 OK with status "UP"
```

---

## Contract Validation

**Status**: ✅ Validated against existing backend implementation
**Breaking Changes**: None
**New Endpoints Required**: None
**Backend Changes Required**: None

**Ready for**: Quickstart guide generation
