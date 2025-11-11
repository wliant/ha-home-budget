# API Contract: Development Mode Default User Header

**Feature**: 003-dev-mode-default-header
**Date**: 2025-10-28
**Phase**: Phase 1 - Design

## Overview

This feature does NOT introduce new API endpoints. It modifies the authentication header handling behavior of existing endpoints.

## Changed Behavior

### Before This Feature

**ALL Endpoints** (Budget, Expense, Category):
- **Required Header**: `X-Hass-User` (mandatory)
- **Missing Header Result**: HTTP 400 Bad Request
- **Empty Header Result**: HTTP 400 Bad Request

### After This Feature

**ALL Endpoints** (Budget, Expense, Category):

#### Production Mode (`SPRING_PROFILES_ACTIVE != dev`)
- **Required Header**: `X-Hass-User` (mandatory)
- **Missing Header Result**: HTTP 400 Bad Request (unchanged)
- **Empty Header Result**: HTTP 400 Bad Request (unchanged)
- **Behavior**: No change from current production behavior

#### Development Mode (`SPRING_PROFILES_ACTIVE = dev`)
- **Required Header**: `X-Hass-User` (optional)
- **Missing Header Result**: Uses default user from `app.default-dev-user` config
- **Empty Header Result**: Uses default user from `app.default-dev-user` config
- **Explicit Header Result**: Uses provided value (overrides default)
- **Behavior**: Header is transparently added by interceptor before reaching controller

## Contract Examples

### Example 1: Development Mode - No Header Provided

**Request**:
```http
POST /api/budgets HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "year": 2025,
  "month": 11,
  "totalAmount": 5000,
  "description": "November budget"
}
```

**Processing**:
1. Request arrives without `X-Hass-User` header
2. `AuthHeaderInterceptor` detects `dev-mode=true`
3. Interceptor adds header: `X-Hass-User: dev-user`
4. Controller receives request with header present
5. Budget created with `createdBy = "dev-user"`

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 5,
  "year": 2025,
  "month": 11,
  "totalAmount": 5000,
  "description": "November budget",
  "createdBy": "dev-user",
  "createdAt": "2025-10-28T10:30:00",
  "updatedAt": "2025-10-28T10:30:00",
  "version": 1
}
```

### Example 2: Development Mode - Explicit Header Provided

**Request**:
```http
POST /api/budgets HTTP/1.1
Host: localhost:8081
Content-Type: application/json
X-Hass-User: alice

{
  "year": 2025,
  "month": 12,
  "totalAmount": 6000,
  "description": "December budget"
}
```

**Processing**:
1. Request arrives with `X-Hass-User: alice`
2. `AuthHeaderInterceptor` detects header is present
3. Interceptor does NOT modify header (explicit value takes precedence)
4. Controller receives request with `X-Hass-User: alice`
5. Budget created with `createdBy = "alice"`

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 6,
  "year": 2025,
  "month": 12,
  "totalAmount": 6000,
  "description": "December budget",
  "createdBy": "alice",
  "createdAt": "2025-10-28T10:35:00",
  "updatedAt": "2025-10-28T10:35:00",
  "version": 1
}
```

### Example 3: Production Mode - No Header Provided

**Request**:
```http
POST /api/budgets HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "year": 2025,
  "month": 11,
  "totalAmount": 5000,
  "description": "November budget"
}
```

**Processing**:
1. Request arrives without `X-Hass-User` header
2. `AuthHeaderInterceptor` detects `dev-mode=false`
3. Interceptor does NOT add header
4. Controller attempts to extract `@RequestHeader("X-Hass-User")`
5. Spring Boot throws `MissingRequestHeaderException`

