# Research: Home Assistant Deployment Setup

**Branch**: `015-ha-deployment-setup` | **Date**: 2026-02-15

## Research Tasks

### R1: How HA Ingress Routes Requests to Add-ons

**Decision**: The Supervisor strips the ingress prefix (`/api/hassio_ingress/TOKEN/`) before forwarding to the add-on. The add-on receives requests at `/` with the original path intact (minus the ingress prefix).

**Rationale**: The Supervisor code in `supervisor/api/ingress.py` extracts the path after the token and constructs `http://{addon_ip}:{ingress_port}/{path}`. This means the add-on's nginx receives clean paths like `/`, `/budgets`, `/api/expenses` — not ingress-prefixed paths.

**Alternatives Considered**:
- Full path pass-through (add-on handles stripping) — not how HA works
- Token-based URL rewriting in the add-on — unnecessary since Supervisor handles it

### R2: Headers Sent by Supervisor to Add-ons via Ingress

**Decision**: The Supervisor sends `X-Remote-User-Name`, `X-Remote-User-Display-Name`, `X-Remote-User-Id`, `X-Ingress-Path`, and `X-Forwarded-For`. It does NOT send `X-Hass-User`.

**Rationale**: Confirmed from Supervisor source code and community documentation. The `X-Hass-User` header is a convention used by some add-ons' own nginx proxies, not a Supervisor-provided header. The add-on's nginx must map `X-Remote-User-Name` → `X-Hass-User` for the Spring Boot backend to recognize the user.

**Alternatives Considered**:
- Modify backend to read `X-Remote-User-Name` instead of `X-Hass-User` — would break existing convention used by all other features
- Use `X-Remote-User-Display-Name` — display names may differ from usernames; `X-Remote-User-Name` matches the HA login username

### R3: Ingress Path Stability and Discovery

**Decision**: The ingress path token is stable for the lifetime of an add-on session. It changes when the add-on is reinstalled or the ingress session is recreated. The add-on can discover its own ingress URL at startup by calling `GET http://supervisor/addons/self/info` with the `SUPERVISOR_TOKEN` environment variable.

**Rationale**: The `SUPERVISOR_TOKEN` env var is automatically set by the Supervisor for all add-ons. The API endpoint returns `data.ingress_url` containing the path like `/api/hassio_ingress/TOKEN`. Since the token is stable per session and the add-on's startup script runs on every start, querying once at startup is sufficient.

**Alternatives Considered**:
- Hardcode ingress path — impossible, token is dynamic
- Use `X-Ingress-Path` header per-request in nginx variables — nginx `sub_filter` doesn't support variable substitution in replacement strings
- Use `tempio` template engine — only processes `/data/options.json`, can't call Supervisor API

### R4: Static Asset Path Problem and Solution

**Decision**: Use nginx `sub_filter` to rewrite `/_next/` references in HTML and JavaScript responses to `{INGRESS_PATH}/_next/`. The ingress path is baked into the nginx config at startup via `sed`.

