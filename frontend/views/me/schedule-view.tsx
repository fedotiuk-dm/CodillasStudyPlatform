"use client";

import { CalendarDays, Clock3, Video } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useListMyGroups, useListMySchedule } from "@/lib/api/enrollment/enrollment/enrollment";
import { safeHref } from "@/lib/utils";

export function ScheduleView() {
  const t = useTranslations("schedule");
  const locale = useLocale();
  const { data: lessons, isLoading, isError } = useListMySchedule();
  const { data: groups } = useListMyGroups();
  const agenda = lessons ?? [];
  const dateFormatter = new Intl.DateTimeFormat(locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  const timeFormatter = new Intl.DateTimeFormat(locale, { hour: "2-digit", minute: "2-digit" });
  const grouped = agenda.reduce<Record<string, typeof agenda>>((result, lesson) => {
    const label = dateFormatter.format(new Date(lesson.scheduledAt));
    result[label] = [...(result[label] ?? []), lesson];
    return result;
  }, {});

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />

      <DataState
        isLoading={isLoading}
        isError={isError}
        isEmpty={agenda.length === 0}
        emptyMessage={t("empty")}
      >
        <div className="grid gap-5">
          {Object.entries(grouped).map(([date, dayLessons]) => (
            <section key={date}>
              <h2 className="mb-2.5 flex items-center gap-2 font-semibold text-sm capitalize">
                <CalendarDays className="size-4 text-primary" />
                {date}
              </h2>
              <Card className="gap-0 overflow-hidden py-0">
                <CardContent className="px-0">
                  {dayLessons.map((lesson, index) => {
                    const meetingHref = lesson.meetLink ? safeHref(lesson.meetLink) : undefined;
                    const group = groups?.find((item) => item.id === lesson.groupId);
                    return (
                      <div
                        key={lesson.id}
                        className="grid gap-3 px-4 py-4 sm:grid-cols-[5.5rem_minmax(0,1fr)_auto] sm:items-center sm:px-5"
                      >
                        <div className="flex items-center gap-2 font-semibold text-primary text-sm">
                          <Clock3 className="size-4" />
                          {timeFormatter.format(new Date(lesson.scheduledAt))}
                        </div>
                        <div className="min-w-0 border-border sm:border-l sm:pl-5">
                          <p className="truncate font-medium">{lesson.title}</p>
                          <p className="mt-0.5 truncate text-muted-foreground text-sm">
                            {group?.name ?? "—"}
                          </p>
                        </div>
                        {meetingHref ? (
                          <Button asChild size="sm" className="w-full sm:w-auto">
                            <a href={meetingHref} target="_blank" rel="noreferrer">
                              <Video className="size-4" />
                              {t("joinMeeting")}
                            </a>
                          </Button>
                        ) : (
                          <span className="text-muted-foreground text-xs">{t("linkPending")}</span>
                        )}
                        {index < dayLessons.length - 1 && (
                          <div className="col-span-full -mx-4 border-b sm:-mx-5" />
                        )}
                      </div>
                    );
                  })}
                </CardContent>
              </Card>
            </section>
          ))}
        </div>
      </DataState>
    </>
  );
}
