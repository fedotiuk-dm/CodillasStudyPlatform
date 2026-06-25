"use client";

import { useGetMyProfile } from "@/lib/api/user/user/user";
import { useKeycloak } from "@/lib/auth";

/**
 * Fetches the current user's profile once authenticated. The GET lazily provisions the profile on
 * the backend, so a user always has one right after their first Keycloak login. Renders nothing.
 */
export function ProfileInit() {
  const { isInitialized, isAuthenticated } = useKeycloak();
  useGetMyProfile({ query: { enabled: isInitialized && isAuthenticated } });
  return null;
}
