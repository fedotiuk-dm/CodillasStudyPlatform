# P2c — Course & cohort lifecycle implementation plan

> **Status (2026-07-03): IMPLEMENTED** on `feat/lms-hardening` — landed in commit `6af4743`
> (backend P0–P2c) plus the frontend integration commits that follow it. The checkboxes below
> were never ticked during execution; treat this banner, the code, and the git history as the
> source of truth, not the boxes.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give `course` and `enrollment` an explicit lifecycle. A `Course` is `DRAFT → PUBLISHED → ARCHIVED`; a `Group` is `DRAFT → RUNNING → ARCHIVED`. Add `publish`/`archive` (course) and `start`/`archive` (group) endpoints, hide `DRAFT` courses from non-staff list queries, validate that a group is only created against a `PUBLISHED` course, reject writes (enroll/schedule) into an `ARCHIVED` group, and surface `RUNNING+ARCHIVED` (never `DRAFT`) in `/api/me/groups`.

**Architecture:** Per the backend AGENTS.md state-machine convention, each lifecycle lives in a data-driven `<Aggregate>StateMachine` bean (allowed-transition map → `ConflictException` on an illegal move, unit-tested in isolation — copy `homework`'s `SubmissionStateMachine` / `assessment`'s `TestStateMachine`). The entities stay anemic. Enforcement lives in the **writer services**, not just the UI. For the group-create "is the course PUBLISHED?" check, `enrollment` keeps its **own event-fed read model** (`course_status_view`, fed by `CoursePublished`/`CourseArchived`/`CourseDeleted`) and checks it by id — **no dependency on `course`** (see the amendment below). Lists gain an optional `status` filter param; `course` additionally drops `DRAFT` for non-staff via `CurrentUser.isStaff()` (added in Batch 1).

> **AMENDMENT (as built) — decoupling overrides the `CourseCatalog` design below.** The original plan exposed a synchronous base-package read port `CourseCatalog.isPublished(courseId)` from `course`, injected into `enrollment`. That was **rejected and replaced** to keep `enrollment` fully decoupled from `course` (no `codillas-course` Maven dependency, no `de.codillas.course.*` import, no new `enrollment → course` Modulith edge), per the events + by-id rule in `overview.md` §6. As built: `course` publishes `CoursePublished` / `CourseArchived` / `CourseDeleted` (and republishes `CoursePublished` for existing PUBLISHED courses on startup so the read model self-heals); `enrollment` consumes them in `CourseStatusProjection` into a local `course_status_view` table (entity `CourseStatusView` + `CourseStatusViewRepository`), and `GroupServiceImpl.createGroup` validates against that read model (→ 409 if absent/not PUBLISHED). Tasks 1–8 below are unchanged; **ignore every `CourseCatalog` / `CourseCatalogImpl` reference in Task 4 and Task 9 Step 4** — they are superseded by the projection. Integration tests Awaitility-await the read model after publishing (the projection is async).

**Tech Stack:** Spring Modulith (Maven multi-module), Java 25, API-first OpenAPI → generated `*Api` + DTOs, MapStruct (`CentralMapperConfig`), Spring Data JPA + Liquibase (Postgres, `ddl-auto=validate`), JUnit 5 + Mockito (module units) + Testcontainers `BaseIntegrationTest` (`main`).

## Global Constraints

- **Read the contract chain first:** root `CLAUDE.md` → `backend/AGENTS.md` → `backend/course/AGENTS.md` / `backend/enrollment/AGENTS.md`. This plan **references** those conventions (layering, MapStruct rules, error → `ProblemDetail`, named `Sort` constants, durable events) rather than restating them.
- **API-first.** Change the OpenAPI spec under `backend/openapi/` first, regenerate, then TDD below it. Generated code under `target/generated-sources` is read-only.
- **TDD.** Pure JUnit unit tests live **in the module** (`@ExtendWith(MockitoExtension.class)`, no Spring); integration/controller tests live **in `main`** extending `BaseIntegrationTest`. Write the failing test first. Every new endpoint gets a role-gating (negative-authz) test.
- **Use the spec's verbatim names:** enums `CourseStatus{DRAFT,PUBLISHED,ARCHIVED}` and `GroupStatus{DRAFT,RUNNING,ARCHIVED}`; beans `CourseStateMachine` / `GroupStateMachine`.
- **Depends on Batch 1** (`p0-object-authz`): `CurrentUser.isStaff()` already exists — **use it, do not redefine it.**
- **Versioning.** Bump each touched OpenAPI `info.version` `0.2.0 → 0.3.0`; new Liquibase changesets live under `changes/0.3.0/`. Modules inherit the aggregator Maven version (`0.1.0-SNAPSHOT`) — there is no per-module `<version>` to bump.
- **Backfill (stated once, applied in every status changeset):** the `status` column is added **nullable**, existing rows are **backfilled** (`courses → PUBLISHED`, `study_groups → RUNNING`) so current data keeps working, then a DB `DEFAULT 'DRAFT'` + `NOT NULL` are added so **new** rows default to `DRAFT` (the entity `@Builder.Default` sets the same). `defaultValue` is **not** used on `addColumn` (that would stamp every existing row `DRAFT`).
- After each task: `cd backend && mvn -pl <module> -am test`, then `mvn spotless:apply`. `ApplicationModules.of(...).verify()` must stay green. **As built (per the amendment) this batch adds NO new module edge** — `enrollment` ↔ `course` stay coupled only through `shared` events and by-id references.
- Commit after each task with a `feat(course):` / `feat(enrollment):` message. Do not push.

## File Structure

