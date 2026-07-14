# LMS hardening & domain completion (design)

Date: 2026-06-28
Scope: `backend/` (+ matching OpenAPI specs in `backend/openapi/`; frontend follows per plan).
Owner: @fedotiuk-dm

> Master design spec for the work that takes the platform from "architecturally complete /
> feature-broad" to "production-grade LMS". Derived from the 2026-06-28 four-thread backend audit.
> The architecture is sound — **nothing here is a rewrite.** Each batch below is its own executable
> plan under `docs/superpowers/plans/2026-06-28-*.md`.

## Problem (what the audit found)

The Modulith skeleton is strong (enforced boundaries, API-first, durable events, RoleHierarchy,
ProblemDetail). But under it:

1. **Object-level authorization is systematically missing.** Role gates exist everywhere; *ownership*
   checks almost nowhere. A student can read/modify another student's submission, attempt, grades,
   download any file by id, and subscribe to any chat room. Five IDOR/auth-bypass holes. The `chat`
   module already has the correct pattern (`requireMember`) — it just isn't applied elsewhere.
2. **Correctness gaps:** no optimistic locking (attempt-submit race double-publishes events), no
   deadline/late handling, grade re-submit hits a unique constraint instead of upserting, no file
   size/type limits, no delete endpoints/FKs (orphan rows on manual delete), and the **email channel
   is built but inert** (`RecipientEmailResolver` is a no-op stub — zero emails actually send).
3. **Domain depth missing for a real LMS:** assessment has no attempt limits, no timer, all-or-nothing
   scoring, no shuffle; homework has no late penalty or rubric; gradebook has no final course grade;
   course/group have no draft/publish/archive lifecycle.

## Global constraints (apply to every batch)

The backend `AGENTS.md` chain is binding and **already encodes the HOW** — plans below reference it
rather than restating it. In particular:

- **API-first.** Change the OpenAPI spec under `backend/openapi/` first, regenerate, then TDD below
  it. Generated code is read-only. Pagination = `x-spring-paginated` + explicit page params; errors
  `$ref` `common.yaml`; times are `Instant`; nullable is jspecify.
- **Layering:** thin controller `implements <Module>Api` → service (`@Transactional`) → repository →
  anemic `@Entity` extending the shared auditable base. Non-trivial lifecycles live in a **data-driven
  `<Aggregate>StateMachine`** bean, unit-tested in isolation. MapStruct mappers only (no hand builders).
- **Events:** publish with `ApplicationEventPublisher` *inside the writer's transaction*; consume with
  `@ApplicationModuleListener`. Cross-module records live in `shared/event/`. Read models own derived
  tables, never join across modules.
- **Persistence:** Liquibase per module, versioned changesets, `ddl-auto=validate`. Use the Hibernate
  static metamodel (`Entity_`) for `Sort`.
- **One reactor version:** every module inherits the single parent `<version>` — a module never
  declares its own `<version>`, and `main` references siblings via `${project.version}`, never a
  literal. Liquibase changeset semver (the `changes/<semver>/` dirs) is independent of the Maven
  version. If a release bump is wanted, bump the *parent* pom once (it cascades), not per-module.
- **Security:** role gates via `shared.security` annotations only. **New rule this spec adds:**
  every service method that loads a user-owned aggregate by id authorizes the *caller against the
  resource*, not just the role (see Batch 1).
- **Testing (TDD):** unit tests in the module (Mockito, no Spring); integration/controller tests in
  `main` (`BaseIntegrationTest`, Testcontainers Postgres). Every new endpoint gets a **negative
  authz test**. `ApplicationModules.of(...).verify()` stays green. Format with `mvn spotless:apply`.
- No new infra dependencies without need. No commits pushed unless asked. SemVer `0.x.y`.

---

## Batch 1 — Object-level authorization (P0, security)

**Plan:** `2026-06-28-p0-object-authz.md`. **This is the blocker for any production use.**

### Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Ownership-mismatch response | **404 NotFound**, not 403 | Matches `chat.requireMember`; avoids id-enumeration leak (don't confirm the resource exists). |
| Where the check lives | **Service layer**, via a private `findByIdForCaller(id, caller)` per aggregate | Controllers stay thin; the rule is unit-testable with a mocked repo. |
| Staff override | TEACHER/ADMIN bypass owner check (read+grade) via `CurrentUser` role test | Teachers must review students' work; RoleHierarchy already makes ADMIN ⊇ TEACHER. |
| Shared helper | Add `default boolean isStaff()` (+ `hasRole(Role)`) to `shared.security.CurrentUser` | One predicate reused by every service; no new bean. |
| File access | A **`FileAccessAuthorizer` SPI** in `files`, resolved by `ReferenceType`, implemented per module | The one justified abstraction: files must not import other modules, but each owning module *can* answer "may caller U see this file". Genuine N-implementations extension point. |
| MATERIAL files | Authorizer = **any authenticated** (deliberate) | Course materials aren't per-user secret; the real leaks are submissions & DM attachments. Tighten to enrollment later if needed. |
| WS subscription | Inbound `ChannelInterceptor` on `SUBSCRIBE` to `/topic/chat/{roomId}` → `chat.isMember` | Frame-level membership check; chat exposes a boolean query, interceptor wired in `main`. |

### Architecture

- `shared.security.CurrentUser`: add `isStaff()` (TEACHER or ADMIN) + `hasRole(Role)` default methods.
- `homework`: `SubmissionServiceImpl` — replace `findByIdOrThrow` with `findByIdForCaller`; student
  ops require ownership, review/grade require `isStaff()`. Same shape in `assessment.AttemptServiceImpl`
  (saveAnswer/submit/getAttempt → owner; gradeAnswer → staff).
- `gradebook`: `getStudentGradebook(studentId)` allowed if `studentId == caller.id()` or staff;
  `getGroupGradebook(groupId)` allowed if staff or caller is in the local `gradebook_membership`
  read model (already fed by `StudentEnrolled`).
- `files`: define `FileAccessAuthorizer { ReferenceType type(); boolean canAccess(StoredFile, UUID userId); }`;
  `FileServiceImpl.download` resolves the authorizer for the file's `referenceType` and calls it.
  Implementers: `homework` (owner-or-staff), `chat` (room member), `files` itself (MATERIAL→permissive).
- `chat`: expose `boolean isMember(UUID roomId, UUID userId)`; `main` adds `ChatSubscriptionInterceptor`
  registered on the client-inbound channel.

### Out of scope
- Full ABAC/Spring-ACL. The id-equality + role predicate covers every real case.
- Teacher-scoped-to-own-groups precision on `getGroupGradebook` (staff-wide read for now; documented).

---

## Batch 2 — Correctness & integrity hardening (P1)

**Plan:** `2026-06-28-p1-correctness.md`.

### Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Concurrency | `@Version lockVersion` on **`BaseAuditableEntity` (every table)** | The two documented races; optimistic lock → 409 on conflict, retried by client. Grade race solved by upsert instead. |
| Late submission | **Flag, don't block.** Store `late` boolean + `submittedAt` on `Submission`, set `late = submittedAt.isAfter(assignment.dueAt)` at submit | Teachers decide consequences; blocking is hostile and unrecoverable. Penalty math is Batch 4. |
| Grade re-grade | `gradeSubmission` **upserts** the `Grade` by submissionId | Re-grading is normal; a 500 from a unique-constraint is a bug. Mirrors gradebook's upsert. |
| File limits | `spring.servlet.multipart.max-file-size/request-size` (25 MB) + MIME allow-list in `FilesProperties`, checked in `upload` → `BadRequestException` | Default ~1 MB silently breaks uploads / opens DoS. |
| Delete + orphans | Add `DELETE /api/courses/{id}` and `DELETE /api/groups/{id}` with **intra-module cascade**; publish `CourseDeleted` / `GroupDeleted`; cross-module consumers clean their rows | Orphan prevention: intra-module via FK `ON DELETE CASCADE`, cross-module via events. |
| FK constraints | Add intra-module FKs (sections→courses, lessons→sections, materials→lessons; memberships/scheduled_lessons→groups, attendance→scheduled_lessons) `ON DELETE CASCADE` | Move integrity into the schema. Cross-module refs stay by-id (no FK), cleaned by events. |
| Email channel | Capture JWT `email` claim onto `Profile` at provisioning; `user` publishes `UserEmailChanged {userId,email}`; `notification` consumes into a `recipient_email` read model; `RecipientEmailResolver` reads it | Wires the dead email channel without crossing module boundaries (notification keeps a local read model). |

### Architecture
- `@Version lockVersion` on `BaseAuditableEntity` (every table) + Liquibase `lock_version` column (default 0).
- `homework`: `Submission.late`, `Submission.submittedAt`; set in `submitSubmission`. `gradeSubmission`
  finds-or-creates `Grade`.
