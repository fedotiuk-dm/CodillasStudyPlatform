# P2b — Grading model: late penalties, rubrics, final course grade (implementation plan)

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make grading real. `homework` gains a configurable **late penalty** (a pure `GradeCalculator` that turns a raw score into an `effectiveScore`) and an optional **rubric** (ordered criteria, grade = Σ criterion scores). The shared `SubmissionGraded`/`AttemptCompleted` events start carrying `awarded` + `maxPoints` + `groupId`. `gradebook` records those, and computes a **final course grade** = points-weighted percent `Σawarded / Σmax` per `(student, group)` with a per-type (homework vs test) breakdown.

**Architecture:** Implements **Batch 4** of `docs/superpowers/specs/2026-06-28-lms-hardening-design.md` (authoritative — its Decisions table governs every choice here). Two pure, unit-tested calculator beans (`homework.GradeCalculator`, `gradebook.WeightedGradeCalculator`) hold the only non-trivial math; everything else is the standard thin controller → `@Transactional` service → repository → anemic `@Entity` chain. The grade path is shared by `homework` and `gradebook` via the widened domain events; `gradebook` stays an event-fed read model that never joins into another module.

**Tech stack:** Spring Modulith, Maven multi-module, Java 25, OpenAPI-generated interfaces + DTOs, MapStruct (`CentralMapperConfig`), Liquibase per module (`ddl-auto=validate`), JUnit 5 + Mockito (module unit tests), Testcontainers Postgres + Awaitility (`main` integration tests).

## Global Constraints

- **Binding contracts:** `backend/AGENTS.md` chain (global → `homework`/`gradebook`/`assessment`/`shared`). This plan references it; it does not restate it. API-first; generated code is read-only; layering, MapStruct rules, event rules, Liquibase rules, TDD rules all per AGENTS.md.
- **API-first:** every endpoint/field below starts as an OpenAPI edit under `backend/openapi/`, then regenerate (`mvn -pl <module> generate-sources` or the module build), then TDD beneath the generated interface. Times are `Instant`; nullable is jspecify; errors `$ref` `common.yaml`.
- **Pure math is a bean, unit-tested in isolation** (no Spring, no DB): `GradeCalculator` and `WeightedGradeCalculator`, mirroring `AttemptGrader`/state-machine convention.
- **Events:** publish with `ApplicationEventPublisher` inside the writer's transaction; consume with `@ApplicationModuleListener`. The widened records live in `shared/event/`. `gradebook` read model owns its tables; **no cross-module joins** — the `(student → group)` lookup uses `gradebook`'s own `gradebook_membership` read model.
- **Versioning — ONE reactor version:** do **NOT** touch any module's Maven `<version>` (a module has none — it inherits the parent `0.2.0-SNAPSHOT`; `main` uses `${project.version}`). Only bump each touched **OpenAPI** `info.version` one minor (its own track), and name new Liquibase change dirs by semver (`changes/0.3.0/` for homework, etc.). A per-module Maven bump breaks the single-reactor pattern — never do it.
- **Depends on P1 (`2026-06-28-p1-correctness`), assumed already merged.** This plan **assumes these exist** and does not redefine them:
  - `homework.Submission` has `boolean late` and `Instant submittedAt` (set at submit time).
  - `homework.SubmissionServiceImpl.gradeSubmission` already **upserts** the `Grade` by `submissionId` (find-or-create), and loads the owner-checked submission via `findByIdForCaller(id, caller)` (Batch 1). Where this plan shows `gradeSubmission`, it **extends** that method; keep the P1 ownership/upsert shape.
  - `shared.security.CurrentUser` has `isStaff()` / `hasRole(Role)` (Batch 1).
- **TDD:** write the failing test first (RED), then minimal code (GREEN). Unit tests live in the module (Mockito); integration tests live in `main` (`BaseIntegrationTest`, Testcontainers). Every test class + method carries a `@DisplayName`.
- **`ApplicationModules.of(...).verify()` stays green.** No new cross-module imports; the only cross-module surface added is event fields. Format with `mvn spotless:apply`. Commit per task with a `feat(...)` / `test(...)` message scoped to the module. **Do not push.**
- **Verify each task:** `cd backend && mvn -pl <module> -am test` (module + deps), `mvn -q compile` (whole backend), `mvn spotless:check`.

## The one cross-module wrinkle — how each event learns `groupId`

The widened events carry `groupId` so `gradebook` can group the final grade by `(student, group)`.

- **homework** knows the group cleanly: `Assignment.groupId` is mandatory, so `SubmissionGraded.groupId = assignment.getGroupId()`. Always populated.
- **assessment** does **not** know the student's group and must not learn it: a `Test` is `lessonId`-scoped and reused across cohorts (it has no single group), and `assessment` depends only on `course`/`user` — **not** `enrollment`. **Decision:** `assessment` publishes `AttemptCompleted.groupId = null`; `gradebook` backfills a null `groupId` from its **own `gradebook_membership` read model** (already fed by `StudentEnrolled`) keyed by `studentId`. The event field still exists per the spec; homework fills it, assessment leaves it null, the read model that already owns `student↔group` resolves it. A student in multiple groups resolves to their (single membership) group; multi-group precision is deferred (documented, like Batch 1's group-scope note).

## File Structure

```
backend/
  shared/
    src/main/java/de/codillas/shared/event/
      SubmissionGraded.java                    MOD  — + awarded, maxPoints, groupId (replaces score)
      AttemptCompleted.java                    MOD  — + awarded, maxPoints, groupId (replaces score)
    pom.xml                                    MOD  — version 0.2.0-SNAPSHOT
  homework/
    src/main/java/de/codillas/homework/
      domain/GradeCalculator.java              NEW  — pure late-penalty bean
      domain/model/Assignment.java             MOD  — + latePenaltyPctPerDay, maxLatePenaltyPct, rubricId
      domain/model/Grade.java                  MOD  — + effectiveScore, maxPoints (score = raw)
      domain/model/Rubric.java                 NEW  — @Entity (assignmentId)
      domain/model/RubricCriterion.java        NEW  — @Entity (rubricId, label, maxPoints, position)
      domain/model/GradeCriterion.java         NEW  — @Entity (gradeId, criterionId, points)
      domain/repository/RubricRepository.java          NEW
      domain/repository/RubricCriterionRepository.java NEW  — BY_POSITION sort
      domain/repository/GradeCriterionRepository.java  NEW
      service/RubricService.java               NEW  — interface
      service/RubricServiceImpl.java           NEW  — create/get rubric
      service/SubmissionServiceImpl.java       MOD  — rubric sum + late penalty + widened event
      mapper/RubricMapper.java                 NEW
      mapper/GradeMapper.java                  MOD  — toResponse(Grade, List<GradeCriterion>)
      web/HomeworkController.java              MOD  — createRubric / getRubric
    src/main/resources/db/changelog/changes/0.3.0/
      0.3.0-add-assignment-grading.yaml        NEW  — late-penalty + rubric_id cols
      0.3.0-add-grade-effective.yaml           NEW  — effective_score + max_points cols
      0.3.0-create-rubrics.yaml                NEW
      0.3.0-create-rubric-criteria.yaml        NEW
      0.3.0-create-grade-criteria.yaml         NEW
    src/main/resources/db/changelog/homework-changelog.yaml  MOD  — include 0.3.0
    (pom.xml UNCHANGED — module inherits the parent reactor version)
  assessment/
    src/main/java/de/codillas/assessment/service/AttemptServiceImpl.java  MOD  — widened event (maxPoints, groupId=null)
    pom.xml                                    MOD  — version 0.2.0-SNAPSHOT
  gradebook/
    src/main/java/de/codillas/gradebook/
      domain/WeightedGradeCalculator.java      NEW  — pure weighted-percent bean (+ WeightedGrade, TypeGrade records)
      domain/model/ProgressEntry.java          MOD  — + maxPoints, groupId (score = awarded)
      domain/repository/ProgressEntryRepository.java       MOD  — findByStudentIdAndGroupId
      domain/repository/GradebookMembershipRepository.java MOD  — findFirstByStudentId
      service/GradebookServiceImpl.java        MOD  — resolve groupId, compute courseGrade
      mapper/GradebookMapper.java              MOD  — map awarded/maxPoints/groupId + CourseGrade
    src/main/resources/db/changelog/changes/0.2.0/
      0.2.0-add-progress-weighting.yaml        NEW  — max_points + group_id cols
    src/main/resources/db/changelog/gradebook-changelog.yaml MOD  — include 0.2.0
    pom.xml                                    MOD  — version 0.2.0-SNAPSHOT
  openapi/
    homework-schemas.yaml  MOD   homework-paths.yaml  MOD
    gradebook-schemas.yaml MOD
  main/src/test/java/de/codillas/integration/gradebook/
    CourseGradeIntegrationTest.java            NEW  — weighted end-to-end (Awaitility)
```

