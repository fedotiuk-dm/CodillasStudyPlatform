"use client";

import { useTranslations } from "next-intl";
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
import { useListTests, usePublishTest } from "@/lib/api/assessment/assessment/assessment";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateTestDialog } from "./create-test-dialog";
import { ManageQuestionsDialog } from "./manage-questions-dialog";
import { TakeAttemptDialog } from "./take-attempt-dialog";

export function AssessmentView() {
  const t = useTranslations("tests");
  const { data, isLoading, isError } = useListTests();
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [createOpen, setCreateOpen] = useState(false);
  const [manageTestId, setManageTestId] = useState<string | null>(null);
  const [takeTest, setTakeTest] = useState<{ id: string; title: string } | null>(null);
  const publish = usePublishTest();

  const tests = data?.content ?? [];

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
          <DataState isLoading={isLoading} isError={isError} isEmpty={tests.length === 0}>
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
                      <Badge variant={test.status === "PUBLISHED" ? "default" : "secondary"}>
                        {test.status}
                      </Badge>
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
                          onClick={() => setTakeTest({ id: test.id, title: test.title })}
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

      <CreateTestDialog open={createOpen} onOpenChange={setCreateOpen} />

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
