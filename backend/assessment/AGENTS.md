# assessment — module contract

Local contract for the `assessment` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Test/control builder + auto-grading.
- **Key entities:** Test, Question, Option, Attempt, Answer
- **Publishes:** AttemptCompleted
- **Consumes:** —
- **Depends on (by id / events / API only):** course (lesson), user (by id)
- **OpenAPI spec:** `backend/openapi/assessment-paths.yaml` (+ `assessment-schemas.yaml`)
- **Status:** implemented (builder + attempts, auto-grade, timer/window, limits, partial credit, shuffle).
