"use client";

import { Download, Trash2, Upload } from "lucide-react";
import { useRef, useState } from "react";
import { toast } from "sonner";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { downloadFile, useDeleteFile, useUploadFile } from "@/lib/api/files/files/files";
import type { StoredFileResponse } from "@/lib/api/files/model";
import { FileReferenceType } from "@/lib/api/files/model";

function humanSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function FilesView() {
  // ponytail: session-local list — the Files API has no list endpoint, only by-id.
  const [files, setFiles] = useState<StoredFileResponse[]>([]);
  const [referenceType, setReferenceType] = useState<FileReferenceType>(FileReferenceType.MATERIAL);
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadFile();
  const remove = useDeleteFile();

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    upload.mutate(
      { data: { file, referenceType } },
      {
        onSuccess: (stored) => {
          setFiles((prev) => [stored, ...prev]);
          toast.success(`Uploaded ${stored.originalFilename}`);
        },
      },
    );
    if (inputRef.current) inputRef.current.value = "";
  }

  async function onDownload(f: StoredFileResponse) {
    try {
      const blob = await downloadFile(f.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = f.originalFilename;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      /* global toast */
    }
  }

  function onDelete(f: StoredFileResponse) {
    remove.mutate(
      { fileId: f.id },
      {
        onSuccess: () => {
          setFiles((prev) => prev.filter((x) => x.id !== f.id));
          toast.success("Deleted");
        },
      },
    );
  }

  return (
    <>
      <PageHeader
        title="Files"
        description="Upload and manage files stored in object storage."
        action={
          <div className="flex items-center gap-2">
            <Select
              value={referenceType}
              onValueChange={(v) => setReferenceType(v as FileReferenceType)}
            >
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.values(FileReferenceType).map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <input ref={inputRef} type="file" className="hidden" onChange={onPick} />
            <Button onClick={() => inputRef.current?.click()} disabled={upload.isPending}>
              <Upload className="size-4" />
              {upload.isPending ? "Uploading…" : "Upload"}
            </Button>
          </div>
        }
      />

      <Card>
        <CardContent className="pt-6">
          <DataState isLoading={false} isError={false} isEmpty={files.length === 0}>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Size</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {files.map((f) => (
                  <TableRow key={f.id}>
                    <TableCell className="font-medium">{f.originalFilename}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{f.referenceType ?? "—"}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{humanSize(f.fileSize)}</TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button variant="outline" size="icon" onClick={() => onDownload(f)}>
                        <Download className="size-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        disabled={remove.isPending}
                        onClick={() => onDelete(f)}
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataState>
        </CardContent>
      </Card>
    </>
  );
}
