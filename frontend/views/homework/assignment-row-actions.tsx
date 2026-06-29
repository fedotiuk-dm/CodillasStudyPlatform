"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { usePublishAssignment } from "@/lib/api/homework/homework/homework";
import { type AssignmentResponse, AssignmentStatus } from "@/lib/api/homework/model";

import { CreateRubricDialog } from "./create-rubric-dialog";
import { SubmissionsDialog } from "./submissions-dialog";

/** One assignment's actions: open submissions, publish a draft, and (staff) attach a rubric. */
export function AssignmentRowActions({
  assignment,
  canManage,
}: {
  assignment: AssignmentResponse;
  canManage: boolean;
}) {
  const t = useTranslations("homework");
  const publish = usePublishAssignment();
  const [subsOpen, setSubsOpen] = useState(false);
  const [rubricOpen, setRubricOpen] = useState(false);

  const assignmentId = assignment.id;
  const hasRubric = !!assignment.rubricId;

  return (
    <div className="flex flex-wrap justify-end gap-2">
      <Button variant="outline" size="sm" onClick={() => setSubsOpen(true)}>
        {t("submissions")}
      </Button>

      {canManage && assignment.status === AssignmentStatus.DRAFT && (
        <Button
          variant="outline"
          size="sm"
          disabled={publish.isPending}
          onClick={() =>
            publish.mutate({ assignmentId }, { onSuccess: () => toast.success(t("published")) })
          }
        >
          {t("publish")}
        </Button>
      )}

      {canManage && !hasRubric && (
        <Button variant="outline" size="sm" onClick={() => setRubricOpen(true)}>
          {t("addRubric")}
        </Button>
      )}

      <SubmissionsDialog
        assignmentId={assignmentId}
        assignmentTitle={assignment.title}
        canManage={canManage}
        hasRubric={hasRubric}
        open={subsOpen}
        onOpenChange={setSubsOpen}
      />

      {canManage && (
        <CreateRubricDialog
          assignmentId={assignmentId}
          open={rubricOpen}
          onOpenChange={setRubricOpen}
        />
      )}
    </div>
  );
}
