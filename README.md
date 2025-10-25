# Home Budget Tracker

A home budget and expense tracking system that runs in a private home network, integrated with Home Assistant authentication.

## Quick Start

### Prerequisites

- Docker Desktop (Mac/Windows) or Docker Engine + Docker Compose (Linux)
- Docker 20.10+, Docker Compose v2+
- Git
- Available ports: 3000, 8080, 3306

### Setup (< 5 minutes)

1. **Clone repository**:
   ```bash
   git clone https://github.com/your-org/home-budget-tracker.git
   cd home-budget-tracker
   ```

2. **Create environment file**:
   ```bash
   cp .env.example .env
   ```

3. **Start development environment**:
   ```bash
   docker-compose up -d
   ```

4. **Verify services**:
   - Frontend: http://localhost:3000
   - Backend Health: http://localhost:8080/actuator/health
   - MySQL: localhost:3306

### Stop Environment

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes database data)
docker-compose down -v
```

## Architecture

- **Frontend**: Next.js 14 + TypeScript + Material-UI v5
- **Backend**: Spring Boot 3.2 + Java 17
- **Database**: MySQL 8.0 (dev), H2 (tests)
- **Orchestration**: Docker Compose
- **Authentication**: Home Assistant (X-Hass-User header)

## Project Structure

```
.
├── budget-frontend/       # Next.js application
├── budget-backend/        # Spring Boot application
├── docker-compose.yml     # Service orchestration
├── .env.example          # Environment template
└── README.md             # This file
```

## Development

### Hot Reload

- **Frontend**: Changes auto-refresh in < 2 seconds
- **Backend**: Automatic restart in < 5 seconds

### Running Tests

```bash
# Backend tests (H2 in-memory database)
docker-compose exec backend ./mvnw test

# Frontend tests
docker-compose exec frontend npm test
```

### Database Migrations

Liquibase automatically applies schema changes on backend startup.

To add a new migration:
1. Create changeset in `budget-backend/src/main/resources/db/changelog/changes/`
2. Include in `db.changelog-master.xml`
3. Restart backend: `docker-compose restart backend`

## Troubleshooting

### Services won't start
```bash
docker-compose ps      # Check container status
docker-compose logs    # View error logs
```

### Port conflicts
Edit `.env` file to change ports, then restart:
```bash
docker-compose down
docker-compose up -d
```

### Database connection errors
Wait 10-15 seconds for MySQL initialization, then:
```bash
docker-compose restart backend
```

## Documentation

For detailed documentation, see:
- [Quickstart Guide](./specs/001-project-scaffolding/quickstart.md)
- [Technical Plan](./specs/001-project-scaffolding/plan.md)
- [Implementation Tasks](./specs/001-project-scaffolding/tasks.md)

## License

[Your License Here]
