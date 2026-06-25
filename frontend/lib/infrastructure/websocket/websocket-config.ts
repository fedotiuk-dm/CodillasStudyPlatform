import { keycloak } from "@/lib/auth";

/** WebSocket connection settings. Auth rides the STOMP CONNECT frame, never the URL. */
export const WebSocketConfig = {
  getWebSocketUrl(): string {
    return process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8081/ws";
  },

  /** STOMP CONNECT headers carrying the current JWT (kept out of the URL / server logs). */
  getConnectHeaders(): Record<string, string> {
    const token = keycloak.token;
    return token ? { Authorization: `Bearer ${token}` } : {};
  },
};
