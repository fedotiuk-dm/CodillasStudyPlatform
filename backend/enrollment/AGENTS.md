# enrollment — module contract

Local contract for the `enrollment` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Cohort **instance**: a Group runs a Course on a schedule; membership + attendance.
- **Key entities:** Group, Membership, ScheduledLesson, Attendance
- **Publishes:** GroupCreated, StudentEnrolled, GroupDeleted, `GroupArchived`, `GroupResumed`
- **Consumes:** CoursePublished / CourseArchived / CourseDeleted (local `course_status_view` read
  model; `CourseArchived` additionally retires the cohorts running the course, `CourseDeleted`
  deletes them), `LessonsDeleted` (clears `ScheduledLesson.lessonId`; the slot keeps its title
  and time)
- **Depends on (by id / events / API only):** course, user (by id)
- **OpenAPI spec:** `backend/openapi/enrollment-paths.yaml` (+ `enrollment-schemas.yaml`)
- **Status:** implemented (groups + lifecycle, membership, schedule, attendance; teachers mark attendance).
