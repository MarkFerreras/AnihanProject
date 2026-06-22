# Deployment Guide — Anihan SRMS (Render + Aiven)

This guide deploys the **entire** application (Spring Boot back-end **and** the
frontend it serves) as one Render web service, backed by an Aiven MySQL database.

> **Why not Netlify/Vercel?** Those host static sites and serverless functions.
> This app is a single, stateful Spring Boot server that serves its own HTML/JS
> from inside the `.jar` and keeps in-memory login sessions. It must run as a
> long-lived Java process — which Render does and Netlify/Vercel cannot.

```
Browser ──HTTPS──▶ Render web service (Spring Boot .jar: API + frontend)
                        │
                        └──SSL──▶ Aiven MySQL (AnihanSRMS)
```

---
## Part 1 — Create the Aiven MySQL database FORMER
1. Sign in at <https://console.aiven.io> and click **Create service → MySQL**.
2. Pick a cloud/region close to your users, choose a plan (the free/hobby tier is
   fine for client testing), name the service (e.g. `anihan-mysql`), and create it.
3. Wait until the service status is **Running**.
4. Open the service's **Overview** tab and copy the connection details:
   - **Host** (e.g. `anihan-mysql-xxxx.aivencloud.com`) -mysql-server-uap-adminv1.j.aivencloud.com
   - **Port** (e.g. `12345`) -11492
   - **User** (usually `avnadmin`) -avnadmin
   - **Password**
   - **Default database** — create one named `AnihanSRMS` under the
     **Databases** tab if it doesn't exist.
5. Aiven requires **SSL**. You'll add `?sslMode=REQUIRED` to the JDBC URL below.
## FORMER

## Part 1 — Create the Aiven MySQL database

1. Sign in at <https://console.aiven.io> and click **Create service → MySQL**.
2. Pick a cloud/region close to your users, choose a plan (the free/hobby tier is
   fine for client testing), name the service (e.g. `anihan-mysql`), and create it.
3. Wait until the service status is **Running**.
4. Open the service's **Overview** tab and copy the connection details
   (substitute your own values for the `<...>` placeholders throughout this guide):
   - **Host** — e.g. `<AIVEN_HOST>` (looks like `xxxx.aivencloud.com`)
   - **Port** — e.g. `<AIVEN_PORT>`
   - **User** — usually `avnadmin`
   - **Password** — `<AIVEN_PASSWORD>`
   - **Default database** — create one named `AnihanSRMS` under the
     **Databases** tab if it doesn't exist.
5. Aiven requires **SSL**. You'll add `?sslMode=REQUIRED` to the JDBC URL below.

> **⚠️ Never commit real credentials.** Keep your Aiven host/password out of any
> file that's tracked by git. If a password ever lands in a committed file,
> rotate it in the Aiven console immediately.

### Apply the schema + seed data to Aiven

