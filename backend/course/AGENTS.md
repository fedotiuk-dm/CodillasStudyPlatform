# course — module contract

Local contract for the `course` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Course **template**: content & structure, authored once.
- **Key entities:** Course → Section → Lesson → Material. Each is a flat aggregate referencing
  its parent by id (`Section.courseId`, `Lesson.sectionId`, `Material.lessonId`); ordered via
  `shared.Sortable`. Delete cascades run in the database (`ON DELETE CASCADE`, since 0.3.0), not
  via JPA cascade — so a deleting writer must publish `LessonsDeleted` itself, reading the ids
  *before* the delete. `Material` is `FILE` (carries `fileId` into files) or `LINK` (carries `url`) —
  which field is required is enforced in the service and, since 0.7.0, in the schema
  (`ck_materials_shape`).
- **Referenced by id (downstream):** `homework.Assignment` and `assessment.Test` point at a
  `lessonId`. A lesson is the hub; do not break that contract without re-pointing those modules.
- **Publishes:** `CourseDeleted`, `CoursePublished`, `CourseArchived`, `LessonsDeleted` (all in
  `shared.event`; lifecycle events let `enrollment` track open courses by id without a synchronous
  read port; `LessonsDeleted` lets enrollment/homework/assessment clear their `lessonId`). The
  publish/archive writers emit them inside the transaction, after the state-machine transition; a
  startup reconciler republishes `CoursePublished` for every already-PUBLISHED course.
- **Consumes:** `FileDeleted` (drops the orphaned FILE materials)
- **Depends on (by id / events / API only):** files (materials, by API)
- **OpenAPI spec:** `backend/openapi/course-paths.yaml` (+ `course-schemas.yaml`) — single `course`
  tag (one tag = one generated `*Api` interface; never add a second tag per module).
- **Status:** implemented (structure CRUD + course tree + DRAFT/PUBLISHED/ARCHIVED lifecycle + builder UI).
