import { useTranslations } from "next-intl";

import { StatusBadge } from "@/components/shared/status-badge";
import { Badge } from "@/components/ui/badge";
import { TableCell, TableRow } from "@/components/ui/table";
import type { AssignmentResponse } from "@/lib/api/homework/model";

import { AssignmentRowActions } from "./assignment-row-actions";

/** One assignment row: title (+ rubric/late-penalty markers), status, due date, and actions. */
export function AssignmentRow({
  assignment,
  canManage,
}: {
  assignment: AssignmentResponse;
  canManage: boolean;
}) {
  const t = useTranslations("homework");

  return (
    <TableRow>
      <TableCell className="font-medium">
        <div className="flex flex-col gap-1">
          <span>{assignment.title}</span>
          <div className="flex flex-wrap gap-1">
            {assignment.rubricId && (
              <Badge variant="outline" className="font-normal">
                {t("rubric")}
              </Badge>
            )}
            {assignment.latePenaltyPctPerDay != null && (
              <Badge variant="outline" className="font-normal text-muted-foreground">
                {t("latePenaltyBadge", {
                  perDay: assignment.latePenaltyPctPerDay,
                  max: assignment.maxLatePenaltyPct ?? 100,
                })}
              </Badge>
            )}
          </div>
        </div>
      </TableCell>
      <TableCell>
        <StatusBadge
          status={assignment.status}
          label={t(`assignmentStatus.${assignment.status}`)}
        />
      </TableCell>
      <TableCell className="text-muted-foreground">
        {assignment.dueAt ? new Date(assignment.dueAt).toLocaleString() : "—"}
      </TableCell>
      <TableCell className="text-right">
        <AssignmentRowActions assignment={assignment} canManage={canManage} />
      </TableCell>
    </TableRow>
  );
}
