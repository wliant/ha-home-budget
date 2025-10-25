# Quickstart Guide: Development Environment

**Feature**: 001-project-scaffolding
**Date**: 2025-10-22
**Purpose**: Developer onboarding and integration testing scenarios

---

## Prerequisites

Before starting, ensure you have:

1. **Docker Desktop** (Mac/Windows) or **Docker Engine + Docker Compose** (Linux)
   - Version: Docker 20.10+, Docker Compose v2+
   - Verify: `docker --version && docker-compose --version`

2. **Git** (for cloning repository)
   - Verify: `git --version`

3. **Available Ports**: 3000, 8080, 3306
   - Check: `lsof -i :3000 -i :8080 -i :3306` (should return nothing)

4. **Minimum System Requirements**:
   - RAM: 4GB available
   - Disk: 2GB free space
   - CPU: 2 cores

---

## Quick Start (5 minutes)

### 1. Clone Repository

```bash
git clone https://github.com/your-org/home-budget-tracker.git
cd home-budget-tracker
```

### 2. Start Development Environment

```bash
# Copy environment template
cp .env.example .env

# Start all services (frontend, backend, database)
docker-compose up -d

# View logs (optional)
docker-compose logs -f
```

**Expected Output**:
```
Creating network "home-budget-tracker_default" ...
Creating volume "home-budget-tracker_mysql-data" ...
Creating home-budget-tracker_mysql_1 ... done
Creating home-budget-tracker_backend_1 ... done
Creating home-budget-tracker_frontend_1 ... done
```

### 3. Verify Services

**Frontend**: Open browser to http://localhost:3000
- Should see Next.js welcome page or Material-UI dashboard

**Backend**: Check health endpoint
```bash
curl http://localhost:8080/actuator/health
```
Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL"
      }
    }
  }
}
```

**Database**: Connect with MySQL client
```bash
mysql -h 127.0.0.1 -P 3306 -u budget_user -p
# Password: budget_password (from .env file)
```

### 4. Stop Environment

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (deletes database data)
docker-compose down -v
```

---

## Integration Testing Scenarios

### Scenario 1: Frontend-Backend Communication

**Objective**: Verify frontend can call backend API

**Steps**:
1. Ensure environment is running (`docker-compose up -d`)
2. Open browser developer console (F12)
3. Navigate to http://localhost:3000
4. In console, run:
   ```javascript
   fetch('http://localhost:8080/actuator/health')
     .then(res => res.json())
     .then(data => console.log('Backend response:', data));
   ```

**Expected Result**: Console shows health response with `"status": "UP"`

**Troubleshooting**:
- CORS errors: Check backend CORS configuration
- Connection refused: Verify backend is running (`docker-compose ps`)

---

### Scenario 2: Database Connection and Migrations

**Objective**: Verify backend connects to MySQL and applies migrations

**Steps**:
1. Start environment: `docker-compose up -d`
2. Check backend logs:
   ```bash
   docker-compose logs backend | grep -i liquibase
   ```
3. Connect to MySQL:
   ```bash
   docker-compose exec mysql mysql -u budget_user -pbudget_password homebudget
   ```
4. List tables:
   ```sql
   SHOW TABLES;
   ```

**Expected Result**:
```
+-------------------------------+
| Tables_in_homebudget          |
+-------------------------------+
| DATABASECHANGELOG             |
| DATABASECHANGELOGLOCK         |
+-------------------------------+
```

**Troubleshooting**:
- No tables: Check Liquibase logs for errors
- Connection refused: Verify MySQL health (`docker-compose ps mysql`)

---

### Scenario 3: Automated Test Execution

**Objective**: Run backend tests with H2 in-memory database

**Steps**:
1. Navigate to backend directory:
   ```bash
   cd budget-backend
   ```
2. Run tests (inside Docker):
   ```bash
   docker-compose exec backend ./mvnw test
   ```
   Or run locally (if Maven installed):
   ```bash
   ./mvnw test
   ```

**Expected Result**:
```
[INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Verification**:
- Tests use H2 (not MySQL)
- Test execution time < 30 seconds
- No test database pollution in development MySQL

---

### Scenario 4: Hot Reload Verification

**Objective**: Verify code changes auto-reload without manual restart

**Frontend Hot Reload**:
1. Open http://localhost:3000 in browser
2. Edit `budget-frontend/src/app/page.tsx`
3. Change text: `<h1>Welcome to Budget Tracker</h1>`
4. Save file

**Expected Result**: Browser auto-refreshes within 2 seconds

**Backend Hot Reload**:
1. Edit `budget-backend/src/main/java/com/homebudget/controller/HealthController.java`
2. Add log statement: `log.info("Health check called");`
3. Save file

**Expected Result**: Backend restarts within 5 seconds
- Verify: `docker-compose logs -f backend | grep "Started Application"`

---

### Scenario 5: Data Persistence

**Objective**: Verify database data survives container restart

**Steps**:
1. Insert test data:
   ```bash
   docker-compose exec mysql mysql -u budget_user -pbudget_password homebudget -e \
     "CREATE TABLE test (id INT, value VARCHAR(50)); INSERT INTO test VALUES (1, 'persisted');"
   ```
2. Stop containers: `docker-compose down`
3. Restart: `docker-compose up -d`
4. Verify data:
   ```bash
   docker-compose exec mysql mysql -u budget_user -pbudget_password homebudget -e \
     "SELECT * FROM test;"
   ```

**Expected Result**: Data still exists after restart

**Cleanup**:
```bash
docker-compose exec mysql mysql -u budget_user -pbudget_password homebudget -e \
  "DROP TABLE test;"
