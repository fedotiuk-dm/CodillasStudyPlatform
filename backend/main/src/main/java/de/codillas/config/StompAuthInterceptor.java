package de.codillas.config;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;

/**
 * Authenticates the STOMP CONNECT frame from its {@code Authorization: Bearer <jwt>} header. STOMP
 * has no declarative equivalent of the HTTP resource server, so this small interceptor is the only
 * glue: it hands the raw token to the standard {@link AuthenticationManager} (a JWT provider built
 * from the auto-configured decoder + converter), so all validation and {@code roles}→{@code ROLE_*}
 * mapping stay in {@code application.yml}. {@code @EnableWebSocketSecurity} then propagates the
 * resulting authentication to every subsequent message.
 */
@Component
@Profile("!integration-test")
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

  private static final String BEARER = "Bearer ";

  private final AuthenticationManager messagingAuthenticationManager;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      String header = accessor.getFirstNativeHeader("Authorization");
      if (header != null && header.startsWith(BEARER)) {
        accessor.setUser(
            messagingAuthenticationManager.authenticate(
                new BearerTokenAuthenticationToken(header.substring(BEARER.length()))));
      }
    }
    return message;
  }
}
