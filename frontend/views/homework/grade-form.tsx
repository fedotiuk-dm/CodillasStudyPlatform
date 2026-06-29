"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useGradeSubmission } from "@/lib/api/homework/homework/homework";
import type { GradeResponse, RubricResponse } from "@/lib/api/homework/model";
import { SubmissionStatus } from "@/lib/api/homework/model";

/**
 * Teacher grading for one submission. With a rubric it collects per-criterion points
 * (→ criterionScores); otherwise a single free-form 0–100 score. On success it shows the
 * effective score (after any late penalty) against the raw score and the max points.
 */
export function GradeForm({
  submissionId,
  status,
  rubric,
}: {
  submissionId: string;
  status: string;
  rubric?: RubricResponse;
}) {
  const t = useTranslations("homework");
  const grade = useGradeSubmission();
  const [result, setResult] = useState<GradeResponse | null>(null);
  const [score, setScore] = useState("");
  const [points, setPoints] = useState<Record<string, string>>({});

  const gradeable = status === SubmissionStatus.SUBMITTED || status === SubmissionStatus.IN_REVIEW;

  function onGraded(g: GradeResponse) {
    setResult(g);
    toast.success(t("graded"));
  }

  function onGrade() {
    const data = rubric
      ? {
          criterionScores: rubric.criteria.map((c) => ({
            criterionId: c.id,
            points: Number(points[c.id] || 0),
          })),
        }
      : { score: Number(score) };
    grade.mutate({ submissionId, data }, { onSuccess: onGraded });
  }

  return (
    <div className="grid gap-2">
      {gradeable &&
        (rubric ? (
          <div className="grid gap-2">
            {rubric.criteria.map((c) => (
              <div key={c.id} className="flex items-center gap-2">
                <span className="flex-1 text-sm">{c.label}</span>
                <Input
                  type="number"
                  min={0}
                  max={c.maxPoints}
                  className="w-20"
                  value={points[c.id] ?? ""}
                  onChange={(e) => setPoints((p) => ({ ...p, [c.id]: e.target.value }))}
                />
                <span className="text-muted-foreground text-sm">/ {c.maxPoints}</span>
              </div>
            ))}
            <Button
              size="sm"
              className="justify-self-start"
              disabled={grade.isPending}
              onClick={onGrade}
            >
              {t("grade")}
            </Button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Input
              type="number"
              min={0}
              max={100}
              className="w-20"
              placeholder={t("scorePlaceholder")}
              value={score}
              onChange={(e) => setScore(e.target.value)}
            />
            <Button size="sm" disabled={!score || grade.isPending} onClick={onGrade}>
              {t("grade")}
            </Button>
          </div>
        ))}

      {result && (
        <div className="grid gap-0.5 rounded-md bg-muted/50 p-2 text-sm">
          <span className="font-medium">
            {t("gradeEffective")}: {result.effectiveScore} / {result.maxPoints}
          </span>
          {result.score !== result.effectiveScore && (
            <span className="text-muted-foreground">
              {t("gradeRaw")}: {result.score} / {result.maxPoints}
            </span>
          )}
        </div>
      )}
    </div>
  );
}
