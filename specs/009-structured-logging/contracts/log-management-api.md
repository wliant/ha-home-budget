# API Contract: Log Management Endpoints

**Feature**: 009-structured-logging
**Date**: 2025-12-28
**Purpose**: Define REST API contracts for runtime log level management

## Overview

This contract defines the Spring Boot Actuator endpoints used for viewing and modifying log levels at runtime. These endpoints enable operational control over logging verbosity without application restart (FR-026).

---

## Endpoint 1: Get All Loggers

### Request

```http
GET /actuator/loggers HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Response (Success)

**Status**: 200 OK

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "levels": ["OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"],
  "loggers": {
    "ROOT": {
      "configuredLevel": "INFO",
      "effectiveLevel": "INFO"
    },
    "com.homebudget": {
      "configuredLevel": "INFO",
      "effectiveLevel": "INFO"
    },
    "com.homebudget.service": {
      "configuredLevel": "DEBUG",
      "effectiveLevel": "DEBUG"
    },
    "com.homebudget.repository": {
      "configuredLevel": null,
      "effectiveLevel": "INFO"
    }
  }
}
```

**Fields**:
- `levels`: Array of available log levels
- `loggers`: Map of logger names to their configured and effective levels
- `configuredLevel`: Explicitly configured level (null if inherited)
- `effectiveLevel`: Actual level in use (inherited from parent if configuredLevel is null)

---

## Endpoint 2: Get Specific Logger

### Request

```http
GET /actuator/loggers/{logger-name} HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**Path Parameters**:
- `logger-name`: Fully qualified logger name (e.g., "com.homebudget.service.BudgetService")

### Response (Success)

**Status**: 200 OK

**Body**:
```json
{
  "configuredLevel": "DEBUG",
  "effectiveLevel": "DEBUG"
}
```

### Response (Logger Not Found)

**Status**: 200 OK

**Body**:
```json
{
  "configuredLevel": null,
  "effectiveLevel": "INFO"
}
```

**Note**: Spring Boot Actuator returns 200 even for non-existent loggers, showing inherited level.

---

## Endpoint 3: Set Logger Level

### Request

```http
POST /actuator/loggers/{logger-name} HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "configuredLevel": "DEBUG"
}
```

**Path Parameters**:
- `logger-name`: Fully qualified logger name

**Body Parameters**:
- `configuredLevel`: Desired log level (TRACE, DEBUG, INFO, WARN, ERROR, or null to reset)

### Response (Success)

**Status**: 204 No Content

**Note**: No response body. Level change takes effect immediately.

### Response (Invalid Level)

**Status**: 400 Bad Request

**Body**:
```json
{
  "timestamp": "2025-12-28T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid log level: INVALID",
  "path": "/actuator/loggers/com.homebudget"
}
```

---

## Endpoint 4: Reset Logger Level

### Request

```http
POST /actuator/loggers/{logger-name} HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "configuredLevel": null
}
```

**Effect**: Removes explicit configuration for logger, causing it to inherit from parent logger.

### Response (Success)

**Status**: 204 No Content

---

## Usage Examples

### Example 1: Enable Debug Logging for Category Service

**Scenario**: Troubleshoot category hierarchy validation issue in production

```bash
# View current level
curl http://localhost:8080/actuator/loggers/com.homebudget.service.CategoryService

# Response:
# {
#   "configuredLevel": null,
#   "effectiveLevel": "INFO"
# }

# Enable DEBUG logging
curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service.CategoryService \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Response: 204 No Content

# Verify change
curl http://localhost:8080/actuator/loggers/com.homebudget.service.CategoryService

# Response:
# {
#   "configuredLevel": "DEBUG",
#   "effectiveLevel": "DEBUG"
# }

# Perform troubleshooting actions...

# Reset to inherited level
curl -X POST http://localhost:8080/actuator/loggers/com.homebudget.service.CategoryService \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": null}'

# Response: 204 No Content
```

### Example 2: Reduce Log Volume for Noisy Package

**Scenario**: Spring Boot actuator endpoints generating excessive INFO logs

```bash
# Set actuator endpoints to WARN to reduce noise
curl -X POST http://localhost:8080/actuator/loggers/org.springframework.boot.actuate \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "WARN"}'

# Response: 204 No Content
```

### Example 3: View All Configured Loggers

**Scenario**: Audit current logging configuration

```bash
curl http://localhost:8080/actuator/loggers | jq '.loggers | to_entries[] | select(.value.configuredLevel != null)'

