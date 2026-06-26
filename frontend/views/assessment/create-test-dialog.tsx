"use client";

import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";

import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useCreateTest } from "@/lib/api/assessment/assessment/assessment";

type FormValues = { title: string };

export function CreateTestDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void;
}) {
  const t = useTranslations("tests");
  const form = useForm<FormValues>({ defaultValues: { title: "" } });
  const createTest = useCreateTest();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("newTest")}
      description={t("dialogDescription")}
      form={form}
      onCreated={onCreated}
      success={t("created")}
      onSubmit={(values) => createTest.mutateAsync({ data: { title: values.title } })}
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
    </FormDialog>
  );
}
