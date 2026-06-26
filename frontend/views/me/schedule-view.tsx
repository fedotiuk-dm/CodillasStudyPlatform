"use client";

import { useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { useListMyGroups, useListMySchedule } from "@/lib/api/enrollment/enrollment/enrollment";
import { safeHref } from "@/lib/utils";

export function ScheduleView() {
  const t = useTranslations("schedule");
  const { data: lessons, isLoading, isError } = useListMySchedule();
  const { data: groups } = useListMyGroups();

  const groupName = (id: string) => groups?.find((g) => g.id === id)?.name ?? "—";
  const agenda = lessons ?? [];

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={agenda.length === 0}
            emptyMessage={t("empty")}
          >
            <ul className="grid gap-3">
              {agenda.map((lesson) => {
                const meetingHref = lesson.meetLink ? safeHref(lesson.meetLink) : undefined;
                return (
                  <li
                    key={lesson.id}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-lg border px-4 py-3"
                  >
                    <div className="grid gap-0.5">
                      <span className="font-medium">{lesson.title}</span>
                      <span className="text-muted-foreground text-sm">
                        {new Date(lesson.scheduledAt).toLocaleString()} ·{" "}
                        {groupName(lesson.groupId)}
                      </span>
                    </div>
                    {meetingHref && (
                      <a
                        className="text-primary text-sm underline"
                        href={meetingHref}
                        target="_blank"
                        rel="noreferrer"
                      >
                        {t("joinMeeting")}
                      </a>
                    )}
                  </li>
                );
              })}
            </ul>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
