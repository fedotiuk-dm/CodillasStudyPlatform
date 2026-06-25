"use client";

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
import { useListCourses } from "@/lib/api/course/course/course";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateCourseDialog } from "./create-course-dialog";

export function CoursesView() {
  const { data, isLoading, isError } = useListCourses();
  const canManage = useHasRole(Role.ADMIN);
  const [open, setOpen] = useState(false);

  const courses = data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Courses"
        description="The catalogue of courses the school runs."
        action={canManage ? <Button onClick={() => setOpen(true)}>New course</Button> : undefined}
      />

      <Card>
        <CardContent className="pt-6">
          <DataState isLoading={isLoading} isError={isError} isEmpty={courses.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Description</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {courses.map((course) => (
                  <TableRow key={course.id}>
                    <TableCell className="font-medium">{course.name}</TableCell>
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
