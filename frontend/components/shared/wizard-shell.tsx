"use client";

import type { ReactNode } from "react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

export function WizardShell({
  open,
  onOpenChange,
  title,
  description,
  stepCount,
  activeStep,
  onBack,
  onNext,
  onSubmit,
  canAdvance = true,
  isSubmitting = false,
  nextLabel,
  backLabel,
  submitLabel,
  submittingLabel,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  stepCount: number;
  activeStep: number;
  onBack: () => void;
  onNext: () => void;
  onSubmit: () => void;
  canAdvance?: boolean;
  isSubmitting?: boolean;
  nextLabel: string;
  backLabel: string;
  submitLabel: string;
  submittingLabel: string;
  children: ReactNode;
}) {
  const isLast = activeStep === stepCount - 1;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          {description && <DialogDescription>{description}</DialogDescription>}
        </DialogHeader>

        <div className="flex items-center gap-1.5" aria-hidden>
          {Array.from({ length: stepCount }).map((_, i) => (
            <span
              // biome-ignore lint/suspicious/noArrayIndexKey: fixed-length progress dots
              key={i}
              className={cn(
                "h-1.5 flex-1 rounded-full",
                i <= activeStep ? "bg-primary" : "bg-muted",
              )}
            />
          ))}
        </div>

        <div className="grid gap-4 py-2">{children}</div>

        <DialogFooter className="sm:justify-between">
          <Button type="button" variant="outline" onClick={onBack} disabled={activeStep === 0}>
            {backLabel}
          </Button>
          {isLast ? (
            <Button type="button" onClick={onSubmit} disabled={!canAdvance || isSubmitting}>
              {isSubmitting ? submittingLabel : submitLabel}
            </Button>
          ) : (
            <Button type="button" onClick={onNext} disabled={!canAdvance}>
              {nextLabel}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
