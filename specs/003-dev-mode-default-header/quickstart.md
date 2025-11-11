# Quickstart: Development Mode Default User Header

**Feature**: 003-dev-mode-default-header
**Date**: 2025-10-28
**Phase**: Phase 1 - Design

## Prerequisites

- Docker and Docker Compose installed
- Existing Home Budget Tracker application running
- curl or similar HTTP client for testing

## Quick Test Scenarios

### Scenario 1: Development Mode - No Header (US1)

**Goal**: Verify that API requests work without the X-Hass-User header in development mode.

**Steps**:

1. **Start application in development mode**:
```bash
# Ensure docker-compose.yml has SPRING_PROFILES_ACTIVE=dev
docker-compose down
docker-compose up -d
```

2. **Wait for backend to be healthy**:
```bash
# Check logs for "DEVELOPMENT MODE" message
docker-compose logs backend | grep "DEVELOPMENT MODE"

# Expected output:
# WARN  c.h.HomeBudgetApplication - APPLICATION RUNNING IN DEVELOPMENT MODE - Default user authentication enabled
# WARN  c.h.HomeBudgetApplication - X-Hass-User header will default to 'dev-user' when not provided
```

3. **Create a budget WITHOUT X-Hass-User header**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2025,
    "month": 11,
    "totalAmount": 4000,
    "description": "Test budget without header"
  }' | jq .
```

**Expected Result**:
```json
{
  "id": 7,
  "year": 2025,
  "month": 11,
  "totalAmount": 4000,
  "description": "Test budget without header",
  "createdBy": "dev-user",
  "createdAt": "2025-10-28T...",
  "updatedAt": "2025-10-28T...",
  "version": 1
}
```

✅ **Success Criteria**: Budget created with `createdBy: "dev-user"`

---

### Scenario 2: Development Mode - Explicit Header (US2)

**Goal**: Verify that explicit X-Hass-User header overrides the default in development mode.

**Steps**:

1. **Create a budget WITH X-Hass-User header**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "year": 2025,
    "month": 12,
    "totalAmount": 4500,
    "description": "Test budget with alice"
  }' | jq .
```

**Expected Result**:
```json
{
  "id": 8,
  "year": 2025,
  "month": 12,
  "totalAmount": 4500,
  "description": "Test budget with alice",
  "createdBy": "alice",
  "createdAt": "2025-10-28T...",
  "updatedAt": "2025-10-28T...",
  "version": 1
}
```

✅ **Success Criteria**: Budget created with `createdBy: "alice"` (not "dev-user")

2. **Create an expense with different user**:
```bash
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{
    "budgetId": 1,
    "amount": 150,
    "description": "Test expense with bob",
    "expenseDate": "2025-10-28"
  }' | jq .
```

**Expected Result**:
```json
{
  "id": 15,
  "amount": 150,
  "description": "Test expense with bob",
  "expenseDate": "2025-10-28",
  "budgetId": 1,
  "createdBy": "bob",
  ...
}
```

✅ **Success Criteria**: Expense created with `createdBy: "bob"`

---

### Scenario 3: Production Mode - Header Required (US1 Negative Test)

**Goal**: Verify that production mode still requires the X-Hass-User header.

**Steps**:

1. **Modify docker-compose.yml to use production profile**:
```yaml
# docker-compose.yml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    # OR remove SPRING_PROFILES_ACTIVE entirely
```

2. **Restart application**:
```bash
docker-compose down
docker-compose up -d
```

3. **Wait for backend to be healthy and check logs**:
```bash
docker-compose logs backend | grep "PRODUCTION MODE"

# Expected output:
# INFO  c.h.HomeBudgetApplication - APPLICATION RUNNING IN PRODUCTION MODE - X-Hass-User header required
```

4. **Try to create a budget WITHOUT X-Hass-User header**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2026,
    "month": 1,
    "totalAmount": 5000,
    "description": "Should fail in production"
  }' | jq .
```

**Expected Result**:
```json
{
  "timestamp": "2025-10-28T...",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request header 'X-Hass-User' for method parameter type String is not present",
  "path": "/api/budgets"
}
```

✅ **Success Criteria**: HTTP 400 error with message about missing X-Hass-User header

5. **Try again WITH X-Hass-User header**:
```bash
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{
    "year": 2026,
    "month": 1,
    "totalAmount": 5000,
    "description": "Should succeed in production"
  }' | jq .
```

**Expected Result**:
```json
{
  "id": 9,
  "year": 2026,
  "month": 1,
  "totalAmount": 5000,
  "description": "Should succeed in production",
  "createdBy": "alice",
  ...
}
```

✅ **Success Criteria**: Budget created successfully with `createdBy: "alice"`

---

### Scenario 4: Development Mode Indicators (US3)

**Goal**: Verify that development mode is clearly indicated in logs.

**Steps**:

1. **Ensure application is running in development mode**:
```bash
docker-compose down
docker-compose up -d
```

2. **Check startup logs**:
```bash
docker-compose logs backend | head -50
```

**Expected Output (look for these lines)**:
```
WARN  c.h.HomeBudgetApplication - APPLICATION RUNNING IN DEVELOPMENT MODE - Default user authentication enabled
WARN  c.h.HomeBudgetApplication - X-Hass-User header will default to 'dev-user' when not provided
```

✅ **Success Criteria**: Clear WARN-level messages indicating development mode is active

3. **Make a request without header and check DEBUG logs**:
```bash
# Enable DEBUG logging temporarily (modify application-dev.properties if needed)
curl -X GET http://localhost:8081/api/budgets

