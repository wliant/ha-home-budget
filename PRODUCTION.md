# Production Deployment (Docker Compose)

This guide describes how to run the production environment using Docker Compose.

## Prerequisites

- Docker and Docker Compose installed
- Ports 80 available on the host (or change `NGINX_PORT` in `.env.prod`)

## Step-by-step (from a fresh clone)

1. Clone the repository:

```bash
git clone <REPO_URL>
cd ha-hello
```

2. Create the production env file:

```bash
cp .env.prod.example .env.prod
```

3. Edit `.env.prod` and set secure passwords:

```bash
# Required
MYSQL_ROOT_PASSWORD=change_me_root
MYSQL_DATABASE=homebudget
MYSQL_USER=budget_user
MYSQL_PASSWORD=change_me_password

# Optional
NGINX_PORT=80
```

4. Build and start the production stack:

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

5. Verify containers are running:

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod ps
```

6. Open the app:

- Browser: http://localhost (or `http://localhost:<NGINX_PORT>` if customized)

## Notes

- The stack includes: MySQL, Spring Boot backend, Next.js frontend, and Nginx reverse proxy.
- Nginx routes `/api/*` to the backend and all other routes to the frontend.
- The backend runs with `SPRING_PROFILES_ACTIVE=prod`.
- The frontend uses:
  - `NEXT_PUBLIC_API_URL=/api` for browser requests
  - `INTERNAL_API_URL=http://backend:8080` for server-side rendering inside Docker

## Stop / Remove

```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod down
```

## Data persistence

- MySQL data is stored in the named volume `mysql-data`.
