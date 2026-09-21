"use client";

import { Download } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useDeleteMaterial } from "@/lib/api/course/course/course";
import type { MaterialResponse } from "@/lib/api/course/model";
import { downloadFile } from "@/lib/api/files/files/files";
import { safeHref } from "@/lib/utils";

/** One lesson material: a type badge + (linked) title, plus a confirmed delete for editors. */
export function MaterialRow({
  material,
  canEdit,
}: {
  material: MaterialResponse;
  canEdit: boolean;
}) {
  const t = useTranslations("course");
  const del = useDeleteMaterial();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const href = material.type === "LINK" && material.url ? safeHref(material.url) : undefined;
  const fileId = material.type === "FILE" ? material.fileId : undefined;

  async function onDownload() {
    if (!fileId) return;
    try {
      const blob = await downloadFile(fileId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = material.title;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error(t("downloadFailed"));
    }
  }

  return (
    <li className="flex items-center justify-between gap-2 rounded border px-3 py-2 text-sm">
      <span className="flex items-center gap-2">
        <Badge variant="secondary">{material.type}</Badge>
        {href ? (
          <a className="text-primary underline" href={href} target="_blank" rel="noreferrer">
            {material.title}
          </a>
        ) : fileId ? (
          <button type="button" className="text-primary underline" onClick={onDownload}>
            {material.title}
          </button>
        ) : (
          <span>{material.title}</span>
        )}
      </span>
      <span className="flex items-center gap-1">
        {fileId && (
          <Button variant="ghost" size="icon" onClick={onDownload} aria-label={t("download")}>
            <Download className="size-4" />
          </Button>
        )}
        {canEdit && (
          <Button
            variant="ghost"
            size="sm"
            disabled={del.isPending}
            onClick={() => setConfirmDelete(true)}
          >
            {t("delete")}
          </Button>
        )}
      </span>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={t("confirmDeleteMaterialTitle")}
        description={t("confirmDeleteMaterial")}
        confirmLabel={t("delete")}
        cancelLabel={t("cancel")}
        pending={del.isPending}
        onConfirm={() =>
          del.mutate(
            { materialId: material.id },
            {
              onSuccess: () => {
                toast.success(t("materialDeleted"));
                setConfirmDelete(false);
              },
            },
          )
        }
      />
    </li>
  );
}
