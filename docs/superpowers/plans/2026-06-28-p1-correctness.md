# P1 — Correctness & integrity hardening (implementation plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the correctness gaps the 2026-06-28 audit found (Batch 2 of `docs/superpowers/specs/2026-06-28-lms-hardening-design.md`): optimistic locking on the two raced aggregates, late-submission flagging, idempotent re-grade, file size/type limits, hard-delete endpoints with intra-module FK cascade + cross-module event cleanup, and wiring the dead email channel.

**Architecture:** Six independent correctness fixes, each shipped TDD-first below its OpenAPI/Liquibase change. No new aggregates; one new SPI-free event-consumer per cross-module cleanup. New cross-module event records (`CourseDeleted`, `GroupDeleted`, `UserEmailChanged`) live in `shared/event/`; consumers own their derived tables and purge by id. Follows the binding chain — `backend/AGENTS.md` (global) → each module's `AGENTS.md`. **This plan references those contracts rather than restating them** (Liquibase changeset format with `not …Exists` + semver, MapStruct policies, events published inside the writer tx & consumed with `@ApplicationModuleListener`, where tests live).

**Tech Stack:** Java 25, Spring Boot 4 / Spring Modulith, Maven multi-module, JPA/Hibernate + Liquibase (Postgres), MapStruct, OpenAPI-generator (API-first), JUnit 5 + Mockito (module units) + Testcontainers/Awaitility (integration in `main`).

## Global Constraints

- **API-first.** Change the spec under `backend/openapi/` first, regenerate, then TDD below it. Generated code is read-only. Errors `$ref` `common.yaml`; times are `Instant`; nullable is jspecify.
- **Layering** per `backend/AGENTS.md`: thin controller `implements <Module>Api` → `@Transactional` service → repository → anemic `@Entity extends BaseAuditableEntity`. MapStruct mappers only. Throw `shared.exception.{NotFoundException,ConflictException,BadRequestException}`.
- **Events:** publish with `ApplicationEventPublisher` **inside the writer's transaction**; consume with `@ApplicationModuleListener`. Cross-module records go in `shared/event/`. Read models own their tables, never join across modules.
- **Persistence:** Liquibase per module — versioned changeset under `db/changelog/changes/<semver>/`, `id: <semver>-<desc>`, `not …Exists` precondition `onFail: MARK_RAN`, included in version order by the module changelog. `ddl-auto=validate`. Use the `Entity_` static metamodel for `Sort`.
- **Security:** role gates via `shared.security` annotations only. **Assume `CurrentUser.isStaff()` and `hasRole(Role)` already exist** (added by Batch 1, `p0-object-authz`) — do not redefine them.
- **Canonical names — use verbatim:** `CourseDeleted`, `GroupDeleted`, `UserEmailChanged`, `recipient_email`.
- **Testing (TDD):** unit tests in the module (`@ExtendWith(MockitoExtension.class)`, no Spring); integration tests in `main` extending `BaseIntegrationTest` (Testcontainers Postgres, real master changelog). Every test class + method carries a `@DisplayName`. Async event flows are asserted with Awaitility. `ApplicationModules.of(...).verify()` stays green.
- **No new infra deps.** Two existing Spring Modulith deps get added to two module poms (Tasks 6, 7). Format with `mvn spotless:apply`. SemVer `0.x.y`. Commit per task; **do not push**.

## File Structure

```
backend/
  shared/src/main/java/de/codillas/shared/
    security/CurrentUser.java                                   MOD  — add `String email()` (Task 8)
    exception/GlobalExceptionHandler.java                       MOD  — 409 on optimistic lock; 413 on oversize (Tasks 1,4)
    event/CourseDeleted.java                                    NEW  — record(UUID courseId) (Task 5)
    event/GroupDeleted.java                                     NEW  — record(UUID groupId) (Task 6)
    event/UserEmailChanged.java                                 NEW  — record(UUID userId, String email) (Task 8)
  shared/src/test/java/.../exception/GlobalExceptionHandlerTest.java  NEW (Task 1)

  main/src/main/java/de/codillas/config/JwtCurrentUser.java     MOD  — read `email` claim (Task 8)
  main/src/test/java/de/codillas/integration/
    assessment/AttemptOptimisticLockIntegrationTest.java        NEW (Task 1)
    course/CourseDeleteIntegrationTest.java                     NEW (Task 5)
    enrollment/GroupDeleteCascadeIntegrationTest.java           NEW (Tasks 6,7)
    notification/EmailChannelIntegrationTest.java               NEW (Task 8)
  main/src/main/resources/application.yml                       MOD  — multipart size + allowed-content-types (Task 4)

  assessment/.../domain/model/Attempt.java                      MOD  — @Version lockVersion (Task 1)
  assessment/.../db/changelog/changes/0.2.0/0.2.0-add-attempt-lock-version.yaml   NEW (Task 1)
  assessment/.../db/changelog/assessment-changelog.yaml         MOD

  homework/.../domain/model/Submission.java                     MOD  — @Version + late + submittedAt (Tasks 1,2)
  homework/.../domain/repository/GradeRepository.java           MOD  — findBySubmissionId (Task 3)
  homework/.../service/SubmissionServiceImpl.java               MOD  — late at submit; grade upsert (Tasks 2,3)
  homework/.../service/HomeworkEventListener.java               NEW  — on(GroupDeleted) (Task 7)
  homework/.../domain/repository/{Assignment,Submission,Review}Repository.java  MOD  — purge finders (Task 7)
  homework/pom.xml                                              MOD  — spring-modulith-events-api (Task 7)
  homework/.../db/changelog/changes/0.3.0/0.3.0-add-submission-lock-version.yaml NEW (Task 1)
  homework/.../db/changelog/changes/0.3.0/0.3.0-add-submission-late.yaml         NEW (Task 2)
  homework/.../db/changelog/homework-changelog.yaml             MOD
  openapi/homework-schemas.yaml                                 MOD  — SubmissionResponse.late (Task 2)

  files/.../config/FilesProperties.java                         MOD  — allowedContentTypes (Task 4)
  files/.../service/FileServiceImpl.java                        MOD  — content-type allow-list (Task 4)

  course/.../service/CourseService(Impl).java                   MOD  — deleteCourse + publish (Task 5)
  course/.../web/CourseController.java                          MOD  — deleteCourse delegate (Task 5)
  course/.../db/changelog/changes/0.3.0/0.3.0-add-course-fk-cascade.yaml         NEW (Task 5)
  course/.../db/changelog/course-changelog.yaml                 MOD
  openapi/course-paths.yaml                                     MOD  — DELETE /api/courses/{courseId} (Task 5)

  enrollment/.../service/GroupService(Impl).java                MOD  — deleteGroup + publish (Task 6)
  enrollment/.../service/EnrollmentEventListener.java           NEW  — on(CourseDeleted) (Task 6)
  enrollment/.../web/EnrollmentController.java                  MOD  — deleteGroup delegate (Task 6)
  enrollment/.../domain/repository/GroupRepository.java         MOD  — findByCourseId already exists; reuse
  enrollment/pom.xml                                            MOD  — spring-modulith-events-api (Task 6)
  enrollment/.../db/changelog/changes/0.3.0/0.3.0-add-enrollment-fk-cascade.yaml NEW (Task 6)
  enrollment/.../db/changelog/enrollment-changelog.yaml         MOD
  openapi/enrollment-paths.yaml                                 MOD  — DELETE /api/groups/{groupId} (Task 6)

  gradebook/.../domain/repository/GradebookMembershipRepository.java  MOD  — deleteByGroupId (Task 7)
  gradebook/.../service/{GradebookEventListener,GradebookService(Impl)}.java  MOD  — on(GroupDeleted) (Task 7)
  notification/.../service/{NotificationEventListener,NotificationService(Impl)}.java  MOD  — on(GroupDeleted) (Task 7)
  notification/.../domain/repository/NotificationMembershipRepository.java  MOD  — deleteByGroupId (Task 7)
  chat/.../service/{ChatEventListener,ChatService(Impl)}.java   MOD  — on(GroupDeleted) (Task 7)
  chat/.../domain/repository/{ChatRoomMember,ChatMessage}Repository.java  MOD  — deleteByRoomId (Task 7)

  user/.../domain/model/Profile.java                            MOD  — email column (Task 8)
  user/.../service/UserServiceImpl.java                         MOD  — capture email + publish (Task 8)
  user/.../db/changelog/changes/0.2.0/0.2.0-add-profile-email.yaml  NEW (Task 8)
  user/.../db/changelog/user-changelog.yaml                     MOD
  notification/.../domain/model/RecipientEmail.java             NEW (Task 8)
  notification/.../domain/repository/RecipientEmailRepository.java  NEW (Task 8)
  notification/.../service/DbRecipientEmailResolver.java        NEW  — replaces UnresolvedRecipientEmailResolver (Task 8)
  notification/.../service/UnresolvedRecipientEmailResolver.java DEL (Task 8)
  notification/.../db/changelog/changes/0.2.0/0.2.0-create-recipient-email.yaml  NEW (Task 8)
  notification/.../db/changelog/notification-changelog.yaml     MOD
```

