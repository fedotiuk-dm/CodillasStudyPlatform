"use client";

import { Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { useFieldArray, useForm } from "react-hook-form";

import { FormDialog } from "@/components/shared/form-dialog";
import { Button } from "@/components/ui/button";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useCreateRubric } from "@/lib/api/homework/homework/homework";

type FormValues = { criteria: { label: string; maxPoints: string }[] };

/** Define a grading rubric: one or more criteria (label + max points) attached to an assignment. */
export function CreateRubricDialog({
  assignmentId,
  open,
  onOpenChange,
}: {
  assignmentId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("homework");
  const form = useForm<FormValues>({
    defaultValues: { criteria: [{ label: "", maxPoints: "10" }] },
  });
  const { fields, append, remove } = useFieldArray({ control: form.control, name: "criteria" });
  const createRubric = useCreateRubric();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("createRubric")}
      description={t("rubricDialogDescription")}
      form={form}
      success={t("rubricCreated")}
      submitLabel={t("createRubric")}
      onReset={() => form.reset({ criteria: [{ label: "", maxPoints: "10" }] })}
      onSubmit={(values) =>
        createRubric.mutateAsync({
          assignmentId,
          data: {
            criteria: values.criteria.map((c) => ({
              label: c.label,
              maxPoints: Number(c.maxPoints),
            })),
          },
        })
      }
    >
      <div className="grid gap-3">
        <FormLabel>{t("criteria")}</FormLabel>
        {fields.map((f, i) => (
          <div key={f.id} className="flex items-start gap-2">
            <FormField
              control={form.control}
              name={`criteria.${i}.label`}
              rules={{ required: t("criterionLabelRequired") }}
              render={({ field }) => (
                <FormItem className="flex-1">
                  <FormControl>
                    <Input placeholder={t("criterionPlaceholder")} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name={`criteria.${i}.maxPoints`}
              rules={{ required: true, min: { value: 1, message: t("maxPointsMin") } }}
              render={({ field }) => (
                <FormItem className="w-24">
                  <FormControl>
                    <Input type="number" min={1} placeholder={t("maxPoints")} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              disabled={fields.length === 1}
              aria-label={t("removeCriterion")}
              onClick={() => remove(i)}
            >
              <Trash2 className="size-4" />
            </Button>
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="justify-self-start"
          onClick={() => append({ label: "", maxPoints: "10" })}
        >
          {t("addCriterion")}
        </Button>
      </div>
    </FormDialog>
  );
}
