# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **home budget and expense tracking system** that runs in a private home network. The application enables household members to collaboratively manage budgets, expenses, and spending categories.

**Technical Stack**:
- **Frontend**: Next.js (user interface, dashboard, budget/expense management)
- **Backend**: Spring Boot (Java) (REST APIs, business logic, data persistence)
- **Authentication**: Home Assistant via `X-Hass-User` HTTP header (nginx proxy)
- **Deployment**: Containerized for Home Assistant add-on, private network only

This repository uses **Specify** (Speckit), a feature specification and implementation workflow system. Specify helps manage feature development through structured phases: specification → planning → task generation → implementation.

## Project Type

**Non-Git Repository Support**: This repository is designed to work both with and without Git. The Specify system falls back to directory-based feature tracking when Git is not available, using the `SPECIFY_FEATURE` environment variable and the `/specs` directory structure.

## Core Workflow Commands

All Specify commands are available as slash commands in the `.claude/commands/` directory:

### Feature Lifecycle

1. **`/speckit.specify [description]`** - Create feature specification
   - Creates a new feature branch (or directory if non-git)
   - Generates `specs/###-feature-name/spec.md`
   - Validates specification quality with checklist
   - Handles clarifications interactively (max 3)
   - Example: `/speckit.specify Add user authentication with OAuth2`

2. **`/speckit.clarify`** - Interactive specification refinement
   - Identifies ambiguities in spec.md
   - Asks up to 5 targeted clarification questions
   - Updates spec incrementally after each answer
   - Must complete BEFORE `/speckit.plan`

3. **`/speckit.plan`** - Generate implementation plan
   - Creates `plan.md` with technical design
   - Runs Phase 0 (research) and Phase 1 (design artifacts)
   - Generates `research.md`, `data-model.md`, `contracts/`, `quickstart.md`
   - Updates agent context with technology choices
   - **Validates against constitution including technical stack constraints**

4. **`/speckit.tasks`** - Generate task breakdown
   - Creates `tasks.md` with dependency-ordered tasks
   - Organizes by user story for independent implementation
   - Marks parallelizable tasks with `[P]`
   - Task format: `- [ ] [T###] [P] [US#] Description with file/path`

5. **`/speckit.implement`** - Execute implementation
   - Checks feature checklist completion status
   - Creates ignore files (.gitignore, .dockerignore, etc.)
   - Executes tasks phase-by-phase
   - Marks completed tasks in tasks.md as `[X]`

6. **`/speckit.analyze`** - Cross-artifact consistency check
   - Non-destructive validation after task generation
   - Validates constitution compliance (including technical stack)

7. **`/speckit.checklist`** - Generate custom feature checklist

8. **`/speckit.constitution`** - Manage project principles

## Directory Structure

```
.
├── .claude/
│   └── commands/          # Slash command definitions
├── .specify/
│   ├── memory/
│   │   └── constitution.md  # Project principles & technical constraints
│   ├── scripts/bash/      # Workflow automation scripts
│   └── templates/         # Spec, plan, tasks templates
└── specs/
    └── ###-feature-name/  # Feature-specific artifacts
        ├── spec.md        # What & why (no tech details)
        ├── plan.md        # Technical design & architecture
        ├── tasks.md       # Execution plan
        ├── research.md    # Phase 0 technical decisions
        ├── data-model.md  # Entities & relationships
        ├── quickstart.md  # Integration scenarios
        ├── contracts/     # API specifications
        └── checklists/    # Quality validation checklists
```

## Helper Scripts

Located in `.specify/scripts/bash/`:

- **`create-new-feature.sh`** - Initialize new feature with branch/directory
  - Usage: `create-new-feature.sh --json --short-name "feature-name" "Full description"`

- **`setup-plan.sh`** - Setup planning environment
  - Usage: `setup-plan.sh --json`

- **`check-prerequisites.sh`** - Validate feature artifacts
  - Usage: `check-prerequisites.sh --json [--require-tasks] [--include-tasks] [--paths-only]`