**Naming decision (spec gap):** the spec says "Liquibase `version` column" for the `@Version` field, but `submissions` already owns a business `version int` (append-only retry number). To avoid the clash the optimistic-lock field is **`lockVersion` → column `lock_version`** on *both* `Attempt` and `Submission` (consistent). Documented again in Task 1.

---

### Task 1: Optimistic locking on `Attempt` & `Submission` (+ 409 on conflict)

The two documented races (attempt-submit double-publish; concurrent submission writes) get an optimistic lock. Hibernate raises `ObjectOptimisticLockingFailureException` **at commit** (outside the service tx), so it cannot be caught and rethrown as `ConflictException` in the service — map it globally to 409.

**Files:** `shared/.../exception/GlobalExceptionHandler.java`, `shared/.../exception/GlobalExceptionHandlerTest.java` (NEW), `assessment/.../domain/model/Attempt.java`, `assessment/.../db/changelog/changes/0.2.0/0.2.0-add-attempt-lock-version.yaml` (NEW) + `assessment-changelog.yaml`, `homework/.../domain/model/Submission.java`, `homework/.../db/changelog/changes/0.3.0/0.3.0-add-submission-lock-version.yaml` (NEW) + `homework-changelog.yaml`, `main/.../integration/assessment/AttemptOptimisticLockIntegrationTest.java` (NEW).

No OpenAPI change: `saveAnswer`, `submitAttempt` (assessment) and `submitSubmission`/`updateSubmission` (homework) **already declare `409 $ref Conflict`**.

- [ ] **Step 1 (RED): handler unit test** — `shared/src/test/java/de/codillas/shared/exception/GlobalExceptionHandlerTest.java`:
  ```java
  package de.codillas.shared.exception;

  import static org.assertj.core.api.Assertions.assertThat;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ProblemDetail;
  import org.springframework.orm.ObjectOptimisticLockingFailureException;

  @DisplayName("GlobalExceptionHandler — concurrency & upload failures")
  class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("maps an optimistic-lock failure to 409 Conflict")
    void optimisticLockIsConflict() {
      ProblemDetail pd =
          handler.handleOptimisticLock(
              new ObjectOptimisticLockingFailureException("attempts", java.util.UUID.randomUUID()));
      assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }
  }
  ```

- [ ] **Step 2 (GREEN): add the handlers** to `GlobalExceptionHandler` (the `MaxUploadSizeExceededException` one is consumed by Task 4 — add it now, it has no other dependency):
  ```java
  import org.springframework.dao.OptimisticLockingFailureException;
  import org.springframework.web.multipart.MaxUploadSizeExceededException;
  // ...

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The resource was modified concurrently; reload and retry");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the size limit");
  }
  ```

- [ ] **Step 3: add `@Version` to `Attempt`** (`assessment/.../domain/model/Attempt.java`) — Hibernate manages the value; column default `0`:
  ```java
  import jakarta.persistence.Version;
  // ...
  @Version
  @Column(name = "lock_version", nullable = false)
  private Long lockVersion;
  ```
  `AttemptMapper.toEntity` already uses `unmappedTargetPolicy = ReportingPolicy.IGNORE`, so the new field needs no mapping. `AttemptResponse` does **not** surface it.

