<!--
Sync Impact Report:
Version change: 1.0.0 → 1.1.0
Modified principles: None (original 7 principles unchanged)
Added sections: "Project-Specific Constraints" with technical stack requirements
Removed sections: None
Templates requiring updates:
  ✅ plan-template.md - Constitution Check will validate against new constraints
  ✅ spec-template.md - No changes needed (remains tech-agnostic)
  ✅ tasks-template.md - No changes needed (task format unchanged)
  ✅ All command files - /speckit.plan will check new constraints
  ✅ CLAUDE.md - Updated to reflect project context
Follow-up TODOs: None
-->

# Home Budget Tracker Constitution

## Core Principles

### I. Specification-First (NON-NEGOTIABLE)

Features MUST begin with a technology-agnostic specification that describes WHAT users need and WHY, without prescribing HOW to implement. Specifications MUST be written for business stakeholders, not developers. Implementation details (languages, frameworks, databases) are strictly prohibited in spec.md.

**Rationale**: Clear separation between problem space (spec) and solution space (plan) enables better requirement validation, reduces premature technical commitments, and allows stakeholders to approve features before engineering resources are committed.

### II. Clarify Before Planning

Specifications MUST be clarified through `/speckit.clarify` before advancing to `/speckit.plan`. Clarifications are limited to a maximum of 3 markers in specifications and 5 interactive questions during clarification. Vague requirements ("fast", "scalable", "intuitive") MUST be replaced with measurable criteria.

**Rationale**: Addressing ambiguity early prevents costly rework during implementation. The question limit enforces conciseness and forces specification authors to make reasonable default decisions.

### III. Incremental, Story-Based Delivery

Features MUST be decomposed into prioritized user stories (P1, P2, P3, etc.) where each story is independently testable and delivers standalone value. Tasks MUST be organized by user story to enable incremental implementation. Minimum Viable Product (MVP) MUST consist of only the highest priority story (P1).

**Rationale**: Independent user stories reduce implementation risk, enable parallel work, allow early user feedback, and provide natural rollback boundaries if issues arise.

### IV. Constitution Gates

Implementation plans MUST pass constitution validation before Phase 0 research begins. Plans MUST be re-validated after Phase 1 design. Any violations MUST be either resolved or explicitly justified in the plan's Complexity Tracking section.

**Rationale**: Early gate validation prevents architectural drift and ensures features align with project principles before significant effort is invested.

### V. Task Traceability

Every task in tasks.md MUST follow the strict format: `- [ ] [T###] [P] [US#] Description with file/path`. Tasks MUST be traceable to user stories via `[US#]` labels. Tasks affecting the same files MUST execute sequentially. Tasks marked `[P]` (parallelizable) MUST have no dependencies on incomplete tasks.

**Rationale**: Strict task formatting enables automated validation, clear dependency management, and efficient parallel execution while maintaining implementation correctness.

### VI. Test-Optional, Test-First When Included

Tests are OPTIONAL and MUST only be included when explicitly requested in the specification or when Test-Driven Development (TDD) is specified. When tests are included, they MUST be written before implementation (Red-Green-Refactor cycle). Contract tests MUST precede their implementation tasks.

**Rationale**: Not all features require automated tests (prototypes, exploratory spikes). When tests are warranted, TDD ensures testable design and prevents implementation bias in test creation.

### VII. Artifact Consistency

Cross-artifact consistency MUST be validated via `/speckit.analyze` after task generation and before implementation. The analysis MUST flag: requirements without tasks, tasks without requirements, constitution violations, ambiguous criteria, and terminology drift. CRITICAL issues MUST be resolved before `/speckit.implement`.

**Rationale**: Inconsistencies between spec, plan, and tasks lead to implementation confusion, missed requirements, and scope drift. Early detection prevents these issues.

## Project-Specific Constraints

### Application Context

This is a **home budget and expense tracking system** that runs in a private home network. The application enables household members to collaboratively manage budgets, expenses, and spending categories.

### Technical Stack (NON-NEGOTIABLE)

All features MUST use this architectural stack:

- **Frontend**: Next.js application for user interface
  - Manages budget creation and editing
  - Manages expense entry and tracking
  - Manages spending category configuration
  - Provides dashboard views for household financial overview

- **Backend**: Spring Boot (Java) application for business logic and data persistence
  - Exposes REST APIs consumed by Next.js frontend
  - Handles data validation and business rules
  - Manages database operations

### Authentication and Authorization

The application MUST integrate with Home Assistant authentication:

- All requests MUST be proxied through Home Assistant Add-on (nginx)
- User identity MUST be determined by reading the `X-Hass-User` HTTP header
- The backend MUST trust the `X-Hass-User` header as authoritative (no additional authentication layer required)
- The application MUST support multiple household users accessing the same system

