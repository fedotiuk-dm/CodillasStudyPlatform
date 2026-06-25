package de.codillas.shared.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated user id (Keycloak subject) from the security context. The resource server
 * sets the JWT {@code sub} as the authentication name.
 */
@Component
public class JwtCurrentUser implements CurrentUser {

  @Override
  public UUID id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user");
    }
    return UUID.fromString(auth.getName());
  }
}
