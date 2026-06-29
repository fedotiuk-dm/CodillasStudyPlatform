"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useListCourses } from "@/lib/api/course/course/course";
import { CourseStatus } from "@/lib/api/course/model";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

import { CourseRow } from "./course-row";
import { CreateCourseDialog } from "./create-course-dialog";

const ALL = "ALL";

export function CoursesView() {
  const t = useTranslations("courses");
  const canManage = useHasRole(Role.ADMIN);
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<string>(ALL);

  const { data, isLoading, isError } = useListCourses(
    status === ALL ? undefined : { status: status as CourseStatus },
  );
  const courses = data?.content ?? [];

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          canManage ? <Button onClick={() => setOpen(true)}>{t("newCourse")}</Button> : undefined
        }
      />

      <Card>
        <CardContent className="space-y-4 pt-6">
          {canManage && (
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>{t("filterAll")}</SelectItem>
                <SelectItem value={CourseStatus.DRAFT}>{t("status.DRAFT")}</SelectItem>
                <SelectItem value={CourseStatus.PUBLISHED}>{t("status.PUBLISHED")}</SelectItem>
                <SelectItem value={CourseStatus.ARCHIVED}>{t("status.ARCHIVED")}</SelectItem>
              </SelectContent>
            </Select>
          )}

          <DataState isLoading={isLoading} isError={isError} isEmpty={courses.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("descriptionLabel")}</TableHead>
                  <TableHead>{t("statusLabel")}</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {courses.map((course) => (
                  <CourseRow key={course.id} course={course} canManage={canManage} />
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>

      <CreateCourseDialog open={open} onOpenChange={setOpen} />
    </>
  );
}
