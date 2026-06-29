import { useTranslations } from "next-intl";

import { StatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import { TableCell, TableRow } from "@/components/ui/table";
import type { GroupResponse } from "@/lib/api/enrollment/model";

import { GroupRowActions } from "./group-row-actions";

/** One group row: name, course, start date, status badge, manage button + (for staff) lifecycle actions. */
export function GroupRow({
  group,
  courseName,
  canManage,
  onManage,
}: {
  group: GroupResponse;
  courseName: string;
  canManage: boolean;
  onManage: () => void;
}) {
  const t = useTranslations("groups");

  return (
    <TableRow>
      <TableCell className="font-medium">{group.name}</TableCell>
      <TableCell className="text-muted-foreground">{courseName}</TableCell>
      <TableCell className="text-muted-foreground">{group.startDate ?? "—"}</TableCell>
      <TableCell>
        <StatusBadge status={group.status} label={t(`status.${group.status}`)} />
      </TableCell>
      <TableCell className="text-right">
        {canManage && (
          <div className="flex items-center justify-end gap-2">
            <Button variant="outline" size="sm" onClick={onManage}>
              {t("manage")}
            </Button>
            <GroupRowActions group={group} />
          </div>
        )}
      </TableCell>
    </TableRow>
  );
}
