"use client";

import { useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
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
import { useListMyGroups } from "@/lib/api/enrollment/enrollment/enrollment";

export function MyCoursesView() {
  const t = useTranslations("myCourses");
  const { data, isLoading, isError } = useListMyGroups();

  const groups = data ?? [];

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={groups.length === 0}
            emptyMessage={t("empty")}
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("group")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((g) => (
                  <TableRow key={g.id}>
                    <TableCell className="font-medium">
                      <Link href={`/dashboard/courses/${g.courseId}`} className="hover:underline">
                        {g.name}
                      </Link>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
