import { RxStompClient } from "./rx-stomp-client";
import { WebSocketConfig } from "./websocket-config";

// ponytail: one module-level STOMP client for the session — connects on first use.
let client: RxStompClient | null = null;

export function getChatSocket(): RxStompClient {
  if (!client) {
    client = new RxStompClient({
      url: WebSocketConfig.getWebSocketUrl(),
      connectHeadersProvider: WebSocketConfig.getConnectHeaders,
    });
    client.connect();
  }
  return client;
}
