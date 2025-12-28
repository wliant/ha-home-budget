# Test Suite Template

**Purpose**: Standard format for all test suite documentation files

**Location**: `test-results/{suite-id}.md`

**Version**: 1.0.0

## Template

```markdown
# Test Suite: {Suite Name}

**Suite ID**: {suite-id}
**User Stories**: {US#, US#, ...}
**Functional Requirements**: {FR-###, FR-###, ...}
**Database Reset Required**: Yes | No
**Execution Order**: {integer}
**Status**: Not Started | In Progress | Completed | Blocked

## Overview

{Brief description of what this test suite validates}

## Prerequisites

### Environment Setup
- Application deployed and accessible
- {Browser name} {version} installed
- Browser DevTools available

### Database State
- {If Database Reset Required = Yes}: Clean database (zero records in all tables)
- {If Database Reset Required = No}: Existing data from previous test suite

### Test Data Preparation
1. {Step to create test data if needed}
2. {Step to configure test users if needed}

### User Authentication
- Simulated user: {username} (via X-Hass-User header)
- Alternative user for multi-user tests: {username2}

## Test Case Summary

| Test ID | Title | Priority | Status | Defects |
|---------|-------|----------|--------|---------|
| TC-### | {Brief title} | P# | NOT_RUN | - |
| TC-### | {Brief title} | P# | PASS | - |
| TC-### | {Brief title} | P# | FAIL | DEF-### |

**Summary Statistics**:
- Total Test Cases: {count}
- Passed: {count}
- Failed: {count}
- Blocked: {count}
- Skipped: {count}
- Not Run: {count}
- **Pass Rate**: {percentage}%

## Test Case Details

### TC-###: {Test Case Title}

**Functional Requirement**: FR-###
**User Story**: US#
**Acceptance Scenario**: {Reference to spec.md acceptance scenario}

**Priority**: P1 | P2 | P3

**Preconditions**:
- {State that must exist before test}
- {Logged in as: username}
- {Database state: description}

**Test Steps**:
1. {Action to perform}
2. {Action to perform}
3. {Action to perform}

**Expected Outcome**:
{What should happen if the application works correctly}

**Actual Outcome**:
{What actually happened during test execution - filled after execution}

**Status**: NOT_RUN | PASS | FAIL | BLOCKED | SKIPPED

**Test Execution Details**:
- **Tested By**: {Tester name}
- **Tested Date**: YYYY-MM-DD
- **Browser**: {Browser name} {version}
- **Screen Size**: {e.g., 1920x1080, 375x667 (mobile)}
- **Network Conditions**: {Normal, Slow 3G, Offline, etc.}

**Evidence**:
- {Screenshot filename if applicable}
- {Network log snippet if applicable}

**Notes**:
{Additional observations, workarounds, or context}

**Linked Defects**:
- DEF-### (if status = FAIL)

---

{Repeat ### TC-###: section for each test case}

## Suite Completion Notes

**Execution Summary**:
- Started: YYYY-MM-DD HH:MM
- Completed: YYYY-MM-DD HH:MM
- Duration: {hours/minutes}
- Tester(s): {Name(s)}

**Overall Assessment**:
{Summary of test suite results, key findings, risks identified}

**Blockers Encountered**:
- {Any blocking issues that prevented test execution}

**Recommendations**:
- {Suggested next steps based on test results}

**Defects Found**: {count} ({CRITICAL: #, HIGH: #, MEDIUM: #, LOW: #})

**Next Test Suite**: {suite-id} ({suite name})
```

## Usage Instructions

1. **Create New Test Suite File**:
   - Copy this template
   - Replace `{suite-id}` with actual suite ID (e.g., category-management)
   - Fill in Overview, Prerequisites, User Stories, Functional Requirements

2. **Before Execution**:
   - Create test case entries with Expected Outcomes filled
   - Set all Status to NOT_RUN
   - Verify Prerequisites are achievable

3. **During Execution**:
   - Execute test steps
   - Record Actual Outcome
   - Update Status (PASS/FAIL/BLOCKED/SKIPPED)
   - Capture Evidence (screenshots, logs)
   - Fill Test Execution Details

4. **After Test Case Failure**:
   - Create defect entry in `defects.md`
   - Link defect ID in test case "Linked Defects"
   - Add defect ID to Test Case Summary table

5. **After Suite Completion**:
   - Calculate Summary Statistics
   - Fill Suite Completion Notes
   - Update Overall Assessment
   - List Defects Found with severity counts

## Validation Checklist

Before marking suite as Completed:
- [ ] All test cases have Status (none are NOT_RUN or blank)
- [ ] All FAIL test cases have linked defects
- [ ] All test cases have Tested By and Tested Date filled
- [ ] Summary Statistics calculated correctly
- [ ] Pass Rate calculated (passed / total * 100)
- [ ] Suite Completion Notes filled
- [ ] Defects logged in defects.md

## Example Test Case

### TC-042: Create Child Category with Parent Selection

**Functional Requirement**: FR-006
**User Story**: US6
**Acceptance Scenario**: Given I have category "Food", When I create "Groceries" with "Food" as parent, Then "Groceries" appears nested under "Food" in the hierarchy view

**Priority**: P2

**Preconditions**:
- Database reset completed (clean state)
- Logged in as: alice (X-Hass-User: alice)
- Parent category "Food" already created (TC-041 passed)

**Test Steps**:
1. Navigate to Categories page
2. Click "Add Category" button
3. Enter category name "Groceries"
4. Select "Food" from parent category dropdown
5. Click "Save" button
6. View categories list

**Expected Outcome**:
- "Groceries" category is created successfully
- Success message displayed: "Category created successfully"
- "Groceries" appears as child under "Food" in hierarchy view
- Indentation or visual indicator shows parent-child relationship

**Actual Outcome**:
- "Groceries" category created successfully
- Success message displayed correctly
- "Groceries" shows under "Food" with indentation in category list
- Parent-child relationship clearly visible

**Status**: PASS

**Test Execution Details**:
- **Tested By**: John Doe
- **Tested Date**: 2025-12-28
- **Browser**: Chrome 120.0.6099.109
- **Screen Size**: 1920x1080 (desktop)
- **Network Conditions**: Normal

**Evidence**:
- Screenshot: `screenshots/TC-042-category-hierarchy.png`

**Notes**:
Hierarchy display uses indentation (2 spaces per level). Very clear visual indication.

**Linked Defects**: None
