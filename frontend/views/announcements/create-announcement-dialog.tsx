"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { FormDialog } from "@/components/shared/form-dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAnnouncement } from "@/lib/api/announcement/announcement/announcement";
import { CreateAnnouncementBody } from "@/lib/api/announcement/zod/announcement/announcement.zod";

// z.input: the schema's `pinned` has a zod default, so its input type (what the form holds) keeps
// the field optional while the output stays boolean.
type FormValues = z.input<typeof CreateAnnouncementBody>;

export function CreateAnnouncementDialog({
  groupId,
  open,
  onOpenChange,
}: {
  groupId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("announcements");
  const form = useForm<FormValues>({
    resolver: zodResolver(CreateAnnouncementBody),
    defaultValues: { title: "", body: "", pinned: false },
  });
  const create = useCreateAnnouncement();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("newAnnouncement")}
      description={t("dialogDescription")}
      form={form}
      success={t("posted")}
      submitLabel={t("postLabel")}
      pendingLabel={t("posting")}
      onSubmit={(values) => create.mutateAsync({ groupId, data: values })}
    >
      <FormField
        control={form.control}
        name="title"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("titleLabel")}</FormLabel>
            <FormControl>
              <Input placeholder={t("titlePlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="body"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("bodyLabel")}</FormLabel>
            <FormControl>
              <Textarea
                placeholder={t("bodyPlaceholder")}
                rows={4}
                {...field}
                value={field.value ?? ""}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="pinned"
        render={({ field }) => (
          <FormItem className="flex flex-row items-center gap-2 space-y-0">
            <FormControl>
              <Checkbox checked={field.value ?? false} onCheckedChange={field.onChange} />
            </FormControl>
            <FormLabel className="!mt-0 font-normal">{t("pinned")}</FormLabel>
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