- [ ] **Step 4: Liquibase** — `assessment/.../db/changelog/changes/0.2.0/0.2.0-add-attempt-lock-version.yaml`:
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.2.0-add-attempt-lock-version
        author: codillas
        comment: Add optimistic-lock version column to attempts (0.2.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: attempts
                  columnName: lock_version
        changes:
          - addColumn:
              tableName: attempts
              columns:
                - column:
                    name: lock_version
                    type: bigint
                    defaultValueNumeric: 0
                    constraints: { nullable: false }
  ```
  Add a `0.2.0` section to `assessment-changelog.yaml`:
  ```yaml
    # 0.2.0 — optimistic locking
    - include:
        file: changes/0.2.0/0.2.0-add-attempt-lock-version.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 5: same for `Submission`** (`homework/.../domain/model/Submission.java`) — note the existing business `version int` is untouched; the lock field is named `lockVersion`:
  ```java
  import jakarta.persistence.Version;
  // ...
  @Version
  @Column(name = "lock_version", nullable = false)
  private Long lockVersion;
  ```
  Liquibase `homework/.../db/changelog/changes/0.3.0/0.3.0-add-submission-lock-version.yaml` (same shape, `tableName: submissions`, `columnName: lock_version`), and start a `0.3.0` section in `homework-changelog.yaml`:
  ```yaml
    # 0.3.0 — optimistic locking + late submissions
    - include:
        file: changes/0.3.0/0.3.0-add-submission-lock-version.yaml
        relativeToChangelogFile: true
  ```

- [ ] **Step 6 (integration, in `main`): proves the lock + 409 path** — `main/.../integration/assessment/AttemptOptimisticLockIntegrationTest.java`. Clearing the persistence context yields two independent snapshots; the stale write fails:
  ```java
  package de.codillas.integration.assessment;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  import java.util.UUID;

  import jakarta.persistence.EntityManager;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.orm.ObjectOptimisticLockingFailureException;
  import org.springframework.transaction.annotation.Transactional;

  import de.codillas.assessment.domain.model.Attempt;
  import de.codillas.assessment.domain.model.AttemptStatus;
  import de.codillas.assessment.domain.repository.AttemptRepository;
  import de.codillas.integration.BaseIntegrationTest;

  @Transactional
  @DisplayName("Attempt optimistic locking (integration)")
  class AttemptOptimisticLockIntegrationTest extends BaseIntegrationTest {

    @Autowired AttemptRepository repository;
    @Autowired EntityManager em;

    @Test
    @DisplayName("a stale write over a newer version raises an optimistic-lock failure")
    void staleWriteIsRejected() {
      UUID id =
          repository
              .saveAndFlush(
                  Attempt.builder()
                      .testId(UUID.randomUUID())
                      .studentId(UUID.randomUUID())
                      .status(AttemptStatus.IN_PROGRESS)
                      .build())
              .getId();
      em.clear();

      Attempt stale = repository.findById(id).orElseThrow(); // lock_version 0
      em.detach(stale);
      Attempt fresh = repository.findById(id).orElseThrow(); // lock_version 0
      fresh.setScore(50);
      repository.saveAndFlush(fresh); // -> lock_version 1

      stale.setScore(99);
      assertThatThrownBy(() -> repository.saveAndFlush(stale))
          .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
  }
  ```

- [ ] **Step 7: verify & commit.** `cd backend && mvn -pl shared,assessment,homework,main -am test && mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "fix(correctness): optimistic locking on Attempt/Submission, 409 on conflict"
  ```

---

### Task 2: Late-submission flag (homework)

Flag, don't block: stamp `submittedAt` and compute `late = dueAt != null && submittedAt.isAfter(dueAt)` at submit. Surface `late` in `SubmissionResponse`.

**Files:** `openapi/homework-schemas.yaml`, `homework/.../domain/model/Submission.java`, `homework/.../service/SubmissionServiceImpl.java`, `homework/.../db/changelog/changes/0.3.0/0.3.0-add-submission-late.yaml` (NEW) + `homework-changelog.yaml`, unit test.

- [ ] **Step 1 (OpenAPI): add `late` to `SubmissionResponse`** in `homework-schemas.yaml` (bump `info.version` → `0.2.0`), under `properties`:
  ```yaml
        late:
          type: boolean
          description: True when this submission was submitted after the assignment's due date.
  ```
  (Leave it out of `required` — it is null on a still-DRAFT submission until submit.) Regenerate.

- [ ] **Step 2: entity fields** (`Submission.java`):
  ```java
  import java.time.Instant;
  // ...
  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(nullable = false)
  @Builder.Default
  private boolean late = false;
  ```
  `SubmissionMapper.toResponse` auto-maps `late` (name match); `submittedAt` stays internal (kept for the Batch 4 late-penalty math).

- [ ] **Step 3: Liquibase** — `0.3.0/0.3.0-add-submission-late.yaml`:
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-add-submission-late
        author: codillas
        comment: Add submitted_at + late flag to submissions (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - columnExists:
                  tableName: submissions
                  columnName: late
        changes:
          - addColumn:
              tableName: submissions
              columns:
                - column:
                    name: submitted_at
                    type: timestamptz
                - column:
                    name: late
                    type: boolean
                    defaultValueBoolean: false
                    constraints: { nullable: false }
  ```
  Include it under the `0.3.0` section of `homework-changelog.yaml`, **after** `0.3.0-add-submission-lock-version.yaml`.

- [ ] **Step 4 (RED): unit test** — `homework/src/test/java/de/codillas/homework/service/SubmissionServiceLateTest.java`:
  ```java
  package de.codillas.homework.service;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;

  import java.time.Instant;
  import java.util.Optional;
  import java.util.UUID;

  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.context.ApplicationEventPublisher;

  import de.codillas.homework.domain.SubmissionStateMachine;
  import de.codillas.homework.domain.model.Assignment;
  import de.codillas.homework.domain.model.Submission;
  import de.codillas.homework.domain.repository.AssignmentRepository;
  import de.codillas.homework.domain.repository.GradeRepository;
  import de.codillas.homework.domain.repository.ReviewRepository;
  import de.codillas.homework.domain.repository.SubmissionRepository;
  import de.codillas.homework.mapper.GradeMapper;
  import de.codillas.homework.mapper.ReviewMapper;
  import de.codillas.homework.mapper.SubmissionMapper;
  import de.codillas.shared.security.CurrentUser;

  @ExtendWith(MockitoExtension.class)
  @DisplayName("SubmissionServiceImpl — late flag at submit")
  class SubmissionServiceLateTest {

    @Mock SubmissionRepository repository;
    @Mock ReviewRepository reviewRepository;
    @Mock GradeRepository gradeRepository;
    @Mock AssignmentRepository assignmentRepository;
    @Mock SubmissionMapper mapper;
    @Mock ReviewMapper reviewMapper;
    @Mock GradeMapper gradeMapper;
    @Mock SubmissionStateMachine stateMachine;
    @Mock CurrentUser currentUser;
    @Mock ApplicationEventPublisher events;
    @InjectMocks SubmissionServiceImpl service;

    @Test
    @DisplayName("marks the submission late when submitted after the due date")
    void marksLate() {
      UUID id = UUID.randomUUID();
      UUID assignmentId = UUID.randomUUID();
      Submission submission = Submission.builder().assignmentId(assignmentId).build();
      Assignment past = Assignment.builder().dueAt(Instant.now().minusSeconds(3600)).build();
      when(repository.findById(id)).thenReturn(Optional.of(submission));
      when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(past));
      when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.submitSubmission(id);

      assertThat(submission.isLate()).isTrue();
      assertThat(submission.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("does not mark late when there is no due date")
    void noDueDateNotLate() {
      UUID id = UUID.randomUUID();
      UUID assignmentId = UUID.randomUUID();
      Submission submission = Submission.builder().assignmentId(assignmentId).build();
      when(repository.findById(id)).thenReturn(Optional.of(submission));
      when(assignmentRepository.findById(assignmentId))
          .thenReturn(Optional.of(Assignment.builder().build()));
      when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.submitSubmission(id);

      assertThat(submission.isLate()).isFalse();
    }
  }
  ```

- [ ] **Step 5 (GREEN): set the flag in `submitSubmission`** — inject `AssignmentRepository`, load the assignment, stamp:
  ```java
  private final AssignmentRepository assignmentRepository; // add to the constructor field list
  // ...
  @Override
  @Transactional
  public SubmissionResponse submitSubmission(UUID submissionId) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.SUBMITTED);
    Instant submittedAt = Instant.now();
    Assignment assignment =
        assignmentRepository
            .findById(submission.getAssignmentId())
            .orElseThrow(() -> new NotFoundException("Assignment", submission.getAssignmentId()));
    submission.setSubmittedAt(submittedAt);
    submission.setLate(
        assignment.getDueAt() != null && submittedAt.isAfter(assignment.getDueAt()));
    return mapper.toResponse(repository.save(submission));
  }
  ```

- [ ] **Step 6: verify & commit.** `mvn -pl homework -am test`, `mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "feat(homework): flag late submissions at submit; surface late in DTO"
  ```

---

### Task 3: Idempotent re-grade (homework)

`gradeSubmission` re-grading currently (a) hits the `uq_grades_submission` unique constraint → 500, and (b) the state machine rejects `GRADED → GRADED` → 409. Fix both: upsert the `Grade` by `submissionId`, and only transition when not already `GRADED`.

**Files:** `homework/.../domain/repository/GradeRepository.java`, `homework/.../service/SubmissionServiceImpl.java`, unit test.

- [ ] **Step 1: repository finder** (`GradeRepository.java`):
  ```java
  import java.util.Optional;
  // ...
  Optional<Grade> findBySubmissionId(UUID submissionId);
  ```

- [ ] **Step 2 (RED): unit test** — `homework/.../service/SubmissionServiceGradeTest.java`:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("SubmissionServiceImpl — grade upsert")
  class SubmissionServiceGradeTest {
    // same @Mock field set + @InjectMocks SubmissionServiceImpl service as Task 2

    @Test
    @DisplayName("re-grading updates the existing grade instead of inserting a second")
    void reGradeUpserts() {
      UUID submissionId = UUID.randomUUID();
      UUID teacher = UUID.randomUUID();
      Submission graded =
          Submission.builder().assignmentId(UUID.randomUUID()).studentId(UUID.randomUUID()).build();
      graded.setStatus(SubmissionStatus.GRADED);
      Grade existing = Grade.builder().submissionId(submissionId).score(60).gradedBy(teacher).build();

      when(repository.findById(submissionId)).thenReturn(Optional.of(graded));
      when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));
      when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(currentUser.id()).thenReturn(teacher);
      when(gradeMapper.toResponse(any())).thenReturn(null);

      service.gradeSubmission(submissionId, new CreateGradeRequest().score(95));

      assertThat(existing.getScore()).isEqualTo(95); // mutated, not recreated
      verify(gradeMapper, never()).toEntity(any(), any(), any()); // no new Grade built
      verifyNoInteractions(stateMachine); // already GRADED -> no transition
    }
  }
  ```

- [ ] **Step 3 (GREEN): upsert in `gradeSubmission`:**
  ```java
  @Override
  @Transactional
  public GradeResponse gradeSubmission(UUID submissionId, CreateGradeRequest request) {
    Submission submission = findByIdOrThrow(submissionId);
    if (submission.getStatus() != SubmissionStatus.GRADED) {
      stateMachine.transitionTo(submission, SubmissionStatus.GRADED);
      repository.save(submission);
    }
    Grade grade =
        gradeRepository
            .findBySubmissionId(submissionId)
            .map(
                existing -> {
                  existing.setScore(request.getScore());
                  existing.setGradedBy(currentUser.id());
                  return existing;
                })
            .orElseGet(() -> gradeMapper.toEntity(request, submissionId, currentUser.id()));
    grade = gradeRepository.save(grade);
    events.publishEvent(
        new SubmissionGraded(
            submissionId, submission.getAssignmentId(), submission.getStudentId(), grade.getScore()));
    return gradeMapper.toResponse(grade);
  }
  ```
  *(Spec gap: the spec named only the unique-constraint 500; re-grade also needed the `GRADED→GRADED` state-machine guard above. Re-grading from `GRADED` is allowed; the machine stays pure.)*

- [ ] **Step 4: verify & commit.** `mvn -pl homework -am test`, `mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "fix(homework): upsert grade on re-grade (no unique-constraint 500)"
  ```

---

### Task 4: File upload size + content-type allow-list (files)

Default multipart limit (~1 MB) silently breaks uploads; no MIME gate. Set 25 MB, add an allow-list checked in `upload` → `BadRequestException`. (The `MaxUploadSizeExceededException` → 413 handler shipped in Task 1.)

**Files:** `main/.../application.yml`, `files/.../config/FilesProperties.java`, `files/.../service/FileServiceImpl.java`, unit test.

- [ ] **Step 1: config** (`application.yml`) — under `spring:` add the multipart block; under `codillas.files:` add the allow-list:
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 25MB
        max-request-size: 25MB

  codillas:
    files:
      # ... existing bucket/endpoint/region/access-key/secret-key ...
      allowed-content-types:
        - application/pdf
        - image/png
        - image/jpeg
        - image/gif
        - text/plain
        - application/zip
        - application/msword
        - application/vnd.openxmlformats-officedocument.wordprocessingml.document
        - application/vnd.openxmlformats-officedocument.presentationml.presentation
        - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  ```

- [ ] **Step 2: bind it** (`FilesProperties.java`) — add the field to the record:
  ```java
  import java.util.List;

  @ConfigurationProperties("codillas.files")
  public record FilesProperties(
      String bucket,
      String endpoint,
      String region,
      String accessKey,
      String secretKey,
      List<String> allowedContentTypes) {}
  ```
  (`S3Config` reads only the first five components — unaffected.)

- [ ] **Step 3 (RED): unit test** — `files/.../service/FileServiceUploadTypeTest.java`:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("FileServiceImpl — content-type allow-list")
  class FileServiceUploadTypeTest {
    @Mock StoredFileRepository repository;
    @Mock ObjectStorage storage;
    @Mock StoredFileMapper mapper;
    FileServiceImpl service;

    @BeforeEach
    void setUp() {
      service =
          new FileServiceImpl(
              repository,
              storage,
              mapper,
              new FilesProperties("b", "e", "r", "a", "s", List.of("application/pdf")));
    }

    @Test
    @DisplayName("rejects a disallowed content type with 400")
    void rejectsDisallowed() {
      MockMultipartFile bad =
          new MockMultipartFile("file", "x.exe", "application/x-msdownload", new byte[] {1});
      assertThatThrownBy(
              () -> service.upload(bad, FileReferenceType.MATERIAL, UUID.randomUUID(), UUID.randomUUID()))
          .isInstanceOf(BadRequestException.class);
      verifyNoInteractions(storage);
    }
  }
  ```
  (Adjust the `FileServiceImpl` constructor arg order to match Lombok's `@RequiredArgsConstructor` field order after Step 4.)

- [ ] **Step 4 (GREEN): validate in `upload`** — inject `FilesProperties`, check after resolving `contentType`, before `storage.put`:
  ```java
  private final FilesProperties properties; // add to the field list

  // inside upload(...), right after `contentType` is resolved:
  List<String> allowed = properties.allowedContentTypes();
  if (allowed != null && !allowed.isEmpty() && !allowed.contains(contentType)) {
    throw new BadRequestException("Unsupported file type: " + contentType);
  }
  ```

- [ ] **Step 5: verify & commit.** `mvn -pl files -am test`, `mvn spotless:apply`. (The existing MinIO end-to-end integration test still passes — happy-path types are in the allow-list.)
  ```bash
  git add -A && git commit -m "feat(files): 25MB multipart limit + content-type allow-list"
  ```

---

### Task 5: `CourseDeleted` event + delete course endpoint + course FK cascade

Add `DELETE /api/courses/{courseId}`. Intra-module integrity moves into the schema (FK `ON DELETE CASCADE` sections→courses, lessons→sections, materials→lessons); the service just deletes the root and publishes `CourseDeleted`.

**Files:** `shared/event/CourseDeleted.java` (NEW), `openapi/course-paths.yaml`, `course/.../service/CourseService.java` + `CourseServiceImpl.java`, `course/.../web/CourseController.java`, `course/.../db/changelog/changes/0.3.0/0.3.0-add-course-fk-cascade.yaml` (NEW) + `course-changelog.yaml`, course unit test + `main` integration test.

- [ ] **Step 1: event record** — `shared/src/main/java/de/codillas/shared/event/CourseDeleted.java`:
  ```java
  package de.codillas.shared.event;

  import java.util.UUID;

  /**
   * Published when a course is hard-deleted. Lives in {@code shared}: enrollment consumes it to
   * cascade-delete the course's groups.
   */
  public record CourseDeleted(UUID courseId) {}
  ```

- [ ] **Step 2: OpenAPI** — add `delete` to `/api/courses/{courseId}` in `course-paths.yaml` (bump `info.version` → `0.3.0`), mirroring the section delete:
  ```yaml
      delete:
        tags: [course]
        operationId: deleteCourse
        summary: Delete a course and its entire Section → Lesson → Material structure
        parameters:
          - $ref: "#/components/parameters/CourseId"
        responses:
          "204":
            description: Deleted
          "403":
            $ref: "common.yaml#/components/responses/Forbidden"
          "404":
            $ref: "common.yaml#/components/responses/NotFound"
  ```
  Regenerate (`CourseApi` gains `deleteCourse`).

- [ ] **Step 3: Liquibase FK cascade** — `course/.../db/changelog/changes/0.3.0/0.3.0-add-course-fk-cascade.yaml` (one file, three idempotent changeSets):
  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-fk-sections-course
        author: codillas
        comment: FK sections.course_id -> courses.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_sections_course }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_sections_course
              baseTableName: sections
              baseColumnNames: course_id
              referencedTableName: courses
              referencedColumnNames: id
              onDelete: CASCADE
    - changeSet:
        id: 0.3.0-fk-lessons-section
        author: codillas
        comment: FK lessons.section_id -> sections.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_lessons_section }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_lessons_section
              baseTableName: lessons
              baseColumnNames: section_id
              referencedTableName: sections
              referencedColumnNames: id
              onDelete: CASCADE
    - changeSet:
        id: 0.3.0-fk-materials-lesson
        author: codillas
        comment: FK materials.lesson_id -> lessons.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_materials_lesson }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_materials_lesson
              baseTableName: materials
              baseColumnNames: lesson_id
              referencedTableName: lessons
              referencedColumnNames: id
              onDelete: CASCADE
  ```
  Add a `0.3.0` section to `course-changelog.yaml`:
  ```yaml
    # 0.3.0 — intra-module FK cascade + course delete
    - include:
        file: changes/0.3.0/0.3.0-add-course-fk-cascade.yaml
        relativeToChangelogFile: true
  ```
  (The FK makes the manual cascade in `deleteSection`/`deleteLesson` redundant but harmless — leave those.)

- [ ] **Step 4 (RED): course unit test** — `course/.../service/CourseServiceDeleteTest.java`:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("CourseServiceImpl — delete course")
  class CourseServiceDeleteTest {
    @Mock CourseRepository courseRepository;
    @Mock SectionRepository sectionRepository;
    @Mock LessonRepository lessonRepository;
    @Mock MaterialRepository materialRepository;
    @Mock CourseMapper courseMapper;
    @Mock LessonMapper lessonMapper;
    @Mock ApplicationEventPublisher events;
    @InjectMocks CourseServiceImpl service;

    @Test
    @DisplayName("deletes the course and publishes CourseDeleted")
    void deletesAndPublishes() {
      UUID id = UUID.randomUUID();
      Course course = Course.builder().build();
      when(courseRepository.findById(id)).thenReturn(Optional.of(course));

      service.deleteCourse(id);

      verify(courseRepository).delete(course);
      verify(events).publishEvent(new CourseDeleted(id));
    }

    @Test
    @DisplayName("404 when the course does not exist")
    void missing404() {
      UUID id = UUID.randomUUID();
      when(courseRepository.findById(id)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.deleteCourse(id)).isInstanceOf(NotFoundException.class);
    }
  }
  ```

