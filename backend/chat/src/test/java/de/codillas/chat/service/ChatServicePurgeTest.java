package de.codillas.chat.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.chat.domain.model.ChatRoom;
import de.codillas.chat.domain.model.ChatRoomType;
import de.codillas.chat.domain.repository.ChatMessageRepository;
import de.codillas.chat.domain.repository.ChatRoomMemberRepository;
import de.codillas.chat.domain.repository.ChatRoomRepository;
import de.codillas.chat.mapper.ChatMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService — purge on GroupDeleted")
class ChatServicePurgeTest {
  @Mock ChatRoomRepository roomRepository;
  @Mock ChatRoomMemberRepository memberRepository;
  @Mock ChatMessageRepository messageRepository;
  @Mock ChatMapper mapper;
  @Mock ChatBroadcastService broadcast;
  @Mock ApplicationEventPublisher events;
  @InjectMocks ChatServiceImpl service;

  @Test
  @DisplayName("deletes the group room with its messages and members")
  void purgesRoom() {
    UUID groupId = UUID.randomUUID();
    ChatRoom room = ChatRoom.builder().build();
    room.setId(UUID.randomUUID());
    when(roomRepository.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId))
        .thenReturn(Optional.of(room));

    service.onGroupDeleted(groupId);

    verify(messageRepository).deleteByRoomId(room.getId());
    verify(memberRepository).deleteByRoomId(room.getId());
    verify(roomRepository).delete(room);
  }

  @Test
  @DisplayName("does nothing when the group has no room")
  void noRoomNoDeletes() {
    UUID groupId = UUID.randomUUID();
    when(roomRepository.findByTypeAndReferenceId(ChatRoomType.GROUP, groupId))
        .thenReturn(Optional.empty());

    service.onGroupDeleted(groupId);

    verifyNoInteractions(messageRepository);
    verifyNoInteractions(memberRepository);
  }
}
