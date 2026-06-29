"use client";

import { Bell } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Link, useRouter } from "@/i18n/navigation";
import { NotificationType } from "@/lib/api/notification/model";
import {
  useListMyNotifications,
  useMarkAllRead,
  useMarkRead,
} from "@/lib/api/notification/notification/notification";
import { useLocalizedNotification } from "@/lib/hooks/use-localized-notification";
import { useNotificationSound } from "@/lib/hooks/use-notification-sound";

/** Where clicking a notification takes you — a chat notification opens that specific room. */
function notificationHref(type: NotificationType, referenceId?: string): string {
  switch (type) {
    case NotificationType.DIRECT_MESSAGE:
      return referenceId ? `/dashboard/chat?room=${referenceId}` : "/dashboard/chat";
    case NotificationType.ATTEMPT_COMPLETED:
      return "/dashboard/tests";
    default:
      // ASSIGNMENT_PUBLISHED / ASSIGNMENT_DUE_SOON / SUBMISSION_GRADED
      return "/dashboard/homework";
  }
}

/**
 * Header notification bell: an unread-count badge + a popover of recent items. Polled every 30s for
 * near-live updates (notifications have no WebSocket channel — only chat does).
 */
export function NotificationBell() {
  const t = useTranslations("notifications");
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const { data } = useListMyNotifications({ size: 8 }, { query: { refetchInterval: 30_000 } });
  const markRead = useMarkRead();
  const markAll = useMarkAllRead();
  const localize = useLocalizedNotification();

  const items = data?.content ?? [];
  const unread = data?.unread ?? 0;

  // Chime when the unread count rises (a new notification arrived between polls), like boosting.
  const { playNotification } = useNotificationSound();
  const prevUnread = useRef(unread);
  useEffect(() => {
    if (unread > prevUnread.current) playNotification();
    prevUnread.current = unread;
  }, [unread, playNotification]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
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
            <Button
              variant="ghost"
              size="sm"
              className="h-auto px-1 text-xs"
              onClick={() => markAll.mutate()}
            >
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
            items.map((n) => {
              const { title, body } = localize(n);
              return (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => {
                    if (!n.read) markRead.mutate({ notificationId: n.id });
                    setOpen(false);
                    router.push(notificationHref(n.type, n.referenceId));
                  }}
                  className={`flex w-full flex-col items-start gap-0.5 border-b px-4 py-3 text-left last:border-b-0 hover:bg-muted/50 ${
                    n.read ? "opacity-60" : ""
                  }`}
                >
                  <span className="flex items-center gap-2 font-medium text-sm">
                    {!n.read && <span className="h-2 w-2 shrink-0 rounded-full bg-primary" />}
                    {title}
                  </span>
                  {body && <span className="text-muted-foreground text-xs">{body}</span>}
                </button>
              );
            })
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
