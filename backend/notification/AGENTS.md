# notification — module contract

Local contract for the `notification` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Email + in-app notifications, event-driven (Thymeleaf templates, mailpit in dev).
- **Key entities:** Notification, NotificationMembership (local group roster)
- **Publishes:** —
- **Consumes:** StudentEnrolled (roster), GroupDeleted (purge), AssignmentPublished +
  AssignmentDueSoon + AnnouncementPosted (fan-out to group), SubmissionGraded, AttemptCompleted,
  DirectMessagePosted (DM-scoped — group-channel per-message noise is deliberately not consumed),
  UserEmailChanged (email read model).
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
  ids and may not read the user module. `DbRecipientEmailResolver` keeps a local read model fed by
  the `UserEmailChanged` event, so email works end-to-end; without a mail server it degrades to
  in-app only.
