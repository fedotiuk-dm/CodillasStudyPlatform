# notification — module contract

Local contract for the `notification` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Email + in-app notifications, event-driven (Thymeleaf templates, mailpit in dev).
- **Key entities:** Notification, NotificationMembership (local group roster)
- **Publishes:** —
- **Consumes:** StudentEnrolled (roster), AssignmentPublished (fan-out to group), SubmissionGraded,
  AttemptCompleted. MessagePosted is **not** consumed in v1 (chat delivers messages real-time;
  per-message email would be noise).
- **Depends on (by id / events / API only):** user (by id)
- **OpenAPI spec:** `backend/openapi/notification-paths.yaml` (+ `notification-schemas.yaml`)
- **Status:** implemented (v0.1.0 in-app + email infrastructure).

## Conventions

- **Recipient resolution.** Direct events name their recipient (SubmissionGraded/AttemptCompleted →
  the student). Group events don't, so a local `NotificationMembership` roster (fed by
  StudentEnrolled, like gradebook/chat) lets AssignmentPublished fan out to members.
- **Email is best-effort and decoupled.** `EmailNotifier` renders a Thymeleaf template and sends via
  an **optional** `JavaMailSender` (present only when `spring.mail.*` is set — mailpit in dev). The
  recipient address comes from `RecipientEmailResolver` — a seam, since the module only knows user
  ids and may not read the user module. The default resolver has no source yet, so **email is
  skipped** (in-app always works); wire a real resolver (a user-email event or API) to enable it.
