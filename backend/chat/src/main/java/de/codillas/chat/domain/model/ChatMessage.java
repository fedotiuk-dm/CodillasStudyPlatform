package de.codillas.chat.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A message posted to a room. {@code sentAt} is the auditable {@code createdAt}. */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChatMessage extends BaseAuditableEntity {

  @Column(name = "room_id", nullable = false)
  private UUID roomId;

  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Column(nullable = false, columnDefinition = "text")
  private String content;
}
