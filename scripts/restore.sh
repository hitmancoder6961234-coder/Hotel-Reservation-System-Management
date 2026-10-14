#!/usr/bin/env bash
# ============================================================================
#  Restore a backup.   bash scripts/restore.sh <backup.sql.gz> [--into-production]
#
#  SAFE BY DEFAULT: restores into a separate scratch database `stayease_restore_test`
#  so you can inspect it (counts, sample rows) without touching live data.
#  --into-production overwrites the live database; it asks you to type the database name
#  and makes a safety backup first.
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."
FILE="${1:?usage: restore.sh <backup.sql.gz> [--into-production]}"
MODE="${2:-test}"
[ -f "$FILE" ] || { echo "No such file: $FILE"; exit 1; }

MYSQL='mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4'

if [ "$MODE" != "--into-production" ]; then
  TARGET=stayease_restore_test
  echo "==> Restoring into scratch database '$TARGET' (production untouched)"
  docker compose exec -T db sh -c "$MYSQL -e 'DROP DATABASE IF EXISTS $TARGET; CREATE DATABASE $TARGET CHARACTER SET utf8mb4'"
  gzip -dc "$FILE" | docker compose exec -T db sh -c "$MYSQL $TARGET"
  docker compose exec -T db sh -c "$MYSQL $TARGET -e 'SELECT COUNT(*) AS rooms FROM Room; SELECT COUNT(*) AS bookings, COALESCE(SUM(totalPrice),0) AS total FROM Booking;'"
  echo "Verify the numbers above, then drop the scratch DB with:"
  echo "  docker compose exec db sh -c 'mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -e \"DROP DATABASE $TARGET\"'"
  exit 0
fi

DB=$(docker compose exec -T db sh -c 'printf %s "$MYSQL_DATABASE"')
echo "!!! This REPLACES the live database '$DB' with the contents of $FILE."
read -r -p "Type the database name ($DB) to continue: " ANSWER
[ "$ANSWER" = "$DB" ] || { echo "Aborted."; exit 1; }
echo "==> Safety backup of the current data first"
bash scripts/backup.sh backups/pre-restore
docker compose stop backend
docker compose exec -T db sh -c "$MYSQL -e 'DROP DATABASE \`$DB\`; CREATE DATABASE \`$DB\` CHARACTER SET utf8mb4'"
gzip -dc "$FILE" | docker compose exec -T db sh -c "$MYSQL $DB"
# grants of the application user live in the `mysql` schema and are not affected by dropping the database,
# but re-apply them in case the database was re-created:
docker compose exec -T db sh -c "$MYSQL -e \"GRANT SELECT, INSERT, UPDATE, DELETE ON \\\`$DB\\\`.* TO '\$APP_DB_USER'@'%'\""
docker compose start backend
echo "Restored. Check the site and the backend logs:  docker compose logs --tail=30 backend"
