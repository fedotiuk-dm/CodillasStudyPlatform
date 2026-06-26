# Test Wizards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bare test-creation dialogs with a 3-step creation wizard (with quick content templates) and replace the single-scroll attempt dialog with a one-question-per-step taking wizard.

**Architecture:** Two new wizard components on the existing shadcn/ui + react-hook-form + TanStack Query stack, sharing one thin `WizardShell` (Dialog + progress + Back/Next footer). Templates are hard-coded frontend TS presets. The creation wizard orchestrates the existing `1 + N` create-test-then-add-questions API calls; the taking wizard saves each answer on "Next" via the idempotent `useSaveAnswer` PUT. **No backend changes, no new dependencies.**

**Tech Stack:** Next.js (App Router), TypeScript, shadcn/ui (Radix), react-hook-form, TanStack Query (Orval-generated client), next-intl, sonner, lucide-react, Tailwind.

## Global Constraints

- Frontend only. **No backend changes. No new npm dependencies** (no `pnpm add`).
- Verify with `pnpm exec tsc --noEmit` and a running `next dev` — **never** `next build` while the dev stack is up.
- This repo has **no frontend test runner**. "Tests" here = a passing `tsc --noEmit` plus the explicit manual smoke check named in each task. Do not add a test framework.
- All user-facing strings go through `useTranslations("tests")`; add new keys to `frontend/messages/{uk,en,de}.json` under the existing `"tests"` namespace.
- Generated API client under `frontend/lib/api/assessment/` is **read-only** (Orval). Import from it; never edit it.
- Question types (`QuestionType` enum): `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `TRUE_FALSE`, `SHORT_TEXT`, `CODE`. Only `SINGLE_CHOICE` / `MULTIPLE_CHOICE` / `TRUE_FALSE` carry options.
- Commit after each task with a `feat(frontend):` / `refactor(frontend):` message. Do not push.
- All new components start with `"use client";`.

## File Structure

```
frontend/
  components/shared/wizard-shell.tsx          NEW  — shared Dialog + progress + footer nav
  views/assessment/
    templates/
      types.ts                                NEW  — TestTemplate type + helper
      index.ts                                NEW  — getTestTemplates()
      java-basics.ts                          NEW  — preset
      sql-basics.ts                           NEW  — preset
      html-css-intro.ts                       NEW  — preset
    question-list-editor.tsx                  NEW  — draft-question add/edit/remove/reorder (creation step 2)
    test-wizard.tsx                           NEW  — 3-step creation wizard
    question-answer-input.tsx                 NEW  — per-type answer input (extracted from take-attempt-dialog)
    result-panel.tsx                          NEW  — graded result view (extracted from take-attempt-dialog)
    attempt-wizard.tsx                         NEW  — one-question-per-step taking wizard
    assessment-view.tsx                        MOD  — wire the two wizards, drop old dialogs
    create-test-dialog.tsx                     DEL  (replaced by test-wizard)
    take-attempt-dialog.tsx                    DEL  (replaced by attempt-wizard)
    manage-questions-dialog.tsx                UNCHANGED (still used for editing existing draft tests)
  messages/{uk,en,de}.json                     MOD  — new "tests" keys
