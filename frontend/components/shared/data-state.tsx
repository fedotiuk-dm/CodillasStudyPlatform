import type { ReactNode } from "react";

import { Skeleton } from "@/components/ui/skeleton";

interface DataStateProps {
  isLoading: boolean;
  isError: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  children: ReactNode;
}

/** Standard query surface: skeletons while loading, a message on error/empty, else the content. */
export function DataState({
  isLoading,
  isError,
  isEmpty,
  emptyMessage = "Nothing here yet.",
  children,
}: DataStateProps) {
  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 4 }).map((_, i) => (
          // biome-ignore lint/suspicious/noArrayIndexKey: static skeleton placeholders
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }
  if (isError) {
    return <p className="text-destructive text-sm">Failed to load. Please try again.</p>;
  }
  if (isEmpty) {
    return <p className="text-muted-foreground text-sm">{emptyMessage}</p>;
  }
  return <>{children}</>;
}
