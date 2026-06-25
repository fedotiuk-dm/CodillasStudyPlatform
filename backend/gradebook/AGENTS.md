# gradebook — module contract

Local contract for the `gradebook` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Progress journal — **event-fed read model**, no joins into other modules.
- **Key entities:** ProgressEntry (derived)
- **Publishes:** —
- **Consumes:** StudentEnrolled, SubmissionGraded, AttemptCompleted
- **Depends on (by id / events / API only):** — (events only)
- **OpenAPI spec:** `backend/openapi/gradebook-paths.yaml` (+ `gradebook-schemas.yaml`)
- **Status:** skeleton — implement in phase 5.