- **`update-agent-context.sh`** - Update AI agent context files
  - Usage: `update-agent-context.sh claude`

- **`common.sh`** - Shared functions (get_repo_root, get_current_branch, etc.)

## Important Workflow Rules

### Specification Phase
- Focus on **WHAT** users need and **WHY** (no implementation details)
- Written for business stakeholders, not developers
- User stories must be prioritized (P1, P2, P3) and independently testable
- Maximum 3 `[NEEDS CLARIFICATION]` markers allowed
- Success criteria must be measurable and technology-agnostic

### Planning Phase
- Runs `/speckit.clarify` BEFORE `/speckit.plan` (recommended)
- Constitution gates must pass before Phase 0 research
- **MUST use Next.js for frontend and Spring Boot (Java) for backend**
- **MUST integrate with Home Assistant authentication (`X-Hass-User` header)**
- Generates all design artifacts in Phase 1
- Updates agent context with technology stack

### Task Generation
- Tasks organized by user story for incremental delivery
- Each phase should be independently testable
- Task format is strict: `- [ ] [T###] [P] [US#] Description with file/path`
- `[P]` marker indicates parallelizable tasks
- `[US#]` maps to user stories from spec.md
- Tests are OPTIONAL unless explicitly requested

### Implementation Phase
- Checks checklist completion before starting
- Creates/verifies ignore files based on tech stack
- Executes tasks phase-by-phase respecting dependencies
- Marks completed tasks as `[X]` in tasks.md
- TDD approach if tests are included in tasks

## Feature Branch Naming

When using Git:
- Format: `###-feature-name` (e.g., `001-user-auth`, `042-payment-flow`)
- Three-digit prefix allows multiple branches per feature
- Example: `004-fix-bug` and `004-add-feature` both work with `specs/004-original-name/`

When not using Git:
- Set `SPECIFY_FEATURE` environment variable to feature name
- Uses directory-based tracking in `/specs`

## Constitution & Governance

The project constitution (`.specify/memory/constitution.md`) defines:
- Core development principles (7 workflow principles)
- **Project-specific technical constraints (NON-NEGOTIABLE)**:
  - Next.js frontend
  - Spring Boot (Java) backend
  - Home Assistant authentication integration
  - Private home network deployment
  - Multi-user household support
- Quality gates (enforced at planning phase)
- Complexity justification requirements
- Governance rules

## Task Execution Notes

When implementing features:
1. Always read relevant design artifacts (spec.md, plan.md, data-model.md, contracts/)
2. Follow task order unless marked `[P]` for parallel execution
3. Tasks on the same file must run sequentially
4. Update tasks.md to mark completion (`[X]`)
5. Report progress after each task
6. **All features must use Next.js + Spring Boot stack per constitution**
7. **All backend endpoints must read `X-Hass-User` header for user identity**

## Technology Detection

The implementation phase auto-detects technology and creates appropriate ignore files:
- **Next.js/Node.js**: .gitignore with node_modules/, .next/, out/
- **Java/Spring Boot**: .gitignore with target/, *.class, *.jar
- **Docker**: .dockerignore for containerization

## Script Execution

All bash scripts support:
- JSON output with `--json` flag for programmatic parsing
- Single quotes in arguments: use `'I'\''m Groot'` or `"I'm Groot"`
- Absolute paths in all JSON outputs
- Both Git and non-Git repository modes

## Project-Specific Development Notes

### Authentication Flow
1. User accesses app through Home Assistant
2. Nginx proxy forwards request with `X-Hass-User` header
3. Spring Boot backend reads header to identify current user
4. No additional authentication layer needed (trust proxy)

### Multi-User Household Considerations
- All features should support multiple household members
- Capture user identity for audit trails (who created/modified)
- Default to shared visibility unless feature explicitly requires isolation

