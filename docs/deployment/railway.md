# Deploying Codillas Study Platform to Railway

The app runs on Railway as separate services behind one public gateway. Do not deploy the
repository root as one Railpack service: it is a Maven + Next.js monorepo.

## Services

Create these services in one Railway project/environment:

1. **PostgreSQL** — add Railway's PostgreSQL service. Set backend `DB_URL` to
   `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`,
   `DB_USER` to `${{Postgres.PGUSER}}`, and `DB_PASSWORD` to `${{Postgres.PGPASSWORD}}`.
2. **Backend** — connect this repository. In **Settings → Build**, leave Root Directory as `/`
   and set Dockerfile Path to `/backend/Dockerfile`. Railway must use the repository root as build
   context because the Dockerfile builds the Maven reactor from `backend/`. In **Settings → Deploy**,
   set Healthcheck Path to `/actuator/health` and timeout to 600 seconds. Set `PORT=8080`,
   `KEYCLOAK_ISSUER_URI` to `https://<keycloak-domain>/realms/codillas`,
   `APP_CORS_ALLOWED_ORIGINS` to the gateway's full HTTPS origin, and the `FILES_*` variables
   described below. Leave **Settings → Config-as-code → Railway Config File** empty:
   new services that have never used the legacy feature cannot opt into it.
3. **Frontend** — create a separate service connected to this repository. In **Settings → Build**,
   leave Root Directory as `/` and set Dockerfile Path to `/frontend/Dockerfile`; build context must
   include `backend/openapi`, which the image uses to generate the API client. Set `PORT=3000`,
   `NEXT_PUBLIC_WS_URL=wss://<gateway-domain>/ws`, `NEXT_PUBLIC_KEYCLOAK_URL` to the public
   Keycloak origin, and `NEXT_PUBLIC_APP_URL` to the gateway's full HTTPS origin. Leave
   `NEXT_PUBLIC_API_URL` unset: generated paths start with `/api`, so the browser calls the gateway
   on the same origin. Set these before deploying; Next.js embeds them at build time.
4. **Gateway** — create a service from the `caddy:2-alpine` image. Set `PORT=8080`,
   `BACKEND_UPSTREAM` to
   `http://${{Backend.RAILWAY_PRIVATE_DOMAIN}}:8080`, and `FRONTEND_UPSTREAM` to
   `http://${{Frontend.RAILWAY_PRIVATE_DOMAIN}}:3000` (adjust the service names in the references
   if they differ). Put the Caddy configuration below in a `CADDYFILE` variable and set the start
   command to `sh -c 'printf %s "$CADDYFILE" > /tmp/Caddyfile && exec caddy run --config /tmp/Caddyfile --adapter caddyfile'`.
   Set Healthcheck Path to `/health`. Generate a public domain for this service;
   it sends `/api/*` and `/ws` to the backend and all page routes to Next.js. Caddy forwards
   WebSocket upgrades. The backend and frontend need only private networking.

   ```caddyfile
   {
       admin off
       auto_https off
   }

   :{$PORT:8080} {
       encode zstd gzip
       handle /health {
           respond "ok" 200
       }
       @backend path /api/* /ws /ws/*
       handle @backend {
           reverse_proxy {$BACKEND_UPSTREAM} {
               header_up X-Forwarded-Proto https
               header_up X-Forwarded-Port 443
           }
       }
       handle {
           reverse_proxy {$FRONTEND_UPSTREAM} {
               header_up X-Forwarded-Proto https
               header_up X-Forwarded-Port 443
           }
       }
   }
   ```
5. **Keycloak** — run the `codillas` realm and configure the `codillas-frontend` client with the
   gateway origin in its valid redirect URIs and web origins. Configure its backend
   client/resource server as required by the existing realm export. The committed realm export
   currently only allows localhost redirects, so update the Keycloak client configuration for the
   hosted URL. Set the backend issuer URL and frontend Keycloak URL to the same public instance.
6. **S3-compatible object storage** — provide endpoint, bucket, region, access key and secret via
   backend `FILES_ENDPOINT`, `FILES_BUCKET`, `FILES_REGION`, `FILES_ACCESS_KEY`, and
   `FILES_SECRET_KEY`. Use a persistent object storage service; uploaded files must not live in the
   app container filesystem.

Use Railway private networking between the gateway, frontend, backend, and Postgres. Public
networking is needed for the gateway and browser access to Keycloak. For a first MVP deployment,
email is optional: without SMTP variables the app keeps in-app notifications and skips email.

## Resource expectations

The repository has production Dockerfiles for the backend and frontend. The Railway Free plan may
limit the number of provisioned services before the full stack fits; inspect the current plan
limits before adding a second database for Keycloak. Java, Postgres, and Keycloak also need enough
memory; use Railway metrics to size them.
