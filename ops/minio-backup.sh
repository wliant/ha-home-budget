#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="${1:-$SCRIPT_DIR/backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/minio_$TIMESTAMP.tar.gz"

mkdir -p "$BACKUP_DIR"

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
echo "Backing up MinIO to: $BACKUP_FILE"

# Stop minio for consistent backup
echo "Stopping MinIO..."
docker compose -f "$COMPOSE_FILE" stop minio

docker run --rm \
    -v "$VOLUME_NAME":/data \
    -v "$BACKUP_DIR":/backup \
    alpine tar czf "/backup/$(basename "$BACKUP_FILE")" -C /data .

# Restart minio
echo "Starting MinIO..."
docker compose -f "$COMPOSE_FILE" start minio

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "Backup complete: $BACKUP_FILE ($SIZE)"
