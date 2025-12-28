# Feature Specification: Expense Recording

**Feature Branch**: `007-expense-recording`
**Created**: 2025-12-22
**Status**: Draft
**Input**: User description: "Expense recording feature allow user to create expense. It should have date (editable, default to today), Description (free text), Category (existing category), Created by, based on hass token."

## User Scenarios & Testing

### User Story 1 - Quick Expense Entry (Priority: P1)

A household member wants to quickly record a daily expense (e.g., groceries, gas, dining) immediately after making the purchase. They need to capture the amount, description, and category with minimal friction.

**Why this priority**: Core functionality that delivers immediate value. Without this, the expense tracking system cannot fulfill its primary purpose. This is the foundation for all budget tracking and analysis features.

**Independent Test**: User can navigate to expense entry form, enter amount and description, select a category, and save the expense. The expense appears in their recent activity and is counted against their budget.

**Acceptance Scenarios**:

1. **Given** I am logged in via Home Assistant, **When** I navigate to the expense recording page, **Then** I see a form with fields for date (defaulted to today), amount, description, and category dropdown
2. **Given** I have filled in all required fields (amount, description, category), **When** I submit the form, **Then** the expense is saved with my username (from X-Hass-User header) as the creator
3. **Given** I have successfully saved an expense, **When** I view the expense list or budget summary, **Then** my newly created expense appears in the list and is counted against the appropriate budget
4. **Given** I am filling out the expense form, **When** I change the date field, **Then** the system accepts the custom date instead of today's date

---

### User Story 2 - Category-Based Expense Tracking (Priority: P1)

A user wants to categorize their expenses (groceries, utilities, entertainment, etc.) to understand spending patterns and stay within category-specific budgets.

**Why this priority**: Essential for meaningful budget tracking. Without categories, expenses are just a flat list with no organizational structure or budget allocation.

**Independent Test**: User can select from existing categories when creating an expense. The expense is associated with the chosen category and counted against that category's budget allocation.

**Acceptance Scenarios**:

1. **Given** categories exist in the system, **When** I open the expense recording form, **Then** I see a dropdown listing all active categories
2. **Given** I select a category with a defined budget, **When** I save the expense, **Then** the expense amount is deducted from that category's remaining budget
3. **Given** I select a category that is a child category, **When** I save the expense, **Then** the expense is counted against both the child category budget and the parent category budget

---

### User Story 3 - Expense Date Flexibility (Priority: P2)

