# P2a — Assessment depth Implementation Plan

> **Status (2026-07-03): IMPLEMENTED** on `feat/lms-hardening` — landed in commit `6af4743`
> (backend P0–P2c) plus the frontend integration commits that follow it. The checkboxes below
> were never ticked during execution; treat this banner, the code, and the git history as the
> source of truth, not the boxes.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the `assessment` module real test depth — multiple attempts with a cap, a server-side timer, an availability window, proportional partial credit for `MULTIPLE_CHOICE`, and a deterministic per-attempt shuffle of questions/options in the taker view — plus best-attempt-of-record semantics in `gradebook`. Implements Batch 3 of `docs/superpowers/specs/2026-06-28-lms-hardening-design.md`. Follow its Decisions table exactly.

**Architecture:** API-first below `backend/openapi/assessment-*.yaml`, then TDD per `backend/AGENTS.md` (anemic entities + state machines, pure domain helpers, MapStruct mappers, Liquibase per module, unit tests in the module / integration tests in `main`). All new domain logic — partial credit (`AttemptGrader`) and shuffle (`ShuffleOrder`) — is a **pure helper, unit-tested with no Spring**. The timer, attempt-cap and window live in `AttemptServiceImpl`; the shuffle is applied **only in the read path** (`TestServiceImpl.getTest`) and never leaks `Option.correct`. Best-attempt scoring is a `gradebook` read-model change (no event-payload change — see Cross-batch note).

**Tech Stack:** Java 25, Spring Boot 4 / Spring Modulith, Maven multi-module, MapStruct (`CentralMapperConfig`), Liquibase (Postgres), OpenAPI generator, JUnit 5 + Mockito + AssertJ (module units), Testcontainers + MockMvc (`main` integration).

## Global Constraints

- **API-first.** Edit the OpenAPI spec under `backend/openapi/` first, regenerate (`mvn -pl assessment -am compile`), then TDD below it. Generated code under `target/generated-sources` is read-only.
- **Layering & style per `backend/AGENTS.md`** — thin controller `implements AssessmentApi` → `@Transactional` service → repository → anemic `@Entity` extends `BaseAuditableEntity`; MapStruct mappers only (no hand builders); `shared.exception` `NotFoundException`/`ConflictException`/`BadRequestException`; `Sort` constants from the static metamodel; one top-level type per file; constructor injection only.
- **Tests:** pure unit (`@ExtendWith(MockitoExtension.class)`, no Spring) live in the module; DB/controller integration tests extend `BaseIntegrationTest` in `main`. Every test class and method carries a `@DisplayName`. Keep `ApplicationModules.of(...).verify()` green.
- **Versioning.** Bump the touched modules' Maven minor and the OpenAPI `info.version` per `backend/AGENTS.md`. This plan targets **assessment `0.3.0`** and **gradebook `0.3.0`** and places Liquibase changes under `changes/0.3.0/`. *Caveat:* this assumes the P0→P1 batches (which run first per the spec's execution order, and touch both modules) already advanced them to `0.2.0`. If a module is still at `0.1.0-SNAPSHOT` when you start, use `0.2.0` instead and name the changeset directory to match.
- **Precondition from earlier batches (do not re-add):** P1 already added `@Version Long version` to `Attempt`. Leave it. P0 already added `CurrentUser.isStaff()`; this plan does **not** depend on it (shuffle gates on "caller has an attempt", not on role).
- **No new dependencies. No commits pushed.** Commit after each task (`feat(assessment): …` / `feat(gradebook): …`). Format with `mvn spotless:apply` before each commit.
- Verify commands (run from `backend/`): `mvn -pl assessment -am test`, `mvn -pl gradebook -am test`, `mvn -q compile`, `mvn spotless:check`.

## File Structure

```
backend/
  openapi/
    assessment-schemas.yaml                     MOD  — Test config fields, Attempt fields, version → 0.3.0
    assessment-paths.yaml                        MOD  — info.version → 0.3.0
  assessment/
    pom.xml                                      MOD  — version → 0.3.0-SNAPSHOT
    src/main/resources/db/changelog/
      assessment-changelog.yaml                  MOD  — include the two 0.3.0 changesets
      changes/0.3.0/0.3.0-add-test-config.yaml   NEW  — tests config columns
      changes/0.3.0/0.3.0-attempt-number.yaml    NEW  — attempts: attempt_number, started_at, unique swap
    src/main/java/de/codillas/assessment/
      domain/model/Test.java                     MOD  — maxAttempts, durationMinutes, availableFrom/Until, shuffleQuestions/Options
      domain/model/Attempt.java                  MOD  — attemptNumber, startedAt, unique (test,student,attemptNumber)
      domain/AttemptGrader.java                  MOD  — proportional MULTIPLE_CHOICE branch
      domain/ShuffleOrder.java                   NEW  — pure deterministic order helper
      domain/repository/AttemptRepository.java   MOD  — in-progress finder, list-all, latest finder
      mapper/AttemptMapper.java                  MOD  — toEntity(testId, studentId, attemptNumber, startedAt)
      service/AttemptServiceImpl.java            MOD  — cap + window + timer + finalizeAttempt
      service/TestServiceImpl.java               MOD  — apply shuffle in getTest taker path
    src/test/java/de/codillas/assessment/
      domain/AttemptGraderTest.java              MOD  — partial-credit edge cases
      domain/ShuffleOrderTest.java               NEW  — determinism
      service/AttemptServiceTest.java            MOD  — cap, window, next-number, timer
  gradebook/
    pom.xml                                      MOD  — version → 0.3.0-SNAPSHOT
    src/main/java/de/codillas/gradebook/
      domain/repository/ProgressEntryRepository.java  MOD  — findBySourceAndReferenceIdAndStudentId
      service/GradebookServiceImpl.java          MOD  — recordAttempt keeps the best score
    src/test/java/de/codillas/gradebook/service/
      GradebookServiceTest.java                  MOD  — best-attempt-of-record cases
  main/src/test/java/de/codillas/integration/assessment/
    AssessmentControllerIntegrationTest.java     MOD  — attempt-cap 409, window 409
```

