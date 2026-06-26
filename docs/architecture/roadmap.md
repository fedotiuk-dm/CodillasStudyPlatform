# CodillasStudyPlatform — Status & Roadmap

> Status: living status doc · Date: 2026-06-26 · Owner: @fedotiuk-dm
>
> **What this is:** the single source of truth for *what is built, what is a stub,
> and what is left* — per module. Architecture lives in [`overview.md`](overview.md);
> conventions live in the `AGENTS.md` chain. This file is the only place that tracks
> progress and TODOs, so those two stay stable contracts (not changelogs).
>
> **How to use (humans & agents):** before touching a module, read its row here for
> current maturity and the open TODOs. When you finish work, flip the status and tick
> the TODO — don't add TODOs to `AGENTS.md`.

## ✅ Core complete (Google-Classroom-replacement surface)

P0–P2 functional scope is done and green, on stacked `feat/*` branches:
`course-backbone` → `enrollment` → `roles` → `notification` → `gradebook` → `chat` → `i18n`.

Covered: course **structure** (Section→Lesson→Material) + teacher builder + file upload to
minio; **cohorts** (groups, schedule↔lesson, attendance) + student "my courses/schedule";
**homework** (assign→submit→review→grade, versioned) + **tests** (build + auto-grade);
**gradebook** (event-fed) + analytics + CSV; **chat** (group + canonical DM, WebSocket);
**notifications** (in-app + email + deadline reminders); **RBAC** with ADMIN>TEACHER>STUDENT
gradation; full **uk/en/de** i18n. Every feature has tests; the `main` integration suite is green.

Deliberately deferred (YAGNI / your call): admin-panel shell, master OpenAPI aggregation,
file versioning/preview, assessment metadata-FieldRenderer + code question type, `MessagePosted`
notifications (needs presence). **Future vision** (multi-tenant + payments) is separate, below.

## Legend

- **stub** — scaffold only (entity + CRUD plumbing), no real domain logic.
- **partial** — real domain logic exists, but core flows or UI are missing.
- **done** — core flows work end-to-end (backend + UI + tests); only polish remains.

## Module status at a glance

| Module         | Backend  | Frontend | Notes                                                              |
|----------------|----------|----------|--------------------------------------------------------------------|
| `shared`       | done     | —        | Kernel: events, security, error handling, API conventions.         |
| `user`         | stub     | partial  | Only `Profile`. No account provisioning, no profile edit.          |
| `course`       | partial  | done     | Backbone + builder UI done. TODO: re-point homework; controller IT tests.  |
| `enrollment`   | partial  | partial  | Group/Membership/ScheduledLesson/Attendance exist; no schedule UI.  |
| `homework`     | done     | done     | Full lifecycle + versioned submissions + review/grade. State machine.|
| `assessment`   | done     | done     | Test/Question/Attempt + auto-grade. Strongest module.               |
| `gradebook`    | partial  | partial  | Event-fed read model; no analytics/export.                          |
| `chat`         | partial  | done     | Group rooms + WebSocket. No DM / per-assignment thread.             |
| `notification` | partial  | done     | In-app works. Email + deadline reminders not wired.                 |
| `files`        | partial  | done     | Upload/download/delete. No versioning, no preview.                  |

## The #1 gap: the course backbone

`overview.md` makes `Lesson` the hub everything hangs off — materials, per-lesson
assignments, per-lesson tests, `ScheduledLesson` (Meet link), attendance. Today
`course` has only the `Course` entity. **`Section`, `Lesson`, `Material` do not
exist**, so homework, tests, and the schedule float free of course structure.
This is the central missing piece; most of the roadmap below depends on it.

## Per-module detail & TODOs

