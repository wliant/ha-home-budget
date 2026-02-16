# Implementation Plan: Bulk Upload Enhancement

**Branch**: `022-bulk-upload-enhancement` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/022-bulk-upload-enhancement/spec.md`

## Summary

Enhance the existing bulk upload feature to support a proper job lifecycle (UPLOADED → PROCESSING → PROCESSED/RETRYABLE/FAILED → COMPLETED), expandable job rows with inline-editable temporary records, merge/delete workflows for extracted records, and a "complete job" action that converts temporary records into actual expenses. This builds on the existing `ExpenseInputJob` / `TemporaryExpenseRecord` infrastructure, requiring status enum changes (rename INIT→UPLOADED, COMPLETED→PROCESSED, add new COMPLETED), three new backend endpoints (merge records, delete records, complete job), a database migration for status values, and a frontend page rewrite from flat table to expandable row layout with inline editing.

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript 5.x (frontend)
**Primary Dependencies**: Spring Boot 3.2.0, Spring Data JPA, Next.js 14.x, Material-UI v5, React 18.x, Axios
**Storage**: MySQL 8.0 (existing tables: `expense_input_jobs`, `temporary_expense_records`, `expense_files`)
**Testing**: Not requested
**Target Platform**: Private home network, Home Assistant add-on (nginx proxy)
**Project Type**: Web application (frontend + backend)
**Performance Goals**: Upload visible within 2s, status polling within 5s, inline edits without page reload
**Constraints**: X-Hass-User header authentication, multi-user household shared visibility
**Scale/Scope**: Single household, low concurrent users (<5)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | spec.md completed and clarified before planning |
| II. Clarify Before Planning | PASS | `/speckit.clarify` completed (1 question: processing trigger) |
| III. Incremental Story-Based Delivery | PASS | 5 user stories (3x P1, 2x P2) with independent tests |
| IV. Constitution Gates | PASS | Validating now; will re-check after Phase 1 |
| V. Task Traceability | PASS | Will enforce in tasks.md generation |
| VI. Test-Optional | PASS | Tests not requested in spec |
| VII. Artifact Consistency | PASS | Will validate via `/speckit.analyze` |
| Technical Stack: Next.js frontend | PASS | Using existing Next.js 14.x |
| Technical Stack: Spring Boot backend | PASS | Using existing Spring Boot 3.2.0 |
| Authentication: X-Hass-User | PASS | All endpoints read X-Hass-User header |
| Multi-user household | PASS | Jobs shared, createdBy tracked |

## Project Structure

### Documentation (this feature)

```text
specs/022-bulk-upload-enhancement/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── expense-input-api.yaml
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
budget-backend/
├── src/main/java/com/homebudget/
│   ├── controller/
│   │   └── ExpenseInputJobController.java    # Add merge, delete-records, complete endpoints
│   ├── dto/
│   │   ├── ExpenseInputJobDTO.java           # Update status values in mapping
│   │   ├── TemporaryExpenseRecordDTO.java    # No changes
│   │   ├── MergeTemporaryRecordsRequest.java # NEW: merge request DTO
│   │   ├── DeleteTemporaryRecordsRequest.java # NEW: delete records request DTO
│   │   └── UpdateTemporaryExpenseRecordRequest.java # No changes
│   ├── model/
│   │   ├── ExpenseInputJob.java              # Update Status enum (UPLOADED, PROCESSED, COMPLETED)
│   │   └── TemporaryExpenseRecord.java       # No changes
│   ├── repository/
│   │   ├── ExpenseInputJobRepository.java    # Add query for new statuses
│   │   └── TemporaryExpenseRecordRepository.java # Add deleteByIdIn
│   └── service/
│       ├── ExpenseInputJobService.java       # Update status transitions, add merge/delete/complete
│       └── OcrProcessorClient.java           # No changes
└── src/main/resources/db/changelog/changes/
    └── 013-update-job-status-enum.xml        # NEW: Liquibase migration

budget-frontend/
├── src/
│   ├── app/expenses/bulk-upload/
│   │   └── page.tsx                          # Complete rewrite: expandable rows, inline editing
│   └── services/
│       └── expenseInputJobService.ts         # Add merge, deleteRecords, completeJob methods
```

**Structure Decision**: Web application structure. Enhances existing files in `budget-backend/` and `budget-frontend/` directories. Three new DTO files, one new Liquibase migration. Frontend page is a complete rewrite; service has additions.

## Complexity Tracking

> No constitution violations detected. All changes use existing stack and patterns.
