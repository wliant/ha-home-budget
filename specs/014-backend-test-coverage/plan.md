# Implementation Plan: Backend Test Coverage Improvement

**Branch**: `014-backend-test-coverage` | **Date**: 2026-02-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/014-backend-test-coverage/spec.md`

## Summary

Improve backend Spring Boot test coverage to 75%+ by: (1) auditing and fixing all existing tests, (2) adding unit tests for untested controllers and services, (3) adding integration tests for untested repositories, (4) adding E2E tests for the bulk upload workflow, (5) adding infrastructure/cross-cutting tests, and (6) configuring JaCoCo for coverage measurement and enforcement. No production code changes required.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Spring MVC, Testcontainers 1.19.3
**Storage**: MySQL 8.0 (via Testcontainers for tests), H2 (in-memory for unit tests)
**Testing**: JUnit 5 (Jupiter), Mockito, AssertJ, MockMvc, TestRestTemplate, Testcontainers, JaCoCo (new)
**Target Platform**: Linux container (Home Assistant add-on)
**Project Type**: Backend-only (Spring Boot)
**Performance Goals**: Unit tests < 60s, full suite < 10 minutes
**Constraints**: No production code changes; Docker must be running for integration/E2E tests
**Scale/Scope**: ~54 main source files, ~16 existing test files, ~18 new test files planned

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Phase 0 Check

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Specification-First | PASS | spec.md complete with 6 user stories, 13 FRs, 8 SCs |
| II. Clarify Before Planning | PASS | /speckit.clarify ran; no ambiguities found |
| III. Incremental Story-Based Delivery | PASS | 6 stories: P1 (US1,US2,US6), P2 (US3,US4), P3 (US5) |
| IV. Constitution Gates | PASS | Running this check now |
| V. Task Traceability | N/A | Tasks not yet generated |
| VI. Test-Optional, Test-First | PASS | Feature IS about tests; explicitly requested |
| VII. Artifact Consistency | N/A | Analysis runs post-tasks |
| Technical Stack: Spring Boot | PASS | Backend-only feature using existing Spring Boot 3.2.0 |
| Technical Stack: Next.js | N/A | No frontend changes |
| Authentication: X-Hass-User | PASS | E2E tests validate header-based auth |
| Multi-User Support | PASS | Tests use "test-user" identity via X-Hass-User |

### Post-Phase 1 Re-check

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Specification-First | PASS | No changes to spec |
| III. Incremental Story-Based Delivery | PASS | Each US can be implemented and tested independently |
| IV. Constitution Gates | PASS | All gates pass |
| VI. Test-Optional, Test-First | PASS | Tests ARE the deliverable |
| Technical Stack | PASS | Only adds JaCoCo plugin to existing pom.xml |

**Result**: All gates PASS. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/014-backend-test-coverage/
├── plan.md              # This file
├── research.md          # Phase 0: Technology decisions
├── data-model.md        # Phase 1: Entities under test
├── quickstart.md        # Phase 1: Test execution guide
├── contracts/           # Phase 1: Existing API endpoints under test
└── tasks.md             # Phase 2: Task breakdown (via /speckit.tasks)
```

### Source Code (repository root)

```text
budget-backend/
├── pom.xml                          # MODIFIED: Add JaCoCo plugin (US6)
└── src/test/java/com/homebudget/
    ├── config/
    │   ├── AbstractIntegrationTest.java          # EXISTING base class
    │   └── SmokeIntegrationTest.java             # EXISTING smoke test
    ├── controller/
    │   ├── HealthControllerTest.java             # EXISTING
    │   ├── BudgetControllerTest.java             # NEW (US2) - @WebMvcTest
    │   ├── CategoryControllerTest.java           # NEW (US2) - @WebMvcTest
    │   ├── ExpenseControllerTest.java            # NEW (US2) - @WebMvcTest
    │   └── ExpenseInputJobControllerTest.java    # NEW (US2) - @WebMvcTest
    ├── service/
    │   ├── BudgetServiceTest.java                # EXISTING unit test
    │   ├── CategoryServiceTest.java              # EXISTING unit test
    │   ├── ExpenseServiceTest.java               # EXISTING unit test
    │   ├── ExpenseInputJobServiceTest.java       # NEW (US2) - Mockito
    │   ├── BudgetServiceIntegrationTest.java     # EXISTING integration
    │   ├── CategoryServiceIntegrationTest.java   # EXISTING integration
    │   ├── ExpenseServiceIntegrationTest.java    # EXISTING integration
    │   └── ExpenseInputJobServiceIntegrationTest.java # NEW (US3) - Testcontainers
    ├── repository/
    │   ├── BudgetRepositoryIntegrationTest.java            # EXISTING
    │   ├── CategoryRepositoryIntegrationTest.java          # EXISTING
    │   ├── ExpenseRepositoryIntegrationTest.java           # EXISTING
    │   ├── ExpenseFileRepositoryIntegrationTest.java       # NEW (US3)
    │   ├── ExpenseInputJobRepositoryIntegrationTest.java   # NEW (US3)
    │   └── TemporaryExpenseRecordRepositoryIntegrationTest.java # NEW (US3)
    ├── e2e/
    │   ├── BudgetE2ETest.java                    # EXISTING
    │   ├── CategoryE2ETest.java                  # EXISTING
    │   ├── ExpenseE2ETest.java                   # EXISTING
    │   └── ExpenseInputJobE2ETest.java           # NEW (US4)
    ├── exception/
    │   └── GlobalExceptionHandlerTest.java       # NEW (US2) - MockMvc
    ├── filter/
    │   ├── HassUserHeaderFilterTest.java         # NEW (US5) - MockFilterChain
    │   ├── CorrelationIdFilterTest.java          # NEW (US5) - MockFilterChain
    │   └── AuthHeaderInterceptorTest.java        # NEW (US5) - MockFilterChain
    ├── logging/
    │   ├── PerformanceLoggingAspectTest.java     # NEW (US5) - Mock JoinPoint
    │   └── LoggingInterceptorTest.java           # NEW (US5) - MockMvc
    └── util/
        ├── TestLogAppender.java                  # EXISTING utility
        ├── SensitiveDataMaskerTest.java          # NEW (US5) - Plain JUnit
        └── LogContextTest.java                   # NEW (US5) - Plain JUnit
```

