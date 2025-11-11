# Research: Development Mode Default User Header

**Feature**: 003-dev-mode-default-header
**Date**: 2025-10-28
**Phase**: Phase 0 - Research

## Research Questions

1. How should Spring Boot detect and supply default headers in development mode?
2. How should Next.js handle optional headers in development mode?
3. What is the best way to configure environment-based behavior?
4. How can we ensure production security is not compromised?

---

## Decision 1: Spring Boot Header Handling Strategy

**Decision**: Use a Spring `HandlerInterceptor` to intercept requests and add default X-Hass-User header when missing in development mode.

**Rationale**:
- `HandlerInterceptor` executes before controller methods, allowing header injection before `@RequestHeader` extraction
- Cleaner than modifying all controller methods to make header optional
- Centralized logic in one place (`AuthHeaderInterceptor`)
- Can check Spring profile (`dev` vs `prod`) to determine behavior
- Transparent to existing controller code - no changes needed to `@RequestHeader` annotations

**Alternatives Considered**:
1. **Modify all controllers to use `@RequestHeader(required=false)`**
   - Rejected: Requires changes to every controller method
   - Rejected: Opens security risk - production code would allow missing headers
   - Rejected: Business logic would need environment checks in multiple places

2. **Use a Servlet Filter**
   - Rejected: Filters run before Spring MVC, more complex integration
   - Rejected: Less idiomatic for Spring Boot applications
   - HandlerInterceptor is the Spring-native approach

3. **Custom ArgumentResolver for @RequestHeader**
   - Rejected: More complex implementation
   - Rejected: Requires framework-level customization
   - Interceptor approach is simpler and well-documented

**Implementation Pattern**:
```java
@Component
public class AuthHeaderInterceptor implements HandlerInterceptor {
    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.default-dev-user:dev-user}")
    private String defaultDevUser;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        if (devMode && request.getHeader("X-Hass-User") == null) {
            // Wrap request to add header
            request = new HeaderModifyingRequestWrapper(request, "X-Hass-User", defaultDevUser);
        }
        return true;
    }
}
```

---

## Decision 2: Request Wrapper for Header Modification

**Decision**: Use `HttpServletRequestWrapper` to add the X-Hass-User header dynamically.

**Rationale**:
- `HttpServletRequest` is immutable - cannot directly modify headers
- `HttpServletRequestWrapper` is the standard Java EE pattern for request modification
- Override `getHeader()` and `getHeaders()` methods to include the default header
- Spring Boot controllers will see the modified request with the header present

**Alternatives Considered**:
1. **Use request attributes instead of headers**
   - Rejected: Would require changing all controllers to read attributes instead of headers
   - Rejected: Breaks the contract with existing X-Hass-User header mechanism

2. **Modify ServletRequest in place**
   - Rejected: Not possible - HttpServletRequest is immutable

**Implementation Pattern**:
```java
public class HeaderModifyingRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders;

    public HeaderModifyingRequestWrapper(HttpServletRequest request,
                                         String name, String value) {
        super(request);
        this.customHeaders = new HashMap<>();
        this.customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(Arrays.asList(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }
}
```

---

## Decision 3: Environment Configuration Strategy

**Decision**: Use Spring Profiles with `application-dev.properties` for backend, `NODE_ENV` for frontend.

**Rationale**:
- Spring Profiles are the standard Spring Boot mechanism for environment-specific configuration
- Profile `dev` activates development-specific beans and properties
- Profile `prod` (or no profile) maintains production behavior
- Configurable via `SPRING_PROFILES_ACTIVE` environment variable
- Next.js automatically loads `.env.development` when `NODE_ENV=development`

**Configuration Files**:

**backend/src/main/resources/application.properties**:
```properties
# Default to production mode (secure)
app.dev-mode=false
app.default-dev-user=
```

**backend/src/main/resources/application-dev.properties**:
```properties
# Development mode settings
app.dev-mode=true
app.default-dev-user=dev-user
```

**frontend/.env.development**:
```
# Development mode - X-Hass-User header optional in api.ts
NEXT_PUBLIC_DEV_MODE=true
```

**docker-compose.yml**:
```yaml
backend:
  environment:
    - SPRING_PROFILES_ACTIVE=dev
frontend:
  environment:
    - NODE_ENV=development
```

**Alternatives Considered**:
1. **Use single environment variable across both services**
   - Rejected: Each framework has its own conventions (Spring Profiles vs NODE_ENV)
   - Chosen approach respects framework idioms

2. **Hard-code development mode detection**
   - Rejected: Less flexible, requires code changes to toggle behavior
   - Configuration-based approach is more maintainable

---

## Decision 4: Frontend Header Handling

**Decision**: Frontend does NOT need to change header handling - it already uses axios which allows optional headers.

**Rationale**:
- Current frontend code in `api.ts` likely already supports optional headers
- Backend interceptor handles the missing header case
- Frontend can continue to omit X-Hass-User in development
- Frontend can still provide explicit X-Hass-User to override default (US2 requirement)
- Simplest implementation: no frontend code changes needed for US1

**Verification Needed**: Check current `api.ts` implementation to confirm headers are not required.

**Alternatives Considered**:
1. **Add frontend environment check to conditionally add header**
   - Rejected: Unnecessary complexity
   - Backend already handles missing header

