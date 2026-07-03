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

## ✅ Hardening to production — DONE (implemented 2026-06-29…07-01 on `feat/lms-hardening`)

The 2026-06-28 backend audit found three gap classes blocking real use
(master spec: [`specs/2026-06-28-lms-hardening-design.md`](../superpowers/specs/2026-06-28-lms-hardening-design.md)).
All five plans are **implemented** — backend in commit `6af4743`, frontend integration in the
commits that follow it on `feat/lms-hardening`:

1. **P0 — object-level authorization** ✅ — owner-or-staff checks (`findByIdForCaller`, 404 not
   403), `FileAccessAuthorizer` SPI, WS subscription authz.
2. **P1 — correctness & integrity** ✅ — optimistic locking (`@Version` on the shared base),
   late-flag, grade upsert, upload size/type limits, deletes + cascades, and the email channel
   is live (`DbRecipientEmailResolver` fed by `UserEmailChanged`).
3. **P2a — assessment depth** ✅ — attempt limits, timer + availability window, partial credit,
   deterministic shuffle (`ShuffleOrder`).
4. **P2b — grading model** ✅ — late penalties, rubrics, weighted course grade.
5. **P2c — lifecycle** ✅ — `CourseStateMachine` + `GroupStateMachine` (DRAFT/PUBLISHED/ARCHIVED,
   DRAFT/RUNNING/ARCHIVED).

## ✅ Classroom-parity final mile (2026-07-03)

The last visible gaps against Google Classroom, closed on `feat/lms-hardening`:

- **Announcements** — new `announcement` module (the class stream): teacher posts with pin,
  member-scoped reads (404 outside), `AnnouncementPosted` → notification fan-out (in-app +
  email), frontend stream view + dashboard feed + bell deep-link. uk/en/de.
- **Files page persistent** — `GET /api/files/my` (uploader-scoped, paginated); the page no
  longer forgets uploads on refresh; delete is teacher-gated in UI like the backend.
- **Attendance for teachers** — mark/read relaxed from `@RequiresAdmin` to `@RequiresTeacher`.
- **Profile edit** — dialog on the user menu over the existing `PUT /api/users/me`.
- **Schedule month view + `.ics` export** — calendar grid with lesson dots + client-side
  RFC 5545 export (bridge for Google Calendar users).
- **PWA manifest + icon** — installable, branded.
- **Modulith boundary test** — `ModulithArchitectureTest` now actually enforces the §6 rules in
  the build (files SPI exposed via `@NamedInterface`; `main`'s `config` wiring excluded).
- **Ops docs** — [`operations/accounts.md`](../operations/accounts.md) (Keycloak provisioning for
  non-devs) + [`operations/deployment.md`](../operations/deployment.md) (go-live checklist).

**What actually remains before the school can drop Google:** the deploy itself — follow
[`operations/deployment.md`](../operations/deployment.md) (TLS, real secrets, backups, smoke test).

## Legend

- **stub** — scaffold only (entity + CRUD plumbing), no real domain logic.
- **partial** — real domain logic exists, but core flows or UI are missing.
- **done** — core flows work end-to-end (backend + UI + tests); only polish remains.

## Module status at a glance (refreshed 2026-07-03)

| Module         | Backend | Frontend | Notes                                                               |
|----------------|---------|----------|----------------------------------------------------------------------|
| `shared`       | done    | —        | Kernel: events, security, error handling, `@Version` base.          |
| `user`         | done    | done     | Profile + roles on `/me`; edit dialog. Provisioning stays in Keycloak (see operations/accounts.md). |
| `course`       | done    | done     | Backbone + builder UI + DRAFT/PUBLISHED/ARCHIVED lifecycle.         |
| `enrollment`   | done    | done     | Groups + lifecycle, schedule UI, attendance (teacher-markable).     |
| `homework`     | done    | done     | Full lifecycle, versioned submissions, rubrics, late penalties.     |
| `assessment`   | done    | done     | Builder + attempts, timer/window/limits, partial credit, CODE type. |
| `gradebook`    | done    | done     | Event-fed read model; analytics + CSV export (client-side).         |
| `chat`         | done    | done     | Group rooms + canonical DM, WebSocket, object-level WS authz.       |
| `notification` | done    | done     | In-app + email + deadline reminders + viewer-locale templates.      |
| `announcement` | done    | done     | Class stream: teacher posts, member reads, notification fan-out.    |
| `files`        | done    | done     | Upload/download/delete + "my uploads" list. No versioning/preview (deliberate). |

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
- [x] Attendance: read endpoint `listAttendance` + current-state display in the group dialog. Committed `0fd4c37`. Opened to teachers (`@RequiresTeacher`) in the 2026-07-03 final-mile batch.

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
- [x] **Admin overview** `/dashboard/admin` (ADMIN-only) — consolidated landing: platform totals (courses/groups/people) + quick links. Committed `38c1c48`. The admin *capabilities* already live in the role-gated views; this is just the home.
- [x] Frontend role-gating is client-side/UX only; the real boundary is backend `@PreAuthorize` + `RoleHierarchy`. Kept that way (documented in overview §9).
- [~] Master OpenAPI aggregation — **deferred**: a dev-docs convenience (single Swagger UI), not user-facing; per-module specs already drive Orval + generated server interfaces.

## Prioritized roadmap

1. **Deploy** — follow [`operations/deployment.md`](../operations/deployment.md); the feature
   surface is done, hosting it reliably (TLS, secrets, backups, smoke test) is what's left.
2. **Run the school on it** — feedback from real lessons drives the next items, not this doc.
3. **Then** revisit the deferred niceties (file preview, per-assignment auto-threads, presence)
   and the future vision below.

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
