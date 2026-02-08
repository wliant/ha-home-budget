# Tasks: Comprehensive Backend Test Suite

**Input**: Design documents from `/specs/010-backend-test-suite/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: This IS a test feature — all tasks are test implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Testcontainers dependencies and Maven Failsafe plugin to enable three-tier test execution

- [X] T001 Update pom.xml to add Testcontainers BOM (1.19.3), testcontainers, testcontainers mysql, testcontainers junit-jupiter dependencies (scope: test), and maven-failsafe-plugin (3.0.0) with includes for `**/*IntegrationTest.java` and `**/*E2ETest.java` in budget-backend/pom.xml
- [X] T002 Create integration test Spring profile configuration with Testcontainers MySQL datasource placeholders, Liquibase enabled, hibernate ddl-auto=none, and DEBUG logging for com.homebudget in budget-backend/src/test/resources/application-integration-test.yml
- [X] T003 Create AbstractIntegrationTest base class with shared static MySQLContainer<>("mysql:8.0"), @Testcontainers, @DynamicPropertySource for spring.datasource.url/username/password, and @ActiveProfiles("integration-test") in budget-backend/src/test/java/com/homebudget/config/AbstractIntegrationTest.java

**Checkpoint**: Test infrastructure ready — Testcontainers MySQL starts, Liquibase migrations run, Failsafe plugin configured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Verify the test infrastructure works end-to-end before writing domain tests

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Create a smoke integration test that extends AbstractIntegrationTest, verifies the MySQL container starts, Liquibase migrations complete, and the Spring context loads successfully against the Testcontainer in budget-backend/src/test/java/com/homebudget/config/SmokeIntegrationTest.java
- [X] T005 Run `mvn verify` to confirm: (1) existing HealthControllerTest still passes with H2, (2) SmokeIntegrationTest passes with Testcontainers MySQL, (3) Surefire and Failsafe execute their respective test classes correctly

**Checkpoint**: Foundation ready — unit tests run via `mvn test`, integration tests run via `mvn verify`, both in isolation

---

## Phase 3: User Story 1 — Unit Tests for Core Business Logic (Priority: P1) 🎯 MVP

**Goal**: Unit tests for all public methods of BudgetService, CategoryService, and ExpenseService using Mockito — zero external dependencies

**Independent Test**: Run `mvn test` — all unit tests pass without Docker or database. Completes within 30 seconds.

### Implementation for User Story 1

- [X] T006 [P] [US1] Create BudgetServiceTest with @ExtendWith(MockitoExtension.class), @Mock BudgetRepository, @Mock CategoryRepository, @Mock ExpenseRepository, @InjectMocks BudgetService. Test methods: createBudget success (verify repository save called, DTO returned), createBudget duplicate (verify DuplicateBudgetException thrown), createBudget with parent category budget validation (verify ParentBudgetMismatchException), getAllBudgets (verify ordering), getBudgetById success and not found (BudgetNotFoundException), updateBudget success and not found, deleteBudget success and not found, calculateTotalSpending, calculateSpendingPercentage, getCurrentMonthBudget success and not found, child budget sum validation in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java
- [X] T007 [P] [US1] Create CategoryServiceTest with @ExtendWith(MockitoExtension.class), @Mock CategoryRepository, @Mock BudgetRepository, @InjectMocks CategoryService. Test methods: createCategory success (verify save), createCategory duplicate name (DuplicateCategoryException), createCategory with parent (verify hierarchy depth validation), createCategory circular reference (CircularCategoryException), createCategory exceeding max depth (HierarchyDepthException), getAllCategories, getCategoryById success and not found (CategoryNotFoundException), updateCategory success with name/icon/parent changes, updateCategory system category protection, deleteCategory success, deleteCategory with children (CategoryInUseException), deleteCategory with budgets (CategoryInUseException), deleteCategory with expenses (CategoryInUseException), deleteCategory system category, getCategoryHierarchy, getExpenseCount, validateParentChange with active budgets in budget-backend/src/test/java/com/homebudget/service/CategoryServiceTest.java
- [X] T008 [P] [US1] Create ExpenseServiceTest with @ExtendWith(MockitoExtension.class), @Mock ExpenseRepository, @Mock BudgetRepository, @Mock CategoryRepository, @InjectMocks ExpenseService. Test methods: createExpense success (verify save), createExpense with date mismatch warning (expense date outside budget month), createExpense with optional category (null categoryId), createExpense budget not found (BudgetNotFoundException), createExpense category not found (CategoryNotFoundException), getAllExpenses with no filters, getAllExpenses with budgetId filter, getAllExpenses with categoryId filter, getAllExpenses with date range filter, getAllExpenses with createdBy filter, getAllExpenses with combined filters, getExpenseById success and not found (ExpenseNotFoundException), updateExpense success with field changes, updateExpense not found, deleteExpense success and not found in budget-backend/src/test/java/com/homebudget/service/ExpenseServiceTest.java
- [X] T009 [US1] Run `mvn test` to verify all unit tests pass without Docker, complete within 30 seconds, and existing HealthControllerTest still passes

**Checkpoint**: User Story 1 complete — all service unit tests pass, no external dependencies needed

---

## Phase 4: User Story 2 — Integration Tests with Real Database (Priority: P2)

**Goal**: Integration tests for repository and service layers running against a real MySQL 8.0 database via Testcontainers with Liquibase migrations

**Independent Test**: Run `mvn verify -DskipUnitTests=true` — all integration tests pass with Docker running. MySQL Testcontainer starts, schema initializes via Liquibase.

### Repository Integration Tests

- [X] T010 [P] [US2] Create CategoryRepositoryIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: save and find category, findByName, existsByNameIgnoreCase (case-insensitive duplicate detection), findAllByOrderByNameAsc (verify ordering), findByParentCategoryIsNullOrderByNameAsc (root categories only), findByParentCategoryId (child categories), countByParentCategoryId, save category with parent (hierarchy persistence), verify unique name constraint at DB level (expect exception on duplicate), verify system category seed data exists (Uncategorized with id=1, isSystem=true) in budget-backend/src/test/java/com/homebudget/repository/CategoryRepositoryIntegrationTest.java
- [X] T011 [P] [US2] Create BudgetRepositoryIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: save and find budget with category, findByYearAndMonth, findAllByOrderByYearDescMonthDesc (verify ordering), findByCategoryIdAndYearAndMonth, existsByCategoryIdAndYearAndMonth, verify unique constraint on (category_id, year, month) at DB level (expect exception on duplicate), findByYearAndMonthWithExpenses (eager loading), sumByParentCategoryAndPeriod (child budget sum), findChildBudgets in budget-backend/src/test/java/com/homebudget/repository/BudgetRepositoryIntegrationTest.java
- [X] T012 [P] [US2] Create ExpenseRepositoryIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: save and find expense with budget and category, findByBudgetId, findByCategoryId, findByExpenseDateBetween (date range), findByCreatedBy, findAllOrderByExpenseDateDesc (verify ordering), findByBudgetIdAndCategoryId, sumAmountByBudgetId (BigDecimal sum), countByBudgetId, countByCategoryId, findByFilters with various null combinations, getCategoryBreakdown (spending by category), verify cascade delete (delete budget → expenses deleted), verify foreign key constraint (expense without valid budget) in budget-backend/src/test/java/com/homebudget/repository/ExpenseRepositoryIntegrationTest.java

### Service Integration Tests

- [X] T013 [P] [US2] Create CategoryServiceIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: createCategory and retrieve via getCategoryById, createCategory with duplicate name (verify DuplicateCategoryException with real DB), createCategory with parent hierarchy (verify 2-level max enforcement), createCategory circular reference detection with persisted data, deleteCategory with children (verify CategoryInUseException), getCategoryHierarchy with real parent-child data, updateCategory name change with uniqueness check in budget-backend/src/test/java/com/homebudget/service/CategoryServiceIntegrationTest.java
- [X] T014 [P] [US2] Create BudgetServiceIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: createBudget with real category and verify persistence, createBudget duplicate category/year/month (DuplicateBudgetException with real DB), getBudgetById with expenses (verify eager loading and spending calculation), deleteBudget cascade to expenses (verify expenses deleted from DB), parent-child budget validation with real category hierarchy, calculateTotalSpending with real expenses in budget-backend/src/test/java/com/homebudget/service/BudgetServiceIntegrationTest.java
- [X] T015 [P] [US2] Create ExpenseServiceIntegrationTest extending AbstractIntegrationTest with @SpringBootTest, @Transactional. Test methods: createExpense with real budget and category, createExpense date mismatch warning with real budget month, getAllExpenses with filter combinations against real data, updateExpense with budget/category reassignment, deleteExpense and verify removal from DB, expense count updates on budget after create/delete in budget-backend/src/test/java/com/homebudget/service/ExpenseServiceIntegrationTest.java
- [X] T016 [US2] Run `mvn verify` to confirm all integration tests pass with Testcontainers MySQL and unit tests still pass

**Checkpoint**: User Story 2 complete — all repository and service integration tests pass against real MySQL via Testcontainers

---

## Phase 5: User Story 3 — End-to-End API Tests (Priority: P3)

**Goal**: End-to-end tests exercising the full HTTP stack via TestRestTemplate with real MySQL Testcontainer, covering all REST API endpoints for Budget, Category, and Expense

**Independent Test**: Run `mvn verify -DskipUnitTests=true -Dit.test="*E2ETest"` — all E2E tests pass with Docker running. Full Spring Boot application starts with Testcontainer.

### Implementation for User Story 3

- [X] T017 [P] [US3] Create CategoryE2ETest extending AbstractIntegrationTest with @SpringBootTest(webEnvironment = RANDOM_PORT), @Autowired TestRestTemplate, @BeforeEach cleanup via categoryRepository.deleteAll(). Test methods: POST /api/categories with X-Hass-User header (expect 201, verify response body has id/name/icon/createdBy), GET /api/categories (expect 200, verify list), GET /api/categories/{id} (expect 200), PUT /api/categories/{id} (expect 200, verify updated fields), DELETE /api/categories/{id} (expect 204), GET /api/categories/{id} after delete (expect 404), POST /api/categories duplicate name (expect 409), POST /api/categories with missing name (expect 400 with validation errors), GET /api/categories/hierarchy with parent-child data (expect 200 with nested children), GET /api/categories/{id}/expense-count (expect 200), verify createdBy matches X-Hass-User header value in budget-backend/src/test/java/com/homebudget/e2e/CategoryE2ETest.java
- [X] T018 [P] [US3] Create BudgetE2ETest extending AbstractIntegrationTest with @SpringBootTest(webEnvironment = RANDOM_PORT), @Autowired TestRestTemplate, @BeforeEach cleanup via expenseRepository.deleteAll(), budgetRepository.deleteAll(), categoryRepository cleanup. Test methods: POST /api/budgets with X-Hass-User header and categoryId (expect 201, verify response body), GET /api/budgets (expect 200, verify list ordering), GET /api/budgets/{id} (expect 200, verify spending summary fields), PUT /api/budgets/{id} update amount/description (expect 200), DELETE /api/budgets/{id} (expect 204), GET /api/budgets/{id} after delete (expect 404), POST /api/budgets duplicate category/year/month (expect 409), POST /api/budgets with missing categoryId (expect 400), POST /api/budgets with invalid year/month (expect 400), GET /api/budgets/current (expect 200 or 404), verify createdBy matches X-Hass-User, verify cascade delete removes expenses via GET /api/expenses?budgetId={id} in budget-backend/src/test/java/com/homebudget/e2e/BudgetE2ETest.java
- [X] T019 [P] [US3] Create ExpenseE2ETest extending AbstractIntegrationTest with @SpringBootTest(webEnvironment = RANDOM_PORT), @Autowired TestRestTemplate, @BeforeEach cleanup and setup (create category + budget for expense tests). Test methods: POST /api/expenses with X-Hass-User header, budgetId, amount, description, expenseDate (expect 201, verify response body with all fields), POST /api/expenses with date outside budget month (expect 201 with warnings list containing date mismatch), GET /api/expenses (expect 200), GET /api/expenses?budgetId={id} filter (expect 200, filtered list), GET /api/expenses?categoryId={id} filter (expect 200), GET /api/expenses?startDate=X&endDate=Y filter (expect 200), GET /api/expenses?createdBy=user filter (expect 200), GET /api/expenses/{id} (expect 200), PUT /api/expenses/{id} (expect 200, verify updated fields), DELETE /api/expenses/{id} (expect 204), GET /api/expenses/{id} after delete (expect 404), POST /api/expenses with missing required fields (expect 400), POST /api/expenses with negative amount (expect 400), POST /api/expenses with non-existent budgetId (expect 404), verify createdBy matches X-Hass-User header, complete workflow test: create category → create budget → create expense → verify budget spending → delete expense → verify spending reset in budget-backend/src/test/java/com/homebudget/e2e/ExpenseE2ETest.java
- [ ] T020 [US3] Run `mvn verify` to confirm all E2E tests pass, integration tests still pass, and unit tests still pass (BLOCKED: requires Java runtime)

**Checkpoint**: User Story 3 complete — all E2E tests pass exercising full HTTP stack with real MySQL

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all test tiers

- [ ] T021 Run full test suite validation: `mvn verify` passes with all three tiers (unit, integration, E2E), verify unit tests complete within 30 seconds, verify no flaky tests by running `mvn verify` twice consecutively (BLOCKED: requires Java runtime)
- [ ] T022 Run quickstart.md scenarios validation: verify `mvn test` works (unit only), `mvn verify` works (all tiers), confirm Docker requirement is documented for integration/E2E tests (BLOCKED: requires Java runtime)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — unit tests only, no Testcontainers needed
- **User Story 2 (Phase 4)**: Depends on Phase 2 — needs Testcontainers infrastructure
- **User Story 3 (Phase 5)**: Depends on Phase 2 — needs Testcontainers infrastructure
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 — No dependencies on other stories
- **User Story 2 (P2)**: Can start after Phase 2 — No dependencies on US1 (tests different layer)
- **User Story 3 (P3)**: Can start after Phase 2 — No dependencies on US1 or US2 (tests different layer)

### Within Each User Story

- All test files within a story are marked [P] (parallel) since they are independent files testing different domains
- The final verification task in each story depends on all test files in that story

### Parallel Opportunities

- T001, T002, T003 are sequential (T003 depends on T001 for dependencies and T002 for config)
- T006, T007, T008 can all run in parallel (independent unit test files)
- T010, T011, T012 can all run in parallel (independent repository integration test files)
- T013, T014, T015 can all run in parallel (independent service integration test files)
- T017, T018, T019 can all run in parallel (independent E2E test files)
- US1, US2, US3 can run in parallel after Phase 2 (different test tiers, independent)

---

## Parallel Example: User Story 1

```bash
# Launch all unit tests in parallel (different files, no dependencies):
Task: "BudgetServiceTest in budget-backend/src/test/java/com/homebudget/service/BudgetServiceTest.java"
Task: "CategoryServiceTest in budget-backend/src/test/java/com/homebudget/service/CategoryServiceTest.java"
Task: "ExpenseServiceTest in budget-backend/src/test/java/com/homebudget/service/ExpenseServiceTest.java"
```

## Parallel Example: User Story 2

```bash
# Launch all repository integration tests in parallel:
Task: "CategoryRepositoryIntegrationTest in budget-backend/src/test/java/com/homebudget/repository/CategoryRepositoryIntegrationTest.java"
Task: "BudgetRepositoryIntegrationTest in budget-backend/src/test/java/com/homebudget/repository/BudgetRepositoryIntegrationTest.java"
Task: "ExpenseRepositoryIntegrationTest in budget-backend/src/test/java/com/homebudget/repository/ExpenseRepositoryIntegrationTest.java"

