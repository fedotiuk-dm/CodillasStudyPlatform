import { Circle } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type StatusTone = "neutral" | "info" | "warning" | "success" | "danger";

const STATUS_TONE: Record<string, StatusTone> = {
  DRAFT: "neutral",
  PUBLISHED: "info",
  RUNNING: "success",
  ARCHIVED: "neutral",
  IN_PROGRESS: "info",
  SUBMITTED: "warning",
  IN_REVIEW: "warning",
  RETURNED: "danger",
  GRADED: "success",
  OVERDUE: "danger",
};

const TONE_CLASS: Record<StatusTone, string> = {
  neutral: "border-border bg-muted text-muted-foreground",
  info: "border-info/20 bg-info/10 text-info",
  warning: "border-warning/25 bg-warning/12 text-warning",
  success: "border-success/20 bg-success/10 text-success",
  danger: "border-destructive/20 bg-destructive/10 text-destructive",
};

/** Central semantic presentation for every lifecycle status in the application. */
export function StatusBadge({ status, label }: { status: string; label: string }) {
  const tone = STATUS_TONE[status] ?? "neutral";

  return (
    <Badge variant="outline" className={cn("gap-1.5 rounded-full px-2.5", TONE_CLASS[tone])}>
      <Circle className="size-1.5 fill-current" aria-hidden="true" />
      {label}
    </Badge>
  );
}
