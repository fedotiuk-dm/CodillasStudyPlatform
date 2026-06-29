"use client";

import { useTranslations } from "next-intl";

import { PageHeader } from "@/components/shared/page-header";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

import { GroupGradebook } from "./group-gradebook";
import { MyGradebook } from "./my-gradebook";

export function GradebookView() {
  const t = useTranslations("gradebook");
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);

  return (
    <>
      <PageHeader title={t("title")} description={t("description")} />
      <div className="grid gap-6">
        <MyGradebook />
        {canManage && <GroupGradebook />}
      </div>
    </>
  );
}
