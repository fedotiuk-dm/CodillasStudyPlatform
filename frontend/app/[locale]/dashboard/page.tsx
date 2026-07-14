"use client";

import {
  ArrowRight,
  Bell,
  BookOpen,
  CalendarDays,
  Clock3,
  GraduationCap,
  Megaphone,
  MessageSquare,
  Users,
} from "lucide-react";
import { useLocale, useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Link } from "@/i18n/navigation";
import { useListMyAnnouncements } from "@/lib/api/announcement/announcement/announcement";
import { useListMyRooms } from "@/lib/api/chat/chat/chat";
import { useListCourses } from "@/lib/api/course/course/course";
import {
  useListGroups,
  useListMyGroups,
  useListMySchedule,
} from "@/lib/api/enrollment/enrollment/enrollment";
import { useListMyNotifications } from "@/lib/api/notification/notification/notification";
import { useListProfiles } from "@/lib/api/user/user/user";
import { useKeycloak } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { safeHref } from "@/lib/utils";

function MetricCard({
  icon: Icon,
  label,
  value,
  href,
}: {
  icon: typeof BookOpen;
  label: string;
  value: number | undefined;
  href: string;
}) {
  return (
    <Link href={href} className="group">
      <Card className="h-full gap-4 py-5 transition-all hover:-translate-y-0.5 hover:border-primary/25 hover:shadow-md">
        <CardContent className="flex items-center gap-4">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
            <Icon className="size-5" />
          </span>
          <div className="min-w-0">
            <p className="text-muted-foreground text-sm">{label}</p>
            <p className="mt-0.5 font-semibold text-2xl tracking-tight">{value ?? "—"}</p>
          </div>
          <ArrowRight className="ml-auto size-4 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
        </CardContent>
      </Card>
    </Link>
  );
}

