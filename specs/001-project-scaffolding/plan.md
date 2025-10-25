# Implementation Plan: Development Environment Setup

**Branch**: `001-project-scaffolding` | **Date**: 2025-10-22 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-project-scaffolding/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature establishes the foundational development environment for the Home Budget Tracker application. It creates a containerized development setup with Next.js frontend, Spring Boot (Java) backend, and MySQL database, enabling developers to start the entire stack with a single command. The environment includes automated testing infrastructure with isolated test databases, database schema migration support via Liquibase, and Material Design UI components for consistent visual presentation.

## Technical Context

**Language/Version**:
- Frontend: JavaScript/TypeScript with Next.js 14.x
- Backend: Java 17+ with Spring Boot 3.x

**Primary Dependencies**:
- Frontend: Next.js, React 18, Material-UI (MUI) v5, Axios/Fetch for API calls
- Backend: Spring Boot, Spring Data JPA, Spring Web, Liquibase, MySQL Connector/J
- Testing: JUnit 5, Mockito, H2 Database (in-memory for tests)
- Container Orchestration: Docker, Docker Compose

**Storage**:
- Development: MySQL 8.0 (containerized)
- Testing: H2 in-memory database (embedded)
- Data Persistence: Docker volumes for MySQL data

**Testing**:
- Backend: JUnit 5 + Spring Boot Test + H2
- Frontend: Jest + React Testing Library (standard Next.js setup)
- Integration: Manual testing via browser and API calls

**Target Platform**:
- Development: Docker containers on Mac/Linux/Windows (WSL)
- Runtime: Home Assistant Add-on environment (private home network)
- Browser Support: Modern browsers (Chrome, Firefox, Safari, Edge - last 2 versions)

**Project Type**: Web application (frontend + backend)

**Performance Goals**:
- Development environment startup: < 60 seconds
- Hot reload time (frontend): < 2 seconds
- Backend test suite execution: < 30 seconds
- Frontend build time: < 2 minutes

**Constraints**:
- Must use Next.js for frontend (constitution requirement)
- Must use Spring Boot (Java) for backend (constitution requirement)
- Must integrate with Home Assistant authentication via `X-Hass-User` header (constitution requirement)
- Must be containerizable for Home Assistant add-on deployment
- Development ports: Frontend (3000), Backend (8080), MySQL (3306) - configurable
- No public internet exposure required (private home network only)

**Scale/Scope**:
- Single household use (5-10 users)
- Development team: 1-5 developers
- Initial MVP: Infrastructure only (no business logic)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Technical Stack Compliance

✅ **Frontend: Next.js** - Complies with constitution "Technical Stack (NON-NEGOTIABLE)"
✅ **Backend: Spring Boot (Java)** - Complies with constitution "Technical Stack (NON-NEGOTIABLE)"
✅ **Authentication: Home Assistant Integration** - Complies with "Authentication and Authorization" (X-Hass-User header)
✅ **Deployment: Containerizable** - Complies with "Deployment Environment" (Home Assistant add-on)

### Core Principles Compliance

✅ **Principle I: Specification-First** - Spec.md is technology-agnostic, implementation details in plan.md
✅ **Principle II: Clarify Before Planning** - Clarification completed (no ambiguities found)
✅ **Principle III: Incremental, Story-Based Delivery** - 3 prioritized user stories (P1, P2, P3)
✅ **Principle IV: Constitution Gates** - This check performed before Phase 0
✅ **Principle V: Task Traceability** - Will be enforced during `/speckit.tasks`
✅ **Principle VI: Test-Optional, Test-First When Included** - Tests not explicitly required in spec; infrastructure-focused
✅ **Principle VII: Artifact Consistency** - Will be validated via `/speckit.analyze`

### Multi-User Household Support

✅ **Constitution Requirement** - While this feature is infrastructure-only, the environment setup supports future multi-user features:
- Backend will read `X-Hass-User` header for user identity
- Database schema will support user associations
- Shared household data visibility (no user isolation by default)

### Gate Result: **PASS** ✅

No constitution violations detected. All technical stack requirements and core principles are satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/001-project-scaffolding/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (technical decisions)
├── data-model.md        # Phase 1 output (infrastructure entities)
├── quickstart.md        # Phase 1 output (developer onboarding guide)
├── contracts/           # Phase 1 output (API specifications)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Web application structure (frontend + backend)

budget-frontend/
├── src/
│   ├── app/             # Next.js 14 app directory
│   ├── components/      # Reusable React components (Material-UI)
│   ├── services/        # API client services
│   ├── styles/          # Global styles and themes
│   └── types/           # TypeScript type definitions
├── public/              # Static assets
├── tests/               # Jest tests
├── package.json
├── next.config.js
├── tsconfig.json
└── Dockerfile           # Development container

budget-backend/
├── src/
│   ├── main/
│   │   ├── java/com/homebudget/
│   │   │   ├── config/          # Spring configuration
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── service/         # Business logic services
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── model/           # JPA entities
│   │   │   └── Application.java # Main entry point
│   │   └── resources/
│   │       ├── application.yml  # Spring Boot configuration
│   │       └── db/
│   │           └── changelog/   # Liquibase migrations
│   └── test/
│       ├── java/com/homebudget/ # JUnit tests
│       └── resources/
│           └── application-test.yml  # Test configuration (H2)
├── pom.xml              # Maven configuration
└── Dockerfile           # Development container

docker-compose.yml       # Orchestrates frontend, backend, MySQL
.env.example             # Environment variable template
.gitignore               # Git ignore patterns
README.md                # Developer onboarding guide
```

**Structure Decision**: Selected "Option 2: Web application (frontend + backend)" structure as this project has distinct Next.js frontend and Spring Boot backend applications that communicate via REST APIs. The `budget-frontend/` and `budget-backend/` directories house separate applications, each with their own build systems, dependencies, and deployment artifacts. The root `docker-compose.yml` orchestrates all services for local development.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations - this section is empty. The feature fully complies with all constitution requirements.
