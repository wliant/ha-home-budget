# Data Model: Comprehensive Functional Testing

**Feature**: 008-comprehensive-functional-testing
**Date**: 2025-12-28
**Phase**: Phase 1 - Design

## Overview

This document defines the data entities used for manual functional testing documentation. These are NOT application data models (those exist in features 001-007), but rather the structure of test artifacts created during test execution.

## Entities

### Test Suite

A collection of related test cases organized by feature area.

**Attributes**:
- `suite_id` (string, unique): Unique identifier (e.g., "category-management", "budget-management")
- `name` (string, required): Human-readable name (e.g., "Category Management Testing")
- `description` (text, required): Purpose and scope of test suite
- `user_stories` (list, required): User story IDs covered (e.g., [US1, US6])
- `functional_requirements` (list, required): FR IDs covered (e.g., [FR-006, FR-007, FR-008, FR-009, FR-010])
- `prerequisites` (text, optional): Setup steps required before execution
- `database_reset_required` (boolean, required): Whether clean database state needed
- `execution_order` (integer, required): Sequence number for dependency ordering

**Relationships**:
- Has many `Test Case` entities
- References `User Story` entities (from spec.md)
- References `Functional Requirement` entities (from spec.md)

**Validation Rules**:
- `suite_id` must be unique across all test suites
- `execution_order` must be positive integer
- `functional_requirements` list cannot be empty

**States**:
- Not Started: No test cases executed
- In Progress: Some test cases executed
- Completed: All test cases have results
- Blocked: Cannot proceed due to dependencies

---

### Test Case

An individual test scenario with preconditions, actions, and expected results.

**Attributes**:
- `test_id` (string, unique): Unique identifier (e.g., "TC-001", "TC-042")
- `suite_id` (string, required): Parent test suite identifier
- `title` (string, required): Brief description (e.g., "Create root category without parent")
- `preconditions` (text, required): State required before test (e.g., "Database reset, logged in as alice")
- `test_steps` (list of text, required): Actions to perform (numbered steps)
- `expected_outcome` (text, required): What should happen if test passes
- `actual_outcome` (text, filled during execution): What actually happened
- `status` (enum, filled during execution): PASS | FAIL | BLOCKED | SKIPPED | NOT_RUN
- `tested_by` (string, optional): Name of tester who executed
- `tested_date` (date, optional): When test was executed
- `browser` (string, optional): Browser used (e.g., "Chrome 120", "Firefox 121")
- `notes` (text, optional): Additional observations, workarounds, or context
- `defect_ids` (list, optional): Linked defect IDs if test failed (e.g., [DEF-001])

**Relationships**:
- Belongs to one `Test Suite`
- References zero or more `Defect` entities
- Maps to one or more `Functional Requirement` entities (from spec.md)
- Maps to one `Acceptance Scenario` (from spec.md user stories)

**Validation Rules**:
- `test_id` must be unique across all test cases
- `status` must be one of the valid enum values
- If `status` = FAIL, `defect_ids` list should not be empty
- If `status` != NOT_RUN, `actual_outcome` must be filled

**States**:
- NOT_RUN: Initial state, no execution yet
- PASS: Expected outcome matched actual outcome
- FAIL: Expected outcome did not match actual outcome
- BLOCKED: Cannot execute due to prerequisite failure or missing dependency
- SKIPPED: Intentionally not executed (e.g., known issue, out of scope)

---

### Test Result

Outcome of test case execution (embedded within Test Case in Markdown format).

**Attributes**:
- `test_id` (string, required): Reference to test case
- `execution_timestamp` (datetime, required): When test was run
- `status` (enum, required): PASS | FAIL | BLOCKED | SKIPPED
- `actual_outcome` (text, required): What happened during test execution
- `expected_outcome` (text, required): Reference to test case expected outcome
- `delta` (text, optional): Difference between expected and actual
- `notes` (text, optional): Observations, screenshots, logs
- `environment` (object, optional): Browser version, screen size, network conditions

**Relationships**:
- Belongs to one `Test Case`
- May reference one or more `Defect` entities

**Validation Rules**:
- If `status` = PASS, `expected_outcome` should equal `actual_outcome`
- If `status` = FAIL, `delta` should describe the difference
- `execution_timestamp` must be valid ISO 8601 datetime

---

### Defect

Issue discovered during testing.