- `files`: `FilesProperties.allowedContentTypes` + size config; validate in `upload`.
- `course`/`enrollment`: delete endpoints + `CourseStateMachine`-free simple cascade in service;
  `CourseDeleted`/`GroupDeleted` event records in `shared/event/`. Consumers: `enrollment` (on
  CourseDeleted → block/cascade groups), `gradebook`/`homework`/`notification`/`chat` (on GroupDeleted
  → purge their read-model rows / assignments referencing the group).
- `user`: add `email` to `Profile` (from JWT `email` claim in provisioning); publish `UserEmailChanged`.
  `notification`: `RecipientEmailReadModel` + replace `UnresolvedRecipientEmailResolver`.

### Out of scope
- Distributed scheduler lock on `DueReminderJob` — single-instance deploy; revisit at horizontal scale.
- Soft-delete / restore. Hard delete + cascade is enough for this school.

---

## Batch 3 — Assessment depth (P2a)

**Plan:** `2026-06-28-p2a-assessment-depth.md`.

### Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Multiple attempts | Add `Test.maxAttempts` (null = unlimited) + `Attempt.attemptNumber`, unique (test, student, attemptNumber) | Real tests allow N tries; current single-row model can't. |
| Score of record | Gradebook keeps the **best** attempt score | Standard default; "last/average" is a later toggle. |
| Timer | `Test.durationMinutes` (null = untimed) + use `Attempt.startedAt`; server rejects `saveAnswer`/`submit` after `startedAt + duration`, auto-finalizing | Client timers are advisory; the server is the clock. |
| Control window | Optional `Test.availableFrom` / `availableUntil`; `startAttempt` outside the window → 409 | Control tests open for a fixed window. |
| Partial credit | `MULTIPLE_CHOICE`: `points * max(0, (correct − incorrect)/totalCorrect)`. `SINGLE_CHOICE`/`TRUE_FALSE`: unchanged all-or-nothing | Penalize guessing-all; never below 0. SINGLE has one right answer so partial is meaningless. |
| Shuffle | `Test.shuffleQuestions` / `shuffleOptions`; taker DTO order is **deterministic, seeded by `attemptId`** | Stable across reloads, differs per student; no anti-cheat theater beyond ordering. |

### Architecture
- `assessment.Test`: `maxAttempts`, `durationMinutes`, `availableFrom/Until`, `shuffleQuestions/Options`.
- `Attempt`: `attemptNumber`, ensure `startedAt`. `AttemptServiceImpl.startAttempt` enforces window +
  attempt count; `saveAnswer`/`submit` enforce the timer.
- `AttemptGrader`: proportional `MULTIPLE_CHOICE` branch (unit-tested edge cases: all-correct,
  all-wrong, partial, empty, over-select).
- Shuffle in the read path only: a pure `ShuffleOrder.seededBy(attemptId)` helper orders questions/
  options for the taker DTO; correct answers still never leave the server.

### Out of scope
- Question banks / random subset selection, adaptive/branching tests, proctoring — genuinely new scope,
  not needed to beat Google Classroom.

---

## Batch 4 — Grading model: late penalties, rubrics, final grade (P2b)

**Plan:** `2026-06-28-p2b-grading-model.md`. Touches `homework` + `gradebook` (they share the grade path).

### Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Late penalty | `Assignment.latePenaltyPctPerDay` + `maxLatePenaltyPct` (both nullable=off); at grade time compute `effectiveScore` from raw + days-late, store both | Configurable, transparent (raw kept), applied once at grading. |
| Rubric | Optional `Rubric` (ordered `RubricCriterion{label, maxPoints}`) on `Assignment`; `Review` carries per-criterion scores; grade = Σ criterion scores (overrides free-form points when present) | Structured grading without forcing it; absent rubric = today's single score. |
| Final course grade | `gradebook` computes **points-weighted percent**: `Σ awarded / Σ max` across a student's graded items in a group, plus per-type breakdown (homework vs test) | Zero extra config, standard, correct. Arbitrary category weights deferred. |
| Event payload | `SubmissionGraded`/`AttemptCompleted` carry `awarded` + `maxPoints` (+ groupId) | Gradebook needs the denominator to weight; currently only a flat score travels. |

### Architecture
- `homework`: `Rubric`/`RubricCriterion` entities under `homework`, `Assignment.rubricId` (nullable);
  `Review` gains criterion scores; `latePenaltyPctPerDay`/`maxLatePenaltyPct` on `Assignment`. A pure
  `GradeCalculator` bean (raw → effective given days-late + caps), unit-tested.