---

### Task 1: `GradeCalculator` — pure late-penalty bean (homework)

The only late-penalty math, isolated and exhaustively unit-tested. No event/schema changes.

**Files:**
- Create: `backend/homework/src/main/java/de/codillas/homework/domain/GradeCalculator.java`
- Create: `backend/homework/src/test/java/de/codillas/homework/domain/GradeCalculatorTest.java`

**Interfaces — produces:**
```java
int effectiveScore(int rawScore, long daysLate, @Nullable Integer penaltyPctPerDay, @Nullable Integer maxPenaltyPct)
```
`penaltyPctPerDay == null` ⇒ penalty off ⇒ raw returned unchanged. Penalty `= min(daysLate * pctPerDay, maxPenaltyPct (if set), 100)`; effective `= round(raw * (100 - penalty)/100)`, floored at 0.

- [ ] **Step 1 (RED): write the test.**
  ```java
  package de.codillas.homework.domain;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("GradeCalculator (late penalty)")
  class GradeCalculatorTest {

    private final GradeCalculator calculator = new GradeCalculator();

    @Test
    @DisplayName("on time (0 days late) returns the raw score unchanged")
    void onTime() {
      assertThat(calculator.effectiveScore(80, 0, 10, 50)).isEqualTo(80);
    }

    @Test
    @DisplayName("one day late deducts one day's penalty")
    void oneDayLate() {
      // 80 * (100 - 10)/100 = 72
      assertThat(calculator.effectiveScore(80, 1, 10, 50)).isEqualTo(72);
    }

    @Test
    @DisplayName("penalty is capped at maxPenaltyPct")
    void capped() {
      // 10 days * 10% = 100%, capped at 50% -> 80 * 0.5 = 40
      assertThat(calculator.effectiveScore(80, 10, 10, 50)).isEqualTo(40);
    }

    @Test
    @DisplayName("a null maxPenaltyPct still floors the score at 0, never negative")
    void uncappedFloorsAtZero() {
      // 20 days * 10% = 200% -> clamped to 100% -> 0
      assertThat(calculator.effectiveScore(80, 20, 10, null)).isEqualTo(0);
    }

    @Test
    @DisplayName("penalty off (null pctPerDay) returns the raw score regardless of days late")
    void penaltyOff() {
      assertThat(calculator.effectiveScore(80, 3, null, null)).isEqualTo(80);
    }
  }
  ```

- [ ] **Step 2 (GREEN): write the bean.**
  ```java
  package de.codillas.homework.domain;

  import org.jspecify.annotations.Nullable;
  import org.springframework.stereotype.Component;

  /**
   * Pure late-penalty math: turns a raw graded score into the effective score after a per-day
   * deduction, capped. {@code penaltyPctPerDay == null} means the assignment has no late penalty.
   */
  @Component
  public class GradeCalculator {

    public int effectiveScore(
        int rawScore,
        long daysLate,
        @Nullable Integer penaltyPctPerDay,
        @Nullable Integer maxPenaltyPct) {
      if (penaltyPctPerDay == null || daysLate <= 0) {
        return rawScore;
      }
      long penaltyPct = penaltyPctPerDay * daysLate;
      if (maxPenaltyPct != null) {
        penaltyPct = Math.min(penaltyPct, maxPenaltyPct);
      }
      penaltyPct = Math.min(penaltyPct, 100);
      int effective = (int) Math.round(rawScore * (100 - penaltyPct) / 100.0);
      return Math.max(0, effective);
    }
  }
  ```

- [ ] **Step 3: verify.** `cd backend && mvn -pl homework test -Dtest=GradeCalculatorTest`. Expected: 5 green. `mvn spotless:apply`.

- [ ] **Step 4: commit.**
  ```bash
  git add backend/homework/src/main/java/de/codillas/homework/domain/GradeCalculator.java \
          backend/homework/src/test/java/de/codillas/homework/domain/GradeCalculatorTest.java
  git commit -m "feat(homework): pure GradeCalculator late-penalty bean (effectiveScore)"
  ```

---

### Task 2: Rubric authoring (homework — entities, columns, OpenAPI, service, endpoints)

Add the optional rubric aggregate (`Rubric` + ordered `RubricCriterion{label,maxPoints}`), the three `Assignment` grading columns, and rubric author/read endpoints. No grade computation yet (Task 3) and no event change (Task 4).

**Files:** OpenAPI (`homework-schemas.yaml`, `homework-paths.yaml`); entities + repos + Liquibase + `Assignment` columns; `RubricService`/`Impl`, `RubricMapper`, controller; bump `homework` pom to `0.3.0-SNAPSHOT`.

- [ ] **Step 1 (API-first): `homework-schemas.yaml`** — add rubric schemas and the three `Assignment` grading fields.
  ```yaml
    CreateRubricCriterionRequest:
      type: object
      required: [label, maxPoints]
      properties:
        label: { type: string, minLength: 1, maxLength: 200 }
        maxPoints: { type: integer, minimum: 1 }
    CreateRubricRequest:
      type: object
      required: [criteria]
      properties:
        criteria:
          type: array
          minItems: 1
          items: { $ref: "#/components/schemas/CreateRubricCriterionRequest" }
    RubricCriterionResponse:
      type: object
      required: [id, label, maxPoints]
      properties:
        id: { type: string, format: uuid }
        label: { type: string }
        maxPoints: { type: integer }
    RubricResponse:
      type: object
      required: [id, assignmentId, criteria]
      properties:
        id: { type: string, format: uuid }
        assignmentId: { type: string, format: uuid }
        criteria:
          type: array
          items: { $ref: "#/components/schemas/RubricCriterionResponse" }
  ```
  In `CreateAssignmentRequest` **and** `AssignmentResponse` add (both nullable = off):
  ```yaml
        latePenaltyPctPerDay:
          type: integer
          minimum: 1
          maximum: 100
          description: Percent deducted from the raw score per day late. Omit to disable.
        maxLatePenaltyPct:
          type: integer
          minimum: 1
          maximum: 100
          description: Cap on the cumulative late penalty.
  ```
  In `AssignmentResponse` only, also add `rubricId: { type: string, format: uuid }`.

