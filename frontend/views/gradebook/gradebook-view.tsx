"use client";

import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { useProfileNames } from "@/lib/hooks/use-profile-names";

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

const csvCell = (v: string | number) => {
  const s = String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

function GroupGradebook() {
  const { data: groupsData } = useListGroups();
  const groups = groupsData?.content ?? [];
  const [groupId, setGroupId] = useState("");
  const nameOf = useProfileNames();
  const { data, isLoading, isError } = useGetGroupGradebook(groupId, {
    query: { enabled: !!groupId },
  });
  const students = data?.students ?? [];

  const rows = students.map((s) => {
    const total = s.entries.reduce((sum, e) => sum + e.score, 0);
    return {
      studentId: s.studentId,
      name: nameOf(s.studentId),
      entries: s.entries.length,
      total,
      average: s.entries.length ? Math.round(total / s.entries.length) : 0,
    };
  });
  const groupAverage = rows.length
    ? Math.round(rows.reduce((sum, r) => sum + r.average, 0) / rows.length)
    : 0;

  function exportCsv() {
    const groupName = groups.find((g) => g.id === groupId)?.name ?? "group";
    const header = ["Student", "Entries", "Total", "Average"];
    const lines = rows.map((r) => [r.name, r.entries, r.total, r.average].map(csvCell).join(","));
    const csv = [header.join(","), ...lines].join("\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = `gradebook-${groupName}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-4 space-y-0">
        <CardTitle className="text-base">Group grades</CardTitle>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" disabled={rows.length === 0} onClick={exportCsv}>
            Export CSV
          </Button>
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
        </div>
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
                  <TableHead className="text-right">Average</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => (
                  <TableRow key={r.studentId}>
                    <TableCell>{r.name}</TableCell>
                    <TableCell className="text-right">{r.entries}</TableCell>
                    <TableCell className="text-right">{r.total}</TableCell>
                    <TableCell className="text-right font-medium">{r.average}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <p className="mt-3 text-muted-foreground text-sm">
              Group average: <span className="font-medium text-foreground">{groupAverage}</span> ·{" "}
              {rows.length} students
            </p>
          </DataState>
        )}
      </CardContent>
    </Card>
  );
}
