# shared — module contract

The OPEN shared kernel (`@ApplicationModule(type = OPEN)`) — every module may use it.
Follows `backend/AGENTS.md`.

- **Holds:** `CentralMapperConfig` (MapStruct), `BaseAuditableEntity` (mapped superclass),
  `exception/` (`NotFoundException` / `ConflictException` / `BadRequestException` +
  `GlobalExceptionHandler`, RFC 9457 ProblemDetail), `security/` role annotations (`@RequiresAdmin`
  / `@RequiresTeacher` / `@RequiresStudent` / `@RequiresAuthenticated`), and **cross-module event
  records** (events consumed by 2+ modules live here, not in the publishing module).
- **No domain of its own**, no endpoints, no Liquibase changelog (the `GlobalExceptionHandler`
  advice is cross-cutting, not a REST resource).
- **Rule:** keep it minimal — add a type here only when 2+ modules need it; module-local logic
  stays in the module.
