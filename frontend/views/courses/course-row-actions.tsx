"use client";

import { MoreHorizontal } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  useArchiveCourse,
  useDeleteCourse,
  usePublishCourse,
} from "@/lib/api/course/course/course";
import { type CourseResponse, CourseStatus } from "@/lib/api/course/model";

/** One course's lifecycle actions: publish a draft, archive a published one, delete (confirmed). */
export function CourseRowActions({ course }: { course: CourseResponse }) {
  const t = useTranslations("courses");
  const publish = usePublishCourse();
  const archive = useArchiveCourse();
  const remove = useDeleteCourse();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const courseId = course.id;
  const pending = publish.isPending || archive.isPending || remove.isPending;

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" disabled={pending} aria-label={t("actions")}>
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {course.status !== CourseStatus.PUBLISHED && (
            <DropdownMenuItem onClick={() => publish.mutate({ courseId })}>
              {course.status === CourseStatus.ARCHIVED ? t("restore") : t("publish")}
            </DropdownMenuItem>
          )}
          {course.status === CourseStatus.PUBLISHED && (
            <DropdownMenuItem onClick={() => archive.mutate({ courseId })}>
              {t("archive")}
            </DropdownMenuItem>
          )}
          {course.status !== CourseStatus.ARCHIVED && (
            <DropdownMenuItem className="text-destructive" onClick={() => setConfirmOpen(true)}>
              {t("delete")}
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("confirmDeleteTitle")}
        description={t("confirmDelete")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={remove.isPending}
        onConfirm={() => remove.mutate({ courseId }, { onSuccess: () => setConfirmOpen(false) })}
      />
    </>
  );
}