### `course` — backbone built (P0, backend done)
- [x] Model `Section → Lesson → Material` (OpenAPI-first → entities → Liquibase 0.2.0). Structure CRUD + nested course-tree read. Unit-tested (material validation, cascade delete); schema validated by `main` integration suite.
- [x] Re-point `homework.Assignment` at a `lessonId` — already wired: `Assignment.lessonId` (nullable) + spec field exist, same as `assessment.Test`. No data change needed. (Optional follow-up: homework↔lesson **UX** — pick a lesson when creating homework, show it under the lesson.)
- [x] Course detail / structure UI: Orval regen + teacher builder + student read-only view, role-gated, i18n (uk/en/de), `safeHref` guard on rendered links. Committed `3859c9d`.
- [x] Reusable `FileUploadField` → minio via the `files` module (one gateway, others reference by id); wired into course materials. Committed `b68b847`. Reuse for homework/chat next.
- [x] Controller integration tests in `main` for the new endpoints (roles, 404s, cascade). Committed `d6e5598`.
- [~] Publish a `CourseStructureChanged` / `LessonCreated` event — **skipped (YAGNI)**: no module consumes it yet. Add when `enrollment` actually needs to react.

**P0 done.** Course backbone + builder UI + reusable file upload + tests all green and committed.

### `enrollment` — partial (P1)
- [x] Wire `ScheduledLesson` to a real `Lesson` (optional `lessonId` by-id ref). Committed `f90dd25`.
- [x] Confirm `StudentEnrolled` event fans out — verified: consumed by `gradebook`, `chat`, `notification`.
- [x] Student-facing views: "My courses" + "My schedule" (backend `/api/me/groups` + `/api/me/schedule`, current-user scoped; frontend views + nav). Committed `0f308b2`.
- [x] Attendance: read endpoint `listAttendance` + current-state display in the group dialog. Committed `0fd4c37`. Note: attendance is `@RequiresAdmin` (matches all enrollment writes in this module) — opening it to teachers, and i18n for the groups views, are separate follow-ups.

**Enrollment P1 block done.**

### `user` / security — partial (P1)
- [x] **Role gradation** — `RoleHierarchy` ADMIN > TEACHER > STUDENT + roles surfaced on `/api/users/me` + graded frontend gating. Committed `662b55b`. (This was the real gap: `@RequiresX` existed but weren't graded — an admin didn't satisfy a teacher gate.)
- [~] Admin account provisioning → Keycloak — **dropped (YAGNI)**: admin self-registration is pointless and the admin-client write can't be verified here. Accounts created in the Keycloak console; default STUDENT via realm default-roles.
- [x] Profile view/edit — GET/PUT `/api/users/me` already done.
- [ ] (optional) In-app role assignment UI — only if you later want role management in the platform (needs Keycloak write).

### `notification` — P1 done
- [x] Email channel — already built (`EmailNotifier`: Thymeleaf + `JavaMailSender` via `ObjectProvider` (skips gracefully when no server) + `RecipientEmailResolver`, `@Async`); dev → mailpit. Wired into every event handler.
- [x] Deadline reminders — `AssignmentDueSoon` event + homework `DueReminderJob` (hourly, fires once via `due_reminder_sent`) → notification fans out in-app + email. Committed `2b280e4`.
- [x] Events consumed: `AssignmentPublished`, `SubmissionGraded`, `AttemptCompleted` ✅.
- [x] `MessagePosted` → notification — done, **DM-scoped**: chat publishes `DirectMessagePosted` for DIRECT rooms only (one clear recipient → no presence needed, no group spam); notification sends in-app + email. Committed `ca02346`. Group-channel-on-every-message notifications still intentionally skipped.

### `gradebook` — P2 done
- [x] Read model is event-fed (`GradebookEventListener` consumes `SubmissionGraded` + `AttemptCompleted`). Attendance is not a grade source.
- [x] Teacher analytics (per-student + group average) + CSV export — client-side from the existing group gradebook (no backend needed). Committed `0e2b995`.

