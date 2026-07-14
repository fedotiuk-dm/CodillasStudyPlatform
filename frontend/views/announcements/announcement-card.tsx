"use client";

import { Pin, PinOff, Trash2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  useDeleteAnnouncement,
  useUpdateAnnouncement,
} from "@/lib/api/announcement/announcement/announcement";
import type { AnnouncementResponse } from "@/lib/api/announcement/model";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

/** One stream post: title, body, author + date, pinned badge; staff can pin/unpin and delete. */
export function AnnouncementCard({
  announcement,
  authorName,
}: {
  announcement: AnnouncementResponse;
  authorName: string;
}) {
  const t = useTranslations("announcements");
  const tc = useTranslations("common");
  const locale = useLocale();
  const isStaff = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const update = useUpdateAnnouncement();
  const remove = useDeleteAnnouncement();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const posted = announcement.createdAt
    ? new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" }).format(
        new Date(announcement.createdAt),
      )
    : "";

  function togglePin() {
    update.mutate({
      announcementId: announcement.id,
      data: { pinned: !announcement.pinned },
    });
  }

  function onDelete() {
    remove.mutate(
      { announcementId: announcement.id },
      {
        onSuccess: () => {
          setConfirmOpen(false);
          toast.success(t("deleted"));
        },
      },
    );
  }

  return (
    <Card className={announcement.pinned ? "border-primary/40" : undefined}>
      <CardContent className="flex items-start justify-between gap-4 pt-6">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-semibold">{announcement.title}</span>
            {announcement.pinned && <Badge>{t("pinnedBadge")}</Badge>}
          </div>
          {announcement.body && (
            <p className="mt-1.5 whitespace-pre-wrap text-muted-foreground text-sm">
              {announcement.body}
            </p>
          )}
          <p className="mt-2 text-muted-foreground text-xs">
            {authorName} · {posted}
          </p>
        </div>

        {isStaff && (
          <div className="flex shrink-0 gap-1">
            <Button
              variant="ghost"
              size="icon"
              disabled={update.isPending}
              onClick={togglePin}
              aria-label={announcement.pinned ? t("unpin") : t("pin")}
            >
              {announcement.pinned ? <PinOff className="size-4" /> : <Pin className="size-4" />}
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setConfirmOpen(true)}
              aria-label={tc("delete")}
            >
              <Trash2 className="size-4" />
            </Button>
          </div>
        )}
      </CardContent>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={t("deleteTitle")}
        description={t("deleteDescription")}
        confirmLabel={tc("delete")}
        cancelLabel={tc("cancel")}
        onConfirm={onDelete}
        pending={remove.isPending}
      />
    </Card>
  );
}
