# Technical Research: Development Environment Setup

**Feature**: 001-project-scaffolding
**Date**: 2025-10-22
**Purpose**: Document technical decisions and best practices for establishing the development environment

## Research Areas

This document consolidates research findings for all technical decisions required to implement the project scaffolding feature.

---

## 1. Docker Compose Orchestration

### Decision
Use Docker Compose v2 with multi-stage Dockerfiles for development environment orchestration

### Rationale
- Industry standard for local multi-container development
- Single command (`docker-compose up`) starts all services
- Service dependency management (wait-for-it patterns for MySQL readiness)
- Volume persistence for MySQL data between restarts
- Hot reload support for both Next.js and Spring Boot
- Environment variable management via `.env` files

### Alternatives Considered
- **Manual Docker commands**: Rejected - requires multiple commands, error-prone
- **Kubernetes (minikube)**: Rejected - overkill for local development, slower startup
- **Podman Compose**: Rejected - less widespread adoption, compatibility issues

### Implementation Notes
```yaml
# Key docker-compose.yml structure:
services:
  mysql:
    image: mysql:8.0
    volumes: mysql-data:/var/lib/mysql
    healthcheck: mysqladmin ping

  backend:
    build: ./budget-backend
    depends_on: mysql condition:service_healthy
    volumes: ./budget-backend:/app (hot reload)

  frontend:
    build: ./budget-frontend
    volumes: ./budget-frontend:/app (hot reload)
    depends_on: backend
```

---

## 2. Next.js 14 Configuration

### Decision
Use Next.js 14.x with App Router, TypeScript, and Material-UI v5

### Rationale
- **App Router**: Next.js 14's recommended approach, better performance
- **TypeScript**: Type safety, better IDE support, prevents runtime errors
- **Material-UI (MUI) v5**: Most popular React UI framework, comprehensive components, aligns with "modern design system" requirement
- **Server Components**: Default in App Router, better performance for household budget app

### Alternatives Considered
- **Pages Router**: Rejected - App Router is newer standard
- **JavaScript**: Rejected - TypeScript provides better developer experience
- **Ant Design**: Rejected - MUI has larger ecosystem
- **Chakra UI**: Rejected - smaller component library

### Implementation Notes
```javascript
// next.config.js key settings:
module.exports = {
  reactStrictMode: true,
  output: 'standalone', // For Docker containerization
}

// Material-UI theme customization for household budget aesthetics
// Hot reload enabled by default in development mode
```

---

## 3. Spring Boot 3.x Configuration

### Decision
Spring Boot 3.2.x with Java 17, Maven, Spring Data JPA, and Liquibase

### Rationale
- **Spring Boot 3.x**: Latest LTS, native support, better performance
- **Java 17**: LTS version, modern language features, required for Spring Boot 3
- **Maven**: More widespread in enterprise Java than Gradle
- **Spring Data JPA**: Simplifies database operations, reduces boilerplate
- **Liquibase**: Database migration tool, better than Flyway for complex schemas

### Alternatives Considered
- **Spring Boot 2.x**: Rejected - reaching end of support
- **Java 11**: Rejected - Java 17 is current LTS
- **Gradle**: Rejected - Maven more familiar to most Java developers
- **Flyway**: Rejected - Liquibase has better rollback support

### Implementation Notes
```xml
<!-- pom.xml key dependencies -->
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.0</version>
</parent>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
  </dependency>
</dependencies>
```

---

## 4. MySQL Development Database

### Decision
MySQL 8.0 in Docker container with persistent volumes

### Rationale
- **MySQL 8.0**: Production-grade, compatible with Home Assistant ecosystem
- **Docker volume**: Persists data between container restarts
- **Port mapping**: 3306 exposed to host for debugging tools (MySQL Workbench)
- **Health checks**: Ensures MySQL is ready before backend starts

### Alternatives Considered
- **PostgreSQL**: Rejected - MySQL more common in home automation space
- **MariaDB**: Rejected - MySQL has broader tool support
- **In-memory H2 for dev**: Rejected - need production-like environment

### Implementation Notes
```yaml
# MySQL configuration in docker-compose.yml
mysql:
  image: mysql:8.0
  environment:
    MYSQL_ROOT_PASSWORD: dev_root_password
    MYSQL_DATABASE: homebudget
    MYSQL_USER: budget_user
    MYSQL_PASSWORD: budget_password
  volumes:
    - mysql-data:/var/lib/mysql
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 5s
    timeout: 3s
    retries: 10
```

---

## 5. H2 In-Memory Test Database

### Decision
H2 database for backend unit and integration tests

### Rationale
- **In-memory**: Fast test execution (< 30 seconds target)
- **MySQL compatibility mode**: H2 can emulate MySQL syntax
- **No external dependencies**: Tests run in CI/CD without MySQL container
- **Automatic cleanup**: Database recreated for each test run

### Alternatives Considered
- **Testcontainers MySQL**: Rejected - slower, requires Docker in CI/CD
- **Shared MySQL for tests**: Rejected - test isolation compromised

### Implementation Notes
```yaml
# application-test.yml (test profile)
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  liquibase:
    enabled: false  # Use Hibernate DDL for tests
```

---

## 6. Liquibase Database Migrations

### Decision
Liquibase with XML changelog format for database versioning

### Rationale
- **Version control**: All schema changes tracked in git
- **Automatic application**: Runs on backend startup
- **Rollback support**: Can undo migrations if needed
- **Team synchronization**: Developers automatically get latest schema
- **XML format**: More structured than SQL, better for code reviews

### Alternatives Considered
- **Flyway**: Rejected - less flexible rollback support
- **JPA hibernate.ddl-auto**: Rejected - not suitable for production
- **Manual SQL scripts**: Rejected - no automatic tracking

