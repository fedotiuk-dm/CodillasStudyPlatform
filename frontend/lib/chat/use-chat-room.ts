"use client";

import { useEffect, useRef, useState } from "react";

import type { ChatMessageResponse } from "@/lib/api/chat/model";
import { RxStompClient } from "@/lib/infrastructure/websocket/rx-stomp-client";
import type { ConnectionState } from "@/lib/infrastructure/websocket/types";
import { WebSocketConfig } from "@/lib/infrastructure/websocket/websocket-config";

/**
 * Real-time chat for one room. Subscribes to {@code /topic/chat/{roomId}} and sends via
 * {@code /app/chat/{roomId}/send}. Seed {@code initialMessages} from the REST history hook
 * (orval-generated) and this hook appends live ones on top.
 */
export function useChatRoom(
  roomId: string | undefined,
  initialMessages: ChatMessageResponse[] = [],
) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>(initialMessages);
  const [connection, setConnection] = useState<ConnectionState>("disconnected");
  const clientRef = useRef<RxStompClient | null>(null);

  useEffect(() => {
    if (!roomId) return;

    const client = new RxStompClient({
      url: WebSocketConfig.getWebSocketUrl(),
      connectHeadersProvider: () => WebSocketConfig.getConnectHeaders(),
    });
    clientRef.current = client;

    const stateSub = client.connectionState$.subscribe(setConnection);
    const messageSub = client
      .watch<ChatMessageResponse>(`/topic/chat/${roomId}`)
      .subscribe((message) => setMessages((prev) => [...prev, message]));
    client.connect();

    return () => {
      stateSub.unsubscribe();
      messageSub.unsubscribe();
      void client.disconnect();
      clientRef.current = null;
    };
  }, [roomId]);

  function sendMessage(content: string) {
    if (!roomId || !content.trim()) return;
    clientRef.current?.publish(`/app/chat/${roomId}/send`, { content });
  }

  return { messages, connection, sendMessage };
}
