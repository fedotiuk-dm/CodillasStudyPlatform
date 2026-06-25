"use client";

import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useListCourses } from "@/lib/api/course/course/course";
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateGroupDialog } from "./create-group-dialog";
import { GroupDetailDialog } from "./group-detail-dialog";

export function GroupsView() {
  const { data, isLoading, isError } = useListGroups();
  const { data: coursesData } = useListCourses();
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [open, setOpen] = useState(false);
  const [detail, setDetail] = useState<{ id: string; name: string } | null>(null);

  const groups = data?.content ?? [];
  const courseName = (id: string) => coursesData?.content?.find((c) => c.id === id)?.name ?? "—";

  return (
    <>
      <PageHeader
        title="Groups"
        description="Cohorts running a course on a schedule."
        action={canManage ? <Button onClick={() => setOpen(true)}>New group</Button> : undefined}
      />

      <Card>
        <CardContent className="pt-6">
          <DataState isLoading={isLoading} isError={isError} isEmpty={groups.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>Start date</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((g) => (
                  <TableRow key={g.id}>
                    <TableCell className="font-medium">{g.name}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {courseName(g.courseId)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{g.startDate ?? "—"}</TableCell>
                    <TableCell className="text-right">
                      {canManage && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setDetail({ id: g.id, name: g.name })}
                        >
                          Manage
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>

      <CreateGroupDialog open={open} onOpenChange={setOpen} />

      {detail && (
        <GroupDetailDialog
          groupId={detail.id}
          groupName={detail.name}
          open={!!detail}
          onOpenChange={(o) => !o && setDetail(null)}
        />
      )}
    </>
  );
}