### API Contract Pattern
- Frontend (Next.js) → REST API → Backend (Spring Boot)
- OpenAPI specifications in `contracts/` directory
- Backend handles all business logic and data persistence

### Logging and Observability
**Framework**: Logback (SLF4J) with Logstash JSON Encoder (Feature 009)

**Key Features**:
- **Structured JSON Logging**: All logs emitted as JSON with @timestamp, level, correlation_id, user_id
- **Correlation ID Tracking**: UUID per request tracked across all log entries via MDC
- **User Context**: X-Hass-User header value automatically captured in logs
- **Error Logging**: Full stack traces with sensitive data masking (passwords, tokens, secrets)
- **Debug Logging**: Business logic troubleshooting in service layer (CategoryService, BudgetService, ExpenseService)
- **Performance Logging**: AOP-based method execution time tracking with slow method detection (>100ms)
- **Runtime Log Level Management**: Change log levels via Spring Boot Actuator without restart

**Log Level Configuration**:
- **Development**: DEBUG level for com.homebudget.* (application-dev.properties)
- **Production**: INFO level for com.homebudget.* (application-prod.properties)
- **Runtime Changes**: Use Actuator endpoints `/actuator/loggers/{name}` (GET/POST)

**Actuator Endpoints**:
- `GET /actuator/loggers` - List all loggers with levels
- `GET /actuator/loggers/{name}` - Get specific logger level
- `POST /actuator/loggers/{name}` - Change level dynamically (Body: `{"configuredLevel": "DEBUG"}`)

**Performance Thresholds**:
- Slow HTTP requests: >1000ms (logged at WARN level)
- Slow service methods: >100ms (logged at WARN level)
- Async logging: 10,000 entry bounded queue

**Best Practices**:
- Use SLF4J parameterized logging: `logger.debug("Value: {}", value)` (lazy evaluation)
- Add isDebugEnabled() guards for expensive debug operations
- Never log sensitive data (passwords, tokens, secrets) - use SensitiveDataMasker
- logger.info() for request/response, business events
- logger.debug() for troubleshooting details
- logger.warn() for recoverable issues, slow operations
- logger.error() for exceptions with stack traces

## Active Technologies
- MySQL 8.0 (via Docker Compose from Feature 001) (002-budget-management)
- Java 17 (backend), TypeScript/JavaScript (frontend) (004-hierarchical-category-budgets)
- MySQL 8.0 database with JPA/Hibernate ORM (004-hierarchical-category-budgets)
- TypeScript 5.x (frontend), Java 17 (backend) + Next.js 14.x, Material-UI v5, Spring Boot 3.2.0, React 18.x (006-homepage-update)
- MySQL 8.0 (existing database with budgets and expenses tables) (006-homepage-update)
- TypeScript 5.x (frontend), Java 17 (existing backend) + Next.js 14.x, Material-UI v5, React 18.x (007-expense-recording)
- MySQL 8.0 (via existing backend, no changes needed) (007-expense-recording)
- Manual testing (no code implementation); Documentation in Markdown (008-comprehensive-functional-testing)
- Test results stored in Markdown files; Test data managed in MySQL 8.0 database (reset before each major test suite) (008-comprehensive-functional-testing)
- Java 17 (existing Spring Boot backend) + Logback (SLF4J implementation), Logstash Logback Encoder for JSON formatting, Spring Boot AOP for request interception (009-structured-logging)
- N/A (logs written to stdout, consumed by external aggregation) (009-structured-logging)
- Java 17 + Spring Boot 3.2.0, spring-boot-starter-test (JUnit 5, Mockito, AssertJ), Testcontainers (MySQL module), Maven Failsafe Plugin (010-backend-test-suite)
- MySQL 8.0 (via Testcontainers for integration/E2E), H2 in-memory (retained for existing unit tests) (010-backend-test-suite)

## Recent Changes
- 002-budget-management: Added MySQL 8.0 (via Docker Compose from Feature 001)
