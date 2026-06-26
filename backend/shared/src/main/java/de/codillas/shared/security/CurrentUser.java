package de.codillas.shared.security;

import java.util.Set;
import java.util.UUID;

/** Accessor for the currently authenticated user's identity. */
public interface CurrentUser {

  /** The Keycloak subject (user id) of the authenticated request. */
  UUID id();

  /**
   * A human display name from the token ({@code name} / {@code preferred_username}), else the id.
   */
  String displayName();

  /**
   * The platform roles granted to this request (Keycloak realm roles, {@code ROLE_} prefix stripped
   * — e.g. {@code ADMIN}, {@code TEACHER}). The raw granted roles; the {@code ADMIN > TEACHER >
   * STUDENT} hierarchy is applied at authorization time, not reflected here.
   */
  Set<String> roles();
}
