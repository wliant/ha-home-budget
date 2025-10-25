# Data Model: Development Environment Setup

**Feature**: 001-project-scaffolding
**Date**: 2025-10-22
**Purpose**: Document infrastructure entities (no business domain entities in this feature)

## Overview

This feature focuses on development environment infrastructure and does not introduce business domain entities. This document outlines configuration entities and system-level structures required for the scaffolding.

---

## Infrastructure Configuration Entities

### 1. Environment Configuration

**Purpose**: Stores environment-specific configuration for development, test, and future production environments

**Structure**:
```yaml
# .env file structure (not a database entity)
FRONTEND_PORT: integer (default: 3000)
BACKEND_PORT: integer (default: 8080)
MYSQL_PORT: integer (default: 3306)
MYSQL_DATABASE: string (default: "homebudget")
MYSQL_USER: string (default: "budget_user")
MYSQL_PASSWORD: string (sensitive)
MYSQL_ROOT_PASSWORD: string (sensitive)
```

**Validation Rules**:
- Ports must be between 1024-65535
- Database name must be alphanumeric
- Passwords must not be empty

**State Transitions**: N/A (static configuration)

---

### 2. Docker Compose Service Definitions

**Purpose**: Defines the three services that comprise the development environment

**Services**:

```yaml
mysql:
  Type: Database Service
  Image: mysql:8.0
  Dependencies: None
  Health Check: mysqladmin ping
  Persistence: Volume (mysql-data)

backend:
  Type: Application Service
  Build Context: ./budget-backend
  Dependencies: mysql (healthy)
  Exposed Ports: 8080
  Mount: Source code (hot reload)

frontend:
  Type: Application Service
  Build Context: ./budget-frontend
  Dependencies: backend (started)
  Exposed Ports: 3000
  Mount: Source code (hot reload)
```

**Relationships**:
- frontend → depends on → backend
- backend → depends on → mysql
- All services communicate via Docker network

---

### 3. Liquibase Changelog Metadata

**Purpose**: Tracks applied database migrations (managed by Liquibase)

**Liquibase System Tables** (auto-created):
```sql
DATABASECHANGELOG:
  - ID: string (migration identifier)
  - AUTHOR: string (who created migration)
  - FILENAME: string (changelog file path)
  - DATEEXECUTED: timestamp
  - ORDEREXECUTED: integer
  - EXECTYPE: enum (EXECUTED, FAILED, SKIPPED)
  - MD5SUM: string (checksum for change detection)

DATABASECHANGELOGLOCK:
  - ID: integer (lock identifier)
  - LOCKED: boolean
  - LOCKGRANTED: timestamp
  - LOCKEDBY: string (instance identifier)
```

**Validation Rules**:
- Migrations must have unique ID + AUTHOR + FILENAME combination
- Checksums must match for previously executed migrations
- Only one instance can hold migration lock

**State Transitions**:
- PENDING → EXECUTING → EXECUTED (success)
- PENDING → EXECUTING → FAILED (error)

---

## Application Configuration Entities

### 4. Spring Boot Application Properties

**Purpose**: Backend application configuration

**Key Properties**:
```yaml
spring.application.name: string ("home-budget-backend")
spring.datasource.url: string (JDBC URL)
spring.datasource.username: string
spring.datasource.password: string (sensitive)
spring.jpa.hibernate.ddl-auto: enum (none, validate, update, create-drop)
spring.liquibase.change-log: string (path to master changelog)
server.port: integer (8080)
```

**Profile-Specific**:
- **default (development)**: Liquibase enabled, MySQL datasource
- **test**: Liquibase disabled, H2 in-memory datasource

---

### 5. Next.js Configuration

**Purpose**: Frontend application configuration

**Key Properties**:
```javascript
// next.config.js
{
  reactStrictMode: boolean (true),
  output: string ("standalone" for Docker),
  env: {
    NEXT_PUBLIC_API_URL: string (backend URL)
  }
}
```

---

## No Business Domain Entities

This feature is infrastructure-only. Business domain entities will be introduced in future features:
- **Future**: User entity (for tracking household members)
- **Future**: Budget entity (for household budgets)
- **Future**: Expense entity (for expense tracking)
- **Future**: Category entity (for spending categories)

---

## Database Schema (Initial State)

After infrastructure setup, the database will contain only Liquibase metadata tables:

```sql
-- Initial schema (empty except Liquibase tables)
SHOW TABLES;
+-------------------------------+
| Tables_in_homebudget          |
+-------------------------------+
| DATABASECHANGELOG             |
| DATABASECHANGELOGLOCK         |
+-------------------------------+
```

---

## Migration File Structure

**Liquibase Changelog Organization**:
```
budget-backend/src/main/resources/db/changelog/
├── db.changelog-master.xml          # Master changelog (includes all)
└── changes/
    └── 001-initial-setup.xml        # Creates metadata tables (auto-generated)
```

**Future Migration Pattern**:
```xml
<!-- Example future migration -->
<changeSet id="002" author="developer">
  <createTable tableName="users">
    <column name="id" type="bigint" autoIncrement="true">
      <constraints primaryKey="true"/>
    </column>
    <column name="username" type="varchar(255)">
      <constraints nullable="false" unique="true"/>
    </column>
    <column name="created_by" type="varchar(255)"/>
    <column name="created_date" type="timestamp"/>
  </createTable>
</changeSet>
```

---

## Entity Relationship Diagram

Since this feature has no business entities, the ERD shows only infrastructure relationships:

```
┌─────────────────┐
│  Docker Compose │
│   (Orchestrator)│
└────────┬────────┘
         │
         ├── manages ──> ┌──────────┐
         │              │  MySQL   │
         │              │  Service │
         │              └────┬─────┘
         │                   │
         │                   │ contains
         │                   ▼
         │              ┌──────────────────┐
         │              │ Liquibase Tables │
         │              │  (CHANGELOG)     │
         │              └──────────────────┘
         │
         ├── manages ──> ┌──────────────┐
         │              │ Spring Boot  │
         │              │   Backend    │
         │              └──────┬───────┘
         │                     │
         │                     │ reads config
         │                     ▼
         │              ┌──────────────────┐
         │              │ application.yml  │
         │              └──────────────────┘
         │
         └── manages ──> ┌──────────────┐
                        │  Next.js     │
                        │  Frontend    │
                        └──────┬───────┘
                               │
                               │ reads config
                               ▼
                        ┌──────────────────┐
                        │ next.config.js   │
                        └──────────────────┘
```

---

## Summary

This feature establishes infrastructure configuration and metadata entities only:

| Entity | Type | Purpose | Persistence |
|--------|------|---------|-------------|
| Environment Config | Configuration | Dev environment settings | .env file |
| Service Definitions | Configuration | Docker container specs | docker-compose.yml |
| Liquibase Metadata | Database | Migration tracking | MySQL tables |
| Spring Config | Configuration | Backend settings | application.yml |
| Next.js Config | Configuration | Frontend settings | next.config.js |

**No business domain entities are introduced in this feature.** All business logic entities (Users, Budgets, Expenses, Categories) will be defined in subsequent features after the development environment is operational.
