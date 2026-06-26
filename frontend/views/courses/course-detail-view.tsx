"use client";

import { ChevronDown, ChevronRight } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useDeleteLesson,
  useDeleteMaterial,
  useDeleteSection,
  useGetCourse,
  useGetLesson,
} from "@/lib/api/course/course/course";
import type { LessonSummary, SectionResponse } from "@/lib/api/course/model";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { safeHref } from "@/lib/utils";
import { AddMaterialDialog } from "./add-material-dialog";
import { LessonDialog } from "./lesson-dialog";
import { SectionDialog } from "./section-dialog";

const bySortOrder = (a: { sortOrder: number }, b: { sortOrder: number }) =>
  a.sortOrder - b.sortOrder;

export function CourseDetailView({ courseId }: { courseId: string }) {
  const t = useTranslations("course");
  const { data, isLoading, isError } = useGetCourse(courseId);
  const isTeacher = useHasRole(Role.TEACHER);
  const [createSection, setCreateSection] = useState(false);
  const [renameSection, setRenameSection] = useState<{ id: string; title: string } | null>(null);

  const sections = [...(data?.sections ?? [])].sort(bySortOrder);

  return (
    <>
      <PageHeader
        title={data?.name ?? t("course")}
        description={data?.description ?? undefined}
        action={
          isTeacher ? (
            <Button onClick={() => setCreateSection(true)}>{t("addSection")}</Button>
          ) : undefined
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={sections.length === 0}
            emptyMessage={t("noSections")}
          >
            <div className="grid gap-4">
              {sections.map((section) => (
                <SectionBlock
                  key={section.id}
                  section={section}
                  isTeacher={isTeacher}
                  onRename={() => setRenameSection({ id: section.id, title: section.title })}
                />
              ))}
            </div>
          </DataState>
        </CardContent>
      </Card>

      {isTeacher && createSection && (
        <SectionDialog
          courseId={courseId}
          open
          onOpenChange={(o) => !o && setCreateSection(false)}
        />
      )}
      {isTeacher && renameSection && (
        <SectionDialog
          courseId={courseId}
          section={renameSection}
          open
          onOpenChange={(o) => !o && setRenameSection(null)}
        />
      )}
    </>
  );
}

function SectionBlock({
  section,
  isTeacher,
  onRename,
}: {
  section: SectionResponse;
  isTeacher: boolean;
  onRename: () => void;
}) {
  const t = useTranslations("course");
  const del = useDeleteSection();
  const [addLesson, setAddLesson] = useState(false);
  const lessons = [...section.lessons].sort(bySortOrder);

  function onDelete() {
    // ponytail: native confirm — no confirm-dialog component in the kit
    if (!window.confirm(t("confirmDeleteSection"))) return;
    del.mutate({ sectionId: section.id }, { onSuccess: () => toast.success(t("sectionDeleted")) });
  }

  return (
    <div className="rounded-lg border">
      <div className="flex items-center justify-between gap-2 border-b px-4 py-3">
        <h3 className="font-semibold">{section.title}</h3>
        {isTeacher && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setAddLesson(true)}>
              {t("addLesson")}
            </Button>
            <Button variant="outline" size="sm" onClick={onRename}>
              {t("rename")}
            </Button>
            <Button variant="ghost" size="sm" disabled={del.isPending} onClick={onDelete}>
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
          <LessonItem
            key={lesson.id}
            lesson={lesson}
            sectionId={section.id}
            isTeacher={isTeacher}
          />
        ))}
      </div>

      {isTeacher && addLesson && (
        <LessonDialog sectionId={section.id} open onOpenChange={(o) => !o && setAddLesson(false)} />
      )}
    </div>
  );
}

function LessonItem({
  lesson,
  sectionId,
  isTeacher,
}: {
  lesson: LessonSummary;
  sectionId: string;
  isTeacher: boolean;
}) {
  const t = useTranslations("course");
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState(false);
  const [addMaterial, setAddMaterial] = useState(false);
  const { data: detail, isLoading } = useGetLesson(lesson.id, { query: { enabled: open } });
  const delLesson = useDeleteLesson();
  const delMaterial = useDeleteMaterial();

  const materials = [...(detail?.materials ?? [])].sort(bySortOrder);
  const meetingHref = detail?.meetingUrl ? safeHref(detail.meetingUrl) : undefined;
  const recordingHref = detail?.recordingUrl ? safeHref(detail.recordingUrl) : undefined;

  function onDeleteLesson() {
    if (!window.confirm(t("confirmDeleteLesson"))) return;
    delLesson.mutate(
      { lessonId: lesson.id },
      { onSuccess: () => toast.success(t("lessonDeleted")) },
    );
  }

  function onDeleteMaterial(materialId: string) {
    if (!window.confirm(t("confirmDeleteMaterial"))) return;
    delMaterial.mutate({ materialId }, { onSuccess: () => toast.success(t("materialDeleted")) });
  }

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
                  {materials.map((m) => {
                    const href = m.type === "LINK" && m.url ? safeHref(m.url) : undefined;
                    return (
                      <li
                        key={m.id}
                        className="flex items-center justify-between gap-2 rounded border px-3 py-2 text-sm"
                      >
                        <span className="flex items-center gap-2">
                          <Badge variant="secondary">{m.type}</Badge>
                          {href ? (
                            <a
                              className="text-primary underline"
                              href={href}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {m.title}
                            </a>
                          ) : (
                            <span>{m.title}</span>
                          )}
                        </span>
                        {isTeacher && (
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={delMaterial.isPending}
                            onClick={() => onDeleteMaterial(m.id)}
                          >
                            {t("delete")}
                          </Button>
                        )}
                      </li>
                    );
                  })}
                </ul>
              </div>

              {isTeacher && (
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
                    onClick={onDeleteLesson}
                  >
                    {t("delete")}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {isTeacher && edit && detail && (
        <LessonDialog
          sectionId={sectionId}
          lesson={detail}
          open
          onOpenChange={(o) => !o && setEdit(false)}
        />
      )}
      {isTeacher && addMaterial && (
        <AddMaterialDialog
          lessonId={lesson.id}
          open
          onOpenChange={(o) => !o && setAddMaterial(false)}
        />
      )}
    </div>
  );
}