- `shared/event`: extend `SubmissionGraded`/`AttemptCompleted` with `awarded`, `maxPoints`, `groupId`
  (assessment must learn the student's group for the attempt — carried from the test's group context).
- `gradebook`: `ProgressEntry` gains `awarded`/`maxPoints`/`groupId`; new derived `course grade`
  computed in `GradebookServiceImpl` (weighted percent + breakdown); idempotent upsert unchanged.

### Out of scope
- Arbitrary weighted grade categories with a config UI (default points-weighting covers it). Curve/
  scaling, drop-lowest. Add when a teacher actually asks.

---

## Batch 5 — Course & cohort lifecycle (P2c)

**Plan:** `2026-06-28-p2c-lifecycle.md`.

### Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Course status | `DRAFT` → `PUBLISHED` (+ `ARCHIVED`); only `PUBLISHED` selectable when creating a group; `DRAFT` hidden from non-staff | Authors prepare courses unseen; published is the contract. |
| Group status | `DRAFT` → `RUNNING` → `ARCHIVED`; writes (enroll/schedule) allowed in DRAFT/RUNNING, ARCHIVED read-only | Distinguish running cohorts from historical ones; student "my courses" shows RUNNING+ARCHIVED. |
| Lifecycle impl | Data-driven `CourseStateMachine` / `GroupStateMachine` beans (allowed-transition maps → 409) | Per AGENTS.md state-machine convention; unit-tested in isolation. |
| Enforcement | Status checked in the writer services + a `status` filter on list endpoints | Backend boundary, mirrored in UI gating. |
| How enrollment learns a course is PUBLISHED | **Event-fed local read model** (`course_status_view`) fed by `CoursePublished`/`CourseArchived`/`CourseDeleted`; group-create checks it by id. A synchronous `CourseCatalog` read port was considered and **rejected**. | Keeps `enrollment` decoupled from `course` (no `codillas-course` dep, no `de.codillas.course.*` import) per the events + by-id rule (overview §6). |

### Architecture
- `course.Course`: `status` enum + `CourseStateMachine`; `POST /api/courses/{id}/publish|archive`;
  list/catalog endpoints filter by status; publishing/archiving/deleting emits
  `CoursePublished`/`CourseArchived`/`CourseDeleted`.
- `enrollment.Group`: `status` enum + `GroupStateMachine`; `POST /api/groups/{id}/start|archive`;
  enroll/schedule writers reject when ARCHIVED; `/api/me/groups` returns RUNNING+ARCHIVED.
  Group-create validates the course is PUBLISHED against enrollment's **own** `course_status_view`
  read model (event-fed) — never by importing or depending on `course`.

### Out of scope
- Prerequisites, self-service catalog enrollment, course versioning/cloning, capacity/waitlists —
  a different (self-serve SaaS) product shape; this school is cohort- and admin-driven. Revisit post-core.

---

## Cross-cutting decisions

- **Authorize-the-caller becomes a documented convention** (Batch 1) added to `backend/AGENTS.md`
  once shipped: "service methods loading a user-owned aggregate authorize caller vs resource (404 on
  mismatch, staff bypass), not only the role."
- **New events** (`CoursePublished`, `CourseArchived`, `CourseDeleted`, `GroupDeleted`,
  `UserEmailChanged`, and the extended `SubmissionGraded`/`AttemptCompleted`) are added to the event
  map in `overview.md` §7. `CoursePublished`/`CourseArchived`/`CourseDeleted` are consumed by
  `enrollment` into its `course_status_view` read model (the P2c decoupling).
- **Frontend** changes are per-plan and follow the existing mutation pattern (create-on-click, blanket
  invalidation) + Orval regen from the changed specs; i18n keys in uk/en/de for any new copy.

## Testing

Per the AGENTS.md TDD contract. Non-negotiable additions this spec introduces:
- **One negative-authz integration test per IDOR fixed** (student can't touch another's
  submission/attempt/grade/file; non-member can't subscribe to a room).
- **State-machine and scoring units** (`AttemptGrader` partial credit, `GradeCalculator` late penalty,
  `CourseStateMachine`/`GroupStateMachine` illegal transitions) — pure JUnit, no Spring.
- `ApplicationModules.of(...).verify()` stays green after every batch (new SPI/events must not break
  boundaries).

## Execution order

`p0-object-authz` → `p1-correctness` → `p2a-assessment-depth` → `p2b-grading-model` → `p2c-lifecycle`.
P0 first (security blocker); P1 before P2 (P2b's event-payload change rides on P1's discipline). Each
plan produces working, tested software on its own and is committed per-task (not pushed).
