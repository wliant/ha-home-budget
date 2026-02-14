# Feature Specification: Home Assistant Deployment Setup

**Feature Branch**: `015-ha-deployment-setup`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "Deploy app on home LAN with Home Assistant server directing traffic. The ha-apps-proxy folder contains HA addon configuration. Ensure frontend, backend and database are deployed and work with this deployment setup, with correct asset base paths."

## Clarifications

### Session 2026-02-15

- Q: What is the intended deployment architecture — proxy-only add-on, all-in-one add-on, or multi-add-on? → A: Proxy-only add-on. The ha-apps-proxy is a lightweight HA add-on running on the Home Assistant host. The database, backend, and frontend run on a separate host on the same local network.
- Q: What is the scope of allowed code changes for ingress path support — proxy-only, frontend+proxy, or full adaptation? → A: Full adaptation. Any frontend and backend changes needed to fully support the HA ingress deployment model are in scope.
- Q: Must the application continue to work when accessed directly (not through HA ingress)? → A: No. HA ingress only. All user access goes through Home Assistant; direct access is not required.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Access Budget App Through Home Assistant (Priority: P1)

A household member opens their Home Assistant dashboard and clicks the "Budget" panel icon in the sidebar. The budget application loads fully — including all pages, images, and interactive elements — within Home Assistant's ingress frame. The user can immediately start managing budgets and expenses without any additional login or configuration.

**Why this priority**: This is the core value proposition — without a working deployment through Home Assistant, the application is not accessible to household members on the home network.

**Independent Test**: Can be fully tested by installing the add-on on a Home Assistant instance, clicking the Budget sidebar icon, and verifying the application loads correctly with all visual elements intact.

**Acceptance Scenarios**:

1. **Given** the Home Assistant add-on is installed and running, **When** a household member clicks the "Budget" panel icon in the Home Assistant sidebar, **Then** the budget application homepage loads completely with all visual elements (navigation, charts, data) displayed correctly.
2. **Given** the application is loaded through Home Assistant ingress, **When** the user navigates between pages (e.g., budgets, expenses, categories), **Then** all pages render correctly with no broken images, missing styles, or JavaScript errors.
3. **Given** the application is accessed through Home Assistant, **When** the user interacts with any feature (creating budgets, recording expenses, viewing reports), **Then** all data operations succeed and the user's identity is correctly recognized from the Home Assistant authentication.

---

### User Story 2 - Application Stack Runs Reliably on Separate Host (Priority: P1)

The budget application stack (database, backend, frontend) runs on a dedicated host machine on the same local network as the Home Assistant server. The stack starts automatically, components come up in the correct dependency order, and the HA proxy add-on can reach all services reliably.

**Why this priority**: Equal to P1 because if the application stack is not running or reachable on the separate host, the HA proxy add-on has nothing to serve.

**Independent Test**: Can be tested by starting the application stack on the separate host and verifying all components become healthy. Then installing the HA proxy add-on and confirming it connects successfully.

**Acceptance Scenarios**:

1. **Given** the application stack is deployed on the separate host, **When** the host machine starts up, **Then** the database initializes, the backend connects to the database, and the frontend becomes accessible — all within 2 minutes.
2. **Given** the application stack was previously running, **When** the host machine is rebooted, **Then** all components restart automatically and resume normal operation with existing data preserved.
3. **Given** the HA proxy add-on is running but the application host is temporarily unreachable, **When** a user tries to access the budget app, **Then** the user sees a meaningful error message (e.g., "Budget app is unavailable") rather than a broken page or cryptic error.

---

### User Story 3 - Static Assets Load Correctly Under Ingress Path (Priority: P1)

When the budget app is served through Home Assistant's ingress system, all static assets (stylesheets, JavaScript bundles, fonts, icons) load correctly regardless of the base URL path that Home Assistant assigns. The application adapts to the ingress path so that no assets return 404 errors and no pages appear unstyled or broken.

**Why this priority**: This is the known pain point called out specifically. If assets fail to load due to incorrect base paths, the application appears broken even though the backend works fine.

**Independent Test**: Can be tested by accessing the app through Home Assistant ingress and checking the browser's network tab for any failed resource requests (404s, wrong paths).

**Acceptance Scenarios**:

1. **Given** Home Assistant assigns an ingress path (e.g., `/api/hassio_ingress/<token>/`), **When** the frontend serves pages, **Then** all CSS, JavaScript, image, and font resources load successfully using the correct ingress-relative paths.
2. **Given** the ingress path changes (e.g., after token rotation or HA restart), **When** the user accesses the app, **Then** assets still load correctly because the application dynamically adapts to the current ingress path.
3. **Given** the user bookmarks a page or refreshes the browser, **When** the page reloads, **Then** the application renders correctly without any "page not found" errors for client-side routes.

---

### User Story 4 - Data Persists Across Restarts (Priority: P2)

Household members expect that all their budget data, expense records, and category configurations survive application host reboots, docker restarts, and Home Assistant updates. No data should ever be lost due to routine maintenance.

**Why this priority**: Data persistence is critical for trust but is a lower-risk item since the database naturally handles this — the concern is ensuring the deployment configuration on the application host maps storage correctly.

