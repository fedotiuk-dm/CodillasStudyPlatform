import { useListProfiles } from "@/lib/api/user/user/user";

/** Resolve userId → display name for showing names instead of UUIDs in lists. */
// ponytail: loads up to 100 profiles; fine for a school-sized dev instance.
export function useProfileNames() {
  const { data } = useListProfiles({ size: 100 });
  const map = new Map((data?.content ?? []).map((p) => [p.userId, p.displayName]));
  return (userId: string) => map.get(userId) ?? userId;
}
