# Deployment — going live checklist

> Audience: whoever hosts the platform. Dev runs everything via
> `docker/docker-compose.dev.yml` (`make up`); production uses the two Dockerfiles +
> your own reverse proxy. This is the checklist of what must change between the two.

## Images

```bash
cd docker && make build
# → codillas-backend  (backend/Dockerfile: boot jar, port 8081)
# → codillas-frontend (frontend/Dockerfile: next standalone, port 3000)
```

Infrastructure next to them: Postgres 18, Keycloak 26, minio — same images as the dev
compose. Mailpit is dev-only; point prod at a real SMTP relay.

## Checklist

### Secrets & config (backend env)

- [ ] `DB_URL` / `DB_USER` / `DB_PASSWORD` — real Postgres, strong password.
- [ ] `KEYCLOAK_ISSUER_URI` — the **public** realm URL (`https://auth.…/realms/codillas`).
- [ ] `FILES_ENDPOINT` / `FILES_ACCESS_KEY` / `FILES_SECRET_KEY` / `FILES_BUCKET` — minio
      with real credentials (never the `minioadmin` defaults).
- [ ] `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` (+ auth props if the relay needs them) and
      `NOTIFICATION_EMAIL_FROM` — real sender address.
- [ ] `NOTIFICATION_DEFAULT_LOCALE` — the school's language (default `uk`).
- [ ] Leave `SPRING_LIQUIBASE_CONTEXTS` **unset** (defaults to `prod`) — the `seed`
      context loads demo data and is for dev only.

### Frontend env (build-time `NEXT_PUBLIC_*`)

- [ ] `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_WS_URL` (`wss://…/ws`),
      `NEXT_PUBLIC_KEYCLOAK_URL`, `NEXT_PUBLIC_APP_URL` — all public HTTPS URLs.

### Keycloak

- [ ] Run `start` (production mode, TLS/hostname set) — **not** `start-dev`.
- [ ] Import `backend/keycloak/realm-export.json` once (first boot), then remove the
      import flag — later boots must not re-import over live users.
- [ ] Change the master admin password; delete/disable the three demo users
      (admin/teacher/student, password `password`) or reset their passwords.
- [ ] Realm `codillas` → Clients → `codillas-frontend`: set the real redirect URIs and
      web origins (the deployed frontend URL only).

### TLS / reverse proxy

- [ ] One proxy (Traefik/Caddy/nginx) terminating TLS for frontend (3000),
      backend (8081, incl. `/ws` WebSocket upgrade), Keycloak, minio.
- [ ] Everything browser-reachable is HTTPS — keycloak-js and the WS client refuse
      mixed content.

### Backups (the actual data)

- [ ] **Postgres** — both databases: `pg_dump codillas` and `pg_dump keycloak`
      (users live in Keycloak's DB), nightly, kept off-host.
- [ ] **minio** — `mc mirror` of the bucket (submission/material files), nightly.
- [ ] **Realm config** — re-export after role/client changes:
      `kc.sh export --realm codillas`.
- [ ] Restore drill once: fresh Postgres + restored dumps + mirrored bucket boots and
      signs in.

### Smoke test after deploy

- [ ] Sign in as a real (non-demo) admin → create course → publish → create group →
      enroll a test student.
- [ ] Student signs in → sees the course → submits homework with a file → teacher grades
      → student gets the in-app **and email** notification.
- [ ] Chat sends/receives live (WS through the proxy), announcement fan-out arrives.

## Deliberately not here

Monitoring/alerting stack, Vault, horizontal scaling — single-school single-node deploy
does not need them yet (overview §12); revisit with the multi-tenant future vision.
