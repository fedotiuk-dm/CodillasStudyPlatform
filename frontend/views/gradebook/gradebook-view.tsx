"use client";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useGetStudentGradebook } from "@/lib/api/gradebook/gradebook/gradebook";
import { useKeycloak } from "@/lib/auth";

export function GradebookView() {
  const { userId } = useKeycloak();
  const { data, isLoading, isError } = useGetStudentGradebook(userId ?? "", {
    query: { enabled: !!userId },
  });

  const entries = data?.entries ?? [];

  return (
    <>
      <PageHeader title="Gradebook" description="Your graded homework and tests." />
      <Card>
        <CardContent className="pt-6">
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
    </>
  );
}
