package de.codillas.notification.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A single in-app notification for one recipient. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Notification extends BaseAuditableEntity {

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "reference_id")
  private UUID referenceId;

  /** Structured render params, so the frontend can re-render the text in the viewer's locale. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  @Builder.Default
  private Map<String, String> params = new HashMap<>();

  @Column(nullable = false)
  @Builder.Default
  private boolean read = false;
}
