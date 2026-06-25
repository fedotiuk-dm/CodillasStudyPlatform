package de.codillas.chat.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A chat room. {@code referenceId} links a GROUP/ASSIGNMENT_THREAD room to its group/assignment.
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChatRoom extends BaseAuditableEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChatRoomType type;

  @Column(length = 200)
  private String name;

  @Column(name = "reference_id")
  private UUID referenceId;
}
