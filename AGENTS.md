# CodillasStudyPlatform — agent contract (root)

Read this before working. Full architecture: `docs/architecture/overview.md`.

Learning platform (LMS) for the Codillas IT school. Spring Modulith backend (Maven multi-module,
`backend/`) + Next.js frontend (`frontend/`).

## Contract chain (read top-down before editing)

1. **This file** — project-wide invariants.
2. **`backend/AGENTS.md`** — backend conventions (layering, API-first, MapStruct, events, style).
3. **`backend/<module>/AGENTS.md`** — the module you're touching.

`CLAUDE.md` is a symlink to `AGENTS.md` in every directory — the same file, so Claude Code and
Codex read the same contract. Edit `AGENTS.md`; the two never drift.

## Invariants

- Modules talk via domain **events + by-id references only** — never import another module's `@Entity`.
- **API-first**: change the OpenAPI spec under `backend/openapi/`, then regenerate; generated code
  is read-only.
- **Sorting is the backend's job.** Every list and nested collection comes back from the API already
  in display order (named `Sort` constants / `Pageable` `sort` param); the frontend never re-sorts.
- Build or fill a module with the **`new-modulith-module`** skill — do not scaffold blind.
- **Don't commit unless asked.** Format the backend with `mvn spotless:apply`.

## Update rule

These files are **stable contracts, not changelogs**. Update an `AGENTS.md` only when a real
convention changes (a new event, a new aggregate, a new rule) — not on routine code edits.
