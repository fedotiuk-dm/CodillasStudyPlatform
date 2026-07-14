# Accounts — creating and managing users

> Audience: whoever administers the school (no coding needed). The platform has **no
> self-registration** by design (overview §9): every account is created in the **Keycloak
> admin console** and the platform picks it up on the user's first sign-in.

## Where

- Dev: <http://localhost:8080> — master admin `admin` / `admin`.
- Prod: your Keycloak URL; use the real admin credentials set at deploy time.

After signing in, switch the realm selector (top-left) from **master** to **codillas**.

## Create a student

1. **Users → Add user.** Fill *Username*, *Email*, *First/Last name* → **Create**.
2. **Credentials** tab → **Set password** → enter a password, turn **Temporary ON**
   (the user is forced to change it on first sign-in).
3. Done. The realm's default role is **STUDENT** — no role step needed. The profile row
   in the platform is created automatically on the user's first sign-in.
4. Tell the student their username + temporary password. They sign in at the platform URL.

The student still needs to be **enrolled into a group** by a platform admin
(Groups → group → Enroll) before they see courses, homework or announcements.

## Promote to teacher / admin

1. **Users →** pick the user → **Role mapping** tab → **Assign role**.
2. Filter by realm roles, tick `TEACHER` (or `ADMIN`) → **Assign**.
3. The change takes effect on the user's next sign-in (new token).

Roles are graded — `ADMIN > TEACHER > STUDENT` — so an admin never needs the lower
roles assigned explicitly.

## Everyday operations

| Task | Where in Keycloak (realm `codillas`) |
|---|---|
| Reset a forgotten password | Users → user → Credentials → Reset password (Temporary ON) |
| Lock a user out | Users → user → toggle **Enabled** off |
| Delete a user | Users → user → Delete (their submissions/grades stay, keyed by id) |
| See who has which role | Realm roles → role → Users in role |

## Notes

- Email is used for platform notifications — keep it filled and correct
  (the backend picks up email changes on the user's next sign-in).
- Do not delete or rename the realm roles `ADMIN` / `TEACHER` / `STUDENT` — the
  backend authorizes against these exact names.
