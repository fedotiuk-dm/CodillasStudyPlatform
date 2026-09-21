"use client";

import { createContext, type ReactNode, use, useEffect, useRef, useSyncExternalStore } from "react";
import { toast } from "sonner";
import { Role } from "@/lib/constants";
import { keycloak, keycloakInitOptions } from "./keycloak-config";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthSnapshot {
  status: AuthStatus;
  userId: string | undefined;
  name: string | undefined;
  email: string | undefined;
  roles: string[];
}

interface KeycloakState extends AuthSnapshot {
  isInitialized: boolean;
  isAuthenticated: boolean;
  login: (redirectUri?: string) => Promise<void>;
  logout: () => Promise<void>;
}

// Keep the refreshed token valid for at least this long; refresh silently otherwise.
const TOKEN_REFRESH_MIN_VALIDITY_SECONDS = 30;

// External auth store. All mutable token data is captured in a snapshot so the React Compiler
// can memoize safely — reading keycloak.tokenParsed during render would break purity.
function captureSnapshot(status: AuthStatus): AuthSnapshot {
  const token = keycloak.tokenParsed as
    | {
        sub?: string;
        name?: string;
        preferred_username?: string;
        email?: string;
        realm_access?: { roles?: string[] };
      }
    | undefined;

  return {
    status,
    userId: token?.sub,
    name: token?.name || token?.preferred_username,
    email: token?.email,
    roles: token?.realm_access?.roles ?? [],
  };
}

let currentSnapshot: AuthSnapshot = captureSnapshot("loading");
const listeners = new Set<() => void>();

// Guards handleSessionExpired so a burst of failed requests triggers one teardown + toast, not N.
let sessionExpiredHandled = false;

function setAuthStatus(status: AuthStatus) {
  if (status === "authenticated") {
    sessionExpiredHandled = false;
  }
  currentSnapshot = captureSnapshot(status);
  for (const listener of listeners) {
    listener();
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function getSnapshot(): AuthSnapshot {
  return currentSnapshot;
}

const SERVER_SNAPSHOT: AuthSnapshot = {
  status: "loading",
  userId: undefined,
  name: undefined,
  email: undefined,
  roles: [],
};

function getServerSnapshot(): AuthSnapshot {
  return SERVER_SNAPSHOT;
}

// Module-level actions — stable references, no re-creation per render.
async function login(redirectUri?: string) {
  await keycloak.login({ redirectUri: redirectUri ?? globalThis.window?.location.href });
}

async function logout() {
  await keycloak.logout({ redirectUri: globalThis.window?.location.origin });
}

/** Single source of truth for "the session is dead — refresh failed and the token is expired". */
export function handleSessionExpired() {
  if (sessionExpiredHandled) return;
  sessionExpiredHandled = true;
  keycloak.clearToken();
  setAuthStatus("unauthenticated");
  toast.error("Session expired", {
    description: "Please log in again to continue.",
    action: { label: "Log in", onClick: () => void keycloak.login() },
  });
}

/**
 * Keep the access token valid for at least the configured window, refreshing silently if needed.
 * Returns true when the token is usable; tears the session down and returns false when the refresh
 * token is dead. A transient failure that leaves a still-valid token counts as success.
 */
export async function ensureValidToken(): Promise<boolean> {
  if (!keycloak.authenticated) return false;
  await keycloak.updateToken(TOKEN_REFRESH_MIN_VALIDITY_SECONDS).catch(() => undefined);
  if (keycloak.isTokenExpired()) {
    handleSessionExpired();
    return false;
  }
  return true;
}

const KeycloakContext = createContext<KeycloakState | null>(null);

export function KeycloakProvider({ children }: Readonly<{ children: ReactNode }>) {
  const initCalled = useRef(false);
  const snapshot = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  useEffect(() => {
    if (initCalled.current) return;
    initCalled.current = true;

    keycloak
      .init(keycloakInitOptions)
      .then((auth) => setAuthStatus(auth ? "authenticated" : "unauthenticated"))
      .catch(() => setAuthStatus("unauthenticated"));

    keycloak.onTokenExpired = () => {
      void ensureValidToken();
    };
    keycloak.onAuthSuccess = () => setAuthStatus("authenticated");
    keycloak.onAuthError = () => {
      keycloak.clearToken();
      setAuthStatus("unauthenticated");
    };
    keycloak.onAuthRefreshError = () => {
      keycloak.clearToken();
      setAuthStatus("unauthenticated");
    };
    keycloak.onAuthLogout = () => setAuthStatus("unauthenticated");
  }, []);

  const value: KeycloakState = {
    ...snapshot,
    isInitialized: snapshot.status !== "loading",
    isAuthenticated: snapshot.status === "authenticated",
    login,
    logout,
  };

  return <KeycloakContext.Provider value={value}>{children}</KeycloakContext.Provider>;
}

export function useKeycloak(): KeycloakState {
  const context = use(KeycloakContext);
  if (!context) {
    throw new Error("useKeycloak must be used within a KeycloakProvider");
  }
  return context;
}

export function useRoles(): string[] {
  return useKeycloak().roles;
}

// Mirrors the backend RoleHierarchy (ADMIN > TEACHER > STUDENT): a higher role satisfies a
// lower-role gate, so an admin sees teacher/student UI without holding those roles explicitly.
const ROLE_IMPLIES: Record<string, readonly string[]> = {
  ADMIN: ["ADMIN", "TEACHER", "STUDENT"],
  TEACHER: ["TEACHER", "STUDENT"],
  STUDENT: ["STUDENT"],
};

function effectiveRoles(held: string[]): Set<string> {
  const eff = new Set<string>();
  for (const r of held) for (const implied of ROLE_IMPLIES[r] ?? [r]) eff.add(implied);
  return eff;
}

export function useHasRole(role: string): boolean {
  return effectiveRoles(useRoles()).has(role);
}

/** The highest role the user holds. Keycloak grants STUDENT to everyone, so "is a student" means this === STUDENT. */
export function usePrimaryRole(): Role {
  const roles = useRoles();
  if (roles.includes(Role.ADMIN)) return Role.ADMIN;
  if (roles.includes(Role.TEACHER)) return Role.TEACHER;
  return Role.STUDENT;
}

export function useHasAnyRole(requiredRoles: string[]): boolean {
  const eff = effectiveRoles(useRoles());
  return requiredRoles.some((role) => eff.has(role));
}