**Key facts (verified against the codebase):**
- `Test` is anemic; lifecycle in `TestStateMachine`. `Attempt` status (`IN_PROGRESS→SUBMITTED→GRADED`) is driven by `AttemptStateMachine`; today the unique constraint is `(test_id, student_id)` named `uq_attempts_test_student`.
- `AttemptServiceImpl.startAttempt` currently resumes the single existing attempt; `submitAttempt` grades via `AttemptGrader` and publishes `AttemptCompleted(attemptId, testId, studentId, score)`.
- `AttemptGrader.autoScore` returns `null` for non-choice types; today all choice types are exact-set all-or-nothing.
- `TestServiceImpl.toTestResponse` builds `QuestionResponse`s in `QuestionRepository.BY_ORDER`; `QuestionMapper.toOptionResponse` already drops `Option.correct`.
- `gradebook.GradebookServiceImpl.recordAttempt` upserts a `ProgressEntry` keyed by `(GradeSource.TEST, attemptId)` — one row **per attempt**. `ProgressEntry` carries `referenceId = testId` already.
- `Instant.now()` is fine for the timer/window because tests pin `startedAt` / `availableUntil` far in the past — no `Clock` bean is introduced.

---

### Task 1: OpenAPI — Test config + Attempt fields (+ version bump)

**Files:** Modify `backend/openapi/assessment-schemas.yaml`, `backend/openapi/assessment-paths.yaml`.

- [ ] **Step 1: Bump versions.** In both files set `info.version: 0.3.0`.

- [ ] **Step 2: `CreateTestRequest` — add config (all optional; booleans default false).** Append to its `properties` in `assessment-schemas.yaml`:
  ```yaml
        maxAttempts:
          type: integer
          minimum: 1
          description: Max attempts a student may start; omit for unlimited.
        durationMinutes:
          type: integer
          minimum: 1
          description: Time limit from the attempt's start; omit for untimed.
        availableFrom:
          type: string
          format: date-time
          description: Attempts may only start at or after this instant.
        availableUntil:
          type: string
          format: date-time
          description: Attempts may only start at or before this instant.
        shuffleQuestions:
          type: boolean
          default: false
        shuffleOptions:
          type: boolean
          default: false
  ```

