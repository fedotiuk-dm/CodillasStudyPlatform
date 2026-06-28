# course — module contract

Local contract for the `course` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Course **template**: content & structure, authored once.
- **Key entities:** Course → Section → Lesson → Material. Each is a flat aggregate referencing
  its parent by id (`Section.courseId`, `Lesson.sectionId`, `Material.lessonId`); ordered via
  `shared.Sortable`. Delete cascades are handled in `CourseServiceImpl` (no JPA cascade /
  cross-row FKs). `Material` is `FILE` (carries `fileId` into files) or `LINK` (carries `url`) —
  which field is required is enforced in the service, not the schema.
- **Referenced by id (downstream):** `homework.Assignment` and `assessment.Test` point at a
  `lessonId`. A lesson is the hub; do not break that contract without re-pointing those modules.
- **Publishes:** `CourseDeleted`, `CoursePublished`, `CourseArchived` (all in `shared.event`;
  lifecycle events let `enrollment` track open courses by id without a synchronous read port). The
  publish/archive writers emit them inside the transaction, after the state-machine transition; a
  startup reconciler republishes `CoursePublished` for every already-PUBLISHED course.
- **Consumes:** —
- **Depends on (by id / events / API only):** files (materials, by API)
- **OpenAPI spec:** `backend/openapi/course-paths.yaml` (+ `course-schemas.yaml`) — single `course`
  tag (one tag = one generated `*Api` interface; never add a second tag per module).
- **Status:** backend done (structure CRUD + nested course-tree read). TODO: re-point
  `homework.Assignment` at `lessonId`; controller integration tests for the new endpoints; frontend.
