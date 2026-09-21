"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import { useDeleteSection } from "@/lib/api/course/course/course";
import type { SectionResponse } from "@/lib/api/course/model";
import { LessonDialog } from "./lesson-dialog";
import { LessonItem } from "./lesson-item";

/** One course section: its lessons, plus add-lesson / rename / confirmed-delete for editors. */
export function SectionBlock({
  section,
  canEdit,
  onRename,
}: {
  section: SectionResponse;
  canEdit: boolean;
  onRename: () => void;
}) {
  const t = useTranslations("course");
  const del = useDeleteSection();
  const [addLesson, setAddLesson] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const lessons = section.lessons;

  return (
    <div className="rounded-lg border">
      <div className="flex items-center justify-between gap-2 border-b px-4 py-3">
        <h3 className="font-semibold">{section.title}</h3>
        {canEdit && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setAddLesson(true)}>
              {t("addLesson")}
            </Button>
            <Button variant="outline" size="sm" onClick={onRename}>
              {t("rename")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              disabled={del.isPending}
              onClick={() => setConfirmDelete(true)}
            >
              {t("delete")}
            </Button>
          </div>
        )}
      </div>

      <div>
        {lessons.length === 0 && (
          <p className="px-4 py-3 text-muted-foreground text-sm">{t("noLessons")}</p>
        )}
        {lessons.map((lesson) => (
          <LessonItem key={lesson.id} lesson={lesson} sectionId={section.id} canEdit={canEdit} />
        ))}
      </div>

      {canEdit && addLesson && (
        <LessonDialog sectionId={section.id} open onOpenChange={(o) => !o && setAddLesson(false)} />
      )}

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={t("confirmDeleteSectionTitle")}
        description={t("confirmDeleteSection")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={del.isPending}
        onConfirm={() =>
          del.mutate(
            { sectionId: section.id },
            {
              onSuccess: () => {
                toast.success(t("sectionDeleted"));
                setConfirmDelete(false);
              },
            },
          )
        }
      />
    </div>
  );
}
