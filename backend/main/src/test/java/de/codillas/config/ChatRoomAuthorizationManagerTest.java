package de.codillas.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;

import de.codillas.chat.service.ChatService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomAuthorizationManager")
class ChatRoomAuthorizationManagerTest {

  @Mock private ChatService chatService;

  private static MessageAuthorizationContext<?> subscribeContext(String roomId) {
    Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
    return new MessageAuthorizationContext<>(message, Map.of("roomId", roomId));
  }

  private static Supplier<Authentication> caller(UUID userId) {
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn(userId.toString());
    return () -> auth;
  }

  @Test
  @DisplayName("grants a member subscribing to their room")
  void memberGranted() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(chatService.isMember(roomId, userId)).thenReturn(true);

    AuthorizationDecision decision =
        new ChatRoomAuthorizationManager(chatService)
            .check(caller(userId), subscribeContext(roomId.toString()));

    assertThat(decision.isGranted()).isTrue();
  }

  @Test
  @DisplayName("denies a non-member")
  void nonMemberDenied() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(chatService.isMember(roomId, userId)).thenReturn(false);

    AuthorizationDecision decision =
        new ChatRoomAuthorizationManager(chatService)
            .check(caller(userId), subscribeContext(roomId.toString()));

    assertThat(decision.isGranted()).isFalse();
  }

  @Test
  @DisplayName("denies a malformed room id without consulting the service")
  void malformedRoomDenied() {
    AuthorizationDecision decision =
        new ChatRoomAuthorizationManager(chatService)
            .check(caller(UUID.randomUUID()), subscribeContext("not-a-uuid"));

    assertThat(decision.isGranted()).isFalse();
  }
}
