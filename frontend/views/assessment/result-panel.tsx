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
          {ans.text && <p className="text-muted-foreground">"{ans.text}"</p>}
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
