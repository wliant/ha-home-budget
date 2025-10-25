# Tasks: Development Environment Setup

**Input**: Design documents from `/specs/001-project-scaffolding/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are NOT explicitly requested in the specification. This feature is infrastructure-focused. Test tasks are omitted per constitution Principle VI.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app structure**: `budget-frontend/`, `budget-backend/` at repository root
- Docker Compose orchestration at root level

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure for both frontend and backend applications

- [X] T001 Create root project structure with budget-frontend/ and budget-backend/ directories
- [X] T002 [P] Create .gitignore file at repository root with Node.js, Java, and Docker patterns
- [X] T003 [P] Create .env.example file at repository root with FRONTEND_PORT, BACKEND_PORT, MYSQL configuration
- [X] T004 [P] Create README.md at repository root with quick start instructions from quickstart.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create docker-compose.yml at repository root with mysql, backend, and frontend services
- [X] T006 [P] Configure MySQL service in docker-compose.yml with health check and volume persistence
- [X] T007 [P] Initialize Next.js 14 project in budget-frontend/ with TypeScript and Material-UI dependencies
- [X] T008 [P] Initialize Spring Boot 3.2 Maven project in budget-backend/ with Java 17
- [X] T009 [P] Add Spring Boot dependencies to budget-backend/pom.xml (Spring Web, Spring Data JPA, Liquibase, MySQL Connector, H2, DevTools)
- [X] T010 [P] Add Material-UI v5 dependencies to budget-frontend/package.json
- [X] T011 [P] Create Dockerfile for budget-backend with Maven and Java 17 base image
- [X] T012 [P] Create Dockerfile for budget-frontend with Node.js base image
- [X] T013 Configure Spring Boot application.yml in budget-backend/src/main/resources/ with MySQL datasource configuration
- [X] T014 [P] Create application-test.yml in budget-backend/src/test/resources/ with H2 in-memory database configuration
- [X] T015 [P] Configure Next.js next.config.js in budget-frontend/ with standalone output for Docker
- [X] T016 [P] Create Material-UI theme configuration in budget-frontend/src/styles/theme.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Developer Onboarding (Priority: P1) 🎯 MVP

**Goal**: Enable developers to start the entire development environment (frontend, backend, database) with a single command

**Independent Test**: Clone repository, run `docker-compose up -d`, access http://localhost:3000 in browser, verify frontend loads and can communicate with backend at http://localhost:8080/actuator/health

### Implementation for User Story 1

- [X] T017 [P] [US1] Create Spring Boot main application class in budget-backend/src/main/java/com/homebudget/Application.java
- [X] T018 [P] [US1] Create health check controller in budget-backend/src/main/java/com/homebudget/controller/HealthController.java exposing /actuator/health
- [X] T019 [P] [US1] Enable Spring Boot Actuator in budget-backend/pom.xml and application.yml
- [X] T020 [P] [US1] Create Next.js root page in budget-frontend/src/app/page.tsx with Material-UI welcome component
- [X] T021 [P] [US1] Create API client service in budget-frontend/src/services/api.ts for backend communication
- [X] T022 [P] [US1] Add CORS configuration in budget-backend/src/main/java/com/homebudget/config/CorsConfig.java to allow frontend origin
- [X] T023 [US1] Configure docker-compose.yml backend service with depends_on mysql health check
- [X] T024 [US1] Configure docker-compose.yml frontend service with depends_on backend and API_URL environment variable
- [X] T025 [US1] Add health check integration in frontend to call backend /actuator/health on page load
- [X] T026 [US1] Configure volume mounts in docker-compose.yml for hot reload (frontend source, backend source)
- [X] T027 [US1] Test full stack startup: `docker-compose up -d` completes within 60 seconds
- [X] T028 [US1] Verify frontend accessible at http://localhost:3000 and displays Material-UI welcome page
- [X] T029 [US1] Verify backend health endpoint responds with status UP at http://localhost:8080/actuator/health
- [X] T030 [US1] Verify MySQL connection succeeds (backend logs show successful database connection)

**Checkpoint**: At this point, User Story 1 should be fully functional - developers can start environment with one command

---

## Phase 4: User Story 2 - Automated Testing Setup (Priority: P2)

**Goal**: Enable developers to run automated backend tests using H2 in-memory database without affecting development MySQL database

**Independent Test**: Run `docker-compose exec backend ./mvnw test` and verify all tests execute successfully using H2, test execution completes in < 30 seconds, development MySQL database remains unaffected

### Implementation for User Story 2

- [X] T031 [P] [US2] Create sample JUnit test class in budget-backend/src/test/java/com/homebudget/HealthControllerTest.java
- [X] T032 [P] [US2] Add @SpringBootTest annotation and test profile configuration to test class
- [X] T033 [US2] Implement health endpoint test that verifies /actuator/health returns UP status
- [X] T034 [US2] Configure Maven Surefire plugin in budget-backend/pom.xml for test execution
- [X] T035 [US2] Verify H2 database auto-configuration for test profile (MODE=MySQL compatibility)
- [X] T036 [US2] Add Maven wrapper (mvnw) to budget-backend/ if not present
- [X] T037 [US2] Test execution: Run `./mvnw test` inside backend container and verify tests pass
- [X] T038 [US2] Test isolation: Verify running tests does not affect development MySQL database
- [X] T039 [US2] Test cleanup: Verify H2 database is cleaned up after test execution (check logs)
- [X] T040 [US2] Performance verification: Confirm test suite executes in < 30 seconds

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - environment starts with one command, tests run in isolation

---

## Phase 5: User Story 3 - Database Schema Management (Priority: P3)

**Goal**: Enable automatic database schema initialization and versioned migrations via Liquibase so all developers stay synchronized

**Independent Test**: Start backend with empty database, verify Liquibase creates DATABASECHANGELOG tables automatically; add a migration file, restart backend, verify migration is applied automatically

### Implementation for User Story 3

- [X] T041 [P] [US3] Create Liquibase master changelog file in budget-backend/src/main/resources/db/changelog/db.changelog-master.xml
- [X] T042 [P] [US3] Create initial Liquibase migration in budget-backend/src/main/resources/db/changelog/changes/001-initial-setup.xml
- [X] T043 [US3] Configure Liquibase properties in application.yml (change-log path, enabled: true)
- [X] T044 [US3] Disable Liquibase for test profile in application-test.yml (use Hibernate DDL instead)
- [X] T045 [US3] Start backend and verify Liquibase creates DATABASECHANGELOG and DATABASECHANGELOGLOCK tables in MySQL
- [X] T046 [US3] Verify Liquibase migration logs show successful execution of 001-initial-setup
- [X] T047 [US3] Test migration workflow: Add new changeset 002-test-migration.xml, restart backend, verify it applies
- [X] T048 [US3] Test rollback capability: Verify Liquibase can rollback migrations if needed
- [X] T049 [US3] Test team synchronization: Simulate developer pulling new migration, verify automatic application on startup
- [X] T050 [US3] Document migration workflow in README.md (how to add new migrations)

**Checkpoint**: All user stories should now be independently functional - environment starts, tests run isolated, schema migrations auto-apply

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and finalize the development environment

- [X] T051 [P] Create comprehensive README.md at repository root based on quickstart.md (prerequisites, quick start, troubleshooting)
- [X] T052 [P] Add logging configuration to budget-backend/src/main/resources/logback-spring.xml for development debugging
- [X] T053 [P] Configure Spring Boot DevTools in budget-backend for automatic restart on code changes
- [X] T054 [P] Configure Next.js Fast Refresh for frontend hot reload (verify next.config.js settings)
- [X] T055 [P] Add Home Assistant authentication header filter stub in budget-backend/src/main/java/com/homebudget/filter/HassUserHeaderFilter.java (reads X-Hass-User)
- [X] T056 [P] Add error handling for port conflicts in docker-compose.yml (document in README)
- [X] T057 [P] Add database connection error handling in backend (graceful failure messages)
- [X] T058 [P] Create .dockerignore files in budget-frontend/ and budget-backend/ to exclude build artifacts
- [X] T059 Test hot reload frontend: Edit page.tsx, verify browser auto-refreshes within 2 seconds
- [X] T060 Test hot reload backend: Edit Java file, verify backend restarts within 5 seconds
- [X] T061 Test data persistence: Stop containers with `docker-compose down`, restart, verify MySQL data persists
- [X] T062 Test port conflict handling: Simulate port 3000 in use, verify clear error message
- [X] T063 Run full quickstart.md validation: Execute all 7 integration testing scenarios from quickstart.md
- [X] T064 Performance validation: Verify environment startup time < 60 seconds (measure with `time docker-compose up -d`)
- [X] T065 [P] Update CLAUDE.md with project-specific development environment notes (already done by update-agent-context.sh)
- [X] T066 Final integration test: Clone to fresh directory, follow README, verify complete setup in < 5 minutes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Depends on backend from US1 but adds independent testing capability
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on backend/MySQL from US1 but adds independent migration capability

### Within Each User Story

- Backend components before frontend integration
- Configuration before implementation
- Core functionality before verification tasks
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1 (Setup)**: All 4 tasks marked [P] can run in parallel
- **Phase 2 (Foundational)**: 11 of 12 tasks marked [P] can run in parallel (except T013 depends on T008)
- **User Story 1**: 9 of 14 tasks marked [P] can run in parallel (T017-T022 are independent)
- **User Story 2**: 7 of 10 tasks marked [P] can run in parallel (T031-T036 are independent)
- **User Story 3**: 2 of 10 tasks marked [P] can run in parallel (T041-T042)
- **Phase 6 (Polish)**: 9 of 17 tasks marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members after Foundational is complete

---

## Parallel Example: Foundational Phase

```bash
# Launch all independent foundational tasks together:
Task: "Configure MySQL service in docker-compose.yml"
Task: "Initialize Next.js 14 project in budget-frontend/"
Task: "Initialize Spring Boot 3.2 Maven project in budget-backend/"
Task: "Add Spring Boot dependencies to pom.xml"
Task: "Add Material-UI dependencies to package.json"
Task: "Create Dockerfile for budget-backend"
Task: "Create Dockerfile for budget-frontend"
Task: "Create application-test.yml with H2 configuration"
Task: "Configure next.config.js with standalone output"
Task: "Create Material-UI theme configuration"
# Then run sequential task T013 (application.yml) after backend is initialized
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (4 tasks)
2. Complete Phase 2: Foundational (12 tasks) - CRITICAL blocking phase
3. Complete Phase 3: User Story 1 (14 tasks)
4. **STOP and VALIDATE**: Test `docker-compose up -d`, access frontend, verify backend health
5. This is the MVP - developers can now onboard and start development

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (16 tasks)
2. Add User Story 1 → Test independently → **MVP delivered** (30 tasks total)
3. Add User Story 2 → Test independently → Automated testing enabled (40 tasks total)
4. Add User Story 3 → Test independently → Schema management enabled (50 tasks total)
5. Add Polish → Production-ready development environment (66 tasks total)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (16 tasks)
2. Once Foundational is done:
   - Developer A: User Story 1 (Developer Onboarding)
   - Developer B: User Story 2 (Testing Setup)
   - Developer C: User Story 3 (Schema Management)
3. Stories complete and integrate independently
4. Team converges on Polish phase together

---

## Task Summary

- **Total Tasks**: 66
- **Phase 1 (Setup)**: 4 tasks (all parallelizable)
- **Phase 2 (Foundational)**: 12 tasks (11 parallelizable)
- **Phase 3 (US1 - Developer Onboarding)**: 14 tasks (9 parallelizable) 🎯 MVP
- **Phase 4 (US2 - Testing Setup)**: 10 tasks (7 parallelizable)
- **Phase 5 (US3 - Schema Management)**: 10 tasks (2 parallelizable)
- **Phase 6 (Polish)**: 17 tasks (9 parallelizable)
- **Parallel Opportunities**: 42 of 66 tasks (64%) can run in parallel
- **MVP Scope**: 30 tasks (Setup + Foundational + US1)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests are NOT included per constitution Principle VI (not explicitly requested in spec)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Use quickstart.md as integration test suite for final validation
