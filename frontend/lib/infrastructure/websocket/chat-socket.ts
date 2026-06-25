import { keycloak } from "@/lib/auth";

import { RxStompClient } from "./rx-stomp-client";

// ponytail: one module-level STOMP client for the session — connects on first use.
let client: RxStompClient | null = null;

export function getChatSocket(): RxStompClient {
  if (!client) {
    client = new RxStompClient({
      url: process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8081/ws",
      // STOMP CONNECT headers carry the current JWT (kept out of the URL / server logs).
      connectHeadersProvider: (): Record<string, string> =>
        keycloak.token ? { Authorization: `Bearer ${keycloak.token}` } : {},
    });
    client.connect();
  }
  return client;
}
