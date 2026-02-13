# API Contracts: Backend Test Coverage Improvement

**Feature**: 014-backend-test-coverage

## Overview

This feature does not introduce any new API endpoints or modify existing contracts. All existing endpoints will be exercised through new and updated tests.

## Existing Endpoints Under Test

### BudgetController (`/api/budgets`)
- POST `/api/budgets` - Create budget
- GET `/api/budgets` - List all budgets
- GET `/api/budgets/{id}` - Get budget by ID
- PUT `/api/budgets/{id}` - Update budget
- DELETE `/api/budgets/{id}` - Delete budget
- GET `/api/budgets/current` - Current month budget
- GET `/api/budgets/monthly-summary` - Monthly summary
- GET `/api/budgets/validation` - Budget validation hints
- GET `/api/budgets/yearly` - Yearly budget view

### CategoryController (`/api/categories`)
- POST `/api/categories` - Create category
- GET `/api/categories` - List all categories
- GET `/api/categories/{id}` - Get category by ID
- PUT `/api/categories/{id}` - Update category
- DELETE `/api/categories/{id}` - Delete category
- GET `/api/categories/hierarchy` - Category tree
- GET `/api/categories/{id}/expense-count` - Expense count

### ExpenseController (`/api/expenses`)
- POST `/api/expenses` - Create expense (JSON)
- POST `/api/expenses` - Create expense (multipart with files)
- GET `/api/expenses` - List expenses with filters
- GET `/api/expenses/list` - Paginated expense list
- GET `/api/expenses/years` - Distinct expense years
- GET `/api/expenses/creators` - Distinct creators
- GET `/api/expenses/{id}` - Get expense by ID
- PUT `/api/expenses/{id}` - Update expense (JSON)
- PUT `/api/expenses/{id}` - Update expense (multipart)
- DELETE `/api/expenses/{id}` - Delete expense

### ExpenseInputJobController (`/api/expense-input-jobs`)
- POST `/api/expense-input-jobs` - Create jobs (multipart upload)
- GET `/api/expense-input-jobs` - List all jobs
- PATCH `/api/expense-input-jobs/{jobId}/temporary-record` - Update temp record
- POST `/api/expense-input-jobs/confirm` - Confirm jobs
- DELETE `/api/expense-input-jobs` - Delete jobs

### HealthController (`/api`)
- GET `/api/health` - Health check
- GET `/api/info` - Service info