> **PowerShell users (Windows):** PowerShell does **not** support the Bash `<`
> input-redirection operator (you'll get `The '<' operator is reserved for future
> use`) and does **not** use `\` for line continuation. Use `Get-Content … | mysql`
> on a **single line** instead. The commands below are written for PowerShell.

You have the MySQL client available through your local Docker container
(`mysql-server`), so the **recommended** way is to pipe each file through it.
Run these **in order**, one line each (you'll be prompted for the Aiven password):

```powershell
# 1. Structure first
Get-Content src/main/sql/schema.sql | docker exec -i mysql-server mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS

# 2. Required seed data — accounts
Get-Content src/main/sql/seed-accounts.sql | docker exec -i mysql-server mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS

# 3. Required seed data — lookups
Get-Content src/main/sql/seed-lookups.sql | docker exec -i mysql-server mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS

# 4. Optional — demo students (skip for a clean client deployment)
Get-Content src/main/sql/seed-sample-students.sql | docker exec -i mysql-server mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS
```

**If you have the `mysql` client installed natively** (on PATH), drop the
`docker exec -i mysql-server` part:

```powershell
Get-Content src/main/sql/schema.sql | mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS
```

> If any file's characters come through garbled, force UTF-8 reading:
> `Get-Content -Raw -Encoding utf8 src/main/sql/schema.sql | …`

**Bash / macOS / Linux equivalent** (if you ever run it outside PowerShell):

```bash
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/schema.sql
```

### Verify the load

Open an interactive session and check:

```powershell
docker exec -it mysql-server mysql -h mysql-server-uap-adminv1.j.aivencloud.com -P 11492 -u avnadmin -p --ssl-mode=REQUIRED AnihanSRMS
```

Then run `SHOW TABLES;` (should list **19** tables) and
`SELECT username, role FROM users;` (should show `admin`, `registrar`, `trainer`).

---

## Part 2 — Deploy the app to Render

### Option A — Blueprint (recommended)

This repo includes `render.yaml`. In Render:

1. **New → Blueprint**, connect your GitHub repo, and select the
   `AnihanSRMSv3.5` branch (or whichever branch you deploy).
2. Render reads `render.yaml` and proposes one web service. Confirm.
3. Fill in the environment variables it marks as `sync: false` (see below).
4. Click **Apply**.

### Option B — Manual web service

Java is **not** a native Render runtime, so the app is built as a **Docker**
service using the `Dockerfile` in this repo (which builds the jar and runs it).

1. **New → Web Service**, connect the repo, pick the branch.
2. **Language / Runtime:** choose **Docker**. Render auto-detects the root
   `Dockerfile` — no build or start command needs to be entered (the Dockerfile
   handles both: `./gradlew clean bootJar -x test`, then `java -jar app.jar`).
3. Set the environment variables below.

### Required environment variables (set in Render dashboard)

| Variable        | Value                                                                 |
|-----------------|-----------------------------------------------------------------------|
| `DB_URL`        | `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/AnihanSRMS?sslMode=REQUIRED`   |
| `DB_USERNAME`   | `<AIVEN_USER>` (e.g. `avnadmin`)                                       |
| `DB_PASSWORD`   | `<AIVEN_PASSWORD>`                                                     |
| `COOKIE_SECURE` | `true`  (Render serves over HTTPS)                                     |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` (optional — keeps heap under the plan's RAM) |

> `PORT` is injected by Render automatically — do **not** set it yourself.
> `application.properties` already reads `${PORT:8080}`.

5. Deploy. Watch the logs for `Tomcat started on port(s)` and
   `Started SpringbootApplication`.

---

## Part 3 — First login & hardening

1. Visit your Render URL (e.g. `https://anihan-srms.onrender.com`).
2. Log in as `admin` / `password123`.
3. **Immediately change all three default passwords** (admin, registrar,
   trainer) via the Edit Account modal. The defaults in `seed-accounts.sql`
   are for first access only.

---

## Known limitations / follow-ups before real client use

1. **Uploaded files are NOT persistent.** `StorageService` writes ID-photo /
   baptismal-cert uploads to the local filesystem (`STORAGE_ROOT`, default
   `./uploads`). Render's disk is **ephemeral** — these files vanish on every
   restart/redeploy. Two fixes, pick one before the client uploads real files:
   - **Render Disk:** add a persistent disk in `render.yaml`, mount it, and set
     `STORAGE_ROOT` to the mount path. Simplest, but ties files to one instance.
   - **DB BLOB storage (preferred):** rework `StorageService` to store bytes in
     the database (the `documents` table already has a `LONGBLOB` column). This
     survives restarts and matches the air-gapped-server design intent. **This
     is a code change not yet done** — track as a follow-up.
2. **Free-tier cold starts.** Render's free web services spin down when idle and
   take ~30–60s to wake. Fine for a test; use a paid instance for a real demo.
3. **Aiven free tier** has limited storage/connections — adequate for testing,
   not for production load.
