package de.codillas.config;

import java.security.Principal;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import de.codillas.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NonNull;

/**
 * Frame-level chat authorization: a STOMP SUBSCRIBE to {@code /topic/chat/{roomId}} is allowed only
 * for a member of that room. Membership is the access control (mirrors {@code chat.requireMember});
 * a non-member is denied at the channel before any broker subscription is created. Runs after
 * {@link StompAuthInterceptor} has set the user on CONNECT.
 */
@Component
@Profile("!integration-test")
@RequiredArgsConstructor
public class ChatSubscriptionInterceptor implements ChannelInterceptor {

  private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

  private final ChatService chatService;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      return message;
    }
    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
      return message; // other topics are covered by @EnableWebSocketSecurity's authenticated rule
    }
    Principal user = accessor.getUser();
    UUID roomId = parseRoomId(destination);
    if (user == null
        || roomId == null
        || !chatService.isMember(roomId, UUID.fromString(user.getName()))) {
      throw new AccessDeniedException("Not a member of chat room");
    }
    return message;
  }

  private static UUID parseRoomId(String destination) {
    try {
      return UUID.fromString(destination.substring(CHAT_TOPIC_PREFIX.length()));
    } catch (IllegalArgumentException _) {
      return null;
    }
  }
}
