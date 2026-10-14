# Migration report: Next.js / Prisma / SQLite → React / Java Servlets / JDBC / MySQL

Source of truth for the migration: the original project `Hotel-Reservation-main` (untouched, still on the Desktop;
it contained no `.git` history, so the "original branch" is simply that unmodified folder).
This repository contains the migrated application.

## 1. Migration inventory

| Item | Original (verified in the source) | Now | Status |
|------|------------------------------------|-----|--------|
| Frontend framework | Next.js 16.1.3 App Router, React 19.2.3, TypeScript | React 19.2.3 + Vite 7, JavaScript (JSX) | done, built, tested |
| Styling | Tailwind CSS 4.1.18 (`globals.css`, oklch palette), shadcn/ui Button + Toast, lucide-react 0.525 | **same CSS file, same Tailwind version, same libraries** | done |
| Fonts | `next/font` Inter + Playfair Display | `@fontsource-variable/inter` + `playfair-display` (same families, self-hosted) | done |
| Routing | one page `/`, hash routes (`#/`, `#/rooms`, `#/rooms/:id`, `#/booking?…`, `#/confirmation/:ref`) | identical hash router (same file) | done |
| Backend | Next.js route handlers (5 endpoints) | Java 21 Servlets on Tomcat 10.1 | done, tested |
| ORM / data access | Prisma | JDBC + `PreparedStatement` + HikariCP | done, tested |
| Database | SQLite file (`/app/data/custom.db`) | MySQL 8.4 (Docker volume) | done, tested (local MySQL 8.4.11) |
| Web server / proxy | Traefik (external) + Let's Encrypt | Nginx + Cloudflare Origin Certificate (Full strict) | config validated with real nginx; not yet on a server |
| Runtime | Bun in one container | 3 containers: web (nginx), backend (Tomcat), db (MySQL) | Dockerfiles/compose written, **not built** (no Docker available here) |
| CI/CD | GitHub Actions → GHCR | GitHub Actions: tests (incl. MySQL) → GHCR → optional SSH deploy | written, not run |
| Domain | `hotel-reservation.opscloud.eu.cc` | unchanged (`server_name` in nginx.conf) | DNS/Cloudflare not touched |

## 2. Pages (all five migrated, none removed or added)

| Route | Component | API used |
|-------|-----------|----------|
| `#/` | `home-view` (hero, search, featured rooms, facilities, about, reviews, contact) | `GET /api/rooms` |
| `#/rooms[?checkIn&checkOut&guests]` | `rooms-view` (+ capacity filter, search banner) | `GET /api/rooms` |
| `#/rooms/:id` | `room-details-view` (+ "Room not found") | `GET /api/rooms` |
| `#/booking?room=…` | `booking-view` (live total, debounced availability) | `GET /api/rooms/:id/availability`, `POST /api/bookings` |
| `#/confirmation/:ref` | `confirmation-view` | `GET /api/bookings/:ref` |

Navbar, footer, toasts, skeleton/error/empty states and all texts are the original components.

## 3. Design preservation — what was done and how it was checked

* The 17 original TS/TSX source files were converted to JS/JSX **mechanically** (TypeScript compiler strips the type
  annotations; Prettier restores formatting). JSX, class names, texts and logic are untouched.
  `globals.css`, the 5 photographs, `logo.svg`, `robots.txt` were copied byte-for-byte.
* **CSS parity check (run):** the original sources and the migrated sources were compiled with the same Tailwind
  (4.1.18) and compared rule by rule: all utility classes are identical; the only differences are `antialiased`
  (moved from `layout.tsx` to `<body class>` in `index.html`) and `ease-in-out` (picked up from the logo SVG), and Vite's
  minifier writes colours as hex with the original `oklch()` kept as the modern value.
* **Rendering check (run):** the migrated site was opened in headless Edge through the real stack
  (nginx → Tomcat → MySQL) at 1280 px and 390 px: home, rooms, details, booking (with live availability from the
  database), confirmation. The pages show the same warm bronze/gold design, rounded corners, gradient buttons and images.
* **Not done:** a pixel-by-pixel screenshot diff against the *running original*, because the original needs Bun,
  Prisma engines and its own build. Because the JSX and CSS are identical this risk is low, but it is **not verified**.
* Intentional tiny differences: favicon is the local `/logo.svg` (the original linked the same logo from an external CDN);
  `<title>`/meta/Open-Graph tags are copied from `layout.tsx`.

## 4. API migration map (paths and JSON preserved)

| Original route | New servlet | Same behaviour |
|----------------|-------------|----------------|
| `GET /api` | `ApiRootServlet` | `{"message":"Hello, world!"}` |
| `GET /api/rooms` | `RoomsServlet` | `{"rooms":[{id,name,description,price,capacity,facilities[],features[],image,available}]}` ordered by id |
| `GET /api/rooms/:id` | `RoomsServlet` | `{"room":…}`; 400 `Invalid room id.`; 404 `Room not found.` |
| `GET /api/rooms/:id/availability` | `RoomsServlet` | validation order, `available:false + reason`, `conflicts`, `bookedRanges`, overlap rule `existing.in < req.out && existing.out > req.in` |
| `POST /api/bookings` | `BookingsServlet` + `BookingService` | identical field order of checks and messages, `201 {"booking":{…,"room":{…}}}`, 409 on overlap |
| `GET /api/bookings/:ref` | `BookingsServlet` | flattened booking incl. `roomName`, `roomImage`, `createdAt` |

Differences (all deliberate, documented):

