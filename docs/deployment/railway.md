# Deploying Codillas Study Platform to Railway

The app is deployable on Railway as a small set of services. Do not deploy the repository root as
one Railpack service: it is a Maven + Next.js monorepo, not a single app with a `start.sh`.

## Services

Create these services in one Railway project/environment:

1. **PostgreSQL** — add Railway's PostgreSQL service. Set backend `DB_URL` to the internal
   connection string in JDBC form, for example
   `jdbc:postgresql://${{Postgres.RAILWAY_PRIVATE_DOMAIN}}:5432/${{Postgres.PGDATABASE}}`;
   set `DB_USER` and `DB_PASSWORD` from the database service's internal variables.
2. **Backend** — connect this repository. Set root directory to `/` and Dockerfile path to
   `/backend/Dockerfile`. Railway must use the repository root as build context because the
   Dockerfile builds the Maven reactor from `backend/`. Set `KEYCLOAK_ISSUER_URI` to
   `https://<keycloak-domain>/realms/codillas`, `APP_CORS_ALLOWED_ORIGINS` to the frontend's full
   HTTPS origin, and the `FILES_*` variables described below. Railway injects `PORT`; the image
   listens on it (defaults to 8080). The config in `backend/railway.toml` checks
   `/actuator/health`.
3. **Frontend** — connect this repository. Set root directory to `/` and Dockerfile path to
   `/frontend/Dockerfile`; build context must include `backend/openapi`, which the image uses to
   generate the API client. Set build-time `NEXT_PUBLIC_API_URL` to the backend's public origin plus
   `/api`, `NEXT_PUBLIC_WS_URL` to `wss://<backend-domain>/ws`, `NEXT_PUBLIC_KEYCLOAK_URL` to the
   public Keycloak origin, and `NEXT_PUBLIC_APP_URL` to the frontend's public origin. These values
   are embedded into the client during the Next.js image build, so add them before deploying.
4. **Keycloak** — run the `codillas` realm and configure the `codillas-frontend` client with the
   deployed frontend origin in its valid redirect URIs and web origins. Configure its backend
   client/resource server as required by the existing realm export. The committed realm export
   currently only allows localhost redirects, so update the Keycloak client configuration for the
   hosted URL. Set the backend issuer URL and frontend Keycloak URL to the same public instance.
5. **S3-compatible object storage** — provide endpoint, bucket, region, access key and secret via
   backend `FILES_ENDPOINT`, `FILES_BUCKET`, `FILES_REGION`, `FILES_ACCESS_KEY`, and
   `FILES_SECRET_KEY`. Use a persistent object storage service; uploaded files must not live in the
   app container filesystem.

Use Railway private networking for the backend-to-Postgres connection. Public networking is needed
for the frontend, backend API/WebSocket and browser access to Keycloak. For a first MVP deployment,
email is optional: without SMTP variables the app keeps in-app notifications and skips email.

## Resource expectations

The root deployment error is configuration: Railpack could not identify a start command. It does
not indicate that the project is too large. The repository already has production Dockerfiles for
the backend and frontend. The free service memory ceiling can be restrictive for Java + database +
Keycloak; use Railway metrics and the current plan limits to decide whether a paid plan is needed.
