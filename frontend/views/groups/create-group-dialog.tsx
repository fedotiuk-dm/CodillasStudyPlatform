"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import type { z } from "zod";

import { UserPicker } from "@/components/shared/user-picker";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
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

  function onSubmit(values: FormValues) {
    createGroup.mutate(
      { data: { ...values, startDate: values.startDate || undefined } },
      {
        onSuccess: () => {
          toast.success("Group created");
          onCreated();
          onOpenChange(false);
          form.reset({ name: "", courseId: "", teacherId: "", startDate: undefined });
          setTeacherName(undefined);
        },
        onError: () => toast.error("Could not create the group"),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New group</DialogTitle>
          <DialogDescription>A cohort that runs a course on a schedule.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
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
            <DialogFooter>
              <Button type="submit" disabled={createGroup.isPending}>
                {createGroup.isPending ? "Creating…" : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
