COMPOSE = docker compose -f docker-compose.dev.yml

up:    ; $(COMPOSE) up -d
down:  ; $(COMPOSE) down
logs:  ; $(COMPOSE) logs -f
ps:    ; $(COMPOSE) ps
clean: ; $(COMPOSE) down -v

.PHONY: up down logs ps clean
