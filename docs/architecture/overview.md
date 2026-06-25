# CodillasStudyPlatform — Architecture Overview

> Status: design draft · Date: 2026-06-25 · Owner: @fedotiuk-dm
> This is the living architecture doc. It is the source of truth for module
> boundaries and flows. Detailed per-module specs and API schemas live next to
> this file as they are written.

## 1. Vision & goals

A learning platform for the Codillas IT school that replaces the current
Google Classroom + Meet patchwork. The burning problem today is **homework
handoff**: screenshots and files scattered across Classroom, no clear status,
messy teacher↔student exchange.

Goals, in order:

1. **Clean, stateful homework flow** — assign → submit → review → grade →
   return, with versioned submissions and explicit statuses.
2. **Interactive test/quiz builder** + control tests with auto-grading.
3. **Real-time chat** for group + teacher↔student communication.
4. **Progress/gradebook** so students and teachers see where they stand.

This is simultaneously: the real platform for the school, the author's school
defense project, and intended to be **open-sourced / handed off** later. So the
code and docs target production quality and onboarding clarity, not a demo.

## 2. Non-goals (deliberately out of scope)

- **Video calls** — keep Google Meet. We only store the link + recording URL.
  No WebRTC.
- **Billing/payments** — handled outside the platform.
- **Custom auth** — reuse Keycloak (roles, JWT) from the boosting project.
- **Custom file storage** — reuse the S3/minio `files` module from boosting.
- **Mentor/parent/examiner roles** — start with 3 roles; add later if needed.

## 3. Roles

| Role        | Capabilities                                                                          |
|-------------|---------------------------------------------------------------------------------------|
| **Admin**   | Create courses, groups; enroll students; assign teachers; sees everything.            |
| **Teacher** | Build course structure, run lessons, assign/review homework, build/grade tests, chat. |
| **Student** | Take lessons, submit homework, take tests, see own progress, chat.                    |

Backed by Keycloak realm roles. Authorization is enforced at the module API
boundary (method security), not in the UI.

## 4. Domain model — the key split

The model hinges on **template vs instance**:

- `course` is the **template**: structure and content, authored once
  (Course → Section → Lesson → Material, plus assignment/test definitions).
- `enrollment` is the **instance**: a **Group/cohort** runs a course on a
  **schedule** (ScheduledLesson with dates + Meet links), with members and
  attendance.

Homework submissions, test attempts and grades are tied to the **(student,
group)** context — never to the bare template. This is what makes the cohort
model work and keeps grades attributed correctly.

```
course (template)            enrollment (instance)
  Course                       Group ───────── runs ──▶ Course
   └ Section                    ├ Membership (Student/Teacher)
      └ Lesson                  ├ ScheduledLesson (date, Meet link) ─▶ Lesson
         ├ Material             └ Attendance
         ├ Assignment def
         └ Test def
```

## 5. Core flows

### 5.1 End-to-end (happy path)

1. **Admin** creates a course, creates a group, enrolls students, assigns a teacher.
2. **Teacher** builds structure: sections → lessons, attaches materials, Meet
   link, and (optionally) homework + a test per lesson.
3. **Lesson day**: live session over Google Meet (link in the lesson). After:
   recording URL + materials posted.
4. **Homework**: teacher publishes an assignment with a deadline. Student sees a
   card with status **To-do**.
5. **Student submits**: text + files/screenshots + links. Status → **On review**.
6. **Teacher reviews**: comments + grade. Status → **Graded (passed)** or
   **Returned (needs rework)**.
7. **Rework**: student resubmits → new version; full history retained.
8. **Tests/control**: student takes an interactive test; auto-graded; result
   recorded.
9. **Gradebook**: student + teacher see progress (homework grades + test scores
   + attendance).
10. **Notifications**: deadline reminders, "homework returned", "new
    assignment", new chat message.
11. **Chat**: group channel + student↔teacher DM; optional per-assignment thread.

### 5.2 Homework lifecycle (state machine)

