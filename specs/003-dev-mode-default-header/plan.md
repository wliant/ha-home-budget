# Implementation Plan: Development Mode Default User Header

**Branch**: `003-dev-mode-default-header` | **Date**: 2025-10-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-dev-mode-default-header/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Enhance developer experience by providing a default user identity for the X-Hass-User header when running in development mode. This eliminates authentication errors during local development while maintaining production security requirements. The solution will detect the environment (dev vs prod) and automatically supply a default user value when the header is missing in development mode only.

## Technical Context

**Backend**:
- **Language/Version**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Primary Dependencies**: Spring Web, Spring Boot Starter Validation
- **Build Tool**: Maven 3.9
- **Storage**: N/A (configuration-based feature)
- **Testing**: JUnit 5 (optional for this feature)

**Frontend**:
- **Language/Version**: TypeScript 5.3.3
- **Framework**: Next.js 14.0.4
- **Primary Dependencies**: React 18.2, Axios 1.6.2
- **Build Tool**: npm/yarn
- **Storage**: N/A
- **Testing**: Jest 29.7.0 (optional for this feature)

**Project Type**: Web application (Next.js frontend + Spring Boot backend)

**Target Platform**: Docker containers in Home Assistant add-on deployment

**Current Authentication Mechanism**:
- Controllers use `@RequestHeader("X-Hass-User")` to extract user identity
- Required header: `X-Hass-User` (enforced by Spring Boot)
- No interceptor or filter currently handling missing headers
- Production: Home Assistant nginx proxy provides the header
- Development: Developers must manually provide header in API requests

**Performance Goals**: No performance impact - configuration check once per request in header extraction

**Constraints**:
- MUST NOT introduce security vulnerabilities in production
- MUST work transparently with existing controller code
- Solution should be environment-based (Spring Profiles for backend, NODE_ENV for frontend)

**Scale/Scope**: Minimal - affects only authentication header handling logic

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Specification-First
✅ **PASS** - spec.md contains no implementation details, focuses on user value (developer experience)

### Principle II: Clarify Before Planning
✅ **PASS** - No [NEEDS CLARIFICATION] markers in specification, all requirements unambiguous

### Principle III: Incremental, Story-Based Delivery
✅ **PASS** - Three prioritized user stories (P1, P2, P3), each independently testable:
- P1: Default header in dev mode (core MVP)
- P2: Override with explicit header
- P3: Development mode indicators

### Principle IV: Constitution Gates
✅ **PASS** - Performing initial gate validation now, will re-validate after Phase 1

### Principle V: Task Traceability
⏳ **PENDING** - Tasks will be generated in `/speckit.tasks` phase with proper formatting

### Principle VI: Test-Optional, Test-First When Included
✅ **PASS** - Tests not explicitly requested in spec, will be optional

### Principle VII: Artifact Consistency
⏳ **PENDING** - Will run `/speckit.analyze` after task generation

### Project-Specific Constraints: Technical Stack
✅ **PASS** - Feature works within existing Spring Boot + Next.js stack

### Project-Specific Constraints: Authentication
✅ **PASS** - Enhances existing X-Hass-User header mechanism without changing contract

### Project-Specific Constraints: Deployment
✅ **PASS** - Environment-based configuration compatible with Docker/Home Assistant deployment

### Project-Specific Constraints: Multi-User Support
✅ **PASS** - Maintains user attribution, enables testing multi-user scenarios in development

**Overall Status**: ✅ **PASSED** - No violations, ready for Phase 0 research

---

**Post-Phase 1 Re-evaluation**: ✅ **PASSED** - All principles still satisfied after design phase:
- Spec remains technology-agnostic
- Three independent user stories maintained
- Design artifacts complete (research.md, data-model.md, contracts/, quickstart.md)
- No complexity violations introduced
- Agent context updated with tech stack

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── config/
│   │   └── AuthHeaderInterceptor.java       [NEW - US1: intercept and supply default header]
│   ├── controller/
│   │   ├── BudgetController.java            [MODIFY - US2: change to optional header]
│   │   ├── ExpenseController.java           [MODIFY - US2: change to optional header]
│   │   └── CategoryController.java          [MODIFY - US2: change to optional header]
│   └── HomeBudgetApplication.java           [MODIFY - US3: add startup logging]
└── src/main/resources/
    └── application.properties                [MODIFY - US1: add dev mode config]
    └── application-dev.properties            [NEW - US1: development profile settings]

budget-frontend/
├── src/
│   └── services/
│       └── api.ts                            [MODIFY - US2: remove required header in dev]
└── .env.development                          [NEW - US1: frontend dev mode config]

docker-compose.yml                            [MODIFY - US1: set dev environment variables]
```

**Structure Decision**: Web application structure (Option 2) - modifies existing backend and frontend projects. This is a cross-cutting configuration feature affecting authentication handling in both services.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitution violations. This section is not applicable for this feature.