### `chat` — partial (P2)
- [x] Student↔teacher DM rooms — `DIRECT` type + creation + frontend dialog already existed; made creation **idempotent** (one canonical room per pair). Committed `06c670b`. Follow-up (polish): show the other member's name for a nameless DM in the room list (needs the room response to carry members).
- [~] Per-assignment thread — `ASSIGNMENT_THREAD` room type + `referenceId` exist and are creatable; auto-threading from the homework UI is a nicety, deferred.

### `files` — done (no P2 work needed)
- [~] Versioning / soft-delete — **not needed (YAGNI)**: homework already versions submissions, and files are id-referenced content blobs (a changed material is a new upload + ref swap). Decided against.
- [~] File preview — nice-to-have, **deferred**. Upload/download/delete work; the reusable `FileUploadField` covers attachment UX everywhere.

### `homework` / `assessment` — done
- [x] `assessment`: **CODE question type** — code answers (manual-graded like SHORT_TEXT, monospace editor in the taker). Committed `c39e9f5`. The builder already renders new non-option types generically, so the metadata-`FieldRenderer` formalization buys little — not done.
- [~] `assessment`: branching/adaptive tests — deferred (genuinely new scope; not needed to replace Google Classroom).

## Cross-cutting

- [x] **i18n complete** — all views localized across uk/en/de (9 feature namespaces). Committed `d863d59`.
- [x] **Role gradation** — `RoleHierarchy` ADMIN > TEACHER > STUDENT + roles on `/me` + graded frontend gating. Committed `662b55b`.
- [~] **Admin panel** `/admin/*` — **deferred (YAGNI for now)**: every admin capability (create courses, manage groups/members, schedule, attendance) already exists in the role-gated views and an admin sees them through the same nav. A dedicated shell would re-surface existing screens, not add capability. Build it later if you want a consolidated admin home — say the word.
- [x] Frontend role-gating is client-side/UX only; the real boundary is backend `@PreAuthorize` + `RoleHierarchy`. Kept that way (documented in overview §9).
- [~] Master OpenAPI aggregation — **deferred**: a dev-docs convenience (single Swagger UI), not user-facing; per-module specs already drive Orval + generated server interfaces.

## Prioritized roadmap

1. **P0 — `course` backbone** (`Section/Lesson/Material`) + re-point homework/tests at lessons. Unblocks everything structural.
2. **P1 — connect the dots:** lesson↔schedule↔group, student "my courses/schedule" views, account provisioning, email + deadline notifications.
3. **P2 — polish:** attendance UI, gradebook analytics/export, DM chat, admin panel, file preview.

## Future vision (post-completion — NOT scheduled, do not build yet)

Captured for direction only. Gated behind everything above being done. Revisit the
trade-offs when the core platform is complete — most of this is a different product
shape (SaaS) than today's single-school deploy.

- **Multi-tenant school profiles** — the platform hosts many schools, each isolated
  (its own users, courses, branding). Big architectural lever: tenant scoping touches
  every module's data and security. Decide row-level tenant column vs schema-per-tenant
  *before* this, not during.
- **Payments / subscriptions** — ⚠️ reverses overview §2, which currently lists billing
  as a non-goal ("handled outside the platform"). If adopted, this becomes a new
  `billing` module (events: `SubscriptionStarted/Renewed/Expired`), still keeping
  payment-provider specifics behind an adapter.
- **Subscription / profile expiration** — access gating when a subscription lapses
  (read-only or locked state), grace periods, renewal reminders (reuse `notification`).

When this phase starts, update overview §2 and §12 to match — don't let this doc and
the architecture overview disagree on whether billing is in scope.

## Deliberately NOT doing (YAGNI)

- Rebuilding `assessment`/`homework` on the calculator's metadata engine — fixed, small set of question types doesn't need a schema-agnostic form engine.
- Rebuilding anything from scratch — the existing modules conform to the architecture and work.
- WebRTC, billing, custom auth, custom storage — see overview §2.
