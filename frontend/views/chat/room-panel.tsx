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
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-2 overflow-y-auto bg-muted/15 p-4 sm:p-5">
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
                  "max-w-[85%] rounded-2xl px-3.5 py-2.5 text-sm shadow-xs sm:max-w-[70%]",
                  mine
                    ? "rounded-br-md bg-primary text-primary-foreground"
                    : "rounded-bl-md border bg-card",
                )}
              >
                {m.content}
              </div>
            </div>
          );
        })}
      </div>
      <form onSubmit={onSend} className="flex items-center gap-2 border-t bg-card p-3 sm:p-4">
        <Input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          className="h-10 rounded-xl bg-background"
          placeholder={t("messagePlaceholder")}
        />
        <Button type="submit" size="icon" className="size-10 rounded-xl" disabled={!draft.trim()}>
          <Send className="size-4" />
        </Button>
      </form>
    </div>
  );
}