```
backend/openapi/
  course-paths.yaml                MOD  — publish/archive paths; status query param on listCourses; info.version 0.3.0
  course-schemas.yaml              MOD  — CourseStatus enum; status on CourseResponse + CourseDetailResponse
  enrollment-paths.yaml            MOD  — start/archive paths; status query param on listGroups; 409 on createGroup; info.version 0.3.0
  enrollment-schemas.yaml          MOD  — GroupStatus enum; status on GroupResponse

backend/course/src/main/java/de/codillas/course/
  domain/model/CourseStatus.java        NEW  — DRAFT/PUBLISHED/ARCHIVED
  domain/model/Course.java              MOD  — status field
  domain/CourseStateMachine.java        NEW  — allowed-transition map
  domain/repository/CourseRepository.java MOD — findByStatus / findByStatusIn
  CourseCatalog.java                    NEW  — exposed read port (base package)
  service/CourseCatalogImpl.java        NEW  — port impl (package-private)
  service/CourseService.java            MOD  — publishCourse / archiveCourse / status filter
  service/CourseServiceImpl.java        MOD  — transitions, CurrentUser-aware list
  mapper/CourseMapper.java              MOD  — toDomainStatus enum mapping
  web/CourseController.java             MOD  — publish/archive (@RequiresAdmin); status param
backend/course/src/main/resources/db/changelog/
  changes/0.3.0/0.3.0-add-course-status.yaml  NEW
  course-changelog.yaml                 MOD  — include the new changeset
backend/course/src/test/java/de/codillas/course/domain/
  CourseStateMachineTest.java           NEW  — unit, illegal transitions

backend/enrollment/src/main/java/de/codillas/enrollment/
  domain/model/GroupStatus.java         NEW  — DRAFT/RUNNING/ARCHIVED
  domain/model/Group.java               MOD  — status field
  domain/GroupStateMachine.java         NEW  — transitions + assertWritable
  domain/repository/GroupRepository.java MOD — findByStatus / findByIdInAndStatusIn
  service/GroupService.java             MOD  — startGroup / archiveGroup / status filter
  service/GroupServiceImpl.java         MOD  — transitions, CourseCatalog guard
  service/MembershipServiceImpl.java    MOD  — load group + assertWritable
  service/ScheduledLessonServiceImpl.java MOD — load group + assertWritable
  service/MeServiceImpl.java            MOD  — /me/groups RUNNING+ARCHIVED only
  mapper/GroupMapper.java               MOD  — toDomainStatus enum mapping
  web/EnrollmentController.java         MOD  — start/archive (@RequiresAdmin); status param
backend/enrollment/src/main/resources/db/changelog/
  changes/0.3.0/0.3.0-add-group-status.yaml   NEW
  enrollment-changelog.yaml             MOD  — include the new changeset
backend/enrollment/src/test/java/de/codillas/enrollment/domain/
  GroupStateMachineTest.java            NEW  — unit, illegal transitions + assertWritable

backend/main/src/test/java/de/codillas/integration/
  course/CourseLifecycleControllerIntegrationTest.java       NEW
  enrollment/GroupLifecycleControllerIntegrationTest.java    NEW
  enrollment/EnrollmentControllerIntegrationTest.java        MOD  — chain through published course + started group
  enrollment/MeEnrollmentControllerIntegrationTest.java      MOD  — createGroup helper publishes course + starts group
```

---

### Task 1: Course lifecycle — OpenAPI spec

API-first: edit the spec, regenerate, *then* TDD below it. No Java logic yet.

**Files:**
- Modify: `backend/openapi/course-paths.yaml`, `backend/openapi/course-schemas.yaml`

- [ ] **Step 1: `course-schemas.yaml` — add the enum + `status` on the responses.** Bump `info.version` to `0.3.0`. Add the enum schema:

  ```yaml
  CourseStatus:
    type: string
    description: DRAFT is editable and hidden from non-staff; PUBLISHED is selectable for groups; ARCHIVED is retired.
    enum:
      - DRAFT
      - PUBLISHED
      - ARCHIVED
  ```

  Add `status` (required) to **both** `CourseResponse` and `CourseDetailResponse` `properties`:

  ```yaml
        status:
          $ref: "#/components/schemas/CourseStatus"
  ```

  Add `status` to each schema's `required` list (`id, name, status` / `id, name, status, sections`).

- [ ] **Step 2: `course-paths.yaml` — bump `info.version` to `0.3.0`; add a `status` query filter to `listCourses`.** Insert, alongside the existing page params:

  ```yaml
        - name: status
          in: query
          required: false
          description: Optional lifecycle filter. Non-staff callers never receive DRAFT regardless.
          schema:
            $ref: "course-schemas.yaml#/components/schemas/CourseStatus"
  ```

- [ ] **Step 3: `course-paths.yaml` — add publish + archive (mirror the existing `/api/tests/{testId}/publish` shape).**

  ```yaml
    /api/courses/{courseId}/publish:
      post:
        tags: [course]
        operationId: publishCourse
        summary: Publish a draft course (makes it selectable for groups)
        parameters:
          - $ref: "#/components/parameters/CourseId"
        responses:
          "200":
            description: Published
            content:
              application/json:
                schema:
                  $ref: "course-schemas.yaml#/components/schemas/CourseResponse"
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "403": { $ref: "common.yaml#/components/responses/Forbidden" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
          "409": { $ref: "common.yaml#/components/responses/Conflict" }
    /api/courses/{courseId}/archive:
      post:
        tags: [course]
        operationId: archiveCourse
        summary: Archive a course (retire it; read-only thereafter)
        parameters:
          - $ref: "#/components/parameters/CourseId"
        responses:
          "200":
            description: Archived
            content:
              application/json:
                schema:
                  $ref: "course-schemas.yaml#/components/schemas/CourseResponse"
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "403": { $ref: "common.yaml#/components/responses/Forbidden" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
          "409": { $ref: "common.yaml#/components/responses/Conflict" }
  ```

- [ ] **Step 4: Regenerate + sanity compile.** `cd backend && mvn -pl course -am generate-sources`. Confirm `de.codillas.course.api.dto.CourseStatus` and the new `CourseApi.publishCourse/archiveCourse` + `listCourses(CourseStatus, Pageable)` exist. The build is RED (controller no longer implements every `CourseApi` method) — fixed in Task 4.

- [ ] **Step 5: Commit.** `git add backend/openapi/course-*.yaml && git commit -m "feat(course): OpenAPI — course status, publish/archive, status filter"`

---

### Task 2: `CourseStatus` + `Course.status` + `CourseStateMachine` (unit-tested)

**Files:**
- Create: `backend/course/src/main/java/de/codillas/course/domain/model/CourseStatus.java`
- Create: `backend/course/src/main/java/de/codillas/course/domain/CourseStateMachine.java`
- Create: `backend/course/src/test/java/de/codillas/course/domain/CourseStateMachineTest.java`
- Modify: `backend/course/src/main/java/de/codillas/course/domain/model/Course.java`

