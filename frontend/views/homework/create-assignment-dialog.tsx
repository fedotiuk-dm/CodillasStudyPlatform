"use client";

import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";

import { DateTimeField } from "@/components/shared/date-time-field";
import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAssignment } from "@/lib/api/homework/homework/homework";

type FormValues = { title: string; description: string; dueAt: string };

export function CreateAssignmentDialog({
  groupId,
  open,
  onOpenChange,
  onCreated,
}: {
  groupId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void;
}) {
  const t = useTranslations("homework");
  const form = useForm<FormValues>({ defaultValues: { title: "", description: "", dueAt: "" } });
  const createAssignment = useCreateAssignment();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("newAssignment")}
      description={t("dialogDescription")}
      form={form}
      onCreated={onCreated}
      success={t("created")}
      onSubmit={(values) =>
        createAssignment.mutateAsync({
          data: {
            groupId,
            title: values.title,
            description: values.description || undefined,
            dueAt: values.dueAt || undefined,
          },
        })
      }
    >
      <FormField
        control={form.control}
        name="title"
        rules={{ required: t("titleRequired") }}
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
        name="description"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("descriptionLabel")}</FormLabel>
            <FormControl>
              <Textarea placeholder={t("descriptionPlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="dueAt"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("due")}</FormLabel>
            <FormControl>
              <DateTimeField
                mode="datetime"
                value={field.value || undefined}
                onChange={field.onChange}
                placeholder={t("due")}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
