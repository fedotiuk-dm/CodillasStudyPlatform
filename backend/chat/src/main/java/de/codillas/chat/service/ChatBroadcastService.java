package de.codillas.chat.service;

import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import de.codillas.chat.api.dto.ChatMessageResponse;

import lombok.RequiredArgsConstructor;

/**
 * Pushes new messages to room subscribers over STOMP. The messaging template is optional — when the
 * WebSocket broker is absent (e.g. integration-test profile), broadcasting is a no-op and REST
 * still works.
 */
@Service
@RequiredArgsConstructor
public class ChatBroadcastService {

  private static final String ROOM_TOPIC = "/topic/chat/";

  private final ObjectProvider<SimpMessagingTemplate> templateProvider;

  public void broadcastMessage(UUID roomId, ChatMessageResponse message) {
    SimpMessagingTemplate template = templateProvider.getIfAvailable();
    if (template != null) {
      template.convertAndSend(ROOM_TOPIC + roomId, message);
    }
  }
}
