# Implementation Plan: Home Assistant Deployment Setup

**Branch**: `015-ha-deployment-setup` | **Date**: 2026-02-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-ha-deployment-setup/spec.md`

## Summary

Deploy the budget application through Home Assistant ingress using a lightweight nginx proxy add-on (`ha-apps-proxy/`) running on the HA host. The application stack (frontend, backend, database) runs on a separate host on the same LAN. The proxy add-on handles ingress path rewriting for static assets, user identity header mapping (`X-Remote-User-Name` → `X-Hass-User`), and routing. Frontend code is modified to detect the ingress base path at runtime for browser-side API calls and client-side navigation.

## Technical Context

**Language/Version**: TypeScript 5.x (frontend), Java 17 (backend), Shell/nginx (add-on)
**Primary Dependencies**: Next.js 14.x, Spring Boot 3.2.0, nginx:alpine, Material-UI v5
**Storage**: MySQL 8.0 (on application host, existing)
**Testing**: Manual integration testing (HA deployment + browser verification)
**Target Platform**: Home Assistant OS/Supervised (amd64, aarch64) + LAN application host (Linux/Docker)
**Project Type**: Web application (existing) + infrastructure/deployment (new)
**Performance Goals**: Application fully operational within 2 minutes of host startup; LAN-speed page loads
**Constraints**: HA add-on must be lightweight (nginx only, <50MB image); all traffic through HA ingress
**Scale/Scope**: Single household (1-5 users), private LAN, ~10 pages

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Specification-First | PASS | spec.md complete with 3 clarifications |
| II. Clarify Before Planning | PASS | 3 clarifications resolved via /speckit.clarify |
| III. Incremental Story-Based Delivery | PASS | 4 user stories (3×P1, 1×P2), each independently testable |
| IV. Constitution Gates | PASS | This check; re-check after Phase 1 |
| V. Task Traceability | DEFERRED | Validated at /speckit.tasks |
| VI. Test-Optional | PASS | No tests requested; manual testing sufficient |
| VII. Artifact Consistency | DEFERRED | Validated at /speckit.analyze |
| Technical Stack (Next.js + Spring Boot) | PASS | Using existing stack, add-on is nginx infrastructure |
| Authentication (X-Hass-User) | PASS | Mapped from X-Remote-User-Name via proxy |
| Deployment (private network, containerized) | PASS | HA add-on + Docker Compose on LAN |
| Multi-user household | PASS | User identity forwarded via header mapping |

## Architecture Overview

### Deployment Topology

```
┌─────────────────────┐          LAN          ┌──────────────────────────────┐
│   HA Host           │                       │   Application Host           │
│                     │                       │                              │
│  ┌───────────────┐  │                       │  ┌─────────┐                │
│  │ HA Core       │  │                       │  │ MySQL   │ (port 3306)    │
│  │  (port 8123)  │──┼───────────────────────┼─▶│ 8.0     │                │
│  └───────┬───────┘  │                       │  └────┬────┘                │
│          │          │                       │       │                     │
│  ┌───────▼───────┐  │                       │  ┌────▼────┐                │
│  │ Supervisor    │  │                       │  │ Backend │ (port 8080)    │
│  │  (ingress)    │  │                       │  │ Spring  │                │
│  └───────┬───────┘  │                       │  │ Boot    │                │
│          │          │                       │  └────┬────┘                │
│  ┌───────▼───────┐  │   proxy_pass          │       │                     │
│  │ ha-apps-proxy │  │   http://app-host     │  ┌────▼────┐                │
│  │  (nginx)      │──┼──────────────────────▶│  │Frontend │ (port 3000)    │
│  │  port 80      │  │                       │  │ Next.js │                │
│  └───────────────┘  │                       │  └─────────┘                │
└─────────────────────┘                       └──────────────────────────────┘
```

### Request Flow

```
1. Browser → https://ha-host:8123/api/hassio_ingress/TOKEN/budgets
2. HA Core → Supervisor (validates ingress session)
3. Supervisor strips prefix → GET /budgets + headers (X-Ingress-Path, X-Remote-User-Name)
4. Add-on nginx (port 80) receives /budgets
5a. If /api/* → proxy to http://app-host:8080/api/* with X-Hass-User mapped
5b. If /* → proxy to http://app-host:3000/* with sub_filter for /_next/ paths
6. Frontend serves page; nginx rewrites /_next/ paths to {INGRESS_PATH}/_next/
7. Browser loads assets through ingress path → works correctly
```

### Key Design Decisions

1. **Asset path rewriting via nginx `sub_filter`**: The Supervisor strips the ingress prefix before forwarding to the add-on, but the browser still uses the full ingress URL as its origin. Next.js generates absolute `/_next/` asset paths that resolve against the HA origin (not ingress), causing 404s. The add-on's nginx uses `sub_filter` to rewrite `/_next/` references in HTML and JavaScript responses to `{INGRESS_PATH}/_next/`, making browsers request assets through the ingress path.

2. **Ingress path determined at startup**: The ingress token is stable per add-on session (changes only on restart). At startup, the add-on's `run.sh` script queries the Supervisor API (`GET /addons/self/info`) for the ingress URL and generates the nginx config with the path baked into `sub_filter` rules via `sed`.

3. **Direct proxy to services (skip app host nginx)**: The add-on proxies directly to the frontend (port 3000) and backend (port 8080) on the app host, bypassing the app host's own nginx. This avoids double-proxying, simplifies header forwarding, and ensures `sub_filter` processes uncompressed responses from Next.js.

4. **Frontend runtime ingress detection**: A Next.js middleware reads the `X-Ingress-Path` header and injects it into the page as `window.__INGRESS_PATH__`. Client-side code (API client, navigation) reads this value to construct proper ingress-relative URLs for browser-side requests.

5. **User identity mapping**: The Supervisor sends `X-Remote-User-Name` (not `X-Hass-User`). The add-on nginx maps this header so the existing backend authentication works unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/015-ha-deployment-setup/
├── plan.md              # This file
├── research.md          # Phase 0: technical research and decisions
├── quickstart.md        # Phase 1: deployment and testing guide
└── tasks.md             # Phase 2: task breakdown (via /speckit.tasks)
```

### Source Code (files modified or created)

```text
ha-apps-proxy/                       # HA add-on (MODIFIED/CREATED)
├── Dockerfile                       # MODIFIED: add curl/jq, copy run.sh + template
├── config.yaml                      # MODIFIED: proper options schema
├── run.sh                           # NEW: startup script (query API, generate config)
└── nginx.conf.template              # NEW: nginx template with __INGRESS_PATH__ placeholder

budget-frontend/                     # Frontend (MODIFIED)
├── src/
│   ├── middleware.ts                # NEW: read X-Ingress-Path, set cookie
│   ├── contexts/
│   │   └── IngressContext.tsx       # NEW: React context for ingress path
│   ├── app/
│   │   └── layout.tsx              # MODIFIED: inject window.__INGRESS_PATH__
│   └── services/
│       └── api.ts                  # MODIFIED: prefix API calls with ingress path

docker-compose.prod.yml              # MODIFIED: expose ports 3000/8080, restart policies
```

**Structure Decision**: This feature modifies the existing web application structure. No new projects are created. The ha-apps-proxy directory is an existing skeleton that gets completed. Frontend changes are minimal — a middleware, a context provider, and modifications to 2 existing files.

## Complexity Tracking

> No constitution violations. No complexity justification needed.
