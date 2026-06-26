package de.codillas.chat.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import de.codillas.shared.event.DirectMessagePosted;
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
    Set<UUID> members = new LinkedHashSet<>(request.getMemberIds());
    members.add(creatorId);

    // A DM is canonical: one room per pair. Return the existing one instead of a duplicate.
    if (request.getType() == de.codillas.chat.api.dto.ChatRoomType.DIRECT && members.size() == 2) {
      UUID other = members.stream().filter(id -> !id.equals(creatorId)).findFirst().orElseThrow();
      Optional<ChatRoom> existing = findDirectRoom(creatorId, other);
      if (existing.isPresent()) {
        return mapper.toRoomResponse(existing.get());
      }
    }

    ChatRoom room = roomRepository.save(mapper.toRoom(request));
    members.forEach(userId -> addMember(room.getId(), userId));
    return mapper.toRoomResponse(room);
  }

  /** The DIRECT room shared by exactly these two users, if one already exists. */
  private Optional<ChatRoom> findDirectRoom(UUID a, UUID b) {
    Set<UUID> aRoomIds =
        memberRepository.findByUserId(a).stream()
            .map(ChatRoomMember::getRoomId)
            .collect(Collectors.toSet());
    return memberRepository.findByUserId(b).stream()
        .map(ChatRoomMember::getRoomId)
        .filter(aRoomIds::contains)
        .map(roomRepository::findById)
        .flatMap(Optional::stream)
        .filter(room -> room.getType() == ChatRoomType.DIRECT)
        .filter(room -> memberRepository.countByRoomId(room.getId()) == 2)
        .findFirst();
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
    notifyDirectRecipient(roomId, senderId);
    return dto;
  }

  /**
   * For a DIRECT room, alert the single other member — a DM has exactly one recipient, so we can
   * notify without presence tracking and without spamming a group channel. GROUP /
   * ASSIGNMENT_THREAD rooms rely on real-time delivery only.
   */
  private void notifyDirectRecipient(UUID roomId, UUID senderId) {
    ChatRoom room = roomRepository.findById(roomId).orElseThrow();
    if (room.getType() != ChatRoomType.DIRECT) {
      return;
    }
    memberRepository.findByRoomId(roomId).stream()
        .map(ChatRoomMember::getUserId)
        .filter(userId -> !userId.equals(senderId))
        .forEach(
            recipientId ->
                events.publishEvent(new DirectMessagePosted(roomId, recipientId, senderId)));
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
