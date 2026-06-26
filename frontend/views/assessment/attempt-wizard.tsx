"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { WizardShell } from "@/components/shared/wizard-shell";
import { Badge } from "@/components/ui/badge";
import {
  useGetTest,
  useSaveAnswer,
  useSubmitAttempt,
} from "@/lib/api/assessment/assessment/assessment";
import type { AttemptResponse } from "@/lib/api/assessment/model";
import { type AnswerDraft, QuestionAnswerInput } from "./question-answer-input";
import { ResultPanel } from "./result-panel";

function isAnswered(a?: AnswerDraft) {
  return !!a && (a.selectedOptionIds.length > 0 || a.text.trim().length > 0);
}

// Seed editable drafts from the attempt's already-saved answers, so a resumed (or submitted)
// attempt shows the student's prior progress.
function seedDrafts(attempt: AttemptResponse): Record<string, AnswerDraft> {
  return Object.fromEntries(
    attempt.answers.map((ans) => [
      ans.questionId,
      { selectedOptionIds: ans.selectedOptionIds ?? [], text: ans.text ?? "" },
    ]),
  );
}

export function AttemptWizard({
  testId,
  testTitle,
  canManage,
  open,
  onOpenChange,
  attempt: initialAttempt,
}: {
  testId: string;
  testTitle: string;
  canManage: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  // Created by the caller (the "Take" click) and handed in ready. The wizard never starts the
  // attempt itself — so there is no mutation fired on open (which detaches and hangs under React
  // StrictMode). Resource-creating mutations belong in the event handler, not a mount effect.
  attempt: AttemptResponse;
}) {
  const t = useTranslations("tests");
  const tc = useTranslations("common");
  const { data: test } = useGetTest(testId, { query: { enabled: open } });
  const saveAnswer = useSaveAnswer();
  const submit = useSubmitAttempt();

  const [attempt, setAttempt] = useState(initialAttempt);
  const [answers, setAnswers] = useState<Record<string, AnswerDraft>>(() =>
    seedDrafts(initialAttempt),
  );
  const [step, setStep] = useState(0);

  const questions = test?.questions ?? [];
  const stepCount = questions.length + 1; // + review
  const onReview = step === questions.length;
  const submitted = attempt.status !== "IN_PROGRESS";

  function setAnswer(questionId: string, patch: Partial<AnswerDraft>) {
    setAnswers((prev) => {
      const base: AnswerDraft = prev[questionId] ?? { selectedOptionIds: [], text: "" };
      return { ...prev, [questionId]: { ...base, ...patch } };
    });
  }

  async function persist(questionId: string) {
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
    if (!test || submitted) {
      onOpenChange(false);
      return;
    }
    try {
      await Promise.all(
        questions.filter((q) => isAnswered(answers[q.id])).map((q) => persist(q.id)),
      );
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
      description={submitted ? t("result") : onReview ? t("reviewAnswers") : t("answerPrompt")}
      stepCount={stepCount}
      activeStep={step}
      canAdvance={!!test}
      isSubmitting={submit.isPending || saveAnswer.isPending}
      onBack={() => setStep((s) => Math.max(0, s - 1))}
      onNext={onNext}
      onSubmit={onSubmit}
      backLabel={t("wizardBack")}
      nextLabel={t("wizardNext")}
      submitLabel={submitted ? t("wizardBack") : t("submitAttempt")}
      submittingLabel={t("submitting")}
    >
      {!test ? (
        <p className="text-muted-foreground text-sm">{tc("loading")}</p>
      ) : submitted ? (
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
