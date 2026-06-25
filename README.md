# CodillasStudyPlatform

Learning platform for the Codillas IT school. Spring Modulith backend
(Maven multi-module) + Next.js frontend.

- **Architecture:** [`docs/architecture/overview.md`](docs/architecture/overview.md)
- **Backend:** `backend/` — modules: `shared`, `user`, `course`, `enrollment`,
  `homework`, `assessment`, `gradebook`, `chat`, `notification`, `files`, `main`.
- **Frontend:** `frontend/` (Next.js + Orval, added later).

## Run (backend)

Needs Java 25, Maven, and a Postgres + Keycloak (reuse the boosting docker setup).

```bash
cd backend
mvn spring-boot:run -pl main
```

Defaults: app on `:8081`, Postgres `codillas/codillas@localhost:5432/codillas`,
Keycloak realm at `http://localhost:8080/realms/codillas`. Override via
`DB_URL`, `DB_USER`, `DB_PASSWORD`, `KEYCLOAK_ISSUER_URI`, `SERVER_PORT`.
