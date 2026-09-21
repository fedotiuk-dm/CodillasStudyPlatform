"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useListGroupAnnouncements,
  useListMyAnnouncements,
} from "@/lib/api/announcement/announcement/announcement";
import { useListGroups, useListMyGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { useProfileNames } from "@/lib/hooks/use-profile-names";

import { AnnouncementCard } from "./announcement-card";
import { CreateAnnouncementDialog } from "./create-announcement-dialog";

const ALL = "all";

export function AnnouncementsView() {
  const t = useTranslations("announcements");
  const isStaff = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  // Deep-link: /dashboard/announcements?group={id} (e.g. clicked from a notification).
  const groupParam = useSearchParams().get("group");
  const [groupId, setGroupId] = useState(groupParam ?? ALL);
  const [open, setOpen] = useState(false);
  const nameOf = useProfileNames();

  const { data: allGroups } = useListGroups(undefined, { query: { enabled: isStaff } });
  const { data: myGroups } = useListMyGroups({ query: { enabled: !isStaff } });
  const groups = isStaff ? (allGroups?.content ?? []) : (myGroups ?? []);

  const myFeed = useListMyAnnouncements(undefined, { query: { enabled: groupId === ALL } });
  const groupFeed = useListGroupAnnouncements(groupId, undefined, {
    query: { enabled: groupId !== ALL },
  });
  const active = groupId === ALL ? myFeed : groupFeed;
  const items = active.data?.content ?? [];

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t(isStaff ? "staffDescription" : "description")}
        action={
          isStaff ? (
            <Button
              disabled={groupId === ALL}
              title={groupId === ALL ? t("pickGroupFirst") : undefined}
              onClick={() => setOpen(true)}
            >
              {t("newAnnouncement")}
            </Button>
          ) : undefined
        }
      />

      <div className="mb-4 max-w-xs">
        <Select value={groupId} onValueChange={setGroupId}>
          <SelectTrigger>
            <SelectValue placeholder={t("selectGroup")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>{t(isStaff ? "allGroupsStaff" : "allGroups")}</SelectItem>
            {groups.map((g) => (
              <SelectItem key={g.id} value={g.id}>
                {g.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <DataState
        isLoading={active.isLoading}
        isError={active.isError}
        isEmpty={items.length === 0}
        onRetry={active.refetch}
      >
        <div className="grid gap-3">
          {items.map((a) => (
            <AnnouncementCard key={a.id} announcement={a} authorName={nameOf(a.authorId)} />
          ))}
        </div>
      </DataState>

      {groupId !== ALL && (
        <CreateAnnouncementDialog groupId={groupId} open={open} onOpenChange={setOpen} />
      )}
    </>
  );
}