- [ ] **Step 3: `TestResponse` — expose the same config.** Add the six properties above to `TestResponse.properties`, and add `shuffleQuestions` + `shuffleOptions` to its `required` list (booleans are always present). `maxAttempts`/`durationMinutes`/`availableFrom`/`availableUntil` stay optional. (Leave `TestSummary` unchanged — the taker/wizard read the full `TestResponse`; extra entity fields don't break `toSummary`.)

- [ ] **Step 4: `AttemptResponse` — attempt number + start time.** Add to `AttemptResponse.properties`:
  ```yaml
        attemptNumber:
          type: integer
          description: 1-based ordinal of this attempt for the (test, student).
        startedAt:
          type: string
          format: date-time
  ```
  Add `attemptNumber` to `AttemptResponse.required` (always set); leave `startedAt` optional (null on legacy rows).

- [ ] **Step 5: Regenerate + compile.** Run: `cd backend && mvn -pl assessment -am compile`. Expected: DTOs regenerate with the new accessors; build is green (entities not yet changed — only DTOs are generated here).

- [ ] **Step 6: Commit.**
  ```bash
  git add backend/openapi/assessment-schemas.yaml backend/openapi/assessment-paths.yaml
  git commit -m "feat(assessment): OpenAPI test config + attempt fields (0.3.0)"
  ```

---

### Task 2: Liquibase — `tests` config columns (0.3.0)

**Files:** Create `backend/assessment/src/main/resources/db/changelog/changes/0.3.0/0.3.0-add-test-config.yaml`; modify `assessment-changelog.yaml`.

- [ ] **Step 1: Write the changeset.** Mirror the `addColumn` precondition style of `homework/.../0.2.0-add-due-reminder-sent.yaml`:
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-add-test-config
        author: codillas
        comment: Test depth — attempt cap, timer, availability window, shuffle (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: tests
                  columnName: max_attempts
        changes:
          - addColumn:
              tableName: tests
              columns:
                - column:
                    name: max_attempts
                    type: int
                - column:
                    name: duration_minutes
                    type: int
                - column:
                    name: available_from
                    type: timestamptz
                - column:
                    name: available_until
                    type: timestamptz
                - column:
                    name: shuffle_questions
                    type: boolean
                    defaultValueBoolean: false
                    constraints: { nullable: false }
                - column:
                    name: shuffle_options
                    type: boolean
                    defaultValueBoolean: false
                    constraints: { nullable: false }
  ```

- [ ] **Step 2: Include it.** Append to `assessment-changelog.yaml` (after the `0.1.0` block):
  ```yaml
    # 0.3.0 — assessment depth (attempt cap, timer, window, shuffle)
    - include:
        file: changes/0.3.0/0.3.0-add-test-config.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add backend/assessment/src/main/resources/db/changelog
  git commit -m "feat(assessment): liquibase tests config columns (0.3.0)"
  ```

---

### Task 3: Liquibase — `attempts` number + start + unique swap (0.3.0)

**Files:** Create `backend/assessment/src/main/resources/db/changelog/changes/0.3.0/0.3.0-attempt-number.yaml`; modify `assessment-changelog.yaml`.

The old unique constraint `uq_attempts_test_student (test_id, student_id)` blocks multiple attempts; replace it with `(test_id, student_id, attempt_number)`.

- [ ] **Step 1: Write the changeset.**
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-attempt-number
        author: codillas
        comment: Multiple attempts — attempt_number + started_at; re-key the unique constraint (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: attempts
                  columnName: attempt_number
        changes:
          - addColumn:
              tableName: attempts
              columns:
                - column:
                    name: attempt_number
                    type: int
                    defaultValueNumeric: 1
                    constraints: { nullable: false }
                - column:
                    name: started_at
                    type: timestamptz
          - update:
              tableName: attempts
              columns:
                - column: { name: started_at, valueComputed: created_at }
              where: started_at IS NULL
          - dropUniqueConstraint:
              tableName: attempts
              constraintName: uq_attempts_test_student
          - addUniqueConstraint:
              tableName: attempts
              columnNames: test_id, student_id, attempt_number
              constraintName: uq_attempts_test_student_number
  ```
  `started_at` is nullable in the schema (the entity sets it on creation; legacy rows are back-filled from `created_at` above).

- [ ] **Step 2: Include it** in `assessment-changelog.yaml` directly after the `0.3.0-add-test-config` include:
  ```yaml
    - include:
        file: changes/0.3.0/0.3.0-attempt-number.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 3: Commit.**
  ```bash
  git add backend/assessment/src/main/resources/db/changelog
  git commit -m "feat(assessment): liquibase attempt number + started_at + unique swap (0.3.0)"
  ```

---

### Task 4: Entities — `Test` config + `Attempt` number/start

**Files:** Modify `domain/model/Test.java`, `domain/model/Attempt.java`.

- [ ] **Step 1: `Test` fields.** Add inside the class (keep it anemic — no behaviour):
  ```java
    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_until")
    private Instant availableUntil;

    @Column(name = "shuffle_questions", nullable = false)
    @Builder.Default
    private boolean shuffleQuestions = false;

    @Column(name = "shuffle_options", nullable = false)
    @Builder.Default
    private boolean shuffleOptions = false;
  ```
  Add `import java.time.Instant;`.

- [ ] **Step 2: `Attempt` fields + unique constraint.** Change the `@Table` to the three-column unique constraint and add the fields:
  ```java
  @Table(
      name = "attempts",
      uniqueConstraints =
          @UniqueConstraint(columnNames = {"test_id", "student_id", "attempt_number"}))
  ```
  ```java
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private int attemptNumber = 1;

    @Column(name = "started_at")
    private Instant startedAt;
  ```
  Add `import java.time.Instant;`. (Do **not** touch the `@Version` field added by P1.)

- [ ] **Step 3: Compile.** `cd backend && mvn -pl assessment -am compile`. The exhaustive `TestMapper.toResponse`/`AttemptMapper.toResponse` now map the new DTO fields from the entity by name — expect green. (`ddl-auto=validate` is only exercised in integration; the 0.3.0 changesets match these columns.)

- [ ] **Step 4: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment/domain/model
  git commit -m "feat(assessment): Test config + Attempt number/startedAt fields"
  ```

---

### Task 5: `AttemptGrader` — proportional partial credit (TDD)

Per the spec: `MULTIPLE_CHOICE` scores `points * max(0, (correct − incorrect) / totalCorrect)`, rounded to the nearest integer, **never below 0**. `SINGLE_CHOICE` / `TRUE_FALSE` stay exact-set all-or-nothing. The existing `multipleChoice` test encodes the old all-or-nothing rule and **must be replaced**.

**Files:** Modify `domain/AttemptGrader.java`, `src/test/java/de/codillas/assessment/domain/AttemptGraderTest.java`.

- [ ] **Step 1 (RED): Replace the `multipleChoice` test** with the partial-credit edge cases (all-correct, all-wrong, partial, empty, over-select, never-below-zero). Keep `singleChoice`, `shortText_isNull`, `code_isNull`, `totalScore_sumsGraded`, `allGraded`:
  ```java
    @Test
    @DisplayName(
        "multiple choice: proportional partial credit, penalises wrong picks, never below zero")
    void multipleChoice_partialCredit() {
      // 3 correct (A, C, D), 2 incorrect (B, E); points = 6 → each correct worth 2.
      UUID D = UUID.randomUUID();
      UUID E = UUID.randomUUID();
      List<Option> options =
          List.of(
              option(A, true), option(B, false), option(C, true), option(D, true), option(E, false));
      Question q = question(QuestionType.MULTIPLE_CHOICE, 6);

      assertThat(grader.autoScore(q, options, answer(Set.of(A, C, D)))).isEqualTo(6); // all correct
      assertThat(grader.autoScore(q, options, answer(Set.of(A, C)))).isEqualTo(4); // partial 2/3
      assertThat(grader.autoScore(q, options, answer(Set.of(A)))).isEqualTo(2); // partial 1/3
      assertThat(grader.autoScore(q, options, answer(Set.of()))).isZero(); // empty selection
      assertThat(grader.autoScore(q, options, answer(Set.of(B, E)))).isZero(); // all wrong → clamp
      assertThat(grader.autoScore(q, options, answer(Set.of(A, C, D, B, E)))).isEqualTo(2); // (3-2)/3
      assertThat(grader.autoScore(q, options, answer(Set.of(A, B)))).isZero(); // (1-1)/3 → 0
    }
  ```
  Run: `cd backend && mvn -pl assessment test -Dtest=AttemptGraderTest` → RED.

- [ ] **Step 2 (GREEN): Branch the grader.** Replace `autoScore` in `AttemptGrader.java`:
  ```java
    /** Points for one answer, or {@code null} when the question needs manual grading. */
    public Integer autoScore(Question question, List<Option> questionOptions, Answer answer) {
      QuestionType type = question.getType();
      if (!type.isChoice()) {
        return null;
      }
      Set<UUID> correct =
          questionOptions.stream()
              .filter(Option::isCorrect)
              .map(Option::getId)
              .collect(Collectors.toSet());
      if (type == QuestionType.MULTIPLE_CHOICE) {
        return partialScore(question.getPoints(), correct, answer.getSelectedOptionIds());
      }
      // SINGLE_CHOICE / TRUE_FALSE: exactly the correct set earns the points.
      return answer.getSelectedOptionIds().equals(correct) ? question.getPoints() : 0;
    }

    /** {@code points * max(0, (correct − incorrect) / totalCorrect)}, rounded; never below 0. */
    private int partialScore(int points, Set<UUID> correct, Set<UUID> selected) {
      if (correct.isEmpty()) {
        return 0;
      }
      long correctPicks = selected.stream().filter(correct::contains).count();
      long wrongPicks = selected.size() - correctPicks;
      double ratio = (correctPicks - wrongPicks) / (double) correct.size();
      return (int) Math.max(0, Math.round(points * ratio));
    }
  ```
  Add `import de.codillas.assessment.domain.model.QuestionType;`. Run the test → GREEN. (The `singleChoice` test still passes — SINGLE stays exact-match.)

- [ ] **Step 3: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment/domain/AttemptGrader.java backend/assessment/src/test/java/de/codillas/assessment/domain/AttemptGraderTest.java
  git commit -m "feat(assessment): proportional partial credit for multiple choice"
  ```

---

### Task 6: `ShuffleOrder` — pure deterministic order helper (TDD)

A seed-by-`attemptId` helper that reorders any list deterministically, and a `combine(questionId)` to give each question's options their own stable order. Pure — no Spring.

**Files:** Create `domain/ShuffleOrder.java`, `src/test/java/de/codillas/assessment/domain/ShuffleOrderTest.java`.

- [ ] **Step 1 (RED): Test determinism.**
  ```java
  package de.codillas.assessment.domain;

  import static org.assertj.core.api.Assertions.assertThat;

  import java.util.List;
  import java.util.UUID;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;

  @DisplayName("ShuffleOrder")
  class ShuffleOrderTest {

    private static final List<Integer> ITEMS = List.of(1, 2, 3, 4, 5, 6, 7, 8);

    @Test
    @DisplayName("same seed yields the same order and the same multiset")
    void deterministic() {
      UUID seed = UUID.randomUUID();
      List<Integer> a = ShuffleOrder.seededBy(seed).apply(ITEMS);
      List<Integer> b = ShuffleOrder.seededBy(seed).apply(ITEMS);
      assertThat(a).isEqualTo(b).containsExactlyInAnyOrderElementsOf(ITEMS);
    }

    @Test
    @DisplayName("different seeds (or combined keys) generally produce different orders")
    void variesBySeed() {
      List<Integer> a = ShuffleOrder.seededBy(UUID.randomUUID()).apply(ITEMS);
      List<Integer> b = ShuffleOrder.seededBy(UUID.randomUUID()).apply(ITEMS);
      List<Integer> combined =
          ShuffleOrder.seededBy(UUID.randomUUID()).combine(UUID.randomUUID()).apply(ITEMS);
      assertThat(a).isNotEqualTo(b);
      assertThat(combined).containsExactlyInAnyOrderElementsOf(ITEMS);
    }

    @Test
    @DisplayName("apply does not mutate the input list")
    void pure() {
      List<Integer> input = List.of(1, 2, 3);
      ShuffleOrder.seededBy(UUID.randomUUID()).apply(input);
      assertThat(input).containsExactly(1, 2, 3);
    }
  }
  ```

- [ ] **Step 2 (GREEN): Implement `ShuffleOrder`.**
  ```java
  package de.codillas.assessment.domain;

  import java.util.ArrayList;
  import java.util.Collections;
  import java.util.List;
  import java.util.Random;
  import java.util.UUID;

  /**
   * Deterministic, seedable ordering for the taker view. Seeded by an attempt id so the order is
   * stable across reloads yet differs per student; {@link #combine(UUID)} mixes in a question id so
   * each question's options shuffle independently. Pure — no Spring, no correctness data.
   */
  public final class ShuffleOrder {

    private final long seed;

    private ShuffleOrder(long seed) {
      this.seed = seed;
    }

    public static ShuffleOrder seededBy(UUID id) {
      return new ShuffleOrder(mix(id));
    }

    /** A child order keyed by {@code id} (e.g. a question id) — distinct from the parent. */
    public ShuffleOrder combine(UUID id) {
      return new ShuffleOrder(seed * 1099511628211L ^ mix(id));
    }

    /** A new list holding {@code items} in this order; the input is never mutated. */
    public <T> List<T> apply(List<T> items) {
      List<T> copy = new ArrayList<>(items);
      Collections.shuffle(copy, new Random(seed));
      return copy;
    }

    private static long mix(UUID id) {
      return id.getMostSignificantBits() ^ id.getLeastSignificantBits();
    }
  }
  ```
  Run: `cd backend && mvn -pl assessment test -Dtest=ShuffleOrderTest` → GREEN.

- [ ] **Step 3: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment/domain/ShuffleOrder.java backend/assessment/src/test/java/de/codillas/assessment/domain/ShuffleOrderTest.java
  git commit -m "feat(assessment): deterministic ShuffleOrder helper"
  ```

---

### Task 7: `AttemptServiceImpl` — multiple attempts (cap + number + window) (TDD)

`startAttempt` now: resume an `IN_PROGRESS` attempt if one exists; otherwise validate the test is published, within its availability window, and under the attempt cap, then create the next `attemptNumber` with `startedAt = now`.

**Files:** Modify `domain/repository/AttemptRepository.java`, `mapper/AttemptMapper.java`, `service/AttemptServiceImpl.java`, `src/test/java/de/codillas/assessment/service/AttemptServiceTest.java`.

- [ ] **Step 1: Repository finders.** Replace the single `findByTestIdAndStudentId` with the three the service needs:
  ```java
  import java.util.List;
  // ...
    /** All of a student's attempts at a test (for the cap count + next number). */
    List<Attempt> findByTestIdAndStudentId(UUID testId, UUID studentId);

    /** The student's resumable (in-progress) attempt, if any. */
    Optional<Attempt> findByTestIdAndStudentIdAndStatus(
        UUID testId, UUID studentId, AttemptStatus status);

    /** The student's most recent attempt — its id seeds the taker shuffle. */
    Optional<Attempt> findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(
        UUID testId, UUID studentId);
  ```
  Add `import de.codillas.assessment.domain.model.AttemptStatus;`.

- [ ] **Step 2: Mapper factory takes number + start.** In `AttemptMapper`, change the entity factory:
  ```java
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Attempt toEntity(UUID testId, UUID studentId, int attemptNumber, Instant startedAt);
  ```
  Add `import java.time.Instant;`.

- [ ] **Step 3 (RED): Service tests.** Update `startAttempt_resumesExisting` to use the new in-progress finder, and add cap / window / next-number cases:
  ```java
    @Test
    @DisplayName("startAttempt resumes the student's in-progress attempt instead of creating one")
    void startAttempt_resumesInProgress() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      UUID attemptId = UUID.randomUUID();
      Attempt existing = Attempt.builder().id(attemptId).testId(testId).studentId(studentId).build();
      AttemptResponse dto = mock(AttemptResponse.class);

      when(currentUser.id()).thenReturn(studentId);
      when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
          .thenReturn(Optional.of(existing));
      when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
      when(mapper.toAnswerResponses(List.of())).thenReturn(List.of());
      when(mapper.toResponse(eq(existing), any())).thenReturn(dto);

      assertThat(service.startAttempt(testId)).isSameAs(dto);
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("startAttempt creates the next attempt number once previous ones are finished")
    void startAttempt_createsNextNumber() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      Attempt first = Attempt.builder().attemptNumber(1).status(AttemptStatus.GRADED).build();
      Attempt created = Attempt.builder().id(UUID.randomUUID()).build();
      AttemptResponse dto = mock(AttemptResponse.class);

      when(currentUser.id()).thenReturn(studentId);
      when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
          .thenReturn(Optional.empty());
      when(testRepository.findById(testId))
          .thenReturn(Optional.of(Test.builder().status(TestStatus.PUBLISHED).build()));
      when(repository.findByTestIdAndStudentId(testId, studentId)).thenReturn(List.of(first));
      when(mapper.toEntity(eq(testId), eq(studentId), eq(2), any())).thenReturn(created);
      when(repository.save(created)).thenReturn(created);
      when(mapper.toResponse(eq(created), any())).thenReturn(dto);

      assertThat(service.startAttempt(testId)).isSameAs(dto);
      verify(mapper).toEntity(eq(testId), eq(studentId), eq(2), any());
    }

    @Test
    @DisplayName("startAttempt rejects a new attempt once the cap is reached")
    void startAttempt_capReached() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      when(currentUser.id()).thenReturn(studentId);
      when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
          .thenReturn(Optional.empty());
      when(testRepository.findById(testId))
          .thenReturn(
              Optional.of(Test.builder().status(TestStatus.PUBLISHED).maxAttempts(2).build()));
      when(repository.findByTestIdAndStudentId(testId, studentId))
          .thenReturn(
              List.of(
                  Attempt.builder().attemptNumber(1).status(AttemptStatus.GRADED).build(),
                  Attempt.builder().attemptNumber(2).status(AttemptStatus.GRADED).build()));

      assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
          .isThrownBy(() -> service.startAttempt(testId));
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("startAttempt rejects when the test's availability window has closed")
    void startAttempt_windowClosed() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      when(currentUser.id()).thenReturn(studentId);
      when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
          .thenReturn(Optional.empty());
      when(testRepository.findById(testId))
          .thenReturn(
              Optional.of(
                  Test.builder()
                      .status(TestStatus.PUBLISHED)
                      .availableUntil(java.time.Instant.now().minusSeconds(60))
                      .build()));

      assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
          .isThrownBy(() -> service.startAttempt(testId));
      verify(repository, never()).save(any());
    }
  ```
  Keep the existing `startAttempt_notPublished`, `submitAttempt_*` tests; delete the old `startAttempt_resumesExisting`. Add `import de.codillas.assessment.domain.model.AttemptStatus;`. Run → RED.

- [ ] **Step 4 (GREEN): Rewrite `startAttempt` + add window guard.** Replace the method and add the helper:
  ```java
    @Override
    @Transactional
    public AttemptResponse startAttempt(UUID testId) {
      UUID studentId = currentUser.id();
      // Resume an in-progress attempt; never open a new number while one is unfinished.
      Optional<Attempt> inProgress =
          repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS);
      if (inProgress.isPresent()) {
        Attempt attempt = inProgress.get();
        return mapper.toResponse(
            attempt, mapper.toAnswerResponses(answerRepository.findByAttemptId(attempt.getId())));
      }
      Test test =
          testRepository.findById(testId).orElseThrow(() -> new NotFoundException("Test", testId));
      if (test.getStatus() != TestStatus.PUBLISHED) {
        throw new ConflictException("Test is not published");
      }
      assertWithinWindow(test);

      List<Attempt> attempts = repository.findByTestIdAndStudentId(testId, studentId);
      if (test.getMaxAttempts() != null && attempts.size() >= test.getMaxAttempts()) {
        throw new ConflictException("No attempts remaining for this test");
      }
      int next = attempts.stream().mapToInt(Attempt::getAttemptNumber).max().orElse(0) + 1;
      Attempt saved = repository.save(mapper.toEntity(testId, studentId, next, Instant.now()));
      return mapper.toResponse(saved, List.of());
    }

    private void assertWithinWindow(Test test) {
      Instant now = Instant.now();
      if (test.getAvailableFrom() != null && now.isBefore(test.getAvailableFrom())) {
        throw new ConflictException("Test is not yet available");
      }
      if (test.getAvailableUntil() != null && now.isAfter(test.getAvailableUntil())) {
        throw new ConflictException("Test is no longer available");
      }
    }
  ```
  Add `import java.time.Instant;`. Run → GREEN.

- [ ] **Step 5: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment backend/assessment/src/test/java/de/codillas/assessment/service/AttemptServiceTest.java
  git commit -m "feat(assessment): multiple attempts with cap, number and availability window"
  ```

---

### Task 8: `AttemptServiceImpl` — server-side timer (TDD)

The server is the clock: once `startedAt + durationMinutes` has passed, `saveAnswer` is rejected and the attempt is auto-finalized (graded) server-side. **Decision (flagged):** `submitAttempt` always finalizes (it *is* finalization) and does not 409 on expiry — only further answer edits are blocked. Extract the grade-and-publish body into a private `finalizeAttempt` reused by both submit and the expiry path.

**Files:** Modify `service/AttemptServiceImpl.java`, `AttemptServiceTest.java`.

- [ ] **Step 1 (RED): Timer test.** Add to `AttemptServiceTest`:
  ```java
    @Test
    @DisplayName("saveAnswer past the time limit auto-finalizes the attempt and is rejected (409)")
    void saveAnswer_expired_finalizesAndRejects() {
      UUID attemptId = UUID.randomUUID();
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      Attempt attempt =
          Attempt.builder()
              .id(attemptId)
              .testId(testId)
              .studentId(studentId)
              .startedAt(java.time.Instant.now().minus(java.time.Duration.ofHours(2)))
              .build();

      when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(testRepository.findById(testId))
          .thenReturn(Optional.of(Test.builder().durationMinutes(60).build()));
      when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
      when(questionRepository.findByTestId(eq(testId), any())).thenReturn(List.of());
      when(grader.totalScore(any())).thenReturn(0);
      when(grader.allGraded(any())).thenReturn(true);
      when(mapper.toResponse(any(), any())).thenReturn(mock(AttemptResponse.class));

      assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
          .isThrownBy(() -> service.saveAnswer(attemptId, mock(SaveAnswerRequest.class)));
      verify(stateMachine).transitionTo(attempt, AttemptStatus.GRADED);
      verify(events).publishEvent(any(AttemptCompleted.class));
      verify(answerRepository, never()).save(any());
    }
  ```
  Add `import de.codillas.assessment.api.dto.SaveAnswerRequest;` and `import static org.mockito.ArgumentMatchers.eq;` is already present. Run → RED.

- [ ] **Step 2 (GREEN): Add the deadline guard + refactor submit.** In `AttemptServiceImpl`:
  - At the top of `saveAnswer`, after `stateMachine.assertInProgress(attempt);` add `enforceDeadline(attempt);`.
  - Replace the body of `submitAttempt` with:
    ```java
      @Override
      @Transactional
      public AttemptResponse submitAttempt(UUID attemptId) {
        Attempt attempt = findByIdOrThrow(attemptId);
        stateMachine.assertInProgress(attempt);
        return finalizeAttempt(attempt);
      }
    ```
  - Add the two private methods (move the existing grade-and-publish logic into `finalizeAttempt`):
    ```java
      /** Grade every answer, score + transition the attempt, publish AttemptCompleted. */
      private AttemptResponse finalizeAttempt(Attempt attempt) {
        List<Answer> answers = answerRepository.findByAttemptId(attempt.getId());
        Map<UUID, Question> questionsById =
            questionRepository.findByTestId(attempt.getTestId(), QuestionRepository.BY_ORDER).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<UUID, List<Option>> optionsByQuestion =
            questionsById.isEmpty()
                ? Map.of()
                : optionRepository.findByQuestionIdIn(questionsById.keySet()).stream()
                    .collect(Collectors.groupingBy(Option::getQuestionId));

        for (Answer answer : answers) {
          Question question = questionsById.get(answer.getQuestionId());
          if (question != null) {
            answer.setAwardedPoints(
                grader.autoScore(
                    question, optionsByQuestion.getOrDefault(question.getId(), List.of()), answer));
          }
        }
        answerRepository.saveAll(answers);

        attempt.setScore(grader.totalScore(answers));
        stateMachine.transitionTo(
            attempt, grader.allGraded(answers) ? AttemptStatus.GRADED : AttemptStatus.SUBMITTED);
        repository.save(attempt);
        events.publishEvent(
            new AttemptCompleted(
                attempt.getId(), attempt.getTestId(), attempt.getStudentId(), attempt.getScore()));
        return mapper.toResponse(attempt, mapper.toAnswerResponses(answers));
      }

      /** When the time limit has elapsed, auto-finalize and reject further edits. */
      private void enforceDeadline(Attempt attempt) {
        Test test =
            testRepository
                .findById(attempt.getTestId())
                .orElseThrow(() -> new NotFoundException("Test", attempt.getTestId()));
        Integer minutes = test.getDurationMinutes();
        if (minutes == null || attempt.getStartedAt() == null) {
          return;
        }
        if (Instant.now().isAfter(attempt.getStartedAt().plus(Duration.ofMinutes(minutes)))) {
          finalizeAttempt(attempt);
          throw new ConflictException("Attempt time has expired");
        }
      }
    ```
  Add `import java.time.Duration;` (Instant already imported in Task 7). Run → GREEN; the existing `submitAttempt_*` tests still pass (`finalizeAttempt` reproduces the old sequence).

- [ ] **Step 3: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment/service/AttemptServiceImpl.java backend/assessment/src/test/java/de/codillas/assessment/service/AttemptServiceTest.java
  git commit -m "feat(assessment): server-side attempt timer with auto-finalize"
  ```

---

### Task 9: `TestServiceImpl` — shuffle in the taker read path (TDD)

`getTest` applies `ShuffleOrder` only when the caller already has an attempt for the test (i.e. a student taking it) and the test's shuffle flags are on; teachers (no attempt) and authoring flows get builder order. Correct-answer flags are still stripped by `QuestionMapper`.

**Files:** Modify `service/TestServiceImpl.java`, `AttemptServiceTest`'s sibling `TestServiceTest.java` (add a case).

- [ ] **Step 1: Inject the seed source.** Add fields to `TestServiceImpl` and update `toTestResponse`:
  ```java
    private final CurrentUser currentUser;
    private final AttemptRepository attemptRepository;
  ```
  ```java
    @Override
    public TestResponse getTest(UUID testId) {
      Test test = findByIdOrThrow(testId);
      UUID seed =
          attemptRepository
              .findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(testId, currentUser.id())
              .map(Attempt::getId)
              .orElse(null);
      return toTestResponse(test, seed);
    }
  ```
  Change the `publishTest` return to `toTestResponse(repository.save(test), null)` and update `toTestResponse` to take the seed and order accordingly:
  ```java
    private TestResponse toTestResponse(Test test, UUID shuffleSeed) {
      List<Question> questions =
          questionRepository.findByTestId(test.getId(), QuestionRepository.BY_ORDER);
      if (shuffleSeed != null && test.isShuffleQuestions()) {
        questions = ShuffleOrder.seededBy(shuffleSeed).apply(questions);
      }
      Map<UUID, List<Option>> optionsByQuestion =
          questions.isEmpty()
              ? Map.of()
              : optionRepository
                  .findByQuestionIdIn(questions.stream().map(Question::getId).toList())
                  .stream()
                  .sorted(Comparator.comparingInt(Option::getPosition))
                  .collect(Collectors.groupingBy(Option::getQuestionId));

      List<QuestionResponse> questionResponses =
          questions.stream()
              .map(
                  q -> {
                    List<Option> options = optionsByQuestion.getOrDefault(q.getId(), List.of());
                    if (shuffleSeed != null && test.isShuffleOptions()) {
                      options = ShuffleOrder.seededBy(shuffleSeed).combine(q.getId()).apply(options);
                    }
                    return questionMapper.toResponse(
                        q, questionMapper.toOptionResponses(options));
                  })
              .toList();
      return mapper.toResponse(test, questionResponses);
    }
  ```
  Add imports: `de.codillas.assessment.domain.ShuffleOrder`, `de.codillas.assessment.domain.model.Attempt`, `de.codillas.assessment.domain.repository.AttemptRepository`, `de.codillas.shared.security.CurrentUser`.

- [ ] **Step 2: Update `TestServiceTest`.** `TestServiceImpl` now has two more constructor deps — add `@Mock CurrentUser currentUser;` and `@Mock AttemptRepository attemptRepository;` so `@InjectMocks` still wires, and any `getTest` test stubs `currentUser.id()` + `attemptRepository.findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(...)` returning `Optional.empty()` (builder order). Add one case:
  ```java
    @Test
    @DisplayName("getTest returns builder order when the caller has no attempt")
    void getTest_noAttempt_builderOrder() {
      UUID testId = UUID.randomUUID();
      Test test = Test.builder().id(testId).title("t").shuffleQuestions(true).build();
      when(repository.findById(testId)).thenReturn(Optional.of(test));
      when(currentUser.id()).thenReturn(UUID.randomUUID());
      when(attemptRepository.findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(any(), any()))
          .thenReturn(Optional.empty());
      when(questionRepository.findByTestId(eq(testId), any())).thenReturn(List.of());
      when(mapper.toResponse(eq(test), any())).thenReturn(mock(TestResponse.class));

      assertThat(service.getTest(testId)).isNotNull();
    }
  ```
  (Match the existing `TestServiceTest` import/mocks style; if it already exercises `getTest`, just add the two mocks and stub the new collaborators there too.)

- [ ] **Step 3: Verify.** `cd backend && mvn -pl assessment -am test`. Expected: all assessment unit tests GREEN.

- [ ] **Step 4: Commit.**
  ```bash
  git add backend/assessment/src/main/java/de/codillas/assessment/service/TestServiceImpl.java backend/assessment/src/test/java/de/codillas/assessment/service/TestServiceTest.java
  git commit -m "feat(assessment): deterministic per-attempt shuffle in the taker view"
  ```

---

### Task 10: `gradebook` — best-attempt score of record (TDD)

Multiple attempts now emit one `AttemptCompleted` per attempt for the same `(student, test)`. The gradebook must collapse them to one `ProgressEntry` keeping the **best** score. Key TEST entries by `(source, referenceId=testId, studentId)` and keep `max`. **No event-payload change** — `AttemptCompleted` already carries `testId`, `studentId`, `score` (the `awarded`/`maxPoints` extension is Batch 4; see Cross-batch note). HOMEWORK upsert is unchanged.

**Files:** Modify `domain/repository/ProgressEntryRepository.java`, `service/GradebookServiceImpl.java`, `src/test/java/de/codillas/gradebook/service/GradebookServiceTest.java`.

- [ ] **Step 1: Repository finder.** Add to `ProgressEntryRepository`:
  ```java
    Optional<ProgressEntry> findBySourceAndReferenceIdAndStudentId(
        GradeSource source, UUID referenceId, UUID studentId);
  ```

- [ ] **Step 2 (RED): Best-score tests.** Add to `GradebookServiceTest`:
  ```java
    @Test
    @DisplayName("recordAttempt inserts the first attempt's score")
    void recordAttempt_firstAttempt() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      AttemptCompleted event = new AttemptCompleted(UUID.randomUUID(), testId, studentId, 60);
      ProgressEntry fresh = ProgressEntry.builder().score(60).build();
      when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
          .thenReturn(Optional.empty());
      when(mapper.toEntry(event)).thenReturn(fresh);

      service.recordAttempt(event);

      verify(repository).save(fresh);
      assertThat(fresh.getScore()).isEqualTo(60);
    }

    @Test
    @DisplayName("recordAttempt raises the recorded score when a later attempt scores higher")
    void recordAttempt_betterAttempt_updates() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      UUID betterAttemptId = UUID.randomUUID();
      AttemptCompleted event = new AttemptCompleted(betterAttemptId, testId, studentId, 90);
      ProgressEntry existing = ProgressEntry.builder().id(UUID.randomUUID()).score(60).build();
      when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
          .thenReturn(Optional.of(existing));

      service.recordAttempt(event);

      assertThat(existing.getScore()).isEqualTo(90);
      assertThat(existing.getSourceId()).isEqualTo(betterAttemptId);
      verify(repository).save(existing);
    }

    @Test
    @DisplayName("recordAttempt keeps the best score when a later attempt scores lower")
    void recordAttempt_worseAttempt_keepsBest() {
      UUID testId = UUID.randomUUID();
      UUID studentId = UUID.randomUUID();
      AttemptCompleted event = new AttemptCompleted(UUID.randomUUID(), testId, studentId, 40);
      ProgressEntry existing = ProgressEntry.builder().id(UUID.randomUUID()).score(80).build();
      when(repository.findBySourceAndReferenceIdAndStudentId(GradeSource.TEST, testId, studentId))
          .thenReturn(Optional.of(existing));

      service.recordAttempt(event);

      assertThat(existing.getScore()).isEqualTo(80);
      verify(repository, never()).save(any());
    }
  ```
  Add imports: `de.codillas.shared.event.AttemptCompleted`, `de.codillas.gradebook.domain.model.GradeSource` (present). Run → RED.

- [ ] **Step 3 (GREEN): Best-of upsert.** Replace `recordAttempt` in `GradebookServiceImpl`:
  ```java
    @Override
    @Transactional
    public void recordAttempt(AttemptCompleted event) {
      ProgressEntry entry =
          repository
              .findBySourceAndReferenceIdAndStudentId(
                  GradeSource.TEST, event.testId(), event.studentId())
              .orElseGet(() -> mapper.toEntry(event));
      if (entry.getId() == null || event.score() > entry.getScore()) {
        entry.setScore(event.score());
        entry.setSourceId(event.attemptId()); // point at the best attempt
        repository.save(entry);
      }
    }
  ```
  Run: `cd backend && mvn -pl gradebook -am test` → GREEN. (`recordSubmissionGrade` and the shared `upsert` helper are untouched.)

- [ ] **Step 4: Commit.**
  ```bash
  git add backend/gradebook/src/main/java/de/codillas/gradebook backend/gradebook/src/test/java/de/codillas/gradebook/service/GradebookServiceTest.java
  git commit -m "feat(gradebook): keep the best attempt score across retries"
  ```

---

### Task 11: Integration — attempt cap & availability window (`main`)

Add controller-level integration tests for the two new 409 paths. (The timer and partial-credit grading are covered by fast unit tests; a timer integration test would require wall-clock waiting — out of scope, noted.)

**Files:** Modify `main/src/test/java/de/codillas/integration/assessment/AssessmentControllerIntegrationTest.java`.

- [ ] **Step 1: Attempt cap (409).** Add a test that creates a `maxAttempts: 1` test, publishes it, starts + submits one attempt, then a second `startAttempt` returns 409:
  ```java
    @Test
    @DisplayName("a second attempt past the cap is rejected (409)")
    void startAttempt_overCap_conflict() throws Exception {
      UUID teacher = UUID.randomUUID();
      UUID student = UUID.randomUUID();

      String test =
          mockMvc
              .perform(
                  post("/api/tests")
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"title\":\"Capped\",\"maxAttempts\":1}"))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));
      mockMvc
          .perform(post("/api/tests/{testId}/publish", testId).with(as(teacher, "TEACHER")))
          .andExpect(status().isOk());

      String attempt =
          mockMvc
              .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.attemptNumber").value(1))
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID attemptId = UUID.fromString(JsonPath.read(attempt, "$.id"));
      mockMvc
          .perform(post("/api/attempts/{attemptId}/submit", attemptId).with(as(student, "STUDENT")))
          .andExpect(status().isOk());

      // Cap reached → no new attempt.
      mockMvc
          .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
          .andExpect(status().isConflict());
    }
  ```

- [ ] **Step 2: Availability window (409).** Add a test that creates a test whose `availableUntil` is in the past, publishes, and expects `startAttempt` → 409:
  ```java
    @Test
    @DisplayName("starting an attempt after the availability window closes is rejected (409)")
    void startAttempt_afterWindow_conflict() throws Exception {
      UUID teacher = UUID.randomUUID();
      UUID student = UUID.randomUUID();

      String test =
          mockMvc
              .perform(
                  post("/api/tests")
                      .with(as(teacher, "TEACHER"))
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"title\":\"Closed\",\"availableUntil\":\"2000-01-01T00:00:00Z\"}"))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      UUID testId = UUID.fromString(JsonPath.read(test, "$.id"));
      mockMvc
          .perform(post("/api/tests/{testId}/publish", testId).with(as(teacher, "TEACHER")))
          .andExpect(status().isOk());

      mockMvc
          .perform(post("/api/tests/{testId}/attempts", testId).with(as(student, "STUDENT")))
          .andExpect(status().isConflict());
    }
  ```

- [ ] **Step 3: Run.** `cd backend && mvn -pl main -am test -Dtest=AssessmentControllerIntegrationTest`. Expected: GREEN (existing `fullAssessmentFlow` and `startAttempt_twice_resumesSameAttempt` still pass — the resume path is preserved, and `attemptNumber` is now present in the body).

- [ ] **Step 4: Commit.**
  ```bash
  git add backend/main/src/test/java/de/codillas/integration/assessment/AssessmentControllerIntegrationTest.java
  git commit -m "test(assessment): integration coverage for attempt cap and availability window"
  ```

---

### Task 12: Version bumps + module-boundary verify + format

**Files:** Modify `backend/assessment/pom.xml`, `backend/gradebook/pom.xml`.

- [ ] **Step 1: Bump Maven versions.** Set the module `<version>` to `0.3.0-SNAPSHOT` in both `assessment/pom.xml` and `gradebook/pom.xml` (the parent `<version>` reference stays as the reactor's). Confirm the OpenAPI `info.version` is `0.3.0` from Task 1.

- [ ] **Step 2: Full build + boundaries.** Run:
  ```bash
  cd backend && mvn -q compile
  cd backend && mvn -pl assessment -am test && mvn -pl gradebook -am test && mvn -pl main -am test
  ```
  `ApplicationModules.of(...).verify()` (run by the modulith test) must stay green — no new cross-module imports were added (`ShuffleOrder` is module-local; gradebook reads only the `AttemptCompleted` event).

- [ ] **Step 3: Format.**
  ```bash
  cd backend && mvn spotless:apply && mvn spotless:check
  ```

- [ ] **Step 4: Commit.**
  ```bash
  git add backend/assessment/pom.xml backend/gradebook/pom.xml
  git commit -m "chore(assessment,gradebook): bump module versions to 0.3.0"
  ```

---

## Self-Review

**Spec coverage (Batch 3 Decisions):**
- Multiple attempts: `Test.maxAttempts` + `Attempt.attemptNumber`, unique `(test, student, attemptNumber)`, `startAttempt` enforces the cap and creates the next number → Tasks 1–4, 7. ✓
- Timer: `Test.durationMinutes` + `Attempt.startedAt`; `saveAnswer` rejected after `startedAt + duration`, auto-finalized server-side → Tasks 1–4, 8. ✓
- Availability window: optional `availableFrom`/`availableUntil`; outside → 409 → Tasks 1–4, 7, 11. ✓
- Partial credit: proportional `MULTIPLE_CHOICE` (`points * max(0,(correct−incorrect)/totalCorrect)`), SINGLE/TRUE_FALSE unchanged; edge cases all-correct/all-wrong/partial/empty/over-select, never < 0 → Task 5. ✓
- Deterministic shuffle: `shuffleQuestions`/`shuffleOptions` + pure `ShuffleOrder.seededBy(attemptId)`, applied only in the read path, correctness never leaves the server → Tasks 1, 6, 9. ✓
- Gradebook best attempt of record → Task 10. ✓

**Convention compliance:** OpenAPI-first then regenerate (Task 1); anemic entities, logic in services + pure helpers (Tasks 4–9); Liquibase versioned changesets with `not columnExists` preconditions + named unique constraint swap (Tasks 2–3); MapStruct factory extended, no hand builders (Task 7); unit tests in the module, integration in `main` with `@DisplayName` everywhere (Tasks 5–11); module versions bumped, boundaries verified, spotless applied (Task 12). ✓

**Placeholder scan:** no TBD/TODO; every code/test step is complete; verify commands are concrete. ✓

**Flagged decisions (spec gaps / interpretations):**
1. **`submitAttempt` does not 409 on expiry** — only `saveAnswer` is rejected after the deadline; submit always finalizes (submitting *is* finalization, so a 409 there would be hostile and the score identical). The spec lumps "saveAnswer/submit reject"; this is the sensible split.
2. **`startAttempt` resume vs. new** — an `IN_PROGRESS` attempt is resumed (not counted as a new start); the cap/window are checked only when opening a *new* numbered attempt. Preserves the existing resume behaviour (and its integration test) while honouring the cap.
3. **Shuffle gating** — applied when the caller already has an attempt for the test (a taker), not via a role check; this avoids depending on `CurrentUser.isStaff()` and still gives teachers/authors builder order. `MULTIPLE_CHOICE` rounding uses `Math.round` (nearest int), clamped at 0.
4. **Auto-finalize trigger** — an abandoned expired attempt stays `IN_PROGRESS` until the next `saveAnswer`/`submit` (no scheduler; consistent with Batch 2's "single-instance, no distributed lock" stance).

**Cross-batch note (Batch 4 event change):** No conflict. Best-attempt scoring is achieved purely in `gradebook` by re-keying TEST entries to `(testId, studentId)` and keeping `max(score)` — it needs only the existing `AttemptCompleted(attemptId, testId, studentId, score)`. Batch 4 will extend `AttemptCompleted` with `awarded`/`maxPoints`/`groupId` for points-weighted course grades; that is additive and the best-of upsert here continues to work (it will then compare on `awarded`). The event is **not** changed in this batch.
