"use client";

import { isSameDay } from "date-fns";
import { CalendarDays, Clock3, Download, Video } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useListMyGroups, useListMySchedule } from "@/lib/api/enrollment/enrollment/enrollment";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { downloadIcs } from "@/lib/ics";
import { safeHref } from "@/lib/utils";

export function ScheduleView() {
  const t = useTranslations("schedule");
  const locale = useLocale();
  const isStaff = useHasRole(Role.TEACHER);
  const { data: lessons, isLoading, isError, refetch } = useListMySchedule();
  const { data: groups } = useListMyGroups();
  const [selectedDay, setSelectedDay] = useState<Date | undefined>(new Date());
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

  const groupName = (groupId: string) => groups?.find((item) => item.id === groupId)?.name;
  const lessonDays = agenda.map((lesson) => new Date(lesson.scheduledAt));
  const dayLessons = selectedDay
    ? agenda.filter((lesson) => isSameDay(new Date(lesson.scheduledAt), selectedDay))
    : [];

  function exportIcs() {
    // ponytail: lessons carry no duration — export every lesson as a 60-minute event.
    downloadIcs(
      "codillas-schedule.ics",
      agenda.map((lesson) => ({
        id: lesson.id,
        title: lesson.title,
        start: lesson.scheduledAt,
        durationMinutes: 60,
        description: groupName(lesson.groupId),
        url: lesson.meetLink ? (safeHref(lesson.meetLink) ?? undefined) : undefined,
      })),
    );
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t(isStaff ? "staffDescription" : "description")}
        action={
          agenda.length > 0 ? (
            <Button variant="outline" onClick={exportIcs}>
              <Download className="size-4" />
              {t("exportIcs")}
            </Button>
          ) : undefined
        }
      />

      <DataState
        isLoading={isLoading}
        isError={isError}
        isEmpty={agenda.length === 0}
        emptyMessage={t(isStaff ? "staffEmpty" : "empty")}
        onRetry={refetch}
      >
        <Tabs defaultValue="agenda">
          <TabsList className="mb-3">
            <TabsTrigger value="agenda">{t("agendaView")}</TabsTrigger>
            <TabsTrigger value="month">{t("monthView")}</TabsTrigger>
          </TabsList>

          <TabsContent value="agenda">
            <div className="grid gap-5">
              {Object.entries(grouped).map(([date, entries]) => (
                <section key={date}>
                  <h2 className="mb-2.5 flex items-center gap-2 font-semibold text-sm capitalize">
                    <CalendarDays className="size-4 text-primary" />
                    {date}
                  </h2>
                  <Card className="gap-0 overflow-hidden py-0">
                    <CardContent className="px-0">
                      {entries.map((lesson, index) => {
                        const meetingHref = lesson.meetLink ? safeHref(lesson.meetLink) : undefined;
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
                                {groupName(lesson.groupId) ?? "—"}
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
                              <span className="text-muted-foreground text-xs">
                                {t("linkPending")}
                              </span>
                            )}
                            {index < entries.length - 1 && (
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
          </TabsContent>

          <TabsContent value="month">
            <div className="grid gap-4 lg:grid-cols-[auto_minmax(0,1fr)] lg:items-start">
              <Card className="w-fit py-2">
                <CardContent className="px-2">
                  <Calendar
                    mode="single"
                    selected={selectedDay}
                    onSelect={setSelectedDay}
                    modifiers={{ hasLesson: lessonDays }}
                    modifiersClassNames={{
                      hasLesson:
                        "[&>button]:after:absolute [&>button]:after:bottom-1 [&>button]:after:left-1/2 [&>button]:after:-translate-x-1/2 [&>button]:after:size-1 [&>button]:after:rounded-full [&>button]:after:bg-primary [&>button]:relative",
                    }}
                  />
                </CardContent>
              </Card>

              <Card>
                <CardContent className="pt-6">
                  <h2 className="mb-3 flex items-center gap-2 font-semibold text-sm capitalize">
                    <CalendarDays className="size-4 text-primary" />
                    {selectedDay ? dateFormatter.format(selectedDay) : t("pickDay")}
                  </h2>
                  {dayLessons.length === 0 ? (
                    <p className="text-muted-foreground text-sm">{t("noLessonsThatDay")}</p>
                  ) : (
                    <div className="grid gap-2">
                      {dayLessons.map((lesson) => {
                        const meetingHref = lesson.meetLink ? safeHref(lesson.meetLink) : undefined;
                        return (
                          <div
                            key={lesson.id}
                            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-muted/20 p-3"
                          >
                            <div className="min-w-0">
                              <p className="flex items-center gap-2 font-medium text-sm">
                                <Clock3 className="size-4 text-primary" />
                                {timeFormatter.format(new Date(lesson.scheduledAt))} ·{" "}
                                {lesson.title}
                              </p>
                              <p className="mt-0.5 text-muted-foreground text-xs">
                                {groupName(lesson.groupId) ?? "—"}
                              </p>
                            </div>
                            {meetingHref && (
                              <Button asChild size="sm" variant="outline">
                                <a href={meetingHref} target="_blank" rel="noreferrer">
                                  <Video className="size-4" />
                                  {t("joinMeeting")}
                                </a>
                              </Button>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </DataState>
    </>
  );
}
