"use client";

import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import {
  useGetTest,
  useGradeAnswer,
  useSaveAnswer,
  useStartAttempt,
  useSubmitAttempt,
} from "@/lib/api/assessment/assessment/assessment";
import type { AttemptResponse, QuestionResponse } from "@/lib/api/assessment/model";

type AnswerDraft = { selectedOptionIds: string[]; text: string };

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
          toast.error("Could not start attempt");
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
      toast.success("Submitted");
    } catch {
      toast.error("Submit failed");
    }
  }

  const submitted = attempt && attempt.status !== "IN_PROGRESS";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{testTitle}</DialogTitle>
          <DialogDescription>
            {submitted ? "Your result." : "Answer the questions and submit."}
          </DialogDescription>
        </DialogHeader>

        {submitted ? (
          <ResultPanel attempt={attempt} test={test?.questions ?? []} canManage={canManage} />
        ) : (
          <div className="grid gap-4">
            {test?.questions.map((q, i) => (
              <QuestionInput
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
              {submit.isPending ? "Submitting…" : "Submit attempt"}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function QuestionInput({
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
  const selected = draft?.selectedOptionIds ?? [];
  const single = q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE";

  return (
    <div className="grid gap-2 rounded-md border p-3">
      <p className="font-medium text-sm">
        {index + 1}. {q.prompt}{" "}
        <span className="font-normal text-muted-foreground">({q.points} pt)</span>
      </p>
      {q.type === "SHORT_TEXT" ? (
        <Input
          placeholder="Your answer…"
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

function ResultPanel({
  attempt,
  test,
  canManage,
}: {
  attempt: AttemptResponse;
  test: QuestionResponse[];
  canManage: boolean;
}) {
  const grade = useGradeAnswer();
  const [scores, setScores] = useState<Record<string, string>>({});
  const promptFor = (questionId: string) => test.find((q) => q.id === questionId)?.prompt ?? "—";

  return (
    <div className="grid gap-3">
      <div className="flex items-center gap-2">
        <Badge variant={attempt.status === "GRADED" ? "default" : "secondary"}>
          {attempt.status}
        </Badge>
        <span className="font-medium">Score: {attempt.score}</span>
      </div>
      <Separator />
      {attempt.answers.map((ans) => (
        <div key={ans.id} className="grid gap-1 rounded-md border p-3 text-sm">
          <p className="font-medium">{promptFor(ans.questionId)}</p>
          {ans.text && <p className="text-muted-foreground">“{ans.text}”</p>}
          <p className="text-muted-foreground">Awarded: {ans.awardedPoints ?? "pending"}</p>
          {canManage && ans.awardedPoints == null && (
            <div className="flex items-center gap-2">
              <Input
                type="number"
                min={0}
                className="w-24"
                placeholder="points"
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
                    { onSuccess: () => toast.success("Graded") },
                  )
                }
              >
                Grade
              </Button>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
