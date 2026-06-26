"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateLesson, useUpdateLesson } from "@/lib/api/course/course/course";
import { CreateLessonBody } from "@/lib/api/course/zod/course/course.zod";

type FormValues = z.infer<typeof CreateLessonBody>;

/** Create a lesson in a section, or edit an existing lesson when `lesson` is supplied. */
export function LessonDialog({
  sectionId,
  lesson,
  open,
  onOpenChange,
}: {
  sectionId: string;
  lesson?: {
    id: string;
    title: string;
    summary?: string;
    meetingUrl?: string;
    recordingUrl?: string;
  };
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("course");
  const create = useCreateLesson();
  const update = useUpdateLesson();
  const editing = !!lesson;

  const form = useForm<FormValues>({
    resolver: zodResolver(CreateLessonBody),
    defaultValues: {
      title: lesson?.title ?? "",
      summary: lesson?.summary ?? "",
      meetingUrl: lesson?.meetingUrl ?? "",
      recordingUrl: lesson?.recordingUrl ?? "",
    },
  });

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={editing ? t("editLesson") : t("newLesson")}
      form={form}
      success={editing ? t("lessonUpdated") : t("lessonCreated")}
      submitLabel={editing ? t("save") : t("create")}
      pendingLabel={t("saving")}
      onSubmit={(values) => {
        // ponytail: drop empty optionals so we don't persist blank strings
        const data = {
          title: values.title,
          summary: values.summary || undefined,
          meetingUrl: values.meetingUrl || undefined,
          recordingUrl: values.recordingUrl || undefined,
        };
        return editing
          ? update.mutateAsync({ lessonId: lesson.id, data })
          : create.mutateAsync({ sectionId, data });
      }}
    >
      <FormField
        control={form.control}
        name="title"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("lessonTitle")}</FormLabel>
            <FormControl>
              <Input placeholder={t("lessonTitlePlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="summary"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("summary")}</FormLabel>
            <FormControl>
              <Textarea
                placeholder={t("summaryPlaceholder")}
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
        name="meetingUrl"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("meetingUrl")}</FormLabel>
            <FormControl>
              <Input placeholder="https://meet.google.com/…" {...field} value={field.value ?? ""} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="recordingUrl"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("recordingUrl")}</FormLabel>
            <FormControl>
              <Input placeholder="https://…" {...field} value={field.value ?? ""} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
