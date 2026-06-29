"use client";

import { ArrowLeft, MessageCircle } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useListMyRooms } from "@/lib/api/chat/chat/chat";
import { useKeycloak } from "@/lib/auth";
import { cn } from "@/lib/utils";

import { CreateRoomDialog } from "./create-room-dialog";
import { RoomPanel } from "./room-panel";

export function ChatView() {
  const t = useTranslations("chat");
  const { userId } = useKeycloak();
  const { data } = useListMyRooms();
  const rooms = data ?? [];
  const [activeRoom, setActiveRoom] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  // Deep-link: /dashboard/chat?room={id} (e.g. clicked from a notification) opens that room.
  const roomParam = useSearchParams().get("room");
  useEffect(() => {
    if (roomParam) setActiveRoom(roomParam);
  }, [roomParam]);

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("description")}
        action={<Button onClick={() => setCreateOpen(true)}>{t("newRoom")}</Button>}
      />

      <Card className="grid h-[calc(100dvh-12rem)] min-h-[32rem] overflow-hidden p-0 md:grid-cols-[17rem_minmax(0,1fr)]">
        <div className={cn("overflow-y-auto md:border-r", activeRoom && "hidden md:block")}>
          {rooms.length === 0 && (
            <div className="grid h-full place-items-center p-6 text-center">
              <div>
                <span className="mx-auto grid size-11 place-items-center rounded-xl bg-primary/10 text-primary">
                  <MessageCircle className="size-5" />
                </span>
                <p className="mt-3 text-muted-foreground text-sm">{t("noRooms")}</p>
              </div>
            </div>
          )}
          {rooms.map((r) => (
            <button
              type="button"
              key={r.id}
              onClick={() => setActiveRoom(r.id)}
              className={cn(
                "flex min-h-14 w-full items-center gap-3 border-b px-4 py-3 text-left text-sm transition-colors hover:bg-muted/60",
                activeRoom === r.id && "bg-primary/8 font-medium text-primary",
              )}
            >
              <span className="grid size-9 shrink-0 place-items-center rounded-full bg-secondary text-secondary-foreground">
                <MessageCircle className="size-4" />
              </span>
              <span className="truncate">{r.name || r.type}</span>
            </button>
          ))}
        </div>
        {activeRoom ? (
          <div className="flex min-h-0 min-w-0 flex-col">
            <div className="flex h-12 items-center gap-2 border-b px-3 md:hidden">
              <Button
                variant="ghost"
                size="icon"
                aria-label={t("backToRooms")}
                onClick={() => setActiveRoom(null)}
              >
                <ArrowLeft className="size-5" />
              </Button>
              <span className="truncate font-medium text-sm">
                {rooms.find((room) => room.id === activeRoom)?.name ?? t("conversation")}
              </span>
            </div>
            <RoomPanel roomId={activeRoom} selfId={userId} />
          </div>
        ) : (
          <div className="hidden items-center justify-center text-muted-foreground text-sm md:flex">
            {t("selectRoom")}
          </div>
        )}
      </Card>

      <CreateRoomDialog open={createOpen} onOpenChange={setCreateOpen} />
    </>
  );
}
