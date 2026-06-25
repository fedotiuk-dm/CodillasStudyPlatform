# chat — module contract

Local contract for the `chat` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Full WebSocket chat — copy/adapt from boosting: group channel, student↔teacher DM, per-assignment thread.
- **Key entities:** ChatRoom, ChatMessage
- **Publishes:** MessagePosted
- **Consumes:** StudentEnrolled
- **Depends on (by id / events / API only):** user (by id)
- **OpenAPI spec:** `backend/openapi/chat-paths.yaml` (+ `chat-schemas.yaml`)
- **Status:** skeleton — implement in phase 6.
