# notification — module contract

Local contract for the `notification` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Email + in-app notifications, event-driven (Thymeleaf templates, mailpit in dev).
- **Key entities:** Notification
- **Publishes:** —
- **Consumes:** AssignmentPublished, SubmissionGraded, AttemptCompleted, MessagePosted
- **Depends on (by id / events / API only):** user (by id)
- **OpenAPI spec:** `backend/openapi/notification-paths.yaml` (+ `notification-schemas.yaml`)
- **Status:** skeleton — implement in phase 7.