- [ ] **Step 5 (GREEN): service + controller.** Inject `ApplicationEventPublisher events` into `CourseServiceImpl`; add to the `CourseService` interface + impl:
  ```java
  @Override
  @Transactional
  public void deleteCourse(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    courseRepository.delete(course); // sections/lessons/materials removed by FK ON DELETE CASCADE
    events.publishEvent(new CourseDeleted(courseId));
  }
  ```
  `CourseController` (match `createCourse` → `@RequiresAdmin`):
  ```java
  @Override
  @RequiresAdmin
  public ResponseEntity<Void> deleteCourse(UUID courseId) {
    service.deleteCourse(courseId);
    return ResponseEntity.noContent().build();
  }
  ```

- [ ] **Step 6 (integration, `main`): FK cascade end-to-end** — `main/.../integration/course/CourseDeleteIntegrationTest.java`: seed a course → section → lesson → material via repositories, `DELETE /api/courses/{id}` as ADMIN → 204, assert the section/lesson/material rows are gone (proves the FK changeset under the production schema). Auth via the `jwt().jwt(j -> j.subject(...)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))` post-processor used by `GradebookIntegrationTest`.

- [ ] **Step 7: verify & commit.** `mvn -pl shared,course,main -am test`, `mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "feat(course): delete course endpoint + FK cascade + CourseDeleted event"
  ```

