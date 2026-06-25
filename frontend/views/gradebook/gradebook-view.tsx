"use client";

import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import {
  useGetGroupGradebook,
  useGetStudentGradebook,
} from "@/lib/api/gradebook/gradebook/gradebook";
import { useHasAnyRole, useKeycloak } from "@/lib/auth";
import { Role } from "@/lib/constants";

export function GradebookView() {
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);

  return (
    <>
      <PageHeader title="Gradebook" description="Graded homework and tests." />
      <div className="grid gap-6">
        <MyGradebook />
        {canManage && <GroupGradebook />}
      </div>
    </>
  );
}

function MyGradebook() {
  const { userId } = useKeycloak();
  const { data, isLoading, isError } = useGetStudentGradebook(userId ?? "", {
    query: { enabled: !!userId },
  });
  const entries = data?.entries ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">My grades</CardTitle>
      </CardHeader>
      <CardContent>
        <DataState
          isLoading={isLoading || !userId}
          isError={isError}
          isEmpty={entries.length === 0}
          emptyMessage="No grades yet."
        >
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Source</TableHead>
                <TableHead className="text-right">Score</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.map((e) => (
                <TableRow key={e.id}>
                  <TableCell>
                    <Badge variant="secondary">{e.source}</Badge>
                  </TableCell>
                  <TableCell className="text-right font-medium">{e.score}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </DataState>
      </CardContent>
    </Card>
  );
}

function GroupGradebook() {
  const { data: groupsData } = useListGroups();
  const groups = groupsData?.content ?? [];
  const [groupId, setGroupId] = useState("");
  const { data, isLoading, isError } = useGetGroupGradebook(groupId, {
    query: { enabled: !!groupId },
  });
  const students = data?.students ?? [];

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-4 space-y-0">
        <CardTitle className="text-base">Group grades</CardTitle>
        <Select value={groupId} onValueChange={setGroupId}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Select a group" />
          </SelectTrigger>
          <SelectContent>
            {groups.map((g) => (
              <SelectItem key={g.id} value={g.id}>
                {g.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent>
        {groupId && (
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={students.length === 0}
            emptyMessage="No grades for this group yet."
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead className="text-right">Entries</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {students.map((s) => {
                  const total = s.entries.reduce((sum, e) => sum + e.score, 0);
                  return (
                    <TableRow key={s.studentId}>
                      <TableCell className="font-mono text-xs">{s.studentId}</TableCell>
                      <TableCell className="text-right">{s.entries.length}</TableCell>
                      <TableCell className="text-right font-medium">{total}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </DataState>
        )}
      </CardContent>
    </Card>
  );
}
