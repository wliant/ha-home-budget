# Data Model: Expense Server-Side Filtering

No new entities or schema changes required. This feature changes the API contract only.

## Existing Entities (unchanged)

### Expense
- `id` (unique identifier)
- `amount` (decimal)
- `description` (text)
- `expenseDate` (date)
- `categoryId` (FK to Category)
- `createdBy` (string, user identity)

### Category
- `id` (unique identifier)
- `name` (text)
- `icon` (text, optional)
- `parentCategoryId` (FK to Category, nullable; null = root category)

## Query Model Changes

### Expense List Query

**Current**: Accepts optional `categoryId` (single). If parent category, service expands to list of parent + children IDs internally.

**New**: Additionally accepts optional `categoryIds` (list). When provided, used directly as `IN` clause — no server-side expansion. Client is responsible for including all desired IDs.

**Precedence**: `categoryIds` (list) takes priority over `categoryId` (single) when both provided.

## Validation Rules

- `categoryIds` values must be valid positive integers
- Invalid/non-existent IDs in the list are silently ignored (database `IN` clause naturally excludes non-matching)
- Empty list treated as "no category filter" (same as parameter not provided)