---

### Task 6: `GroupDeleted` event + delete group endpoint + enrollment FK cascade + `CourseDeleted` consumer

`DELETE /api/groups/{groupId}` deletes the group (FK `ON DELETE CASCADE` clears memberships/scheduled_lessons/attendance) and publishes `GroupDeleted`. Enrollment also consumes `CourseDeleted`, deleting each of the course's groups through the same path (so each republishes `GroupDeleted` for downstream cleanup in Task 7).

**Files:** `shared/event/GroupDeleted.java` (NEW), `openapi/enrollment-paths.yaml`, `enrollment/pom.xml`, `enrollment/.../service/GroupService.java` + `GroupServiceImpl.java`, `enrollment/.../service/EnrollmentEventListener.java` (NEW), `enrollment/.../web/EnrollmentController.java`, `enrollment/.../db/changelog/changes/0.3.0/0.3.0-add-enrollment-fk-cascade.yaml` (NEW) + `enrollment-changelog.yaml`, enrollment unit tests.

- [ ] **Step 1: event record** — `shared/src/main/java/de/codillas/shared/event/GroupDeleted.java`:
  ```java
  package de.codillas.shared.event;

  import java.util.UUID;

  /**
   * Published when a group (cohort) is hard-deleted. Lives in {@code shared}: gradebook, homework,
   * notification and chat consume it to purge their group-scoped rows.
   */
  public record GroupDeleted(UUID groupId) {}
  ```

- [ ] **Step 2: pom** — add to `enrollment/pom.xml` (new `@ApplicationModuleListener` consumer):
  ```xml
  <!-- @ApplicationModuleListener for consuming CourseDeleted. -->
  <dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-api</artifactId>
  </dependency>
  ```

- [ ] **Step 3: OpenAPI** — add `delete` to `/api/groups/{groupId}` in `enrollment-paths.yaml` (bump `info.version` → `0.3.0`):
  ```yaml
    /api/groups/{groupId}:
      delete:
        tags: [enrollment]
        operationId: deleteGroup
        summary: Delete a group and its memberships, schedule and attendance
        parameters:
          - name: groupId
            in: path
            required: true
            schema: { type: string, format: uuid }
        responses:
          "204":
            description: Deleted
          "403":
            $ref: "common.yaml#/components/responses/Forbidden"
          "404":
            $ref: "common.yaml#/components/responses/NotFound"
  ```
  Regenerate (`EnrollmentApi` gains `deleteGroup`).

- [ ] **Step 4: Liquibase FK cascade** — `enrollment/.../db/changelog/changes/0.3.0/0.3.0-add-enrollment-fk-cascade.yaml` (three changeSets, same shape as Task 5):
  - `fk_group_members_group`: `group_members.group_id → study_groups.id` `ON DELETE CASCADE`
  - `fk_scheduled_lessons_group`: `scheduled_lessons.group_id → study_groups.id` `ON DELETE CASCADE`
  - `fk_attendance_scheduled_lesson`: `attendance.scheduled_lesson_id → scheduled_lessons.id` `ON DELETE CASCADE`

  ```yaml
  databaseChangeLog:
    - changeSet:
        id: 0.3.0-fk-group-members-group
        author: codillas
        comment: FK group_members.group_id -> study_groups.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_group_members_group }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_group_members_group
              baseTableName: group_members
              baseColumnNames: group_id
              referencedTableName: study_groups
              referencedColumnNames: id
              onDelete: CASCADE
    - changeSet:
        id: 0.3.0-fk-scheduled-lessons-group
        author: codillas
        comment: FK scheduled_lessons.group_id -> study_groups.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_scheduled_lessons_group }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_scheduled_lessons_group
              baseTableName: scheduled_lessons
              baseColumnNames: group_id
              referencedTableName: study_groups
              referencedColumnNames: id
              onDelete: CASCADE
    - changeSet:
        id: 0.3.0-fk-attendance-scheduled-lesson
        author: codillas
        comment: FK attendance.scheduled_lesson_id -> scheduled_lessons.id ON DELETE CASCADE (0.3.0)
        preConditions:
          - onFail: MARK_RAN
          - not:
              - foreignKeyConstraintExists: { foreignKeyName: fk_attendance_scheduled_lesson }
        changes:
          - addForeignKeyConstraint:
              constraintName: fk_attendance_scheduled_lesson
              baseTableName: attendance
              baseColumnNames: scheduled_lesson_id
              referencedTableName: scheduled_lessons
              referencedColumnNames: id
              onDelete: CASCADE
  ```
  Add a `0.3.0` section to `enrollment-changelog.yaml` including this file.

