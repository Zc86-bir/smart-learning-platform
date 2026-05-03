#!/bin/bash
# Smart Learn Platform - Database Backup Script
# Usage: ./backup.sh [retention_days]
# Runs via cron: 0 2 * * * /opt/smart-learn/scripts/backup.sh 7

set -euo pipefail

BACKUP_DIR="/opt/smart-learn/backups"
RETENTION_DAYS=${1:-7}
DB_NAME="smart_learn"
DB_HOST="mysql"
DB_PORT="3306"
DB_USER="root"
DB_PASS="${DB_PASSWORD:-tBYdA6c@S4T9RmW#^ZKR}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

# Ensure backup directory exists
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting database backup: ${DB_NAME} -> ${BACKUP_FILE}"

# Perform backup with compression
mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --set-gtid-purged=OFF \
    "$DB_NAME" | gzip > "$BACKUP_FILE"

BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date)] Backup complete: ${BACKUP_FILE} (${BACKUP_SIZE})"

# Cleanup old backups
echo "[$(date)] Removing backups older than ${RETENTION_DAYS} days"
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
REMAINING=$(ls -1 "${BACKUP_DIR}/${DB_NAME}_"*.sql.gz 2>/dev/null | wc -l)
echo "[$(date)] Remaining backups: ${REMAINING}"

# Optional: push to remote storage (uncomment when configured)
# aws s3 cp "$BACKUP_FILE" s3://your-backup-bucket/
# rclone copy "$BACKUP_DIR" remote:backups
