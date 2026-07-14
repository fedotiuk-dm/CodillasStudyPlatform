"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useListCourses } from "@/lib/api/course/course/course";
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import { type GroupResponse, GroupStatus } from "@/lib/api/enrollment/model";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

import { CreateGroupDialog } from "./create-group-dialog";
import { GroupDetailDialog } from "./group-detail-dialog";
import { GroupRow } from "./group-row";

const ALL = "ALL";

export function GroupsView() {
  const t = useTranslations("groups");
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<string>(ALL);
  const [detail, setDetail] = useState<GroupResponse | null>(null);

  const { data, isLoading, isError, refetch } = useListGroups(
    status === ALL ? undefined : { status: status as GroupStatus },
  );
  const { data: coursesData } = useListCourses();

  const groups = data?.content ?? [];
  const courseName = (id: string) => coursesData?.content?.find((c) => c.id === id)?.name ?? "—";

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          canManage ? <Button onClick={() => setOpen(true)}>{t("newGroup")}</Button> : undefined
        }
      />

      <Card>
        <CardContent className="space-y-4 pt-6">
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{t("filterAll")}</SelectItem>
              <SelectItem value={GroupStatus.DRAFT}>{t("status.DRAFT")}</SelectItem>
              <SelectItem value={GroupStatus.RUNNING}>{t("status.RUNNING")}</SelectItem>
              <SelectItem value={GroupStatus.ARCHIVED}>{t("status.ARCHIVED")}</SelectItem>
            </SelectContent>
          </Select>

          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={groups.length === 0}
            variant="table"
            onRetry={refetch}
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("name")}</TableHead>
                  <TableHead>{t("course")}</TableHead>
                  <TableHead>{t("startDate")}</TableHead>
                  <TableHead>{t("statusLabel")}</TableHead>
                  <TableHead className="text-right">{t("actions")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((g) => (
                  <GroupRow
                    key={g.id}
                    group={g}
                    courseName={courseName(g.courseId)}
                    canManage={canManage}
                    onManage={() => setDetail(g)}
                  />
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>

      <CreateGroupDialog open={open} onOpenChange={setOpen} />

      {detail && (
        <GroupDetailDialog
          group={detail}
          open={!!detail}
          onOpenChange={(o) => !o && setDetail(null)}
        />
      )}
    </>
  );
}
