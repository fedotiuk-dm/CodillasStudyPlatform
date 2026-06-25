"use client";

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
  const form = useForm<FormValues>({ defaultValues: { title: "" } });
  const createTest = useCreateTest();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title="New test"
      description="Create a draft, then add questions and publish."
      form={form}
      onCreated={onCreated}
      success="Test created (draft)"
      onSubmit={(values) => createTest.mutateAsync({ data: { title: values.title } })}
    >
      <FormField
        control={form.control}
        name="title"
        rules={{ required: "Title is required" }}
        render={({ field }) => (
          <FormItem>
            <FormLabel>Title</FormLabel>
            <FormControl>
              <Input placeholder="e.g. Module 1 quiz" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
