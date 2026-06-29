"use client";

import { CircleAlert, Inbox, RotateCcw } from "lucide-react";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";
import { ErrorBoundary } from "react-error-boundary";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

type SkeletonVariant = "list" | "table" | "cards" | "detail";

// Stable, unique keys for the static skeleton placeholders — keyed by identity, not array index.
const LIST_KEYS = ["a", "b", "c"];
const TABLE_KEYS = ["a", "b", "c", "d", "e"];
const CARD_KEYS = ["a", "b", "c", "d", "e", "f"];

interface DataStateProps {
  isLoading: boolean;
  isError: boolean;
  isEmpty?: boolean;
  emptyMessage?: string;
  emptyTitle?: string;
  action?: ReactNode;
  /** Retry handler for the data-error state (e.g. a query `refetch`) — adds a "try again" button. */
  onRetry?: () => void;
  /** Shape the loading skeleton to the content it replaces. Defaults to a list. */
  variant?: SkeletonVariant;
  /** Reset the render-error boundary when any of these change (e.g. `[courseId]`). */
  resetKeys?: unknown[];
  children: ReactNode;
}

/** Shared loading/error/empty surface used by every query-backed view. Also catches render errors
 *  in `children` (not just query errors) so a thrown child shows the error state, never a blank page. */
export function DataState({
  isLoading,
  isError,
  isEmpty,
  emptyMessage,
  emptyTitle,
  action,
  onRetry,
  variant = "list",
  resetKeys,
  children,
}: DataStateProps) {
  if (isLoading) return <LoadingState variant={variant} />;
  if (isError) return <ErrorState onRetry={onRetry} />;
  if (isEmpty)
    return <EmptyState emptyTitle={emptyTitle} emptyMessage={emptyMessage} action={action} />;

  return (
    <ErrorBoundary
      resetKeys={resetKeys}
      fallbackRender={({ resetErrorBoundary }) => <ErrorState onRetry={resetErrorBoundary} />}
    >
      {children}
    </ErrorBoundary>
  );
}

function LoadingState({ variant }: { variant: SkeletonVariant }) {
  const t = useTranslations("common");

  if (variant === "table") {
    return (
      <div className="space-y-2.5" role="status" aria-label={t("loading")}>
        <Skeleton className="h-9 w-full rounded-lg" />
        {TABLE_KEYS.map((k) => (
          <Skeleton key={k} className="h-12 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (variant === "cards") {
    return (
      <div
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
        role="status"
        aria-label={t("loading")}
      >
        {CARD_KEYS.map((k) => (
          <Skeleton key={k} className="h-36 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (variant === "detail") {
    return (
      <div className="space-y-4" role="status" aria-label={t("loading")}>
        <Skeleton className="h-7 w-1/3" />
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  return (
    <div className="space-y-3" role="status" aria-label={t("loading")}>
      <Skeleton className="h-5 w-36" />
      {LIST_KEYS.map((k) => (
        <Skeleton key={k} className="h-14 w-full rounded-xl" />
      ))}
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry?: () => void }) {
  const t = useTranslations("common");
  return (
    <div className="grid min-h-44 place-items-center rounded-xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
      <div>
        <span className="mx-auto grid size-11 place-items-center rounded-xl bg-destructive/10 text-destructive">
          <CircleAlert className="size-5" />
        </span>
        <p className="mt-3 font-medium">{t("loadError")}</p>
        <p className="mt-1 text-muted-foreground text-sm">{t("loadErrorHint")}</p>
        {onRetry ? (
          <Button variant="outline" size="sm" onClick={onRetry} className="mt-4">
            <RotateCcw className="size-4" />
            {t("retry")}
          </Button>
        ) : null}
      </div>
    </div>
  );
}

function EmptyState({
  emptyTitle,
  emptyMessage,
  action,
}: {
  emptyTitle?: string;
  emptyMessage?: string;
  action?: ReactNode;
}) {
  const t = useTranslations("common");
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