```

**Key API facts (from `frontend/lib/api/assessment/assessment/assessment.ts`):**
- `useCreateTest()` → `mutateAsync({ data: CreateTestRequest })` resolves to `TestResponse` (`.id`).
- `useAddQuestion()` → `mutateAsync({ testId, data: CreateQuestionRequest })`.
- `usePublishTest()` → `mutateAsync({ testId })`.
- `useStartAttempt()` → `mutate({ testId }, { onSuccess })` resolves to `AttemptResponse`.
- `useSaveAnswer()` → `mutateAsync({ attemptId, data: SaveAnswerRequest })` (idempotent PUT).
- `useSubmitAttempt()` → `mutateAsync({ attemptId })` resolves to graded `AttemptResponse`.
- `useGetTest(testId, { query: { enabled } })` → `TestResponse` with `.questions`.
- `CreateQuestionRequest = { type, prompt, points, options?: { text, correct }[] }`.
- `SaveAnswerRequest = { questionId, selectedOptionIds?, text? }`.
- A global MutationCache already surfaces error toasts and list invalidation (the current `CreateTestDialog` relies on it) — do not add manual query invalidation unless a smoke test shows the list is stale.

---

### Task 1: Shared `WizardShell` + i18n keys

**Files:**
- Create: `frontend/components/shared/wizard-shell.tsx`
- Modify: `frontend/messages/uk.json`, `frontend/messages/en.json`, `frontend/messages/de.json` (under `"tests"`)

**Interfaces:**
- Produces: `WizardShell` component:
  ```ts
  function WizardShell(props: {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    title: string;
    description?: string;
    stepCount: number;       // total steps incl. review
    activeStep: number;      // 0-based
    onBack: () => void;
    onNext: () => void;      // called on the Next button (non-last steps)
    onSubmit: () => void;    // called on the final-step primary button
    canAdvance?: boolean;    // disables Next/Submit
    isSubmitting?: boolean;
    nextLabel: string;
    backLabel: string;
    submitLabel: string;
    submittingLabel: string;
    children: React.ReactNode;
  }): JSX.Element
  ```

- [ ] **Step 1: Add i18n keys.** In each of `frontend/messages/uk.json`, `en.json`, `de.json`, inside the existing `"tests"` object, add these keys (values below are uk / en / de):

  ```
  "wizardBack"        : "Назад"                    / "Back"               / "Zurück"
  "wizardNext"        : "Далі"                     / "Next"               / "Weiter"
  "step"              : "Крок {current} / {total}" / "Step {current} / {total}" / "Schritt {current} / {total}"
  "createTest"        : "Створити тест"            / "Create test"        / "Test erstellen"
  "creating"          : "Створення…"               / "Creating…"          / "Erstellen…"
  "stepStart"         : "Старт"                    / "Start"              / "Start"
  "stepQuestions"     : "Питання"                  / "Questions"          / "Fragen"
  "stepReview"        : "Огляд"                    / "Review"             / "Überblick"
  "blankTest"         : "Порожній тест"            / "Blank test"         / "Leerer Test"
  "useTemplate"       : "Почати з шаблону"         / "Start from a template" / "Mit Vorlage starten"
  "templateQuestions" : "{count} питань"           / "{count} questions"  / "{count} Fragen"
  "publishNow"        : "Опублікувати одразу"      / "Publish immediately" / "Sofort veröffentlichen"
  "totalPoints"       : "Усього балів"             / "Total points"       / "Gesamtpunkte"
  "noQuestions"       : "Додайте хоча б одне питання." / "Add at least one question." / "Fügen Sie mindestens eine Frage hinzu."
  "questionFailed"    : "Не вдалося додати питання №{number}. Тест збережено як чернетку." / "Failed to add question #{number}. The test was saved as a draft." / "Frage #{number} konnte nicht hinzugefügt werden. Der Test wurde als Entwurf gespeichert."
  "removeQuestion"    : "Видалити питання"         / "Remove question"    / "Frage entfernen"
  "moveUp"            : "Вгору"                     / "Move up"            / "Nach oben"
  "moveDown"          : "Вниз"                      / "Move down"          / "Nach unten"
  "questionNumber"    : "Питання {number}"         / "Question {number}"  / "Frage {number}"
  "answered"          : "Відповідь надано"          / "Answered"           / "Beantwortet"
  "unanswered"        : "Без відповіді"             / "Not answered"       / "Nicht beantwortet"
  "reviewAnswers"     : "Перевірте відповіді перед надсиланням." / "Review your answers before submitting." / "Überprüfen Sie Ihre Antworten vor dem Absenden."
  "questionProgress"  : "Питання {current} / {total}" / "Question {current} / {total}" / "Frage {current} / {total}"
  ```

- [ ] **Step 2: Write `WizardShell`.**

  ```tsx
  "use client";

  import type { ReactNode } from "react";

  import { Button } from "@/components/ui/button";
  import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
  } from "@/components/ui/dialog";
  import { cn } from "@/lib/utils";

  export function WizardShell({
    open,
    onOpenChange,
    title,
    description,
    stepCount,
    activeStep,
    onBack,
    onNext,
    onSubmit,
    canAdvance = true,
    isSubmitting = false,
    nextLabel,
    backLabel,
    submitLabel,
    submittingLabel,
    children,
  }: {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    title: string;
    description?: string;
    stepCount: number;
    activeStep: number;
    onBack: () => void;
    onNext: () => void;
    onSubmit: () => void;
    canAdvance?: boolean;
    isSubmitting?: boolean;
    nextLabel: string;
    backLabel: string;
    submitLabel: string;
    submittingLabel: string;
    children: ReactNode;
  }) {
    const isLast = activeStep === stepCount - 1;

    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            {description && <DialogDescription>{description}</DialogDescription>}
          </DialogHeader>

          <div className="flex items-center gap-1.5" aria-hidden>
            {Array.from({ length: stepCount }).map((_, i) => (
              <span
                // biome-ignore lint/suspicious/noArrayIndexKey: fixed-length progress dots
                key={i}
                className={cn(
                  "h-1.5 flex-1 rounded-full",
                  i <= activeStep ? "bg-primary" : "bg-muted",
                )}
              />
            ))}
          </div>

          <div className="grid gap-4 py-2">{children}</div>

          <DialogFooter className="sm:justify-between">
            <Button type="button" variant="outline" onClick={onBack} disabled={activeStep === 0}>
              {backLabel}
            </Button>
            {isLast ? (
              <Button type="button" onClick={onSubmit} disabled={!canAdvance || isSubmitting}>
                {isSubmitting ? submittingLabel : submitLabel}
              </Button>
            ) : (
              <Button type="button" onClick={onNext} disabled={!canAdvance}>
                {nextLabel}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }
  ```

- [ ] **Step 3: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no new errors. (`cn` lives at `@/lib/utils` — confirm the import resolves; it is used across `components/ui`.)

- [ ] **Step 4: Commit.**
  ```bash
  git add frontend/components/shared/wizard-shell.tsx frontend/messages
  git commit -m "feat(frontend): shared WizardShell + i18n keys for test wizards"
  ```

---

### Task 2: Test templates (frontend presets)

**Files:**
- Create: `frontend/views/assessment/templates/types.ts`
- Create: `frontend/views/assessment/templates/java-basics.ts`
- Create: `frontend/views/assessment/templates/sql-basics.ts`
- Create: `frontend/views/assessment/templates/html-css-intro.ts`
- Create: `frontend/views/assessment/templates/index.ts`

**Interfaces:**
- Produces:
  ```ts
  type TestTemplate = {
    id: string;
    label: string;
    description: string;
    suggestedTitle: string;
    questions: CreateQuestionRequest[];
  };
  function getTestTemplates(): TestTemplate[];
  ```

- [ ] **Step 1: `types.ts`.**
  ```ts
  import type { CreateQuestionRequest } from "@/lib/api/assessment/model";

  export type TestTemplate = {
    id: string;
    label: string;
    description: string;
    suggestedTitle: string;
    questions: CreateQuestionRequest[];
  };
  ```

- [ ] **Step 2: `java-basics.ts`.**
  ```ts
  import { QuestionType } from "@/lib/api/assessment/model";

  import type { TestTemplate } from "./types";

  export const javaBasics: TestTemplate = {
    id: "java-basics",
    label: "Java основи",
    description: "10 питань: типи, JVM, синтаксис.",
    suggestedTitle: "Java основи",
    questions: [
      {
        type: QuestionType.SINGLE_CHOICE,
        prompt: "Що таке JVM?",
        points: 1,
        options: [
          { text: "Віртуальна машина, що виконує байткод", correct: true },
          { text: "Компілятор Java у машинний код", correct: false },
          { text: "Менеджер пакетів", correct: false },
        ],
      },
      {
        type: QuestionType.TRUE_FALSE,
        prompt: "Примітив int може містити значення null.",
        points: 1,
        options: [
          { text: "Правда", correct: false },
          { text: "Хиба", correct: true },
        ],
      },
      {
        type: QuestionType.MULTIPLE_CHOICE,
        prompt: "Які з наведеного — примітивні типи Java?",
        points: 2,
        options: [
          { text: "int", correct: true },
          { text: "boolean", correct: true },
          { text: "String", correct: false },
          { text: "double", correct: true },
        ],
      },
      {
        type: QuestionType.SHORT_TEXT,
        prompt: "Яким ключовим словом оголошують константу?",
        points: 1,
      },
      {
        type: QuestionType.CODE,
        prompt: "Напишіть метод, що повертає суму двох int.",
        points: 3,
      },
    ],
  };
  ```

- [ ] **Step 3: `sql-basics.ts`.**
  ```ts
  import { QuestionType } from "@/lib/api/assessment/model";

  import type { TestTemplate } from "./types";

  export const sqlBasics: TestTemplate = {
    id: "sql-basics",
    label: "SQL базовий",
    description: "Вибірки, фільтри, агрегати.",
    suggestedTitle: "SQL базовий",
    questions: [
      {
        type: QuestionType.SINGLE_CHOICE,
        prompt: "Який оператор вибирає всі рядки таблиці?",
        points: 1,
        options: [
          { text: "SELECT * FROM t", correct: true },
          { text: "GET * FROM t", correct: false },
          { text: "FETCH t", correct: false },
        ],
      },
      {
        type: QuestionType.TRUE_FALSE,
        prompt: "WHERE фільтрує рядки до групування.",
        points: 1,
        options: [
          { text: "Правда", correct: true },
          { text: "Хиба", correct: false },
        ],
      },
      {
        type: QuestionType.SHORT_TEXT,
        prompt: "Яка функція рахує кількість рядків?",
        points: 1,
      },
    ],
  };
  ```

- [ ] **Step 4: `html-css-intro.ts`.**
  ```ts
  import { QuestionType } from "@/lib/api/assessment/model";

  import type { TestTemplate } from "./types";

  export const htmlCssIntro: TestTemplate = {
    id: "html-css-intro",
    label: "HTML/CSS вступ",
    description: "Теги, селектори, блокова модель.",
    suggestedTitle: "HTML/CSS вступ",
    questions: [
      {
        type: QuestionType.SINGLE_CHOICE,
        prompt: "Який тег створює гіперпосилання?",
        points: 1,
        options: [
          { text: "<a>", correct: true },
          { text: "<link>", correct: false },
          { text: "<href>", correct: false },
        ],
      },
      {
        type: QuestionType.MULTIPLE_CHOICE,
        prompt: "Які з наведеного — валідні CSS-селектори?",
        points: 2,
        options: [
          { text: ".class", correct: true },
          { text: "#id", correct: true },
          { text: "$name", correct: false },
        ],
      },
      {
        type: QuestionType.SHORT_TEXT,
        prompt: "Яка властивість задає зовнішній відступ?",
        points: 1,
      },
    ],
  };
  ```

- [ ] **Step 5: `index.ts`.**
  ```ts
  import { htmlCssIntro } from "./html-css-intro";
  import { javaBasics } from "./java-basics";
  import { sqlBasics } from "./sql-basics";
  import type { TestTemplate } from "./types";

  export type { TestTemplate };

  export function getTestTemplates(): TestTemplate[] {
    return [javaBasics, sqlBasics, htmlCssIntro];
  }
  ```

- [ ] **Step 6: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no errors (every `options` array on choice/true-false questions is present; `SHORT_TEXT`/`CODE` omit options).

- [ ] **Step 7: Commit.**
  ```bash
  git add frontend/views/assessment/templates
  git commit -m "feat(frontend): seed test templates (java/sql/html-css)"
  ```

---

### Task 3: `QuestionListEditor` (creation step 2)

**Files:**
- Create: `frontend/views/assessment/question-list-editor.tsx`

**Interfaces:**
- Consumes: `CreateQuestionRequest`, `QuestionType` from `@/lib/api/assessment/model`.
- Produces:
  ```ts
  type DraftQuestion = CreateQuestionRequest & { _key: string };
  function newDraftQuestion(): DraftQuestion;   // SINGLE_CHOICE, 2 blank options
  function QuestionListEditor(props: {
    value: DraftQuestion[];
    onChange: (next: DraftQuestion[]) => void;
  }): JSX.Element
  ```
  `_key` is a stable client-side React key (never sent to the API; strip it before POST).

- [ ] **Step 1: Write the component.**
  ```tsx
  "use client";

  import { Trash2 } from "lucide-react";
  import { useTranslations } from "next-intl";

  import { Button } from "@/components/ui/button";
  import { Checkbox } from "@/components/ui/checkbox";
  import { Input } from "@/components/ui/input";
  import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
  } from "@/components/ui/select";
  import { Separator } from "@/components/ui/separator";
  import { type CreateQuestionRequest, QuestionType } from "@/lib/api/assessment/model";

  export type DraftQuestion = CreateQuestionRequest & { _key: string };

  const NEEDS_OPTIONS: QuestionType[] = [
    QuestionType.SINGLE_CHOICE,
    QuestionType.MULTIPLE_CHOICE,
    QuestionType.TRUE_FALSE,
  ];

  export function newDraftQuestion(): DraftQuestion {
    return {
      _key: crypto.randomUUID(),
      type: QuestionType.SINGLE_CHOICE,
      prompt: "",
      points: 1,
      options: [
        { text: "", correct: true },
        { text: "", correct: false },
      ],
    };
  }

  export function QuestionListEditor({
    value,
    onChange,
  }: {
    value: DraftQuestion[];
    onChange: (next: DraftQuestion[]) => void;
  }) {
    const t = useTranslations("tests");

    function patch(index: number, fields: Partial<DraftQuestion>) {
      onChange(value.map((q, i) => (i === index ? { ...q, ...fields } : q)));
    }

    function patchOption(qi: number, oi: number, fields: Partial<{ text: string; correct: boolean }>) {
      const q = value[qi];
      const options = (q.options ?? []).map((o, i) => (i === oi ? { ...o, ...fields } : o));
      patch(qi, { options });
    }

    function move(index: number, delta: number) {
      const next = [...value];
      const target = index + delta;
      if (target < 0 || target >= next.length) return;
      [next[index], next[target]] = [next[target], next[index]];
      onChange(next);
    }

    return (
      <div className="grid gap-4">
        {value.map((q, qi) => {
          const needsOptions = NEEDS_OPTIONS.includes(q.type);
          return (
            <div key={q._key} className="grid gap-3 rounded-md border p-3">
              <div className="flex items-center justify-between">
                <span className="font-medium text-sm">{t("questionNumber", { number: qi + 1 })}</span>
                <div className="flex items-center gap-1">
                  <Button type="button" variant="ghost" size="sm" onClick={() => move(qi, -1)} disabled={qi === 0}>
                    {t("moveUp")}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => move(qi, 1)}
                    disabled={qi === value.length - 1}
                  >
                    {t("moveDown")}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={t("removeQuestion")}
                    onClick={() => onChange(value.filter((_, i) => i !== qi))}
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
              </div>

              <Select
                value={q.type}
                onValueChange={(v) => {
                  const type = v as QuestionType;
                  const becomesOptions = NEEDS_OPTIONS.includes(type);
                  patch(qi, {
                    type,
                    options: becomesOptions
                      ? q.options && q.options.length
                        ? q.options
                        : [
                            { text: "", correct: true },
                            { text: "", correct: false },
                          ]
                      : undefined,
                  });
                }}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(QuestionType).map((qt) => (
                    <SelectItem key={qt} value={qt}>
                      {qt}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Input
                placeholder={t("promptPlaceholder")}
                value={q.prompt}
                onChange={(e) => patch(qi, { prompt: e.target.value })}
              />

              <Input
                type="number"
                min={1}
                value={q.points}
                onChange={(e) => patch(qi, { points: Number(e.target.value) })}
              />

              {needsOptions && (
                <div className="grid gap-2">
                  {(q.options ?? []).map((o, oi) => (
                    // biome-ignore lint/suspicious/noArrayIndexKey: options have no stable id pre-save
                    <div key={oi} className="flex items-center gap-2">
                      <Checkbox
                        checked={o.correct}
                        onCheckedChange={(c) => patchOption(qi, oi, { correct: c === true })}
                      />
                      <Input
                        className="flex-1"
                        placeholder={t("optionPlaceholder", { number: oi + 1 })}
                        value={o.text}
                        onChange={(e) => patchOption(qi, oi, { text: e.target.value })}
                      />
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() =>
                          patch(qi, { options: (q.options ?? []).filter((_, i) => i !== oi) })
                        }
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  ))}
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="justify-self-start"
                    onClick={() => patch(qi, { options: [...(q.options ?? []), { text: "", correct: false }] })}
                  >
                    {t("addOption")}
                  </Button>
                </div>
              )}
            </div>
          );
        })}

        <Separator />
        <Button
          type="button"
          variant="outline"
          className="justify-self-start"
          onClick={() => onChange([...value, newDraftQuestion()])}
        >
          {t("addQuestion")}
        </Button>
      </div>
    );
  }
  ```

- [ ] **Step 2: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no errors.

- [ ] **Step 3: Commit.**
  ```bash
  git add frontend/views/assessment/question-list-editor.tsx
  git commit -m "feat(frontend): draft question list editor for the creation wizard"
  ```

---

### Task 4: `TestWizard` + wire creation flow

**Files:**
- Create: `frontend/views/assessment/test-wizard.tsx`
- Modify: `frontend/views/assessment/assessment-view.tsx`
- Delete: `frontend/views/assessment/create-test-dialog.tsx`

**Interfaces:**
- Consumes: `WizardShell` (Task 1), `getTestTemplates`/`TestTemplate` (Task 2), `QuestionListEditor`/`DraftQuestion`/`newDraftQuestion` (Task 3), `useCreateTest`/`useAddQuestion`/`usePublishTest`.
- Produces:
  ```ts
  function TestWizard(props: { open: boolean; onOpenChange: (open: boolean) => void }): JSX.Element
  ```

- [ ] **Step 1: Write `TestWizard`.**
  ```tsx
  "use client";

  import { useTranslations } from "next-intl";
  import { useState } from "react";
  import { toast } from "sonner";

  import { WizardShell } from "@/components/shared/wizard-shell";
  import { Card, CardContent } from "@/components/ui/card";
  import { Checkbox } from "@/components/ui/checkbox";
  import { Input } from "@/components/ui/input";
  import { Label } from "@/components/ui/label";
  import {
    useAddQuestion,
    useCreateTest,
    usePublishTest,
  } from "@/lib/api/assessment/assessment/assessment";
  import { type DraftQuestion, QuestionListEditor } from "./question-list-editor";
  import { getTestTemplates, type TestTemplate } from "./templates";

  type Step = 0 | 1 | 2;

  function toRequests(questions: DraftQuestion[]) {
    return questions.map(({ _key, ...q }) => ({
      ...q,
      options: q.options?.filter((o) => o.text.trim()),
    }));
  }

  export function TestWizard({
    open,
    onOpenChange,
  }: {
    open: boolean;
    onOpenChange: (open: boolean) => void;
  }) {
    const t = useTranslations("tests");
    const createTest = useCreateTest();
    const addQuestion = useAddQuestion();
    const publish = usePublishTest();

    const [step, setStep] = useState<Step>(0);
    const [title, setTitle] = useState("");
    const [questions, setQuestions] = useState<DraftQuestion[]>([]);
    const [publishNow, setPublishNow] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    function reset() {
      setStep(0);
      setTitle("");
      setQuestions([]);
      setPublishNow(false);
      setSubmitting(false);
    }

    function applyTemplate(tpl: TestTemplate | null) {
      if (!tpl) {
        setQuestions([]);
        setStep(1);
        return;
      }
      if (!title.trim()) setTitle(tpl.suggestedTitle);
      setQuestions(tpl.questions.map((q) => ({ ...q, _key: crypto.randomUUID() })));
      setStep(1);
    }

    const totalPoints = questions.reduce((sum, q) => sum + (Number(q.points) || 0), 0);
    const canAdvance =
      step === 0 ? title.trim().length > 0 : step === 1 ? questions.length > 0 : true;

    async function onCreate() {
      if (questions.length === 0) {
        toast.error(t("noQuestions"));
        return;
      }
      setSubmitting(true);
      try {
        const test = await createTest.mutateAsync({ data: { title: title.trim() } });
        const requests = toRequests(questions);
        for (let i = 0; i < requests.length; i++) {
          try {
            await addQuestion.mutateAsync({ testId: test.id, data: requests[i] });
          } catch {
            toast.error(t("questionFailed", { number: i + 1 }));
            onOpenChange(false);
            reset();
            return;
          }
        }
        if (publishNow) await publish.mutateAsync({ testId: test.id });
        toast.success(t("created"));
        onOpenChange(false);
        reset();
      } finally {
        setSubmitting(false);
      }
    }

    function handleOpenChange(next: boolean) {
      if (!next) reset();
      onOpenChange(next);
    }

    return (
      <WizardShell
        open={open}
        onOpenChange={handleOpenChange}
        title={t("newTest")}
        description={t("dialogDescription")}
        stepCount={3}
        activeStep={step}
        canAdvance={canAdvance}
        isSubmitting={submitting}
        onBack={() => setStep((s) => Math.max(0, s - 1) as Step)}
        onNext={() => setStep((s) => Math.min(2, s + 1) as Step)}
        onSubmit={onCreate}
        backLabel={t("wizardBack")}
        nextLabel={t("wizardNext")}
        submitLabel={t("createTest")}
        submittingLabel={t("creating")}
      >
        {step === 0 && (
          <div className="grid gap-4">
            <div className="grid gap-1.5">
              <Label htmlFor="test-title">{t("titleLabel")}</Label>
              <Input
                id="test-title"
                placeholder={t("titlePlaceholder")}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>
            <p className="text-muted-foreground text-sm">{t("useTemplate")}</p>
            <div className="grid gap-2 sm:grid-cols-2">
              {getTestTemplates().map((tpl) => (
                <Card
                  key={tpl.id}
                  className="cursor-pointer transition-colors hover:border-primary"
                  onClick={() => applyTemplate(tpl)}
                >
                  <CardContent className="grid gap-1 p-4">
                    <span className="font-medium">{tpl.label}</span>
                    <span className="text-muted-foreground text-xs">{tpl.description}</span>
                    <span className="text-muted-foreground text-xs">
                      {t("templateQuestions", { count: tpl.questions.length })}
                    </span>
                  </CardContent>
                </Card>
              ))}
              <Card
                className="cursor-pointer transition-colors hover:border-primary"
                onClick={() => applyTemplate(null)}
              >
                <CardContent className="flex h-full items-center p-4 font-medium">
                  {t("blankTest")}
                </CardContent>
              </Card>
            </div>
          </div>
        )}

        {step === 1 && <QuestionListEditor value={questions} onChange={setQuestions} />}

        {step === 2 && (
          <div className="grid gap-3">
            <div className="flex items-center justify-between">
              <span className="font-medium">{title}</span>
              <span className="text-muted-foreground text-sm">
                {t("templateQuestions", { count: questions.length })} · {t("totalPoints")}: {totalPoints}
              </span>
            </div>
            <ol className="grid gap-1.5 text-sm">
              {questions.map((q, i) => (
                <li key={q._key} className="flex gap-2">
                  <span className="text-muted-foreground">{i + 1}.</span>
                  <span className="flex-1">{q.prompt || "—"}</span>
                  <span className="text-muted-foreground">{q.type}</span>
                </li>
              ))}
            </ol>
            <label className="flex items-center gap-2 text-sm">
              <Checkbox checked={publishNow} onCheckedChange={(c) => setPublishNow(c === true)} />
              {t("publishNow")}
            </label>
          </div>
        )}
      </WizardShell>
    );
  }
  ```

  Note: `Label` is `@/components/ui/label` — confirm it exists (`ls frontend/components/ui/label.tsx`); if not, replace the `<Label>` with a plain `<span className="text-sm font-medium">`.

- [ ] **Step 2: Wire into `assessment-view.tsx`.** Replace the `CreateTestDialog` import and usage:
  - Change import line `import { CreateTestDialog } from "./create-test-dialog";` → `import { TestWizard } from "./test-wizard";`
  - Change `<CreateTestDialog open={createOpen} onOpenChange={setCreateOpen} />` → `<TestWizard open={createOpen} onOpenChange={setCreateOpen} />`

- [ ] **Step 3: Delete the old dialog.**
  ```bash
  rm frontend/views/assessment/create-test-dialog.tsx
  ```

- [ ] **Step 4: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no errors, no dangling reference to `create-test-dialog`.

- [ ] **Step 5: Manual smoke.** With `next dev` running, open `dashboard/tests` as a TEACHER/ADMIN → "Новий тест":
  - Step Start: pick "Java основи" → questions prefill, title suggested; or "Порожній тест".
  - Step Questions: add/edit/remove/reorder a question.
  - Step Review: totals correct; toggle "Опублікувати одразу".
  - Create → toast, dialog closes, the new test appears in the list (DRAFT, or PUBLISHED if toggled).

- [ ] **Step 6: Commit.**
  ```bash
  git add frontend/views/assessment/test-wizard.tsx frontend/views/assessment/assessment-view.tsx
  git add -A frontend/views/assessment/create-test-dialog.tsx
  git commit -m "feat(frontend): 3-step test creation wizard with templates; drop create-test-dialog"
  ```

---

### Task 5: Extract `QuestionAnswerInput` + `ResultPanel`

Refactor only — pull the two inner components out of `take-attempt-dialog.tsx` into their own files so the taking wizard can reuse them. No behavior change.

**Files:**
- Create: `frontend/views/assessment/question-answer-input.tsx`
- Create: `frontend/views/assessment/result-panel.tsx`
- Modify: `frontend/views/assessment/take-attempt-dialog.tsx` (import the extracted pieces)

**Interfaces:**
- Produces:
  ```ts
  type AnswerDraft = { selectedOptionIds: string[]; text: string };
  function QuestionAnswerInput(props: {
    index: number;
    question: QuestionResponse;
    draft?: AnswerDraft;
    onChange: (patch: Partial<AnswerDraft>) => void;
  }): JSX.Element
  function ResultPanel(props: {
    attempt: AttemptResponse;
    test: QuestionResponse[];
    canManage: boolean;
  }): JSX.Element
  ```

- [ ] **Step 1: Create `question-answer-input.tsx`** with the exact body of the current `QuestionInput` function from `take-attempt-dialog.tsx` (lines 145–217), renamed and exported, plus the `AnswerDraft` type:
  ```tsx
  "use client";

  import { useTranslations } from "next-intl";

  import { Checkbox } from "@/components/ui/checkbox";
  import { Input } from "@/components/ui/input";
  import { Textarea } from "@/components/ui/textarea";
  import type { QuestionResponse } from "@/lib/api/assessment/model";

  export type AnswerDraft = { selectedOptionIds: string[]; text: string };

  export function QuestionAnswerInput({
    index,
    question: q,
    draft,
    onChange,
  }: {
    index: number;
    question: QuestionResponse;
    draft?: AnswerDraft;
    onChange: (patch: Partial<AnswerDraft>) => void;
  }) {
    const t = useTranslations("tests");
    const selected = draft?.selectedOptionIds ?? [];
    const single = q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE";

    return (
      <div className="grid gap-2 rounded-md border p-3">
        <p className="font-medium text-sm">
          {index + 1}. {q.prompt}{" "}
          <span className="font-normal text-muted-foreground">
            ({q.points} {t("pt")})
          </span>
        </p>
        {q.type === "CODE" ? (
          <Textarea
            className="font-mono text-sm"
            rows={8}
            spellCheck={false}
            placeholder={t("codePlaceholder")}
            value={draft?.text ?? ""}
            onChange={(e) => onChange({ text: e.target.value })}
          />
        ) : q.type === "SHORT_TEXT" ? (
          <Input
            placeholder={t("answerPlaceholder")}
            value={draft?.text ?? ""}
            onChange={(e) => onChange({ text: e.target.value })}
          />
        ) : (
          <div className="grid gap-1.5">
            {q.options.map((o) =>
              single ? (
                <label key={o.id} className="flex items-center gap-2 text-sm">
                  <input
                    type="radio"
                    name={`q-${q.id}`}
                    checked={selected[0] === o.id}
                    onChange={() => onChange({ selectedOptionIds: [o.id] })}
                  />
                  {o.text}
                </label>
              ) : (
                // biome-ignore lint/a11y/noLabelWithoutControl: Radix Checkbox is the control
                <label key={o.id} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={selected.includes(o.id)}
                    onCheckedChange={(c) =>
                      onChange({
                        selectedOptionIds: c
                          ? [...selected, o.id]
                          : selected.filter((id) => id !== o.id),
                      })
                    }
                  />
                  {o.text}
                </label>
              ),
            )}
          </div>
        )}
      </div>
    );
  }
  ```

- [ ] **Step 2: Create `result-panel.tsx`** with the exact body of the current `ResultPanel` (lines 219–283), exported, with its own imports:
  ```tsx
  "use client";

  import { useTranslations } from "next-intl";
  import { useState } from "react";
  import { toast } from "sonner";

  import { Badge } from "@/components/ui/badge";
  import { Button } from "@/components/ui/button";
  import { Input } from "@/components/ui/input";
  import { Separator } from "@/components/ui/separator";
  import { useGradeAnswer } from "@/lib/api/assessment/assessment/assessment";
  import type { AttemptResponse, QuestionResponse } from "@/lib/api/assessment/model";

  export function ResultPanel({
    attempt,
    test,
    canManage,
  }: {
    attempt: AttemptResponse;
    test: QuestionResponse[];
    canManage: boolean;
  }) {
    const t = useTranslations("tests");
    const grade = useGradeAnswer();
    const [scores, setScores] = useState<Record<string, string>>({});
    const promptFor = (questionId: string) => test.find((q) => q.id === questionId)?.prompt ?? "—";

    return (
      <div className="grid gap-3">
        <div className="flex items-center gap-2">
          <Badge variant={attempt.status === "GRADED" ? "default" : "secondary"}>
            {attempt.status}
          </Badge>
          <span className="font-medium">
            {t("score")}: {attempt.score}
          </span>
        </div>
        <Separator />
        {attempt.answers.map((ans) => (
          <div key={ans.id} className="grid gap-1 rounded-md border p-3 text-sm">
            <p className="font-medium">{promptFor(ans.questionId)}</p>
            {ans.text && <p className="text-muted-foreground">“{ans.text}”</p>}
            <p className="text-muted-foreground">
              {t("awarded")}: {ans.awardedPoints ?? t("pending")}
            </p>
            {canManage && ans.awardedPoints == null && (
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  min={0}
                  className="w-24"
                  placeholder={t("pointsPlaceholder")}
                  value={scores[ans.id] ?? ""}
                  onChange={(e) => setScores((p) => ({ ...p, [ans.id]: e.target.value }))}
                />
                <Button
                  size="sm"
                  disabled={!scores[ans.id]}
                  onClick={() =>
                    grade.mutate(
                      {
                        attemptId: attempt.id,
                        answerId: ans.id,
                        data: { awardedPoints: Number(scores[ans.id]) },
                      },
                      { onSuccess: () => toast.success(t("graded")) },
                    )
                  }
                >
                  {t("grade")}
                </Button>
              </div>
            )}
          </div>
        ))}
      </div>
    );
  }
  ```

- [ ] **Step 3: Update `take-attempt-dialog.tsx`** to use the extracted components: remove the now-duplicated `QuestionInput` and `ResultPanel` function definitions (lines 145–283) and the imports only they used (`Badge`, `Textarea`, `useGradeAnswer`); add:
  ```tsx
  import { type AnswerDraft, QuestionAnswerInput } from "./question-answer-input";
  import { ResultPanel } from "./result-panel";
  ```
  Replace the local `type AnswerDraft = …` with the import above, and change the JSX `<QuestionInput …>` (line 124) to `<QuestionAnswerInput …>`. Keep all other behavior identical.

- [ ] **Step 4: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no errors.

- [ ] **Step 5: Manual smoke.** Take a published test through the **old** dialog — confirm answering and submit/result still behave exactly as before (pure refactor).

- [ ] **Step 6: Commit.**
  ```bash
  git add frontend/views/assessment/question-answer-input.tsx frontend/views/assessment/result-panel.tsx frontend/views/assessment/take-attempt-dialog.tsx
  git commit -m "refactor(frontend): extract QuestionAnswerInput and ResultPanel from take-attempt-dialog"
  ```

---

### Task 6: `AttemptWizard` + wire taking flow

**Files:**
- Create: `frontend/views/assessment/attempt-wizard.tsx`
- Modify: `frontend/views/assessment/assessment-view.tsx`
- Delete: `frontend/views/assessment/take-attempt-dialog.tsx`

**Interfaces:**
- Consumes: `WizardShell`, `QuestionAnswerInput`/`AnswerDraft`, `ResultPanel`, `useGetTest`/`useStartAttempt`/`useSaveAnswer`/`useSubmitAttempt`.
- Produces:
  ```ts
  function AttemptWizard(props: {
    testId: string;
    testTitle: string;
    canManage: boolean;
    open: boolean;
    onOpenChange: (open: boolean) => void;
  }): JSX.Element
  ```

- [ ] **Step 1: Write `AttemptWizard`.** Steps `0..N-1` are questions, step `N` is review. After submit, render `ResultPanel` inside the same shell (the footer Submit becomes a no-op once submitted; closing resets).
  ```tsx
  "use client";

  import { useTranslations } from "next-intl";
  import { useEffect, useRef, useState } from "react";
  import { toast } from "sonner";

  import { WizardShell } from "@/components/shared/wizard-shell";
  import { Badge } from "@/components/ui/badge";
  import { Button } from "@/components/ui/button";
  import {
    useGetTest,
    useSaveAnswer,
    useStartAttempt,
    useSubmitAttempt,
  } from "@/lib/api/assessment/assessment/assessment";
  import type { AttemptResponse } from "@/lib/api/assessment/model";
  import { type AnswerDraft, QuestionAnswerInput } from "./question-answer-input";
  import { ResultPanel } from "./result-panel";

  function isAnswered(a?: AnswerDraft) {
    return !!a && (a.selectedOptionIds.length > 0 || a.text.trim().length > 0);
  }

  export function AttemptWizard({
    testId,
    testTitle,
    canManage,
    open,
    onOpenChange,
  }: {
    testId: string;
    testTitle: string;
    canManage: boolean;
    open: boolean;
    onOpenChange: (open: boolean) => void;
  }) {
    const t = useTranslations("tests");
    const { data: test } = useGetTest(testId, { query: { enabled: open } });
    const start = useStartAttempt();
    const saveAnswer = useSaveAnswer();
    const submit = useSubmitAttempt();

    const [attempt, setAttempt] = useState<AttemptResponse | null>(null);
    const [answers, setAnswers] = useState<Record<string, AnswerDraft>>({});
    const [step, setStep] = useState(0);
    const startedRef = useRef(false);

    useEffect(() => {
      if (!open) {
        setAttempt(null);
        setAnswers({});
        setStep(0);
        startedRef.current = false;
        return;
      }
      if (startedRef.current) return;
      startedRef.current = true;
      start.mutate(
        { testId },
        {
          onSuccess: (a) => setAttempt(a),
          onError: () => {
            startedRef.current = false;
          },
        },
      );
    }, [open, testId, start]);

    const questions = test?.questions ?? [];
    const stepCount = questions.length + 1; // + review
    const onReview = step === questions.length;
    const submitted = attempt && attempt.status !== "IN_PROGRESS";

    function setAnswer(questionId: string, patch: Partial<AnswerDraft>) {
      setAnswers((prev) => {
        const base: AnswerDraft = prev[questionId] ?? { selectedOptionIds: [], text: "" };
        return { ...prev, [questionId]: { ...base, ...patch } };
      });
    }

    async function persist(questionId: string) {
      if (!attempt) return;
      const a = answers[questionId];
      if (!isAnswered(a)) return;
      await saveAnswer.mutateAsync({
        attemptId: attempt.id,
        data: {
          questionId,
          selectedOptionIds: a.selectedOptionIds.length ? a.selectedOptionIds : undefined,
          text: a.text || undefined,
        },
      });
    }

    async function onNext() {
      const q = questions[step];
      if (q) {
        try {
          await persist(q.id);
        } catch {
          return; // global toast already fired; stay on the question
        }
      }
      setStep((s) => Math.min(questions.length, s + 1));
    }

    async function onSubmit() {
      if (!attempt || !test || submitted) {
        onOpenChange(false);
        return;
      }
      try {
        await Promise.all(questions.filter((q) => isAnswered(answers[q.id])).map((q) => persist(q.id)));
        const graded = await submit.mutateAsync({ attemptId: attempt.id });
        setAttempt(graded);
        toast.success(t("submitted"));
      } catch {
        /* global toast */
      }
    }

    return (
      <WizardShell
        open={open}
        onOpenChange={onOpenChange}
        title={testTitle}
        description={
          submitted ? t("result") : onReview ? t("reviewAnswers") : t("answerPrompt")
        }
        stepCount={stepCount}
        activeStep={step}
        canAdvance={!!attempt}
        isSubmitting={submit.isPending || saveAnswer.isPending}
        onBack={() => setStep((s) => Math.max(0, s - 1))}
        onNext={onNext}
        onSubmit={onSubmit}
        backLabel={t("wizardBack")}
        nextLabel={t("wizardNext")}
        submitLabel={submitted ? t("wizardBack") : t("submitAttempt")}
        submittingLabel={t("submitting")}
      >
        {submitted ? (
          <ResultPanel attempt={attempt} test={questions} canManage={canManage} />
        ) : onReview ? (
          <div className="grid gap-2">
            <p className="text-muted-foreground text-sm">
              {t("questionProgress", { current: questions.length, total: questions.length })}
            </p>
            {questions.map((q, i) => (
              <button
                key={q.id}
                type="button"
                className="flex items-center gap-2 rounded-md border p-2 text-left text-sm hover:border-primary"
                onClick={() => setStep(i)}
              >
                <span className="text-muted-foreground">{i + 1}.</span>
                <span className="flex-1 truncate">{q.prompt}</span>
                <Badge variant={isAnswered(answers[q.id]) ? "default" : "secondary"}>
                  {isAnswered(answers[q.id]) ? t("answered") : t("unanswered")}
                </Badge>
              </button>
            ))}
          </div>
        ) : questions[step] ? (
          <div className="grid gap-2">
            <p className="text-muted-foreground text-sm">
              {t("questionProgress", { current: step + 1, total: questions.length })}
            </p>
            <QuestionAnswerInput
              index={step}
              question={questions[step]}
              draft={answers[questions[step].id]}
              onChange={(patch) => setAnswer(questions[step].id, patch)}
            />
          </div>
        ) : null}
      </WizardShell>
    );
  }
  ```

  Note: after submit, the footer primary button is relabelled to "Назад/Back" and simply closes the dialog (`onSubmit` early-returns `onOpenChange(false)` when `submitted`). `Button` import is kept for the review-list buttons; remove it if unused after final review of the file.

- [ ] **Step 2: Wire into `assessment-view.tsx`.**
  - Change import `import { TakeAttemptDialog } from "./take-attempt-dialog";` → `import { AttemptWizard } from "./attempt-wizard";`
  - Change the JSX `<TakeAttemptDialog … />` → `<AttemptWizard … />` (same props: `testId`, `testTitle`, `canManage`, `open`, `onOpenChange`).

- [ ] **Step 3: Delete the old dialog.**
  ```bash
  rm frontend/views/assessment/take-attempt-dialog.tsx
  ```

- [ ] **Step 4: Typecheck.** Run: `cd frontend && pnpm exec tsc --noEmit`. Expected: no errors, no dangling reference to `take-attempt-dialog`.

- [ ] **Step 5: Manual smoke.** With `next dev` running, as a STUDENT open a PUBLISHED test → "Пройти":
  - One question per screen; progress dots advance; Back/Next work.
  - Answer types render correctly (single radio, multi checkbox, short text, code textarea).
  - Reopen mid-attempt → previously saved answers persist on the server (the saved-on-Next behavior; note local draft state resets but submitted answers were stored).
  - Review step lists all questions with answered/unanswered badges; clicking jumps back.
  - Submit → result panel shows score; as TEACHER, manual grading inputs appear for text/code answers.

- [ ] **Step 6: Commit.**
  ```bash
  git add frontend/views/assessment/attempt-wizard.tsx frontend/views/assessment/assessment-view.tsx
  git add -A frontend/views/assessment/take-attempt-dialog.tsx
  git commit -m "feat(frontend): one-question-per-step attempt wizard; drop take-attempt-dialog"
  ```

---

## Self-Review

**Spec coverage:**
- Shared `WizardShell` → Task 1. ✓
- Content templates (frontend presets) → Task 2. ✓
- Creation wizard 3 steps (Start/Questions/Review) + `1 + N` save + publish toggle + partial-failure handling → Task 4 (uses Task 3 editor). ✓
- Taking wizard one-per-step + save-on-Next + review + submit + reuse ResultPanel → Tasks 5–6. ✓
- `QuestionAnswerInput` extraction → Task 5. ✓
- Wiring into `assessment-view`, removing old dialogs, keeping `ManageQuestionsDialog` for editing → Tasks 4 & 6. ✓
- No backend changes, no new deps, tsc + next dev verification → Global Constraints, every task. ✓

**Placeholder scan:** No TBD/TODO; every code step shows full code; smoke checks are concrete. ✓

**Type consistency:** `DraftQuestion` (`CreateQuestionRequest & { _key }`) defined in Task 3, consumed in Task 4 via `toRequests` which strips `_key` and empty options. `AnswerDraft` defined in Task 5, consumed in Task 6. API hook signatures match `assessment.ts`. ✓

**Open verification points the implementer must confirm (noted inline, not blockers):** `@/components/ui/label` exists (Task 4 Step 1 fallback given); `cn` at `@/lib/utils` (Task 1).
