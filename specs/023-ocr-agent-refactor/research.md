# Research: OCR Agent Refactor

**Feature**: 023-ocr-agent-refactor
**Date**: 2026-02-16

## R1: LangChain Structured Output with Ollama

**Decision**: Use `ChatOllama.with_structured_output(PydanticModel)` for structured LLM output in the self-hosted agent.

**Rationale**: LangChain >=0.3 supports `.with_structured_output()` on ChatOllama, which automatically handles JSON schema injection and response parsing into Pydantic models. This eliminates manual JSON extraction (`response_text.find("{")`) and provides validation out of the box.

**Alternatives considered**:
- Manual JSON parsing from LLM text output (current approach) — fragile, requires regex/find heuristics
- LangChain OutputParser with format instructions — works but `.with_structured_output()` is the recommended approach for newer LangChain versions

**Implementation note**: `ChatOllama.with_structured_output(PydanticModel)` requires Ollama to support structured/JSON mode. The `llama3.1` model supports this. If a model doesn't support it, LangChain falls back to prompt-based JSON extraction.

## R2: ChatPromptTemplate for Prompt Construction

**Decision**: Replace raw string formatting with `ChatPromptTemplate.from_messages()` for all LLM prompts.

**Rationale**: ChatPromptTemplate provides type-safe variable injection, proper message role handling (system/human), and integration with LangChain's structured output pipeline. Raw f-strings with `{{` escaping are error-prone and don't leverage LangChain's prompt management.

**Alternatives considered**:
- `PromptTemplate` (single-string) — doesn't support multi-message chat format
- Raw f-strings (current approach) — no validation, no message roles

## R3: Multimodal Vision Model for Paid Agent

**Decision**: Use Anthropic Claude (via `langchain-anthropic` / `ChatAnthropic`) as the multimodal vision model for the paid agent.

**Rationale**: Claude supports vision natively via base64-encoded images in message content. The `langchain-anthropic` package provides `ChatAnthropic` which supports `.with_structured_output()` for Pydantic models and handles image content blocks. This enables sending the receipt image directly to the LLM without OCR text extraction.

**Alternatives considered**:
- OpenAI GPT-4o (via `langchain-openai`) — equally capable but requires OpenAI API key; Claude is more aligned with the project's AI tooling
- Google Gemini — less mature LangChain integration for structured output

**Configuration**: `ANTHROPIC_API_KEY` env var, `paid_model` setting (default: `claude-sonnet-4-5-20250929`).

## R4: Conditional Edge in LangGraph

**Decision**: Use `add_conditional_edges()` in the LangGraph to route from `extract` to either `classify` or `format` based on whether a user-selected category is present in state.

**Rationale**: LangGraph's conditional edges are the standard mechanism for branching. A simple function checks `state.get("selected_category")` — if present, route to `format`; otherwise route to `classify`.

**Alternatives considered**:
- Checking inside the classify node and returning early — less clean, the node still executes
- Two separate graphs with no conditional — code duplication

## R5: Nullable Category in ExpenseOutput

**Decision**: Make `category_id` and `category_name` optional (nullable) in `ExpenseOutput` Pydantic model and `OcrExpenseDTO` Java DTO.

**Rationale**: The classify step may return null when the LLM lacks confidence. The backend already uses `Long` for `categoryId` in `OcrExpenseDTO` (which is nullable by nature). The Python `ExpenseOutput` model needs to change from `category_id: int` to `category_id: int | None`.

**Backend impact**: `ExpenseInputJobService.processPendingJobs()` already handles null categories when creating `TemporaryExpenseRecord` (the `if (ocrExpense.getCategoryId() != null)` check exists). No backend DTO changes needed since `Long` is already nullable.

## R6: Agent Registry Pattern

**Decision**: Create an `agents/` package with an `__init__.py` that provides a `get_agent(name: str)` factory function. Each agent (self_hosted.py, paid.py) exports a `build_graph()` function that returns a compiled LangGraph.

**Rationale**: Clean separation of agent implementations. The factory pattern allows adding new agents without modifying existing code. Configuration-driven agent selection via `settings.agent_name`.

**Alternatives considered**:
- Single agent.py with if/else — doesn't scale, violates SRP
- Plugin/dynamic loading — over-engineered for 2 agents

## R7: Description Separator for Consolidated Expenses

**Decision**: Use comma-space (`, `) as the separator when joining line item descriptions in the format step.

**Rationale**: Comma-separated is the most readable for short descriptions (e.g., "Coffee, Sandwich, Water"). Newlines would be awkward in a single-line description field.
