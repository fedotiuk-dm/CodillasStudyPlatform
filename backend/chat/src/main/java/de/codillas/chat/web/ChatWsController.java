package de.codillas.chat.web;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import de.codillas.chat.api.dto.SendMessageRequest;
import de.codillas.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

/**
 * STOMP entry point. Clients send to {@code /app/chat/{roomId}/send}; the saved message is
 * broadcast to subscribers of {@code /topic/chat/{roomId}}. The authenticated principal is the JWT
 * subject.
 */
@Controller
@RequiredArgsConstructor
public class ChatWsController {

  private final ChatService chatService;

  @MessageMapping("/chat/{roomId}/send")
  public void send(
      @DestinationVariable UUID roomId, @Payload SendMessageRequest request, Principal principal) {
    if (principal == null) {
      throw new IllegalStateException("Unauthenticated STOMP message");
    }
    chatService.postMessage(UUID.fromString(principal.getName()), roomId, request.getContent());
  }
}
