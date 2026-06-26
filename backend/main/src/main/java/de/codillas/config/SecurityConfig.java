package de.codillas.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource-server security. Validates Keycloak JWTs; realm roles are read from the standard nested
 * {@code realm_access.roles} claim and mapped to {@code ROLE_*} by {@link
 * KeycloakRealmRolesConverter} (see the {@code jwtAuthenticationConverter} bean). Authorization is
 * enforced per endpoint via method security at module boundaries.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Maps {@code realm_access.roles} → {@code ROLE_*} authorities. Picked up by the HTTP resource
   * server (this filter chain) and the STOMP auth manager, so both paths share one role source.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter(
      KeycloakRealmRolesConverter rolesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
    return converter;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) {
    // CSRF disabled: stateless API authed by bearer JWT (no cookies/session) — CSRF doesn't apply.
    // CORS enabled so the browser SPA (localhost:3000) can call the API on a different origin; the
    // CorsFilter answers preflight OPTIONS before authorization, so they don't 401.
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        // The WebSocket handshake carries no Authorization header (browsers can't
                        // set
                        // one); auth happens on the STOMP CONNECT frame via StompAuthInterceptor.
                        "/ws/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
    return http.build();
  }

  /**
   * Allows the browser SPA origin(s) to call the API. Configurable via {@code
   * app.cors.allowed-origins}.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
