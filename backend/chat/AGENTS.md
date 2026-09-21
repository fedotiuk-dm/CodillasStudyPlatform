# chat — module contract

Local contract for the `chat` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Full WebSocket chat — copy/adapt from boosting: group channel, student↔teacher DM, per-assignment thread.
- **Key entities:** ChatRoom, ChatRoomMember, ChatMessage
- **Publishes:** MessagePosted, DirectMessagePosted (DM rooms only — one clear recipient)
- **Consumes:** GroupCreated (creates the group channel + seats the teacher), StudentEnrolled (adds the student)
- **Depends on (by id / events / API only):** user (by id), files (FileAccessAuthorizer SPI)
- **OpenAPI spec:** `backend/openapi/chat-paths.yaml` (+ `chat-schemas.yaml`)
- **Status:** implemented (v0.1.0 real-time core).

## Conventions

- **Access control = membership.** `ChatRoomMember` gates read/post; a non-member is told the room
  does not exist (404, never leaks existence). No role annotations on room access — it's data-driven.
- **Real-time = STOMP.** The broker/auth infra lives in `main` (`WebSocketConfig` +
  `StompAuthInterceptor`, `@Profile("!integration-test")`): handshake `/ws`, publish `/app/**`,
  subscribe `/topic/chat/{roomId}`. CONNECT is authenticated from the `Authorization: Bearer` header
  (same `roles`→`ROLE_*` mapping as HTTP). `@EnableWebSocketSecurity` guards every frame.
- **REST + WS share the service.** `ChatWsController` (`@MessageMapping`) and the REST `sendMessage`
  both call `ChatService.postMessage`, which persists, broadcasts via `ChatBroadcastService`
  (optional `SimpMessagingTemplate` — no-op when the broker is absent) and publishes `MessagePosted`.
- v1 scope: rooms + members + messages + history. No presence/typing/reactions/read-receipts yet.
