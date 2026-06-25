# Backend — binding contracts

Binding work contract for everything under `backend/`. Anything you touch here must stay
understandable from this file plus the module's own `AGENTS.md`. Read the chain before editing.
These are **stable contracts, not changelogs** — update an `AGENTS.md` only when a module-level
convention changes (a new published event, a new aggregate, a changed rule), not on routine edits.
(`CLAUDE.md` is a symlink to `AGENTS.md` in each dir — one file, read by both Claude Code and
Codex.) To create or fill a module, use the **`new-modulith-module`** skill — do not scaffold blind.

## Architecture

Spring Modulith, Maven multi-module. Base package `de.codillas`. Each module is its own Maven
module **and** an `@ApplicationModule` package. See `docs/architecture/overview.md`.

- Modules talk via **domain events**, never by calling another module's repository/service internals.
- Cross-module references are **by id only** — never import another module's `@Entity`.
- Each module **owns its tables** (its own Liquibase changelog). No cross-module joins.
- `shared` is the OPEN shared kernel (CentralMapperConfig, base types, cross-module event records).

## Module layout (`backend/<module>/src/main/java/de/codillas/<module>/`)

```
package-info.java            @ApplicationModule
api/ (generated)             OpenAPI-generated <Module>Api interface + dto/  (target/generated-sources)
web/                         @RestController implements <Module>Api  (thin delegator)
service/                     <X>Service interface + <X>ServiceImpl
domain/model/                @Entity, enums, value objects
domain/repository/           Spring Data repositories
mapper/                      MapStruct mappers (config = CentralMapperConfig.class)
event/                       domain event records published by this module
config/                      module-local @ConfigurationProperties
```

## API-first (OpenAPI → generated Spring interfaces)

- Specs live in `backend/openapi/`: `common.yaml` (shared `PageResponse`, page params,
  `ProblemDetail`, reusable error `responses`), `<module>-paths.yaml`, `<module>-schemas.yaml`.
  Each `-paths.yaml` carries a full header (info + license + `servers` + `tags`).
- The module pom adds an `openapi-generator` `<execution>` (config inherited from the parent) +
  `build-helper` to add generated sources. `apiPackage = de.codillas.<module>.api`,
  `modelPackage = de.codillas.<module>.api.dto`.
- **Pagination**: set BOTH `x-spring-paginated: true` (backend → `Pageable`) AND the explicit
  `$ref` page/size/sort params from `common.yaml` (Orval and other clients don't understand the
  vendor extension; the Spring generator ignores the params, so no duplication). List responses are
  `allOf [PageResponse, { content }]`.
- **Errors**: `$ref common.yaml#/components/responses/{BadRequest,Unauthorized,Forbidden,NotFound,Conflict}`
  instead of re-declaring error bodies. The backend emits matching RFC 9457 `ProblemDetail`.
- Times are `Instant` (generator maps OffsetDateTime/LocalDateTime → Instant). Nullable is jspecify.
- Generated code is **read-only** — change the spec, regenerate. Orval consumes the same specs.

## MVC layering (thin everything)

1. **Controller** (`web/`) — `@RestController @RequiredArgsConstructor implements <Module>Api`.
   Thin delegator: no business logic, delegates to the service, returns `ResponseEntity<Dto>`
   with status codes (201 create, 200 read/update, 204 delete). Authorize per method with the
   `shared.security` annotations — `@RequiresAdmin` / `@RequiresTeacher` / `@RequiresStudent` /
   `@RequiresAuthenticated` — never raw `@PreAuthorize` strings in controllers.
2. **Service** — `<X>Service` interface + `<X>ServiceImpl` (`@Service @RequiredArgsConstructor
   @Slf4j @Transactional(readOnly = true)`; `@Transactional` on writers). Holds business logic;
   maps via the mapper; throws `shared.exception` `NotFoundException` / `ConflictException` /
   `BadRequestException` (RFC 9457 ProblemDetail, rendered by Spring + `GlobalExceptionHandler`).
   Pattern: private `findByIdOrThrow(id)`.
3. **Repository** — `@Repository public interface … extends JpaRepository<Entity, UUID>` + derived finders.
4. **Domain entity** — `@Entity @Getter @Setter @NoArgsConstructor @SuperBuilder`, extends the
   shared auditable base. `@Builder.Default` for collections/defaults.

