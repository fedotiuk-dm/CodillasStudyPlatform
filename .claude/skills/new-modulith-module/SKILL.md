---
name: new-modulith-module
description: Use when adding a new backend module to CodillasStudyPlatform, or filling in an existing module skeleton. Encodes the canonical Spring Modulith + Maven multi-module pattern (package layout, pom, Liquibase, OpenAPI-first, MapStruct, events) so every module looks the same.
---

# Adding / filling a Modulith module

Reference: `docs/architecture/overview.md`. Pattern mirrors the boosting project
(`/home/iddqd/IdeaProjects/BoostingJavaSpringNextjs/backend`). Each backend
module is its own Maven module **and** an `@ApplicationModule` package.

## Canonical layout (`backend/<module>/`)

```
pom.xml                                  parent = codillas-study-platform, depends on codillas-shared
src/main/java/de/codillas/<module>/
  package-info.java                      @ApplicationModule
  domain/model/                          @Entity, enums, value objects
  domain/repository/                     Spring Data repositories (+ Specs)
  service/                               <X>Service interface + <X>ServiceImpl, @ApplicationModuleListener listeners
  web/                                   REST controllers (implement OpenAPI-generated interfaces)
  web/dto/                               request/response DTOs (or use generated ones)
  mapper/                                MapStruct mappers (entity <-> dto)
  config/                                module-local @ConfigurationProperties / config
src/main/resources/db/changelog/
  <module>-changelog.yaml                included by main's master changelog
  changes/<version>/...                  individual changesets
```

## Rules (non-negotiable — they keep the monolith clean)

- Cross-module references **by id only** — never import another module's `@Entity`.
- No calling another module's repository/service internals. Talk via:
  - **events** for notifications/side effects (publish `ApplicationEventPublisher`,
    consume with `@ApplicationModuleListener`), or
  - a module's **published API** (an interface in the module root package) for queries.
- Each module **owns its tables**; no cross-module joins. `gradebook` is a read
  model fed by events.
- `@EnableMethodSecurity` is on; guard endpoints with `@PreAuthorize` using
  roles `ROLE_ADMIN` / `ROLE_TEACHER` / `ROLE_STUDENT`.

## Steps to add a module named `foo`

1. `backend/foo/` with the layout above; `package-info.java`:
   ```java
   @ApplicationModule
   package de.codillas.foo;
   import org.springframework.modulith.ApplicationModule;
   ```
2. `backend/foo/pom.xml`: parent `codillas-study-platform`, artifactId
   `codillas-foo`, dependency on `codillas-shared`. Common deps (web, jpa,
   validation, modulith-core, mapstruct) are inherited from the parent — only
   add module-specific ones (e.g. websocket for `chat`).
3. Register in `backend/pom.xml` `<modules>` and add a `codillas-foo` dependency
   in `backend/main/pom.xml`.
4. Liquibase: `db/changelog/foo-changelog.yaml` (`databaseChangeLog: []` to
   start), then add an `include` line in
   `backend/main/src/main/resources/db/changelog/codillas-changelog.yaml`.
5. **API-first**: write the OpenAPI spec, wire `openapi-generator` to produce
   server interfaces + DTOs, implement them in `web/`. Orval consumes the same
   spec for the frontend client.
6. Events: define event records in the module root (or in `shared` if multiple
   modules consume them — see the event map in the overview doc).
7. Test: `@ApplicationModuleTest` for the slice; `ApplicationModules.of(app).verify()`
   in a Modulith test guards boundaries.

## Verify

```bash
cd backend && mvn -pl foo -am test          # module + its deps
cd backend && mvn -q compile                # whole backend
```

ponytail: don't add web/jpa/websocket deps a module doesn't use; don't create
`domain/`, `mapper/`, `config/` folders until there's something to put in them.
