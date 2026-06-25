package de.codillas.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.chat.api.dto.ChatMessageListResponse;
import de.codillas.chat.api.dto.ChatMessageResponse;
import de.codillas.chat.api.dto.ChatRoomResponse;
import de.codillas.chat.api.dto.CreateRoomRequest;

public interface ChatService {

  List<ChatRoomResponse> listMyRooms(UUID userId);

  ChatRoomResponse createRoom(UUID creatorId, CreateRoomRequest request);

  ChatMessageListResponse getMessages(UUID userId, UUID roomId, Pageable pageable);

  ChatMessageResponse postMessage(UUID senderId, UUID roomId, String content);

  /** Ensure the group's channel exists and the student is a member (fed by StudentEnrolled). */
  void onStudentEnrolled(UUID groupId, UUID userId);
}
