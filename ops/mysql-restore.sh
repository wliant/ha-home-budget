#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.sql.gz>"
    echo ""
    echo "Available backups:"
    ls -lh "$SCRIPT_DIR/backups"/mysql_*.sql.gz 2>/dev/null || echo "  (none found in $SCRIPT_DIR/backups/)"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Detect compose file
if [ -f "$PROJECT_DIR/docker-compose.prod.yml" ] && docker compose -f "$PROJECT_DIR/docker-compose.prod.yml" ps --status running 2>/dev/null | grep -q mysql; then
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"
else
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
fi

# Read credentials from .env or use defaults
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
fi
DB_NAME="${MYSQL_DATABASE:-homebudget}"
DB_USER="${MYSQL_USER:-budget_user}"
DB_PASS="${MYSQL_PASSWORD:-budget_password}"

echo "Using compose file: $COMPOSE_FILE"
echo "Restoring MySQL from: $BACKUP_FILE"
read -p "This will overwrite the '$DB_NAME' database. Continue? [y/N] " confirm
if [[ "$confirm" != [yY] ]]; then
    echo "Aborted."
    exit 0
fi

gunzip -c "$BACKUP_FILE" \
    | docker compose -f "$COMPOSE_FILE" exec -T mysql \
        mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME"

echo "Restore complete."
