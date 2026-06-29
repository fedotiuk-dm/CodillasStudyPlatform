"use client";

import { ChevronDown, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useDeleteLesson, useGetLesson } from "@/lib/api/course/course/course";
import type { LessonSummary } from "@/lib/api/course/model";
import { safeHref } from "@/lib/utils";
import { AddMaterialDialog } from "./add-material-dialog";
import { LessonDialog } from "./lesson-dialog";
import { MaterialRow } from "./material-row";

/** A collapsible lesson row: expands to its detail (materials, links) and owns lesson-level edits. */
export function LessonItem({
  lesson,
  sectionId,
  canEdit,
}: {
  lesson: LessonSummary;
  sectionId: string;
  canEdit: boolean;
}) {
  const t = useTranslations("course");
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState(false);
  const [addMaterial, setAddMaterial] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { data: detail, isLoading } = useGetLesson(lesson.id, { query: { enabled: open } });
  const delLesson = useDeleteLesson();

  const materials = [...(detail?.materials ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  const meetingHref = detail?.meetingUrl ? safeHref(detail.meetingUrl) : undefined;
  const recordingHref = detail?.recordingUrl ? safeHref(detail.recordingUrl) : undefined;

  return (
    <div className="border-t first:border-t-0">
      <button
        type="button"
        className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm hover:bg-muted/50"
        onClick={() => setOpen((o) => !o)}
      >
        {open ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" />}
        <span className="font-medium">{lesson.title}</span>
      </button>

      {open && (
        <div className="grid gap-3 px-4 pb-4 pl-10">
          {isLoading && <Skeleton className="h-16 w-full" />}
          {detail && (
            <>
              {detail.summary && <p className="text-muted-foreground text-sm">{detail.summary}</p>}
              <div className="flex flex-wrap gap-4 text-sm">
                {meetingHref && (
                  <a
                    className="text-primary underline"
                    href={meetingHref}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {t("joinMeeting")}
                  </a>
                )}
                {recordingHref && (
                  <a
                    className="text-primary underline"
                    href={recordingHref}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {t("watchRecording")}
                  </a>
                )}
              </div>

              <div className="grid gap-2">
                <p className="font-medium text-sm">{t("materials")}</p>
                {materials.length === 0 && (
                  <p className="text-muted-foreground text-sm">{t("noMaterials")}</p>
                )}
                <ul className="grid gap-1">
                  {materials.map((m) => (
                    <MaterialRow key={m.id} material={m} canEdit={canEdit} />
                  ))}
                </ul>
              </div>

              {canEdit && (
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => setAddMaterial(true)}>
                    {t("addMaterial")}
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => setEdit(true)}>
                    {t("editLesson")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    disabled={delLesson.isPending}
                    onClick={() => setConfirmDelete(true)}
                  >
                    {t("delete")}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {canEdit && edit && detail && (
        <LessonDialog
          sectionId={sectionId}
          lesson={detail}
          open
          onOpenChange={(o) => !o && setEdit(false)}
        />
      )}
      {canEdit && addMaterial && (
        <AddMaterialDialog
          lessonId={lesson.id}
          open
          onOpenChange={(o) => !o && setAddMaterial(false)}
        />
      )}

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={t("confirmDeleteLessonTitle")}
        description={t("confirmDeleteLesson")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={delLesson.isPending}
        onConfirm={() =>
          delLesson.mutate(
            { lessonId: lesson.id },
            {
              onSuccess: () => {
                toast.success(t("lessonDeleted"));
                setConfirmDelete(false);
              },
            },
          )
        }
      />
    </div>
  );
}
