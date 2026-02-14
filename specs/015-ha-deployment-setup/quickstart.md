# Quickstart: Home Assistant Deployment Setup

**Branch**: `015-ha-deployment-setup` | **Date**: 2026-02-15

## Prerequisites

- Home Assistant OS or Supervised installation with Supervisor
- A separate host on the same LAN running Docker and Docker Compose
- The budget application repository cloned on the application host
- Network connectivity between HA host and application host

## Deployment Steps

### Step 1: Start the Application Stack on the Application Host

```bash
# On the application host
cd /path/to/ha-hello

# Create production environment file
cp .env.prod.example .env.prod
# Edit .env.prod with your MySQL credentials

# Start the stack
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verify all services are healthy
docker compose -f docker-compose.prod.yml ps
```

Expected: mysql (healthy), backend (running), frontend (running)

### Step 2: Install the HA Add-on

1. In Home Assistant, go to **Settings → Add-ons → Add-on Store**
2. Click the **⋮** menu → **Repositories**
3. Add the repository URL pointing to this repo (or copy `ha-apps-proxy/` to your local add-on directory)
4. Find "Home Budget" in the add-on store and click **Install**

### Step 3: Configure the Add-on

1. Go to the add-on's **Configuration** tab
2. Set `server_host` to the application host's IP address (e.g., `192.168.1.100`)
3. Optionally adjust `frontend_port` (default: 3000) and `backend_port` (default: 8080)
4. Click **Save**

### Step 4: Start the Add-on

1. Click **Start** on the add-on page
2. Check the **Log** tab for startup messages
3. Expected log output:
   ```
   [INFO] Discovering ingress path from Supervisor API...
   [INFO] Ingress path: /api/hassio_ingress/TOKEN
   [INFO] Generating nginx configuration...
   [INFO] Starting nginx...
   ```

### Step 5: Access the Budget App

1. Click "Budget" in the Home Assistant sidebar (wallet icon)
2. The budget application should load in the HA panel
3. Verify: all pages load, navigation works, data operations succeed

## Verification Checklist

### US1: Access Budget App Through HA
- [ ] Click "Budget" sidebar icon → homepage loads completely
- [ ] Navigate to Budgets, Categories, Expenses → all pages render
- [ ] Create a budget → succeeds, user identity is correct
- [ ] Record an expense → succeeds, data appears in lists

### US2: Application Stack Reliability
- [ ] Restart the application host → stack auto-starts
- [ ] Restart the HA add-on → reconnects to app host
- [ ] Stop the app host → HA shows "unavailable" message (not raw error)

### US3: Static Assets
- [ ] Open browser DevTools → Network tab
- [ ] Navigate through pages → no 404 errors for /_next/ resources
- [ ] Check that CSS, JS, fonts load correctly
- [ ] Verify no "unstyled" flash on page load

### US4: Data Persistence
- [ ] Create test data (budget, expense, category)
- [ ] Restart the application host
- [ ] Verify all data is still present

## Troubleshooting

### Add-on shows "502 Bad Gateway"
- The application host is unreachable. Check:
  - Is the application stack running? (`docker compose ps` on app host)
  - Is the IP address correct in add-on configuration?
  - Can you ping the app host from the HA host?

### Assets not loading (broken CSS/JS)
- Check the add-on logs for the ingress path detection
- Verify `sub_filter` is rewriting `/_next/` paths (check browser Network tab)
- Ensure the add-on was restarted after any configuration changes

### User not recognized (authentication errors)
- Check that the add-on's nginx maps `X-Remote-User-Name` → `X-Hass-User`
- Verify the backend's `X-Hass-User` header handling is working
- Check the add-on logs for header forwarding

### Pages load but API calls fail
- Check browser DevTools → Console for network errors
- Verify the ingress path is being detected by the frontend middleware
- Check that `/api/*` routes are being proxied to the backend (port 8080)