- [ ] **Step 1 (RED): `CourseStateMachineTest`.** Allowed: `DRAFT→PUBLISHED`, `DRAFT→ARCHIVED`, `PUBLISHED→ARCHIVED`. Everything else 409.

  ```java
  package de.codillas.course.domain;

  import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
  import static org.assertj.core.api.Assertions.assertThatNoException;
  import static org.assertj.core.api.Assertions.assertThat;

  import de.codillas.course.domain.model.Course;
  import de.codillas.course.domain.model.CourseStatus;
  import de.codillas.shared.exception.ConflictException;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("CourseStateMachine")
  class CourseStateMachineTest {

    private final CourseStateMachine stateMachine = new CourseStateMachine();

    private static Course inStatus(CourseStatus status) {
      return Course.builder().name("c").status(status).build();
    }

    @Test
    @DisplayName("DRAFT can be published or archived")
    void draftTransitions() {
      Course published = inStatus(CourseStatus.DRAFT);
      stateMachine.transitionTo(published, CourseStatus.PUBLISHED);
      assertThat(published.getStatus()).isEqualTo(CourseStatus.PUBLISHED);

      assertThatNoException()
          .isThrownBy(
              () -> stateMachine.transitionTo(inStatus(CourseStatus.DRAFT), CourseStatus.ARCHIVED));
    }

    @Test
    @DisplayName("PUBLISHED can be archived")
    void publishedToArchived() {
      Course archived = inStatus(CourseStatus.PUBLISHED);
      stateMachine.transitionTo(archived, CourseStatus.ARCHIVED);
      assertThat(archived.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
    }

    @Test
    @DisplayName("re-publishing, un-archiving and un-publishing are conflicts")
    void illegalTransitions_conflict() {
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(
              () -> stateMachine.transitionTo(inStatus(CourseStatus.PUBLISHED), CourseStatus.PUBLISHED));
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(
              () -> stateMachine.transitionTo(inStatus(CourseStatus.PUBLISHED), CourseStatus.DRAFT));
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(
              () -> stateMachine.transitionTo(inStatus(CourseStatus.ARCHIVED), CourseStatus.PUBLISHED));
    }
  }
  ```

- [ ] **Step 2: `CourseStatus`.**

  ```java
  package de.codillas.course.domain.model;

  /** A course is editable while DRAFT, selectable for groups once PUBLISHED, retired when ARCHIVED. */
  public enum CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
  }
  ```

- [ ] **Step 3: `CourseStateMachine`** (copy the `TestStateMachine` shape).

  ```java
  package de.codillas.course.domain;

  import java.util.EnumSet;
  import java.util.Map;
  import java.util.Set;

  import org.springframework.stereotype.Component;

  import de.codillas.course.domain.model.Course;
  import de.codillas.course.domain.model.CourseStatus;
  import de.codillas.shared.domain.StateMachines;

  /** Declarative state machine for {@link Course}: DRAFT → PUBLISHED → ARCHIVED (DRAFT may also be archived). */
  @Component
  public class CourseStateMachine {

    private static final Map<CourseStatus, Set<CourseStatus>> ALLOWED =
        Map.of(
            CourseStatus.DRAFT, EnumSet.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED),
            CourseStatus.PUBLISHED, EnumSet.of(CourseStatus.ARCHIVED));

    public void transitionTo(Course course, CourseStatus target) {
      StateMachines.transition("course", course.getStatus(), target, ALLOWED, course::setStatus);
    }
  }
  ```

