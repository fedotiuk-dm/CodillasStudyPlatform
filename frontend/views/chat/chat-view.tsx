"use client";

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

      <Card className="grid h-[70vh] grid-cols-[16rem_1fr] overflow-hidden p-0">
        <div className="overflow-y-auto border-r">
          {rooms.length === 0 && (
            <p className="p-4 text-muted-foreground text-sm">{t("noRooms")}</p>
          )}
          {rooms.map((r) => (
            <button
              type="button"
              key={r.id}
              onClick={() => setActiveRoom(r.id)}
              className={cn(
                "w-full border-b px-4 py-3 text-left text-sm hover:bg-muted",
                activeRoom === r.id && "bg-muted font-medium",
              )}
            >
              {r.name || r.type}
            </button>
          ))}
        </div>
        {activeRoom ? (
          <RoomPanel roomId={activeRoom} selfId={userId} />
        ) : (
          <div className="flex items-center justify-center text-muted-foreground text-sm">
            {t("selectRoom")}
          </div>
        )}
      </Card>

      <CreateRoomDialog open={createOpen} onOpenChange={setCreateOpen} />
    </>
  );
}
