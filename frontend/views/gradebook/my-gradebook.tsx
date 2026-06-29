"use client";

import { useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useGetStudentGradebook } from "@/lib/api/gradebook/gradebook/gradebook";
import { useKeycloak } from "@/lib/auth";

import { CourseGradeCard } from "./course-grade-card";

/** The signed-in student's own gradebook: the weighted course grade card + the per-item entries. */
export function MyGradebook() {
  const t = useTranslations("gradebook");
  const { userId } = useKeycloak();
  const { data, isLoading, isError, refetch } = useGetStudentGradebook(userId ?? "", {
    query: { enabled: !!userId },
  });
  const entries = data?.entries ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{t("myGrades")}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {data?.courseGrade && <CourseGradeCard grade={data.courseGrade} />}
        <DataState
          isLoading={isLoading || !userId}
          isError={isError}
          isEmpty={entries.length === 0}
          emptyMessage={t("noGrades")}
          variant="table"
          onRetry={refetch}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("source")}</TableHead>
                <TableHead className="text-right">{t("score")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.map((e) => (
                <TableRow key={e.id}>
                  <TableCell>
                    <Badge variant="secondary">{e.source}</Badge>
                  </TableCell>
                  <TableCell className="text-right font-medium">{e.score}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DataState>
      </CardContent>
    </Card>
  );
}
