"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { DateTimeField } from "@/components/shared/date-time-field";
import { FormDialog } from "@/components/shared/form-dialog";
import { UserPicker } from "@/components/shared/user-picker";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useListCourses } from "@/lib/api/course/course/course";
import { CourseStatus } from "@/lib/api/course/model";
import { useCreateGroup } from "@/lib/api/enrollment/enrollment/enrollment";
import { CreateGroupBody } from "@/lib/api/enrollment/zod/enrollment/enrollment.zod";

type FormValues = z.infer<typeof CreateGroupBody>;

export function CreateGroupDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void;
}) {
  const t = useTranslations("groups");
  const { data: coursesData } = useListCourses({ status: CourseStatus.PUBLISHED });
  const courses = coursesData?.content ?? [];
  const createGroup = useCreateGroup();
  const [teacherName, setTeacherName] = useState<string>();

  const form = useForm<FormValues>({
    resolver: zodResolver(CreateGroupBody),
    defaultValues: { name: "", courseId: "", teacherId: "", startDate: undefined },
  });

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("newGroup")}
      description={t("dialogDescription")}
      form={form}
      onCreated={onCreated}
      onReset={() => setTeacherName(undefined)}
      success={t("created")}
      onSubmit={(values) =>
        createGroup.mutateAsync({ data: { ...values, startDate: values.startDate || undefined } })
      }
    >
      <FormField
        control={form.control}
        name="name"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("name")}</FormLabel>
            <FormControl>
              <Input placeholder={t("namePlaceholder")} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="courseId"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("course")}</FormLabel>
            <Select onValueChange={field.onChange} value={field.value}>
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder={t("selectCourse")} />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                {courses.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="teacherId"
        rules={{ required: t("pickTeacher") }}
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("teacher")}</FormLabel>
            <FormControl>
              <UserPicker
                value={field.value}
                displayName={teacherName}
                placeholder={t("searchTeachers")}
                onChange={(id, name) => {
                  field.onChange(id);
                  setTeacherName(name);
                }}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="startDate"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("startDate")}</FormLabel>
            <FormControl>
              <DateTimeField
                mode="date"
                value={field.value ?? undefined}
                onChange={field.onChange}
                placeholder={t("startDate")}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
