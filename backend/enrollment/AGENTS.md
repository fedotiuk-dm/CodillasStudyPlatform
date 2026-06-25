# enrollment — module contract

Local contract for the `enrollment` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Cohort **instance**: a Group runs a Course on a schedule; membership + attendance.
- **Key entities:** Group, Membership, ScheduledLesson, Attendance
- **Publishes:** StudentEnrolled
- **Consumes:** —
- **Depends on (by id / events / API only):** course, user (by id)
- **OpenAPI spec:** `backend/openapi/enrollment-paths.yaml` (+ `enrollment-schemas.yaml`)
- **Status:** skeleton — implement in phase 2.
