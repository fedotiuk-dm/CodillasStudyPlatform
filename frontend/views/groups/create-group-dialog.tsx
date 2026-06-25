"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import type { z } from "zod";

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
  onCreated: () => void;
}) {
  const { data: coursesData } = useListCourses();
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
      title="New group"
      description="A cohort that runs a course on a schedule."
      form={form}
      onCreated={onCreated}
      onReset={() => setTeacherName(undefined)}
      success="Group created"
      error="Could not create the group"
      onSubmit={(values) =>
        createGroup.mutateAsync({ data: { ...values, startDate: values.startDate || undefined } })
      }
    >
      <FormField
        control={form.control}
        name="name"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Name</FormLabel>
            <FormControl>
              <Input placeholder="e.g. Cohort A" {...field} />
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
            <FormLabel>Course</FormLabel>
            <Select onValueChange={field.onChange} value={field.value}>
              <FormControl>
                <SelectTrigger>
                  <SelectValue placeholder="Select a course" />
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
        rules={{ required: "Pick a teacher" }}
        render={({ field }) => (
          <FormItem>
            <FormLabel>Teacher</FormLabel>
            <FormControl>
              <UserPicker
                value={field.value}
                displayName={teacherName}
                placeholder="Search teachers…"
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
            <FormLabel>Start date</FormLabel>
            <FormControl>
              <Input type="date" {...field} value={field.value ?? ""} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
