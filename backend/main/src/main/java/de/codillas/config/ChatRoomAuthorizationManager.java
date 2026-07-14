package de.codillas.config;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.stereotype.Component;

import de.codillas.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

/**
 * Frame-level chat authorization: a STOMP SUBSCRIBE to {@code /topic/chat/{roomId}} is granted only
 * to a member of that room (mirrors {@code chat.requireMember}). Wired into {@link
 * WebSocketConfig}'s message {@code AuthorizationManager} via {@code
 * simpSubscribeDestMatchers("/topic/chat/{roomId}")} — Spring Security matches the SUBSCRIBE
 * command, extracts {@code roomId}, and denies non-members, so this bean carries only the domain
 * check, no manual frame plumbing.
 */
@Component
@Profile("!integration-test")
@RequiredArgsConstructor
public class ChatRoomAuthorizationManager {

  private final ChatService chatService;

  /** Grant when the authenticated caller is a member of the {@code {roomId}} being subscribed. */
  public AuthorizationDecision check(
      Supplier<? extends Authentication> authentication, MessageAuthorizationContext<?> context) {
    UUID roomId = parseUuid(context.getVariables().get("roomId"));
    Authentication auth = authentication.get();
    UUID userId = auth == null ? null : parseUuid(auth.getName());
    boolean member = roomId != null && userId != null && chatService.isMember(roomId, userId);
    return new AuthorizationDecision(member);
  }

  private static UUID parseUuid(String value) {
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }
}
