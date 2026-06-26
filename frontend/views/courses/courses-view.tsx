"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Link } from "@/i18n/navigation";
import { useListCourses } from "@/lib/api/course/course/course";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateCourseDialog } from "./create-course-dialog";

export function CoursesView() {
  const t = useTranslations("courses");
  const { data, isLoading, isError } = useListCourses();
  const canManage = useHasRole(Role.ADMIN);
  const [open, setOpen] = useState(false);

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
        <CardContent className="pt-6">
          <DataState isLoading={isLoading} isError={isError} isEmpty={courses.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("descriptionLabel")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {courses.map((course) => (
                  <TableRow key={course.id}>
                    <TableCell className="font-medium">
                      <Link href={`/dashboard/courses/${course.id}`} className="hover:underline">
                        {course.name}
                      </Link>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {course.description ?? "—"}
                    </TableCell>
                  </TableRow>
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
