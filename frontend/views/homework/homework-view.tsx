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
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import { useListAssignments } from "@/lib/api/homework/homework/homework";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

import { AssignmentRow } from "./assignment-row";
import { CreateAssignmentDialog } from "./create-assignment-dialog";

export function HomeworkView() {
  const t = useTranslations("homework");
  const { data: groupsData } = useListGroups();
  const groups = groupsData?.content ?? [];
  const [groupId, setGroupId] = useState("");
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [open, setOpen] = useState(false);

  const { data, isLoading, isError, refetch } = useListAssignments(
    { groupId },
    { query: { enabled: !!groupId } },
  );
  const assignments = data?.content ?? [];

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          canManage && groupId ? (
            <Button onClick={() => setOpen(true)}>{t("newAssignment")}</Button>
          ) : undefined
        }
      />

      <div className="mb-4 max-w-xs">
        <Select value={groupId} onValueChange={setGroupId}>
          <SelectTrigger>
            <SelectValue placeholder={t("selectGroup")} />
          </SelectTrigger>
          <SelectContent>
            {groups.map((g) => (
              <SelectItem key={g.id} value={g.id}>
                {g.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {groupId && (
        <Card>
          <CardContent className="pt-6">
            <DataState
              isLoading={isLoading}
              isError={isError}
              isEmpty={assignments.length === 0}
              variant="table"
              onRetry={refetch}
            >
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("titleLabel")}</TableHead>
                    <TableHead>{t("status")}</TableHead>
                    <TableHead>{t("due")}</TableHead>
                    <TableHead className="text-right">{t("actions")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assignments.map((a) => (
                    <AssignmentRow key={a.id} assignment={a} canManage={canManage} />
                  ))}
                </TableBody>
              </Table>
            </DataState>
          </CardContent>
        </Card>
      )}

      {groupId && <CreateAssignmentDialog groupId={groupId} open={open} onOpenChange={setOpen} />}
    </>
  );
}
