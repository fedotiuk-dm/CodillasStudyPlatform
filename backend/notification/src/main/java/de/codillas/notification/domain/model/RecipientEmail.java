package de.codillas.notification.domain.model;

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

/**
 * Recipient address read model (fed by UserEmailChanged) so email delivery can resolve a user id.
 */
@Entity
@Table(name = "recipient_email", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RecipientEmail extends BaseAuditableEntity {

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(nullable = false, length = 320)
  private String email;
}
