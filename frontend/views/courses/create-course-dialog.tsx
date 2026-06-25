"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useCreateCourse } from "@/lib/api/course/course/course";
import { CreateCourseBody } from "@/lib/api/course/zod/course/course.zod";

type FormValues = z.infer<typeof CreateCourseBody>;

export function CreateCourseDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated?: () => void;
}) {
  const form = useForm<FormValues>({
    resolver: zodResolver(CreateCourseBody),
    defaultValues: { name: "", description: "" },
  });
  const createCourse = useCreateCourse();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title="New course"
      description="Add a course to the catalogue."
      form={form}
      onCreated={onCreated}
      success="Course created"
      onSubmit={(values) => createCourse.mutateAsync({ data: values })}
    >
      <FormField
        control={form.control}
        name="name"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Name</FormLabel>
            <FormControl>
              <Input placeholder="e.g. Java Fundamentals" {...field} />
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
            <FormLabel>Description</FormLabel>
            <FormControl>
              <Textarea
                placeholder="What the course covers…"
                {...field}
                value={field.value ?? ""}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