**Independent Test**: Can be tested by creating budget data, restarting the application stack on the host, and verifying all data is still present.

**Acceptance Scenarios**:

1. **Given** a household member has recorded expenses and created budgets, **When** the application host is restarted, **Then** all data remains intact and accessible.
2. **Given** the Home Assistant server is rebooted, **When** the HA proxy add-on auto-starts and reconnects to the application host, **Then** all previous data is accessible without any manual intervention.

---

### Edge Cases

- What happens when the application host is powered off or unreachable? The HA proxy add-on should show a clear "app unavailable" message, not a raw error.
- What happens if the database takes longer than expected to initialize on the application host? The backend should retry connections rather than crash.
- What happens if the user accesses the app while the application host is still starting? A friendly "loading" or "unavailable" response should be shown instead of errors.
- What happens if the Home Assistant ingress token/path changes mid-session? The application should handle this gracefully, potentially requiring a page refresh rather than breaking entirely.
- What happens if the LAN connection between HA host and application host degrades? The proxy should show meaningful timeout errors and recover when connectivity is restored.
- What happens if the configured application host address changes (e.g., DHCP lease renewal)? The administrator should be able to update the address in the add-on configuration without reinstalling.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The HA proxy add-on MUST be installable and manageable from the Home Assistant Supervisor panel.
- **FR-002**: The HA proxy add-on MUST allow configuration of the application host address (IP or hostname) so it knows where to route traffic on the local network.
- **FR-003**: The HA proxy add-on MUST serve the frontend application through Home Assistant's ingress system so users access it via the Home Assistant sidebar.
- **FR-004**: The system MUST correctly resolve all frontend static asset paths (stylesheets, scripts, images, fonts) relative to the Home Assistant ingress base path.
- **FR-005**: The HA proxy add-on MUST forward the authenticated user identity from Home Assistant to the backend for every request so the application knows which household member is making the request.
- **FR-006**: The application stack MUST persist all database data on the application host in a location that survives container restarts and host reboots.
- **FR-007**: The HA proxy add-on MUST route frontend page requests to the frontend service and backend API requests to the backend service on the application host.
- **FR-008**: The system MUST handle the dynamic ingress path that Home Assistant assigns, rather than assuming a fixed URL path.
- **FR-009**: The HA proxy add-on MUST support at least the `amd64` and `aarch64` architectures to cover common Home Assistant hardware (x86 PCs and Raspberry Pi/ARM servers).
- **FR-010**: The HA proxy add-on MUST provide a panel icon and title in the Home Assistant sidebar for easy access to the budget application.
- **FR-011**: The application stack on the host MUST start components in the correct dependency order (database first, then backend, then frontend) so the system is healthy when the proxy connects.
- **FR-012**: The system MUST support client-side navigation (single-page app routing) so that page refreshes and direct URL access work correctly under the ingress path.
- **FR-013**: The HA proxy add-on MUST show a meaningful error page when the application host is unreachable, rather than a raw proxy error.
- **FR-014**: The frontend MUST route all browser-side API calls through the correct ingress-relative path so they reach the backend via the HA proxy, not Home Assistant's own API endpoints.
- **FR-015**: The backend MUST accept and correctly process requests that arrive through the HA proxy, including properly reading the forwarded user identity header.

### Assumptions

- The Home Assistant instance has Supervisor capabilities (Home Assistant OS or Supervised installation), which is required for add-on support.
- The HA proxy add-on runs on the Home Assistant host; the application stack (database, backend, frontend) runs on a separate host on the same local network.
- The application host is reachable from the HA host via a stable IP address or hostname on the LAN.
- The home network provides sufficient bandwidth for local traffic between the HA host and the application host (not a concern for LAN deployment).
- The HA proxy add-on is lightweight and requires minimal resources on the Home Assistant host (nginx only).
- The existing database schema does not need changes. Frontend and backend code changes are in scope to the extent needed for proper ingress path handling, asset loading, API routing, and authentication forwarding.
- All user access is through Home Assistant ingress. Direct access to the application stack (bypassing HA) is not a supported use case and does not need to be preserved.
- The `ha-apps-proxy` folder in the repository is the designated location for the Home Assistant add-on configuration.
- Home Assistant handles SSL/TLS termination at its own level; the add-on communicates with the application host over plain HTTP within the LAN.

### Dependencies

- Home Assistant Supervisor API (for add-on lifecycle management and ingress routing)
- Existing budget-frontend and budget-backend application code (running on the application host via docker-compose)
- Local network connectivity between Home Assistant host and application host

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of frontend pages load without any 404 errors for static assets when accessed through Home Assistant ingress.
- **SC-002**: The application stack becomes fully operational within 2 minutes of starting on the application host, and the HA proxy connects successfully.
- **SC-003**: All user data persists across 100% of application host restarts with zero data loss.
- **SC-004**: The authenticated user identity is correctly recognized for 100% of requests made through the Home Assistant interface.
- **SC-005**: The add-on installs and runs successfully on both x86 (amd64) and ARM (aarch64) hardware platforms.
- **SC-006**: Users can navigate between all application pages and perform all CRUD operations without any errors caused by path routing issues.
