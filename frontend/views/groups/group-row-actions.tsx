"use client";

import { MoreHorizontal } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  useArchiveGroup,
  useDeleteGroup,
  useStartGroup,
} from "@/lib/api/enrollment/enrollment/enrollment";
import { type GroupResponse, GroupStatus } from "@/lib/api/enrollment/model";

/** One group's lifecycle actions: start a draft, archive a running one, delete (confirmed). */
export function GroupRowActions({ group }: { group: GroupResponse }) {
  const t = useTranslations("groups");
  const start = useStartGroup();
  const archive = useArchiveGroup();
  const remove = useDeleteGroup();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const groupId = group.id;
  const pending = start.isPending || archive.isPending || remove.isPending;

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" disabled={pending} aria-label={t("actions")}>
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {group.status === GroupStatus.DRAFT && (
            <DropdownMenuItem onClick={() => start.mutate({ groupId })}>
              {t("start")}
            </DropdownMenuItem>
          )}
          {group.status === GroupStatus.RUNNING && (
            <DropdownMenuItem onClick={() => archive.mutate({ groupId })}>
              {t("archive")}
            </DropdownMenuItem>
          )}
          {group.status !== GroupStatus.ARCHIVED && (
            <DropdownMenuItem className="text-destructive" onClick={() => setConfirmOpen(true)}>
              {t("delete")}
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("confirmDeleteTitle")}
        description={t("confirmDelete")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={remove.isPending}
        onConfirm={() => remove.mutate({ groupId }, { onSuccess: () => setConfirmOpen(false) })}
      />
    </>
  );
}
