# Test wizards — creation & taking (design)

Date: 2026-06-26
Scope: `frontend/` only. **No backend changes, no new dependencies.**

## Problem

Test authoring in `dashboard/tests` is bare and fragmented: `CreateTestDialog` takes only a
title, then a separate `ManageQuestionsDialog` adds questions one at a time. There is no quick
start. Test taking (`take-attempt-dialog.tsx`) is a single long scroll of all questions, and
closing the dialog loses unsaved answers.

Goal: two step-by-step wizards on the existing shadcn/ui + react-hook-form + TanStack Query stack —
a **creation wizard** with quick content templates, and a **taking wizard** that walks one question
at a time.

## Constraints / facts

- Stack: shadcn/ui (Radix), react-hook-form, TanStack Query, Orval-generated client, sonner, lucide.
- Test model is flat: `Test { title, status, questions[] }`,
  `Question { type, prompt, points, options[] }`. 5 question types: `SINGLE_CHOICE`,
  `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_TEXT`, `CODE`.
- Creation API is two-phase: `POST /api/tests { title }` → then `POST /api/tests/{id}/questions`
  per question. **No batch create.** The wizard orchestrates `1 + N` calls on save.
- Taking API supports incremental, idempotent answer saves:
  `useStartAttempt` (POST), `useSaveAnswer` (PUT, upsert one answer), `useSubmitAttempt` (POST),
  `useGetAttempt`, `useGradeAnswer`. Backend auto-grades choice answers on submit; text answers
  (`SHORT_TEXT`/`CODE`) are graded manually by a teacher afterward.
- No timer (chosen). Templates are frontend-only, hard-coded (no backend template storage).

## Decisions

- Templates = **ready-made tests with real content** (not empty structural skeletons),
  shipped as hard-coded frontend TS presets. Only developers add them; teachers don't.
- Taking wizard = **one question per screen** with progress + Back/Next + review-before-submit.
- Creation wizard replaces `CreateTestDialog` + `ManageQuestionsDialog` **for creating** a test.
  Editing an existing test keeps using `ManageQuestionsDialog` (out of scope here).

## Architecture

All new code under `frontend/views/assessment/` unless noted.

### Shared: `WizardShell`

`components/shared/wizard-shell.tsx` — thin shadcn `Dialog` wrapper providing the common chrome:
title, a progress indicator (`step N of M` + a dot/bar), and a footer with Back / Next / Submit
buttons. Props: `steps` labels, `activeStep`, `onBack`, `onNext`, `onSubmit`, `canNext`,
`isSubmitting`, and the current step's content as children. Step state lives in each consumer as
plain `useState` — no shared state manager. Two consumers justify exactly this one shared piece;
nothing else is abstracted.

### 1. Creation wizard — `TestWizard`

`test-wizard.tsx`. Local state: `title`, `questions: DraftQuestion[]`, `publishOnCreate: boolean`.

- **Step 1 — Start.** Grid of template cards (`getTestTemplates()`) plus a "Blank test" card, and
  the title `Input`. Selecting a template sets `title` (suggested, editable) and fills `questions`
  with the template's questions. Selecting "Blank" leaves `questions` empty.
- **Step 2 — Questions.** Inline editable list: add / remove / edit / reorder. Reuses the existing
  per-question form fields from `ManageQuestionsDialog` (prompt, type, points, options with a
  `correct` flag). Reorder via drag is nice-to-have; if it complicates, ship up/down buttons.
- **Step 3 — Review.** Read-only summary (title, question count, total points, per-question
  preview) + a "publish immediately" toggle. "Create" runs the save sequence.

Save sequence (on Review → Create):
1. `createTest({ title })` → returns `testId`.
2. For each question **in order**, `createQuestion(testId, q)` — sequential to keep `sortOrder`
   deterministic (server assigns order by insertion).
3. If `publishOnCreate`, call the publish mutation.
4. Success toast, close, invalidate the tests query.

Partial-failure handling: if a question POST fails mid-sequence, surface a sonner error naming
which question failed and leave the (draft) test created so the teacher can finish it via the
existing manage-questions flow rather than losing everything.

### Templates

`templates/` directory:
- `templates/types.ts` — `TestTemplate = { id, label, description, suggestedTitle, questions:
  CreateQuestionRequest[] }`.
- `templates/index.ts` — `getTestTemplates(): TestTemplate[]`.
- One file per preset (e.g. `java-basics.ts`, `sql-basics.ts`, `html-css-intro.ts`). 3–4 to start,
  covering a mix of question types so they double as worked examples.

### 2. Taking wizard — `AttemptWizard`

`attempt-wizard.tsx`. Replaces `take-attempt-dialog.tsx`.

- On open: `useStartAttempt({ testId })` → `attempt`. Load `test.questions` for the prompts.
- Local `answers: Record<questionId, AnswerDraft>`.
- One question per step. Progress shows `current / total`.
- **On Next**: if the current answer is non-empty and changed, `useSaveAnswer` (idempotent PUT)
  persists it before advancing. This makes progress durable — closing mid-attempt no longer loses
  saved answers. Back does not re-save.
- **Final step — Review.** List every question with answered/blank status; clicking one jumps back
  to it. "Submit" flushes any unsaved answers, then `useSubmitAttempt` → render the existing
  `ResultPanel` (extracted/kept) for scores and, for teachers, manual grading.
- No timer.

### Reuse: `QuestionAnswerInput`

Extract the per-type answer inputs currently inline in `take-attempt-dialog.tsx` into
`question-answer-input.tsx`:
- `SINGLE_CHOICE` / `TRUE_FALSE` → radios (one selected option id).
- `MULTIPLE_CHOICE` → checkboxes (many option ids).
- `SHORT_TEXT` → text input.
- `CODE` → monospace textarea, `spellCheck=false`.

Used by `AttemptWizard` per step. `ResultPanel` is kept as-is and rendered after submit.

## Wiring

`assessment-view.tsx` "New Test" opens `TestWizard` instead of `CreateTestDialog`. The "take" entry
point opens `AttemptWizard` instead of `take-attempt-dialog`. Old `CreateTestDialog` and
`take-attempt-dialog` are removed once their replacements are wired; `ManageQuestionsDialog` stays
for editing existing tests.

## Out of scope (deliberately skipped)

- Batch "create test with questions" backend endpoint (add if `1 + N` latency hurts).
- Backend-stored / teacher-authored templates (frontend presets only for now).
- Edit-mode inside `TestWizard` (existing `ManageQuestionsDialog` covers editing).
- Timer / timed attempts (would need a server-side attempt deadline).

## Testing

Frontend verification per project rules: `tsc` typecheck + running `next dev` (no `next build`
while the hot stack is up). Manual smoke: create a test from a template, edit a question, create &
publish; take the test as a student through all five question types, navigate Back/Next, reopen
mid-attempt to confirm saved answers persist, submit and see results. Keep one small assertion-level
check on the save-sequence ordering helper if it is non-trivial.
