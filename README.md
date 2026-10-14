# StayEase — Hotel Reservation System Management

A hotel booking website: browse rooms, check live availability for chosen dates, book with automatic price
calculation and get an instant confirmation with a booking reference (`STY-2026-001`).

This project is the **technology migration** of the original Next.js / Prisma / SQLite StayEase site
(`Hotel-Reservation-main`). **The website design is unchanged** — same components, same CSS, same images, same
texts, same routes — only the technology underneath was replaced.

| Layer     | Technology                                                                      |
|-----------|---------------------------------------------------------------------------------|
| Frontend  | HTML5 + CSS3 (Tailwind CSS, as in the original) + JavaScript + **React 19** (Vite) |
| Backend   | **Java 21 Servlets** (Jakarta Servlet 6) on **Apache Tomcat 10.1**, REST + JSON  |
| Data      | **MySQL 8.4** through **JDBC** (`PreparedStatement`, HikariCP pool, transactions) |
| Edge      | **Nginx** (static files, `/api` reverse proxy, TLS) behind **Cloudflare**         |
| Runtime   | **Docker** + **Docker Compose** on **Ubuntu Linux**                              |
| Tooling   | Git, GitHub (Actions → GHCR), JDK 21, Maven                                      |

```
 visitor ──HTTPS──> Cloudflare ──HTTPS (Full strict)──> nginx :443 ─┬─ /      React build (static)
                                                                    └─ /api/  ──> Tomcat :8080 ──JDBC──> MySQL :3306
                                       only nginx is published ─────┘            (private Docker networks)
```

## Repository layout

```
frontend/   React app (src/, public/ images, Dockerfile, tests)
backend/    Maven project: Servlets, services, JDBC repositories, tests, Dockerfile
database/   init/ (app user, schema.sql, seed.sql)  migration/ (SQLite → MySQL)
nginx/      nginx.conf (production TLS), nginx.local.conf (plain HTTP), Cloudflare real-IP, headers
scripts/    ubuntu-setup.sh  deploy.sh  backup.sh  restore.sh  seed.sh
docs/       DEPLOYMENT.md  MIGRATION.md
compose.yaml  compose.local.yaml  .env.example
.github/workflows/ci-cd.yml
```

## Features (all of the original site's features)

Home (hero, search, featured rooms, facilities, about, reviews, contact) · Rooms list with guest-capacity filter ·
Room details · Booking form with live nights/total and debounced availability check · Booking confirmation with
copyable booking ID · responsive layout, toasts, loading/empty/error states.
Server side: validation, no double-booking (overlap rule), server-calculated nights/total, booking references.
Not part of the original and therefore **not built**: login, admin panel, payments, e-mail.

## Run it locally

**With Docker** (needs Docker Desktop / Engine with Compose v2.24+):

```bash
cp .env.example .env            # then edit the two passwords
docker compose -f compose.yaml -f compose.local.yaml up -d --build
# open http://localhost:8080
docker compose logs -f
```

**Without Docker** (JDK 21, Maven, Node 22 and a MySQL 8 are needed):

```bash
# 1. database
mysql -uroot -p -e "CREATE DATABASE stayease CHARACTER SET utf8mb4"
mysql -uroot -p stayease < database/init/01-schema.sql
mysql -uroot -p stayease < database/init/02-seed.sql
mysql -uroot -p -e "CREATE USER 'stayease_app'@'%' IDENTIFIED BY 'a-password';
                    GRANT SELECT,INSERT,UPDATE,DELETE ON stayease.* TO 'stayease_app'@'%';"
# 2. backend  (Tomcat 10.1: drop target/ROOT.war into its webapps/, or use any Servlet 6 container)
cd backend && mvn package
export DB_HOST=localhost DB_PORT=3306 DB_NAME=stayease DB_USER=stayease_app DB_PASSWORD=a-password
# 3. frontend (proxies /api to http://localhost:8080)
cd ../frontend && npm ci && npm run dev        # http://localhost:3000
```

## Tests

```bash
cd frontend && npm test                          # 24 tests (Vitest + Testing Library)
cd backend  && mvn test                          # 31 unit/servlet tests
# + 8 MySQL integration tests when pointed at a throw-away database:
TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/stayease?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
TEST_DB_USER=stayease_app TEST_DB_PASSWORD=... mvn test
```

The integration tests include **12 concurrent requests for the same room/dates (exactly one wins)** and
**12 concurrent bookings of different rooms (all references unique)**.
CI (`.github/workflows/ci-cd.yml`) runs everything against a real MySQL service container.
Results actually obtained so far are listed in [docs/MIGRATION.md](docs/MIGRATION.md#7-verification-status).

## API (unchanged paths and JSON)

| Method | Path | Result |
|--------|------|--------|
| GET  | `/api` | `{"message":"Hello, world!"}` health ping |
| GET  | `/api/rooms` | `{"rooms":[…]}` |
| GET  | `/api/rooms/:id` | `{"room":{…}}` · 400 invalid id · 404 |
| GET  | `/api/rooms/:id/availability?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD` | `{available, conflicts[], bookedRanges[]}` |
| POST | `/api/bookings` | `201 {"booking":{…,"room":{…}}}` · 400 validation · 404 room · 409 not available |
| GET  | `/api/bookings/:ref` | `{"booking":{…}}` · 404 |

Errors are always `{"error":"message"}`. A database outage answers `503`; unexpected errors `500` with a generic
message (details only in the server log).

## Deployment, backups, security

* Ubuntu + Docker + Cloudflare step by step: **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**
* Backup / restore: `scripts/backup.sh`, `scripts/restore.sh` (restore is non-destructive by default)
* Secrets only in `.env` (git-ignored); MySQL and Tomcat are never published; the app uses a DML-only MySQL user;
  prepared statements everywhere; JSON errors without stack traces; API responses are `no-store`.

## Database persistence

MySQL data lives in the Docker **named volume `db_data`**; it survives `docker compose restart/up/build`.
**Never run `docker compose down -v` on the server** (deletes the volume). A volume is not a backup — use
`scripts/backup.sh` and copy the dumps off the server.
