#!/usr/bin/env bash
# ============================================================================
#  Update the running stack to the latest code:   bash scripts/deploy.sh
#  - never removes volumes (the database is untouched)
#  - takes a database backup first, rebuilds, restarts, waits for health
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "ERROR: .env is missing (copy .env.example and fill it in)"; exit 1; }

echo "==> Backup before update"
bash scripts/backup.sh || echo "WARNING: backup failed (first deployment?) - continuing"

if [ -d .git ]; then
  echo "==> git pull (fast-forward only)"
  git pull --ff-only
fi

echo "==> Build and start"
docker compose up -d --build --remove-orphans

echo "==> Waiting for the services to become healthy"
for i in $(seq 1 40); do
  unhealthy=$(docker compose ps --format '{{.Name}} {{.Health}}' | grep -v -E ' healthy$' || true)
  if [ -z "$unhealthy" ]; then echo "All services healthy."; break; fi
  if [ "$i" -eq 40 ]; then echo "Not healthy after 200 s:"; echo "$unhealthy"; docker compose logs --tail=50; exit 1; fi
  sleep 5
done

docker compose ps
docker image prune -f >/dev/null
echo "Deployed. (Never use 'docker compose down -v' in production: it deletes the database volume.)"
