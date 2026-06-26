"use client";

import { useTranslations } from "next-intl";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  useListMyNotifications,
  useMarkRead,
} from "@/lib/api/notification/notification/notification";

export function NotificationsView() {
  const t = useTranslations("notifications");
  const { data, isLoading, isError } = useListMyNotifications();
  const markRead = useMarkRead();

  const items = data?.content ?? [];
  const unread = data?.unread ?? 0;

  return (
    <>
      <PageHeader
        title={t("title")}
        description={unread > 0 ? t("unread", { count: unread }) : t("allCaughtUp")}
      />
      <DataState isLoading={isLoading} isError={isError} isEmpty={items.length === 0}>
        <div className="grid gap-3">
          {items.map((n) => (
            <Card key={n.id} className={n.read ? "opacity-70" : undefined}>
              <CardContent className="flex items-start justify-between gap-4 pt-6">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{n.title}</span>
                    {!n.read ? <Badge>{t("new")}</Badge> : null}
                  </div>
                  {n.body ? <p className="text-muted-foreground mt-1 text-sm">{n.body}</p> : null}
                </div>
                {!n.read ? (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={markRead.isPending}
                    onClick={() => markRead.mutate({ notificationId: n.id })}
                  >
                    {t("markRead")}
                  </Button>
                ) : null}
              </CardContent>
            </Card>
          ))}
        </div>
      </DataState>
    </>
  );
}
