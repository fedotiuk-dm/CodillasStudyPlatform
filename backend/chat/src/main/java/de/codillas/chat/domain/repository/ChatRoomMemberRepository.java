package de.codillas.chat.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.chat.domain.model.ChatRoomMember;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

  boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

  List<ChatRoomMember> findByUserId(UUID userId);

  List<ChatRoomMember> findByRoomId(UUID roomId);

  long countByRoomId(UUID roomId);
}
