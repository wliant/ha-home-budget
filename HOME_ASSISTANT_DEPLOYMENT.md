# Home Assistant Deployment Guide

Complete guide to deploy the Home Budget Tracker as a Home Assistant add-on with authentication integration.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Deployment Architecture](#deployment-architecture)
4. [Installation Methods](#installation-methods)
5. [Configuration](#configuration)
6. [Post-Installation](#post-installation)
7. [Troubleshooting](#troubleshooting)

---

## Overview

This application integrates with Home Assistant to provide household budget tracking with automatic user authentication via the `X-Hass-User` HTTP header. When deployed as a Home Assistant add-on, it leverages the Home Assistant Ingress feature to provide seamless authentication without requiring separate login credentials.

### Key Features

- Automatic authentication using Home Assistant users
- Multi-user household support
- Private home network deployment
- Integrated with Home Assistant dashboard
- Docker-based containerized deployment

---

## Prerequisites

### Home Assistant Requirements

- Home Assistant OS, Supervised, or Container installation
- Version 2023.1 or newer
- Supervisor access (for add-on installation)
- At least 2GB free RAM
- 5GB free disk space

### Network Requirements

- Home Assistant accessible on your local network
- Ports available: 8080 (backend), 3000 (frontend), 3306 (MySQL)
- Or use Home Assistant Ingress (recommended - no port exposure needed)

### Optional

- Git (for repository-based installation)
- SSH access to Home Assistant host (for advanced configuration)

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────┐
│             Home Assistant Frontend                  │
│  (User accesses via: http://homeassistant:8123)     │
└───────────────────┬─────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────┐
│          Home Assistant Ingress Proxy               │
│    (Automatically adds X-Hass-User header)          │
└───────────────────┬─────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────┐
│              Budget Tracker Add-on                   │
│                                                      │
│  ┌──────────────────┐      ┌───────────────────┐   │
│  │  Next.js Frontend│◄────►│  Spring Boot      │   │
│  │  (Port 3000)     │      │  Backend (8080)   │   │
│  └──────────────────┘      └─────────┬─────────┘   │
│                                       │              │
│                            ┌──────────▼─────────┐   │
│                            │   MySQL 8.0        │   │
│                            │   (Port 3306)      │   │
│                            └────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## Installation Methods

### Method 1: Home Assistant Add-on (Recommended)

This method packages the application as a proper Home Assistant add-on with Ingress support.

#### Step 1: Create Add-on Structure

Create the following directory structure in your Home Assistant add-ons directory:

```
/addons/budget-tracker/
├── config.yaml
├── Dockerfile
├── run.sh
├── docker-compose.yml
├── budget-backend/
├── budget-frontend/
└── README.md
```

#### Step 2: Create `config.yaml`

```yaml
name: Home Budget Tracker
version: "1.0.0"
slug: budget-tracker
description: Household budget and expense tracking with category management
arch:
  - aarch64
  - amd64
  - armhf
  - armv7
  - i386
startup: application
boot: auto
ports:
  8080/tcp: 8080
  3000/tcp: 3000
ports_description:
  8080/tcp: Backend API
  3000/tcp: Frontend UI
ingress: true
ingress_port: 3000
panel_icon: mdi:calculator
panel_title: Budget Tracker
map:
  - config:rw
  - share:rw
  - ssl:ro
options:
  mysql_root_password: "homeassistant"
  mysql_database: "homebudget"
  mysql_user: "budget_user"
  mysql_password: "change_me_in_production"
  backend_port: 8080
  frontend_port: 3000
  spring_profiles_active: "prod"
  log_level: "info"
schema:
  mysql_root_password: password
  mysql_database: str
  mysql_user: str
  mysql_password: password
  backend_port: port
  frontend_port: port
  spring_profiles_active: list(dev|prod)
  log_level: list(debug|info|warn|error)
```

#### Step 3: Create `Dockerfile`

```dockerfile
ARG BUILD_FROM
FROM $BUILD_FROM

# Install dependencies
RUN apk add --no-cache \
    docker \
    docker-compose \
    openjdk17 \
    nodejs \
    npm \
    mysql-client \
    bash

# Copy application files
COPY budget-backend /app/budget-backend
COPY budget-frontend /app/budget-frontend
COPY docker-compose.yml /app/
COPY run.sh /app/

WORKDIR /app

# Make run script executable
RUN chmod +x /app/run.sh

# Expose ports
EXPOSE 3000 8080

# Run the application
CMD ["/app/run.sh"]
```

#### Step 4: Create `run.sh`

```bash
#!/usr/bin/with-contenv bashio

# Read configuration from Home Assistant
export MYSQL_ROOT_PASSWORD=$(bashio::config 'mysql_root_password')
export MYSQL_DATABASE=$(bashio::config 'mysql_database')
export MYSQL_USER=$(bashio::config 'mysql_user')
export MYSQL_PASSWORD=$(bashio::config 'mysql_password')
export BACKEND_PORT=$(bashio::config 'backend_port')
export FRONTEND_PORT=$(bashio::config 'frontend_port')
export SPRING_PROFILES_ACTIVE=$(bashio::config 'spring_profiles_active')

# Set production environment
export NODE_ENV=production
export NEXT_PUBLIC_API_URL=http://localhost:${BACKEND_PORT}

bashio::log.info "Starting Home Budget Tracker..."
bashio::log.info "MySQL Database: ${MYSQL_DATABASE}"
bashio::log.info "Backend Port: ${BACKEND_PORT}"
bashio::log.info "Frontend Port: ${FRONTEND_PORT}"
bashio::log.info "Spring Profile: ${SPRING_PROFILES_ACTIVE}"

# Start services using docker-compose
cd /app
docker-compose up
```

#### Step 5: Copy Application Files

Copy the entire `budget-backend` and `budget-frontend` directories plus `docker-compose.yml` to `/addons/budget-tracker/`.

#### Step 6: Install the Add-on

1. Navigate to Home Assistant: **Settings → Add-ons → Add-on Store**
2. Click the three dots menu (⋮) → **Repositories**
3. Add your repository URL or use **Local add-ons** if files are in `/addons/`
4. Find "Home Budget Tracker" and click **Install**
5. Configure the add-on (see Configuration section)
6. Click **Start**
7. Enable **Start on boot** and **Watchdog**

---

### Method 2: Docker Compose with nginx Proxy

For manual deployment alongside Home Assistant without using the add-on system.

#### Step 1: Create nginx Configuration

Create `/config/nginx/budget-tracker.conf`:

```nginx
server {
    listen 8099;
    server_name _;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Add Home Assistant user header
        # This should be set by Home Assistant Ingress
        # For manual setup, you need to authenticate first
        proxy_set_header X-Hass-User $http_x_hass_user;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Hass-User $http_x_hass_user;
    }
}
```

#### Step 2: Update docker-compose.yml for Production

Create `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: homebudget-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - /config/budget-tracker/mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p$$MYSQL_ROOT_PASSWORD"]
      interval: 5s
      timeout: 3s
      retries: 10
    networks:
      - homebudget-network
    restart: unless-stopped

  backend:
    build:
      context: ./budget-backend
      dockerfile: Dockerfile.prod
    container_name: homebudget-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - homebudget-network
    restart: unless-stopped

  frontend:
    build:
      context: ./budget-frontend
      dockerfile: Dockerfile.prod
    container_name: homebudget-frontend
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080
      NODE_ENV: production
    ports:
      - "3000:3000"
    depends_on:
      - backend
    networks:
      - homebudget-network
    restart: unless-stopped

networks:
  homebudget-network:
    driver: bridge
```

#### Step 3: Create Production Dockerfiles

**budget-backend/Dockerfile.prod**:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**budget-frontend/Dockerfile.prod**:

```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine
WORKDIR /app
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package*.json ./
EXPOSE 3000
CMD ["npm", "start"]
```

#### Step 4: Deploy

```bash
# Create directory for persistent data
mkdir -p /config/budget-tracker

# Create .env file
cat > /config/budget-tracker/.env <<EOF
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_DATABASE=homebudget
MYSQL_USER=budget_user
MYSQL_PASSWORD=your_secure_password
EOF

# Start services
docker-compose -f docker-compose.prod.yml up -d
```

---

## Configuration

### Environment Variables

Create `.env` file or configure in Home Assistant add-on settings:

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_ROOT_PASSWORD` | - | MySQL root password (required) |
| `MYSQL_DATABASE` | `homebudget` | Database name |
| `MYSQL_USER` | `budget_user` | Database username |
| `MYSQL_PASSWORD` | - | Database password (required) |
| `BACKEND_PORT` | `8080` | Backend API port |
| `FRONTEND_PORT` | `3000` | Frontend UI port |
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile (`dev` or `prod`) |
| `NODE_ENV` | `production` | Node environment |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Backend API URL for frontend |

### Spring Boot Configuration (Production)

The backend automatically handles authentication based on the `SPRING_PROFILES_ACTIVE` setting:

- **`prod` profile**: Requires `X-Hass-User` header (fails if missing)
- **`dev` profile**: Uses default user `dev-user` if header is missing

For production deployment with Home Assistant, always use `prod` profile to enforce authentication.

### Home Assistant Ingress Integration

When using Ingress (recommended), Home Assistant automatically:

1. Authenticates the user
2. Adds the `X-Hass-User` header with the logged-in username
3. Proxies requests to your add-on
4. Handles SSL/TLS termination

No manual nginx configuration needed!

---

## Post-Installation

### 1. Verify Services are Running

```bash
# Check container status
docker ps | grep homebudget

# Check logs
docker logs homebudget-backend
docker logs homebudget-frontend
docker logs homebudget-mysql
```

### 2. Test Database Connection

```bash
# Connect to MySQL
docker exec -it homebudget-mysql mysql -u budget_user -p homebudget

# Verify tables were created by Liquibase
mysql> SHOW TABLES;
```

### 3. Access the Application

**Via Home Assistant Ingress** (Recommended):
- Navigate to **Settings → Add-ons → Budget Tracker → Open Web UI**
- Or access from the sidebar if **Show in sidebar** is enabled

**Direct Access**:
- Frontend: `http://homeassistant.local:3000`
- Backend API: `http://homeassistant.local:8080/api/`
- Health Check: `http://homeassistant.local:8080/actuator/health`

### 4. Create Your First Budget

1. Log into Home Assistant
2. Open Budget Tracker
3. You'll be automatically authenticated as your Home Assistant user
4. Navigate to Budgets → Create New Budget
5. Enter month, amount, and category
6. Start tracking expenses!

---

## Troubleshooting

### Issue: Authentication Errors (X-Hass-User missing)

**Symptoms**: Backend returns 401/403 errors

**Solutions**:

1. **Check Spring Profile**:
   ```bash
   docker exec homebudget-backend env | grep SPRING_PROFILES_ACTIVE
   ```
   Should be `prod` for production deployment

2. **Verify Ingress is Enabled**:
   - Home Assistant Add-on settings → Configuration
   - Ensure `ingress: true` in `config.yaml`

3. **Check nginx Configuration** (if not using Ingress):
   - Verify `proxy_set_header X-Hass-User` is set
   - Check nginx logs: `docker logs <nginx-container>`

4. **Development Mode Workaround**:
   ```bash
   # Temporarily use dev profile (NOT for production!)
   SPRING_PROFILES_ACTIVE=dev docker-compose restart backend
   ```

### Issue: Database Connection Failures

**Symptoms**: Backend crashes on startup, "Unable to connect to MySQL"

**Solutions**:

1. **Wait for MySQL Initialization**:
   ```bash
   docker logs homebudget-mysql | grep "ready for connections"
   # Wait 15-30 seconds, then restart backend
   docker restart homebudget-backend
   ```

2. **Check Database Credentials**:
   ```bash
   docker exec homebudget-mysql mysql -u budget_user -p
   # Enter password from .env
   ```

3. **Verify Network Connectivity**:
   ```bash
   docker exec homebudget-backend ping -c 3 mysql
   ```

### Issue: Frontend Can't Connect to Backend

**Symptoms**: Frontend loads but API calls fail

**Solutions**:

1. **Check Backend is Running**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Verify API URL Configuration**:
   ```bash
   docker exec homebudget-frontend env | grep NEXT_PUBLIC_API_URL
   ```
   Should match your backend URL

3. **Check CORS Configuration**:
   Backend should allow requests from frontend origin (configured in `budget-backend/src/main/java/com/homebudget/config/CorsConfig.java`)

### Issue: Port Conflicts

**Symptoms**: "Address already in use" errors

**Solutions**:

1. **Find Conflicting Process**:
   ```bash
   sudo lsof -i :3000
   sudo lsof -i :8080
   sudo lsof -i :3306
   ```

2. **Change Ports in Configuration**:
   Edit `.env` or add-on configuration:
   ```bash
   BACKEND_PORT=8081
   FRONTEND_PORT=3001
   MYSQL_PORT=3307
   ```

3. **Restart Services**:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

### Issue: Data Loss After Restart

**Symptoms**: Budgets and expenses disappear after container restart

**Solutions**:

1. **Verify Volume Mounts**:
   ```bash
   docker volume ls | grep mysql-data
   docker volume inspect homebudget_mysql-data
   ```

2. **Check Volume Configuration** in `docker-compose.yml`:
   ```yaml
   volumes:
     mysql-data:
       driver: local
   ```

3. **Use Persistent Path** (for add-ons):
   ```yaml
   volumes:
     - /config/budget-tracker/mysql-data:/var/lib/mysql
   ```

### Issue: High Memory Usage

**Symptoms**: System slowdown, Out of Memory errors

**Solutions**:

1. **Increase JVM Heap Size** for backend:
   ```yaml
   environment:
     JAVA_OPTS: "-Xmx512m -Xms256m"
   ```

2. **Limit Container Resources**:
   ```yaml
   services:
     backend:
       deploy:
         resources:
           limits:
             memory: 1G
   ```

3. **Monitor Resource Usage**:
   ```bash
   docker stats
   ```

---

## Backup and Restore

### Backup Database

```bash
# Create backup
docker exec homebudget-mysql mysqldump -u root -p homebudget > backup-$(date +%Y%m%d).sql

# Or use automated backup script
cat > /config/budget-tracker/backup.sh <<'EOF'
#!/bin/bash
BACKUP_DIR="/config/budget-tracker/backups"
mkdir -p $BACKUP_DIR
docker exec homebudget-mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD homebudget > $BACKUP_DIR/backup-$(date +%Y%m%d-%H%M%S).sql
find $BACKUP_DIR -name "backup-*.sql" -mtime +30 -delete
EOF

chmod +x /config/budget-tracker/backup.sh

# Add to cron (Home Assistant)
# Settings → Automations → Create automation to run script daily
```

### Restore Database

```bash
# Restore from backup
docker exec -i homebudget-mysql mysql -u root -p homebudget < backup-20250101.sql
```

---

## Updating the Application

### Home Assistant Add-on Update

1. Navigate to **Settings → Add-ons → Budget Tracker**
2. Check for updates
3. Click **Update** button
4. Wait for rebuild and restart

### Manual Update

```bash
# Pull latest changes
cd /config/budget-tracker
git pull

# Rebuild and restart
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

---

## Security Considerations

1. **Change Default Passwords**: Always change MySQL passwords in production
2. **Use HTTPS**: Access via Home Assistant's HTTPS endpoint
3. **Network Isolation**: Keep on private home network only
4. **Regular Backups**: Automate database backups
5. **Update Regularly**: Keep Home Assistant and add-on updated
6. **Production Profile**: Always use `SPRING_PROFILES_ACTIVE=prod` in production

---

## Support and Resources

- **Project Documentation**: See `/specs/` directory in repository
- **Home Assistant Forums**: https://community.home-assistant.io/
- **Report Issues**: [GitHub Issues](https://github.com/your-org/ha-hello/issues)
- **Development Guide**: See `README.md` for local development setup

---

## License

[Your License Here]
