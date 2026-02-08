# Research: Comprehensive Backend Test Suite

**Feature**: 010-backend-test-suite
**Date**: 2026-02-08

## Decision 1: Unit Test Mocking Framework

**Decision**: Use Mockito (included in `spring-boot-starter-test`) with `@ExtendWith(MockitoExtension.class)` for unit tests.

**Rationale**: Mockito is already bundled with the project via `spring-boot-starter-test`. It integrates seamlessly with JUnit 5, supports `@Mock`, `@InjectMocks`, and `@Spy` annotations, and provides clear verification APIs. No additional dependency is needed.

**Alternatives considered**:
- **Spring MockMvc with @WebMvcTest**: Too heavy for service-layer unit tests — loads Spring context. Better suited for controller-layer tests.
- **Manual stubs**: More verbose, harder to maintain, no automatic verification.

## Decision 2: Integration Test Database Strategy

**Decision**: Use Testcontainers for MySQL (`org.testcontainers:mysql`) with Liquibase migrations for schema initialization.

**Rationale**: The user explicitly requested Testcontainers. MySQL Testcontainers provides production parity (same engine, same SQL dialect, same constraints). The existing Liquibase migrations will initialize the schema identically to production, catching migration-related issues early. The current H2 test profile uses `MODE=MySQL` but cannot replicate all MySQL-specific behaviors (e.g., `ON UPDATE CURRENT_TIMESTAMP`, specific unique constraint semantics).

**Alternatives considered**:
- **H2 in MySQL mode (current approach)**: Already configured but has dialect mismatches — `ON UPDATE CURRENT_TIMESTAMP` computed default works differently, some MySQL-specific features are unsupported.
- **Embedded MySQL**: Heavier than Testcontainers, less maintained, platform-specific issues.

## Decision 3: Testcontainers Configuration Pattern

**Decision**: Use a shared abstract base class (`AbstractIntegrationTest`) with `@Testcontainers` annotation and a static `MySQLContainer` with `@Container` to share one container across all integration test classes.

**Rationale**: Starting a MySQL container per test class adds ~5-10 seconds of overhead. A shared static container with `@DynamicPropertySource` to inject connection properties reduces total test execution time significantly. Each test still gets transaction isolation via `@Transactional` with automatic rollback.

**Alternatives considered**:
- **Container per test class**: Simpler isolation but much slower (each class starts a new container).
- **Container per test method**: Extremely slow, impractical for integration suites.
- **Singleton container pattern (manual)**: More boilerplate than `@Testcontainers` annotation approach.

## Decision 4: Test Tier Separation Strategy

**Decision**: Use Maven Surefire for unit tests (`*Test.java`) and Maven Failsafe for integration/E2E tests (`*IntegrationTest.java`, `*E2ETest.java`). Add the `maven-failsafe-plugin` to pom.xml.

**Rationale**: Maven Surefire (already configured at 3.0.0) runs unit tests during the `test` phase. Maven Failsafe runs integration tests during the `integration-test` phase, after the `package` phase. This separation ensures unit tests can be run independently (`mvn test`) and integration tests separately (`mvn verify`). Failsafe also handles pre/post-integration-test lifecycle phases for container management.

**Alternatives considered**:
- **Maven profiles**: More complex configuration, requires `-P` flag to select profile.
- **JUnit 5 tags with `@Tag`**: Works but requires surefire/failsafe configuration anyway to filter by tag.
- **Single plugin with includes/excludes**: Less standard, harder to maintain.

## Decision 5: E2E Test HTTP Client

**Decision**: Use Spring's `TestRestTemplate` with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` for E2E tests.

**Rationale**: E2E tests should exercise the full HTTP stack including servlet filters (CorrelationIdFilter, HassUserHeaderFilter), CORS configuration, and serialization. `TestRestTemplate` makes real HTTP calls through the embedded server, unlike `MockMvc` which bypasses the servlet container. Spring Boot auto-configures `TestRestTemplate` when using `RANDOM_PORT`.

**Alternatives considered**:
- **MockMvc**: Bypasses servlet container (no filter chain, no real HTTP). Good for controller tests but doesn't qualify as true E2E.
- **WebTestClient**: Reactive-oriented, works but project uses traditional servlet stack.
- **RestAssured**: External dependency, adds unnecessary complexity when `TestRestTemplate` suffices.

## Decision 6: Test Data Cleanup Strategy

**Decision**: Use `@Transactional` with automatic rollback for integration tests. For E2E tests (which use `TestRestTemplate` on a real server), use `@DirtiesContext` or explicit cleanup via `@BeforeEach` with repository `deleteAll()`.

**Rationale**: `@Transactional` on integration test classes causes Spring to rollback after each test method automatically — this is the standard approach and requires no cleanup code. E2E tests with `TestRestTemplate` run in a separate thread from the server, so `@Transactional` rollback doesn't work — explicit cleanup is needed.

**Alternatives considered**:
- **`@Sql` scripts**: Works but more fragile (schema changes require script updates).
- **`@DirtiesContext` on every test**: Rebuilds entire context each time — very slow.
- **Database truncation utility**: Custom code, more maintenance.

## Decision 7: Testcontainers MySQL Version

**Decision**: Use MySQL 8.0 container image (`mysql:8.0`) to match production.

**Rationale**: Production uses MySQL 8.0 (documented in CLAUDE.md). Using the same version ensures test fidelity for MySQL-specific features like `ON UPDATE CURRENT_TIMESTAMP` defaults, JSON columns, and window functions.

**Alternatives considered**:
- **MySQL 8.4 (latest)**: Could introduce behavior differences with production.
- **MySQL 5.7**: Older version, missing features used in production.

## Decision 8: Liquibase in Testcontainers

**Decision**: Enable Liquibase in the Testcontainers test profile (`application-integration-test.yml`) to run the same migrations as production.

**Rationale**: The current H2 test profile disables Liquibase and uses `hibernate.ddl-auto=create-drop`. For Testcontainers integration tests, enabling Liquibase ensures the schema matches production exactly, including indexes, constraints, and seed data (e.g., the "Uncategorized" system category). This catches migration-related issues.

**Alternatives considered**:
- **Hibernate `create-drop`**: Creates schema from JPA annotations, may differ from Liquibase migrations.
- **Manual SQL scripts**: Duplicates migration logic, drift risk.

## Decision 9: Assertion Library

**Decision**: Use AssertJ (included in `spring-boot-starter-test`) as the primary assertion library.

**Rationale**: AssertJ provides fluent, readable assertions with strong IDE support. It's already bundled with Spring Boot test starter. Compared to JUnit 5 assertions, it offers better error messages, collection assertions, and exception assertions.

**Alternatives considered**:
- **JUnit 5 Assertions**: Built-in but less expressive for complex assertions.
- **Hamcrest**: Also bundled but less fluent than AssertJ.