- [ ] **Step 5 (RED): unit tests** — `enrollment/.../service/GroupServiceDeleteTest.java`:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("GroupServiceImpl — delete + CourseDeleted cascade")
  class GroupServiceDeleteTest {
    @Mock GroupRepository repository;
    @Mock GroupMapper mapper;
    @Mock ApplicationEventPublisher events;
    @InjectMocks GroupServiceImpl service;

    @Test
    @DisplayName("deletes the group and publishes GroupDeleted")
    void deletesAndPublishes() {
      UUID id = UUID.randomUUID();
      Group group = Group.builder().build();
      when(repository.findById(id)).thenReturn(Optional.of(group));

      service.deleteGroup(id);

      verify(repository).delete(group);
      verify(events).publishEvent(new GroupDeleted(id));
    }

    @Test
    @DisplayName("CourseDeleted cascades to every group of that course")
    void courseDeletedCascades() {
      UUID courseId = UUID.randomUUID();
      Group g1 = Group.builder().build();
      Group g2 = Group.builder().build();
      g1.setId(UUID.randomUUID());
      g2.setId(UUID.randomUUID());
      when(repository.findByCourseId(courseId)).thenReturn(List.of(g1, g2));
      when(repository.findById(g1.getId())).thenReturn(Optional.of(g1));
      when(repository.findById(g2.getId())).thenReturn(Optional.of(g2));

      service.onCourseDeleted(new CourseDeleted(courseId));

      verify(events).publishEvent(new GroupDeleted(g1.getId()));
      verify(events).publishEvent(new GroupDeleted(g2.getId()));
    }
  }
  ```

- [ ] **Step 6 (GREEN): service + listener + controller.** Inject `ApplicationEventPublisher events` into `GroupServiceImpl`; add to `GroupService` + impl:
  ```java
  @Override
  @Transactional
  public void deleteGroup(UUID groupId) {
    Group group = repository.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
    repository.delete(group); // memberships/scheduled_lessons/attendance removed by FK ON DELETE CASCADE
    events.publishEvent(new GroupDeleted(groupId));
  }

  @Override
  @Transactional
  public void onCourseDeleted(CourseDeleted event) {
    repository.findByCourseId(event.courseId()).forEach(group -> deleteGroup(group.getId()));
  }
  ```
  New `EnrollmentEventListener` (package-private `@Component`):
  ```java
  package de.codillas.enrollment.service;

  import org.springframework.modulith.events.ApplicationModuleListener;
  import org.springframework.stereotype.Component;

  import de.codillas.shared.event.CourseDeleted;

  import lombok.RequiredArgsConstructor;

  /** Cascades a deleted course to its cohorts (each republishes GroupDeleted). */
  @Component
  @RequiredArgsConstructor
  class EnrollmentEventListener {
    private final GroupService service;

    @ApplicationModuleListener
    void on(CourseDeleted event) {
      service.onCourseDeleted(event);
    }
  }
  ```
  `EnrollmentController` (match `createGroup` → `@RequiresAdmin`):
  ```java
  @Override
  @RequiresAdmin
  public ResponseEntity<Void> deleteGroup(UUID groupId) {
    groupService.deleteGroup(groupId);
    return ResponseEntity.noContent().build();
  }
  ```

- [ ] **Step 7: verify & commit.** `mvn -pl shared,enrollment,main -am test`, `mvn spotless:apply`. (Integration coverage of the FK cascade + the full `GroupDeleted` fan-out lands in Task 7.)
  ```bash
  git add -A && git commit -m "feat(enrollment): delete group endpoint + FK cascade + CourseDeleted->GroupDeleted"
  ```

---

### Task 7: Cross-module `GroupDeleted` consumers (gradebook, notification, chat, homework)

Each owning module purges its group-scoped rows on `GroupDeleted`. gradebook/notification drop their roster read-model rows; chat drops the GROUP room + members + messages; homework deletes the group's assignments and their submissions/reviews/grades.

**Files:** gradebook (`GradebookMembershipRepository`, `GradebookEventListener`, `GradebookService(Impl)`), notification (`NotificationMembershipRepository`, `NotificationEventListener`, `NotificationService(Impl)`), chat (`ChatRoomMemberRepository`, `ChatMessageRepository`, `ChatEventListener`, `ChatService(Impl)`), homework (`HomeworkEventListener` NEW, `AssignmentRepository`/`SubmissionRepository`/`ReviewRepository`/`GradeRepository`, `homework/pom.xml`), and `main/.../integration/enrollment/GroupDeleteCascadeIntegrationTest.java` (NEW).

- [ ] **Step 1: gradebook.** Repo: `void deleteByGroupId(UUID groupId);` on `GradebookMembershipRepository`. Service method `purgeGroup(GroupDeleted event)` → `repository.deleteByGroupId(event.groupId())` (`@Transactional`). Listener: add to `GradebookEventListener`:
  ```java
  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.purgeGroup(event);
  }
  ```

- [ ] **Step 2: notification.** Repo: `void deleteByGroupId(UUID groupId);` on `NotificationMembershipRepository`. Service `onGroupDeleted(GroupDeleted event)` → `membershipRepository.deleteByGroupId(event.groupId())`. Listener: add `@ApplicationModuleListener void on(GroupDeleted event) { service.onGroupDeleted(event); }` to `NotificationEventListener`.

- [ ] **Step 3: chat.** Repos: `void deleteByRoomId(UUID roomId);` on **both** `ChatRoomMemberRepository` and `ChatMessageRepository`. `ChatServiceImpl.onGroupDeleted`:
  ```java
  @Override
  @Transactional
  public void onGroupDeleted(UUID groupId) {
    roomRepository
        .findByTypeAndReferenceId(ChatRoomType.GROUP, groupId)
        .ifPresent(
            room -> {
              messageRepository.deleteByRoomId(room.getId());
              memberRepository.deleteByRoomId(room.getId());
              roomRepository.delete(room);
            });
  }
  ```
  Listener: add to `ChatEventListener`:
  ```java
  @ApplicationModuleListener
  void on(GroupDeleted event) {
    service.onGroupDeleted(event.groupId());
  }
  ```

- [ ] **Step 4: homework.** Add the modulith dep to `homework/pom.xml` (same block as Task 6 Step 2 — first consumer in this module). Repo finders/deletes:
  - `AssignmentRepository`: `List<Assignment> findByGroupId(UUID groupId);` (overload alongside the paged one); `void deleteByGroupId(UUID groupId);`
  - `SubmissionRepository`: `List<Submission> findByAssignmentIdIn(java.util.Collection<UUID> assignmentIds);` `void deleteByAssignmentIdIn(java.util.Collection<UUID> assignmentIds);`
  - `ReviewRepository`: `void deleteBySubmissionIdIn(java.util.Collection<UUID> submissionIds);`
  - `GradeRepository`: `void deleteBySubmissionIdIn(java.util.Collection<UUID> submissionIds);`

  New `HomeworkEventListener` + a service writer (`SubmissionService.onGroupDeleted` or a small `HomeworkCleanupService`). Writer body (cascade children first — homework has no intra-module FK cascade, this is intentional per the spec's FK scope):
  ```java
  @Override
  @Transactional
  public void onGroupDeleted(UUID groupId) {
    List<UUID> assignmentIds =
        assignmentRepository.findByGroupId(groupId).stream().map(Assignment::getId).toList();
    if (assignmentIds.isEmpty()) return;
    List<UUID> submissionIds =
        submissionRepository.findByAssignmentIdIn(assignmentIds).stream()
            .map(Submission::getId)
            .toList();
    if (!submissionIds.isEmpty()) {
      gradeRepository.deleteBySubmissionIdIn(submissionIds);
      reviewRepository.deleteBySubmissionIdIn(submissionIds);
      submissionRepository.deleteByAssignmentIdIn(assignmentIds);
    }
    assignmentRepository.deleteByGroupId(groupId);
  }
  ```
  Listener:
  ```java
  @Component
  @RequiredArgsConstructor
  class HomeworkEventListener {
    private final SubmissionService service;

    @ApplicationModuleListener
    void on(GroupDeleted event) {
      service.onGroupDeleted(event.groupId());
    }
  }
  ```

- [ ] **Step 5 (RED): one purge unit test per consumer.** Example (gradebook) — the others mirror it:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("GradebookService — purge on GroupDeleted")
  class GradebookServicePurgeTest {
    @Mock GradebookMembershipRepository membershipRepository;
    // ... other gradebook deps mocked ...
    @InjectMocks GradebookServiceImpl service;

    @Test
    @DisplayName("deletes the group's membership rows")
    void purges() {
      UUID groupId = UUID.randomUUID();
      service.purgeGroup(new GroupDeleted(groupId));
      verify(membershipRepository).deleteByGroupId(groupId);
    }
  }
  ```
  For chat, assert `findByTypeAndReferenceId(GROUP, groupId)` present → `messageRepository`/`memberRepository`.`deleteByRoomId(roomId)` + `roomRepository.delete(room)`; absent → no deletes. For homework, mock the four repos and verify the delete order (grades/reviews → submissions → assignments).

