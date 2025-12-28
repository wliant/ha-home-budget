# Research & Technical Decisions: Comprehensive Functional Testing

**Feature**: 008-comprehensive-functional-testing
**Date**: 2025-12-28
**Phase**: Phase 0 - Research

## Overview

This document captures technical decisions and best practices for comprehensive manual functional testing of the home budget and expense tracking application. Since this is a testing documentation project (not code implementation), research focuses on test organization, documentation standards, and manual testing methodologies.

## Decision 1: Test Documentation Format

**Decision**: Use Markdown tables for test results with columns: Test Case ID, Status, Expected, Actual, Notes

**Rationale**:
- Human-readable and easy to edit during manual testing
- Version control friendly (Git diff shows changes clearly)
- Can be converted to other formats (HTML, PDF) if needed
- Supports inline code blocks for error messages and technical details
- Aligns with existing Specify workflow documentation standards

**Alternatives Considered**:
- **Spreadsheet (CSV/Excel)**: Rejected because requires separate tool, not version control friendly, harder to include formatted technical details
- **Test management tool (Jira, TestRail)**: Rejected because adds tooling dependency, out of scope for manual testing focus
- **Plain text logs**: Rejected because lacks structure for systematic review and analysis

**Example Format**:
```markdown
| Test ID | Status | Expected Outcome | Actual Outcome | Notes |
|---------|--------|------------------|----------------|-------|
| TC-001  | PASS   | Category created | Category created successfully | - |
| TC-002  | FAIL   | Error message shown | Application crashed | Defect #001 |
```

## Decision 2: Database Reset Strategy

**Decision**: Reset MySQL database to clean state before each major test suite (Category Management, Budget Management, Expense Recording, Dashboard, Integration)

**Rationale**:
- Ensures test isolation - tests don't depend on execution order within a suite
- Balances reproducibility with execution efficiency (not resetting per individual test case)
- Aligns with clarification answer from `/speckit.clarify` (Option B)
- Prevents test data accumulation that could mask bugs
- Allows parallel test execution in future if needed

**Alternatives Considered**:
- **Additive testing (no resets)**: Rejected because tests become interdependent, harder to debug failures, can't run subsets
- **Reset per individual test case**: Rejected because too time-consuming for manual testing, may require scripting infrastructure
- **Pre-seeded database**: Rejected because doesn't test data creation flows, can't validate fresh installation scenarios

**Implementation**:
- Document SQL script or procedure to reset database in quickstart.md
- Include steps to restore clean state in each test suite's setup section
- Verify reset successful by checking for zero records in all tables

## Decision 3: Defect Severity Classification

**Decision**: Use 4-level severity system: Critical / High / Medium / Low

**Rationale**:
- Industry standard that development teams understand immediately
- Clear distinction between blocking issues (Critical/High) and non-blocking (Medium/Low)
- Aligns with clarification answer from `/speckit.clarify`
- Sufficient granularity without excessive complexity

**Severity Definitions**:
- **Critical**: System crash, data loss, security vulnerability, complete feature failure
- **High**: Major functionality broken, no workaround available, significantly impacts user
- **Medium**: Functionality works with workaround, minor user impact, edge case failure
- **Low**: Cosmetic issue, minor UI inconsistency, no functional impact

**Alternatives Considered**:
- **Blocker/Major/Minor (3 levels)**: Rejected because insufficient granularity for distinguishing High vs Medium issues
- **P1/P2/P3/P4 priorities**: Rejected because conflicts with user story priority labels, less intuitive severity meaning
- **Numeric scale (1-5)**: Rejected because requires remembering which end is most severe, less self-documenting

## Decision 4: Multi-User Testing Approach

**Decision**: Use browser DevTools or curl to manually set X-Hass-User header for simulating different users

**Rationale**:
- Avoids need for actual Home Assistant installation in test environment
- Allows quick switching between users (alice, bob, etc.)
- Tests the application's multi-user logic without infrastructure dependencies
- Browser DevTools available in all major browsers

**Alternatives Considered**:
- **Full Home Assistant setup**: Rejected because adds complexity, slower test execution, not necessary for validating X-Hass-User handling
- **Modify application code for test mode**: Rejected because violates "no code changes" principle for testing documentation project
- **Multiple browser profiles**: Rejected because doesn't actually change X-Hass-User header, only simulates separate sessions

**Implementation Methods**:
1. **Browser DevTools**: Network tab → right-click request → Edit and Resend → modify X-Hass-User header
2. **Browser Extension**: ModHeader, Simple Modify Headers (add X-Hass-User to all requests)
3. **curl commands**: For API testing: `curl -H "X-Hass-User: alice" http://localhost:8080/api/budgets`

## Decision 5: Browser Version Testing Scope

**Decision**: Test on latest stable version only of each major browser (Chrome, Firefox, Safari, Edge, iOS Safari, Chrome Mobile)

**Rationale**:
- Home network users typically have auto-updates enabled
- Testing older versions has low ROI for private home deployment
- Aligns with clarification answer from `/speckit.clarify` (Option B)
- Keeps testing scope manageable for manual execution
- Covers all major rendering engines: Chromium (Chrome/Edge), Gecko (Firefox), WebKit (Safari)

**Browser List**:
- Chrome (latest stable) - Chromium engine, Windows/Mac/Linux
- Firefox (latest stable) - Gecko engine, Windows/Mac/Linux
- Safari (latest stable) - WebKit engine, macOS
- Edge (latest stable) - Chromium engine, Windows
- iOS Safari (latest stable) - WebKit engine, iOS mobile
- Chrome Mobile (latest stable) - Chromium engine, Android mobile

