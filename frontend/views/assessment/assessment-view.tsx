"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusBadge } from "@/components/shared/status-badge";
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
  useListTests,
  usePublishTest,
  useStartAttempt,
} from "@/lib/api/assessment/assessment/assessment";
import type { AttemptResponse } from "@/lib/api/assessment/model";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { AttemptWizard } from "./attempt-wizard";
import { ManageQuestionsDialog } from "./manage-questions-dialog";
import { TestWizard } from "./test-wizard";

export function AssessmentView() {
  const t = useTranslations("tests");
  const { data, isLoading, isError, refetch } = useListTests();
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [createOpen, setCreateOpen] = useState(false);
  const [manageTestId, setManageTestId] = useState<string | null>(null);
  const [takeTest, setTakeTest] = useState<{
    id: string;
    title: string;
    attempt: AttemptResponse;
  } | null>(null);
  const publish = usePublishTest();
  const startAttempt = useStartAttempt();

  const tests = data?.content ?? [];

  // Create (or resume) the attempt on the click, then open the wizard with it already in hand —
  // mirrors Boosting's handleTipConfirm. No mutation is fired from inside the dialog on open.
  async function onTake(test: { id: string; title: string }) {
    if (startAttempt.isPending) return;
    try {
      const attempt = await startAttempt.mutateAsync({ testId: test.id });
      setTakeTest({ id: test.id, title: test.title, attempt });
    } catch {
      /* global error toast */
    }
  }

  function onPublish(testId: string) {
    publish.mutate(
      { testId },
      {
        onSuccess: () => {
          toast.success(t("published"));
        },
      },
    );
  }

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={
          canManage ? (
            <Button onClick={() => setCreateOpen(true)}>{t("newTest")}</Button>
          ) : undefined
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState
            isLoading={isLoading}
            isError={isError}
            isEmpty={tests.length === 0}
            variant="table"
            onRetry={refetch}
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("titleLabel")}</TableHead>
                  <TableHead>{t("status")}</TableHead>
                  <TableHead className="text-right">{t("actions")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tests.map((test) => (
                  <TableRow key={test.id}>
                    <TableCell className="font-medium">{test.title}</TableCell>
                    <TableCell>
                      <StatusBadge status={test.status} label={t(`testStatus.${test.status}`)} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      {canManage && test.status === "DRAFT" && (
                        <>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setManageTestId(test.id)}
                          >
                            {t("questions")}
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={publish.isPending}
                            onClick={() => onPublish(test.id)}
                          >
                            {t("publish")}
                          </Button>
                        </>
                      )}
                      {test.status === "PUBLISHED" && (
                        <Button
                          size="sm"
                          disabled={startAttempt.isPending}
                          onClick={() => onTake(test)}
                        >
                          {t("take")}
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

      <TestWizard open={createOpen} onOpenChange={setCreateOpen} />

      {manageTestId && (
        <ManageQuestionsDialog
          testId={manageTestId}
          open={!!manageTestId}
          onOpenChange={(o) => !o && setManageTestId(null)}
        />
      )}

      {takeTest && (
        <AttemptWizard
          testId={takeTest.id}
          testTitle={takeTest.title}
          attempt={takeTest.attempt}
          canManage={canManage}
          open={!!takeTest}
          onOpenChange={(o) => !o && setTakeTest(null)}
        />
      )}
    </>
  );
}
