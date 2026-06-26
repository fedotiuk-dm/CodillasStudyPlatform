"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useCreateSection, useUpdateSection } from "@/lib/api/course/course/course";
import { CreateSectionBody } from "@/lib/api/course/zod/course/course.zod";

type FormValues = z.infer<typeof CreateSectionBody>;

/** Create a section (when only `courseId`) or rename one (when `section` is supplied). */
export function SectionDialog({
  courseId,
  section,
  open,
  onOpenChange,
}: {
  courseId: string;
  section?: { id: string; title: string };
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("course");
  const create = useCreateSection();
  const update = useUpdateSection();
  const editing = !!section;

  const form = useForm<FormValues>({
    resolver: zodResolver(CreateSectionBody),
    defaultValues: { title: section?.title ?? "" },
  });

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? t("renameSection") : t("newSection")}
      form={form}
      success={editing ? t("sectionRenamed") : t("sectionCreated")}
      submitLabel={editing ? t("save") : t("create")}
      pendingLabel={t("saving")}
      onSubmit={(values) =>
        editing
          ? update.mutateAsync({ sectionId: section.id, data: values })
          : create.mutateAsync({ courseId, data: values })
      }
    >
      <FormField
        control={form.control}
        name="title"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("sectionTitle")}</FormLabel>
            <FormControl>
              <Input placeholder={t("sectionTitlePlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
