"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import { useRouter } from "@/i18n/navigation";
import {
  useArchiveCourse,
  useDeleteCourse,
  usePublishCourse,
} from "@/lib/api/course/course/course";
import { type CourseDetailResponse, CourseStatus } from "@/lib/api/course/model";

/** Admin lifecycle for the course-detail header: publish a draft, archive a published one, delete (confirmed) then navigate away. */
export function CourseDetailActions({ course }: { course: CourseDetailResponse }) {
  const t = useTranslations("courses");
  const router = useRouter();
  const publish = usePublishCourse();
  const archive = useArchiveCourse();
  const remove = useDeleteCourse();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const courseId = course.id;
  const pending = publish.isPending || archive.isPending || remove.isPending;

  return (
    <div className="flex items-center gap-2">
      {course.status !== CourseStatus.PUBLISHED && (
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => publish.mutate({ courseId })}
        >
          {course.status === CourseStatus.ARCHIVED ? t("restore") : t("publish")}
        </Button>
      )}
      {course.status === CourseStatus.PUBLISHED && (
        <Button
          variant="outline"
          size="sm"
          disabled={pending}
          onClick={() => archive.mutate({ courseId })}
        >
          {t("archive")}
        </Button>
      )}
      {course.status !== CourseStatus.ARCHIVED && (
        <Button
          variant="ghost"
          size="sm"
          className="text-destructive"
          disabled={pending}
          onClick={() => setConfirmOpen(true)}
        >
          {t("delete")}
        </Button>
      )}

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("confirmDeleteTitle")}
        description={t("confirmDelete")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={remove.isPending}
        onConfirm={() =>
          remove.mutate({ courseId }, { onSuccess: () => router.push("/dashboard/courses") })
        }
      />
    </div>
  );
}