**Attributes**:
- `defect_id` (string, unique): Unique identifier (e.g., "DEF-001", "DEF-042")
- `title` (string, required): Brief summary (e.g., "Budget creation crashes with empty category")
- `severity` (enum, required): CRITICAL | HIGH | MEDIUM | LOW
- `status` (enum, required): OPEN | IN_PROGRESS | FIXED | VERIFIED | CLOSED | WONT_FIX
- `description` (text, required): Detailed explanation of the issue
- `steps_to_reproduce` (list of text, required): How to reproduce (Given/When/Then format)
- `expected_behavior` (text, required): What should happen
- `actual_behavior` (text, required): What actually happens
- `affected_features` (list, required): Feature areas impacted (e.g., ["Budget Management", "Dashboard"])
- `functional_requirements` (list, required): FR IDs violated (e.g., [FR-011, FR-014])
- `test_ids` (list, required): Test cases that found this defect (e.g., [TC-015])
- `browser` (string, optional): Browser where defect occurs (or "All browsers")
- `screenshots` (list of file paths, optional): Evidence attachments
- `network_logs` (text, optional): Relevant API requests/responses
- `discovered_by` (string, required): Tester who found the defect
- `discovered_date` (date, required): When defect was found
- `notes` (text, optional): Additional context, workarounds, related defects

**Relationships**:
- Referenced by one or more `Test Case` entities
- References one or more `Functional Requirement` entities (from spec.md)
- May reference other `Defect` entities (duplicates, related issues)

**Validation Rules**:
- `defect_id` must be unique across all defects
- `severity` must be CRITICAL | HIGH | MEDIUM | LOW
- `status` must be valid enum value
- `steps_to_reproduce` list must have at least 1 step
- `affected_features` list cannot be empty
- `functional_requirements` list cannot be empty

**Severity Definitions**:
- **CRITICAL**: System crash, data loss, security vulnerability, complete feature failure
- **HIGH**: Major functionality broken, no workaround available, significantly impacts user
- **MEDIUM**: Functionality works with workaround, minor user impact, edge case failure
- **LOW**: Cosmetic issue, minor UI inconsistency, no functional impact

**States**:
- OPEN: Defect discovered, not yet assigned or being worked on
- IN_PROGRESS: Being investigated or fixed
- FIXED: Code changes made, awaiting verification
- VERIFIED: Fix confirmed by retesting
- CLOSED: Resolved and verified, no further action
- WONT_FIX: Acknowledged but will not be addressed

---

### Test Coverage

Metrics tracking which requirements and user stories have been validated.

**Attributes**:
- `functional_requirement_id` (string, unique): FR ID (e.g., "FR-001")
- `requirement_text` (text, required): Full requirement description
- `test_case_ids` (list, required): Test cases validating this requirement
- `coverage_status` (enum, required): COVERED | PARTIAL | NOT_COVERED
- `user_story_id` (string, required): User story this requirement belongs to (e.g., "US1")
- `priority` (enum, required): P1 | P2 | P3 (from user story priority)
- `pass_count` (integer, default 0): Number of test cases passed
- `fail_count` (integer, default 0): Number of test cases failed
- `total_test_cases` (integer, required): Total test cases for this requirement

**Relationships**:
- References multiple `Test Case` entities
- References one `Functional Requirement` (from spec.md)
- References one `User Story` (from spec.md)

**Validation Rules**:
- `functional_requirement_id` must match an FR ID from spec.md
- `pass_count` + `fail_count` <= `total_test_cases`
- If `coverage_status` = COVERED, `total_test_cases` must be > 0
- If `total_test_cases` = 0, `coverage_status` must be NOT_COVERED

**Coverage Status Definitions**:
- **COVERED**: All test cases for this requirement executed and passed
- **PARTIAL**: Some test cases executed, or some failed
- **NOT_COVERED**: No test cases executed for this requirement

---

## Entity Relationships Diagram

```
[User Story] ----< [Functional Requirement] >---- [Test Coverage]
     |                        |                          |
     |                        |                          |
     v                        v                          v
[Test Suite] ----< [Test Case] >---- [Test Result]
                       |
                       |
                       v
                   [Defect]
```

**Relationship Details**:
- User Story (1) → Functional Requirements (many)
- Functional Requirement (1) → Test Cases (many)
- Test Suite (1) → Test Cases (many)
- Test Case (1) → Test Result (1)
- Test Case (many) → Defect (many)
- Functional Requirement (1) → Test Coverage (1)

## File-Based Storage

These entities are stored in Markdown files (not a database):

### Test Suites & Test Cases

