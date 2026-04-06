# Contributing to Home Budget Tracker

Thank you for your interest in contributing to the Home Budget Tracker project.

## Prerequisites

- Docker Desktop (Mac/Windows) or Docker Engine + Docker Compose (Linux)
- Java 17 (for backend development outside Docker)
- Node.js 18+ (for frontend development outside Docker)
- Git

## Development Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/wliant/ha-home-budget.git
   cd ha-home-budget
   ```

2. **Create environment file**:
   ```bash
   cp .env.example .env
   ```

3. **Start the development environment**:
   ```bash
   docker-compose up -d
   ```

4. **Verify services are running**:
   - Frontend: http://localhost:3000
   - Backend: http://localhost:8080/actuator/health
   - MinIO Console: http://localhost:9001

## Project Structure

| Directory | Description | Stack |
|-----------|-------------|-------|
| `budget-backend/` | REST API and business logic | Spring Boot 3.2, Java 17, MySQL 8.0 |
| `budget-frontend/` | User interface | Next.js 14, TypeScript, Material-UI v5 |
| `ocr-processor/` | Receipt OCR extraction | Python 3.11+, FastAPI, PaddleOCR |
| `nginx/` | Reverse proxy configuration | Nginx |
| `ha-apps-proxy/` | Home Assistant proxy integration | - |
| `specs/` | Feature specifications (Specify framework) | Markdown |

## Branch Naming

This project uses the [Specify](https://github.com/speckit) workflow. Feature branches follow the pattern:

```
###-feature-name
```

Examples: `001-project-scaffolding`, `019-dashboard-spending-trends`

The three-digit prefix maps to the feature specification directory under `specs/`.

## Feature Development Workflow

New features go through structured phases using Specify:

1. **Specify** - Write a feature specification (`spec.md`) describing what and why
2. **Clarify** - Refine the spec through targeted questions
3. **Plan** - Generate a technical implementation plan (`plan.md`)
4. **Tasks** - Break the plan into dependency-ordered tasks (`tasks.md`)
5. **Implement** - Execute tasks phase by phase

Feature artifacts are stored in `specs/###-feature-name/`.

## Code Conventions

### Backend (Java / Spring Boot)
- Java 17 language features
- Spring Data JPA for persistence
- Liquibase for database migrations (add changesets in `budget-backend/src/main/resources/db/changelog/changes/`)
- All endpoints must read the `X-Hass-User` header for user identity
- SLF4J parameterized logging (`logger.info("Value: {}", value)`)

### Frontend (TypeScript / Next.js)
- Next.js App Router
- Material-UI v5 components
- Axios for HTTP requests
- Service layer pattern (`src/services/`)

## Running Tests

```bash
# Backend tests
docker-compose exec backend ./mvnw test

# Frontend tests
docker-compose exec frontend npm test
```

## Pull Requests

- Keep PRs focused on a single feature or fix
- Reference the feature spec number if applicable (e.g., "Feature 019")
- Ensure `docker-compose up` works with your changes
- Verify both frontend and backend build without errors

## Authentication

This application uses Home Assistant authentication via the `X-Hass-User` HTTP header. In development, the backend accepts this header directly. No additional auth setup is needed for local development.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
