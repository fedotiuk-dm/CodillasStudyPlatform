COMPOSE = docker compose -f docker-compose.dev.yml
FULL    = $(COMPOSE) --profile full

# `up` brings up EVERYTHING (infra + backend + frontend containers).
init:      ; ./init.sh
up:        ; $(FULL) up -d --build
build:     ; $(FULL) build
down:      ; $(FULL) down
logs:      ; $(FULL) logs -f
ps:        ; $(FULL) ps
clean:     ; $(FULL) down -v

# Infra only (Postgres + Keycloak + minio + mailpit) — run backend/frontend on the host.
up-infra:  ; $(COMPOSE) up -d
down-infra:; $(COMPOSE) down

.PHONY: init up build down logs ps clean up-infra down-infra
