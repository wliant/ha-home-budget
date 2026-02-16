# Feature Specification: OCR Agent Refactor

**Feature Branch**: `023-ocr-agent-refactor`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "Refactor the OCR receipt processor to accept a user-selected category, support dual agent backends (self-hosted and paid), use Pydantic structured LLM output with proper LangChain prompt templates, and redesign the agent flow with conditional category classification."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pre-selected Category Skips Classification (Priority: P1)

A user uploads receipts through the bulk upload dialog with a pre-selected default category. When the OCR processor processes these receipts, it should skip the category classification step entirely and assign the user-selected category to all extracted line items. This reduces processing time and ensures the user's intent is respected.

**Why this priority**: This is the primary user-facing change. Users who already know the category of their receipts should not have their selection overridden by an imperfect LLM classification.

**Independent Test**: Can be tested by sending a receipt to the OCR processor with a category ID included. The returned expense records should all have the provided category, and no classification LLM call should occur.

**Acceptance Scenarios**:

1. **Given** a receipt image and a user-selected category (ID and name), **When** the OCR processor processes the receipt, **Then** all returned expense records have the user-selected category and no classification step is executed.
2. **Given** a receipt image with no user-selected category, **When** the OCR processor processes the receipt, **Then** the system attempts to classify each line item into categories as before.
3. **Given** a receipt image and a user-selected category, **When** the receipt contains multiple line items, **Then** all line items are assigned the user-selected category and the format step consolidates them into a single expense record with the total amount.

---

### User Story 2 - Structured LLM Output with Pydantic Models (Priority: P1)

The OCR processor should use Pydantic models for all LLM interactions, ensuring structured output parsing. LLM calls should use LangChain's ChatPromptTemplate (not raw string formatting) and request structured output from the model. This improves reliability by eliminating free-form JSON parsing from raw text.

**Why this priority**: Structured output reduces parsing failures and makes the extraction and classification steps more reliable, directly improving the user experience of getting accurate expense records.

**Independent Test**: Can be tested by processing any receipt and verifying the extraction and classification steps return properly validated Pydantic model instances (no raw JSON string parsing).

**Acceptance Scenarios**:

1. **Given** a receipt image, **When** the extract step calls the LLM, **Then** it uses a Pydantic model to define the expected output schema (receipt date, total amount, line items with description and amount).
2. **Given** a receipt with line items requiring classification, **When** the classify step calls the LLM, **Then** it uses a Pydantic model for structured output and a ChatPromptTemplate for the prompt.
3. **Given** a receipt where the LLM cannot confidently classify a line item, **When** the classify step runs, **Then** that line item's category is set to null rather than forcing an incorrect category.

---

### User Story 3 - Dual Agent Backend Support (Priority: P2)

The system supports two processing agents: a "self-hosted" agent (using the local Ollama LLM with OCR text extraction) and a "paid" agent (using a cloud-based multimodal vision model that reads receipt images directly without OCR). Each agent is a separate LangGraph workflow. A configuration property determines which agent is invoked when processing a receipt.

**Why this priority**: Having a paid agent option provides higher accuracy through multimodal vision (no OCR errors), but the self-hosted agent must work first as it's the existing capability.

**Independent Test**: Can be tested by configuring the agent name property and verifying the correct agent graph is invoked. Each agent should independently process receipts end-to-end.

**Acceptance Scenarios**:

1. **Given** the agent configuration is set to "self-hosted", **When** a receipt is processed, **Then** the self-hosted LangGraph agent (using Ollama) is invoked.
2. **Given** the agent configuration is set to "paid", **When** a receipt is processed, **Then** the paid LangGraph agent (using a cloud LLM) is invoked.
3. **Given** an invalid agent name in configuration, **When** the system starts, **Then** it fails with a clear error message indicating the valid options.

---

### User Story 4 - Improved Format Step with Smart Consolidation (Priority: P2)

The format step intelligently consolidates line items into expense records. If all line items share the same non-null category, they are merged into a single expense record with the receipt's total amount and date. If line items have different categories or any have a null category, each line item becomes its own expense record.

**Why this priority**: Smart consolidation reduces the number of expense records users need to review while preserving category-level detail when items span multiple categories.

**Independent Test**: Can be tested by providing pre-classified line items to the format step and verifying the correct number of expense records are returned.

**Acceptance Scenarios**:

1. **Given** three line items all classified as "Groceries" (non-null), **When** the format step runs, **Then** a single expense record is returned with the total amount, receipt date, and all line item descriptions joined together.
2. **Given** three line items where two are "Groceries" and one is "Dining", **When** the format step runs, **Then** three separate expense records are returned, one per line item.
3. **Given** three line items where one has a null category, **When** the format step runs, **Then** three separate expense records are returned, one per line item, with the null-category item having no category assigned.
4. **Given** a single line item with any category, **When** the format step runs, **Then** one expense record is returned.

