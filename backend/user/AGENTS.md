# user — module contract

Local contract for the `user` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** User profiles keyed by the Keycloak subject. Users are created by an admin
  directly in the **Keycloak console** (no backend admin-client); the default role STUDENT comes
  from the realm's default-roles. The platform reads/updates the profile (`/api/users/me`) and
  surfaces the caller's granted roles on it (`UserProfile.roles`, derived from the JWT via
  `CurrentUser.roles()` — not stored on the entity, list results omit them).
- **Key entities:** Profile (platform roles live in `shared.security.Role`; the `ADMIN > TEACHER >
  STUDENT` hierarchy is configured in `main` `config/RoleHierarchyConfig`)
- **Publishes:** `UserEmailChanged` (notification keeps a local address read model from it)
- **Consumes:** —
- **Depends on (by id / events / API only):** — (identity from the JWT via `shared` `CurrentUser`)
- **OpenAPI spec:** `backend/openapi/user-paths.yaml` (+ `user-schemas.yaml`)
- **Status:** "me" profile feature done (GET/PUT). Admin user management stays in Keycloak.

## Conventions

- **The Keycloak subject is the identity, everywhere.** `CurrentUser.id()` is the token `sub`, and
  every cross-module reference (`submissions.student_id`, `group_members.user_id`,
  `notifications.recipient_id`, …) stores that — never `user_profiles.id`, which is a local PK
  nothing points at. A profile is therefore a *cache of identity*, not the identity.
- **Never delete a profile row.** Deleting a user is done in the Keycloak console; the local
  profile stays behind as a tombstone. It is the only thing that renders a `sub` as a human name,
  so dropping it turns every past grade, submission and chat message by that person into a bare
  UUID in the UI — while the rows themselves survive, since they key on the `sub`. There is
  deliberately no delete endpoint; do not add one.
- Re-provisioning is safe by construction: a returning user hits `getMyProfile`, which recreates
  the row from the token under the same `user_id` (UNIQUE), and all their history reattaches. The
  only thing lost is a locally-edited display name.
