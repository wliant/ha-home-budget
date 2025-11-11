# Tasks: Development Mode Default User Header

**Input**: Design documents from `/specs/003-dev-mode-default-header/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are OPTIONAL and not explicitly requested in the specification. Test tasks are not included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `budget-backend/src/main/java/com/homebudget/`, `budget-frontend/src/`
- Configuration: `budget-backend/src/main/resources/`, `docker-compose.yml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configuration files and infrastructure setup

- [ ] T001 Create application-dev.properties in budget-backend/src/main/resources/
- [ ] T002 [P] Create HeaderModifyingRequestWrapper.java in budget-backend/src/main/java/com/homebudget/util/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Create AuthHeaderInterceptor.java in budget-backend/src/main/java/com/homebudget/config/
- [ ] T004 Register AuthHeaderInterceptor in WebMvcConfigurer in budget-backend/src/main/java/com/homebudget/config/
- [ ] T005 Configure dev-mode properties in budget-backend/src/main/resources/application.properties
- [ ] T006 Configure dev-mode properties in budget-backend/src/main/resources/application-dev.properties

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Local Development Without Authentication Setup (Priority: P1) 🎯 MVP

**Goal**: Developers can make API requests without X-Hass-User header in development mode, and the application uses a default user identity instead of throwing errors.

**Independent Test**: Start application with SPRING_PROFILES_ACTIVE=dev, make POST /api/budgets without X-Hass-User header, verify budget is created with createdBy="dev-user"

### Implementation for User Story 1

- [ ] T007 [US1] Implement HeaderModifyingRequestWrapper.getHeader() method in budget-backend/src/main/java/com/homebudget/util/HeaderModifyingRequestWrapper.java
- [ ] T008 [US1] Implement HeaderModifyingRequestWrapper.getHeaders() method in budget-backend/src/main/java/com/homebudget/util/HeaderModifyingRequestWrapper.java
- [ ] T009 [US1] Implement HeaderModifyingRequestWrapper.getHeaderNames() method in budget-backend/src/main/java/com/homebudget/util/HeaderModifyingRequestWrapper.java
- [ ] T010 [US1] Implement AuthHeaderInterceptor.preHandle() method with dev mode check in budget-backend/src/main/java/com/homebudget/config/AuthHeaderInterceptor.java
- [ ] T011 [US1] Add empty header handling (treat empty header same as missing) in budget-backend/src/main/java/com/homebudget/config/AuthHeaderInterceptor.java
- [ ] T012 [US1] Update docker-compose.yml to set SPRING_PROFILES_ACTIVE=dev for backend service

**Checkpoint**: At this point, User Story 1 should be fully functional - API requests without X-Hass-User header work in dev mode

---

## Phase 4: User Story 2 - Override Default User in Development (Priority: P2)

**Goal**: Developers can provide explicit X-Hass-User header to override the default development user when testing multi-user scenarios.

**Independent Test**: Start application in dev mode, make POST /api/budgets with X-Hass-User: alice header, verify budget is created with createdBy="alice" (not "dev-user")

### Implementation for User Story 2

- [ ] T013 [US2] Add header precedence logic in AuthHeaderInterceptor.preHandle() - explicit header takes priority over default in budget-backend/src/main/java/com/homebudget/config/AuthHeaderInterceptor.java
- [ ] T014 [US2] Verify all controllers honor explicit X-Hass-User header in development mode (BudgetController, ExpenseController, CategoryController)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently - both default user and explicit user headers work correctly

---

## Phase 5: User Story 3 - Clear Development Mode Indicators (Priority: P3)

**Goal**: Developers see clear log messages indicating when the application is using development mode default authentication behavior.

**Independent Test**: Start application in dev mode, check startup logs for "DEVELOPMENT MODE" message; make request without header, check logs for "Adding default X-Hass-User header" DEBUG message

### Implementation for User Story 3

- [ ] T015 [P] [US3] Add startup logging in HomeBudgetApplication.main() or @PostConstruct method in budget-backend/src/main/java/com/homebudget/HomeBudgetApplication.java
- [ ] T016 [P] [US3] Add per-request DEBUG logging in AuthHeaderInterceptor.preHandle() when default user is applied in budget-backend/src/main/java/com/homebudget/config/AuthHeaderInterceptor.java
- [ ] T017 [US3] Add production mode startup logging in HomeBudgetApplication to indicate X-Hass-User header is required

**Checkpoint**: All user stories should now be independently functional - development mode is clearly indicated in logs

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and production safety verification

- [ ] T018 [P] Verify production mode behavior: start backend without dev profile, attempt request without X-Hass-User header, confirm it fails with 400 error
- [ ] T019 [P] Verify production mode behavior: start backend without dev profile, make request with X-Hass-User: alice, confirm it succeeds
- [ ] T020 [P] Test edge case: empty X-Hass-User header in dev mode should use default user
- [ ] T021 [P] Test edge case: whitespace-only X-Hass-User header in dev mode should use default user
- [ ] T022 [P] Run quickstart.md Scenario 1 validation (dev mode without header)
- [ ] T023 [P] Run quickstart.md Scenario 2 validation (dev mode with explicit header)
- [ ] T024 [P] Run quickstart.md Scenario 3 validation (production mode enforcement)
- [ ] T025 [P] Run quickstart.md Scenario 4 validation (development mode indicators in logs)
- [ ] T026 Update README.md or development documentation with dev mode usage instructions

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
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Enhances US1 but US1 must be implemented first for testing
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Can be implemented independently of US1/US2

