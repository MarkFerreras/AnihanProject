# Break-Glass Procedure — Unlocking an Account Directly in the Database

## When to use this

Use this procedure **only** when an account cannot be recovered through the
application's own admin "Unlock Account" action — most notably when the
**sole ADMIN account** is the one that is locked or disabled, so there is no
other admin available to click the unlock button in the UI.

This is a manual, direct-database fallback. It exists because the security
questions feature (forgot-password + account lockout) intentionally applies
the same lockout rules to every role, including ADMIN — see
`memory-bank/decisions.md` for the rationale. Removing that protection from
ADMIN accounts was considered and rejected as too risky; this document is the
accepted trade-off instead.

**Status: pending implementation.** This procedure is written against the
column names anticipated by the current security-questions design
(`enabled`, `security_locked`, `failed_security_attempts`, and related
timestamp columns on `users`). **Before relying on this document in
production, confirm these column names against the actual migration that
ships the feature** (see `src/main/sql/migrations/`) and update this file if
they differ.

## Who may perform this

Restrict this to whoever holds physical/administrative access to the
Anihan SRMS server and its MySQL credentials — per `techContext.md`, the
server is physically located in the Registrar's office and is not reachable
over the internet. Do not distribute this document or the server's MySQL
credentials more broadly than necessary. Do not hardcode real production
credentials into this file or into any file committed to source control.

## Steps

### 1. Back up first

Always take a full backup before making any manual change, following the
project's existing convention (see the other `backup-*.sql` files in this
folder for examples of the command used):

```
mysqldump --databases AnihanSRMS --routines --triggers > backup-YYYY-MM-DD-pre-breakglass-unlock.sql
```

### 2. Connect to MySQL

Using the project's existing Docker-based access pattern:

```
docker exec -it mysql-server mysql -u root -p AnihanSRMS
```

(Substitute the actual container name and credentials for this
deployment — do not assume the development defaults documented in
`CLAUDE.md` apply to the live server.)

### 3. Check the account's current state

```sql
SELECT user_id, username, role, enabled, security_locked,
       failed_security_attempts, security_lockout_started_at
FROM users
WHERE username = '<username>';
```

Confirm which condition(s) are actually blocking access before changing
anything — the account may be `enabled = 0` (an admin deliberately
deactivated it), `security_locked = 1` (it tripped the forgot-password
lockout), or both at once.

### 4. Clear the blocking condition(s)

Clear only what is actually set. It is safe to run all of the following —
each is a no-op if that condition wasn't set:

```sql
UPDATE users
SET enabled = 1
WHERE username = '<username>';

UPDATE users
SET security_locked = 0,
    failed_security_attempts = 0,
    security_lockout_started_at = NULL
WHERE username = '<username>';
```

### 5. Verify

Re-run the `SELECT` from step 3 and confirm `enabled = 1`,
`security_locked = 0`, and `failed_security_attempts = 0`.

### 6. Record what was done

This action bypasses the application entirely, so it will **not** produce a
`system_logs` row automatically. Manually insert one so the audit trail
still reflects what happened and why:

```sql
INSERT INTO system_logs (user_id, username, role, action, ip_address, `timestamp`)
VALUES (
  (SELECT user_id FROM users WHERE username = '<username>'),
  '<username>',
  (SELECT role FROM users WHERE username = '<username>'),
  'Break-glass unlock performed directly via database by <operator name> — reason: <why>',
  'N/A (direct DB access)',
  NOW()
);
```

### 7. Have the user log in and reset their password

Unlocking the account does not reset its password. If the reason the
account was locked was a forgotten password (the usual case), the user
should still go through the normal forgot-password flow once unlocked, or
have an admin issue a temporary password through the existing admin
reset-password action.

## Notes

- `system_logs` is append-only elsewhere in this project; this is the one
  documented exception where a row is added manually rather than by the
  application, precisely because this procedure exists for situations where
  the application itself is not reachable through its own UI.
- If this procedure is needed often, that is a signal the lockout policy or
  the security-question UX (see `memory-bank/decisions.md`) needs revisiting
  — it is meant to be rare.
