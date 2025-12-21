# Implementation Plan: Hierarchical Category Budgets

**Branch**: `004-hierarchical-category-budgets` | **Date**: 2025-11-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-hierarchical-category-budgets/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature enables hierarchical organization of spending categories with parent-child relationships and category-based budgeting. Users can create two-level category hierarchies (e.g., "Food" parent with "Groceries" and "Dining Out" children) and assign monthly budgets to each category. The system enforces validation where parent category budgets must equal the sum of child category budgets, ensuring budgetary consistency across the hierarchy.

**Technical Approach**: Extends existing Category and Budget entities with foreign key relationships. Backend implements validation logic for parent-child sum constraints and circular reference prevention. Frontend enhances category management UI with parent selection and hierarchical budget displays.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript/JavaScript (frontend)
**Primary Dependencies**:
- Backend: Spring Boot 3.2.0, Spring Data JPA, Liquibase, MySQL Connector
- Frontend: Next.js 14.0.4, React 18, Material-UI v5
**Storage**: MySQL 8.0 database with JPA/Hibernate ORM
**Testing**: JUnit 5 + Mockito (backend), Jest + React Testing Library (frontend) - when tests are explicitly requested
**Target Platform**: Docker containers for Home Assistant add-on deployment
**Project Type**: Web application (Spring Boot REST API + Next.js frontend)
**Performance Goals**: Budget validation under 2 seconds, category hierarchy rendering under 500ms for up to 100 categories
**Constraints**:
- Two-level hierarchy maximum (parent-child only)
- One budget per category per time period (year-month)
- Parent budget must equal sum of child budgets
- Backward compatible with existing budgets (which currently lack category association)
**Scale/Scope**:
- Support 10+ parent categories with 10+ children each
- Handle multiple concurrent users (household members)
- Maintain sub-second response times for budget operations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Specification-First ✅ PASS
- Specification in spec.md is technology-agnostic, focused on user needs
- No implementation details (frameworks, specific APIs) in requirements
- Written for business stakeholders understanding budgeting concepts

### Principle II: Clarify Before Planning ✅ PASS
- No [NEEDS CLARIFICATION] markers in specification
- All requirements have measurable acceptance criteria
- Vague terms avoided - specific validation rules defined

### Principle III: Incremental, Story-Based Delivery ✅ PASS
- 5 user stories prioritized P1-P3
- P1 stories (Hierarchical Category Management + Category-Based Budget Creation) form viable MVP
- Each story independently testable and delivers standalone value
- Story dependencies clear: US1 enables US2, which enables US3

### Principle IV: Constitution Gates ✅ PASS
- This gate check being performed before Phase 0
- Will re-validate after Phase 1 design artifacts generated
- No violations identified requiring justification