- [ ] **Step 2 (API-first): `homework-paths.yaml`** — add the rubric sub-resource.
  ```yaml
    /api/assignments/{assignmentId}/rubric:
      post:
        tags: [homework]
        operationId: createRubric
        summary: Attach (or replace) a grading rubric on an assignment
        parameters:
          - { name: assignmentId, in: path, required: true, schema: { type: string, format: uuid } }
        requestBody:
          required: true
          content:
            application/json:
              schema:
                $ref: "homework-schemas.yaml#/components/schemas/CreateRubricRequest"
        responses:
          "201":
            description: Rubric created
            content:
              application/json:
                schema:
                  $ref: "homework-schemas.yaml#/components/schemas/RubricResponse"
          "400": { $ref: "common.yaml#/components/responses/BadRequest" }
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "403": { $ref: "common.yaml#/components/responses/Forbidden" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
      get:
        tags: [homework]
        operationId: getRubric
        summary: The assignment's rubric, if any
        parameters:
          - { name: assignmentId, in: path, required: true, schema: { type: string, format: uuid } }
        responses:
          "200":
            description: Rubric
            content:
              application/json:
                schema:
                  $ref: "homework-schemas.yaml#/components/schemas/RubricResponse"
          "401": { $ref: "common.yaml#/components/responses/Unauthorized" }
          "404": { $ref: "common.yaml#/components/responses/NotFound" }
  ```
  Bump `info.version` to `0.3.0` in both homework yaml files. Regenerate (`mvn -pl homework generate-sources`).

