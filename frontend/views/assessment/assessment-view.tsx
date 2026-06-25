"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
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
import {
  getListTestsQueryKey,
  useListTests,
  usePublishTest,
} from "@/lib/api/assessment/assessment/assessment";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateTestDialog } from "./create-test-dialog";
import { ManageQuestionsDialog } from "./manage-questions-dialog";
import { TakeAttemptDialog } from "./take-attempt-dialog";

export function AssessmentView() {
  const { data, isLoading, isError } = useListTests();
  const queryClient = useQueryClient();
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [createOpen, setCreateOpen] = useState(false);
  const [manageTestId, setManageTestId] = useState<string | null>(null);
  const [takeTest, setTakeTest] = useState<{ id: string; title: string } | null>(null);
  const publish = usePublishTest();

  const tests = data?.content ?? [];
  const invalidate = () => queryClient.invalidateQueries({ queryKey: getListTestsQueryKey() });

  function onPublish(testId: string) {
    publish.mutate(
      { testId },
      {
        onSuccess: () => {
          toast.success("Test published");
          invalidate();
        },
        onError: () => toast.error("Could not publish — does it have questions?"),
      },
    );
  }

  return (
    <>
      <PageHeader
        title="Tests"
        description="Auto-graded tests and controls."
        action={
          canManage ? <Button onClick={() => setCreateOpen(true)}>New test</Button> : undefined
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState isLoading={isLoading} isError={isError} isEmpty={tests.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tests.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium">{t.title}</TableCell>
                    <TableCell>
                      <Badge variant={t.status === "PUBLISHED" ? "default" : "secondary"}>
                        {t.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      {canManage && t.status === "DRAFT" && (
                        <>
                          <Button variant="outline" size="sm" onClick={() => setManageTestId(t.id)}>
                            Questions
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={publish.isPending}
                            onClick={() => onPublish(t.id)}
                          >
                            Publish
                          </Button>
                        </>
                      )}
                      {t.status === "PUBLISHED" && (
                        <Button size="sm" onClick={() => setTakeTest({ id: t.id, title: t.title })}>
                          Take
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

      <CreateTestDialog open={createOpen} onOpenChange={setCreateOpen} onCreated={invalidate} />

      {manageTestId && (
        <ManageQuestionsDialog
          testId={manageTestId}
          open={!!manageTestId}
          onOpenChange={(o) => !o && setManageTestId(null)}
        />
      )}

      {takeTest && (
        <TakeAttemptDialog
          testId={takeTest.id}
          testTitle={takeTest.title}
          canManage={canManage}
          open={!!takeTest}
          onOpenChange={(o) => !o && setTakeTest(null)}
        />
      )}
    </>
  );
}