```
DRAFT ─▶ PUBLISHED ─▶ SUBMITTED ─▶ IN_REVIEW ─┬─▶ GRADED(passed)         [done]
                          ▲                    └─▶ RETURNED(needs_rework)
                          └────── RESUBMITTED ◀─────────────┘
OVERDUE = derived flag when deadline passes and not SUBMITTED.
```

Each `Submission` is **versioned**: files, text, links, timestamp, author.
The teacher↔student handoff happens in one place with an explicit status —
no more screenshots and DMs.

### 5.3 Test / control lifecycle

```
[teacher] build test (question types: single / multiple / text / code)
   ─▶ PUBLISHED (control test: with a time window)
   ─▶ [student] ATTEMPT (timer, attempt limit) ─▶ auto-grade ─▶ result in gradebook
```

## 6. Module architecture (Spring Modulith)

Single Spring Boot deployment. Base package `de.codillas`. Each module is a
direct sub-package `de.codillas.<module>`. Rules that keep the monolith from
rotting into a ball of mud:

- Modules talk via **domain events**, not by calling each other's repositories.
- Cross-module references are **by id only**, never by foreign entity.
- Each module **owns its tables** (its own Flyway migrations); no cross-module
  schema access.
- `@ApplicationModuleTest` + Modulith's `verifies()` enforce boundaries in CI.

**Packaging:** Maven multi-module (mirrors the boosting project) — each module is
its own Maven module with its own `pom.xml` and an `@ApplicationModule` package,
and a `main` module assembles them into the runnable app. **API-first**: each
module ships an OpenAPI spec; `openapi-generator` produces server interfaces +
DTOs, and Orval generates the frontend client from the same specs. Mapping via
MapStruct.

### New domain modules

| # | Module       | Responsibility                               | Key entities                                   |
|---|--------------|----------------------------------------------|------------------------------------------------|
| 1 | `user`       | Profiles + roles on top of Keycloak          | User, Profile, Role                            |
| 2 | `course`     | Course template: content & structure         | Course, Section, Lesson, Material              |
| 3 | `enrollment` | Group/cohort runs a course on a schedule     | Group, Membership, ScheduledLesson, Attendance |
| 4 | `homework`   | Assignments + versioned submissions + review | Assignment, Submission, Review, Grade          |
| 5 | `assessment` | Test/control builder + auto-grading          | Test, Question, Option, Attempt, Answer        |
| 6 | `gradebook`  | Progress journal — event-driven read model   | ProgressEntry (derived)                        |

### Copied from boosting (pattern exists — do not rewrite)

| # | Module         | Responsibility                                                                |
|---|----------------|-------------------------------------------------------------------------------|
| 7 | `chat`         | Full WebSocket chat: group channel, student↔teacher DM, per-assignment thread |
| 8 | `notification` | Email + in-app notifications, event-driven                                    |
| 9 | `files`        | S3/minio storage; attachments for homework / materials / chat                 |

### Cross-cutting (shared kernel / infra — not domain modules)

- `shared` — base types, event contracts, security context, error handling, API conventions.
- Security/Keycloak config — JWT, realm role mapping (from boosting).
- `openapi` — OpenAPI spec generation feeding the Orval frontend client.

## 7. Inter-module events

The glue. Publishers don't know their consumers.

| Event                 | Published by | Consumed by                                      |
|-----------------------|--------------|--------------------------------------------------|
| `StudentEnrolled`     | enrollment   | chat (add to channel), gradebook (init progress) |
| `AssignmentPublished` | homework     | notification                                     |
| `SubmissionGraded`    | homework     | gradebook, notification                          |
| `AttemptCompleted`    | assessment   | gradebook, notification                          |
| `MessagePosted`       | chat         | notification (if recipient offline)              |

Events are persisted via Spring Modulith's event publication registry
(at-least-once, retried on restart) so a consumer failure never silently drops
a notification or a gradebook update.

## 8. Persistence

