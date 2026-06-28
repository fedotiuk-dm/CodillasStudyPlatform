package de.codillas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * STOMP-over-WebSocket setup. Handshake at {@code /ws}; clients publish to {@code /app/**} and
 * subscribe to {@code /topic/**} / {@code /user/**}. Authentication is established on CONNECT by
 * {@link StompAuthInterceptor}; {@code @EnableWebSocketSecurity} then guards every message.
 * Disabled under the {@code integration-test} profile (REST + events are tested without a live
 * broker).
 */
@Configuration
@Profile("!integration-test")
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthInterceptor stompAuthInterceptor;
  private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthInterceptor, chatSubscriptionInterceptor);
  }

  @Bean
  AuthorizationManager<Message<?>> messageAuthorizationManager(
      MessageMatcherDelegatingAuthorizationManager.Builder messages) {
    return messages
        .simpTypeMatchers(
            SimpMessageType.CONNECT,
            SimpMessageType.DISCONNECT,
            SimpMessageType.HEARTBEAT,
            SimpMessageType.UNSUBSCRIBE,
            SimpMessageType.OTHER)
        .permitAll()
        .simpDestMatchers("/app/**")
        .authenticated()
        .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**")
        .authenticated()
        .anyMessage()
        .denyAll()
        .build();
  }

  /** No-op CSRF interceptor — WebSocket is authed by the bearer JWT, not cookies. */
  @Bean(name = "csrfChannelInterceptor")
  ChannelInterceptor csrfChannelInterceptor() {
    return new ChannelInterceptor() {};
  }
}