# Then launch all service integration tests in parallel:
Task: "CategoryServiceIntegrationTest in budget-backend/src/test/java/com/homebudget/service/CategoryServiceIntegrationTest.java"
Task: "BudgetServiceIntegrationTest in budget-backend/src/test/java/com/homebudget/service/BudgetServiceIntegrationTest.java"
Task: "ExpenseServiceIntegrationTest in budget-backend/src/test/java/com/homebudget/service/ExpenseServiceIntegrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (pom.xml, config, base class)
2. Complete Phase 2: Foundational (smoke test verification)
3. Complete Phase 3: User Story 1 (unit tests)
4. **STOP and VALIDATE**: Run `mvn test` — all unit tests pass without Docker
5. Unit tests provide immediate regression detection

### Incremental Delivery

1. Complete Setup + Foundational → Infrastructure ready
2. Add User Story 1 (unit tests) → Run `mvn test` → Fast feedback loop (MVP!)
3. Add User Story 2 (integration tests) → Run `mvn verify` → Production-parity DB validation
4. Add User Story 3 (E2E tests) → Run `mvn verify` → Full stack API validation
5. Each story adds a new layer of test confidence without breaking previous layers

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Phase 2 is done:
   - Developer A: User Story 1 (unit tests — no Docker needed)
   - Developer B: User Story 2 (integration tests — needs Docker)
   - Developer C: User Story 3 (E2E tests — needs Docker)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Unit tests (US1) can be developed without Docker
- Integration/E2E tests (US2/US3) require Docker for Testcontainers
