# Deploy on Vercel (frontend) + Render (backend)

An alternative to the Docker/Nginx setup in [DEPLOYMENT.md](DEPLOYMENT.md). Nothing in the repository was moved:
the two deployment roots are simply two existing folders.

| Part | Host | Root directory | Config |
|------|------|----------------|--------|
| React app | Vercel | `frontend` | `frontend/vercel.json` |
| Java API (Tomcat) | Render (Docker) | `backend` | `render.yaml` |
| MySQL 8 | external host (Render has no MySQL) | - | environment variables |

## 1. Database
Create a MySQL 8 database on a host that offers it (Aiven, TiDB Cloud, a paid Render private service, ...).
Run `database/init/01-schema.sql` and `02-seed.sql` once, and create a user with
`SELECT, INSERT, UPDATE, DELETE` only. Keep host/user/password out of GitHub.

## 2. Render (backend)
New > Blueprint > pick this repository. Render reads `render.yaml`; fill in `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` when asked. Check `https://<service>.onrender.com/api` returns `{"message":"Hello, world!"}`.

## 3. Vercel (frontend)
New Project > import this repository > **Root Directory: `frontend`** (build/output are read from `vercel.json`).
Before deploying, edit `frontend/vercel.json` and replace `YOUR-RENDER-SERVICE.onrender.com` with your Render
host. The rewrite forwards `/api/*` to Render, so the browser stays on one origin and no CORS setup is needed.

## 4. Optional keep-alive
Set the repository variable `RENDER_URL` (e.g. `https://<service>.onrender.com/api`) and
`.github/workflows/render-keepalive.yml` pings it every 10 minutes. This only softens free-tier cold starts.
Free Render services may still sleep; free hosts are fine for a demo, not for real reservations.
