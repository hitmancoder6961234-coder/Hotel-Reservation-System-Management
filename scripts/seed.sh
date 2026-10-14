#!/usr/bin/env bash
# Re-apply the room seed (idempotent upsert of the 3 room types) on a running stack:
#   bash scripts/seed.sh
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose exec -T db sh -c 'exec mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < database/init/02-seed.sql
echo "Rooms seeded."