- [ ] **Step 3: `Assignment` columns.** Add to `Assignment.java`:
  ```java
    @Column(name = "late_penalty_pct_per_day")
    private Integer latePenaltyPctPerDay;

    @Column(name = "max_late_penalty_pct")
    private Integer maxLatePenaltyPct;

    @Column(name = "rubric_id")
    private UUID rubricId;
  ```
  Liquibase `0.3.0-add-assignment-grading.yaml` (precondition `not columnExists late_penalty_pct_per_day`):
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-add-assignment-grading
        author: codillas
        comment: Late-penalty config + rubric link on assignments (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists: { tableName: assignments, columnName: late_penalty_pct_per_day }
        changes:
          - addColumn:
              tableName: assignments
              columns:
                - column: { name: late_penalty_pct_per_day, type: int }
                - column: { name: max_late_penalty_pct, type: int }
                - column: { name: rubric_id, type: uuid }
  ```

- [ ] **Step 4: rubric entities.** `Rubric.java` (anemic aggregate root; `assignmentId` for lookup):
  ```java
  @Entity
  @Table(name = "rubrics")
  @Getter @Setter @NoArgsConstructor @SuperBuilder
  public class Rubric extends BaseAuditableEntity {
    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;
  }
  ```
  `RubricCriterion.java` (ordered):
  ```java
  @Entity
  @Table(name = "rubric_criteria")
  @Getter @Setter @NoArgsConstructor @SuperBuilder
  public class RubricCriterion extends BaseAuditableEntity {
    @Column(name = "rubric_id", nullable = false) private UUID rubricId;
    @Column(nullable = false, length = 200) private String label;
    @Column(name = "max_points", nullable = false) private int maxPoints;
    @Column(nullable = false) private int position;
  }
  ```
  Liquibase `0.3.0-create-rubrics.yaml` and `0.3.0-create-rubric-criteria.yaml` follow the `0.1.0-create-grades.yaml` shape (uuid PK + `created_at`/`updated_at` timestamptz; for criteria: `rubric_id uuid not null`, `label varchar(200) not null`, `max_points int not null`, `position int not null`).

- [ ] **Step 5: repositories.**
  ```java
  @Repository
  public interface RubricRepository extends JpaRepository<Rubric, UUID> {
    Optional<Rubric> findByAssignmentId(UUID assignmentId);
  }
  ```
  ```java
  @Repository
  public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {
    Sort BY_POSITION = Sort.by(Sort.Order.asc(RubricCriterion_.POSITION));
    List<RubricCriterion> findByRubricId(UUID rubricId, Sort sort);
    void deleteByRubricId(UUID rubricId);
  }
  ```

- [ ] **Step 6: `RubricMapper`.**
  ```java
  @Mapper(config = CentralMapperConfig.class)
  public interface RubricMapper {
    RubricCriterionResponse toCriterionResponse(RubricCriterion criterion);
    List<RubricCriterionResponse> toCriterionResponses(List<RubricCriterion> criteria);
    RubricResponse toResponse(Rubric rubric, List<RubricCriterionResponse> criteria);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "rubricId", source = "rubricId")
    @Mapping(target = "position", source = "position")
    RubricCriterion toCriterion(CreateRubricCriterionRequest request, UUID rubricId, int position);
  }
  ```

- [ ] **Step 7 (RED→GREEN): `RubricService` + `RubricServiceImpl`.** Interface:
  ```java
  public interface RubricService {
    RubricResponse createRubric(UUID assignmentId, CreateRubricRequest request);
    RubricResponse getRubric(UUID assignmentId);
  }
  ```
  Impl (`@Service @RequiredArgsConstructor @Transactional(readOnly = true)`): inject `AssignmentRepository`, `RubricRepository`, `RubricCriterionRepository`, `RubricMapper`.
  ```java
  @Override @Transactional
  public RubricResponse createRubric(UUID assignmentId, CreateRubricRequest request) {
    Assignment assignment =
        assignmentRepository
            .findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Assignment", assignmentId));
    // Replace any prior rubric (re-author): drop old criteria + rubric.
    rubricRepository
        .findByAssignmentId(assignmentId)
        .ifPresent(
            old -> {
              criterionRepository.deleteByRubricId(old.getId());
              rubricRepository.delete(old);
            });
    Rubric rubric = rubricRepository.save(Rubric.builder().assignmentId(assignmentId).build());
    int position = 0;
    List<RubricCriterion> criteria = new ArrayList<>();
    for (CreateRubricCriterionRequest c : request.getCriteria()) {
      criteria.add(criterionRepository.save(mapper.toCriterion(c, rubric.getId(), position++)));
    }
    assignment.setRubricId(rubric.getId());
    assignmentRepository.save(assignment);
    return mapper.toResponse(rubric, mapper.toCriterionResponses(criteria));
  }

  @Override
  public RubricResponse getRubric(UUID assignmentId) {
    Rubric rubric =
        rubricRepository
            .findByAssignmentId(assignmentId)
            .orElseThrow(() -> new NotFoundException("Rubric", assignmentId));
    return mapper.toResponse(
        rubric,
        mapper.toCriterionResponses(
            criterionRepository.findByRubricId(rubric.getId(), RubricCriterionRepository.BY_POSITION)));
  }
  ```
  Unit test `RubricServiceTest` (Mockito): `createRubric` saves a rubric, saves N criteria with positions `0..N-1`, sets `assignment.rubricId`, returns the mapped response; `getRubric` throws `NotFoundException` when absent. (Mirror `SubmissionServiceTest` style — mock repos/mapper, assert interactions.)

- [ ] **Step 8: wire the controller.** In `HomeworkController` inject `RubricService` and implement:
  ```java
  @Override
  @RequiresTeacher
  public ResponseEntity<RubricResponse> createRubric(UUID assignmentId, CreateRubricRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(rubricService.createRubric(assignmentId, req));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<RubricResponse> getRubric(UUID assignmentId) {
    return ResponseEntity.ok(rubricService.getRubric(assignmentId));
  }
  ```

- [ ] **Step 9: verify + commit.** `mvn -pl homework -am test`; `mvn -q compile`; `mvn spotless:apply`.
  ```bash
  git add backend/openapi/homework-*.yaml backend/homework
  git commit -m "feat(homework): rubric authoring + assignment late-penalty config (0.3.0)"
  ```

---

### Task 3: Grade computation — rubric sum + late penalty (homework)

Extend `Grade` with `effectiveScore` + `maxPoints`, persist a per-criterion breakdown (`GradeCriterion`), and rewrite `gradeSubmission` to: derive the raw score (Σ criterion scores when a rubric is present, else the free-form `score`), apply `GradeCalculator`, store raw + effective + maxPoints + breakdown. **Event stays the P1 arity in this task** (publishes `effectiveScore` as the score); Task 4 widens it.

**Files:** OpenAPI grade schemas; `Grade` + `GradeCriterion` entities + Liquibase + repo; `GradeMapper`; `SubmissionServiceImpl.gradeSubmission`.

- [ ] **Step 1 (API-first): `homework-schemas.yaml`** — make `score` optional, add criterion scores + grade response fields.
  ```yaml
    CriterionScoreInput:
      type: object
      required: [criterionId, points]
      properties:
        criterionId: { type: string, format: uuid }
        points: { type: integer, minimum: 0 }
    CriterionScoreResponse:
      type: object
      required: [criterionId, points]
      properties:
        criterionId: { type: string, format: uuid }
        points: { type: integer }
  ```
  Change `CreateGradeRequest` (drop `required: [score]`):
  ```yaml
    CreateGradeRequest:
      type: object
      properties:
        score:
          type: integer
          minimum: 0
          maximum: 100
          description: Free-form score; ignored when the assignment has a rubric.
        criterionScores:
          type: array
          items: { $ref: "#/components/schemas/CriterionScoreInput" }
          description: Required when the assignment has a rubric; one entry per criterion.
  ```
  Extend `GradeResponse` with `effectiveScore` (int, required), `maxPoints` (int, required), `criterionScores` (array of `CriterionScoreResponse`). Keep `score` as the **raw** score. Regenerate.

- [ ] **Step 2: entities.** `Grade.java` add:
  ```java
    @Column(name = "effective_score", nullable = false)
    private int effectiveScore;

    @Column(name = "max_points", nullable = false)
    @Builder.Default
    private int maxPoints = 100;
  ```
  `GradeCriterion.java` (the per-criterion breakdown the spec attaches to the graded artefact):
  ```java
  @Entity
  @Table(name = "grade_criteria")
  @Getter @Setter @NoArgsConstructor @SuperBuilder
  public class GradeCriterion extends BaseAuditableEntity {
    @Column(name = "grade_id", nullable = false) private UUID gradeId;
    @Column(name = "criterion_id", nullable = false) private UUID criterionId;
    @Column(nullable = false) private int points;
  }
  ```
  > **Spec note:** the spec wording is "`Review` carries per-criterion scores". This plan attaches the breakdown to the **`Grade`** (`GradeCriterion`), submitted on the grade request, because grade-with-rubric and the published `awarded`/`maxPoints` must be one atomic act derived from the same scores (the spec's OpenAPI line says "grading-with-rubric"); `reviewSubmission` stays free-text. Functionally identical (teacher-authored at grade time), without coupling the score to "which review".

  Liquibase: `0.3.0-add-grade-effective.yaml` (addColumn `effective_score int not null default 0`, `max_points int not null default 100` on `grades`), and `0.3.0-create-grade-criteria.yaml` (uuid PK, `grade_id`/`criterion_id` uuid not null, `points int not null`, audit cols).

- [ ] **Step 3: repository.**
  ```java
  @Repository
  public interface GradeCriterionRepository extends JpaRepository<GradeCriterion, UUID> {
    List<GradeCriterion> findByGradeId(UUID gradeId);
    void deleteByGradeId(UUID gradeId);
  }
  ```
  Add to `GradeRepository`: `Optional<Grade> findBySubmissionId(UUID submissionId);` (used by the P1 upsert).

- [ ] **Step 4: `GradeMapper`** — response now carries the breakdown.
  ```java
  @Mapping(target = "criterionScores", source = "criteria")
  GradeResponse toResponse(Grade entity, List<GradeCriterion> criteria);

  CriterionScoreResponse toCriterionScore(GradeCriterion criterion);
  List<CriterionScoreResponse> toCriterionScores(List<GradeCriterion> criteria);
  ```
  (Drop the old single-arg `toResponse(Grade)`; the entity-factory `toEntity` is replaced by builder use in the service since rubric/penalty fields are computed, not mapped.)

- [ ] **Step 5 (RED): extend `SubmissionServiceTest.gradeSubmission_*`.** Add cases (mock `AssignmentRepository`, `GradeCalculator`, `RubricCriterionRepository`, `GradeCriterionRepository`):
  - **flat, on time:** assignment has no `rubricId`, no penalty, submission not late ⇒ `grade.score == request.score`, `grade.effectiveScore == request.score`, `grade.maxPoints == 100`.
  - **flat, late + penalty:** assignment `latePenaltyPctPerDay=10`, `submission.late=true`, `submittedAt` 1 day after `dueAt` ⇒ `GradeCalculator.effectiveScore` invoked with `daysLate=1`; stored effective comes from the (mocked) calculator.
  - **rubric:** assignment has `rubricId`; criteria `[A maxPoints 30, B maxPoints 10]`; request `criterionScores [A=24, B=8]` ⇒ raw `32`, `maxPoints 40`, two `GradeCriterion` rows saved.

  Example body (rubric case):
  ```java
  @Test
  @DisplayName("gradeSubmission with a rubric scores Σ criterion points over Σ max points")
  void gradeSubmission_rubric() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID rubricId = UUID.randomUUID();
    UUID cA = UUID.randomUUID();
    UUID cB = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(studentId).version(1).build();
    Assignment assignment =
        Assignment.builder().groupId(groupId).title("HW").rubricId(rubricId).build();
    CreateGradeRequest request = mock(CreateGradeRequest.class);
    when(request.getCriterionScores())
        .thenReturn(
            List.of(criterionScore(cA, 24), criterionScore(cB, 8)));
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(criterionRepository.findByRubricId(eq(rubricId), any()))
        .thenReturn(
            List.of(
                RubricCriterion.builder().id(cA).rubricId(rubricId).maxPoints(30).position(0).build(),
                RubricCriterion.builder().id(cB).rubricId(rubricId).maxPoints(10).position(1).build()));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    when(gradeCalculator.effectiveScore(eq(32), eq(0L), any(), any())).thenReturn(32);
    when(gradeMapper.toResponse(any(), any())).thenReturn(mock(GradeResponse.class));

    service.gradeSubmission(submissionId, request);

    ArgumentCaptor<Grade> grade = ArgumentCaptor.forClass(Grade.class);
    verify(gradeRepository).save(grade.capture());
    assertThat(grade.getValue().getScore()).isEqualTo(32);
    assertThat(grade.getValue().getMaxPoints()).isEqualTo(40);
    assertThat(grade.getValue().getEffectiveScore()).isEqualTo(32);
  }
  ```
  (`criterionScore(id, pts)` is a tiny test helper building a mocked/real `CriterionScoreInput`.)

- [ ] **Step 6 (GREEN): rewrite `gradeSubmission`.** Inject `AssignmentRepository`, `RubricCriterionRepository`, `GradeCriterionRepository`, `GradeCalculator`. Keep the P1 ownership load + upsert.
  ```java
  @Override
  @Transactional
  public GradeResponse gradeSubmission(UUID submissionId, CreateGradeRequest request) {
    Submission submission = findByIdForCaller(submissionId, currentUser); // P1: owner/staff check
    Assignment assignment =
        assignmentRepository
            .findById(submission.getAssignmentId())
            .orElseThrow(() -> new NotFoundException("Assignment", submission.getAssignmentId()));
    stateMachine.transitionTo(submission, SubmissionStatus.GRADED);
    repository.save(submission);

    RawScore raw = rawScore(assignment, request);
    long daysLate = lateDays(assignment, submission);
    int effective =
        gradeCalculator.effectiveScore(
            raw.points(), daysLate,
            assignment.getLatePenaltyPctPerDay(), assignment.getMaxLatePenaltyPct());

    Grade grade =
        gradeRepository
            .findBySubmissionId(submissionId)
            .orElseGet(() -> Grade.builder().submissionId(submissionId).build());
    grade.setGradedBy(currentUser.id());
    grade.setScore(raw.points());
    grade.setMaxPoints(raw.maxPoints());
    grade.setEffectiveScore(effective);
    Grade saved = gradeRepository.save(grade);

    gradeCriterionRepository.deleteByGradeId(saved.getId());
    List<GradeCriterion> breakdown =
        raw.criteria().entrySet().stream()
            .map(e -> GradeCriterion.builder()
                .gradeId(saved.getId()).criterionId(e.getKey()).points(e.getValue()).build())
            .toList();
    gradeCriterionRepository.saveAll(breakdown);

    events.publishEvent(
        new SubmissionGraded(
            submissionId, assignment.getId(), submission.getStudentId(), effective)); // P1 arity; Task 4 widens
    return gradeMapper.toResponse(saved, gradeCriterionRepository.findByGradeId(saved.getId()));
  }

  /** Raw points + denominator for one grade, from the rubric when present, else the flat score. */
  private RawScore rawScore(Assignment assignment, CreateGradeRequest request) {
    if (assignment.getRubricId() == null) {
      if (request.getScore() == null) {
        throw new BadRequestException("score is required when the assignment has no rubric");
      }
      return new RawScore(request.getScore(), 100, Map.of());
    }
    List<RubricCriterion> criteria =
        rubricCriterionRepository.findByRubricId(
            assignment.getRubricId(), RubricCriterionRepository.BY_POSITION);
    Map<UUID, Integer> maxById =
        criteria.stream().collect(Collectors.toMap(RubricCriterion::getId, RubricCriterion::getMaxPoints));
    Map<UUID, Integer> given = new LinkedHashMap<>();
    for (CriterionScoreInput in : Optional.ofNullable(request.getCriterionScores()).orElse(List.of())) {
      Integer max = maxById.get(in.getCriterionId());
      if (max == null) {
        throw new BadRequestException("Unknown rubric criterion " + in.getCriterionId());
      }
      if (in.getPoints() > max) {
        throw new BadRequestException("Criterion score exceeds its maxPoints");
      }
      given.put(in.getCriterionId(), in.getPoints());
    }
    int points = given.values().stream().mapToInt(Integer::intValue).sum();
    int maxPoints = criteria.stream().mapToInt(RubricCriterion::getMaxPoints).sum();
    return new RawScore(points, maxPoints, given);
  }

  /** Whole days late (ceiling), 0 when on time or P1 late metadata is absent. */
  private long lateDays(Assignment assignment, Submission submission) {
    if (!submission.isLate()
        || submission.getSubmittedAt() == null
        || assignment.getDueAt() == null) {
      return 0;
    }
    long minutes =
        Duration.between(assignment.getDueAt(), submission.getSubmittedAt()).toMinutes();
    return minutes <= 0 ? 0 : (long) Math.ceil(minutes / 1440.0);
  }

  private record RawScore(int points, int maxPoints, Map<UUID, Integer> criteria) {}
  ```

- [ ] **Step 7: verify + commit.** `mvn -pl homework -am test`; `mvn -q compile`; `mvn spotless:apply`.
  ```bash
  git add backend/openapi/homework-*.yaml backend/homework
  git commit -m "feat(homework): rubric + late-penalty grade computation (raw + effectiveScore + maxPoints)"
  ```

---

### Task 4: Widen the shared grade events + migrate read-model columns

Atomic cross-module change (a shared record arity change cannot be split without breaking the build): both events carry `awarded`, `maxPoints`, `groupId` (replacing `score`); homework + assessment publish them; `gradebook` gains `ProgressEntry.maxPoints`/`groupId` and resolves a null `groupId` from its own membership read model.

**Files:** `shared` events (+ pom `0.2.0`); homework publisher; assessment publisher (+ pom `0.2.0`); gradebook entity/Liquibase/mapper/service (+ pom `0.2.0`); all affected unit tests.

- [ ] **Step 1: widen the events.**
  ```java
  // SubmissionGraded.java
  public record SubmissionGraded(
      UUID submissionId, UUID assignmentId, UUID studentId, int awarded, int maxPoints, UUID groupId) {}
  ```
  ```java
  // AttemptCompleted.java  (groupId is nullable: assessment cannot know it — see the cross-module note)
  public record AttemptCompleted(
      UUID attemptId, UUID testId, UUID studentId, int awarded, int maxPoints, UUID groupId) {}
  ```
  Update both Javadocs to note `awarded`/`maxPoints` (gradebook weights `awarded/maxPoints`) and that `groupId` may be null on `AttemptCompleted`.

- [ ] **Step 2: homework publisher.** In `gradeSubmission` change the publish line to the new arity:
  ```java
    events.publishEvent(
        new SubmissionGraded(
            submissionId, assignment.getId(), submission.getStudentId(),
            effective, raw.maxPoints(), assignment.getGroupId()));
  ```
  Update `SubmissionServiceTest` event assertions to `new SubmissionGraded(submissionId, assignmentId, studentId, effective, maxPoints, groupId)`.

- [ ] **Step 3: assessment publisher.** In `AttemptServiceImpl.submitAttempt`, compute `maxPoints` from the already-loaded questions and publish `groupId = null`:
  ```java
    int maxPoints = questionsById.values().stream().mapToInt(Question::getPoints).sum();
    events.publishEvent(
        new AttemptCompleted(
            attemptId, attempt.getTestId(), attempt.getStudentId(),
            attempt.getScore(), maxPoints, null)); // groupId resolved by gradebook from membership
  ```
  Update any assessment unit test asserting the published `AttemptCompleted` to the new arity. Bump `assessment` pom to `0.2.0-SNAPSHOT`.

- [ ] **Step 4: `gradebook` ProgressEntry columns.** Add to `ProgressEntry.java` (existing `score` now holds the **awarded** points):
  ```java
    @Column(name = "max_points", nullable = false)
    @Builder.Default
    private int maxPoints = 100;

    @Column(name = "group_id")
    private UUID groupId;
  ```
  Liquibase `gradebook/.../changes/0.2.0/0.2.0-add-progress-weighting.yaml`:
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.2.0-add-progress-weighting
        author: codillas
        comment: Weighting denominator + group scope on progress entries (0.2.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists: { tableName: progress_entries, columnName: max_points }
        changes:
          - addColumn:
              tableName: progress_entries
              columns:
                - column: { name: max_points, type: int, defaultValueNumeric: 100, constraints: { nullable: false } }
                - column: { name: group_id, type: uuid }
  ```
  Add the include to `gradebook-changelog.yaml` (after the 0.1.0 block, before/after seed per version order).

