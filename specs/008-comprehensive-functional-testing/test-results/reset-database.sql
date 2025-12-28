-- Database Reset Script for Comprehensive Functional Testing
-- Purpose: Reset MySQL database to clean state before each major test suite
-- Feature: 008-comprehensive-functional-testing
-- Date: 2025-12-28

USE budget_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Truncate all tables to remove test data while preserving schema
TRUNCATE TABLE expenses;
TRUNCATE TABLE budgets;
TRUNCATE TABLE categories;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify reset successful
SELECT 'Database reset complete' AS status;
SELECT COUNT(*) AS expenses_count FROM expenses;
SELECT COUNT(*) AS budgets_count FROM budgets;
SELECT COUNT(*) AS categories_count FROM categories;

-- Expected output: All counts should be 0
