import { RxStomp, RxStompState } from "@stomp/rx-stomp";
import type { Observable } from "rxjs";
import { map } from "rxjs/operators";

import type { ConnectionConfig, ConnectionState } from "./types";

const STATE_MAP: Record<RxStompState, ConnectionState> = {
  [RxStompState.CONNECTING]: "connecting",
  [RxStompState.OPEN]: "connected",
  [RxStompState.CLOSING]: "disconnected",
  [RxStompState.CLOSED]: "disconnected",
};

function parseMessageBody(body: string): unknown {
  try {
    return JSON.parse(body);
  } catch {
    return body;
  }
}

/**
 * Thin reactive STOMP client over WebSocket. Connection lifecycle, auto-reconnect, subscription
 * re-establishment, and offline publish queueing are all delegated to RxStomp. This wrapper only
 * maps RxStomp's state enum to the app's {@link ConnectionState} and parses message bodies.
 */
export class RxStompClient {
  private readonly rxStomp: RxStomp;

  constructor(config: Partial<ConnectionConfig> = {}) {
    this.rxStomp = new RxStomp();
    const headersProvider = config.connectHeadersProvider;

    this.rxStomp.configure({
      brokerURL: config.url ?? "",
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      // Refresh auth headers before each (re)connect so the CONNECT frame always has a fresh JWT.
      beforeConnect: headersProvider
        ? (client) => {
            client.configure({ connectHeaders: headersProvider() });
          }
        : undefined,
    });
  }

  get connectionState$(): Observable<ConnectionState> {
    return this.rxStomp.connectionState$.pipe(map((state) => STATE_MAP[state] ?? "disconnected"));
  }

  connect(): void {
    this.rxStomp.activate();
  }

  disconnect(): Promise<void> {
    return this.rxStomp.deactivate();
  }

  /** Subscribe to a destination; RxStomp re-subscribes automatically on reconnect. */
  watch<T = unknown>(destination: string): Observable<T> {
    return this.rxStomp
      .watch(destination)
      .pipe(map((message) => parseMessageBody(message.body) as T));
  }

  /** Publish to a destination; RxStomp queues the frame locally while offline. */
  publish(destination: string, body: unknown, headers?: Record<string, string>): void {
    this.rxStomp.publish({ destination, body: JSON.stringify(body), headers });
  }
}
