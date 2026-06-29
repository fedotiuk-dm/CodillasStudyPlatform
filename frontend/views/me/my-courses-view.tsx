"use client";

import { ArrowRight, BookOpen, CalendarDays } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Link } from "@/i18n/navigation";
import { useListMyGroups } from "@/lib/api/enrollment/enrollment/enrollment";

export function MyCoursesView() {
  const t = useTranslations("myCourses");
  const locale = useLocale();
  const { data, isLoading, isError, refetch } = useListMyGroups();
  const groups = data ?? [];

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />

      <DataState
        isLoading={isLoading}
        isError={isError}
        isEmpty={groups.length === 0}
        emptyMessage={t("empty")}
        variant="cards"
        onRetry={refetch}
      >
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {groups.map((group) => (
            <Link key={group.id} href={`/dashboard/courses/${group.courseId}`} className="group">
              <Card className="h-full overflow-hidden pt-0 transition-all hover:-translate-y-0.5 hover:border-primary/25 hover:shadow-md">
                <div className="h-1.5 bg-gradient-to-r from-primary to-info" />
                <CardHeader className="pt-5">
                  <div className="flex items-start justify-between gap-3">
                    <span className="grid size-11 place-items-center rounded-xl bg-primary/10 text-primary">
                      <BookOpen className="size-5" />
                    </span>
                    <StatusBadge status={group.status} label={t(`status.${group.status}`)} />
                  </div>
                  <CardTitle className="mt-3 text-lg leading-snug">{group.name}</CardTitle>
                </CardHeader>
                <CardContent className="mt-auto flex items-end justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-2 text-muted-foreground text-sm">
                    <CalendarDays className="size-4 shrink-0" />
                    <span className="truncate">
                      {group.startDate
                        ? new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(
                            new Date(group.startDate),
                          )
                        : t("startNotSet")}
                    </span>
                  </div>
                  <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </DataState>
    </>
  );
}
