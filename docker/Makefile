# Containers run as the host user so generated files (target/, lib/api) stay host-owned.
export HOST_UID := $(shell id -u)
export HOST_GID := $(shell id -g)

COMPOSE = docker compose -f docker-compose.dev.yml
INFRA   = postgres keycloak minio minio-init mailpit

# Generate the Orval client on the HOST (host-owned lib/api). Run before the frontend container.
gen:       ; cd ../frontend && pnpm install --frozen-lockfile && ./node_modules/.bin/orval

# `up` runs the WHOLE dev stack (infra + backend + frontend) in dev mode — hot reload everywhere.
init:      ; ./init.sh
up: gen    ; $(COMPOSE) up -d
down:      ; $(COMPOSE) down
logs:      ; $(COMPOSE) logs -f
ps:        ; $(COMPOSE) ps
clean:     ; $(COMPOSE) down -v

# Infra only — when you'd rather run backend/frontend on the host yourself.
up-infra:  ; $(COMPOSE) up -d $(INFRA)

# Build the production images (Dockerfiles) — for CI / a prod deploy, not the dev stack.
build:
	docker build -f ../backend/Dockerfile -t codillas-backend ../backend
	docker build -f ../frontend/Dockerfile -t codillas-frontend ..

.PHONY: gen init up down logs ps clean up-infra build