**Rationale**: Next.js generates absolute paths like `/_next/static/chunks/main.js`. In the browser (running in HA's iframe), these resolve to `https://ha-host:8123/_next/static/...` which is HA Core's origin — not the ingress route. By rewriting to `{INGRESS_PATH}/_next/static/...`, the browser requests go through the ingress handler and reach the add-on correctly.

The `sub_filter` module is included in nginx:alpine by default and operates on uncompressed response bodies. Since we proxy directly to Next.js (port 3000) without an intermediate nginx, responses are uncompressed by default. We also set `Accept-Encoding: ""` to ensure this.

**Alternatives Considered**:
- Next.js `basePath` — build-time only; ingress path is dynamic
- Next.js `assetPrefix: '.'` — relative paths break for nested routes (`/budgets/123`)
- HTML `<base href>` tag — doesn't affect absolute paths (starting with `/`)
- OpenResty/Lua dynamic rewriting — requires different base image, adds complexity
- Serve static files from the add-on container — defeats proxy-only architecture

### R5: Browser-side API Calls Through Ingress

**Decision**: Create a Next.js middleware that reads the `X-Ingress-Path` header and sets it as a cookie. Inject the value into `window.__INGRESS_PATH__` via the root layout's server component. The API client (`api.ts`) reads this value and prefixes browser-side API calls.

**Rationale**: The API client currently uses an empty `baseURL` for production, making requests to same-origin paths like `/api/expenses`. Through HA ingress, `/api/expenses` resolves to HA Core's API (not the budget app). The frontend must prepend the ingress path so requests go to `/api/hassio_ingress/TOKEN/api/expenses`, which the Supervisor routes to the add-on.

**Alternatives Considered**:
- `sub_filter` for API URLs in HTML — doesn't catch JavaScript-initiated requests (axios/fetch)
- Build-time `NEXT_PUBLIC_API_URL` — can't know ingress path at build time
- Detect base path from `window.location` — fragile, requires knowing the app route depth

### R6: Client-side Navigation Under Ingress

**Decision**: Client-side navigation via Next.js `router.push()` and `<Link>` components will push paths WITHOUT the ingress prefix (e.g., `/budgets` instead of `/api/hassio_ingress/TOKEN/budgets`). This is acceptable because:
1. The app runs in an HA iframe — users typically click the sidebar panel to return, which always loads the correct ingress URL
2. If the user refreshes within the iframe at a path like `/budgets`, the iframe reloads from the top-level HA page (which has the correct ingress URL)
3. SPA navigation works without page reloads — the URL is cosmetic within the iframe

**Rationale**: Wrapping every `Link` and `router.push` call to prepend the ingress path would require touching dozens of components and adding runtime overhead. Since the app operates within HA's iframe context, the URL bar is not directly visible or usable by the user. The trade-off of simpler code vs. perfect URL fidelity favors simplicity.

**Alternatives Considered**:
- Custom `IngressLink` wrapper component — high code churn for minimal user benefit
- Next.js `basePath` at runtime — not supported by Next.js architecture
- Hash-based routing (`/#/budgets`) — requires major Next.js reconfiguration, loses SSR

### R7: Docker Compose Configuration for Application Host

**Decision**: Modify `docker-compose.prod.yml` to expose frontend port 3000 and backend port 8080 directly on the host. Add `restart: unless-stopped` to all services for automatic recovery. The existing app-host nginx service remains but is not required for the HA ingress deployment path.

**Rationale**: The add-on proxies directly to the services, bypassing the app host's nginx. Exposing ports directly ensures no double-proxying, simpler header forwarding, and `sub_filter` compatibility (Next.js responses are uncompressed). The existing nginx service is kept for potential non-HA access or debugging.

**Alternatives Considered**:
- Proxy through app host nginx — adds complexity, requires nginx to forward `X-Ingress-Path`, risk of response compression breaking `sub_filter`
- Remove app host nginx entirely — may be useful for debugging or future use cases

### R8: HA Add-on Security Best Practices

**Decision**: Restrict add-on nginx to only accept connections from the Supervisor IP (`172.30.32.2`) using `allow/deny` directives. No additional authentication is needed because the Supervisor handles session validation before forwarding requests.

**Rationale**: This follows the pattern established by the Zigbee2MQTT Proxy add-on and official HA examples (hassio-auth). The Supervisor validates ingress sessions before routing, so the add-on only needs to trust the Supervisor.

**Alternatives Considered**:
- Open access (no IP restriction) — unnecessary security risk
- Add-on level authentication — redundant with Supervisor's session management

### R9: Reference Architecture — Zigbee2MQTT Proxy Add-on

**Decision**: Use the Zigbee2MQTT Proxy add-on as the reference implementation for the ha-apps-proxy add-on. It is the most directly relevant example: an nginx-only proxy add-on that forwards to an external service on the LAN.

**Key patterns adopted**:
- nginx:alpine base image with minimal dependencies
- `tempio` for processing `/data/options.json` into nginx config (we extend this with `sed` for the ingress path)
- `allow 172.30.32.2; deny all;` for access control
- WebSocket upgrade support via `map` directive
- Custom `run.sh` entrypoint

**Source**: https://github.com/zigbee2mqtt/hassio-zigbee2mqtt/tree/master/zigbee2mqtt-proxy
