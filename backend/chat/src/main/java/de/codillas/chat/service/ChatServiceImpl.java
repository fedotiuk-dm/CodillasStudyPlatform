package de.codillas.chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.chat.api.dto.ChatMessageListResponse;
import de.codillas.chat.api.dto.ChatMessageResponse;
import de.codillas.chat.api.dto.ChatRoomResponse;
import de.codillas.chat.api.dto.CreateRoomRequest;
import de.codillas.chat.domain.model.ChatMessage;
import de.codillas.chat.domain.model.ChatRoom;
import de.codillas.chat.domain.model.ChatRoomMember;
import de.codillas.chat.domain.model.ChatRoomType;
import de.codillas.chat.domain.repository.ChatMessageRepository;
import de.codillas.chat.domain.repository.ChatRoomMemberRepository;
import de.codillas.chat.domain.repository.ChatRoomRepository;
import de.codillas.chat.mapper.ChatMapper;
import de.codillas.shared.domain.repository.GenericSpecification;
import de.codillas.shared.event.MessagePosted;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

  private final ChatRoomRepository roomRepository;
  private final ChatRoomMemberRepository memberRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatMapper mapper;
  private final ChatBroadcastService broadcast;
  private final ApplicationEventPublisher events;

  @Override
  public List<ChatRoomResponse> listMyRooms(UUID userId) {
    List<UUID> roomIds =
        memberRepository.findByUserId(userId).stream().map(ChatRoomMember::getRoomId).toList();
    return mapper.toRoomResponses(roomRepository.findAllById(roomIds));
  }

  @Override
  @Transactional
  public ChatRoomResponse createRoom(UUID creatorId, CreateRoomRequest request) {
    ChatRoom room = roomRepository.save(mapper.toRoom(request));
    Set<UUID> members = new LinkedHashSet<>(request.getMemberIds());
    members.add(creatorId);
    members.forEach(userId -> addMember(room.getId(), userId));
    return mapper.toRoomResponse(room);
  }

  @Override
  public ChatMessageListResponse getMessages(UUID userId, UUID roomId, Pageable pageable) {
    requireMember(roomId, userId);
    return mapper.toMessageListResponse(
        messageRepository.findByRoomId(
            roomId,
            GenericSpecification.withDefaultSort(pageable, ChatMessageRepository.NEWEST_FIRST)));
  }

  @Override
  @Transactional
  public ChatMessageResponse postMessage(UUID senderId, UUID roomId, String content) {
    requireMember(roomId, senderId);
    ChatMessage message = messageRepository.save(mapper.toMessage(roomId, senderId, content));
    ChatMessageResponse dto = mapper.toMessageResponse(message);
    broadcast.broadcastMessage(roomId, dto);
    events.publishEvent(new MessagePosted(roomId, message.getId(), senderId));
    return dto;
  }

  @Override
  @Transactional
  public void onStudentEnrolled(UUID groupId, UUID userId) {
    ChatRoom room =
        roomRepository
            .findByTypeAndReferenceId(ChatRoomType.GROUP, groupId)
            .orElseGet(() -> roomRepository.save(mapper.toGroupRoom(groupId)));
    addMember(room.getId(), userId);
  }

  private void addMember(UUID roomId, UUID userId) {
    if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
      memberRepository.save(mapper.toMember(roomId, userId));
    }
  }

  /** Membership is the access control — non-members are told the room does not exist. */
  private void requireMember(UUID roomId, UUID userId) {
    if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
      throw new NotFoundException("Room", roomId);
    }
  }
}
