"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { StatusBadge } from "@/components/shared/status-badge";
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
  const questionFor = (questionId: string) => test.find((q) => q.id === questionId);
  const totalPoints = test.reduce((sum, q) => sum + q.points, 0);

  return (
    <div className="grid gap-3">
      <div className="flex items-center gap-2">
        <StatusBadge status={attempt.status} label={t(`attemptStatus.${attempt.status}`)} />
        <span className="font-medium">
          {t("score")}: {attempt.score} / {totalPoints}
        </span>
      </div>
      <Separator />
      {attempt.answers.map((ans) => {
        const q = questionFor(ans.questionId);
        return (
          <div key={ans.id} className="grid gap-1 rounded-md border p-3 text-sm">
            <p className="font-medium">{q?.prompt ?? "—"}</p>
            {ans.text && <p className="text-muted-foreground">"{ans.text}"</p>}
            <p className="text-muted-foreground">
              {t("awarded")}: {ans.awardedPoints ?? t("pending")} / {q?.points ?? 0} {t("pt")}
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
        );
      })}
    </div>
  );
}
