"use client";

import { Upload } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRef } from "react";
import { toast } from "sonner";

import {
  ALLOWED_CONTENT_TYPES,
  uploadErrorReason,
  validateUploadFile,
} from "@/components/shared/file-upload-field";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useUploadFile } from "@/lib/api/files/files/files";
import type { StoredFileResponse } from "@/lib/api/files/model";
import { FileReferenceType } from "@/lib/api/files/model";

/**
 * Reference-type picker + upload button. Validates the file against the shared guardrails before it
 * leaves the browser and reports a rejected upload (size/type) via toast.
 */
export function FileUploadControl({
  referenceType,
  onReferenceTypeChange,
  onUploaded,
}: {
  referenceType: FileReferenceType;
  onReferenceTypeChange: (value: FileReferenceType) => void;
  onUploaded: (file: StoredFileResponse) => void;
}) {
  const t = useTranslations("files");
  const inputRef = useRef<HTMLInputElement>(null);
  const upload = useUploadFile();

  function messageFor(reason: "size" | "type" | "generic") {
    if (reason === "size") return t("tooLarge");
    if (reason === "type") return t("unsupportedType");
    return t("uploadFailed");
  }

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (inputRef.current) inputRef.current.value = "";
    if (!file) return;

    const reason = validateUploadFile(file);
    if (reason) {
      toast.error(messageFor(reason));
      return;
    }
    upload.mutate(
      { data: { file, referenceType } },
      {
        onSuccess: (stored) => {
          onUploaded(stored);
          toast.success(t("uploaded", { name: stored.originalFilename }));
        },
        onError: (err) => toast.error(messageFor(uploadErrorReason(err))),
      },
    );
  }

  return (
    <div className="flex items-center gap-2">
      <Select
        value={referenceType}
        onValueChange={(v) => onReferenceTypeChange(v as FileReferenceType)}
      >
        <SelectTrigger className="w-36">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {Object.values(FileReferenceType).map((ref) => (
            <SelectItem key={ref} value={ref}>
              {ref}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <input
        ref={inputRef}
        type="file"
        accept={ALLOWED_CONTENT_TYPES.join(",")}
        className="hidden"
        onChange={onPick}
      />
      <Button onClick={() => inputRef.current?.click()} disabled={upload.isPending}>
        <Upload className="size-4" />
        {upload.isPending ? t("uploading") : t("upload")}
      </Button>
    </div>
  );
}
