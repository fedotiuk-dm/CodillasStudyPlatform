"use client";

import type { ReactNode } from "react";
import type { FieldValues, UseFormReturn } from "react-hook-form";
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
import { Form } from "@/components/ui/form";

/**
 * Dialog shell for a create/edit form: owns the Dialog + Form provider + submit footer and the
 * success lifecycle (toast → onCreated → close → reset). Callers supply only the fields and an
 * `onSubmit` that performs the mutation (throw to surface the error toast). Pending state comes
 * from react-hook-form's `formState.isSubmitting`, so `onSubmit` must return its promise.
 */
export function FormDialog<T extends FieldValues>({
  open,
  onOpenChange,
  title,
  description,
  form,
  onSubmit,
  success,
  error,
  onCreated,
  onReset,
  submitDisabled,
  submitLabel = "Create",
  pendingLabel = "Creating…",
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  form: UseFormReturn<T>;
  onSubmit: (values: T) => Promise<unknown>;
  success: string;
  error: string;
  onCreated?: () => void;
  onReset?: () => void;
  submitDisabled?: boolean;
  submitLabel?: string;
  pendingLabel?: string;
  children: ReactNode;
}) {
  const submitting = form.formState.isSubmitting;

  async function handle(values: T) {
    try {
      await onSubmit(values);
      toast.success(success);
      onCreated?.();
      onOpenChange(false);
      form.reset();
      onReset?.();
    } catch {
      toast.error(error);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description && <DialogDescription>{description}</DialogDescription>}
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handle)} className="grid gap-4">
            {children}
            <DialogFooter>
              <Button type="submit" disabled={submitting || submitDisabled}>
                {submitting ? pendingLabel : submitLabel}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
