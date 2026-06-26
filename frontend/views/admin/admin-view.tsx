"use client";

import { useTranslations } from "next-intl";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Link } from "@/i18n/navigation";
import { useListCourses } from "@/lib/api/course/course/course";
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import { useListProfiles } from "@/lib/api/user/user/user";
import { useHasRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

export function AdminView() {
  const t = useTranslations("admin");
  const isAdmin = useHasRole(Role.ADMIN);
  const courses = useListCourses();
  const groups = useListGroups();
  const people = useListProfiles();

  if (!isAdmin) {
    return <PageHeader title={t("title")} description={t("forbidden")} />;
  }

  const stats = [
    { label: t("courses"), value: courses.data?.totalElements, href: "/dashboard/courses" },
    { label: t("groups"), value: groups.data?.totalElements, href: "/dashboard/groups" },
    { label: t("people"), value: people.data?.totalElements, href: "/dashboard/people" },
  ];

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />
      <div className="grid gap-4 sm:grid-cols-3">
        {stats.map((s) => (
          <Link key={s.href} href={s.href}>
            <Card className="transition-colors hover:border-primary">
              <CardHeader>
                <CardTitle className="font-medium text-muted-foreground text-sm">
                  {s.label}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="font-semibold text-3xl">{s.value ?? "—"}</p>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </>
  );
}
