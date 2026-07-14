import { Braces } from "lucide-react";

import { SITE_SHORT_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";

export function AppBrand({
  compact = false,
  className,
}: {
  compact?: boolean;
  className?: string;
}) {
  return (
    <div className={cn("flex min-w-0 items-center gap-3", className)}>
      <span className="relative grid size-10 shrink-0 place-items-center overflow-hidden rounded-xl bg-primary text-primary-foreground shadow-md shadow-primary/20">
        <span className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent" />
        <Braces className="relative size-5" strokeWidth={2.25} />
      </span>
      {!compact && (
        <span className="grid min-w-0 leading-none">
          <span className="truncate font-semibold text-base tracking-tight">{SITE_SHORT_NAME}</span>
          <span className="mt-1 text-sidebar-muted text-xs">Study Platform</span>
        </span>
      )}
    </div>
  );
}
