package de.codillas.chat.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.chat.domain.model.ChatMessage;
import de.codillas.shared.domain.BaseAuditableEntity_;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

  /** Newest first — chat history is read most-recent-down. */
  Sort NEWEST_FIRST = Sort.by(Sort.Order.desc(BaseAuditableEntity_.CREATED_AT));

  Page<ChatMessage> findByRoomId(UUID roomId, Pageable pageable);
}
