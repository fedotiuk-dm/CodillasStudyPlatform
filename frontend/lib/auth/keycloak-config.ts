import Keycloak, { type KeycloakInitOptions } from "keycloak-js";

export interface KeycloakConfiguration {
  url: string;
  realm: string;
  clientId: string;
}

export const keycloakConfig: KeycloakConfiguration = {
  url: process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:8080",
  realm: process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "codillas",
  clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "codillas-frontend",
};

// `check-sso` silently restores an existing session if present but never auto-redirects to the
// login form — the app decides when to call keycloak.login() (a guarded route, or the header).
export const keycloakInitOptions: KeycloakInitOptions = {
  onLoad: "check-sso",
  checkLoginIframe: false,
  pkceMethod: "S256",
};

export const keycloak = new Keycloak(keycloakConfig);
