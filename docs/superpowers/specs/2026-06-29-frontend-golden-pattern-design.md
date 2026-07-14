# Frontend: golden-pattern dashboard + backend integration (design)

Date: 2026-06-29
Scope: `frontend/` — refactor the dashboard to the "golden pattern" **and** wire every backend change
from the 2026-06-28 LMS hardening (`feat/lms-hardening`, commit `6af4743`). No backend changes.
Owner: @fedotiuk-dm

> The golden pattern is the boosting dashboard
> (`/home/iddqd/IdeaProjects/BoostingJavaSpringNextjs/frontend/views/public/dashboard/` +
> its `views/AGENTS.md`). We adopt its **discipline**, not its machinery — see Decisions.

## Problem

The Codillas dashboard is ~70% of the way to the pattern: `views/<feature>/` folders exist, but each
is a **flat monolith** (a big `*-view.tsx` + inline dialogs), Orval hooks are called inline, multi-step
flows prop-drill, and there's no per-feature decomposition. Meanwhile the backend just grew a large
surface (course/group lifecycle, late flags, rubrics, assessment config, weighted course grade, file
limits) that the UI **does not reflect at all**. Two jobs, done together per feature: **decompose to
the pattern** and **wire the new API** in the same pass.

## The golden pattern (boosting) — what we adopt vs skip

| From the golden pattern | Decision | Why |
|---|---|---|
| **Per-feature folder**, one folder per dashboard page | **Adopt** | Already the shape; deepen it. |
| Route `page.tsx` = **thin delegator** (no hooks, no components) → renders one island | **Adopt** | Already thin; keep it. |
| **`<Feature>Interactive` island** ("use client") = the one client entry: holds state, mutations, builds actions, gates loading/error | **Adopt** (our `*-view.tsx` becomes this) | The orchestration seam. |
| **Presentational + 1-logic components** — each component does ONE thing, dumb, props-in | **Adopt — the hard rule** | This is the user's core ask: `1 component = 1 logic`. |
| `configs/index.ts` for all copy | **Adopt via existing i18n** — copy lives in `messages/{uk,en,de}.json` namespaces (our config layer) | Don't duplicate; i18n already is the single source of copy. |
| **`adapters/to*ViewModel.ts`** (DTO→VM, unit-tested) | **Optional** — only when there's a real transform (aggregation, merge, derived fields). Clean Orval DTOs are consumed **directly**. | User's call; ponytail. Most of our DTOs are UI-ready (generated from clean OpenAPI). |
| **`createDataAccessor` / `ViewState` / `PageShell` / context-selector data reads** (components read data via `useFoo(selectFn)`, no props) | **Skip** | We already have `components/shared/data-state.tsx` (loading/error/empty gate) and props-down. Porting boosting's accessor machinery is over-engineering and contradicts "no folders-in-folders". |
| **`hooks/use*Data` + `use*Live`** split | **Adopt lightly** — a per-feature `use-<feature>.ts` only when it removes real duplication (multi-call flows, shared filter state, derived data). Otherwise call Orval hooks inline. | Kill the wizard/flow duplication without a mandatory layer. |
| Deep subfolders (`hooks/ adapters/ components/ context/ configs/ types/`) | **Optional** — files may sit flat in the feature folder; add a `components/` subfolder only past ~5 components. | User: "папки-в-папках не обовʼязкові; файли можуть бути в одній відповідній папці." |
| Adapter unit tests (vitest) | **Skip** — repo has **no frontend test runner** (do not add one). Verify with `tsc` + running `next dev`. | Matches `2026-06-26-test-wizards` precedent + memory. |

## Target feature shape (Codillas-adapted)

```
views/<feature>/
  <feature>-view.tsx          the Interactive island: "use client", state + Orval mutations,
                              builds handlers, wraps body in <DataState> (loading/error/empty)
  use-<feature>.ts            (optional) per-feature hook: orchestrates multi-call flows /
                              shared filter state / derived view data. Wraps Orval hooks.
  <thing>-card.tsx            1-logic presentational components — props in, no fetching, no mutations.
  <thing>-dialog.tsx          a form dialog (uses components/shared/form-dialog.tsx), one per concern.
  <thing>-badge.tsx           e.g. status badge. One file = one logical piece.
  <feature>.types.ts          (optional) local view types not coming from lib/api.
  components/                 (only if the feature has many components) — same files, nested.
```
Route stays: `app/[locale]/dashboard/<feature>/page.tsx` → `<Feature>View`. Copy → i18n namespace.
Mutations stay **create-on-click + pass-as-prop + blanket invalidation** (existing rule, do not change).

## Reuse map (don't re-build)

