package de.codillas.config;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import de.codillas.shared.security.CurrentUser;

/**
 * The {@link CurrentUser} implementation. Lives in {@code main} (not {@code shared}) because
 * reading JWT claims needs the OAuth2 resource server on the classpath, which only {@code main}
 * assembles. The resource server sets the JWT {@code sub} as the authentication name and the {@link
 * Jwt} as the principal.
 */
@Component
public class JwtCurrentUser implements CurrentUser {

  @Override
  public UUID id() {
    return UUID.fromString(authentication().getName());
  }

  @Override
  public String displayName() {
    Authentication auth = authentication();
    if (auth instanceof JwtAuthenticationToken token) {
      Jwt jwt = token.getToken();
      String name = jwt.getClaimAsString("name");
      if (name == null) {
        name = jwt.getClaimAsString("preferred_username");
      }
      if (name != null) {
        return name;
      }
    }
    return auth.getName();
  }

  private static Authentication authentication() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user");
    }
    return auth;
  }
}
