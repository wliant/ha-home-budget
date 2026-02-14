# Tasks: Home Assistant Deployment Setup

**Input**: Design documents from `/specs/015-ha-deployment-setup/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Not requested. Manual verification per quickstart.md.

**Organization**: Tasks are grouped by user story. US3 (Static Assets) is primarily implemented by foundational tasks (T002 nginx sub_filter) and has no separate phase. US4 (Data Persistence) is addressed by US2's docker-compose tasks.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Foundational — HA Add-on Core Configuration

**Purpose**: Create the complete HA proxy add-on. ALL user stories depend on these files being correct. The nginx template implements routing (US1), asset path rewriting (US3), header mapping (US1), and error handling (US2). The startup script discovers the dynamic ingress path (US3).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 [P] Update ha-apps-proxy/config.yaml with proper HA add-on manifest: set options schema with `server_host` (str), `frontend_port` (port, default 3000), `backend_port` (port, default 8080); keep `ingress: true`, `ingress_port: 80`; set `arch` to aarch64 and amd64 only; keep `panel_icon: mdi:wallet`, `panel_title: Budget`; add `hassio_api: true` to allow Supervisor API access from the add-on; remove the obsolete `target_url` option and its schema entry. Reference: research.md R9 (Zigbee2MQTT Proxy pattern).

- [X] T002 [P] Create ha-apps-proxy/nginx.conf.template with complete reverse proxy configuration using `__SERVER_HOST__`, `__FRONTEND_PORT__`, `__BACKEND_PORT__`, and `__INGRESS_PATH__` as sed placeholders. The template must include: (1) `map $http_upgrade $connection_upgrade` block for WebSocket support; (2) `server` block listening on port 80 with `allow 172.30.32.2; deny all;` for Supervisor-only access; (3) `location /api/` block that proxies to `http://__SERVER_HOST__:__BACKEND_PORT__/api/` with `proxy_set_header X-Hass-User $http_x_remote_user_name;` for user identity mapping, plus standard proxy headers (Host, X-Real-IP, X-Forwarded-For, X-Forwarded-Proto); (4) `location /` block that proxies to `http://__SERVER_HOST__:__FRONTEND_PORT__/` with WebSocket upgrade headers, `proxy_set_header Accept-Encoding "";` to disable upstream compression, `sub_filter '/_next/' '__INGRESS_PATH__/_next/';` with `sub_filter_once off;` and `sub_filter_types text/html application/javascript text/javascript;` for asset path rewriting, plus `proxy_set_header X-Ingress-Path $http_x_ingress_path;`; (5) `error_page 502 504 /error.html;` with `location = /error.html` serving the custom error page from `/usr/share/nginx/html/error.html` with `internal;` directive. Reference: research.md R1, R2, R4, R8.

- [X] T003 [P] Create ha-apps-proxy/run.sh startup script (shell, executable). The script must: (1) read `server_host`, `frontend_port`, `backend_port` from `/data/options.json` using jq; (2) query the Supervisor API at `http://supervisor/addons/self/info` with header `Authorization: Bearer ${SUPERVISOR_TOKEN}` using curl, extract `.data.ingress_url` with jq, and store as INGRESS_PATH; (3) log the discovered ingress path to stdout for add-on log visibility; (4) use sed to replace `__SERVER_HOST__`, `__FRONTEND_PORT__`, `__BACKEND_PORT__`, and `__INGRESS_PATH__` placeholders in `/nginx.conf.template` and write to `/tmp/nginx.conf`; (5) validate the generated config with `nginx -t -c /tmp/nginx.conf`; (6) exec nginx in foreground: `exec nginx -g 'daemon off;' -c /tmp/nginx.conf`. Add error handling: if Supervisor API call fails, log a warning and set INGRESS_PATH to empty string (sub_filter becomes a no-op, degraded but functional). Reference: research.md R3, R9.

- [X] T004 [P] Create ha-apps-proxy/error.html as a self-contained HTML page (no external dependencies) that displays a user-friendly "Budget App Unavailable" message with the wallet icon (using inline SVG or Unicode), a brief explanation that the application host may be offline, and a suggestion to check the add-on configuration. Style with inline CSS using a clean, centered layout. This file will be served by nginx when the application host returns 502/504. Reference: spec.md FR-013.

- [X] T005 Update ha-apps-proxy/Dockerfile to: use `nginx:alpine` as base image; `RUN apk add --no-cache curl jq` to install dependencies for run.sh; `COPY nginx.conf.template /nginx.conf.template` for the proxy template; `COPY run.sh /run.sh` and `RUN chmod +x /run.sh` for the startup script; `COPY error.html /usr/share/nginx/html/error.html` for the custom error page; set `CMD ["/run.sh"]` as the entrypoint. Remove the existing `COPY nginx.conf /etc/nginx/nginx.conf` line since the config is now generated at startup. Reference: research.md R9 (Zigbee2MQTT Proxy Dockerfile pattern).

**Checkpoint**: HA add-on is fully configured. Install on HA instance to verify it starts, discovers the ingress path, and generates nginx config. The add-on should show in the sidebar but will return 502 if the application host is not yet configured.