# Check logs for interceptor message
docker-compose logs backend | grep "AuthHeaderInterceptor"
```

**Expected Output**:
```
DEBUG c.h.config.AuthHeaderInterceptor - Development mode: Adding default X-Hass-User header: dev-user
```

✅ **Success Criteria**: DEBUG-level log shows when default header is applied

---

## Integration Test Flows

### Flow 1: Multi-User Testing in Development

This flow tests the ability to switch between users in development mode (US2).

```bash
# 1. Create budget as default user (no header)
curl -X POST http://localhost:8081/api/budgets \
  -H "Content-Type: application/json" \
  -d '{"year": 2025, "month": 11, "totalAmount": 3000, "description": "Household budget"}' \
  | jq '.id, .createdBy'

# Expected: createdBy = "dev-user"

# 2. Add expense as alice
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: alice" \
  -d '{"budgetId": 1, "amount": 100, "description": "Groceries by Alice", "expenseDate": "2025-10-28"}' \
  | jq '.id, .createdBy'

# Expected: createdBy = "alice"

# 3. Add expense as bob
curl -X POST http://localhost:8081/api/expenses \
  -H "Content-Type: application/json" \
  -H "X-Hass-User: bob" \
  -d '{"budgetId": 1, "amount": 50, "description": "Gas by Bob", "expenseDate": "2025-10-28"}' \
  | jq '.id, .createdBy'

# Expected: createdBy = "bob"

# 4. Verify budget shows expenses from both users
curl -X GET "http://localhost:8081/api/expenses?budgetId=1" | jq '.[] | {description, createdBy}'

# Expected: Mix of "alice" and "bob" in createdBy fields
```

✅ **Success Criteria**: Can easily switch between users for testing without reconfiguring

---

### Flow 2: Frontend Development Workflow

This flow simulates a frontend developer using the application.

```bash
# 1. Frontend developer starts working, no authentication setup
# Open browser to http://localhost:3001
# Navigate to Budgets page
# Click "Create Budget"
# Fill form and submit

# Behind the scenes (verify with logs):
docker-compose logs backend | grep "Adding default X-Hass-User"

# Expected: "Adding default X-Hass-User header: dev-user"

# 2. Developer decides to test multi-user scenario
# Temporarily modify frontend api.ts to add header:
# headers: { 'X-Hass-User': 'test-user' }

# Submit another budget via frontend
# Verify in backend logs that "test-user" is used (not "dev-user")
```

✅ **Success Criteria**: Frontend developer can work without configuring authentication initially, then easily test multi-user scenarios when needed

---

## Configuration Verification

### Verify Current Mode

```bash
# Check which profile is active
docker-compose exec backend env | grep SPRING_PROFILES_ACTIVE

# Expected (development): SPRING_PROFILES_ACTIVE=dev
# Expected (production): SPRING_PROFILES_ACTIVE=prod or empty

# Check application properties
docker-compose exec backend cat /app/application.properties | grep "app.dev-mode"

# Expected: app.dev-mode=false (default)

docker-compose exec backend cat /app/application-dev.properties | grep "app.dev-mode"

# Expected: app.dev-mode=true
```

### Verify Default User Configuration

```bash
# Check default user value
docker-compose exec backend cat /app/application-dev.properties | grep "app.default-dev-user"

# Expected: app.default-dev-user=dev-user
```

---

## Troubleshooting

### Problem: Still getting "Missing header" error in development mode

**Diagnosis**:
```bash
# Check if dev profile is actually active
docker-compose logs backend | grep "APPLICATION RUNNING"

# If shows "PRODUCTION MODE", check docker-compose.yml
cat docker-compose.yml | grep -A 3 "backend:"
```

**Solution**:
```yaml
# Ensure docker-compose.yml has:
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=dev
```

Then restart:
```bash
docker-compose down && docker-compose up -d
```

---

### Problem: Default user not being applied

**Diagnosis**:
```bash
# Check if app.default-dev-user is configured
docker-compose exec backend cat /app/application-dev.properties | grep default-dev-user

# Check interceptor logs
docker-compose logs backend | grep AuthHeaderInterceptor
```

**Solution**: Ensure `application-dev.properties` contains:
```properties
app.default-dev-user=dev-user
```

---

### Problem: Production mode not enforcing header

**Diagnosis**:
```bash
# Verify production profile is active (or no profile)
docker-compose logs backend | grep "APPLICATION RUNNING"

# Should show "PRODUCTION MODE"
```

**Solution**: Remove or set `SPRING_PROFILES_ACTIVE=prod` in docker-compose.yml

---

## Summary

This quickstart demonstrates:
- ✅ US1: Default header in development mode works transparently
- ✅ US2: Explicit headers override defaults for multi-user testing
- ✅ US3: Clear log messages indicate current mode
- ✅ Production security is maintained
- ✅ Zero configuration needed for basic development workflow

**Time to test all scenarios**: ~10 minutes

**Developer experience improvement**: No authentication configuration needed to start development!
