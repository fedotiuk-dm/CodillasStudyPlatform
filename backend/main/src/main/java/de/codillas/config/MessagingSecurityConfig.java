package de.codillas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

/**
 * JWT authentication for STOMP, in its own config so it does not sit on the WebSocketConfig →
 * StompAuthInterceptor → AuthenticationManager cycle. Reuses the resource server's auto-configured
 * decoder + converter, so the {@code roles}→{@code ROLE_*} mapping stays in {@code application.yml}.
 */
@Configuration
@Profile("!integration-test")
public class MessagingSecurityConfig {

  @Bean
  AuthenticationManager messagingAuthenticationManager(
      JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
    JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
    provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
    return new ProviderManager(provider);
  }
}
