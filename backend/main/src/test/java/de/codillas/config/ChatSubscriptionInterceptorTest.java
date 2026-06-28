package de.codillas.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import de.codillas.chat.service.ChatService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatSubscriptionInterceptor")
class ChatSubscriptionInterceptorTest {

  @Mock private ChatService chatService;

  private static Message<?> subscribe(UUID roomId, UUID userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/chat/" + roomId);
    accessor.setUser(userId::toString); // Principal::getName
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("a non-member SUBSCRIBE to a room topic is rejected")
  void nonMemberRejected() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(chatService.isMember(roomId, userId)).thenReturn(false);
    ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(chatService);

    assertThatExceptionOfType(AccessDeniedException.class)
        .isThrownBy(
            () -> interceptor.preSend(subscribe(roomId, userId), mock(MessageChannel.class)));
  }

  @Test
  @DisplayName("a member SUBSCRIBE passes through unchanged")
  void memberAllowed() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(chatService.isMember(roomId, userId)).thenReturn(true);
    ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(chatService);

    Message<?> message = subscribe(roomId, userId);
    assertThat(interceptor.preSend(message, mock(MessageChannel.class))).isSameAs(message);
  }
}
