#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.tar.gz>"
    echo ""
    echo "Available backups:"
    ls -lh "$SCRIPT_DIR/backups"/minio_*.tar.gz 2>/dev/null || echo "  (none found in $SCRIPT_DIR/backups/)"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Detect compose file
if [ -f "$PROJECT_DIR/docker-compose.prod.yml" ] && docker compose -f "$PROJECT_DIR/docker-compose.prod.yml" ps --status running 2>/dev/null | grep -q minio; then
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.prod.yml"
else
    COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"
fi

# Derive volume name from compose project
COMPOSE_PROJECT="$(basename "$PROJECT_DIR" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9_-]//g')"
VOLUME_NAME="${COMPOSE_PROJECT}_minio-data"

echo "Using volume: $VOLUME_NAME"
echo "Restoring MinIO from: $BACKUP_FILE"
read -p "This will overwrite the MinIO data volume. Continue? [y/N] " confirm
if [[ "$confirm" != [yY] ]]; then
    echo "Aborted."
    exit 0
fi

# Stop minio before restore
echo "Stopping MinIO..."
docker compose -f "$COMPOSE_FILE" stop minio

# Clear existing data and restore
docker run --rm \
    -v "$VOLUME_NAME":/data \
    -v "$(cd "$(dirname "$BACKUP_FILE")" && pwd)":/backup \
    alpine sh -c "rm -rf /data/* /data/.minio.sys 2>/dev/null; tar xzf /backup/$(basename "$BACKUP_FILE") -C /data"

# Restart minio
echo "Starting MinIO..."
docker compose -f "$COMPOSE_FILE" start minio

echo "Restore complete."