---

### Edge Cases

- What happens when the receipt has no line items but has a total amount? The system creates a single line item with the total as the amount and a generic description.
- What happens when the LLM extraction fails to return a valid Pydantic model? The system raises a retryable error.
- What happens when the user-selected category ID does not match any of the provided categories? The system still uses the user-selected category (it is trusted input from the backend).
- What happens when the paid agent's cloud API is unreachable? The system returns a retryable error, same as the self-hosted agent when Ollama is down.
- What happens when all line items have null categories after classification? Each line item becomes a separate expense record with null category.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The processing endpoint MUST accept an optional user-selected category (ID and name) in addition to the file and categories list.
- **FR-002**: When a user-selected category is provided, the agent workflow MUST skip the classification step and assign the provided category to all line items.
- **FR-003**: When no user-selected category is provided, the agent MUST attempt to classify each line item using the LLM, and MAY set category to null if the LLM is not confident.
- **FR-004**: The system MUST support two named agent configurations: "self-hosted" (local Ollama LLM with OCR text extraction) and "paid" (cloud-based multimodal vision model that processes receipt images directly without OCR).
- **FR-005**: A configuration property MUST determine which agent is used for processing. The default MUST be "self-hosted".
- **FR-006**: Both agents MUST follow the same node flow: validate, extract, classify (conditional), format.
- **FR-007**: All LLM calls MUST use Pydantic models for structured output and LangChain ChatPromptTemplate for prompt construction.
- **FR-008**: The extract step MUST produce a structured output containing: receipt date, total amount, and a list of line items (each with description and amount).
- **FR-009**: The classify step MUST accept null as a valid category assignment when the LLM lacks confidence.
- **FR-010**: The format step MUST consolidate all line items into a single expense record when all share the same non-null category, using the receipt's total amount and joining all line item descriptions (e.g., "Coffee, Sandwich, Water").
- **FR-011**: The format step MUST return each line item as a separate expense record when line items have different categories or any have a null category.
- **FR-012**: The backend caller MUST handle null category values in expense records returned by the OCR processor.
- **FR-013**: Expense records with null categories MUST have category_id and category_name set to null in the response.

### Key Entities

- **Receipt Extraction**: Represents the structured output of the extract step — receipt date, total amount, and a list of line items (description + amount).
- **Line Item**: An individual item on a receipt with a description and amount. After classification, it may also have a category (or null).
- **Expense Record**: The final output — amount, description, date, and optional category. One or more per receipt depending on consolidation logic.
- **Agent Configuration**: Identifies which processing agent to use — "self-hosted" or "paid".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Receipts uploaded with a pre-selected category are processed without any classification step, reducing per-receipt processing time.
- **SC-002**: All LLM interactions use structured output parsing — no raw JSON string extraction from LLM text responses.
- **SC-003**: The system correctly switches between self-hosted and paid agents based on configuration without code changes.
- **SC-004**: When line items cannot be confidently classified, null categories are returned and handled gracefully by the backend (no errors, no forced incorrect categories).
- **SC-005**: Receipts with all same-category line items produce a single consolidated expense record; mixed-category receipts produce one record per line item.

## Clarifications

### Session 2026-02-16

- Q: Should the paid agent use the same OCR pipeline with a different LLM, or a multimodal vision model that reads receipt images directly? → A: Multimodal vision model — send the receipt image directly to the LLM, skip OCR text extraction entirely.
- Q: When the format step consolidates all line items into a single expense record, what should the description contain? → A: Join all line item descriptions (e.g., "Coffee, Sandwich, Water").

## Assumptions

- The "paid" agent will use a cloud-based multimodal vision model (e.g., OpenAI GPT-4o, Anthropic Claude) that processes receipt images directly without OCR. The specific provider will be determined during planning.
- The self-hosted agent continues to use Ollama as it does today.
- The user-selected category is trusted input from the backend and does not need validation against the categories list.
- The backend (Spring Boot) will be updated to pass the user-selected category to the OCR processor API.
- Existing tests will be updated to cover the new structured output models and conditional classification flow.

## Scope Boundaries

**In scope**:
- OCR processor API changes (new parameter, response changes)
- Agent workflow refactor (structured output, conditional classify, dual agents)
- Backend client updates to pass category and handle null categories
- Format step consolidation logic

**Out of scope**:
- Frontend changes (already handled by the bulk upload dialog category selection feature)
- Changes to the receipt file storage or upload flow
- Adding new OCR engines or replacing PaddleOCR/Tesseract
- Monitoring or analytics dashboards for agent performance
