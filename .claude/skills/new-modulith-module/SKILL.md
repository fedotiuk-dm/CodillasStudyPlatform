---
name: new-modulith-module
description: Use when adding a new backend module to CodillasStudyPlatform, or filling in an existing module skeleton with endpoints. Encodes the canonical pattern — Maven multi-module + Spring Modulith, API-first OpenAPI (x-spring-paginated), thin MapStruct mappers (CentralMapperConfig, no manual builders), thin MVC controllers implementing generated interfaces — so every module looks the same.
---

# Module builder

Authoritative conventions: `backend/AGENTS.md` (read it first) and the module's own
`backend/<module>/AGENTS.md`. This skill is the step-by-step. Pattern mirrors the boosting
backend. Generated code is read-only; change the spec and regenerate.

## Layout (`backend/<module>/src/main/java/de/codillas/<module>/`)

```
package-info.java     @ApplicationModule
web/                  @RestController implements <Module>Api    (thin)
service/              <X>Service + <X>ServiceImpl
domain/model/         @Entity, enums
domain/repository/    JpaRepository
mapper/               @Mapper(config = CentralMapperConfig.class)
event/                domain event records
config/               module-local @ConfigurationProperties
src/main/resources/db/changelog/<module>-changelog.yaml
backend/openapi/<module>-paths.yaml, <module>-schemas.yaml      (specs live at backend root)
```

## Steps to add/fill module `foo`

### 1. Register the module
- `backend/foo/pom.xml`: parent `codillas-study-platform`, artifactId `codillas-foo`, depend on
  `codillas-shared`. Add only module-specific deps (common ones inherit from parent).
- Add `<module>foo</module>` to `backend/pom.xml`; add `codillas-foo` dep to `backend/main/pom.xml`.
- `package-info.java`: `@ApplicationModule` over `package de.codillas.foo;`.

### 2. Write the OpenAPI spec (API-first)
`backend/openapi/foo-paths.yaml` + `foo-schemas.yaml`. Use **block YAML** (expanded), not flow
`{}`. Each `-paths.yaml` opens with a full header (`info` + `license` + `servers` + `tags`).
Paginated list operation:
```yaml
/api/foos:
  get:
    tags:
      - foo
    operationId: listFoos
    x-spring-paginated: true            # backend → Pageable (generator ignores the params below)
    parameters:
      - name: search
        in: query
        required: false
        schema:
          type: string
      - $ref: "common.yaml#/components/parameters/PageNumber"   # explicit params so Orval gets pagination
      - $ref: "common.yaml#/components/parameters/PageSize"
      - $ref: "common.yaml#/components/parameters/Sort"
    responses:
      "200":
        content:
          application/json:
            schema:
              $ref: "foo-schemas.yaml#/components/schemas/FooListResponse"
      "401":
        $ref: "common.yaml#/components/responses/Unauthorized"
  post:
    tags:
      - foo
    operationId: createFoo
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: "foo-schemas.yaml#/components/schemas/CreateFooRequest"
    responses:
      "201":
        content:
          application/json:
            schema:
              $ref: "foo-schemas.yaml#/components/schemas/Foo"
      "400":
        $ref: "common.yaml#/components/responses/BadRequest"
```
```yaml
# foo-schemas.yaml
FooListResponse:
  allOf:
    - $ref: "common.yaml#/components/schemas/PageResponse"
    - type: object
      required:
        - content
      properties:
        content:
          type: array
          items:
            $ref: "#/components/schemas/Foo"
```

### 3. Wire the generator (foo/pom.xml `<build><plugins>`)
Config is inherited from the parent pluginManagement — only the execution + build-helper here:
```xml
<plugin>
  <groupId>org.openapitools</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <executions><execution>
    <id>generate-foo-api</id>
    <goals><goal>generate</goal></goals>
    <phase>generate-sources</phase>
    <configuration>
      <inputSpec>${project.parent.basedir}/openapi/foo-paths.yaml</inputSpec>
      <apiPackage>de.codillas.foo.api</apiPackage>
      <modelPackage>de.codillas.foo.api.dto</modelPackage>
    </configuration>
  </execution></executions>
</plugin>
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>build-helper-maven-plugin</artifactId>
  <executions><execution>
    <id>add-generated-source</id>
    <goals><goal>add-source</goal></goals>
    <phase>generate-sources</phase>
    <configuration><sources>
      <source>${project.build.directory}/generated-sources/openapi/src/main/java</source>
    </sources></configuration>
  </execution></executions>
</plugin>
```

### 4. Domain + repository
```java
@Entity @Table(name = "foos")
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class Foo extends BaseAuditableEntity {
  @Column(nullable = false) private String name;
}

@Repository
public interface FooRepository extends JpaRepository<Foo, UUID> {}
```

### 5. Mapper (thin — no manual building)
```java
@Mapper(config = CentralMapperConfig.class)
public interface FooMapper {
  Foo /*dto*/ toDto(Foo entity);                         // exhaustive (unmapped target = build error)

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Foo toEntity(CreateFooRequest request);                // id/timestamps owned by Hibernate/builder

  @BeanMapping(ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "name")
  void updateEntity(@MappingTarget Foo entity, UpdateFooRequest request);

  FooListResponse toListResponse(Page<Foo> page);        // Page envelope auto-mapped
}
```

### 6. Service (interface + impl)
```java
public interface FooService {
  FooListResponse listFoos(Pageable pageable);
  Foo createFoo(CreateFooRequest request);
}

@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly = true)
public class FooServiceImpl implements FooService {
  private final FooRepository repository;
  private final FooMapper mapper;

  @Override public FooListResponse listFoos(Pageable pageable) {
    return mapper.toListResponse(repository.findAll(pageable));
  }

  @Override @Transactional public Foo createFoo(CreateFooRequest request) {
    return mapper.toDto(repository.save(mapper.toEntity(request)));
  }

  private Foo findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Foo", id));
  }
}
```

### 7. Controller (thin delegator)
```java
@RestController @RequiredArgsConstructor
public class FooController implements FooApi {
  private final FooService service;

  @Override @RequiresAuthenticated
  public ResponseEntity<FooListResponse> listFoos(String search, Pageable pageable) {
    return ResponseEntity.ok(service.listFoos(pageable));
  }

  @Override @RequiresAdmin
  public ResponseEntity<Foo> createFoo(CreateFooRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createFoo(request));
  }
}
```

### 8. Liquibase
Changeset under `db/changelog/changes/<semver>/<semver>-create-foos.yaml` (`id: <semver>-create-foos`,
`author`, `comment`, and a `not tableExists` precondition with `onFail: MARK_RAN`). `include` it (in
version order) from `db/changelog/foo-changelog.yaml`. The module changelog is already `include`d by
`main`'s master `codillas-changelog.yaml` — no edit there.

### 9. Events (if the module signals other modules)
Record in `event/`; publish via `ApplicationEventPublisher`; consumers use `@ApplicationModuleListener`.

## Verify
```bash
cd backend && mvn -pl foo -am test     # generates spec, compiles, module tests
mvn spotless:apply                     # format
```

ponytail: don't create `domain/`, `mapper/`, `event/`, `config/` until there's something to put
in them. Reference other modules by id + events, never by their `@Entity`.
