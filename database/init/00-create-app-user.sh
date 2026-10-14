#!/bin/bash
# ============================================================================
#  Runs once, when the MySQL data volume is created (official mysql image
#  executes /docker-entrypoint-initdb.d/*.sh before the *.sql files).
#
#  Creates the dedicated application user with the minimum privileges the
#  backend needs (DML only: no DROP / ALTER / CREATE, and no root usage).
#  Credentials come from the container environment (compose .env), never
#  from this file.
# ============================================================================
set -e

: "${APP_DB_USER:?APP_DB_USER must be set}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD must be set}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE must be set}"

# During first-start initialisation the temporary server listens on this socket only, and root
# already has MYSQL_ROOT_PASSWORD. (Works whether the entrypoint runs or sources this file.)
mysql --protocol=socket --socket=/var/run/mysqld/mysqld.sock -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
  CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';
  GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.* TO '${APP_DB_USER}'@'%';
  FLUSH PRIVILEGES;
EOSQL

echo "[stayease] application user '${APP_DB_USER}' created with SELECT/INSERT/UPDATE/DELETE on ${MYSQL_DATABASE}"