1. **Unsupported methods / unknown `/api/*` paths** return JSON `405` / `404` instead of Next.js defaults.
2. **Database down** → `503 {"error":"Service temporarily unavailable. Please try again."}` (original: 500).
3. **Booking creation is serialised** by locking the room rows in id order inside one transaction. Together with
   the overlap check this is race-free; the original (count-then-insert in SQLite) could race.
4. **Booking reference** = highest existing sequence + 1 (original: count + 1). Identical while no booking is deleted.
5. New limits: name ≤ 100 characters (`Name must be at most 100 characters.`), e-mail ≤ 254, body ≤ 16 KB, total price must fit an `INT`.
6. `createdAt` is serialised as ISO-8601 UTC (`…Z`), dates as `YYYY-MM-DD` — same as before.
7. Today's date is evaluated in `APP_TIMEZONE` (default `Asia/Kolkata`, as `TZ` in the original compose file).

## 5. Database and data migration

Schema mapping (`database/init/01-schema.sql`): table/column names are unchanged (`Room`, `Booking`, `bookingRef`, …);
`checkIn/checkOut` text → `DATE`; `createdAt` → `DATETIME(3)` in UTC; booleans → `TINYINT(1)`;
primary key, unique `bookingRef` and the `Booking.roomId → Room.id` foreign key (RESTRICT/CASCADE) are preserved.
Added safety nets: `CHECK (checkOut > checkIn)`, `CHECK (guests >= 1 AND nights >= 1)`, a composite index for the
overlap query. `facilities`/`features` stay comma-separated text (as in the original).

Seed rooms (`02-seed.sql`) are copied from `scripts/seed.ts`. **Difference:** the original re-seeded (overwrote) the 3 rooms
on *every* boot; now the seed runs only when the database volume is created (so later edits to rooms are not overwritten).
Re-apply manually with `bash scripts/seed.sh`.

Importing existing data of the old site (if the real `custom.db` has bookings):

```bash
cp /path/to/data/custom.db ~/custom.db.backup                         # 1. keep the original untouched
python3 database/migration/sqlite_to_mysql.py --counts ~/custom.db     # 2. note the row counts + total
python3 database/migration/sqlite_to_mysql.py ~/custom.db > migrate.sql # 3. generate upserts (read-only access to SQLite)
docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < migrate.sql   # 4. load (one transaction, re-runnable)
docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SELECT COUNT(*),SUM(totalPrice) FROM Booking"'  # 5. compare
```

The script refuses (stops with an error) rows it cannot convert instead of skipping them. **The repository contains no
production `custom.db`, so the real data migration has not been run** — only the script, against a synthetic Prisma-shaped
database (see §7). Keep the old database file until the counts match.

## 6. Docker services (written, not built here)

`db` (mysql:8.4, volume `db_data`, internal network, health check, init scripts), `backend` (Maven build → Tomcat 10.1 JDK 21,
non-root, no default webapps, health check `GET /api`), `web` (Node build → nginx; publishes 80/443; mounts the Cloudflare origin certificate).

## 7. Verification status

**Actually run (on Windows with portable JDK 21, Maven 3.9, Node 22, Tomcat 10.1.61, MySQL 8.4.11, nginx 1.28):**

| Check | Result |
|-------|--------|
| Frontend `npm test` | **24 / 24 passed** (5 files: validation & date/price helpers, hash router, RoomCard, BookingView flow, app shell incl. error/retry/filter/404) |
| Frontend `npm run build` | succeeded |
| Backend `mvn test` with MySQL | **39 / 39 passed** = 31 unit/servlet + 8 MySQL integration (incl. 12-thread same-room race → exactly 1 booking; 12-thread different rooms → 12 unique references; constraints; transaction rollback) |
| Backend `mvn package` | WAR built (6.8 MB) |
| WAR deployed on real Tomcat 10.1, API scenarios (curl) | **32 / 32 passed** directly; **31 / 32** through nginx (the 1 "failure" is a check that `/manager/html` yields JSON 404 — through nginx it correctly returns the React page; Tomcat has no manager app) |
| API via nginx over HTTPS (self-signed cert) and plain HTTP | 31 / 32 each, same as above |
| nginx production config (TLS, HTTP→HTTPS redirect, headers, asset caching, `/api` proxy, 64 KB body limit → 413) | validated with a real nginx 1.28 |
| MySQL outage: stop MySQL → API `503`; start MySQL → API `200` again without restarting Tomcat | verified |
| Schema + seed on MySQL 8.4 | applied without errors |
| SQLite → MySQL script on a synthetic Prisma DB (epoch-ms and ISO `createdAt`, apostrophes, unicode) | counts and `totalPrice` sum equal; re-run is idempotent |
| Shell scripts | `bash -n` syntax OK; YAML files parse |
| Rendering in headless Edge (desktop + mobile) | home/rooms/details/booking/confirmation render correctly |

**Not verified (no Docker, no Linux server, no Cloudflare/GitHub access in the build environment):**
Docker image builds, `docker compose up`, MySQL `docker-entrypoint-initdb.d` execution (`00-create-app-user.sh`),
container health checks, volume persistence across container restarts, `scripts/backup.sh`/`restore.sh`/`deploy.sh` on Linux,
the GitHub Actions workflow, Cloudflare DNS/SSL, deployment to the server, mobile layout beyond one headless screenshot,
and a pixel diff against the running original site.

## 8. Known limitations

* Features absent in the original stay absent: login, admin panel, booking cancellation/modification, payments, e-mail.
  (The room page text "Free cancellation up to 24 hours" is original marketing copy; no cancellation feature exists.)
* `facilities`/`features` are CSV text, as in the original; rooms can only be changed in the database.
* One MySQL instance, no replication; backups are manual/cron (`scripts/backup.sh`).
