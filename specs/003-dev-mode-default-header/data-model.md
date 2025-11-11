# Data Model: Development Mode Default User Header

**Feature**: 003-dev-mode-default-header
**Date**: 2025-10-28
**Phase**: Phase 1 - Design

## Overview

This feature does NOT introduce new data entities or database schema changes. It modifies configuration and request handling behavior only.

## Configuration Entities

### Application Configuration

**Purpose**: Control development mode behavior through environment-specific properties.

**Attributes**:
- `app.dev-mode` (boolean): Enable/disable development mode default header behavior
  - Default: `false` (production mode)
  - Development: `true` (when Spring profile `dev` is active)
  - Validation: Must be explicitly set to `true` to enable development features

- `app.default-dev-user` (string): The default user identity to use when X-Hass-User header is missing in development mode
  - Default: `""` (empty, no default user in production)
  - Development: `"dev-user"` (configurable)
  - Validation: Non-empty string required when `dev-mode=true`
  - Constraints: Should be clearly identifiable as development (e.g., "dev-user", "developer", "local-dev")

**Environment Mapping**:

| Environment | Spring Profile | `app.dev-mode` | `app.default-dev-user` | Behavior |
|-------------|----------------|----------------|------------------------|----------|
| Production  | (none) or `prod` | `false` | `""` | Require X-Hass-User header, fail if missing |
| Development | `dev` | `true` | `"dev-user"` | Use default user when header missing |

## Existing Entities (No Changes)

This feature does NOT modify any existing database entities:
- ✅ `Budget` - No changes (still tracks `createdBy`)
- ✅ `Expense` - No changes (still tracks `createdBy`)
- ✅ `Category` - No changes (still tracks `createdBy`)

The `createdBy` field in these entities will receive either:
- The value from `X-Hass-User` header (when explicitly provided)
- The value from `app.default-dev-user` config (when header missing in dev mode)
- An error will occur (when header missing in production mode)

## State Transitions

### Request Processing States

```
┌─────────────────┐
│ Request Arrives │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│ Check X-Hass-User header│
└────────┬────────────────┘
         │
         ├─── Header Present ──────────────┐
         │                                  │
         └─── Header Missing               │
              │                             │
              ▼                             │
         ┌──────────────┐                  │
         │ Check Mode   │                  │
         └──────┬───────┘                  │
                │                           │
         ┌──────┴─────────┐                │
         │                │                │
    Production       Development          │
         │                │                │
         ▼                ▼                │
    ┌────────┐      ┌──────────┐          │
    │ ERROR  │      │ Add      │          │
    │ 400/401│      │ Default  │          │
    └────────┘      │ Header   │          │
                    └─────┬────┘          │
                          │                │
                          └────────┬───────┘
                                   │
                                   ▼
                          ┌────────────────┐
                          │ Controller     │
                          │ Processes      │
                          │ Request with   │
                          │ X-Hass-User    │
                          └────────────────┘
```

## Validation Rules

### Development Mode Validation

1. **Mode Detection**:
   - If Spring profile contains "dev" → development mode
   - Otherwise → production mode
   - Rule: Profile is authoritative source for mode determination

2. **Default User Validation (Development Mode)**:
   - Must be non-null
   - Must be non-empty after trimming
   - Should be clearly identifiable as development user
   - Recommended pattern: lowercase with hyphen (e.g., "dev-user", "local-dev")

3. **Header Handling (Development Mode)**:
   - If `X-Hass-User` header present and non-empty → use provided value
   - If `X-Hass-User` header missing or empty → use default user
   - Rule: Explicit header always takes precedence over default

4. **Header Handling (Production Mode)**:
   - `X-Hass-User` header MUST be present
   - `X-Hass-User` header MUST be non-empty
   - Rule: No fallback, fail fast with clear error message

### Security Constraints

1. **Production Safety**:
   - Default configuration MUST be production-safe (`dev-mode=false`)
   - Development mode MUST require explicit activation
   - No automatic mode detection based on hostname or port

2. **Header Priority**:
   - Explicit `X-Hass-User` header MUST override any defaults
   - Prevents developer confusion when testing multi-user scenarios
   - Ensures production behavior is always testable in development

## Relationships

### Configuration → Runtime Behavior

```
application.properties          application-dev.properties
┌─────────────────────┐        ┌──────────────────────┐
│ app.dev-mode=false  │  ┌───→ │ app.dev-mode=true    │
│ app.default-dev-user│  │     │ app.default-dev-user │
│ = ""                │  │     │ = "dev-user"         │
└─────────────────────┘  │     └──────────────────────┘
                         │
                         │ Spring Profile: dev
                         │
                         ▼
              ┌──────────────────────┐
              │ AuthHeaderInterceptor│
              │                      │
              │ @Value dev-mode      │
              │ @Value default-user  │
              └──────────┬───────────┘
                         │
                         │ Injects into
                         ▼
              ┌──────────────────────┐
              │ HttpServletRequest   │
              │                      │
              │ X-Hass-User: <value> │
              └──────────┬───────────┘
                         │
                         │ Read by
                         ▼
              ┌──────────────────────┐
              │ Controllers          │
              │ @RequestHeader       │
              │ ("X-Hass-User")      │
              └──────────────────────┘
```

## Non-Functional Attributes

### Performance

- **Overhead**: Negligible
  - Single boolean check per request (`if devMode`)
  - Single string comparison (`if header == null`)
  - Header map lookup is O(1)
- **No database queries**: Configuration loaded at startup
- **No I/O operations**: All operations in-memory

### Security

- **Fail-Safe Default**: Production mode by default
- **Explicit Opt-In**: Development mode requires profile activation
- **Audit Trail Preserved**: All actions still attributed to user (default or explicit)
- **No Bypass Mechanism**: Production authentication cannot be disabled at runtime

### Maintainability

- **Configuration-Based**: Behavior controlled by properties, not code
- **Centralized Logic**: Single interceptor class handles all logic
- **Framework-Native**: Uses Spring Boot standard patterns
- **Logging**: Clear visibility into mode and header decisions

## Summary

This feature is purely configuration and infrastructure-focused:
- ✅ No new database entities
- ✅ No changes to existing entity schemas
- ✅ No data migrations required
- ✅ Configuration-driven behavior
- ✅ Transparent to business logic layer

The "data model" is limited to runtime configuration properties that control request processing behavior.
