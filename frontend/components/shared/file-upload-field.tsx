"use client";

import { Paperclip, X } from "lucide-react";
import { useRef } from "react";

import { Button } from "@/components/ui/button";
import { useUploadFile } from "@/lib/api/files/files/files";
import type { FileReferenceType, StoredFileResponse } from "@/lib/api/files/model";

/**
 * Reusable file picker that uploads to the `files` module (S3/minio) and hands back the stored
 * file. Every module that needs an attachment uses this and keeps only the returned `id` — no
 * module talks to storage directly. Upload happens on pick; errors surface via the global toast.
 */
export function FileUploadField({
  referenceType,
  referenceId,
  value,
  onUploaded,
  onClear,
  chooseLabel = "Choose file",
  uploadingLabel = "Uploading…",
}: {
  referenceType: FileReferenceType;
  referenceId?: string;
  /** Currently-attached file, or null when nothing is attached yet. */
  value?: { id: string; originalFilename: string } | null;
  onUploaded: (file: StoredFileResponse) => void;
  onClear?: () => void;
  chooseLabel?: string;
  uploadingLabel?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadFile();

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    // ponytail: uploads immediately; a cancelled dialog leaves an unreferenced blob in storage
    // (files have no GC yet) — acceptable until a sweep job exists.
    upload.mutate({ data: { file, referenceType, referenceId } }, { onSuccess: onUploaded });
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div className="flex items-center gap-2">
      <input ref={inputRef} type="file" className="hidden" onChange={onPick} />
      {value ? (
        <>
          <span className="flex items-center gap-1.5 text-sm">
            <Paperclip className="size-3.5" />
            {value.originalFilename}
          </span>
          {onClear && (
            <Button type="button" variant="ghost" size="sm" onClick={onClear}>
              <X className="size-3.5" />
            </Button>
          )}
        </>
      ) : (
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={upload.isPending}
          onClick={() => inputRef.current?.click()}
        >
          <Paperclip className="size-4" />
          {upload.isPending ? uploadingLabel : chooseLabel}
        </Button>
      )}
    </div>
  );
}
