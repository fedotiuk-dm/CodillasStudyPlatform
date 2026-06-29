"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";

import { DataState } from "@/components/shared/data-state";
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
import { useGetGroupGradebook } from "@/lib/api/gradebook/gradebook/gradebook";
import { useProfileNames } from "@/lib/hooks/use-profile-names";

const csvCell = (v: string | number) => {
  const s = String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

/** A teacher's group gradebook: per-student raw entries + the weighted final grade, with CSV export. */
export function GroupGradebook() {
  const t = useTranslations("gradebook");
  const { data: groupsData } = useListGroups();
  const groups = groupsData?.content ?? [];
  const [groupId, setGroupId] = useState("");
  const nameOf = useProfileNames();
  const { data, isLoading, isError } = useGetGroupGradebook(groupId, {
    query: { enabled: !!groupId },
  });
  const students = data?.students ?? [];

  const rows = students.map((s) => ({
    studentId: s.studentId,
    name: nameOf(s.studentId),
    entries: s.entries.length,
    total: s.entries.reduce((sum, e) => sum + e.score, 0),
    final: s.courseGrade ? Math.round(s.courseGrade.percent) : null,
  }));
  const graded = rows.filter((r) => r.final !== null);
  const groupAverage = graded.length
    ? Math.round(graded.reduce((sum, r) => sum + (r.final ?? 0), 0) / graded.length)
    : 0;

  function exportCsv() {
    const groupName = groups.find((g) => g.id === groupId)?.name ?? "group";
    const header = [t("student"), t("entries"), t("total"), t("finalPct")];
    const lines = rows.map((r) =>
      [r.name, r.entries, r.total, r.final ?? ""].map(csvCell).join(","),
    );
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
        <CardTitle className="text-base">{t("groupGrades")}</CardTitle>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" disabled={rows.length === 0} onClick={exportCsv}>
            {t("exportCsv")}
          </Button>
          <Select value={groupId} onValueChange={setGroupId}>
            <SelectTrigger className="w-48">
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
      </CardHeader>
      <CardContent>
        {groupId && (
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={students.length === 0}
            emptyMessage={t("noGroupGrades")}
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("student")}</TableHead>
                  <TableHead className="text-right">{t("entries")}</TableHead>
                  <TableHead className="text-right">{t("total")}</TableHead>
                  <TableHead className="text-right">{t("finalPct")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => (
                  <TableRow key={r.studentId}>
                    <TableCell>{r.name}</TableCell>
                    <TableCell className="text-right">{r.entries}</TableCell>
                    <TableCell className="text-right">{r.total}</TableCell>
                    <TableCell className="text-right font-medium">
                      {r.final === null ? "—" : `${r.final}%`}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <p className="mt-3 text-muted-foreground text-sm">
              {t("groupAverage")}:{" "}
              <span className="font-medium text-foreground">{groupAverage}%</span> ·{" "}
              {t("studentsCount", { count: rows.length })}
            </p>
          </DataState>
        )}
      </CardContent>
    </Card>
  );
}
