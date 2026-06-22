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

## Part 1 — Create the Aiven MySQL database

1. Sign in at <https://console.aiven.io> and click **Create service → MySQL**.
2. Pick a cloud/region close to your users, choose a plan (the free/hobby tier is
   fine for client testing), name the service (e.g. `anihan-mysql`), and create it.
3. Wait until the service status is **Running**.
4. Open the service's **Overview** tab and copy the connection details:
   - **Host** (e.g. `anihan-mysql-xxxx.aivencloud.com`)
   - **Port** (e.g. `12345`)
   - **User** (usually `avnadmin`)
   - **Password**
   - **Default database** — create one named `AnihanSRMS` under the
     **Databases** tab if it doesn't exist.
5. Aiven requires **SSL**. You'll add `?sslMode=REQUIRED` to the JDBC URL below.

### Apply the schema + seed data to Aiven

From your local machine (which already has the `mysql` client via Docker), run
the SQL files **in this order** against the Aiven host:

```bash
# Structure first
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p \
      --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/schema.sql

# Required seed data
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p \
      --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/seed-accounts.sql
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p \
      --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/seed-lookups.sql

# Optional — demo students (skip for a clean client deployment)
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p \
      --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/seed-sample-students.sql
```

> If you don't have a local `mysql` client, you can pipe through the Docker one:
> `docker exec -i mysql-server mysql -h <HOST> -P <PORT> -u <USER> -p --ssl-mode=REQUIRED AnihanSRMS < src/main/sql/schema.sql`

Verify: `SHOW TABLES;` should list **19** tables and `SELECT username, role FROM users;`
should show `admin`, `registrar`, `trainer`.

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

1. **New → Web Service**, connect the repo, pick the branch.
2. **Build command:**
   ```
   ./gradlew clean bootJar -x test
   ```
3. **Start command:**
   ```
   java -jar build/libs/springboot-0.0.1-SNAPSHOT.jar
   ```
4. Set the environment variables below.

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
