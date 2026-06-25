package de.codillas.shared.security;

/**
 * Platform roles, backed by Keycloak realm roles. New users default to {@link #STUDENT} (enforced
 * by the realm's {@code default-roles-codillas} composite); promotion to {@code TEACHER} / {@code
 * ADMIN} is explicit. Use the {@code @Requires*} annotations to guard endpoints; this enum is the
 * source of truth for role names elsewhere.
 */
public enum Role {
  ADMIN,
  TEACHER,
  STUDENT;

  /** System default for newly created users. */
  public static final Role DEFAULT = STUDENT;

  /** Spring Security authority name, e.g. {@code ROLE_STUDENT}. */
  public String authority() {
    return "ROLE_" + name();
  }
}