- [ ] **Step 6 (integration, `main`, Awaitility): the full fan-out** — `GroupDeleteCascadeIntegrationTest`. Seed a group; enrol a student (`POST /api/groups/{id}/members` as ADMIN) so `StudentEnrolled` populates gradebook/notification/chat rosters + the chat GROUP room; create+grade a homework assignment for the group. Then `DELETE /api/groups/{id}` and await all rows gone:
  ```java
  @DisplayName("Group delete cascade (integration)")
  class GroupDeleteCascadeIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired GradebookMembershipRepository gradebookMemberships;
    @Autowired NotificationMembershipRepository notificationMemberships;
    @Autowired ChatRoomRepository chatRooms;
    @Autowired GroupRepository groups;

    @Test
    @DisplayName("deleting a group purges enrollment rows and every read model")
    void cascades() throws Exception {
      UUID groupId = /* create a group + enrol a student via the API, await rosters populated */ null;

      mockMvc
          .perform(delete("/api/groups/{id}", groupId).with(as(admin, "ADMIN")))
          .andExpect(status().isNoContent());

      await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(groups.findById(groupId)).isEmpty();
                assertThat(gradebookMemberships.findByGroupId(groupId)).isEmpty();
                assertThat(notificationMemberships.findByGroupId(groupId)).isEmpty();
                assertThat(chatRooms.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId)).isEmpty();
              });
    }
  }
  ```
  (Use the same `as(userId, role)` JWT post-processor helper as `GradebookIntegrationTest`. The first `await` before the delete ensures `StudentEnrolled` has been consumed so there is something to purge.)

- [ ] **Step 7: verify & commit.** `mvn -pl gradebook,notification,chat,homework,main -am test`, `mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "feat(events): purge gradebook/notification/chat/homework rows on GroupDeleted"
  ```

---

### Task 8: Wire the email channel (`UserEmailChanged` → `recipient_email`)

Capture the JWT `email` claim onto `Profile` at provisioning; `user` publishes `UserEmailChanged`; `notification` consumes it into a `recipient_email` read model; replace the no-op resolver with one that reads it. Integration-test (Awaitility) that the recipient is now resolvable.

**Files:** `shared/security/CurrentUser.java`, `main/.../config/JwtCurrentUser.java`, `shared/event/UserEmailChanged.java` (NEW), `user/.../domain/model/Profile.java`, `user/.../service/UserServiceImpl.java`, `user/.../db/changelog/changes/0.2.0/0.2.0-add-profile-email.yaml` (NEW) + `user-changelog.yaml`, notification (`RecipientEmail` NEW, `RecipientEmailRepository` NEW, `DbRecipientEmailResolver` NEW, `UnresolvedRecipientEmailResolver` DEL, `NotificationEventListener` MOD, `0.2.0-create-recipient-email.yaml` NEW + `notification-changelog.yaml`), `main/.../integration/notification/EmailChannelIntegrationTest.java` (NEW).

- [ ] **Step 1: `CurrentUser.email()`** — add to the `shared` interface (nullable; the token may carry no `email`). *(Spec gap: the user module can't read the JWT directly — only `main` assembles the resource server — so the claim is exposed via this `CurrentUser` seam, alongside Batch 1's `isStaff()`.)*
  ```java
  /** The verified email from the token's {@code email} claim, or {@code null} if absent. */
  String email();
  ```
  Implement in `JwtCurrentUser`:
  ```java
  @Override
  public String email() {
    Authentication auth = authentication();
    return auth instanceof JwtAuthenticationToken token
        ? token.getToken().getClaimAsString("email")
        : null;
  }
  ```

- [ ] **Step 2: event record** — `shared/src/main/java/de/codillas/shared/event/UserEmailChanged.java`:
  ```java
  package de.codillas.shared.event;

  import java.util.UUID;

  /**
   * Published when a user's email is captured/changed at provisioning. Lives in {@code shared}:
   * notification consumes it into its recipient_email read model.
   */
  public record UserEmailChanged(UUID userId, String email) {}
  ```

- [ ] **Step 3: `Profile.email`** (`user/.../domain/model/Profile.java`):
  ```java
  @Column(length = 320)
  private String email;
  ```
  Liquibase `user/.../db/changelog/changes/0.2.0/0.2.0-add-profile-email.yaml` (addColumn `email varchar(320)`, nullable, `not columnExists`), included under a new `0.2.0` section of `user-changelog.yaml`. `ProfileMapper.toResponse` stays exhaustive (DTO has no `email`; extra entity field is fine); `updateEntity` is `ignoreByDefault=true` so it never touches `email`.

