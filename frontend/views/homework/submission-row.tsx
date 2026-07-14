"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { StatusBadge } from "@/components/shared/status-badge";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  useReturnSubmission,
  useReviewSubmission,
  useSubmitSubmission,
  useUpdateSubmission,
} from "@/lib/api/homework/homework/homework";
import type { RubricResponse, SubmissionResponse } from "@/lib/api/homework/model";
import { SubmissionStatus } from "@/lib/api/homework/model";

import { GradeForm } from "./grade-form";

/** One submission card: student draft editing, teacher review/grade/return, and a late marker. */
export function SubmissionRow({
  submission: s,
  canManage,
  rubric,
}: {
  submission: SubmissionResponse;
  canManage: boolean;
  rubric?: RubricResponse;
}) {
  const t = useTranslations("homework");
  const update = useUpdateSubmission();
  const submit = useSubmitSubmission();
  const review = useReviewSubmission();
  const ret = useReturnSubmission();

  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(s.content ?? "");
  const [comment, setComment] = useState("");

  const done = (msg: string) => () => toast.success(msg);
  const inReview =
    s.status === SubmissionStatus.SUBMITTED || s.status === SubmissionStatus.IN_REVIEW;

  return (
    <div className="grid gap-2 rounded-md border p-3 text-sm">
      <div className="flex items-center gap-2">
        <StatusBadge status={s.status} label={t(`submissionStatus.${s.status}`)} />
        {s.late && <Badge variant="destructive">{t("late")}</Badge>}
        <span className="text-muted-foreground">v{s.version}</span>
      </div>

      {editing ? (
        <Textarea value={content} onChange={(e) => setContent(e.target.value)} />
      ) : (
        <p className="whitespace-pre-wrap">{s.content || "—"}</p>
      )}

      <div className="flex flex-wrap gap-2">
        {/* Student actions on own draft */}
        {!canManage && s.status === SubmissionStatus.DRAFT && !editing && (
          <>
            <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
              {t("edit")}
            </Button>
            <Button
              size="sm"
              disabled={submit.isPending}
              onClick={() =>
                submit.mutate({ submissionId: s.id }, { onSuccess: done(t("submitted")) })
              }
            >
              {t("submit")}
            </Button>
          </>
        )}
        {!canManage && s.status === SubmissionStatus.DRAFT && editing && (
          <Button
            size="sm"
            disabled={update.isPending}
            onClick={() =>
              update.mutate(
                { submissionId: s.id, data: { content } },
                {
                  onSuccess: () => {
                    toast.success(t("updated"));
                    setEditing(false);
                  },
                },
              )
            }
          >
            {t("save")}
          </Button>
        )}

        {/* Teacher review */}
        {canManage && inReview && (
          <div className="flex flex-1 items-center gap-2">
            <Input
              className="flex-1"
              placeholder={t("reviewCommentPlaceholder")}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
            <Button
              size="sm"
              disabled={!comment.trim() || review.isPending}
              onClick={() =>
                review.mutate(
                  { submissionId: s.id, data: { comment } },
                  { onSuccess: done(t("reviewSent")) },
                )
              }
            >
              {t("sendReview")}
            </Button>
          </div>
        )}

        {/* Teacher return */}
        {canManage && s.status === SubmissionStatus.GRADED && (
          <Button
            variant="outline"
            size="sm"
            disabled={ret.isPending}
            onClick={() => ret.mutate({ submissionId: s.id }, { onSuccess: done(t("returned")) })}
          >
            {t("returnToStudent")}
          </Button>
        )}
      </div>

      {/* Teacher grading: rubric per-criterion or free-form score */}
      {canManage && (inReview || s.status === SubmissionStatus.GRADED) && (
        <GradeForm submissionId={s.id} status={s.status} rubric={rubric} />
      )}
    </div>
  );
}