**Response**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2025-10-28T10:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request header 'X-Hass-User' for method parameter type String is not present",
  "path": "/api/budgets"
}
```

## Affected Endpoints

### Budget Endpoints

All budget endpoints support the new header behavior:

- `POST /api/budgets` - Create budget
- `GET /api/budgets` - List budgets
- `GET /api/budgets/{id}` - Get budget by ID
- `PUT /api/budgets/{id}` - Update budget
- `DELETE /api/budgets/{id}` - Delete budget
- `GET /api/budgets/current` - Get current month budget

### Expense Endpoints

All expense endpoints support the new header behavior:

- `POST /api/expenses` - Create expense
- `GET /api/expenses` - List expenses (with filters)
- `GET /api/expenses/{id}` - Get expense by ID
- `PUT /api/expenses/{id}` - Update expense
- `DELETE /api/expenses/{id}` - Delete expense

### Category Endpoints

All category endpoints support the new header behavior:

- `POST /api/categories` - Create category
- `GET /api/categories` - List categories
- `GET /api/categories/{id}` - Get category by ID
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category
- `GET /api/categories/{id}/expense-count` - Get expense count

## Configuration Contract

### Backend Configuration

**application.properties** (Default - Production):
```properties
# Authentication configuration
app.dev-mode=false
app.default-dev-user=
```

**application-dev.properties** (Development):
```properties
# Development mode authentication
app.dev-mode=true
app.default-dev-user=dev-user
```

### Environment Variables

**docker-compose.yml** (Development):
```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=dev
```

**Production Deployment**:
```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=prod
    # OR omit SPRING_PROFILES_ACTIVE entirely
```

## Logging Contract

### Startup Logging

**Development Mode**:
```
WARN  c.h.HomeBudgetApplication - APPLICATION RUNNING IN DEVELOPMENT MODE - Default user authentication enabled
WARN  c.h.HomeBudgetApplication - X-Hass-User header will default to 'dev-user' when not provided
```

**Production Mode**:
```
INFO  c.h.HomeBudgetApplication - APPLICATION RUNNING IN PRODUCTION MODE - X-Hass-User header required
```

### Per-Request Logging (Development Mode)

**When Default User Applied**:
```
DEBUG c.h.config.AuthHeaderInterceptor - Development mode: Adding default X-Hass-User header: dev-user
```

**When Explicit Header Provided**:
```
(No log - explicit header passes through transparently)
```

## Backward Compatibility

### API Clients

✅ **No Breaking Changes**: Existing API clients that provide `X-Hass-User` header continue to work unchanged in both development and production modes.

✅ **Additive Change**: Development mode now *allows* missing header, but doesn't require clients to change.

✅ **Production Safety**: Production mode behavior is identical to current behavior - no regression risk.

### Frontend

✅ **No Frontend Changes Required**: Frontend can continue to use axios without modification. The backend interceptor handles missing headers transparently.

✅ **Optional Enhancement**: Frontend *could* be updated to omit header in development, but this is not required for the feature to work.

## Security Contract

### Production Guarantees

1. **Default is Secure**: Base `application.properties` has `dev-mode=false`
2. **Explicit Activation Required**: Development mode requires setting Spring profile to `dev`
3. **No Runtime Toggle**: Mode cannot be changed without restarting application
4. **Header Priority**: Explicit headers always override defaults (even in dev mode)

### Development Guarantees

1. **Consistent Testing**: Developers can test both scenarios:
   - Default user (omit header)
   - Specific user (provide header)
2. **Production Simulation**: Developers can test production behavior by switching to `prod` profile
3. **Audit Trail**: All actions are still attributed to a user (default or explicit)

## Non-Functional Contract

### Performance

- **Overhead**: < 1ms per request (boolean check + optional header addition)
- **No Database Impact**: Configuration loaded once at startup
- **No External Calls**: All logic is in-memory

### Reliability

- **Fail-Safe**: If configuration is invalid, application fails at startup (not at runtime)
- **No Partial Failures**: Either all endpoints have default header support or none do
- **Stateless**: No session state or caching - each request is independent

## Summary

This feature modifies **infrastructure behavior** only:
- ✅ No new endpoints
- ✅ No changes to request/response schemas
- ✅ No changes to business logic
- ✅ Transparent header injection in development mode
- ✅ Zero impact on production mode behavior
- ✅ Backward compatible with all existing clients