**Structure Decision**: Backend-only. All changes are in `budget-backend/src/test/` with the single exception of `pom.xml` (JaCoCo plugin configuration). No production source code changes.

## Architecture & Design

### Test Layer Strategy

```
┌─────────────────────────────────────────────────────────────┐
│  E2E Tests (@SpringBootTest + RANDOM_PORT + TestRestTemplate)│
│  → Full HTTP stack, MySQL Testcontainers                    │
│  → Tests: BudgetE2E, CategoryE2E, ExpenseE2E,              │
│           ExpenseInputJobE2E (NEW)                          │
├─────────────────────────────────────────────────────────────┤
│  Integration Tests (@SpringBootTest + Testcontainers)       │
│  → Real DB, real service/repository wiring                  │
│  → Tests: *ServiceIntegration, *RepositoryIntegration       │
├─────────────────────────────────────────────────────────────┤
│  Controller Unit Tests (@WebMvcTest + @MockBean)            │
│  → MockMvc, mocked services, tests HTTP mapping/validation  │
│  → Tests: BudgetController, CategoryController, etc. (NEW)  │
├─────────────────────────────────────────────────────────────┤
│  Service Unit Tests (@ExtendWith(MockitoExtension) + Mocks) │
│  → Fast, isolated, mock repositories                        │
│  → Tests: BudgetService, CategoryService, ExpenseService,   │
│           ExpenseInputJobService (NEW)                      │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Tests (Plain JUnit / MockFilterChain)       │
│  → Filters, interceptors, aspects, utilities                │
│  → Tests: HassUserHeaderFilter, CorrelationIdFilter, etc.   │
└─────────────────────────────────────────────────────────────┘
```

### JaCoCo Configuration

Add to `pom.xml`:
- `jacoco-maven-plugin` with `prepare-agent` and `report` goals
- Execution data merged from Surefire + Failsafe
- HTML report at `target/site/jacoco/index.html`
- Exclusions: Application.class, model/*, dto/*, exception/*Exception.class

### Controller Test Pattern (@WebMvcTest)

Each controller test:
1. Uses `@WebMvcTest(SpecificController.class)` for slice loading
2. Mocks service layer with `@MockBean`
3. Tests each endpoint for:
   - Happy path (correct status code, response body)
   - Validation errors (400 Bad Request)
   - Not found scenarios (404)
   - X-Hass-User header binding
4. Imports `GlobalExceptionHandler` to verify error responses

### Infrastructure Test Pattern

| Class | Approach | Key Assertions |
|-------|----------|----------------|
| HassUserHeaderFilter | MockFilterChain + MockHttpServletRequest | Header extraction, MDC propagation |
| CorrelationIdFilter | MockFilterChain + MockHttpServletRequest | UUID generation, MDC set/clear |
| AuthHeaderInterceptor | MockFilterChain + MockHttpServletRequest | Header forwarding behavior |
| PerformanceLoggingAspect | Mock ProceedingJoinPoint | Timing, slow method detection, log output |
| LoggingInterceptor | MockHttpServletRequest/Response | Request/response logging |
| SensitiveDataMasker | Plain JUnit 5 | Pattern matching, masking output |
| LogContext | Plain JUnit 5 | MDC set/get/clear |
| GlobalExceptionHandler | MockMvc (via controller tests) | Status codes, error response structure |

## Implementation Phases

### Phase 1: Foundation (US6 + US1) - Coverage Tool + Test Audit

1. Configure JaCoCo in pom.xml
2. Run existing test suite, identify and fix any failures
3. Generate baseline coverage report

### Phase 2: Unit Tests (US2) - Controllers + Services

1. ExpenseInputJobServiceTest (Mockito)
2. BudgetControllerTest (@WebMvcTest)
3. CategoryControllerTest (@WebMvcTest)
4. ExpenseControllerTest (@WebMvcTest)
5. ExpenseInputJobControllerTest (@WebMvcTest)
6. GlobalExceptionHandlerTest (MockMvc)

### Phase 3: Integration Tests (US3) - Repositories + Services

1. ExpenseFileRepositoryIntegrationTest
2. ExpenseInputJobRepositoryIntegrationTest
3. TemporaryExpenseRecordRepositoryIntegrationTest
4. ExpenseInputJobServiceIntegrationTest

### Phase 4: E2E Tests (US4) - API Flows

1. ExpenseInputJobE2ETest (full bulk upload workflow)
2. Review existing E2E tests for completeness

### Phase 5: Infrastructure Tests (US5) - Cross-Cutting

1. HassUserHeaderFilterTest
2. CorrelationIdFilterTest
3. AuthHeaderInterceptorTest
4. PerformanceLoggingAspectTest
5. LoggingInterceptorTest
6. SensitiveDataMaskerTest
7. LogContextTest

### Phase 6: Verification

1. Run full test suite (`mvn verify`)
2. Verify 75%+ coverage
3. If below 75%, identify and address remaining gaps

## Complexity Tracking

> No constitution violations. No complexity justifications needed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | — | — |
