#!/usr/bin/env bash
# ============================================================================
#  Database backup:   bash scripts/backup.sh [output-dir]
#  Writes a gzip-compressed, consistent (single-transaction) mysqldump to ./backups/.
#  Keep copies OFF this server too (a Docker volume is not a backup).
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-backups}"
mkdir -p "$OUT"
chmod 700 "$OUT"
FILE="$OUT/stayease-$(date +%Y%m%d-%H%M%S).sql.gz"

# The root password is read INSIDE the container from its own environment: never on the command line.
docker compose exec -T db sh -c 'exec mysqldump --single-transaction --routines --triggers --no-tablespaces \
  --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' | gzip > "$FILE"

# a valid dump of this schema always contains the Booking table definition
if ! gzip -dc "$FILE" | grep -q 'CREATE TABLE `Booking`'; then
  rm -f "$FILE"
  echo "ERROR: backup looks invalid, removed"
  exit 1
fi
chmod 600 "$FILE"
echo "Backup written: $FILE ($(du -h "$FILE" | cut -f1))"

# keep the 14 most recent local backups
ls -1t "$OUT"/stayease-*.sql.gz 2>/dev/null | tail -n +15 | xargs -r rm -f