- [ ] **Step 5: `GradebookMapper`** — map the new event fields (`score` ← `awarded`).
  ```java
  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "source", constant = "HOMEWORK")
  @Mapping(target = "sourceId", source = "submissionId")
  @Mapping(target = "referenceId", source = "assignmentId")
  @Mapping(target = "score", source = "awarded")
  @Mapping(target = "maxPoints", source = "maxPoints")
  @Mapping(target = "groupId", source = "groupId")
  ProgressEntry toEntry(SubmissionGraded event);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "source", constant = "TEST")
  @Mapping(target = "sourceId", source = "attemptId")
  @Mapping(target = "referenceId", source = "testId")
  @Mapping(target = "score", source = "awarded")
  @Mapping(target = "maxPoints", source = "maxPoints")
  @Mapping(target = "groupId", source = "groupId")
  ProgressEntry toEntry(AttemptCompleted event);
  ```

- [ ] **Step 6: resolve a null `groupId` + carry weighting through upsert.** Add `findFirstByStudentId` to `GradebookMembershipRepository`:
  ```java
  Optional<GradebookMembership> findFirstByStudentId(UUID studentId);
  ```
  In `GradebookServiceImpl`, the upsert now copies `maxPoints`/`groupId` and backfills a null group from membership:
  ```java
  @Override @Transactional
  public void recordSubmissionGrade(SubmissionGraded event) {
    upsert(GradeSource.HOMEWORK, event.submissionId(), mapper.toEntry(event));
  }

  @Override @Transactional
  public void recordAttempt(AttemptCompleted event) {
    upsert(GradeSource.TEST, event.attemptId(), mapper.toEntry(event));
  }

  private void upsert(GradeSource source, UUID sourceId, ProgressEntry fresh) {
    if (fresh.getGroupId() == null) {
      membershipRepository
          .findFirstByStudentId(fresh.getStudentId())
          .ifPresent(m -> fresh.setGroupId(m.getGroupId()));
    }
    ProgressEntry entry = repository.findBySourceAndSourceId(source, sourceId).orElse(fresh);
    entry.setScore(fresh.getScore());
    entry.setMaxPoints(fresh.getMaxPoints());
    entry.setGroupId(fresh.getGroupId());
    repository.save(entry);
  }
  ```
  (The mapper already populated `studentId` on `fresh` from the event; the membership lookup keys off it.)