- **Postgres**, one database.
- **Liquibase**, changelogs scoped per module
  (`<module>/src/main/resources/db/changelog/`), each module owning its own
  tables; a master changelog in `main` includes them. Module boundaries are
  enforced in data, not just in code.
- `gradebook` keeps its own derived tables, populated from events — no joins
  into other modules' tables.

## 9. Security

- Dedicated **Keycloak** realm `codillas` with realm roles `ADMIN` / `TEACHER` /
  `STUDENT` (`backend/keycloak/realm-export.json`).
- **No self-registration** (`registrationAllowed: false`) — admin/teacher create
  accounts. In prod via the platform's admin screen → backend `user` module →
  `keycloak-admin-client`; in dev, three seed users (admin/teacher/student,
  password `password`).
- **Default role is `STUDENT`**, assigned at user creation; promotion to
  `TEACHER` / `ADMIN` is explicit.
- Roles reach the app as a flat `roles` claim (realm "realm-roles-flat" mapper);
  Spring maps it to `ROLE_*` via config. The resource server validates the JWT;
  method-level `@PreAuthorize` guards endpoints at module API boundaries.

## 10. Tech stack

| Layer        | Choice                                                       |
|--------------|--------------------------------------------------------------|
| Language     | Java 25                                                      |
| Backend      | Spring Boot + Spring Modulith (Maven multi-module)           |
| Persistence  | Postgres + Liquibase + Spring Data JPA                       |
| Auth         | Keycloak (OIDC / JWT)                                        |
| Realtime     | WebSocket (chat module)                                      |
| Storage      | S3 / minio (files module)                                    |
| API contract | OpenAPI → Orval-generated client                             |
| Frontend     | Next.js (+ Orval client)                                     |
| Infra        | Docker (reuse boosting's Traefik / minio / Keycloak compose) |

Exact dependency versions are pinned in `pom.xml` at scaffold time.

## 11. Build order (roadmap)

Even with no deadline pressure, build in dependency order so each layer is
usable and testable before the next:

1. **Foundation** — project skeleton, Spring Modulith setup, Postgres/Liquibase,
   Keycloak security, `shared`, `user`. (+ copy `files`.)
2. **Course & cohort** — `course`, `enrollment` (the structural backbone).
3. **Homework** — `homework` (the wedge that solves the real pain) + `files`
   integration + `notification`.
4. **Assessment** — `assessment` test/control builder + auto-grading.
5. **Gradebook** — `gradebook` read model wired to homework/assessment events.
6. **Chat** — copy + adapt the boosting `chat` module.
7. **Notification polish** — wire all events, email + in-app.

Each phase gets its own spec + implementation plan.

## 12. Decisions log

| Decision       | Choice                                | Rationale                                                                    |
|----------------|---------------------------------------|------------------------------------------------------------------------------|
| Architecture   | Spring Modulith (modular monolith)    | One deploy, enforced boundaries, can split to services later without rewrite |
| Learning model | Cohort (groups/streams)               | Matches how the school actually runs                                         |
| Roles at start | 3 (Admin/Teacher/Student)             | Covers all current needs; YAGNI on more                                      |
| Chat           | Full WebSocket (copied from boosting) | Pattern already exists and is production-grade                               |
| Gradebook      | Separate event-driven read model      | Clean for dashboards; decoupled from source modules                          |
| Video          | Stay on Google Meet                   | Don't reinvent WebRTC                                                        |
| Stack          | Next.js + Orval + Postgres            | Matches author's other production projects                                   |
| Admin UI       | Single Next.js, role-gated `/admin/*` | One stack/pipeline; no second SPA or Vaadin in the Modulith backend          |
| Dev infra      | Lean self-contained docker (no Vault) | Zero-config local + friendlier for OSS; Vault/Traefik/monitoring at deploy   |

## 13. Open questions

- Question types for the test builder beyond single/multiple/text/code?
- Does a control test need anti-cheat (tab-switch detection, question shuffling)?
- Attendance: manual by teacher, or derived from Meet/login?
