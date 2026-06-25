package de.codillas.chat.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.chat.domain.model.ChatRoom;
import de.codillas.chat.domain.model.ChatRoomType;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

  Optional<ChatRoom> findByTypeAndReferenceId(ChatRoomType type, UUID referenceId);
}
