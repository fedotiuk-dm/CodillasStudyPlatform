export type ConnectionState = "connecting" | "connected" | "disconnected";

export interface ConnectionConfig {
  url: string;
  /** Called before every (re)connect so the STOMP CONNECT frame carries a fresh JWT. */
  connectHeadersProvider?: () => Record<string, string>;
}