### Within Each User Story

- **US1**: Wrapper methods (T007-T009) can be done in parallel, then interceptor logic (T010-T011), then docker-compose update (T012)
- **US2**: Sequential tasks that modify interceptor logic
- **US3**: All logging tasks can be done in parallel (T015-T017)

### Parallel Opportunities

- Phase 1: T001 and T002 can run in parallel (different files)
- Phase 2: T005 and T006 can run in parallel after T003-T004 (different property files)
- US3: T015, T016, T017 can all run in parallel (different methods/classes)
- Phase 6: All validation and testing tasks (T018-T025) can run in parallel

---

## Parallel Example: User Story 1

```bash
# After foundational phase is complete:

# These can run in parallel (different methods in same class):
Task T007: "Implement HeaderModifyingRequestWrapper.getHeader() method"
Task T008: "Implement HeaderModifyingRequestWrapper.getHeaders() method"
Task T009: "Implement HeaderModifyingRequestWrapper.getHeaderNames() method"

# Then these run sequentially (same class, dependent logic):
Task T010: "Implement AuthHeaderInterceptor.preHandle() with dev mode check"
Task T011: "Add empty header handling in AuthHeaderInterceptor"

# Finally:
Task T012: "Update docker-compose.yml"
```

---

## Parallel Example: User Story 3

```bash
# All logging tasks can run in parallel (different files/methods):

Task T015: "Add startup logging in HomeBudgetApplication"
Task T016: "Add per-request DEBUG logging in AuthHeaderInterceptor"
Task T017: "Add production mode startup logging in HomeBudgetApplication"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002) - ~15 minutes
2. Complete Phase 2: Foundational (T003-T006) - ~30 minutes
3. Complete Phase 3: User Story 1 (T007-T012) - ~45 minutes
4. **STOP and VALIDATE**: Test User Story 1 independently using quickstart.md Scenario 1
5. Deploy/demo if ready - **This is a usable MVP!**

**Time Estimate for MVP**: ~1.5 hours

### Incremental Delivery

1. Complete Setup + Foundational (Phases 1-2) → Foundation ready
2. Add User Story 1 (Phase 3) → Test independently → Deploy/Demo (MVP!) 🎯
3. Add User Story 2 (Phase 4) → Test independently → Deploy/Demo
4. Add User Story 3 (Phase 5) → Test independently → Deploy/Demo
5. Complete Polish (Phase 6) → Full validation → Final deployment

**Total Time Estimate**: ~3 hours for all user stories + validation

### Parallel Team Strategy

With 3 developers:

1. Team completes Setup + Foundational together (~45 minutes)
2. Once Foundational is done:
   - Developer A: User Story 1 (T007-T012)
   - Developer B: User Story 3 (T015-T017) - can start immediately
   - Developer C: User Story 2 (T013-T014) - waits for US1 completion
3. Stories complete and integrate independently

With 2 developers:

1. Team completes Setup + Foundational together
2. Developer A: User Story 1
3. Developer B: User Story 3 (parallel with US1)
4. Developer A: User Story 2 (after US1 complete)
5. Both: Phase 6 validation tasks in parallel

---

## Production Safety Checklist

Before deploying to production:

- [ ] Verify default configuration is secure: `app.dev-mode=false` in application.properties
- [ ] Verify production deployment does NOT set SPRING_PROFILES_ACTIVE=dev
- [ ] Test production mode rejects requests without X-Hass-User header (T018)
- [ ] Test production mode accepts requests with explicit X-Hass-User header (T019)
- [ ] Verify startup logs show "PRODUCTION MODE" when dev profile not active
- [ ] Confirm docker-compose.yml for production does not include SPRING_PROFILES_ACTIVE=dev

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- No frontend changes needed - backend interceptor handles everything
- Tests are optional and not included per specification

---

## Task Count Summary

- **Phase 1 (Setup)**: 2 tasks (~15 minutes)
- **Phase 2 (Foundational)**: 4 tasks (~30 minutes)
- **Phase 3 (US1 - MVP)**: 6 tasks (~45 minutes)
- **Phase 4 (US2)**: 2 tasks (~15 minutes)
- **Phase 5 (US3)**: 3 tasks (~20 minutes)
- **Phase 6 (Polish)**: 9 tasks (~45 minutes)

**Total**: 26 tasks (~3 hours)
**MVP Only** (Phases 1-3): 12 tasks (~1.5 hours)

**Parallel Opportunities**: 11 tasks can run in parallel with proper coordination
**Independent User Stories**: 3 stories can be implemented and tested independently
