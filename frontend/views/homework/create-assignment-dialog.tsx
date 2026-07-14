"use client";

import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";

import { DateTimeField } from "@/components/shared/date-time-field";
import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateAssignment } from "@/lib/api/homework/homework/homework";

type FormValues = {
  title: string;
  description: string;
  dueAt: string;
  latePenaltyPctPerDay: string;
  maxLatePenaltyPct: string;
};

/** Optional 1–100 percent: parsed to a number, or omitted when the field is left blank. */
function pct(value: string): number | undefined {
  const trimmed = value.trim();
  return trimmed ? Number(trimmed) : undefined;
}

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
  const form = useForm<FormValues>({
    defaultValues: {
      title: "",
      description: "",
      dueAt: "",
      latePenaltyPctPerDay: "",
      maxLatePenaltyPct: "",
    },
  });
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
            latePenaltyPctPerDay: pct(values.latePenaltyPctPerDay),
            maxLatePenaltyPct: pct(values.maxLatePenaltyPct),
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
      <div className="grid grid-cols-2 gap-4">
        <FormField
          control={form.control}
          name="latePenaltyPctPerDay"
          rules={{
            min: { value: 1, message: t("pctRange") },
            max: { value: 100, message: t("pctRange") },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t("latePenaltyPerDay")}</FormLabel>
              <FormControl>
                <Input type="number" min={1} max={100} placeholder={t("optional")} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="maxLatePenaltyPct"
          rules={{
            min: { value: 1, message: t("pctRange") },
            max: { value: 100, message: t("pctRange") },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t("maxLatePenalty")}</FormLabel>
              <FormControl>
                <Input type="number" min={1} max={100} placeholder={t("optional")} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
      </div>
    </FormDialog>
  );
}
