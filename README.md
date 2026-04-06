# Home Budget Tracker

A home budget and expense tracking system that runs in a private home network, integrated with Home Assistant authentication.

## Features

- **Budget Management** - Create and track monthly budgets with spending progress
- **Expense Recording** - Log expenses with categories, dates, and user attribution
- **Hierarchical Categories** - Two-level category hierarchy with parent budget validation
- **Receipt OCR** - Scan receipts and auto-extract expense data using PaddleOCR
- **Spending Trends** - Visualize spending patterns with filters and category breakdowns
- **Dashboard** - At-a-glance overview of current month budget, recent activity, and alerts
- **Multi-User** - Household members share budgets with per-user expense tracking
- **Home Assistant Integration** - Seamless authentication via HA proxy

## Quick Start

### Prerequisites

- Docker 20.10+ and Docker Compose v2+
- Git
- Available ports: 3000, 3306, 8080, 8082, 9000, 9001

### Setup

1. **Clone repository**:
   ```bash
   git clone https://github.com/wliant/ha-home-budget.git
   cd ha-home-budget
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
   - MinIO Console: http://localhost:9001

### Stop Environment

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

## Architecture

| Service | Technology | Port |
|---------|-----------|------|
| Frontend | Next.js 14, TypeScript, Material-UI v5 | 3000 |
| Backend | Spring Boot 3.2, Java 17 | 8080 |
| Database | MySQL 8.0 | 3306 |
| OCR Processor | Python 3.11+, FastAPI, PaddleOCR | 8082 |
| Object Storage | MinIO (S3-compatible) | 9000 / 9001 |
| Event Streaming | Redis 7 | 6379 |
| Authentication | Home Assistant (`X-Hass-User` header) | - |
| Orchestration | Docker Compose | - |

## Project Structure

```
.
├── budget-frontend/       # Next.js application
├── budget-backend/        # Spring Boot application
├── ocr-processor/         # Python OCR receipt processor
├── nginx/                 # Reverse proxy configuration
├── ha-apps-proxy/         # Home Assistant proxy integration
├── ops/                   # Operations and deployment configs
├── specs/                 # Feature specifications (Specify framework)
├── docker-compose.yml     # Development orchestration
├── docker-compose.prod.yml # Production orchestration
└── .env.example           # Environment template
```

## Development

### Hot Reload

- **Frontend**: Changes auto-refresh in < 2 seconds
- **Backend**: Automatic restart in < 5 seconds

### Running Tests

```bash
# Backend tests
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

## Home Assistant Integration

This application is designed to run behind a Home Assistant Ingress proxy:

1. User accesses the app through Home Assistant
2. The proxy forwards requests with an `X-Hass-User` header identifying the user
3. The backend reads this header for user identity — no additional login required

See [HOME_ASSISTANT_DEPLOYMENT.md](./HOME_ASSISTANT_DEPLOYMENT.md) for full deployment instructions.

## Production Deployment

For production, use the dedicated compose file:

```bash
cp .env.prod.example .env.prod
# Edit .env.prod with your production values
docker-compose -f docker-compose.prod.yml up -d
```

See [PRODUCTION.md](./PRODUCTION.md) for details.

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

- [Home Assistant Deployment](./HOME_ASSISTANT_DEPLOYMENT.md)
- [Production Deployment](./PRODUCTION.md)
- [Contributing](./CONTRIBUTING.md)
- [Feature Specifications](./specs/)

## License

This project is licensed under the [Apache License 2.0](LICENSE).
