"use client";

import type { AxiosError } from "axios";
import { Paperclip, X } from "lucide-react";
import { useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { useUploadFile } from "@/lib/api/files/files/files";
import type { FileReferenceType, StoredFileResponse } from "@/lib/api/files/model";

/**
 * Client-side upload guardrails — mirror the `files` backend so a doomed upload never leaves the
 * browser. Keep these in sync with `spring.servlet.multipart.max-file-size` and
 * `files.allowed-content-types` in `backend/main/src/main/resources/application.yml`.
 */
export const MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB
export const ALLOWED_CONTENT_TYPES = [
  "application/pdf",
  "image/png",
  "image/jpeg",
  "image/gif",
  "text/plain",
  "application/zip",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
] as const;

export type FileRejectionReason = "size" | "type";

/** Why an upload is unacceptable, or `null` when the file passes the guardrails. */
export function validateUploadFile(file: File): FileRejectionReason | null {
  if (file.size > MAX_FILE_SIZE_BYTES) return "size";
  if (!ALLOWED_CONTENT_TYPES.includes(file.type as (typeof ALLOWED_CONTENT_TYPES)[number])) {
    return "type";
  }
  return null;
}

/** Maps a rejected upload response (413/400) to a friendly reason, else `"generic"`. */
export function uploadErrorReason(error: unknown): FileRejectionReason | "generic" {
  const status = (error as AxiosError | undefined)?.response?.status;
  if (status === 413) return "size";
  if (status === 400) return "type";
  return "generic";
}

/**
 * Reusable file picker that uploads to the `files` module (S3/minio) and hands back the stored
 * file. Every module that needs an attachment uses this and keeps only the returned `id` — no
 * module talks to storage directly. Upload happens on pick; the file is validated client-side first
 * and a rejected upload (size/type) surfaces as an inline error.
 */
export function FileUploadField({
  referenceType,
  referenceId,
  value,
  onUploaded,
  onClear,
  chooseLabel = "Choose file",
  uploadingLabel = "Uploading…",
  tooLargeLabel = "File is too large (max 25 MB).",
  unsupportedTypeLabel = "Unsupported file type.",
  uploadFailedLabel = "Upload failed. Please try again.",
}: {
  referenceType: FileReferenceType;
  referenceId?: string;
  /** Currently-attached file, or null when nothing is attached yet. */
  value?: { id: string; originalFilename: string } | null;
  onUploaded: (file: StoredFileResponse) => void;
  onClear?: () => void;
  chooseLabel?: string;
  uploadingLabel?: string;
  tooLargeLabel?: string;
  unsupportedTypeLabel?: string;
  uploadFailedLabel?: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadFile();
  const [error, setError] = useState<string | null>(null);

  function messageFor(reason: FileRejectionReason | "generic") {
    if (reason === "size") return tooLargeLabel;
    if (reason === "type") return unsupportedTypeLabel;
    return uploadFailedLabel;
  }

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (inputRef.current) inputRef.current.value = "";
    if (!file) return;

    const reason = validateUploadFile(file);
    if (reason) {
      setError(messageFor(reason));
      return;
    }
    setError(null);
    // ponytail: uploads immediately; a cancelled dialog leaves an unreferenced blob in storage
    // (files have no GC yet) — acceptable until a sweep job exists.
    upload.mutate(
      { data: { file, referenceType, referenceId } },
      {
        onSuccess: onUploaded,
        onError: (err) => setError(messageFor(uploadErrorReason(err))),
      },
    );
  }

  return (
    <div className="space-y-1">
      <div className="flex items-center gap-2">
        <input
          ref={inputRef}
          type="file"
          accept={ALLOWED_CONTENT_TYPES.join(",")}
          className="hidden"
          onChange={onPick}
        />
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
      {error && <p className="text-destructive text-sm">{error}</p>}
    </div>
  );
}
