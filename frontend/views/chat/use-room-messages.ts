"use client";

import { useEffect, useState } from "react";

import { useGetMessages } from "@/lib/api/chat/chat/chat";
import type { ChatMessageResponse } from "@/lib/api/chat/model";
import { getChatSocket } from "@/lib/infrastructure/websocket/chat-socket";

/**
 * Merges REST history with the live STOMP feed for one room. History loads once; new messages
 * arrive on `/topic/chat/{roomId}` and are appended (deduped by id). Sending publishes to
 * `/app/chat/{roomId}/send` — the server echoes it back over the topic.
 */
export function useRoomMessages(roomId: string) {
  const { data, isLoading, isError } = useGetMessages(roomId, undefined, {
    query: { enabled: !!roomId },
  });
  const [live, setLive] = useState<ChatMessageResponse[]>([]);

  // Reset the live buffer when switching rooms.
  useEffect(() => {
    setLive([]);
  }, [roomId]);

  useEffect(() => {
    if (!roomId) return;
    const sub = getChatSocket()
      .watch<ChatMessageResponse>(`/topic/chat/${roomId}`)
      .subscribe((msg) => setLive((prev) => [...prev, msg]));
    return () => sub.unsubscribe();
  }, [roomId]);

  const history = data?.content ?? [];
  const byId = new Map<string, ChatMessageResponse>();
  for (const m of [...history, ...live]) byId.set(m.id, m);
  const messages = [...byId.values()].sort((a, b) => (a.sentAt ?? "").localeCompare(b.sentAt ?? ""));

  function send(content: string) {
    getChatSocket().publish(`/app/chat/${roomId}/send`, { content });
  }

  return { messages, isLoading, isError, send };
}