---

## Phase 2: User Story 1 — Access Budget App Through Home Assistant (Priority: P1) 🎯 MVP

**Goal**: Household members can access the budget app through the HA sidebar. All pages load, data operations work, and user identity is correctly recognized.

**Independent Test**: Install the HA add-on, click the Budget sidebar icon, verify the homepage loads with all visual elements. Navigate to Budgets, Categories, Expenses. Create a budget and verify the correct user identity is captured.

### Implementation for User Story 1

- [X] T006 [P] [US1] Create Next.js middleware to detect the ingress path in budget-frontend/src/middleware.ts. The middleware should: (1) run on all routes using `export const config = { matcher: '/:path*' }`; (2) read the `x-ingress-path` header from the incoming request (set by the HA proxy's nginx via `$http_x_ingress_path`); (3) if the header exists, set a cookie named `__ingress_path` with the value (e.g., `/api/hassio_ingress/TOKEN`), with `path: '/'`, `sameSite: 'lax'`, and no expiry (session cookie); (4) call `NextResponse.next()` with the cookie set in the response. This middleware enables client-side JavaScript to discover the ingress base path. Reference: research.md R5.

- [X] T007 [P] [US1] Create IngressContext React context in budget-frontend/src/contexts/IngressContext.tsx. The context should: (1) define `IngressContextType` with `ingressPath: string` (the base path, e.g., `/api/hassio_ingress/TOKEN` or empty string); (2) create `IngressProvider` component that reads `window.__INGRESS_PATH__` on mount (via useEffect) and stores in state; (3) export `useIngressPath()` hook that returns the ingress path string; (4) export a helper function `getIngressApiUrl(path: string): string` that prepends the ingress path to API paths (e.g., `getIngressApiUrl('/api/budgets')` returns `/api/hassio_ingress/TOKEN/api/budgets`). Default to empty string when no ingress path is detected (non-HA access). Reference: research.md R5.

- [X] T008 [US1] Modify the root layout in budget-frontend/src/app/layout.tsx to: (1) in the server component, import `headers` from `next/headers` and read the `x-ingress-path` header value; (2) inject a `<script dangerouslySetInnerHTML>` tag in the `<head>` that sets `window.__INGRESS_PATH__ = '{value}'` (JSON-stringify the value to prevent XSS); (3) wrap the app children with `<IngressProvider>` from IngressContext.tsx. The ingress path value comes from the `X-Ingress-Path` header that the HA proxy nginx forwards to the Next.js server. When accessed without ingress (e.g., direct), the header is absent and the value defaults to empty string. Depends on T007.

- [X] T009 [US1] Modify the API client in budget-frontend/src/services/api.ts to prefix browser-side API requests with the ingress path. Changes: (1) for the browser-side (non-SSR) code path, read `window.__INGRESS_PATH__` (if it exists) and use it as the base URL prefix; (2) construct the Axios `baseURL` as `(window.__INGRESS_PATH__ || '') + (process.env.NEXT_PUBLIC_API_URL || '')` for browser-side requests; (3) keep the server-side (SSR) code path unchanged — it should continue using `INTERNAL_API_URL` or `NEXT_PUBLIC_API_URL` as before; (4) ensure the interceptors and error handling remain unchanged. This ensures browser API calls go through the ingress path (e.g., `/api/hassio_ingress/TOKEN/api/budgets`) instead of directly to `/api/budgets` which would hit HA Core. Reference: research.md R5.

**Checkpoint**: At this point, the budget app should be fully accessible through HA ingress. All pages load, navigation works, API calls succeed, and user identity is recognized. US1 acceptance scenarios can be verified.

---

## Phase 3: User Story 2 — Application Stack Reliability (Priority: P1)

**Goal**: The application stack on the separate host starts automatically, restarts on failure, and presents meaningful errors when unreachable.

**Independent Test**: Start the application stack on the host with `docker compose up -d`. Reboot the host. Verify all services come back automatically. Stop the stack and verify the HA add-on shows the custom error page.

### Implementation for User Story 2

- [X] T010 [US2] Modify docker-compose.prod.yml to support the HA proxy deployment model: (1) add `ports: ["3000:3000"]` to the frontend service so the HA add-on can proxy directly to it (bypassing the app host's nginx); (2) add `ports: ["8080:8080"]` to the backend service for the same reason; (3) add `restart: unless-stopped` to the mysql, backend, and frontend services for automatic recovery on host reboot; (4) verify the mysql service has a robust health check (current: `mysqladmin ping`); (5) verify the backend `depends_on` uses `condition: service_healthy` for mysql; (6) the existing nginx service can remain for backward compatibility but is not needed for the HA ingress path. Reference: research.md R7, spec.md FR-006, FR-011.

- [X] T011 [P] [US2] Update .env.prod.example with complete configuration guidance: add comments documenting each variable, add `SERVER_HOST` variable placeholder with comment explaining this is the IP/hostname that the HA add-on uses to reach this host, and document the relationship between docker-compose ports and the HA add-on's frontend_port/backend_port settings. Reference: quickstart.md Step 1.

**Checkpoint**: The application stack starts automatically on host boot, services restart after failures, and the HA add-on shows meaningful errors when the host is unreachable. US2 acceptance scenarios (and US4 data persistence) can be verified.

---

## Phase 4: User Story 3 — Static Assets Under Ingress Path (Priority: P1)

**Note**: US3 is primarily implemented by T002 (nginx sub_filter in the foundational phase). The sub_filter rewrites all `/_next/` references in HTML and JavaScript responses to include the ingress path prefix, ensuring browsers request assets through the ingress route.

No additional tasks are needed. Verification: open browser DevTools Network tab, navigate through pages, confirm zero 404 errors for `/_next/` resources.

---

## Phase 5: User Story 4 — Data Persistence (Priority: P2)

**Note**: US4 is covered by T010 (docker-compose with named volumes and restart policies). The existing `mysql-data` named Docker volume already persists data across container restarts. The `restart: unless-stopped` policy ensures services recover after host reboots.

No additional tasks are needed. Verification: create test data, restart the application host, verify all data is intact.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and documentation

- [X] T012 Remove obsolete ha-apps-proxy/nginx.conf file (replaced by nginx.conf.template which is generated at runtime by run.sh)

- [ ] T013 Run quickstart.md verification checklist: install the HA add-on, configure it with the application host address, verify all 4 user story acceptance scenarios per specs/015-ha-deployment-setup/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately
- **US1 (Phase 2)**: Depends on Foundational (Phase 1) completion — BLOCKS on add-on being ready
- **US2 (Phase 3)**: Depends on Foundational (Phase 1) completion — can run in PARALLEL with US1
- **US3 (Phase 4)**: Fully covered by Foundational (Phase 1) — no additional work
- **US4 (Phase 5)**: Covered by US2 (Phase 3) — no additional work
- **Polish (Phase 6)**: Depends on all previous phases

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 1. Requires T006-T009 (frontend changes). Primary MVP.
- **US2 (P1)**: Can start after Phase 1. Requires T010-T011 (docker-compose). Independent of US1.
- **US3 (P1)**: No additional tasks. Covered by T002 (nginx sub_filter). Verify after Phase 1.
- **US4 (P2)**: No additional tasks. Covered by T010 (volumes + restart). Verify after Phase 3.

### Within Each Phase

**Phase 1**: T001, T002, T003, T004 are all [P] (parallel, different files). T005 depends on all of them (Dockerfile copies all files).

**Phase 2**: T006 and T007 are [P] (parallel, different files). T008 depends on T007 (uses IngressContext). T009 depends on T008 (needs window.__INGRESS_PATH__ injected).

**Phase 3**: T010 and T011 are [P] (different files).

### Parallel Opportunities

```
Phase 1 (all parallel):
  T001 (config.yaml) | T002 (nginx template) | T003 (run.sh) | T004 (error.html)
  → T005 (Dockerfile) after all complete

Phase 2 + Phase 3 (can run concurrently):
  Phase 2: T006 (middleware) | T007 (context) → T008 (layout) → T009 (api.ts)
  Phase 3: T010 (docker-compose) | T011 (.env.prod.example)
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Foundational (T001-T005)
2. Complete Phase 2: US1 (T006-T009)
3. **STOP and VALIDATE**: Install add-on, verify app loads through HA ingress, test API calls and auth
4. Deploy if ready — this delivers core value

### Incremental Delivery

1. Phase 1 (Foundational) → Add-on installable on HA
2. Phase 2 (US1) → App accessible through HA ingress (MVP!)
3. Phase 3 (US2) → Reliable auto-restart on app host
4. Phase 6 (Polish) → Cleanup and documentation
5. Each phase adds value without breaking previous phases

### FR Coverage

| FR | Covered By |
|----|------------|
| FR-001 (installable add-on) | T001, T005 |
| FR-002 (configurable host) | T001, T003 |
| FR-003 (ingress serving) | T002 |
| FR-004 (asset paths) | T002 (sub_filter) |
| FR-005 (user identity) | T002 (header mapping) |
| FR-006 (data persistence) | T010 (volumes) |
| FR-007 (routing) | T002 |
| FR-008 (dynamic ingress) | T003 (API discovery) |
| FR-009 (multi-arch) | T001, T005 |
| FR-010 (sidebar panel) | T001 |
| FR-011 (dependency order) | T010 (health checks) |
| FR-012 (SPA routing) | T002 (try_files/proxy) |
| FR-013 (error page) | T002, T004 |
| FR-014 (API through ingress) | T006-T009 |
| FR-015 (backend accepts proxy) | T002 (header forwarding) |

---

## Notes

- [P] tasks = different files, no dependencies
- [US#] label maps task to specific user story for traceability
- US3 and US4 have no dedicated tasks — they are covered by foundational and US2 tasks respectively
- No tests are included (not requested in spec)
- All tasks include exact file paths for immediate executability
- Commit after each task or logical group
