"use client";

import { Download, Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { downloadFile, useDeleteFile } from "@/lib/api/files/files/files";
import type { StoredFileResponse } from "@/lib/api/files/model";

/** One stored file's actions: download the blob, or delete it (then notify the parent list). */
export function FileRowActions({
  file,
  onDeleted,
}: {
  file: StoredFileResponse;
  onDeleted: (id: string) => void;
}) {
  const t = useTranslations("files");
  const remove = useDeleteFile();

  async function onDownload() {
    try {
      const blob = await downloadFile(file.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = file.originalFilename;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error(t("downloadFailed"));
    }
  }

  function onDelete() {
    remove.mutate(
      { fileId: file.id },
      {
        onSuccess: () => {
          onDeleted(file.id);
          toast.success(t("deleted"));
        },
      },
    );
  }

  return (
    <div className="space-x-2 text-right">
      <Button variant="outline" size="icon" onClick={onDownload} aria-label={t("download")}>
        <Download className="size-4" />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        disabled={remove.isPending}
        onClick={onDelete}
        aria-label={t("delete")}
      >
        <Trash2 className="size-4" />
      </Button>
    </div>
  );
}
