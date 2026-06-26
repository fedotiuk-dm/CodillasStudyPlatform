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
- [ ] Re-point `homework.Assignment` at a `lessonId` (`assessment.Test` already references `lessonId`).
- [x] Course detail / structure UI: Orval regen + teacher builder + student read-only view, role-gated, i18n (uk/en/de), `safeHref` guard on rendered links. Committed `3859c9d`.
- [x] Reusable `FileUploadField` → minio via the `files` module (one gateway, others reference by id); wired into course materials. Committed `b68b847`. Reuse for homework/chat next.
- [ ] Controller integration tests in `main` for the new endpoints (roles, 404s, cascade).
- [ ] Publish a `CourseStructureChanged` / `LessonCreated` event if `enrollment` needs to react.

### `enrollment` — partial (P1)
- [ ] Wire `ScheduledLesson` to a real `Lesson` (currently no `Lesson` to reference).
- [ ] Student-facing views: "my courses", "my schedule" (calendar/agenda).
- [ ] Attendance UI (manual by teacher — see open question in overview §13).
- [ ] Confirm `StudentEnrolled` event fans out to `chat` (add to room) + `gradebook` (init).

### `user` — stub (P1)
- [ ] Admin account provisioning → Keycloak via `keycloak-admin-client` (no self-registration).
- [ ] Default role STUDENT at creation; explicit promotion to TEACHER/ADMIN.
- [ ] Profile view/edit (decide split between Keycloak-owned vs platform `Profile`).

### `notification` — partial (P1)
- [ ] Email channel (currently in-app only).
- [ ] Deadline reminders (scheduled job off assignment due dates).
- [ ] Verify all events from overview §7 are consumed: `AssignmentPublished`, `SubmissionGraded`, `AttemptCompleted`, `MessagePosted`.

### `gradebook` — partial (P2)
- [ ] Confirm read model is populated from `SubmissionGraded` + `AttemptCompleted` + attendance.
- [ ] Teacher analytics (per-group stats), export (CSV).

### `chat` — partial (P2)
- [ ] Student↔teacher DM rooms (only group rooms today).
- [ ] Optional per-assignment thread (overview §5.1.11).

### `files` — partial (P2)
- [ ] Decide if versioning / soft-delete is needed (homework already versions submissions — may not be).
- [ ] File preview (nice-to-have).

### `homework` / `assessment` — done (polish only)
- [ ] `assessment`: formalize a metadata-driven `FieldRenderer` (one component per question type) so new question types are cheap. Borrow the *pattern* from the boosting calculator, **not** its FormulaNode engine.
- [ ] `assessment`: code question type + (later, if wanted) branching/adaptive tests — out of scope for "completeness".

## Cross-cutting

- [ ] **Admin panel** `/admin/*` (role-gated) — overview §12 decided one Next.js app; admin tasks currently done through teacher flows.
- [ ] Frontend role-gating is client-side/UX only; the real boundary is backend `@PreAuthorize`. Keep it that way; don't trust the client.
- [ ] One master OpenAPI aggregation endpoint (each module publishes separately today).

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