### Principle V: Task Traceability ⏳ PENDING
- Tasks.md not yet generated (Phase 2 output, created by /speckit.tasks)
- Will ensure strict format: `- [ ] [T###] [P] [US#] Description with file/path`
- Will trace all tasks to user stories via [US#] labels

### Principle VI: Test-Optional, Test-First When Included ✅ PASS
- Specification does not explicitly request automated tests
- Tests optional for this feature
- If tests added later, will follow TDD (contract tests before implementation)

### Principle VII: Artifact Consistency ⏳ PENDING
- Will validate via /speckit.analyze after task generation
- Will ensure requirements map to tasks, tasks map to requirements
- Will check terminology consistency across spec, plan, tasks

### Project-Specific Constraints ✅ PASS

**Technical Stack Compliance**:
- ✅ Using Spring Boot (Java) for backend REST APIs
- ✅ Using Next.js for frontend UI
- ✅ MySQL for persistence via JPA

**Authentication Integration**:
- ✅ Will rely on X-Hass-User header for user identity
- ✅ Budget ownership tracked via createdBy field (from X-Hass-User)
- ✅ No additional authentication layer required

**Deployment Environment**:
- ✅ Docker containerizable (existing setup maintained)
- ✅ Runs in private home network via Home Assistant
- ✅ No public internet exposure

**Multi-User Household**:
- ✅ Multiple users can create categories and budgets
- ✅ User identity captured in createdBy fields for audit
- ✅ Shared visibility (no user-level data isolation)

**GATE STATUS (Initial)**: ✅ PASSED - Proceeded to Phase 0 research
**GATE STATUS (Phase 1 Re-Check)**: ✅ PASSED - All design artifacts validated, ready for Phase 2

## Project Structure

### Documentation (this feature)

```text
specs/004-hierarchical-category-budgets/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── api-contract.md  # REST API changes and examples
├── checklists/
│   └── requirements.md  # Specification quality checklist (completed)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/
│   ├── java/com/homebudget/
│   │   ├── model/
│   │   │   ├── Category.java          # EXTEND: Add parentCategory relationship
│   │   │   └── Budget.java            # EXTEND: Add category relationship
│   │   ├── repository/
│   │   │   ├── CategoryRepository.java # EXTEND: Add hierarchy queries
│   │   │   └── BudgetRepository.java   # EXTEND: Add category filtering
│   │   ├── service/
│   │   │   ├── CategoryService.java    # EXTEND: Add parent-child logic
│   │   │   └── BudgetService.java      # EXTEND: Add validation logic
│   │   ├── dto/
│   │   │   ├── CategoryDTO.java        # EXTEND: Add parent/children fields
│   │   │   └── BudgetDTO.java          # EXTEND: Add category field
│   │   ├── controller/
│   │   │   ├── CategoryController.java # MODIFY: Support hierarchy operations
│   │   │   └── BudgetController.java   # MODIFY: Require category, validate sum
│   │   └── exception/
│   │       ├── ParentBudgetMismatchException.java # NEW
│   │       └── CircularCategoryException.java     # NEW
│   └── resources/
│       └── db/changelog/
│           └── changes/
│               └── 004-add-category-budgets.xml # NEW: Liquibase migration
└── src/test/ # Tests optional, only if explicitly requested

budget-frontend/
├── src/
│   ├── components/
│   │   ├── CategoryForm.tsx        # EXTEND: Add parent selection dropdown
│   │   ├── CategoryList.tsx        # EXTEND: Display hierarchy with nesting
│   │   ├── BudgetForm.tsx          # EXTEND: Add category selection, show validation
│   │   └── BudgetSummary.tsx       # EXTEND: Show category-based breakdown
│   ├── services/
│   │   ├── categoryService.ts      # EXTEND: API calls for hierarchy operations
│   │   └── budgetService.ts        # EXTEND: API calls with category filtering
│   └── types/
│       ├── category.ts             # EXTEND: Add parentId, children fields
│       └── budget.ts               # EXTEND: Add categoryId, category fields
└── tests/ # Tests optional, only if explicitly requested
```

**Structure Decision**: Web application structure selected based on existing Spring Boot backend + Next.js frontend architecture. Feature extends current entities (Category, Budget) rather than creating new ones, minimizing disruption. Liquibase migration ensures database schema changes are versioned and reversible.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations identified. Feature aligns with all constitution principles and project-specific constraints.

---

## Phase Completion Summary

### Phase 0: Research ✅ COMPLETED

**Output**: `research.md` with 6 technical decisions:
1. Self-referencing foreign key for category hierarchy
2. Service-layer budget sum validation
3. Application-level circular reference check
4. Nullable category FK with migration strategy
5. Recursive React component for hierarchy display
6. Cascading restriction with explicit reassignment

**Outcome**: All technical ambiguities resolved with clear rationale and implementation approach.

### Phase 1: Design & Contracts ✅ COMPLETED

**Outputs**:
- `data-model.md`: Entity definitions, relationships, validation rules, state transitions
- `contracts/api-contract.md`: REST API changes with 9 endpoint specifications, error formats, and example flows
- `quickstart.md`: 12 integration test scenarios covering all user stories, troubleshooting guide, performance tests
- `CLAUDE.md`: Updated with Java 17, TypeScript, MySQL 8.0 technical context

**Outcome**: Complete design documentation ready for implementation task generation.

### Constitution Re-Validation ✅ PASSED

All principles validated after Phase 1:
- ✅ **Principle I**: Specification remains technology-agnostic
- ✅ **Principle II**: No clarifications needed in spec
- ✅ **Principle III**: 5 user stories with clear priorities and dependencies
- ✅ **Principle IV**: Both gate checks passed
- ⏳ **Principle V**: Pending task generation (Phase 2)
- ✅ **Principle VI**: Tests optional, TDD approach if added
- ⏳ **Principle VII**: Will validate via `/speckit.analyze` after tasks

### Next Steps

**Ready for Phase 2**: Execute `/speckit.tasks` to generate `tasks.md` with:
- Dependency-ordered tasks for all 5 user stories
- Task format: `- [ ] [T###] [P#] [US#] Description (file/path)`
- Traceability from requirements → user stories → tasks
- Implementation guidance from research and data model decisions

**Artifacts Available**:
- ✅ Feature specification (`spec.md`)
- ✅ Quality checklist (`checklists/requirements.md`)
- ✅ Implementation plan (`plan.md`)
- ✅ Technical research (`research.md`)
- ✅ Data model (`data-model.md`)
- ✅ API contracts (`contracts/api-contract.md`)
- ✅ Integration tests (`quickstart.md`)
- ⏳ Task list (`tasks.md` - awaiting `/speckit.tasks`)

**Planning Complete**: All design artifacts generated and validated. Feature 004 ready for task generation and implementation.