export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const locale = useLocale();
  const { name, roles } = useKeycloak();
  const isAdmin = roles.includes(Role.ADMIN);
  const isTeacher = roles.includes(Role.TEACHER);
  const canManage = isAdmin || isTeacher;

  const { data: myGroups } = useListMyGroups();
  const { data: schedule } = useListMySchedule();
  const { data: announcements } = useListMyAnnouncements({ size: 3 });
  const { data: notifications } = useListMyNotifications({ size: 5 });
  const { data: rooms } = useListMyRooms();
  const { data: groups } = useListGroups(undefined, { query: { enabled: canManage } });
  const { data: courses } = useListCourses(undefined, { query: { enabled: isAdmin } });
  const { data: profiles } = useListProfiles(undefined, { query: { enabled: isAdmin } });

  const upcomingLessons = schedule?.filter(
    (lesson) => new Date(lesson.scheduledAt).getTime() >= Date.now(),
  );
  const upcomingLesson = upcomingLessons?.[0];
  const upcomingGroup = myGroups?.find((group) => group.id === upcomingLesson?.groupId);
  const meetHref = upcomingLesson?.meetLink ? safeHref(upcomingLesson.meetLink) : undefined;
  const dateTime = upcomingLesson
    ? new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" }).format(
        new Date(upcomingLesson.scheduledAt),
      )
    : undefined;

  return (
    <div className="grid gap-6">
      <section className="relative overflow-hidden rounded-2xl border border-primary/15 bg-gradient-to-br from-primary via-primary to-info px-5 py-7 text-primary-foreground shadow-lg shadow-primary/10 sm:px-8 sm:py-9">
        <div className="absolute -top-20 -right-16 size-64 rounded-full border border-white/15 bg-white/10" />
        <div className="absolute -right-4 -bottom-24 size-48 rounded-full border border-white/10" />
        <div className="relative max-w-2xl">
          <p className="font-medium text-sm text-white/75">{t("eyebrow")}</p>
          <h1 className="mt-2 text-balance font-semibold text-3xl tracking-tight sm:text-4xl">
            {t("welcome", { name: name?.split(" ")[0] ?? t("learner") })}
          </h1>
          <p className="mt-3 max-w-xl text-pretty text-sm text-white/80 sm:text-base">
            {canManage ? t("staffIntro") : t("studentIntro")}
          </p>
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="font-semibold text-lg tracking-tight">{t("overview")}</h2>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            icon={GraduationCap}
            label={t("myCourses")}
            value={myGroups?.length}
            href="/dashboard/my-courses"
          />
          <MetricCard
            icon={CalendarDays}
            label={t("upcomingLessons")}
            value={upcomingLessons?.length}
            href="/dashboard/schedule"
          />
          <MetricCard
            icon={Bell}
            label={t("unreadNotifications")}
            value={notifications?.unread}
            href="/dashboard/notifications"
          />
          {canManage ? (
            <MetricCard
              icon={Users}
              label={t("groups")}
              value={groups?.totalElements}
              href="/dashboard/groups"
            />
          ) : (
            <MetricCard
              icon={MessageSquare}
              label={t("messages")}
              value={rooms?.length}
              href="/dashboard/chat"
            />
          )}
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(20rem,0.65fr)]">
        <Card>
          <CardHeader className="flex-row items-center justify-between gap-4">
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-[0.14em]">
                {t("nextUp")}
              </p>
              <CardTitle className="mt-1.5 text-lg">{t("nextLesson")}</CardTitle>
            </div>
            <span className="grid size-10 place-items-center rounded-xl bg-info/10 text-info">
              <Clock3 className="size-5" />
            </span>
          </CardHeader>
          <CardContent>
            {upcomingLesson ? (
              <div className="flex flex-col gap-4 rounded-xl border bg-muted/20 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-semibold">{upcomingLesson.title}</p>
                  <p className="mt-1 text-muted-foreground text-sm">
                    {dateTime} {upcomingGroup ? `· ${upcomingGroup.name}` : ""}
                  </p>
                </div>
                {meetHref ? (
                  <Button asChild>
                    <a href={meetHref} target="_blank" rel="noreferrer">
                      {t("joinLesson")}
                      <ArrowRight className="size-4" />
                    </a>
                  </Button>
                ) : (
                  <Button asChild variant="outline">
                    <Link href="/dashboard/schedule">{t("viewSchedule")}</Link>
                  </Button>
                )}
              </div>
            ) : (
              <div className="rounded-xl border border-dashed bg-muted/20 p-6 text-center">
                <p className="font-medium">{t("noUpcoming")}</p>
                <p className="mt-1 text-muted-foreground text-sm">{t("noUpcomingHint")}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t("quickActions")}</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {(canManage
              ? [
                  { href: "/dashboard/homework", label: t("reviewHomework"), icon: BookOpen },
                  { href: "/dashboard/groups", label: t("manageGroups"), icon: Users },
                  { href: "/dashboard/schedule", label: t("openSchedule"), icon: CalendarDays },
                ]
              : [
                  { href: "/dashboard/homework", label: t("openHomework"), icon: BookOpen },
                  { href: "/dashboard/tests", label: t("openTests"), icon: GraduationCap },
                  { href: "/dashboard/chat", label: t("openChat"), icon: MessageSquare },
                ]
            ).map((action) => (
              <Button key={action.href} asChild variant="ghost" className="h-11 justify-start px-3">
                <Link href={action.href}>
                  <action.icon className="size-4 text-primary" />
                  {action.label}
                  <ArrowRight className="ml-auto size-4 text-muted-foreground" />
                </Link>
              </Button>
            ))}
          </CardContent>
        </Card>
      </section>

      {(announcements?.content?.length ?? 0) > 0 && (
        <Card>
          <CardHeader className="flex-row items-center justify-between gap-4">
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-[0.14em]">
                {t("fromYourGroups")}
              </p>
              <CardTitle className="mt-1.5 text-lg">{t("latestAnnouncements")}</CardTitle>
            </div>
            <span className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary">
              <Megaphone className="size-5" />
            </span>
          </CardHeader>
          <CardContent className="grid gap-2">
            {announcements?.content?.map((a) => (
              <Link
                key={a.id}
                href={`/dashboard/announcements?group=${a.groupId}`}
                className="rounded-xl border bg-muted/20 p-3 transition-colors hover:border-primary/25"
              >
                <p className="font-medium text-sm">{a.title}</p>
                {a.body && (
                  <p className="mt-0.5 line-clamp-2 text-muted-foreground text-xs">{a.body}</p>
                )}
              </Link>
            ))}
            <Button asChild variant="ghost" size="sm" className="justify-start px-3">
              <Link href="/dashboard/announcements">
                {t("allAnnouncements")}
                <ArrowRight className="size-4" />
              </Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {isAdmin && (
        <section>
          <h2 className="mb-3 font-semibold text-lg tracking-tight">{t("schoolOverview")}</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            <MetricCard
              icon={BookOpen}
              label={t("courses")}
              value={courses?.totalElements}
              href="/dashboard/courses"
            />
            <MetricCard
              icon={Users}
              label={t("groups")}
              value={groups?.totalElements}
              href="/dashboard/groups"
            />
            <MetricCard
              icon={GraduationCap}
              label={t("people")}
              value={profiles?.totalElements}
              href="/dashboard/people"
            />
          </div>
        </section>
      )}
    </div>
  );
}