```

---

### Scenario 6: Port Conflict Resolution

**Objective**: Handle already-in-use ports gracefully

**Steps**:
1. Simulate port conflict:
   ```bash
   # Start something on port 3000
   python3 -m http.server 3000 &
   ```
2. Try starting environment:
   ```bash
   docker-compose up -d
   ```

**Expected Result**: Error message indicating port 3000 is in use

**Resolution**:
1. Stop conflicting process
2. Or modify `.env` file:
   ```bash
   FRONTEND_PORT=3001
   ```
3. Update docker-compose.yml port mapping
4. Restart: `docker-compose up -d`

---

### Scenario 7: Database Migration Failure Recovery

**Objective**: Handle and recover from migration failures

**Simulation**:
1. Create intentionally failing migration:
   ```bash
   # Add to db/changelog/999-test-failure.xml
   <changeSet id="999" author="test">
     <sql>INVALID SQL SYNTAX HERE;</sql>
   </changeSet>
   ```
2. Start backend: `docker-compose up backend`

**Expected Behavior**:
- Backend fails to start
- Liquibase logs show error
- Database lock released after failure

**Recovery**:
1. Fix or remove bad migration file
2. Check lock status:
   ```sql
   SELECT * FROM DATABASECHANGELOGLOCK;
   ```
3. If locked, manually release:
   ```sql
   UPDATE DATABASECHANGELOGLOCK SET LOCKED=0;
   ```
4. Restart backend

---

## Development Workflow

### Daily Development Cycle

1. **Start environment** (morning):
   ```bash
   docker-compose up -d
   ```

2. **Check status**:
   ```bash
   docker-compose ps
   ```

3. **View logs** (if needed):
   ```bash
   docker-compose logs -f [service]
   ```

4. **Make changes** (hot reload active)
   - Edit code in `budget-frontend/` or `budget-backend/`
   - Changes apply automatically

5. **Run tests**:
   ```bash
   docker-compose exec backend ./mvnw test
   docker-compose exec frontend npm test
   ```

6. **Stop environment** (end of day):
   ```bash
   docker-compose down
   ```

---

## Common Commands Reference

### Docker Compose

```bash
# Start services
docker-compose up -d               # Detached mode
docker-compose up                  # Foreground (see logs)

# Stop services
docker-compose down                # Stop containers
docker-compose down -v             # Stop + remove volumes (deletes data)

# Restart specific service
docker-compose restart backend     # Restart backend only

# View logs
docker-compose logs -f             # All services, follow
docker-compose logs -f backend     # Backend only

# Check status
docker-compose ps                  # List running containers

# Execute commands
docker-compose exec backend bash   # Open shell in backend
docker-compose exec mysql bash     # Open shell in MySQL
```

### Backend (Maven)

```bash
# Inside backend container
docker-compose exec backend ./mvnw test       # Run tests
docker-compose exec backend ./mvnw clean      # Clean build
docker-compose exec backend ./mvnw package    # Build JAR
```

### Frontend (NPM)

```bash
# Inside frontend container
docker-compose exec frontend npm test         # Run tests
docker-compose exec frontend npm run build    # Production build
docker-compose exec frontend npm run lint     # Linting
```

### Database (MySQL)

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u budget_user -pbudget_password homebudget

# Dump database
docker-compose exec mysql mysqldump -u budget_user -pbudget_password homebudget > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u budget_user -pbudget_password homebudget < backup.sql
```

---

## Troubleshooting

### Problem: Services won't start

**Check**:
```bash
docker-compose ps      # Are containers running?
docker-compose logs    # Any errors?
docker --version       # Docker installed?
```

**Solutions**:
- Restart Docker Desktop
- Run `docker-compose down -v` then `docker-compose up -d`
- Check available disk space: `df -h`

---

### Problem: Frontend can't reach backend

**Check**:
```bash
curl http://localhost:8080/actuator/health  # Backend responding?
docker-compose logs backend                  # Backend errors?
```

**Solutions**:
- Verify backend is running: `docker-compose ps backend`
- Check CORS configuration in Spring Boot
- Verify `NEXT_PUBLIC_API_URL` in frontend `.env`

---

### Problem: Database connection errors

**Check**:
```bash
docker-compose logs mysql                    # MySQL startup errors?
docker-compose exec mysql mysqladmin ping    # MySQL responding?
```

**Solutions**:
- Wait 10-15 seconds for MySQL initialization
- Check credentials in `.env` match `application.yml`
- Restart MySQL: `docker-compose restart mysql`

---

### Problem: Hot reload not working

**Frontend**:
- Verify volume mount: `docker-compose config | grep volumes`
- Check Next.js is in dev mode (not production build)
- Restart frontend: `docker-compose restart frontend`

**Backend**:
- Verify Spring Boot DevTools in `pom.xml`
- Check volume mount for `src/` directory
- Restart backend: `docker-compose restart backend`

---

## Performance Tuning

### Faster Startup

```yaml
# docker-compose.yml optimization
services:
  mysql:
    command: --skip-log-bin  # Disable binary logging (dev only)
```

### Reduce Memory Usage

```yaml
services:
  backend:
    environment:
      JAVA_OPTS: "-Xmx512m"  # Limit JVM heap
```

---

## Next Steps

After completing this quickstart:

1. **Run `/speckit.tasks`** to generate implementation tasks
2. **Review architecture**: See `plan.md` for detailed technical design
3. **Begin implementation**: Follow task-by-task workflow
4. **Add business features**: Create new feature specs with `/speckit.specify`

---

## Support

For issues:
1. Check this quickstart guide
2. Review Docker Compose logs
3. Consult `plan.md` and `research.md`
4. File issue in project repository
