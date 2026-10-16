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

## Admin login, dashboard and Excel export
The admin area is at `https://<your-site>/#/admin` (not linked from the public pages).

* **Credentials come from the environment, not from the source code.** Set `ADMIN_USER` and `ADMIN_PASSWORD_HASH`
  (Render: environment variables; Docker: `.env`). Without them the admin login is disabled.
* **Make the hash** (PowerShell, nothing is installed or sent anywhere; replace the password):

  ```powershell
  $pw = 'your-password-here'; $salt = New-Object byte[] 16
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($salt)
  $k = New-Object Security.Cryptography.Rfc2898DeriveBytes($pw, $salt, 310000, [Security.Cryptography.HashAlgorithmName]::SHA256)
  "pbkdf2_sha256:310000:" + [Convert]::ToBase64String($salt) + ":" + [Convert]::ToBase64String($k.GetBytes(32))
  ```
* The session is an HttpOnly, Secure, SameSite=Strict cookie that expires after 30 idle minutes. Five wrong
  passwords within 15 minutes lock the login for the rest of that window. Over plain HTTP by IP the browser
  refuses the Secure cookie: set `COOKIE_SECURE=false` for such a test and remove it afterwards.
* **Dashboard:** every booking in the fields `id, bookingRef, guestName, email, mobile, roomId, roomName, checkIn,
  checkOut, guests, nights, totalPrice, status, createdAt`, with the total at the top.
* **Excel:** the *Download Excel* button gives `stayease-bookings-<date>.xlsx` generated live from the database:
  sheet *Bookings* (all rows, filters, frozen header) and sheet *Summary* (bookings and revenue per room, status
  counts, a pie chart of bookings per room and a column chart of revenue per room). The file contains guests'
  e-mails and phone numbers - do not commit or share it publicly.
