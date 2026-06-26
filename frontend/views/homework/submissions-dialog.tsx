"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import {
  useCreateSubmission,
  useGradeSubmission,
  useListSubmissions,
  useReturnSubmission,
  useReviewSubmission,
  useSubmitSubmission,
  useUpdateSubmission,
} from "@/lib/api/homework/homework/homework";
import type { SubmissionResponse } from "@/lib/api/homework/model";

const STATUS_VARIANT: Record<string, "default" | "secondary" | "outline"> = {
  DRAFT: "secondary",
  SUBMITTED: "outline",
  IN_REVIEW: "outline",
  GRADED: "default",
  RETURNED: "secondary",
};

export function SubmissionsDialog({
  assignmentId,
  assignmentTitle,
  canManage,
  open,
  onOpenChange,
}: {
  assignmentId: string;
  assignmentTitle: string;
  canManage: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("homework");
  const { data } = useListSubmissions(assignmentId, { query: { enabled: open } });
  const submissions = data ?? [];

  const create = useCreateSubmission();
  const update = useUpdateSubmission();
  const submit = useSubmitSubmission();
  const review = useReviewSubmission();
  const grade = useGradeSubmission();
  const ret = useReturnSubmission();

  const [draft, setDraft] = useState("");

  const done = (msg: string) => () => {
    toast.success(msg);
  };

  function onCreate() {
    if (!draft.trim()) return;
    create.mutate(
      { assignmentId, data: { content: draft } },
      {
        onSuccess: () => {
          toast.success(t("draftSaved"));
          setDraft("");
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{assignmentTitle}</DialogTitle>
          <DialogDescription>
            {canManage ? t("manageDescription") : t("studentDescription")}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3">
          {submissions.length === 0 && (
            <p className="text-muted-foreground text-sm">{t("noSubmissions")}</p>
          )}
          {submissions.map((s) => (
            <SubmissionRow
              key={s.id}
              submission={s}
              canManage={canManage}
              onUpdate={(content) =>
                update.mutate(
                  { submissionId: s.id, data: { content } },
                  { onSuccess: done(t("updated")) },
                )
              }
              onSubmit={() =>
                submit.mutate({ submissionId: s.id }, { onSuccess: done(t("submitted")) })
              }
              onReview={(comment) =>
                review.mutate(
                  { submissionId: s.id, data: { comment } },
                  { onSuccess: done(t("reviewSent")) },
                )
              }
              onGrade={(score) =>
                grade.mutate(
                  { submissionId: s.id, data: { score } },
                  { onSuccess: done(t("graded")) },
                )
              }
              onReturn={() =>
                ret.mutate({ submissionId: s.id }, { onSuccess: done(t("returned")) })
              }
            />
          ))}
        </div>

        {!canManage && (
          <>
            <Separator />
            <div className="grid gap-2">
              <p className="font-medium text-sm">{t("newSubmission")}</p>
              <Textarea
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder={t("workPlaceholder")}
              />
              <Button
                className="justify-self-start"
                disabled={create.isPending || !draft.trim()}
                onClick={onCreate}
              >
                {t("saveDraft")}
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function SubmissionRow({
  submission: s,
  canManage,
  onUpdate,
  onSubmit,
  onReview,
  onGrade,
  onReturn,
}: {
  submission: SubmissionResponse;
  canManage: boolean;
  onUpdate: (content: string) => void;
  onSubmit: () => void;
  onReview: (comment: string) => void;
  onGrade: (score: number) => void;
  onReturn: () => void;
}) {
  const t = useTranslations("homework");
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(s.content ?? "");
  const [comment, setComment] = useState("");
  const [score, setScore] = useState("");

  return (
    <div className="grid gap-2 rounded-md border p-3 text-sm">
      <div className="flex items-center gap-2">
        <Badge variant={STATUS_VARIANT[s.status] ?? "outline"}>{s.status}</Badge>
        <span className="text-muted-foreground">v{s.version}</span>
      </div>

      {editing ? (
        <Textarea value={content} onChange={(e) => setContent(e.target.value)} />
      ) : (
        <p className="whitespace-pre-wrap">{s.content || "—"}</p>
      )}

      <div className="flex flex-wrap gap-2">
        {/* Student actions on own draft */}
        {!canManage && s.status === "DRAFT" && !editing && (
          <>
            <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
              {t("edit")}
            </Button>
            <Button size="sm" onClick={onSubmit}>
              {t("submit")}
            </Button>
          </>
        )}
        {!canManage && s.status === "DRAFT" && editing && (
          <Button
            size="sm"
            onClick={() => {
              onUpdate(content);
              setEditing(false);
            }}
          >
            {t("save")}
          </Button>
        )}

        {/* Teacher actions by status */}
        {canManage && (s.status === "SUBMITTED" || s.status === "IN_REVIEW") && (
          <>
            <div className="flex flex-1 items-center gap-2">
              <Input
                className="flex-1"
                placeholder={t("reviewCommentPlaceholder")}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
              />
              <Button size="sm" disabled={!comment.trim()} onClick={() => onReview(comment)}>
                {t("sendReview")}
              </Button>
            </div>
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
              <Button size="sm" disabled={!score} onClick={() => onGrade(Number(score))}>
                {t("grade")}
              </Button>
            </div>
          </>
        )}
        {canManage && s.status === "GRADED" && (
          <Button variant="outline" size="sm" onClick={onReturn}>
            {t("returnToStudent")}
          </Button>
        )}
      </div>
    </div>
  );
}
