#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${1:-$SCRIPT_DIR/backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/mysql_$TIMESTAMP.sql.gz"

mkdir -p "$BACKUP_DIR"

# Detect compose file and project name
if [ -f "$PROJECT_DIR/docker-compose.prod.yml" ] && docker compose -f "$PROJECT_DIR/docker-compose.prod.yml" ps --status running 2>/dev/null | grep -q mysql; then
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"
else
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
fi

echo "Using compose file: $COMPOSE_FILE"
echo "Backing up MySQL to: $BACKUP_FILE"

# Read credentials from .env or use defaults
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
fi
DB_NAME="${MYSQL_DATABASE:-homebudget}"
DB_USER="${MYSQL_USER:-budget_user}"
DB_PASS="${MYSQL_PASSWORD:-budget_password}"

docker compose -f "$COMPOSE_FILE" exec -T mysql \
    mysqldump -u"$DB_USER" -p"$DB_PASS" --single-transaction --routines --triggers "$DB_NAME" \
    | gzip > "$BACKUP_FILE"

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup complete: $BACKUP_FILE ($SIZE)"