2. **Create frontend interceptor to add default header**
   - Rejected: Duplicates backend logic
   - Backend is the authoritative source for authentication

---

## Decision 5: Logging Strategy

**Decision**: Add startup logging in `HomeBudgetApplication` and per-request logging in `AuthHeaderInterceptor`.

**Rationale**:
- Startup logging clearly indicates which mode is active when application starts
- Per-request logging shows when default user is being applied
- Helps developers understand behavior without reading code
- Satisfies US3 requirement for "Clear Development Mode Indicators"

**Implementation**:

**HomeBudgetApplication.java**:
```java
@SpringBootApplication
public class HomeBudgetApplication {
    private static final Logger logger = LoggerFactory.getLogger(HomeBudgetApplication.class);

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    public static void main(String[] args) {
        SpringApplication.run(HomeBudgetApplication.class, args);
    }

    @PostConstruct
    public void logStartupMode() {
        if (devMode) {
            logger.warn("APPLICATION RUNNING IN DEVELOPMENT MODE - Default user authentication enabled");
            logger.warn("X-Hass-User header will default to '{}' when not provided", defaultDevUser);
        } else {
            logger.info("APPLICATION RUNNING IN PRODUCTION MODE - X-Hass-User header required");
        }
    }
}
```

**AuthHeaderInterceptor**:
```java
if (devMode && request.getHeader("X-Hass-User") == null) {
    logger.debug("Development mode: Adding default X-Hass-User header: {}", defaultDevUser);
    // ... add header logic
}
```

**Alternatives Considered**:
1. **Only log at startup**
   - Rejected: Doesn't show per-request behavior
   - Developers may not realize default is being applied

2. **Log at INFO level**
   - Rejected: Too noisy in development
   - DEBUG level is more appropriate for per-request logging

---

## Decision 6: Edge Case Handling

**Decision**: Treat empty header (`X-Hass-User: ""`) same as missing header in development mode.

**Rationale**:
- Empty string is functionally equivalent to missing
- Prevents accidental authentication errors from empty headers
- Simplifies developer experience

**Implementation**:
```java
String hassUser = request.getHeader("X-Hass-User");
if (devMode && (hassUser == null || hassUser.trim().isEmpty())) {
    // Apply default user
}
```

**Alternatives Considered**:
1. **Reject empty headers with error**
   - Rejected: Defeats the purpose of improving developer experience
   - Empty header is likely a mistake, should be corrected automatically in dev mode

2. **Only handle null, not empty**
   - Rejected: Empty string would still cause authentication failures
   - More lenient approach improves DX

---

## Security Considerations

### Production Safety Checklist

✅ **Default configuration is secure**: `app.dev-mode=false` in base `application.properties`

✅ **Development mode must be explicitly enabled**: Requires setting Spring profile to `dev`

✅ **Interceptor checks dev mode flag**: Production deployments will NOT add default headers

✅ **No code changes to production path**: Controllers still require `@RequestHeader("X-Hass-User")`

✅ **Accidental deployment protection**: If dev mode accidentally enabled in production, only adds header - doesn't bypass authentication

✅ **Explicit header always honored**: If X-Hass-User provided, it's used regardless of mode

### Potential Risks

⚠️ **Risk**: Developer accidentally deploys with `SPRING_PROFILES_ACTIVE=dev`
**Mitigation**:
- Docker Compose and deployment docs should explicitly use `prod` profile
- Startup logging makes mode visible
- Consider adding environment variable validation in production

⚠️ **Risk**: Default user has elevated permissions
**Mitigation**:
- This application doesn't have role-based permissions (home household use)
- All users have same access level
- Risk is minimal for this use case

---

## Testing Strategy

### Manual Testing Plan

**US1 - Default Header in Development**:
1. Start backend with `SPRING_PROFILES_ACTIVE=dev`
2. Use curl without X-Hass-User header
3. Verify budget creation succeeds
4. Verify `createdBy` field shows "dev-user"

**US2 - Override with Explicit Header**:
1. Start backend with `SPRING_PROFILES_ACTIVE=dev`
2. Use curl with `X-Hass-User: alice`
3. Verify budget creation succeeds
4. Verify `createdBy` field shows "alice"

**US3 - Production Mode Enforcement**:
1. Start backend WITHOUT dev profile (or with `prod`)
2. Use curl without X-Hass-User header
3. Verify request fails with 400/401 error
4. Check logs show "PRODUCTION MODE"

### Automated Testing

Tests are optional per constitution. If tests are added later:
- Unit test for `AuthHeaderInterceptor` logic
- Unit test for `HeaderModifyingRequestWrapper`
- Integration test for controller behavior in both modes

---

## Summary

The implementation will use:
1. Spring `HandlerInterceptor` with `HttpServletRequestWrapper` to inject default header
2. Spring Profiles (`dev` vs `prod`) for environment detection
3. Configuration properties for default user value
4. Startup and per-request logging for visibility
5. No frontend changes needed (backend handles everything)

This approach is:
- ✅ Minimal code changes (1 new class, minor config changes)
- ✅ Centralized logic in interceptor
- ✅ Secure by default (production mode unless explicitly enabled)
- ✅ Transparent to existing controllers
- ✅ Fully configurable via environment variables
- ✅ Clear logging for developer awareness
