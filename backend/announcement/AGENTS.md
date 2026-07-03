# announcement — module contract

Local contract for the `announcement` module. Follows the chain: `backend/AGENTS.md` (global) → this file.
Build/fill with the **`new-modulith-module`** skill — do not scaffold blind.

- **Responsibility:** Group-scoped announcements — the class stream: teachers post, members read.
- **Key entities:** Announcement, AnnouncementMembership (local group roster)
- **Publishes:** AnnouncementPosted (→ notification fans out to the group)
- **Consumes:** StudentEnrolled (roster), GroupDeleted (purge)
- **Depends on (by id / events / API only):** enrollment (group by id), user (author by id)
- **OpenAPI spec:** `backend/openapi/announcement-paths.yaml` (+ `announcement-schemas.yaml`)

## Conventions

- **Stream order** is pinned-first, then newest (`AnnouncementRepository.STREAM_ORDER`).
- **Reads are member-or-staff.** The module keeps its own `AnnouncementMembership` roster (fed by
  `StudentEnrolled`, purged by `GroupDeleted`) — a non-member caller gets **404, never 403**
  (don't leak the group's existence). Staff (TEACHER/ADMIN) read any group.
- **Writes are `@RequiresTeacher`** (post/edit/delete); the module does not validate the group id —
  it is a by-id reference like everywhere else.
- `GET /api/me/announcements` is the dashboard feed: a student sees their groups' posts, staff see
  the latest across all groups.