- [ ] **Step 4 (RED): user unit test** — provisioning publishes the event:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("UserServiceImpl — capture email at provisioning")
  class UserServiceEmailTest {
    @Mock ProfileRepository repository;
    @Mock ProfileMapper mapper;
    @Mock CurrentUser currentUser;
    @Mock ApplicationEventPublisher events;
    @InjectMocks UserServiceImpl service;

    @Test
    @DisplayName("provisioning a new profile publishes UserEmailChanged")
    void publishesOnProvision() {
      UUID userId = UUID.randomUUID();
      when(currentUser.id()).thenReturn(userId);
      when(currentUser.displayName()).thenReturn("Stu");
      when(currentUser.email()).thenReturn("stu@codillas.de");
      when(repository.findByUserId(userId)).thenReturn(Optional.empty());
      when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(currentUser.roles()).thenReturn(Set.of("STUDENT"));
      when(mapper.toResponse(any(), any())).thenReturn(null);

      service.getMyProfile();

      verify(events).publishEvent(new UserEmailChanged(userId, "stu@codillas.de"));
    }
  }
  ```

- [ ] **Step 5 (GREEN): `UserServiceImpl`** — inject `ApplicationEventPublisher events`; capture + publish in `provisionFromToken` (only when the claim is present):
  ```java
  private Profile provisionFromToken(UUID userId) {
    String email = currentUser.email();
    Profile profile =
        repository.save(
            Profile.builder().userId(userId).displayName(currentUser.displayName()).email(email).build());
    if (email != null) {
      events.publishEvent(new UserEmailChanged(userId, email));
    }
    log.info("Auto-created profile for user {} on first access", userId);
    return profile;
  }
  ```

- [ ] **Step 6: notification read model.** `RecipientEmail` entity (`@Table(name = "recipient_email")`, unique `user_id`, fields `UUID userId`, `String email`), `RecipientEmailRepository` (`Optional<RecipientEmail> findByUserId(UUID userId);`), Liquibase `0.2.0-create-recipient-email.yaml` (`not tableExists`, columns id/user_id/email/created_at/updated_at + `uq_recipient_email_user` on `user_id`), included under a `0.2.0` section of `notification-changelog.yaml`.

  Replace `UnresolvedRecipientEmailResolver` (delete it) with `DbRecipientEmailResolver` — owns the read model both ways:
  ```java
  @Component
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  class DbRecipientEmailResolver implements RecipientEmailResolver {
    private final RecipientEmailRepository repository;

    @Override
    public Optional<String> resolve(UUID userId) {
      return repository.findByUserId(userId).map(RecipientEmail::getEmail);
    }

    @Transactional
    void record(UserEmailChanged event) {
      RecipientEmail row =
          repository
              .findByUserId(event.userId())
              .map(
                  existing -> {
                    existing.setEmail(event.email());
                    return existing;
                  })
              .orElseGet(
                  () -> RecipientEmail.builder().userId(event.userId()).email(event.email()).build());
      repository.save(row);
    }
  }
  ```
  Add the consumer to `NotificationEventListener` (inject the concrete `DbRecipientEmailResolver` — same package):
  ```java
  private final DbRecipientEmailResolver recipientEmail;

  @ApplicationModuleListener
  void on(UserEmailChanged event) {
    recipientEmail.record(event);
  }
  ```

- [ ] **Step 7 (RED): notification unit test** — `DbRecipientEmailResolver` resolves a recorded address:
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("DbRecipientEmailResolver — read model")
  class DbRecipientEmailResolverTest {
    @Mock RecipientEmailRepository repository;
    @InjectMocks DbRecipientEmailResolver resolver;

    @Test
    @DisplayName("resolves a previously recorded email")
    void resolvesRecorded() {
      UUID userId = UUID.randomUUID();
      when(repository.findByUserId(userId))
          .thenReturn(Optional.of(RecipientEmail.builder().userId(userId).email("a@b.de").build()));
      assertThat(resolver.resolve(userId)).contains("a@b.de");
    }
  }
  ```

- [ ] **Step 8 (integration, `main`, Awaitility): the channel resolves** — `EmailChannelIntegrationTest`. Provisioning a student profile with an `email` claim publishes `UserEmailChanged`; notification consumes it; the resolver (previously a no-op, so a graded submission resolved nothing) now returns the address:
  ```java
  @DisplayName("Email channel (UserEmailChanged -> recipient_email, integration)")
  class EmailChannelIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired RecipientEmailResolver recipientEmailResolver;

    @Test
    @DisplayName("a provisioned profile makes the recipient email resolvable")
    void resolvesAfterProvisioning() throws Exception {
      UUID student = UUID.randomUUID();
      mockMvc
          .perform(
              get("/api/users/me")
                  .with(
                      jwt()
                          .jwt(
                              j ->
                                  j.subject(student.toString())
                                      .claim("name", "Stu")
                                      .claim("email", "stu@codillas.de"))
                          .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
          .andExpect(status().isOk());

      await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> assertThat(recipientEmailResolver.resolve(student)).contains("stu@codillas.de"));
    }
  }
  ```

- [ ] **Step 9: verify & commit.** `mvn -pl shared,user,notification,main -am test`, then `cd backend && mvn -q compile` and `mvn spotless:apply`.
  ```bash
  git add -A && git commit -m "feat(notification): wire email channel — UserEmailChanged -> recipient_email resolver"
  ```

---

## Self-Review

**Spec coverage (Batch 2 scope items):**
1. `@Version` on `Attempt` + `Submission` → 409 on conflict — Task 1 (`lockVersion`/`lock_version`, global handler, persistence test). ✓
2. `Submission.late` + `submittedAt` set at submit (`late = submittedAt.isAfter(dueAt)`); flag-only; `late` surfaced in DTO — Task 2. ✓
3. `gradeSubmission` upserts `Grade` by `submissionId` (no unique-constraint 500) — Task 3 (+ `GRADED→GRADED` guard). ✓
4. `multipart.max-file-size/request-size` (25 MB) + `FilesProperties.allowedContentTypes` allow-list → `BadRequestException` — Task 4 (+ 413 on oversize). ✓
5. `DELETE /api/courses/{id}` + `DELETE /api/groups/{id}` with intra-module FK `ON DELETE CASCADE`; `CourseDeleted`/`GroupDeleted` in `shared/event/`; consumers purge (gradebook/homework/notification/chat on `GroupDeleted`; enrollment on `CourseDeleted`) — Tasks 5–7. ✓
6. `Profile.email` from JWT claim at provisioning; `user` publishes `UserEmailChanged`; `notification` consumes into `recipient_email`; resolver reads it; Awaitility test — Task 8. ✓

**Conventions:** API-first (specs changed + regenerated before code) for the three new endpoints + `late`; Liquibase changesets carry `not …Exists`/`foreignKeyConstraintExists` preconditions + semver folders, included in version order; events published inside the writer tx, consumed with `@ApplicationModuleListener`; new cross-module records in `shared/event/`; module units (Mockito) + `main` integration (Testcontainers/Awaitility); negative/`NotFound` cases covered; `events-api` added where a module gains a consumer. `ApplicationModules.of(...).verify()` must stay green (run it after Tasks 6–8). ✓

**Placeholder scan:** every production change shows full code; each behavior has a real test body (parallel purge tests explicitly "mirror" a fully-written example). The Task 6/7 integration tests sketch the seed prelude in comments but assert concretely. ✓

**Decisions the spec didn't fully cover (also in the per-task notes):**
- Optimistic-lock column is `lock_version` (not `version`) to avoid colliding with `Submission`'s business `version`; same name on `Attempt` for consistency.
- 409 mapped via a global `@ExceptionHandler(OptimisticLockingFailureException.class)` — the failure surfaces at commit, outside the service tx.
- Re-grade also needed a `GRADED→GRADED` state-machine guard (skip the transition when already graded), not just the Grade upsert.
- Homework `GroupDeleted` cleanup cascades in-service (grades→reviews→submissions→assignments); the spec's FK-cascade list covers only course + enrollment schemas, so homework keeps no intra-module FK.
- Added `CurrentUser.email()` (+ `JwtCurrentUser`) because the user module can't read the JWT directly; `UserEmailChanged` published only when the claim is present.
- enrollment's `CourseDeleted` consumer reuses `deleteGroup` so each group republishes `GroupDeleted`, chaining downstream cleanup.
- gradebook's `GroupDeleted` purge clears the `gradebook_membership` roster only (current `ProgressEntry` carries no `groupId`).
- Added `spring-modulith-events-api` to `enrollment` and `homework` poms (new consumers); MaxUploadSize→413 handler added alongside the allow-list.
