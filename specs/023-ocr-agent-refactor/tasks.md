# Tasks: OCR Agent Refactor

**Input**: Design documents from `/specs/023-ocr-agent-refactor/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in spec. Test tasks omitted.

**Organization**: Tasks grouped by user story. US2 (Structured Output) comes before US1 (Category Skip) because US1 depends on the refactored models and nodes from US2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Exact file paths included in descriptions

---

## Phase 1: Setup

**Purpose**: Add new dependencies and update shared configuration

- [x] T001 Add `langchain-anthropic` dependency to `ocr-processor/pyproject.toml`
- [x] T002 Update Settings class with `agent_name`, `anthropic_api_key`, and `paid_model` fields in `ocr-processor/src/ocr_processor/config.py`
- [x] T003 Create `ocr-processor/src/ocr_processor/agents/` package directory with empty `__init__.py`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Update shared Pydantic models and AgentState that all user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Add new Pydantic models (ReceiptExtraction, ExtractedLineItem, ClassificationResult, ClassifiedLineItem) and update ExpenseOutput to make category_id/category_name nullable in `ocr-processor/src/ocr_processor/models.py`
- [x] T005 Update AgentState TypedDict to add `selected_category`, `total_amount`, `classified_items` fields and type `line_items` as list of ExtractedLineItem in `ocr-processor/src/ocr_processor/agent.py`

**Checkpoint**: Foundation ready — shared models and state definition updated

---

## Phase 3: User Story 2 — Structured LLM Output (Priority: P1) MVP

**Goal**: Refactor extract and classify nodes to use Pydantic structured output with ChatPromptTemplate, eliminating raw JSON parsing

**Independent Test**: Process any receipt and verify extract/classify return validated Pydantic model instances. No `response_text.find("{")` or `json.loads()` of raw LLM output should exist.

### Implementation for User Story 2

- [x] T006 [US2] Refactor extract node in `ocr-processor/src/ocr_processor/nodes/extract.py`: replace PARSE_PROMPT string + manual JSON parsing with ChatPromptTemplate.from_messages() and ChatOllama.with_structured_output(ReceiptExtraction). Return typed ExtractedLineItem list and total_amount in state.
- [x] T007 [US2] Refactor classify node in `ocr-processor/src/ocr_processor/nodes/classify.py`: replace CLASSIFY_PROMPT_TEMPLATE string formatting + manual JSON parsing with ChatPromptTemplate.from_messages() and ChatOllama.with_structured_output(ClassificationResult). Allow null category_id/category_name in output. Write classified items to `classified_items` state key.
- [x] T008 [US2] Update validate node in `ocr-processor/src/ocr_processor/nodes/validate.py` to pass through `selected_category` field from state (no validation needed, trusted input)
- [x] T009 [US2] Update format node in `ocr-processor/src/ocr_processor/nodes/format.py` to read from `classified_items` state key (list of ClassifiedLineItem) instead of untyped `line_items` dicts. Handle nullable category_id/category_name when creating ExpenseOutput.

**Checkpoint**: Self-hosted agent processes receipts using structured LLM output. No raw JSON parsing remains in extract or classify nodes.

---

## Phase 4: User Story 1 — Pre-selected Category Skips Classification (Priority: P1)

**Goal**: Accept optional user-selected category via API, skip classify step when present, and pass category from backend

**Independent Test**: Send a receipt to `/process` with `selected_category` field. All returned expenses should have the given category. Without the field, classification runs as normal.

### Implementation for User Story 1

- [x] T010 [US1] Update FastAPI endpoint in `ocr-processor/src/ocr_processor/main.py` to accept optional `selected_category` Form parameter (JSON string of {id, name}), parse it into CategoryInput, and pass to agent
- [x] T011 [US1] Add conditional edge in self-hosted agent graph: after extract node, route to `format` if `selected_category` is present in state, otherwise route to `classify`. Update `ocr-processor/src/ocr_processor/agent.py` (or move graph building to `ocr-processor/src/ocr_processor/agents/self_hosted.py`)
- [x] T012 [US1] Update format node in `ocr-processor/src/ocr_processor/nodes/format.py` to check for `selected_category` in state: if present and no `classified_items`, build classified_items from line_items with the selected category applied to all items before consolidation
- [x] T013 [US1] Update `OcrProcessorClient.java` in `budget-backend/src/main/java/com/homebudget/service/OcrProcessorClient.java` to accept an optional Category parameter and send it as `selected_category` JSON form field to the OCR processor
- [x] T014 [US1] Update `ExpenseInputJobService.processPendingJobs()` in `budget-backend/src/main/java/com/homebudget/service/ExpenseInputJobService.java` to pass job's `defaultCategory` to `OcrProcessorClient.processReceipt()` when present

**Checkpoint**: Receipts with pre-selected category skip classification and consolidate into a single expense. Receipts without category go through full classify flow. Backend passes default category from job.

---

## Phase 5: User Story 4 — Smart Format Consolidation (Priority: P2)

**Goal**: Format step consolidates line items into fewer expense records when all share the same non-null category

**Independent Test**: Provide classified line items to format node — all same category returns 1 record with total; mixed categories returns N records.

### Implementation for User Story 4

- [x] T015 [US4] Implement smart consolidation logic in format node `ocr-processor/src/ocr_processor/nodes/format.py`: check if all classified_items share the same non-null category_id. If yes, return single ExpenseOutput with total_amount from state and descriptions joined with ", ". If no (different categories or any null), return one ExpenseOutput per classified item.

**Checkpoint**: Format node correctly consolidates same-category items and splits mixed-category items.

---

## Phase 6: User Story 3 — Dual Agent Backend Support (Priority: P2)

**Goal**: Create paid agent using Anthropic Claude multimodal vision, and an agent registry to select between self-hosted and paid

**Independent Test**: Set `AGENT_NAME=paid` and `ANTHROPIC_API_KEY=...`, send a receipt. The paid agent processes it via vision model without OCR.

### Implementation for User Story 3

- [x] T016 [US3] Create self-hosted agent graph builder in `ocr-processor/src/ocr_processor/agents/self_hosted.py`: move and refactor the existing build_graph() from agent.py. Wire nodes: validate → extract → conditional(classify or format) → format → END. Name property: "self-hosted".
- [x] T017 [US3] Create multimodal vision extract node in `ocr-processor/src/ocr_processor/nodes/extract_vision.py`: use ChatAnthropic with base64-encoded image content and .with_structured_output(ReceiptExtraction). Send receipt image directly to Claude vision model. Return same state keys as OCR extract (line_items, receipt_date, total_amount).
- [x] T018 [US3] Create paid agent graph builder in `ocr-processor/src/ocr_processor/agents/paid.py`: wire nodes: validate → extract_vision → conditional(classify or format) → format → END. Use ChatAnthropic for classify node as well. Name property: "paid".
- [x] T019 [US3] Implement agent registry in `ocr-processor/src/ocr_processor/agents/__init__.py`: provide `get_agent(name: str)` factory function that returns compiled graph for "self-hosted" or "paid". Raise ValueError for invalid names. Validate agent_name on startup.
- [x] T020 [US3] Update `ocr-processor/src/ocr_processor/main.py` to use agent registry: replace direct agent.py import with `get_agent(settings.agent_name)` call. Remove old agent.py graph building code (or keep as import redirect).
- [x] T021 [US3] Update `ocr-processor/src/ocr_processor/agent.py`: refactor to delegate to agents/__init__.py get_agent(). Keep process_receipt() as the public API but route to the configured agent internally.

**Checkpoint**: Both self-hosted and paid agents process receipts end-to-end. Agent selection is config-driven via AGENT_NAME env var.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and validation

- [x] T022 Remove old raw PARSE_PROMPT and CLASSIFY_PROMPT_TEMPLATE string constants if still present after refactor in extract.py and classify.py
- [x] T023 Run quickstart.md manual test scenarios (pre-selected category, no category, paid agent) to validate end-to-end flow

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US2 (Phase 3)**: Depends on Foundational — refactors node internals
- **US1 (Phase 4)**: Depends on US2 — needs structured models and refactored nodes
- **US4 (Phase 5)**: Depends on US2 — needs classified_items state key from refactored format node
- **US3 (Phase 6)**: Depends on US2 — needs clean refactored nodes to build second agent graph
- **Polish (Phase 7)**: Depends on all user stories complete

### User Story Dependencies

- **US2 (P1)**: Foundational refactor — must complete first
- **US1 (P1)**: Depends on US2 (needs structured models, refactored nodes)
- **US4 (P2)**: Depends on US2 (needs classified_items). Can run parallel with US1.
- **US3 (P2)**: Depends on US2 (needs clean nodes). Can run parallel with US1 and US4.

### Within Each User Story

- Models/state before node implementation
- Node refactors before graph wiring
- Python changes before Java backend changes

### Parallel Opportunities

- T001, T002, T003 (Setup) can run in parallel
- T004, T005 (Foundational) are sequential (T005 depends on T004 models)
- US4 (Phase 5) and US3 (Phase 6) can run in parallel after US2 completes
- T016, T017 (US3 self-hosted graph + vision extract) can run in parallel
- T013, T014 (US1 backend changes) can run in parallel

---

## Implementation Strategy

### MVP First (US2 + US1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational models
3. Complete Phase 3: US2 — Structured output refactor
4. Complete Phase 4: US1 — Category skip + backend integration
5. **STOP and VALIDATE**: Test with and without pre-selected category
6. Deploy if ready — self-hosted agent works with all new features

### Incremental Delivery

1. Setup + Foundational → Models ready
2. US2 → Structured output working → Validate
3. US1 → Category skip working → Validate end-to-end with backend
4. US4 → Smart consolidation → Validate format output
5. US3 → Paid agent → Validate with Anthropic API key
6. Polish → Cleanup and final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2 must complete before US1 due to model/node dependencies
- US3 and US4 can proceed in parallel after US2
- Backend Java changes (T013, T014) only in US1 phase
- No test tasks included (not requested in spec)
- Commit after each task or logical group
