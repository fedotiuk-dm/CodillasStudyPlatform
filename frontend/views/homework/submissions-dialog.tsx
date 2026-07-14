"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import {
  useCreateSubmission,
  useGetRubric,
  useListSubmissions,
} from "@/lib/api/homework/homework/homework";

import { SubmissionRow } from "./submission-row";

/** Submissions for one assignment: list + (teacher) rubric + grading, or the student draft form. */
export function SubmissionsDialog({
  assignmentId,
  assignmentTitle,
  canManage,
  hasRubric,
  open,
  onOpenChange,
}: {
  assignmentId: string;
  assignmentTitle: string;
  canManage: boolean;
  hasRubric: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("homework");
  const { data } = useListSubmissions(assignmentId, { query: { enabled: open } });
  const { data: rubric } = useGetRubric(assignmentId, { query: { enabled: open && hasRubric } });
  const submissions = data ?? [];

  const create = useCreateSubmission();
  const [draft, setDraft] = useState("");

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

        {canManage && rubric && (
          <div className="grid gap-1 rounded-md border p-3 text-sm">
            <p className="font-medium">{t("rubric")}</p>
            {rubric.criteria.map((c) => (
              <div key={c.id} className="flex justify-between text-muted-foreground">
                <span>{c.label}</span>
                <span>
                  {c.maxPoints} {t("maxPoints")}
                </span>
              </div>
            ))}
          </div>
        )}

        <div className="grid gap-3">
          {submissions.length === 0 && (
            <p className="text-muted-foreground text-sm">{t("noSubmissions")}</p>
          )}
          {submissions.map((s) => (
            <SubmissionRow key={s.id} submission={s} canManage={canManage} rubric={rubric} />
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
