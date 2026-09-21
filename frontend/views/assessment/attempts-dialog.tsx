"use client";

import { useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useGetTest, useListTestAttempts } from "@/lib/api/assessment/assessment/assessment";
import { useProfileNames } from "@/lib/hooks/use-profile-names";

import { ResultPanel } from "./result-panel";

/** Staff view of every attempt at a test: one result panel per attempt, with manual grading. */
export function AttemptsDialog({
  testId,
  testTitle,
  open,
  onOpenChange,
}: {
  testId: string;
  testTitle: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("tests");
  const nameOf = useProfileNames();
  const { data: test } = useGetTest(testId, { query: { enabled: open } });
  const { data, isLoading, isError, refetch } = useListTestAttempts(testId, {
    query: { enabled: open },
  });
  const attempts = data ?? [];
  const questions = test?.questions ?? [];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{testTitle}</DialogTitle>
          <DialogDescription>{t("attemptsDescription")}</DialogDescription>
        </DialogHeader>
        <DataState
          isLoading={isLoading || (attempts.length > 0 && !test)}
          isError={isError}
          isEmpty={attempts.length === 0}
          emptyMessage={t("noAttempts")}
          variant="list"
          onRetry={refetch}
        >
          <div className="grid gap-4">
            {attempts.map((attempt) => (
              <section key={attempt.id} className="grid gap-2 rounded-lg border p-3">
                <p className="font-medium">
                  {nameOf(attempt.studentId)}
                  <span className="ml-2 text-muted-foreground text-sm">
                    {t("attemptNumber", { number: attempt.attemptNumber })}
                  </span>
                </p>
                <ResultPanel attempt={attempt} test={questions} canManage />
              </section>
            ))}
          </div>
        </DataState>
      </DialogContent>
    </Dialog>
  );
}