# Response (filtered):
# {
#   "key": "com.homebudget",
#   "value": {
#     "configuredLevel": "INFO",
#     "effectiveLevel": "INFO"
#   }
# }
# {
#   "key": "com.homebudget.service",
#   "value": {
#     "configuredLevel": "DEBUG",
#     "effectiveLevel": "DEBUG"
#   }
# }
```

---

## Security Considerations

### Authentication

**Requirement**: Log management endpoints SHOULD be secured in production

**Options**:
1. **Basic Authentication**: Use Spring Security with actuator endpoint security
2. **IP Whitelisting**: Restrict access to management network only
3. **Disabled in Production**: Disable endpoints entirely (`management.endpoints.web.exposure.exclude=loggers`)

**Configuration**:
```properties
# Secure actuator endpoints (recommended)
management.endpoints.web.exposure.include=health,loggers
management.endpoint.loggers.enabled=true
spring.security.user.name=admin
spring.security.user.password=${ACTUATOR_PASSWORD}
```

### Authorization

**Requirement**: Only authorized operators should modify log levels

**Risk**: Unauthorized DEBUG logging could expose sensitive data in logs

**Mitigation**:
- Use strong passwords for actuator authentication
- Monitor log level changes via application logs
- Periodically audit configured log levels

---

## Performance Impact

### GET Requests

- **Impact**: Negligible (<1ms)
- **Reason**: Reads in-memory logger configuration

### POST Requests (Level Changes)

- **Impact**: Low (<10ms)
- **Reason**: Updates in-memory logger configuration, propagates to child loggers
- **Note**: Does not trigger garbage collection or restart

### Log Volume Impact

- **Changing to DEBUG**: Increases log volume significantly (10x-100x depending on code)
- **Changing to ERROR**: Decreases log volume significantly (90%+ reduction)
- **Recommendation**: Use targeted package-level changes (e.g., `com.homebudget.service.CategoryService`) rather than broad changes (e.g., `com.homebudget`)

---

## Testing Contract Compliance

### Test Case 1: Verify GET Loggers Endpoint

```java
@Test
void testGetLoggers() {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        "/actuator/loggers",
        Map.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody().containsKey("loggers"));
    assertTrue(response.getBody().containsKey("levels"));
}
```

### Test Case 2: Verify Set Logger Level

```java
@Test
void testSetLoggerLevel() {
    Map<String, String> request = Map.of("configuredLevel", "DEBUG");

    ResponseEntity<Void> response = restTemplate.postForEntity(
        "/actuator/loggers/com.homebudget.service.CategoryService",
        request,
        Void.class
    );

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    // Verify level change
    ResponseEntity<Map> getResponse = restTemplate.getForEntity(
        "/actuator/loggers/com.homebudget.service.CategoryService",
        Map.class
    );

    assertEquals("DEBUG", getResponse.getBody().get("configuredLevel"));
}
```

### Test Case 3: Verify Invalid Level Rejection

```java
@Test
void testInvalidLogLevel() {
    Map<String, String> request = Map.of("configuredLevel", "INVALID");

    try {
        restTemplate.postForEntity(
            "/actuator/loggers/com.homebudget",
            request,
            Void.class
        );
        fail("Expected 400 Bad Request");
    } catch (HttpClientErrorException e) {
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }
}
```

---

## OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Log Management API
  description: Runtime log level management via Spring Boot Actuator
  version: 1.0.0
servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /actuator/loggers:
    get:
      summary: Get all loggers
      description: Retrieve all configured loggers with their levels
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  levels:
                    type: array
                    items:
                      type: string
                      enum: [OFF, ERROR, WARN, INFO, DEBUG, TRACE]
                  loggers:
                    type: object
                    additionalProperties:
                      type: object
                      properties:
                        configuredLevel:
                          type: string
                          nullable: true
                        effectiveLevel:
                          type: string

  /actuator/loggers/{loggerName}:
    get:
      summary: Get specific logger
      description: Retrieve configuration for a specific logger
      parameters:
        - name: loggerName
          in: path
          required: true
          schema:
            type: string
          description: Fully qualified logger name
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: object
                properties:
                  configuredLevel:
                    type: string
                    nullable: true
                  effectiveLevel:
                    type: string

    post:
      summary: Set logger level
      description: Update log level for specific logger
      parameters:
        - name: loggerName
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                configuredLevel:
                  type: string
                  enum: [TRACE, DEBUG, INFO, WARN, ERROR, null]
                  nullable: true
      responses:
        '204':
          description: Level changed successfully
        '400':
          description: Invalid log level
          content:
            application/json:
              schema:
                type: object
                properties:
                  timestamp:
                    type: string
                    format: date-time
                  status:
                    type: integer
                  error:
                    type: string
                  message:
                    type: string
                  path:
                    type: string
```

---

## Summary

This contract defines the Spring Boot Actuator log management API supporting:
- ✅ GET all loggers with current levels
- ✅ GET specific logger configuration
- ✅ POST to set logger level (immediate effect, no restart)
- ✅ POST with null to reset logger to inherited level
- ✅ 400 error for invalid log levels
- ✅ Security recommendations for production use
- ✅ Test cases for contract compliance

**Compliance**: Meets FR-023 (configurable log levels), FR-024 (per-component levels), FR-026 (reload without restart)