**Alternatives Considered**:
- **Latest 2 versions of each browser**: Rejected because manual testing overhead, diminishing returns for home network deployment
- **Last 12 months of versions**: Rejected because version tracking complexity, most users auto-update
- **Home Assistant compatibility matrix**: Rejected because unnecessary - testing application compatibility, not Home Assistant itself

## Decision 6: Edge Case Testing Philosophy

**Decision**: Validate graceful degradation - system shows error messages but remains functional without crashes or data corruption

**Rationale**:
- Aligns with clarification answer from `/speckit.clarify` (Option A)
- Matches production best practices (resilience over strict blocking)
- Better user experience than application crashes
- Aligns with Success Criteria SC-009 (graceful handling on 100% of pages)

**Expected Behaviors**:
- Invalid inputs → validation error messages, form remains usable
- Backend unavailable → error state UI, no application crash
- Concurrent operations → one succeeds with conflict message to other, no data corruption
- Missing authentication → clear error prompt, no security bypass

**Alternatives Considered**:
- **Strict error prevention**: Rejected because may require too much client-side logic, doesn't test backend resilience
- **Mixed approach**: Rejected because inconsistent user experience, harder to document expected behaviors
- **Document actual behavior**: Rejected because fails to validate against quality standards

## Decision 7: Test Result File Organization

**Decision**: Separate Markdown file per major test category (Category Management, Budget Management, Expense Recording, Dashboard, Integration, UI/Navigation, Date Handling) plus consolidated defects.md

**Rationale**:
- Logical separation makes files easier to navigate
- Each file size remains manageable (not one giant file)
- Enables parallel testing (different testers can work on different categories)
- Defect consolidation prevents duplicate tracking across files

**File Structure**:
```
test-results/
├── category-management.md    # FR-006 to FR-010 (5 requirements)
├── budget-management.md       # FR-011 to FR-015 (5 requirements)
├── expense-recording.md       # FR-016 to FR-022 (7 requirements)
├── dashboard.md               # FR-023 to FR-028 (6 requirements)
├── integration.md             # FR-039 to FR-043 (5 requirements)
├── ui-navigation.md           # FR-034 to FR-038 (5 requirements) + edge cases
├── date-handling.md           # Date-specific acceptance scenarios
└── defects.md                 # Consolidated defect log (all severities)
```

**Alternatives Considered**:
- **Single test-results.md file**: Rejected because would be very long (43 requirements + edge cases), hard to navigate
- **One file per user story**: Rejected because some requirements span multiple stories, creates fragmentation
- **One file per functional requirement**: Rejected because 43+ files is excessive overhead for manual testing

## Decision 8: Test Execution Order

**Decision**: Execute test suites in dependency order: Integration → Category → Budget → Expense → Dashboard → UI/Navigation → Date Handling

**Rationale**:
- **Integration first**: Validates API communication works before testing features
- **Category before Budget**: Budgets depend on categories existing
- **Budget before Expense**: Expenses may depend on budgets for validation
- **Dashboard after data creation**: Dashboard displays budgets/expenses, needs data to test
- **UI/Navigation anytime**: Can run in parallel with others (marked [P])
- **Date Handling anytime**: Can run in parallel with others (marked [P])

**Database Reset Points**:
1. Before Integration tests (verify API contracts)
2. Before Category tests (test fresh category creation)
3. Before Budget tests (test budget creation with clean categories)
4. Before Expense tests (test expense recording)
5. Before Dashboard tests (test empty state, then populated state)

## Technology Stack Summary

**No New Technologies Introduced**

This testing feature uses only existing stack components:

- **Testing Approach**: Manual functional testing (human tester executes scenarios)
- **Documentation Format**: Markdown (GitHub Flavored Markdown)
- **Test Data Storage**: Existing MySQL 8.0 database
- **Testing Tools**: Browser DevTools (built into Chrome/Firefox/Safari/Edge)
- **Browser Targets**: Latest stable versions (auto-updated by users)
- **Authentication Simulation**: HTTP header manipulation via DevTools or curl

**Existing Stack Being Tested**:
- Next.js frontend (already implemented in features 001-007)
- Spring Boot Java 17 backend (already implemented in features 001-007)
- MySQL 8.0 database (already implemented in feature 001)
- Home Assistant authentication integration (already implemented in feature 003)

## Best Practices Applied

### Test Case Documentation
- Each test case has unique ID (TC-XXX format)
- Expected outcome written before test execution
- Actual outcome recorded during execution
- Notes field captures defect references, observations, browser-specific behaviors

### Defect Tracking
- Each defect has unique ID (DEF-XXX format)
- Severity assigned using Critical/High/Medium/Low
- Reproduction steps documented (Given/When/Then format)
- Affected features/requirements referenced
- Screenshots or network logs attached when relevant

### Test Data Management
- Use realistic data (category names like "Food", "Groceries" not "Test123")
- Use consistent test user names (alice, bob) for multi-user scenarios
- Document test data creation steps in each test suite setup
- Reset database before each major suite to ensure reproducibility

### Browser Testing
- Test responsive layouts using browser DevTools device emulation
- Verify mobile breakpoints (320px, 768px, 1024px)
- Test keyboard navigation (Tab, Enter, Escape)
- Monitor Network tab for API calls and response times

### Performance Validation
- Use browser Performance tab or Network tab to measure load times
- Validate < 2 second API response (Success Criterion SC-003)
- Validate < 3 second dashboard load (Success Criterion SC-006)
- Note any performance degradation with large data sets (10+ categories with 5 children)

## Open Questions

**None.** All clarifications completed in `/speckit.clarify` phase.

## References

- Feature Specification: [specs/008-comprehensive-functional-testing/spec.md](./spec.md)
- Clarifications: See spec.md "Clarifications" section (5 questions answered)
- Constitution: [.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- Existing Features: specs/001 through specs/007 (application being tested)