### Implementation Notes
```xml
<!-- db/changelog/db.changelog-master.xml -->
<databaseChangeLog>
  <include file="db/changelog/001-initial-schema.xml"/>
  <include file="db/changelog/002-add-user-tracking.xml"/>
</databaseChangeLog>

<!-- Liquibase runs automatically on Spring Boot startup -->
```

---

## 7. Material-UI (MUI) Theme Configuration

### Decision
Material-UI v5 with custom theme for household budget application aesthetics

### Rationale
- **Component library**: 100+ pre-built components (tables, forms, dialogs)
- **Theming system**: Consistent colors, typography, spacing across app
- **Responsive**: Built-in breakpoints for mobile/tablet/desktop
- **Accessibility**: ARIA attributes, keyboard navigation built-in
- **Documentation**: Extensive examples and guides

### Alternatives Considered
- **Custom CSS**: Rejected - reinventing the wheel, inconsistent
- **Bootstrap**: Rejected - less React-friendly
- **Tailwind CSS**: Rejected - utility-first doesn't align with component library approach

### Implementation Notes
```javascript
// src/styles/theme.ts
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },    // Blue for household finances
    secondary: { main: '#dc004e' },  // Red for expense alerts
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

// Wrap app in ThemeProvider
```

---

## 8. Home Assistant Authentication Integration

### Decision
Backend reads `X-Hass-User` header from nginx proxy, no additional auth layer

### Rationale
- **Constitution requirement**: Must integrate with Home Assistant
- **Trusted proxy model**: Backend trusts nginx to authenticate
- **Simplicity**: No OAuth/JWT complexity for home network
- **User tracking**: Header value used for audit trails
- **Spring Security**: Configure to extract user from header

### Alternatives Considered
- **Session-based auth**: Rejected - Home Assistant already handles sessions
- **JWT tokens**: Rejected - unnecessary complexity for private network

### Implementation Notes
```java
// Spring Security configuration
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .addFilterBefore(new HassUserHeaderFilter(),
                           UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );
        return http.build();
    }
}

// Custom filter extracts X-Hass-User header
// Sets SecurityContextHolder with user identity
```

---

## 9. Development Hot Reload Configuration

### Decision
Next.js Fast Refresh for frontend, Spring Boot DevTools for backend

### Rationale
- **Fast Refresh**: Preserves React component state, < 2 second reload
- **DevTools**: Automatic restart on Java file changes, < 5 second reload
- **Docker volumes**: Bind-mount source code for hot reload in containers
- **Developer productivity**: No manual restarts during development

### Alternatives Considered
- **Manual restarts**: Rejected - slow development cycle
- **JRebel**: Rejected - commercial license required

### Implementation Notes
```yaml
# docker-compose.yml volume mounts
services:
  frontend:
    volumes:
      - ./budget-frontend:/app
      - /app/node_modules  # Anonymous volume for node_modules

  backend:
    volumes:
      - ./budget-backend/src:/app/src
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
```

---

## 10. Port Configuration and Service Discovery

### Decision
Fixed development ports with environment variable overrides

### Rationale
- **Consistency**: Same ports across all developers
- **Discoverability**: Frontend knows backend URL, backend knows MySQL URL
- **Configurability**: `.env` file allows port changes for conflicts
- **Docker networking**: Services communicate via service names

### Alternatives Considered
- **Random ports**: Rejected - harder to remember, inconsistent
- **Service mesh**: Rejected - overkill for 3 services

### Implementation Notes
```bash
# .env.example
FRONTEND_PORT=3000
BACKEND_PORT=8080
MYSQL_PORT=3306

# Services communicate via Docker network
BACKEND_URL=http://backend:8080
MYSQL_HOST=mysql
```

```javascript
// Frontend API configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
```

---

## Summary of Key Decisions

| Technology | Decision | Primary Rationale |
|------------|----------|-------------------|
| Orchestration | Docker Compose v2 | Single-command startup, industry standard |
| Frontend | Next.js 14 + TypeScript | Constitution requirement, modern features |
| UI Library | Material-UI v5 | Comprehensive components, accessibility |
| Backend | Spring Boot 3.2 + Java 17 | Constitution requirement, LTS support |
| Dev Database | MySQL 8.0 | Production-like environment |
| Test Database | H2 in-memory | Fast execution, no external dependencies |
| Migrations | Liquibase XML | Version control, automatic application |
| Authentication | X-Hass-User header | Constitution requirement, trusted proxy |
| Hot Reload | Fast Refresh + DevTools | Developer productivity |

---

## Risk Mitigation

1. **Port conflicts**: `.env` file allows port customization
2. **MySQL startup race**: Health checks ensure database ready before backend
3. **Migration failures**: Liquibase rollback support + database backups
4. **Docker issues**: Clear error messages + troubleshooting guide in README
5. **Version drift**: Lock file dependencies (package-lock.json, pom.xml)

---

## Performance Considerations

- **Startup time target**: < 60 seconds
  - MySQL: ~10 seconds (health check)
  - Backend: ~20 seconds (JVM + Spring context)
  - Frontend: ~10 seconds (Next.js)
  - Total: ~40 seconds (within target)

- **Hot reload targets**:
  - Frontend: < 2 seconds ✅ (Fast Refresh)
  - Backend: < 5 seconds ✅ (DevTools)

---

## Next Steps

After research phase completion:
1. Generate data-model.md (infrastructure entities, if any)
2. Generate contracts/ (API specifications for health checks)
3. Generate quickstart.md (developer onboarding guide)
4. Update agent context with technology stack
