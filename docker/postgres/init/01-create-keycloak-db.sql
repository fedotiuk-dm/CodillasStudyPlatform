-- Keycloak gets its own database in the same Postgres instance (one container, two databases).
-- Runs once, on first cluster init, via /docker-entrypoint-initdb.d. Owned by the app superuser.
CREATE DATABASE keycloak;
