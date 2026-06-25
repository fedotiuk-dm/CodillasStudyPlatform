# user — module contract

Local contract for the `user` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Profiles + roles on top of Keycloak; admin/teacher create users via keycloak-admin-client (default role STUDENT).
- **Key entities:** User, Profile, Role (enum)
- **Publishes:** —
- **Consumes:** —
- **Depends on (by id / events / API only):** Keycloak (admin client)
- **OpenAPI spec:** `backend/openapi/user-paths.yaml` (+ `user-schemas.yaml`)
- **Status:** skeleton — implement in phase 1.