**Rationale**: Home Assistant provides centralized authentication for all home automation services. Leveraging the existing authentication infrastructure simplifies deployment and provides a consistent user experience across home services.

### Deployment Environment

The application MUST run within a private home network:

- No public internet exposure required
- Home Assistant nginx add-on acts as reverse proxy
- Backend and frontend MUST be containerizable for Home Assistant add-on deployment

### Multi-User Household Support

All features MUST account for household collaborative use:

- Multiple users MUST be able to add budgets, expenses, and categories
- User identity MUST be captured for audit trails (who created/modified what)
- Shared visibility of household finances (no user-level data isolation unless explicitly specified in a feature)

## Development Workflow

### Phase Sequence

Feature development MUST follow this sequence:

1. **Specification** (`/speckit.specify`) - Define WHAT and WHY
2. **Clarification** (`/speckit.clarify`) - Resolve ambiguities (recommended)
3. **Planning** (`/speckit.plan`) - Define HOW with technical design
4. **Task Generation** (`/speckit.tasks`) - Break down into actionable tasks
5. **Analysis** (`/speckit.analyze`) - Validate consistency (recommended)
6. **Implementation** (`/speckit.implement`) - Execute tasks

Skipping phases (except optional clarify/analyze) is permitted for exploratory work but increases rework risk.

### Quality Checklists

Features MAY include custom checklists via `/speckit.checklist`. When checklists exist in `specs/###-feature/checklists/`, the implementation phase MUST validate completion status and prompt user approval before proceeding if any checklist has incomplete items.

### Documentation Standards

- **spec.md**: User-centric, no technical details, measurable success criteria
- **plan.md**: Technical context, architecture, research decisions, file structure
- **tasks.md**: Executable checklist with strict formatting, dependency information
- **research.md**: Technical decisions with rationale and alternatives considered
- **data-model.md**: Entities, relationships, validation rules (implementation-agnostic)
- **contracts/**: API specifications (OpenAPI, GraphQL schemas)
- **quickstart.md**: Integration scenarios and test flows

### Branch and Feature Naming

When using Git, feature branches MUST follow format `###-feature-name` (e.g., `001-user-auth`). The three-digit numeric prefix enables multiple branches to work on the same specification directory (e.g., `004-fix-bug` and `004-add-tests` both reference `specs/004-original-name/`).

For non-Git repositories, the `SPECIFY_FEATURE` environment variable MUST be set to the feature directory name.

## Technology and Tool Requirements

### Repository Flexibility

The workflow MUST support both Git and non-Git repositories. All scripts MUST detect Git availability and gracefully fall back to directory-based feature tracking when Git is unavailable.

### Script Standards

All bash scripts MUST:
- Support `--json` flag for programmatic output parsing
- Return absolute paths in JSON output
- Handle single quotes in arguments (support both `'I'\''m Groot'` and `"I'm Groot"`)
- Detect repository root via `get_repo_root()` function in `common.sh`

### Automation Scripts

Required automation scripts in `.specify/scripts/bash/`:
- `create-new-feature.sh` - Initialize feature structure
- `setup-plan.sh` - Prepare planning environment
- `check-prerequisites.sh` - Validate artifact existence
- `update-agent-context.sh` - Update AI agent context with tech stack
- `common.sh` - Shared utility functions

### Technology Detection

Implementation phase MUST auto-detect technology stack from plan.md and create appropriate ignore files (.gitignore, .dockerignore, .eslintignore, etc.) with technology-specific patterns.

## Governance

### Constitution Authority

This constitution is the authoritative source for all Specify workflow practices. In conflicts between this constitution and other documentation, the constitution prevails.

### Amendment Process

Constitution changes MUST:
1. Use semantic versioning (MAJOR.MINOR.PATCH)
2. Document version bump rationale in Sync Impact Report (HTML comment at file top)
3. Update dependent templates (plan, spec, tasks) for consistency
4. Verify command files reflect new principles
5. Update CLAUDE.md if workflow changes affect AI agent guidance

Version semantics:
- **MAJOR**: Backward-incompatible principle changes or removals
- **MINOR**: New principles added or material expansions
- **PATCH**: Clarifications, wording improvements, non-semantic fixes

### Complexity Justification

When features violate constitution principles, violations MUST be documented in the plan.md Complexity Tracking table with:
- Specific principle violated
- Business justification for violation
- Why simpler alternatives are insufficient

Unjustified violations MUST block implementation.

### Compliance Verification

The `/speckit.analyze` command enforces constitution compliance by flagging violations as CRITICAL issues. Constitution conflicts MUST be resolved by adjusting spec/plan/tasks, not by diluting or reinterpreting principles.

**Version**: 1.1.0 | **Ratified**: 2025-10-22 | **Last Amended**: 2025-10-22
