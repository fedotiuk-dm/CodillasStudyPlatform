"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useGetCourse } from "@/lib/api/course/course/course";
import { CourseStatus } from "@/lib/api/course/model";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CourseDetailActions } from "./course-detail-actions";
import { SectionBlock } from "./section-block";
import { SectionDialog } from "./section-dialog";

export function CourseDetailView({ courseId }: { courseId: string }) {
  const t = useTranslations("course");
  const tCourses = useTranslations("courses");
  const { data, isLoading, isError } = useGetCourse(courseId);
  const isAdmin = useHasRole(Role.ADMIN);
  const isTeacher = useHasRole(Role.TEACHER);
  const [createSection, setCreateSection] = useState(false);
  const [renameSection, setRenameSection] = useState<{ id: string; title: string } | null>(null);

  // Archived courses are read-only: hide every edit/add affordance.
  const canEdit = isTeacher && data?.status !== CourseStatus.ARCHIVED;
  const sections = [...(data?.sections ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <>
      <PageHeader
        title={data?.name ?? t("course")}
        description={data?.description ?? undefined}
        action={
          data ? (
            <div className="flex flex-wrap items-center justify-end gap-2">
              <StatusBadge status={data.status} label={tCourses(`status.${data.status}`)} />
              {isAdmin && <CourseDetailActions course={data} />}
              {canEdit && <Button onClick={() => setCreateSection(true)}>{t("addSection")}</Button>}
            </div>
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
                  canEdit={canEdit}
                  onRename={() => setRenameSection({ id: section.id, title: section.title })}
                />
              ))}
            </div>
          </DataState>
        </CardContent>
      </Card>

      {canEdit && createSection && (
        <SectionDialog
          courseId={courseId}
          open
          onOpenChange={(o) => !o && setCreateSection(false)}
        />
      )}
      {canEdit && renameSection && (
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
