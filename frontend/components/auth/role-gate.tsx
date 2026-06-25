"use client";

import type { ReactNode } from "react";
import { useEffect, useRef } from "react";

import { useKeycloak } from "@/lib/auth";
import type { Role } from "@/lib/constants";
import { AuthLoading } from "./auth-loading";

/**
 * Client-side route guard. Unauthenticated users are sent to Keycloak; authenticated users without
 * one of {@code roles} get a 403 surface. (The JWT lives in browser memory, so gating must be
 * client-side — a server component can't read it.)
 */
export function RoleGate({ children, roles }: { children: ReactNode; roles?: readonly Role[] }) {
  const { isInitialized, isAuthenticated, roles: userRoles, login } = useKeycloak();
  const loginTriggered = useRef(false);

  useEffect(() => {
    if (!isInitialized || isAuthenticated || loginTriggered.current) return;
    loginTriggered.current = true;
    void login();
  }, [isInitialized, isAuthenticated, login]);

  if (!isInitialized || !isAuthenticated) {
    return <AuthLoading message="Redirecting to sign in…" />;
  }
  if (roles?.length && !roles.some((role) => userRoles.includes(role))) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        You don&apos;t have access to this page.
      </div>
    );
  }
  return <>{children}</>;
}
