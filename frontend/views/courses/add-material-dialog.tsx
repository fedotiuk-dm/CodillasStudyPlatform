"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { FileUploadField } from "@/components/shared/file-upload-field";
import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAddMaterial } from "@/lib/api/course/course/course";
import type { CreateMaterialRequest } from "@/lib/api/course/model";
import { FileReferenceType } from "@/lib/api/files/model";

export function AddMaterialDialog({
  lessonId,
  open,
  onOpenChange,
}: {
  lessonId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("course");
  const addMaterial = useAddMaterial();
  const [fileName, setFileName] = useState<string | null>(null);

  // ponytail: hand-rolled schema so we can enforce the FILE/LINK one-of rule the spec can't express
  const schema = z
    .object({
      type: z.enum(["FILE", "LINK"]),
      title: z.string().min(1).max(200),
      url: z.string().optional(),
      fileId: z.string().optional(),
    })
    .superRefine((v, ctx) => {
      if (v.type === "LINK" && !v.url?.trim()) {
        ctx.addIssue({ code: "custom", path: ["url"], message: t("urlRequired") });
      }
      if (v.type === "FILE" && !v.fileId?.trim()) {
        ctx.addIssue({ code: "custom", path: ["fileId"], message: t("fileIdRequired") });
      }
    });

  type FormValues = z.infer<typeof schema>;

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: "LINK", title: "", url: "", fileId: "" },
  });
  const type = form.watch("type");

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("addMaterial")}
      form={form}
      success={t("materialAdded")}
      submitLabel={t("add")}
      pendingLabel={t("saving")}
      onSubmit={(values) => {
        const data: CreateMaterialRequest = {
          type: values.type,
          title: values.title,
          url: values.type === "LINK" ? values.url : undefined,
          fileId: values.type === "FILE" ? values.fileId : undefined,
        };
        return addMaterial.mutateAsync({ lessonId, data });
      }}
    >
      <FormField
        control={form.control}
        name="type"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("materialType")}</FormLabel>
            <Select onValueChange={field.onChange} value={field.value}>
              <FormControl>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                <SelectItem value="LINK">{t("typeLink")}</SelectItem>
                <SelectItem value="FILE">{t("typeFile")}</SelectItem>
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="title"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("materialTitle")}</FormLabel>
            <FormControl>
              <Input placeholder={t("materialTitlePlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      {type === "LINK" ? (
        <FormField
          control={form.control}
          name="url"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t("url")}</FormLabel>
              <FormControl>
                <Input placeholder="https://…" {...field} value={field.value ?? ""} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      ) : (
        <FormField
          control={form.control}
          name="fileId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t("file")}</FormLabel>
              <FileUploadField
                referenceType={FileReferenceType.MATERIAL}
                value={
                  field.value && fileName ? { id: field.value, originalFilename: fileName } : null
                }
                onUploaded={(stored) => {
                  field.onChange(stored.id);
                  setFileName(stored.originalFilename);
                }}
                onClear={() => {
                  field.onChange("");
                  setFileName(null);
                }}
                chooseLabel={t("chooseFile")}
                uploadingLabel={t("uploading")}
              />
              <FormMessage />
            </FormItem>
          )}
        />
      )}
    </FormDialog>
  );
}
