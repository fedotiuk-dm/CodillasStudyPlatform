"use client";

import { Send } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

import { useRoomMessages } from "./use-room-messages";

/** One room's live feed: REST history + STOMP messages, with a composer that publishes to the room. */
export function RoomPanel({ roomId, selfId }: { roomId: string; selfId?: string }) {
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
