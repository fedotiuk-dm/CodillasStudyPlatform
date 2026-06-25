# user — module contract

Local contract for the `user` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** User profiles keyed by the Keycloak subject. Users are created by an admin
  directly in the **Keycloak console** (no backend admin-client); the default role STUDENT comes
  from the realm's default-roles. The platform only reads/updates the profile (`/api/users/me`).
- **Key entities:** Profile (platform roles live in `shared.security.Role`)
- **Publishes:** —
- **Consumes:** —
- **Depends on (by id / events / API only):** — (identity from the JWT via `shared` `CurrentUser`)
- **OpenAPI spec:** `backend/openapi/user-paths.yaml` (+ `user-schemas.yaml`)
- **Status:** "me" profile feature done (GET/PUT). Admin user management stays in Keycloak.
