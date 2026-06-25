package de.codillas.chat.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Room membership — controls who can read and post. Unique per (room, user). */
@Entity
@Table(
    name = "chat_room_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChatRoomMember extends BaseAuditableEntity {

  @Column(name = "room_id", nullable = false)
  private UUID roomId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;
}
