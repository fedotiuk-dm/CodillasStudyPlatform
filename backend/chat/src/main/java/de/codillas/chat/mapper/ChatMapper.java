package de.codillas.chat.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import de.codillas.chat.api.dto.ChatMessageListResponse;
import de.codillas.chat.api.dto.ChatMessageResponse;
import de.codillas.chat.api.dto.ChatRoomResponse;
import de.codillas.chat.api.dto.CreateRoomRequest;
import de.codillas.chat.domain.model.ChatMessage;
import de.codillas.chat.domain.model.ChatRoom;
import de.codillas.chat.domain.model.ChatRoomMember;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface ChatMapper {

  ChatRoomResponse toRoomResponse(ChatRoom room);

  List<ChatRoomResponse> toRoomResponses(List<ChatRoom> rooms);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  ChatRoom toRoom(CreateRoomRequest request);

  @Mapping(target = "sentAt", source = "createdAt")
  ChatMessageResponse toMessageResponse(ChatMessage message);

  ChatMessageListResponse toMessageListResponse(Page<ChatMessage> page);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  ChatMessage toMessage(UUID roomId, UUID senderId, String content);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "type", constant = "GROUP")
  ChatRoom toGroupRoom(UUID referenceId);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  ChatRoomMember toMember(UUID roomId, UUID userId);
}