- [ ] **Step 4: `Course.status`** (copy `assessment.Test`'s status block verbatim).

  ```java
  import jakarta.persistence.EnumType;
  import jakarta.persistence.Enumerated;
  import lombok.Builder;
  // ...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;
  ```

- [ ] **Step 5: GREEN.** `cd backend && mvn -pl course test -Dtest=CourseStateMachineTest`. (Whole-module compile is still RED until Task 4 wires the controller — that's fine; run the focused unit test.)

- [ ] **Step 6: Commit.** `git commit -am "feat(course): CourseStatus enum + Course.status + CourseStateMachine"`

---

### Task 3: Liquibase — `courses.status` with PUBLISHED backfill

**Files:**
- Create: `backend/course/src/main/resources/db/changelog/changes/0.3.0/0.3.0-add-course-status.yaml`
- Modify: `backend/course/src/main/resources/db/changelog/course-changelog.yaml`

- [ ] **Step 1: changeset.** Runs after the 0.1.0 seed, so the two seeded courses are backfilled to `PUBLISHED`.

  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-add-course-status
        author: codillas
        comment: >-
          Add courses.status lifecycle column. Rows that predate the lifecycle were already live, so
          they are BACKFILLED to PUBLISHED; the DB default (and the entity @Builder.Default) make NEW
          rows DRAFT. defaultValue is not used on addColumn so the backfill is not overwritten. 0.3.0.
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: courses
                  columnName: status
        changes:
          - addColumn:
              tableName: courses
              columns:
                - column:
                    name: status
                    type: varchar(20)
          - update:
              tableName: courses
              columns:
                - column:
                    name: status
                    value: PUBLISHED
              where: status IS NULL
          - addDefaultValue:
              tableName: courses
              columnName: status
              defaultValue: DRAFT
          - addNotNullConstraint:
              tableName: courses
              columnName: status
              columnDataType: varchar(20)
  ```

- [ ] **Step 2: include it** in `course-changelog.yaml` (append, in version order):

  ```yaml
    # 0.3.0 — course lifecycle status
    - include:
        file: changes/0.3.0/0.3.0-add-course-status.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 3: Commit.** `git commit -am "feat(course): Liquibase courses.status (backfill existing → PUBLISHED)"`

---

### Task 4: Course service/port/mapper/controller wiring

**Files:**
- Create: `backend/course/src/main/java/de/codillas/course/CourseCatalog.java`
- Create: `backend/course/src/main/java/de/codillas/course/service/CourseCatalogImpl.java`
- Modify: `CourseService.java`, `CourseServiceImpl.java`, `CourseRepository.java`, `CourseMapper.java`, `CourseController.java`

- [ ] **Step 1: `CourseCatalog` read port** (exposed base-package type — the only thing `enrollment` may import from `course`).

  ```java
  package de.codillas.course;

  import java.util.UUID;

  /**
   * Cross-module read port: lets another module ask, by id, whether a course is PUBLISHED, without
   * importing course internals. Kept in the module's base package so it is an exposed Modulith type.
   */
  public interface CourseCatalog {
    boolean isPublished(UUID courseId);
  }
  ```

- [ ] **Step 2: `CourseCatalogImpl`** (package-private; only the interface crosses the boundary).

  ```java
  package de.codillas.course.service;

  import java.util.UUID;

  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import de.codillas.course.CourseCatalog;
  import de.codillas.course.domain.model.CourseStatus;
  import de.codillas.course.domain.repository.CourseRepository;

  import lombok.RequiredArgsConstructor;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  class CourseCatalogImpl implements CourseCatalog {

    private final CourseRepository courseRepository;

    @Override
    public boolean isPublished(UUID courseId) {
      return courseRepository
          .findById(courseId)
          .map(c -> c.getStatus() == CourseStatus.PUBLISHED)
          .orElse(false);
    }
  }
  ```

- [ ] **Step 3: `CourseRepository`** — add status finders.

  ```java
  import java.util.Collection;
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.Pageable;
  import de.codillas.course.domain.model.CourseStatus;
  // ...
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByStatusIn(Collection<CourseStatus> statuses, Pageable pageable);
  ```

- [ ] **Step 4: `CourseMapper`** — map the generated dto enum → domain enum (MapStruct matches by name; `nullValueCheckStrategy = ALWAYS` from `CentralMapperConfig` keeps it null-safe). `toResponse(Course)` already maps domain → dto status automatically.

  ```java
    de.codillas.course.domain.model.CourseStatus toDomainStatus(
        de.codillas.course.api.dto.CourseStatus status);
  ```

- [ ] **Step 5: `CourseService`** — extend the interface.

  ```java
  import de.codillas.course.api.dto.CourseStatus;
  // ...
    CourseListResponse listCourses(CourseStatus status, Pageable pageable);

    CourseResponse publishCourse(UUID courseId);

    CourseResponse archiveCourse(UUID courseId);
  ```

- [ ] **Step 6: `CourseServiceImpl`** — transitions + status-filtered, staff-aware list. Inject `CourseStateMachine` and `CurrentUser`.

  ```java
  // new fields
  private final CourseStateMachine stateMachine;
  private final de.codillas.shared.security.CurrentUser currentUser;

  @Override
  @Transactional
  public CourseResponse publishCourse(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    stateMachine.transitionTo(course, de.codillas.course.domain.model.CourseStatus.PUBLISHED);
    return courseMapper.toResponse(courseRepository.save(course));
  }

  @Override
  @Transactional
  public CourseResponse archiveCourse(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    stateMachine.transitionTo(course, de.codillas.course.domain.model.CourseStatus.ARCHIVED);
    return courseMapper.toResponse(courseRepository.save(course));
  }

  @Override
  public CourseListResponse listCourses(
      de.codillas.course.api.dto.CourseStatus status, Pageable pageable) {
    var filter = courseMapper.toDomainStatus(status); // null when no filter
    Page<Course> page;
    if (currentUser.isStaff()) {
      page =
          filter == null
              ? courseRepository.findAll(pageable)
              : courseRepository.findByStatus(filter, pageable);
    } else if (filter == de.codillas.course.domain.model.CourseStatus.DRAFT) {
      page = Page.empty(pageable); // non-staff never see drafts
    } else if (filter == null) {
      page =
          courseRepository.findByStatusIn(
              java.util.EnumSet.of(
                  de.codillas.course.domain.model.CourseStatus.PUBLISHED,
                  de.codillas.course.domain.model.CourseStatus.ARCHIVED),
              pageable);
    } else {
      page = courseRepository.findByStatus(filter, pageable);
    }
    return courseMapper.toListResponse(page);
  }
  ```

  Add the `org.springframework.data.domain.Page` import.

- [ ] **Step 7: `CourseController`** — pass the filter through; gate the two lifecycle endpoints `@RequiresAdmin` (matches `createCourse`).

  ```java
  @Override
  @RequiresAuthenticated
  public ResponseEntity<CourseListResponse> listCourses(
      de.codillas.course.api.dto.CourseStatus status, Pageable pageable) {
    return ResponseEntity.ok(service.listCourses(status, pageable));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> publishCourse(UUID courseId) {
    return ResponseEntity.ok(service.publishCourse(courseId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<CourseResponse> archiveCourse(UUID courseId) {
    return ResponseEntity.ok(service.archiveCourse(courseId));
  }
  ```

  (The generated `listCourses` signature now leads with `CourseStatus status` — update the `@Override` to match.)

- [ ] **Step 8: GREEN.** `cd backend && mvn -pl course -am test && mvn spotless:apply`.

- [ ] **Step 9: Commit.** `git commit -am "feat(course): publish/archive + status-filtered list + CourseCatalog port"`

---

### Task 5: Course controller integration tests

**Files:**
- Create: `backend/main/src/test/java/de/codillas/integration/course/CourseLifecycleControllerIntegrationTest.java`

- [ ] **Step 1: Write the test** (publish/archive happy path + role gating + DRAFT hidden from non-staff). `BaseIntegrationTest` boots the real app + Postgres; `jwt()` mocks roles.

  ```java
  package de.codillas.integration.course;

  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.http.MediaType;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
  import org.springframework.test.web.servlet.MockMvc;

  import de.codillas.integration.BaseIntegrationTest;

  import com.jayway.jsonpath.JsonPath;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("Course lifecycle endpoints (integration)")
  class CourseLifecycleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor withRole(String role) {
      return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String createDraftCourse(String name) throws Exception {
      String body =
          mockMvc
              .perform(
                  post("/api/courses")
                      .with(withRole("ADMIN"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"name\":\"%s\"}".formatted(name)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.status").value("DRAFT"))
              .andReturn()
              .getResponse()
              .getContentAsString();
      return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("ADMIN publishes then archives a course (200 each, status advances)")
    void publishThenArchive_asAdmin() throws Exception {
      String id = createDraftCourse("Lifecycle A");

      mockMvc
          .perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PUBLISHED"));

      mockMvc
          .perform(post("/api/courses/{id}/archive", id).with(withRole("ADMIN")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("publishing twice is a 409 conflict")
    void republish_conflicts() throws Exception {
      String id = createDraftCourse("Lifecycle B");
      mockMvc.perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")));
      mockMvc
          .perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("publish without the ADMIN role returns 403")
    void publish_asStudent_returns403() throws Exception {
      String id = createDraftCourse("Lifecycle C");
      mockMvc
          .perform(post("/api/courses/{id}/publish", id).with(withRole("STUDENT")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/courses as STUDENT never returns a DRAFT course")
    void listCourses_asStudent_excludesDraft() throws Exception {
      String draftId = createDraftCourse("Hidden Draft");
      mockMvc
          .perform(get("/api/courses").param("size", "200").with(withRole("STUDENT")))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.content[?(@.id == '%s')]".formatted(draftId)).isEmpty());
    }
  }
  ```

- [ ] **Step 2: Run.** `cd backend && mvn -pl main -am test -Dtest=CourseLifecycleControllerIntegrationTest`.

- [ ] **Step 3: Commit.** `git commit -am "test(course): lifecycle controller integration (publish/archive, gating, draft hidden)"`

---

### Task 6: Group lifecycle — OpenAPI spec

**Files:**
- Modify: `backend/openapi/enrollment-paths.yaml`, `backend/openapi/enrollment-schemas.yaml`

- [ ] **Step 1: `enrollment-schemas.yaml`** — bump `info.version` to `0.3.0`; add the enum and `status` (required) on `GroupResponse`.

  ```yaml
  GroupStatus:
    type: string
    description: DRAFT is being set up; RUNNING is an active cohort; ARCHIVED is historical (read-only).
    enum:
      - DRAFT
      - RUNNING
      - ARCHIVED
  ```

  In `GroupResponse`: add `status: { $ref: "#/components/schemas/GroupStatus" }` to `properties` and `status` to `required`.

- [ ] **Step 2: `enrollment-paths.yaml`** — bump `info.version` to `0.3.0`; add a `status` query filter to `listGroups` (same block as course); add `"409"` to `createGroup` responses (`$ref: "common.yaml#/components/responses/Conflict"`); add start + archive:

  ```yaml
    /api/groups/{groupId}/start:
      post:
        tags: [enrollment]
        operationId: startGroup
        summary: Start a draft group (mark the cohort RUNNING)
        parameters:
          - name: groupId
            in: path
            required: true
            schema: { type: string, format: uuid }
        responses:
          "200":
            description: Running
            content:
              application/json:
                schema:
                  $ref: "enrollment-schemas.yaml#/components/schemas/GroupResponse"
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "403": { $ref: "common.yaml#/components/responses/Forbidden" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
          "409": { $ref: "common.yaml#/components/responses/Conflict" }
    /api/groups/{groupId}/archive:
      post:
        tags: [enrollment]
        operationId: archiveGroup
        summary: Archive a group (retire the cohort; read-only thereafter)
        parameters:
          - name: groupId
            in: path
            required: true
            schema: { type: string, format: uuid }
        responses:
          "200":
            description: Archived
            content:
              application/json:
                schema:
                  $ref: "enrollment-schemas.yaml#/components/schemas/GroupResponse"
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "403": { $ref: "common.yaml#/components/responses/Forbidden" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
          "409": { $ref: "common.yaml#/components/responses/Conflict" }
  ```

  And the `listGroups` filter param:

  ```yaml
        - name: status
          in: query
          required: false
          schema:
            $ref: "enrollment-schemas.yaml#/components/schemas/GroupStatus"
  ```

- [ ] **Step 3: Regenerate.** `cd backend && mvn -pl enrollment -am generate-sources`. Confirm `GroupStatus`, `EnrollmentApi.startGroup/archiveGroup`, `listGroups(GroupStatus, Pageable)`. Build RED until Task 9.

- [ ] **Step 4: Commit.** `git add backend/openapi/enrollment-*.yaml && git commit -m "feat(enrollment): OpenAPI — group status, start/archive, status filter, createGroup 409"`

---

### Task 7: `GroupStatus` + `Group.status` + `GroupStateMachine` (unit-tested)

**Files:**
- Create: `domain/model/GroupStatus.java`, `domain/GroupStateMachine.java`, `src/test/.../domain/GroupStateMachineTest.java`
- Modify: `domain/model/Group.java`

- [ ] **Step 1 (RED): `GroupStateMachineTest`.** Allowed: `DRAFT→RUNNING`, `DRAFT→ARCHIVED`, `RUNNING→ARCHIVED`. `assertWritable` passes for DRAFT/RUNNING, 409 for ARCHIVED.

  ```java
  package de.codillas.enrollment.domain;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
  import static org.assertj.core.api.Assertions.assertThatNoException;

  import de.codillas.enrollment.domain.model.Group;
  import de.codillas.enrollment.domain.model.GroupStatus;
  import de.codillas.shared.exception.ConflictException;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("GroupStateMachine")
  class GroupStateMachineTest {

    private final GroupStateMachine stateMachine = new GroupStateMachine();

    private static Group inStatus(GroupStatus status) {
      return Group.builder().name("g").courseId(java.util.UUID.randomUUID())
          .teacherId(java.util.UUID.randomUUID()).status(status).build();
    }

    @Test
    @DisplayName("DRAFT can start or be archived; RUNNING can be archived")
    void legalTransitions() {
      Group running = inStatus(GroupStatus.DRAFT);
      stateMachine.transitionTo(running, GroupStatus.RUNNING);
      assertThat(running.getStatus()).isEqualTo(GroupStatus.RUNNING);

      assertThatNoException()
          .isThrownBy(() -> stateMachine.transitionTo(inStatus(GroupStatus.DRAFT), GroupStatus.ARCHIVED));
      assertThatNoException()
          .isThrownBy(() -> stateMachine.transitionTo(inStatus(GroupStatus.RUNNING), GroupStatus.ARCHIVED));
    }

    @Test
    @DisplayName("un-archiving and restarting are conflicts")
    void illegalTransitions_conflict() {
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> stateMachine.transitionTo(inStatus(GroupStatus.ARCHIVED), GroupStatus.RUNNING));
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> stateMachine.transitionTo(inStatus(GroupStatus.RUNNING), GroupStatus.DRAFT));
    }

    @Test
    @DisplayName("only a non-archived group is writable")
    void assertWritable_rejectsArchived() {
      assertThatNoException().isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.DRAFT)));
      assertThatNoException().isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.RUNNING)));
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.ARCHIVED)));
    }
  }
  ```

- [ ] **Step 2: `GroupStatus`.**

  ```java
  package de.codillas.enrollment.domain.model;

  /** A cohort is DRAFT while set up, RUNNING while active, ARCHIVED (read-only) once finished. */
  public enum GroupStatus {
    DRAFT,
    RUNNING,
    ARCHIVED
  }
  ```

- [ ] **Step 3: `GroupStateMachine`** (transitions + the write guard, mirroring `SubmissionStateMachine.assertEditable`).

  ```java
  package de.codillas.enrollment.domain;

  import java.util.EnumSet;
  import java.util.Map;
  import java.util.Set;

  import org.springframework.stereotype.Component;

  import de.codillas.enrollment.domain.model.Group;
  import de.codillas.enrollment.domain.model.GroupStatus;
  import de.codillas.shared.domain.StateMachines;
  import de.codillas.shared.exception.ConflictException;

  /** Declarative state machine for {@link Group}: DRAFT → RUNNING → ARCHIVED. ARCHIVED is read-only. */
  @Component
  public class GroupStateMachine {

    private static final Map<GroupStatus, Set<GroupStatus>> ALLOWED =
        Map.of(
            GroupStatus.DRAFT, EnumSet.of(GroupStatus.RUNNING, GroupStatus.ARCHIVED),
            GroupStatus.RUNNING, EnumSet.of(GroupStatus.ARCHIVED));

    public void transitionTo(Group group, GroupStatus target) {
      StateMachines.transition("group", group.getStatus(), target, ALLOWED, group::setStatus);
    }

    /** Enrolment and scheduling are rejected once a group is archived. */
    public void assertWritable(Group group) {
      if (group.getStatus() == GroupStatus.ARCHIVED) {
        throw new ConflictException("Cannot modify an ARCHIVED group");
      }
    }
  }
  ```

- [ ] **Step 4: `Group.status`** (same block as `Course`).

  ```java
  import jakarta.persistence.EnumType;
  import jakarta.persistence.Enumerated;
  import lombok.Builder;
  // ...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupStatus status = GroupStatus.DRAFT;
  ```

- [ ] **Step 5: GREEN.** `cd backend && mvn -pl enrollment test -Dtest=GroupStateMachineTest`.

- [ ] **Step 6: Commit.** `git commit -am "feat(enrollment): GroupStatus enum + Group.status + GroupStateMachine"`

---

### Task 8: Liquibase — `study_groups.status` with RUNNING backfill

**Files:**
- Create: `backend/enrollment/src/main/resources/db/changelog/changes/0.3.0/0.3.0-add-group-status.yaml`
- Modify: `backend/enrollment/src/main/resources/db/changelog/enrollment-changelog.yaml`

- [ ] **Step 1: changeset** (same shape as Task 3; the seeded "Java Cohort A" backfills to `RUNNING`, so it keeps showing in `/api/me/groups`).

  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-add-group-status
        author: codillas
        comment: >-
          Add study_groups.status. Pre-lifecycle rows were active cohorts, so they are BACKFILLED to
          RUNNING; DB default + entity @Builder.Default make NEW rows DRAFT. No defaultValue on
          addColumn so the backfill survives. 0.3.0.
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: study_groups
                  columnName: status
        changes:
          - addColumn:
              tableName: study_groups
              columns:
                - column:
                    name: status
                    type: varchar(20)
          - update:
              tableName: study_groups
              columns:
                - column:
                    name: status
                    value: RUNNING
              where: status IS NULL
          - addDefaultValue:
              tableName: study_groups
              columnName: status
              defaultValue: DRAFT
          - addNotNullConstraint:
              tableName: study_groups
              columnName: status
              columnDataType: varchar(20)
  ```

- [ ] **Step 2: include it** in `enrollment-changelog.yaml`:

  ```yaml
    # 0.3.0 — group lifecycle status
    - include:
        file: changes/0.3.0/0.3.0-add-group-status.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 3: Commit.** `git commit -am "feat(enrollment): Liquibase study_groups.status (backfill existing → RUNNING)"`

---

### Task 9: Enrollment service/mapper/controller wiring + writer enforcement

**Files:**
- Modify: `GroupRepository.java`, `GroupMapper.java`, `GroupService.java`, `GroupServiceImpl.java`, `MembershipServiceImpl.java`, `ScheduledLessonServiceImpl.java`, `MeServiceImpl.java`, `EnrollmentController.java`

- [ ] **Step 1: `GroupRepository`** — status finders + a status-scoped `/me/groups` finder.

  ```java
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.Pageable;
  import de.codillas.enrollment.domain.model.GroupStatus;
  // ...
    Page<Group> findByStatus(GroupStatus status, Pageable pageable);

    List<Group> findByIdInAndStatusIn(Collection<UUID> ids, Collection<GroupStatus> statuses, Sort sort);
  ```

- [ ] **Step 2: `GroupMapper`** — dto enum → domain enum (response direction is already automatic).

  ```java
    de.codillas.enrollment.domain.model.GroupStatus toDomainStatus(
        de.codillas.enrollment.api.dto.GroupStatus status);
  ```

- [ ] **Step 3: `GroupService`** — extend.

  ```java
  import de.codillas.enrollment.api.dto.GroupStatus;
  import java.util.UUID;
  // ...
    GroupListResponse listGroups(GroupStatus status, Pageable pageable);

    GroupResponse startGroup(UUID groupId);

    GroupResponse archiveGroup(UUID groupId);
  ```

- [ ] **Step 4: `GroupServiceImpl`** — inject `CourseCatalog`, `GroupStateMachine`; add a `findByIdOrThrow`; gate create on a PUBLISHED course (→ 409).

  ```java
  // new imports
  import de.codillas.course.CourseCatalog;
  import de.codillas.enrollment.domain.GroupStateMachine;
  import de.codillas.enrollment.domain.model.Group;
  import de.codillas.enrollment.domain.model.GroupStatus;
  import de.codillas.shared.exception.ConflictException;
  import de.codillas.shared.exception.NotFoundException;
  import java.util.UUID;

  // new fields
  private final CourseCatalog courseCatalog;
  private final GroupStateMachine stateMachine;

  @Override
  @Transactional
  public GroupResponse createGroup(CreateGroupRequest request) {
    if (!courseCatalog.isPublished(request.getCourseId())) {
      throw new ConflictException("A group can only be created for a PUBLISHED course");
    }
    return mapper.toResponse(repository.save(mapper.toEntity(request)));
  }

  @Override
  public GroupListResponse listGroups(
      de.codillas.enrollment.api.dto.GroupStatus status, Pageable pageable) {
    return mapper.toListResponse(
        status == null
            ? repository.findAll(pageable)
            : repository.findByStatus(mapper.toDomainStatus(status), pageable));
  }

  @Override
  @Transactional
  public GroupResponse startGroup(UUID groupId) {
    Group group = findByIdOrThrow(groupId);
    stateMachine.transitionTo(group, GroupStatus.RUNNING);
    return mapper.toResponse(repository.save(group));
  }

  @Override
  @Transactional
  public GroupResponse archiveGroup(UUID groupId) {
    Group group = findByIdOrThrow(groupId);
    stateMachine.transitionTo(group, GroupStatus.ARCHIVED);
    return mapper.toResponse(repository.save(group));
  }

  private Group findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Group", id));
  }
  ```

- [ ] **Step 5: `MembershipServiceImpl`** — load the group, reject if archived. Inject `GroupRepository` + `GroupStateMachine`.

  ```java
  // new fields
  private final GroupRepository groupRepository;
  private final GroupStateMachine groupStateMachine;

  @Override
  @Transactional
  public MembershipResponse enrollStudent(UUID groupId, EnrollStudentRequest request) {
    Group group =
        groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
    groupStateMachine.assertWritable(group);
    Membership saved = repository.save(mapper.toEntity(request, groupId));
    events.publishEvent(new StudentEnrolled(groupId, request.getUserId()));
    return mapper.toResponse(saved);
  }
  ```

  (Add imports for `Group`, `GroupRepository`, `GroupStateMachine`, `NotFoundException`.)

- [ ] **Step 6: `ScheduledLessonServiceImpl`** — identical guard before `scheduleLesson` saves.

  ```java
  private final GroupRepository groupRepository;
  private final GroupStateMachine groupStateMachine;

  @Override
  @Transactional
  public ScheduledLessonResponse scheduleLesson(UUID groupId, ScheduleLessonRequest request) {
    Group group =
        groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
    groupStateMachine.assertWritable(group);
    return mapper.toResponse(repository.save(mapper.toEntity(request, groupId)));
  }
  ```

- [ ] **Step 7: `MeServiceImpl`** — `/api/me/groups` returns RUNNING+ARCHIVED only (schedule is unchanged).

  ```java
  import de.codillas.enrollment.domain.model.GroupStatus;
  import java.util.EnumSet;
  // ...
  @Override
  public List<GroupResponse> listMyGroups() {
    List<UUID> groupIds = myGroupIds();
    if (groupIds.isEmpty()) {
      return List.of();
    }
    return groupMapper.toResponseList(
        groupRepository.findByIdInAndStatusIn(
            groupIds, EnumSet.of(GroupStatus.RUNNING, GroupStatus.ARCHIVED), GroupRepository.BY_NAME));
  }
  ```

- [ ] **Step 8: `EnrollmentController`** — status param on `listGroups`; `start`/`archive` `@RequiresAdmin` (matches `createGroup`).

  ```java
  @Override
  @RequiresAuthenticated
  public ResponseEntity<GroupListResponse> listGroups(
      de.codillas.enrollment.api.dto.GroupStatus status, Pageable pageable) {
    return ResponseEntity.ok(groupService.listGroups(status, pageable));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<GroupResponse> startGroup(UUID groupId) {
    return ResponseEntity.ok(groupService.startGroup(groupId));
  }

  @Override
  @RequiresAdmin
  public ResponseEntity<GroupResponse> archiveGroup(UUID groupId) {
    return ResponseEntity.ok(groupService.archiveGroup(groupId));
  }
  ```

- [ ] **Step 9: GREEN.** `cd backend && mvn -pl enrollment -am test && mvn spotless:apply`. Then `mvn -q compile` for the whole backend.

- [ ] **Step 10: Commit.** `git commit -am "feat(enrollment): start/archive, published-course guard, archived-group write guard, /me/groups status filter"`

---

### Task 10: Enrollment integration tests (new) + fix the existing ones

The writer guards change two existing tests' assumptions: `createGroup` now needs a **published** course, and `enroll`/`schedule` now load the group (random ids → 404). Fix those, then add the lifecycle/negative tests.

**Files:**
- Create: `backend/main/src/test/java/de/codillas/integration/enrollment/GroupLifecycleControllerIntegrationTest.java`
- Modify: `EnrollmentControllerIntegrationTest.java`, `MeEnrollmentControllerIntegrationTest.java`

- [ ] **Step 1: Shared helper chain.** In each enrollment integration test, replace ad-hoc random-id group creation with a real chain. Add these helpers (admin-driven: create course → publish → create group → optionally start):

  ```java
  private String createPublishedCourse() throws Exception {
    String course =
        mockMvc
            .perform(
                post("/api/courses").with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
            .andReturn().getResponse().getContentAsString();
    String courseId = JsonPath.read(course, "$.id");
    mockMvc.perform(post("/api/courses/{id}/publish", courseId).with(withRole("ADMIN")))
        .andExpect(status().isOk());
    return courseId;
  }

  private UUID createGroup() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/groups").with(withRole("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Cohort A\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                            .formatted(createPublishedCourse(), UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(body, "$.id"));
  }

  private void startGroup(UUID groupId) throws Exception {
    mockMvc.perform(post("/api/groups/{id}/start", groupId).with(withRole("ADMIN")))
        .andExpect(status().isOk());
  }
  ```

- [ ] **Step 2: Update `EnrollmentControllerIntegrationTest`.** `createGroup_asAdmin_returns201` → create against a published course (use `createPublishedCourse()`); assert `$.status` == `"DRAFT"`. `enrollStudent_thenListMembers` and `scheduleLesson_thenList` → use `createGroup()` + `startGroup(...)` instead of `UUID.randomUUID()` for the group id. `createGroup_withoutAdmin_returns403` and `markAttendance_upserts` stay as-is (the 403 fires before the service; attendance is out of this batch's write-guard scope).

- [ ] **Step 3: Update `MeEnrollmentControllerIntegrationTest`.** Replace the `createGroup()` helper with the chain above **and call `startGroup(id)`** before returning — otherwise the new RUNNING+ARCHIVED filter on `/api/me/groups` would exclude the (DRAFT) group and the existing isolation/`listMyGroups`/`listMySchedule` assertions would see 0. (`/api/me/schedule` is unaffected by status, but the groups must be RUNNING to appear in `/api/me/groups`.)

- [ ] **Step 4: Write `GroupLifecycleControllerIntegrationTest`** — start/archive happy + gating, the two negative writer guards, and the `/me/groups` DRAFT exclusion.

  ```java
  package de.codillas.integration.enrollment;

  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import java.util.UUID;

  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.http.MediaType;
  import org.springframework.security.core.authority.SimpleGrantedAuthority;
  import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
  import org.springframework.test.web.servlet.MockMvc;

  import de.codillas.integration.BaseIntegrationTest;

  import com.jayway.jsonpath.JsonPath;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("Group lifecycle endpoints (integration)")
  class GroupLifecycleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor withRole(String role) {
      return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static JwtRequestPostProcessor as(UUID userId, String role) {
      return jwt().jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String createPublishedCourse() throws Exception {
      String course =
          mockMvc.perform(post("/api/courses").with(withRole("ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Course %s\"}".formatted(UUID.randomUUID())))
              .andReturn().getResponse().getContentAsString();
      String id = JsonPath.read(course, "$.id");
      mockMvc.perform(post("/api/courses/{id}/publish", id).with(withRole("ADMIN")))
          .andExpect(status().isOk());
      return id;
    }

    private UUID createGroupFor(String courseId) throws Exception {
      String body =
          mockMvc.perform(post("/api/groups").with(withRole("ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Cohort\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                      .formatted(courseId, UUID.randomUUID())))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.status").value("DRAFT"))
              .andReturn().getResponse().getContentAsString();
      return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    @Test
    @DisplayName("ADMIN starts then archives a group (status advances)")
    void startThenArchive_asAdmin() throws Exception {
      UUID group = createGroupFor(createPublishedCourse());
      mockMvc.perform(post("/api/groups/{id}/start", group).with(withRole("ADMIN")))
          .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
      mockMvc.perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
          .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("start without the ADMIN role returns 403")
    void start_asStudent_returns403() throws Exception {
      UUID group = createGroupFor(createPublishedCourse());
      mockMvc.perform(post("/api/groups/{id}/start", group).with(withRole("STUDENT")))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("creating a group against a DRAFT (unpublished) course is a 409")
    void createGroup_draftCourse_conflicts() throws Exception {
      String draft =
          JsonPath.read(
              mockMvc.perform(post("/api/courses").with(withRole("ADMIN"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"name\":\"Draft Course\"}"))
                  .andReturn().getResponse().getContentAsString(),
              "$.id");
      mockMvc.perform(post("/api/groups").with(withRole("ADMIN"))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"name\":\"X\",\"courseId\":\"%s\",\"teacherId\":\"%s\"}"
                  .formatted(draft, UUID.randomUUID())))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("enrolling into an ARCHIVED group is a 409")
    void enroll_archivedGroup_conflicts() throws Exception {
      UUID group = createGroupFor(createPublishedCourse());
      mockMvc.perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
          .andExpect(status().isOk());
      mockMvc.perform(post("/api/groups/{id}/members", group).with(withRole("ADMIN"))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"userId\":\"%s\"}".formatted(UUID.randomUUID())))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("scheduling a lesson in an ARCHIVED group is a 409")
    void schedule_archivedGroup_conflicts() throws Exception {
      UUID group = createGroupFor(createPublishedCourse());
      mockMvc.perform(post("/api/groups/{id}/archive", group).with(withRole("ADMIN")))
          .andExpect(status().isOk());
      mockMvc.perform(post("/api/groups/{id}/lessons", group).with(withRole("ADMIN"))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"title\":\"L\",\"scheduledAt\":\"2026-09-01T10:00:00Z\"}"))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("/api/me/groups excludes DRAFT groups and includes them once RUNNING")
    void myGroups_excludesDraftUntilStarted() throws Exception {
      UUID group = createGroupFor(createPublishedCourse());
      UUID student = UUID.randomUUID();
      mockMvc.perform(post("/api/groups/{id}/members", group).with(withRole("ADMIN"))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"userId\":\"%s\"}".formatted(student)))
          .andExpect(status().isCreated());

      mockMvc.perform(get("/api/me/groups").with(as(student, "STUDENT")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0)); // still DRAFT

      mockMvc.perform(post("/api/groups/{id}/start", group).with(withRole("ADMIN")))
          .andExpect(status().isOk());
      mockMvc.perform(get("/api/me/groups").with(as(student, "STUDENT")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].id").value(group.toString()));
    }
  }
  ```

- [ ] **Step 5: Run the suite + boundary check.** `cd backend && mvn -pl main -am test`. Confirm `ApplicationModules.of(...).verify()` (existing modulith test) stays green — as built there is **no** new `enrollment → course` edge (the read model is event-fed). `mvn spotless:apply`.

- [ ] **Step 6: Commit.** `git commit -am "test(enrollment): group lifecycle + writer guards integration; fix existing enrolment tests for the chain"`

---

## Self-Review

**Spec coverage (Batch 5 decisions):**
- Course `DRAFT/PUBLISHED/ARCHIVED` + `CourseStateMachine` + `publish`/`archive` → Tasks 1, 2, 4, 5. ✓
- DRAFT hidden from non-staff in list endpoints (via `CurrentUser.isStaff()`) → Task 4 Step 6, test Task 5 Step 1. ✓
- Group-create validates course is PUBLISHED → 409 (as built: via enrollment's event-fed `course_status_view` read model, **not** `CourseCatalog`) → Task 9 Step 4 + amendment, test Task 10. ✓
- Group `DRAFT/RUNNING/ARCHIVED` + `GroupStateMachine` + `start`/`archive` → Tasks 6, 7, 9, 10. ✓
- Enroll + schedule writers reject when ARCHIVED (409) → Task 9 Steps 5–6, tests Task 10. ✓
- `/api/me/groups` returns RUNNING+ARCHIVED (not DRAFT) → Task 9 Step 7, test Task 10 Step 4. ✓
- Liquibase status columns, default DRAFT new rows, backfill existing → PUBLISHED course / RUNNING group, stated in the changeset comment → Tasks 3, 8. ✓
- `status` filter on list endpoints, enforced in writer services not just UI → Tasks 4/6/9. ✓
- State machines unit-tested in isolation (illegal transitions) → Tasks 2, 7. ✓
- Controller integration tests (publish/archive happy + gating; group-create vs DRAFT course; write vs ARCHIVED group) → Tasks 5, 10. ✓

**Convention compliance:** API-first (spec → regen → TDD); anemic entities + data-driven `<Aggregate>StateMachine` via `StateMachines.transition`; thin controllers `implements *Api` with `shared.security` annotations; MapStruct enum mapping (no hand conversion); `findByIdOrThrow` + `NotFoundException`/`ConflictException` → `ProblemDetail`; Liquibase versioned changeset under `0.3.0/` with `not columnExists` idempotency precondition; `ddl-auto=validate` honoured (validate ignores DB defaults; `varchar(20)`/`NOT NULL` match the `@Enumerated(STRING)` column). ✓

**Boundary safety (as built):** there is **no** new cross-module dependency — `enrollment` and `course` stay coupled only through `shared` events (`CoursePublished`/`CourseArchived`/`CourseDeleted`) and by-id references. The `enrollment → course` edge the plan originally introduced via `CourseCatalog` was removed → `ApplicationModules.verify()` stays green with no new edge. ✓

**Placeholder scan:** no TBD/TODO; every code/test/YAML step is concrete. ✓

**Decision the spec left open (flagged) — RESOLVED as event-fed:** how `enrollment` learns a course's status across the boundary. The plan originally chose a synchronous exposed read port `CourseCatalog.isPublished(courseId)`. **As built this was reversed:** `enrollment` keeps an **event-fed `course_status_view` read model** (consuming `CoursePublished`/`CourseArchived`/`CourseDeleted`) and checks it by id — the decoupling rule (no `codillas-course` dependency, no `de.codillas.course.*` import, no new Modulith edge) outweighs the synchronous port's simplicity. The cost (new table + async delivery + Awaitility in the integration tests) is accepted; `course` republishes `CoursePublished` for existing PUBLISHED courses on startup so the read model self-heals and there is no permanent eventual-consistency gap. Sub-decisions: group-create rejection uses **409 Conflict** (not 400) to match the lifecycle-state-conflict theme; `publish`/`archive`/`start`/`archive` are gated `@RequiresAdmin` (matching `createCourse`/`createGroup`); `ARCHIVED` is terminal and `DRAFT` may be archived directly (discard); `getCourse`-by-id detail is **not** DRAFT-hidden (only list endpoints are, per the spec wording).