Stored in separate files per suite:
- `test-results/category-management.md`
- `test-results/budget-management.md`
- `test-results/expense-recording.md`
- `test-results/dashboard.md`
- `test-results/integration.md`
- `test-results/ui-navigation.md`
- `test-results/date-handling.md`

**Format**:
```markdown
# Test Suite: [Name]

**Suite ID**: [suite_id]
**User Stories**: [US#, US#]
**Functional Requirements**: [FR-###, FR-###]
**Database Reset Required**: Yes/No
**Execution Order**: #

## Prerequisites
[Setup steps]

## Test Cases

| Test ID | Title | Status | Expected | Actual | Notes |
|---------|-------|--------|----------|--------|-------|
| TC-001  | [title] | PASS  | [expected] | [actual] | [notes] |
| TC-002  | [title] | FAIL  | [expected] | [actual] | See DEF-001 |

## Test Case Details

### TC-001: [Title]

**Preconditions**: [state before test]

**Steps**:
1. [action]
2. [action]
3. [action]

**Expected Outcome**: [what should happen]

**Actual Outcome**: [what happened]

**Status**: PASS

**Tested By**: [name]
**Tested Date**: YYYY-MM-DD
**Browser**: [browser version]

**Notes**: [observations]
```

### Defects

Stored in consolidated file:
- `test-results/defects.md`

**Format**:
```markdown
# Defect Log

| Defect ID | Severity | Title | Status | Affected Features |
|-----------|----------|-------|--------|-------------------|
| DEF-001   | HIGH     | [title] | OPEN | Budget Management |

## Defect Details

### DEF-001: [Title]

**Severity**: HIGH
**Status**: OPEN
**Affected Features**: Budget Management, Dashboard
**Functional Requirements**: FR-011, FR-014
**Test Cases**: TC-015, TC-022

**Description**: [detailed explanation]

**Steps to Reproduce**:
1. **Given** [precondition]
2. **When** [action]
3. **Then** [expected but didn't happen]

**Expected Behavior**: [what should happen]

**Actual Behavior**: [what actually happens]

**Browser**: Chrome 120 (also reproduced in Firefox 121)

**Discovered By**: [tester name]
**Discovered Date**: YYYY-MM-DD

**Notes**: [workarounds, additional context]
```

### Test Coverage

Stored in summary file:
- `test-results/coverage-summary.md`

**Format**:
```markdown
# Test Coverage Summary

| FR ID   | Requirement | User Story | Priority | Test Cases | Pass | Fail | Coverage |
|---------|-------------|------------|----------|------------|------|------|----------|
| FR-001  | [text]      | US1        | P1       | 5          | 5    | 0    | COVERED  |
| FR-002  | [text]      | US2        | P1       | 3          | 2    | 1    | PARTIAL  |
```

## Data Lifecycle

### Test Execution Flow

1. **Setup Phase**:
   - Create test suite Markdown file from template
   - Document prerequisites and database reset procedure
   - Create test case entries with expected outcomes

2. **Execution Phase**:
   - Reset database (if required)
   - Execute test steps
   - Record actual outcomes in test case table
   - Note status (PASS/FAIL/BLOCKED/SKIPPED)

3. **Defect Creation**:
   - For each FAIL result, create defect entry
   - Assign severity level
   - Document reproduction steps
   - Link test case(s) to defect

4. **Coverage Analysis**:
   - After test suite completion, update coverage summary
   - Calculate pass/fail counts per functional requirement
   - Identify gaps (NOT_COVERED requirements)

5. **Reporting**:
   - Generate consolidated view of all test results
   - Summary statistics (total tests, pass rate, defect counts by severity)
   - Risk assessment based on P1/P2 failures

## Traceability

Every test case must trace back to:
1. **Functional Requirement** (FR-###) from spec.md
2. **Acceptance Scenario** from spec.md user stories
3. **User Story** (US#) from spec.md

This ensures:
- All requirements have test coverage
- All tests validate specified behavior
- Gaps in testing are visible

## Validation Checklist

Before considering testing complete:
- [ ] All 43 functional requirements have at least 1 test case
- [ ] All P1 user stories have test cases executed
- [ ] All test cases have status (not NOT_RUN)
- [ ] All FAIL test cases have linked defects
- [ ] All defects have severity assigned
- [ ] All defects have reproduction steps
- [ ] Coverage summary shows COVERED or PARTIAL for all requirements
- [ ] No CRITICAL or HIGH severity defects in OPEN status (or justified)