## MapStruct (thin mappers, no manual building)

- Every mapper: `@Mapper(config = CentralMapperConfig.class)` (Spring component,
  `unmappedTargetPolicy = ERROR`). **No hand-written builders/converters** — MapStruct maps both
  directions, including **into entities**.
- DTO-direction methods (`toDto`, `toResponse`) are exhaustive — an unmapped field breaks the build.
- Entity factories (`toEntity`): `@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)` —
  id/version/timestamps/defaults are owned by Hibernate + the builder, not by mapping.
- Updates (`updateEntity(@MappingTarget Entity e, UpdateRequest r)`): `@BeanMapping(ignoreByDefault
  = true, nullValuePropertyMappingStrategy = IGNORE)` + explicit `@Mapping(target = …)` per field —
  null request fields never overwrite the entity.
- Page → list response: `ListResponse toListResponse(Page<Entity> page)` — MapStruct maps the Page
  envelope automatically.

## Events (Modulith)

- Define event records in the module's `event/` package, or in `shared` if 2+ modules consume them.
- Publish with `ApplicationEventPublisher`; consume with `@ApplicationModuleListener`. See the event
  map in `docs/architecture/overview.md`.

## Persistence

- Liquibase per module: `src/main/resources/db/changelog/<module>-changelog.yaml`, included by the
  master changelog in `main`. Each module owns its tables. `gradebook` is an event-fed read model.
- `spring.jpa.hibernate.ddl-auto = validate` — Liquibase owns the schema.

## Style

- Java 25. Formatting: `mvn spotless:apply` (google-java-format) — enforced.
- Lombok for boilerplate (`@RequiredArgsConstructor`, `@Getter/@Setter`, `@SuperBuilder`, `@Slf4j`).
- One top-level type per file. Constructor injection only (no field `@Autowired`).
- No magic strings for roles — use `de.codillas.user.Role`.

## Testing (TDD)

Write the test first, watch it fail, then the minimal code. The OpenAPI spec is **design** (not
code under test) — write it first; it generates the interface + DTOs. TDD everything below it.

Per-feature order (double loop):
1. OpenAPI spec → generate `<Module>Api` + DTOs.
2. Acceptance test (RED) — `@ApplicationModuleTest` / `@SpringBootTest`: status, body, role
   (`@PreAuthorize`), pagination.
3. Inner loop (inside-out): entity domain logic → repository → service, each RED→GREEN.
4. Wire the thin controller → acceptance goes GREEN. Refactor.
5. `ApplicationModules.of(...).verify()` guards boundaries.

**Where tests live** (this is what makes autowiring resolve with no `@SuppressWarnings`):
- **Per-module → pure unit tests** (`@ExtendWith(MockitoExtension.class)`, mock the
  repository/mapper; no Spring, no DB). Most tests live here. Library modules have no
  `@SpringBootApplication`, so **never put `@SpringBootTest` in a module** — the IDE can't model its
  beans and false-flags `@Autowired`.
- **`main` → integration tests** that need the DB or full context. Extend `BaseIntegrationTest`
  (boots the real app against a Testcontainers **Postgres** via `@ServiceConnection`, runs the
  production master Liquibase changelog, `ddl-auto: validate`). Field `@Autowired` resolves cleanly
  because `main` has the `@SpringBootApplication`. Never H2; never `@Container` + `@DynamicPropertySource`.

Test layers:
- **Domain logic** — pure JUnit in the module (status machines, scoring). Fastest.
- **Service** — module unit test, mocked repository/mapper; happy path + `NotFound` / `Conflict`.
- **Repository / persistence** — integration test in `main` (`BaseIntegrationTest`): real schema + mapping.
- **Controller** — integration test in `main` (`@AutoConfigureMockMvc` + mock `jwt()`): status, roles, pagination.

## Verify

```bash
cd backend && mvn -pl <module> -am test     # module + deps
cd backend && mvn -q compile                # whole backend
mvn spotless:check                          # formatting
```
`@ApplicationModuleTest` + `ApplicationModules.of(...).verify()` guard module boundaries.
