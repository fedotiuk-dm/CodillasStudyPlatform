"use client";

import { Send } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useListMyRooms } from "@/lib/api/chat/chat/chat";
import { useKeycloak } from "@/lib/auth";
import { cn } from "@/lib/utils";
import { CreateRoomDialog } from "./create-room-dialog";
import { useRoomMessages } from "./use-room-messages";

function RoomPanel({ roomId, selfId }: { roomId: string; selfId?: string }) {
  const t = useTranslations("chat");
  const { messages, isLoading, send } = useRoomMessages(roomId);
  const [draft, setDraft] = useState("");

  function onSend(e: React.FormEvent) {
    e.preventDefault();
    const content = draft.trim();
    if (!content) return;
    send(content);
    setDraft("");
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 space-y-2 overflow-y-auto p-4">
        {isLoading && <p className="text-muted-foreground text-sm">{t("loading")}</p>}
        {!isLoading && messages.length === 0 && (
          <p className="text-muted-foreground text-sm">{t("noMessages")}</p>
        )}
        {messages.map((m) => {
          const mine = m.senderId === selfId;
          return (
            <div key={m.id} className={cn("flex", mine ? "justify-end" : "justify-start")}>
              <div
                className={cn(
                  "max-w-[75%] rounded-lg px-3 py-2 text-sm",
                  mine ? "bg-primary text-primary-foreground" : "bg-muted",
                )}
              >
                {m.content}
              </div>
            </div>
          );
        })}
      </div>
      <form onSubmit={onSend} className="flex items-center gap-2 border-t p-3">
        <Input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={t("messagePlaceholder")}
        />
        <Button type="submit" size="icon" disabled={!draft.trim()}>
          <Send className="size-4" />
        </Button>
      </form>
    </div>
  );
}

export function ChatView() {
  const t = useTranslations("chat");
  const { userId } = useKeycloak();
  const { data } = useListMyRooms();
  const rooms = data ?? [];
  const [activeRoom, setActiveRoom] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

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
