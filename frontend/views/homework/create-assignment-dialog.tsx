"use client";

import { useForm } from "react-hook-form";
import { toast } from "sonner";

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
  onCreated: () => void;
}) {
  const form = useForm<FormValues>({ defaultValues: { title: "", description: "", dueAt: "" } });
  const createAssignment = useCreateAssignment();

  function onSubmit(values: FormValues) {
    createAssignment.mutate(
      {
        data: {
          groupId,
          title: values.title,
          description: values.description || undefined,
          // datetime-local → offset ISO the API expects
          dueAt: values.dueAt ? new Date(values.dueAt).toISOString() : undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success("Assignment created (draft)");
          onCreated();
          onOpenChange(false);
          form.reset();
        },
        onError: () => toast.error("Could not create the assignment"),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New assignment</DialogTitle>
          <DialogDescription>Created as a draft — publish it to make it visible.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
            <FormField
              control={form.control}
              name="title"
              rules={{ required: "Title is required" }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Title</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g. Week 1 exercises" {...field} />
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
                    <Textarea placeholder="Instructions…" {...field} />
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
                  <FormLabel>Due</FormLabel>
                  <FormControl>
                    <Input type="datetime-local" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="submit" disabled={createAssignment.isPending}>
                {createAssignment.isPending ? "Creating…" : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