- Loading/error/empty gate → **`components/shared/data-state.tsx`** (our `PageBoundary`/`StateMessage`).
- Form dialogs → **`components/shared/form-dialog.tsx`** (reset/close/toast already handled).
- File upload → **`components/shared/file-upload-field.tsx`**. Date/time → `date-time-field.tsx`.
- User pick → `user-picker.tsx`. Page header → `page-header.tsx`. Wizard → `wizard-shell.tsx`.
- Role gating → `useHasRole`/`useHasAnyRole`. Links → `@/i18n/navigation`. `safeHref` on rendered URLs.
- New shared atoms to ADD (used by ≥2 features): `StatusBadge` (DRAFT/PUBLISHED/ARCHIVED & group
  states), a small `ConfirmDialog` (replaces scattered `window.confirm`).

## Backend integration mapping (what to wire, per feature)

Derived from the API checklist. Each item is a UI change in the named view.

- **courses** (`views/courses/`): `status` badge on cards + detail; **Publish/Archive/Delete** buttons
  (POST `/courses/{id}/publish|archive`, DELETE); `status` filter on the list; DRAFT hidden from non-staff.
- **groups** (`views/groups/`): `status` badge; **Start/Archive/Delete** buttons; **course picker shows
  only PUBLISHED** + handle 409 on create against a non-published course; `status` filter; ARCHIVED → read-only.
- **homework** (`views/homework/`): **`late` badge** on submissions; **rubric authoring** dialog
  (criteria: label + maxPoints); **grade-with-rubric** (per-criterion inputs when `rubricId` present,
  else free-form 0–100); **late-penalty config** (`latePenaltyPctPerDay`/`maxLatePenaltyPct`) on the
  assignment form; show `effectiveScore` vs raw + `maxPoints` in the grade view.
- **assessment** (`views/assessment/`): test-config section in the creation wizard (`maxAttempts`,
  `durationMinutes`, `availableFrom/Until`, `shuffleQuestions/Options`); **attempt timer** (countdown
  from `durationMinutes`+`startedAt`, auto-submit); `attemptNumber`/window messaging + 409 handling;
  partial-credit shown in results. (Server sends pre-shuffled order — never re-shuffle client-side.)
- **gradebook** (`views/gradebook/`): **course-grade card** (`courseGrade.percent` + Σawarded/Σmax) and
  **per-type breakdown** (`byType`: HOMEWORK vs TEST); add a final-grade column to the group table.
- **files** (`views/files/`): client-side **25 MB + content-type** validation before upload; friendly
  413 handling.
- **notification/chat/people/me/admin**: no new API; decompose to the pattern only.

## Execution order (each = decompose + integrate in one pass)

1. **Foundation** — `pnpm generate:api` (done); fix any tsc breakage from changed/required DTO fields;
   add shared `StatusBadge` + `ConfirmDialog`.
2. **courses** + course-detail (lifecycle — highest visible impact, cleanest decomposition example).
3. **groups/enrollment** (lifecycle + published-course constraint).
4. **homework** (late + rubric + late-penalty + grade view).
5. **assessment** (wizard decomposition + `use-assessment` flow hooks + test config + timer).
6. **gradebook** (course-grade card + breakdown).
7. **files** (validation) + the light read-only views (notifications/chat/people/me/admin) to the pattern.

## Verification (per feature)

- `pnpm type-check` (tsc --noEmit) green; `pnpm lint` (biome) clean. (Note: the script is `type-check`,
  not `typecheck` — the frontend `AGENTS.md` command list is slightly off.)
- Manual smoke against a running `pnpm dev` — **never `next build` while the dev stack is up** (memory).
- i18n: every new string keyed in `uk`/`en`/`de`. No hardcoded copy.
- React Compiler is on → no hand `useMemo/useCallback/memo`; keep components pure.

## Out of scope (YAGNI)

- Porting boosting's `createDataAccessor`/`PageShell`/context-selector apparatus or its page-builder skills.
- A frontend test runner / adapter unit tests.
- The per-tag invalidation rules map (blanket invalidation stays until a measured problem).
- New visual redesign — match the existing Codillas look; this is structure + wiring, not a re-skin.

## Decisions log

| Decision | Choice | Rationale |
|---|---|---|
| Machinery vs discipline | Adopt the pattern's discipline; reuse our `data-state`/`form-dialog`; skip boosting's accessor/shell machinery | Simpler, ponytail, matches "no folders-in-folders" |
| Data into components | **Props-down** (not context-selector reads) | We have no `createDataAccessor`; props are simpler and already used |
| Adapters | Optional, only on real transforms | User's call; clean Orval DTOs go direct |
| Copy/config | i18n namespaces are the config layer | Don't duplicate copy into `configs/` |
| Tests | None (tsc + `next dev` smoke) | No runner in repo; don't add one |
| Branch | Continue on `feat/lms-hardening`, frontend as its own commits (not squashed into the backend commit) | Keeps the backend commit clean; frontend history readable |
