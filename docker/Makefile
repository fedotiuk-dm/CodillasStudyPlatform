COMPOSE = docker compose -f docker-compose.dev.yml

# Infra only (Postgres + Keycloak + minio + mailpit); run backend/frontend on the host.
init:      ; ./init.sh
up:        ; $(COMPOSE) up -d
down:      ; $(COMPOSE) down
logs:      ; $(COMPOSE) logs -f
ps:        ; $(COMPOSE) ps
clean:     ; $(COMPOSE) down -v

# Full stack — also build + run the backend and frontend containers (compose profile `full`).
up-full:   ; $(COMPOSE) --profile full up -d --build
down-full: ; $(COMPOSE) --profile full down

.PHONY: init up down logs ps clean up-full down-full
