import { CircleAlert, Inbox } from "lucide-react";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

import { Skeleton } from "@/components/ui/skeleton";

interface DataStateProps {
  isLoading: boolean;
  isError: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  emptyTitle?: string;
  action?: ReactNode;
  children: ReactNode;
}

/** Shared loading/error/empty surface used by every query-backed view. */
export function DataState({
  isLoading,
  isError,
  isEmpty,
  emptyMessage,
  emptyTitle,
  action,
  children,
}: DataStateProps) {
  const t = useTranslations("common");

  if (isLoading) {
    return (
      <div className="space-y-3" role="status" aria-label={t("loading")}>
        <Skeleton className="h-5 w-36" />
        {Array.from({ length: 3 }).map((_, i) => (
          // biome-ignore lint/suspicious/noArrayIndexKey: static skeleton placeholders
          <Skeleton key={i} className="h-14 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="grid min-h-44 place-items-center rounded-xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
        <div>
          <span className="mx-auto grid size-11 place-items-center rounded-xl bg-destructive/10 text-destructive">
            <CircleAlert className="size-5" />
          </span>
          <p className="mt-3 font-medium">{t("loadError")}</p>
          <p className="mt-1 text-muted-foreground text-sm">{t("loadErrorHint")}</p>
        </div>
      </div>
    );
  }

  if (isEmpty) {
    return (
      <div className="grid min-h-44 place-items-center rounded-xl border border-dashed bg-muted/20 px-6 py-10 text-center">
        <div className="max-w-sm">
          <span className="mx-auto grid size-11 place-items-center rounded-xl bg-primary/10 text-primary">
            <Inbox className="size-5" />
          </span>
          <p className="mt-3 font-medium">{emptyTitle ?? t("emptyTitle")}</p>
          <p className="mt-1 text-muted-foreground text-sm">{emptyMessage ?? t("emptyHint")}</p>
          {action ? <div className="mt-4 flex justify-center">{action}</div> : null}
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
