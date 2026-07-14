"use client";

import { format } from "date-fns";
import { useTranslations } from "next-intl";

import { Badge } from "@/components/ui/badge";
import { AttemptTimer } from "./attempt-timer";

// Presentational meta row for an in-progress attempt: which attempt this is, the live countdown
// (timed tests only), and the availability window. All branching lives here so the wizard stays flat.
export function AttemptHeader({
  attemptNumber,
  deadline,
  availableFrom,
  availableUntil,
  onExpire,
}: {
  attemptNumber: number;
  deadline?: number;
  availableFrom?: string;
  availableUntil?: string;
  onExpire: () => void;
}) {
  const t = useTranslations("tests");
  const note = availableUntil
    ? t("availableUntilNote", { date: format(new Date(availableUntil), "PPp") })
    : availableFrom
      ? t("availableFromNote", { date: format(new Date(availableFrom), "PPp") })
      : undefined;

  return (
    <div className="grid gap-1.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Badge variant="secondary">{t("attemptNumber", { number: attemptNumber })}</Badge>
        {deadline !== undefined && <AttemptTimer deadline={deadline} onExpire={onExpire} />}
      </div>
      {note && <p className="text-muted-foreground text-xs">{note}</p>}
    </div>
  );
}
