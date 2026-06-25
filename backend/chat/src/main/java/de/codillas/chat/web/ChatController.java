package de.codillas.chat.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.chat.api.ChatApi;
import de.codillas.chat.api.dto.ChatMessageListResponse;
import de.codillas.chat.api.dto.ChatMessageResponse;
import de.codillas.chat.api.dto.ChatRoomResponse;
import de.codillas.chat.api.dto.CreateRoomRequest;
import de.codillas.chat.api.dto.SendMessageRequest;
import de.codillas.chat.service.ChatService;
import de.codillas.shared.security.CurrentUser;
import de.codillas.shared.security.RequiresAuthenticated;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link ChatApi} (REST side; real-time is STOMP). */
@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

  private final ChatService chatService;
  private final CurrentUser currentUser;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<List<ChatRoomResponse>> listMyRooms() {
    return ResponseEntity.ok(chatService.listMyRooms(currentUser.id()));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<ChatRoomResponse> createRoom(CreateRoomRequest createRoomRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(chatService.createRoom(currentUser.id(), createRoomRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<ChatMessageListResponse> getMessages(UUID roomId, Pageable pageable) {
    return ResponseEntity.ok(chatService.getMessages(currentUser.id(), roomId, pageable));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<ChatMessageResponse> sendMessage(
      UUID roomId, SendMessageRequest sendMessageRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(chatService.postMessage(currentUser.id(), roomId, sendMessageRequest.getContent()));
  }
}
