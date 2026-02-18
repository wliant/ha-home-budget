# Ops Scripts

Backup and restore scripts for the MySQL database and MinIO object storage Docker volumes.

## Scripts

| Script | Description |
|---|---|
| `mysql-backup.sh` | Exports the MySQL database using `mysqldump` with `--single-transaction` (no downtime). Output is a gzipped SQL file. |
| `mysql-restore.sh` | Restores a `.sql.gz` backup into the running MySQL container. Prompts for confirmation before overwriting. |
| `minio-backup.sh` | Stops MinIO, creates a gzipped tar archive of the data volume, then restarts MinIO. Brief downtime required for consistency. |
| `minio-restore.sh` | Stops MinIO, clears the data volume, extracts a `.tar.gz` backup into it, then restarts MinIO. Prompts for confirmation before overwriting. |

All scripts auto-detect whether the dev (`docker-compose.yml`) or prod (`docker-compose.prod.yml`) environment is running.

## Setup

1. Docker and Docker Compose must be installed and accessible from your shell.
2. The target containers (mysql, minio) must be running before you execute the scripts.
3. Database credentials are read from the project `.env` file. If no `.env` exists, the defaults from `docker-compose.yml` are used (`budget_user` / `budget_password` / `homebudget`).
4. Scripts are already executable. If not, run:
   ```
   chmod +x ops/*.sh
   ```

## Usage

All backups are saved to `ops/backups/` by default. You can pass a custom directory as the first argument to the backup scripts.

### Back up MySQL

```
./ops/mysql-backup.sh
```

Custom output directory:

```
./ops/mysql-backup.sh /path/to/backups
```

### Restore MySQL

```
./ops/mysql-restore.sh ops/backups/mysql_20260217_120000.sql.gz
```

Run without arguments to list available backups:

```
./ops/mysql-restore.sh
```

### Back up MinIO

```
./ops/minio-backup.sh
```

Custom output directory:

```
./ops/minio-backup.sh /path/to/backups
```

### Restore MinIO

```
./ops/minio-restore.sh ops/backups/minio_20260217_120000.tar.gz
```

Run without arguments to list available backups:

```
./ops/minio-restore.sh
```
