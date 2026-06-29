"use client";

import { Bell } from "lucide-react";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Link } from "@/i18n/navigation";
import {
  useListMyNotifications,
  useMarkRead,
} from "@/lib/api/notification/notification/notification";

/**
 * Header notification bell: an unread-count badge + a popover of recent items. Polled every 30s for
 * near-live updates (notifications have no WebSocket channel — only chat does).
 */
export function NotificationBell() {
  const t = useTranslations("notifications");
  const { data } = useListMyNotifications({ size: 8 }, { query: { refetchInterval: 30_000 } });
  const markRead = useMarkRead();

  const items = data?.content ?? [];
  const unread = data?.unread ?? 0;

  // ponytail: no bulk "mark all read" endpoint — loop per unread item. Fine at LMS volume; add a
  // bulk endpoint if a user ever has hundreds of unread notifications.
  function markAllRead() {
    for (const n of items) {
      if (!n.read) markRead.mutate({ notificationId: n.id });
    }
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label={t("title")}>
          <Bell className="h-5 w-5" />
          {unread > 0 && (
            <span className="-right-0.5 -top-0.5 absolute flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 font-medium text-[10px] text-primary-foreground">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between border-b px-4 py-2">
          <span className="font-medium text-sm">{t("title")}</span>
          {unread > 0 && (
            <Button variant="ghost" size="sm" className="h-auto px-1 text-xs" onClick={markAllRead}>
              {t("markAllRead")}
            </Button>
          )}
        </div>

        <div className="max-h-80 overflow-y-auto">
          {items.length === 0 ? (
            <p className="px-4 py-6 text-center text-muted-foreground text-sm">
              {t("allCaughtUp")}
            </p>
          ) : (
            items.map((n) => (
              <button
                key={n.id}
                type="button"
                onClick={() => {
                  if (!n.read) markRead.mutate({ notificationId: n.id });
                }}
                className={`flex w-full flex-col items-start gap-0.5 border-b px-4 py-3 text-left last:border-b-0 hover:bg-muted/50 ${
                  n.read ? "opacity-60" : ""
                }`}
              >
                <span className="flex items-center gap-2 font-medium text-sm">
                  {!n.read && <span className="h-2 w-2 shrink-0 rounded-full bg-primary" />}
                  {n.title}
                </span>
                {n.body && <span className="text-muted-foreground text-xs">{n.body}</span>}
              </button>
            ))
          )}
        </div>

        <div className="border-t p-2">
          <Button asChild variant="ghost" size="sm" className="w-full justify-center text-xs">
            <Link href="/dashboard/notifications">{t("seeAll")}</Link>
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
