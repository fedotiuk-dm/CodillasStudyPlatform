package de.codillas.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

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
import de.codillas.shared.event.MessagePosted;
import de.codillas.shared.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

  @Mock private ChatRoomRepository roomRepository;
  @Mock private ChatRoomMemberRepository memberRepository;
  @Mock private ChatMessageRepository messageRepository;
  @Mock private ChatMapper mapper;
  @Mock private ChatBroadcastService broadcast;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private ChatServiceImpl service;

  @Test
  @DisplayName("postMessage saves, broadcasts and publishes MessagePosted")
  void postMessage_savesBroadcastsPublishes() {
    UUID roomId = UUID.randomUUID();
    UUID senderId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    ChatMessage message =
        ChatMessage.builder().id(messageId).roomId(roomId).senderId(senderId).build();
    ChatMessageResponse dto = mock(ChatMessageResponse.class);
    when(memberRepository.existsByRoomIdAndUserId(roomId, senderId)).thenReturn(true);
    when(mapper.toMessage(roomId, senderId, "hi")).thenReturn(message);
    when(messageRepository.save(message)).thenReturn(message);
    when(mapper.toMessageResponse(message)).thenReturn(dto);

    assertThat(service.postMessage(senderId, roomId, "hi")).isSameAs(dto);
    verify(broadcast).broadcastMessage(roomId, dto);
    verify(events).publishEvent(new MessagePosted(roomId, messageId, senderId));
  }

  @Test
  @DisplayName("a non-member cannot read a room's messages")
  void getMessages_nonMember_notFound() {
    UUID roomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(memberRepository.existsByRoomIdAndUserId(roomId, userId)).thenReturn(false);
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () ->
                service.getMessages(
                    userId, roomId, org.springframework.data.domain.Pageable.unpaged()));
  }

  @Test
  @DisplayName("createRoom adds the requested members plus the creator")
  void createRoom_addsMembersAndCreator() {
    UUID creator = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    CreateRoomRequest request = mock(CreateRoomRequest.class);
    when(request.getMemberIds()).thenReturn(List.of(other));
    ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).type(ChatRoomType.DIRECT).build();
    ChatRoomResponse dto = mock(ChatRoomResponse.class);
    when(mapper.toRoom(request)).thenReturn(room);
    when(roomRepository.save(room)).thenReturn(room);
    when(memberRepository.existsByRoomIdAndUserId(eq(room.getId()), any())).thenReturn(false);
    when(mapper.toMember(eq(room.getId()), any())).thenReturn(new ChatRoomMember());
    when(memberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(mapper.toRoomResponse(room)).thenReturn(dto);

    assertThat(service.createRoom(creator, request)).isSameAs(dto);
    verify(mapper).toMember(room.getId(), creator);
    verify(mapper).toMember(room.getId(), other);
  }

  @Test
  @DisplayName(
      "creating a DIRECT room returns the existing DM between the same two users (no duplicate)")
  void createRoom_directExisting_returnsExistingRoom() {
    UUID creator = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();
    CreateRoomRequest request = mock(CreateRoomRequest.class);
    when(request.getType()).thenReturn(de.codillas.chat.api.dto.ChatRoomType.DIRECT);
    when(request.getMemberIds()).thenReturn(List.of(other));
    ChatRoom existing = ChatRoom.builder().id(roomId).type(ChatRoomType.DIRECT).build();
    ChatRoomResponse dto = mock(ChatRoomResponse.class);
    when(memberRepository.findByUserId(creator))
        .thenReturn(List.of(ChatRoomMember.builder().roomId(roomId).userId(creator).build()));
    when(memberRepository.findByUserId(other))
        .thenReturn(List.of(ChatRoomMember.builder().roomId(roomId).userId(other).build()));
    when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));
    when(memberRepository.countByRoomId(roomId)).thenReturn(2L);
    when(mapper.toRoomResponse(existing)).thenReturn(dto);

    assertThat(service.createRoom(creator, request)).isSameAs(dto);
    verify(roomRepository, never()).save(any());
    verify(mapper, never()).toRoom(any());
  }

  @Test
  @DisplayName("onStudentEnrolled creates the group room on first enrolment and adds the member")
  void onStudentEnrolled_createsRoomAndAddsMember() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ChatRoom room =
        ChatRoom.builder()
            .id(UUID.randomUUID())
            .type(ChatRoomType.GROUP)
            .referenceId(groupId)
            .build();
    when(roomRepository.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId))
        .thenReturn(Optional.empty());
    when(mapper.toGroupRoom(groupId)).thenReturn(room);
    when(roomRepository.save(room)).thenReturn(room);
    when(memberRepository.existsByRoomIdAndUserId(room.getId(), userId)).thenReturn(false);
    when(mapper.toMember(room.getId(), userId)).thenReturn(new ChatRoomMember());

    service.onStudentEnrolled(groupId, userId);

    verify(roomRepository).save(room);
    verify(memberRepository).save(any(ChatRoomMember.class));
  }

  @Test
  @DisplayName("onStudentEnrolled is idempotent for an existing membership")
  void onStudentEnrolled_existingMember_skips() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ChatRoom room =
        ChatRoom.builder()
            .id(UUID.randomUUID())
            .type(ChatRoomType.GROUP)
            .referenceId(groupId)
            .build();
    when(roomRepository.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId))
        .thenReturn(Optional.of(room));
    when(memberRepository.existsByRoomIdAndUserId(room.getId(), userId)).thenReturn(true);

    service.onStudentEnrolled(groupId, userId);

    verify(memberRepository, never()).save(any());
  }
}
