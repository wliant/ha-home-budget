# Test Contracts: Comprehensive Backend Test Suite

**Feature**: 010-backend-test-suite
**Date**: 2026-02-08

## Overview

This feature does not introduce new API endpoints. It tests the existing API contracts. This document lists the API endpoints and their expected behaviors as they will be verified by the E2E test suite.

## Budget API Contracts

### POST /api/budgets
- **Request Headers**: `X-Hass-User` (required), `Content-Type: application/json`
- **Request Body**: `{ "year": int, "month": int, "totalAmount": decimal, "description": string, "categoryId": long }`
- **Success**: 201 Created with BudgetDTO body
- **Errors**: 400 (validation), 409 (duplicate category/year/month)

### GET /api/budgets
- **Response**: 200 OK with `List<BudgetSummaryDTO>` ordered by year desc, month desc

### GET /api/budgets/{id}
- **Response**: 200 OK with BudgetSummaryDTO (includes expenses)
- **Errors**: 404 (not found)

### PUT /api/budgets/{id}
- **Request Body**: `{ "totalAmount": decimal, "description": string }`
- **Response**: 200 OK with updated BudgetDTO
- **Errors**: 404 (not found)

### DELETE /api/budgets/{id}
- **Response**: 204 No Content
- **Errors**: 404 (not found)

### GET /api/budgets/current
- **Response**: 200 OK with BudgetSummaryDTO for current month
- **Errors**: 404 (no budget for current month)

## Category API Contracts

### POST /api/categories
- **Request Headers**: `X-Hass-User` (required), `Content-Type: application/json`
- **Request Body**: `{ "name": string, "icon": string, "parentCategoryId": long? }`
- **Success**: 201 Created with CategoryDTO body
- **Errors**: 400 (validation), 409 (duplicate name)

### GET /api/categories
- **Response**: 200 OK with `List<CategoryDTO>` ordered by name asc

### GET /api/categories/{id}
- **Response**: 200 OK with CategoryDTO
- **Errors**: 404 (not found)

### PUT /api/categories/{id}
- **Request Body**: `{ "name": string, "icon": string, "parentCategoryId": long? }`
- **Response**: 200 OK with updated CategoryDTO
- **Errors**: 404 (not found), 409 (duplicate name, circular ref, hierarchy depth)

### DELETE /api/categories/{id}
- **Response**: 204 No Content
- **Errors**: 404 (not found), 409 (in use)

### GET /api/categories/hierarchy
- **Response**: 200 OK with `List<CategoryDTO>` with nested children

### GET /api/categories/{id}/expense-count
- **Response**: 200 OK with `long` count

## Expense API Contracts

### POST /api/expenses
- **Request Headers**: `X-Hass-User` (required), `Content-Type: application/json`
- **Request Body**: `{ "amount": decimal, "description": string, "expenseDate": date, "budgetId": long, "categoryId": long? }`
- **Success**: 201 Created with ExpenseDTO body (may include warnings)
- **Errors**: 400 (validation), 404 (budget/category not found)

### GET /api/expenses
- **Query Parameters**: `budgetId`, `categoryId`, `startDate`, `endDate`, `createdBy` (all optional)
- **Response**: 200 OK with `List<ExpenseDTO>`

### GET /api/expenses/{id}
- **Response**: 200 OK with ExpenseDTO
- **Errors**: 404 (not found)

### PUT /api/expenses/{id}
- **Request Body**: `{ "amount": decimal, "description": string, "expenseDate": date, "budgetId": long, "categoryId": long? }`
- **Response**: 200 OK with updated ExpenseDTO
- **Errors**: 404 (not found)

### DELETE /api/expenses/{id}
- **Response**: 204 No Content
- **Errors**: 404 (not found)

## Error Response Contract

All error responses follow:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": { "fieldName": "error message" },
  "timestamp": "2026-02-08T12:00:00"
}
```

- `status`: HTTP status code (integer)
- `message`: Human-readable error description (string)
- `errors`: Field-level error details (object, nullable)
- `timestamp`: ISO-8601 timestamp (string)
