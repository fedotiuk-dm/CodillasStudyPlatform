"use client";

import { useTranslations } from "next-intl";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  useGetTest,
  useSaveAnswer,
  useStartAttempt,
  useSubmitAttempt,
} from "@/lib/api/assessment/assessment/assessment";
import type { AttemptResponse } from "@/lib/api/assessment/model";

import { type AnswerDraft, QuestionAnswerInput } from "./question-answer-input";
import { ResultPanel } from "./result-panel";

export function TakeAttemptDialog({
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
  const startedRef = useRef(false);

  // Start exactly one attempt per dialog open.
  useEffect(() => {
    if (!open) {
      setAttempt(null);
      setAnswers({});
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

  function setAnswer(questionId: string, patch: Partial<AnswerDraft>) {
    setAnswers((prev) => {
      const base: AnswerDraft = prev[questionId] ?? { selectedOptionIds: [], text: "" };
      return { ...prev, [questionId]: { ...base, ...patch } };
    });
  }

  async function onSubmit() {
    if (!attempt || !test) return;
    try {
      // answers are independent — save them concurrently before the final submit
      await Promise.all(
        test.questions
          .filter((q) => answers[q.id])
          .map((q) => {
            const a = answers[q.id];
            return saveAnswer.mutateAsync({
              attemptId: attempt.id,
              data: {
                questionId: q.id,
                selectedOptionIds: a.selectedOptionIds.length ? a.selectedOptionIds : undefined,
                text: a.text || undefined,
              },
            });
          }),
      );
      const graded = await submit.mutateAsync({ attemptId: attempt.id });
      setAttempt(graded);
      toast.success(t("submitted"));
    } catch {
      /* global toast */
    }
  }

  const submitted = attempt && attempt.status !== "IN_PROGRESS";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{testTitle}</DialogTitle>
          <DialogDescription>{submitted ? t("result") : t("answerPrompt")}</DialogDescription>
        </DialogHeader>

        {submitted ? (
          <ResultPanel attempt={attempt} test={test?.questions ?? []} canManage={canManage} />
        ) : (
          <div className="grid gap-4">
            {test?.questions.map((q, i) => (
              <QuestionAnswerInput
                key={q.id}
                index={i}
                question={q}
                draft={answers[q.id]}
                onChange={(patch) => setAnswer(q.id, patch)}
              />
            ))}
            <Button
              onClick={onSubmit}
              disabled={!attempt || submit.isPending || saveAnswer.isPending}
            >
              {submit.isPending ? t("submitting") : t("submitAttempt")}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
