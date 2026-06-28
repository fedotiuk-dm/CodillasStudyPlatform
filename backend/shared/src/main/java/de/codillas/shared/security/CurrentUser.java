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

  /** The verified email from the token's {@code email} claim, or {@code null} if absent. */
  String email();

  /**
   * True if this request was granted {@code role} (the raw granted role; hierarchy is not applied
   * here).
   */
  default boolean hasRole(Role role) {
    return roles().contains(role.name());
  }

  /**
   * True for staff — {@link Role#TEACHER} or {@link Role#ADMIN}. The object-level authorization
   * bypass: staff may read and grade any student's work. Tests both raw roles, so no RoleHierarchy
   * lookup is needed.
   */
  default boolean isStaff() {
    return hasRole(Role.ADMIN) || hasRole(Role.TEACHER);
  }
}
