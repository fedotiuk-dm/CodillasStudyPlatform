# homework — module contract

Local contract for the `homework` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Assignments + versioned submissions + teacher review/grade.
- **Key entities:** Assignment, Submission, Review, Grade
- **Publishes:** AssignmentPublished, SubmissionGraded
- **Consumes:** `GroupDeleted` (purge the group's assignments + submissions/reviews/grades),
  `LessonsDeleted` (clears `Assignment.lessonId`; the assignment survives)
- **Depends on (by id / events / API only):** course (lesson), files, user (by id)
- **OpenAPI spec:** `backend/openapi/homework-paths.yaml` (+ `homework-schemas.yaml`)
- **Status:** implemented (full lifecycle, versioned submissions, rubrics, late penalties, reminders).