- [ ] **Step 7: fix gradebook unit tests.** Update `GradebookServiceTest` and `GradebookEventListenerTest` to the new event arity (`new SubmissionGraded(..., awarded, maxPoints, groupId)`, `new AttemptCompleted(..., awarded, maxPoints, null)`); add a case: **a null-group `AttemptCompleted` resolves the group from membership** (`membershipRepository.findFirstByStudentId(student)` returns a membership ⇒ saved entry's `groupId` equals it).

- [ ] **Step 8: verify + commit.** `mvn -q compile` (whole backend — confirms the atomic change links), `mvn -pl homework,assessment,gradebook -am test`, `mvn spotless:apply`. Run `ApplicationModules.of(...).verify()` (via the existing modulith test in `main`).
  ```bash
  git add backend/shared backend/homework backend/assessment backend/gradebook
  git commit -m "feat(shared): widen SubmissionGraded/AttemptCompleted with awarded/maxPoints/groupId; gradebook records them"
  ```

---

### Task 5: Final course grade — `WeightedGradeCalculator` + gradebook response

Compute the points-weighted percent `Σawarded/Σmax` per `(student, group)` with a per-type breakdown, surface it on the gradebook responses.

**Files:** `WeightedGradeCalculator` (+ `WeightedGrade`/`TypeGrade` records) + test; `gradebook-schemas.yaml`; `ProgressEntryRepository`; `GradebookMapper`; `GradebookServiceImpl`.

- [ ] **Step 1 (API-first): `gradebook-schemas.yaml`.** Extend `ProgressEntryResponse` with `maxPoints` (int). Add:
  ```yaml
    TypeGradeResponse:
      type: object
      required: [source, awarded, maxPoints, percent]
      properties:
        source: { $ref: "#/components/schemas/GradeSource" }
        awarded: { type: integer }
        maxPoints: { type: integer }
        percent: { type: number, format: double }
    CourseGradeResponse:
      type: object
      required: [awarded, maxPoints, percent, byType]
      properties:
        awarded: { type: integer }
        maxPoints: { type: integer }
        percent: { type: number, format: double }
        byType:
          type: array
          items: { $ref: "#/components/schemas/TypeGradeResponse" }
  ```
  Add `courseGrade: { $ref: "#/components/schemas/CourseGradeResponse" }` (optional) to `StudentGradebookResponse`. Bump `info.version` to `0.2.0`; regenerate.

- [ ] **Step 2 (RED): `WeightedGradeCalculatorTest`.**
  ```java
  package de.codillas.gradebook.domain;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.within;

  import java.util.List;
  import de.codillas.gradebook.domain.model.GradeSource;
  import de.codillas.gradebook.domain.model.ProgressEntry;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("WeightedGradeCalculator")
  class WeightedGradeCalculatorTest {

    private final WeightedGradeCalculator calculator = new WeightedGradeCalculator();

    private static ProgressEntry entry(GradeSource source, int awarded, int max) {
      return ProgressEntry.builder().source(source).score(awarded).maxPoints(max).build();
    }

    @Test
    @DisplayName("weights Σawarded/Σmax across types and breaks the percent down per type")
    void weightedPercentAndBreakdown() {
      WeightedGrade grade =
          calculator.compute(
              List.of(
                  entry(GradeSource.HOMEWORK, 24, 30),
                  entry(GradeSource.HOMEWORK, 10, 10),
                  entry(GradeSource.TEST, 18, 20)));

      assertThat(grade.awarded()).isEqualTo(52);
      assertThat(grade.maxPoints()).isEqualTo(60);
      assertThat(grade.percent()).isCloseTo(86.667, within(0.01));
      assertThat(grade.byType())
          .anySatisfy(t -> {
            assertThat(t.source()).isEqualTo(GradeSource.HOMEWORK);
            assertThat(t.awarded()).isEqualTo(34);
            assertThat(t.maxPoints()).isEqualTo(40);
            assertThat(t.percent()).isCloseTo(85.0, within(0.01));
          })
          .anySatisfy(t -> {
            assertThat(t.source()).isEqualTo(GradeSource.TEST);
            assertThat(t.percent()).isCloseTo(90.0, within(0.01));
          });
    }

    @Test
    @DisplayName("empty entries yield a zero grade with no division by zero")
    void empty() {
      WeightedGrade grade = calculator.compute(List.of());
      assertThat(grade.awarded()).isZero();
      assertThat(grade.maxPoints()).isZero();
      assertThat(grade.percent()).isZero();
      assertThat(grade.byType()).isEmpty();
    }
  }
  ```

- [ ] **Step 3 (GREEN): the bean + value records** (gradebook-local, not shared).
  ```java
  package de.codillas.gradebook.domain;

  public record TypeGrade(GradeSource source, int awarded, int maxPoints) {
    public double percent() {
      return maxPoints == 0 ? 0.0 : awarded * 100.0 / maxPoints;
    }
  }
  ```
  ```java
  public record WeightedGrade(int awarded, int maxPoints, List<TypeGrade> byType) {
    public double percent() {
      return maxPoints == 0 ? 0.0 : awarded * 100.0 / maxPoints;
    }
  }
  ```
  ```java
  @Component
  public class WeightedGradeCalculator {
    public WeightedGrade compute(Collection<ProgressEntry> entries) {
      List<TypeGrade> byType =
          entries.stream()
              .collect(Collectors.groupingBy(ProgressEntry::getSource))
              .entrySet().stream()
              .map(e -> new TypeGrade(
                  e.getKey(),
                  e.getValue().stream().mapToInt(ProgressEntry::getScore).sum(),
                  e.getValue().stream().mapToInt(ProgressEntry::getMaxPoints).sum()))
              .sorted(Comparator.comparing(TypeGrade::source))
              .toList();
      int awarded = byType.stream().mapToInt(TypeGrade::awarded).sum();
      int maxPoints = byType.stream().mapToInt(TypeGrade::maxPoints).sum();
      return new WeightedGrade(awarded, maxPoints, byType);
    }
  }
  ```

- [ ] **Step 4: repository + mapper.** Add to `ProgressEntryRepository`:
  ```java
  List<ProgressEntry> findByStudentIdAndGroupId(UUID studentId, UUID groupId, Sort sort);
  ```
  `GradebookMapper`: add `maxPoints` to the entry response (auto), and the course-grade mappings:
  ```java
  TypeGradeResponse toTypeGrade(TypeGrade type);
  CourseGradeResponse toCourseGrade(WeightedGrade grade);

  @Mapping(target = "recordedAt", source = "entry.createdAt")
  ProgressEntryResponse toEntryResponse(ProgressEntry entry);  // unchanged signature; maxPoints auto-maps

  StudentGradebookResponse toStudentGradebook(
      UUID studentId, List<ProgressEntryResponse> entries, CourseGradeResponse courseGrade);
  ```
  (`toCourseGrade`/`toTypeGrade` map the record accessors incl. the computed `percent()` → `percent`.)

- [ ] **Step 5 (GREEN): compute the course grade in `GradebookServiceImpl`.** Inject `WeightedGradeCalculator`. `getGroupGradebook` scopes each student's entries to that group (now that entries carry `groupId`); `getStudentGradebook` aggregates across the student's entries.
  ```java
  @Override
  public StudentGradebookResponse getStudentGradebook(UUID studentId) {
    List<ProgressEntry> entries =
        repository.findByStudentId(studentId, ProgressEntryRepository.BY_RECORDED);
    return studentGradebook(studentId, entries);
  }

  // inside getGroupGradebook: build byStudent from findByStudentIdAndGroupId(..., groupId, ...)
  // then per student -> studentGradebook(id, groupScopedEntries)

  private StudentGradebookResponse studentGradebook(UUID studentId, List<ProgressEntry> entries) {
    WeightedGrade weighted = weightedGradeCalculator.compute(entries);
    return mapper.toStudentGradebook(
        studentId, mapper.toEntryResponses(entries), mapper.toCourseGrade(weighted));
  }
  ```
  Adjust `getGroupGradebook` to query `findByStudentIdAndGroupId` per member (or `findByStudentIdIn` then filter by `groupId == groupId`), so the per-student `courseGrade` is **per `(student, group)`**.

- [ ] **Step 6: update `GradebookServiceTest`.** `getStudentGradebook_maps` now stubs `weightedGradeCalculator.compute(...)` and `mapper.toCourseGrade(...)`/`toStudentGradebook(id, entries, courseGrade)`. Add a case asserting a two-type entry set yields the expected `courseGrade` wiring (delegation to the calculator).

- [ ] **Step 7: verify + commit.** `mvn -pl gradebook -am test`, `mvn -q compile`, `mvn spotless:apply`.
  ```bash
  git add backend/openapi/gradebook-schemas.yaml backend/gradebook
  git commit -m "feat(gradebook): weighted final course grade (Σawarded/Σmax) + per-type breakdown"
  ```

---

### Task 6: Weighted course grade — end-to-end integration test (`main`)

Prove the whole chain with the real schema + async event delivery: enroll a student in a group, grade a homework, complete a test, then assert the gradebook's `courseGrade` equals `Σawarded/Σmax` for that `(student, group)`.

**Files:**
- Create: `backend/main/src/test/java/de/codillas/integration/gradebook/CourseGradeIntegrationTest.java`

- [ ] **Step 1: write the test** (extends `BaseIntegrationTest`; `as(uuid, role)` helper as in `GradebookIntegrationTest`). Flow: enroll the student (so membership exists ⇒ the test attempt's null `groupId` resolves); create + grade a homework worth `88/100`; create a 1-question test worth `10` points, publish, attempt, answer correctly, submit (`10/10`); await the group gradebook and assert the weighted percent.
  ```java
  package de.codillas.integration.gradebook;

  import static org.awaitility.Awaitility.await;
  import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

  import java.time.Duration;
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

  @DisplayName("Course grade (weighted homework + test, integration)")
  class CourseGradeIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private static JwtRequestPostProcessor as(UUID userId, String role) {
      return jwt()
          .jwt(j -> j.subject(userId.toString()))
          .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String body(String json, MockMvcRequestPostProcessorRole... ignored) { return json; }

    @Test
    @DisplayName("final grade weights Σawarded/Σmax across a homework and a test in the group")
    void weightedCourseGrade() throws Exception {
      UUID teacher = UUID.randomUUID();
      UUID student = UUID.randomUUID();
      UUID groupId = UUID.randomUUID();

      // enrol the student (creates the gradebook membership; the test attempt's group resolves from it)
      mockMvc.perform(
              post("/api/groups/{id}/members", groupId)
                  .with(as(teacher, "TEACHER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"userId\":\"%s\"}".formatted(student)))
          .andExpect(status().isCreated());

      // homework: 88 / 100
      String assignment =
          mockMvc.perform(
                  post("/api/assignments")
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"groupId\":\"%s\",\"title\":\"HW\"}".formatted(groupId)))
              .andReturn().getResponse().getContentAsString();
      UUID assignmentId = UUID.fromString(JsonPath.read(assignment, "$.id"));
      String submission =
          mockMvc.perform(
                  post("/api/assignments/{id}/submissions", assignmentId)
                      .with(as(student, "STUDENT"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"content\":\"answer\"}"))
              .andReturn().getResponse().getContentAsString();
      UUID submissionId = UUID.fromString(JsonPath.read(submission, "$.id"));
      mockMvc.perform(put("/api/submissions/{id}/submit", submissionId).with(as(student, "STUDENT")))
          .andExpect(status().isOk());
      mockMvc.perform(
              post("/api/submissions/{id}/grade", submissionId)
                  .with(as(teacher, "TEACHER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"score\":88}"))
          .andExpect(status().isCreated());

      // test: one SINGLE_CHOICE question worth 10, answered correctly -> 10 / 10
      String test =
          mockMvc.perform(
                  post("/api/tests")
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"title\":\"Quiz\"}"))
              .andReturn().getResponse().getContentAsString();
      UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));
      String question =
          mockMvc.perform(
                  post("/api/tests/{id}/questions", testId)
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"type\":\"SINGLE_CHOICE\",\"prompt\":\"2+2?\",\"points\":10,"
                              + "\"options\":[{\"text\":\"4\",\"correct\":true},"
                              + "{\"text\":\"5\",\"correct\":false}]}"))
              .andReturn().getResponse().getContentAsString();
      UUID optionId = UUID.fromString(JsonPath.read(question, "$.options[0].id"));
      UUID questionId = UUID.fromString(JsonPath.read(question, "$.id"));
      mockMvc.perform(post("/api/tests/{id}/publish", testId).with(as(teacher, "TEACHER")))
          .andExpect(status().isOk());
      String attempt =
          mockMvc.perform(
                  post("/api/tests/{id}/attempts", testId).with(as(student, "STUDENT")))
              .andReturn().getResponse().getContentAsString();
      UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));
      mockMvc.perform(
              put("/api/attempts/{id}/answers", attemptId)
                  .with(as(student, "STUDENT"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"questionId\":\"%s\",\"selectedOptionIds\":[\"%s\"]}"
                          .formatted(questionId, optionId)))
          .andExpect(status().isOk());
      mockMvc.perform(post("/api/attempts/{id}/submit", attemptId).with(as(student, "STUDENT")))
          .andExpect(status().isOk());

      // both events are delivered asynchronously via the publication registry
      // weighted = (88 + 10) / (100 + 10) = 98 / 110 ~= 89.09%
      await()
          .atMost(Duration.ofSeconds(20))
          .untilAsserted(
              () ->
                  mockMvc
                      .perform(get("/api/gradebook/groups/{id}", groupId).with(as(teacher, "TEACHER")))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.students[0].courseGrade.awarded").value(98))
                      .andExpect(jsonPath("$.students[0].courseGrade.maxPoints").value(110))
                      .andExpect(jsonPath("$.students[0].courseGrade.percent").value(89.0909090909091))
                      .andExpect(jsonPath("$.students[0].courseGrade.byType.length()").value(2)));
    }
  }
  ```
  > Confirm the assessment paths used here against `assessment-paths.yaml` (`POST /api/tests`, `POST /api/tests/{id}/questions`, `POST /api/tests/{id}/publish`, `POST /api/tests/{id}/attempts`, `PUT /api/attempts/{id}/answers`, `POST /api/attempts/{id}/submit`) and the enrollment body field (`userId`). Drop the unused `body(...)` helper — it's only illustrative; adjust the `percent` assertion to the exact double the JSON serializer emits (assert `awarded`/`maxPoints` as the hard contract and `percent` `closeTo` if the literal is brittle).

- [ ] **Step 2: verify + commit.** `mvn -pl main test -Dtest=CourseGradeIntegrationTest` (Testcontainers Postgres). Then full `mvn -q test` once. `mvn spotless:apply`.
  ```bash
  git add backend/main/src/test/java/de/codillas/integration/gradebook/CourseGradeIntegrationTest.java
  git commit -m "test(gradebook): weighted course-grade end-to-end (homework + test, Awaitility)"
  ```

---

## Self-Review

**Spec coverage (Batch 4 Decisions table):**
- Late penalty `Assignment.latePenaltyPctPerDay`/`maxLatePenaltyPct` (nullable=off), pure `GradeCalculator` → `effectiveScore`, raw + effective stored on `Grade` → Tasks 1, 2, 3. ✓
- Calculator unit-tested on-time / 1 day / capped / penalty-off → Task 1. ✓
- Rubric (`Rubric` + ordered `RubricCriterion{label,maxPoints}`), `Assignment.rubricId` (nullable), grade = Σ criterion scores overriding free-form points, OpenAPI authoring + grading-with-rubric → Tasks 2, 3. ✓ (per-criterion breakdown placed on `Grade`, documented deviation from "Review carries").
- Events carry `awarded` + `maxPoints` + `groupId`; homework + assessment publishers updated → Task 4. ✓
- `ProgressEntry` gains `maxPoints`/`groupId` (Liquibase alter); listener populates; idempotent upsert unchanged → Task 4. ✓
- Final course grade = points-weighted percent `Σawarded/Σmax` per `(student,group)` + per-type breakdown; new gradebook response field; weighted result integration-tested with Awaitility → Tasks 5, 6. ✓
- Spec names used verbatim: `GradeCalculator`, `effectiveScore`, `awarded`, `maxPoints`. ✓
- Assumes Batch 1 (`isStaff`, `findByIdForCaller`) and Batch 3 (best-attempt) without redefining; builds on P1's `Submission.late`/`submittedAt` + grade upsert. ✓

**The cross-module wrinkle (groupId):** homework fills `groupId` from `Assignment.groupId`; assessment publishes `null` (a test is lesson-scoped, shared across cohorts, and assessment has no enrollment dependency), and `gradebook` resolves it from its own `gradebook_membership` read model. Documented in "The one cross-module wrinkle" section and Task 4 Step 6. Multi-group precision deferred. ✓

**Placeholder scan:** no TBD/TODO; calculators + their tests + entities + events + the `gradeSubmission` rewrite + the gradebook upsert + Liquibase + OpenAPI + the integration test are full code. Two inline "confirm against the generated DTO / actual paths" notes are verification reminders, not gaps. ✓

**Build-green discipline:** the only inherently atomic change (shared record arity) is contained in Task 4, which updates every producer/consumer + their tests together; every other task compiles and tests green on its own. `ApplicationModules...verify()` unaffected (no new cross-module imports — only event fields). ✓
