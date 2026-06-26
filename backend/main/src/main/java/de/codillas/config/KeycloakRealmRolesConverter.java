package de.codillas.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Extracts realm roles from a Keycloak JWT's standard nested {@code realm_access.roles} claim and
 * maps each to a {@code ROLE_<role>} authority (so {@code hasRole('ADMIN')} and the {@code ADMIN >
 * TEACHER > STUDENT} hierarchy work).
 *
 * <p>Reading {@code realm_access.roles} — which Keycloak always puts in the access token — instead
 * of a custom flat {@code roles} claim means role mapping works with any standard token, with no
 * dependency on a per-client protocol mapper being present and aimed at the access token. Used by
 * both authentication paths (the HTTP resource server's {@code JwtAuthenticationConverter} and the
 * STOMP CONNECT auth manager), so the claim location lives in exactly one place.
 */
@Component
public class KeycloakRealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (realmAccess == null || !(realmAccess.get(ROLES_CLAIM) instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }
}
