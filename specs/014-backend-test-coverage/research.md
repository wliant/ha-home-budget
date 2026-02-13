# Research: Backend Test Coverage Improvement

**Feature**: 014-backend-test-coverage
**Date**: 2026-02-11

## Decision 1: Code Coverage Tool

**Decision**: JaCoCo (Java Code Coverage)

**Rationale**: JaCoCo is the de facto standard for Java code coverage in Maven projects. It integrates natively with Maven via `jacoco-maven-plugin`, supports line, branch, and instruction coverage metrics, produces HTML/XML/CSV reports, and works seamlessly with both Surefire (unit tests) and Failsafe (integration/E2E tests). It uses a Java agent for bytecode instrumentation, requiring zero production code changes.

**Alternatives Considered**:
- **Cobertura**: Older tool, less actively maintained, limited Java 17 support
- **Clover**: Commercial (Atlassian), overkill for this project
- **OpenClover**: Open-source Clover fork, smaller community than JaCoCo
- **IntelliJ built-in coverage**: IDE-only, not reproducible in CI/build

**Configuration Approach**:
- Use `prepare-agent` goal in `initialize` phase to instrument classes
- Use `report` goal in `verify` phase to generate HTML report after all tests run
- Use merged execution data from both Surefire and Failsafe runs
- Exclude DTOs, entity models, exception classes, and Application main class from coverage enforcement (they contain mostly getters/setters)

## Decision 2: Controller Unit Testing Approach

**Decision**: MockMvc with @WebMvcTest (slice test)

**Rationale**: @WebMvcTest loads only the web layer (controllers, filters, exception handlers) without the full application context, making tests fast and focused. MockMvc enables testing HTTP request/response mapping, content negotiation, validation, and error handling without a running server. Service dependencies are mocked with @MockBean.

**Alternatives Considered**:
- **Full @SpringBootTest with MockMvc**: Loads entire context unnecessarily for unit-level controller tests; slower startup
- **TestRestTemplate**: Requires running server; better suited for E2E tests (already used there)
- **WebTestClient**: Reactive stack; this project uses servlet-based Spring MVC

**Key Considerations**:
- @WebMvcTest auto-configures MockMvc, Jackson, validation, and exception handlers
- Must explicitly include GlobalExceptionHandler with @Import if not auto-detected
- Filters (HassUserHeaderFilter, CorrelationIdFilter) may need explicit inclusion or exclusion depending on test intent
- X-Hass-User header should be tested at controller level to verify correct parameter binding

## Decision 3: Infrastructure Test Approach

**Decision**: Mixed approach - plain JUnit 5 for utilities, MockMvc for filters/interceptors

**Rationale**: Infrastructure classes vary significantly in their dependencies:
- **SensitiveDataMasker, LogContext**: Pure Java utilities with no Spring dependencies - plain JUnit 5 + AssertJ
- **HassUserHeaderFilter, CorrelationIdFilter, AuthHeaderInterceptor**: Servlet filter chain - use MockFilterChain/MockHttpServletRequest from spring-test
- **PerformanceLoggingAspect**: AOP - test with real Spring context (@SpringBootTest) to verify aspect weaving, or use mock ProceedingJoinPoint for unit-level
- **LoggingInterceptor**: HandlerInterceptor - use MockHttpServletRequest/Response
- **GlobalExceptionHandler**: @RestControllerAdvice - test via MockMvc to verify HTTP response mapping

**Alternatives Considered**:
- **Full @SpringBootTest for all infrastructure**: Overkill for pure utilities; slow startup for simple assertions
- **Mockito-only for filters**: Misses servlet chain behavior; MockFilterChain provides more realistic testing

## Decision 4: JaCoCo Coverage Exclusion Patterns

**Decision**: Exclude the following from coverage enforcement (but still measure):
- `com/homebudget/Application.class` (main entry point)
- `com/homebudget/model/*` (JPA entities - mostly getters/setters/lifecycle)
- `com/homebudget/dto/*` (DTOs - mostly getters/setters)
- `com/homebudget/exception/*Exception.class` (custom exceptions - constructors only)

**Rationale**: These classes consist primarily of boilerplate code (getters, setters, constructors) that provides minimal behavioral value when tested. Including them in the coverage denominator unfairly inflates the effort needed to reach 75%. The exclusions follow the industry-standard practice of focusing coverage on behavioral code (controllers, services, repositories, infrastructure).

**Note**: GlobalExceptionHandler is NOT excluded - it contains meaningful routing logic that should be tested.

## Decision 5: Integration Test Strategy for ExpenseInputJob

**Decision**: Follow existing patterns - extend AbstractIntegrationTest, use @Transactional for isolation

**Rationale**: The existing test infrastructure is well-established and consistent. ExpenseInputJobService involves file I/O (MultipartFile processing), so integration tests will need:
- Testcontainers MySQL for database operations
- Mock MultipartFile objects for file upload simulation
- Temporary directory for file storage (cleaned up in @AfterEach)
- Real service-repository stack to validate the full workflow

**Key Challenge**: The `processPendingJobs()` method is @Scheduled - tests should call it directly rather than relying on scheduler timing.

## Decision 6: E2E Test for Bulk Upload Workflow

**Decision**: TestRestTemplate with multipart support

**Rationale**: The ExpenseInputJobController accepts MultipartFile uploads. TestRestTemplate supports multipart via `MultiValueMap<String, Object>` with `FileSystemResource` or `ByteArrayResource`. The E2E test should exercise the full workflow:
1. POST multipart files to create jobs
2. GET to verify jobs are listed
3. PATCH to update temporary records
4. POST /confirm to confirm jobs
5. Verify expenses are created

**Key Considerations**:
- Test files should be small (a few bytes) to keep tests fast
- Use `@TempDir` JUnit annotation for temporary file creation
- The scheduled `processPendingJobs()` method may need to be called manually in E2E tests to avoid race conditions
