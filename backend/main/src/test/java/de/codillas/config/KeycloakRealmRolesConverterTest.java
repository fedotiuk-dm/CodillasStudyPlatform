package de.codillas.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KeycloakRealmRolesConverter")
class KeycloakRealmRolesConverterTest {

  private final KeycloakRealmRolesConverter converter = new KeycloakRealmRolesConverter();

  private static Jwt jwtWith(Map<String, Object> claims) {
    Jwt.Builder builder =
        Jwt.withTokenValue("t").header("alg", "none").issuedAt(Instant.EPOCH).subject("u");
    claims.forEach(builder::claim);
    return builder.build();
  }

  @Test
  @DisplayName("maps realm_access.roles to ROLE_* authorities")
  void mapsRealmRoles() {
    Jwt jwt = jwtWith(Map.of("realm_access", Map.of("roles", List.of("ADMIN", "TEACHER"))));
    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TEACHER");
  }

  @Test
  @DisplayName("returns no authorities when realm_access is absent")
  void noRealmAccess_empty() {
    assertThat(converter.convert(jwtWith(Map.of("scope", "openid")))).isEmpty();
  }

  @Test
  @DisplayName("returns no authorities when realm_access has no roles list")
  void noRoles_empty() {
    Jwt jwt = jwtWith(Map.of("realm_access", Map.of("other", "x")));
    assertThat(converter.convert(jwt)).isEmpty();
  }

  @Test
  @DisplayName("an admin's ROLE_ADMIN authority is recognized as a granted role")
  void sanity_authorityShape() {
    Jwt jwt = jwtWith(Map.of("realm_access", Map.of("roles", List.of("ADMIN"))));
    assertThat(AuthorityUtils.authorityListToSet(converter.convert(jwt))).contains("ROLE_ADMIN");
  }
}