A user wants to record past expenses (forgot to enter yesterday's purchase) or future planned expenses (scheduled payments) with accurate dates.

**Why this priority**: Improves data accuracy and usability, but the system is functional without it. Users can still record expenses with today's date as a workaround.

**Independent Test**: User can change the date field to any past or future date, save the expense, and verify it appears with the correct date in expense history.

**Acceptance Scenarios**:

1. **Given** I am recording an expense, **When** I click the date field, **Then** I see a date picker allowing me to select any date
2. **Given** I select a past date, **When** I save the expense, **Then** the expense is saved with the chosen date and appears in the correct chronological position in expense history
3. **Given** I select a future date, **When** I save the expense, **Then** the expense is saved as a planned expense with the future date

---

### User Story 4 - Multi-User Attribution (Priority: P2)

In a household with multiple members, each user wants to see who created each expense for accountability and tracking individual spending patterns.

**Why this priority**: Valuable for multi-user households but not critical for single-user scenarios. The system functions without explicit attribution, though it's still captured in the background.

**Independent Test**: Two different users (via different X-Hass-User headers) create expenses. Each expense displays the creator's name in the expense list and activity feed.

**Acceptance Scenarios**:

1. **Given** I am authenticated via Home Assistant with username "alice", **When** I create an expense, **Then** the expense is saved with "alice" as the creator
2. **Given** multiple household members have created expenses, **When** I view the expense list, **Then** each expense shows the creator's name
3. **Given** I filter or view expenses by creator, **When** I select my name, **Then** I see only expenses I created

---

### Edge Cases

- What happens when a user tries to save an expense without selecting a category?
- What happens when a user enters a negative amount?
- What happens when a user enters an extremely large amount (e.g., $1,000,000)?
- What happens when the date field is left blank or set to an invalid date?
- What happens when the description exceeds reasonable length (e.g., 1000 characters)?
- What happens if the selected category is deleted after the expense is created?
- What happens when the backend is unavailable during expense submission?
- What happens if a user's X-Hass-User header is missing or invalid?

## Requirements

### Functional Requirements

#### Core Form Fields

- **FR-001**: System MUST display an expense recording form with fields for date, amount, description, and category
- **FR-002**: System MUST default the date field to today's date when the form loads
- **FR-003**: System MUST allow users to edit the date field via a date picker component to select past or future dates
- **FR-004**: System MUST provide a dropdown/selector populated with all existing categories, displaying category names with parent hierarchy notation
- **FR-005**: System MUST require amount, description, and category fields before allowing submission

#### Input Constraints

- **FR-006**: System MUST accept description as free-form text input with a maximum length of 500 characters
- **FR-007**: System MUST display a character counter showing remaining characters for the description field
- **FR-008**: System MUST validate that amount is a positive decimal number greater than 0, with maximum 2 decimal places
- **FR-009**: System MUST format amount field to 2 decimal places when user completes input (on blur)

#### User Attribution

- **FR-010**: System MUST capture the creator's username from the X-Hass-User HTTP header
- **FR-011**: System MUST display the current user context in the form header before submission
- **FR-012**: System MUST save expenses with the creator's username, date, amount, description, and selected category

#### Validation

- **FR-013**: System MUST validate that the selected category exists in the system
- **FR-014**: System MUST validate that a budget exists for the selected expense date
- **FR-015**: System MUST display inline validation errors for individual fields (amount, description, category) when validation fails
- **FR-016**: System MUST display a global error message when no budget is found for the selected date, preventing form submission

#### Budget Association

- **FR-017**: System MUST automatically associate the expense with the appropriate budget by matching the expense date against budget date ranges (where expenseDate >= startDate AND expenseDate <= endDate)
- **FR-018**: System MUST display feedback to the user indicating which budget was auto-selected for the chosen date

#### Success & Error Handling

- **FR-019**: System MUST display a success message "Expense created for [username]!" after successfully saving an expense
- **FR-020**: System MUST navigate users to the homepage after successful creation, with a 2-second delay after showing the success message
- **FR-021**: System MUST display validation errors if required fields are missing or invalid
- **FR-022**: System MUST handle backend errors (network failures, timeout, server errors) and display user-friendly error messages
- **FR-023**: System MUST handle the case where no X-Hass-User header is present (dev mode fallback or error)

#### User Experience Enhancements

- **FR-024**: System MUST display loading states (spinner/progress indicator) during asynchronous operations (category loading, form submission)
- **FR-025**: System MUST reset the form to default values after successful submission and before navigation
- **FR-026**: System MUST disable the submit button while form submission is in progress or when validation fails
- **FR-027**: System MUST support keyboard shortcut (Escape key) to cancel form entry and navigate to homepage

#### Non-Functional Requirements

- **NFR-001**: Form MUST be responsive and functional on mobile devices (min width 320px)
- **NFR-002**: Form MUST include accessibility attributes (ARIA labels, keyboard navigation support, screen reader compatibility)
- **NFR-003**: Form MUST load within 500 milliseconds on standard network connections
- **NFR-004**: Category dropdown MUST display icons for each category (if icon is defined in category data)

### Key Entities

- **Expense**: Represents a household spending transaction with date, amount, description, category, and creator attribution
- **Category**: Existing entity that expenses are associated with for budget allocation
- **User**: Identified by Home Assistant username (X-Hass-User header), represents the household member creating the expense
- **Budget**: Existing entity that expenses are counted against based on category and date

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can complete expense entry in under 30 seconds for routine transactions
- **SC-002**: 95% of expense submissions succeed on the first attempt without validation errors
- **SC-003**: All expenses correctly associate with the user's Home Assistant username
- **SC-004**: Newly created expenses appear in the expense list within 2 seconds of submission
- **SC-005**: Date field defaults to today's date in 100% of form loads
- **SC-006**: Users can successfully edit the date field and save expenses with custom dates
- **SC-007**: Category dropdown displays all available categories without requiring additional user action

## Assumptions

- Categories already exist in the system (from Feature 005 - Category Management)
- Budgets already exist in the system (from Feature 002 - Budget Management)
- User authentication via X-Hass-User header is already implemented and working (from Feature 003 - Dev Mode Headers)
- The backend API for creating expenses already exists (from Feature 002 - Record Expenses Against Budgets)
- Amount will be entered as a decimal number (currency formatting handled by UI)
- Time of day for the expense is not required (date only)
- Receipt attachments are out of scope for this feature
- Editing or deleting expenses is out of scope for this feature

## Out of Scope

- Expense editing/deletion functionality
- Expense search or filtering beyond basic list view
- Receipt photo uploads
- Recurring expense automation
- Expense splitting between multiple users
- Bulk expense import
- Expense approval workflows
- Budget threshold warnings during expense creation (handled by existing budget display)

## Dependencies

- **Feature 002**: Budget Management API endpoints for expense creation
- **Feature 003**: Dev mode X-Hass-User header handling
- **Feature 005**: Category Management for category dropdown population
- **Next.js**: Frontend framework already in use
- **Material-UI**: UI component library already in use
