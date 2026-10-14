# Deployment guide — Ubuntu + Docker Compose + Nginx + Cloudflare

> Status: **written and configuration-checked, but not yet executed on a real server** (no server access was available
> while building). Everything below that can be verified without a server was verified — see
> [MIGRATION.md §7](MIGRATION.md#7-verification-status).

## 0. What you need

* An Ubuntu 22.04/24.04 server with a public IP, a user with `sudo`, ports 22/80/443 reachable.
* The domain `hotel-reservation.opscloud.eu.cc` (the one the original site used) managed in **Cloudflare**.
  If you use another domain, change `server_name` in `nginx/nginx.conf`.
* Do **not** run this on a server where ports 80/443 are already used by another website without
  adapting the stack (see "Sharing a server" at the end).

## 1. Prepare the server (once)

```bash
ssh <user>@<server-ip>
sudo mkdir -p /opt/stayease && sudo chown "$USER": /opt/stayease
git clone https://github.com/hitmancoder6961234-coder/Hotel-Reservation-System-Management.git /opt/stayease
cd /opt/stayease
bash scripts/ubuntu-setup.sh         # Docker Engine + Compose plugin + UFW (22, 80, 443). Skip if Docker exists.
# log out / in once (docker group), then:
docker compose version
```

The repository is private: `git clone` asks for a GitHub username and a **personal access token** (read-only
"Contents" permission is enough), or use a read-only deploy key.

## 2. Secrets

```bash
cd /opt/stayease
cp .env.example .env && chmod 600 .env
# set strong passwords (letters + digits only):
sed -i "s/^MYSQL_ROOT_PASSWORD=.*/MYSQL_ROOT_PASSWORD=$(openssl rand -base64 30 | tr -dc A-Za-z0-9 | cut -c1-28)/" .env
sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$(openssl rand -base64 30 | tr -dc A-Za-z0-9 | cut -c1-28)/" .env
```

`.env` never leaves the server and is git-ignored.

## 3. Cloudflare

1. **DNS** → add an `A` record: name `hotel-reservation` (the host part of the domain), content = the server's public IP,
   proxy status **Proxied (orange cloud)**. Do not delete existing records you do not recognise.
2. **SSL/TLS → Overview**: set encryption mode **Full (strict)**.
3. **SSL/TLS → Origin Server → Create Certificate** (RSA, 15 years, hostnames `hotel-reservation.opscloud.eu.cc`).
   Copy the certificate to `/opt/stayease/nginx/certs/origin.pem` and the private key to
   `/opt/stayease/nginx/certs/origin.key`, then `chmod 600 nginx/certs/origin.key`.
   (The key is shown only once. These files are git-ignored.)
4. Optional hardening: *SSL/TLS → Edge Certificates → Always Use HTTPS*, and in UFW allow 80/443 only from
   [Cloudflare's IP ranges](https://www.cloudflare.com/ips/).
5. Cloudflare may cache static files; the API sends `Cache-Control: no-store`, so booking data is never cached.
   Do not create a Cache Rule that caches `/api/*`.

## 4. First start

```bash
cd /opt/stayease
docker compose up -d --build        # builds backend + web images, creates the database on first start
docker compose ps                   # db, backend, web must become "healthy" (1–3 minutes the first time)
docker compose logs --tail=50 backend
curl -s http://127.0.0.1/healthz              # ok
curl -sk https://127.0.0.1/api/rooms | head   # the 3 rooms (-k: the origin cert is for the public hostname)
```

Then open **https://hotel-reservation.opscloud.eu.cc**.

On the first start MySQL runs `database/init/*` automatically (application user with DML-only rights, schema,
3 seeded rooms). These scripts run **only when the data volume is empty**.

## 5. Updating

```bash
cd /opt/stayease && bash scripts/deploy.sh      # backup → git pull → docker compose up -d --build → wait for health
```

With the GitHub Actions pipeline you can instead let CI build/push images to GHCR and deploy by SSH
(set the secrets/variable listed at the top of `.github/workflows/ci-cd.yml`).
Roll back: `git checkout <previous-commit> && docker compose up -d --build`.

## 6. Backups

```bash
bash scripts/backup.sh                            # → backups/stayease-YYYYmmdd-HHMMSS.sql.gz (14 kept)
# cron, daily 03:15:
( crontab -l 2>/dev/null; echo '15 3 * * * cd /opt/stayease && bash scripts/backup.sh >> backups/backup.log 2>&1' ) | crontab -
# copy dumps OFF the server regularly, e.g.:  rsync -a backups/ user@backup-host:/backups/stayease/
bash scripts/restore.sh backups/<file>.sql.gz     # restores into a scratch DB and prints row counts (production untouched)
bash scripts/restore.sh backups/<file>.sql.gz --into-production   # overwrites live data (asks you to type the DB name)
```

Importing the data of the original SQLite site: see [MIGRATION.md §5](MIGRATION.md#5-database-and-data-migration).

## 7. Troubleshooting

| Symptom | Check |
|---------|-------|
| **Cloudflare 522 / 521** | `docker compose ps` (web healthy?), `sudo ufw status` (443 open?), DNS `A` record = server IP, `curl -skI https://127.0.0.1/` on the server, nginx logs `docker compose logs web` |
| **Cloudflare 525 / 526** | `nginx/certs/origin.pem/key` missing or not the Cloudflare Origin certificate; mode "Full (strict)" needs a valid origin cert |
| `web` restarts: "cannot load certificate" | create `nginx/certs/origin.pem` + `origin.key` (step 3) |
| API returns 503 | MySQL not ready/reachable: `docker compose logs db backend`; it recovers by itself when MySQL is back |
| `DB_PASSWORD must be set` on compose | `.env` missing or empty value |
| Changed `DB_PASSWORD` in `.env` but login fails | the app user is created only when the volume is first initialised: `docker compose exec db mysql -uroot -p -e "ALTER USER 'stayease_app'@'%' IDENTIFIED BY '<new>'"` |
| Port 80/443 already in use | another web server runs on the host — see below |
| See logs | `docker compose logs -f` (add a service name) |

## Sharing a server with another site

If ports 80/443 are taken (e.g. an existing nginx/Traefik), do not stop it. Change the `ports:` of the `web` service
to e.g. `"8080:80"` and let the existing reverse proxy forward the hostname to `127.0.0.1:8080`
(then TLS terminates in that proxy and `nginx.local.conf` can be mounted as the config). Never reuse an existing
MySQL container or volume: this stack creates its own `stayease_db_data` volume.

## Security checklist (what the stack already does)

* Only nginx publishes ports (80/443). MySQL and Tomcat are reachable only on private Docker networks;
  the DB network is `internal` (no internet).
* Application DB user has `SELECT, INSERT, UPDATE, DELETE` on one database only (not root, no DDL).
* Secrets only in `.env` (mode 600, git-ignored) → container environment; no secrets in images or the repo.
* Tomcat: no default webapps (no manager), shutdown port disabled, no version/stack traces in error pages, runs as a non-root user.
* Nginx: TLS 1.2/1.3, HSTS, security headers, request-size and rate limits on `/api`, `no-store` on the API.
* Prepared statements for every SQL query; server-side validation; generic error messages to clients.
* Keep the OS updated: `sudo apt update && sudo apt upgrade`, and rebuild images periodically for base-image patches.
* SSH: use keys, disable password login (`PasswordAuthentication no` in `/etc/ssh/sshd_config`).
