# shared — module contract

The OPEN shared kernel (`@ApplicationModule(type = OPEN)`) — every module may use it.
Follows `backend/AGENTS.md`.

- **Holds:** `CentralMapperConfig` (MapStruct), base auditable entity, common exceptions
  (`NotFoundException`, `ConflictException`), security context helpers, and **cross-module event
  records** (events consumed by 2+ modules live here, not in the publishing module).
- **No domain of its own**, no REST API, no Liquibase changelog.
- **Rule:** keep it minimal — add a type here only when 2+ modules need it; module-local logic
  stays in the module.
