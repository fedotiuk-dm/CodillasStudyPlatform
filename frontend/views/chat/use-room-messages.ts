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

  // REST history is NEWEST_FIRST → reverse to oldest-first; live messages arrive chronologically
  // and go last. We don't sort by sentAt: a just-broadcast message has no sentAt yet (the server
  // emits it before the timestamp is flushed), and (null) would otherwise sort it to the top.
  const history = data?.content ?? [];
  const byId = new Map<string, ChatMessageResponse>();
  for (const m of [...[...history].reverse(), ...live]) byId.set(m.id, m);
  const messages = [...byId.values()];

  function send(content: string) {
    getChatSocket().publish(`/app/chat/${roomId}/send`, { content });
  }

  return { messages, isLoading, isError, send };
}
